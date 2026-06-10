package karpiuk.ivan.miniagent.domain.repository

import karpiuk.ivan.miniagent.domain.model.Chat
import karpiuk.ivan.miniagent.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeChats(): Flow<List<Chat>>
    fun observeMessages(chatId: String): Flow<List<Message>>
    suspend fun createChat(title: String): Chat
    suspend fun addMessage(message: Message)
    suspend fun getMessagesOnce(chatId: String): List<Message>
    suspend fun deleteChat(chatId: String)
}
