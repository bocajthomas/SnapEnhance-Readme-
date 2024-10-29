package me.rhunk.snapenhance.core.features.impl.ui

import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hook

class HideQuickAddSuggestions : Feature("Hide Quick Add Suggestions") {
    override fun init() {
        if (!context.config.userInterface.hideQuickAddSuggestions.get()) return

        context.androidContext.classLoader.loadClass("io.requery.android.database.sqlite.SQLiteDatabase")
            .hook("rawQueryWithFactory", HookStage.BEFORE) { param ->
                var sqlRequest = param.arg<String>(1)

                fun patchRequest() {
                    sqlRequest.lastIndexOf("WHERE").takeIf { it != -1 }?.let {
                        sqlRequest = sqlRequest.substring(0, it + 5) + " 0 = 1 AND " + sqlRequest.substring(it + 5)
                        param.setArg(1, sqlRequest)
                    }
                }

                if (sqlRequest.contains("SuggestedFriendPlacement")) {
                    patchRequest()
                }
            }
    }
}