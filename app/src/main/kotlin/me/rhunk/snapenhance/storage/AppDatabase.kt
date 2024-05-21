package me.rhunk.snapenhance.storage

import android.database.sqlite.SQLiteDatabase
import me.rhunk.snapenhance.RemoteSideContext
import me.rhunk.snapenhance.common.data.MessagingFriendInfo
import me.rhunk.snapenhance.common.data.MessagingGroupInfo
import me.rhunk.snapenhance.common.util.SQLiteDatabaseHelper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class AppDatabase(
    val context: RemoteSideContext,
) {
    val executor: ExecutorService = Executors.newSingleThreadExecutor()
    lateinit var database: SQLiteDatabase

    var receiveMessagingDataCallback: (friends: List<MessagingFriendInfo>, groups: List<MessagingGroupInfo>) -> Unit = { _, _ -> }

    fun executeAsync(block: () -> Unit) {
        executor.execute {
            runCatching {
                block()
            }.onFailure {
                context.log.error("Failed to execute async block", it)
            }
        }
    }

    fun init() {
        database = context.androidContext.openOrCreateDatabase("main.db", 0, null)
        SQLiteDatabaseHelper.createTablesFromSchema(database, mapOf(
            "friends" to listOf(
                "id INTEGER PRIMARY KEY AUTOINCREMENT",
                "userId CHAR(36) UNIQUE",
                "dmConversationId VARCHAR(36)",
                "displayName VARCHAR",
                "mutableUsername VARCHAR",
                "bitmojiId VARCHAR",
                "selfieId VARCHAR"
            ),
            "groups" to listOf(
                "id INTEGER PRIMARY KEY AUTOINCREMENT",
                "conversationId CHAR(36) UNIQUE",
                "name VARCHAR",
                "participantsCount INTEGER"
            ),
            "rules" to listOf(
                "id INTEGER PRIMARY KEY AUTOINCREMENT",
                "type VARCHAR",
                "targetUuid VARCHAR"
            ),
            "streaks" to listOf(
                "id VARCHAR PRIMARY KEY",
                "notify BOOLEAN",
                "expirationTimestamp BIGINT",
                "length INTEGER"
            ),
            "scripts" to listOf(
                "id INTEGER PRIMARY KEY AUTOINCREMENT",
                "name VARCHAR NOT NULL",
                "version VARCHAR NOT NULL",
                "displayName VARCHAR",
                "description VARCHAR",
                "author VARCHAR NOT NULL",
                "enabled BOOLEAN"
            ),
            "tracker_rules" to listOf(
                "id INTEGER PRIMARY KEY AUTOINCREMENT",
                "enabled BOOLEAN DEFAULT 1",
                "name VARCHAR",
            ),
            "tracker_scopes" to listOf(
                "id INTEGER PRIMARY KEY AUTOINCREMENT",
                "rule_id INTEGER",
                "scope_type VARCHAR",
                "scope_id CHAR(36)"
            ),
            "tracker_rules_events" to listOf(
                "id INTEGER PRIMARY KEY AUTOINCREMENT",
                "rule_id INTEGER",
                "flags INTEGER DEFAULT 1",
                "event_type VARCHAR",
                "params TEXT",
                "actions TEXT"
            ),
            "quick_tiles" to listOf(
                "key VARCHAR PRIMARY KEY",
                "position INTEGER",
            )
        ))
    }
}