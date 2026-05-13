package dev.a10101100.snipcraft.feature.onboarding

import dev.a10101100.snipcraft.core.accessibility.ServiceHealthChecker
import dev.a10101100.snipcraft.core.data.SnippetRepository
import dev.a10101100.snipcraft.core.domain.Snippet
import dev.a10101100.snipcraft.core.variables.DateVariableResolver
import dev.a10101100.snipcraft.core.variables.TimeVariableResolver
import dev.a10101100.snipcraft.core.variables.VariableEngine
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
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

class OnboardingViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val mockHealthChecker = mockk<ServiceHealthChecker>()
    private val mockSnippetRepo = mockk<SnippetRepository>()
    private val variableEngine = VariableEngine(listOf(DateVariableResolver(), TimeVariableResolver()))

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { mockSnippetRepo.observeEnabled() } returns flowOf(emptyList())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun makeViewModel(isServiceEnabled: Boolean = false): OnboardingViewModel {
        every { mockHealthChecker.isServiceEnabled() } returns isServiceEnabled
        return OnboardingViewModel(mockHealthChecker, mockSnippetRepo, variableEngine)
    }

    @Test
    fun `initial card is 0 and accessibility not granted when service disabled`() = runTest {
        val vm = makeViewModel(isServiceEnabled = false)
        assertEquals(0, vm.uiState.value.currentCard)
        assertFalse(vm.uiState.value.accessibilityGranted)
    }

    @Test
    fun `initial state marks accessibilityGranted when service already enabled`() = runTest {
        val vm = makeViewModel(isServiceEnabled = true)
        assertTrue(vm.uiState.value.accessibilityGranted)
    }

    @Test
    fun `checkPermissionsOnResume sets accessibilityGranted and advances from card 0`() = runTest {
        every { mockHealthChecker.isServiceEnabled() } returns false
        val vm = makeViewModel(isServiceEnabled = false)
        assertEquals(0, vm.uiState.value.currentCard)

        every { mockHealthChecker.isServiceEnabled() } returns true
        vm.checkPermissionsOnResume()

        assertTrue(vm.uiState.value.accessibilityGranted)
        assertEquals(1, vm.uiState.value.currentCard)
    }

    @Test
    fun `checkPermissionsOnResume does not advance when not on card 0`() = runTest {
        val vm = makeViewModel(isServiceEnabled = true)
        vm.onSkip()  // advance to card 1

        vm.checkPermissionsOnResume()

        assertEquals(1, vm.uiState.value.currentCard)
    }

    @Test
    fun `onSkip from card 0 advances to card 1`() = runTest {
        val vm = makeViewModel(isServiceEnabled = false)
        vm.onSkip()
        assertEquals(1, vm.uiState.value.currentCard)
    }

    @Test
    fun `onSkip from card 2 advances to card 3`() = runTest {
        val vm = makeViewModel(isServiceEnabled = false)
        vm.onSkip()
        vm.onSkip()
        vm.onSkip()
        assertEquals(3, vm.uiState.value.currentCard)
    }

    @Test
    fun `onNotificationPermissionResult granted sets notificationGranted and advances from card 1`() = runTest {
        val vm = makeViewModel(isServiceEnabled = false)
        vm.onSkip()  // → card 1

        vm.onNotificationPermissionResult(granted = true)

        assertTrue(vm.uiState.value.notificationGranted)
        assertEquals(2, vm.uiState.value.currentCard)
    }

    @Test
    fun `onSandboxTextChanged with semicolonToday space expands to date pattern`() = runTest {
        val snippet = Snippet(id = "s1", shortcut = ";today", body = "{{date:yyyy-MM-dd}}")
        every { mockSnippetRepo.observeEnabled() } returns flowOf(listOf(snippet))
        val vm = makeViewModel(isServiceEnabled = false)

        vm.onSandboxTextChanged(";today ")

        val sandboxText = vm.uiState.value.sandboxText
        assertTrue(
            sandboxText.matches(Regex("\\d{4}-\\d{2}-\\d{2}")),
            "Expected date pattern but got: $sandboxText"
        )
    }

    @Test
    fun `onSandboxTextChanged with non-matching text does not expand`() = runTest {
        val vm = makeViewModel(isServiceEnabled = false)
        vm.onSandboxTextChanged("hello")
        assertEquals("hello", vm.uiState.value.sandboxText)
    }

    @Test
    fun `onComplete emits NavigateToLibrary event`() = runTest {
        val vm = makeViewModel(isServiceEnabled = false)
        val events = mutableListOf<OnboardingEvent>()
        val job = launch(dispatcher) {
            vm.events.collect { events.add(it) }
        }

        vm.onComplete()
        job.cancel()

        assertTrue(events.any { it is OnboardingEvent.NavigateToLibrary })
    }
}
