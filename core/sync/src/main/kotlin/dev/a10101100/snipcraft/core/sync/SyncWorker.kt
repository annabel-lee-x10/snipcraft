package dev.a10101100.snipcraft.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.a10101100.snipcraft.core.common.AppLogger
import java.io.IOException
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncEngine: SyncEngine,
    private val configStore: SyncConfigStore,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val config = configStore.load() ?: run {
            AppLogger.i("SyncWorker: no config saved, skipping")
            return Result.success()
        }
        return try {
            val result = syncEngine.sync(config)
            if (result.isSuccess) {
                AppLogger.i("SyncWorker: %s", result.summary())
                Result.success()
            } else {
                AppLogger.w("SyncWorker: %s", result.errors.firstOrNull())
                Result.retry()
            }
        } catch (e: IOException) {
            AppLogger.w("SyncWorker: network error — %s", e.message)
            Result.retry()
        } catch (e: Exception) {
            AppLogger.e(e, "SyncWorker: unexpected error — %s", e.message)
            Result.failure()
        }
    }

    companion object {
        internal const val WORK_NAME = "snipcraft_sync"

        fun schedule(context: Context, intervalHours: Int) {
            val wm = WorkManager.getInstance(context)
            if (intervalHours <= 0) {
                wm.cancelUniqueWork(WORK_NAME)
                return
            }
            val request = PeriodicWorkRequestBuilder<SyncWorker>(
                intervalHours.toLong(), TimeUnit.HOURS
            ).build()
            wm.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        fun triggerNow(context: Context) {
            WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<SyncWorker>().build())
        }
    }
}
