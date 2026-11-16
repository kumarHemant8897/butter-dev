package com.yourcompany.musicsocial.feature.profile.presentation.state

import androidx.compose.runtime.Immutable
import com.yourcompany.musicsocial.core.domain.result.AppError
import com.yourcompany.musicsocial.core.domain.usecase.UsernameRule
import com.yourcompany.musicsocial.core.domain.usecase.Validated

@Immutable
data class EditProfileUiState(
    /** True until the current profile has been loaded into the form. */
    val isLoading: Boolean = true,
    val displayName: String = "",
    val username: String = "",
    val bio: String = "",
    val displayNameError: Validated.Rule? = null,
    val usernameError: UsernameRule? = null,
    val bioError: Validated.Rule? = null,
    /** The backend reported the chosen username as already taken. */
    val usernameTaken: Boolean = false,
    val isSaving: Boolean = false,
    /** Loading or saving failed on the backend. */
    val error: AppError? = null,
    /** Set once the profile was saved; the screen then navigates back. */
    val isSaved: Boolean = false,
    /** Name and current photo, for the avatar at the top of the form. */
    val avatarName: String = "",
    val avatarUrl: String? = null,
    val isUploadingPhoto: Boolean = false,
    /** The photo could not be read, or the upload failed. */
    val photoError: AppError? = null,
) {
    /** Whether the form could not be loaded at all (nothing to edit). */
    val loadFailed: Boolean get() = isLoading && error != null
}
