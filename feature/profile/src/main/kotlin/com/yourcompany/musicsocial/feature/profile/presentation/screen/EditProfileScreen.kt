package com.yourcompany.musicsocial.feature.profile.presentation.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourcompany.musicsocial.core.designsystem.component.AppTextField
import com.yourcompany.musicsocial.core.designsystem.component.ButtonSize
import com.yourcompany.musicsocial.core.designsystem.component.CircleIconButton
import com.yourcompany.musicsocial.core.designsystem.component.LoadingView
import com.yourcompany.musicsocial.core.designsystem.component.PrimaryButton
import com.yourcompany.musicsocial.core.designsystem.icon.AppIcons
import com.yourcompany.musicsocial.core.designsystem.theme.ButterTheme
import com.yourcompany.musicsocial.core.designsystem.theme.Spacing
import com.yourcompany.musicsocial.core.domain.model.UserProfile
import com.yourcompany.musicsocial.core.domain.usecase.UsernameRule
import com.yourcompany.musicsocial.core.domain.usecase.Validated
import com.yourcompany.musicsocial.core.ui.error.AppErrorView
import com.yourcompany.musicsocial.core.ui.error.toErrorCopy
import com.yourcompany.musicsocial.feature.profile.R
import com.yourcompany.musicsocial.feature.profile.presentation.state.EditProfileUiState
import com.yourcompany.musicsocial.feature.profile.presentation.viewmodel.EditProfileViewModel

@Composable
internal fun EditProfileRoute(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.isSaved) { if (state.isSaved) onBack() }
    // The system photo picker: no storage permission needed, and the user picks one photo only.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.onPhotoPicked(uri.toString())
    }
    EditProfileScreen(
        state = state,
        contentPadding = contentPadding,
        onPickPhoto = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onBack = onBack,
        onDisplayNameChange = viewModel::onDisplayNameChange,
        onUsernameChange = viewModel::onUsernameChange,
        onBioChange = viewModel::onBioChange,
        onSave = viewModel::onSave,
        onRetry = viewModel::load,
    )
}

@Composable
internal fun EditProfileScreen(
    state: EditProfileUiState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onPickPhoto: () -> Unit = {},
) {
    Column(modifier.fillMaxSize().padding(contentPadding).imePadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIconButton(icon = AppIcons.Back, contentDescription = stringResource(R.string.profile_back), onClick = onBack)
            Text(
                stringResource(R.string.profile_edit),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f).padding(start = Spacing.sm).semantics { heading() },
            )
            PrimaryButton(
                text = stringResource(R.string.profile_save),
                onClick = onSave,
                loading = state.isSaving,
                enabled = !state.isLoading,
                size = ButtonSize.Compact,
            )
        }

        when {
            state.loadFailed -> Box(Modifier.weight(1f)) { AppErrorView(error = state.error!!, onRetry = onRetry) }
            state.isLoading -> Box(Modifier.weight(1f)) { LoadingView() }
            else -> Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                PhotoSection(state, onPickPhoto)
                AppTextField(
                    value = state.displayName,
                    onValueChange = onDisplayNameChange,
                    label = stringResource(R.string.profile_field_name),
                    errorText = state.displayNameError?.let { stringResource(it.nameMessage()) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                AppTextField(
                    value = state.username,
                    onValueChange = onUsernameChange,
                    label = stringResource(R.string.profile_field_username),
                    errorText = when {
                        state.usernameTaken -> stringResource(R.string.profile_error_username_taken)
                        state.usernameError != null -> stringResource(state.usernameError.message())
                        else -> null
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                AppTextField(
                    value = state.bio,
                    onValueChange = onBioChange,
                    label = stringResource(R.string.profile_field_bio),
                    supportingText = stringResource(R.string.profile_bio_count, state.bio.trim().length, UserProfile.BIO_MAX_LENGTH),
                    errorText = state.bioError?.let { stringResource(R.string.profile_error_bio_long) },
                    singleLine = false,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Default),
                    modifier = Modifier.fillMaxWidth(),
                )
                state.error?.let { error ->
                    Text(
                        stringResource(error.toErrorCopy().message),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

private fun Validated.Rule.nameMessage(): Int = when (this) {
    Validated.Rule.TOO_LONG -> R.string.profile_error_name_long
    else -> R.string.profile_error_name_blank
}

private fun UsernameRule.message(): Int = when (this) {
    UsernameRule.TOO_SHORT -> R.string.profile_error_username_short
    UsernameRule.TOO_LONG -> R.string.profile_error_username_long
    UsernameRule.INVALID_CHARACTERS -> R.string.profile_error_username_chars
    UsernameRule.INVALID_DOT_PLACEMENT -> R.string.profile_error_username_dots
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0F, heightDp = 700)
@Composable
private fun EditProfileScreenPreview() {
    ButterTheme {
        EditProfileScreen(
            state = EditProfileUiState(isLoading = false, displayName = "Hemant Kumar", username = "hemantkumar", bio = "late commits"),
            contentPadding = PaddingValues(),
            onBack = {}, onDisplayNameChange = {}, onUsernameChange = {}, onBioChange = {}, onSave = {}, onRetry = {},
        )
    }
}

/** The current photo with "Change photo"; a spinner covers it while uploading. */
@Composable
private fun PhotoSection(state: EditProfileUiState, onPickPhoto: () -> Unit) {
    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
        Box(contentAlignment = Alignment.Center) {
            com.yourcompany.musicsocial.core.designsystem.component.UserAvatar(
                name = state.avatarName.ifBlank { state.displayName },
                imageUrl = state.avatarUrl,
                size = 72.dp,
            )
            if (state.isUploadingPhoto) com.yourcompany.musicsocial.core.designsystem.component.LoadingIndicator()
        }
        androidx.compose.foundation.layout.Spacer(Modifier.width(Spacing.lg))
        Column {
            com.yourcompany.musicsocial.core.designsystem.component.SecondaryButton(
                text = stringResource(R.string.profile_change_photo),
                onClick = onPickPhoto,
                loading = state.isUploadingPhoto,
                size = ButtonSize.Compact,
            )
            state.photoError?.let { error ->
                Text(
                    stringResource(
                        if (error is com.yourcompany.musicsocial.core.domain.result.AppError.Validation) R.string.profile_photo_unreadable
                        else error.toErrorCopy().message,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
            }
        }
    }
}
