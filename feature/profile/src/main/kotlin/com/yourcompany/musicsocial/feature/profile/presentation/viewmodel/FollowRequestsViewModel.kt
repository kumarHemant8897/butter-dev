package com.yourcompany.musicsocial.feature.profile.presentation.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourcompany.musicsocial.core.domain.model.FollowRequest
import com.yourcompany.musicsocial.core.domain.repository.UserRepository
import com.yourcompany.musicsocial.core.domain.result.AppError
import com.yourcompany.musicsocial.core.domain.result.AppResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class FollowRequestsUiState(
    val isLoading: Boolean = true,
    val requests: List<FollowRequest> = emptyList(),
    val loadError: AppError? = null,
    /** An accept or decline failed; the request was put back. */
    val actionError: AppError? = null,
)

/** Requests waiting for the signed-in user. Accepting makes the requester a follower. */
@HiltViewModel
class FollowRequestsViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FollowRequestsUiState())
    val uiState: StateFlow<FollowRequestsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onRetry() = load()

    fun onAccept(requesterId: String) = answer(requesterId) { userRepository.acceptFollowRequest(it) }

    fun onDecline(requesterId: String) = answer(requesterId) { userRepository.declineFollowRequest(it) }

    /** Removes the row at once; puts it back where it was if the backend refuses. */
    private fun answer(requesterId: String, call: suspend (String) -> AppResult<Unit>) {
        val before = _uiState.value.requests
        val request = before.firstOrNull { it.requester.id == requesterId } ?: return
        _uiState.update { it.copy(requests = it.requests - request, actionError = null) }
        viewModelScope.launch {
            val result = call(requesterId)
            if (result is AppResult.Failure) {
                _uiState.update { state ->
                    val restored = (state.requests + request).sortedByDescending { it.requestedAt }
                    state.copy(requests = restored, actionError = result.error)
                }
            }
        }
    }

    private fun load() {
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            when (val result = userRepository.getIncomingRequests()) {
                is AppResult.Success -> _uiState.update { it.copy(isLoading = false, requests = result.data) }
                is AppResult.Failure -> _uiState.update { it.copy(isLoading = false, loadError = result.error) }
            }
        }
    }
}
