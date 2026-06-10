package karpiuk.ivan.miniagent.domain.agent

import karpiuk.ivan.miniagent.domain.model.Message

interface LlmClient {
    suspend fun complete(messages: List<Message>): LlmResult
}
