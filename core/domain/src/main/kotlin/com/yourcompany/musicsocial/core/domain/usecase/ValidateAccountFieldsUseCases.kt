package com.yourcompany.musicsocial.core.domain.usecase

import com.yourcompany.musicsocial.core.domain.model.UserProfile
import com.yourcompany.musicsocial.core.domain.usecase.Validated.Invalid
import com.yourcompany.musicsocial.core.domain.usecase.Validated.Rule
import com.yourcompany.musicsocial.core.domain.usecase.Validated.Valid
import javax.inject.Inject

/**
 * Email check for fast feedback only: the auth service makes the final decision.
 * A deliberately simple shape check; real verification is the confirmation email.
 */
class ValidateEmailUseCase @Inject constructor() {
    operator fun invoke(raw: String): Validated<String> {
        val email = raw.trim()
        return when {
            email.isEmpty() -> Invalid(Rule.BLANK)
            email.length > MAX_LENGTH -> Invalid(Rule.TOO_LONG)
            !SHAPE.matches(email) -> Invalid(Rule.INVALID_FORMAT)
            else -> Valid(email)
        }
    }

    companion object {
        const val MAX_LENGTH = 254
        private val SHAPE = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}

/**
 * Password rules. [MIN_LENGTH] matches `minimum_password_length` in backend/supabase/config.toml and
 * must be kept in sync with the hosted project's Auth settings. The password is never trimmed.
 * [MAX_LENGTH] is bcrypt's 72-byte limit.
 */
class ValidatePasswordUseCase @Inject constructor() {
    operator fun invoke(password: String): Validated<String> = when {
        password.isEmpty() -> Invalid(Rule.BLANK)
        password.length < MIN_LENGTH -> Invalid(Rule.TOO_SHORT)
        password.toByteArray().size > MAX_LENGTH -> Invalid(Rule.TOO_LONG)
        else -> Valid(password)
    }

    companion object {
        const val MIN_LENGTH = 8
        const val MAX_LENGTH = 72
    }
}

/** Display name: 1 to [UserProfile.DISPLAY_NAME_MAX_LENGTH] characters after trimming. */
class ValidateDisplayNameUseCase @Inject constructor() {
    operator fun invoke(raw: String): Validated<String> {
        val name = raw.trim().replace(Regex("\\s+"), " ")
        return when {
            name.isEmpty() -> Invalid(Rule.BLANK)
            name.length > UserProfile.DISPLAY_NAME_MAX_LENGTH -> Invalid(Rule.TOO_LONG)
            else -> Valid(name)
        }
    }
}

/** Bio is optional: blank is valid and becomes an empty string. */
class ValidateBioUseCase @Inject constructor() {
    operator fun invoke(raw: String): Validated<String> {
        val bio = raw.trim()
        return if (bio.length > UserProfile.BIO_MAX_LENGTH) Invalid(Rule.TOO_LONG) else Valid(bio)
    }
}
