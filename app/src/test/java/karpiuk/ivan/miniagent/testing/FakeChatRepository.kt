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

    override suspend fun getChatById(chatId: String): Chat? =
        chats.value.find { it.id == chatId }

    override suspend fun updateChatSummary(chatId: String, summary: String, coversCount: Int) {
        chats.value = chats.value.map {
            if (it.id == chatId) it.copy(summary = summary, summaryCoversCount = coversCount) else it
        }
    }

    override suspend fun updateChatFacts(chatId: String, facts: String) {
        chats.value = chats.value.map {
            if (it.id == chatId) it.copy(facts = facts) else it
        }
    }

    override suspend fun branchChatFromMessage(sourceChatId: String, messageId: String): String {
        val title = getChatById(sourceChatId)?.title ?: "Chat"
        val all = getMessagesOnce(sourceChatId)
        val cutoff = all.indexOfFirst { it.id == messageId }
        val toCopy = if (cutoff >= 0) all.take(cutoff + 1) else all
        val branch = createChat("$title (branch)")
        toCopy.forEach { message ->
            addMessage(message.copy(id = UUID.randomUUID().toString(), chatId = branch.id))
        }
        return branch.id
    }

    fun seedChat(chat: Chat) {
        chats.value = chats.value + chat
    }

    fun seedChat(chatId: String) {
        chats.value = chats.value + Chat(id = chatId, title = "Test Chat", createdAt = 0L)
    }
}
