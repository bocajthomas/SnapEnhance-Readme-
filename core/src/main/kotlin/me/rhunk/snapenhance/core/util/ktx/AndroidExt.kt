package me.rhunk.snapenhance.core.util.ktx

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.content.res.Resources.Theme
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.graphics.ColorUtils
import me.rhunk.snapenhance.common.Constants


@SuppressLint("DiscouragedApi")
fun Resources.getIdentifier(name: String, type: String): Int {
    return getIdentifier(name, type, Constants.SNAPCHAT_PACKAGE_NAME)
}

fun Resources.getId(name: String): Int {
    return getIdentifier(name, "id")
}

fun Resources.getLayoutId(name: String): Int {
    return getIdentifier(name, "layout")
}

fun Resources.getDimens(name: String): Int {
    return getDimensionPixelSize(getIdentifier(name, "dimen").takeIf { it > 0 } ?: return 0)
}

fun Resources.getDimensFloat(name: String): Float {
    return getDimension(getIdentifier(name, "dimen").takeIf { it > 0 } ?: return 0F)
}

fun Resources.getStyledAttributes(name: String, theme: Theme): TypedArray {
    return getIdentifier(name, "attr").let {
        theme.obtainStyledAttributes(intArrayOf(it))
    }
}

fun Resources.getDrawable(name: String, theme: Theme): Drawable {
    return getDrawable(getIdentifier(name, "drawable"), theme)
}

@SuppressLint("MissingPermission")
fun Context.vibrateLongPress() {
    getSystemService(Vibrator::class.java).vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
}

@SuppressLint("DiscouragedApi")
fun Context.isDarkTheme(): Boolean {
    return theme.obtainStyledAttributes(
        intArrayOf(resources.getIdentifier("sigColorTextPrimary", "attr", packageName))
    ).getColor(0, 0).let {
        ColorUtils.calculateLuminance(it) > 0.5
    }
}