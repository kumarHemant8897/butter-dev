package com.yourcompany.musicsocial.core.network

import com.yourcompany.musicsocial.core.common.logging.AppLogger
import com.yourcompany.musicsocial.core.common.logging.LogCategory
import com.yourcompany.musicsocial.core.domain.result.AppError
import com.yourcompany.musicsocial.core.domain.result.AppResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.BadRequestRestException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.NotFoundRestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.exceptions.UnauthorizedRestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.TimeoutCancellationException
import java.io.IOException
import java.net.SocketTimeoutException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Runs a remote operation and converts every expected failure into [AppResult.Failure].
 *
 * - Coroutine cancellation is rethrown, never swallowed, so structured concurrency keeps working.
 * - Missing credentials become [AppError.BackendNotConfigured] without touching the network.
 * - Errors are logged once here with the operation name, so callers do not log them again.
 */
suspend inline fun <T> SupabaseClientProvider.call(
    logger: AppLogger,
    operation: String,
    /** Optional operation-specific mapping, tried before the generic [toAppError]. */
    errorMapper: (Throwable) -> AppError? = { null },
    block: (SupabaseClient) -> T,
): AppResult<T> {
    val client = clientOrNull() ?: return AppResult.Failure(AppError.BackendNotConfigured)
    return try {
        AppResult.Success(block(client))
    } catch (e: TimeoutCancellationException) {
        logger.warn(LogCategory.Network, "$operation timed out", e)
        AppResult.Failure(AppError.Network.Timeout)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        val error = errorMapper(e) ?: e.toAppError()
        val category = if (error is AppError.Auth) LogCategory.Auth else LogCategory.Network
        if (error is AppError.Network) logger.warn(category, "$operation failed: $error") else logger.error(category, "$operation failed: $error", e)
        AppResult.Failure(error)
    }
}

/** Maps Supabase/Ktor/IO exceptions to [AppError]. Order matters: most specific first. */
fun Throwable.toAppError(): AppError = when (this) {
    is HttpRequestTimeoutException, is SocketTimeoutException -> AppError.Network.Timeout
    is IOException -> AppError.Network.Unavailable
    is HttpRequestException -> when (cause) {
        is HttpRequestTimeoutException, is SocketTimeoutException -> AppError.Network.Timeout
        else -> AppError.Network.Unavailable
    }
    is UnauthorizedRestException -> AppError.Auth.SessionExpired
    is NotFoundRestException -> AppError.NotFound
    is BadRequestRestException -> AppError.Validation(field = "request", rule = "bad_request")
    is RestException -> statusCode.toAppError()
    else -> AppError.Unknown
}

internal fun Int.toAppError(): AppError = when (this) {
    401 -> AppError.Auth.SessionExpired
    403 -> AppError.Auth.Forbidden
    404 -> AppError.NotFound
    409 -> AppError.Conflict
    400, 422 -> AppError.Validation(field = "request", rule = "rejected")
    in 500..599 -> AppError.Server(this)
    else -> AppError.Server(this)
}
