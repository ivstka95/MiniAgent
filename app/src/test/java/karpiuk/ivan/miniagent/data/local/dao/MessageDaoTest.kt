package karpiuk.ivan.miniagent.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import karpiuk.ivan.miniagent.data.local.AppDatabase
import karpiuk.ivan.miniagent.data.local.entity.ChatEntity
import karpiuk.ivan.miniagent.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MessageDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var messageDao: MessageDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        messageDao = db.messageDao()
        runTest {
            db.chatDao().insert(ChatEntity(id = "chat-1", title = "Test", createdAt = 0L))
        }
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `insert and observe messages in chronological order`() = runTest {
        messageDao.insert(MessageEntity(id = "m1", chatId = "chat-1", role = "user", content = "hello", timestamp = 1000L))
        messageDao.insert(MessageEntity(id = "m2", chatId = "chat-1", role = "assistant", content = "hi", timestamp = 2000L))

        val result = messageDao.observeMessages("chat-1").first()
        assertEquals(2, result.size)
        assertEquals("m1", result[0].id)
        assertEquals("m2", result[1].id)
    }

    @Test
    fun `getByChat returns only messages for the given chat`() = runTest {
        db.chatDao().insert(ChatEntity(id = "chat-2", title = "Other", createdAt = 0L))
        messageDao.insert(MessageEntity(id = "m1", chatId = "chat-1", role = "user", content = "a", timestamp = 1000L))
        messageDao.insert(MessageEntity(id = "m2", chatId = "chat-2", role = "user", content = "b", timestamp = 1000L))

        val result = messageDao.getByChat("chat-1")
        assertEquals(1, result.size)
        assertEquals("m1", result[0].id)
    }

    @Test
    fun `deleting chat cascades to messages`() = runTest {
        messageDao.insert(MessageEntity(id = "m1", chatId = "chat-1", role = "user", content = "a", timestamp = 1000L))
        db.chatDao().deleteById("chat-1")

        val result = messageDao.getByChat("chat-1")
        assertEquals(0, result.size)
    }
}
