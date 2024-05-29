package me.rhunk.snapenhance.core.util.ktx

import android.os.Build
import android.os.ParcelFileDescriptor
import me.rhunk.snapenhance.bridge.storage.FileHandleManager
import me.rhunk.snapenhance.common.bridge.FileHandleScope
import me.rhunk.snapenhance.common.util.ktx.longHashCode
import me.rhunk.snapenhance.core.ModContext
import java.io.FileOutputStream
import kotlin.math.absoluteValue

fun FileHandleManager.getFileHandleLocalPath(
    context: ModContext,
    scope: FileHandleScope,
    name: String,
    fileUniqueIdentifier: String,
): String? {
    return getFileHandle(scope.key, name)?.open(ParcelFileDescriptor.MODE_READ_ONLY)?.use { pfd ->
        val cacheFile = context.androidContext.cacheDir.resolve((fileUniqueIdentifier + Build.FINGERPRINT).longHashCode().absoluteValue.toString(16))
        if (!cacheFile.exists() || pfd.statSize != cacheFile.length()) {
            FileOutputStream(cacheFile).use { output ->
                ParcelFileDescriptor.AutoCloseInputStream(pfd).use { input ->
                    input.copyTo(output)
                }
            }
        }
        cacheFile.absolutePath
    }
}