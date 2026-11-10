package com.yourcompany.musicsocial.feature.home.presentation.viewmodel

import app.cash.turbine.test
import com.yourcompany.musicsocial.core.testing.MainDispatcherRule
import com.yourcompany.musicsocial.core.testing.fake.FakeBackendAvailability
import com.yourcompany.musicsocial.core.testing.fake.FakeNetworkMonitor
import com.yourcompany.musicsocial.feature.home.presentation.state.TimeOfDay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun `greeting boundaries follow the local hour`() {
        assertEquals(TimeOfDay.NIGHT, HomeViewModel.timeOfDayFor(LocalTime.of(4, 59)))
        assertEquals(TimeOfDay.MORNING, HomeViewModel.timeOfDayFor(LocalTime.of(5, 0)))
        assertEquals(TimeOfDay.AFTERNOON, HomeViewModel.timeOfDayFor(LocalTime.of(12, 0)))
        assertEquals(TimeOfDay.EVENING, HomeViewModel.timeOfDayFor(LocalTime.of(17, 0)))
        assertEquals(TimeOfDay.NIGHT, HomeViewModel.timeOfDayFor(LocalTime.of(22, 0)))
    }

    @Test
    fun `state reports missing backend configuration honestly`() = runTest {
        val viewModel = HomeViewModel(eveningClock, FakeBackendAvailability(isConfigured = false), FakeNetworkMonitor())

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isBackendConfigured)
            assertEquals(TimeOfDay.EVENING, state.timeOfDay)
        }
    }

    @Test
    fun `offline banner state follows connectivity changes`() = runTest {
        val network = FakeNetworkMonitor(initiallyOnline = true)
        val viewModel = HomeViewModel(eveningClock, FakeBackendAvailability(isConfigured = true), network)

        viewModel.uiState.test {
            assertFalse(awaitItem().isOffline)

            network.setOnline(false)
            assertTrue(awaitItem().isOffline)

            network.setOnline(true)
            assertFalse(awaitItem().isOffline)
        }
    }
}
