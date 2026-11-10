package com.yourcompany.musicsocial.core.common.logging

/**
 * Removes credentials and personal data from log text before it reaches any log sink
 * (Logcat in debug, the crash reporter in release).
 *
 * This is a safety net, not a licence to log sensitive values: callers must still avoid
 * passing passwords or tokens to the logger in the first place.
 */
object LogRedactor {

    private const val REDACTED = "[REDACTED]"

    private val jwt = Regex("eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+")
    private val bearer = Regex("(?i)\\bbearer\\s+[A-Za-z0-9._~+/=-]+")
    private val secretPair = Regex(
        "(?i)(\"?(?:password|passwd|access_token|refresh_token|token|apikey|api_key|secret|authorization)\"?\\s*[:=]\\s*\"?)(?!bearer\\s)[^\"&,\\s}]+",
    )
    private val email = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")

    fun redact(message: String): String = message
        .replace(jwt, REDACTED)
        .replace(bearer, "Bearer $REDACTED")
        .replace(secretPair) { it.groupValues[1] + REDACTED }
        .replace(email, "[REDACTED_EMAIL]")
}
