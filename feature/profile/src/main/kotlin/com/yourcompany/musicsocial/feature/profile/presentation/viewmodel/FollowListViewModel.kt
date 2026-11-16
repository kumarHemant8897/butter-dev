package com.yourcompany.musicsocial.feature.profile.presentation.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourcompany.musicsocial.core.domain.model.AuthState
import com.yourcompany.musicsocial.core.domain.model.UserProfile
import com.yourcompany.musicsocial.core.domain.repository.AuthRepository
import com.yourcompany.musicsocial.core.domain.repository.UserRepository
import com.yourcompany.musicsocial.core.domain.result.AppError
import com.yourcompany.musicsocial.core.domain.result.AppResult
import com.yourcompany.musicsocial.core.navigation.FollowListKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class FollowListUiState(
    val isFollowers: Boolean,
    /** The person whose list this is. */
    val name: String,
    val isLoading: Boolean = true,
    val people: List<UserProfile> = emptyList(),
    val error: AppError? = null,
    /** The signed-in user, shown as "You" and not opened as someone else's profile. */
    val myId: String? = null,
)

/** Followers or following of one person. Anyone signed in can see these lists, like the counts. */
@HiltViewModel
class FollowListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val userId: String = savedStateHandle.get<String>(ARG_USER_ID).orEmpty()

    private val _uiState = MutableStateFlow(
        FollowListUiState(
            isFollowers = savedStateHandle.get<String>(ARG_KIND) != FollowListKind.FOLLOWING,
            name = savedStateHandle.get<String>(ARG_TITLE).orEmpty(),
        ),
    )
    val uiState: StateFlow<FollowListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onRetry() = load()

    /** Back from someone's profile: follows may have changed there. */
    fun refresh() {
        if (!_uiState.value.isLoading) load(showLoading = false)
    }

    private fun load(showLoading: Boolean = true) {
        if (showLoading) _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val me = (authRepository.authState.first { it !is AuthState.Initializing } as? AuthState.SignedIn)?.userId
            val result = if (_uiState.value.isFollowers) userRepository.getFollowers(userId) else userRepository.getFollowing(userId)
            _uiState.update {
                when (result) {
                    is AppResult.Success -> it.copy(isLoading = false, people = result.data, error = null, myId = me)
                    // A failed quiet refresh keeps the list on screen.
                    is AppResult.Failure -> if (showLoading || it.people.isEmpty()) it.copy(isLoading = false, error = result.error) else it.copy(isLoading = false)
                }
            }
        }
    }

    companion object {
        /** Match the property names of FollowListRoute, which is how Navigation stores them. */
        const val ARG_USER_ID = "userId"
        const val ARG_KIND = "kind"
        const val ARG_TITLE = "title"
    }
}
