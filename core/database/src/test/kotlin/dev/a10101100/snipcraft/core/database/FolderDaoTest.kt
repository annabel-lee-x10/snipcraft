package dev.a10101100.snipcraft.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FolderDaoTest {

    private lateinit var db: SnipcraftDatabase
    private lateinit var dao: FolderDao

    @Before
    fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, SnipcraftDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.folderDao()
    }

    @After
    fun teardown() { db.close() }

    @Test
    fun `upsert and observeAll`() = runTest {
        dao.upsert(FolderEntity(id = "f1", name = "Work", color = null, sortOrder = 0, createdAt = 0L, updatedAt = 0L))
        dao.observeAll().test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("Work", list.first().name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `delete removes folder`() = runTest {
        val folder = FolderEntity(id = "f2", name = "Personal", color = null, sortOrder = 1, createdAt = 0L, updatedAt = 0L)
        dao.upsert(folder)
        dao.delete(folder)
        dao.observeAll().test {
            assertEquals(0, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
