package dev.a10101100.snipcraft.core.accessibility

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dev.a10101100.snipcraft.core.common.AppLogger

class SnipForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.createAll(this)
        AppLogger.d("SnipForegroundService: onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            AppLogger.i("SnipForegroundService: stop requested")
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildNotification(this)
        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        AppLogger.i("SnipForegroundService: started foreground")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        AppLogger.d("SnipForegroundService: onDestroy")
    }

    companion object {
        const val ACTION_STOP = "dev.a10101100.snipcraft.STOP_FOREGROUND_SERVICE"
        private const val NOTIFICATION_ID = 1001

        fun startIntent(context: Context) = Intent(context, SnipForegroundService::class.java)

        fun stopIntent(context: Context) =
            Intent(context, SnipForegroundService::class.java).apply {
                action = ACTION_STOP
            }

        private fun buildNotification(context: Context): Notification {
            val stopPendingIntent = PendingIntent.getService(
                context,
                0,
                stopIntent(context),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            return NotificationCompat.Builder(context, NotificationChannels.FOREGROUND_SERVICE_CHANNEL_ID)
                .setContentTitle("Snipcraft")
                .setContentText("Text expansion is active")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .addAction(0, "Stop", stopPendingIntent)
                .build()
        }
    }
}
