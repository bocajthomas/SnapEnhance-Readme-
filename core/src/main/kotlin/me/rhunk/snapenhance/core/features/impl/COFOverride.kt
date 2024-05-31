package me.rhunk.snapenhance.core.features.impl

import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.features.FeatureLoadParams
import me.rhunk.snapenhance.core.util.dataBuilder
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hook
import me.rhunk.snapenhance.core.util.ktx.getObjectField
import me.rhunk.snapenhance.mapper.impl.COFObservableMapper
import java.lang.reflect.Method

class COFOverride : Feature("COF Override", loadParams = FeatureLoadParams.INIT_SYNC) {
    var hasActionMenuV2 = false

    override fun init() {
        val cofExperiments by context.config.experimental.cofExperiments

        context.mappings.useMapper(COFObservableMapper::class) {
            classReference.getAsClass()?.hook(getBooleanObservable.get() ?: return@useMapper, HookStage.AFTER) { param ->
                val configId = param.arg<String>(0)
                val result by lazy { param.getResult()?.getObjectField("b") }

                fun setBooleanResult(state: Boolean) {
                    param.setResult((param.method() as Method).returnType.dataBuilder {
                        set("a", 4)
                        set("b", state)
                    })
                }

                if (cofExperiments.contains(configId.lowercase())) {
                    setBooleanResult(true)
                }

                if (configId == "ANDROID_ACTION_MENU_V2" && result == true) {
                    hasActionMenuV2 = true
                }
            }
        }
    }
}