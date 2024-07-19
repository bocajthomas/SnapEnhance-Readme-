package me.rhunk.snapenhance.core.features.impl.ui

import android.content.res.TypedArray
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.ParcelFileDescriptor.AutoCloseInputStream
import android.util.TypedValue
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.ui.graphics.toArgb
import com.google.gson.reflect.TypeToken
import me.rhunk.snapenhance.common.bridge.FileHandleScope
import me.rhunk.snapenhance.common.data.DatabaseThemeContent
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hook
import me.rhunk.snapenhance.core.util.ktx.getIdentifier
import me.rhunk.snapenhance.core.util.ktx.getObjectField

class CustomTheming: Feature("Custom Theming") {
    private fun getAttribute(name: String): Int {
        return context.resources.getIdentifier(name, "attr")
    }

    private fun parseAttributeList(vararg attributes: Pair<String, Number>): Map<Int, Int> {
        return attributes.toMap().mapKeys {
            getAttribute(it.key)
        }.filterKeys { it != 0 }.mapValues {
            it.value.toInt()
        }
    }

    override fun init() {
        val customThemeName = context.config.userInterface.customTheme.getNullable() ?: return
        var currentTheme = mapOf<Int, Int>() // resource id -> color

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val colorScheme = dynamicDarkColorScheme(context.androidContext)
            val light = customThemeName == "material_you_light"
            val surfaceVariant = (if (light) colorScheme.surfaceVariant else colorScheme.onSurfaceVariant).toArgb()
            val background = (if (light) colorScheme.onBackground else colorScheme.background).toArgb()

            currentTheme = parseAttributeList(
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
            )
        }

        if (customThemeName == "amoled_dark_mode") {
            currentTheme = parseAttributeList(
                "sigColorTextPrimary" to 0xFFFFFFFF,
                "sigColorChatChat" to 0xFFFFFFFF,
                "sigColorChatPendingSending" to 0xFFFFFFFF,
                "sigColorChatSnapWithSound" to 0xFFFFFFFF,
                "sigColorChatSnapWithoutSound" to 0xFFFFFFFF,
                "sigColorBackgroundMain" to 0xFF000000,
                "sigColorBackgroundSurface" to 0xFF000000,
                "listDivider" to 0xFF000000,
                "actionSheetBackgroundDrawable" to 0xFF000000,
                "actionSheetRoundedBackgroundDrawable" to 0xFF000000,
                "sigExceptionColorCameraGridLines" to 0xFF000000,
            )
        }

        if (customThemeName == "custom") {
            val availableThemes = context.fileHandlerManager.getFileHandle(FileHandleScope.THEME.key, "")?.open(ParcelFileDescriptor.MODE_READ_ONLY)?.use { pfd ->
                AutoCloseInputStream(pfd).use { it.readBytes() }
            }?.let {
                context.gson.fromJson(it.toString(Charsets.UTF_8), object: TypeToken<List<DatabaseThemeContent>>() {})
            } ?: run {
                context.log.verbose("no custom themes found")
                return
            }

            val customThemeColors = mutableMapOf<Int, Int>()

            context.log.verbose("loading ${availableThemes.size} custom themes")

            availableThemes.forEach { themeContent ->
                themeContent.colors.forEach colors@{ colorEntry ->
                    customThemeColors[getAttribute(colorEntry.key).takeIf { it != 0 }.also {
                        if (it == null) {
                            context.log.warn("unknown color attribute: ${colorEntry.key}")
                        }
                    } ?: return@colors] = colorEntry.value
                }
            }

            currentTheme = customThemeColors

            context.log.verbose("loaded ${customThemeColors.size} custom theme colors")
        }

        onNextActivityCreate {
            if (currentTheme.isEmpty()) return@onNextActivityCreate

            context.androidContext.theme.javaClass.getMethod("obtainStyledAttributes", IntArray::class.java).hook(
                HookStage.AFTER) { param ->
                val array = param.arg<IntArray>(0)
                val customColor = (currentTheme[array[0]] as? Number)?.toInt() ?: return@hook

                val result = param.getResult() as TypedArray
                val typedArrayData = result.getObjectField("mData") as IntArray

                when (val attributeType = result.getType(0)) {
                    TypedValue.TYPE_INT_COLOR_ARGB8, TypedValue.TYPE_INT_COLOR_RGB8, TypedValue.TYPE_INT_COLOR_ARGB4, TypedValue.TYPE_INT_COLOR_RGB4 -> {
                        typedArrayData[1] = customColor // index + STYLE_DATA
                    }
                    TypedValue.TYPE_STRING -> {
                        val stringValue = result.getString(0)
                        if (stringValue?.endsWith(".xml") == true) {
                            typedArrayData[0] = TypedValue.TYPE_INT_COLOR_ARGB4 // STYLE_TYPE
                            typedArrayData[1] = customColor // STYLE_DATA
                            typedArrayData[5] = 0; // STYLE_DENSITY
                        }
                    }
                    else -> context.log.warn("unknown attribute type: ${attributeType.toString(16)}")
                }
            }
        }
    }
}