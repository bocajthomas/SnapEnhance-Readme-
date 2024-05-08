package me.rhunk.snapenhance.common.action

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.ui.graphics.vector.ImageVector


enum class EnumAction(
    val key: String,
    val icon: ImageVector,
    val exitOnFinish: Boolean = false,
) {
    EXPORT_CHAT_MESSAGES("export_chat_messages", Icons.AutoMirrored.Default.Chat),
    EXPORT_MEMORIES("export_memories", Icons.Default.Image),
    BULK_MESSAGING_ACTION("bulk_messaging_action", Icons.Default.DeleteOutline),
    CLEAN_CACHE("clean_snapchat_cache", Icons.Default.CleaningServices, exitOnFinish = true),
    MANAGE_FRIEND_LIST("manage_friend_list", Icons.Default.PersonOutline);

    companion object {
        const val ACTION_PARAMETER = "se_action"
    }
}