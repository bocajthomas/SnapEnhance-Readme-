package me.rhunk.snapenhance.storage

import android.content.ContentValues
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import me.rhunk.snapenhance.common.util.ktx.getStringOrNull


fun AppDatabase.getRepositories(): List<String> {
    return runBlocking(executor.asCoroutineDispatcher()) {
        database.rawQuery("SELECT url FROM repositories", null).use { cursor ->
            val repos = mutableListOf<String>()
            while (cursor.moveToNext()) {
                repos.add(cursor.getStringOrNull("url") ?: continue)
            }
            repos
        }
    }
}

fun AppDatabase.removeRepo(url: String) {
    runBlocking(executor.asCoroutineDispatcher()) {
        database.delete("repositories", "url = ?", arrayOf(url))
    }
}

fun AppDatabase.addRepo(url: String) {
    runBlocking(executor.asCoroutineDispatcher()) {
        database.insert("repositories", null, ContentValues().apply {
            put("url", url)
        })
    }
}

