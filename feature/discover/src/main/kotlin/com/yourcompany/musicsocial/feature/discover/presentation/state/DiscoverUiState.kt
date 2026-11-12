package com.yourcompany.musicsocial.feature.discover.presentation.state

import androidx.compose.runtime.Immutable
import com.yourcompany.musicsocial.core.domain.model.ArtistProfile
import com.yourcompany.musicsocial.core.domain.model.ChartGenre
import com.yourcompany.musicsocial.core.domain.model.FollowStatus
import com.yourcompany.musicsocial.core.domain.model.Song
import com.yourcompany.musicsocial.core.domain.result.AppError

@Immutable
data class DiscoverUiState(
    val query: String = "",
    /** Top of the real India chart (shared with Home). */
    val topSongs: List<Song> = emptyList(),
    /** The live Bollywood chart: what listeners in India are playing most. */
    val communityChart: CommunityChart? = null,
    /** Lead artists of the India chart, in chart order. */
    val artists: List<ArtistProfile> = emptyList(),
    /** People to follow: real profiles when signed in, sample people when the build has no backend. */
    val people: List<DiscoverPerson> = emptyList(),
    val arePeopleSample: Boolean = false,
    val isLoadingPeople: Boolean = false,
    val peopleError: AppError? = null,
    /** A follow or unfollow failed; the button was put back to its previous state. */
    val actionError: AppError? = null,
    val feelings: List<String> = emptyList(),
    val selectedFeeling: String? = null,
    /** Song search. While it is not [SearchState.Idle] the search results replace the browse sections. */
    val search: SearchState = SearchState.Idle,
    /** Id of the song loaded in the player, to highlight its row. */
    val activeSongId: String? = null,
    val isActiveSongPlaying: Boolean = false,
)

sealed interface SearchState {
    /** Nothing typed (or too short): show the browse sections. */
    data object Idle : SearchState

    data object Loading : SearchState

    data class Results(val songs: List<Song>) : SearchState

    data object Empty : SearchState

    data class Error(val error: AppError) : SearchState
}

/** A genre chart shown as a playlist card; [songs] is the full list the card opens. */
@Immutable
data class CommunityChart(val genre: ChartGenre, val songs: List<Song>) {
    val artworkUrl: String? get() = songs.firstNotNullOfOrNull { it.artworkUrl }
}

@Immutable
data class DiscoverPerson(
    val id: String,
    val name: String,
    val avatarUrl: String?,
    val detail: PersonDetail,
    /** Every account approves followers, so Follow first becomes REQUESTED. */
    val status: FollowStatus,
)

sealed interface PersonDetail {
    /** Sample-only: taste matching needs listening history, which does not exist yet. */
    data class TasteMatch(val percent: Int, val sharedArtists: Int) : PersonDetail

    data class Handle(val username: String, val followerCount: Int) : PersonDetail
}
