package karpiuk.ivan.miniagent.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import karpiuk.ivan.miniagent.data.local.dao.ChatDao
import karpiuk.ivan.miniagent.data.local.dao.MessageDao
import karpiuk.ivan.miniagent.data.local.entity.ChatEntity
import karpiuk.ivan.miniagent.data.local.entity.MessageEntity

@Database(
    entities = [ChatEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
}
