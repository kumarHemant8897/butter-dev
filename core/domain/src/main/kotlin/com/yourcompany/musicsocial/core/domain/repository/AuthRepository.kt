package com.yourcompany.musicsocial.core.domain.repository

import com.yourcompany.musicsocial.core.domain.model.AuthState
import com.yourcompany.musicsocial.core.domain.model.SignUpOutcome
import com.yourcompany.musicsocial.core.domain.result.AppResult
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** Hot stream of the current authentication state. Emits [AuthState.Initializing] first. */
    val authState: Flow<AuthState>

    suspend fun signInWithEmail(email: String, password: String): AppResult<Unit>

    /**
     * Creates the account. [username] and [displayName] travel as sign-up metadata and become the
     * profile through a database trigger. If [username] is already taken the profile gets a
     * placeholder username that the user can change in Edit profile.
     */
    suspend fun signUpWithEmail(
        email: String,
        password: String,
        username: String,
        displayName: String,
    ): AppResult<SignUpOutcome>

    suspend fun signOut(): AppResult<Unit>
}
