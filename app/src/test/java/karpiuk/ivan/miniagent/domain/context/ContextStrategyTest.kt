package karpiuk.ivan.miniagent.domain.context

import karpiuk.ivan.miniagent.domain.model.Message
import karpiuk.ivan.miniagent.domain.model.Role
import karpiuk.ivan.miniagent.testing.FakeChatRepository
import karpiuk.ivan.miniagent.testing.FakeLlmClient
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ContextStrategyTest {

    private lateinit var fakeLlmClient: FakeLlmClient
    private lateinit var fakeRepository: FakeChatRepository
    private val chatId = "chat-1"

    @Before
    fun setUp() {
        fakeLlmClient = FakeLlmClient()
        fakeRepository = FakeChatRepository()
    }

    // — helpers —

    private fun msg(role: Role, content: String = "text", index: Int = 0) = Message(
        id = "m$index", chatId = chatId, role = role, content = content,
        timestamp = index.toLong(),
    )

    /** Builds a properly alternating USER/ASSISTANT history of size n, ending with USER. */
    private fun history(n: Int): List<Message> = (0 until n).map { i ->
        msg(if (i % 2 == 0) Role.USER else Role.ASSISTANT, "msg$i", i)
    }

    // ——————————————— NoCompressionStrategy ———————————————

    @Test
    fun `NoCompressionStrategy returns allMessages unchanged`() = runTest {
        val strategy = NoCompressionStrategy()
        val messages = history(5)
        assertEquals(messages, strategy.buildContext(messages))
    }

    @Test
    fun `NoCompressionStrategy returns empty list unchanged`() = runTest {
        val strategy = NoCompressionStrategy()
        assertTrue(strategy.buildContext(emptyList()).isEmpty())
    }

    // ——————————————— SummarizationStrategy ———————————————

    @Test
    fun `SummarizationStrategy returns allMessages when size is at most KEEP_LAST_N`() = runTest {
        fakeRepository.seedChat(chatId)
        val strategy = SummarizationStrategy(fakeLlmClient, fakeRepository)
        val messages = history(SummarizationStrategy.KEEP_LAST_N)
        assertEquals(messages, strategy.buildContext(messages))
    }

    @Test
    fun `SummarizationStrategy returns allMessages when olderMessages exist but uncoveredCount below threshold`() = runTest {
        fakeRepository.seedChat(chatId)
        val strategy = SummarizationStrategy(fakeLlmClient, fakeRepository)
        // olderMessages.size == 1, SUMMARIZE_EVERY == 10: no summary yet
        val messages = history(SummarizationStrategy.KEEP_LAST_N + 1)
        assertEquals(messages, strategy.buildContext(messages))
    }

    @Test
    fun `SummarizationStrategy calls summarize when uncoveredCount reaches threshold`() = runTest {
        fakeRepository.seedChat(chatId)
        fakeLlmClient.summarizeResult = "a concise summary"
        val strategy = SummarizationStrategy(fakeLlmClient, fakeRepository)
        val messages = history(SummarizationStrategy.KEEP_LAST_N + SummarizationStrategy.SUMMARIZE_EVERY + 1)

        val result = strategy.buildContext(messages)

        assertEquals(1, fakeLlmClient.capturedSummarizeMessages.size)
        assertTrue(result.first().content.contains("a concise summary"))
        assertEquals(Role.USER, result.first().role)
        assertEquals(SummarizationStrategy.KEEP_LAST_N, result.size - 1) // summary + lastN
    }

    @Test
    fun `SummarizationStrategy uses cached summary when uncoveredCount is below threshold`() = runTest {
        fakeRepository.seedChat(chatId)
        // Pre-seed a summary covering all older messages
        val totalMessages = SummarizationStrategy.KEEP_LAST_N + SummarizationStrategy.SUMMARIZE_EVERY + 1
        fakeRepository.updateChatSummary(chatId, "old summary", totalMessages - SummarizationStrategy.KEEP_LAST_N)
        val strategy = SummarizationStrategy(fakeLlmClient, fakeRepository)
        val messages = history(totalMessages)

        val result = strategy.buildContext(messages)

        // No new summarize call — uses cached
        assertEquals(0, fakeLlmClient.capturedSummarizeMessages.size)
        assertTrue(result.first().content.contains("old summary"))
    }

    @Test
    fun `SummarizationStrategy falls back to full history when no summary and summarize throws`() = runTest {
        fakeRepository.seedChat(chatId)
        fakeLlmClient.summarizeException = RuntimeException("network error")
        val strategy = SummarizationStrategy(fakeLlmClient, fakeRepository)
        val messages = history(SummarizationStrategy.KEEP_LAST_N + SummarizationStrategy.SUMMARIZE_EVERY + 1)

        val result = strategy.buildContext(messages)

        assertEquals(messages, result)
    }

    @Test
    fun `SummarizationStrategy falls back to cached summary when re-summarize throws`() = runTest {
        fakeRepository.seedChat(chatId)
        fakeRepository.updateChatSummary(chatId, "cached summary", 0) // coveredCount=0 → uncovered >= threshold
        fakeLlmClient.summarizeException = RuntimeException("network error")
        val strategy = SummarizationStrategy(fakeLlmClient, fakeRepository)
        val messages = history(SummarizationStrategy.KEEP_LAST_N + SummarizationStrategy.SUMMARIZE_EVERY + 1)

        val result = strategy.buildContext(messages)

        assertTrue(result.first().content.contains("cached summary"))
        assertEquals(Role.USER, result.first().role)
    }

    @Test
    fun `SummarizationStrategy result starts with USER summary followed by ASSISTANT`() = runTest {
        fakeRepository.seedChat(chatId)
        fakeLlmClient.summarizeResult = "test summary"
        val strategy = SummarizationStrategy(fakeLlmClient, fakeRepository)
        val messages = history(SummarizationStrategy.KEEP_LAST_N + SummarizationStrategy.SUMMARIZE_EVERY + 1)

        val result = strategy.buildContext(messages)

        assertEquals(Role.USER, result[0].role)
        assertEquals(Role.ASSISTANT, result[1].role)
    }
}
