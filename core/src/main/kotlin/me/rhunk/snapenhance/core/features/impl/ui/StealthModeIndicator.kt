package me.rhunk.snapenhance.core.features.impl.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.Shape
import androidx.core.content.res.use
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rhunk.snapenhance.core.event.events.impl.BindViewEvent
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.features.impl.spying.StealthMode
import me.rhunk.snapenhance.core.ui.addForegroundDrawable
import me.rhunk.snapenhance.core.ui.randomTag
import me.rhunk.snapenhance.core.ui.removeForegroundDrawable
import me.rhunk.snapenhance.core.util.EvictingMap
import me.rhunk.snapenhance.core.util.ktx.getDimens
import me.rhunk.snapenhance.core.util.ktx.getIdentifier

class StealthModeIndicator : Feature("StealthModeIndicator") {
    private val stealthMode by lazy { context.feature(StealthMode::class) }
    private val listeners = EvictingMap<String, (Boolean) -> Unit>(100)

    inner class UpdateHandler {
        private var fetchJob: Job? = null
        private var listener = { _: Boolean -> }

        private fun requestUpdate(conversationId: String) {
            fetchJob?.cancel()
            fetchJob = context.coroutineScope.launch {
                val isStealth = stealthMode.getState(conversationId)
                withContext(Dispatchers.Main) {
                    listener(isStealth)
                }
            }
        }

        fun subscribe(conversationId: String, onStateChange: (Boolean) -> Unit) {
            listener = onStateChange.also {
                listeners[conversationId] = it
            }
            requestUpdate(conversationId)
        }
    }

    private val stealthModeIndicatorTag = randomTag()

    override fun init() {
        if (!context.config.userInterface.stealthModeIndicator.get()) return

        onNextActivityCreate {
            val secondaryTextSize = context.resources.getDimens("ff_feed_cell_secondary_text_size").toFloat()
            val sigColorTextPrimary = context.mainActivity!!.obtainStyledAttributes(
                intArrayOf(context.resources.getIdentifier("sigColorTextPrimary", "attr"))
            ).use { it.getColor(0, 0) }

            stealthMode.addStateListener { conversationId, state ->
                runCatching {
                    listeners[conversationId]?.invoke(state)
                }.onFailure {
                    context.log.error("Failed to update stealth mode indicator", it)
                }
            }

            context.event.subscribe(BindViewEvent::class) { event ->
                fun updateStealthIndicator(isStealth: Boolean = true) {
                    event.view.removeForegroundDrawable("stealthModeIndicator")
                    if (!isStealth || !event.view.isAttachedToWindow) return
                    event.view.addForegroundDrawable("stealthModeIndicator", ShapeDrawable(object : Shape() {
                        override fun draw(canvas: Canvas, paint: Paint) {
                            paint.textSize = secondaryTextSize
                            paint.color = sigColorTextPrimary
                            canvas.drawText(
                                "\uD83D\uDC7B",
                                0f,
                                canvas.height.toFloat() - secondaryTextSize / 2,
                                paint
                            )
                        }
                    }))
                }

                event.friendFeedItem { conversationId ->
                    val updateHandler = event.view.getTag(stealthModeIndicatorTag) as? UpdateHandler ?: run {
                        val handler = UpdateHandler()
                        event.view.setTag(stealthModeIndicatorTag, handler)
                        handler
                    }

                    event.view.post {
                        synchronized(listeners) {
                            updateHandler.subscribe(conversationId) { isStealth ->
                                updateStealthIndicator(isStealth)
                            }
                        }
                    }
                    return@subscribe
                }

                event.view.setTag(stealthModeIndicatorTag, null)
            }
        }
    }
}