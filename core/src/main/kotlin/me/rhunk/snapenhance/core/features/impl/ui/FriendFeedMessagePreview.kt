package me.rhunk.snapenhance.core.features.impl.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.Shape
import android.text.TextPaint
import android.view.View
import android.view.ViewGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rhunk.snapenhance.common.data.ContentType
import me.rhunk.snapenhance.common.util.protobuf.ProtoReader
import me.rhunk.snapenhance.core.event.events.impl.BindViewEvent
import me.rhunk.snapenhance.core.event.events.impl.BuildMessageEvent
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.features.impl.experiments.EndToEndEncryption
import me.rhunk.snapenhance.core.ui.addForegroundDrawable
import me.rhunk.snapenhance.core.ui.removeForegroundDrawable
import me.rhunk.snapenhance.core.util.EvictingMap
import me.rhunk.snapenhance.core.util.ktx.getId
import me.rhunk.snapenhance.core.wrapper.impl.getMessageText
import java.util.WeakHashMap
import kotlin.math.absoluteValue

class FriendFeedMessagePreview : Feature("FriendFeedMessagePreview") {
    @OptIn(ExperimentalCoroutinesApi::class)
    private val coroutineDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val setting get() = context.config.userInterface.friendFeedMessagePreview

    private val cachedLayouts = WeakHashMap<String, View>()
    private val messageCache = EvictingMap<String, List<String>>(100)
    private val friendNameCache = EvictingMap<String, String>(100)

    private suspend fun fetchMessages(conversationId: String, callback: suspend () -> Unit) {
        val messages = context.database.getMessagesFromConversationId(conversationId, setting.amount.get().absoluteValue)?.mapNotNull { message ->
            val messageContainer =
                message.messageContent
                    ?.let { ProtoReader(it) }
                    ?.followPath(4, 4)?.let {
                        if (context.config.experimental.e2eEncryption.globalState == true) context.feature(EndToEndEncryption::class).decryptDatabaseMessage(message) else it
                    }
                    ?: return@mapNotNull null

            val contentType = ContentType.fromMessageContainer(messageContainer) ?: ContentType.fromId(message.contentType)
            val messageString = messageContainer.getBuffer().getMessageText(contentType) ?: "[${context.translation.getCategory("content_type")[contentType.name]}]"

            val friendName = friendNameCache.getOrPut(message.senderId ?: return@mapNotNull null) {
                context.database.getFriendInfo(message.senderId ?: return@mapNotNull null)?.let { it.displayName?: it.mutableUsername } ?: "Unknown"
            }
            "$friendName: $messageString"
        }?.takeIf { it.isNotEmpty() }?.reversed()

        withContext(Dispatchers.Main) {
            messages?.also { messageCache[conversationId] = it } ?: run {
                messageCache.remove(conversationId)
            }
            callback()
        }
    }

    override fun init() {
        if (setting.globalState != true) return

        onNextActivityCreate {
            val ffItemId = context.resources.getId("ff_item")

            val density = context.resources.displayMetrics.density

            val secondaryTextSize = 10 * density
            val ffSdlAvatarMargin = (7 * density).toInt()
            val ffSdlAvatarSize = (43 * density).toInt()
            val ffSdlPrimaryTextStartMargin = 6 * density

            val feedEntryHeight = ffSdlAvatarSize + ffSdlAvatarMargin * 2 + (4 * density).toInt()
            val separatorHeight = (density * 2).toInt()
            val textPaint = TextPaint().apply {
                textSize = secondaryTextSize
            }

            val typeface by lazy {
                context.userInterface.avenirNextTypeface
            }

            context.event.subscribe(BuildMessageEvent::class) { param ->
                val conversationId = param.message.messageDescriptor?.conversationId?.toString() ?: return@subscribe
                val cachedView = cachedLayouts[conversationId] ?: return@subscribe
                context.coroutineScope.launch {
                    fetchMessages(conversationId) {
                        cachedView.postInvalidateDelayed(100L)
                    }
                }
            }

            context.event.subscribe(BindViewEvent::class) { param ->
                param.friendFeedItem { conversationId ->
                    val frameLayout = param.view as ViewGroup
                    val ffItem = frameLayout.findViewById<View>(ffItemId)

                    context.coroutineScope.launch(coroutineDispatcher) {
                        withContext(Dispatchers.Main) {
                            cachedLayouts.remove(conversationId)
                            frameLayout.removeForegroundDrawable("ffItem")
                        }

                        fetchMessages(conversationId) {
                            var maxTextHeight = 0
                            val previewContainerHeight = messageCache[conversationId]?.sumOf { msg ->
                                val rect = Rect()
                                textPaint.getTextBounds(msg, 0, msg.length, rect)
                                rect.height().also {
                                    if (it > maxTextHeight) maxTextHeight = it
                                }.plus(separatorHeight)
                            } ?: run {
                                ffItem.layoutParams = ffItem.layoutParams.apply {
                                    height = ViewGroup.LayoutParams.MATCH_PARENT
                                }
                                return@fetchMessages
                            }

                            ffItem.layoutParams = ffItem.layoutParams.apply {
                                height = feedEntryHeight + previewContainerHeight + separatorHeight
                            }

                            cachedLayouts[conversationId] = frameLayout

                            frameLayout.addForegroundDrawable("ffItem", ShapeDrawable(object: Shape() {
                                override fun draw(canvas: Canvas, paint: Paint) {
                                    val offsetY = canvas.height.toFloat() - previewContainerHeight
                                    paint.textSize = secondaryTextSize
                                    paint.color = context.userInterface.colorPrimary
                                    paint.typeface = typeface

                                    messageCache[conversationId]?.forEachIndexed { index, messageString ->
                                        canvas.drawText(messageString,
                                            feedEntryHeight + ffSdlPrimaryTextStartMargin,
                                            offsetY + index * maxTextHeight,
                                            paint
                                        )
                                    }
                                }
                            }))
                        }
                    }
                }
            }
        }
    }
}