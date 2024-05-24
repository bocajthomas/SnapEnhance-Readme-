package me.rhunk.snapenhance.common.config.impl

import me.rhunk.snapenhance.common.config.ConfigContainer
import me.rhunk.snapenhance.common.util.PURGE_DISABLED_KEY
import me.rhunk.snapenhance.common.util.PURGE_VALUES
import me.rhunk.snapenhance.common.util.PURGE_TRANSLATION_KEY

class FriendTrackerConfig: ConfigContainer(hasGlobalState = true) {
    val recordMessagingEvents = boolean("record_messaging_events", false)
    val allowRunningInBackground = boolean("allow_running_in_background", false)
    val autoPurge = unique("auto_purge", *PURGE_VALUES) {
        customOptionTranslationPath = PURGE_TRANSLATION_KEY
        disabledKey = PURGE_DISABLED_KEY
    }.apply { set(PURGE_DISABLED_KEY) }
}