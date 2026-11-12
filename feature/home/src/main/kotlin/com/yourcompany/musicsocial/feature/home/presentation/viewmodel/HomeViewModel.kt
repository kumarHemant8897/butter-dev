package com.yourcompany.musicsocial.feature.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourcompany.musicsocial.core.domain.model.AuthState
import com.yourcompany.musicsocial.core.domain.model.Song
import com.yourcompany.musicsocial.core.domain.playback.PlaybackController
import com.yourcompany.musicsocial.core.domain.repository.AuthRepository
import com.yourcompany.musicsocial.core.domain.repository.MusicRepository
import com.yourcompany.musicsocial.core.domain.repository.UserRepository
import com.yourcompany.musicsocial.core.domain.result.fold
import com.yourcompany.musicsocial.core.domain.result.getOrNull
import com.yourcompany.musicsocial.core.domain.system.NetworkMonitor
import com.yourcompany.musicsocial.feature.home.presentation.state.ChartState
import com.yourcompany.musicsocial.feature.home.presentation.state.HomeUiState
import com.yourcompany.musicsocial.feature.home.presentation.state.TimeOfDay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalTime
import javax.inject.Inject

/**
 * Everything on Home is real: the greeting comes from the signed-in profile, the chart from the
 * music catalogue, and "Back in your orbit" from songs played in this session.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val clock: Clock,
    networkMonitor: NetworkMonitor,
    authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val musicRepository: MusicRepository,
    private val playbackController: PlaybackController,
) : ViewModel() {

    private val reloadCount = MutableStateFlow(0)
    private val chart = MutableStateFlow<ChartState>(ChartState.Loading)

    init {
        loadChart()
    }

    /** Called when the screen becomes visible again, so a name changed in Edit profile shows up. */
    fun refresh() {
        reloadCount.value++
        // A failed chart gets another chance when the user comes back; a loaded one is kept.
        if (chart.value is ChartState.Error) loadChart()
    }

    fun onRetryChart() = loadChart()

    /** Plays the chart from [index]; tapping the song that is already loaded pauses or resumes it. */
    fun onChartSongClick(index: Int) {
        val songs = (chart.value as? ChartState.Content)?.songs ?: return
        play(songs, index)
    }

    fun onRecentSongClick(index: Int) = play(playbackController.recentlyPlayed.value, index)

    private fun play(songs: List<Song>, index: Int) {
        val song = songs.getOrNull(index) ?: return
        if (song.id == playbackController.state.value.song?.id) {
            playbackController.togglePlayPause()
        } else {
            playbackController.play(songs, index)
        }
    }

    private fun loadChart() {
        chart.value = ChartState.Loading
        viewModelScope.launch {
            musicRepository.getTopSongs(CHART_SIZE).fold(
                onSuccess = { songs -> chart.value = ChartState.Content(songs) },
                onFailure = { error -> chart.value = ChartState.Error(error) },
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val firstName: Flow<String?> = combine(authRepository.authState, reloadCount) { auth, _ -> auth }
        .flatMapLatest { auth ->
            when (auth) {
                is AuthState.SignedIn -> flow {
                    emit(userRepository.getProfile(auth.userId).getOrNull()?.displayName?.let(::firstNameOf))
                }
                AuthState.Initializing, AuthState.SignedOut, AuthState.Unavailable -> flowOf(null)
            }
        }

    /**
     * WhileSubscribed(5s) stops upstream collection shortly after the screen leaves the
     * foreground, but survives configuration changes without restarting.
     */
    val uiState: StateFlow<HomeUiState> = combine(
        networkMonitor.isOnline,
        firstName,
        chart,
        playbackController.recentlyPlayed,
        playbackController.state,
    ) { online, name, chartState, recent, playback ->
        HomeUiState(
            firstName = name,
            timeOfDay = timeOfDayFor(LocalTime.now(clock)),
            isOffline = !online,
            chart = chartState,
            recentlyPlayed = recent,
            activeSongId = playback.song?.id,
            isActiveSongPlaying = playback.isPlaying,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HomeUiState(firstName = null, timeOfDay = timeOfDayFor(LocalTime.now(clock)), isOffline = false),
    )

    companion object {
        /** Apple's chart feed offers 10, 25, 50 or 100; 25 fills Home without a long scroll. */
        const val CHART_SIZE = 25

        /** 05:00-11:59 morning, 12:00-16:59 afternoon, 17:00-21:59 evening, otherwise night. */
        fun timeOfDayFor(time: LocalTime): TimeOfDay = when (time.hour) {
            in 5..11 -> TimeOfDay.MORNING
            in 12..16 -> TimeOfDay.AFTERNOON
            in 17..21 -> TimeOfDay.EVENING
            else -> TimeOfDay.NIGHT
        }

        fun firstNameOf(displayName: String): String? = displayName.trim().substringBefore(' ').takeIf { it.isNotEmpty() }
    }
}
