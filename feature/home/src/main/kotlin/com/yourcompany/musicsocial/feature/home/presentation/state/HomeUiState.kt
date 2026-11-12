package com.yourcompany.musicsocial.feature.home.presentation.state

import androidx.compose.runtime.Immutable
import com.yourcompany.musicsocial.core.domain.model.Song
import com.yourcompany.musicsocial.core.domain.result.AppError

enum class TimeOfDay { MORNING, AFTERNOON, EVENING, NIGHT }

@Immutable
data class HomeUiState(
    /** Null while unknown (loading, or the profile could not be read). */
    val firstName: String?,
    val timeOfDay: TimeOfDay,
    val isOffline: Boolean,
    /** Apple Music's current top songs in India. */
    val chart: ChartState = ChartState.Loading,
    /** Songs started in this session, most recent first. */
    val recentlyPlayed: List<Song> = emptyList(),
    /** Id of the song loaded in the player, to highlight its row and show pause. */
    val activeSongId: String? = null,
    val isActiveSongPlaying: Boolean = false,
)

sealed interface ChartState {
    data object Loading : ChartState

    data class Content(val songs: List<Song>) : ChartState

    data class Error(val error: AppError) : ChartState
}
