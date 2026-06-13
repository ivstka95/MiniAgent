package karpiuk.ivan.miniagent.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import karpiuk.ivan.miniagent.data.local.entity.ChatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY createdAt DESC")
    fun observeChats(): Flow<List<ChatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chat: ChatEntity)

    @Query("UPDATE chats SET title = :title WHERE id = :chatId")
    suspend fun updateTitle(chatId: String, title: String)

    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun deleteById(chatId: String)

    @Query("SELECT * FROM chats WHERE id = :chatId LIMIT 1")
    suspend fun getById(chatId: String): ChatEntity?

    @Query("UPDATE chats SET summary = :summary, summaryCoversCount = :coversCount WHERE id = :chatId")
    suspend fun updateSummary(chatId: String, summary: String, coversCount: Int)

    @Query("UPDATE chats SET facts = :facts WHERE id = :chatId")
    suspend fun updateFacts(chatId: String, facts: String)
}
