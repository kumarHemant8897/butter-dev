package com.yourcompany.musicsocial.data.auth.repository

import com.yourcompany.musicsocial.core.common.coroutines.ApplicationScope
import com.yourcompany.musicsocial.core.domain.model.AuthState
import com.yourcompany.musicsocial.core.domain.model.SignUpOutcome
import com.yourcompany.musicsocial.core.domain.repository.AuthRepository
import com.yourcompany.musicsocial.core.domain.result.AppResult
import com.yourcompany.musicsocial.data.auth.datasource.AuthRemoteDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shares one auth-state subscription across every collector in the app, and replays the latest
 * state to new collectors so screens never flash a signed-out state while a session is restored.
 */
@Singleton
internal class AuthRepositoryImpl @Inject constructor(
    private val remote: AuthRemoteDataSource,
    @ApplicationScope scope: CoroutineScope,
) : AuthRepository {

    override val authState: StateFlow<AuthState> = remote.authState.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = AuthState.Initializing,
    )

    override suspend fun signInWithEmail(email: String, password: String): AppResult<Unit> =
        remote.signInWithEmail(email.trim(), password)

    override suspend fun signUpWithEmail(
        email: String,
        password: String,
        username: String,
        displayName: String,
    ): AppResult<SignUpOutcome> = remote.signUpWithEmail(email.trim(), password, username.trim(), displayName.trim())

    override suspend fun signOut(): AppResult<Unit> = remote.signOut()
}
