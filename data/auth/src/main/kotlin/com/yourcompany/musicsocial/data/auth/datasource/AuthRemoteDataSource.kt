package com.yourcompany.musicsocial.data.auth.datasource

import com.yourcompany.musicsocial.core.domain.model.AuthState
import com.yourcompany.musicsocial.core.domain.result.AppResult
import kotlinx.coroutines.flow.Flow

/**
 * Boundary to the authentication provider. Only this interface knows that Supabase Auth exists;
 * replacing the provider means writing a new implementation, not touching repositories or UI.
 *
 * Session tokens are persisted and refreshed by the Supabase SDK (in SharedPreferences-backed
 * storage by default). They never pass through this interface.
 */
interface AuthRemoteDataSource {
    val authState: Flow<AuthState>

    suspend fun signInWithEmail(email: String, password: String): AppResult<Unit>

    suspend fun signUpWithEmail(email: String, password: String): AppResult<Unit>

    suspend fun signOut(): AppResult<Unit>
}
