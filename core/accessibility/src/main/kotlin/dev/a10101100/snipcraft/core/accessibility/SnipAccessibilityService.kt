package dev.a10101100.snipcraft.core.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import dev.a10101100.snipcraft.core.common.AppLogger

class SnipAccessibilityService : AccessibilityService() {

    private val eventProcessor = AccessibilityEventProcessor()

    override fun onServiceConnected() {
        AppLogger.i("SnipAccessibilityService: connected — text expansion active")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val snapshot = eventProcessor.process(event) ?: return
        if (snapshot.isPassword) return   // Hard exclusion — never expand in password fields

        // Pass 4: wire snapshot → TrieMatcher → ExpansionEngine → CompatibilityStrategy
        AppLogger.d(
            "SnipAccessibilityService: event pkg=%s text=%s",
            snapshot.packageName,
            snapshot.text,
        )
    }

    override fun onInterrupt() {
        AppLogger.w("SnipAccessibilityService: interrupted")
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        AppLogger.w("SnipAccessibilityService: unbound — service will reconnect")
        return super.onUnbind(intent)
    }
}
