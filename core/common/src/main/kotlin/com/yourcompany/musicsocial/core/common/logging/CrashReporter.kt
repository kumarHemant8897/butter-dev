package com.yourcompany.musicsocial.core.common.logging

import android.util.Log
import timber.log.Timber
import javax.inject.Inject

/**
 * Seam for a crash reporting SDK (Firebase Crashlytics, Sentry, ...).
 * No vendor is chosen yet, so [NoOpCrashReporter] is bound. Adding one later means writing an
 * implementation of this interface and changing a single Hilt binding.
 */
interface CrashReporter {
    fun log(message: String)
    fun recordNonFatal(throwable: Throwable)
    fun setUserId(userId: String?)
}

internal class NoOpCrashReporter @Inject constructor() : CrashReporter {
    override fun log(message: String) = Unit
    override fun recordNonFatal(throwable: Throwable) = Unit
    override fun setUserId(userId: String?) = Unit
}

/**
 * Timber tree planted in release builds: forwards warnings and errors to the [CrashReporter]
 * and drops verbose/debug/info logs entirely.
 */
class CrashReportingTree(private val crashReporter: CrashReporter) : Timber.Tree() {

    override fun isLoggable(tag: String?, priority: Int): Boolean = priority >= Log.WARN

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        crashReporter.log("${tag ?: "MS"}: $message")
        if (t != null && priority >= Log.ERROR) crashReporter.recordNonFatal(t)
    }
}
