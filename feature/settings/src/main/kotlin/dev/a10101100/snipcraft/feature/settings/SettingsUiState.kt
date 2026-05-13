package dev.a10101100.snipcraft.feature.settings

import dev.a10101100.snipcraft.core.backup.ImportResult

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class SettingsUiState(
    val isAccessibilityServiceEnabled: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appVersion: String = "",
    val blacklistedPackages: List<String> = emptyList(),
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val showImportConflictDialog: Boolean = false,
    val pendingImportJson: String? = null,
    val importResult: ImportResult? = null,
)

sealed interface SettingsEvent {
    data class ShareExport(val json: String) : SettingsEvent
    data class ShowImportResult(val result: ImportResult) : SettingsEvent
}
