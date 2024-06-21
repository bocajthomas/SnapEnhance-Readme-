package me.rhunk.snapenhance.core.features.impl.ui

import android.content.res.TypedArray
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.TypedValue
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.ui.graphics.toArgb
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.features.FeatureLoadParams
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.Hooker
import me.rhunk.snapenhance.core.util.hook.hook
import me.rhunk.snapenhance.core.util.ktx.getIdentifier

class CustomizeUI: Feature("Customize UI", loadParams = FeatureLoadParams.ACTIVITY_CREATE_SYNC) {
    private fun getAttribute(name: String): Int {
        return context.resources.getIdentifier(name, "attr")
    }

    override fun onActivityCreate() {
        val customizeUIConfig = context.config.userInterface.customizeUi
        val themePicker = customizeUIConfig.themePicker.getNullable() ?: return
        val colorsConfig = context.config.userInterface.customizeUi.colors
        val experimentalColors = context.config.experimental.experimentalColors
        val isColorDebug = customizeUIConfig.colorsDebug.get()
        
        if (themePicker == "custom") {
            themes.clear()
            themes[themePicker] = mapOf(
                "sigColorTextPrimary" to colorsConfig.textColor.getNullable(),
                "sigColorChatChat" to colorsConfig.chatChatTextColor.getNullable(),
                "sigColorChatPendingSending" to  colorsConfig.pendingSendingTextColor.getNullable(),
                "sigColorChatSnapWithSound" to colorsConfig.snapWithSoundTextColor.getNullable(),
                "sigColorChatSnapWithoutSound" to colorsConfig.snapWithoutSoundTextColor.getNullable(),
                "sigColorBackgroundMain" to colorsConfig.backgroundColor.getNullable(),
                "listDivider" to colorsConfig.friendFeedConversationsLineColor.getNullable(),
                "sigColorBackgroundSurface" to colorsConfig.backgroundColorSurface.getNullable(),
                "actionSheetBackgroundDrawable" to colorsConfig.actionMenuBackgroundColor.getNullable(),
                "actionSheetRoundedBackgroundDrawable" to colorsConfig.actionMenuRoundBackgroundColor.getNullable(),
                "sigExceptionColorCameraGridLines" to colorsConfig.cameraGridLines.getNullable(),
                "listBackgroundDrawable" to colorsConfig.listBackgroundDrawable.getNullable(),
                "sigColorIconPrimary" to colorsConfig.sigColorIconPrimary.getNullable(),
                "actionSheetDescriptionTextColor" to colorsConfig.actionSheetDescriptionTextColor.getNullable(),
                "ringColor" to experimentalColors.ringColor.getNullable(),
                "sigColorIconSecondary" to experimentalColors.sigColorIconSecondary.getNullable(),
                "itemShapeFillColor" to experimentalColors.itemShapeFillColor.getNullable(),
                "ringStartColor" to experimentalColors.ringStartColor.getNullable(),
                "sigColorLayoutPlaceholder" to experimentalColors.sigColorLayoutPlaceholder.getNullable(),
                "scButtonColor" to experimentalColors.scButtonColor.getNullable(),
                "recipientPillBackgroundDrawable" to experimentalColors.recipientPillBackgroundDrawable.getNullable(),
                "boxBackgroundColor" to experimentalColors.boxBackgroundColor.getNullable(),
                "editTextColor" to experimentalColors.editTextColor.getNullable(),
                "chipBackgroundColor" to experimentalColors.chipBackgroundColor.getNullable(),
                "recipientInputStyle" to experimentalColors.recipientInputStyle.getNullable(),
                "rangeFillColor" to experimentalColors.rangeFillColor.getNullable(),
                "pstsIndicatorColor" to experimentalColors.pstsIndicatorColor.getNullable(),
                "pstsTabBackground" to experimentalColors.pstsTabBackground.getNullable(),
                "pstsDividerColor" to experimentalColors.pstsDividerColor.getNullable(),
                "tabTextColor" to experimentalColors.tabTextColor.getNullable(),
                "statusBarForeground" to experimentalColors.statusBarForeground.getNullable(),
                "statusBarBackground" to experimentalColors.statusBarBackground.getNullable(),
                "strokeColor" to experimentalColors.strokeColor.getNullable(),
                "storyReplayViewRingColor" to experimentalColors.storyReplayViewRingColor.getNullable(),
                "sigColorButtonPrimary" to experimentalColors.sigColorButtonPrimary.getNullable(),
                "sigColorBaseAppYellow" to experimentalColors.sigColorBaseAppYellow.getNullable(),
                "sigColorBackgroundSurfaceTranslucent" to experimentalColors.sigColorBackgroundSurfaceTranslucent.getNullable(),
                "sigColorStoryRingFriendsFeedStoryRing" to experimentalColors.sigColorStoryRingFriendsFeedStoryRing.getNullable(),
                "sigColorStoryRingDiscoverTabThumbnailStoryRing" to experimentalColors.sigColorStoryRingDiscoverTabThumbnailStoryRing.getNullable(),
            ).filterValues { it != null }.map { (key, value) ->
                getAttribute(key) to value!!
            }.toMap()
        } 
        if (themePicker == "material_you_light" || themePicker == "material_you_dark") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val colorScheme = dynamicDarkColorScheme(context.androidContext)
                val light = themePicker == "material_you_light"
                themes.clear()
                val surfaceVariant = (if (light) colorScheme.surfaceVariant else colorScheme.onSurfaceVariant).toArgb()
                val background = (if (light) colorScheme.onBackground else colorScheme.background).toArgb()

                themes[themePicker] = mapOf(
                    "sigColorTextPrimary" to surfaceVariant,
                    "sigColorChatChat" to surfaceVariant,
                    "sigColorChatPendingSending" to surfaceVariant,
                    "sigColorChatSnapWithSound" to surfaceVariant,
                    "sigColorChatSnapWithoutSound" to surfaceVariant,
                    "sigColorBackgroundMain" to background,
                    "sigColorBackgroundSurface" to background,
                    "listDivider" to colorScheme.onPrimary.copy(alpha = 0.12f).toArgb(),
                    "actionSheetBackgroundDrawable" to background,
                    "actionSheetRoundedBackgroundDrawable" to background,
                    "sigExceptionColorCameraGridLines" to background,
                ).map { getAttribute(it.key) to it.value }.toMap()
            }
        }

        context.androidContext.theme.javaClass.getMethod("obtainStyledAttributes", IntArray::class.java).hook(
            HookStage.AFTER) { param ->
            val array = param.arg<IntArray>(0)
            val result = param.getResult() as TypedArray
            if(isColorDebug) {
                context.log.verbose(context.resources.getResourceName(array[0]))
            }

            fun ephemeralHook(methodName: String, content: Any) {
                Hooker.ephemeralHookObjectMethod(result::class.java, result, methodName, HookStage.BEFORE) {
                    it.setResult(content)
                }
            }

            themes[themePicker]?.get(array[0])?.let { value ->
                when (val attributeType = result.getType(0)) {
                    TypedValue.TYPE_INT_COLOR_ARGB8, TypedValue.TYPE_INT_COLOR_RGB8, TypedValue.TYPE_INT_COLOR_ARGB4, TypedValue.TYPE_INT_COLOR_RGB4 -> {
                        ephemeralHook("getColor", (value as Number).toInt())
                    }
                    TypedValue.TYPE_STRING -> {
                        val stringValue = result.getString(0)
                        if (stringValue?.endsWith(".xml") == true) {
                            ephemeralHook("getDrawable", ColorDrawable((value as Number).toInt()))
                        }
                    }
                    else -> context.log.warn("unknown attribute type: ${attributeType.toString(16)}")
                }
            }
        }
    }

    private val themes by lazy {
       mapOf(
           "amoled_dark_mode" to mapOf(
               "sigColorTextPrimary" to 0xFFFFFFFF,
               "sigColorBackgroundMain" to 0xFF000000,
               "sigColorBackgroundSurface" to 0xFF000000,
               "listDivider" to 0xFF000000,
               "actionSheetBackgroundDrawable" to 0xFF000000,
               "actionSheetRoundedBackgroundDrawable" to 0xFF000000
           ),
           "light_blue" to mapOf(
               "sigColorTextPrimary" to 0xFF03BAFC,
               "sigColorBackgroundMain" to 0xFFBDE6FF,
               "sigColorBackgroundSurface" to 0xFF78DBFF,
               "listDivider" to 0xFFBDE6FF,
               "actionSheetBackgroundDrawable" to 0xFF78DBFF,
               "sigColorChatChat" to 0xFF08D6FF,
               "sigColorChatPendingSending" to 0xFF08D6FF,
               "sigColorChatSnapWithSound" to 0xFF08D6FF,
               "sigColorChatSnapWithoutSound" to 0xFF08D6FF,
               "sigExceptionColorCameraGridLines" to 0xFF08D6FF
           ),
           "dark_blue" to mapOf(
               "sigColorTextPrimary" to 0xFF98C2FD,
               "sigColorBackgroundMain" to 0xFF192744,
               "sigColorBackgroundSurface" to 0xFF192744,
               "actionSheetBackgroundDrawable" to 0xFF192744,
               "sigColorChatChat" to 0xFF98C2FD,
               "sigColorChatPendingSending" to 0xFF98C2FD,
               "sigColorChatSnapWithSound" to 0xFF98C2FD,
               "sigColorChatSnapWithoutSound" to 0xFF98C2FD,
               "sigExceptionColorCameraGridLines" to 0xFF192744
           ),
           "midnight_slate" to mapOf(
               "sigColorTextPrimary" to 0xFFFFFFFF,
               "sigColorBackgroundMain" to 0xFF1F2833,
               "sigColorBackgroundSurface" to 0xFF1F2833,
               "actionSheetBackgroundDrawable" to 0xFF2C3E50,
               "actionSheetRoundedBackgroundDrawable" to 0xFF34495E,
               "listDivider" to 0xFF1F2833,
               "sigColorChatChat" to 0xFFFFFF00,
               "sigColorChatPendingSending" to 0xFF00FFFF,
               "sigColorChatSnapWithSound" to 0xFFFF9800,
               "sigColorChatSnapWithoutSound" to 0xFF00E676,
               "sigExceptionColorCameraGridLines" to 0xFFFFFFFF,
               "sigColorIconPrimary" to 0xFFFFFF00
           ),
           "earthy_autumn" to mapOf(
               "sigColorTextPrimary" to 0xFFF7CAC9,
               "sigColorBackgroundMain" to 0xFF800000,
               "sigColorBackgroundSurface" to 0xFF800000,
               "actionSheetBackgroundDrawable" to 0xFF800000,
               "sigColorChatChat" to 0xFFF7CAC9,
               "sigColorChatPendingSending" to 0xFFF7CAC9,
               "sigColorChatSnapWithSound" to 0xFFF7CAC9,
               "sigColorChatSnapWithoutSound" to 0xFFF7CAC9,
               "sigExceptionColorCameraGridLines" to 0xFF800000
           ),
           "mint_chocolate" to mapOf(
               "sigColorTextPrimary" to 0xFFFFFFFF,
               "sigColorBackgroundMain" to 0xFF98FF98,
               "sigColorBackgroundSurface" to 0xFF98FF98,
               "actionSheetBackgroundDrawable" to 0xFF98FF98,
               "sigColorChatChat" to 0xFFFFFFFF,
               "sigColorChatPendingSending" to 0xFFFFFFFF,
               "sigColorChatSnapWithSound" to 0xFFFFFFFF,
               "sigColorChatSnapWithoutSound" to 0xFFFFFFFF,
               "sigExceptionColorCameraGridLines" to 0xFF98FF98
           ),
           "ginger_snap" to mapOf(
               "sigColorTextPrimary" to 0xFFFFFFFF,
               "sigColorBackgroundMain" to 0xFFC6893A,
               "sigColorBackgroundSurface" to 0xFFC6893A,
               "actionSheetBackgroundDrawable" to 0xFFC6893A,
               "sigColorChatChat" to 0xFFFFFFFF,
               "sigColorChatPendingSending" to 0xFFFFFFFF,
               "sigColorChatSnapWithSound" to 0xFFFFFFFF,
               "sigColorChatSnapWithoutSound" to 0xFFFFFFFF,
               "sigExceptionColorCameraGridLines" to 0xFFC6893A
           ),
           "lemon_meringue" to mapOf(
               "sigColorTextPrimary" to 0xFF000000,
               "sigColorBackgroundMain" to 0xFFFCFFE7,
               "sigColorBackgroundSurface" to 0xFFFCFFE7,
               "actionSheetBackgroundDrawable" to 0xFFFCFFE7,
               "sigColorChatChat" to 0xFF000000,
               "sigColorChatPendingSending" to 0xFF000000,
               "sigColorChatSnapWithSound" to 0xFF000000,
               "sigColorChatSnapWithoutSound" to 0xFF000000,
               "sigExceptionColorCameraGridLines" to 0xFFFCFFE7
           ),
           "lava_flow" to mapOf(
               "sigColorTextPrimary" to 0xFFFFCC00,
               "sigColorBackgroundMain" to 0xFFC70039,
               "sigColorBackgroundSurface" to 0xFFC70039,
               "actionSheetBackgroundDrawable" to 0xFFC70039,
               "sigColorChatChat" to 0xFFFFCC00,
               "sigColorChatPendingSending" to 0xFFFFCC00,
               "sigColorChatSnapWithSound" to 0xFFFFCC00,
               "sigColorChatSnapWithoutSound" to 0xFFFFCC00,
               "sigExceptionColorCameraGridLines" to 0xFFC70039
           ),
           "ocean_fog" to mapOf(
               "sigColorTextPrimary" to 0xFF333333,
               "sigColorBackgroundMain" to 0xFFB0C4DE,
               "sigColorBackgroundSurface" to 0xFFB0C4DE,
               "actionSheetBackgroundDrawable" to 0xFFB0C4DE,
               "sigColorChatChat" to 0xFF333333,
               "sigColorChatPendingSending" to 0xFF333333,
               "sigColorChatSnapWithSound" to 0xFF333333,
               "sigColorChatSnapWithoutSound" to 0xFF333333,
               "sigExceptionColorCameraGridLines" to 0xFFB0C4DE
           ),
           "alien_landscape" to mapOf(
               "sigColorTextPrimary" to 0xFFFFFFFF,
               "sigColorBackgroundMain" to 0xFF9B59B6,
               "sigColorBackgroundSurface" to 0xFF9B59B6,
               "actionSheetBackgroundDrawable" to 0xFF9B59B6,
               "sigColorChatChat" to 0xFFFFFFFF,
               "sigColorChatPendingSending" to 0xFFFFFFFF,
               "sigColorChatSnapWithSound" to 0xFFFFFFFF,
               "sigColorChatSnapWithoutSound" to 0xFFFFFFFF,
               "sigExceptionColorCameraGridLines" to 0xFF9B59B6
           ),
           "watercolor_wash" to mapOf(
               "sigColorTextPrimary" to 0xFF3F51B5,
               "sigColorBackgroundMain" to 0xFFFFF5F3,
               "sigColorBackgroundSurface" to 0xFFFFF5F3,
               "actionSheetBackgroundDrawable" to 0xFFFFF5F3,
               "sigColorChatChat" to 0xFF3F51B5,
               "sigColorChatPendingSending" to 0xFF3F51B5,
               "sigColorChatSnapWithSound" to 0xFF3F51B5,
               "sigColorChatSnapWithoutSound" to 0xFF3F51B5,
               "sigExceptionColorCameraGridLines" to 0xFFFFF5F3
           ),
           "zesty_lemon" to mapOf(
               "sigColorTextPrimary" to 0xFF222222,
               "sigColorBackgroundMain" to 0xFFFFFFE0,
               "sigColorBackgroundSurface" to 0xFFFFFFE0,
               "actionSheetBackgroundDrawable" to 0xFFFFFFE0,
               "sigColorChatChat" to 0xFF222222,
               "sigColorChatPendingSending" to 0xFF222222,
               "sigColorChatSnapWithSound" to 0xFF222222,
               "sigColorChatSnapWithoutSound" to 0xFF222222,
               "sigExceptionColorCameraGridLines" to 0xFFFFFFE0
           ),
           "tropical_paradise" to mapOf(
               "sigColorTextPrimary" to 0xFF000000,
               "sigColorBackgroundMain" to 0xFFD3FFCE,
               "sigColorBackgroundSurface" to 0xFFD3FFCE,
               "actionSheetBackgroundDrawable" to 0xFFD3FFCE,
               "sigColorChatChat" to 0xFF000000,
               "sigColorChatPendingSending" to 0xFF000000,
               "sigColorChatSnapWithSound" to 0xFF000000,
               "sigColorChatSnapWithoutSound" to 0xFF000000,
               "sigExceptionColorCameraGridLines" to 0xFFD3FFCE
           ),
           "industrial_chic" to mapOf(
               "sigColorTextPrimary" to 0xFF424242,
               "sigColorBackgroundMain" to 0xFFEEEEEE,
               "sigColorBackgroundSurface" to 0xFFEEEEEE,
               "actionSheetBackgroundDrawable" to 0xFFEEEEEE,
               "sigColorChatChat" to 0xFF424242,
               "sigColorChatPendingSending" to 0xFF424242,
               "sigColorChatSnapWithSound" to 0xFF424242,
               "sigColorChatSnapWithoutSound" to 0xFF424242,
               "sigExceptionColorCameraGridLines" to 0xFFEEEEEE
           ),
           "cherry_bomb" to mapOf(
               "sigColorTextPrimary" to 0xFFFFFFFF,
               "sigColorBackgroundMain" to 0xFFC24641,
               "sigColorBackgroundSurface" to 0xFFC24641,
               "actionSheetBackgroundDrawable" to 0xFFC24641,
               "sigColorChatChat" to 0xFFFFFFFF,
               "sigColorChatPendingSending" to 0xFFFFFFFF,
               "sigColorChatSnapWithSound" to 0xFFFFFFFF,
               "sigColorChatSnapWithoutSound" to 0xFFFFFFFF,
               "sigExceptionColorCameraGridLines" to 0xFFC24641
           ),
           "woodland_mystery" to mapOf(
               "sigColorTextPrimary" to 0xFFC2C2F0,
               "sigColorBackgroundMain" to 0xFF333333,
               "sigColorBackgroundSurface" to 0xFF333333,
               "actionSheetBackgroundDrawable" to 0xFF333333,
               "sigColorChatChat" to 0xFFC2C2F0,
               "sigColorChatPendingSending" to 0xFFC2C2F0,
               "sigColorChatSnapWithSound" to 0xFFC2C2F0,
               "sigColorChatSnapWithoutSound" to 0xFFC2C2F0,
               "sigExceptionColorCameraGridLines" to 0xFF333333
           ),
           "galaxy_glitter" to mapOf(
               "sigColorTextPrimary" to 0xFFFFFFFF,
               "sigColorBackgroundMain" to 0xFF2F4F4F,
               "sigColorBackgroundSurface" to 0xFF2F4F4F,
               "actionSheetBackgroundDrawable" to 0xFF2F4F4F,
               "sigColorChatChat" to 0xFFFFFFFF,
               "sigColorChatPendingSending" to 0xFFFFFFFF,
               "sigColorChatSnapWithSound" to 0xFFFFFFFF,
               "sigColorChatSnapWithoutSound" to 0xFFFFFFFF,
               "sigExceptionColorCameraGridLines" to 0xFF2F4F4F
           ),
           "creamy_vanilla" to mapOf(
               "sigColorTextPrimary" to 0xFF333333,
               "sigColorBackgroundMain" to 0xFFF1F1F1,
               "sigColorBackgroundSurface" to 0xFFF1F1F1,
               "actionSheetBackgroundDrawable" to 0xFFF1F1F1,
               "sigColorChatChat" to 0xFF333333,
               "sigColorChatPendingSending" to 0xFF333333,
               "sigColorChatSnapWithSound" to 0xFF333333,
               "sigColorChatSnapWithoutSound" to 0xFF333333,
               "sigExceptionColorCameraGridLines" to 0xFFF1F1F1
           ),
           "spicy_chili" to mapOf(
               "sigColorTextPrimary" to 0xFFF5F5F5,
               "sigColorBackgroundMain" to 0xFFC70039,
               "sigColorBackgroundSurface" to 0xFFC70039,
               "actionSheetBackgroundDrawable" to 0xFFC70039,
               "sigColorChatChat" to 0xFFF5F5F5,
               "sigColorChatPendingSending" to 0xFFF5F5F5,
               "sigColorChatSnapWithSound" to 0xFFF5F5F5,
               "sigColorChatSnapWithoutSound" to 0xFFF5F5F5,
               "sigExceptionColorCameraGridLines" to 0xFFC70039
           ),
           "spring_meadow" to mapOf(
               "sigColorTextPrimary" to 0xFF388E3C,
               "sigColorBackgroundMain" to 0xFFF5FBE0,
               "sigColorBackgroundSurface" to 0xFFF5FBE0,
               "actionSheetBackgroundDrawable" to 0xFFF5FBE0,
               "sigColorChatChat" to 0xFF388E3C,
               "sigColorChatPendingSending" to 0xFF388E3C,
               "sigColorChatSnapWithSound" to 0xFF388E3C,
               "sigColorChatSnapWithoutSound" to 0xFF388E3C,
               "sigExceptionColorCameraGridLines" to 0xFFF5FBE0
           ),
           "midnight_library" to mapOf(
               "sigColorTextPrimary" to 0xFFEAEAEA,
               "sigColorBackgroundMain" to 0xFF424242,
               "sigColorBackgroundSurface" to 0xFF424242,
               "actionSheetBackgroundDrawable" to 0xFF424242,
               "sigColorChatChat" to 0xFFEAEAEA,
               "sigColorChatPendingSending" to 0xFFEAEAEA,
               "sigColorChatSnapWithSound" to 0xFFEAEAEA,
               "sigColorChatSnapWithoutSound" to 0xFFEAEAEA,
               "sigExceptionColorCameraGridLines" to 0xFF424242
           ),
           "lemon_sorbet" to mapOf(
               "sigColorTextPrimary" to 0xFF000000,
               "sigColorBackgroundMain" to 0xFFFCFFE7,
               "sigColorBackgroundSurface" to 0xFFFCFFE7,
               "actionSheetBackgroundDrawable" to 0xFFFCFFE7,
               "sigColorChatChat" to 0xFF000000,
               "sigColorChatPendingSending" to 0xFF000000,
               "sigColorChatSnapWithSound" to 0xFF000000,
               "sigColorChatSnapWithoutSound" to 0xFF000000,
               "sigExceptionColorCameraGridLines" to 0xFFFCFFE7
           ),
           "cosmic_night" to mapOf(
               "sigColorTextPrimary" to 0xFFFFFFFF,
               "sigColorBackgroundMain" to 0xFF2F4F4F,
               "sigColorBackgroundSurface" to 0xFF2F4F4F,
               "actionSheetBackgroundDrawable" to 0xFF2F4F4F,
               "sigColorChatChat" to 0xFFFFFFFF,
               "sigColorChatPendingSending" to 0xFFFFFFFF,
               "sigColorChatSnapWithSound" to 0xFFFFFFFF,
               "sigColorChatSnapWithoutSound" to 0xFFFFFFFF,
               "sigExceptionColorCameraGridLines" to 0xFF2F4F4F
           ),
           "spicy_mustard" to mapOf(
               "sigColorTextPrimary" to 0xFFFFFFFF,
               "sigColorBackgroundMain" to 0xFFFCC01E,
               "sigColorBackgroundSurface" to 0xFFFCC01E,
               "actionSheetBackgroundDrawable" to 0xFFFCC01E,
               "sigColorChatChat" to 0xFFFFFFFF,
               "sigColorChatPendingSending" to 0xFFFFFFFF,
               "sigColorChatSnapWithSound" to 0xFFFFFFFF,
               "sigColorChatSnapWithoutSound" to 0xFFFFFFFF,
               "sigExceptionColorCameraGridLines" to 0xFFFCC01E
           ),
           "peppermint_candy" to mapOf(
               "sigColorTextPrimary" to 0xFF29ABCA,
               "sigColorBackgroundMain" to 0xFFFFDDCF,
               "sigColorBackgroundSurface" to 0xFFFFDDCF,
               "actionSheetBackgroundDrawable" to 0xFFFFDDCF,
               "sigColorChatChat" to 0xFF29ABCA,
               "sigColorChatPendingSending" to 0xFF29ABCA,
               "sigColorChatSnapWithSound" to 0xFF29ABCA,
               "sigColorChatSnapWithoutSound" to 0xFF29ABCA,
               "sigExceptionColorCameraGridLines" to 0xFFFFDDCF
           ),
           "gingerbread_house" to mapOf(
               "sigColorTextPrimary" to 0xFF333333,
               "sigColorBackgroundMain" to 0xFFCDB391,
               "sigColorBackgroundSurface" to 0xFFCDB391,
               "actionSheetBackgroundDrawable" to 0xFFCDB391,
               "sigColorChatChat" to 0xFF333333,
               "sigColorChatPendingSending" to 0xFF333333,
               "sigColorChatSnapWithSound" to 0xFF333333,
               "sigColorChatSnapWithoutSound" to 0xFF333333,
               "sigExceptionColorCameraGridLines" to 0xFFCDB391
           ),
           "art_deco_glam" to mapOf(
               "sigColorTextPrimary" to 0xFF000000,
               "sigColorBackgroundMain" to 0xFFF8F8F8,
               "sigColorBackgroundSurface" to 0xFFF8F8F8,
               "actionSheetBackgroundDrawable" to 0xFFF8F8F8,
               "sigColorChatChat" to 0xFF000000,
               "sigColorChatPendingSending" to 0xFF000000,
               "sigColorChatSnapWithSound" to 0xFF000000,
               "sigColorChatSnapWithoutSound" to 0xFF000000,
               "sigExceptionColorCameraGridLines" to 0xFFF8F8F8
           ),
           "ocean_depths" to mapOf(
               "sigColorTextPrimary" to 0xFFFFFFFF,
               "sigColorBackgroundMain" to 0xFF000080,
               "sigColorBackgroundSurface" to 0xFF000080,
               "actionSheetBackgroundDrawable" to 0xFF000080,
               "sigColorChatChat" to 0xFFFFFFFF,
               "sigColorChatPendingSending" to 0xFFFFFFFF,
               "sigColorChatSnapWithSound" to 0xFFFFFFFF,
               "sigColorChatSnapWithoutSound" to 0xFFFFFFFF,
               "sigExceptionColorCameraGridLines" to 0xFF000080
           ),
           "bubblegum_pink" to mapOf(
               "sigColorTextPrimary" to 0xFFFFFFFF,
               "sigColorBackgroundMain" to 0xFFFFC0CB,
               "sigColorBackgroundSurface" to 0xFFFFC0CB,
               "actionSheetBackgroundDrawable" to 0xFFFFC0CB,
               "sigColorChatChat" to 0xFFFFFFFF,
               "sigColorChatPendingSending" to 0xFFFFFFFF,
               "sigColorChatSnapWithSound" to 0xFFFFFFFF,
               "sigColorChatSnapWithoutSound" to 0xFFFFFFFF,
               "sigExceptionColorCameraGridLines" to 0xFFFFC0CB
           ),
           "firefly_night" to mapOf(
               "sigColorTextPrimary" to 0xFFFFFFF0,
               "sigColorBackgroundMain" to 0xFF222222,
               "sigColorBackgroundSurface" to 0xFF222222,
               "actionSheetBackgroundDrawable" to 0xFF222222,
               "sigColorChatChat" to 0xFFFFFFF0,
               "sigColorChatPendingSending" to 0xFFFFFFF0,
               "sigColorChatSnapWithSound" to 0xFFFFFFF0,
               "sigColorChatSnapWithoutSound" to 0xFFFFFFF0,
               "sigExceptionColorCameraGridLines" to 0xFF222222
           ),
           "apple_orchard" to mapOf(
               "sigColorTextPrimary" to 0xFF333333,
               "sigColorBackgroundMain" to 0xFFF4D35E,
               "sigColorBackgroundSurface" to 0xFFF4D35E,
               "actionSheetBackgroundDrawable" to 0xFFF4D35E,
               "sigColorChatChat" to 0xFF333333,
               "sigColorChatPendingSending" to 0xFF333333,
               "sigColorChatSnapWithSound" to 0xFF333333,
               "sigColorChatSnapWithoutSound" to 0xFF333333,
               "sigExceptionColorCameraGridLines" to 0xFFF4D35E
           ),
           "lavender_field" to mapOf(
               "sigColorTextPrimary" to 0xFF293145,
               "sigColorBackgroundMain" to 0xFFBDBDBD,
               "sigColorBackgroundSurface" to 0xFFBDBDBD,
               "actionSheetBackgroundDrawable" to 0xFFBDBDBD,
               "sigColorChatChat" to 0xFF293145,
               "sigColorChatPendingSending" to 0xFF293145,
               "sigColorChatSnapWithSound" to 0xFF293145,
               "sigColorChatSnapWithoutSound" to 0xFF293145,
               "sigExceptionColorCameraGridLines" to 0xFFBDBDBD
           ),
           "lemon_drop" to mapOf(
               "sigColorTextPrimary" to 0xFF000000,
               "sigColorBackgroundMain" to 0xFFFCE5C7,
               "sigColorBackgroundSurface" to 0xFFFCE5C7,
               "actionSheetBackgroundDrawable" to 0xFFFCE5C7,
               "sigColorChatChat" to 0xFF000000,
               "sigColorChatPendingSending" to 0xFF000000,
               "sigColorChatSnapWithSound" to 0xFF000000,
               "sigColorChatSnapWithoutSound" to 0xFF000000,
               "sigExceptionColorCameraGridLines" to 0xFFFCE5C7
           ),
           "modern_farmhouse" to mapOf(
               "sigColorTextPrimary" to 0xFF333333,
               "sigColorBackgroundMain" to 0xFFF2F2F2,
               "sigColorBackgroundSurface" to 0xFFF2F2F2,
               "actionSheetBackgroundDrawable" to 0xFFF2F2F2,
               "sigColorChatChat" to 0xFF333333,
               "sigColorChatPendingSending" to 0xFF333333,
               "sigColorChatSnapWithSound" to 0xFF333333,
               "sigColorChatSnapWithoutSound" to 0xFF333333,
               "sigExceptionColorCameraGridLines" to 0xFFF2F2F2
           ),
       ).mapValues { (_, attributes) ->
            attributes.map { (key, value) ->
                getAttribute(key) to value as Any
            }.toMap()
        }.toMutableMap()
    }
}
