package me.rhunk.snapenhance.core.features.impl.tweaks

import android.media.AudioManager
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hook

class HideActiveMusic: Feature("Hide Active Music") {
    override fun init() {
        if (!context.config.global.hideActiveMusic.get()) return
        onNextActivityCreate {
            AudioManager::class.java.hook("isMusicActive", HookStage.BEFORE) {
                it.setResult(false)
            }
        }
    }
}