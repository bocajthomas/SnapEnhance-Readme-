package me.rhunk.snapenhance.core.features.impl.experiments

import android.os.ParcelFileDescriptor
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.rhunk.snapenhance.common.ui.AppMaterialTheme
import me.rhunk.snapenhance.common.ui.createComposeAlertDialog
import me.rhunk.snapenhance.common.ui.createComposeView
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.features.FeatureLoadParams
import me.rhunk.snapenhance.core.features.impl.downloader.MediaDownloader
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.Hooker
import me.rhunk.snapenhance.core.util.hook.hook
import me.rhunk.snapenhance.core.wrapper.impl.composer.ComposerMarshaller
import me.rhunk.snapenhance.nativelib.NativeLib
import java.lang.reflect.Proxy
import kotlin.math.absoluteValue
import kotlin.random.Random

class ComposerHooks: Feature("ComposerHooks", loadParams = FeatureLoadParams.INIT_SYNC) {
    private val config by lazy { context.config.experimental.nativeHooks.composerHooks }
    private val exportedFunctionName = Random.nextInt().absoluteValue.toString(16)

    private val composerConsole by lazy {
        createComposeAlertDialog(context.mainActivity!!) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                var result by remember { mutableStateOf("") }
                var codeContent by remember { mutableStateOf("") }

                Text("Composer Console", fontSize = 18.sp, fontWeight = FontWeight.Bold)

                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle.Default.copy(fontSize = 12.sp),
                    value = codeContent,
                    placeholder = { Text("Enter your JS code here:") },
                    onValueChange = {
                        codeContent = it
                    }
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        context.log.verbose("input: $codeContent", "ComposerConsole")
                        result = "Running..."
                        context.coroutineScope.launch {
                            result = (context.native.composerEval("""
                                (() => {
                                    try {
                                        $codeContent
                                    } catch (e) {
                                        return e.toString()
                                    }
                                })()
                            """.trimIndent()) ?: "(no result)").also {
                                context.log.verbose("result: $it", "ComposerConsole")
                            }
                        }
                    }
                ) {
                    Text("Run")
                }

                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(result)
                }
            }
        }
    }

    private val composerConsoleTag = Random.nextLong().toString()

    private fun injectConsole() {
        val root = context.mainActivity!!.findViewById<FrameLayout>(android.R.id.content)
        root.post {
            if (root.findViewWithTag<View>(composerConsoleTag) != null) return@post
            root.addView(createComposeView(root.context) {
                AppMaterialTheme {
                    FilledIconButton(
                        onClick = {
                            composerConsole.show()
                        },
                        modifier = Modifier.padding(top = 100.dp, end = 16.dp)
                    ) {
                        Icon(Icons.Default.BugReport, contentDescription = "Debug Console")
                    }
                }
            }.apply {
                tag = composerConsoleTag
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                    gravity = android.view.Gravity.TOP or android.view.Gravity.END
                }
            })
        }
    }

    private fun newComposerFunction(block: (composerMarshaller: ComposerMarshaller) -> Boolean): Any {
        val composerFunctionClass = findClass("com.snap.composer.callable.ComposerFunction")
        return Proxy.newProxyInstance(
            composerFunctionClass.classLoader,
            arrayOf(composerFunctionClass)
        ) { _, method, args ->
            if (method.name != "perform") return@newProxyInstance null
            block(ComposerMarshaller(args?.get(0) ?: return@newProxyInstance false))
        }
    }

    private fun getConfig(): Map<String, Any> {
        return mapOf<String, Any>(
            "operaDownloadButton" to context.config.downloader.operaDownloadButton.get(),
            "bypassCameraRollLimit" to config.bypassCameraRollLimit.get(),
            "showFirstCreatedUsername" to config.showFirstCreatedUsername.get(),
            "composerConsole" to config.composerConsole.get(),
            "composerLogs" to config.composerLogs.get()
        )
    }

    private fun handleExportCall(composerMarshaller: ComposerMarshaller): Boolean {
        val argc = composerMarshaller.getSize()
        if (argc < 1) return false
        val action = composerMarshaller.getUntyped(0) as? String ?: return false

        when (action) {
            "getConfig" -> composerMarshaller.pushUntyped(getConfig())
            "showToast" -> {
                if (argc < 2) return false
                context.shortToast(composerMarshaller.getUntyped(1) as? String ?: return false)
            }
            "downloadLastOperaMedia" -> context.feature(MediaDownloader::class).downloadLastOperaMediaAsync(composerMarshaller.getUntyped(1) == true)
            "getFriendInfoByUsername" -> {
                if (argc < 2) return false
                val username = composerMarshaller.getUntyped(1) as? String ?: return false
                runCatching {
                    composerMarshaller.pushUntyped(context.database.getFriendInfoByUsername(username)?.let {
                        context.gson.toJson(it)
                    })
                }.onFailure {
                    composerMarshaller.pushUntyped(null)
                }
            }
            "log" -> {
                if (argc < 3) return false
                val logLevel = composerMarshaller.getUntyped(1) as? String ?: return false
                val message = composerMarshaller.getUntyped(2) as? String ?: return false

                val tag = "ComposerLogs"

                when (logLevel) {
                    "log" -> context.log.verbose(message, tag)
                    "debug" -> context.log.debug(message, tag)
                    "info" -> context.log.info(message, tag)
                    "warn" -> context.log.warn(message, tag)
                    "error" -> context.log.error(message, tag)
                }
            }
            else -> context.log.warn("Unknown action: $action", "Composer")
        }

        return true
    }

    private fun loadHooks() {
        if (!NativeLib.initialized) {
            context.log.error("ComposerHooks cannot be loaded without NativeLib")
            return
        }
        val loaderScript = context.scriptRuntime.scripting.getScriptContent("composer/loader.js")?.let { pfd ->
            ParcelFileDescriptor.AutoCloseInputStream(pfd).use {
                it.readBytes().toString(Charsets.UTF_8)
            }
        } ?: run {
            context.log.error("Failed to load composer loader script")
            return
        }

        context.native.setComposerLoader("""
            (() => { const callExport = require('composer_core/src/DeviceBridge')["$exportedFunctionName"]; try { $loaderScript
                } catch (e) {
                    try {
                        callExport("log", "error", e.toString() + "\n" + e.stack);
                    } catch (t) {}
                }
            })();
        """.trimIndent().trim())

        if (config.composerConsole.get()) {
            injectConsole()
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun init() {
        if (config.globalState != true) return
        findClass("com.snapchat.client.composer.NativeBridge").apply {
            hook("createViewLoaderManager", HookStage.AFTER) { loadHooks() }
            hook("registerNativeModuleFactory", HookStage.BEFORE) { param ->
                val moduleFactory = param.argNullable<Any>(1) ?: return@hook
                if (moduleFactory.javaClass.getMethod("getModulePath").invoke(moduleFactory)?.toString() != "DeviceBridge") return@hook
                Hooker.ephemeralHookObjectMethod(moduleFactory.javaClass, moduleFactory, "loadModule", HookStage.AFTER) { methodParam ->
                    val functions = methodParam.getResult() as? MutableMap<String, Any> ?: return@ephemeralHookObjectMethod
                    functions[exportedFunctionName] = newComposerFunction {
                        handleExportCall(it)
                    }
                }
            }
        }
    }
}