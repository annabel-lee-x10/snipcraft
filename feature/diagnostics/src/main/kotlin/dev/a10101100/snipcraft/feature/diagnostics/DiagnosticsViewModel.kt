package dev.a10101100.snipcraft.feature.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.a10101100.snipcraft.core.data.ExpansionHistoryRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import javax.inject.Inject

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val checker: DiagnosticsChecker,
    private val historyRepository: ExpansionHistoryRepository,
) : ViewModel() {

    private val _events = Channel<DiagnosticsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val uiState = combine(
        historyRepository.observeRecent(),
    ) { (expansions) ->
        DiagnosticsUiState(
            accessibilityEnabled = checker.isAccessibilityEnabled(),
            foregroundServiceRunning = checker.isForegroundRunning(),
            watchdogScheduled = checker.isWatchdogScheduled(),
            batteryExemptionGranted = checker.isBatteryExempt(),
            notificationPermissionGranted = checker.areNotificationsEnabled(),
            currentImePackage = checker.currentImePackage(),
            recentExpansions = expansions,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DiagnosticsUiState(
            accessibilityEnabled = checker.isAccessibilityEnabled(),
            foregroundServiceRunning = checker.isForegroundRunning(),
            watchdogScheduled = checker.isWatchdogScheduled(),
            batteryExemptionGranted = checker.isBatteryExempt(),
            notificationPermissionGranted = checker.areNotificationsEnabled(),
            currentImePackage = checker.currentImePackage(),
        ),
    )

    fun triggerWatchdog() {
        checker.triggerWatchdog()
    }

    fun exportDiagnostics() {
        viewModelScope.launch {
            val state = uiState.value
            val json = buildDiagnosticsJson(state)
            _events.send(DiagnosticsEvent.ShareDiagnostics(json))
        }
    }

    private fun buildDiagnosticsJson(state: DiagnosticsUiState): String {
        val obj = buildJsonObject {
            put("exportedAt", System.currentTimeMillis())
            put("appVersion", "0.1.0")
            put("deviceModel", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            put("androidVersion", android.os.Build.VERSION.SDK_INT)
            putJsonObject("diagnostics") {
                put("accessibilityEnabled", state.accessibilityEnabled)
                put("foregroundServiceRunning", state.foregroundServiceRunning)
                put("watchdogScheduled", state.watchdogScheduled)
                put("batteryExemptionGranted", state.batteryExemptionGranted)
                put("notificationPermissionGranted", state.notificationPermissionGranted)
                put("currentIme", state.currentImePackage)
            }
            putJsonArray("recentExpansions") {
                state.recentExpansions.take(50).forEach { event ->
                    add(buildJsonObject {
                        put("shortcut", event.shortcut)
                        put("packageName", event.packageName)
                        put("timestamp", event.timestamp)
                        put("success", event.success)
                        event.errorReason?.let { put("errorReason", it) }
                    })
                }
            }
        }
        return Json { prettyPrint = true }.encodeToString(obj)
    }
}
