package com.yourcompany.musicsocial.feature.player.presentation.viewmodel

import com.yourcompany.musicsocial.core.domain.model.LibraryKind
import com.yourcompany.musicsocial.core.domain.model.Song
import com.yourcompany.musicsocial.core.domain.playback.PlaybackState
import com.yourcompany.musicsocial.core.domain.result.AppError
import com.yourcompany.musicsocial.core.testing.MainDispatcherRule
import com.yourcompany.musicsocial.core.domain.model.Playlist
import com.yourcompany.musicsocial.core.testing.fake.FakeLibraryRepository
import com.yourcompany.musicsocial.core.testing.fake.FakePlaylistRepository
import com.yourcompany.musicsocial.core.testing.fake.FakePlaybackController
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PlayerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val playback = FakePlaybackController()
    private val library = FakeLibraryRepository()

    // Lazy: created inside the test, after MainDispatcherRule has installed the test dispatcher.
    private val playlists = FakePlaylistRepository()
    private val chats = com.yourcompany.musicsocial.core.testing.fake.FakeChatRepository()
    private val viewModel by lazy { PlayerViewModel(playback, library, playlists, chats) }

    @Test
    fun `share lists my chats and sends the playing song into the one picked`() {
        val luna = com.yourcompany.musicsocial.core.domain.model.ConversationSummary(
            id = "c1",
            type = com.yourcompany.musicsocial.core.domain.model.ConversationType.DIRECT,
            title = null,
            otherUser = com.yourcompany.musicsocial.core.domain.model.UserSummary("u2", "luna", "Luna Waves", null),
            lastMessage = null,
            unreadCount = 0,
            updatedAt = java.time.Instant.EPOCH,
        )
        chats.conversations = listOf(luna)
        playback.play(listOf(song), 0)

        viewModel.onOpenShare()
        assertEquals(listOf("c1"), viewModel.shareUi.value.conversations?.map { it.id })
        viewModel.onShareTo(luna)

        assertEquals(listOf("c1" to com.yourcompany.musicsocial.core.domain.model.OutgoingMessage.SongShare(song, null)), chats.sent)
        assertEquals("Luna Waves", viewModel.playlistUi.value.sharedWith)
        assertEquals(false, viewModel.shareUi.value.isSheetOpen)
    }

    @Test
    fun `add to playlist lists my playlists and puts the playing song in the one picked`() {
        playlists.playlists += Playlist("pl-a", "me", "Late night", null, null, true, 0)
        playback.play(listOf(song), 0)

        viewModel.onOpenAddToPlaylist()
        assertEquals(listOf("Late night"), viewModel.playlistUi.value.playlists?.map { it.title })

        viewModel.onPickPlaylist(playlists.playlists.single())
        assertEquals(listOf(song), playlists.songs["pl-a"])
        assertEquals("Late night", viewModel.playlistUi.value.addedTo)
        assertEquals(false, viewModel.playlistUi.value.isSheetOpen)
    }

    @Test
    fun `a new playlist from the sheet is created with the playing song in it`() {
        playback.play(listOf(song), 0)
        viewModel.onOpenAddToPlaylist()
        viewModel.onNewPlaylistFromSheet()

        viewModel.onCreatePlaylist("Road trip", isPublic = false)

        val created = playlists.playlists.single()
        assertEquals("Road trip", created.title)
        assertEquals(false, created.isPublic)
        assertEquals(listOf(song), playlists.songs[created.id])
        assertEquals(false, viewModel.playlistUi.value.isCreateOpen)
    }

    private val song = Song("itunes:1", "Kesariya", emptyList(), null, null, 0, null, null, null)

    @Test
    fun `the heart adds the loaded song to liked songs and a second tap removes it`() {
        playback.play(listOf(song), 0)

        viewModel.onToggleLike()
        assertEquals(setOf("itunes:1"), viewModel.librarySongIds.value[LibraryKind.LIKED])

        viewModel.onToggleLike()
        assertEquals(emptySet<String>(), viewModel.librarySongIds.value[LibraryKind.LIKED])
    }

    @Test
    fun `save is a separate list and a failure is shown`() {
        playback.play(listOf(song), 0)
        viewModel.onToggleSave()
        assertEquals(setOf("itunes:1"), viewModel.librarySongIds.value[LibraryKind.SAVED])
        assertEquals(emptySet<String>(), viewModel.librarySongIds.value[LibraryKind.LIKED])

        library.failWith = AppError.Network.Unavailable
        viewModel.onToggleSave()
        assertEquals(AppError.Network.Unavailable, viewModel.libraryError.value)
    }

    @Test
    fun `starts with nothing loaded`() {
        assertEquals(PlaybackState(), viewModel.state.value)
    }

    @Test
    fun `taps are forwarded to the player`() {
        viewModel.onTogglePlay()
        viewModel.onNext()
        viewModel.onPrevious()
        viewModel.onCycleRepeat()

        assertEquals(1, playback.toggleCount)
        assertEquals(1, playback.nextCount)
        assertEquals(1, playback.previousCount)
        assertEquals(1, playback.repeatCycles)
    }

    @Test
    fun `shuffle asks for the opposite of what is currently set`() {
        viewModel.onToggleShuffle()
        playback.setState(playback.state.value.copy(shuffle = true))
        viewModel.onToggleShuffle()

        assertEquals(listOf(true, false), playback.shuffleRequests)
    }

    @Test
    fun `seeking turns the bar position into a time within what is playing`() {
        playback.setState(PlaybackState(durationMs = 30_000))

        viewModel.onSeek(0.5f)
        viewModel.onSeek(1.7f)
        viewModel.onSeek(-1f)

        assertEquals(listOf(15_000L, 30_000L, 0L), playback.seekRequests)
    }

    @Test
    fun `seeking does nothing before the length is known`() {
        playback.setState(PlaybackState(durationMs = 0))

        viewModel.onSeek(0.5f)

        assertEquals(emptyList<Long>(), playback.seekRequests)
    }

    @Test
    fun `the screen sees every change the player reports`() {
        playback.setState(PlaybackState(status = PlaybackState.Status.LOADING))

        assertEquals(PlaybackState.Status.LOADING, viewModel.state.value.status)
    }
}
