package me.rhunk.snapenhance.core.features.impl.ui

import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.features.FeatureLoadParams
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hook
import me.rhunk.snapenhance.core.util.ktx.getObjectField
import me.rhunk.snapenhance.mapper.impl.StreaksExpirationMapper
import kotlin.time.Duration.Companion.milliseconds

class CustomStreaksExpirationFormat: Feature("CustomStreaksExpirationFormat", loadParams = FeatureLoadParams.ACTIVITY_CREATE_SYNC) {
    private fun Long.padZero(): String {
        return this.toString().padStart(2, '0')
    }

    override fun onActivityCreate() {
        val expirationFormat by context.config.experimental.customStreaksExpirationFormat
        if (expirationFormat.isNotEmpty() || context.config.userInterface.streakExpirationInfo.get()) {
            context.mappings.useMapper(StreaksExpirationMapper::class) {
                runCatching {
                    simpleStreaksFormatterClass.getAsClass()?.hook(formatSimpleStreaksTextMethod.get() ?: return@useMapper, HookStage.BEFORE) { param ->
                        param.setResult(null)
                    }
                }.onFailure {
                    context.log.warn("Failed to hook simpleStreaksFormatterClass : " + it.message)
                }
            }
        }
        if (expirationFormat.isEmpty()) return

        context.mappings.useMapper(StreaksExpirationMapper::class) {
            streaksFormatterClass.getAsClass()?.hook(formatStreaksTextMethod.get() ?: return@useMapper, HookStage.AFTER) { param ->
                val streaksCount = param.argNullable(2) ?: 0
                val streaksExpiration = param.argNullable<Any>(3) ?: return@hook

                val hourGlassTimeRemaining = streaksExpiration.getObjectField(hourGlassTimeRemainingField.get() ?: return@hook) as? Long ?: return@hook
                val expirationTime = streaksExpiration.getObjectField(expirationTimeField.get() ?: return@hook) as? Long ?: return@hook
                val delta = (expirationTime - System.currentTimeMillis()).milliseconds

                val hourGlassEmoji = if (delta.inWholeMilliseconds in 1..hourGlassTimeRemaining) if (expirationTime % 2 == 0L) "\u23F3" else "\u231B" else ""

                param.setResult(expirationFormat
                    .replace("%c", streaksCount.toString())
                    .replace("%e", hourGlassEmoji)
                    .replace("%d", delta.inWholeDays.toString())
                    .replace("%h", (delta.inWholeHours % 24).padZero())
                    .replace("%m", (delta.inWholeMinutes % 60).padZero())
                    .replace("%s", (delta.inWholeSeconds % 60).padZero())
                    .replace("%w", delta.toString())
                )
            }
        }
    }
}