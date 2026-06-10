package karpiuk.ivan.miniagent.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import karpiuk.ivan.miniagent.data.local.AppDatabase
import karpiuk.ivan.miniagent.data.local.entity.ChatEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChatDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: ChatDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.chatDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `insert and observe chat`() = runTest {
        val chat = ChatEntity(id = "c1", title = "First", createdAt = 1000L)
        dao.insert(chat)

        val result = dao.observeChats().first()
        assertEquals(1, result.size)
        assertEquals("c1", result[0].id)
        assertEquals("First", result[0].title)
    }

    @Test
    fun `observe chats returns newest first`() = runTest {
        dao.insert(ChatEntity(id = "old", title = "Old", createdAt = 1000L))
        dao.insert(ChatEntity(id = "new", title = "New", createdAt = 2000L))

        val result = dao.observeChats().first()
        assertEquals("new", result[0].id)
        assertEquals("old", result[1].id)
    }

    @Test
    fun `delete removes chat`() = runTest {
        dao.insert(ChatEntity(id = "c1", title = "Test", createdAt = 1000L))
        dao.deleteById("c1")

        val result = dao.observeChats().first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `updateTitle changes title for existing chat`() = runTest {
        dao.insert(ChatEntity(id = "c1", title = "New chat", createdAt = 1_000L))

        dao.updateTitle("c1", "Kotlin coroutines explained")

        val result = dao.observeChats().first()
        assertEquals("Kotlin coroutines explained", result.first().title)
    }
}
