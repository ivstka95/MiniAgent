package karpiuk.ivan.miniagent.domain.agent

import karpiuk.ivan.miniagent.domain.context.ContextStrategy
import karpiuk.ivan.miniagent.domain.context.ContextStrategyManager
import karpiuk.ivan.miniagent.domain.context.ContextStrategyType
import karpiuk.ivan.miniagent.domain.context.NoCompressionStrategy
import karpiuk.ivan.miniagent.domain.context.SummarizationStrategy
import karpiuk.ivan.miniagent.domain.model.Chat
import karpiuk.ivan.miniagent.domain.model.Message
import karpiuk.ivan.miniagent.domain.model.Role
import karpiuk.ivan.miniagent.testing.FakeChatRepository
import karpiuk.ivan.miniagent.testing.FakeLlmClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AgentTest {
    private lateinit var repository: FakeChatRepository
    private lateinit var llmClient: FakeLlmClient
    private lateinit var agent: Agent

    @Before
    fun setUp() {
        repository = FakeChatRepository()
        llmClient = FakeLlmClient()
        val strategyManager = ContextStrategyManager()
        val strategies: Map<ContextStrategyType, ContextStrategy> = mapOf(
            ContextStrategyType.NONE to NoCompressionStrategy(),
            ContextStrategyType.SUMMARIZATION to SummarizationStrategy(llmClient, repository),
        )
        agent = Agent(repository, llmClient, TestScope(UnconfinedTestDispatcher()), strategyManager, strategies)
    }

    @Test
    fun `persists user message before calling LlmClient`() = runTest {
        agent.sendMessage(chatId = "chat-1", userInput = "hello")

        val history = repository.getMessagesOnce("chat-1")
        assertEquals(1, history.count { it.role == Role.USER })
        assertEquals("hello", history.first { it.role == Role.USER }.content)
    }

    @Test
    fun `given empty chat sends only the new user message to LlmClient`() = runTest {
        agent.sendMessage(chatId = "chat-1", userInput = "first")

        val sent = llmClient.capturedMessages.last()
        assertEquals(1, sent.size)
        assertEquals(Role.USER, sent[0].role)
        assertEquals("first", sent[0].content)
    }

    @Test
    fun `sends full history including new user message to LlmClient`() = runTest {
        repository.addMessage(
            Message(id = "m0", chatId = "chat-1", role = Role.USER, content = "previous", timestamp = 1_000L)
        )

        agent.sendMessage(chatId = "chat-1", userInput = "new message")

        val sent = llmClient.capturedMessages.last()
        assertEquals(2, sent.size)
        assertEquals("previous", sent[0].content)
        assertEquals("new message", sent[1].content)
    }

    @Test
    fun `persists assistant reply after LlmClient responds`() = runTest {
        llmClient.result = LlmResult(assistantText = "assistant reply", inputTokens = 10, outputTokens = 5)

        agent.sendMessage(chatId = "chat-1", userInput = "hello")

        val history = repository.getMessagesOnce("chat-1")
        val assistant = history.firstOrNull { it.role == Role.ASSISTANT }
        assertNotNull(assistant)
        assertEquals("assistant reply", assistant!!.content)
    }

    @Test
    fun `returns AgentResponse with reply text and token usage`() = runTest {
        llmClient.result = LlmResult(assistantText = "the reply", inputTokens = 100, outputTokens = 50, thinkingTokens = 10)

        val response = agent.sendMessage(chatId = "chat-1", userInput = "hello")

        assertEquals("the reply", response.replyText)
        assertEquals(100, response.inputTokens)
        assertEquals(50, response.outputTokens)
        assertEquals(10, response.thinkingTokens)
    }

    @Test
    fun `updates chat title after first exchange`() = runTest {
        repository.seedChat(Chat(id = "chat-1", title = "New chat", createdAt = 0L))
        llmClient.result = LlmResult(assistantText = "Coroutines are lightweight", inputTokens = 10, outputTokens = 5)

        agent.sendMessage(chatId = "chat-1", userInput = "How do coroutines work?")
        advanceUntilIdle()

        val title = repository.observeChats().first().first().title
        assertEquals("Coroutines are lightweight", title)
    }

    @Test
    fun `does not update chat title on subsequent exchanges`() = runTest {
        repository.seedChat(Chat(id = "chat-1", title = "Existing title", createdAt = 0L))
        repository.addMessage(Message(id = "m1", chatId = "chat-1", role = Role.USER, content = "first", timestamp = 1L))
        repository.addMessage(Message(id = "m2", chatId = "chat-1", role = Role.ASSISTANT, content = "answer", timestamp = 2L))

        agent.sendMessage(chatId = "chat-1", userInput = "follow-up")
        advanceUntilIdle()

        val title = repository.observeChats().first().first().title
        assertEquals("Existing title", title)
    }

    @Test
    fun `keeps New chat title and does not throw when title generation fails`() = runTest {
        repository.seedChat(Chat(id = "chat-1", title = "New chat", createdAt = 0L))
        llmClient.completePromptException = RuntimeException("API error")

        agent.sendMessage(chatId = "chat-1", userInput = "Hello")
        advanceUntilIdle()

        val title = repository.observeChats().first().first().title
        assertEquals("New chat", title)
    }
}
