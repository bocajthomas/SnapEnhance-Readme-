package me.rhunk.snapenhance.storage

import me.rhunk.snapenhance.common.util.ktx.getStringOrNull


fun AppDatabase.getQuickTiles(): List<String> {
    return database.rawQuery("SELECT `key` FROM quick_tiles ORDER BY position ASC", null).use { cursor ->
        val keys = mutableListOf<String>()
        while (cursor.moveToNext()) {
            keys.add(cursor.getStringOrNull("key") ?: continue)
        }
        keys
    }
}

fun AppDatabase.setQuickTiles(keys: List<String>) {
    executeAsync {
        database.execSQL("DELETE FROM quick_tiles")
        keys.forEachIndexed { index, key ->
            database.execSQL("INSERT INTO quick_tiles (`key`, position) VALUES (?, ?)", arrayOf(
                key,
                index
            ))
        }
    }
}