package com.yourcompany.musicsocial.data.user.repository

import com.yourcompany.musicsocial.core.domain.model.Artist
import com.yourcompany.musicsocial.core.domain.model.LibraryKind
import com.yourcompany.musicsocial.core.domain.model.Song
import com.yourcompany.musicsocial.core.domain.result.AppError
import com.yourcompany.musicsocial.core.domain.result.AppResult
import com.yourcompany.musicsocial.core.domain.result.getOrNull
import com.yourcompany.musicsocial.data.user.datasource.LibraryKeyDto
import com.yourcompany.musicsocial.data.user.datasource.LibraryRemoteDataSource
import com.yourcompany.musicsocial.data.user.datasource.LibrarySongDto
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryRepositoryImplTest {

    private val remote = mockk<LibraryRemoteDataSource>()
    private val repository = LibraryRepositoryImpl(remote)

    private val song = Song(
        id = "itunes:42",
        title = "Kesariya",
        artists = listOf(Artist("itunes-artist:1", "Arijit Singh", null), Artist("itunes-artist:2", "Pritam", null)),
        albumTitle = "Brahmastra",
        artworkUrl = "https://is1.mzstatic.com/a.jpg",
        durationMs = 268_000,
        previewUrl = "https://audio-ssl.itunes.apple.com/clip.m4a",
        externalUrl = "http://not-https.example/x",
        catalogRef = null,
    )

    @Test
    fun `my song ids are grouped by list and unknown lists are ignored`() = runTest {
        coEvery { remote.getMine() } returns AppResult.Success(
            listOf(LibraryKeyDto("liked", "itunes:1"), LibraryKeyDto("saved", "itunes:2"), LibraryKeyDto("liked", "itunes:3"), LibraryKeyDto("pinned", "itunes:4")),
        )

        repository.refreshMine()

        assertEquals(
            mapOf(LibraryKind.LIKED to setOf("itunes:1", "itunes:3"), LibraryKind.SAVED to setOf("itunes:2")),
            repository.mySongIds.value,
        )
    }

    @Test
    fun `adding stores display data only, never the preview address or a non-https link`() = runTest {
        val row = slot<LibrarySongDto>()
        coEvery { remote.insert(capture(row)) } returns AppResult.Success(Unit)

        repository.add(LibraryKind.LIKED, song)

        assertEquals("Arijit Singh, Pritam", row.captured.artistNames)
        assertEquals("https://is1.mzstatic.com/a.jpg", row.captured.artworkUrl)
        assertNull(row.captured.externalUrl)
        assertTrue("itunes:42" in repository.mySongIds.value[LibraryKind.LIKED].orEmpty())
    }

    @Test
    fun `a refused add is undone, a duplicate add is fine`() = runTest {
        coEvery { remote.insert(any()) } returns AppResult.Failure(AppError.Network.Unavailable)
        repository.add(LibraryKind.SAVED, song)
        assertTrue(repository.mySongIds.value[LibraryKind.SAVED].orEmpty().isEmpty())

        coEvery { remote.insert(any()) } returns AppResult.Failure(AppError.Conflict)
        assertEquals(AppResult.Success(Unit), repository.add(LibraryKind.SAVED, song))
        assertTrue("itunes:42" in repository.mySongIds.value[LibraryKind.SAVED].orEmpty())
    }

    @Test
    fun `library rows play through a fresh catalogue lookup`() = runTest {
        coEvery { remote.getSongs("u2", "liked", 50) } returns AppResult.Success(
            listOf(LibrarySongDto("u2", "liked", "itunes:42", "Kesariya", "Arijit Singh")),
        )

        val loaded = repository.getSongs("u2", LibraryKind.LIKED, 50).getOrNull()!!.single()

        assertEquals("itunes:42", loaded.id)
        assertNull(loaded.previewUrl)
        assertEquals("Arijit Singh", loaded.artistNames)
    }
}
