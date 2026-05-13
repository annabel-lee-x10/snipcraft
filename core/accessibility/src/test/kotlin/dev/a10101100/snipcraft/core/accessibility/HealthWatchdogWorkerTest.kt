package dev.a10101100.snipcraft.core.accessibility

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HealthWatchdogWorkerTest {

    @Test
    fun `worker returns success when service is disabled (logs and notifies)`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val worker = TestListenableWorkerBuilder<HealthWatchdogWorker>(context).build()
        val result = worker.doWork()
        assertEquals(Result.success(), result)
    }
}
