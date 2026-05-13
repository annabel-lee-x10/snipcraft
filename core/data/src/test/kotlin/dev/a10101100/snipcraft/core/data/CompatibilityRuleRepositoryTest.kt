package dev.a10101100.snipcraft.core.data

import dev.a10101100.snipcraft.core.database.CompatibilityRuleDao
import dev.a10101100.snipcraft.core.database.CompatibilityRuleEntity
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.coEvery
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CompatibilityRuleRepositoryTest {

    private val dao = mockk<CompatibilityRuleDao>(relaxed = true)
    private val repo = CompatibilityRuleRepositoryImpl(dao)

    @Test
    fun `observeBlacklistedPackages returns package names from dao`() = runTest {
        val entity = CompatibilityRuleEntity(
            id = "1",
            packageName = "com.example.app",
            strategy = "DISABLED",
            triggerOverride = null,
            delayMs = 0,
            notes = null,
            isBuiltIn = false,
            updatedAt = 0L,
        )
        every { dao.observeUserBlacklist() } returns flowOf(listOf(entity))

        val packages = repo.observeBlacklistedPackages().first()

        assertEquals(listOf("com.example.app"), packages)
    }

    @Test
    fun `addToBlacklist upserts record with DISABLED strategy and isBuiltIn false`() = runTest {
        val captured = slot<CompatibilityRuleEntity>()
        coEvery { dao.upsert(capture(captured)) } returns Unit

        repo.addToBlacklist("com.example.new")

        assertEquals("com.example.new", captured.captured.packageName)
        assertEquals("DISABLED", captured.captured.strategy)
        assertEquals(false, captured.captured.isBuiltIn)
    }

    @Test
    fun `removeFromBlacklist delegates to deleteByPackage`() = runTest {
        repo.removeFromBlacklist("com.example.app")

        coVerify { dao.deleteByPackage("com.example.app") }
    }
}
