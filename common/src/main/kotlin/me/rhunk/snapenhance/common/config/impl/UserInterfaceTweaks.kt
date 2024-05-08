package me.rhunk.snapenhance.common.config.impl

import android.graphics.Color
import me.rhunk.snapenhance.common.config.ConfigContainer
import me.rhunk.snapenhance.common.config.FeatureNotice
import me.rhunk.snapenhance.common.data.MessagingRuleType

class UserInterfaceTweaks : ConfigContainer() {
    class BootstrapOverride : ConfigContainer() {
        companion object {
            val tabs = arrayOf("map", "chat", "camera", "discover", "spotlight")
        }

        val appAppearance = unique("app_appearance", "always_light", "always_dark")
        val homeTab = unique("home_tab", *tabs) { addNotices(FeatureNotice.UNSTABLE) }
    }

    inner class FriendFeedMessagePreview : ConfigContainer(hasGlobalState = true) {
        val amount = integer("amount", defaultValue = 1)
    }

    class ColorsConfig : ConfigContainer() {
        val textColor = color("text_color")
        val chatChatTextColor = color("chat_chat_text_color")
        val pendingSendingTextColor = color("pending_sending_text_color")
        val snapWithSoundTextColor = color("snap_with_sound_text_color")
        val snapWithoutSoundTextColor = color("snap_without_sound_text_color")
        val backgroundColor = color("background_color")
        val backgroundColorSurface = color("background_color_surface")
        val actionMenuBackgroundColor = color("action_menu_background_color")
        val actionMenuRoundBackgroundColor = color("action_menu_round_background_color")
        val cameraGridLines = color("camera_grid_lines")
    }

    inner class CustomizeUIConfig : ConfigContainer() {
        val themePicker = unique("theme_picker", 
                                 "amoled_dark_mode", 
                                 "modern_minimalism", 
                                 "serene_nature", 
                                 "energetic_pop", 
                                 "luxurious_night", 
                                 "playful_candy", 
                                 "retro_arcade", 
                                 "rustic_country", 
                                 "ocean_breeze", 
                                 "sunset_glow", 
                                 "space_adventure", 
                                 "light_blue", 
                                 "dark_blue",
                                 "earthy_autumn",
                                 "watercolor_wash",
                                 "lemon_zest",
                                 "tropical_paradise",
                                 "industrial_chic",
                                 "cherry_bomb",
                                 "woodland_mystery",
                                 "galaxy_glitter",
                                 "creamy_vanilla",
                                 "spicy_chili",
                                 "spring_meadow",
                                 "midnight_library",
                                 "lemon_sorbet",
                                 "cosmic_night",
                                 "spicy_mustard",
                                 "peppermint_candy",
                                 "gingerbread_house",
                                 "art_deco_glam",
                                 "ocean_depths",
                                 "bubblegum_pink",
                                 "firefly_night",
                                 "apple_orchard",
                                 "lavender_field",
                                 "lemon_drop",
                                 "modern_farmhouse",
                                 "black_cat",
                                 "mint_chocolate",
                                 "desert_sunset",
                                 "pumpkin_spice",
                                 "sky_blue",
                                 "tropical_jungle",
                                 "black_marble",
                                 "coffee_shop",
                                 "mermaid_lagoon",
                                 "creamsicle",
                                 "vintage_comic",
                                 "neon_arcade",
                                 "space_cadet",
                                 "cherry_blossom",
                                 "cyber_glitch",
                                 "ginger_snap",
                                 "vintage_travel",
                                 "lava_flow",
                                 "cotton_candy_clouds",
                                 "rusty_robot",
                                 "ocean_fog",
                                 "stained_glass",
                                 "spicy_chili_pepper",
                                 "minimalist_night",
                                 "pirate_treasure",
                                 "lemon_meringue",
                                 "alien_landscape",
                                 "custom")
        
        val colors = container("colors", ColorsConfig())
    }

    val friendFeedMenuButtons = multiple(
        "friend_feed_menu_buttons","conversation_info", "mark_snaps_as_seen", "mark_stories_as_seen_locally", *MessagingRuleType.entries.filter { it.showInFriendMenu }.map { it.key }.toTypedArray()
    ).apply {
        set(mutableListOf("conversation_info", MessagingRuleType.STEALTH.key))
    }
    val customizeUi = container("customize_ui", CustomizeUIConfig()) { addNotices(FeatureNotice.UNSTABLE); requireRestart() }
    val friendFeedMessagePreview = container("friend_feed_message_preview", FriendFeedMessagePreview()) { requireRestart() }
    val snapPreview = boolean("snap_preview") { addNotices(FeatureNotice.UNSTABLE); requireRestart() }
    val bootstrapOverride = container("bootstrap_override", BootstrapOverride()) { requireRestart() }
    val mapFriendNameTags = boolean("map_friend_nametags") { requireRestart() }
    val preventMessageListAutoScroll = boolean("prevent_message_list_auto_scroll") { requireRestart(); addNotices(FeatureNotice.UNSTABLE) }
    val streakExpirationInfo = boolean("streak_expiration_info") { requireRestart() }
    val hideFriendFeedEntry = boolean("hide_friend_feed_entry") { requireRestart() }
    val hideStreakRestore = boolean("hide_streak_restore") { requireRestart() }
    val hideQuickAddFriendFeed = boolean("hide_quick_add_friend_feed") { requireRestart() }
    val hideStorySuggestions = multiple("hide_story_suggestions", "hide_friend_suggestions", "hide_suggested_friend_stories", "hide_my_stories") { requireRestart() }
    val hideUiComponents = multiple("hide_ui_components",
        "hide_voice_record_button",
        "hide_stickers_button",
        "hide_live_location_share_button",
        "hide_chat_call_buttons",
        "hide_profile_call_buttons",
        "hide_unread_chat_hint",
    ) { requireRestart() }
    val operaMediaQuickInfo = boolean("opera_media_quick_info") { requireRestart() }
    val oldBitmojiSelfie = unique("old_bitmoji_selfie", "2d", "3d") { requireCleanCache() }
    val disableSpotlight = boolean("disable_spotlight") { requireRestart() }
    val hideSettingsGear = boolean("hide_settings_gear") { requireRestart() }
    val verticalStoryViewer = boolean("vertical_story_viewer") { requireRestart() }
    val messageIndicators = multiple("message_indicators", "encryption_indicator", "platform_indicator", "location_indicator", "ovf_editor_indicator", "director_mode_indicator") { requireRestart() }
    val stealthModeIndicator = boolean("stealth_mode_indicator") { requireRestart() }
    val editTextOverride = multiple("edit_text_override", "multi_line_chat_input", "bypass_text_input_limit") {
        requireRestart(); addNotices(FeatureNotice.BAN_RISK, FeatureNotice.INTERNAL_BEHAVIOR)
    }
}
