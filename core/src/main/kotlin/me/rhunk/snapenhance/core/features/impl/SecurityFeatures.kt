package me.rhunk.snapenhance.core.features.impl

import android.annotation.SuppressLint
import android.system.Os
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotInterested
import me.rhunk.snapenhance.common.config.MOD_DETECTION_VERSION_CHECK
import me.rhunk.snapenhance.common.config.VersionRequirement
import me.rhunk.snapenhance.core.features.Feature

class SecurityFeatures : Feature("Security Features") {
    private fun transact(option: Int, option2: Long) = runCatching { Os.prctl(option, option2, 0, 0, 0) }.getOrNull()

    private val token by lazy {
        transact(0, 0)
    }

    private fun getStatus() = token?.run {
        transact(this, 0)?.toString(2)?.padStart(32, '0')?.count { it == '1' }
    }

    @SuppressLint("SetTextI18n")
    override fun init() {
        token // pre init token

        val status = getStatus()
        val canCheckVersion = context.bridgeClient.getDebugProp("disable_mod_detection_version_check", "false") != "true"
        val snapchatVersionCode = context.androidContext.packageManager.getPackageInfo(context.androidContext.packageName, 0).longVersionCode

        if (status == null || status == 0) {
            context.log.verbose("SIF has not been loaded")
        } else {
            context.log.verbose("SIF = $status")
        }

        if (canCheckVersion && MOD_DETECTION_VERSION_CHECK.checkVersion(snapchatVersionCode)?.second == VersionRequirement.OLDER_REQUIRED && (status == null || status < 2)) {
            onNextActivityCreate {
                context.inAppOverlay.showStatusToast(
                    icon = Icons.Filled.NotInterested,
                    text = "SE Extended is not compatible with this version of Snapchat and will result in a ban.\nUse Snapchat ${MOD_DETECTION_VERSION_CHECK.maxVersion?.first ?: "0.0.0"} or older to avoid detections or use test accounts.",
                    durationMs = 10000,
                    maxLines = 6
                )
            }
        }
    }
}