package com.yourcompany.musicsocial.core.domain.usecase

/** Result of validating one form field. [Invalid.rule] is a stable code; the UI chooses the wording. */
sealed interface Validated<out T> {
    /** [value] is the cleaned input that should be used from here on (for example trimmed). */
    data class Valid<T>(val value: T) : Validated<T>

    data class Invalid(val rule: Rule) : Validated<Nothing>

    enum class Rule { BLANK, TOO_SHORT, TOO_LONG, INVALID_FORMAT }
}
