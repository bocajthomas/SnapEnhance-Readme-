package me.rhunk.snapenhance.core.features.impl.ui

import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.features.FeatureLoadParams
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.Hooker
import me.rhunk.snapenhance.core.util.hook.hook
import me.rhunk.snapenhance.core.util.ktx.getIdentifier

class CustomizeUI: Feature("Customize UI", loadParams = FeatureLoadParams.ACTIVITY_CREATE_SYNC) {
    private fun parseColor(color: String): Int? {
        return color.takeIf { it.isNotEmpty() }?.let {
            runCatching { Color.parseColor(color) }.getOrNull()
        }
    }

    override fun onActivityCreate() {
        
        //TODO: color picker
        val customizeUIConfig = context.config.userInterface.customizeUi
        val colorsConfig = context.config.userInterface.customizeUi.colors
        val themePicker = customizeUIConfig.themePicker.getNullable() ?: return
        val effectiveTextColor by lazy { parseColor(colorsConfig.textColor.get()) }
        val effectivesendAndReceivedTextColor by lazy { parseColor(colorsConfig.sendAndReceivedTextColor.get()) }
        val effectiveBackgroundColor by lazy { parseColor(colorsConfig.backgroundColor.get()) }
        val effectiveBackgroundColorSurface by lazy { parseColor(colorsConfig.backgroundColorSurface.get()) }
        val effectiveActionMenuBackgroundColor by lazy { parseColor(colorsConfig.actionMenuBackgroundColor.get()) }
        val effectiveActionMenuRoundBackgroundColor by lazy { parseColor(colorsConfig.actionMenuRoundBackgroundColor.get()) }
        val effectiveCameraGridLines by lazy { parseColor(colorsConfig.cameraGridLines.get()) }
        
        val attributeCache = mutableMapOf<String, Int>()

        fun getAttribute(name: String): Int {
            if (attributeCache.containsKey(name)) return attributeCache[name]!!
            return context.resources.getIdentifier(name, "attr").also { attributeCache[name] = it }
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
            if (themePicker == "amoled_dark_mode") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFFFFFFFF.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFF000000.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFF000000.toInt()))
                    }
                }
            }
                
            if (themePicker == "modern_minimalism") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFF333333.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFF5F5F5.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFF5F5F5.toInt()))
                    }
                    getAttribute("sigColorChatActivity"),
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFF333333.toInt())
                    }
                }
            }
            
            if (themePicker == "serene_nature") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFF2E8B57.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFFFFDE7.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFFFFDE7.toInt()))
                    }
                    getAttribute("sigColorChatActivity"),
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFF2E8B57.toInt())
                    }
                }
            }

            if (themePicker == "energetic_pop") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                         ephemeralHook("getColor", 0xFFFFDD00.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFF212121.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFF212121.toInt()))
                    }
                    getAttribute("sigColorChatActivity"),
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFFFFDD00.toInt())
                    }
                }
            }

            if (themePicker == "luxurious_night") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFFFDFCCC.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFF303030.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFF303030.toInt()))
                    }
                    getAttribute("sigColorChatActivity"),
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFFFDFCCC.toInt())
                    }
                }
            }

            if (themePicker == "playful_candy") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFFF473B9.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFC2F0F0.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFC2F0F0.toInt()))
                    }
                    getAttribute("sigColorChatActivity"),
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFFF473B9.toInt())
                    }
                }
            }

            if (themePicker == "retro_arcade") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFFFFF9C4.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFF00008B.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFF00008B.toInt()))
                    }
                    getAttribute("sigColorChatActivity"),
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFFFFF9C4.toInt())
                    }
                }
            }

            if (themePicker == "rustic_country") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFF77332E.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFEAEAEA.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFEAEAEA.toInt()))
                    }
                    getAttribute("sigColorChatActivity"),
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFF77332E.toInt())
                    }
                }
            }

            if (themePicker == "ocean_breeze") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFF38B3DE.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFE5E5E5.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFE5E5E5.toInt()))
                    }
                    getAttribute("sigColorChatActivity"),
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFF38B3DE.toInt())
                    }
                }
            }

            if (themePicker == "sunset_glow") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFF293145.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFF0E68C.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFF0E68C.toInt()))
                    }
                    getAttribute("sigColorChatActivity"),
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFF293145.toInt())
                    }
                }
            }

            if (themePicker == "space_adventure") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFFA5BFF7.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFF222222.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFF222222.toInt()))
                    }
                    getAttribute("sigColorChatActivity"),
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFFA5BFF7.toInt())
                    }
                }
            }
            // Credit To @Gabe_does_tech (Telegram) For This Theme 
            if (themePicker == "light_blue") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFF03BAFC.toInt())
                    }
                    getAttribute("sigColorBackgroundMain") -> {
                        ephemeralHook("getColor", 0xFFBDE6FF.toInt())
                    }
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFF78DBFF.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFF78DBFF.toInt()))
                    }
                    getAttribute("sigColorChatActivity"),
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFF08D6FF.toInt())
                    }
                }
            }    
            
            if (themePicker == "custom") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", effectiveTextColor ?: return@hook)
                    }
                        
                    getAttribute("sigColorBackgroundMain") -> {
                        ephemeralHook("getColor", effectiveBackgroundColor ?: return@hook)
                    }
                        
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", effectiveBackgroundColorSurface ?: return@hook)
                    }
                        
                    getAttribute("actionSheetBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(effectiveActionMenuBackgroundColor ?: return@hook))
                    }
                        
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(effectiveActionMenuRoundBackgroundColor ?: return@hook))
                    }
                        
                    getAttribute("sigColorChatActivity") -> {
                        ephemeralHook("getColor", effectivesendAndReceivedTextColor ?: return@hook)
                    }
                        
                    getAttribute("sigColorChatChat") -> {
                        ephemeralHook("getColor", effectivesendAndReceivedTextColor ?: return@hook)
                        
                    }
                        
                    getAttribute("sigColorChatPendingSending") -> {
                        ephemeralHook("getColor", effectivesendAndReceivedTextColor ?: return@hook)
                        
                    }
                        
                    getAttribute("sigColorChatSnapWithSound") -> {
                            ephemeralHook("getColor", effectivesendAndReceivedTextColor ?: return@hook)
                        
                    }
                        
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", effectivesendAndReceivedTextColor ?: return@hook)
                    }

                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", effectiveCameraGridLines ?: return@hook)
                    }
                }
            }
        }
    }
}
