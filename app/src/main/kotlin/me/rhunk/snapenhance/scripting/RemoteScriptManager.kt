package me.rhunk.snapenhance.scripting

import android.annotation.SuppressLint
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.documentfile.provider.DocumentFile
import me.rhunk.snapenhance.RemoteSideContext
import me.rhunk.snapenhance.bridge.scripting.AutoReloadListener
import me.rhunk.snapenhance.bridge.scripting.IPCListener
import me.rhunk.snapenhance.bridge.scripting.IScripting
import me.rhunk.snapenhance.common.scripting.ScriptRuntime
import me.rhunk.snapenhance.common.scripting.bindings.BindingSide
import me.rhunk.snapenhance.common.scripting.impl.ConfigInterface
import me.rhunk.snapenhance.common.scripting.impl.ConfigTransactionType
import me.rhunk.snapenhance.common.scripting.type.ModuleInfo
import me.rhunk.snapenhance.core.util.ktx.toParcelFileDescriptor
import me.rhunk.snapenhance.scripting.impl.IPCListeners
import me.rhunk.snapenhance.scripting.impl.ManagerIPC
import me.rhunk.snapenhance.scripting.impl.ManagerScriptConfig
import me.rhunk.snapenhance.storage.isScriptEnabled
import me.rhunk.snapenhance.storage.syncScripts
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.InputStream
import kotlin.system.exitProcess

class RemoteScriptManager(
    val context: RemoteSideContext,
) : IScripting.Stub() {
    val runtime = ScriptRuntime(context.androidContext, context.log).apply {
        scripting = this@RemoteScriptManager
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder().build()
    }

    private var autoReloadListener: AutoReloadListener? = null
    private val autoReloadHandler by lazy {
        AutoReloadHandler(context.coroutineScope) {
            runCatching {
                autoReloadListener?.restartApp()
                if (context.config.root.scripting.autoReload.getNullable() == "all") {
                    exitProcess(1)
                }
            }.onFailure {
                context.log.warn("Failed to restart app")
                autoReloadListener = null
            }
        }.apply {
            start()
        }
    }

    private val cachedModuleInfo = mutableMapOf<String, ModuleInfo>()
    private val ipcListeners = IPCListeners()

    fun sync() {
        cachedModuleInfo.clear()
        getScriptFileNames().forEach { name ->
            runCatching {
                getScriptInputStream(name) { stream ->
                    stream?.use {
                        runtime.getModuleInfo(it).also { info ->
                            cachedModuleInfo[name] = info
                        }
                    }
                }
            }.onFailure {
                context.log.error("Failed to load module info for $name", it)
            }
        }

        context.database.syncScripts(cachedModuleInfo.values.toList())
    }

    fun init() {
        runtime.buildModuleObject = { module ->
            putConst("currentSide", this, BindingSide.MANAGER.key)
            module.registerBindings(
                ManagerIPC(ipcListeners),
                ManagerScriptConfig(this@RemoteScriptManager)
            )
        }

        sync()
        enabledScripts.forEach { name ->
            runCatching {
                loadScript(name)
            }.onFailure {
                context.log.error("Failed to load script $name", it)
            }
        }
    }

    fun getModulePath(name: String): String? {
        return cachedModuleInfo.entries.find { it.value.name == name }?.key
    }

    fun loadScript(path: String) {
        val content = getScriptContent(path) ?: return
        runtime.load(path, content)
        if (context.config.root.scripting.autoReload.getNullable() != null) {
            autoReloadHandler.addFile(getScriptsFolder()?.findFile(path) ?: return)
        }
    }

    fun unloadScript(scriptPath: String) {
        runtime.unload(scriptPath)
    }

    @SuppressLint("Recycle")
    private fun <R> getScriptInputStream(name: String, callback: (InputStream?) -> R): R {
        val file = getScriptsFolder()?.findFile(name) ?: return callback(null)
        return context.androidContext.contentResolver.openInputStream(file.uri)?.let(callback) ?: callback(null)
    }

    fun getModuleDataFolder(moduleFileName: String): File {
        return context.androidContext.filesDir.resolve("modules").resolve(moduleFileName).also {
            if (!it.exists()) {
                it.mkdirs()
            }
        }
    }

    fun getScriptsFolder() = runCatching {
        DocumentFile.fromTreeUri(context.androidContext, Uri.parse(context.config.root.scripting.moduleFolder.get()))
    }.getOrNull()

    private fun getScriptFileNames(): List<String> {
        return (getScriptsFolder() ?: return emptyList()).listFiles().filter { it.name?.endsWith(".js") ?: false }.map { it.name!! }
    }

    fun importFromUrl(
        url: String
    ): ModuleInfo {
        val response = okHttpClient.newCall(Request.Builder().url(url).build()).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to fetch script. Code: ${response.code}")
        }
        response.body.byteStream().use { inputStream ->
            val bufferedInputStream = inputStream.buffered()
            bufferedInputStream.mark(0)
            val moduleInfo = runtime.readModuleInfo(bufferedInputStream.bufferedReader())
            bufferedInputStream.reset()

            val scriptPath = moduleInfo.name + ".js"
            val scriptFile = getScriptsFolder()?.findFile(scriptPath) ?: getScriptsFolder()?.createFile("text/javascript", scriptPath)
                ?: throw Exception("Failed to create script file")

            context.androidContext.contentResolver.openOutputStream(scriptFile.uri)?.use { output ->
                bufferedInputStream.copyTo(output)
            }

            sync()
            loadScript(scriptPath)
            runtime.removeModule(scriptPath)
            return moduleInfo
        }
    }

    override fun getEnabledScripts(): List<String> {
        return runCatching {
            getScriptFileNames().filter {
                context.database.isScriptEnabled(cachedModuleInfo[it]?.name ?: return@filter false)
            }
        }.onFailure {
            context.log.error("Failed to get enabled scripts", it)
        }.getOrDefault(emptyList())
    }

    override fun getScriptContent(moduleName: String): ParcelFileDescriptor? {
        if (moduleName.startsWith("composer/")) {
            return runCatching {
                context.androidContext.assets.open("composer/${moduleName.removePrefix("composer/")}")
                    .toParcelFileDescriptor(context.coroutineScope)
            }.getOrNull()
        }
        return getScriptInputStream(moduleName) { it?.toParcelFileDescriptor(context.coroutineScope) }
    }

    override fun registerIPCListener(channel: String, eventName: String, listener: IPCListener) {
        ipcListeners.getOrPut(channel) { mutableMapOf() }.getOrPut(eventName) { mutableSetOf() }.add(listener)
    }

    override fun sendIPCMessage(channel: String, eventName: String, args: Array<out String>) {
        runCatching {
            ipcListeners[channel]?.get(eventName)?.toList()?.forEach {
                it.onMessage(args)
            }
        }.onFailure {
            context.log.error("Failed to send message for $eventName", it)
        }
    }

    override fun configTransaction(
        module: String?,
        action: String,
        key: String?,
        value: String?,
        save: Boolean
    ): String? {
        val scriptConfig = runtime.getModuleByName(module ?: return null)?.getBinding(ConfigInterface::class) ?: return null.also {
            context.log.warn("Failed to get config interface for $module")
        }
        val transactionType = ConfigTransactionType.fromKey(action)

        return runCatching {
            scriptConfig.run {
                if (transactionType == ConfigTransactionType.GET) {
                    return get(key ?: return@runCatching null, value)
                }
                when (transactionType) {
                    ConfigTransactionType.SET -> set(key ?: return@runCatching null, value, save)
                    ConfigTransactionType.SAVE -> save()
                    ConfigTransactionType.LOAD -> load()
                    ConfigTransactionType.DELETE -> deleteConfig()
                    else -> {}
                }
                null
            }
        }.onFailure {
            context.log.error("Failed to perform config transaction", it)
        }.getOrDefault("")
    }

    override fun registerAutoReloadListener(listener: AutoReloadListener?) {
        autoReloadListener = listener
    }
}