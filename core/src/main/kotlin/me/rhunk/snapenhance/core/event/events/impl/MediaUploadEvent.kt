package me.rhunk.snapenhance.core.event.events.impl

import me.rhunk.snapenhance.core.event.events.AbstractHookEvent
import me.rhunk.snapenhance.core.wrapper.impl.MessageContent
import me.rhunk.snapenhance.core.wrapper.impl.MessageDestinations

class MediaUploadEvent(
    val localMessageContent: MessageContent,
    val destinations: MessageDestinations,
    val callback: Any,
): AbstractHookEvent() {
    class MediaUploadResult(
        val messageContent: MessageContent
    )

    val mediaUploadCallbacks = mutableListOf<(MediaUploadResult) -> Unit>()

    fun onMediaUploaded(callback: (MediaUploadResult) -> Unit) {
        mediaUploadCallbacks.add(callback)
    }
}