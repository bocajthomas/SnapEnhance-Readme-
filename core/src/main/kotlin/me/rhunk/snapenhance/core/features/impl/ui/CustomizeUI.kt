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
            ).apply {
            }.filterValues { it != null }.map { (key, value) ->
                getAttribute(key) to value!!
            }.toMap()
        } 
        if (themePicker == "material_you") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val colorScheme = dynamicDarkColorScheme(context.androidContext)
                themes.clear()
                themes[themePicker] = mapOf(
                    "sigColorTextPrimary" to colorScheme.onSurfaceVariant.toArgb(),
                    "sigColorChatChat" to colorScheme.onSurfaceVariant.toArgb(),
                    "sigColorChatPendingSending" to colorScheme.onSurfaceVariant.toArgb(),
                    "sigColorChatSnapWithSound" to colorScheme.onSurfaceVariant.toArgb(),
                    "sigColorChatSnapWithoutSound" to colorScheme.onSurfaceVariant.toArgb(),
                    "sigColorBackgroundMain" to colorScheme.background.toArgb(),
                    "sigColorBackgroundSurface" to colorScheme.background.toArgb(),
                    "listDivider" to colorScheme.primary.copy(alpha = 0.12f).toArgb(),
                    "actionSheetBackgroundDrawable" to colorScheme.background.toArgb(),
                    "actionSheetRoundedBackgroundDrawable" to colorScheme.background.toArgb(),
                    "sigExceptionColorCameraGridLines" to colorScheme.background.toArgb(),
                ).apply {
                }.filterValues { true }.map { (key, value) ->
                    getAttribute(key) to value
                }.toMap()
            }
        }

        context.androidContext.theme.javaClass.getMethod("obtainStyledAttributes", IntArray::class.java).hook(
            HookStage.AFTER) { param ->
            val array = param.arg<IntArray>(0)
            val result = param.getResult() as TypedArray

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
           )
       ).mapValues { (_, attributes) ->
            attributes.map { (key, value) ->
                getAttribute(key) to value as Any
            }.toMap()
        }.toMutableMap()
    }
}
