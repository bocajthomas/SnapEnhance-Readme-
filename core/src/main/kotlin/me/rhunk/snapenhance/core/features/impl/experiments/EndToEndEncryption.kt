package me.rhunk.snapenhance.core.features.impl.experiments

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.Shape
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.rhunk.snapenhance.common.data.ContentType
import me.rhunk.snapenhance.common.data.MessageState
import me.rhunk.snapenhance.common.data.MessagingRuleType
import me.rhunk.snapenhance.common.data.RuleState
import me.rhunk.snapenhance.common.database.impl.ConversationMessage
import me.rhunk.snapenhance.common.ui.createComposeView
import me.rhunk.snapenhance.common.util.lazyBridge
import me.rhunk.snapenhance.common.util.protobuf.ProtoEditor
import me.rhunk.snapenhance.common.util.protobuf.ProtoReader
import me.rhunk.snapenhance.common.util.protobuf.ProtoWriter
import me.rhunk.snapenhance.core.event.events.impl.*
import me.rhunk.snapenhance.core.features.MessagingRuleFeature
import me.rhunk.snapenhance.core.features.impl.ui.ConversationToolbox
import me.rhunk.snapenhance.core.ui.ViewAppearanceHelper
import me.rhunk.snapenhance.core.ui.addForegroundDrawable
import me.rhunk.snapenhance.core.ui.findParent
import me.rhunk.snapenhance.core.ui.removeForegroundDrawable
import me.rhunk.snapenhance.core.util.EvictingMap
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hook
import me.rhunk.snapenhance.core.util.ktx.getObjectField
import me.rhunk.snapenhance.core.util.ktx.getObjectFieldOrNull
import me.rhunk.snapenhance.core.wrapper.impl.MessageContent
import me.rhunk.snapenhance.core.wrapper.impl.MessageDestinations
import me.rhunk.snapenhance.core.wrapper.impl.SnapUUID
import me.rhunk.snapenhance.mapper.impl.CallbackMapper
import me.rhunk.snapenhance.nativelib.NativeLib
import java.security.MessageDigest
import kotlin.random.Random

class EndToEndEncryption : MessagingRuleFeature(
    "EndToEndEncryption",
    MessagingRuleType.E2E_ENCRYPTION
) {
    val isEnabled get() = context.config.experimental.e2eEncryption.globalState == true
    private val e2eeInterface by lazyBridge { context.bridgeClient.getE2eeInterface() }

    private val translation by lazy { context.translation.getCategory("end_to_end_encryption") }

    companion object {
        const val REQUEST_PK_MESSAGE_ID = 1
        const val RESPONSE_SK_MESSAGE_ID = 2
        const val ENCRYPTED_MESSAGE_ID = 3
    }

    private val decryptedMessageCache = EvictingMap<Long, Pair<ContentType, ByteArray>>(100)

    private val pkRequests = mutableMapOf<Long, ByteArray>()
    private val secretResponses = mutableMapOf<Long, ByteArray>()
    private val encryptedMessages = mutableListOf<Long>()

    private fun getE2EParticipants(conversationId: String): List<String> {
        return context.database.getConversationParticipants(conversationId)?.filter { friendId ->
            friendId != context.database.myUserId && e2eeInterface.friendKeyExists(friendId)
        } ?: emptyList()
    }

    private fun askForKeys(conversationId: String) {
        val friendId = context.database.getDMOtherParticipant(conversationId) ?: run {
            context.longToast("Can't find friendId for conversationId $conversationId")
            return
        }

        val publicKey = e2eeInterface.createKeyExchange(friendId) ?: run {
            context.longToast("Can't create key exchange for friendId $friendId")
            return
        }

        sendCustomMessage(conversationId, REQUEST_PK_MESSAGE_ID) {
            addBuffer(2, publicKey)
        }
    }

    private fun sendCustomMessage(conversationId: String, messageId: Int, message: ProtoWriter.() -> Unit) {
        context.messageSender.sendCustomChatMessage(
            listOf(SnapUUID(conversationId)),
            ContentType.CHAT,
            message = {
                from(2) {
                    from(1) {
                        addVarInt(1, messageId)
                        addBuffer(2, ProtoWriter().apply(message).toByteArray())
                    }
                }
            }
        )
    }

    private fun warnKeyOverwrite(friendId: String, block: () -> Unit) {
        if (!e2eeInterface.friendKeyExists(friendId)) {
            block()
            return
        }

        context.mainActivity?.runOnUiThread {
            val mainActivity = context.mainActivity ?: return@runOnUiThread
            val translation = translation.getCategory("confirmation_dialogs")
            ViewAppearanceHelper.newAlertDialogBuilder(mainActivity).apply {
                setTitle(translation["title"])
                setMessage(translation["confirmation_1"])
                setPositiveButton(this@EndToEndEncryption.context.translation["button.positive"]) { _, _ ->
                    ViewAppearanceHelper.newAlertDialogBuilder(mainActivity).apply {
                        setTitle(translation["title"])
                        setMessage(translation["confirmation_2"])
                        setNeutralButton(this@EndToEndEncryption.context.translation["button.positive"]) { _, _ -> block() }
                        setPositiveButton(this@EndToEndEncryption.context.translation["button.negative"]) { _, _ -> }
                    }.show()
                }
                setNegativeButton(this@EndToEndEncryption.context.translation["button.negative"]) { _, _ -> }
            }.show()
        }
    }

    private fun handlePublicKeyRequest(conversationId: String, publicKey: ByteArray) {
        val friendId = context.database.getDMOtherParticipant(conversationId) ?: run {
            context.longToast("Can't find friendId for conversationId $conversationId")
            return
        }
        warnKeyOverwrite(friendId) {
            val encapsulatedSecret = e2eeInterface.acceptPairingRequest(friendId, publicKey)
            if (encapsulatedSecret == null) {
                context.longToast(translation["accept_public_key_failure_toast"])
                return@warnKeyOverwrite
            }
            setState(conversationId, true)
            context.longToast(translation["accept_public_key_success_toast"])

            sendCustomMessage(conversationId, RESPONSE_SK_MESSAGE_ID) {
                addBuffer(2, encapsulatedSecret)
            }
        }
    }

    private fun handleSecretResponse(conversationId: String, secret: ByteArray) {
        val friendId = context.database.getDMOtherParticipant(conversationId) ?: run {
            context.longToast("Can't find friendId for conversationId $conversationId")
            return
        }
        warnKeyOverwrite(friendId) {
            val result = e2eeInterface.acceptPairingResponse(friendId, secret)
            if (!result) {
                context.longToast(translation["accept_secret_key_failure_toast"])
                return@warnKeyOverwrite
            }
            setState(conversationId, true)
            context.longToast(translation["accept_secret_key_success_toast"])
        }
    }

    @SuppressLint("SetTextI18n", "DiscouragedApi")
    override fun init() {
        if (!isEnabled) return

        context.mappings.useMapper(CallbackMapper::class) {
            callbacks.getClass("ConversationManagerDelegate")?.hook("onSendComplete", HookStage.BEFORE) { param ->
                val sendMessageResult = param.arg<Any>(0)
                val messageDestinations = MessageDestinations(sendMessageResult.getObjectField("mCompletedDestinations") ?: return@hook)
                if (messageDestinations.mPhoneNumbers?.isNotEmpty() == true || messageDestinations.stories?.isNotEmpty() == true) return@hook

                val completedConversationDestinations = sendMessageResult.getObjectField("mCompletedConversationDestinations") as? ArrayList<*> ?: return@hook
                val messageIds = completedConversationDestinations.filter { getState(SnapUUID(it.getObjectField("mConversationId")).toString()) }.mapNotNull {
                    it.getObjectFieldOrNull("mMessageId") as? Long
                }

                encryptedMessages.addAll(messageIds)
            }
        }

        context.event.subscribe(BuildMessageEvent::class, priority = 0) { event ->
            val message = event.message
            val conversationId = message.messageDescriptor!!.conversationId.toString()
            val isMessageCommitted = message.messageState == MessageState.COMMITTED
            messageHook(
                conversationId = conversationId,
                messageId = message.messageDescriptor!!.messageId!!,
                senderId = message.senderId.toString(),
                messageContent = message.messageContent!!,
                committed = isMessageCommitted
            )

            message.messageContent!!.instanceNonNull()
                .getObjectField("mQuotedMessage")
                ?.getObjectField("mContent")
                ?.also { quotedMessage ->
                    messageHook(
                        conversationId = conversationId,
                        messageId = quotedMessage.getObjectField("mMessageId")?.toString()?.toLong() ?: return@also,
                        senderId = SnapUUID(quotedMessage.getObjectField("mSenderId")).toString(),
                        messageContent = MessageContent(quotedMessage),
                        committed = isMessageCommitted
                    )
                }
        }

        onNextActivityCreate(defer = true) {
            context.feature(ConversationToolbox::class).addComposable(translation["confirmation_dialogs.title"], filter = {
                context.database.getDMOtherParticipant(it) != null
            }) { dialog, conversationId ->
                val friendId = remember {
                    context.database.getDMOtherParticipant(conversationId)
                } ?: return@addComposable
                val fingerprint = remember {
                    runCatching {
                        e2eeInterface.getSecretFingerprint(friendId)
                    }.getOrNull()
                }
                if (fingerprint != null) {
                    Text(translation.format("toolbox.shared_key_fingerprint", "fingerprint" to fingerprint))
                } else {
                    Text(translation["toolbox.no_shared_key"])
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(onClick = {
                    dialog.dismiss()
                    warnKeyOverwrite(friendId) {
                        askForKeys(conversationId)
                    }
                }) {
                    Text(translation["toolbox.initiate_exchange_button"])
                }
            }

            val encryptedMessageIndicator by context.config.experimental.e2eEncryption.encryptedMessageIndicator

            val specialCard = Random.nextLong().toString(16)

            context.event.subscribe(BindViewEvent::class) { event ->
                event.chatMessage { conversationId, messageId ->
                    val viewGroup = event.view.findParent(maxIteration = 3) {
                        it is LinearLayout
                    } as? ViewGroup ?: event.view.parent as? ViewGroup ?: return@chatMessage

                    viewGroup.findViewWithTag<View>(specialCard)?.also {
                        viewGroup.removeView(it)
                    }

                    if (encryptedMessageIndicator) {
                        viewGroup.removeForegroundDrawable("encryptedMessage")

                        if (encryptedMessages.contains(messageId.toLong())) {
                            viewGroup.addForegroundDrawable("encryptedMessage", ShapeDrawable(object: Shape() {
                                override fun draw(canvas: Canvas, paint: Paint) {
                                    paint.textSize = 20f
                                    canvas.drawText("\uD83D\uDD12", 0f, canvas.height / 2f, paint)
                                }
                            }))
                        }
                    }

                    val secret = secretResponses[messageId.toLong()]
                    val publicKey = pkRequests[messageId.toLong()]

                    if (publicKey != null || secret != null) {
                        viewGroup.addView(createComposeView(context.mainActivity!!) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                onClick = {
                                    if (publicKey != null) {
                                        handlePublicKeyRequest(conversationId, publicKey)
                                    }
                                    if (secret != null) {
                                        handleSecretResponse(conversationId, secret)
                                    }
                                }
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (publicKey != null) {
                                        Text(translation["accept_public_key_button"])
                                    }
                                    if (secret != null) {
                                        Text(translation["accept_secret_button"])
                                    }
                                }
                            }
                        }.apply {
                            tag = specialCard
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                            )
                        })
                    }
                }
            }
        }

        defer {
            val forceMessageEncryption by context.config.experimental.e2eEncryption.forceMessageEncryption

            context.event.subscribe(MediaUploadEvent::class) { event ->
                val e2eeConversations = event.destinations.getEndToEndConversations()
                if (e2eeConversations.isEmpty()) return@subscribe

                if (event.destinations.conversations!!.size != e2eeConversations.size || event.destinations.stories?.isNotEmpty() == true) {
                    context.log.debug("skipping encryption")
                    return@subscribe
                }

                event.onMediaUploaded { result ->
                    runCatching {
                        result.messageContent.content = ProtoWriter().apply {
                            writeEncryptedMessage(e2eeConversations.map { getE2EParticipants(it) }.flatten().distinct(), result.messageContent.content!!)
                        }.toByteArray()
                    }.onFailure {
                        context.log.error("Failed to encrypt message", it)
                        context.longToast(translation["encryption_failed_toast"])
                    }
                }
            }

            // trick to disable fidelius encryption
            context.event.subscribe(SendMessageWithContentEvent::class) { event ->
                val messageContent = event.messageContent
                val destinations = event.destinations

                val e2eeConversations = destinations.getEndToEndConversations().takeIf { it.isNotEmpty() } ?: return@subscribe

                if (e2eeConversations.size != destinations.conversations!!.size || destinations.stories?.isNotEmpty() == true) {
                    if (!forceMessageEncryption) return@subscribe
                    context.longToast(translation["unencrypted_conversation_send_failure_toast"])
                    event.canceled = true
                    return@subscribe
                }

                if (!NativeLib.initialized) {
                    context.longToast(translation["native_hooks_send_failure_toast"])
                    event.canceled = true
                    return@subscribe
                }

                event.addInvokeLater {
                    // check if the content is already encrypted
                    if (ProtoReader(messageContent.content!!).getByteArray(2, 1, 2) != null) {
                        return@addInvokeLater
                    }
                    if (event.messageContent.localMediaReferences?.isEmpty() == true) {
                        runCatching {
                            event.messageContent.content = ProtoWriter().apply {
                                writeEncryptedMessage(e2eeConversations.map { getE2EParticipants(it) }.flatten().distinct(), messageContent.content!!)
                            }.toByteArray()
                        }.onFailure {
                            context.log.error("Failed to encrypt message", it)
                            context.longToast(translation["encryption_failed_toast"])
                        }
                    }

                    if (event.messageContent.contentType == ContentType.SNAP) {
                        event.messageContent.contentType = ContentType.EXTERNAL_MEDIA
                    }
                }
            }

            context.event.subscribe(NativeUnaryCallEvent::class) { event ->
                if (event.uri != "/messagingcoreservice.MessagingCoreService/CreateContentMessage") return@subscribe
                val protoReader = ProtoReader(event.buffer)
                val messageReader = protoReader.followPath(4) ?: return@subscribe

                if (messageReader.getVarInt(4, 2, 1, 5) == 1L) {
                    event.buffer = ProtoEditor(event.buffer).apply {
                        edit(4) {
                            remove(2)
                            addVarInt(2, ContentType.SNAP.id)
                            context.log.verbose("fixed snap content type")
                        }
                    }.toByteArray()
                }
            }
        }
    }

    private fun fixContentType(contentType: ContentType?, message: ProtoReader)
        = ContentType.fromMessageContainer(message) ?: contentType

    private fun hashParticipantId(participantId: String, salt: ByteArray): ByteArray {
        return MessageDigest.getInstance("SHA-256").apply {
            update(participantId.toByteArray())
            update(salt)
        }.digest()
    }

    fun decryptDatabaseMessage(conversationMessage: ConversationMessage): ProtoReader {
        return tryDecryptMessage(
            senderId = conversationMessage.senderId!!,
            clientMessageId = conversationMessage.clientMessageId.toLong(),
            conversationId = conversationMessage.clientConversationId!!,
            contentType = ContentType.fromId(conversationMessage.contentType),
            messageBuffer = ProtoReader(conversationMessage.messageContent!!).getByteArray(4, 4)!!
        ).let { (_, buffer) ->
            ProtoReader(buffer)
        }
    }

    private fun tryDecryptMessage(senderId: String, clientMessageId: Long, conversationId: String, contentType: ContentType, messageBuffer: ByteArray): Pair<ContentType, ByteArray> {
        if (contentType != ContentType.STATUS && decryptedMessageCache.containsKey(clientMessageId)) {
            return decryptedMessageCache[clientMessageId]!!
        }

        val reader = ProtoReader(messageBuffer)
        var outputBuffer = messageBuffer
        var outputContentType = fixContentType(contentType, reader) ?: contentType
        val conversationParticipants by lazy {
            getE2EParticipants(conversationId)
        }

        fun setDecryptedMessage(buffer: ByteArray) {
            outputBuffer = buffer
            outputContentType = fixContentType(outputContentType, ProtoReader(buffer)) ?: outputContentType
            decryptedMessageCache[clientMessageId] = outputContentType to buffer
            encryptedMessages.add(clientMessageId)
        }

        fun setWarningMessage() {
            encryptedMessages.add(clientMessageId)
            outputContentType = ContentType.CHAT
            outputBuffer = ProtoWriter().apply {
                from(2) {
                    addString(1, "Failed to decrypt message, id=$clientMessageId. Check logs for more details.")
                }
            }.toByteArray()
        }

        fun replaceMessageText(text: String) {
            outputBuffer = ProtoWriter().apply {
                from(2) {
                    addString(1, text)
                }
            }.toByteArray()
        }

        // decrypt messages
        reader.followPath(2, 1) {
            val messageTypeId = getVarInt(1)?.toInt() ?: return@followPath
            val isMe = context.database.myUserId == senderId

            if (messageTypeId == ENCRYPTED_MESSAGE_ID) {
                runCatching {
                    eachBuffer(2) {
                        if (decryptedMessageCache.containsKey(clientMessageId)) return@eachBuffer

                        val participantIdHash = getByteArray(1) ?: return@eachBuffer
                        val iv = getByteArray(2) ?: return@eachBuffer
                        val ciphertext = getByteArray(3) ?: return@eachBuffer

                        if (isMe) {
                            if (conversationParticipants.isEmpty()) return@eachBuffer
                            val participantId = conversationParticipants.firstOrNull { participantIdHash.contentEquals(hashParticipantId(it, iv)) } ?: return@eachBuffer
                            setDecryptedMessage(e2eeInterface.decryptMessage(participantId, ciphertext, iv) ?: run {
                                context.log.warn("Failed to decrypt message for participant $participantId")
                                setWarningMessage()
                                return@eachBuffer
                            })
                            return@eachBuffer
                        }

                        if (!participantIdHash.contentEquals(hashParticipantId(context.database.myUserId, iv))) return@eachBuffer

                        setDecryptedMessage(e2eeInterface.decryptMessage(senderId, ciphertext, iv)?: run {
                            setWarningMessage()
                            return@eachBuffer
                        })
                    }
                }.onFailure {
                    context.log.error("Failed to decrypt message id: $clientMessageId", it)
                    setWarningMessage()
                }

                return@followPath
            }

            val payload = getByteArray(2, 2) ?: return@followPath

            if (senderId == context.database.myUserId) {
                when (messageTypeId) {
                    REQUEST_PK_MESSAGE_ID -> {
                        replaceMessageText("[${translation["outgoing_pk_message"]}]")
                    }
                    RESPONSE_SK_MESSAGE_ID -> {
                        replaceMessageText("[${translation["outgoing_secret_message"]}]")
                    }
                }
                return@followPath
            }

            when (messageTypeId) {
                REQUEST_PK_MESSAGE_ID -> {
                    pkRequests[clientMessageId] = payload
                    replaceMessageText(translation["incoming_pk_message"])
                }
                RESPONSE_SK_MESSAGE_ID -> {
                    secretResponses[clientMessageId] = payload
                    replaceMessageText(translation["incoming_secret_message"])
                }
            }
        }

        return outputContentType to outputBuffer
    }

    private fun messageHook(conversationId: String, messageId: Long, senderId: String, messageContent: MessageContent, committed: Boolean) {
        val (contentType, buffer) = tryDecryptMessage(senderId, messageId, conversationId, messageContent.contentType ?: ContentType.CHAT, messageContent.content!!)
        messageContent.contentType = contentType
        messageContent.content = buffer
        // remove messages currently being sent from the cache
        if (!committed) {
            decryptedMessageCache.remove(messageId)
            encryptedMessages.remove(messageId)
        }
    }

    private fun ProtoWriter.writeEncryptedMessage(
        participantsIds: List<String>,
        messageContent: ByteArray,
    ) {
        from(2) {
            from(1) {
                addVarInt(1, ENCRYPTED_MESSAGE_ID)
                participantsIds.forEach { participantId ->
                    val encryptedMessage = e2eeInterface.encryptMessage(participantId,
                        messageContent
                    ) ?: run {
                        throw Exception("Failed to encrypt message for participant $participantId")
                    }
                    context.log.debug("encrypted message size = ${encryptedMessage.ciphertext.size}")
                    from(2) {
                        // participantId is hashed with iv to prevent leaking it when sending to multiple conversations
                        addBuffer(1, hashParticipantId(participantId, encryptedMessage.iv))
                        addBuffer(2, encryptedMessage.iv)
                        addBuffer(3, encryptedMessage.ciphertext)
                    }
                }
                if (ContentType.fromMessageContainer(ProtoReader(messageContent)) == ContentType.SNAP) {
                    addVarInt(5, 1)
                }
            }
        }
    }

    private fun MessageDestinations.getEndToEndConversations(): List<String> {
        return conversations!!.filter { getState(it.toString()) && getE2EParticipants(it.toString()).isNotEmpty() }.map { it.toString() }
    }

    override fun getRuleState() = RuleState.WHITELIST
}