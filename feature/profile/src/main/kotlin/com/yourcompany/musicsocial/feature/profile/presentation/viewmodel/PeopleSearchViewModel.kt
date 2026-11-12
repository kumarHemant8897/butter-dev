package com.yourcompany.musicsocial.feature.profile.presentation.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourcompany.musicsocial.core.domain.model.FollowStatus
import com.yourcompany.musicsocial.core.domain.model.PageRequest
import com.yourcompany.musicsocial.core.domain.model.UserProfile
import com.yourcompany.musicsocial.core.domain.repository.UserRepository
import com.yourcompany.musicsocial.core.domain.result.AppError
import com.yourcompany.musicsocial.core.domain.result.AppResult
import com.yourcompany.musicsocial.core.domain.result.getOrNull
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

@Immutable
data class PersonResult(val profile: UserProfile, val status: FollowStatus)

@Immutable
data class PeopleSearchUiState(
    val query: String = "",
    /** Suggestions while the query is short, search results otherwise. */
    val people: List<PersonResult> = emptyList(),
    val isSearching: Boolean = false,
    val isShowingSuggestions: Boolean = true,
    val error: AppError? = null,
    val actionError: AppError? = null,
)

/**
 * "Find your people": search by name or username, newest members as suggestions. The button on a
 * row asks to follow, or cancels a waiting request; unfollowing happens on the person's profile.
 */
@HiltViewModel
class PeopleSearchViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PeopleSearchUiState(isSearching = true))
    val uiState: StateFlow<PeopleSearchUiState> = _uiState.asStateFlow()

    private val queries = MutableStateFlow("")
    private var following: Set<String> = emptySet()
    private var requested: Set<String> = emptySet()
    private val pending = mutableSetOf<String>()

    init {
        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            queries.debounce(SEARCH_DEBOUNCE_MS).map { it.trim() }.distinctUntilChanged().collectLatest { search(it) }
        }
    }

    fun onQueryChange(value: String) {
        queries.value = value
        _uiState.update { it.copy(query = value, isSearching = true) }
    }

    fun onRetry() {
        viewModelScope.launch { search(_uiState.value.query.trim()) }
    }

    /** Back from a profile: follow states may have changed there. */
    fun refresh() = onRetry()

    fun onFollowClick(userId: String) {
        val person = _uiState.value.people.firstOrNull { it.profile.id == userId } ?: return
        if (person.status == FollowStatus.FOLLOWING || !pending.add(userId)) return
        val wasRequested = person.status == FollowStatus.REQUESTED
        setStatus(userId, if (wasRequested) FollowStatus.NONE else FollowStatus.REQUESTED)
        viewModelScope.launch {
            val result = if (wasRequested) userRepository.cancelFollowRequest(userId) else userRepository.requestFollow(userId)
            pending.remove(userId)
            if (result is AppResult.Failure) {
                setStatus(userId, person.status)
                _uiState.update { it.copy(actionError = result.error) }
            } else {
                requested = if (wasRequested) requested - userId else requested + userId
                _uiState.update { it.copy(actionError = null) }
            }
        }
    }

    private suspend fun search(query: String) {
        _uiState.update { it.copy(isSearching = true, error = null) }
        following = userRepository.getFollowingIds().getOrNull() ?: following
        requested = userRepository.getRequestedIds().getOrNull() ?: requested
        val suggestions = query.length < MIN_QUERY_LENGTH
        val result = if (suggestions) {
            userRepository.getSuggestedProfiles(SUGGESTION_LIMIT)
        } else {
            when (val page = userRepository.searchProfiles(query, PageRequest(limit = SEARCH_LIMIT))) {
                is AppResult.Success -> AppResult.Success(page.data.items)
                is AppResult.Failure -> page
            }
        }
        when (result) {
            is AppResult.Success -> _uiState.update {
                it.copy(isSearching = false, isShowingSuggestions = suggestions, people = result.data.map { profile -> PersonResult(profile, statusOf(profile.id)) })
            }
            is AppResult.Failure -> _uiState.update { it.copy(isSearching = false, error = result.error, people = emptyList()) }
        }
    }

    private fun statusOf(userId: String) = when (userId) {
        in following -> FollowStatus.FOLLOWING
        in requested -> FollowStatus.REQUESTED
        else -> FollowStatus.NONE
    }

    private fun setStatus(userId: String, status: FollowStatus) {
        _uiState.update { state ->
            state.copy(people = state.people.map { if (it.profile.id == userId) it.copy(status = status) else it })
        }
    }

    companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
        private const val MIN_QUERY_LENGTH = 2
        private const val SEARCH_LIMIT = 30
        private const val SUGGESTION_LIMIT = 20
    }
}
