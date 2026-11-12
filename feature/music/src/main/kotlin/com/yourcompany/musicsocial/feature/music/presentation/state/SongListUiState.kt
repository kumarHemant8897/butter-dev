package com.yourcompany.musicsocial.feature.music.presentation.state

import androidx.compose.runtime.Immutable
import com.yourcompany.musicsocial.core.domain.model.Song
import com.yourcompany.musicsocial.core.domain.result.AppError

/** What kind of list is shown; decides the header copy. */
enum class SongListType { CHART, GENRE, ARTIST, PLAYLIST }

@Immutable
data class SongListUiState(
    val type: SongListType,
    /** From the route at first, replaced by the real name once loaded. */
    val title: String,
    /** Artist genre on artist pages. */
    val subtitle: String? = null,
    val artworkUrl: String? = null,
    val content: SongListContent = SongListContent.Loading,
    val activeSongId: String? = null,
    val isActiveSongPlaying: Boolean = false,
    /** Playlists only: the signed-in user owns it, so songs can be removed and the playlist deleted. */
    val isOwner: Boolean = false,
    /** Playlists only. */
    val isPublic: Boolean = true,
    /** A song waiting for "Remove from playlist?" confirmation. */
    val pendingRemoval: Song? = null,
    val isConfirmingDelete: Boolean = false,
    /** The playlist was deleted; the screen goes back. */
    val isDeleted: Boolean = false,
    val actionError: AppError? = null,
) {
    val songs: List<Song> get() = (content as? SongListContent.Songs)?.songs.orEmpty()
}

sealed interface SongListContent {
    data object Loading : SongListContent

    data class Songs(val songs: List<Song>) : SongListContent

    data class Error(val error: AppError) : SongListContent
}
