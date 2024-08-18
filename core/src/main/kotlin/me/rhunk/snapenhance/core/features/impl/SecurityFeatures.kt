package me.rhunk.snapenhance.core.features.impl

import android.system.Os
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import me.rhunk.snapenhance.common.util.protobuf.ProtoEditor
import me.rhunk.snapenhance.common.util.protobuf.ProtoReader
import me.rhunk.snapenhance.core.event.events.impl.UnaryCallEvent
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.util.ktx.setObjectField
import java.io.FileDescriptor

class SecurityFeatures : Feature("Security Features") {
    private fun transact(option: Int, option2: Long) = kotlin.runCatching { Os.prctl(option, option2, 0, 0, 0) }.getOrNull()

    private val token by lazy {
        transact(0, 0)
    }

    private fun getStatus() = token?.run {
        transact(this, 0)?.toString(2)?.padStart(32, '0')?.count { it == '1' }
    }

    override fun init() {
        token // pre init token

        context.event.subscribe(UnaryCallEvent::class) { event ->
            if (!event.uri.contains("/Login")) return@subscribe

            // intercept login response
            event.addResponseCallback {
                val response = ProtoReader(buffer)
                val isBlocked = when {
                    event.uri.contains("TLv") -> response.getVarInt(1) == 14L
                    else -> response.getVarInt(1) == 16L
                }

                val errorDataIndex = when {
                    response.contains(11) -> 11
                    response.contains(10) -> 10
                    response.contains(8) -> 8
                    else -> return@addResponseCallback
                }

                if (isBlocked) {
                    val status = transact(token ?: return@addResponseCallback, 1)?.let {
                        val buffer = ByteArray(8192)
                        val fd = FileDescriptor().apply {
                            setObjectField("descriptor", it)
                        }
                        val read = Os.read(fd, buffer, 0, buffer.size)
                        Os.close(fd)
                        buffer.copyOfRange(0, read).decodeToString()
                    }!!

                    buffer = ProtoEditor(buffer).apply {
                        edit(errorDataIndex) {
                            remove(1)
                            addString(1, status)
                        }
                    }.toByteArray()
                }
            }
        }

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
                                statusText = "SIF not loaded!"
                            } else {
                                textColor = Color.Green
                                statusText = "SIF = $status"
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