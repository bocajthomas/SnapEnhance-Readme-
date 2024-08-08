package me.rhunk.snapenhance.core.features

import android.app.Activity
import kotlinx.coroutines.launch
import me.rhunk.snapenhance.core.ModContext

abstract class Feature(
    val key: String
) {
    lateinit var context: ModContext
    lateinit var registerNextActivityCallback: ((Activity) -> Unit) -> Unit

    protected fun defer(block: suspend () -> Unit) {
        context.coroutineScope.launch {
            runCatching {
                block()
            }.onFailure {
                context.log.error("Failed to run defer callback", it)
            }
        }
    }

    protected fun onNextActivityCreate(defer: Boolean = false, block: (Activity) -> Unit) {
        if (defer) {
            registerNextActivityCallback {
                defer {
                    block(it)
                }
            }
            return
        }
        registerNextActivityCallback(block)
    }

    open fun init() {}


    protected fun findClass(name: String): Class<*> {
        return context.androidContext.classLoader.loadClass(name)
    }

    protected fun runOnUiThread(block: () -> Unit) {
        context.runOnUiThread(block)
    }
}