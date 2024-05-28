package me.rhunk.snapenhance.common.util.ktx

import android.content.ClipData
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.ApplicationInfoFlags
import android.os.Build
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStream

fun PackageManager.getApplicationInfoCompat(packageName: String, flags: Int) =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getApplicationInfo(packageName, ApplicationInfoFlags.of(flags.toLong()))
    } else {
        @Suppress("DEPRECATION")
        getApplicationInfo(packageName, flags)
    }

fun Context.copyToClipboard(data: String, label: String = "Copied Text") {
    getSystemService(android.content.ClipboardManager::class.java).setPrimaryClip(
        ClipData.newPlainText(label, data))
}

fun InputStream.toParcelFileDescriptor(coroutineScope: CoroutineScope): ParcelFileDescriptor {
    val pfd = ParcelFileDescriptor.createPipe()
    val fos = ParcelFileDescriptor.AutoCloseOutputStream(pfd[1])

    coroutineScope.launch(Dispatchers.IO) {
        try {
            copyTo(fos)
        } finally {
            close()
            fos.flush()
            fos.close()
        }
    }

    return pfd[0]
}
