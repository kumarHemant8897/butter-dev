package com.yourcompany.musicsocial.data.music.repository

import com.yourcompany.musicsocial.core.common.logging.AppLogger
import com.yourcompany.musicsocial.core.common.logging.LogCategory
import com.yourcompany.musicsocial.core.database.dao.SongDao
import com.yourcompany.musicsocial.core.database.entity.CachedSongEntity
import com.yourcompany.musicsocial.core.domain.model.Page
import com.yourcompany.musicsocial.core.domain.model.PageRequest
import com.yourcompany.musicsocial.core.domain.model.Song
import com.yourcompany.musicsocial.core.domain.repository.MusicRepository
import com.yourcompany.musicsocial.core.domain.result.AppError
import com.yourcompany.musicsocial.core.domain.result.AppResult
import com.yourcompany.musicsocial.core.domain.result.map
import com.yourcompany.musicsocial.data.music.datasource.MusicRemoteDataSource
import com.yourcompany.musicsocial.data.music.mapper.toDomain
import com.yourcompany.musicsocial.data.music.mapper.toEntity
import java.time.Clock
import java.time.Duration
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Cache policy for catalogue songs (metadata changes rarely):
 * 1. Fresh cache hit (younger than [CACHE_TTL]) -> return it, no network request.
 * 2. Otherwise fetch remotely and update the cache.
 * 3. If the fetch fails for a network reason and a stale copy exists -> return the stale copy,
 *    so previously seen songs keep working offline.
 */
internal class MusicRepositoryImpl @Inject constructor(
    private val remote: MusicRemoteDataSource,
    private val songDao: SongDao,
    private val clock: Clock,
    private val logger: AppLogger,
) : MusicRepository {

    override suspend fun getSong(songId: String): AppResult<Song> {
        val cached = readCache(songId)
        if (cached != null && cached.isFresh()) return AppResult.Success(cached.toDomain())

        return when (val remoteResult = remote.getSong(songId)) {
            is AppResult.Success -> {
                val song = remoteResult.data.toDomain()
                writeCache(listOf(song))
                AppResult.Success(song)
            }
            is AppResult.Failure ->
                if (cached != null && remoteResult.error is AppError.Network) {
                    AppResult.Success(cached.toDomain())
                } else {
                    remoteResult
                }
        }
    }

    override suspend fun searchSongs(query: String, page: PageRequest): AppResult<Page<Song>> {
        val trimmed = query.trim()
        if (trimmed.length < MIN_QUERY_LENGTH) return AppResult.Success(Page(emptyList(), nextCursor = null))

        val offset = page.cursor?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        return remote.searchSongs(trimmed, offset, page.limit).map { dtos ->
            val songs = dtos.map { it.toDomain() }
            writeCache(songs)
            val reachedEnd = songs.size < page.limit || offset + songs.size >= MAX_SEARCH_RESULTS
            Page(songs, nextCursor = if (reachedEnd) null else (offset + songs.size).toString())
        }
    }

    private fun CachedSongEntity.isFresh(): Boolean =
        clock.millis() - cachedAtEpochMs < CACHE_TTL.toMillis()

    /** A broken cache must never break the feature: failures are logged and treated as a miss. */
    private suspend fun readCache(songId: String): CachedSongEntity? = try {
        songDao.getById(songId)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logger.error(LogCategory.Database, "Reading song cache failed", e)
        null
    }

    private suspend fun writeCache(songs: List<Song>) {
        if (songs.isEmpty()) return
        try {
            val now = clock.millis()
            songDao.upsert(songs.map { it.toEntity(cachedAtEpochMs = now) })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error(LogCategory.Database, "Writing song cache failed", e)
        }
    }

    companion object {
        val CACHE_TTL: Duration = Duration.ofHours(24)
        const val MIN_QUERY_LENGTH = 2
        const val MAX_SEARCH_RESULTS = 200
    }
}
