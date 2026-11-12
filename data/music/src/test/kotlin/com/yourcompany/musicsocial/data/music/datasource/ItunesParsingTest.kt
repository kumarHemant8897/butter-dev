package com.yourcompany.musicsocial.data.music.datasource

import com.yourcompany.musicsocial.data.music.datasource.ItunesMusicRemoteDataSource.Companion.toSongDto
import com.yourcompany.musicsocial.data.music.datasource.ItunesMusicRemoteDataSource.Companion.toSongs
import com.yourcompany.musicsocial.data.music.datasource.ItunesMusicRemoteDataSource.Companion.trustedHttpsUrl
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ItunesParsingTest {

    private val json = Json { ignoreUnknownKeys = true }

    /** Made-up data in the shape the API returns, including fields the app ignores. */
    private val response = """
        {"resultCount":3,"results":[
          {"wrapperType":"track","kind":"song","trackId":111,"artistId":9,"trackName":"Test Song",
           "artistName":"A Singer & B Composer","collectionName":"Test Album","primaryGenreName":"Bollywood",
           "artworkUrl100":"https://is1-ssl.mzstatic.com/image/thumb/x/100x100bb.jpg",
           "previewUrl":"https://audio-ssl.itunes.apple.com/clip.m4a",
           "trackViewUrl":"https://music.apple.com/in/album/x?i=111","trackTimeMillis":201000,"isStreamable":true,"currency":"INR"},
          {"wrapperType":"track","kind":"music-video","trackId":222,"trackName":"A Video","artistName":"Someone"},
          {"wrapperType":"track","kind":"song","trackName":"Missing id","artistName":"Someone"}
        ]}
    """.trimIndent()

    @Test
    fun `a song result maps to the app's song with a larger artwork`() {
        val songs = json.decodeFromString<ItunesResponse>(response).toSongs()

        val song = songs.single()
        assertEquals("itunes:111", song.id)
        assertEquals("Test Song", song.title)
        assertEquals("Test Album", song.albumTitle)
        assertEquals(201_000, song.durationMs)
        assertEquals("https://is1-ssl.mzstatic.com/image/thumb/x/600x600bb.jpg", song.artworkUrl)
        assertEquals("https://audio-ssl.itunes.apple.com/clip.m4a", song.previewUrl)
        assertEquals("A Singer & B Composer", song.songArtists.single().artist.name)
        assertEquals("itunes", song.catalogProvider)
        assertEquals("111", song.catalogExternalId)
    }

    @Test
    fun `videos and rows without an id are skipped instead of breaking the list`() {
        val tracks = json.decodeFromString<ItunesResponse>(response).results

        assertEquals(listOf(true, false, false), tracks.map { it.toSongDto() != null })
    }

    @Test
    fun `an empty or unexpected response gives no songs`() {
        assertEquals(emptyList<SongDto>(), json.decodeFromString<ItunesResponse>("""{"resultCount":0,"results":[]}""").toSongs())
        assertEquals(emptyList<SongDto>(), json.decodeFromString<ItunesResponse>("{}").toSongs())
    }

    @Test
    fun `urls are only trusted when they are https on the expected hosts`() {
        assertEquals("https://a.mzstatic.com/x.jpg", "https://a.mzstatic.com/x.jpg".trustedHttpsUrl("mzstatic.com"))
        assertEquals("https://apple.com/x", "https://apple.com/x".trustedHttpsUrl("apple.com"))
        assertNull("http://a.mzstatic.com/x.jpg".trustedHttpsUrl("mzstatic.com"))
        assertNull("https://evil.example/mzstatic.com".trustedHttpsUrl("mzstatic.com"))
        assertNull("https://notapple.com/x".trustedHttpsUrl("apple.com"))
        assertNull("javascript:alert(1)".trustedHttpsUrl("apple.com"))
        assertNull("intent://x#Intent;end".trustedHttpsUrl("apple.com"))
        assertNull(null.trustedHttpsUrl("apple.com"))
    }

    @Test
    fun `untrusted urls in a result are dropped but the song is kept`() {
        val track = ItunesTrack(
            kind = "song", trackId = 5, trackName = "T", artistName = "A",
            previewUrl = "http://audio-ssl.itunes.apple.com/clip.m4a",
            trackViewUrl = "https://evil.example/buy",
            artworkUrl100 = "https://evil.example/100x100bb.jpg",
        )

        val song = track.toSongDto()!!
        assertNull(song.previewUrl)
        assertNull(song.externalUrl)
        assertNull(song.artworkUrl)
    }

    @Test
    fun `chart ids keep the ranking and ignore anything that is not a number`() {
        val chart = json.decodeFromString<ChartResponse>(
            """{"feed":{"title":"Top Songs","results":[{"id":"30"},{"id":"10"},{"id":"x1"},{"id":"30"},{}]}}""",
        )

        assertEquals(listOf("30", "10"), ItunesMusicRemoteDataSource.chartIds(chart))
    }

    @Test
    fun `the fallback chart format gives the same ids`() {
        val legacy = json.decodeFromString<LegacyChartResponse>(
            """{"feed":{"entry":[{"id":{"label":"u","attributes":{"im:id":"30"}}},{"id":{"attributes":{"im:id":"10"}}},{"id":{}}]}}""",
        )

        assertEquals(listOf("30", "10"), ItunesMusicRemoteDataSource.legacyChartIds(legacy))
    }

    @Test
    fun `artist rows are read from a lookup and song rows are left out`() {
        val body = """
            {"resultCount":3,"results":[
              {"wrapperType":"artist","artistId":459885,"artistName":"Arijit Singh","primaryGenreName":"Bollywood"},
              {"wrapperType":"track","kind":"song","trackId":1,"artistId":459885,"artistName":"Arijit Singh","trackName":"Kesariya"},
              {"wrapperType":"artist","artistId":7,"artistName":" "}
            ]}
        """.trimIndent()

        assertEquals(
            listOf(ArtistInfoDto("itunes-artist:459885", "Arijit Singh", "Bollywood")),
            ItunesMusicRemoteDataSource.decodeArtists(body),
        )
        assertEquals(listOf("itunes:1"), ItunesMusicRemoteDataSource.decodeSongs(body).map { it.id })
    }

    @Test
    fun `only well formed artist ids are accepted for lookups`() {
        assertEquals("459885", ItunesMusicRemoteDataSource.parseArtistId("itunes-artist:459885"))
        assertNull(ItunesMusicRemoteDataSource.parseArtistId("itunes-artist:1,2"))
        assertNull(ItunesMusicRemoteDataSource.parseArtistId("itunes:459885"))
    }

    @Test
    fun `only well formed song ids are accepted for lookups`() {
        assertEquals("1635014240", ItunesMusicRemoteDataSource.parseTrackId("itunes:1635014240"))
        assertNull(ItunesMusicRemoteDataSource.parseTrackId("itunes:abc"))
        assertNull(ItunesMusicRemoteDataSource.parseTrackId("itunes:1&limit=500"))
        assertNull(ItunesMusicRemoteDataSource.parseTrackId("spotify:123"))
        assertNull(ItunesMusicRemoteDataSource.parseTrackId("a1b2c3d4-0000-4000-8000-000000000000"))
    }
}
