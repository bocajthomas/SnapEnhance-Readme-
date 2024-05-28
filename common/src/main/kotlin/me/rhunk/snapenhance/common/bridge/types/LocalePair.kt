package me.rhunk.snapenhance.common.bridge.types

import android.os.ParcelFileDescriptor
import java.util.Locale

data class LocalePair(
    val locale: String,
    val content: ParcelFileDescriptor
) {
    fun getLocale(): Locale {
        if (locale.contains("_")) {
            val split = locale.split("_")
            return Locale(split[0], split[1])
        }
        return Locale(locale)
    }
}