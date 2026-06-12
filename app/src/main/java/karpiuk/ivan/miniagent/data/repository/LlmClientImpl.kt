package karpiuk.ivan.miniagent.data.repository

import javax.inject.Inject
import karpiuk.ivan.miniagent.data.remote.AnthropicApi
import karpiuk.ivan.miniagent.data.remote.dto.AnthropicRequestDto
import karpiuk.ivan.miniagent.data.remote.dto.CountTokensRequestDto
import karpiuk.ivan.miniagent.data.remote.dto.MessageDto
import karpiuk.ivan.miniagent.domain.agent.LlmClient
import karpiuk.ivan.miniagent.domain.agent.LlmResult
import karpiuk.ivan.miniagent.domain.model.Message
import karpiuk.ivan.miniagent.domain.model.Role
import retrofit2.HttpException

class LlmClientImpl @Inject constructor(private val api: AnthropicApi) : LlmClient {

    override suspend fun complete(messages: List<Message>): LlmResult {
        val request = AnthropicRequestDto(
            model = MODEL,
            maxTokens = MAX_TOKENS,
            messages = messages.map { it.toDto() },
        )
        val response = wrapApiErrors { api.createMessage(request) }
        val text = response.content.firstOrNull { it.type == "text" }?.text
            ?: throw LlmException("No text block in LLM response")
        return LlmResult(
            assistantText = text,
            inputTokens = response.usage.inputTokens,
            outputTokens = response.usage.outputTokens,
            thinkingTokens = response.usage.outputTokensDetails?.thinkingTokens ?: 0,
        )
    }

    override suspend fun countTokens(messages: List<Message>): Int {
        val request = CountTokensRequestDto(
            model = MODEL,
            messages = messages.map { it.toDto() },
        )
        return wrapApiErrors("Token count request failed") { api.countTokens(request).inputTokens }
    }

    override suspend fun completePrompt(prompt: String): String {
        val request = AnthropicRequestDto(
            model = MODEL,
            maxTokens = MAX_TOKENS,
            messages = listOf(MessageDto(role = "user", content = prompt)),
        )
        val response = wrapApiErrors { api.createMessage(request) }
        return response.content.firstOrNull { it.type == "text" }?.text?.trim()
            ?: throw LlmException("No text block in LLM response")
    }

    private suspend fun <T> wrapApiErrors(
        prefix: String = "LLM request failed",
        block: suspend () -> T,
    ): T = try {
        block()
    } catch (e: HttpException) {
        val body = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
        throw LlmException("$prefix: ${body ?: "HTTP ${e.code()}"}", e)
    } catch (e: Exception) {
        throw LlmException("$prefix: ${e.message}", e)
    }

    private fun Message.toDto() = MessageDto(role = role.toApiString(), content = content)

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
