package com.yourcompany.musicsocial.feature.chat.presentation.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourcompany.musicsocial.core.domain.model.AuthState
import com.yourcompany.musicsocial.core.domain.model.UserProfile
import com.yourcompany.musicsocial.core.domain.repository.AuthRepository
import com.yourcompany.musicsocial.core.domain.repository.ChatRepository
import com.yourcompany.musicsocial.core.domain.repository.UserRepository
import com.yourcompany.musicsocial.core.domain.result.AppError
import com.yourcompany.musicsocial.core.domain.result.AppResult
import com.yourcompany.musicsocial.core.domain.result.getOrNull
import com.yourcompany.musicsocial.core.navigation.ConversationRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class NewGroupUiState(
    val title: String = "",
    /** People the signed-in user is connected with (follows or is followed by), by name. */
    val people: List<UserProfile> = emptyList(),
    val selected: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val isCreating: Boolean = false,
    val error: AppError? = null,
    /** Set once the group exists; the screen opens it. */
    val created: ConversationRoute? = null,
) {
    val titleTooLong: Boolean get() = title.trim().length > ChatRepository.GROUP_TITLE_MAX_LENGTH
    val canCreate: Boolean get() = title.isNotBlank() && !titleTooLong && selected.isNotEmpty() && !isCreating
}

/** Name a group and pick members from the people you are connected with (the database allows only those). */
@HiltViewModel
class NewGroupViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val chatRepository: ChatRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewGroupUiState())
    val uiState: StateFlow<NewGroupUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onRetry() = load()

    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value, error = null) }

    fun onToggle(userId: String) = _uiState.update {
        it.copy(selected = if (userId in it.selected) it.selected - userId else it.selected + userId, error = null)
    }

    fun onCreate() {
        val state = _uiState.value
        if (!state.canCreate) return
        _uiState.update { it.copy(isCreating = true, error = null) }
        viewModelScope.launch {
            val title = state.title.trim()
            when (val result = chatRepository.createGroup(title, state.selected.toList())) {
                is AppResult.Success -> _uiState.update { it.copy(isCreating = false, created = ConversationRoute(result.data, title, isGroup = true)) }
                is AppResult.Failure -> _uiState.update { it.copy(isCreating = false, error = result.error) }
            }
        }
    }

    fun onOpened() = _uiState.update { it.copy(created = null) }

    private fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val me = (authRepository.authState.first { it !is AuthState.Initializing } as? AuthState.SignedIn)?.userId
            if (me == null) {
                _uiState.update { it.copy(isLoading = false, error = AppError.Auth.NotSignedIn) }
                return@launch
            }
            val followers = async { userRepository.getFollowers(me) }
            val following = async { userRepository.getFollowing(me) }
            val a = followers.await()
            val b = following.await()
            if (a is AppResult.Failure && b is AppResult.Failure) {
                _uiState.update { it.copy(isLoading = false, error = a.error) }
                return@launch
            }
            val people = (a.getOrNull().orEmpty() + b.getOrNull().orEmpty())
                .distinctBy { it.id }
                .filter { it.id != me }
                .sortedBy { it.displayName.lowercase() }
            _uiState.update { it.copy(isLoading = false, people = people) }
        }
    }
}
