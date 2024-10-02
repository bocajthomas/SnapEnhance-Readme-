package me.rhunk.snapenhance.core.event.events.impl

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import me.rhunk.snapenhance.common.database.impl.ConversationMessage
import me.rhunk.snapenhance.core.event.Event

class BindViewEvent(
    val prevModel: Any,
    val nextModel: Any?,
    var view: View
): Event() {
    val databaseMessage by lazy {
        var message: ConversationMessage? = null
        chatMessage { _, messageId ->
            message = context.database.getConversationMessageFromId(messageId.toLong())
        }
        message
    }

    inline fun chatMessage(block: (conversationId: String, messageId: String) -> Unit) {
        val modelToString = prevModel.toString()
        if (!modelToString.startsWith("ChatViewModel")) return
        if (view !is LinearLayout) {
            view = (view as ViewGroup).getChildAt(0)
        }
        modelToString.substringAfter("messageId=").substringBefore(",").split(":").apply {
            if (size != 3) return
            block(this[0], this[2])
        }
    }

    inline fun friendFeedItem(block: (conversationId: String) -> Unit) {
        val modelToString = prevModel.toString()
        if (!modelToString.startsWith("FriendFeedItemViewModel")) return
        val conversationId = modelToString.substringAfter("conversationId: ").substringBefore("\n")
        block(conversationId)
    }
}