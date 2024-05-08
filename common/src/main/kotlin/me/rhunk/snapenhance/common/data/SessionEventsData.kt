package me.rhunk.snapenhance.common.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


data class FriendPresenceState(
    val bitmojiPresent: Boolean,
    val typing: Boolean,
    val wasTyping: Boolean,
    val speaking: Boolean,
    val peeking: Boolean
)

open class SessionEvent(
    val type: SessionEventType,
    val conversationId: String,
    val authorUserId: String,
)

class SessionMessageEvent(
    type: SessionEventType,
    conversationId: String,
    authorUserId: String,
    val serverMessageId: Long,
    val messageData: ByteArray? = null,
    val reactionId: Int? = null,
) : SessionEvent(type, conversationId, authorUserId)


enum class SessionEventType(
    val key: String
) {
    MESSAGE_READ_RECEIPTS("message_read_receipts"),
    MESSAGE_DELETED("message_deleted"),
    MESSAGE_SAVED("message_saved"),
    MESSAGE_UNSAVED("message_unsaved"),
    MESSAGE_EDITED("message_edited"),
    MESSAGE_REACTION_ADD("message_reaction_add"),
    MESSAGE_REACTION_REMOVE("message_reaction_remove"),
    SNAP_OPENED("snap_opened"),
    SNAP_REPLAYED("snap_replayed"),
    SNAP_REPLAYED_TWICE("snap_replayed_twice"),
    SNAP_SCREENSHOT("snap_screenshot"),
    SNAP_SCREEN_RECORD("snap_screen_record"),
}

enum class TrackerEventType(
    val key: String
) {
    // pcs events
    CONVERSATION_ENTER("conversation_enter"),
    CONVERSATION_EXIT("conversation_exit"),
    STARTED_TYPING("started_typing"),
    STOPPED_TYPING("stopped_typing"),
    STARTED_SPEAKING("started_speaking"),
    STOPPED_SPEAKING("stopped_speaking"),
    STARTED_PEEKING("started_peeking"),
    STOPPED_PEEKING("stopped_peeking"),

    // mcs events
    MESSAGE_READ("message_read"),
    MESSAGE_DELETED("message_deleted"),
    MESSAGE_SAVED("message_saved"),
    MESSAGE_UNSAVED("message_unsaved"),
    MESSAGE_EDITED("message_edited"),
    MESSAGE_REACTION_ADD("message_reaction_add"),
    MESSAGE_REACTION_REMOVE("message_reaction_remove"),
    SNAP_OPENED("snap_opened"),
    SNAP_REPLAYED("snap_replayed"),
    SNAP_REPLAYED_TWICE("snap_replayed_twice"),
    SNAP_SCREENSHOT("snap_screenshot"),
    SNAP_SCREEN_RECORD("snap_screen_record"),
}


@Parcelize
class TrackerEventsResult(
    val rules: Map<ScopedTrackerRule, List<TrackerRuleEvent>>,
): Parcelable {
    fun getActions(): Map<TrackerRuleAction, TrackerRuleActionParams> {
        return rules.flatMap {
            it.value
        }.fold(mutableMapOf()) { acc, ruleEvent ->
            ruleEvent.actions.forEach { action ->
                acc[action] = acc[action]?.merge(ruleEvent.params) ?: ruleEvent.params
            }
            acc
        }
    }

    fun canTrackOn(conversationId: String?, userId: String?): Boolean {
        return rules.any { (scopedRule, events) ->
            if (!events.any { it.enabled }) return@any false
            val scopes = scopedRule.scopes

            when (scopes[userId]) {
                TrackerScopeType.WHITELIST -> return@any true
                TrackerScopeType.BLACKLIST -> return@any false
                else -> {}
            }

            when (scopes[conversationId]) {
                TrackerScopeType.WHITELIST -> return@any true
                TrackerScopeType.BLACKLIST -> return@any false
                else -> {}
            }

            return@any scopes.isEmpty() || scopes.any { it.value == TrackerScopeType.BLACKLIST }
        }
    }
}

enum class TrackerRuleAction(
    val key: String
) {
    LOG("log"),
    IN_APP_NOTIFICATION("in_app_notification"),
    PUSH_NOTIFICATION("push_notification"),
    CUSTOM("custom");

    companion object {
        fun fromString(value: String): TrackerRuleAction? {
            return entries.find { it.key == value }
        }
    }
}

@Parcelize
data class TrackerRuleActionParams(
    var onlyInsideConversation: Boolean = false,
    var onlyOutsideConversation: Boolean = false,
    var onlyWhenAppActive: Boolean = false,
    var onlyWhenAppInactive: Boolean = false,
    var noPushNotificationWhenAppActive: Boolean = false,
): Parcelable {
    fun merge(other: TrackerRuleActionParams): TrackerRuleActionParams {
        return TrackerRuleActionParams(
            onlyInsideConversation = onlyInsideConversation || other.onlyInsideConversation,
            onlyOutsideConversation = onlyOutsideConversation || other.onlyOutsideConversation,
            onlyWhenAppActive = onlyWhenAppActive || other.onlyWhenAppActive,
            onlyWhenAppInactive = onlyWhenAppInactive || other.onlyWhenAppInactive,
            noPushNotificationWhenAppActive = noPushNotificationWhenAppActive || other.noPushNotificationWhenAppActive,
        )
    }
}

@Parcelize
data class TrackerRule(
    val id: Int,
    val name: String,
): Parcelable

@Parcelize
data class ScopedTrackerRule(
    val rule: TrackerRule,
    val scopes: Map<String, TrackerScopeType>
): Parcelable

enum class TrackerScopeType(
    val key: String
) {
    WHITELIST("whitelist"),
    BLACKLIST("blacklist");
}

@Parcelize
data class TrackerRuleEvent(
    val id: Int,
    val enabled: Boolean,
    val eventType: String,
    val params: TrackerRuleActionParams,
    val actions: List<TrackerRuleAction>
): Parcelable
