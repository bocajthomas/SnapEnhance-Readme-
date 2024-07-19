package me.rhunk.snapenhance.core.features.impl.ui

import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.features.impl.messaging.Messaging
import me.rhunk.snapenhance.core.util.dataBuilder
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hookConstructor
import me.rhunk.snapenhance.core.util.ktx.getObjectField
import me.rhunk.snapenhance.core.util.ktx.getObjectFieldOrNull
import me.rhunk.snapenhance.core.util.ktx.setObjectField
import me.rhunk.snapenhance.core.wrapper.impl.SnapUUID

class HideStreakRestore : Feature("HideStreakRestore") {
    override fun init() {
        if (!context.config.userInterface.hideStreakRestore.get()) return

        findClass("com.snapchat.client.messaging.FeedEntry").hookConstructor(HookStage.AFTER) { param ->
            val instance = param.thisObject<Any>()
            if (instance.getObjectFieldOrNull("mDisplayInfo")
                    ?.getObjectFieldOrNull("mFeedItem")
                    ?.getObjectFieldOrNull("mConversation")
                    ?.getObjectFieldOrNull("mState")
                    ?.toString() == "STREAK_RESTORE") {
                instance.getObjectFieldOrNull("mDisplayInfo")
                    ?.getObjectFieldOrNull("mFeedItem")
                    ?.setObjectField("mConversation", null)
                val conversationId = SnapUUID(instance.getObjectField("mConversationId")).toString()
                context.feature(Messaging::class).conversationManager?.dismissStreakRestore(
                    conversationId,
                    onError = {
                        context.log.error("Failed to dismiss streak restore: $it")
                    }, onSuccess = {
                        context.log.info("Dismissed streak restore for conversation $conversationId")
                    }
                )
            }
        }

        findClass("com.snapchat.client.messaging.StreakMetadata").hookConstructor(HookStage.AFTER) { param ->
            param.thisObject<Any>().dataBuilder {
                set("mExpiredStreak", null)
            }
        }
    }
}