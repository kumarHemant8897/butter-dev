package com.yourcompany.musicsocial.core.common.logging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LogRedactorTest {

    @Test
    fun `jwt access tokens are removed`() {
        val token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyIn0.c2lnbmF0dXJl"
        val redacted = LogRedactor.redact("session restored with $token")

        assertFalse(redacted.contains(token))
        assertEquals("session restored with [REDACTED]", redacted)
    }

    @Test
    fun `bearer authorization values are removed`() {
        assertEquals(
            "Authorization: Bearer [REDACTED]",
            LogRedactor.redact("Authorization: Bearer abc.def-123"),
        )
    }

    @Test
    fun `password and api key fields are removed in json and query strings`() {
        assertEquals(
            """{"email_verified":true,"password":"[REDACTED]"}""",
            LogRedactor.redact("""{"email_verified":true,"password":"hunter2"}"""),
        )
        assertEquals(
            "GET /rest/v1/songs?apikey=[REDACTED]&limit=30",
            LogRedactor.redact("GET /rest/v1/songs?apikey=sb_publishable_abc123&limit=30"),
        )
    }

    @Test
    fun `email addresses are masked`() {
        assertEquals("sign in failed for [REDACTED_EMAIL]", LogRedactor.redact("sign in failed for nova@example.com"))
    }

    @Test
    fun `ordinary messages are left untouched`() {
        val message = "loaded 30 songs in 120ms (page 2)"
        assertEquals(message, LogRedactor.redact(message))
    }
}
