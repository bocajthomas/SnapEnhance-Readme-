package me.rhunk.snapenhance.core.features.impl

import android.system.Os
import me.rhunk.snapenhance.core.features.Feature

class SecurityFeatures : Feature("Security Features") {
    private fun transact(option: Int, option2: Long) = kotlin.runCatching { Os.prctl(option, option2, 0, 0, 0) }.getOrNull()

    private val token by lazy {
        transact(0, 0)
    }

    private fun getStatus() = token?.run {
        transact(this, 0)?.toString(2)?.padStart(32, '0')?.count { it == '1' }
    }

    override fun init() {
        token // pre init token

        val status = getStatus()
        if (status == null || status == 0) {
            context.log.verbose("SIF has not been loaded")
        } else {
            context.log.verbose("SIF = $status")
        }
    }
}