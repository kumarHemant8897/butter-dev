package com.yourcompany.musicsocial.data.auth.datasource

import com.yourcompany.musicsocial.core.common.logging.AppLogger
import com.yourcompany.musicsocial.core.domain.model.AuthState
import com.yourcompany.musicsocial.core.domain.result.AppError
import com.yourcompany.musicsocial.core.domain.result.AppResult
import com.yourcompany.musicsocial.core.network.SupabaseClientProvider
import com.yourcompany.musicsocial.core.network.call
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class SupabaseAuthRemoteDataSource @Inject constructor(
    private val supabase: SupabaseClientProvider,
    private val logger: AppLogger,
) : AuthRemoteDataSource {

    override val authState: Flow<AuthState> = flow {
        val client = supabase.clientOrNull()
        if (client == null) {
            emit(AuthState.Unavailable)
            return@flow
        }
        emitAll(client.auth.sessionStatus.map { status -> status.toAuthState(client) })
    }

    override suspend fun signInWithEmail(email: String, password: String): AppResult<Unit> =
        supabase.call(logger, "auth.signInWithEmail", ::mapAuthError) { client ->
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
        }

    override suspend fun signUpWithEmail(email: String, password: String): AppResult<Unit> =
        supabase.call(logger, "auth.signUpWithEmail", ::mapAuthError) { client ->
            client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
        }

    override suspend fun signOut(): AppResult<Unit> =
        supabase.call(logger, "auth.signOut") { client -> client.auth.signOut() }

    private fun SessionStatus.toAuthState(client: SupabaseClient): AuthState = when (this) {
        SessionStatus.Initializing -> AuthState.Initializing
        is SessionStatus.Authenticated -> session.user?.let { AuthState.SignedIn(it.id, it.email) } ?: AuthState.SignedOut
        is SessionStatus.NotAuthenticated -> AuthState.SignedOut
        // A refresh failed (usually offline). The stored session still exists, so stay signed in.
        is SessionStatus.RefreshFailure ->
            client.auth.currentUserOrNull()?.let { AuthState.SignedIn(it.id, it.email) } ?: AuthState.SignedOut
    }

    private fun mapAuthError(error: Throwable): AppError? =
        (error as? AuthRestException)?.let {
            when (it.errorCode) {
                AuthErrorCode.InvalidCredentials -> AppError.Auth.InvalidCredentials
                AuthErrorCode.UserAlreadyExists, AuthErrorCode.EmailExists -> AppError.Conflict
                AuthErrorCode.WeakPassword -> AppError.Validation(field = "password", rule = "weak")
                else -> null
            }
        }
}
