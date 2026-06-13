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

    override suspend fun summarize(messages: List<Message>): String {
        val prompt = "Summarize the following conversation concisely, preserving key facts:\n\n" +
            messages.joinToString("\n") { "${it.role.toApiString().replaceFirstChar(Char::uppercase)}: ${it.content}" }
        return completePrompt(prompt)
    }

    override suspend fun extractFacts(currentFacts: String?, messages: List<Message>): String {
        val existing = if (currentFacts.isNullOrBlank()) "(none yet)" else currentFacts
        val conversation =
            messages.joinToString("\n") { "${it.role.toApiString().replaceFirstChar(Char::uppercase)}: ${it.content}" }
        val prompt = "You maintain a running key-value fact store for a conversation. " +
            "Given the existing facts and the conversation, return an UPDATED set of facts as a compact JSON " +
            "object of key-value pairs, capturing the user's goal, constraints, preferences, decisions, and " +
            "agreements. Merge new information into the existing facts; do not lose prior facts unless they are " +
            "contradicted. Keep it concise. Return ONLY the JSON object, with no prose and no markdown fences.\n\n" +
            "Existing facts: $existing\n\nConversation:\n$conversation"
        return completePrompt(prompt)
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
