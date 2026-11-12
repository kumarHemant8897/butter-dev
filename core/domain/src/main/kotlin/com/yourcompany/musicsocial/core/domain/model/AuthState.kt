package com.yourcompany.musicsocial.core.domain.model

/** Authentication state of the current device. */
sealed interface AuthState {
    /** Session is being restored from storage; do not route yet. */
    data object Initializing : AuthState

    data object SignedOut : AuthState

    data class SignedIn(val userId: String, val email: String?) : AuthState

    /** Authentication cannot work at all, e.g. the backend is not configured in this build. */
    data object Unavailable : AuthState
}

/** What happened after a successful sign-up request. */
enum class SignUpOutcome {
    /** Email confirmation is off: the user already has a session. */
    SIGNED_IN,

    /** The backend sent a confirmation email; the user must open it before signing in. */
    EMAIL_CONFIRMATION_REQUIRED,
}
