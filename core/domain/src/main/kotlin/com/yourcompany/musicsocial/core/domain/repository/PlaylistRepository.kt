package com.yourcompany.musicsocial.core.domain.repository

import com.yourcompany.musicsocial.core.domain.model.Playlist
import com.yourcompany.musicsocial.core.domain.model.PlaylistDetail
import com.yourcompany.musicsocial.core.domain.model.Song
import com.yourcompany.musicsocial.core.domain.result.AppResult

/**
 * User playlists of catalogue songs. A playlist is visible to its owner, and to the owner's
 * approved followers when public (enforced by the database).
 */
interface PlaylistRepository {
    /** [userId]'s playlists, most recently changed first. Others' private playlists are never returned. */
    suspend fun getPlaylists(userId: String): AppResult<List<Playlist>>

    /** The signed-in user's playlists, most recently changed first. */
    suspend fun getMyPlaylists(): AppResult<List<Playlist>>

    suspend fun getPlaylist(playlistId: String): AppResult<PlaylistDetail>

    /** [title] is trimmed; 1 to 100 characters. */
    suspend fun createPlaylist(title: String, isPublic: Boolean): AppResult<Playlist>

    suspend fun deletePlaylist(playlistId: String): AppResult<Unit>

    /** Idempotent: adding a song already in the playlist succeeds. */
    suspend fun addSong(playlistId: String, song: Song): AppResult<Unit>

    suspend fun removeSong(playlistId: String, songId: String): AppResult<Unit>

    companion object {
        /** Matches the playlists.title CHECK constraint. */
        const val TITLE_MAX_LENGTH = 100
    }
}
