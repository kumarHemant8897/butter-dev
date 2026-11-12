package com.yourcompany.musicsocial.feature.profile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourcompany.musicsocial.core.domain.model.AuthState
import com.yourcompany.musicsocial.core.domain.model.LibraryKind
import com.yourcompany.musicsocial.core.domain.playback.PlaybackController
import com.yourcompany.musicsocial.core.domain.repository.AuthRepository
import com.yourcompany.musicsocial.core.domain.repository.LibraryRepository
import com.yourcompany.musicsocial.core.domain.repository.PlaylistRepository
import com.yourcompany.musicsocial.core.domain.repository.UserRepository
import com.yourcompany.musicsocial.core.domain.result.AppResult
import com.yourcompany.musicsocial.core.domain.result.fold
import com.yourcompany.musicsocial.core.domain.result.getOrNull
import com.yourcompany.musicsocial.core.sample.SampleData
import com.yourcompany.musicsocial.feature.profile.presentation.components.LibraryLists
import com.yourcompany.musicsocial.feature.profile.presentation.state.ProfileUiState
import com.yourcompany.musicsocial.feature.profile.presentation.state.SampleSections
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The signed-in user's own profile: header, liked and saved songs, and the number of follow
 * requests waiting. Without a backend (AuthState.Unavailable) it shows the sample profile.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val libraryRepository: LibraryRepository,
    private val playlistRepository: PlaylistRepository,
    private val playbackController: PlaybackController,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var currentUserId: String? = null

    init {
        viewModelScope.launch {
            authRepository.authState.collectLatest { auth ->
                when (auth) {
                    AuthState.Initializing, AuthState.SignedOut -> {
                        currentUserId = null
                        _uiState.value = ProfileUiState.Loading
                    }
                    AuthState.Unavailable -> {
                        currentUserId = null
                        _uiState.value = sampleContent()
                    }
                    is AuthState.SignedIn -> {
                        currentUserId = auth.userId
                        load(auth.userId, showLoading = true)
                    }
                }
            }
        }
        viewModelScope.launch {
            playbackController.activeSong().collect { active -> updateContent { it.copy(library = it.library.withActive(active)) } }
        }
        // A heart tapped in the Player changes the list here too. The first value is the current one,
        // which load() already handled.
        viewModelScope.launch {
            libraryRepository.mySongIds.drop(1).collect { refresh() }
        }
    }

    /** Called when the screen becomes visible again, e.g. after Edit profile. Keeps showing the old data meanwhile. */
    fun refresh() {
        val userId = currentUserId ?: return
        // A load is already in flight (first visit); do not fetch twice.
        if (_uiState.value is ProfileUiState.Loading) return
        viewModelScope.launch { load(userId, showLoading = false) }
    }

    fun onRetry() {
        val userId = currentUserId ?: return
        viewModelScope.launch { load(userId, showLoading = true) }
    }

    fun onSongClick(kind: LibraryKind, index: Int) {
        val content = _uiState.value as? ProfileUiState.Content ?: return
        playbackController.playOrToggle(content.library.songs(kind), index)
    }

    fun onNewPlaylist() = updateContent { it.copy(isCreatePlaylistOpen = true, actionError = null) }

    fun onDismissNewPlaylist() = updateContent { it.copy(isCreatePlaylistOpen = false) }

    fun onCreatePlaylist(title: String, isPublic: Boolean) {
        val content = _uiState.value as? ProfileUiState.Content ?: return
        if (content.isCreatingPlaylist) return
        updateContent { it.copy(isCreatingPlaylist = true) }
        viewModelScope.launch {
            when (val result = playlistRepository.createPlaylist(title, isPublic)) {
                is AppResult.Success -> updateContent {
                    it.copy(
                        isCreatingPlaylist = false,
                        isCreatePlaylistOpen = false,
                        library = it.library.copy(playlists = listOf(result.data) + it.library.playlists),
                    )
                }
                is AppResult.Failure -> updateContent { it.copy(isCreatingPlaylist = false, actionError = result.error) }
            }
        }
    }

    fun onSignOut() {
        val content = _uiState.value as? ProfileUiState.Content ?: return
        if (content.isSigningOut) return
        _uiState.value = content.copy(isSigningOut = true)
        viewModelScope.launch {
            // On success the app shell reacts to the new auth state and replaces this screen.
            authRepository.signOut().fold(
                onSuccess = {},
                onFailure = { updateContent { it.copy(isSigningOut = false) } },
            )
        }
    }

    /** TESTING ONLY: toggles a heart on a sample song. */
    fun onToggleLike(songId: String) {
        updateContent { content ->
            val sample = content.sample ?: return@updateContent content
            val unliked = if (songId in sample.unlikedSongIds) sample.unlikedSongIds - songId else sample.unlikedSongIds + songId
            content.copy(sample = sample.copy(unlikedSongIds = unliked))
        }
    }

    private suspend fun load(userId: String, showLoading: Boolean) {
        if (showLoading) _uiState.value = ProfileUiState.Loading
        val liked = viewModelScope.async { libraryRepository.getSongs(userId, LibraryKind.LIKED, LIBRARY_LIMIT).getOrNull() }
        val saved = viewModelScope.async { libraryRepository.getSongs(userId, LibraryKind.SAVED, LIBRARY_LIMIT).getOrNull() }
        val playlists = viewModelScope.async { playlistRepository.getPlaylists(userId).getOrNull() }
        val requests = viewModelScope.async { userRepository.getIncomingRequests().getOrNull()?.size }
        userRepository.getProfile(userId).fold(
            onSuccess = { profile ->
                val previous = _uiState.value as? ProfileUiState.Content
                val active = playbackController.state.value.let { it.song?.id to it.isPlaying }
                _uiState.value = ProfileUiState.Content(
                    profile = profile,
                    // A list that failed to load keeps what was shown before instead of looking empty.
                    library = LibraryLists(
                        liked = liked.await() ?: previous?.library?.liked.orEmpty(),
                        saved = saved.await() ?: previous?.library?.saved.orEmpty(),
                        playlists = playlists.await() ?: previous?.library?.playlists.orEmpty(),
                    ).withActive(active),
                    requestCount = requests.await() ?: previous?.requestCount ?: 0,
                )
            },
            onFailure = { error ->
                // A failed background refresh must not replace profile data the user is looking at.
                if (showLoading || _uiState.value !is ProfileUiState.Content) _uiState.value = ProfileUiState.Error(error)
            },
        )
    }

    private inline fun updateContent(crossinline change: (ProfileUiState.Content) -> ProfileUiState.Content) {
        _uiState.update { state -> (state as? ProfileUiState.Content)?.let(change) ?: state }
    }

    private fun sampleContent() = ProfileUiState.Content(
        profile = SampleData.me,
        sample = SampleSections(
            topArtists = SampleData.myTopArtists,
            favoriteSongs = SampleData.myFavoriteSongs,
            publicMixes = SampleData.myPublicMixes,
            lastPlayed = SampleData.myLastPlayed,
        ),
    )

    private companion object {
        const val LIBRARY_LIMIT = 50
    }
}
