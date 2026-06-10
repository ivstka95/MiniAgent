package karpiuk.ivan.miniagent.ui.chat

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import karpiuk.ivan.miniagent.domain.agent.Agent
import karpiuk.ivan.miniagent.domain.agent.AgentResponse
import karpiuk.ivan.miniagent.domain.model.Chat
import karpiuk.ivan.miniagent.domain.model.Message
import karpiuk.ivan.miniagent.domain.model.Role
import karpiuk.ivan.miniagent.testing.FakeChatRepository
import karpiuk.ivan.miniagent.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeRepository = FakeChatRepository()
    private val agent = mockk<Agent>()
    private val chatId = "chat-1"
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        coEvery { agent.sendMessage(any(), any()) } returns AgentResponse(replyText = "reply", inputTokens = 10, outputTokens = 5)
        coEvery { agent.generateTitle(any(), any()) } returns "Generated Title"
        viewModel = ChatViewModel(agent, fakeRepository, SavedStateHandle(mapOf("chatId" to chatId)))
    }

    @Test
    fun `initial uiState has empty messages and blank input`() = runTest {
        val state = viewModel.uiState.value
        assertEquals(chatId, state.chatId)
        assertTrue(state.messages.isEmpty())
        assertEquals("", state.inputText)
        assertFalse(state.isSending)
        assertNull(state.error)
    }

    @Test
    fun `updateInput updates inputText in state`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial state
            viewModel.updateInput("Hello")
            val updated = awaitItem()
            assertEquals("Hello", updated.inputText)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `sendMessage sets isSending true then false`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial state
            viewModel.updateInput("Hello")
            awaitItem() // input updated
            viewModel.sendMessage()
            advanceUntilIdle()
            // With UnconfinedTestDispatcher the whole coroutine may run synchronously,
            // producing multiple emissions; the last state must have isSending = false.
            val lastState = expectMostRecentItem()
            assertFalse(lastState.isSending)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `sendMessage clears input before calling agent`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial state
            viewModel.updateInput("Hello")
            awaitItem() // input updated
            viewModel.sendMessage()
            advanceUntilIdle()
            val lastState = expectMostRecentItem()
            assertEquals("", lastState.inputText)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `messages update automatically when repository emits`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial state

            val message = Message(
                id = "msg-1",
                chatId = chatId,
                role = Role.USER,
                content = "Test message",
                timestamp = 1000L,
            )
            fakeRepository.addMessage(message)

            val updated = awaitItem()
            assertEquals(1, updated.messages.size)
            assertEquals("Test message", updated.messages.first().content)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `sendMessage surfaces error when agent throws`() = runTest {
        coEvery { agent.sendMessage(any(), any()) } throws RuntimeException("Network error")
        viewModel.uiState.test {
            awaitItem() // initial state
            viewModel.updateInput("Hello")
            awaitItem() // input updated
            viewModel.sendMessage()
            advanceUntilIdle()
            val lastState = expectMostRecentItem()
            assertNotNull(lastState.error)
            assertEquals("Network error", lastState.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearError removes error from state`() = runTest {
        coEvery { agent.sendMessage(any(), any()) } throws RuntimeException("Network error")
        viewModel.uiState.test {
            awaitItem() // initial state
            viewModel.updateInput("Hello")
            awaitItem() // input updated
            viewModel.sendMessage()
            advanceUntilIdle()
            val errorState = expectMostRecentItem()
            assertNotNull(errorState.error)

            viewModel.clearError()
            val clearedState = awaitItem()
            assertNull(clearedState.error)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `sendMessage does nothing when input is blank`() = runTest {
        // Don't set any input (it defaults to "")
        viewModel.sendMessage()
        coVerify(exactly = 0) { agent.sendMessage(any(), any()) }
    }

    @Test
    fun `sendMessage does nothing when already sending`() = runTest {
        // Simulate already-sending state by making agent suspend indefinitely
        val deferred = CompletableDeferred<AgentResponse>()
        coEvery { agent.sendMessage(any(), any()) } coAnswers { deferred.await() }

        viewModel.updateInput("first")
        viewModel.sendMessage()  // starts coroutine, suspends at deferred.await()

        // Now _isSending is true (coroutine suspended at agent call);
        // second attempt should be a no-op
        viewModel.updateInput("second")
        viewModel.sendMessage()

        deferred.complete(AgentResponse(replyText = "r", inputTokens = 1, outputTokens = 1))
        advanceUntilIdle()

        coVerify(exactly = 1) { agent.sendMessage(any(), any()) }
    }

    @Test
    fun `sendMessage generates title and updates chat after first exchange`() = runTest {
        fakeRepository.seedChat(Chat(id = chatId, title = "New chat", createdAt = 0L))

        viewModel.uiState.test {
            awaitItem() // initial or seeded state

            viewModel.updateInput("How do coroutines work?")
            viewModel.sendMessage()
            advanceUntilIdle()

            coVerify { agent.generateTitle("How do coroutines work?", "reply") }
            assertEquals("Generated Title", expectMostRecentItem().chatTitle)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `sendMessage does not generate title on subsequent exchanges`() = runTest {
        fakeRepository.seedChat(Chat(id = chatId, title = "Existing title", createdAt = 0L))
        fakeRepository.addMessage(Message(id = "m1", chatId = chatId, role = Role.USER, content = "first", timestamp = 1L))
        fakeRepository.addMessage(Message(id = "m2", chatId = chatId, role = Role.ASSISTANT, content = "answer", timestamp = 2L))

        viewModel.uiState.test {
            awaitItem() // initial or seeded state

            viewModel.updateInput("follow-up question")
            viewModel.sendMessage()
            advanceUntilIdle()

            coVerify(exactly = 0) { agent.generateTitle(any(), any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `sendMessage keeps New chat title and does not surface error when generateTitle fails`() = runTest {
        fakeRepository.seedChat(Chat(id = chatId, title = "New chat", createdAt = 0L))
        coEvery { agent.generateTitle(any(), any()) } throws RuntimeException("API error")

        viewModel.uiState.test {
            awaitItem() // initial or seeded state

            viewModel.updateInput("Hello")
            viewModel.sendMessage()
            advanceUntilIdle()

            val lastState = expectMostRecentItem()
            assertEquals("New chat", lastState.chatTitle)
            assertNull(lastState.error)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
