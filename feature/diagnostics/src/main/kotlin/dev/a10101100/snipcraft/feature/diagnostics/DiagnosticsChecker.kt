package dev.a10101100.snipcraft.feature.diagnostics

interface DiagnosticsChecker {
    fun isAccessibilityEnabled(): Boolean
    fun isForegroundRunning(): Boolean
    fun isWatchdogScheduled(): Boolean
    fun isBatteryExempt(): Boolean
    fun areNotificationsEnabled(): Boolean
    fun currentImePackage(): String
    fun triggerWatchdog()
}
