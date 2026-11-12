package com.yourcompany.musicsocial.data.music.datasource

import com.yourcompany.musicsocial.core.domain.result.AppResult

/**
 * Where catalogue data comes from. Today that is the iTunes Search API ([ItunesMusicRemoteDataSource]);
 * a licensed streaming provider would be another implementation of this interface.
 */
internal interface MusicRemoteDataSource {
    /** [songId] is the app's id for a song, e.g. "itunes:1635014240". */
    suspend fun getSong(songId: String): AppResult<SongDto>

    /**
     * Offset-based: search results are ranked, not a live feed, and people rarely page deeply.
     * Implementations that cannot skip rows server-side fetch more and drop the first [offset].
     */
    suspend fun searchSongs(query: String, offset: Int, limit: Int): AppResult<List<SongDto>>

    suspend fun getPreviewUrl(songId: String): AppResult<String>

    /** The current top songs chart for the storefront, in chart order. */
    suspend fun getTopSongs(limit: Int): AppResult<List<SongDto>>

    /** The current top songs of one genre ([genreId] is the provider's genre id), in chart order. */
    suspend fun getGenreChart(genreId: String, limit: Int): AppResult<List<SongDto>>

    /** Names and genres for [artistIds] (app ids such as "itunes-artist:123"), in the given order. */
    suspend fun getArtists(artistIds: List<String>): AppResult<List<ArtistInfoDto>>

    /** One artist and their most popular songs. */
    suspend fun getArtistWithSongs(artistId: String, limit: Int): AppResult<Pair<ArtistInfoDto, List<SongDto>>>
}

internal data class ArtistInfoDto(val id: String, val name: String, val genre: String?)
