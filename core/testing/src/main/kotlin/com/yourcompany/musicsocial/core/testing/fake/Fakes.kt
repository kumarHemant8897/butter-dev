package com.yourcompany.musicsocial.core.testing.fake

import com.yourcompany.musicsocial.core.domain.model.AuthState
import com.yourcompany.musicsocial.core.domain.repository.AuthRepository
import com.yourcompany.musicsocial.core.domain.result.AppResult
import com.yourcompany.musicsocial.core.domain.system.BackendAvailability
import com.yourcompany.musicsocial.core.domain.system.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Network state that a test can flip with [setOnline]. */
class FakeNetworkMonitor(initiallyOnline: Boolean = true) : NetworkMonitor {
    private val state = MutableStateFlow(initiallyOnline)
    override val isOnline: Flow<Boolean> = state

    fun setOnline(online: Boolean) {
        state.value = online
    }
}

class FakeBackendAvailability(override val isConfigured: Boolean) : BackendAvailability

/**
 * In-memory [AuthRepository] for ViewModel tests. Tests drive [authState] directly with [emit];
 * the sign-in methods only record calls and return [nextResult].
 */
class FakeAuthRepository(initial: AuthState = AuthState.Initializing) : AuthRepository {
    private val state = MutableStateFlow(initial)
    override val authState: Flow<AuthState> = state

    var nextResult: AppResult<Unit> = AppResult.Success(Unit)
    val signInAttempts = mutableListOf<String>()

    fun emit(newState: AuthState) {
        state.value = newState
    }

    override suspend fun signInWithEmail(email: String, password: String): AppResult<Unit> {
        signInAttempts += email
        return nextResult
    }

    override suspend fun signUpWithEmail(email: String, password: String): AppResult<Unit> = nextResult

    override suspend fun signOut(): AppResult<Unit> {
        state.value = AuthState.SignedOut
        return AppResult.Success(Unit)
    }
}
