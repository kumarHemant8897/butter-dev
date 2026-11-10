package com.yourcompany.musicsocial.feature.profile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourcompany.musicsocial.core.domain.model.AuthState
import com.yourcompany.musicsocial.core.domain.repository.AuthRepository
import com.yourcompany.musicsocial.core.sample.SampleData
import com.yourcompany.musicsocial.feature.profile.presentation.state.AccountStatus
import com.yourcompany.musicsocial.feature.profile.presentation.state.ProfileUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Account status is real: it comes from the AuthRepository interface (Supabase implementation in
 * :data:auth, wired by the app module). Profile content is SampleData (TESTING ONLY) until
 * UserRepository.getProfile is implemented in Phase 2.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    authRepository: AuthRepository,
) : ViewModel() {

    private val unliked = MutableStateFlow<Set<String>>(emptySet())

    private val initial = ProfileUiState(
        account = AccountStatus.Loading,
        profile = SampleData.me,
        topArtists = SampleData.myTopArtists,
        favoriteSongs = SampleData.myFavoriteSongs,
        publicMixes = SampleData.myPublicMixes,
        lastPlayed = SampleData.myLastPlayed,
        isSample = true,
    )

    val uiState: StateFlow<ProfileUiState> = combine(authRepository.authState, unliked) { auth, unlikedIds ->
        initial.copy(account = auth.toAccountStatus(), unlikedSongIds = unlikedIds)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    fun onToggleLike(songId: String) {
        unliked.update { if (songId in it) it - songId else it + songId }
    }

    private fun AuthState.toAccountStatus(): AccountStatus = when (this) {
        AuthState.Initializing -> AccountStatus.Loading
        AuthState.Unavailable -> AccountStatus.BackendUnavailable
        AuthState.SignedOut -> AccountStatus.SignedOut
        is AuthState.SignedIn -> AccountStatus.SignedIn(userId)
    }
}
