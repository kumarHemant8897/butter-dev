package com.yourcompany.musicsocial.feature.discover.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.yourcompany.musicsocial.core.sample.SampleData
import com.yourcompany.musicsocial.feature.discover.presentation.state.ArtistWithGenre
import com.yourcompany.musicsocial.feature.discover.presentation.state.DiscoverUiState
import com.yourcompany.musicsocial.feature.discover.presentation.state.FeaturedPlaylist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Discover state. Content is SampleData (TESTING ONLY); follow and feeling selections are
 * local UI state until UserRepository.follow and recommendations are implemented.
 */
@HiltViewModel
class DiscoverViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(
        DiscoverUiState(
            topSongs = SampleData.trending,
            featuredPlaylist = FeaturedPlaylist(SampleData.cityIsAsleep, saves = 8_200),
            artists = SampleData.artists.take(3).map { ArtistWithGenre(it, SampleData.artistGenres[it.id].orEmpty()) },
            listeners = SampleData.listenerMatches,
            feelings = SampleData.feelings,
            selectedFeeling = SampleData.feelings.firstOrNull(),
        ),
    )
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun onToggleFollow(userId: String) {
        _uiState.update { state ->
            val followed = if (userId in state.followedIds) state.followedIds - userId else state.followedIds + userId
            state.copy(followedIds = followed)
        }
    }

    /** Tapping the selected feeling again clears the selection. */
    fun onSelectFeeling(feeling: String) {
        _uiState.update { it.copy(selectedFeeling = if (it.selectedFeeling == feeling) null else feeling) }
    }
}
