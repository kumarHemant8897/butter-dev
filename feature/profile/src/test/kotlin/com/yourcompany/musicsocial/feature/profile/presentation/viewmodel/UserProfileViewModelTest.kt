package com.yourcompany.musicsocial.feature.profile.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.yourcompany.musicsocial.core.domain.model.FollowRequest
import com.yourcompany.musicsocial.core.domain.model.FollowStatus
import com.yourcompany.musicsocial.core.domain.model.LibraryKind
import com.yourcompany.musicsocial.core.domain.model.Relationship
import com.yourcompany.musicsocial.core.domain.model.Song
import com.yourcompany.musicsocial.core.domain.model.UserProfile
import com.yourcompany.musicsocial.core.domain.result.AppError
import com.yourcompany.musicsocial.core.navigation.ConversationRoute
import com.yourcompany.musicsocial.core.testing.MainDispatcherRule
import com.yourcompany.musicsocial.core.testing.fake.FakeChatRepository
import com.yourcompany.musicsocial.core.testing.fake.FakeLibraryRepository
import com.yourcompany.musicsocial.core.testing.fake.FakePlaybackController
import com.yourcompany.musicsocial.core.testing.fake.FakeUserRepository
import com.yourcompany.musicsocial.feature.profile.presentation.state.UserProfileUiState
import com.yourcompany.musicsocial.feature.profile.presentation.viewmodel.UserProfileViewModel.FollowLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class UserProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val luna = UserProfile("u2", "luna.waves", "Luna Waves", "late nights", null, 7, 3)
    private val users = FakeUserRepository(meId = "me", initialProfiles = listOf(luna))
    private val library = FakeLibraryRepository(meId = "me")
    private val chats = FakeChatRepository(meId = "me")
    private val playback = FakePlaybackController()

    private fun song(id: String) = Song("itunes:$id", "Song $id", emptyList(), null, null, 0, null, null, null)

    private fun viewModel() = UserProfileViewModel(
        SavedStateHandle(mapOf(UserProfileViewModel.ARG_USER_ID to "u2")),
        users,
        library,
        playlists,
        chats,
        playback,
    )

    private val playlists = com.yourcompany.musicsocial.core.testing.fake.FakePlaylistRepository(meId = "me")

    @Test
    fun `an approved follower sees their playlists, a stranger does not even ask`() {
        playlists.playlists += com.yourcompany.musicsocial.core.domain.model.Playlist("pl-1", "u2", "Rainy days", null, null, true, 4)

        assertTrue(content(viewModel()).library.playlists.isEmpty())

        users.following += "u2"
        assertEquals(listOf("Rainy days"), content(viewModel()).library.playlists.map { it.title })
    }

    private fun content(viewModel: UserProfileViewModel) = viewModel.uiState.value as UserProfileUiState.Content

    @Test
    fun `a stranger sees name, bio and counts but not their songs, and cannot message`() {
        library.songs["u2" to LibraryKind.LIKED] = mutableListOf(song("1"))

        val state = content(viewModel())

        assertEquals(luna, state.profile)
        assertEquals(FollowStatus.NONE, state.relationship.status)
        assertTrue(state.library.liked.isEmpty())
        assertTrue(library.requests.isEmpty())
        assertFalse(state.relationship.canMessage)
    }

    @Test
    fun `follow sends a request, a second tap cancels it`() {
        val viewModel = viewModel()

        viewModel.onFollowClick()
        assertEquals(FollowStatus.REQUESTED, content(viewModel).relationship.status)
        assertTrue("u2" in users.requested)

        viewModel.onFollowClick()
        assertEquals(FollowStatus.NONE, content(viewModel).relationship.status)
        assertTrue(users.requested.isEmpty())
    }

    @Test
    fun `an approved follower sees liked and saved songs and can play them`() {
        users.following += "u2"
        library.songs["u2" to LibraryKind.LIKED] = mutableListOf(song("1"), song("2"))
        library.songs["u2" to LibraryKind.SAVED] = mutableListOf(song("3"))
        val viewModel = viewModel()

        val state = content(viewModel)
        assertEquals(listOf("itunes:1", "itunes:2"), state.library.liked.map { it.id })
        assertEquals(listOf("itunes:3"), state.library.saved.map { it.id })
        assertTrue(state.relationship.canMessage)

        viewModel.onSongClick(LibraryKind.SAVED, 0)
        assertEquals(listOf(listOf(song("3")) to 0), playback.playRequests)
    }

    @Test
    fun `unfollowing hides their songs at once and lowers the follower count`() {
        users.following += "u2"
        library.songs["u2" to LibraryKind.LIKED] = mutableListOf(song("1"))
        val viewModel = viewModel()

        viewModel.onFollowClick()

        val state = content(viewModel)
        assertEquals(FollowStatus.NONE, state.relationship.status)
        assertTrue(state.library.liked.isEmpty())
        assertEquals(6, state.profile.followerCount)
        assertFalse("u2" in users.following)
    }

    @Test
    fun `a failed follow puts the button back and shows the error`() {
        val viewModel = viewModel()
        users.failWith = AppError.Network.Unavailable

        viewModel.onFollowClick()

        val state = content(viewModel)
        assertEquals(FollowStatus.NONE, state.relationship.status)
        assertEquals(AppError.Network.Unavailable, state.actionError)
    }

    @Test
    fun `Message opens the direct chat when they follow me, even before I follow them`() {
        users.followers += "u2"
        chats.allowedPeers += "u2"
        val viewModel = viewModel()
        assertEquals(FollowLabel.FOLLOW_BACK, UserProfileViewModel.followLabel(content(viewModel).relationship))

        viewModel.onMessageClick()

        assertEquals(ConversationRoute("dm-u2", "Luna Waves", "u2"), content(viewModel).openConversation)
        viewModel.onConversationOpened()
        assertNull(content(viewModel).openConversation)
    }

    @Test
    fun `Message is refused while nobody follows the other`() {
        val viewModel = viewModel()

        viewModel.onMessageClick()

        assertNull(content(viewModel).openConversation)
    }

    @Test
    fun `follow labels cover every state`() {
        assertEquals(FollowLabel.FOLLOW, UserProfileViewModel.followLabel(Relationship(FollowStatus.NONE, followsMe = false)))
        assertEquals(FollowLabel.FOLLOW_BACK, UserProfileViewModel.followLabel(Relationship(FollowStatus.NONE, followsMe = true)))
        assertEquals(FollowLabel.REQUESTED, UserProfileViewModel.followLabel(Relationship(FollowStatus.REQUESTED, followsMe = false)))
        assertEquals(FollowLabel.FOLLOWING, UserProfileViewModel.followLabel(Relationship(FollowStatus.FOLLOWING, followsMe = true)))
    }
}

class FollowRequestsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val users = FakeUserRepository(meId = "me")

    private fun request(id: String, at: Long) = FollowRequest(UserProfile(id, "user$id", "User $id", null, null, 0, 0), Instant.ofEpochSecond(at))

    @Test
    fun `accepting makes the requester a follower and removes the row`() {
        users.incoming += listOf(request("a", 2), request("b", 1))
        val viewModel = FollowRequestsViewModel(users)

        viewModel.onAccept("a")

        assertEquals(listOf("b"), viewModel.uiState.value.requests.map { it.requester.id })
        assertTrue("a" in users.followers)
    }

    @Test
    fun `declining removes the request without a follow`() {
        users.incoming += request("a", 1)
        val viewModel = FollowRequestsViewModel(users)

        viewModel.onDecline("a")

        assertTrue(viewModel.uiState.value.requests.isEmpty())
        assertFalse("a" in users.followers)
    }

    @Test
    fun `a failed answer puts the request back in its place`() {
        users.incoming += listOf(request("a", 2), request("b", 1))
        val viewModel = FollowRequestsViewModel(users)
        users.failWith = AppError.Network.Unavailable

        viewModel.onAccept("b")

        val state = viewModel.uiState.value
        assertEquals(listOf("a", "b"), state.requests.map { it.requester.id })
        assertEquals(AppError.Network.Unavailable, state.actionError)
    }
}
