package com.yourcompany.musicsocial.data.user.datasource

import com.yourcompany.musicsocial.core.common.logging.AppLogger
import com.yourcompany.musicsocial.core.domain.result.AppResult
import com.yourcompany.musicsocial.core.domain.result.map
import com.yourcompany.musicsocial.core.network.SupabaseClientProvider
import com.yourcompany.musicsocial.core.network.call
import com.yourcompany.musicsocial.core.network.requireUserId
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

/** `public.playlists` and `public.playlist_songs`. RLS decides who sees what. */
internal interface PlaylistRemoteDataSource {
    suspend fun getPlaylists(ownerId: String): AppResult<List<PlaylistDto>>

    /** Null when the playlist does not exist or is not visible to the signed-in user. */
    suspend fun getPlaylist(playlistId: String): AppResult<PlaylistDto?>

    suspend fun getSongs(playlistId: String): AppResult<List<PlaylistSongDto>>

    suspend fun insertPlaylist(title: String, isPublic: Boolean): AppResult<PlaylistDto>

    suspend fun deletePlaylist(playlistId: String): AppResult<Unit>

    suspend fun insertSong(row: PlaylistSongDto): AppResult<Unit>

    suspend fun deleteSong(playlistId: String, songId: String): AppResult<Unit>

    fun currentUserId(): String?
}

@Serializable
internal data class PlaylistDto(
    val id: String,
    @SerialName("owner_id") val ownerId: String,
    val title: String,
    val description: String? = null,
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("is_public") val isPublic: Boolean = true,
    @SerialName("song_count") val songCount: Int = 0,
)

@Serializable
internal data class PlaylistSongDto(
    @SerialName("playlist_id") val playlistId: String,
    @SerialName("song_id") val songId: String,
    val title: String,
    @SerialName("artist_names") val artistNames: String,
    @SerialName("album_title") val albumTitle: String? = null,
    @SerialName("artwork_url") val artworkUrl: String? = null,
    @SerialName("external_url") val externalUrl: String? = null,
    @SerialName("duration_ms") val durationMs: Long = 0,
)

internal class SupabasePlaylistRemoteDataSource @Inject constructor(
    private val supabase: SupabaseClientProvider,
    private val logger: AppLogger,
) : PlaylistRemoteDataSource {

    override suspend fun getPlaylists(ownerId: String): AppResult<List<PlaylistDto>> =
        supabase.call(logger, "playlists.list") { client ->
            client.from(PLAYLISTS).select(PLAYLIST_COLUMNS) {
                filter { eq("owner_id", ownerId) }
                order("updated_at", Order.DESCENDING)
                limit(MAX_PLAYLISTS)
            }.decodeList<PlaylistDto>()
        }

    override suspend fun getPlaylist(playlistId: String): AppResult<PlaylistDto?> =
        supabase.call(logger, "playlists.get") { client ->
            client.from(PLAYLISTS).select(PLAYLIST_COLUMNS) { filter { eq("id", playlistId) } }.decodeSingleOrNull<PlaylistDto>()
        }

    override suspend fun getSongs(playlistId: String): AppResult<List<PlaylistSongDto>> =
        supabase.call(logger, "playlists.songs") { client ->
            client.from(SONGS).select(SONG_COLUMNS) {
                filter { eq("playlist_id", playlistId) }
                order("added_at", Order.ASCENDING)
                limit(MAX_SONGS)
            }.decodeList<PlaylistSongDto>()
        }

    override suspend fun insertPlaylist(title: String, isPublic: Boolean): AppResult<PlaylistDto> =
        supabase.call(logger, "playlists.insert") { client ->
            client.from(PLAYLISTS).insert(NewPlaylist(ownerId = client.requireUserId(), title = title, isPublic = isPublic)) {
                select(PLAYLIST_COLUMNS)
            }.decodeSingle<PlaylistDto>()
        }

    override suspend fun deletePlaylist(playlistId: String): AppResult<Unit> =
        supabase.call(logger, "playlists.delete") { client ->
            val me = client.requireUserId()
            client.from(PLAYLISTS).delete {
                filter {
                    eq("id", playlistId)
                    eq("owner_id", me)
                }
            }
        }.map { }

    override suspend fun insertSong(row: PlaylistSongDto): AppResult<Unit> =
        supabase.call(logger, "playlists.addSong") { client -> client.from(SONGS).insert(row) }.map { }

    override suspend fun deleteSong(playlistId: String, songId: String): AppResult<Unit> =
        supabase.call(logger, "playlists.removeSong") { client ->
            client.from(SONGS).delete {
                filter {
                    eq("playlist_id", playlistId)
                    eq("song_id", songId)
                }
            }
        }.map { }

    override fun currentUserId(): String? = supabase.clientOrNull()?.auth?.currentUserOrNull()?.id

    @Serializable
    private data class NewPlaylist(
        @SerialName("owner_id") val ownerId: String,
        val title: String,
        @SerialName("is_public") val isPublic: Boolean,
    )

    private companion object {
        const val PLAYLISTS = "playlists"
        const val SONGS = "playlist_songs"
        const val MAX_PLAYLISTS = 100L
        const val MAX_SONGS = 500L
        val PLAYLIST_COLUMNS = Columns.list("id", "owner_id", "title", "description", "cover_url", "is_public", "song_count")
        val SONG_COLUMNS = Columns.list(
            "playlist_id", "song_id", "title", "artist_names", "album_title", "artwork_url", "external_url", "duration_ms",
        )
    }
}
