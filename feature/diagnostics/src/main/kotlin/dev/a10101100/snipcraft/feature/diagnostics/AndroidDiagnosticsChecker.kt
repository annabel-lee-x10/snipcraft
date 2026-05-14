package dev.a10101100.snipcraft.feature.diagnostics

import android.app.ActivityManager
import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dev.a10101100.snipcraft.core.accessibility.HealthWatchdogWorker
import dev.a10101100.snipcraft.core.accessibility.ServiceHealthChecker
import dev.a10101100.snipcraft.core.accessibility.SnipForegroundService
import javax.inject.Inject

class AndroidDiagnosticsChecker @Inject constructor(
    private val context: Context,
) : DiagnosticsChecker {

    override fun isAccessibilityEnabled() =
        ServiceHealthChecker(context).isServiceEnabled()

    override fun isForegroundRunning(): Boolean {
        @Suppress("DEPRECATION")
        return (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
            .getRunningServices(100)
            .any { it.service.className == SnipForegroundService::class.java.name }
    }

    override fun isWatchdogScheduled(): Boolean = try {
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork("snipcraft_health_watchdog")
            .get()
            .any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
    } catch (_: Exception) { false }

    override fun isBatteryExempt(): Boolean =
        (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .isIgnoringBatteryOptimizations(context.packageName)

    override fun areNotificationsEnabled(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    override fun currentImePackage(): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.substringBefore("/") ?: ""

    override fun triggerWatchdog() = HealthWatchdogWorker.schedule(context)
}
