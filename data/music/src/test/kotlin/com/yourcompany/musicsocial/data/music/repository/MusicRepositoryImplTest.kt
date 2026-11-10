package com.yourcompany.musicsocial.data.music.repository

import com.yourcompany.musicsocial.core.common.logging.AppLogger
import com.yourcompany.musicsocial.core.database.dao.SongDao
import com.yourcompany.musicsocial.core.database.entity.CachedArtist
import com.yourcompany.musicsocial.core.database.entity.CachedSongEntity
import com.yourcompany.musicsocial.core.domain.model.PageRequest
import com.yourcompany.musicsocial.core.domain.result.AppError
import com.yourcompany.musicsocial.core.domain.result.AppResult
import com.yourcompany.musicsocial.core.domain.result.getOrNull
import com.yourcompany.musicsocial.data.music.datasource.ArtistDto
import com.yourcompany.musicsocial.data.music.datasource.MusicRemoteDataSource
import com.yourcompany.musicsocial.data.music.datasource.SongArtistDto
import com.yourcompany.musicsocial.data.music.datasource.SongDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class MusicRepositoryImplTest {

    private val now = Instant.parse("2026-11-09T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val remote = mockk<MusicRemoteDataSource>()
    private val songDao = mockk<SongDao>(relaxUnitFun = true)
    private val logger = mockk<AppLogger>(relaxed = true)
    private val repository = MusicRepositoryImpl(remote, songDao, clock, logger)

    @Test
    fun `fresh cache hit is returned without a network request`() = runTest {
        coEvery { songDao.getById("s1") } returns cachedSong(ageHours = 1)

        val result = repository.getSong("s1")

        assertEquals("Cached title", result.getOrNull()?.title)
        coVerify(exactly = 0) { remote.getSong(any()) }
    }

    @Test
    fun `cache miss fetches remotely, orders artists and stores the result`() = runTest {
        coEvery { songDao.getById("s1") } returns null
        coEvery { remote.getSong("s1") } returns AppResult.Success(remoteSong())
        val stored = slot<List<CachedSongEntity>>()
        coEvery { songDao.upsert(capture(stored)) } returns Unit

        val song = repository.getSong("s1").getOrNull()!!

        assertEquals("Lead Artist, Featured Artist", song.artistNames)
        assertEquals(now.toEpochMilli(), stored.captured.single().cachedAtEpochMs)
    }

    @Test
    fun `stale cache is served when the network is unavailable`() = runTest {
        coEvery { songDao.getById("s1") } returns cachedSong(ageHours = 48)
        coEvery { remote.getSong("s1") } returns AppResult.Failure(AppError.Network.Unavailable)

        val result = repository.getSong("s1")

        assertEquals("Cached title", result.getOrNull()?.title)
    }

    @Test
    fun `stale cache is not served for non-network errors such as a deleted song`() = runTest {
        coEvery { songDao.getById("s1") } returns cachedSong(ageHours = 48)
        coEvery { remote.getSong("s1") } returns AppResult.Failure(AppError.NotFound)

        assertEquals(AppResult.Failure(AppError.NotFound), repository.getSong("s1"))
    }

    @Test
    fun `a failing cache does not break the request`() = runTest {
        coEvery { songDao.getById("s1") } throws IllegalStateException("database is locked")
        coEvery { remote.getSong("s1") } returns AppResult.Success(remoteSong())
        coEvery { songDao.upsert(any()) } throws IllegalStateException("database is locked")

        assertEquals("Remote title", repository.getSong("s1").getOrNull()?.title)
    }

    @Test
    fun `search ignores too-short queries without calling the backend`() = runTest {
        val page = repository.searchSongs(" a ", PageRequest()).getOrNull()!!

        assertEquals(emptyList<Any>(), page.items)
        assertNull(page.nextCursor)
        coVerify(exactly = 0) { remote.searchSongs(any(), any(), any()) }
    }

    @Test
    fun `search returns a next cursor only when a full page came back`() = runTest {
        coEvery { remote.searchSongs("lofi", 0, 2) } returns AppResult.Success(listOf(remoteSong("a"), remoteSong("b")))
        coEvery { remote.searchSongs("lofi", 2, 2) } returns AppResult.Success(listOf(remoteSong("c")))

        val first = repository.searchSongs("lofi", PageRequest(limit = 2)).getOrNull()!!
        val second = repository.searchSongs("lofi", PageRequest(limit = 2, cursor = first.nextCursor)).getOrNull()!!

        assertEquals("2", first.nextCursor)
        assertEquals(listOf("c"), second.items.map { it.id })
        assertNull(second.nextCursor)
    }

    private fun cachedSong(ageHours: Long) = CachedSongEntity(
        id = "s1",
        title = "Cached title",
        artists = listOf(CachedArtist("a1", "Lead Artist")),
        albumTitle = null,
        artworkUrl = null,
        durationMs = 180_000,
        previewUrl = null,
        externalUrl = null,
        catalogProvider = null,
        catalogExternalId = null,
        cachedAtEpochMs = now.minusSeconds(ageHours * 3600).toEpochMilli(),
    )

    private fun remoteSong(id: String = "s1") = SongDto(
        id = id,
        title = "Remote title",
        durationMs = 200_000,
        songArtists = listOf(
            SongArtistDto(position = 2, artist = ArtistDto("a2", "Featured Artist")),
            SongArtistDto(position = 1, artist = ArtistDto("a1", "Lead Artist")),
        ),
    )
}
