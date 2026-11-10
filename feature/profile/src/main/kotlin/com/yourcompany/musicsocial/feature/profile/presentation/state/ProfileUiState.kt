package com.yourcompany.musicsocial.feature.profile.presentation.state

import androidx.compose.runtime.Immutable
import com.yourcompany.musicsocial.core.domain.model.Artist
import com.yourcompany.musicsocial.core.domain.model.Playlist
import com.yourcompany.musicsocial.core.domain.model.Song
import com.yourcompany.musicsocial.core.domain.model.UserProfile
import com.yourcompany.musicsocial.core.sample.LastPlayed

/** Real account state from AuthRepository (independent of the sample profile content). */
sealed interface AccountStatus {
    data object Loading : AccountStatus

    /** This build has no backend credentials, so there is no account system yet. */
    data object BackendUnavailable : AccountStatus

    data object SignedOut : AccountStatus

    data class SignedIn(val userId: String) : AccountStatus
}

@Immutable
data class ProfileUiState(
    val account: AccountStatus,
    val profile: UserProfile,
    val topArtists: List<Artist>,
    val favoriteSongs: List<Song>,
    val publicMixes: List<Playlist>,
    val lastPlayed: LastPlayed?,
    /** Ids of songs the user has un-hearted on this screen (local until likes are persisted). */
    val unlikedSongIds: Set<String> = emptySet(),
    val isSample: Boolean,
)
