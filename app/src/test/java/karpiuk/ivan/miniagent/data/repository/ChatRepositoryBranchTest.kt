package karpiuk.ivan.miniagent.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import karpiuk.ivan.miniagent.data.local.AppDatabase
import karpiuk.ivan.miniagent.domain.model.Message
import karpiuk.ivan.miniagent.domain.model.Role
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChatRepositoryBranchTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: ChatRepositoryImpl

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = ChatRepositoryImpl(db.chatDao(), db.messageDao())
    }

    @After
    fun tearDown() = db.close()

    /** Seeds a chat with 5 alternating messages m0..m4 (timestamps 0..4) and returns its id. */
    private suspend fun seedSourceChat(): String {
        val chat = repository.createChat("Source")
        repeat(5) { i ->
            repository.addMessage(
                Message(
                    id = "m$i",
                    chatId = chat.id,
                    role = if (i % 2 == 0) Role.USER else Role.ASSISTANT,
                    content = "msg$i",
                    timestamp = i.toLong(),
                    tokenCount = i,
                ),
            )
        }
        return chat.id
    }

    @Test
    fun `branch copies messages up to and including the checkpoint with new ids and new chatId`() = runTest {
        val sourceId = seedSourceChat()

        val branchId = repository.branchChatFromMessage(sourceId, "m2")

        val copied = repository.getMessagesOnce(branchId)
        assertEquals(3, copied.size) // m0, m1, m2
        assertEquals(listOf("msg0", "msg1", "msg2"), copied.map { it.content })
        // role + tokenCount preserved, in order
        assertEquals(listOf(Role.USER, Role.ASSISTANT, Role.USER), copied.map { it.role })
        assertEquals(listOf(0, 1, 2), copied.map { it.tokenCount })
        // new ids and new chatId
        copied.forEach { assertEquals(branchId, it.chatId) }
        assertTrue(copied.none { it.id in setOf("m0", "m1", "m2") })
        // branch title indicates a branch
        assertTrue(repository.getChatById(branchId)?.title?.endsWith("(branch)") == true)
    }

    @Test
    fun `branch leaves the source chat unchanged`() = runTest {
        val sourceId = seedSourceChat()

        repository.branchChatFromMessage(sourceId, "m2")

        val source = repository.getMessagesOnce(sourceId)
        assertEquals(5, source.size)
        assertEquals(listOf("m0", "m1", "m2", "m3", "m4"), source.map { it.id })
    }

    @Test
    fun `branching the same message twice yields two independent chats`() = runTest {
        val sourceId = seedSourceChat()

        val branch1 = repository.branchChatFromMessage(sourceId, "m2")
        val branch2 = repository.branchChatFromMessage(sourceId, "m2")

        assertNotEquals(branch1, branch2)
        assertEquals(3, repository.getMessagesOnce(branch1).size)
        assertEquals(3, repository.getMessagesOnce(branch2).size)

        // Continuing one branch does not affect the other.
        repository.addMessage(
            Message(
                id = "extra",
                chatId = branch1,
                role = Role.ASSISTANT,
                content = "only in branch1",
                timestamp = 100L,
            ),
        )
        assertEquals(4, repository.getMessagesOnce(branch1).size)
        assertEquals(3, repository.getMessagesOnce(branch2).size)
    }
}
