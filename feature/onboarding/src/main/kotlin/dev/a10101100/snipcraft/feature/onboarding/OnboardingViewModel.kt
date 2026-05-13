package dev.a10101100.snipcraft.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.a10101100.snipcraft.core.accessibility.ServiceHealthChecker
import dev.a10101100.snipcraft.core.data.SnippetRepository
import dev.a10101100.snipcraft.core.domain.ExpansionContext
import dev.a10101100.snipcraft.core.engine.TrieMatcher
import dev.a10101100.snipcraft.core.variables.VariableEngine
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val serviceHealthChecker: ServiceHealthChecker,
    private val snippetRepository: SnippetRepository,
    private val variableEngine: VariableEngine,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _events = Channel<OnboardingEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val sandboxTrie = TrieMatcher()
    private var shortcutToBody: Map<String, String> = emptyMap()

    init {
        val enabled = runCatching { serviceHealthChecker.isServiceEnabled() }.getOrDefault(false)
        _uiState.update { it.copy(accessibilityGranted = enabled) }

        viewModelScope.launch {
            val snippets = snippetRepository.observeEnabled().first()
            shortcutToBody = snippets.associate { it.shortcut to it.body }
            sandboxTrie.rebuild(snippets.map { it.shortcut })
        }
    }

    fun checkPermissionsOnResume() {
        val enabled = runCatching { serviceHealthChecker.isServiceEnabled() }.getOrDefault(false)
        if (enabled && !_uiState.value.accessibilityGranted) {
            _uiState.update { it.copy(accessibilityGranted = true) }
        }
        if (enabled && _uiState.value.currentCard == 0) {
            _uiState.update { it.copy(currentCard = 1) }
        }
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(notificationGranted = granted) }
        if (granted && _uiState.value.currentCard == 1) {
            _uiState.update { it.copy(currentCard = 2) }
        }
    }

    fun onBatteryExemptGranted(granted: Boolean) {
        _uiState.update { it.copy(batteryExemptGranted = granted) }
    }

    fun onSkip() {
        val current = _uiState.value.currentCard
        if (current < TOTAL_CARDS - 1) {
            _uiState.update { it.copy(currentCard = current + 1) }
        }
    }

    fun onSandboxTextChanged(newText: String) {
        _uiState.update { it.copy(sandboxText = newText, sandboxExpanded = false) }
        if (newText.isEmpty() || newText.last() !in DELIMITERS) return

        val textBeforeDelim = newText.dropLast(1)
        val matchedShortcut = sandboxTrie.matchSuffix(textBeforeDelim) ?: return
        val body = shortcutToBody[matchedShortcut] ?: return

        viewModelScope.launch {
            val ctx = ExpansionContext(
                packageName = "dev.a10101100.snipcraft",
                fieldText = textBeforeDelim,
                cursorPosition = textBeforeDelim.length,
            )
            val expanded = variableEngine.resolve(body, ctx)
            _uiState.update { it.copy(sandboxText = expanded, sandboxExpanded = true) }
        }
    }

    fun onComplete() {
        viewModelScope.launch { _events.send(OnboardingEvent.NavigateToLibrary) }
    }

    companion object {
        private val DELIMITERS = setOf(' ', '\t', '\n', '.', ',', '!', '?', ';', ':', ')', ']', '}', '\'', '"')
    }
}
