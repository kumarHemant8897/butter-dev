package com.yourcompany.musicsocial.feature.player.presentation.viewmodel

import com.yourcompany.musicsocial.feature.player.presentation.state.RepeatMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerViewModelTest {

    @Test
    fun `starts paused and play toggles`() {
        val viewModel = PlayerViewModel()
        assertFalse(viewModel.uiState.value.isPlaying)

        viewModel.onTogglePlay()
        assertTrue(viewModel.uiState.value.isPlaying)

        viewModel.onTogglePlay()
        assertFalse(viewModel.uiState.value.isPlaying)
    }

    @Test
    fun `repeat cycles off, all, one, off`() {
        val viewModel = PlayerViewModel()
        val seen = (1..3).map { viewModel.onCycleRepeat(); viewModel.uiState.value.repeatMode }

        assertEquals(listOf(RepeatMode.ALL, RepeatMode.ONE, RepeatMode.OFF), seen)
    }

    @Test
    fun `progress is the position as a fraction of the duration`() {
        val state = PlayerViewModel().uiState.value

        assertEquals(state.positionMs.toFloat() / state.song.durationMs, state.progress, 0.0001f)
        assertEquals(1f, state.copy(positionMs = state.song.durationMs * 2).progress, 0f)
    }
}
