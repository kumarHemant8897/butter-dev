package com.yourcompany.musicsocial

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.yourcompany.musicsocial.core.designsystem.component.LoadingView
import com.yourcompany.musicsocial.core.domain.model.AuthState
import com.yourcompany.musicsocial.core.domain.repository.AuthRepository
import com.yourcompany.musicsocial.feature.auth.presentation.screen.AuthRoute
import com.yourcompany.musicsocial.navigation.MusicSocialAppShell
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Which top-level experience to show. Derived from [AuthState] so a token refresh never rebuilds the app. */
enum class AppGate { RESTORING_SESSION, SIGN_IN, MAIN }

@HiltViewModel
class AppViewModel @Inject constructor(authRepository: AuthRepository) : ViewModel() {

    val gate: StateFlow<AppGate> = authRepository.authState
        .map { state ->
            when (state) {
                AuthState.Initializing -> AppGate.RESTORING_SESSION
                AuthState.SignedOut -> AppGate.SIGN_IN
                // No backend in this build: the main app runs on sample data instead of blocking on sign-in.
                is AuthState.SignedIn, AuthState.Unavailable -> AppGate.MAIN
            }
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppGate.RESTORING_SESSION)
}

@Composable
fun ButterApp(viewModel: AppViewModel = hiltViewModel()) {
    val gate by viewModel.gate.collectAsStateWithLifecycle()
    // Text outside a Scaffold (sign-in, loading) takes its colour from the nearest Surface. Without
    // this one it falls back to Material's default black, which is invisible on the dark background.
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Crossfade(targetState = gate, label = "appGate") { current ->
            when (current) {
                AppGate.RESTORING_SESSION -> LoadingView()
                AppGate.SIGN_IN -> AuthRoute()
                AppGate.MAIN -> MusicSocialAppShell()
            }
        }
    }
}
