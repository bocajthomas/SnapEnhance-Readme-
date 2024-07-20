package me.rhunk.snapenhance

import android.annotation.SuppressLint
import android.os.Build
import me.rhunk.snapenhance.common.bridge.InternalFileHandleType
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

class RemoteSharedLibraryManager(
    private val remoteSideContext: RemoteSideContext
) {
    private val okHttpClient = OkHttpClient()

    private fun getVersion(): String? {
        return runCatching {
            okHttpClient.newCall(
                Request.Builder()
                    .url("https://raw.githubusercontent.com/SnapEnhance/resources/main/sif/version")
                    .build()
            ).execute().use { response ->
                if (!response.isSuccessful) {
                    return null
                }
                response.body.string()
            }
        }.getOrNull()
    }

    private fun downloadLatest(outputFile: File): Boolean {
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: return false
        val request = Request.Builder()
            .url("https://raw.githubusercontent.com/SnapEnhance/resources/main/sif/$abi.so")
            .build()
        runCatching {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return false
                }
                response.body.byteStream().use { input ->
                    outputFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                return true
            }
        }.onFailure {
            remoteSideContext.log.error("Failed to download latest sif", it)
        }
        return false
    }

    @SuppressLint("ApplySharedPref")
    fun init() {
        val libraryFile = InternalFileHandleType.SIF.resolve(remoteSideContext.androidContext)
        val currentVersion = remoteSideContext.sharedPreferences.getString("sif", null)?.trim()
        if (currentVersion == null || currentVersion == "false") {
            libraryFile.takeIf { it.exists() }?.delete()
            remoteSideContext.log.info("sif can't be loaded due to user preference")
            return
        }
        val latestVersion = getVersion()?.trim() ?: run {
            remoteSideContext.log.warn("Failed to get latest sif version")
            return
        }

        if (currentVersion == latestVersion) {
            remoteSideContext.log.info("sif is up to date ($currentVersion)")
            return
        }

        remoteSideContext.log.info("Updating sif from $currentVersion to $latestVersion")
        if (downloadLatest(libraryFile)) {
            remoteSideContext.sharedPreferences.edit().putString("sif", latestVersion).commit()
            remoteSideContext.shortToast("SIF updated to $latestVersion!")
            // force restart snapchat
            runCatching {
                remoteSideContext.config.configStateListener?.takeIf { it.asBinder().pingBinder() }?.onRestartRequired()
            }
        } else {
            remoteSideContext.log.warn("Failed to download latest sif")
        }
    }
}