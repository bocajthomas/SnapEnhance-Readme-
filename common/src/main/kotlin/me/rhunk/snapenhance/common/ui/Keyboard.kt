package me.rhunk.snapenhance.common.ui


import android.view.ViewTreeObserver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.runtime.getValue

@Composable
fun keyboardState(): State<Boolean> {
    val keyboardState = remember { mutableStateOf(false) }
    val localView = LocalView.current
    val viewTreeObserver = localView.viewTreeObserver

    DisposableEffect(viewTreeObserver) {
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            keyboardState.value = ViewCompat.getRootWindowInsets(localView)
                ?.isVisible(WindowInsetsCompat.Type.ime()) != false
        }
        viewTreeObserver.addOnGlobalLayoutListener(listener)
        onDispose {
            viewTreeObserver.takeIf { it.isAlive }?.removeOnGlobalLayoutListener(listener)
        }
    }

    return keyboardState
}

@Composable
fun AutoClearKeyboardFocus(
    onFocusClear: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    val keyboardState by keyboardState()

    if (!keyboardState) {
        onFocusClear()
        focusManager.clearFocus()
    }
}
