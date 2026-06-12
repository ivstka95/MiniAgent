package karpiuk.ivan.miniagent.domain.agent

import karpiuk.ivan.miniagent.domain.context.ContextStrategy
import karpiuk.ivan.miniagent.domain.context.ContextStrategyManager
import karpiuk.ivan.miniagent.domain.context.ContextStrategyType
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
    private val strategyManager: ContextStrategyManager,
    private val strategies: Map<ContextStrategyType, ContextStrategy>,
) {
    suspend fun sendMessage(chatId: String, userInput: String): AgentResponse {
        val prevMessages = repository.getMessagesOnce(chatId)
        val isFirstMessage = prevMessages.isEmpty()
        val userMessage = Message(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            role = Role.USER,
            content = userInput,
            timestamp = System.currentTimeMillis(),
        )
        val userTokenCount = try {
            llmClient.countTokens(listOf(userMessage))
        } catch (_: Exception) {
            null
        }
        val savedUserMessage = userMessage.copy(tokenCount = userTokenCount)
        repository.addMessage(savedUserMessage)
        val allMessages = prevMessages + savedUserMessage
        val activeStrategy = strategies[strategyManager.activeType.value]
            ?: strategies[ContextStrategyType.NONE]!!
        val contextMessages = activeStrategy.buildContext(allMessages)
        val result = llmClient.complete(contextMessages)
        val assistantMessage = Message(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            role = Role.ASSISTANT,
            content = result.assistantText,
            timestamp = System.currentTimeMillis(),
            tokenCount = result.outputTokens,
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
            conversationTotalTokens = result.inputTokens + result.outputTokens,
        )
    }

    private suspend fun generateTitle(userMessage: String, assistantMessage: String): String {
        val prompt = "Generate a concise 4–6 word title for a conversation that started with:\n" +
                "User: $userMessage\n" +
                "Assistant: $assistantMessage\n" +
                "Reply with only the title, no punctuation."
        return llmClient.completePrompt(prompt)
    }
}
