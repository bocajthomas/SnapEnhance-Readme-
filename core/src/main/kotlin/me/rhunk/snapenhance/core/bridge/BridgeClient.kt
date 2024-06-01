package me.rhunk.snapenhance.core.bridge


import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.DeadObjectException
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.Process
import android.util.Log
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import me.rhunk.snapenhance.bridge.AccountStorage
import me.rhunk.snapenhance.bridge.BridgeInterface
import me.rhunk.snapenhance.bridge.ConfigStateListener
import me.rhunk.snapenhance.bridge.DownloadCallback
import me.rhunk.snapenhance.bridge.SyncCallback
import me.rhunk.snapenhance.bridge.e2ee.E2eeInterface
import me.rhunk.snapenhance.bridge.logger.LoggerInterface
import me.rhunk.snapenhance.bridge.logger.TrackerInterface
import me.rhunk.snapenhance.bridge.scripting.IScripting
import me.rhunk.snapenhance.bridge.snapclient.MessagingBridge
import me.rhunk.snapenhance.bridge.storage.FileHandleManager
import me.rhunk.snapenhance.common.Constants
import me.rhunk.snapenhance.common.data.MessagingFriendInfo
import me.rhunk.snapenhance.common.data.MessagingGroupInfo
import me.rhunk.snapenhance.common.data.MessagingRuleType
import me.rhunk.snapenhance.common.data.SocialScope
import me.rhunk.snapenhance.common.util.toSerialized
import me.rhunk.snapenhance.core.ModContext
import java.util.concurrent.Executors
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class BridgeClient(
    private val context: ModContext
):  ServiceConnection {
    private var continuation: Continuation<Boolean>? = null
    private lateinit var service: BridgeInterface

    private val onConnectedCallbacks = mutableListOf<suspend () -> Unit>()

    fun addOnConnectedCallback(callback: suspend () -> Unit) {
        synchronized(onConnectedCallbacks) {
            onConnectedCallbacks.add(callback)
        }
    }

    suspend fun connect(onFailure: (Throwable) -> Unit): Boolean? {
        if (this::service.isInitialized && service.asBinder().pingBinder()) {
            return true
        }

        return withTimeoutOrNull(5000L) {
            suspendCancellableCoroutine { cancellableContinuation ->
                continuation = cancellableContinuation
                with(context.androidContext) {
                    //ensure the remote process is running
                    runCatching {
                        startActivity(Intent()
                            .setClassName(Constants.SE_PACKAGE_NAME, "me.rhunk.snapenhance.bridge.ForceStartActivity")
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                        )
                    }

                    runCatching {
                        val intent = Intent()
                            .setClassName(Constants.SE_PACKAGE_NAME, "me.rhunk.snapenhance.bridge.BridgeService")
                        runCatching {
                            if (this@BridgeClient::service.isInitialized) {
                                unbindService(this@BridgeClient)
                            }
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            bindService(
                                intent,
                                Context.BIND_AUTO_CREATE,
                                Executors.newSingleThreadExecutor(),
                                this@BridgeClient
                            )
                        } else {
                            XposedHelpers.callMethod(
                                this,
                                "bindServiceAsUser",
                                intent,
                                this@BridgeClient,
                                Context.BIND_AUTO_CREATE,
                                Handler(HandlerThread("BridgeClient").apply {
                                    start()
                                }.looper),
                                Process.myUserHandle()
                            )
                        }
                    }.onFailure {
                        onFailure(it)
                        continuation = null
                        cancellableContinuation.resume(false)
                    }
                }
            }
        }
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        this.service = BridgeInterface.Stub.asInterface(service)
        runBlocking {
            onConnectedCallbacks.forEach {
                runCatching {
                    it()
                }.onFailure {
                    context.log.error("Failed to run onConnectedCallback", it)
                }
            }
        }
        continuation?.resume(true)
        continuation = null
    }

    override fun onNullBinding(name: ComponentName) {
        Log.d("BridgeClient", "bridge null binding")
        continuation?.resume(false)
        continuation = null
    }

    override fun onServiceDisconnected(name: ComponentName) {
        continuation = null
    }

    private fun tryReconnect() {
        runBlocking {
            Log.d("BridgeClient", "service is dead, restarting")
            val canLoad = connect {
                Log.e("BridgeClient", "connection failed", it)
                context.softRestartApp()
            }
            if (canLoad != true) {
                Log.e("BridgeClient", "failed to reconnect to service", Throwable())
                context.softRestartApp()
            }
        }
    }

    private fun <T> safeServiceCall(block: () -> T): T {
        return runCatching {
            block()
        }.getOrElse { throwable ->
            if (throwable is DeadObjectException) {
                tryReconnect()
                return@getOrElse runCatching {
                    block()
                }.getOrElse {
                    Log.e("BridgeClient", "service call failed", it)
                    if (it is DeadObjectException) {
                        context.softRestartApp()
                    }
                    throw it
                }
            }
            throw throwable
        }
    }

    fun broadcastLog(tag: String, level: String, message: String) {
        message.chunked(1024 * 256).forEach {
            safeServiceCall {
                service.broadcastLog(tag, level, it)
            }
        }
    }

    fun getApplicationApkPath(): String = safeServiceCall { service.applicationApkPath }

    fun enqueueDownload(intent: Intent, callback: DownloadCallback) = safeServiceCall {
        service.enqueueDownload(intent, callback)
    }

    fun convertMedia(
        input: ParcelFileDescriptor,
        inputExtension: String,
        outputExtension: String,
        audioCodec: String?,
        videoCodec: String?
    ): ParcelFileDescriptor? = safeServiceCall {
        service.convertMedia(input, inputExtension, outputExtension, audioCodec, videoCodec)
    }

    fun sync(callback: SyncCallback) {
        if (!context.database.hasMain()) return
        safeServiceCall {
            service.sync(callback)
        }
    }

    fun triggerSync(scope: SocialScope, id: String) = safeServiceCall {
        service.triggerSync(scope.key, id)
    }

    fun passGroupsAndFriends(groups: List<MessagingGroupInfo>, friends: List<MessagingFriendInfo>) =
        safeServiceCall {
            service.passGroupsAndFriends(
                groups.mapNotNull { it.toSerialized() },
                friends.mapNotNull { it.toSerialized() }
            )
        }

    fun getRules(targetUuid: String): List<MessagingRuleType> = safeServiceCall {
        service.getRules(targetUuid).mapNotNull { MessagingRuleType.getByName(it) }
    }

    fun getRuleIds(ruleType: MessagingRuleType): List<String> = safeServiceCall {
        service.getRuleIds(ruleType.key)
    }

    fun setRule(targetUuid: String, type: MessagingRuleType, state: Boolean) = safeServiceCall {
        service.setRule(targetUuid, type.key, state)
    }

    fun getScriptingInterface(): IScripting = safeServiceCall { service.scriptingInterface }

    fun getE2eeInterface(): E2eeInterface = safeServiceCall { service.e2eeInterface }

    fun getMessageLogger(): LoggerInterface = safeServiceCall { service.logger }

    fun getTracker(): TrackerInterface = safeServiceCall { service.tracker }

    fun getAccountStorage(): AccountStorage = safeServiceCall { service.accountStorage }

    fun getFileHandlerManager(): FileHandleManager = safeServiceCall { service.fileHandleManager }

    fun registerMessagingBridge(bridge: MessagingBridge) = safeServiceCall { service.registerMessagingBridge(bridge) }

    fun openSettingsOverlay() = safeServiceCall { service.openSettingsOverlay() }
    fun closeSettingsOverlay() = safeServiceCall { service.closeSettingsOverlay() }

    fun registerConfigStateListener(listener: ConfigStateListener) = safeServiceCall { service.registerConfigStateListener(listener) }

    fun getDebugProp(name: String, defaultValue: String? = null): String? = safeServiceCall { service.getDebugProp(name, defaultValue) }
}
