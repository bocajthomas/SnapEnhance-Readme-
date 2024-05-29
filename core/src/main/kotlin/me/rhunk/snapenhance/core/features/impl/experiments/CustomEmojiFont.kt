package me.rhunk.snapenhance.core.features.impl.experiments

import me.rhunk.snapenhance.common.bridge.FileHandleScope
import me.rhunk.snapenhance.core.ModContext
import me.rhunk.snapenhance.core.util.ktx.getFileHandleLocalPath


private var cacheFontPath: String? = null

fun getCustomEmojiFontPath(
    context: ModContext
): String? {
    val customFileName = context.config.experimental.nativeHooks.customEmojiFont.getNullable() ?: return null
    if (cacheFontPath == null) {
        cacheFontPath = runCatching {
             context.bridgeClient.getFileHandlerManager().getFileHandleLocalPath(
                context,
                FileHandleScope.USER_IMPORT,
                customFileName,
                "custom_emoji_font"
            )
        }.onFailure {
            context.log.error("Failed to get custom emoji font", it)
        }.getOrNull() ?: ""
    }
    return cacheFontPath?.takeIf { it.isNotEmpty() }
}


