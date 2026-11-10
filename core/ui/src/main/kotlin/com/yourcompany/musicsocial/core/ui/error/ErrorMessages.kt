package com.yourcompany.musicsocial.core.ui.error

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.yourcompany.musicsocial.core.designsystem.component.ErrorView
import com.yourcompany.musicsocial.core.designsystem.icon.AppIcons
import com.yourcompany.musicsocial.core.domain.result.AppError
import com.yourcompany.musicsocial.core.ui.R

/** User-facing copy for an [AppError]. One mapping for the whole app keeps wording consistent. */
data class ErrorCopy(
    @StringRes val title: Int,
    @StringRes val message: Int,
    /** Whether a retry button makes sense for this error. */
    val isRetryable: Boolean,
)

fun AppError.toErrorCopy(): ErrorCopy = when (this) {
    AppError.Network.Unavailable -> ErrorCopy(R.string.error_offline_title, R.string.error_offline_message, true)
    AppError.Network.Timeout -> ErrorCopy(R.string.error_timeout_title, R.string.error_timeout_message, true)
    AppError.Auth.InvalidCredentials -> ErrorCopy(R.string.error_auth_title, R.string.error_invalid_credentials, false)
    AppError.Auth.SessionExpired -> ErrorCopy(R.string.error_auth_title, R.string.error_session_expired, false)
    AppError.Auth.Forbidden -> ErrorCopy(R.string.error_forbidden_title, R.string.error_forbidden_message, false)
    AppError.Auth.NotSignedIn -> ErrorCopy(R.string.error_auth_title, R.string.error_not_signed_in, false)
    is AppError.Server -> ErrorCopy(R.string.error_server_title, R.string.error_server_message, true)
    AppError.NotFound -> ErrorCopy(R.string.error_not_found_title, R.string.error_not_found_message, false)
    AppError.Conflict -> ErrorCopy(R.string.error_conflict_title, R.string.error_conflict_message, false)
    is AppError.Validation -> ErrorCopy(R.string.error_validation_title, R.string.error_validation_message, false)
    AppError.BackendNotConfigured ->
        ErrorCopy(R.string.error_backend_not_configured_title, R.string.error_backend_not_configured_message, false)
    AppError.Unknown -> ErrorCopy(R.string.error_unknown_title, R.string.error_unknown_message, true)
}

/** [ErrorView] for an [AppError], showing a retry button only for retryable errors. */
@Composable
fun AppErrorView(
    error: AppError,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    val copy = error.toErrorCopy()
    ErrorView(
        title = stringResource(copy.title),
        message = stringResource(copy.message),
        icon = if (error is AppError.Network) AppIcons.Offline else AppIcons.Error,
        retryLabel = stringResource(R.string.action_retry),
        onRetry = onRetry?.takeIf { copy.isRetryable },
        modifier = modifier,
    )
}
