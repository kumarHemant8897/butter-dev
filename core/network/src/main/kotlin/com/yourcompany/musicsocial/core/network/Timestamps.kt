package com.yourcompany.musicsocial.core.network

import java.time.Instant
import java.time.OffsetDateTime

private val SHORT_OFFSET = Regex("[+-]\\d{2}$")

/**
 * Postgres `timestamptz` as sent by PostgREST and Realtime, e.g. "2026-11-11T10:00:00.123456+00:00".
 * Also accepts a space instead of "T", a short "+00" offset and a "Z" suffix. Returns null for
 * anything unreadable so callers can skip one bad row instead of failing a whole list.
 */
fun parseSupabaseTimestamp(value: String?): Instant? {
    if (value.isNullOrBlank()) return null
    var text = value.trim().replace(' ', 'T')
    if (SHORT_OFFSET.containsMatchIn(text)) text += ":00"
    return runCatching { OffsetDateTime.parse(text).toInstant() }
        .recoverCatching { Instant.parse(text) }
        .getOrNull()
}
