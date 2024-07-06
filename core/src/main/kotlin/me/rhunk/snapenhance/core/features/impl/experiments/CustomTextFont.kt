package me.rhunk.snapenhance.core.features.impl.experiments

import me.rhunk.snapenhance.common.bridge.FileHandleScope
import me.rhunk.snapenhance.core.ModContext
import me.rhunk.snapenhance.core.util.ktx.getFileHandleLocalPath

private var cacheTextFontPath: String? = null

fun getCustomTextFontPath(
    context: ModContext
): String? {
    val customFileName = context.config.experimental.nativeHooks.customTextFont.getNullable()?.takeIf { it.isNotBlank() } ?: return null
    if (cacheTextFontPath == null) {
        cacheTextFontPath = runCatching {
             context.fileHandlerManager.getFileHandleLocalPath(
                context,
                FileHandleScope.USER_IMPORT,
                customFileName,
                "custom_text_font"
            )
        }.onFailure {
            context.log.error("Failed to get custom text font", it)
        }.getOrNull() ?: ""
    }
    return cacheTextFontPath?.takeIf { it.isNotEmpty() }
}
