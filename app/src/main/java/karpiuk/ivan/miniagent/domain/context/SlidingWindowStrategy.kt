package karpiuk.ivan.miniagent.domain.context

import karpiuk.ivan.miniagent.domain.model.Message
import karpiuk.ivan.miniagent.domain.model.Role
import javax.inject.Inject

class SlidingWindowStrategy
    @Inject
    constructor() : ContextStrategy {
        companion object {
            // Raise/lower this to change how many recent messages are kept (default 6).
            const val WINDOW_SIZE = 6
        }

        override suspend fun buildContext(allMessages: List<Message>): List<Message> {
            // Keep only the last WINDOW_SIZE messages; discard everything older.
            val window = allMessages.takeLast(WINDOW_SIZE)
            // Alternation invariant: the first message sent must be USER. takeLast can begin on an
            // ASSISTANT message (mid-alternation); drop it so the window starts with USER. For lists
            // shorter than WINDOW_SIZE, takeLast returns the whole (USER-first) list, so no drop.
            return if (window.firstOrNull()?.role == Role.ASSISTANT) window.drop(1) else window
        }
    }
