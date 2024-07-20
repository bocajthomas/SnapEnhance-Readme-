package me.rhunk.snapenhance.core.features.impl.ui

import me.rhunk.snapenhance.common.data.MessagingRuleType
import me.rhunk.snapenhance.common.data.RuleState
import me.rhunk.snapenhance.core.features.MessagingRuleFeature
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.Hooker
import me.rhunk.snapenhance.core.util.hook.hook
import me.rhunk.snapenhance.core.util.hook.hookConstructor
import me.rhunk.snapenhance.core.util.ktx.getObjectField
import me.rhunk.snapenhance.core.util.ktx.setObjectField
import me.rhunk.snapenhance.core.wrapper.impl.SnapUUID

class PinConversations : MessagingRuleFeature("PinConversations", MessagingRuleType.PIN_CONVERSATION) {
    override fun init() {
        if (!context.config.messaging.unlimitedConversationPinning.get()) return

        context.classCache.feedManager.hook("setPinnedConversationStatus", HookStage.BEFORE) { param ->
            val conversationUUID = SnapUUID(param.arg(0))
            val isPinned = param.arg<Any>(1).toString() == "PINNED"
            setState(conversationUUID.toString(), isPinned)
            val callback = param.arg<Any>(2)
            mutableSetOf<() -> Unit>().apply {
                addAll(Hooker.ephemeralHookObjectMethod(callback::class.java, callback,"onSuccess", HookStage.BEFORE) {
                    forEach { it() }
                })
                addAll(Hooker.ephemeralHookObjectMethod(callback::class.java, callback,"onError", HookStage.BEFORE) { methodParam ->
                    methodParam.setResult(null)
                    callback::class.java.getDeclaredMethod("onSuccess").invoke(callback)
                })
            }
        }

        context.classCache.conversation.hookConstructor(HookStage.AFTER) { param ->
            val instance = param.thisObject<Any>()
            val conversationUUID = SnapUUID(instance.getObjectField("mConversationId"))
            if (getState(conversationUUID.toString())) {
                instance.setObjectField("mPinnedTimestampMs", 1L)
            }
        }

        context.classCache.feedEntry.hookConstructor(HookStage.AFTER) { param ->
            val instance = param.thisObject<Any>()
            val conversationUUID = SnapUUID(instance.getObjectField("mConversationId") ?: return@hookConstructor)
            val isPinned = getState(conversationUUID.toString())
            if (isPinned) {
                instance.setObjectField("mPinnedTimestampMs", 1L)
            }
        }
    }

    override fun getRuleState() = RuleState.WHITELIST
}