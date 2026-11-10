package com.yourcompany.musicsocial.core.common.logging

import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/** [AppLogger] backed by Timber. Which trees are planted (debug vs release) is decided in the app module. */
@Singleton
internal class TimberAppLogger @Inject constructor() : AppLogger {

    override fun debug(category: LogCategory, message: String) {
        Timber.tag(category.tag).d(LogRedactor.redact(message))
    }

    override fun info(category: LogCategory, message: String) {
        Timber.tag(category.tag).i(LogRedactor.redact(message))
    }

    override fun warn(category: LogCategory, message: String, throwable: Throwable?) {
        Timber.tag(category.tag).w(throwable, LogRedactor.redact(message))
    }

    override fun error(category: LogCategory, message: String, throwable: Throwable?) {
        Timber.tag(category.tag).e(throwable, LogRedactor.redact(message))
    }
}
