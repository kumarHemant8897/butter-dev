package com.yourcompany.musicsocial.core.domain.usecase

import com.yourcompany.musicsocial.core.domain.usecase.Validated.Invalid
import com.yourcompany.musicsocial.core.domain.usecase.Validated.Rule
import com.yourcompany.musicsocial.core.domain.usecase.Validated.Valid
import org.junit.Assert.assertEquals
import org.junit.Test

class ValidateAccountFieldsUseCasesTest {

    @Test
    fun `email is trimmed and must look like an address`() {
        val validate = ValidateEmailUseCase()

        assertEquals(Valid("hemant@example.com"), validate("  hemant@example.com "))
        assertEquals(Invalid(Rule.BLANK), validate("   "))
        listOf("hemant", "hemant@", "@example.com", "hemant@example", "he mant@example.com").forEach {
            assertEquals(it, Invalid(Rule.INVALID_FORMAT), validate(it))
        }
        assertEquals(Invalid(Rule.TOO_LONG), validate("a".repeat(250) + "@b.co"))
    }

    @Test
    fun `password length follows the backend setting and spaces are kept`() {
        val validate = ValidatePasswordUseCase()

        assertEquals(Invalid(Rule.BLANK), validate(""))
        assertEquals(Invalid(Rule.TOO_SHORT), validate("1234567"))
        assertEquals(Valid("12345678"), validate("12345678"))
        assertEquals(Valid("  pass word  "), validate("  pass word  "))
        assertEquals(Valid("a".repeat(72)), validate("a".repeat(72)))
        assertEquals(Invalid(Rule.TOO_LONG), validate("a".repeat(73)))
    }

    @Test
    fun `display name is trimmed and inner whitespace collapsed`() {
        val validate = ValidateDisplayNameUseCase()

        assertEquals(Valid("Hemant Kumar"), validate("  Hemant    Kumar "))
        assertEquals(Invalid(Rule.BLANK), validate("   "))
        assertEquals(Valid("a".repeat(50)), validate("a".repeat(50)))
        assertEquals(Invalid(Rule.TOO_LONG), validate("a".repeat(51)))
    }

    @Test
    fun `bio is optional and limited to 160 characters`() {
        val validate = ValidateBioUseCase()

        assertEquals(Valid(""), validate("   "))
        assertEquals(Valid("late commits"), validate(" late commits "))
        assertEquals(Valid("a".repeat(160)), validate("a".repeat(160)))
        assertEquals(Invalid(Rule.TOO_LONG), validate("a".repeat(161)))
    }
}
