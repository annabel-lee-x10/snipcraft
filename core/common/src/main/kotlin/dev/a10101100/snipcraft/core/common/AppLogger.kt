package dev.a10101100.snipcraft.core.common

import timber.log.Timber

object AppLogger {

    fun plantDebugTree() {
        Timber.plant(Timber.DebugTree())
    }

    fun plantReleaseTree() {
        // No-op release tree — swallows all logs in release builds.
        Timber.plant(object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) = Unit
        })
    }

    fun d(message: String, vararg args: Any?) = Timber.d(message, *args)
    fun i(message: String, vararg args: Any?) = Timber.i(message, *args)
    fun w(message: String, vararg args: Any?) = Timber.w(message, *args)
    fun e(t: Throwable? = null, message: String, vararg args: Any?) = Timber.e(t, message, *args)
}
