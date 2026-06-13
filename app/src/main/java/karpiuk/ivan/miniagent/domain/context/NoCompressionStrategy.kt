package karpiuk.ivan.miniagent.domain.context

import karpiuk.ivan.miniagent.domain.model.Message
import javax.inject.Inject

class NoCompressionStrategy @Inject constructor() : ContextStrategy {
    override suspend fun buildContext(allMessages: List<Message>): List<Message> = allMessages
}
