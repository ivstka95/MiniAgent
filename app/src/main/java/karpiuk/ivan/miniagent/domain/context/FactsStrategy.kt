package karpiuk.ivan.miniagent.domain.context

import karpiuk.ivan.miniagent.domain.agent.LlmClient
import karpiuk.ivan.miniagent.domain.model.Message
import karpiuk.ivan.miniagent.domain.model.Role
import karpiuk.ivan.miniagent.domain.repository.ChatRepository
import javax.inject.Inject

class FactsStrategy
    @Inject
    constructor(
        private val llmClient: LlmClient,
        private val repository: ChatRepository,
    ) : ContextStrategy {
        companion object {
            // How many recent messages to keep alongside the facts block.
            // KEEP_LAST_N MUST be even — see alternation invariant in buildContext.
            const val KEEP_LAST_N = 6
        }

        override suspend fun buildContext(allMessages: List<Message>): List<Message> {
            if (allMessages.isEmpty()) return allMessages
            val chatId = allMessages.first().chatId

            // Re-extract and merge facts on every send; persist them. On failure keep stored facts.
            val currentFacts = repository.getChatById(chatId)?.facts
            val updatedFacts: String? =
                try {
                    val facts = llmClient.extractFacts(currentFacts, allMessages)
                    repository.updateChatFacts(chatId, facts)
                    facts
                } catch (_: Exception) {
                    currentFacts
                }

            // No facts available yet (extraction failed and nothing stored) — send everything as-is.
            if (updatedFacts.isNullOrBlank()) return allMessages

            // Alternation invariant:
            // The facts message is USER-role, so the last-N window must start with ASSISTANT
            // (USER facts -> ASSISTANT -> USER -> ...). allMessages ends with the current USER
            // message; with KEEP_LAST_N even, takeLast(KEEP_LAST_N) starts at an odd 0-based index
            // and therefore on an ASSISTANT message. Defensively drop a leading USER message (short
            // history / odd cases) so no two USER messages end up adjacent.
            var window = allMessages.takeLast(KEEP_LAST_N)
            if (window.firstOrNull()?.role == Role.USER) window = window.drop(1)

            val factsMessage =
                Message(
                    id = "facts-$chatId",
                    chatId = chatId,
                    role = Role.USER,
                    content = "Known facts: $updatedFacts",
                    timestamp = 0L,
                )
            return listOf(factsMessage) + window
        }
    }
