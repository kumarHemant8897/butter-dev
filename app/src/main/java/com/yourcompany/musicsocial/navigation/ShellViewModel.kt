package com.yourcompany.musicsocial.navigation

import androidx.lifecycle.ViewModel
import com.yourcompany.musicsocial.core.domain.playback.PlaybackController
import com.yourcompany.musicsocial.core.domain.playback.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Gives the app shell what the mini player needs; the player itself lives in [PlaybackController]. */
@HiltViewModel
class ShellViewModel @Inject constructor(
    private val playbackController: PlaybackController,
) : ViewModel() {

    val playback: StateFlow<PlaybackState> = playbackController.state

    fun onTogglePlay() = playbackController.togglePlayPause()

    fun onPrevious() = playbackController.previous()

    fun onNext() = playbackController.next()

    /** [fraction] is 0..1 of what is playing (the preview length, not the full song). */
    fun onSeek(fraction: Float) {
        val duration = playbackController.state.value.durationMs
        if (duration > 0) playbackController.seekTo((duration * fraction.coerceIn(0f, 1f)).toLong())
    }
}
