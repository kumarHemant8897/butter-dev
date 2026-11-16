package com.yourcompany.musicsocial.feature.profile.presentation.state

import androidx.compose.runtime.Immutable
import com.yourcompany.musicsocial.core.domain.model.Artist
import com.yourcompany.musicsocial.core.domain.model.Playlist
import com.yourcompany.musicsocial.core.domain.model.Relationship
import com.yourcompany.musicsocial.core.domain.model.Song
import com.yourcompany.musicsocial.core.domain.model.UserProfile
import com.yourcompany.musicsocial.core.domain.result.AppError
import com.yourcompany.musicsocial.core.navigation.ConversationRoute
import com.yourcompany.musicsocial.core.sample.LastPlayed
import com.yourcompany.musicsocial.feature.profile.presentation.components.LibraryLists

sealed interface ProfileUiState {
    data object Loading : ProfileUiState

    data class Error(val error: AppError) : ProfileUiState

    /**
     * [profile] is the real profile when signed in, with the user's real [library] and the number of
     * follow requests waiting ([requestCount]). When the build has no backend ([sample] != null) it is
     * the sample profile and [sample] carries the sample sections below the header.
     */
    @Immutable
    data class Content(
        val profile: UserProfile,
        val library: LibraryLists = LibraryLists(),
        val requestCount: Int = 0,
        val sample: SampleSections? = null,
        val isSigningOut: Boolean = false,
        val isCreatePlaylistOpen: Boolean = false,
        val isCreatingPlaylist: Boolean = false,
        val actionError: AppError? = null,
    ) : ProfileUiState
}

/** TESTING ONLY: sections shown when the build has no backend. */
@Immutable
data class SampleSections(
    val topArtists: List<Artist>,
    val favoriteSongs: List<Song>,
    val publicMixes: List<Playlist>,
    val lastPlayed: LastPlayed?,
    /** Ids of songs the user has un-hearted on this screen (local only). */
    val unlikedSongIds: Set<String> = emptySet(),
)

/** Someone else's profile. */
sealed interface UserProfileUiState {
    data object Loading : UserProfileUiState

    data class Error(val error: AppError) : UserProfileUiState

    @Immutable
    data class Content(
        val profile: UserProfile,
        val relationship: Relationship,
        /** Filled only when [Relationship.canSeeLibrary]; the database hides it from everyone else. */
        val library: LibraryLists = LibraryLists(),
        val isLibraryLoading: Boolean = false,
        /** A follow, cancel, unfollow or message request is in flight. */
        val isUpdating: Boolean = false,
        val isOpeningChat: Boolean = false,
        val actionError: AppError? = null,
        /** Set once the chat is ready; the screen navigates and calls onConversationOpened. */
        val openConversation: ConversationRoute? = null,
        /** Which confirmation or form is open on top of the profile. */
        val dialog: ProfileDialog? = null,
        val isReporting: Boolean = false,
        /** A report was sent; shown as a short thank-you. */
        val reportSent: Boolean = false,
    ) : UserProfileUiState
}

enum class ProfileDialog { BLOCK, UNBLOCK, REPORT }
