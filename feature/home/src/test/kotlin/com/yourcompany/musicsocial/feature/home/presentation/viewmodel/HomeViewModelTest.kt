package com.yourcompany.musicsocial.feature.home.presentation.viewmodel

import app.cash.turbine.test
import com.yourcompany.musicsocial.core.domain.model.AuthState
import com.yourcompany.musicsocial.core.domain.model.Song
import com.yourcompany.musicsocial.core.domain.model.UserProfile
import com.yourcompany.musicsocial.core.domain.result.AppError
import com.yourcompany.musicsocial.core.testing.MainDispatcherRule
import com.yourcompany.musicsocial.core.testing.fake.FakeAuthRepository
import com.yourcompany.musicsocial.core.testing.fake.FakeMusicRepository
import com.yourcompany.musicsocial.core.testing.fake.FakeNetworkMonitor
import com.yourcompany.musicsocial.core.testing.fake.FakePlaybackController
import com.yourcompany.musicsocial.core.testing.fake.FakeUserRepository
import com.yourcompany.musicsocial.feature.home.presentation.state.ChartState
import com.yourcompany.musicsocial.feature.home.presentation.state.TimeOfDay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val eveningClock = Clock.fixed(Instant.parse("2026-11-09T19:30:00Z"), ZoneOffset.UTC)
    private val ada = UserProfile("u1", "ada", "Ada Lovelace", null, null, 0, 0)
    private fun song(id: String) = Song("itunes:$id", "Song $id", emptyList(), null, null, 0, null, null, null)
    private val chart = (1..25).map { song(it.toString()) }

    private val music = FakeMusicRepository().apply { topSongs = chart }
    private val playback = FakePlaybackController()

    private fun viewModel(
        auth: AuthState = AuthState.SignedIn("u1", null),
        users: FakeUserRepository = FakeUserRepository(meId = "u1", initialProfiles = listOf(ada)),
        network: FakeNetworkMonitor = FakeNetworkMonitor(),
    ) = HomeViewModel(eveningClock, network, FakeAuthRepository(auth), users, music, playback)

    @Test
    fun `greeting boundaries follow the local hour`() {
        assertEquals(TimeOfDay.NIGHT, HomeViewModel.timeOfDayFor(LocalTime.of(4, 59)))
        assertEquals(TimeOfDay.MORNING, HomeViewModel.timeOfDayFor(LocalTime.of(5, 0)))
        assertEquals(TimeOfDay.AFTERNOON, HomeViewModel.timeOfDayFor(LocalTime.of(12, 0)))
        assertEquals(TimeOfDay.EVENING, HomeViewModel.timeOfDayFor(LocalTime.of(17, 0)))
        assertEquals(TimeOfDay.NIGHT, HomeViewModel.timeOfDayFor(LocalTime.of(22, 0)))
    }

    @Test
    fun `the chart comes from the catalogue, in order`() = runTest {
        viewModel().uiState.test {
            val state = expectMostRecentItem()
            assertEquals(chart.map { it.id }, (state.chart as ChartState.Content).songs.map { it.id })
            assertEquals(1, music.topSongsRequests)
        }
    }

    @Test
    fun `a failed chart shows the error and retry loads it`() = runTest {
        music.failWith = AppError.Network.Unavailable
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(ChartState.Error(AppError.Network.Unavailable), expectMostRecentItem().chart)

            music.failWith = null
            viewModel.onRetryChart()

            assertTrue(expectMostRecentItem().chart is ChartState.Content)
        }
    }

    @Test
    fun `coming back to Home retries a failed chart but keeps a loaded one`() = runTest {
        val viewModel = viewModel()
        viewModel.refresh()
        assertEquals("a loaded chart is not fetched again", 1, music.topSongsRequests)

        music.failWith = AppError.Network.Timeout
        viewModel.onRetryChart()
        music.failWith = null
        viewModel.refresh()

        assertEquals(3, music.topSongsRequests)
        viewModel.uiState.test { assertTrue(expectMostRecentItem().chart is ChartState.Content) }
    }

    @Test
    fun `tapping a chart song plays the whole chart from it and highlights it`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onChartSongClick(4)

            val (queue, start) = playback.playRequests.single()
            assertEquals(chart, queue)
            assertEquals(4, start)
            val state = expectMostRecentItem()
            assertEquals("itunes:5", state.activeSongId)
            assertTrue(state.isActiveSongPlaying)
        }
    }

    @Test
    fun `tapping the song that is already loaded toggles it instead of restarting`() {
        val viewModel = viewModel()
        viewModel.onChartSongClick(0)

        viewModel.onChartSongClick(0)

        assertEquals(1, playback.playRequests.size)
        assertEquals(1, playback.toggleCount)
    }

    @Test
    fun `played songs show up in Back in your orbit, newest first, and play from there`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertTrue(expectMostRecentItem().recentlyPlayed.isEmpty())

            viewModel.onChartSongClick(2)
            viewModel.onChartSongClick(7)
            assertEquals(listOf("itunes:8", "itunes:3"), expectMostRecentItem().recentlyPlayed.map { it.id })

            viewModel.onRecentSongClick(1)
            val (queue, start) = playback.playRequests.last()
            assertEquals("itunes:3", queue[start].id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `signed in users are greeted by the first name from their real profile`() = runTest {
        viewModel().uiState.test {
            assertEquals("Ada", expectMostRecentItem().firstName)
        }
    }

    @Test
    fun `an unreadable profile leaves the name empty instead of showing someone else's`() = runTest {
        val users = FakeUserRepository(meId = "u1", initialProfiles = listOf(ada)).apply { failWith = AppError.Network.Unavailable }

        viewModel(users = users).uiState.test {
            assertNull(expectMostRecentItem().firstName)
        }
    }

    @Test
    fun `refresh picks up a name changed in Edit profile`() = runTest {
        val users = FakeUserRepository(meId = "u1", initialProfiles = listOf(ada))
        val viewModel = viewModel(users = users)

        viewModel.uiState.test {
            assertEquals("Ada", expectMostRecentItem().firstName)

            users.profiles["u1"] = ada.copy(displayName = "Grace Hopper")
            viewModel.refresh()

            assertEquals("Grace", expectMostRecentItem().firstName)
        }
    }

    @Test
    fun `offline banner state follows connectivity changes`() = runTest {
        val network = FakeNetworkMonitor(initiallyOnline = true)

        viewModel(network = network).uiState.test {
            assertFalse(expectMostRecentItem().isOffline)

            network.setOnline(false)
            assertTrue(expectMostRecentItem().isOffline)

            network.setOnline(true)
            assertFalse(expectMostRecentItem().isOffline)
        }
    }

    @Test
    fun `first name is the first word and tolerates extra spaces`() {
        assertEquals("Hemant", HomeViewModel.firstNameOf("  Hemant   Kumar "))
        assertEquals("Nova", HomeViewModel.firstNameOf("Nova"))
        assertNull(HomeViewModel.firstNameOf("   "))
    }
}
