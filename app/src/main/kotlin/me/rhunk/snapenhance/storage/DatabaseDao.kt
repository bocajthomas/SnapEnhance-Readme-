package me.rhunk.snapenhance.storage

import androidx.room.*
import me.rhunk.snapenhance.common.data.Friend
import me.rhunk.snapenhance.common.data.FriendStreaks
import me.rhunk.snapenhance.common.data.Group

@Dao
interface FriendStreaksDao {
    @Query("SELECT * FROM friendstreaks WHERE userId = :userId")
    fun getByUserId(userId: String): FriendStreaks?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(friendStreaks: FriendStreaks)

    @Query("DELETE FROM friendstreaks WHERE userId = :userId")
    fun delete(userId: String)

    @Query("UPDATE friendstreaks SET length = :length, expirationTimestamp = :timestamp WHERE userId = :userId")
    fun update(userId: String, length: Int, timestamp: Long)

    fun insertOrUpdate(friendStreaks: FriendStreaks) {
        val existing = getByUserId(friendStreaks.userId)
        if (existing == null) {
            insert(friendStreaks.copy(notify = true))
        } else {
            update(friendStreaks.userId, friendStreaks.length, friendStreaks.expirationTimestamp)
        }
    }

    @Query("UPDATE friendstreaks SET notify = :notify WHERE userId = :userId")
    fun setNotify(userId: String, notify: Boolean)
}

@Dao
interface FriendDao {
    @Query("SELECT * FROM friend")
    fun getAll(): List<Friend>

    @Query("SELECT * FROM friend ORDER BY id DESC")
    fun getAllDesc(): List<Friend>

    @Query("SELECT * FROM friend WHERE userId = :userId")
    fun getByUserId(userId: String): Friend?

    @Query("SELECT * FROM friend WHERE userId = :userId")
    fun exists(userId: String): Boolean

    @Query("SELECT * FROM friend WHERE dmConversationId = :dmConversationId")
    fun getByDmConversationId(dmConversationId: String): Friend?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(friend: Friend)

    @Update
    fun update(friend: Friend)

    @Query("DELETE FROM friend WHERE userId = :userId")
    fun delete(userId: String)

    fun insertOrUpdate(friend: Friend) {
        val existing = getByUserId(friend.userId)
        if (existing == null) {
            insert(friend)
        } else {
            update(friend)
        }
    }
}

@Dao
interface GroupDao {
    @Query("SELECT * FROM `group`")
    fun getAll(): List<Group>

    @Query("SELECT * FROM `group` WHERE conversationId = :conversationId")
    fun getByConversationId(conversationId: String): Group?

    @Query("SELECT * FROM `group` WHERE conversationId = :conversationId")
    fun exists(conversationId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(group: Group)

    @Update
    fun update(group: Group)

    @Query("DELETE FROM `group` WHERE conversationId = :conversationId")
    fun delete(conversationId: String)

    fun insertOrUpdate(group: Group) {
        val existing = getByConversationId(group.conversationId)
        if (existing == null) {
            insert(group)
        } else {
            update(group)
        }
    }
}

@Entity
data class MessagingRule(
    @PrimaryKey val id: Int? = null,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "targetUuid") val targetUuid: String
)

@Dao
interface MessagingRuleDao {
    @Query("SELECT targetUuid FROM messagingrule WHERE type = :type")
    fun getIds(type: String): List<String>

    @Query("SELECT * FROM messagingrule WHERE targetUuid = :targetUuid")
    fun getAll(targetUuid: String): List<MessagingRule>

    @Query("SELECT * FROM messagingrule WHERE targetUuid = :targetUuid AND type = :type")
    fun getAll(targetUuid: String, type: String): List<MessagingRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(messagingRule: MessagingRule)

    @Query("DELETE FROM messagingrule WHERE targetUuid = :targetUuid AND type = :type")
    fun delete(targetUuid: String, type: String)

    fun setState(messagingRule: MessagingRule, state: Boolean) {
        if (state) {
            delete(messagingRule.targetUuid, messagingRule.type)
            insert(messagingRule)
        } else {
            delete(messagingRule.targetUuid, messagingRule.type)
        }
    }
}
