package me.rhunk.snapenhance.storage

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import me.rhunk.snapenhance.common.scripting.type.ModuleInfo
import me.rhunk.snapenhance.common.util.ktx.getInteger
import me.rhunk.snapenhance.common.util.ktx.getStringOrNull


fun AppDatabase.getScripts(): List<ModuleInfo> {
    return database.rawQuery("SELECT * FROM scripts ORDER BY id DESC", null).use { cursor ->
        val scripts = mutableListOf<ModuleInfo>()
        while (cursor.moveToNext()) {
            scripts.add(
                ModuleInfo(
                    name = cursor.getStringOrNull("name")!!,
                    version = cursor.getStringOrNull("version")!!,
                    displayName = cursor.getStringOrNull("displayName"),
                    description = cursor.getStringOrNull("description"),
                    author = cursor.getStringOrNull("author"),
                    grantedPermissions = emptyList()
                )
            )
        }
        scripts
    }
}

fun AppDatabase.setScriptEnabled(name: String, enabled: Boolean) {
    executeAsync {
        database.execSQL("UPDATE scripts SET enabled = ? WHERE name = ?", arrayOf(
            if (enabled) 1 else 0,
            name
        ))
    }
}

fun AppDatabase.isScriptEnabled(name: String): Boolean {
    return database.rawQuery("SELECT enabled FROM scripts WHERE name = ?", arrayOf(name)).use { cursor ->
        if (!cursor.moveToFirst()) return@use false
        cursor.getInteger("enabled") == 1
    }
}

fun AppDatabase.syncScripts(availableScripts: List<ModuleInfo>) {
    runBlocking(executor.asCoroutineDispatcher()) {
        val enabledScripts = getScripts()
        val enabledScriptPaths = enabledScripts.map { it.name }
        val availableScriptPaths = availableScripts.map { it.name }

        enabledScripts.forEach { script ->
            if (!availableScriptPaths.contains(script.name)) {
                database.execSQL("DELETE FROM scripts WHERE name = ?", arrayOf(script.name))
            }
        }

        availableScripts.forEach { script ->
            if (!enabledScriptPaths.contains(script.name) || script != enabledScripts.find { it.name == script.name }) {
                database.execSQL(
                    "INSERT OR REPLACE INTO scripts (name, version, displayName, description, author, enabled) VALUES (?, ?, ?, ?, ?, ?)",
                    arrayOf(
                        script.name,
                        script.version,
                        script.displayName,
                        script.description,
                        script.author,
                        0
                    )
                )
            }
        }
    }
}