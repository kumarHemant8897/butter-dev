package com.yourcompany.musicsocial.feature.discover.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourcompany.musicsocial.core.domain.model.AuthState
import com.yourcompany.musicsocial.core.domain.model.PageRequest
import com.yourcompany.musicsocial.core.domain.model.UserProfile
import com.yourcompany.musicsocial.core.domain.playback.PlaybackController
import com.yourcompany.musicsocial.core.domain.repository.AuthRepository
import com.yourcompany.musicsocial.core.domain.repository.MusicRepository
import com.yourcompany.musicsocial.core.domain.repository.UserRepository
import com.yourcompany.musicsocial.core.domain.result.AppResult
import com.yourcompany.musicsocial.core.domain.result.errorOrNull
import com.yourcompany.musicsocial.core.domain.result.fold
import com.yourcompany.musicsocial.core.domain.result.getOrNull
import com.yourcompany.musicsocial.core.sample.SampleData
import com.yourcompany.musicsocial.core.domain.model.ChartGenre
import com.yourcompany.musicsocial.core.domain.model.FollowStatus
import com.yourcompany.musicsocial.feature.discover.presentation.state.CommunityChart
import com.yourcompany.musicsocial.feature.discover.presentation.state.DiscoverPerson
import com.yourcompany.musicsocial.feature.discover.presentation.state.DiscoverUiState
import com.yourcompany.musicsocial.feature.discover.presentation.state.PersonDetail
import com.yourcompany.musicsocial.feature.discover.presentation.state.SearchState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Everything music-related is real catalogue data: the India chart, its lead artists and the live
 * Bollywood chart (MusicRepository). People to follow are real profiles (UserRepository). Without a
 * backend the people list is sample (TESTING ONLY), and follow only changes local state.
 * Feelings are still a fixed list.
 */
@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val musicRepository: MusicRepository,
    private val playbackController: PlaybackController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DiscoverUiState(
            feelings = SampleData.feelings,
            selectedFeeling = SampleData.feelings.firstOrNull(),
        ),
    )
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    /** People whose follow request is in flight; extra taps on them are ignored. */
    private val pending = mutableSetOf<String>()

    private val queries = MutableStateFlow("")

    init {
        viewModelScope.launch {
            // The repository caches the chart, so Home and Discover share one download. Artists come
            // after it on purpose: they are derived from the same (now cached) chart.
            musicRepository.getTopSongs(CHART_SIZE).getOrNull()?.let { songs ->
                _uiState.update { it.copy(topSongs = songs.take(TOP_SONGS_SHOWN)) }
            }
            musicRepository.getTopArtists(ARTIST_LIMIT).getOrNull()?.let { artists ->
                _uiState.update { it.copy(artists = artists) }
            }
        }
        viewModelScope.launch {
            musicRepository.getGenreChart(COMMUNITY_GENRE, CHART_SIZE).getOrNull()?.takeIf { it.isNotEmpty() }?.let { songs ->
                _uiState.update { it.copy(communityChart = CommunityChart(COMMUNITY_GENRE, songs)) }
            }
        }
        viewModelScope.launch {
            authRepository.authState.collectLatest { auth ->
                when (auth) {
                    is AuthState.SignedIn -> loadPeople()
                    AuthState.Unavailable -> _uiState.update {
                        it.copy(people = samplePeople(), arePeopleSample = true, isLoadingPeople = false, peopleError = null)
                    }
                    AuthState.Initializing, AuthState.SignedOut -> _uiState.update {
                        it.copy(people = emptyList(), arePeopleSample = false, isLoadingPeople = false, peopleError = null)
                    }
                }
            }
        }
        // Wait until the user pauses typing: the catalogue allows only about 20 requests a minute.
        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            queries.debounce(SEARCH_DEBOUNCE_MS).map { it.trim() }.distinctUntilChanged().collectLatest { search(it) }
        }
        viewModelScope.launch {
            playbackController.state
                .map { it.song?.id to it.isPlaying }
                .distinctUntilChanged()
                .collect { (songId, playing) ->
                    _uiState.update { it.copy(activeSongId = songId, isActiveSongPlaying = playing) }
                }
        }
    }

    fun onQueryChange(query: String) {
        queries.value = query
        _uiState.update { state ->
            val searchable = query.trim().length >= MIN_QUERY_LENGTH
            state.copy(
                query = query,
                search = when {
                    !searchable -> SearchState.Idle
                    // Show the spinner straight away instead of the browse sections for the debounce delay.
                    state.search is SearchState.Idle -> SearchState.Loading
                    else -> state.search
                },
            )
        }
    }

    fun onRetrySearch() {
        val query = _uiState.value.query.trim()
        viewModelScope.launch { search(query) }
    }

    /** Plays the tapped result, or pauses/resumes it when it is already the loaded song. */
    fun onSongClick(index: Int) {
        val results = (_uiState.value.search as? SearchState.Results)?.songs ?: return
        val song = results.getOrNull(index) ?: return
        if (song.id == playbackController.state.value.song?.id) {
            playbackController.togglePlayPause()
        } else {
            playbackController.play(results, index)
        }
    }

    /** Plays the chart from the tapped song; tapping the loaded song pauses or resumes it. */
    fun onTopSongClick(index: Int) {
        val songs = _uiState.value.topSongs
        val song = songs.getOrNull(index) ?: return
        if (song.id == playbackController.state.value.song?.id) playbackController.togglePlayPause() else playbackController.play(songs, index)
    }

    fun onRetryPeople() {
        viewModelScope.launch { loadPeople() }
    }

    /**
     * Follow asks for approval (REQUESTED); tapping Requested cancels the request. People already
     * followed are unfollowed from their profile, so the row button does nothing for them.
     */
    fun onToggleFollow(personId: String) {
        val state = _uiState.value
        val person = state.people.firstOrNull { it.id == personId } ?: return
        if (person.status == FollowStatus.FOLLOWING || !pending.add(personId)) return

        val wasRequested = person.status == FollowStatus.REQUESTED
        setStatus(personId, if (wasRequested) FollowStatus.NONE else FollowStatus.REQUESTED)
        if (state.arePeopleSample) {
            pending.remove(personId)
            return
        }

        viewModelScope.launch {
            val result = if (wasRequested) userRepository.cancelFollowRequest(personId) else userRepository.requestFollow(personId)
            pending.remove(personId)
            if (result is AppResult.Failure) {
                setStatus(personId, person.status)
                _uiState.update { it.copy(actionError = result.error) }
            } else {
                _uiState.update { it.copy(actionError = null) }
            }
        }
    }

    /** Back from a profile or people search: follow states may have changed there. */
    fun refreshPeople() {
        if (_uiState.value.arePeopleSample || _uiState.value.isLoadingPeople) return
        viewModelScope.launch { loadPeople(showLoading = false) }
    }

    /** Tapping the selected feeling again clears the selection. */
    fun onSelectFeeling(feeling: String) {
        _uiState.update { it.copy(selectedFeeling = if (it.selectedFeeling == feeling) null else feeling) }
    }

    private suspend fun search(query: String) {
        if (query.length < MIN_QUERY_LENGTH) {
            _uiState.update { it.copy(search = SearchState.Idle) }
            return
        }
        _uiState.update { it.copy(search = SearchState.Loading) }
        musicRepository.searchSongs(query, PageRequest(limit = SEARCH_LIMIT)).fold(
            onSuccess = { page ->
                val result = if (page.items.isEmpty()) SearchState.Empty else SearchState.Results(page.items)
                _uiState.update { it.copy(search = result) }
            },
            onFailure = { error -> _uiState.update { it.copy(search = SearchState.Error(error)) } },
        )
    }

    private suspend fun loadPeople(showLoading: Boolean = true) {
        if (showLoading) _uiState.update { it.copy(isLoadingPeople = true, peopleError = null, arePeopleSample = false) }
        val suggested = userRepository.getSuggestedProfiles(SUGGESTION_LIMIT)
        val following = userRepository.getFollowingIds()
        val requested = userRepository.getRequestedIds()
        val profiles = suggested.getOrNull()
        val followingIds = following.getOrNull()
        val requestedIds = requested.getOrNull()
        if (profiles == null || followingIds == null || requestedIds == null) {
            if (!showLoading) return
            val error = suggested.errorOrNull() ?: following.errorOrNull() ?: requested.errorOrNull()
            _uiState.update { it.copy(isLoadingPeople = false, people = emptyList(), peopleError = error) }
            return
        }
        _uiState.update {
            it.copy(
                isLoadingPeople = false,
                peopleError = null,
                people = profiles.map { profile ->
                    val status = when (profile.id) {
                        in followingIds -> FollowStatus.FOLLOWING
                        in requestedIds -> FollowStatus.REQUESTED
                        else -> FollowStatus.NONE
                    }
                    profile.toPerson(status)
                },
            )
        }
    }

    /** A request does not change follower counts; only an accepted request does. */
    private fun setStatus(personId: String, status: FollowStatus) {
        _uiState.update { state ->
            state.copy(people = state.people.map { person -> if (person.id == personId) person.copy(status = status) else person })
        }
    }

    private fun UserProfile.toPerson(status: FollowStatus) = DiscoverPerson(
        id = id,
        name = displayName,
        avatarUrl = avatarUrl,
        detail = PersonDetail.Handle(username, followerCount),
        status = status,
    )

    private fun samplePeople() = SampleData.listenerMatches.map {
        DiscoverPerson(
            id = it.person.id,
            name = it.person.displayName,
            avatarUrl = it.person.avatarUrl,
            detail = PersonDetail.TasteMatch(it.tasteMatchPercent, it.sharedArtists),
            status = FollowStatus.NONE,
        )
    }

    companion object {
        const val SEARCH_DEBOUNCE_MS = 450L
        private const val CHART_SIZE = 25
        private const val TOP_SONGS_SHOWN = 5
        private const val ARTIST_LIMIT = 12
        private val COMMUNITY_GENRE = ChartGenre.BOLLYWOOD
        const val MIN_QUERY_LENGTH = 2
        private const val SEARCH_LIMIT = 25
        private const val SUGGESTION_LIMIT = 10
    }
}
