package dev.a10101100.snipcraft.feature.settings

import app.cash.turbine.test
import dev.a10101100.snipcraft.core.backup.BackupManager
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val mockBackupManager = mockk<BackupManager>(relaxed = true)

    @BeforeEach
    fun setUp() { Dispatchers.setMain(testDispatcher) }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `initial state shows service as disabled by default`() = runTest {
        val vm = SettingsViewModel(isServiceEnabled = false, backupManager = mockBackupManager)
        vm.uiState.test {
            assertFalse(awaitItem().isAccessibilityServiceEnabled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `theme toggle cycles through modes`() = runTest {
        val vm = SettingsViewModel(isServiceEnabled = false, backupManager = mockBackupManager)
        vm.uiState.test {
            assertEquals(ThemeMode.SYSTEM, awaitItem().themeMode)
            cancelAndIgnoreRemainingEvents()
        }
        vm.onThemeModeChange(ThemeMode.DARK)
        vm.uiState.test {
            assertEquals(ThemeMode.DARK, awaitItem().themeMode)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
