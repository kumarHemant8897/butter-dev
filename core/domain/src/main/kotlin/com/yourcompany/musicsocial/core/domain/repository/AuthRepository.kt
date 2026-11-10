package com.yourcompany.musicsocial.core.domain.repository

import com.yourcompany.musicsocial.core.domain.model.AuthState
import com.yourcompany.musicsocial.core.domain.result.AppResult
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** Hot stream of the current authentication state. Emits [AuthState.Initializing] first. */
    val authState: Flow<AuthState>

    suspend fun signInWithEmail(email: String, password: String): AppResult<Unit>

    suspend fun signUpWithEmail(email: String, password: String): AppResult<Unit>

    suspend fun signOut(): AppResult<Unit>
}
