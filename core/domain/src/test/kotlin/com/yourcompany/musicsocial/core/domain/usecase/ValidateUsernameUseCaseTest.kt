package com.yourcompany.musicsocial.core.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class ValidateUsernameUseCaseTest {

    private val validate = ValidateUsernameUseCase()

    @Test
    fun `valid username is trimmed and lowercased`() {
        assertEquals(UsernameValidation.Valid("dj_nova.99"), validate("  DJ_Nova.99 "))
    }

    @Test
    fun `length limits are enforced after trimming`() {
        assertEquals(UsernameValidation.Invalid(UsernameRule.TOO_SHORT), validate("  ab  "))
        assertEquals(UsernameValidation.Valid("abc"), validate("abc"))
        assertEquals(UsernameValidation.Valid("a".repeat(30)), validate("a".repeat(30)))
        assertEquals(UsernameValidation.Invalid(UsernameRule.TOO_LONG), validate("a".repeat(31)))
    }

    @Test
    fun `characters outside the allowed set are rejected`() {
        listOf("with space", "emoji\uD83C\uDFB5", "dash-name", "at@name").forEach { input ->
            assertEquals(input, UsernameValidation.Invalid(UsernameRule.INVALID_CHARACTERS), validate(input))
        }
    }

    @Test
    fun `dots cannot lead, trail or repeat`() {
        listOf(".nova", "nova.", "no..va").forEach { input ->
            assertEquals(input, UsernameValidation.Invalid(UsernameRule.INVALID_DOT_PLACEMENT), validate(input))
        }
    }
}
