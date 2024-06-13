package me.rhunk.snapenhance.common.config.impl

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Memory
import me.rhunk.snapenhance.common.config.ConfigContainer
import me.rhunk.snapenhance.common.config.ConfigFlag
import me.rhunk.snapenhance.common.config.FeatureNotice

class Experimental : ConfigContainer() {
    companion object {
        val cofExperimentList = listOf(
            "android_action_menu_v2",
            "android_action_menu_adjust_message_position",
            "chat_emoji_reactions_sending_enabled",
            "chat_text_message_plugin",
        )
    }

    class BetterTranscriptConfig: ConfigContainer(hasGlobalState = true) {
        val forceTranscription = boolean("force_transcription") { requireRestart() }
        val preferredTranscriptionLang = string("preferred_transcription_lang") { requireRestart() }
        val enhancedTranscript = boolean("enhanced_transcript") { requireRestart(); addNotices(FeatureNotice.UNSTABLE) }
    }

    class ComposerHooksConfig: ConfigContainer(hasGlobalState = true) {
        val showFirstCreatedUsername = boolean("show_first_created_username")
        val bypassCameraRollLimit = boolean("bypass_camera_roll_limit")
        val composerConsole = boolean("composer_console")
        val composerLogs = boolean("composer_logs")
    }

    class NativeHooks : ConfigContainer(hasGlobalState = true) {
        val composerHooks = container("composer_hooks", ComposerHooksConfig()) { requireRestart() }
        val disableBitmoji = boolean("disable_bitmoji")
        val customEmojiFont = string("custom_emoji_font") {
            requireRestart()
            addNotices(FeatureNotice.UNSTABLE)
            addFlags(ConfigFlag.USER_IMPORT)
            filenameFilter = { it.endsWith(".ttf") }
        }
        val remapExecutable = boolean("remap_executable") { requireRestart(); addNotices(FeatureNotice.INTERNAL_BEHAVIOR, FeatureNotice.UNSTABLE) }
    }

    class E2EEConfig : ConfigContainer(hasGlobalState = true) {
        val encryptedMessageIndicator = boolean("encrypted_message_indicator")
        val forceMessageEncryption = boolean("force_message_encryption")
    }

    class AccountSwitcherConfig : ConfigContainer(hasGlobalState = true) {
        val autoBackupCurrentAccount = boolean("auto_backup_current_account", defaultValue = true)
    }

    class AppLockConfig: ConfigContainer(hasGlobalState = true) {
        val lockOnResume = boolean("lock_on_resume", defaultValue = true)
    }

    class ExperimentalColors: ConfigContainer() {
        val ringColor = color("ring_color")
        val sigColorIconSecondary = color("sig_color_icon_secondary")
        val itemShapeFillColor = color("item_shape_fill_color")
        val ringStartColor = color("ring_start_color")
        val sigColorLayoutPlaceholder = color("sig_color_layout_place_holder")
        val scButtonColor = color("sc_button_color")
        val recipientPillBackgroundDrawable = color("recipient_pill_background_drawable")
        val boxBackgroundColor = color("box_background_color")
        val editTextColor = color("edit_text_color")
        val chipBackgroundColor = color("chip_background_color")
        val recipientInputStyle = color("recipient_input_style")
        val rangeFillColor = color("range_fill_color")
        val pstsIndicatorColor = color("psts_indicator_color")
        val pstsTabBackground = color("psts_tab_background")
        val pstsDividerColor = color("psts_divider_color")
        val tabTextColor = color("tab_text_color")
        val statusBarForeground = color("status_bar_foreground")
        val statusBarBackground = color("status_bar_background")
        val strokeColor = color("stroke_color")
        val storyReplayViewRingColor = color("story_replay_view_ring_color")
        val sigColorButtonPrimary = color("sig_color_button_primary")
        val sigColorBaseAppYellow = color("sig_color_base_app_yellow")
        val sigColorBackgroundSurfaceTranslucent = color("sig_color_background_surface_translucent")
        val sigColorStoryRingFriendsFeedStoryRing = color("sig_color_story_ring_friends_feed_story_ring")
        val sigColorStoryRingDiscoverTabThumbnailStoryRing = color("sig_color_story_ring_discover_tab_thumbnail_story_ring")
    }

    val nativeHooks = container("native_hooks", NativeHooks()) { icon = Icons.Default.Memory; requireRestart() }
    val spoof = container("spoof", Spoof()) { icon = Icons.Default.Fingerprint ; addNotices(FeatureNotice.BAN_RISK); requireRestart() }
    val experimentalColors = container("experimental_colors", ExperimentalColors()) { addNotices(FeatureNotice.UNSTABLE); requireRestart() }
    val convertMessageLocally = boolean("convert_message_locally") { requireRestart() }
    val mediaFilePicker = boolean("media_file_picker") { requireRestart(); addNotices(FeatureNotice.UNSTABLE) }
    val storyLogger = boolean("story_logger") { requireRestart(); addNotices(FeatureNotice.UNSTABLE); }
    val callRecorder = boolean("call_recorder") { requireRestart(); addNotices(FeatureNotice.UNSTABLE); }
    val accountSwitcher = container("account_switcher", AccountSwitcherConfig()) { requireRestart(); addNotices(FeatureNotice.UNSTABLE) }
    val betterTranscript = container("better_transcript", BetterTranscriptConfig()) { requireRestart() }
    val editMessage = boolean("edit_message") { requireRestart() }
    val contextMenuFix = boolean("context_menu_fix") { requireRestart() }
    val cofExperiments = multiple("cof_experiments", *cofExperimentList.toTypedArray()) { requireRestart(); addFlags(ConfigFlag.NO_TRANSLATE); addNotices(FeatureNotice.UNSTABLE) }
    val appLock = container("app_lock", AppLockConfig()) { requireRestart(); addNotices(FeatureNotice.UNSTABLE) }
    val infiniteStoryBoost = boolean("infinite_story_boost")
    val meoPasscodeBypass = boolean("meo_passcode_bypass")
    val noFriendScoreDelay = boolean("no_friend_score_delay") { requireRestart()}
    val bestFriendPinning = boolean("best_friend_pinning") { requireRestart(); addNotices(FeatureNotice.UNSTABLE) }
    val e2eEncryption = container("e2ee", E2EEConfig()) { requireRestart(); nativeHooks() }
    val hiddenSnapchatPlusFeatures = boolean("hidden_snapchat_plus_features") {
        addNotices(FeatureNotice.BAN_RISK, FeatureNotice.UNSTABLE)
        requireRestart()
    }
    val customStreaksExpirationFormat = string("custom_streaks_expiration_format") { requireRestart() }
    val addFriendSourceSpoof = unique("add_friend_source_spoof",
        "added_by_username",
        "added_by_mention",
        "added_by_group_chat",
        "added_by_qr_code",
        "added_by_community",
        "added_by_quick_add",
    ) { addNotices(FeatureNotice.BAN_RISK) }
    val preventForcedLogout = boolean("prevent_forced_logout") { requireRestart(); addNotices(FeatureNotice.BAN_RISK, FeatureNotice.INTERNAL_BEHAVIOR); }
}