package com.yourcompany.musicsocial.feature.profile.presentation.viewmodel

import com.yourcompany.musicsocial.core.domain.model.AuthState
import com.yourcompany.musicsocial.core.domain.model.FollowRequest
import com.yourcompany.musicsocial.core.domain.model.LibraryKind
import com.yourcompany.musicsocial.core.domain.model.Song
import com.yourcompany.musicsocial.core.domain.model.UserProfile
import com.yourcompany.musicsocial.core.domain.result.AppError
import com.yourcompany.musicsocial.core.testing.MainDispatcherRule
import com.yourcompany.musicsocial.core.testing.fake.FakeAuthRepository
import com.yourcompany.musicsocial.core.testing.fake.FakeLibraryRepository
import com.yourcompany.musicsocial.core.testing.fake.FakePlaybackController
import com.yourcompany.musicsocial.core.testing.fake.FakeUserRepository
import java.time.Instant
import com.yourcompany.musicsocial.feature.profile.presentation.state.ProfileUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val me = UserProfile("me", "hemant", "Hemant Kumar", "bio", null, 3, 4)
    private val users = FakeUserRepository(meId = "me", initialProfiles = listOf(me))
    private val library = FakeLibraryRepository(meId = "me")
    private val playback = FakePlaybackController()

    private fun content(viewModel: ProfileViewModel) = viewModel.uiState.value as ProfileUiState.Content

    @Suppress("TestFunctionName")
    private fun ProfileViewModel(auth: FakeAuthRepository, users: FakeUserRepository) =
        com.yourcompany.musicsocial.feature.profile.presentation.viewmodel.ProfileViewModel(auth, users, library, playlists, playback)

    private val playlists = com.yourcompany.musicsocial.core.testing.fake.FakePlaylistRepository(meId = "me")

    @Test
    fun `own playlists are listed and a new one appears at the top`() {
        playlists.playlists += com.yourcompany.musicsocial.core.domain.model.Playlist("pl-old", "me", "Old", null, null, true, 3)
        val viewModel = ProfileViewModel(FakeAuthRepository(AuthState.SignedIn("me", null)), users)
        assertEquals(listOf("Old"), content(viewModel).library.playlists.map { it.title })

        viewModel.onNewPlaylist()
        assertTrue(content(viewModel).isCreatePlaylistOpen)
        viewModel.onCreatePlaylist("Road trip", isPublic = true)

        val state = content(viewModel)
        assertFalse(state.isCreatePlaylistOpen)
        assertEquals(listOf("Road trip", "Old"), state.library.playlists.map { it.title })
    }

    private fun song(id: String) = Song("itunes:$id", "Song $id", emptyList(), null, null, 0, null, null, null)

    @Test
    fun `own profile shows real liked and saved songs and the waiting request count`() {
        library.songs["me" to LibraryKind.LIKED] = mutableListOf(song("1"), song("2"))
        library.songs["me" to LibraryKind.SAVED] = mutableListOf(song("3"))
        users.incoming += FollowRequest(UserProfile("u2", "luna", "Luna", null, null, 0, 0), Instant.EPOCH)

        val state = content(ProfileViewModel(FakeAuthRepository(AuthState.SignedIn("me", null)), users))

        assertEquals(listOf("itunes:1", "itunes:2"), state.library.liked.map { it.id })
        assertEquals(listOf("itunes:3"), state.library.saved.map { it.id })
        assertEquals(1, state.requestCount)
    }

    @Test
    fun `tapping a liked song plays the liked list from it`() {
        library.songs["me" to LibraryKind.LIKED] = mutableListOf(song("1"), song("2"))
        val viewModel = ProfileViewModel(FakeAuthRepository(AuthState.SignedIn("me", null)), users)

        viewModel.onSongClick(LibraryKind.LIKED, 1)

        assertEquals(listOf(listOf(song("1"), song("2")) to 1), playback.playRequests)
        assertEquals("itunes:2", content(viewModel).library.activeSongId)
    }

    @Test
    fun `waits in the loading state while the session is restored`() {
        val viewModel = ProfileViewModel(FakeAuthRepository(AuthState.Initializing), users)

        assertEquals(ProfileUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `signed in users see their real profile without sample sections`() {
        val viewModel = ProfileViewModel(FakeAuthRepository(AuthState.SignedIn("me", null)), users)

        val state = content(viewModel)
        assertEquals(me, state.profile)
        assertNull(state.sample)
    }

    @Test
    fun `a load failure shows the error and retry loads again`() {
        users.failWith = AppError.Network.Unavailable
        val viewModel = ProfileViewModel(FakeAuthRepository(AuthState.SignedIn("me", null)), users)
        assertEquals(ProfileUiState.Error(AppError.Network.Unavailable), viewModel.uiState.value)

        users.failWith = null
        viewModel.onRetry()

        assertEquals(me, content(viewModel).profile)
    }

    @Test
    fun `refresh picks up edits without flashing the loading state`() {
        val viewModel = ProfileViewModel(FakeAuthRepository(AuthState.SignedIn("me", null)), users)
        users.profiles["me"] = me.copy(displayName = "Hemant K")

        viewModel.refresh()

        assertEquals("Hemant K", content(viewModel).profile.displayName)
    }

    @Test
    fun `a failed refresh keeps showing the profile the user already sees`() {
        val viewModel = ProfileViewModel(FakeAuthRepository(AuthState.SignedIn("me", null)), users)
        users.failWith = AppError.Network.Timeout

        viewModel.refresh()

        assertEquals(me, content(viewModel).profile)
    }

    @Test
    fun `a build without a backend shows the sample profile with sample sections`() {
        val viewModel = ProfileViewModel(FakeAuthRepository(AuthState.Unavailable), users)

        val state = content(viewModel)
        assertEquals("Hemant Kumar", state.profile.displayName)
        assertNotNull(state.sample)
    }

    @Test
    fun `sign out calls the repository once`() {
        val auth = FakeAuthRepository(AuthState.SignedIn("me", null))
        val viewModel = ProfileViewModel(auth, users)

        viewModel.onSignOut()
        viewModel.onSignOut()

        assertEquals(1, auth.signOutCount)
    }

    @Test
    fun `un-hearting a sample song toggles only that song`() {
        val viewModel = ProfileViewModel(FakeAuthRepository(AuthState.Unavailable), users)
        val songs = content(viewModel).sample!!.favoriteSongs.map { it.id }

        viewModel.onToggleLike(songs[0])
        val unliked = content(viewModel).sample!!.unlikedSongIds
        assertTrue(songs[0] in unliked)
        assertFalse(songs[1] in unliked)

        viewModel.onToggleLike(songs[0])
        assertTrue(content(viewModel).sample!!.unlikedSongIds.isEmpty())
    }
}
