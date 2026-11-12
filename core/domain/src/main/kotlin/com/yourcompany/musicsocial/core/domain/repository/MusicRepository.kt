package com.yourcompany.musicsocial.core.domain.repository

import com.yourcompany.musicsocial.core.domain.model.ArtistDetail
import com.yourcompany.musicsocial.core.domain.model.ArtistProfile
import com.yourcompany.musicsocial.core.domain.model.ChartGenre
import com.yourcompany.musicsocial.core.domain.model.Page
import com.yourcompany.musicsocial.core.domain.model.PageRequest
import com.yourcompany.musicsocial.core.domain.model.Song
import com.yourcompany.musicsocial.core.domain.result.AppResult

interface MusicRepository {
    /** Returns the song, preferring the local cache and falling back to the backend. */
    suspend fun getSong(songId: String): AppResult<Song>

    suspend fun searchSongs(query: String, page: PageRequest): AppResult<Page<Song>>

    /**
     * Address of the 30-second preview audio for [songId]. Fetched fresh each time and never stored:
     * the provider's terms allow streaming previews but not saving or caching them.
     */
    suspend fun getPreviewUrl(songId: String): AppResult<String>

    /** Most played songs in India right now, in chart order (at most [limit]). */
    suspend fun getTopSongs(limit: Int): AppResult<List<Song>>

    /** Top songs of one genre in India, in chart order (at most [limit]). */
    suspend fun getGenreChart(genre: ChartGenre, limit: Int): AppResult<List<Song>>

    /** The lead artists of the current India chart, in chart order, without duplicates. */
    suspend fun getTopArtists(limit: Int): AppResult<List<ArtistProfile>>

    /** [artistId] as found in [ArtistProfile.id] or [com.yourcompany.musicsocial.core.domain.model.Artist.id]. */
    suspend fun getArtist(artistId: String): AppResult<ArtistDetail>
}
