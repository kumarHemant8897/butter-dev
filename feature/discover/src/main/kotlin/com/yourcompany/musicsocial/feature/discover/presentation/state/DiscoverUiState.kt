package com.yourcompany.musicsocial.feature.discover.presentation.state

import androidx.compose.runtime.Immutable
import com.yourcompany.musicsocial.core.domain.model.Artist
import com.yourcompany.musicsocial.core.domain.model.Playlist
import com.yourcompany.musicsocial.core.sample.ListenerMatch
import com.yourcompany.musicsocial.core.sample.TrendingSong

@Immutable
data class DiscoverUiState(
    val query: String = "",
    val topSongs: List<TrendingSong> = emptyList(),
    val featuredPlaylist: FeaturedPlaylist? = null,
    val artists: List<ArtistWithGenre> = emptyList(),
    val listeners: List<ListenerMatch> = emptyList(),
    /** Ids of people the user has followed from this screen. */
    val followedIds: Set<String> = emptySet(),
    val feelings: List<String> = emptyList(),
    val selectedFeeling: String? = null,
)

@Immutable
data class FeaturedPlaylist(val playlist: Playlist, val saves: Int)

@Immutable
data class ArtistWithGenre(val artist: Artist, val genre: String)
