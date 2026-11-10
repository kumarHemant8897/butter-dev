package com.yourcompany.musicsocial.core.domain.usecase

import javax.inject.Inject

/**
 * Username rules. They mirror the `profiles_username_format` CHECK constraint in
 * backend/supabase/migrations so the client rejects bad input before a network round trip.
 * The database constraint stays the source of truth.
 *
 * - 3 to 30 characters after trimming
 * - lowercase letters, digits, '.' and '_' only (input is lowercased first)
 * - must not start or end with '.', and must not contain ".."
 */
class ValidateUsernameUseCase @Inject constructor() {

    operator fun invoke(raw: String): UsernameValidation {
        val username = raw.trim().lowercase()
        return when {
            username.length < MIN_LENGTH -> UsernameValidation.Invalid(UsernameRule.TOO_SHORT)
            username.length > MAX_LENGTH -> UsernameValidation.Invalid(UsernameRule.TOO_LONG)
            !ALLOWED.matches(username) -> UsernameValidation.Invalid(UsernameRule.INVALID_CHARACTERS)
            username.startsWith('.') || username.endsWith('.') || ".." in username ->
                UsernameValidation.Invalid(UsernameRule.INVALID_DOT_PLACEMENT)
            else -> UsernameValidation.Valid(username)
        }
    }

    companion object {
        const val MIN_LENGTH = 3
        const val MAX_LENGTH = 30
        private val ALLOWED = Regex("^[a-z0-9._]+$")
    }
}

sealed interface UsernameValidation {
    /** [normalized] is the trimmed, lowercased value that should be stored. */
    data class Valid(val normalized: String) : UsernameValidation
    data class Invalid(val rule: UsernameRule) : UsernameValidation
}

enum class UsernameRule { TOO_SHORT, TOO_LONG, INVALID_CHARACTERS, INVALID_DOT_PLACEMENT }
