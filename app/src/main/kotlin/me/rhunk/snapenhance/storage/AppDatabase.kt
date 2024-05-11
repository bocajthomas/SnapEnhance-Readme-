package me.rhunk.snapenhance.storage

import androidx.room.Database
import androidx.room.RoomDatabase
import me.rhunk.snapenhance.common.data.Friend
import me.rhunk.snapenhance.common.data.FriendStreaks
import me.rhunk.snapenhance.common.data.Group

@Database(entities = [
    Friend::class,
    FriendStreaks::class,
    Group::class,
    MessagingRule::class
], version = 2)
abstract class AppDatabase: RoomDatabase() {
    abstract fun friendStreaksDao(): FriendStreaksDao
    abstract fun friendDao(): FriendDao
    abstract fun groupDao(): GroupDao
    abstract fun messagingRuleDao(): MessagingRuleDao
}