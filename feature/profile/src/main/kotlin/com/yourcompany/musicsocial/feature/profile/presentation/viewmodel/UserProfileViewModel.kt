package com.yourcompany.musicsocial.feature.profile.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourcompany.musicsocial.core.domain.model.FollowStatus
import com.yourcompany.musicsocial.core.domain.model.LibraryKind
import com.yourcompany.musicsocial.core.domain.model.Relationship
import com.yourcompany.musicsocial.core.domain.playback.PlaybackController
import com.yourcompany.musicsocial.core.domain.repository.ChatRepository
import com.yourcompany.musicsocial.core.domain.repository.LibraryRepository
import com.yourcompany.musicsocial.core.domain.repository.PlaylistRepository
import com.yourcompany.musicsocial.core.domain.repository.UserRepository
import com.yourcompany.musicsocial.core.domain.result.AppResult
import com.yourcompany.musicsocial.core.domain.result.getOrNull
import com.yourcompany.musicsocial.core.navigation.ConversationRoute
import com.yourcompany.musicsocial.feature.profile.presentation.components.LibraryLists
import com.yourcompany.musicsocial.feature.profile.presentation.state.UserProfileUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Another person's profile. Name, photo, bio and counts are public; liked and saved songs load only
 * once the signed-in user is an approved follower. The follow button walks through
 * Follow -> Requested (tap to cancel) -> Following (tap to unfollow), and Message opens the direct
 * chat once either person follows the other.
 */
@HiltViewModel
class UserProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository,
    private val libraryRepository: LibraryRepository,
    private val playlistRepository: PlaylistRepository,
    private val chatRepository: ChatRepository,
    private val playbackController: PlaybackController,
) : ViewModel() {

    private val userId: String = savedStateHandle.get<String>(ARG_USER_ID).orEmpty()

    private val _uiState = MutableStateFlow<UserProfileUiState>(UserProfileUiState.Loading)
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    init {
        load(showLoading = true)
        viewModelScope.launch {
            playbackController.activeSong().collect { active -> updateContent { it.copy(library = it.library.withActive(active)) } }
        }
    }

    fun onRetry() = load(showLoading = true)

    /** Screen became visible again: the other person may have accepted the request meanwhile. */
    fun refresh() {
        if (_uiState.value is UserProfileUiState.Content) load(showLoading = false)
    }

    fun onFollowClick() {
        val content = _uiState.value as? UserProfileUiState.Content ?: return
        if (content.isUpdating) return
        val before = content.relationship
        val after = when (before.status) {
            FollowStatus.NONE -> FollowStatus.REQUESTED
            FollowStatus.REQUESTED, FollowStatus.FOLLOWING -> FollowStatus.NONE
        }
        // Optimistic: the button changes at once and goes back if the backend refuses.
        updateContent {
            it.copy(
                relationship = before.copy(status = after),
                isUpdating = true,
                actionError = null,
                // Unfollowing hides their library straight away, as the database will.
                library = if (after == FollowStatus.NONE) LibraryLists().withActive(it.library.activeSongId to it.library.isActiveSongPlaying) else it.library,
                profile = if (before.status == FollowStatus.FOLLOWING) it.profile.copy(followerCount = (it.profile.followerCount - 1).coerceAtLeast(0)) else it.profile,
            )
        }
        viewModelScope.launch {
            val result = when (before.status) {
                FollowStatus.NONE -> userRepository.requestFollow(userId)
                FollowStatus.REQUESTED -> userRepository.cancelFollowRequest(userId)
                FollowStatus.FOLLOWING -> userRepository.unfollow(userId)
            }
            if (result is AppResult.Failure) {
                // Put the button, the count and the songs back exactly as they were. No reload: the
                // network that just failed would most likely fail again.
                updateContent {
                    it.copy(
                        relationship = before,
                        profile = content.profile,
                        library = content.library.copy(activeSongId = it.library.activeSongId, isActiveSongPlaying = it.library.isActiveSongPlaying),
                        isUpdating = false,
                        actionError = result.error,
                    )
                }
            } else {
                updateContent { it.copy(isUpdating = false) }
            }
        }
    }

    fun onMessageClick() {
        val content = _uiState.value as? UserProfileUiState.Content ?: return
        if (content.isOpeningChat || !content.relationship.canMessage) return
        updateContent { it.copy(isOpeningChat = true, actionError = null) }
        viewModelScope.launch {
            when (val result = chatRepository.openDirectConversation(userId)) {
                is AppResult.Success -> updateContent {
                    it.copy(isOpeningChat = false, openConversation = ConversationRoute(result.data, it.profile.displayName, userId))
                }
                is AppResult.Failure -> updateContent { it.copy(isOpeningChat = false, actionError = result.error) }
            }
        }
    }

    fun onConversationOpened() = updateContent { it.copy(openConversation = null) }

    fun onSongClick(kind: LibraryKind, index: Int) {
        val content = _uiState.value as? UserProfileUiState.Content ?: return
        playbackController.playOrToggle(content.library.songs(kind), index)
    }

    private fun load(showLoading: Boolean) {
        viewModelScope.launch {
            if (showLoading) _uiState.value = UserProfileUiState.Loading
            val profileCall = async { userRepository.getProfile(userId) }
            val relationshipCall = async { userRepository.getRelationship(userId) }
            val profile = profileCall.await()
            val relationship = relationshipCall.await()
            if (profile is AppResult.Failure || relationship is AppResult.Failure) {
                val error = (profile as? AppResult.Failure)?.error ?: (relationship as AppResult.Failure).error
                if (showLoading || _uiState.value !is UserProfileUiState.Content) _uiState.value = UserProfileUiState.Error(error)
                return@launch
            }
            val loadedRelationship = (relationship as AppResult.Success).data
            val previous = _uiState.value as? UserProfileUiState.Content
            val active = playbackController.state.value.let { it.song?.id to it.isPlaying }
            _uiState.value = UserProfileUiState.Content(
                profile = (profile as AppResult.Success).data,
                relationship = loadedRelationship,
                library = if (loadedRelationship.canSeeLibrary) previous?.library ?: LibraryLists().withActive(active) else LibraryLists().withActive(active),
                isLibraryLoading = loadedRelationship.canSeeLibrary,
                actionError = previous?.actionError,
            )
            if (loadedRelationship.canSeeLibrary) loadLibrary(active)
        }
    }

    private suspend fun loadLibrary(active: Pair<String?, Boolean>) {
        val liked = viewModelScope.async { libraryRepository.getSongs(userId, LibraryKind.LIKED, LIBRARY_LIMIT).getOrNull() }
        val saved = viewModelScope.async { libraryRepository.getSongs(userId, LibraryKind.SAVED, LIBRARY_LIMIT).getOrNull() }
        // Only their public playlists come back (database rule).
        val playlists = viewModelScope.async { playlistRepository.getPlaylists(userId).getOrNull() }
        val likedSongs = liked.await()
        val savedSongs = saved.await()
        val playlistList = playlists.await()
        updateContent {
            it.copy(
                isLibraryLoading = false,
                // A list that failed to load keeps what was shown before; the next visit tries again.
                library = LibraryLists(
                    liked = likedSongs ?: it.library.liked,
                    saved = savedSongs ?: it.library.saved,
                    playlists = playlistList ?: it.library.playlists,
                ).withActive(active),
            )
        }
    }

    private inline fun updateContent(crossinline change: (UserProfileUiState.Content) -> UserProfileUiState.Content) {
        _uiState.update { state -> (state as? UserProfileUiState.Content)?.let(change) ?: state }
    }

    companion object {
        /** Matches the property name of UserProfileRoute, which is how Navigation stores it. */
        const val ARG_USER_ID = "userId"
        private const val LIBRARY_LIMIT = 50

        /** Label to show on the follow button. */
        fun followLabel(relationship: Relationship): FollowLabel = when (relationship.status) {
            FollowStatus.FOLLOWING -> FollowLabel.FOLLOWING
            FollowStatus.REQUESTED -> FollowLabel.REQUESTED
            FollowStatus.NONE -> if (relationship.followsMe) FollowLabel.FOLLOW_BACK else FollowLabel.FOLLOW
        }
    }

    enum class FollowLabel { FOLLOW, FOLLOW_BACK, REQUESTED, FOLLOWING }
}
