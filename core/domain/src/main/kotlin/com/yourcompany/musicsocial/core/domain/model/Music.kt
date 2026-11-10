package com.yourcompany.musicsocial.core.domain.model

/**
 * A song in the catalogue.
 *
 * Audio is not assumed to be hosted by us. A song points at a licensed catalogue entry through
 * [catalogRef], and playback uses [previewUrl] only when the provider permits previews.
 */
data class Song(
    val id: String,
    val title: String,
    val artists: List<Artist>,
    val albumTitle: String?,
    val artworkUrl: String?,
    val durationMs: Long,
    val previewUrl: String?,
    val externalUrl: String?,
    val catalogRef: CatalogRef?,
) {
    val artistNames: String get() = artists.joinToString(", ") { it.name }
}

data class Artist(
    val id: String,
    val name: String,
    val imageUrl: String?,
)

/** Reference into an external licensed catalogue, e.g. provider = "spotify", externalId = track id. */
data class CatalogRef(
    val provider: String,
    val externalId: String,
)

data class Playlist(
    val id: String,
    val ownerId: String,
    val title: String,
    val description: String?,
    val coverUrl: String?,
    val isPublic: Boolean,
    val songCount: Int,
)
