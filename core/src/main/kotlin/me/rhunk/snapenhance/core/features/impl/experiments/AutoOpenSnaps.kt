package me.rhunk.snapenhance.core.features.impl.experiments

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import me.rhunk.snapenhance.common.data.ContentType
import me.rhunk.snapenhance.common.data.MessageUpdate
import me.rhunk.snapenhance.common.data.MessagingRuleType
import me.rhunk.snapenhance.core.event.events.impl.BuildMessageEvent
import me.rhunk.snapenhance.core.features.MessagingRuleFeature
import me.rhunk.snapenhance.core.features.impl.messaging.Messaging
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.random.Random

class AutoOpenSnaps: MessagingRuleFeature("Auto Open Snaps", MessagingRuleType.AUTO_OPEN_SNAPS) {
    private val snapQueue = MutableSharedFlow<Pair<String, Long>>()
    private var snapQueueSize = AtomicInteger(0)
    private val openedSnaps = mutableListOf<Long>()

    private val notificationManager by lazy {
        context.androidContext.getSystemService(NotificationManager::class.java)
    }

    private val notificationId by lazy { Random.nextInt() }

    private val channelId by lazy {
        "auto_open_snaps".also {
            notificationManager.createNotificationChannel(
                NotificationChannel(it, context.translation["auto_open_snaps.title"], NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun sendStatusNotification(count: Int) {
        notificationManager.notify(
            notificationId,
            Notification.Builder(context.androidContext, channelId)
                .setContentTitle(context.translation["auto_open_snaps.title"])
                .setContentText(context.translation.format("auto_open_snaps.notification_content", "count" to count.toString()))
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setProgress(0, 0, true)
                .build().apply {
                    flags = flags or Notification.FLAG_ONLY_ALERT_ONCE
                }
        )
    }

    override fun init() {
        if (getRuleState() == null) return
        val messaging = context.feature(Messaging::class)

        context.coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            snapQueue.collect { (conversationId, messageId) ->
                snapQueueSize.addAndGet(-1)
                delay(Random.nextLong(50, 100))
                var result: String?

                for (i in 0..5) {
                    while (context.isMainActivityPaused || messaging.conversationManager == null) {
                        delay(2000)
                    }

                    result = suspendCoroutine { continuation ->
                        runCatching {
                            messaging.conversationManager?.updateMessage(conversationId, messageId, MessageUpdate.READ) { result ->
                                continuation.resume(result)
                            }
                        }.getOrNull() ?: continuation.resume("ConversationManager is null")
                    }

                    if (result != null && result != "DUPLICATEREQUEST") {
                        context.log.warn("Failed to mark snap as read, retrying in 3 second")
                        delay(3000)
                        continue
                    }
                    break
                }

                if (snapQueueSize.get() <= 5) {
                    notificationManager.cancel(notificationId)
                    synchronized(openedSnaps) {
                        openedSnaps.clear()
                    }
                } else {
                    sendStatusNotification(openedSnaps.size)
                }
            }
        }

        context.event.subscribe(BuildMessageEvent::class, priority = 103) { event ->
            if (event.message.senderId?.toString() == context.database.myUserId) return@subscribe
            val conversationId = event.message.messageDescriptor?.conversationId?.toString() ?: return@subscribe
            val clientMessageId = event.message.messageDescriptor?.messageId ?: return@subscribe

            if (
                event.message.messageContent?.contentType != ContentType.SNAP ||
                event.message.messageMetadata?.openedBy?.any { it.toString() == context.database.myUserId } == true
            ) {
                return@subscribe
            }

            context.coroutineScope.launch {
                if (!canUseRule(conversationId)) return@launch
                synchronized(openedSnaps) {
                    if (openedSnaps.contains(clientMessageId)) {
                        return@launch
                    }
                    openedSnaps.add(clientMessageId)
                }
                snapQueueSize.addAndGet(1)
                snapQueue.emit(conversationId to clientMessageId)
            }
        }
    }
}
