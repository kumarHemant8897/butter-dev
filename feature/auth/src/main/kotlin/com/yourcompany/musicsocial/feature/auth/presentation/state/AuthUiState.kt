package com.yourcompany.musicsocial.feature.auth.presentation.state

import androidx.compose.runtime.Immutable
import com.yourcompany.musicsocial.core.domain.result.AppError
import com.yourcompany.musicsocial.core.domain.usecase.UsernameRule
import com.yourcompany.musicsocial.core.domain.usecase.Validated

enum class AuthMode { SIGN_IN, SIGN_UP }

enum class AuthNotice { CONFIRM_EMAIL }

@Immutable
data class AuthUiState(
    val mode: AuthMode = AuthMode.SIGN_IN,
    val email: String = "",
    val password: String = "",
    val username: String = "",
    val displayName: String = "",
    val emailError: Validated.Rule? = null,
    val passwordError: Validated.Rule? = null,
    val usernameError: UsernameRule? = null,
    val displayNameError: Validated.Rule? = null,
    val isSubmitting: Boolean = false,
    /** Failure reported by the backend (wrong password, email already used, offline...). */
    val error: AppError? = null,
    val notice: AuthNotice? = null,
)
