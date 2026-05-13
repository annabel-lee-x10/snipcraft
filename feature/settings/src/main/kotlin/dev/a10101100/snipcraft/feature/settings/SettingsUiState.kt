package dev.a10101100.snipcraft.feature.settings

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class SettingsUiState(
    val isAccessibilityServiceEnabled: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appVersion: String = "",
)
