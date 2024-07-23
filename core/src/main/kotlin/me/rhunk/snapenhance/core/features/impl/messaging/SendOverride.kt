package me.rhunk.snapenhance.core.features.impl.messaging

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import me.rhunk.snapenhance.common.data.ContentType
import me.rhunk.snapenhance.common.util.protobuf.ProtoEditor
import me.rhunk.snapenhance.common.util.protobuf.ProtoReader
import me.rhunk.snapenhance.common.util.protobuf.ProtoWriter
import me.rhunk.snapenhance.core.event.events.impl.MediaUploadEvent
import me.rhunk.snapenhance.core.event.events.impl.NativeUnaryCallEvent
import me.rhunk.snapenhance.core.event.events.impl.SendMessageWithContentEvent
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.features.impl.experiments.MediaFilePicker
import me.rhunk.snapenhance.core.messaging.MessageSender
import me.rhunk.snapenhance.core.ui.ViewAppearanceHelper
import me.rhunk.snapenhance.nativelib.NativeLib
import java.util.Locale

class SendOverride : Feature("Send Override") {
    private val typeNames by lazy {
        mutableListOf("ORIGINAL", "SNAP", "NOTE").also {
            if (NativeLib.initialized) {
                it.add("SAVEABLE_SNAP")
            }
        }.associateWith { it }
    }

    override fun init() {
        val stripSnapMetadata = context.config.messaging.stripMediaMetadata.get()
        var postSavePolicy: Int? = null

        context.event.subscribe(SendMessageWithContentEvent::class, {
            stripSnapMetadata.isNotEmpty()
        }) { event ->
            val contentType = event.messageContent.contentType ?: return@subscribe

            val newMessageContent = ProtoEditor(event.messageContent.content!!).apply {
                when (contentType) {
                    ContentType.SNAP, ContentType.EXTERNAL_MEDIA -> {
                        edit(*(if (contentType == ContentType.SNAP) intArrayOf(11) else intArrayOf(3, 3))) {
                            if (stripSnapMetadata.contains("hide_caption_text")) {
                                edit(5) {
                                    editEach(1) {
                                        remove(2)
                                    }
                                }
                            }
                            if (stripSnapMetadata.contains("hide_snap_filters")) {
                                remove(9)
                                remove(11)
                            }
                            if (stripSnapMetadata.contains("hide_extras")) {
                                remove(13)
                                edit(5, 1) {
                                    remove(2)
                                }
                            }
                        }
                    }
                    ContentType.NOTE -> {
                        if (stripSnapMetadata.contains("remove_audio_note_duration")) {
                            edit(6, 1, 1) {
                                remove(13)
                            }
                        }
                        if (stripSnapMetadata.contains("remove_audio_note_transcript_capability")) {
                            edit(6, 1) {
                                remove(3)
                            }
                        }
                    }
                    else -> return@subscribe
                }
            }.toByteArray()

            event.messageContent.content = newMessageContent
        }

        val configOverrideType = context.config.messaging.galleryMediaSendOverride.getNullable() ?: return

        context.event.subscribe(MediaUploadEvent::class) { event ->
            ProtoReader(event.localMessageContent.content!!).followPath(11, 5)?.let { snapDocPlayback ->
                event.onMediaUploaded { result ->
                    result.messageContent.content = ProtoEditor(result.messageContent.content!!).apply {
                        edit(11, 5) {
                            // remove media upload hint when viewing snap
                            edit(1) {
                                edit(1) {
                                    remove(27)
                                    addBuffer(26, byteArrayOf())
                                }
                            }

                            remove(2)
                            snapDocPlayback.getByteArray(2)?.let {
                                addBuffer(2, it)
                            }
                        }
                    }.toByteArray()
                }
            }
        }

        context.event.subscribe(NativeUnaryCallEvent::class) { event ->
            if (event.uri != "/messagingcoreservice.MessagingCoreService/CreateContentMessage") return@subscribe
            postSavePolicy?.let { savePolicy ->
                context.log.verbose("post save policy $savePolicy")
                event.buffer = ProtoEditor(event.buffer).apply {
                    edit {
                        edit(4) {
                            remove(7)
                            addVarInt(7, savePolicy)
                        }
                        add(6) {
                            from(9) {
                                addVarInt(1, 1)
                            }
                        }
                    }
                }.toByteArray()
            }
        }

        context.event.subscribe(SendMessageWithContentEvent::class) { event ->
            postSavePolicy = null
            if (event.destinations.stories?.isNotEmpty() == true && event.destinations.conversations?.isEmpty() == true) return@subscribe
            val localMessageContent = event.messageContent
            if (localMessageContent.contentType != ContentType.EXTERNAL_MEDIA) return@subscribe

            //prevent story replies
            val messageProtoReader = ProtoReader(localMessageContent.content!!)
            if (messageProtoReader.contains(7)) return@subscribe

            event.canceled = true

            fun sendMedia(overrideType: String): Boolean {
                if (overrideType != "ORIGINAL" && messageProtoReader.followPath(3)?.getCount(3) != 1) {
                    context.inAppOverlay.showStatusToast(
                        icon = Icons.Default.WarningAmber,
                        context.translation["gallery_media_send_override.multiple_media_toast"]
                    )
                    return false
                }

                when (overrideType) {
                    "SNAP", "SAVEABLE_SNAP" -> {
                        postSavePolicy = if (overrideType == "SAVEABLE_SNAP") 3 /* VIEW_SESSION */ else 1 /* PROHIBITED */

                        val extras = messageProtoReader.followPath(3, 3, 13)?.getBuffer()
                        localMessageContent.contentType = ContentType.SNAP
                        localMessageContent.content = ProtoWriter().apply {
                            from(11) {
                                from(5) {
                                    from(1) {
                                        from(1) {
                                            addVarInt(2, 0)
                                            addVarInt(12, 0)
                                            addVarInt(15, 0)
                                        }
                                        addVarInt(6, 0)
                                    }
                                    messageProtoReader.getByteArray(3, 3, 5, 2)?.let {
                                        addBuffer(2, it)
                                    }
                                }
                                extras?.let {
                                    addBuffer(13, it)
                                }
                            }
                        }.toByteArray()
                    }
                    "NOTE" -> {
                        localMessageContent.contentType = ContentType.NOTE
                        localMessageContent.content =
                            MessageSender.audioNoteProto(
                                messageProtoReader.getVarInt(3, 3, 5, 1, 1, 15) ?: context.feature(MediaFilePicker::class).lastMediaDuration ?: 0,
                                Locale.getDefault().toLanguageTag()
                            )
                    }
                }

                return true
            }

            if (configOverrideType != "always_ask") {
                if (sendMedia(configOverrideType)) {
                    event.invokeOriginal()
                }
                return@subscribe
            }

            context.runOnUiThread {
                ViewAppearanceHelper.newAlertDialogBuilder(context.mainActivity!!)
                    .setItems(typeNames.values.map {
                        context.translation["features.options.gallery_media_send_override.$it"]
                    }.toTypedArray()) { dialog, which ->
                        dialog.dismiss()
                        if (sendMedia(typeNames.keys.toTypedArray()[which])) {
                            event.invokeOriginal()
                        }
                    }
                    .setTitle(context.translation["send_override_dialog.title"])
                    .setNegativeButton(context.translation["button.cancel"], null)
                    .show()
            }
        }
    }
}