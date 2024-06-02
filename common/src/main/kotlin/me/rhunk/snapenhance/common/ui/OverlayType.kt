package me.rhunk.snapenhance.common.ui

enum class OverlayType(
    val key: String
) {
    SETTINGS("settings"),
    BETTER_LOCATION("better_location");

    companion object {
        fun fromKey(key: String): OverlayType? {
            return entries.find { it.key == key }
        }
    }
}