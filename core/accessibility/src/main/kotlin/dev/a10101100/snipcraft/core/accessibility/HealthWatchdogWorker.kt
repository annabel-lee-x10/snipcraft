package dev.a10101100.snipcraft.core.accessibility

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dev.a10101100.snipcraft.core.common.AppLogger
import java.util.concurrent.TimeUnit

class HealthWatchdogWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val checker = ServiceHealthChecker(applicationContext)
        val enabled = checker.isServiceEnabled()
        AppLogger.i("HealthWatchdogWorker: service enabled = %b", enabled)

        if (!enabled) {
            notifyReEnable()
        }
        return Result.success()
    }

    private fun notifyReEnable() {
        NotificationChannels.createAll(applicationContext)
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val settingsIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            applicationContext,
            0,
            settingsIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(
            applicationContext,
            NotificationChannels.HEALTH_ALERT_CHANNEL_ID,
        )
            .setContentTitle("Snipcraft needs attention")
            .setContentText("Tap to re-enable text expansion in Accessibility settings")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val WORK_NAME = "snipcraft_health_watchdog"
        private const val NOTIFICATION_ID = 1002

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<HealthWatchdogWorker>(30, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
