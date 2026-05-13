package dev.a10101100.snipcraft.core.accessibility

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Robolectric

@RunWith(RobolectricTestRunner::class)
class SnipForegroundServiceTest {

    @Test
    fun `service can be created without crashing`() {
        val controller = Robolectric.buildService(SnipForegroundService::class.java)
        val service = controller.create().get()
        assertNotNull(service)
        controller.destroy()
    }

    @Test
    fun `notification channel is registered after service start`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val controller = Robolectric.buildService(SnipForegroundService::class.java)
        controller.create().startCommand(0, 1)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = nm.getNotificationChannel(NotificationChannels.FOREGROUND_SERVICE_CHANNEL_ID)
        assertNotNull(channel)
        controller.destroy()
    }
}
