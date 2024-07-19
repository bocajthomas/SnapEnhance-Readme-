package me.rhunk.snapenhance.core.features.impl.messaging

import me.rhunk.snapenhance.core.event.events.impl.BuildMessageEvent
import me.rhunk.snapenhance.core.features.Feature

class BypassMessageActionRestrictions : Feature("Bypass Message Action Restrictions") {
    override fun init() {
        if (!context.config.messaging.bypassMessageActionRestrictions.get()) return
        onNextActivityCreate {
            context.event.subscribe(BuildMessageEvent::class, priority = 102) { event ->
                event.message.messageMetadata?.apply {
                    isSaveable = true
                    isReactable = true
                }
            }
        }
    }
}