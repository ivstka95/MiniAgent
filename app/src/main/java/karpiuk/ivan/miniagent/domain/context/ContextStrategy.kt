package karpiuk.ivan.miniagent.domain.context

import karpiuk.ivan.miniagent.domain.model.Message

interface ContextStrategy {
    suspend fun buildContext(allMessages: List<Message>): List<Message>
}
