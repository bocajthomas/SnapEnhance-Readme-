package me.rhunk.snapenhance.common.action

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.ui.graphics.vector.ImageVector


enum class EnumAction(
    val key: String,
    val icon: ImageVector,
    val exitOnFinish: Boolean = false,
) {
    EXPORT_CHAT_MESSAGES("export_chat_messages", Icons.AutoMirrored.Rounded.Chat),
    EXPORT_MEMORIES("export_memories", Icons.Rounded.Image),
    BULK_MESSAGING_ACTION("bulk_messaging_action", Icons.Rounded.DeleteOutline),
    CLEAN_CACHE("clean_snapchat_cache", Icons.Rounded.CleaningServices, exitOnFinish = true),
    MANAGE_FRIEND_LIST("manage_friend_list", Icons.Rounded.PersonOutline);

    companion object {
        const val ACTION_PARAMETER = "se_action"
    }
}