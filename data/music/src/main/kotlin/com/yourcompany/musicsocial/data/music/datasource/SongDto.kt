package com.yourcompany.musicsocial.data.music.datasource

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Row of `public.songs` with embedded artists (see SONG_COLUMNS). Mirrors the SQL schema. */
@Serializable
internal data class SongDto(
    val id: String,
    val title: String,
    @SerialName("album_title") val albumTitle: String? = null,
    @SerialName("artwork_url") val artworkUrl: String? = null,
    @SerialName("duration_ms") val durationMs: Long = 0,
    @SerialName("preview_url") val previewUrl: String? = null,
    @SerialName("external_url") val externalUrl: String? = null,
    @SerialName("catalog_provider") val catalogProvider: String? = null,
    @SerialName("catalog_external_id") val catalogExternalId: String? = null,
    @SerialName("song_artists") val songArtists: List<SongArtistDto> = emptyList(),
)

@Serializable
internal data class SongArtistDto(
    val position: Int,
    val artist: ArtistDto,
)

@Serializable
internal data class ArtistDto(
    val id: String,
    val name: String,
    @SerialName("image_url") val imageUrl: String? = null,
)
