package com.yourcompany.musicsocial.data.music.datasource

import com.yourcompany.musicsocial.core.common.logging.AppLogger
import com.yourcompany.musicsocial.core.common.logging.LogCategory
import com.yourcompany.musicsocial.core.domain.result.AppError
import com.yourcompany.musicsocial.core.domain.result.AppResult
import com.yourcompany.musicsocial.core.domain.result.flatMap
import com.yourcompany.musicsocial.core.network.await
import com.yourcompany.musicsocial.core.network.toAppError
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Catalogue from the iTunes Search API, using the Indian storefront so Bollywood titles are found.
 *
 * Terms that shape this class (Apple's Search API documentation):
 * - previews are 30-second clips for promoting the song: the UI shows a link to Apple Music and the
 *   credit "provided courtesy of iTunes" next to them, and the audio is streamed only, never saved;
 * - the API is limited to roughly 20 requests per minute per IP, so requests come from each phone
 *   (every user has their own allowance) and callers delay while the user is typing.
 *
 * Everything in the response is untrusted: URLs are only accepted when they are HTTPS on Apple's hosts.
 */
internal class ItunesMusicRemoteDataSource @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val logger: AppLogger,
) : MusicRemoteDataSource {

    /** Chart feeds sometimes hang; give up after a few seconds so the fallback feed gets its turn. */
    private val chartClient: OkHttpClient by lazy { okHttpClient.newBuilder().callTimeout(CHART_TIMEOUT_SECONDS, TimeUnit.SECONDS).build() }

    /** The marketing feed hangs often; when it does, the fallback should start quickly. */
    private val primaryChartClient: OkHttpClient by lazy {
        okHttpClient.newBuilder().callTimeout(PRIMARY_CHART_TIMEOUT_SECONDS, TimeUnit.SECONDS).build()
    }

    override suspend fun searchSongs(query: String, offset: Int, limit: Int): AppResult<List<SongDto>> {
        // The API has no offset parameter: ask for everything up to the end of the wanted page and drop the start.
        val wanted = (offset + limit).coerceIn(1, MAX_RESULTS)
        val url = BASE.newBuilder()
            .addPathSegment("search")
            .addQueryParameter("term", query)
            .addQueryParameter("media", "music")
            .addQueryParameter("entity", "song")
            .addQueryParameter("country", STOREFRONT)
            .addQueryParameter("limit", wanted.toString())
            .build()
        return fetch("itunes.search", url) { body -> decodeSongs(body).drop(offset).take(limit) }
    }

    /**
     * Apple's public "Top Songs" chart for India gives the ranking (ids only); one batch lookup then
     * returns full details and preview addresses for all of them. Two requests for the whole chart.
     */
    override suspend fun getTopSongs(limit: Int): AppResult<List<SongDto>> {
        val count = limit.coerceIn(1, MAX_CHART)
        val chartUrl = CHART_BASE.newBuilder()
            .addPathSegments("api/v2/${STOREFRONT.lowercase()}/music/most-played/$count/songs.json")
            .build()
        // The marketing feed sometimes answers 502 or times out; Apple's older iTunes feed (another
        // server, same chart) is the fallback so Home does not depend on one flaky endpoint.
        val idsResult = fetch("apple.chart", chartUrl, primaryChartClient) { body -> chartIds(JSON.decodeFromString<ChartResponse>(body)) }
            .let { primary ->
                if (primary is AppResult.Failure || (primary as AppResult.Success).data.isEmpty()) {
                    val legacyUrl = BASE.newBuilder()
                        .addPathSegments("${STOREFRONT.lowercase()}/rss/topsongs/limit=$count/json")
                        .build()
                    fetch("itunes.legacyChart", legacyUrl, chartClient) { body -> legacyChartIds(JSON.decodeFromString<LegacyChartResponse>(body)) }
                } else {
                    primary
                }
            }
        return idsResult.flatMap { ids -> lookupSongsInOrder(ids) }
    }

    /** Genre charts only exist in the older iTunes feed, e.g. `in/rss/topsongs/limit=25/genre=1263/json`. */
    override suspend fun getGenreChart(genreId: String, limit: Int): AppResult<List<SongDto>> {
        if (!DIGITS.matches(genreId)) return AppResult.Failure(AppError.NotFound)
        val count = limit.coerceIn(1, MAX_CHART)
        val url = BASE.newBuilder()
            .addPathSegments("${STOREFRONT.lowercase()}/rss/topsongs/limit=$count/genre=$genreId/json")
            .build()
        return fetch("itunes.genreChart", url, chartClient) { body -> legacyChartIds(JSON.decodeFromString<LegacyChartResponse>(body)) }
            .flatMap { ids -> lookupSongsInOrder(ids) }
    }

    override suspend fun getArtists(artistIds: List<String>): AppResult<List<ArtistInfoDto>> {
        val ids = artistIds.mapNotNull(::parseArtistId).distinct()
        if (ids.isEmpty()) return AppResult.Success(emptyList())
        val url = BASE.newBuilder()
            .addPathSegment("lookup")
            .addQueryParameter("id", ids.joinToString(","))
            .addQueryParameter("country", STOREFRONT)
            .build()
        return fetch("itunes.lookupArtists", url) { body ->
            val byId = decodeArtists(body).associateBy { it.id }
            ids.mapNotNull { byId["$ARTIST_ID_PREFIX$it"] }
        }
    }

    override suspend fun getArtistWithSongs(artistId: String, limit: Int): AppResult<Pair<ArtistInfoDto, List<SongDto>>> {
        val id = parseArtistId(artistId) ?: return AppResult.Failure(AppError.NotFound)
        val url = BASE.newBuilder()
            .addPathSegment("lookup")
            .addQueryParameter("id", id)
            .addQueryParameter("entity", "song")
            .addQueryParameter("limit", limit.coerceIn(1, MAX_RESULTS).toString())
            .addQueryParameter("country", STOREFRONT)
            .build()
        return fetch("itunes.lookupArtistSongs", url) { body -> decodeArtists(body).firstOrNull() to decodeSongs(body) }
            .flatMap { (artist, songs) ->
                if (artist == null) AppResult.Failure(AppError.NotFound) else AppResult.Success(artist to songs)
            }
    }

    /** One batch lookup for [ids]; results come back in any order, so the given ranking is restored. */
    private suspend fun lookupSongsInOrder(ids: List<String>): AppResult<List<SongDto>> {
        if (ids.isEmpty()) return AppResult.Success(emptyList())
        val url = BASE.newBuilder()
            .addPathSegment("lookup")
            .addQueryParameter("id", ids.joinToString(","))
            .addQueryParameter("country", STOREFRONT)
            .addQueryParameter("entity", "song")
            .build()
        return fetch("itunes.lookupChart", url) { body ->
            val byId = decodeSongs(body).associateBy { it.catalogExternalId }
            ids.mapNotNull { byId[it] }
        }
    }

    override suspend fun getSong(songId: String): AppResult<SongDto> {
        val trackId = parseTrackId(songId) ?: return AppResult.Failure(AppError.NotFound)
        val url = BASE.newBuilder()
            .addPathSegment("lookup")
            .addQueryParameter("id", trackId)
            .addQueryParameter("country", STOREFRONT)
            .addQueryParameter("entity", "song")
            .build()
        return fetch("itunes.lookup", url) { body -> decodeSongs(body).firstOrNull() }
            .flatMap { song -> song?.let { AppResult.Success(it) } ?: AppResult.Failure(AppError.NotFound) }
    }

    override suspend fun getPreviewUrl(songId: String): AppResult<String> =
        getSong(songId).flatMap { song ->
            song.previewUrl?.let { AppResult.Success(it) } ?: AppResult.Failure(AppError.NotFound)
        }

    /** Runs the request and hands the body to [parse]; a body that does not parse counts as a failure. */
    private suspend fun <T> fetch(operation: String, url: HttpUrl, client: OkHttpClient = okHttpClient, parse: (String) -> T): AppResult<T> =
        try {
            val request = Request.Builder().url(url).header("Accept", "application/json").build()
            client.newCall(request).await().use { response ->
                if (!response.isSuccessful) {
                    // iTunes answers 403 (not 429) when the rate limit is exceeded; both mean "try again later".
                    val error = if (response.code == 403 || response.code == 429) AppError.Server(429) else response.code.toAppError()
                    logger.warn(LogCategory.Network, "$operation failed: HTTP ${response.code}")
                    AppResult.Failure(error)
                } else {
                    AppResult.Success(parse(response.body.string()))
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val error = e.toAppError()
            if (error is AppError.Network) {
                logger.warn(LogCategory.Network, "$operation failed: $error")
            } else {
                logger.error(LogCategory.Network, "$operation failed: $error", e)
            }
            AppResult.Failure(error)
        }

    internal companion object {
        private val BASE = "https://itunes.apple.com".toHttpUrl()
        private val CHART_BASE = "https://rss.marketingtools.apple.com".toHttpUrl()
        private val DIGITS = Regex("^\\d{1,20}$")
        private val JSON = Json { ignoreUnknownKeys = true }
        private val TRACK_ID = Regex("^itunes:(\\d{1,20})$")

        /** iTunes returns at most 200 results per request. */
        const val MAX_RESULTS = 200

        /** The chart feed offers 10, 25, 50 or 100 entries; 25 keeps the lookup URL short. */
        const val MAX_CHART = 25
        private const val CHART_TIMEOUT_SECONDS = 8L
        private const val PRIMARY_CHART_TIMEOUT_SECONDS = 4L
        const val STOREFRONT = "IN"
        const val ID_PREFIX = "itunes:"
        const val ARTIST_ID_PREFIX = "itunes-artist:"
        private val ARTIST_ID = Regex("^itunes-artist:(\\d{1,20})$")

        fun parseArtistId(artistId: String): String? = ARTIST_ID.matchEntire(artistId)?.groupValues?.get(1)

        /** Artist rows of a lookup response (wrapperType "artist"). */
        fun decodeArtists(body: String): List<ArtistInfoDto> =
            JSON.decodeFromString<ItunesResponse>(body).results.mapNotNull { row ->
                val id = row.artistId ?: return@mapNotNull null
                val name = row.artistName?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                if (row.wrapperType != "artist") return@mapNotNull null
                ArtistInfoDto(id = "$ARTIST_ID_PREFIX$id", name = name, genre = row.genre?.takeIf { it.isNotBlank() })
            }

        fun parseTrackId(songId: String): String? = TRACK_ID.matchEntire(songId)?.groupValues?.get(1)

        fun decodeSongs(body: String): List<SongDto> = JSON.decodeFromString<ItunesResponse>(body).toSongs()

        /** Chart ids in ranking order; anything that is not a plain number is ignored. */
        fun chartIds(chart: ChartResponse): List<String> =
            chart.feed.results.mapNotNull { entry -> entry.id?.takeIf { DIGITS.matches(it) } }.distinct()

        fun legacyChartIds(chart: LegacyChartResponse): List<String> =
            chart.feed.entry.mapNotNull { entry -> entry.id?.attributes?.imId?.takeIf { DIGITS.matches(it) } }.distinct()

        fun ItunesResponse.toSongs(): List<SongDto> = results.mapNotNull { it.toSongDto() }

        fun ItunesTrack.toSongDto(): SongDto? {
            val id = trackId ?: return null
            val title = trackName?.takeIf { it.isNotBlank() } ?: return null
            val artist = artistName?.takeIf { it.isNotBlank() } ?: return null
            if (kind != "song") return null
            return SongDto(
                id = "$ID_PREFIX$id",
                title = title,
                albumTitle = collectionName?.takeIf { it.isNotBlank() },
                artworkUrl = artworkUrl100.trustedHttpsUrl("mzstatic.com")?.replace("100x100bb", "600x600bb"),
                durationMs = trackTimeMillis ?: 0,
                previewUrl = previewUrl.trustedHttpsUrl("apple.com"),
                externalUrl = trackViewUrl.trustedHttpsUrl("apple.com"),
                catalogProvider = "itunes",
                catalogExternalId = id.toString(),
                songArtists = listOf(SongArtistDto(position = 1, artist = ArtistDto(id = "$ARTIST_ID_PREFIX${artistId ?: id}", name = artist))),
            )
        }

        /** [this] only if it is an HTTPS URL on one of [hostSuffixes]; anything else is dropped. */
        fun String?.trustedHttpsUrl(vararg hostSuffixes: String): String? {
            val url = this?.toHttpUrlOrNull() ?: return null
            if (url.scheme != "https") return null
            return if (hostSuffixes.any { url.host == it || url.host.endsWith(".$it") }) this else null
        }
    }
}

@Serializable
internal data class ItunesResponse(
    val resultCount: Int = 0,
    val results: List<ItunesTrack> = emptyList(),
)

@Serializable
internal data class ItunesTrack(
    val wrapperType: String? = null,
    val kind: String? = null,
    val trackId: Long? = null,
    val artistId: Long? = null,
    val trackName: String? = null,
    val artistName: String? = null,
    val collectionName: String? = null,
    val artworkUrl100: String? = null,
    val previewUrl: String? = null,
    val trackViewUrl: String? = null,
    val trackTimeMillis: Long? = null,
    @SerialName("primaryGenreName") val genre: String? = null,
)

/** Apple's marketing chart feed. Only the ids are used; details come from the lookup API. */
@Serializable
internal data class ChartResponse(val feed: ChartFeed = ChartFeed())

@Serializable
internal data class ChartFeed(val results: List<ChartEntry> = emptyList())

@Serializable
internal data class ChartEntry(val id: String? = null)

/** Apple's older iTunes RSS chart (fallback). Shape: feed.entry[].id.attributes["im:id"]. */
@Serializable
internal data class LegacyChartResponse(val feed: LegacyFeed = LegacyFeed())

@Serializable
internal data class LegacyFeed(val entry: List<LegacyEntry> = emptyList())

@Serializable
internal data class LegacyEntry(val id: LegacyId? = null)

@Serializable
internal data class LegacyId(val attributes: LegacyIdAttributes? = null)

@Serializable
internal data class LegacyIdAttributes(@SerialName("im:id") val imId: String? = null)
