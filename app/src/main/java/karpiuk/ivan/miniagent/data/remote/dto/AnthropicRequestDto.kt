package karpiuk.ivan.miniagent.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnthropicRequestDto(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int,
    val messages: List<MessageDto>,
)

@Serializable
data class MessageDto(
    val role: String,
    val content: String,
)
