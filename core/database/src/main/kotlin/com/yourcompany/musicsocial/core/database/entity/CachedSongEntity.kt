package com.yourcompany.musicsocial.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Local copy of a catalogue song. This is a cache: the backend `songs` table is the source of
 * truth, and rows may be evicted at any time. [cachedAtEpochMs] drives freshness checks.
 */
@Entity(
    tableName = "cached_songs",
    indices = [Index("cached_at")],
)
data class CachedSongEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** Ordered list of credited artists, stored as JSON (see Converters). */
    val artists: List<CachedArtist>,
    @ColumnInfo(name = "album_title") val albumTitle: String?,
    @ColumnInfo(name = "artwork_url") val artworkUrl: String?,
    @ColumnInfo(name = "duration_ms") val durationMs: Long,
    @ColumnInfo(name = "preview_url") val previewUrl: String?,
    @ColumnInfo(name = "external_url") val externalUrl: String?,
    @ColumnInfo(name = "catalog_provider") val catalogProvider: String?,
    @ColumnInfo(name = "catalog_external_id") val catalogExternalId: String?,
    @ColumnInfo(name = "cached_at") val cachedAtEpochMs: Long,
)

@Serializable
data class CachedArtist(
    val id: String,
    val name: String,
    val imageUrl: String? = null,
)
