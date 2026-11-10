package com.yourcompany.musicsocial.core.common.logging

/**
 * The only logging entry point for app code. Every message is passed through [LogRedactor].
 *
 * [LogCategory] keeps logs filterable (`adb logcat -s MS/Network`) and lets the crash reporter
 * group non-fatal errors by subsystem.
 */
interface AppLogger {
    fun debug(category: LogCategory, message: String)
    fun info(category: LogCategory, message: String)
    fun warn(category: LogCategory, message: String, throwable: Throwable? = null)
    fun error(category: LogCategory, message: String, throwable: Throwable? = null)
}

enum class LogCategory(val tag: String) {
    App("MS/App"),
    Network("MS/Network"),
    Database("MS/Database"),
    Auth("MS/Auth"),
    Realtime("MS/Realtime"),
    Playback("MS/Playback"),
    Ui("MS/Ui"),
}
