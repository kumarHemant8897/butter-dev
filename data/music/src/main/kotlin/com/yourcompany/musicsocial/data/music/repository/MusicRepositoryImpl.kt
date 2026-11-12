package com.yourcompany.musicsocial.data.music.repository

import com.yourcompany.musicsocial.core.common.logging.AppLogger
import com.yourcompany.musicsocial.core.common.logging.LogCategory
import com.yourcompany.musicsocial.core.database.dao.SongDao
import com.yourcompany.musicsocial.core.database.entity.CachedSongEntity
import com.yourcompany.musicsocial.core.domain.model.ArtistDetail
import com.yourcompany.musicsocial.core.domain.model.ArtistProfile
import com.yourcompany.musicsocial.core.domain.model.ChartGenre
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Cache policy for catalogue songs (metadata changes rarely):
 * 1. Fresh cache hit (younger than [CACHE_TTL]) -> return it, no network request.
 * 2. Otherwise fetch remotely and update the cache.
 * 3. If the fetch fails for a network reason and a stale copy exists -> return the stale copy,
 *    so previously seen songs keep working offline.
 */
@Singleton
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

    override suspend fun getPreviewUrl(songId: String): AppResult<String> = remote.getPreviewUrl(songId)

    /** Last chart and when it was fetched. Home and Discover share it, and it keeps under the provider's rate limit. */
    @Volatile private var chart: Pair<Long, List<Song>>? = null

    /** Home, Discover and the artists row ask for the chart at the same moment; only one download runs. */
    private val chartMutex = Mutex()

    override suspend fun getTopSongs(limit: Int): AppResult<List<Song>> {
        freshChart(limit)?.let { return AppResult.Success(it) }
        return chartMutex.withLock {
            // Another caller may have downloaded it while this one waited.
            freshChart(limit)?.let { return@withLock AppResult.Success(it) }
            remote.getTopSongs(limit).map { dtos ->
                val songs = dtos.map { it.toDomain() }
                writeCache(songs)
                if (songs.isNotEmpty()) chart = clock.millis() to songs
                songs
            }
        }
    }

    private fun freshChart(limit: Int): List<Song>? = chart?.let { (fetchedAt, songs) ->
        if (clock.millis() - fetchedAt < CHART_TTL.toMillis() && songs.size >= limit) songs.take(limit) else null
    }

    /** Genre charts by genre, with fetch time. Same TTL as the main chart. */
    private val genreCharts = java.util.concurrent.ConcurrentHashMap<ChartGenre, Pair<Long, List<Song>>>()

    override suspend fun getGenreChart(genre: ChartGenre, limit: Int): AppResult<List<Song>> {
        genreCharts[genre]?.let { (fetchedAt, songs) ->
            if (clock.millis() - fetchedAt < CHART_TTL.toMillis() && songs.size >= limit) return AppResult.Success(songs.take(limit))
        }
        return remote.getGenreChart(genre.id, limit).map { dtos ->
            val songs = dtos.map { it.toDomain() }
            writeCache(songs)
            if (songs.isNotEmpty()) genreCharts[genre] = clock.millis() to songs
            songs
        }
    }

    @Volatile private var topArtists: Pair<Long, List<ArtistProfile>>? = null

    /**
     * The chart's lead artists in rank order. The catalogue has no artist photos, so each artist is
     * shown with the art of their highest-ranked chart song.
     */
    override suspend fun getTopArtists(limit: Int): AppResult<List<ArtistProfile>> {
        topArtists?.let { (fetchedAt, artists) ->
            if (clock.millis() - fetchedAt < CHART_TTL.toMillis() && artists.size >= limit) return AppResult.Success(artists.take(limit))
        }
        return when (val chartResult = getTopSongs(CHART_FOR_ARTISTS)) {
            is AppResult.Failure -> chartResult
            is AppResult.Success -> {
                val leads = chartResult.data
                    .mapNotNull { song -> song.artists.firstOrNull()?.let { it to song.artworkUrl } }
                    .distinctBy { (artist, _) -> artist.id }
                    .take(limit)
                val artByArtist = leads.associate { (artist, art) -> artist.id to art }
                val nameByArtist = leads.associate { (artist, _) -> artist.id to artist.name }
                remote.getArtists(leads.map { it.first.id }).map { infos ->
                    val infoById = infos.associateBy { it.id }
                    // Keep chart order; if the lookup misses an artist, the name from the chart is enough.
                    val artists = leads.map { (artist, _) ->
                        val info = infoById[artist.id]
                        ArtistProfile(
                            id = artist.id,
                            name = info?.name ?: nameByArtist.getValue(artist.id),
                            genre = info?.genre,
                            imageUrl = artByArtist[artist.id],
                        )
                    }
                    if (artists.isNotEmpty()) topArtists = clock.millis() to artists
                    artists
                }
            }
        }
    }

    override suspend fun getArtist(artistId: String): AppResult<ArtistDetail> =
        remote.getArtistWithSongs(artistId, ARTIST_SONG_LIMIT).map { (info, dtos) ->
            val songs = dtos.map { it.toDomain() }
            writeCache(songs)
            ArtistDetail(
                artist = ArtistProfile(
                    id = info.id,
                    name = info.name,
                    genre = info.genre,
                    imageUrl = songs.firstNotNullOfOrNull { it.artworkUrl },
                ),
                songs = songs,
            )
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
        val CHART_TTL: Duration = Duration.ofMinutes(30)
        const val MIN_QUERY_LENGTH = 2
        const val MAX_SEARCH_RESULTS = 200
        const val CHART_FOR_ARTISTS = 25
        const val ARTIST_SONG_LIMIT = 50
    }
}
