package com.yourcompany.musicsocial.feature.player.presentation.state

import androidx.compose.runtime.Immutable
import com.yourcompany.musicsocial.core.domain.model.Song

enum class RepeatMode { OFF, ALL, ONE }

/**
 * Player screen state. Controls change this state only: there is no audio engine yet
 * (Media3 arrives in Phase 3), so the progress does not advance.
 */
@Immutable
data class PlayerUiState(
    val song: Song,
    val sourceLabel: String,
    val positionMs: Long,
    val isPlaying: Boolean = false,
    val isLiked: Boolean = false,
    val shuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shareWithName: String,
) {
    val progress: Float get() = if (song.durationMs <= 0) 0f else (positionMs.toFloat() / song.durationMs).coerceIn(0f, 1f)
}
