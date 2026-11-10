package com.yourcompany.musicsocial.data.music.mapper

import com.yourcompany.musicsocial.core.database.entity.CachedArtist
import com.yourcompany.musicsocial.core.database.entity.CachedSongEntity
import com.yourcompany.musicsocial.core.domain.model.Artist
import com.yourcompany.musicsocial.core.domain.model.CatalogRef
import com.yourcompany.musicsocial.core.domain.model.Song
import com.yourcompany.musicsocial.data.music.datasource.SongDto

internal fun SongDto.toDomain(): Song = Song(
    id = id,
    title = title,
    artists = songArtists.sortedBy { it.position }.map { Artist(it.artist.id, it.artist.name, it.artist.imageUrl) },
    albumTitle = albumTitle,
    artworkUrl = artworkUrl,
    durationMs = durationMs,
    previewUrl = previewUrl,
    externalUrl = externalUrl,
    catalogRef = catalogRefOf(catalogProvider, catalogExternalId),
)

internal fun CachedSongEntity.toDomain(): Song = Song(
    id = id,
    title = title,
    artists = artists.map { Artist(it.id, it.name, it.imageUrl) },
    albumTitle = albumTitle,
    artworkUrl = artworkUrl,
    durationMs = durationMs,
    previewUrl = previewUrl,
    externalUrl = externalUrl,
    catalogRef = catalogRefOf(catalogProvider, catalogExternalId),
)

internal fun Song.toEntity(cachedAtEpochMs: Long): CachedSongEntity = CachedSongEntity(
    id = id,
    title = title,
    artists = artists.map { CachedArtist(it.id, it.name, it.imageUrl) },
    albumTitle = albumTitle,
    artworkUrl = artworkUrl,
    durationMs = durationMs,
    previewUrl = previewUrl,
    externalUrl = externalUrl,
    catalogProvider = catalogRef?.provider,
    catalogExternalId = catalogRef?.externalId,
    cachedAtEpochMs = cachedAtEpochMs,
)

private fun catalogRefOf(provider: String?, externalId: String?): CatalogRef? =
    if (provider != null && externalId != null) CatalogRef(provider, externalId) else null
