package me.rhunk.snapenhance.core.features.impl.experiments

import android.annotation.SuppressLint
import android.content.res.TypedArray
import android.graphics.drawable.ColorDrawable
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.features.FeatureLoadParams
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.Hooker
import me.rhunk.snapenhance.core.util.hook.hook
import me.rhunk.snapenhance.core.util.ktx.getIdentifier

class CustomizeUi: Feature("Customize_Ui", loadParams = FeatureLoadParams.ACTIVITY_CREATE_SYNC) {
    @SuppressLint("DiscouragedApi")
    override fun onActivityCreate() {
        if (context.config.userInterface.customizeUi.globalState != true) return
        
        val backgroundColour by context.config.userInterface.customizeUi.backgroundColour.also {
            if (it.getNullable()?.isEmpty() != false) return
        }

        val textColour by context.config.userInterface.customizeUi.textColour.also {
            if (it.getNullable()?.isEmpty() != false) return
        }

        val drawablebackgroundColour by context.config.userInterface.customizeUi.drawablebackgroundColour.also {
            if (it.getNullable()?.isEmpty() != false) return
        }

        
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

            when (array[0]) {
                getAttribute("sigColorTextPrimary") -> {
                    ephemeralHook("getColor", get("textColour").toInt())
                }
                getAttribute("sigColorBackgroundMain"),
                getAttribute("sigColorBackgroundSurface") -> {
                    ephemeralHook("getColor", get("backgroundColour").toInt())
                }
                getAttribute("actionSheetBackgroundDrawable"),
                getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                    ephemeralHook("getDrawable", ColorDrawable(get("drawablebackgroundColour").toInt()))
                }
            }
        }
    }
}
