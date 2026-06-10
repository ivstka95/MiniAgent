package karpiuk.ivan.miniagent.domain.agent

import karpiuk.ivan.miniagent.domain.model.Message
import karpiuk.ivan.miniagent.domain.model.Role
import karpiuk.ivan.miniagent.testing.FakeChatRepository
import karpiuk.ivan.miniagent.testing.FakeLlmClient
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AgentTest {
    private lateinit var repository: FakeChatRepository
    private lateinit var llmClient: FakeLlmClient
    private lateinit var agent: Agent

    @Before
    fun setUp() {
        repository = FakeChatRepository()
        llmClient = FakeLlmClient()
        agent = Agent(repository, llmClient)
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
    fun `generateTitle calls completePrompt with prompt containing user and assistant text`() = runTest {
        llmClient.result = LlmResult(assistantText = "Kotlin coroutines explained", inputTokens = 10, outputTokens = 5)

        agent.generateTitle("How do coroutines work?", "Coroutines are lightweight threads...")

        val prompt = llmClient.capturedPrompts.last()
        assertTrue(prompt.contains("How do coroutines work?"))
        assertTrue(prompt.contains("Coroutines are lightweight threads..."))
    }

    @Test
    fun `generateTitle returns trimmed llm response`() = runTest {
        llmClient.result = LlmResult(assistantText = "  Kotlin coroutines explained  ", inputTokens = 10, outputTokens = 5)

        val title = agent.generateTitle("How do coroutines work?", "Coroutines are lightweight threads...")

        assertEquals("Kotlin coroutines explained", title)
    }
}
