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

    override suspend fun completePrompt(prompt: String): String {
        capturedPrompts.add(prompt)
        return result.assistantText.trim()
    }
}
