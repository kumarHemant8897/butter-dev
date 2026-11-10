package com.yourcompany.musicsocial.feature.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourcompany.musicsocial.core.domain.system.BackendAvailability
import com.yourcompany.musicsocial.core.domain.system.NetworkMonitor
import com.yourcompany.musicsocial.core.sample.SampleData
import com.yourcompany.musicsocial.feature.home.presentation.state.HomeUiState
import com.yourcompany.musicsocial.feature.home.presentation.state.TimeOfDay
import com.yourcompany.musicsocial.feature.home.presentation.state.sampleHomeContent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val clock: Clock,
    backendAvailability: BackendAvailability,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val initialState = HomeUiState(
        firstName = SampleData.me.displayName.substringBefore(' '),
        timeOfDay = currentTimeOfDay(),
        isBackendConfigured = backendAvailability.isConfigured,
        isOffline = false,
        content = sampleHomeContent(),
    )

    /**
     * WhileSubscribed(5s) stops upstream collection shortly after the screen leaves the
     * foreground, but survives configuration changes without restarting.
     */
    val uiState: StateFlow<HomeUiState> = networkMonitor.isOnline
        .map { online -> initialState.copy(timeOfDay = currentTimeOfDay(), isOffline = !online) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialState)

    private fun currentTimeOfDay(): TimeOfDay = timeOfDayFor(LocalTime.now(clock))

    companion object {
        /** 05:00-11:59 morning, 12:00-16:59 afternoon, 17:00-21:59 evening, otherwise night. */
        fun timeOfDayFor(time: LocalTime): TimeOfDay = when (time.hour) {
            in 5..11 -> TimeOfDay.MORNING
            in 12..16 -> TimeOfDay.AFTERNOON
            in 17..21 -> TimeOfDay.EVENING
            else -> TimeOfDay.NIGHT
        }
    }
}
