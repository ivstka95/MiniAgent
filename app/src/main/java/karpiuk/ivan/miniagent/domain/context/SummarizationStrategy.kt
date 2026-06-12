package karpiuk.ivan.miniagent.domain.context

import karpiuk.ivan.miniagent.domain.agent.LlmClient
import karpiuk.ivan.miniagent.domain.model.Message
import karpiuk.ivan.miniagent.domain.model.Role
import karpiuk.ivan.miniagent.domain.repository.ChatRepository
import javax.inject.Inject

class SummarizationStrategy @Inject constructor(
    private val llmClient: LlmClient,
    private val repository: ChatRepository,
) : ContextStrategy {

    companion object {
        // Lower these constants for a faster demo (e.g. KEEP_LAST_N=4, SUMMARIZE_EVERY=3)
        // KEEP_LAST_N MUST be even — see alternation invariant in buildContext
        const val KEEP_LAST_N = 6
        const val SUMMARIZE_EVERY = 10
    }

    override suspend fun buildContext(allMessages: List<Message>): List<Message> {
        if (allMessages.isEmpty()) return allMessages
        val chatId = allMessages.first().chatId

        val lastN = allMessages.takeLast(KEEP_LAST_N)
        val olderMessages = allMessages.dropLast(KEEP_LAST_N)

        // Not enough history to compress — send everything as-is
        if (olderMessages.isEmpty()) return allMessages

        val chat = repository.getChatById(chatId)
        val coveredCount = chat?.summaryCoversCount ?: 0
        val uncoveredCount = olderMessages.size - coveredCount

        // (Re-)summarise when enough new messages have accumulated beyond covered range
        val summary: String? = if (uncoveredCount >= SUMMARIZE_EVERY) {
            try {
                val newSummary = llmClient.summarize(olderMessages)
                repository.updateChatSummary(chatId, newSummary, olderMessages.size)
                newSummary
            } catch (_: Exception) {
                chat?.summary // fall back to existing persisted summary
            }
        } else {
            chat?.summary // use cached summary, no new summarisation needed
        }

        // No summary available yet (history too short for first summarisation) — send all
        if (summary == null) return allMessages

        // Alternation invariant:
        // allMessages always has ODD length (ends with the current USER message).
        // With KEEP_LAST_N even, takeLast(KEEP_LAST_N) starts at an odd 0-based index,
        // so lastN[0].role == ASSISTANT. Prepending a USER summary message therefore
        // produces the valid alternation USER -> ASSISTANT -> ... required by the API.
        val summaryMessage = Message(
            id = "summary-$chatId",
            chatId = chatId,
            role = Role.USER,
            content = "Summary of earlier conversation: $summary",
            timestamp = 0L,
        )
        return listOf(summaryMessage) + lastN
    }
}
