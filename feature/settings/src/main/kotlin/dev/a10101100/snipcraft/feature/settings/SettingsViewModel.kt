package dev.a10101100.snipcraft.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.a10101100.snipcraft.core.accessibility.ServiceHealthChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext context: Context,
) : ViewModel() {

    // Secondary constructor for tests (avoids Android context)
    internal constructor(isServiceEnabled: Boolean) : this(
        context = object : android.content.ContextWrapper(null) {
            override fun getSystemService(name: String): Any? = null
        }
    ) {
        _uiState.update { it.copy(isAccessibilityServiceEnabled = isServiceEnabled) }
    }

    private val healthChecker by lazy { ServiceHealthChecker(context) }

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refreshServiceStatus()
    }

    fun refreshServiceStatus() {
        try {
            _uiState.update { it.copy(isAccessibilityServiceEnabled = healthChecker.isServiceEnabled()) }
        } catch (_: Exception) {
            // Context might be null in test secondary constructor — ignore
        }
    }

    fun onThemeModeChange(mode: ThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
    }
}
