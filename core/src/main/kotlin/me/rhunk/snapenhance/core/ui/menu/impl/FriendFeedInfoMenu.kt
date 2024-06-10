package me.rhunk.snapenhance.core.ui.menu.impl

import android.content.DialogInterface
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotInterested
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.rhunk.snapenhance.common.data.ContentType
import me.rhunk.snapenhance.common.data.FriendLinkType
import me.rhunk.snapenhance.common.database.impl.ConversationMessage
import me.rhunk.snapenhance.common.database.impl.FriendInfo
import me.rhunk.snapenhance.common.scripting.ui.EnumScriptInterface
import me.rhunk.snapenhance.common.scripting.ui.InterfaceManager
import me.rhunk.snapenhance.common.scripting.ui.ScriptInterface
import me.rhunk.snapenhance.common.ui.createComposeView
import me.rhunk.snapenhance.common.util.protobuf.ProtoReader
import me.rhunk.snapenhance.common.util.snap.BitmojiSelfie
import me.rhunk.snapenhance.core.features.impl.experiments.EndToEndEncryption
import me.rhunk.snapenhance.core.features.impl.messaging.AutoMarkAsRead
import me.rhunk.snapenhance.core.features.impl.messaging.Messaging
import me.rhunk.snapenhance.core.features.impl.spying.MessageLogger
import me.rhunk.snapenhance.core.ui.ViewAppearanceHelper
import me.rhunk.snapenhance.core.ui.applyTheme
import me.rhunk.snapenhance.core.ui.menu.AbstractMenu
import me.rhunk.snapenhance.core.ui.triggerRootCloseTouchEvent
import me.rhunk.snapenhance.core.util.ktx.getIdentifier
import me.rhunk.snapenhance.core.util.ktx.isDarkTheme
import java.net.HttpURLConnection
import java.net.URL
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FriendFeedInfoMenu : AbstractMenu() {
    private val avenirNextMediumFont by lazy {
        FontFamily(
            Font(context.resources.getIdentifier("avenir_next_medium", "font"), FontWeight.Medium)
        )
    }
    private val sigColorTextPrimary by lazy {
        context.androidContext.theme.obtainStyledAttributes(
            intArrayOf(context.resources.getIdentifier("sigColorTextPrimary", "attr"))
        ).getColor(0, 0)
    }
    private val sigColorBackgroundSurface by lazy {
        context.androidContext.theme.obtainStyledAttributes(
            intArrayOf(context.resources.getIdentifier("sigColorBackgroundSurface", "attr"))
        ).getColor(0, 0)
    }

    private fun getImageDrawable(url: String): Drawable {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connect()
        val input = connection.inputStream
        return BitmapDrawable(Resources.getSystem(), BitmapFactory.decodeStream(input))
    }

    private fun formatDate(timestamp: Long): String? {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(Date(timestamp))
    }

    private fun showProfileInfo(profile: FriendInfo) {
        var icon: Drawable? = null
        try {
            if (profile.bitmojiSelfieId != null && profile.bitmojiAvatarId != null) {
                icon = getImageDrawable(
                    BitmojiSelfie.getBitmojiSelfie(
                        profile.bitmojiSelfieId.toString(),
                        profile.bitmojiAvatarId.toString(),
                        BitmojiSelfie.BitmojiSelfieType.THREE_D
                    )!!
                )
            }
        } catch (e: Throwable) {
            context.log.error("Error loading bitmoji selfie", e)
        }
        val finalIcon = icon
        val translation = context.translation.getCategory("profile_info")

        context.runOnUiThread {
            val addedTimestamp: Long = profile.addedTimestamp.coerceAtLeast(profile.reverseAddedTimestamp)
            val builder = ViewAppearanceHelper.newAlertDialogBuilder(context.mainActivity)
            builder.setIcon(finalIcon)
            builder.setTitle(profile.displayName ?: profile.username)

            val birthday = Calendar.getInstance()
            birthday[Calendar.MONTH] = (profile.birthday shr 32).toInt() - 1

            builder.setMessage(mapOf(
                translation["first_created_username"] to profile.firstCreatedUsername,
                translation["mutable_username"] to profile.mutableUsername,
                translation["display_name"] to profile.displayName,
                translation["added_date"] to formatDate(addedTimestamp).takeIf { addedTimestamp > 0 },
                null to birthday.getDisplayName(
                    Calendar.MONTH,
                    Calendar.LONG,
                    context.translation.loadedLocale
                )?.let {
                    if (profile.birthday == 0L) context.translation["profile_info.hidden_birthday"]
                    else context.translation.format("profile_info.birthday",
                        "month" to it,
                        "day" to profile.birthday.toInt().toString())
                },
                translation["friendship"] to run {
                    context.translation["friendship_link_type.${FriendLinkType.fromValue(profile.friendLinkType).shortName}"]
                }.takeIf {
                    if (profile.friendLinkType == FriendLinkType.MUTUAL.value) addedTimestamp.toInt() > 0 else true
                },
                translation["add_source"] to context.database.getAddSource(profile.userId!!)?.takeIf { it.isNotEmpty() },
                translation["snapchat_plus"] to run {
                    translation.getCategory("snapchat_plus_state")[if (profile.postViewEmoji != null) "subscribed" else "not_subscribed"]
                }
            ).filterValues { it != null }.map {
                line -> "${line.key?.let { "$it: " } ?: ""}${line.value}"
            }.joinToString("\n"))

            builder.setPositiveButton(
                "OK"
            ) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
            builder.show()
        }
    }

    private fun showPreview(userId: String?, conversationId: String) {
        //query message
        val messageLogger = context.feature(MessageLogger::class)
        val endToEndEncryption = context.feature(EndToEndEncryption::class)
        val messages: List<ConversationMessage> = context.database.getMessagesFromConversationId(
            conversationId,
            context.config.messaging.messagePreviewLength.get()
        )?.reversed() ?: emptyList()

        val participants: Map<String, FriendInfo> = context.database.getConversationParticipants(conversationId)!!
            .map { context.database.getFriendInfo(it)!! }
            .associateBy { it.userId!! }
        
        val messageBuilder = StringBuilder()
        val translation = context.translation.getCategory("content_type")

        messages.forEach { message ->
            val sender = participants[message.senderId]
            val messageProtoReader =
                (
                    messageLogger.takeIf { it.isEnabled && message.contentType == ContentType.STATUS.id }?.getMessageProto(conversationId, message.clientMessageId.toLong()) // process deleted messages if message logger is enabled
                    ?: ProtoReader(message.messageContent!!).followPath(4, 4) // database message
                )?.let {
                    if (endToEndEncryption.isEnabled) endToEndEncryption.decryptDatabaseMessage(message) else it // try to decrypt message if e2ee is enabled
                } ?: return@forEach

            val contentType = ContentType.fromMessageContainer(messageProtoReader) ?: ContentType.fromId(message.contentType)
            var messageString = if (contentType == ContentType.CHAT) {
                messageProtoReader.getString(2, 1) ?: return@forEach
            } else translation.getOrNull(contentType.name) ?: contentType.name

            if (contentType == ContentType.SNAP) {
                messageString = "\uD83D\uDFE5" //red square
                if (message.readTimestamp > 0) {
                    messageString += " \uD83D\uDC40 " //eyes
                    messageString += DateFormat.getDateTimeInstance(
                        DateFormat.SHORT,
                        DateFormat.SHORT
                    ).format(Date(message.readTimestamp))
                }
            }

            var displayUsername = sender?.displayName ?: sender?.usernameForSorting?: context.translation["conversation_preview.unknown_user"]

            if (displayUsername.length > 12) {
                displayUsername = displayUsername.substring(0, 13) + "... "
            }

            messageBuilder.append(displayUsername).append(": ").append(messageString).append("\n")
        }

        val targetPerson = if (userId == null) null else participants[userId]

        targetPerson?.streakExpirationTimestamp?.takeIf { it > 0 }?.let {
            val timeSecondDiff = ((it - System.currentTimeMillis()) / 1000 / 60).toInt()
            if (timeSecondDiff <= 0) return@let
            messageBuilder.append("\n")
                .append("\uD83D\uDD25 ") //fire emoji
                .append(
                    context.translation.format("conversation_preview.streak_expiration",
                    "day" to (timeSecondDiff / 60 / 24).toString(),
                    "hour" to (timeSecondDiff / 60 % 24).toString(),
                    "minute" to (timeSecondDiff % 60).toString()
                ))
        }

        messages.lastOrNull()?.let {
            messageBuilder
                .append("\n\n")
                .append(context.translation.format("conversation_preview.total_messages", "count" to it.serverMessageId.toString()))
                .append("\n")
        }

        //alert dialog
        val builder = ViewAppearanceHelper.newAlertDialogBuilder(context.mainActivity)
        builder.setTitle(context.translation["conversation_preview.title"])
        builder.setMessage(messageBuilder.toString())
        builder.setPositiveButton(
            "OK"
        ) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
        targetPerson?.let {
            builder.setNegativeButton(context.translation["modal_option.profile_info"]) { _, _ ->
                context.executeAsync { showProfileInfo(it) }
            }
        }
        builder.show()
    }

    @Composable
    private fun MenuElement(
        index: Int,
        icon: ImageVector,
        text: String,
        onClick: () -> Unit,
        onLongClick: (() -> Unit)? = null,
        content: @Composable RowScope.() -> Unit = {}
    ) {
        if (index > 0) {
            Spacer(modifier = Modifier
                .height(1.dp)
                .background(remember { if (context.androidContext.isDarkTheme()) Color(0x1affffff) else Color(0xffeeeeee) })
                .fillMaxWidth())
        }
        Surface(
            color = Color(sigColorBackgroundSurface),
            contentColor = Color(sigColorTextPrimary),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                onLongClick?.invoke()
                            },
                            onTap = {
                                onClick()
                            }
                        )
                    }
                    .heightIn(min = 55.dp)
                    .padding(start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier
                    .size(32.dp)
                    .padding(end = 8.dp))
                Text(
                    text = text,
                    modifier = Modifier.weight(1f),
                    lineHeight = 18.sp,
                    fontSize = 16.sp,
                )
                content()
            }
        }
    }

    override fun inject(parent: ViewGroup, view: View, viewConsumer: ((View) -> Unit)) {
        val modContext = context

        val friendFeedMenuOptions by context.config.userInterface.friendFeedMenuButtons
        if (friendFeedMenuOptions.isEmpty()) return

        val messaging = context.feature(Messaging::class)
        val conversationId = messaging.lastFocusedConversationId ?: return
        val targetUser by lazy { context.database.getDMOtherParticipant(conversationId) }
        messaging.resetLastFocusedConversation()

        val translation = context.translation.getCategory("friend_menu_option")

        @Composable
        fun ComposeFriendFeedMenu() {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                var elementIndex by remember { mutableIntStateOf(0) }

                if (friendFeedMenuOptions.contains("conversation_info")) {
                    MenuElement(
                        remember { elementIndex++ },
                        Icons.Outlined.RemoveRedEye,
                        translation["preview"],
                        onClick = {
                            showPreview(targetUser, conversationId)
                        }
                    )
                }

                modContext.features.getRuleFeatures().forEach { ruleFeature ->
                    if (!friendFeedMenuOptions.contains(ruleFeature.ruleType.key)) return@forEach

                    val ruleState = ruleFeature.getRuleState() ?: return@forEach
                    var state by remember { mutableStateOf(ruleFeature.getState(conversationId)) }

                    fun toggle() {
                        state = !ruleFeature.getState(conversationId)
                        ruleFeature.setState(conversationId, state)
                        context.inAppOverlay.showStatusToast(
                            if (ruleFeature.getState(conversationId)) Icons.Default.CheckCircleOutline else Icons.Default.NotInterested,
                            context.translation.format("rules.toasts.${if (ruleFeature.getState(conversationId)) "enabled" else "disabled"}", "ruleName" to context.translation[ruleFeature.ruleType.translateOptionKey(ruleState.key)]),
                            durationMs = 1500
                        )
                        context.mainActivity?.triggerRootCloseTouchEvent()
                    }

                    MenuElement(
                        remember { elementIndex++ },
                        icon = ruleFeature.ruleType.icon,
                        text = context.translation[ruleFeature.ruleType.translateOptionKey(ruleState.key)],
                        onClick = {
                            toggle()
                        }
                    ) {
                        Switch(
                            checked = state,
                            onCheckedChange = {
                                state = it
                                toggle()
                            }
                        )
                    }
                }

                if (friendFeedMenuOptions.contains("mark_snaps_as_seen")) {
                    MenuElement(
                        remember { elementIndex++ },
                        Icons.Outlined.EditNote,
                        translation["mark_snaps_as_seen"],
                        onClick = {
                            context.apply {
                                mainActivity?.triggerRootCloseTouchEvent()
                                feature(AutoMarkAsRead::class).markSnapsAsSeen(conversationId)
                            }
                        }
                    )
                }

                if (targetUser != null && friendFeedMenuOptions.contains("mark_stories_as_seen_locally")) {
                    val markAsSeenTranslation = remember { context.translation.getCategory("mark_as_seen") }

                    MenuElement(
                        remember { elementIndex++ },
                        Icons.Outlined.RemoveRedEye,
                        translation["mark_stories_as_seen_locally"],
                        onClick = {
                            context.apply {
                                mainActivity?.triggerRootCloseTouchEvent()
                                inAppOverlay.showStatusToast(
                                    Icons.Default.Info,
                                    if (database.setStoriesViewedState(targetUser!!, true)) markAsSeenTranslation["seen_toast"]
                                    else markAsSeenTranslation["already_seen_toast"],
                                    durationMs = 2500
                                )
                            }
                        },
                        onLongClick = {
                            view.post {
                                context.apply {
                                    mainActivity?.triggerRootCloseTouchEvent()
                                    inAppOverlay.showStatusToast(
                                        Icons.Default.Info,
                                        if (database.setStoriesViewedState(targetUser!!, false)) markAsSeenTranslation["unseen_toast"]
                                        else markAsSeenTranslation["already_unseen_toast"],
                                        durationMs = 2500
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }

        viewConsumer(
            createComposeView(view.context) {
                CompositionLocalProvider(
                    LocalTextStyle provides LocalTextStyle.current.merge(TextStyle(fontFamily = avenirNextMediumFont))
                ) {
                    ComposeFriendFeedMenu()
                }
            }.apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        )

        if (context.config.scripting.integratedUI.get()) {
            context.scriptRuntime.eachModule {
                val interfaceManager = getBinding(InterfaceManager::class)
                    ?.takeIf {
                        it.hasInterface(EnumScriptInterface.FRIEND_FEED_CONTEXT_MENU)
                    } ?: return@eachModule

                viewConsumer(LinearLayout(view.context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )

                    applyTheme(view.width, hasRadius = true)

                    orientation = LinearLayout.VERTICAL
                    addView(createComposeView(view.context) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            ScriptInterface(interfaceBuilder = remember {
                                interfaceManager.buildInterface(EnumScriptInterface.FRIEND_FEED_CONTEXT_MENU, mapOf(
                                    "conversationId" to conversationId,
                                    "userId" to targetUser
                                ))
                            } ?: return@Surface)
                        }
                    })
                })
            }
        }
    }
}