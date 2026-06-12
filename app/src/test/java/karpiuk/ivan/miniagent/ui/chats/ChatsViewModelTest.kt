package karpiuk.ivan.miniagent.ui.chats

import app.cash.turbine.test
import karpiuk.ivan.miniagent.testing.FakeChatRepository
import karpiuk.ivan.miniagent.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeChatRepository
    private lateinit var viewModel: ChatsViewModel

    @Before
    fun setUp() {
        fakeRepository = FakeChatRepository()
        viewModel = ChatsViewModel(fakeRepository)
    }

    @Test
    fun `chats emits updated list when repository adds a chat`() = runTest {
        fakeRepository.createChat("Test chat")

        viewModel.chats.test {
            val chats = awaitItem()
            assertEquals(1, chats.size)
            assertEquals("Test chat", chats.first().title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `createChat emits new chat id via newChatEvent`() = runTest {
        viewModel.newChatEvent.test {
            viewModel.createChat()
            val emittedId = awaitItem()
            // Verify the emitted id belongs to a chat that was actually created
            fakeRepository.observeChats().test {
                val chats = awaitItem()
                assertTrue(chats.any { it.id == emittedId })
                cancelAndIgnoreRemainingEvents()
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleteChat removes chat from repository`() = runTest {
        val chat = fakeRepository.createChat("To delete")

        viewModel.chats.test {
            // Consume current state with the chat present
            val before = awaitItem()
            assertTrue(before.any { it.id == chat.id })

            viewModel.deleteChat(chat.id)

            val after = awaitItem()
            assertFalse(after.any { it.id == chat.id })
            cancelAndIgnoreRemainingEvents()
        }
    }
}
