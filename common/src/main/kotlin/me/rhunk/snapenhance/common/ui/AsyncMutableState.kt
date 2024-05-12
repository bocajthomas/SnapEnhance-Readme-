package me.rhunk.snapenhance.common.ui

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.CopyOnWriteArrayList

class AsyncUpdateDispatcher(
    val updateOnFirstComposition: Boolean = true
) {
    private val callbacks = CopyOnWriteArrayList<suspend () -> Unit>()

    suspend fun dispatch() {
        callbacks.forEach { it() }
    }

    fun addCallback(callback: suspend () -> Unit) {
        callbacks.add(callback)
    }

    fun removeCallback(callback: suspend () -> Unit) {
        callbacks.remove(callback)
    }
}

@Composable
fun rememberAsyncUpdateDispatcher(): AsyncUpdateDispatcher {
    return remember { AsyncUpdateDispatcher() }
}

@Composable
private fun <T> rememberCommonState(
    initialState: () -> T,
    setter: suspend T.() -> Unit,
    updateDispatcher: AsyncUpdateDispatcher? = null,
    keys: Array<*> = emptyArray<Any>(),
): T {
    return remember { initialState() }.apply {
        var asyncSetCallback by remember { mutableStateOf(suspend {}) }

        LaunchedEffect(Unit) {
            asyncSetCallback = { setter(this@apply) }
            updateDispatcher?.addCallback(asyncSetCallback)
        }

        DisposableEffect(Unit) {
            onDispose { updateDispatcher?.removeCallback(asyncSetCallback) }
        }

        if (updateDispatcher?.updateOnFirstComposition != false) {
            LaunchedEffect(*keys) {
                setter(this@apply)
            }
        }
    }
}

@Composable
fun <T> rememberAsyncMutableState(
    defaultValue: T,
    updateDispatcher: AsyncUpdateDispatcher? = null,
    keys: Array<*> = emptyArray<Any>(),
    getter: () -> T,
): MutableState<T> {
    return rememberCommonState(
        initialState = { mutableStateOf(defaultValue) },
        setter = {
            withContext(Dispatchers.Main) {
                value = withContext(Dispatchers.IO) {
                    getter()
                }
            }
        },
        updateDispatcher = updateDispatcher,
        keys = keys,
    )
}

@Composable
fun <T> rememberAsyncMutableStateList(
    defaultValue: List<T>,
    updateDispatcher: AsyncUpdateDispatcher? = null,
    keys: Array<*> = emptyArray<Any>(),
    getter: () -> List<T>,
): SnapshotStateList<T> {
    return rememberCommonState(
        initialState = { mutableStateListOf<T>().apply {
            addAll(defaultValue)
        }},
        setter = {
            withContext(Dispatchers.Main) {
                clear()
                addAll(withContext(Dispatchers.IO) {
                    getter()
                })
            }
        },
        updateDispatcher = updateDispatcher,
        keys = keys,
    )
}

