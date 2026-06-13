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
    suspend fun updateChatTitle(chatId: String, title: String)
    suspend fun deleteChat(chatId: String)
    suspend fun getChatById(chatId: String): Chat?
    suspend fun updateChatSummary(chatId: String, summary: String, coversCount: Int)
    suspend fun updateChatFacts(chatId: String, facts: String)

    /**
     * Forks [sourceChatId] at [messageId]: creates a new chat copying all messages from the start
     * up to and INCLUDING that message, then returns the new chat's id. The source chat is unchanged.
     */
    suspend fun branchChatFromMessage(sourceChatId: String, messageId: String): String
}
