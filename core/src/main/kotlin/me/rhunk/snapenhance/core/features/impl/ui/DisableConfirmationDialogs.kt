package me.rhunk.snapenhance.core.features.impl.ui

import android.view.View
import android.widget.TextView
import me.rhunk.snapenhance.core.event.events.impl.AddViewEvent
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.ui.children
import me.rhunk.snapenhance.core.ui.triggerRootCloseTouchEvent
import me.rhunk.snapenhance.core.util.ktx.getId
import me.rhunk.snapenhance.core.util.ktx.getIdentifier
import java.util.regex.Pattern

class DisableConfirmationDialogs : Feature("Disable Confirmation Dialogs") {
    override fun init() {
        onNextActivityCreate {
            val disableConfirmationDialogs = context.config.global.disableConfirmationDialogs.get().takeIf { it.isNotEmpty() } ?: return@onNextActivityCreate
            val dialogContent = context.resources.getId("dialog_content")
            val alertDialogTitle = context.resources.getId("alert_dialog_title")

            val questions = listOf(
                "erase_message" to "erase_learn_more_dialog_title",
                "erase_message" to "erase_dialog_title",
                "erase_message" to "snap_erase_dialog_title",
                "erase_message" to "snap_erase_learn_more_dialog_title",
                "remove_friend" to "action_menu_remove_friend_question",
                "block_friend" to "action_menu_block_friend_question",
                "ignore_friend" to "action_menu_ignore_friend_question",
                "hide_friend" to "action_menu_hide_friend_question",
                "hide_conversation" to "hide_or_block_clear_conversation_dialog_title",
                "clear_conversation" to "action_menu_clear_conversation_dialog_title"
            ).map { pair ->
                pair.first to runCatching {
                    Pattern.compile(
                        context.resources.getString(context.resources.getIdentifier(pair.second, "string"))
                            .split("%s").joinToString(".*") {
                                Pattern.quote(it)
                            }, Pattern.CASE_INSENSITIVE)
                }.onFailure {
                    context.log.error("Failed to compile regex for ${pair.second}", it)
                }.getOrNull()
            }

            context.event.subscribe(AddViewEvent::class) { event ->
                if (event.parent.id != dialogContent || !event.view::class.java.name.endsWith("SnapButtonView")) return@subscribe

                val dialogTitle = event.parent.findViewById<TextView>(alertDialogTitle)?.text?.toString() ?: return@subscribe
                if (event.parent.children().count { it::class.java.name.endsWith("SnapButtonView") } != 0) return@subscribe

                questions.forEach { (key, value) ->
                    if (!disableConfirmationDialogs.contains(key)) return@forEach

                    if (value?.matcher(dialogTitle)?.matches() == true) {
                        event.parent.visibility = View.INVISIBLE
                        event.parent.post {
                            event.view.callOnClick()
                        }
                        event.parent.postDelayed({
                            context.mainActivity!!.triggerRootCloseTouchEvent()
                        }, 400)
                    }
                }
            }
        }
    }
}