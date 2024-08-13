package me.rhunk.snapenhance.core.features.impl

import android.system.Os
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import me.rhunk.snapenhance.core.features.Feature

class SecurityFeatures : Feature("Security Features") {
    private fun transact(option: Int) = Os.prctl(option, 0, 0, 0, 0)

    private val token by lazy {
        runCatching { transact(0) }.getOrNull()
    }

    private fun getStatus() = token?.run {
        transact(this).toString(2).padStart(32, '0').count { it == '1' }
    }

    override fun init() {
        token // pre init token

        context.inAppOverlay.addCustomComposable {
            var statusText by remember {
                mutableStateOf("")
            }
            var textColor by remember {
                mutableStateOf(Color.Red)
            }

            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    while (true) {
                        val status = getStatus()
                        withContext(Dispatchers.Main) {
                            if (status == null || status == 0) {
                                textColor = Color.Red
                                statusText = "sif not loaded. Can't get status"
                            } else {
                                textColor = Color.Green
                                statusText = "sif = $status"
                            }
                        }
                        delay(1000)
                    }
                }
            }

            Text(
                text = statusText,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .background(Color.Black, shape = RoundedCornerShape(5.dp))
                    .padding(3.dp),
                fontSize = 10.sp,
                color = textColor
            )
        }
    }
}