package me.rhunk.snapenhance.storage

import androidx.core.database.getStringOrNull

fun AppDatabase.getScopeNotes(id: String): String? {
    return database.rawQuery("SELECT content FROM notes WHERE id = ?", arrayOf(id)).use {
        if (it.moveToNext()) {
            it.getStringOrNull(0)
        } else {
            null
        }
    }
}

fun AppDatabase.setScopeNotes(id: String, content: String?) {
    if (content == null || content.isEmpty() == true) {
        executeAsync {
            database.execSQL("DELETE FROM notes WHERE id = ?", arrayOf(id))
        }
        return
    }

    executeAsync {
        database.execSQL("INSERT OR REPLACE INTO notes (id, content) VALUES (?, ?)", arrayOf(id, content))
    }
}

fun AppDatabase.deleteScopeNotes(id: String) {
    executeAsync {
        database.execSQL("DELETE FROM notes WHERE id = ?", arrayOf(id))
    }
}
