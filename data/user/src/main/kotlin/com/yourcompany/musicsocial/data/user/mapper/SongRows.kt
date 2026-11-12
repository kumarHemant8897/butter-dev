package com.yourcompany.musicsocial.data.user.mapper

import com.yourcompany.musicsocial.core.domain.model.Artist
import com.yourcompany.musicsocial.core.domain.model.CatalogRef
import com.yourcompany.musicsocial.core.domain.model.Song

/**
 * Display data for a catalogue song stored in a user list (library_songs, playlist_songs).
 * The preview address is never stored: the player looks it up at play time (provider terms).
 * Values are cut and filtered to what the database CHECK constraints accept.
 */
internal data class StoredSongFields(
    val songId: String,
    val title: String,
    val artistNames: String,
    val albumTitle: String?,
    val artworkUrl: String?,
    val externalUrl: String?,
    val durationMs: Long,
)

internal fun Song.toStoredFields() = StoredSongFields(
    songId = id,
    title = title.take(300),
    artistNames = artistNames.ifBlank { "Unknown artist" }.take(500),
    albumTitle = albumTitle?.take(300),
    artworkUrl = artworkUrl.httpsOrNull(),
    externalUrl = externalUrl.httpsOrNull(),
    durationMs = durationMs.coerceAtLeast(0),
)

private fun String?.httpsOrNull(): String? = this?.takeIf { it.startsWith("https://") && it.length <= 500 }

/**
 * The stored artist line is display text ("A, B"), so the song gets one display artist. Artist
 * pages are opened from catalogue data, not from stored rows.
 */
internal fun StoredSongFields.toSong() = Song(
    id = songId,
    title = title,
    artists = listOf(Artist(id = "stored:$songId", name = artistNames, imageUrl = null)),
    albumTitle = albumTitle,
    artworkUrl = artworkUrl,
    durationMs = durationMs,
    previewUrl = null,
    externalUrl = externalUrl,
    catalogRef = songId.substringBefore(':', "").takeIf { it.isNotEmpty() }?.let { CatalogRef(it, songId.substringAfter(':')) },
)
