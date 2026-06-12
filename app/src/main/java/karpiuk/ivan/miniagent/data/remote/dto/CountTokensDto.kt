package karpiuk.ivan.miniagent.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CountTokensRequestDto(
    val model: String,
    val messages: List<MessageDto>,
)

@Serializable
data class CountTokensResponseDto(
    @SerialName("input_tokens") val inputTokens: Int,
)
