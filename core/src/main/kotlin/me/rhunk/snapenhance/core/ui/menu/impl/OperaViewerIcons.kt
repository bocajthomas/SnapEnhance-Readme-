package me.rhunk.snapenhance.core.ui.menu.impl

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rhunk.snapenhance.common.ui.createComposeView
import me.rhunk.snapenhance.core.event.events.impl.AddViewEvent
import me.rhunk.snapenhance.core.features.impl.downloader.MediaDownloader
import me.rhunk.snapenhance.core.features.impl.messaging.AutoMarkAsRead
import me.rhunk.snapenhance.core.ui.children
import me.rhunk.snapenhance.core.ui.iterateParent
import me.rhunk.snapenhance.core.ui.menu.AbstractMenu
import me.rhunk.snapenhance.core.ui.triggerCloseTouchEvent
import me.rhunk.snapenhance.core.util.ktx.vibrateLongPress

class OperaViewerIcons : AbstractMenu() {
    private val actionMenuIconSize by lazy { context.userInterface.dpToPx(32) }
    private val actionMenuIconMargin by lazy { context.userInterface.dpToPx(5) }
    private val actionMenuIconMarginTop by lazy { context.userInterface.dpToPx(10) }

    override fun onViewAdded(event: AddViewEvent) {
        if (event.view is FrameLayout && event.parent.javaClass.superclass?.name?.endsWith("OpenLayout") == true) {
            val viewGroup = event.view as? ViewGroup ?: return
            if (
                viewGroup.childCount == 0 ||
                viewGroup.children().any { it !is ImageView } ||
                event.parent.children().none { it.javaClass.name.endsWith("ScalableCircleMaskFrameLayout") }
            ) return
            inject(viewGroup)
        }
    }

    private fun inject(parent: ViewGroup) {
        val mediaDownloader = context.feature(MediaDownloader::class)

        if (context.config.downloader.operaDownloadButton.get()) {
            parent.addView(LinearLayout(parent.context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, actionMenuIconMarginTop * 2 + actionMenuIconSize, 0, 0)
                    marginEnd = actionMenuIconMargin
                    gravity = Gravity.TOP or Gravity.END
                }
                addOnAttachStateChangeListener(object: View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) {
                        v.visibility = View.VISIBLE
                        (parent.parent as? ViewGroup)?.children()?.forEach { child ->
                            if (child !is ViewGroup) return@forEach
                            child.children().forEach {
                                if (it::class.java.name.endsWith("PreviewToolbar")) v.visibility = View.GONE
                            }
                        }
                    }

                    override fun onViewDetachedFromWindow(v: View) {}
                })

                addView(createComposeView(parent.context) {
                    Icon(
                        imageVector = Icons.Outlined.Download,
                        tint = Color.White,
                        contentDescription = null
                    )
                }.apply {
                    setOnClickListener {
                        mediaDownloader.downloadLastOperaMediaAsync(allowDuplicate = false)
                    }
                    setOnLongClickListener {
                        context.vibrateLongPress()
                        mediaDownloader.downloadLastOperaMediaAsync(allowDuplicate = true)
                        true
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        actionMenuIconSize,
                        actionMenuIconSize
                    ).apply {
                        setMargins(0, 0, 0, actionMenuIconMargin * 2)
                    }
                })
            }, 0)
        }

        if (context.config.messaging.markSnapAsSeenButton.get()) {
            fun getMessageId(): Pair<String, String>? {
                return mediaDownloader.lastSeenMapParams?.get("MESSAGE_ID")
                    ?.toString()
                    ?.split(":")
                    ?.takeIf { it.size == 3 }
                    ?.let { return it[0] to it[2] }
            }

            parent.addView(createComposeView(parent.context)  {
                Icon(
                    imageVector = Icons.Default.RemoveRedEye,
                    tint = Color.White,
                    contentDescription = null
                )
            }.apply {
                setOnClickListener {
                    this@OperaViewerIcons.context.apply {
                        coroutineScope.launch {
                            val (conversationId, clientMessageId) = getMessageId() ?: return@launch
                            val result = feature(AutoMarkAsRead::class).markSnapAsSeen(conversationId, clientMessageId.toLong())

                            if (result == "DUPLICATEREQUEST" || result == null) {
                                if (config.messaging.skipWhenMarkingAsSeen.get()) {
                                    withContext(Dispatchers.Main) {
                                        parent.iterateParent {
                                            it.triggerCloseTouchEvent()
                                            false
                                        }
                                    }
                                }
                            }

                            if (result == "DUPLICATEREQUEST") return@launch
                            if (result == null) {
                                inAppOverlay.showStatusToast(
                                    Icons.Default.Info,
                                    translation["mark_as_seen.seen_toast"],
                                    durationMs = 800
                                )
                            } else {
                                inAppOverlay.showStatusToast(
                                    Icons.Default.Info,
                                    "Failed to mark as seen: $result",
                                )
                            }
                        }
                    }
                }

                addOnAttachStateChangeListener(object: View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) {
                        v.visibility = View.GONE
                        this@OperaViewerIcons.context.coroutineScope.launch(Dispatchers.Main) {
                            delay(250)
                            v.visibility = if (getMessageId() != null) View.VISIBLE else View.GONE
                        }
                    }
                    override fun onViewDetachedFromWindow(v: View) {}
                })

                layoutParams = FrameLayout.LayoutParams(
                    (actionMenuIconSize * 1.5).toInt(),
                    (actionMenuIconSize * 1.5).toInt()
                ).apply {
                    setMargins(0, 0, 0, actionMenuIconMarginTop * 2 + this@OperaViewerIcons.context.userInterface.dpToPx(80))
                    marginEnd = actionMenuIconMarginTop * 2
                    marginStart = actionMenuIconMarginTop * 2
                    gravity = Gravity.BOTTOM or Gravity.END
                }
            })
        }
    }
}