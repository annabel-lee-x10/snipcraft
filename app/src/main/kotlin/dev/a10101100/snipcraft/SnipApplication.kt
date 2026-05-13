package dev.a10101100.snipcraft

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import dev.a10101100.snipcraft.core.accessibility.HealthWatchdogWorker
import dev.a10101100.snipcraft.core.common.AppLogger
import javax.inject.Inject

@HiltAndroidApp
class SnipApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            AppLogger.plantDebugTree()
        } else {
            AppLogger.plantReleaseTree()
        }
        HealthWatchdogWorker.schedule(this)
    }
}
