package karpiuk.ivan.miniagent.testing

import karpiuk.ivan.miniagent.domain.model.Chat
import karpiuk.ivan.miniagent.domain.model.Message
import karpiuk.ivan.miniagent.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID

class FakeChatRepository : ChatRepository {
    private val chats = MutableStateFlow<List<Chat>>(emptyList())
    private val messages = MutableStateFlow<List<Message>>(emptyList())

    override fun observeChats(): Flow<List<Chat>> =
        chats.map { it.sortedByDescending { c -> c.createdAt } }

    override fun observeMessages(chatId: String): Flow<List<Message>> =
        messages.map { all ->
            all.filter { it.chatId == chatId }.sortedBy { it.timestamp }
        }

    override suspend fun createChat(title: String): Chat {
        val chat = Chat(
            id = UUID.randomUUID().toString(),
            title = title,
            createdAt = System.currentTimeMillis(),
        )
        chats.value = chats.value + chat
        return chat
    }

    override suspend fun addMessage(message: Message) {
        messages.value = messages.value + message
    }

    override suspend fun getMessagesOnce(chatId: String): List<Message> =
        messages.value.filter { it.chatId == chatId }.sortedBy { it.timestamp }

    override suspend fun updateChatTitle(chatId: String, title: String) {
        chats.value = chats.value.map { if (it.id == chatId) it.copy(title = title) else it }
    }

    override suspend fun deleteChat(chatId: String) {
        chats.value = chats.value.filter { it.id != chatId }
        messages.value = messages.value.filter { it.chatId != chatId }
    }

    fun seedChat(chat: Chat) {
        chats.value = chats.value + chat
    }
}
