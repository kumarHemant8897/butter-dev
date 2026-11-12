package com.yourcompany.musicsocial.feature.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourcompany.musicsocial.core.domain.model.SignUpOutcome
import com.yourcompany.musicsocial.core.domain.repository.AuthRepository
import com.yourcompany.musicsocial.core.domain.result.fold
import com.yourcompany.musicsocial.core.domain.usecase.UsernameValidation
import com.yourcompany.musicsocial.core.domain.usecase.ValidateDisplayNameUseCase
import com.yourcompany.musicsocial.core.domain.usecase.ValidateEmailUseCase
import com.yourcompany.musicsocial.core.domain.usecase.ValidatePasswordUseCase
import com.yourcompany.musicsocial.core.domain.usecase.ValidateUsernameUseCase
import com.yourcompany.musicsocial.core.domain.usecase.Validated
import com.yourcompany.musicsocial.feature.auth.presentation.state.AuthMode
import com.yourcompany.musicsocial.feature.auth.presentation.state.AuthNotice
import com.yourcompany.musicsocial.feature.auth.presentation.state.AuthUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Sign-in / sign-up form. On success it does nothing visible: the app shell observes
 * AuthRepository.authState and swaps this screen for the main app by itself.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val validateEmail: ValidateEmailUseCase,
    private val validatePassword: ValidatePasswordUseCase,
    private val validateUsername: ValidateUsernameUseCase,
    private val validateDisplayName: ValidateDisplayNameUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, emailError = null, error = null, notice = null) }

    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, passwordError = null, error = null) }

    fun onUsernameChange(value: String) = _uiState.update { it.copy(username = value, usernameError = null, error = null) }

    fun onDisplayNameChange(value: String) = _uiState.update { it.copy(displayName = value, displayNameError = null, error = null) }

    fun onModeChange(mode: AuthMode) {
        _uiState.update {
            AuthUiState(mode = mode, email = it.email, notice = null)
        }
    }

    fun onSubmit() {
        val state = _uiState.value
        if (state.isSubmitting) return

        val email = validateEmail(state.email)
        val signingUp = state.mode == AuthMode.SIGN_UP
        // Existing accounts may predate the current password rules, so sign-in only requires a value.
        val password = if (signingUp) validatePassword(state.password) else
            if (state.password.isEmpty()) Validated.Invalid(Validated.Rule.BLANK) else Validated.Valid(state.password)
        val username = if (signingUp) validateUsername(state.username) else null
        val displayName = if (signingUp) validateDisplayName(state.displayName) else null

        val invalid = email is Validated.Invalid || password is Validated.Invalid ||
            username is UsernameValidation.Invalid || displayName is Validated.Invalid
        if (invalid) {
            _uiState.update {
                it.copy(
                    emailError = (email as? Validated.Invalid)?.rule,
                    passwordError = (password as? Validated.Invalid)?.rule,
                    usernameError = (username as? UsernameValidation.Invalid)?.rule,
                    displayNameError = (displayName as? Validated.Invalid)?.rule,
                )
            }
            return
        }

        val cleanEmail = (email as Validated.Valid).value
        _uiState.update { it.copy(isSubmitting = true, error = null, notice = null) }
        viewModelScope.launch {
            if (signingUp) {
                val result = authRepository.signUpWithEmail(
                    email = cleanEmail,
                    password = state.password,
                    username = (username as UsernameValidation.Valid).normalized,
                    displayName = (displayName as Validated.Valid).value,
                )
                result.fold(
                    onSuccess = { outcome ->
                        _uiState.update {
                            when (outcome) {
                                SignUpOutcome.SIGNED_IN -> it.copy(isSubmitting = false)
                                SignUpOutcome.EMAIL_CONFIRMATION_REQUIRED -> AuthUiState(
                                    mode = AuthMode.SIGN_IN,
                                    email = cleanEmail,
                                    notice = AuthNotice.CONFIRM_EMAIL,
                                )
                            }
                        }
                    },
                    onFailure = { error -> _uiState.update { it.copy(isSubmitting = false, error = error) } },
                )
            } else {
                authRepository.signInWithEmail(cleanEmail, state.password).fold(
                    onSuccess = { _uiState.update { it.copy(isSubmitting = false) } },
                    onFailure = { error -> _uiState.update { it.copy(isSubmitting = false, error = error) } },
                )
            }
        }
    }
}
