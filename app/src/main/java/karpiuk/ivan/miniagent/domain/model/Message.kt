package karpiuk.ivan.miniagent.domain.model

data class Message(
    val id: String,
    val chatId: String,
    val role: Role,
    val content: String,
    val timestamp: Long,
    val tokenCount: Int? = null,
)
