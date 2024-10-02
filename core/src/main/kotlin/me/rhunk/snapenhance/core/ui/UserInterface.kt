package me.rhunk.snapenhance.core.ui

import android.content.res.Resources
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.widget.TextView
import me.rhunk.snapenhance.core.ModContext
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hook
import me.rhunk.snapenhance.core.util.ktx.isDarkTheme

class UserInterface(
    private val context: ModContext
) {
    private val fontMap = mutableMapOf<Int, Int>()

    val colorPrimary get() = if (context.androidContext.isDarkTheme()) 0xfff5f5f5.toInt() else 0xff212121.toInt()
    val actionSheetBackground get() = if (context.androidContext.isDarkTheme()) 0xff1e1e1e.toInt() else 0xffffffff.toInt()

    val avenirNextTypeface: Typeface by lazy {
        fontMap[600]?.let { context.resources.getFont(it) } ?: throw IllegalStateException("Avenir Next not loaded")
    }

    fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    @Suppress("unused")
    fun pxToDp(px: Int): Int {
        return (px / context.resources.displayMetrics.density).toInt()
    }

    fun getFontResource(weight: Int): Int? {
        return fontMap[weight]
    }

    fun applyActionButtonTheme(view: TextView) {
        view.apply {
            setTextColor(colorPrimary)
            typeface = avenirNextTypeface
            setShadowLayer(0F, 0F, 0F, 0)
            gravity = Gravity.CENTER_VERTICAL
            isAllCaps = false
            textSize = 16f
            outlineProvider = null
            setPadding(dpToPx(12),  dpToPx(15), 0, dpToPx(15))
            setBackgroundColor(0)
        }
    }

    fun init() {
        Resources::class.java.hook("getValue", HookStage.AFTER) { param ->
            val typedValue = param.arg<TypedValue>(1)
            val path = typedValue.string ?: return@hook
            if (!path.startsWith("res/") || !path.endsWith(".ttf")) return@hook

            val typeface = context.resources.getFont(typedValue.resourceId)
            fontMap.getOrPut(typeface.weight) { typedValue.resourceId }
        }
    }
}