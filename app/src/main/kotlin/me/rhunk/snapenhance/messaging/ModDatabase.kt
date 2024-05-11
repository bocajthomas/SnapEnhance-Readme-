package me.rhunk.snapenhance.messaging

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.google.gson.JsonArray
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import me.rhunk.snapenhance.RemoteSideContext
import me.rhunk.snapenhance.common.data.*
import me.rhunk.snapenhance.common.scripting.type.ModuleInfo
import me.rhunk.snapenhance.common.util.SQLiteDatabaseHelper
import me.rhunk.snapenhance.common.util.ktx.getInteger
import me.rhunk.snapenhance.common.util.ktx.getLongOrNull
import me.rhunk.snapenhance.common.util.ktx.getStringOrNull
import java.util.concurrent.Executors
import kotlin.coroutines.suspendCoroutine


class ModDatabase(
    private val context: RemoteSideContext,
) {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var database: SQLiteDatabase

    var receiveMessagingDataCallback: (friends: List<Friend>, groups: List<Group>) -> Unit = { _, _ -> }

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

    fun getScripts(): List<ModuleInfo> {
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

    fun setScriptEnabled(name: String, enabled: Boolean) {
        executeAsync {
            database.execSQL("UPDATE scripts SET enabled = ? WHERE name = ?", arrayOf(
                if (enabled) 1 else 0,
                name
            ))
        }
    }

    fun isScriptEnabled(name: String): Boolean {
        return database.rawQuery("SELECT enabled FROM scripts WHERE name = ?", arrayOf(name)).use { cursor ->
            if (!cursor.moveToFirst()) return@use false
            cursor.getInteger("enabled") == 1
        }
    }

    fun syncScripts(availableScripts: List<ModuleInfo>) {
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

    fun clearTrackerRules() {
        runBlocking {
            suspendCoroutine { continuation ->
                executeAsync {
                    database.execSQL("DELETE FROM tracker_rules")
                    database.execSQL("DELETE FROM tracker_rules_events")
                    continuation.resumeWith(Result.success(Unit))
                }
            }
        }
    }

    fun deleteTrackerRule(ruleId: Int) {
        executeAsync {
            database.execSQL("DELETE FROM tracker_rules WHERE id = ?", arrayOf(ruleId))
            database.execSQL("DELETE FROM tracker_rules_events WHERE rule_id = ?", arrayOf(ruleId))
        }
    }

    fun newTrackerRule(name: String = "Custom Rule"): Int {
        return runBlocking {
            suspendCoroutine { continuation ->
                executeAsync {
                    val id = database.insert("tracker_rules", null, ContentValues().apply {
                        put("name", name)
                    })
                    continuation.resumeWith(Result.success(id.toInt()))
                }
            }
        }
    }

    fun addOrUpdateTrackerRuleEvent(
        ruleEventId: Int? = null,
        ruleId: Int? = null,
        eventType: String? = null,
        params: TrackerRuleActionParams,
        actions: List<TrackerRuleAction>
    ): Int? {
        return runBlocking {
            suspendCoroutine { continuation ->
                executeAsync {
                    val id = if (ruleEventId != null) {
                        database.execSQL("UPDATE tracker_rules_events SET params = ?, actions = ? WHERE id = ?", arrayOf(
                            context.gson.toJson(params),
                            context.gson.toJson(actions.map { it.key }),
                            ruleEventId
                        ))
                        ruleEventId
                    } else {
                        database.insert("tracker_rules_events", null, ContentValues().apply {
                            put("rule_id", ruleId)
                            put("event_type", eventType)
                            put("params", context.gson.toJson(params))
                            put("actions", context.gson.toJson(actions.map { it.key }))
                        }).toInt()
                    }
                    continuation.resumeWith(Result.success(id))
                }
            }
        }
    }

    fun deleteTrackerRuleEvent(eventId: Int) {
        executeAsync {
            database.execSQL("DELETE FROM tracker_rules_events WHERE id = ?", arrayOf(eventId))
        }
    }

     fun getTrackerRules(): List<TrackerRule> {
        val rules = mutableListOf<TrackerRule>()

        database.rawQuery("SELECT * FROM tracker_rules", null).use { cursor ->
            while (cursor.moveToNext()) {
                rules.add(
                    TrackerRule(
                        id = cursor.getInteger("id"),
                        name = cursor.getStringOrNull("name") ?: "",
                    )
                )
            }
        }
        
        return rules
    }

    fun getTrackerRule(ruleId: Int): TrackerRule? {
        return database.rawQuery("SELECT * FROM tracker_rules WHERE id = ?", arrayOf(ruleId.toString())).use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            TrackerRule(
                id = cursor.getInteger("id"),
                name = cursor.getStringOrNull("name") ?: "",
            )
        }
    }

    fun setTrackerRuleName(ruleId: Int, name: String) {
        executeAsync {
            database.execSQL("UPDATE tracker_rules SET name = ? WHERE id = ?", arrayOf(name, ruleId))
        }
    }

    fun getTrackerEvents(ruleId: Int): List<TrackerRuleEvent> {
        val events = mutableListOf<TrackerRuleEvent>()
        database.rawQuery("SELECT * FROM tracker_rules_events WHERE rule_id = ?", arrayOf(ruleId.toString())).use { cursor ->
            while (cursor.moveToNext()) {
                events.add(
                    TrackerRuleEvent(
                        id = cursor.getInteger("id"),
                        eventType = cursor.getStringOrNull("event_type") ?: continue,
                        enabled = cursor.getInteger("flags") == 1,
                        params = context.gson.fromJson(cursor.getStringOrNull("params") ?: "{}", TrackerRuleActionParams::class.java),
                        actions = context.gson.fromJson(cursor.getStringOrNull("actions") ?: "[]", JsonArray::class.java).mapNotNull {
                            TrackerRuleAction.fromString(it.asString)
                        }
                    )
                )
            }
        }
        return events
    }

    fun getTrackerEvents(eventType: String): Map<TrackerRuleEvent, TrackerRule> {
        val events = mutableMapOf<TrackerRuleEvent, TrackerRule>()
        database.rawQuery("SELECT tracker_rules_events.id as event_id, tracker_rules_events.params as event_params," +
                "tracker_rules_events.actions, tracker_rules_events.flags, tracker_rules_events.event_type, tracker_rules.name, tracker_rules.id as rule_id " +
                "FROM tracker_rules_events " +
                "INNER JOIN tracker_rules " +
                "ON tracker_rules_events.rule_id = tracker_rules.id " +
                "WHERE event_type = ?", arrayOf(eventType)
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val trackerRule = TrackerRule(
                    id = cursor.getInteger("rule_id"),
                    name = cursor.getStringOrNull("name") ?: "",
                )
                val trackerRuleEvent = TrackerRuleEvent(
                    id = cursor.getInteger("event_id"),
                    eventType = cursor.getStringOrNull("event_type") ?: continue,
                    enabled = cursor.getInteger("flags") == 1,
                    params = context.gson.fromJson(cursor.getStringOrNull("event_params") ?: "{}", TrackerRuleActionParams::class.java),
                    actions = context.gson.fromJson(cursor.getStringOrNull("actions") ?: "[]", JsonArray::class.java).mapNotNull {
                        TrackerRuleAction.fromString(it.asString)
                    }
                )
                events[trackerRuleEvent] = trackerRule
            }
        }
        return events
    }

    fun setRuleTrackerScopes(ruleId: Int, type: TrackerScopeType, scopes: List<String>) {
        executeAsync {
            database.execSQL("DELETE FROM tracker_scopes WHERE rule_id = ?", arrayOf(ruleId))
            scopes.forEach { scopeId ->
                database.execSQL("INSERT INTO tracker_scopes (rule_id, scope_type, scope_id) VALUES (?, ?, ?)", arrayOf(
                    ruleId,
                    type.key,
                    scopeId
                ))
            }
        }
    }

    fun getRuleTrackerScopes(ruleId: Int, limit: Int = Int.MAX_VALUE): Map<String, TrackerScopeType> {
        val scopes = mutableMapOf<String, TrackerScopeType>()
        database.rawQuery("SELECT * FROM tracker_scopes WHERE rule_id = ? LIMIT ?", arrayOf(ruleId.toString(), limit.toString())).use { cursor ->
            while (cursor.moveToNext()) {
                scopes[cursor.getStringOrNull("scope_id") ?: continue] = TrackerScopeType.entries.find { it.key == cursor.getStringOrNull("scope_type") } ?: continue
            }
        }
        return scopes
    }

    fun getQuickTiles(): List<String> {
        return database.rawQuery("SELECT `key` FROM quick_tiles ORDER BY position ASC", null).use { cursor ->
            val keys = mutableListOf<String>()
            while (cursor.moveToNext()) {
                keys.add(cursor.getStringOrNull("key") ?: continue)
            }
            keys
        }
    }

    fun setQuickTiles(keys: List<String>) {
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
}