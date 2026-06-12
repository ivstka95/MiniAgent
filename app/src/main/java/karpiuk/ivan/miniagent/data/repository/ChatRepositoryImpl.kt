package karpiuk.ivan.miniagent.data.repository

import javax.inject.Inject
import karpiuk.ivan.miniagent.data.local.dao.ChatDao
import karpiuk.ivan.miniagent.data.local.dao.MessageDao
import karpiuk.ivan.miniagent.data.local.entity.ChatEntity
import karpiuk.ivan.miniagent.data.local.entity.MessageEntity
import karpiuk.ivan.miniagent.domain.model.Chat
import karpiuk.ivan.miniagent.domain.model.Message
import karpiuk.ivan.miniagent.domain.model.Role
import karpiuk.ivan.miniagent.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ChatRepositoryImpl @Inject constructor(
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
) : ChatRepository {

    override fun observeChats(): Flow<List<Chat>> =
        chatDao.observeChats().map { it.map(ChatEntity::toDomain) }

    override fun observeMessages(chatId: String): Flow<List<Message>> =
        messageDao.observeMessages(chatId).map { it.map(MessageEntity::toDomain) }

    override suspend fun createChat(title: String): Chat {
        val chat = Chat(
            id = UUID.randomUUID().toString(),
            title = title,
            createdAt = System.currentTimeMillis(),
        )
        chatDao.insert(chat.toEntity())
        return chat
    }

    override suspend fun addMessage(message: Message) {
        messageDao.insert(message.toEntity())
    }

    override suspend fun getMessagesOnce(chatId: String): List<Message> =
        messageDao.getByChat(chatId).map(MessageEntity::toDomain)

    override suspend fun updateChatTitle(chatId: String, title: String) {
        chatDao.updateTitle(chatId, title)
    }

    override suspend fun deleteChat(chatId: String) {
        chatDao.deleteById(chatId)
    }
}

// — Mappers —

private fun ChatEntity.toDomain() = Chat(id = id, title = title, createdAt = createdAt)
private fun Chat.toEntity() = ChatEntity(id = id, title = title, createdAt = createdAt)

private fun MessageEntity.toDomain() = Message(
    id = id,
    chatId = chatId,
    role = Role.valueOf(role.uppercase()),
    content = content,
    timestamp = timestamp,
    tokenCount = tokenCount,
)

private fun Message.toEntity() = MessageEntity(
    id = id,
    chatId = chatId,
    role = role.name.lowercase(),
    content = content,
    timestamp = timestamp,
    tokenCount = tokenCount,
)
