package com.yourcompany.musicsocial.feature.auth.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourcompany.musicsocial.core.designsystem.component.AppTextField
import com.yourcompany.musicsocial.core.designsystem.component.PrimaryButton
import com.yourcompany.musicsocial.core.designsystem.component.ScreenTitle
import com.yourcompany.musicsocial.core.designsystem.theme.ButterTheme
import com.yourcompany.musicsocial.core.designsystem.theme.Spacing
import com.yourcompany.musicsocial.core.domain.usecase.UsernameRule
import com.yourcompany.musicsocial.core.domain.usecase.Validated
import com.yourcompany.musicsocial.core.ui.error.toErrorCopy
import com.yourcompany.musicsocial.feature.auth.R
import com.yourcompany.musicsocial.feature.auth.presentation.state.AuthMode
import com.yourcompany.musicsocial.feature.auth.presentation.state.AuthNotice
import com.yourcompany.musicsocial.feature.auth.presentation.state.AuthUiState
import com.yourcompany.musicsocial.feature.auth.presentation.viewmodel.AuthViewModel

@Composable
fun AuthRoute(viewModel: AuthViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AuthScreen(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onUsernameChange = viewModel::onUsernameChange,
        onDisplayNameChange = viewModel::onDisplayNameChange,
        onModeChange = viewModel::onModeChange,
        onSubmit = viewModel::onSubmit,
    )
}

@Composable
internal fun AuthScreen(
    state: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onModeChange: (AuthMode) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val signingUp = state.mode == AuthMode.SIGN_UP
    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .imePadding(),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = Spacing.xxl),
        ) {
            ScreenTitle(eyebrow = stringResource(R.string.auth_eyebrow), title = stringResource(R.string.auth_title))

            Column(
                Modifier.padding(horizontal = Spacing.screenHorizontal),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                if (signingUp) {
                    AppTextField(
                        value = state.displayName,
                        onValueChange = onDisplayNameChange,
                        label = stringResource(R.string.auth_display_name),
                        errorText = state.displayNameError?.let { stringResource(it.nameMessage()) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    AppTextField(
                        value = state.username,
                        onValueChange = onUsernameChange,
                        label = stringResource(R.string.auth_username),
                        supportingText = stringResource(R.string.auth_username_hint),
                        errorText = state.usernameError?.let { stringResource(it.message()) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                AppTextField(
                    value = state.email,
                    onValueChange = onEmailChange,
                    label = stringResource(R.string.auth_email),
                    errorText = state.emailError?.let { stringResource(it.emailMessage()) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                AppTextField(
                    value = state.password,
                    onValueChange = onPasswordChange,
                    label = stringResource(R.string.auth_password),
                    supportingText = if (signingUp) stringResource(R.string.auth_password_hint) else null,
                    errorText = state.passwordError?.let { stringResource(it.passwordMessage()) },
                    isPassword = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                    modifier = Modifier.fillMaxWidth(),
                )

                // Announced by screen readers when it appears.
                Box(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                    state.error?.let { error ->
                        Text(
                            stringResource(error.toErrorCopy().message),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    if (state.notice == AuthNotice.CONFIRM_EMAIL) {
                        Text(
                            stringResource(R.string.auth_notice_confirm_email),
                            style = MaterialTheme.typography.bodySmall,
                            color = ButterTheme.colors.live,
                        )
                    }
                }

                PrimaryButton(
                    text = stringResource(if (signingUp) R.string.auth_sign_up else R.string.auth_sign_in),
                    onClick = onSubmit,
                    loading = state.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(
                    onClick = { onModeChange(if (signingUp) AuthMode.SIGN_IN else AuthMode.SIGN_UP) },
                    enabled = !state.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(if (signingUp) R.string.auth_switch_to_sign_in else R.string.auth_switch_to_sign_up),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            Spacer(Modifier.height(Spacing.xl))
        }
    }
}

private fun Validated.Rule.emailMessage(): Int = when (this) {
    Validated.Rule.BLANK -> R.string.auth_error_email_blank
    else -> R.string.auth_error_email_format
}

private fun Validated.Rule.passwordMessage(): Int = when (this) {
    Validated.Rule.BLANK -> R.string.auth_error_password_blank
    Validated.Rule.TOO_LONG -> R.string.auth_error_password_long
    else -> R.string.auth_error_password_short
}

private fun Validated.Rule.nameMessage(): Int = when (this) {
    Validated.Rule.TOO_LONG -> R.string.auth_error_name_long
    else -> R.string.auth_error_name_blank
}

private fun UsernameRule.message(): Int = when (this) {
    UsernameRule.TOO_SHORT -> R.string.auth_error_username_short
    UsernameRule.TOO_LONG -> R.string.auth_error_username_long
    UsernameRule.INVALID_CHARACTERS -> R.string.auth_error_username_chars
    UsernameRule.INVALID_DOT_PLACEMENT -> R.string.auth_error_username_dots
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0F, heightDp = 800)
@Composable
private fun AuthScreenSignUpPreview() {
    ButterTheme {
        androidx.compose.material3.Surface(color = MaterialTheme.colorScheme.background) {
            AuthScreen(AuthUiState(mode = AuthMode.SIGN_UP), {}, {}, {}, {}, {}, {})
        }
    }
}
