package karpiuk.ivan.miniagent.domain.agent

import karpiuk.ivan.miniagent.domain.model.Message
import karpiuk.ivan.miniagent.domain.model.Role
import karpiuk.ivan.miniagent.domain.repository.ChatRepository
import java.util.UUID

class Agent(
    private val repository: ChatRepository,
    private val llmClient: LlmClient,
) {
    suspend fun sendMessage(chatId: String, userInput: String): AgentResponse {
        val userMessage = Message(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            role = Role.USER,
            content = userInput,
            timestamp = System.currentTimeMillis(),
        )
        repository.addMessage(userMessage)
        val history = buildHistory(chatId)
        val result = llmClient.complete(history)
        val assistantMessage = Message(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            role = Role.ASSISTANT,
            content = result.assistantText,
            timestamp = System.currentTimeMillis(),
        )
        repository.addMessage(assistantMessage)
        return AgentResponse(
            replyText = result.assistantText,
            inputTokens = result.inputTokens,
            outputTokens = result.outputTokens,
            thinkingTokens = result.thinkingTokens,
        )
    }

    suspend fun generateTitle(userMessage: String, assistantMessage: String): String {
        val prompt = Message(
            id = UUID.randomUUID().toString(),
            chatId = "",
            role = Role.USER,
            content = "Generate a concise 4–6 word title for a conversation that started with:\n" +
                    "User: $userMessage\n" +
                    "Assistant: $assistantMessage\n" +
                    "Reply with only the title, no punctuation.",
            timestamp = System.currentTimeMillis(),
        )
        return llmClient.complete(listOf(prompt)).assistantText.trim()
    }

    private suspend fun buildHistory(chatId: String): List<Message> =
        repository.getMessagesOnce(chatId)
}
