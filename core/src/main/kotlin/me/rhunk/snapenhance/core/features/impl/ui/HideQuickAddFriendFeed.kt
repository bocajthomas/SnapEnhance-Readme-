package me.rhunk.snapenhance.core.features.impl.ui

import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hookConstructor
import me.rhunk.snapenhance.core.util.ktx.setObjectField
import me.rhunk.snapenhance.mapper.impl.FriendingDataSourcesMapper

class HideQuickAddFriendFeed : Feature("HideQuickAddFriendFeed") {
    override fun init() {
        if (!context.config.userInterface.hideQuickAddFriendFeed.get()) return

        onNextActivityCreate {
            context.mappings.useMapper(FriendingDataSourcesMapper::class) {
                classReference.getAsClass()?.hookConstructor(HookStage.AFTER) { param ->
                    param.thisObject<Any>().setObjectField(
                        quickAddSourceListField.get()!!,
                        arrayListOf<Any>()
                    )
                }
            }
        }
    }
}