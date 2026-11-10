package com.yourcompany.musicsocial.feature.player.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.yourcompany.musicsocial.core.sample.SampleData
import com.yourcompany.musicsocial.feature.player.presentation.state.PlayerUiState
import com.yourcompany.musicsocial.feature.player.presentation.state.RepeatMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * TESTING ONLY content (SampleData.nowPlaying). In Phase 3 this ViewModel observes a playback
 * controller backed by a Media3 MediaSessionService instead of holding the state itself.
 */
@HiltViewModel
class PlayerViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(
        SampleData.nowPlaying.let {
            PlayerUiState(
                song = it.song,
                sourceLabel = it.fromPlaylist.title,
                positionMs = it.positionMs,
                shareWithName = it.sharePrompt.displayName.substringBefore(' '),
            )
        },
    )
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    fun onTogglePlay() = _uiState.update { it.copy(isPlaying = !it.isPlaying) }

    fun onToggleLike() = _uiState.update { it.copy(isLiked = !it.isLiked) }

    fun onToggleShuffle() = _uiState.update { it.copy(shuffle = !it.shuffle) }

    /** OFF -> ALL -> ONE -> OFF, like most music players. */
    fun onCycleRepeat() = _uiState.update {
        it.copy(
            repeatMode = when (it.repeatMode) {
                RepeatMode.OFF -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.OFF
            },
        )
    }
}
