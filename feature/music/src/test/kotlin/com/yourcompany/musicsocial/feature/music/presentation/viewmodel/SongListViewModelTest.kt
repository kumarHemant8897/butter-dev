package com.yourcompany.musicsocial.feature.music.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.yourcompany.musicsocial.core.domain.model.Artist
import com.yourcompany.musicsocial.core.domain.model.ArtistDetail
import com.yourcompany.musicsocial.core.domain.model.ArtistProfile
import com.yourcompany.musicsocial.core.domain.model.ChartGenre
import com.yourcompany.musicsocial.core.domain.model.Song
import com.yourcompany.musicsocial.core.domain.playback.PlaybackState
import com.yourcompany.musicsocial.core.domain.result.AppError
import com.yourcompany.musicsocial.core.navigation.SongListKind
import com.yourcompany.musicsocial.core.testing.MainDispatcherRule
import com.yourcompany.musicsocial.core.domain.model.AuthState
import com.yourcompany.musicsocial.core.domain.model.Playlist
import com.yourcompany.musicsocial.core.testing.fake.FakeAuthRepository
import com.yourcompany.musicsocial.core.testing.fake.FakeMusicRepository
import com.yourcompany.musicsocial.core.testing.fake.FakePlaylistRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import com.yourcompany.musicsocial.core.testing.fake.FakePlaybackController
import com.yourcompany.musicsocial.feature.music.presentation.state.SongListContent
import com.yourcompany.musicsocial.feature.music.presentation.state.SongListType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.random.Random

class SongListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun song(id: String, art: String? = null) =
        Song("itunes:$id", "Song $id", listOf(Artist("itunes-artist:1", "Arijit Singh", null)), null, art, 0, null, null, null)

    private val music = FakeMusicRepository()
    private val playlists = FakePlaylistRepository(meId = "me")
    private val playback = FakePlaybackController()

    private fun viewModel(kind: String, title: String = "Title", id: String? = null) = SongListViewModel(
        SavedStateHandle(mapOf(SongListViewModel.ARG_KIND to kind, SongListViewModel.ARG_TITLE to title, SongListViewModel.ARG_ID to id)),
        music,
        playlists,
        FakeAuthRepository(AuthState.SignedIn("me", null)),
        playback,
    )

    private fun playlist(owner: String, vararg songIds: String): String {
        val playlist = Playlist("pl-$owner", owner, "Late night", null, null, true, songIds.size)
        playlists.playlists += playlist
        playlists.songs[playlist.id] = songIds.map { song(it) }.toMutableList()
        return playlist.id
    }

    @Test
    fun `my playlist shows its songs and lets me remove one after confirming`() {
        val id = playlist("me", "1", "2")
        val viewModel = viewModel(SongListKind.PLAYLIST, id = id)
        assertTrue(viewModel.uiState.value.isOwner)
        assertEquals("Late night", viewModel.uiState.value.title)

        viewModel.onRequestRemove(0)
        assertEquals("itunes:1", viewModel.uiState.value.pendingRemoval?.id)
        viewModel.onConfirmRemove()

        assertEquals(listOf("itunes:2"), viewModel.uiState.value.songs.map { it.id })
        assertEquals(listOf("itunes:2"), playlists.songs[id]!!.map { it.id })
    }

    @Test
    fun `someone else's playlist is read only`() {
        val id = playlist("u2", "1")
        val viewModel = viewModel(SongListKind.PLAYLIST, id = id)

        viewModel.onRequestRemove(0)
        viewModel.onRequestDelete()

        assertFalse(viewModel.uiState.value.isOwner)
        assertNull(viewModel.uiState.value.pendingRemoval)
        assertFalse(viewModel.uiState.value.isConfirmingDelete)
    }

    @Test
    fun `deleting my playlist asks first, then closes the screen`() {
        val id = playlist("me", "1")
        val viewModel = viewModel(SongListKind.PLAYLIST, id = id)

        viewModel.onRequestDelete()
        assertTrue(viewModel.uiState.value.isConfirmingDelete)
        viewModel.onConfirmDelete()

        assertTrue(viewModel.uiState.value.isDeleted)
        assertTrue(playlists.playlists.isEmpty())
    }

    @Test
    fun `a failed removal puts the song back`() {
        val id = playlist("me", "1", "2")
        val viewModel = viewModel(SongListKind.PLAYLIST, id = id)
        playlists.failWith = AppError.Network.Unavailable

        viewModel.onRequestRemove(1)
        viewModel.onConfirmRemove()

        assertEquals(listOf("itunes:1", "itunes:2"), viewModel.uiState.value.songs.map { it.id })
        assertEquals(AppError.Network.Unavailable, viewModel.uiState.value.actionError)
    }

    @Test
    fun `see all shows the whole chart in order`() {
        music.topSongs = (1..25).map { song(it.toString()) }

        val state = viewModel(SongListKind.CHART, title = "Trending in India").uiState.value

        assertEquals(SongListType.CHART, state.type)
        assertEquals("Trending in India", state.title)
        assertEquals(music.topSongs.map { it.id }, state.songs.map { it.id })
    }

    @Test
    fun `a genre list loads that genre's chart and uses the first song's art`() {
        music.genreCharts = mapOf(ChartGenre.BOLLYWOOD to listOf(song("1", art = null), song("2", art = "https://is1.mzstatic.com/a.jpg")))

        val state = viewModel(SongListKind.GENRE, id = ChartGenre.BOLLYWOOD.id).uiState.value

        assertEquals(listOf(ChartGenre.BOLLYWOOD), music.genreChartRequests)
        assertEquals(2, state.songs.size)
        assertEquals("https://is1.mzstatic.com/a.jpg", state.artworkUrl)
    }

    @Test
    fun `an unknown genre is not found`() {
        val state = viewModel(SongListKind.GENRE, id = "999").uiState.value

        assertEquals(SongListContent.Error(AppError.NotFound), state.content)
        assertTrue(music.genreChartRequests.isEmpty())
    }

    @Test
    fun `an artist page shows the artist's real name, genre and songs`() {
        val songs = listOf(song("1"), song("2"))
        music.artists = mapOf(
            "itunes-artist:1" to ArtistDetail(ArtistProfile("itunes-artist:1", "Arijit Singh", "Bollywood", "https://is1.mzstatic.com/b.jpg"), songs),
        )

        val state = viewModel(SongListKind.ARTIST, title = "Arijit", id = "itunes-artist:1").uiState.value

        assertEquals(SongListType.ARTIST, state.type)
        assertEquals("Arijit Singh", state.title)
        assertEquals("Bollywood", state.subtitle)
        assertEquals("https://is1.mzstatic.com/b.jpg", state.artworkUrl)
        assertEquals(songs, state.songs)
    }

    @Test
    fun `a failure shows the error and retry loads again`() {
        music.failWith = AppError.Network.Unavailable
        val viewModel = viewModel(SongListKind.ARTIST, id = "itunes-artist:1")
        assertEquals(SongListContent.Error(AppError.Network.Unavailable), viewModel.uiState.value.content)

        music.failWith = null
        music.artists = mapOf("itunes-artist:1" to ArtistDetail(ArtistProfile("itunes-artist:1", "A", null, null), listOf(song("1"))))
        viewModel.onRetry()

        assertEquals(1, viewModel.uiState.value.songs.size)
        assertEquals(listOf("itunes-artist:1", "itunes-artist:1"), music.artistRequests)
    }

    @Test
    fun `tapping a song plays the list from it, tapping the loaded song toggles it`() {
        music.topSongs = (1..3).map { song(it.toString()) }
        val viewModel = viewModel(SongListKind.CHART)

        viewModel.onSongClick(1)
        assertEquals(listOf(music.topSongs to 1), playback.playRequests)

        playback.setState(PlaybackState(song = music.topSongs[1], status = PlaybackState.Status.PLAYING))
        viewModel.onSongClick(1)
        assertEquals(1, playback.toggleCount)
        assertEquals(music.topSongs[1].id, viewModel.uiState.value.activeSongId)
    }

    @Test
    fun `play all starts in order and shuffle starts anywhere with shuffle on`() {
        music.topSongs = (1..5).map { song(it.toString()) }
        val viewModel = viewModel(SongListKind.CHART)
        viewModel.random = Random(seed = 7)
        val expectedShuffleStart = Random(seed = 7).nextInt(5)

        viewModel.onPlayAll()
        viewModel.onShuffle()

        assertEquals(listOf(false, true), playback.shuffleRequests)
        assertEquals(listOf(music.topSongs to 0, music.topSongs to expectedShuffleStart), playback.playRequests)
    }
}
