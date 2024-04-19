package me.rhunk.snapenhance.core.features.impl

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import com.google.gson.JsonObject
import me.rhunk.snapenhance.common.data.FriendLinkType
import me.rhunk.snapenhance.core.event.events.impl.NetworkApiRequestEvent
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.features.FeatureLoadParams
import me.rhunk.snapenhance.core.util.EvictingMap
import java.io.InputStreamReader

class FriendMutationObserver: Feature("FriendMutationObserver", loadParams = FeatureLoadParams.INIT_SYNC) {
    private val translation by lazy { context.translation.getCategory("friend_mutation_observer") }
    private val addSourceCache = EvictingMap<String, String>(500)

    private val notificationManager by lazy { context.androidContext.getSystemService(NotificationManager::class.java) }
    private val channelId by lazy {
        "friend_mutation_observer".also {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    it,
                    translation["notification_channel_name"],
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }
    }

    fun getFriendAddSource(userId: String): String? {
        return addSourceCache[userId]
    }

    private fun sendFriendRemoveNotification(displayName: String?, username: String) {
        val contentText = (if (displayName != null)
            translation.format("removed_friend_notification_content_with_display_name", "displayName" to displayName, "username" to username)
        else translation.format("removed_friend_notification_content", "username" to username))

        notificationManager.notify(System.nanoTime().toInt(),
            Notification.Builder(context.androidContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(translation["removed_friend_notification_title"])
            .setContentText(contentText)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .build()
        )

        context.inAppOverlay.showStatusToast(
            Icons.Default.WarningAmber,
            contentText,
            durationMs = 6000
        )
    }

    override fun init() {
        context.event.subscribe(NetworkApiRequestEvent::class) { event ->
            if (!event.url.contains("ami/friends")) return@subscribe
            event.onSuccess { buffer ->
                runCatching {
                    val jsonObject = context.gson.fromJson(InputStreamReader(buffer?.inputStream() ?: return@onSuccess, Charsets.UTF_8), JsonObject::class.java)

                    jsonObject.getAsJsonArray("added_friends").map { it.asJsonObject }.forEach { friend ->
                        val userId = friend.get("user_id").asString
                        (friend.get("add_source")?.asString?.takeIf {
                            it.isNotBlank()
                        } ?: friend.get("add_source_type")?.asString?.takeIf {
                            it.isNotBlank()
                        })?.let {
                            addSourceCache[userId] = it
                        }
                    }

                    if (context.config.messaging.relationshipNotifier.get()) {
                        jsonObject.getAsJsonArray("friends").map { it.asJsonObject }.forEach { friend ->
                            val userId = friend.get("user_id")?.asString
                            if (userId == context.database.myUserId) return@forEach
                            val direction = friend.get("direction")?.asString
                            if (direction != "OUTGOING") return@forEach

                            val databaseFriend = context.database.getFriendInfo(userId ?: return@forEach) ?: return@forEach
                            val mutableUsername = friend.get("mutable_username").asString
                            val databaseLinkType = FriendLinkType.fromValue(databaseFriend.friendLinkType)

                            if (databaseLinkType == FriendLinkType.MUTUAL && !friend.has("fidelius_info")) {
                                sendFriendRemoveNotification(databaseFriend.displayName, mutableUsername)
                            }
                        }
                    }
                }.onFailure {
                    context.log.error("Failed to process friends", it)
                }
            }
        }
    }
}