package dev.a10101100.snipcraft.feature.diagnostics

import dev.a10101100.snipcraft.core.domain.ExpansionEvent

data class DiagnosticsUiState(
    val accessibilityEnabled: Boolean = false,
    val foregroundServiceRunning: Boolean = false,
    val watchdogScheduled: Boolean = false,
    val batteryExemptionGranted: Boolean = false,
    val notificationPermissionGranted: Boolean = false,
    val currentImePackage: String = "",
    val recentExpansions: List<ExpansionEvent> = emptyList(),
    val isExportingDiagnostics: Boolean = false,
)

sealed class DiagnosticsEvent {
    data class ShareDiagnostics(val json: String) : DiagnosticsEvent()
}
