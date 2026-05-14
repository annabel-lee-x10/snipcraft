package dev.a10101100.snipcraft.feature.diagnostics

import app.cash.turbine.test
import dev.a10101100.snipcraft.core.data.ExpansionHistoryRepository
import dev.a10101100.snipcraft.core.domain.ExpansionEvent
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DiagnosticsViewModelTest {

    private val historyRepo = mockk<ExpansionHistoryRepository>(relaxed = true)

    @BeforeEach fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @AfterEach fun tearDown() { Dispatchers.resetMain() }

    private fun fakeChecker(
        accessibilityEnabled: Boolean = false,
        foregroundRunning: Boolean = false,
        watchdogScheduled: Boolean = false,
        batteryExempt: Boolean = false,
        notificationsEnabled: Boolean = true,
        imePackage: String = "",
        onWatchdogTriggered: () -> Unit = {},
    ) = object : DiagnosticsChecker {
        override fun isAccessibilityEnabled() = accessibilityEnabled
        override fun isForegroundRunning() = foregroundRunning
        override fun isWatchdogScheduled() = watchdogScheduled
        override fun isBatteryExempt() = batteryExempt
        override fun areNotificationsEnabled() = notificationsEnabled
        override fun currentImePackage() = imePackage
        override fun triggerWatchdog() = onWatchdogTriggered()
    }

    @Test
    fun `initial state reflects service checks`() = runTest {
        every { historyRepo.observeRecent() } returns flowOf(emptyList())
        val vm = DiagnosticsViewModel(
            checker = fakeChecker(accessibilityEnabled = true, batteryExempt = true),
            historyRepository = historyRepo,
        )
        vm.uiState.test {
            val state = awaitItem()
            assertTrue(state.accessibilityEnabled)
            assertTrue(state.batteryExemptionGranted)
            assertFalse(state.foregroundServiceRunning)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `recent expansions are surfaced in state`() = runTest {
        val events = listOf(ExpansionEvent(1L, ";today", "com.gmail.android", 1000L, true, null))
        every { historyRepo.observeRecent() } returns flowOf(events)
        val vm = DiagnosticsViewModel(checker = fakeChecker(), historyRepository = historyRepo)
        vm.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.recentExpansions.size)
            assertEquals(";today", state.recentExpansions[0].shortcut)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ime package is surfaced in state`() = runTest {
        every { historyRepo.observeRecent() } returns flowOf(emptyList())
        val vm = DiagnosticsViewModel(
            checker = fakeChecker(imePackage = "com.google.android.inputmethod.latin"),
            historyRepository = historyRepo,
        )
        vm.uiState.test {
            assertEquals("com.google.android.inputmethod.latin", awaitItem().currentImePackage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `exportDiagnostics emits ShareDiagnostics event with json`() = runTest {
        every { historyRepo.observeRecent() } returns flowOf(emptyList())
        val vm = DiagnosticsViewModel(checker = fakeChecker(), historyRepository = historyRepo)
        vm.events.test {
            vm.exportDiagnostics()
            val event = awaitItem() as DiagnosticsEvent.ShareDiagnostics
            assertTrue(event.json.contains("diagnostics"))
            assertTrue(event.json.contains("appVersion"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `triggerWatchdog delegates to checker`() = runTest {
        every { historyRepo.observeRecent() } returns flowOf(emptyList())
        var triggered = false
        val vm = DiagnosticsViewModel(
            checker = fakeChecker(onWatchdogTriggered = { triggered = true }),
            historyRepository = historyRepo,
        )
        vm.triggerWatchdog()
        assertTrue(triggered)
    }
}
