package com.yourcompany.musicsocial.data.user.repository

import com.yourcompany.musicsocial.core.domain.model.Playlist
import com.yourcompany.musicsocial.core.domain.model.PlaylistDetail
import com.yourcompany.musicsocial.core.domain.model.Song
import com.yourcompany.musicsocial.core.domain.repository.PlaylistRepository
import com.yourcompany.musicsocial.core.domain.result.AppError
import com.yourcompany.musicsocial.core.domain.result.AppResult
import com.yourcompany.musicsocial.core.domain.result.map
import com.yourcompany.musicsocial.data.user.datasource.PlaylistDto
import com.yourcompany.musicsocial.data.user.datasource.PlaylistRemoteDataSource
import com.yourcompany.musicsocial.data.user.datasource.PlaylistSongDto
import com.yourcompany.musicsocial.data.user.mapper.StoredSongFields
import com.yourcompany.musicsocial.data.user.mapper.toSong
import com.yourcompany.musicsocial.data.user.mapper.toStoredFields
import javax.inject.Inject

internal class PlaylistRepositoryImpl @Inject constructor(
    private val remote: PlaylistRemoteDataSource,
) : PlaylistRepository {

    override suspend fun getPlaylists(userId: String): AppResult<List<Playlist>> =
        remote.getPlaylists(userId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun getMyPlaylists(): AppResult<List<Playlist>> {
        val me = remote.currentUserId() ?: return AppResult.Failure(AppError.Auth.NotSignedIn)
        return getPlaylists(me)
    }

    override suspend fun getPlaylist(playlistId: String): AppResult<PlaylistDetail> {
        val playlist = when (val result = remote.getPlaylist(playlistId)) {
            is AppResult.Failure -> return result
            // Missing or private to someone else: the database hides both the same way.
            is AppResult.Success -> result.data ?: return AppResult.Failure(AppError.NotFound)
        }
        return remote.getSongs(playlistId).map { rows -> PlaylistDetail(playlist.toDomain(), rows.map { it.toSong() }) }
    }

    override suspend fun createPlaylist(title: String, isPublic: Boolean): AppResult<Playlist> {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return AppResult.Failure(AppError.Validation(FIELD_TITLE, RULE_BLANK))
        if (trimmed.length > PlaylistRepository.TITLE_MAX_LENGTH) return AppResult.Failure(AppError.Validation(FIELD_TITLE, RULE_TOO_LONG))
        return remote.insertPlaylist(trimmed, isPublic).map { it.toDomain() }
    }

    override suspend fun deletePlaylist(playlistId: String): AppResult<Unit> = remote.deletePlaylist(playlistId)

    /** Already in the playlist is what the user asked for, so a duplicate is not an error. */
    override suspend fun addSong(playlistId: String, song: Song): AppResult<Unit> {
        val fields = song.toStoredFields()
        val row = PlaylistSongDto(
            playlistId = playlistId,
            songId = fields.songId,
            title = fields.title,
            artistNames = fields.artistNames,
            albumTitle = fields.albumTitle,
            artworkUrl = fields.artworkUrl,
            externalUrl = fields.externalUrl,
            durationMs = fields.durationMs,
        )
        return when (val result = remote.insertSong(row)) {
            is AppResult.Failure -> if (result.error == AppError.Conflict) AppResult.Success(Unit) else result
            is AppResult.Success -> result
        }
    }

    override suspend fun removeSong(playlistId: String, songId: String): AppResult<Unit> = remote.deleteSong(playlistId, songId)

    internal companion object {
        const val FIELD_TITLE = "title"
        const val RULE_BLANK = "blank"
        const val RULE_TOO_LONG = "too_long"

        fun PlaylistDto.toDomain() = Playlist(
            id = id,
            ownerId = ownerId,
            title = title,
            description = description?.takeIf { it.isNotBlank() },
            coverUrl = coverUrl,
            isPublic = isPublic,
            songCount = songCount.coerceAtLeast(0),
        )

        fun PlaylistSongDto.toSong(): Song =
            StoredSongFields(songId, title, artistNames, albumTitle, artworkUrl, externalUrl, durationMs).toSong()
    }
}
