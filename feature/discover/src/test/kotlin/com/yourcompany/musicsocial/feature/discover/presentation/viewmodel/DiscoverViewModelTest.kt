package com.yourcompany.musicsocial.feature.discover.presentation.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoverViewModelTest {

    @Test
    fun `follow toggles per person and does not affect others`() {
        val viewModel = DiscoverViewModel()
        val (first, second) = viewModel.uiState.value.listeners.map { it.person.id }

        viewModel.onToggleFollow(first)
        assertTrue(first in viewModel.uiState.value.followedIds)
        assertFalse(second in viewModel.uiState.value.followedIds)

        viewModel.onToggleFollow(first)
        assertFalse(first in viewModel.uiState.value.followedIds)
    }

    @Test
    fun `selecting a feeling replaces the previous one and reselecting clears it`() {
        val viewModel = DiscoverViewModel()
        val feelings = viewModel.uiState.value.feelings

        viewModel.onSelectFeeling(feelings[1])
        assertEquals(feelings[1], viewModel.uiState.value.selectedFeeling)

        viewModel.onSelectFeeling(feelings[1])
        assertNull(viewModel.uiState.value.selectedFeeling)
    }
}
