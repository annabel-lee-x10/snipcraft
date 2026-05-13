package dev.a10101100.snipcraft.core.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SyncWorkerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
    }

    @Test
    fun `schedule enqueues unique periodic work`() = runTest {
        SyncWorker.schedule(context, 6)

        val infos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(SyncWorker.WORK_NAME)
            .get()

        assertFalse("Expected work to be enqueued", infos.isEmpty())
    }

    @Test
    fun `schedule with 0 hours cancels existing work`() = runTest {
        SyncWorker.schedule(context, 6)
        SyncWorker.schedule(context, 0)

        val infos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(SyncWorker.WORK_NAME)
            .get()

        // After cancel, work list is empty or all cancelled
        assertTrue(infos.isEmpty() || infos.all {
            it.state == androidx.work.WorkInfo.State.CANCELLED
        })
    }

    @Test
    fun `cancel removes scheduled work`() = runTest {
        SyncWorker.schedule(context, 6)
        SyncWorker.cancel(context)

        val infos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(SyncWorker.WORK_NAME)
            .get()

        assertTrue(infos.isEmpty() || infos.all {
            it.state == androidx.work.WorkInfo.State.CANCELLED
        })
    }
}
