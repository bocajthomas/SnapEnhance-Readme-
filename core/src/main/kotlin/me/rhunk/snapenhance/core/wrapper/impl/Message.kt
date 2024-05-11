package me.rhunk.snapenhance.core.wrapper.impl

import me.rhunk.snapenhance.common.data.ContentType
import me.rhunk.snapenhance.common.data.MessageState
import me.rhunk.snapenhance.common.util.protobuf.ProtoReader
import me.rhunk.snapenhance.core.wrapper.AbstractWrapper
import org.mozilla.javascript.annotations.JSGetter
import org.mozilla.javascript.annotations.JSSetter


fun ByteArray.getMessageText(contentType: ContentType): String? {
    val protoReader by lazy { ProtoReader(this) }
    return when (contentType) {
        ContentType.CHAT -> protoReader.getString(2, 1) ?: "Failed to parse message"
        ContentType.TINY_SNAP -> protoReader.getString(19, 1, 1)
        ContentType.EXTERNAL_MEDIA -> protoReader.getString(7, 11, 1)
        ContentType.SNAP -> protoReader.followPath(11, 5)?.run {
            val captions = mutableListOf<String>()

            eachBuffer(1) {
                followPath(4) {
                    val caption = getString(3, 2, 1)
                    if (caption != null) {
                        captions.add(caption)
                    }
                }
            }

            captions.takeIf { it.isNotEmpty() }?.joinToString("\n")
        }
        else -> null
    }
}

class Message(obj: Any?) : AbstractWrapper(obj) {
    @get:JSGetter @set:JSSetter
    var orderKey by field<Long>("mOrderKey")
    @get:JSGetter @set:JSSetter
    var senderId by field("mSenderId") { SnapUUID(it) }
    @get:JSGetter @set:JSSetter
    var messageContent by field("mMessageContent") { MessageContent(it) }
    @get:JSGetter @set:JSSetter
    var messageDescriptor by field("mDescriptor") { MessageDescriptor(it) }
    @get:JSGetter @set:JSSetter
    var messageMetadata by field("mMetadata") { MessageMetadata(it) }
    @get:JSGetter @set:JSSetter
    var messageState by enum("mState", MessageState.COMMITTED)

    fun serialize(): String?{
        return  messageContent?.content?.getMessageText(messageContent?.contentType ?: return null)
    }
}