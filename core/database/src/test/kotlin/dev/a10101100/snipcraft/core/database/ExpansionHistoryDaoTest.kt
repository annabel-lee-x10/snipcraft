package dev.a10101100.snipcraft.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExpansionHistoryDaoTest {

    private lateinit var db: SnipcraftDatabase
    private lateinit var dao: ExpansionHistoryDao

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, SnipcraftDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.expansionHistoryDao()
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun `insert and observe last 50`() = runTest {
        val entity = ExpansionHistoryEntity(
            shortcut = ";today",
            packageName = "com.gmail.android",
            timestamp = 1000L,
            success = true,
            errorReason = null,
        )
        dao.insert(entity)
        dao.observeLast50().test {
            val results = awaitItem()
            assertEquals(1, results.size)
            assertEquals(";today", results[0].shortcut)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeLast50 returns at most 50 rows ordered by timestamp desc`() = runTest {
        repeat(60) { i ->
            dao.insert(
                ExpansionHistoryEntity(
                    shortcut = ";s$i",
                    packageName = "pkg",
                    timestamp = i.toLong(),
                    success = true,
                    errorReason = null,
                )
            )
        }
        dao.observeLast50().test {
            val results = awaitItem()
            assertEquals(50, results.size)
            assertTrue(results[0].timestamp > results[1].timestamp)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `insert failed expansion stores errorReason`() = runTest {
        dao.insert(
            ExpansionHistoryEntity(
                shortcut = ";test",
                packageName = "com.foo",
                timestamp = 999L,
                success = false,
                errorReason = "executor_failed",
            )
        )
        dao.observeLast50().test {
            val results = awaitItem()
            assertEquals("executor_failed", results[0].errorReason)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
