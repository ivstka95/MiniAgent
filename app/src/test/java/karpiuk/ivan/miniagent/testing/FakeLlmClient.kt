package karpiuk.ivan.miniagent.testing

import karpiuk.ivan.miniagent.domain.agent.LlmClient
import karpiuk.ivan.miniagent.domain.agent.LlmResult
import karpiuk.ivan.miniagent.domain.model.Message

class FakeLlmClient : LlmClient {
    var result = LlmResult(
        assistantText = "default reply",
        inputTokens = 10,
        outputTokens = 5,
        thinkingTokens = 0,
    )
    val capturedMessages = mutableListOf<List<Message>>()
    val capturedPrompts = mutableListOf<String>()

    override suspend fun complete(messages: List<Message>): LlmResult {
        capturedMessages.add(messages.toList())
        return result
    }

    var completePromptException: Exception? = null

    override suspend fun countTokens(messages: List<Message>): Int = 0

    override suspend fun completePrompt(prompt: String): String {
        capturedPrompts.add(prompt)
        completePromptException?.let { throw it }
        return result.assistantText.trim()
    }

    var summarizeResult: String = "fake summary"
    var summarizeException: Exception? = null
    val capturedSummarizeMessages = mutableListOf<List<Message>>()

    override suspend fun summarize(messages: List<Message>): String {
        capturedSummarizeMessages.add(messages.toList())
        summarizeException?.let { throw it }
        return summarizeResult
    }
}
