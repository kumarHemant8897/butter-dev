package com.yourcompany.musicsocial.data.user.datasource

import com.yourcompany.musicsocial.core.common.logging.AppLogger
import com.yourcompany.musicsocial.core.domain.result.AppResult
import com.yourcompany.musicsocial.core.domain.result.map
import com.yourcompany.musicsocial.core.network.SupabaseClientProvider
import com.yourcompany.musicsocial.core.network.call
import com.yourcompany.musicsocial.core.network.requireUserId
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

/** `public.library_songs`. RLS returns another user's rows only to their approved followers. */
internal interface LibraryRemoteDataSource {
    suspend fun getMine(): AppResult<List<LibraryKeyDto>>

    suspend fun getSongs(userId: String, kind: String, limit: Int): AppResult<List<LibrarySongDto>>

    suspend fun insert(row: LibrarySongDto): AppResult<Unit>

    suspend fun delete(kind: String, songId: String): AppResult<Unit>
}

@Serializable
internal data class LibraryKeyDto(
    val kind: String,
    @SerialName("song_id") val songId: String,
)

@Serializable
internal data class LibrarySongDto(
    @SerialName("user_id") val userId: String,
    val kind: String,
    @SerialName("song_id") val songId: String,
    val title: String,
    @SerialName("artist_names") val artistNames: String,
    @SerialName("album_title") val albumTitle: String? = null,
    @SerialName("artwork_url") val artworkUrl: String? = null,
    @SerialName("external_url") val externalUrl: String? = null,
    @SerialName("duration_ms") val durationMs: Long = 0,
)

internal class SupabaseLibraryRemoteDataSource @Inject constructor(
    private val supabase: SupabaseClientProvider,
    private val logger: AppLogger,
) : LibraryRemoteDataSource {

    override suspend fun getMine(): AppResult<List<LibraryKeyDto>> =
        supabase.call(logger, "library.mine") { client ->
            val me = client.requireUserId()
            client.from(TABLE).select(Columns.list("kind", "song_id")) { filter { eq("user_id", me) } }
                .decodeList<LibraryKeyDto>()
        }

    override suspend fun getSongs(userId: String, kind: String, limit: Int): AppResult<List<LibrarySongDto>> =
        supabase.call(logger, "library.songs") { client ->
            client.from(TABLE).select(COLUMNS) {
                filter {
                    eq("user_id", userId)
                    eq("kind", kind)
                }
                order("created_at", Order.DESCENDING)
                limit(limit.toLong())
            }.decodeList<LibrarySongDto>()
        }

    override suspend fun insert(row: LibrarySongDto): AppResult<Unit> =
        supabase.call(logger, "library.insert") { client ->
            // The row must name the signed-in user; RLS rejects anything else.
            client.from(TABLE).insert(row.copy(userId = client.requireUserId()))
        }.map { }

    override suspend fun delete(kind: String, songId: String): AppResult<Unit> =
        supabase.call(logger, "library.delete") { client ->
            val me = client.requireUserId()
            client.from(TABLE).delete {
                filter {
                    eq("user_id", me)
                    eq("kind", kind)
                    eq("song_id", songId)
                }
            }
        }.map { }

    private companion object {
        const val TABLE = "library_songs"
        val COLUMNS = Columns.list(
            "user_id", "kind", "song_id", "title", "artist_names", "album_title", "artwork_url", "external_url", "duration_ms",
        )
    }
}
