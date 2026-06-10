package karpiuk.ivan.miniagent.domain.agent

import karpiuk.ivan.miniagent.domain.model.Message
import karpiuk.ivan.miniagent.domain.model.Role
import karpiuk.ivan.miniagent.domain.repository.ChatRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID

class Agent(
    private val repository: ChatRepository,
    private val llmClient: LlmClient,
    private val scope: CoroutineScope,
) {
    suspend fun sendMessage(chatId: String, userInput: String): AgentResponse {
        val isFirstMessage = repository.getMessagesOnce(chatId).isEmpty()
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
        if (isFirstMessage) {
            scope.launch {
                try {
                    val title = generateTitle(userInput, result.assistantText)
                    repository.updateChatTitle(chatId, title)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // keep "New chat" on title-generation failure
                }
            }
        }
        return AgentResponse(
            replyText = result.assistantText,
            inputTokens = result.inputTokens,
            outputTokens = result.outputTokens,
            thinkingTokens = result.thinkingTokens,
        )
    }

    private suspend fun generateTitle(userMessage: String, assistantMessage: String): String {
        val prompt = "Generate a concise 4–6 word title for a conversation that started with:\n" +
                "User: $userMessage\n" +
                "Assistant: $assistantMessage\n" +
                "Reply with only the title, no punctuation."
        return llmClient.completePrompt(prompt)
    }

    private suspend fun buildHistory(chatId: String): List<Message> =
        repository.getMessagesOnce(chatId)
}
