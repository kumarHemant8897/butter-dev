package com.yourcompany.musicsocial.feature.profile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourcompany.musicsocial.core.domain.model.AuthState
import com.yourcompany.musicsocial.core.domain.model.ProfileUpdate
import com.yourcompany.musicsocial.core.domain.model.UserProfile
import com.yourcompany.musicsocial.core.domain.repository.AuthRepository
import com.yourcompany.musicsocial.core.domain.repository.UserRepository
import com.yourcompany.musicsocial.core.domain.result.AppError
import com.yourcompany.musicsocial.core.domain.result.fold
import com.yourcompany.musicsocial.core.domain.usecase.UsernameValidation
import com.yourcompany.musicsocial.core.domain.usecase.ValidateBioUseCase
import com.yourcompany.musicsocial.core.domain.usecase.ValidateDisplayNameUseCase
import com.yourcompany.musicsocial.core.domain.usecase.ValidateUsernameUseCase
import com.yourcompany.musicsocial.core.domain.usecase.Validated
import com.yourcompany.musicsocial.feature.profile.presentation.state.EditProfileUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val validateUsername: ValidateUsernameUseCase,
    private val validateDisplayName: ValidateDisplayNameUseCase,
    private val validateBio: ValidateBioUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    /** The profile as loaded; used to send only the fields that actually changed. */
    private var original: UserProfile? = null

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val auth = authRepository.authState.first { it !is AuthState.Initializing }
            if (auth !is AuthState.SignedIn) {
                _uiState.update { it.copy(error = AppError.Auth.NotSignedIn) }
                return@launch
            }
            userRepository.getProfile(auth.userId).fold(
                onSuccess = { profile ->
                    original = profile
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            displayName = profile.displayName,
                            username = profile.username,
                            bio = profile.bio.orEmpty(),
                            error = null,
                        )
                    }
                },
                onFailure = { error -> _uiState.update { it.copy(error = error) } },
            )
        }
    }

    fun onDisplayNameChange(value: String) = _uiState.update { it.copy(displayName = value, displayNameError = null, error = null) }

    fun onUsernameChange(value: String) =
        _uiState.update { it.copy(username = value, usernameError = null, usernameTaken = false, error = null) }

    fun onBioChange(value: String) = _uiState.update { it.copy(bio = value, bioError = null, error = null) }

    fun onSave() {
        val state = _uiState.value
        val current = original
        if (state.isSaving || state.isLoading || current == null) return

        val displayName = validateDisplayName(state.displayName)
        val username = validateUsername(state.username)
        val bio = validateBio(state.bio)
        if (displayName is Validated.Invalid || username is UsernameValidation.Invalid || bio is Validated.Invalid) {
            _uiState.update {
                it.copy(
                    displayNameError = (displayName as? Validated.Invalid)?.rule,
                    usernameError = (username as? UsernameValidation.Invalid)?.rule,
                    bioError = (bio as? Validated.Invalid)?.rule,
                )
            }
            return
        }

        val newName = (displayName as Validated.Valid).value
        val newUsername = (username as UsernameValidation.Valid).normalized
        val newBio = (bio as Validated.Valid).value
        val update = ProfileUpdate(
            displayName = newName.takeIf { it != current.displayName },
            username = newUsername.takeIf { it != current.username },
            bio = newBio.takeIf { it != current.bio.orEmpty() },
        )
        if (update == ProfileUpdate()) {
            _uiState.update { it.copy(isSaved = true) }
            return
        }

        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            userRepository.updateMyProfile(update).fold(
                onSuccess = { saved ->
                    original = saved
                    _uiState.update { it.copy(isSaving = false, isSaved = true) }
                },
                onFailure = { error ->
                    _uiState.update {
                        if (error == AppError.Conflict) it.copy(isSaving = false, usernameTaken = true)
                        else it.copy(isSaving = false, error = error)
                    }
                },
            )
        }
    }
}
