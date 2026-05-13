package dev.a10101100.snipcraft.core.accessibility

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {

    const val FOREGROUND_SERVICE_CHANNEL_ID = "snipcraft_fg_service"
    const val HEALTH_ALERT_CHANNEL_ID = "snipcraft_health_alert"

    fun createAll(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Foreground service channel — minimum importance so the notification is quiet
        nm.createNotificationChannel(
            NotificationChannel(
                FOREGROUND_SERVICE_CHANNEL_ID,
                "Snipcraft running",
                NotificationManager.IMPORTANCE_MIN,
            ).apply {
                description = "Persistent notification that keeps Snipcraft active"
                setShowBadge(false)
            }
        )

        // Health alert channel — default importance for re-enable prompts
        nm.createNotificationChannel(
            NotificationChannel(
                HEALTH_ALERT_CHANNEL_ID,
                "Snipcraft alerts",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Alerts when Snipcraft needs attention (e.g. re-enable after OEM kill)"
            }
        )
    }
}
