package dev.a10101100.snipcraft.feature.settings

import dev.a10101100.snipcraft.core.backup.BackupManager
import dev.a10101100.snipcraft.core.backup.ConflictStrategy
import dev.a10101100.snipcraft.core.backup.ImportResult
import dev.a10101100.snipcraft.core.data.CompatibilityRuleRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelBlacklistTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val mockCompatibilityRepo = mockk<CompatibilityRuleRepository>(relaxed = true)
    private val mockBackupManager = mockk<BackupManager>(relaxed = true)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { mockCompatibilityRepo.observeBlacklistedPackages() } returns flowOf(emptyList())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun makeViewModel(): SettingsViewModel =
        SettingsViewModel(
            isServiceEnabled = false,
            compatibilityRuleRepository = mockCompatibilityRepo,
            backupManager = mockBackupManager,
        )

    @Test
    fun `blacklisted packages loaded from repository on init`() = runTest {
        every { mockCompatibilityRepo.observeBlacklistedPackages() } returns flowOf(listOf("com.example.app"))
        val vm = makeViewModel()

        assertEquals(listOf("com.example.app"), vm.uiState.value.blacklistedPackages)
    }

    @Test
    fun `removeFromBlacklist calls repository`() = runTest {
        val vm = makeViewModel()
        vm.removeFromBlacklist("com.example.app")
        coVerify { mockCompatibilityRepo.removeFromBlacklist("com.example.app") }
    }

    @Test
    fun `addToBlacklist calls repository`() = runTest {
        val vm = makeViewModel()
        vm.addToBlacklist("com.new.app")
        coVerify { mockCompatibilityRepo.addToBlacklist("com.new.app") }
    }

    @Test
    fun `onExport calls backupManager and emits ShareExport event`() = runTest {
        val fakeJson = """{"version":1,"exportedAt":"","snippets":[],"folders":[]}"""
        coEvery { mockBackupManager.export() } returns fakeJson
        val vm = makeViewModel()
        val events = mutableListOf<SettingsEvent>()
        val job = launch(testDispatcher) { vm.events.collect { events.add(it) } }

        vm.onExport()
        job.cancel()

        assertTrue(events.any { it is SettingsEvent.ShareExport })
        val shareEvent = events.filterIsInstance<SettingsEvent.ShareExport>().first()
        assertEquals(fakeJson, shareEvent.json)
    }

    @Test
    fun `onImportJsonReceived shows conflict dialog`() = runTest {
        val vm = makeViewModel()
        val json = """{"version":1,"exportedAt":"","snippets":[],"folders":[]}"""

        vm.onImportJsonReceived(json)

        assertTrue(vm.uiState.value.showImportConflictDialog)
    }

    @Test
    fun `onImportConflictResolved with SKIP calls backupManager and emits result`() = runTest {
        val vm = makeViewModel()
        val json = """{"version":1,"exportedAt":"","snippets":[],"folders":[]}"""
        coEvery { mockBackupManager.import(json, ConflictStrategy.SKIP_EXISTING) } returns
            ImportResult(snippetsImported = 2, foldersImported = 0, skipped = 1)
        vm.onImportJsonReceived(json)

        val events = mutableListOf<SettingsEvent>()
        val job = launch(testDispatcher) { vm.events.collect { events.add(it) } }
        vm.onImportConflictResolved(ConflictStrategy.SKIP_EXISTING)
        job.cancel()

        assertFalse(vm.uiState.value.showImportConflictDialog)
        assertTrue(events.any { it is SettingsEvent.ShowImportResult })
    }
}
