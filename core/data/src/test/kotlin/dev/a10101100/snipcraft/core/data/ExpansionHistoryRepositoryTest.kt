package dev.a10101100.snipcraft.core.data

import app.cash.turbine.test
import dev.a10101100.snipcraft.core.database.ExpansionHistoryDao
import dev.a10101100.snipcraft.core.database.ExpansionHistoryEntity
import dev.a10101100.snipcraft.core.domain.ExpansionEvent
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ExpansionHistoryRepositoryTest {

    private val dao = mockk<ExpansionHistoryDao>(relaxed = true)
    private lateinit var repo: ExpansionHistoryRepository

    @BeforeEach
    fun setUp() {
        repo = ExpansionHistoryRepositoryImpl(dao)
    }

    @Test
    fun `logExpansion inserts entity via dao`() = runTest {
        repo.logExpansion(";today", "com.gmail.android", 1000L, true, null)
        coVerify {
            dao.insert(
                match {
                    it.shortcut == ";today" &&
                    it.packageName == "com.gmail.android" &&
                    it.timestamp == 1000L &&
                    it.success &&
                    it.errorReason == null
                }
            )
        }
    }

    @Test
    fun `observeRecent maps entity to domain model`() = runTest {
        val entity = ExpansionHistoryEntity(
            id = 1L,
            shortcut = ";sig",
            packageName = "com.slack",
            timestamp = 2000L,
            success = true,
            errorReason = null,
        )
        every { dao.observeLast50() } returns flowOf(listOf(entity))

        repo.observeRecent().test {
            val results = awaitItem()
            assertEquals(1, results.size)
            assertEquals(ExpansionEvent(1L, ";sig", "com.slack", 2000L, true, null), results[0])
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `logExpansion with failure stores errorReason`() = runTest {
        repo.logExpansion(";bad", "pkg", 3000L, false, "executor_failed")
        coVerify {
            dao.insert(match { !it.success && it.errorReason == "executor_failed" })
        }
    }
}
