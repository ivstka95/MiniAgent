package karpiuk.ivan.miniagent.domain.agent

import karpiuk.ivan.miniagent.domain.model.Message

interface LlmClient {
    suspend fun complete(messages: List<Message>): LlmResult
    suspend fun completePrompt(prompt: String): String
    suspend fun countTokens(messages: List<Message>): Int
    suspend fun summarize(messages: List<Message>): String
    suspend fun extractFacts(currentFacts: String?, messages: List<Message>): String
}
