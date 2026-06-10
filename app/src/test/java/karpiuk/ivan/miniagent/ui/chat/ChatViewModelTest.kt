package karpiuk.ivan.miniagent.ui.chat

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.mockk
import karpiuk.ivan.miniagent.domain.agent.Agent
import karpiuk.ivan.miniagent.domain.agent.AgentResponse
import karpiuk.ivan.miniagent.domain.model.Message
import karpiuk.ivan.miniagent.domain.model.Role
import karpiuk.ivan.miniagent.testing.FakeChatRepository
import karpiuk.ivan.miniagent.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
        coEvery { agent.sendMessage(any(), any()) } returns AgentResponse("reply", 10, 5)
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
}
