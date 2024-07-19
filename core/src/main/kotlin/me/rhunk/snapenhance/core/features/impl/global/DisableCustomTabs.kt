package me.rhunk.snapenhance.core.features.impl.global

import android.content.Intent
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hook

class DisableCustomTabs: Feature("Disable Custom Tabs") {
    override fun init() {
        if (!context.config.global.disableCustomTabs.get()) return
        onNextActivityCreate { activity ->
            activity.packageManager.javaClass.hook("resolveService", HookStage.BEFORE) { param ->
                val intent = param.arg<Intent>(0)
                if (intent.action == "android.support.customtabs.action.CustomTabsService") {
                    param.setResult(null)
                }
            }
        }
    }
}