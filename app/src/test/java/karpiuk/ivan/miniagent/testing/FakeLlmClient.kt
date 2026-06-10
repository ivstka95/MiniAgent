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

    override suspend fun complete(messages: List<Message>): LlmResult {
        capturedMessages.add(messages.toList())
        return result
    }
}
