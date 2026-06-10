package karpiuk.ivan.miniagent.data.repository

import javax.inject.Inject
import karpiuk.ivan.miniagent.data.remote.AnthropicApi
import karpiuk.ivan.miniagent.data.remote.dto.AnthropicRequestDto
import karpiuk.ivan.miniagent.data.remote.dto.MessageDto
import karpiuk.ivan.miniagent.domain.agent.LlmClient
import karpiuk.ivan.miniagent.domain.agent.LlmResult
import karpiuk.ivan.miniagent.domain.model.Message
import karpiuk.ivan.miniagent.domain.model.Role

class LlmClientImpl @Inject constructor(private val api: AnthropicApi) : LlmClient {

    override suspend fun complete(messages: List<Message>): LlmResult {
        val request = AnthropicRequestDto(
            model = MODEL,
            maxTokens = MAX_TOKENS,
            messages = messages.map { MessageDto(role = it.role.toApiString(), content = it.content) },
        )
        val response = try {
            api.createMessage(request)
        } catch (e: Exception) {
            throw LlmException("LLM request failed: ${e.message}", e)
        }
        val text = response.content.firstOrNull { it.type == "text" }?.text
            ?: throw LlmException("No text block in LLM response")
        return LlmResult(
            assistantText = text,
            inputTokens = response.usage.inputTokens,
            outputTokens = response.usage.outputTokens,
            thinkingTokens = response.usage.outputTokensDetails?.thinkingTokens ?: 0,
        )
    }

    private fun Role.toApiString() = when (this) {
        Role.USER -> "user"
        Role.ASSISTANT -> "assistant"
    }

    companion object {
        private const val MODEL = "claude-haiku-4-5-20251001"
        private const val MAX_TOKENS = 1024
    }
}

class LlmException(message: String, cause: Throwable? = null) : Exception(message, cause)
