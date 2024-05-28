package me.rhunk.snapenhance.nativelib

data class NativeConfig(
    val disableBitmoji: Boolean = false,
    val disableMetrics: Boolean = false,
    val composerHooks: Boolean = false,
    val customEmojiFontPath: String? = null,
)