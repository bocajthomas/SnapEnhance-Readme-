package me.rhunk.snapenhance.common.config.impl

import me.rhunk.snapenhance.common.config.ConfigContainer

class FriendTrackerConfig: ConfigContainer(hasGlobalState = true) {
    val recordMessagingEvents = boolean("record_messaging_events", false)
    val allowRunningInBackground = boolean("allow_running_in_background", false)
}