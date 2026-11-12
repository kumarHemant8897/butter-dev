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

/** An artist as shown on artist chips and artist pages. [imageUrl] is album art: the catalogue has no artist photos. */
data class ArtistProfile(
    val id: String,
    val name: String,
    val genre: String?,
    val imageUrl: String?,
)

/** An artist page: who they are and their songs, most popular first. */
data class ArtistDetail(
    val artist: ArtistProfile,
    val songs: List<Song>,
)

/** Genre charts the catalogue can provide. [id] is the provider's genre id. */
enum class ChartGenre(val id: String) {
    BOLLYWOOD("1263"),
    ;

    companion object {
        fun fromId(id: String?): ChartGenre? = entries.firstOrNull { it.id == id }
    }
}

data class Playlist(
    val id: String,
    val ownerId: String,
    val title: String,
    val description: String?,
    val coverUrl: String?,
    val isPublic: Boolean,
    val songCount: Int,
)

/** A playlist with its songs in the order they were added. */
data class PlaylistDetail(
    val playlist: Playlist,
    val songs: List<Song>,
)
