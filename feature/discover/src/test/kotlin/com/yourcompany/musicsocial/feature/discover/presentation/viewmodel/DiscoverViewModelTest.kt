@file:OptIn(ExperimentalCoroutinesApi::class)

package com.yourcompany.musicsocial.feature.discover.presentation.viewmodel

import com.yourcompany.musicsocial.core.domain.model.ArtistProfile
import com.yourcompany.musicsocial.core.domain.model.AuthState
import com.yourcompany.musicsocial.core.domain.model.ChartGenre
import com.yourcompany.musicsocial.core.domain.model.FollowStatus
import com.yourcompany.musicsocial.core.domain.model.Song
import com.yourcompany.musicsocial.core.domain.model.UserProfile
import com.yourcompany.musicsocial.core.domain.result.AppError
import com.yourcompany.musicsocial.core.testing.MainDispatcherRule
import com.yourcompany.musicsocial.core.testing.fake.FakeAuthRepository
import com.yourcompany.musicsocial.core.testing.fake.FakeMusicRepository
import com.yourcompany.musicsocial.core.testing.fake.FakePlaybackController
import com.yourcompany.musicsocial.core.testing.fake.FakeUserRepository
import com.yourcompany.musicsocial.feature.discover.presentation.state.PersonDetail
import com.yourcompany.musicsocial.feature.discover.presentation.state.SearchState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DiscoverViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun profile(id: String, followers: Int = 5) = UserProfile(id, "user_$id", "User $id", null, null, followers, 0)
    private fun song(id: String) = Song("itunes:$id", "Song $id", emptyList(), null, null, 0, null, null, null)

    private val users = FakeUserRepository(
        meId = "me",
        initialProfiles = listOf(profile("me"), profile("a", followers = 5), profile("b", followers = 0)),
    )
    private val music = FakeMusicRepository()
    private val playback = FakePlaybackController()

    private fun signedInViewModel() =
        DiscoverViewModel(FakeAuthRepository(AuthState.SignedIn("me", null)), users, music, playback)

    // ---- people ------------------------------------------------------------------------------

    @Test
    fun `signed in users see other real profiles, not themselves`() {
        val state = signedInViewModel().uiState.value

        assertEquals(setOf("a", "b"), state.people.map { it.id }.toSet())
        assertFalse(state.arePeopleSample)
        assertTrue(state.people.all { it.detail is PersonDetail.Handle })
    }

    @Test
    fun `people start as followed, requested or neither`() {
        users.following += "a"
        users.requested += "b"

        val people = signedInViewModel().uiState.value.people.associateBy { it.id }

        assertEquals(FollowStatus.FOLLOWING, people.getValue("a").status)
        assertEquals(FollowStatus.REQUESTED, people.getValue("b").status)
    }

    @Test
    fun `follow sends a request and does not change the follower count until it is accepted`() {
        val viewModel = signedInViewModel()

        viewModel.onToggleFollow("a")

        val after = viewModel.uiState.value.people.first { it.id == "a" }
        assertTrue("a" in users.requested)
        assertFalse("a" in users.following)
        assertEquals(FollowStatus.REQUESTED, after.status)
        assertEquals(5, (after.detail as PersonDetail.Handle).followerCount)
    }

    @Test
    fun `tapping Requested cancels the request, tapping Following does nothing here`() {
        users.following += "b"
        val viewModel = signedInViewModel()
        viewModel.onToggleFollow("a")

        viewModel.onToggleFollow("a")
        viewModel.onToggleFollow("b")

        val people = viewModel.uiState.value.people.associateBy { it.id }
        assertEquals(FollowStatus.NONE, people.getValue("a").status)
        assertTrue(users.requested.isEmpty())
        assertEquals(FollowStatus.FOLLOWING, people.getValue("b").status)
        assertTrue("b" in users.following)
    }

    @Test
    fun `a failed request is rolled back and the error is shown`() {
        val viewModel = signedInViewModel()
        users.failWith = AppError.Network.Unavailable

        viewModel.onToggleFollow("a")

        val state = viewModel.uiState.value
        assertEquals(FollowStatus.NONE, state.people.first { it.id == "a" }.status)
        assertEquals(AppError.Network.Unavailable, state.actionError)
        assertFalse("a" in users.requested)
    }

    @Test
    fun `a failed people load shows the error and retry recovers`() {
        users.failWith = AppError.Network.Timeout
        val viewModel = signedInViewModel()
        assertEquals(AppError.Network.Timeout, viewModel.uiState.value.peopleError)
        assertTrue(viewModel.uiState.value.people.isEmpty())

        users.failWith = null
        viewModel.onRetryPeople()

        assertNull(viewModel.uiState.value.peopleError)
        assertEquals(2, viewModel.uiState.value.people.size)
    }

    @Test
    fun `without a backend the people are sample taste matches and follow stays local`() {
        val viewModel = DiscoverViewModel(FakeAuthRepository(AuthState.Unavailable), users, music, playback)
        val state = viewModel.uiState.value
        assertTrue(state.arePeopleSample)
        assertTrue(state.people.all { it.detail is PersonDetail.TasteMatch })

        viewModel.onToggleFollow(state.people.first().id)

        assertEquals(FollowStatus.REQUESTED, viewModel.uiState.value.people.first().status)
        assertTrue(users.requested.isEmpty())
    }

    @Test
    fun `selecting a feeling replaces the previous one and reselecting clears it`() {
        val viewModel = signedInViewModel()
        val feelings = viewModel.uiState.value.feelings

        viewModel.onSelectFeeling(feelings[1])
        assertEquals(feelings[1], viewModel.uiState.value.selectedFeeling)

        viewModel.onSelectFeeling(feelings[1])
        assertNull(viewModel.uiState.value.selectedFeeling)
    }

    // ---- top songs ----------------------------------------------------------------------------

    @Test
    fun `the top songs are the first five of the real chart and play as a queue`() {
        music.topSongs = (1..25).map { song(it.toString()) }
        val viewModel = signedInViewModel()

        val top = viewModel.uiState.value.topSongs
        assertEquals((1..5).map { "itunes:$it" }, top.map { it.id })

        viewModel.onTopSongClick(2)
        val (queue, start) = playback.playRequests.single()
        assertEquals(top, queue)
        assertEquals(2, start)
    }

    @Test
    fun `a failed chart simply hides the top songs section`() {
        music.failWith = AppError.Network.Unavailable

        val state = signedInViewModel().uiState.value
        assertTrue(state.topSongs.isEmpty())
        assertTrue(state.artists.isEmpty())
        assertNull(state.communityChart)
    }

    @Test
    fun `artists and the community chart come from the catalogue`() {
        music.topArtists = (1..15).map { ArtistProfile("itunes-artist:$it", "Artist $it", "Bollywood", null) }
        music.genreCharts = mapOf(ChartGenre.BOLLYWOOD to (1..25).map { song(it.toString()) })

        val state = signedInViewModel().uiState.value

        assertEquals((1..12).map { "itunes-artist:$it" }, state.artists.map { it.id })
        assertEquals(ChartGenre.BOLLYWOOD, state.communityChart?.genre)
        assertEquals(25, state.communityChart?.songs?.size)
    }

    @Test
    fun `an empty community chart hides the card`() {
        music.genreCharts = mapOf(ChartGenre.BOLLYWOOD to emptyList())

        assertNull(signedInViewModel().uiState.value.communityChart)
    }

    // ---- song search --------------------------------------------------------------------------

    @Test
    fun `search waits for typing to pause and sends one request for the final text`() = runTest {
        music.searchResults = listOf(song("1"), song("2"))
        val viewModel = signedInViewModel()

        viewModel.onQueryChange("ar")
        advanceTimeBy(200)
        viewModel.onQueryChange("ari")
        advanceTimeBy(200)
        viewModel.onQueryChange("arijit")
        advanceTimeBy(DiscoverViewModel.SEARCH_DEBOUNCE_MS - 1)
        runCurrent()
        assertTrue("still waiting", music.searchedQueries.isEmpty())
        assertEquals(SearchState.Loading, viewModel.uiState.value.search)

        advanceTimeBy(2)
        runCurrent()

        assertEquals(listOf("arijit"), music.searchedQueries)
        assertEquals(listOf("itunes:1", "itunes:2"), (viewModel.uiState.value.search as SearchState.Results).songs.map { it.id })
    }

    @Test
    fun `queries shorter than two characters never reach the catalogue`() = runTest {
        val viewModel = signedInViewModel()

        viewModel.onQueryChange(" a ")
        advanceTimeBy(DiscoverViewModel.SEARCH_DEBOUNCE_MS + 10)
        runCurrent()

        assertTrue(music.searchedQueries.isEmpty())
        assertEquals(SearchState.Idle, viewModel.uiState.value.search)
    }

    @Test
    fun `clearing the query returns to the browse sections at once`() = runTest {
        music.searchResults = listOf(song("1"))
        val viewModel = signedInViewModel()
        viewModel.onQueryChange("kesar")
        advanceTimeBy(DiscoverViewModel.SEARCH_DEBOUNCE_MS + 10)
        runCurrent()
        assertTrue(viewModel.uiState.value.search is SearchState.Results)

        viewModel.onQueryChange("")

        assertEquals(SearchState.Idle, viewModel.uiState.value.search)
    }

    @Test
    fun `no matches and failures get their own states, and retry works`() = runTest {
        val viewModel = signedInViewModel()
        viewModel.onQueryChange("zzzz")
        advanceTimeBy(DiscoverViewModel.SEARCH_DEBOUNCE_MS + 10)
        runCurrent()
        assertEquals(SearchState.Empty, viewModel.uiState.value.search)

        music.failWith = AppError.Network.Unavailable
        viewModel.onQueryChange("zzzzz")
        advanceTimeBy(DiscoverViewModel.SEARCH_DEBOUNCE_MS + 10)
        runCurrent()
        assertEquals(SearchState.Error(AppError.Network.Unavailable), viewModel.uiState.value.search)

        music.failWith = null
        music.searchResults = listOf(song("9"))
        viewModel.onRetrySearch()
        runCurrent()
        assertTrue(viewModel.uiState.value.search is SearchState.Results)
    }

    @Test
    fun `tapping a result plays the whole result list from that song`() = runTest {
        music.searchResults = listOf(song("1"), song("2"), song("3"))
        val viewModel = signedInViewModel()
        viewModel.onQueryChange("mix")
        advanceTimeBy(DiscoverViewModel.SEARCH_DEBOUNCE_MS + 10)
        runCurrent()

        viewModel.onSongClick(1)

        val (queue, start) = playback.playRequests.single()
        assertEquals(listOf("itunes:1", "itunes:2", "itunes:3"), queue.map { it.id })
        assertEquals(1, start)
        assertEquals("itunes:2", viewModel.uiState.value.activeSongId)
    }

    @Test
    fun `tapping the song that is already loaded pauses or resumes it instead of restarting`() = runTest {
        music.searchResults = listOf(song("1"), song("2"))
        val viewModel = signedInViewModel()
        viewModel.onQueryChange("mix")
        advanceTimeBy(DiscoverViewModel.SEARCH_DEBOUNCE_MS + 10)
        runCurrent()
        viewModel.onSongClick(0)

        viewModel.onSongClick(0)

        assertEquals(1, playback.playRequests.size)
        assertEquals(1, playback.toggleCount)
    }
}
