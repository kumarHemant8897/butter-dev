package com.yourcompany.musicsocial.feature.chat.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourcompany.musicsocial.core.domain.model.AuthState
import com.yourcompany.musicsocial.core.domain.model.ConversationSummary
import com.yourcompany.musicsocial.core.domain.model.ConversationType
import com.yourcompany.musicsocial.core.domain.model.MessageContent
import com.yourcompany.musicsocial.core.domain.repository.AuthRepository
import com.yourcompany.musicsocial.core.domain.repository.ChatRepository
import com.yourcompany.musicsocial.core.domain.result.AppError
import com.yourcompany.musicsocial.core.domain.result.AppResult
import com.yourcompany.musicsocial.feature.chat.presentation.state.ChatFilter
import com.yourcompany.musicsocial.feature.chat.presentation.state.ChatListUiState
import com.yourcompany.musicsocial.feature.chat.presentation.state.ConversationItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/**
 * The signed-in user's real conversations (ChatRepository). A new user has none and sees
 * "Find your people" instead. The screen calls [refresh] when it becomes visible and every few
 * seconds while open, so new chats and unread counts appear without pulling to refresh.
 */
@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val clock: Clock,
) : ViewModel() {

    private val conversations = MutableStateFlow<List<ConversationSummary>>(emptyList())
    private val loadState = MutableStateFlow(LoadState(isLoading = true))
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(ChatFilter.ALL)
    private var myId: String? = null
    private val loadMutex = Mutex()

    val uiState: StateFlow<ChatListUiState> = combine(conversations, loadState, query, filter) { all, load, q, f ->
        ChatListUiState(
            query = q,
            filter = f,
            conversations = all.filter { it.matches(f) && it.matches(q) }.map { it.toItem() },
            totalCount = all.size,
            isLoading = load.isLoading,
            error = load.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatListUiState())

    init {
        viewModelScope.launch {
            authRepository.authState.collect { auth ->
                when (auth) {
                    is AuthState.SignedIn -> {
                        myId = auth.userId
                        load()
                    }
                    AuthState.Unavailable -> loadState.value = LoadState(isLoading = false, error = AppError.BackendNotConfigured)
                    AuthState.Initializing, AuthState.SignedOut -> {
                        myId = null
                        conversations.value = emptyList()
                        loadState.value = LoadState(isLoading = true)
                    }
                }
            }
        }
    }

    fun refresh() {
        if (myId == null) return
        viewModelScope.launch { load() }
    }

    fun onRetry() {
        loadState.value = LoadState(isLoading = true)
        refresh()
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onFilterChange(value: ChatFilter) {
        filter.value = value
    }

    private suspend fun load() = loadMutex.withLock {
        when (val result = chatRepository.getConversations()) {
            is AppResult.Success -> {
                conversations.value = result.data.sortedByDescending { it.updatedAt }
                loadState.value = LoadState(isLoading = false)
            }
            is AppResult.Failure -> {
                // Keep a list the user already sees; only an empty screen shows the error.
                val keep = conversations.value.isNotEmpty()
                loadState.value = LoadState(isLoading = false, error = if (keep) null else result.error)
            }
        }
    }

    private fun ConversationSummary.matches(f: ChatFilter): Boolean = when (f) {
        ChatFilter.ALL -> true
        ChatFilter.FRIENDS -> type == ConversationType.DIRECT
        ChatFilter.GROUPS -> type == ConversationType.GROUP
    }

    private fun ConversationSummary.matches(q: String): Boolean =
        q.isBlank() || displayTitle().contains(q.trim(), ignoreCase = true) ||
            otherUser?.username?.contains(q.trim().removePrefix("@"), ignoreCase = true) == true

    private fun ConversationSummary.displayTitle(): String = title ?: otherUser?.displayName.orEmpty()

    private fun ConversationSummary.toItem(): ConversationItem {
        val last = lastMessage
        val prefix = if (last != null && last.senderId == myId) "You: " else ""
        val (preview, isMusic) = when (val content = last?.content) {
            is MessageContent.Text -> prefix + content.body to false
            is MessageContent.SharedSong -> prefix + "Shared a song" to true
            is MessageContent.SharedPlaylist -> prefix + "Shared a playlist" to true
            MessageContent.Deleted -> "Message deleted" to false
            null -> "Say hi 👋" to false
        }
        return ConversationItem(
            id = id,
            title = displayTitle(),
            avatarUrl = otherUser?.avatarUrl,
            otherUserId = otherUser?.id,
            preview = preview,
            previewIsMusic = isMusic,
            timeLabel = timeLabel(updatedAt),
            unreadCount = unreadCount,
            isGroup = type == ConversationType.GROUP,
        )
    }

    /** Today -> "21:39", yesterday -> "Yesterday", older -> "Nov 3". */
    internal fun timeLabel(instant: Instant): String {
        val zone: ZoneId = clock.zone
        val date = instant.atZone(zone).toLocalDate()
        val today = LocalDate.now(clock)
        return when (date) {
            today -> instant.atZone(zone).format(TIME)
            today.minusDays(1) -> "Yesterday"
            else -> instant.atZone(zone).format(DATE)
        }
    }

    private data class LoadState(val isLoading: Boolean, val error: AppError? = null)

    private companion object {
        val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm", Locale.ROOT)
        val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
    }
}
