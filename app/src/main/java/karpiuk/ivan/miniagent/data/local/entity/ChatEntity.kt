package karpiuk.ivan.miniagent.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val summary: String? = null,
    val summaryCoversCount: Int = 0,
)
