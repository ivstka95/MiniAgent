package karpiuk.ivan.miniagent.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnthropicResponseDto(
    val content: List<ContentBlockDto>,
    @SerialName("stop_reason") val stopReason: String? = null,
    val usage: UsageDto,
)

@Serializable
data class ContentBlockDto(
    val type: String,
    val text: String? = null,
)

@Serializable
data class UsageDto(
    @SerialName("input_tokens") val inputTokens: Int,
    @SerialName("output_tokens") val outputTokens: Int,
    @SerialName("output_tokens_details") val outputTokensDetails: OutputTokensDetailsDto? = null,
)

@Serializable
data class OutputTokensDetailsDto(
    @SerialName("thinking_tokens") val thinkingTokens: Int? = null,
)
