package me.rhunk.snapenhance.core.features.impl.global

import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hook
import me.rhunk.snapenhance.core.util.hook.hookConstructor
import me.rhunk.snapenhance.core.util.ktx.getObjectField
import me.rhunk.snapenhance.core.util.ktx.setObjectField
import me.rhunk.snapenhance.mapper.impl.PlusSubscriptionMapper

class SnapchatPlus: Feature("SnapchatPlus") {
    private val originalSubscriptionTime = (System.currentTimeMillis() - 7776000000L)
    private val expirationTimeMillis = (System.currentTimeMillis() + 15552000000L)

    override fun init() {
        if (!context.config.global.snapchatPlus.get()) return

        context.mappings.useMapper(PlusSubscriptionMapper::class) {
            classReference.get()?.hookConstructor(HookStage.AFTER) { param ->
                val instance = param.thisObject<Any>()
                val tier = instance.getObjectField(tierField.getAsString()!!)
                if (tier == 2) return@hookConstructor
                //subscription tier
                instance.setObjectField(tierField.getAsString()!!, 2)
                //subscription status
                instance.setObjectField(statusField.getAsString()!!, 2)

                instance.setObjectField(originalSubscriptionTimeMillisField.getAsString()!!, originalSubscriptionTime)
                instance.setObjectField(expirationTimeMillisField.getAsString()!!, expirationTimeMillis)
            }
        }

        // optional as ConfigurationOverride does this too
        if (context.config.experimental.hiddenSnapchatPlusFeatures.get()) {
            findClass("com.snap.plus.FeatureCatalog").methods.last {
                !it.name.contains("init") &&
                it.parameterTypes.isNotEmpty() &&
                it.parameterTypes[0].name != "java.lang.Boolean"
            }.hook(HookStage.BEFORE) { param ->
                val instance = param.thisObject<Any>()
                val firstArg = param.arg<Any>(0)

                instance::class.java.declaredFields.filter { it.type == firstArg::class.java }.forEach {
                    it.isAccessible = true
                    it.set(instance, firstArg)
                }
            }
        }
    }
}