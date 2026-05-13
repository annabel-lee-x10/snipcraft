package dev.a10101100.snipcraft.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SnippetDaoTest {

    private lateinit var db: SnipcraftDatabase
    private lateinit var dao: SnippetDao

    @Before
    fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, SnipcraftDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.snippetDao()
    }

    @After
    fun teardown() { db.close() }

    @Test
    fun `upsert and getByShortcut`() = runTest {
        val entity = buildSnippet(shortcut = "/sig", body = "John Smith")
        dao.upsert(entity)
        val result = dao.getByShortcut("/sig")
        assertNotNull(result)
        assertEquals("/sig", result!!.shortcut)
        assertEquals("John Smith", result.body)
    }

    @Test
    fun `getByShortcut returns null when not found`() = runTest {
        assertNull(dao.getByShortcut("/nope"))
    }

    @Test
    fun `observeEnabled emits only enabled snippets`() = runTest {
        dao.upsert(buildSnippet(id = "a", shortcut = "/on", isEnabled = true))
        dao.upsert(buildSnippet(id = "b", shortcut = "/off", isEnabled = false))
        dao.observeEnabled().test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("/on", list.first().shortcut)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `incrementUsage increases count`() = runTest {
        val entity = buildSnippet(id = "x", shortcut = "/x", usageCount = 0)
        dao.upsert(entity)
        dao.incrementUsage("x", timestamp = 1000L)
        val result = dao.getByShortcut("/x")
        assertEquals(1L, result!!.usageCount)
        assertEquals(1000L, result.lastUsedAt)
    }

    @Test
    fun `delete removes snippet`() = runTest {
        val entity = buildSnippet(shortcut = "/del")
        dao.upsert(entity)
        dao.delete(entity)
        assertNull(dao.getByShortcut("/del"))
    }

    private fun buildSnippet(
        id: String = "id-1",
        shortcut: String = "/s",
        body: String = "body",
        isEnabled: Boolean = true,
        usageCount: Long = 0L,
    ) = SnippetEntity(
        id = id,
        shortcut = shortcut,
        body = body,
        type = "PLAIN",
        triggerMode = "ON_DELIMITER",
        folderId = null,
        usageCount = usageCount,
        lastUsedAt = null,
        createdAt = 0L,
        updatedAt = 0L,
        isPinned = false,
        isEnabled = isEnabled,
        caseSensitive = false,
        description = null,
        schemaVersion = 1,
    )
}
