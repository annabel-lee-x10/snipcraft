package dev.a10101100.snipcraft.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.a10101100.snipcraft.core.accessibility.ServiceHealthChecker
import dev.a10101100.snipcraft.core.backup.BackupManager
import dev.a10101100.snipcraft.core.backup.ConflictStrategy
import dev.a10101100.snipcraft.core.data.CompatibilityRuleRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val compatibilityRuleRepository: CompatibilityRuleRepository,
    private val backupManager: BackupManager,
) : ViewModel() {

    // Secondary constructor for tests — accepts all dependencies explicitly
    internal constructor(
        isServiceEnabled: Boolean,
        compatibilityRuleRepository: CompatibilityRuleRepository = NoOpCompatibilityRuleRepository,
        backupManager: BackupManager,
    ) : this(
        context = object : android.content.ContextWrapper(null) {
            override fun getSystemService(name: String): Any? = null
        },
        compatibilityRuleRepository = compatibilityRuleRepository,
        backupManager = backupManager,
    ) {
        _uiState.update { it.copy(isAccessibilityServiceEnabled = isServiceEnabled) }
    }

    // Minimal no-op used when blacklist isn't under test
    private object NoOpCompatibilityRuleRepository : CompatibilityRuleRepository {
        override fun observeBlacklistedPackages(): Flow<List<String>> = flowOf(emptyList())
        override suspend fun addToBlacklist(packageName: String) {}
        override suspend fun removeFromBlacklist(packageName: String) {}
    }

    private val healthChecker by lazy { ServiceHealthChecker(context) }

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        refreshServiceStatus()
        viewModelScope.launch {
            compatibilityRuleRepository.observeBlacklistedPackages().collect { packages ->
                _uiState.update { it.copy(blacklistedPackages = packages) }
            }
        }
    }

    fun refreshServiceStatus() {
        try {
            _uiState.update { it.copy(isAccessibilityServiceEnabled = healthChecker.isServiceEnabled()) }
        } catch (_: Exception) {
            // Context is null in test secondary constructor — ignore
        }
    }

    fun onThemeModeChange(mode: ThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun addToBlacklist(packageName: String) {
        viewModelScope.launch { compatibilityRuleRepository.addToBlacklist(packageName) }
    }

    fun removeFromBlacklist(packageName: String) {
        viewModelScope.launch { compatibilityRuleRepository.removeFromBlacklist(packageName) }
    }

    fun onExport() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            runCatching { backupManager.export() }
                .onSuccess { json -> _events.send(SettingsEvent.ShareExport(json)) }
            _uiState.update { it.copy(isExporting = false) }
        }
    }

    fun onImportJsonReceived(json: String) {
        _uiState.update { it.copy(showImportConflictDialog = true, pendingImportJson = json) }
    }

    fun onImportConflictResolved(strategy: ConflictStrategy) {
        val json = _uiState.value.pendingImportJson ?: return
        _uiState.update { it.copy(showImportConflictDialog = false, pendingImportJson = null, isImporting = true) }
        viewModelScope.launch {
            val result = runCatching { backupManager.import(json, strategy) }.getOrNull()
            _uiState.update { it.copy(isImporting = false, importResult = result) }
            result?.let { _events.send(SettingsEvent.ShowImportResult(it)) }
        }
    }

    fun dismissImportResult() {
        _uiState.update { it.copy(importResult = null) }
    }
}
