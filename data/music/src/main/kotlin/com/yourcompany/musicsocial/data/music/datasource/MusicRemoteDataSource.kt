package com.yourcompany.musicsocial.data.music.datasource

import com.yourcompany.musicsocial.core.common.logging.AppLogger
import com.yourcompany.musicsocial.core.domain.result.AppError
import com.yourcompany.musicsocial.core.domain.result.AppResult
import com.yourcompany.musicsocial.core.domain.result.flatMap
import com.yourcompany.musicsocial.core.network.SupabaseClientProvider
import com.yourcompany.musicsocial.core.network.call
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import javax.inject.Inject

/** Remote access to the music catalogue. */
internal interface MusicRemoteDataSource {
    suspend fun getSong(songId: String): AppResult<SongDto>

    /** Offset-based: search results are ranked, not a live feed, and users rarely page deeply. */
    suspend fun searchSongs(query: String, offset: Int, limit: Int): AppResult<List<SongDto>>
}

internal class SupabaseMusicRemoteDataSource @Inject constructor(
    private val supabase: SupabaseClientProvider,
    private val logger: AppLogger,
) : MusicRemoteDataSource {

    override suspend fun getSong(songId: String): AppResult<SongDto> =
        supabase.call(logger, "songs.getSong") { client ->
            client.from(TABLE).select(Columns.raw(SONG_COLUMNS)) {
                filter { eq("id", songId) }
            }.decodeSingleOrNull<SongDto>()
        }.flatMap { dto -> dto?.let { AppResult.Success(it) } ?: AppResult.Failure(AppError.NotFound) }

    override suspend fun searchSongs(query: String, offset: Int, limit: Int): AppResult<List<SongDto>> =
        supabase.call(logger, "songs.search") { client ->
            client.from(TABLE).select(Columns.raw(SONG_COLUMNS)) {
                filter { ilike("title", "%${escapeLike(query)}%") }
                order("title", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                range(offset.toLong(), (offset + limit - 1).toLong())
            }.decodeList<SongDto>()
        }

    private companion object {
        const val TABLE = "songs"

        /** Escapes LIKE wildcards so user input is matched literally. */
        fun escapeLike(input: String): String =
            input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
    }
}
