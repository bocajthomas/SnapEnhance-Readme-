package me.rhunk.snapenhance.storage

import me.rhunk.snapenhance.common.data.FriendStreaks
import me.rhunk.snapenhance.common.data.MessagingFriendInfo
import me.rhunk.snapenhance.common.data.MessagingGroupInfo
import me.rhunk.snapenhance.common.data.MessagingRuleType
import me.rhunk.snapenhance.common.util.ktx.getInteger
import me.rhunk.snapenhance.common.util.ktx.getLongOrNull
import me.rhunk.snapenhance.common.util.ktx.getStringOrNull


fun AppDatabase.getGroups(): List<MessagingGroupInfo> {
    return database.rawQuery("SELECT * FROM groups", null).use { cursor ->
        val groups = mutableListOf<MessagingGroupInfo>()
        while (cursor.moveToNext()) {
            groups.add(MessagingGroupInfo.fromCursor(cursor))
        }
        groups
    }
}

fun AppDatabase.getFriends(descOrder: Boolean = false): List<MessagingFriendInfo> {
    return database.rawQuery("SELECT * FROM friends LEFT OUTER JOIN streaks ON friends.userId = streaks.id ORDER BY id ${if (descOrder) "DESC" else "ASC"}", null).use { cursor ->
        val friends = mutableListOf<MessagingFriendInfo>()
        while (cursor.moveToNext()) {
            runCatching {
                friends.add(MessagingFriendInfo.fromCursor(cursor))
            }.onFailure {
                context.log.error("Failed to parse friend", it)
            }
        }
        friends
    }
}


fun AppDatabase.syncGroupInfo(conversationInfo: MessagingGroupInfo) {
    executeAsync {
        try {
            database.execSQL("INSERT OR REPLACE INTO groups (conversationId, name, participantsCount) VALUES (?, ?, ?)", arrayOf(
                conversationInfo.conversationId,
                conversationInfo.name,
                conversationInfo.participantsCount
            ))
        } catch (e: Exception) {
            throw e
        }
    }
}

fun AppDatabase.syncFriend(friend: MessagingFriendInfo) {
    executeAsync {
        try {
            database.execSQL(
                "INSERT OR REPLACE INTO friends (userId, dmConversationId, displayName, mutableUsername, bitmojiId, selfieId) VALUES (?, ?, ?, ?, ?, ?)",
                arrayOf(
                    friend.userId,
                    friend.dmConversationId,
                    friend.displayName,
                    friend.mutableUsername,
                    friend.bitmojiId,
                    friend.selfieId
                )
            )
            //sync streaks
            friend.streaks?.takeIf { it.length > 0 }?.let {
                val streaks = getFriendStreaks(friend.userId)

                database.execSQL("INSERT OR REPLACE INTO streaks (id, notify, expirationTimestamp, length) VALUES (?, ?, ?, ?)", arrayOf(
                    friend.userId,
                    streaks?.notify ?: true,
                    it.expirationTimestamp,
                    it.length
                ))
            } ?: database.execSQL("DELETE FROM streaks WHERE id = ?", arrayOf(friend.userId))
        } catch (e: Exception) {
            throw e
        }
    }
}



fun AppDatabase.getRules(targetUuid: String): List<MessagingRuleType> {
    return database.rawQuery("SELECT type FROM rules WHERE targetUuid = ?", arrayOf(
        targetUuid
    )).use { cursor ->
        val rules = mutableListOf<MessagingRuleType>()
        while (cursor.moveToNext()) {
            runCatching {
                rules.add(MessagingRuleType.getByName(cursor.getStringOrNull("type")!!) ?: return@runCatching)
            }.onFailure {
                context.log.error("Failed to parse rule", it)
            }
        }
        rules
    }
}

fun AppDatabase.setRule(targetUuid: String, type: String, enabled: Boolean) {
    executeAsync {
        if (enabled) {
            database.execSQL("INSERT OR REPLACE INTO rules (targetUuid, type) VALUES (?, ?)", arrayOf(
                targetUuid,
                type
            ))
        } else {
            database.execSQL("DELETE FROM rules WHERE targetUuid = ? AND type = ?", arrayOf(
                targetUuid,
                type
            ))
        }
    }
}

fun AppDatabase.getFriendInfo(userId: String): MessagingFriendInfo? {
    return database.rawQuery("SELECT * FROM friends LEFT OUTER JOIN streaks ON friends.userId = streaks.id WHERE userId = ?", arrayOf(userId)).use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        MessagingFriendInfo.fromCursor(cursor)
    }
}

fun AppDatabase.findFriend(conversationId: String): MessagingFriendInfo? {
    return database.rawQuery("SELECT * FROM friends WHERE dmConversationId = ?", arrayOf(conversationId)).use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        MessagingFriendInfo.fromCursor(cursor)
    }
}

fun AppDatabase.deleteFriend(userId: String) {
    executeAsync {
        database.execSQL("DELETE FROM friends WHERE userId = ?", arrayOf(userId))
        database.execSQL("DELETE FROM streaks WHERE id = ?", arrayOf(userId))
        database.execSQL("DELETE FROM rules WHERE targetUuid = ?", arrayOf(userId))
    }
}

fun AppDatabase.deleteGroup(conversationId: String) {
    executeAsync {
        database.execSQL("DELETE FROM groups WHERE conversationId = ?", arrayOf(conversationId))
        database.execSQL("DELETE FROM rules WHERE targetUuid = ?", arrayOf(conversationId))
    }
}

fun AppDatabase.getGroupInfo(conversationId: String): MessagingGroupInfo? {
    return database.rawQuery("SELECT * FROM groups WHERE conversationId = ?", arrayOf(conversationId)).use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        MessagingGroupInfo.fromCursor(cursor)
    }
}

fun AppDatabase.getFriendStreaks(userId: String): FriendStreaks? {
    return database.rawQuery("SELECT * FROM streaks WHERE id = ?", arrayOf(userId)).use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        FriendStreaks(
            notify = cursor.getInteger("notify") == 1,
            expirationTimestamp = cursor.getLongOrNull("expirationTimestamp") ?: 0L,
            length = cursor.getInteger("length")
        )
    }
}

fun AppDatabase.setFriendStreaksNotify(userId: String, notify: Boolean) {
    executeAsync {
        database.execSQL("UPDATE streaks SET notify = ? WHERE id = ?", arrayOf(
            if (notify) 1 else 0,
            userId
        ))
    }
}

fun AppDatabase.getRuleIds(type: String): MutableList<String> {
    return database.rawQuery("SELECT targetUuid FROM rules WHERE type = ?", arrayOf(type)).use { cursor ->
        val ruleIds = mutableListOf<String>()
        while (cursor.moveToNext()) {
            ruleIds.add(cursor.getStringOrNull("targetUuid")!!)
        }
        ruleIds
    }
}

fun AppDatabase.clearRuleIds(type: String) {
    executeAsync {
        database.execSQL("DELETE FROM rules WHERE type = ?", arrayOf(type))
    }
}

