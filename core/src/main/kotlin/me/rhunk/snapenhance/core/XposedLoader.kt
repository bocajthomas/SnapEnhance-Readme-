package me.rhunk.snapenhance.core

import android.app.Application
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.rhunk.snapenhance.common.BuildConfig
import me.rhunk.snapenhance.common.Constants
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hook

class XposedLoader : IXposedHookLoadPackage {
    override fun handleLoadPackage(p0: XC_LoadPackage.LoadPackageParam) {
        if (p0.packageName != Constants.SNAPCHAT_PACKAGE_NAME) return
        // prevent loading in sub-processes
        if (p0.processName.contains(":")) return
        XposedBridge.log("Loading SE Extended v${BuildConfig.VERSION_NAME}#${BuildConfig.GIT_HASH} (package: ${BuildConfig.APPLICATION_ID})")
        Application::class.java.hook("attach", HookStage.BEFORE) { param ->
            SnapEnhance().init(param.arg(0))
        }
    }
}