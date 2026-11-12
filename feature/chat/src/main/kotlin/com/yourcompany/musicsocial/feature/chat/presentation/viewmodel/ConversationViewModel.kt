package com.yourcompany.musicsocial.feature.chat.presentation.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourcompany.musicsocial.core.domain.model.AuthState
import com.yourcompany.musicsocial.core.domain.model.DeliveryStatus
import com.yourcompany.musicsocial.core.domain.model.Message
import com.yourcompany.musicsocial.core.domain.model.MessageContent
import com.yourcompany.musicsocial.core.domain.model.PageRequest
import com.yourcompany.musicsocial.core.domain.repository.AuthRepository
import com.yourcompany.musicsocial.core.domain.repository.ChatRepository
import com.yourcompany.musicsocial.core.domain.result.AppError
import com.yourcompany.musicsocial.core.domain.result.AppResult
import com.yourcompany.musicsocial.core.domain.usecase.SendTextMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.util.UUID
import javax.inject.Inject

/** A message as the thread shows it. Local sends carry [clientId] until the server confirms them. */
@Immutable
data class ThreadMessage(
    val id: String,
    val clientId: String?,
    val isMine: Boolean,
    val content: MessageContent,
    val sentAt: java.time.Instant,
    val status: DeliveryStatus,
)

@Immutable
data class ConversationUiState(
    val title: String,
    val otherUserId: String?,
    /** Newest first, to match the reversed list on screen. */
    val messages: List<ThreadMessage> = emptyList(),
    val draft: String = "",
    val isLoading: Boolean = true,
    val loadError: AppError? = null,
    val sendError: AppError? = null,
)

/**
 * One conversation. History loads once; new messages arrive through Realtime. Because Realtime can
 * drop on a flaky Wi-Fi, the screen also calls [refresh] every few seconds, which merges the latest
 * page by id, so a message is never shown twice and never missed.
 *
 * Sending is optimistic: the bubble appears at once as SENDING, turns SENT when the server stores it,
 * or FAILED with tap-to-retry. A retry reuses the same client id so the server cannot store it twice.
 */
@HiltViewModel
class ConversationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val sendTextMessage: SendTextMessageUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val conversationId: String = savedStateHandle.get<String>(ARG_CONVERSATION_ID).orEmpty()

    private val _uiState = MutableStateFlow(
        ConversationUiState(
            title = savedStateHandle.get<String>(ARG_TITLE).orEmpty(),
            otherUserId = savedStateHandle.get<String>(ARG_OTHER_USER_ID),
        ),
    )
    val uiState: StateFlow<ConversationUiState> = _uiState.asStateFlow()

    private var myId: String? = null
    private val mergeMutex = Mutex()

    init {
        viewModelScope.launch {
            myId = authRepository.authState.filterIsInstance<AuthState.SignedIn>().first().userId
            load(initial = true)
            chatRepository.observeNewMessages(conversationId).collect { message ->
                merge(listOf(message))
                if (message.senderId != myId) chatRepository.markRead(conversationId)
            }
        }
    }

    fun refresh() {
        if (myId == null) return
        viewModelScope.launch { load(initial = false) }
    }

    fun onRetryLoad() {
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        refresh()
    }

    fun onDraftChange(value: String) {
        _uiState.update { it.copy(draft = value, sendError = null) }
    }

    fun onSend() {
        val text = _uiState.value.draft.trim()
        if (text.isEmpty()) return
        if (text.length > SendTextMessageUseCase.MAX_LENGTH) {
            _uiState.update { it.copy(sendError = AppError.Validation(SendTextMessageUseCase.FIELD_BODY, SendTextMessageUseCase.RULE_TOO_LONG)) }
            return
        }
        val clientId = UUID.randomUUID().toString()
        val pending = ThreadMessage(
            id = "local-$clientId",
            clientId = clientId,
            isMine = true,
            content = MessageContent.Text(text),
            sentAt = clock.instant(),
            status = DeliveryStatus.SENDING,
        )
        _uiState.update { it.copy(draft = "", sendError = null, messages = listOf(pending) + it.messages) }
        send(pending, text)
    }

    /** Tap on a FAILED bubble. */
    fun onRetrySend(localId: String) {
        val failed = _uiState.value.messages.firstOrNull { it.id == localId && it.status == DeliveryStatus.FAILED } ?: return
        val text = (failed.content as? MessageContent.Text)?.body ?: return
        replace(localId) { it.copy(status = DeliveryStatus.SENDING) }
        send(failed, text)
    }

    private fun send(local: ThreadMessage, text: String) {
        val clientId = local.clientId ?: return
        viewModelScope.launch {
            when (val result = sendTextMessage(conversationId, text, clientId)) {
                is AppResult.Success -> {
                    // Realtime may have delivered the stored copy already; drop the local one either way.
                    _uiState.update { state -> state.copy(messages = state.messages.filterNot { it.id == local.id }) }
                    merge(listOf(result.data))
                }
                is AppResult.Failure -> {
                    // Sent already by an earlier attempt (unique client id): the next refresh shows it.
                    if (result.error == AppError.Conflict) {
                        _uiState.update { state -> state.copy(messages = state.messages.filterNot { it.id == local.id }) }
                        load(initial = false)
                    } else {
                        replace(local.id) { it.copy(status = DeliveryStatus.FAILED) }
                        _uiState.update { it.copy(sendError = result.error) }
                    }
                }
            }
        }
    }

    private suspend fun load(initial: Boolean) {
        when (val result = chatRepository.getMessages(conversationId, PageRequest(limit = PAGE_SIZE))) {
            is AppResult.Success -> {
                merge(result.data.items, statusesFromServer = true)
                _uiState.update { it.copy(isLoading = false, loadError = null) }
                val newestFromOther = result.data.items.firstOrNull { it.senderId != myId }
                if (initial || newestFromOther != null) chatRepository.markRead(conversationId)
            }
            is AppResult.Failure -> _uiState.update {
                // A failed background refresh keeps the thread; only an empty thread shows the error.
                if (it.messages.isEmpty()) it.copy(isLoading = false, loadError = result.error) else it.copy(isLoading = false)
            }
        }
    }

    /**
     * Adds server messages by id. A server copy replaces a local SENDING bubble with the same text
     * sent by me. [statusesFromServer]: history carries read ticks, so their status wins.
     */
    private suspend fun merge(incoming: List<Message>, statusesFromServer: Boolean = false) = mergeMutex.withLock {
        _uiState.update { state ->
            val byId = state.messages.associateBy { it.id }.toMutableMap()
            incoming.forEach { message ->
                val existing = byId[message.id]
                val item = message.toThread()
                byId[message.id] = if (existing != null && !statusesFromServer) existing else item
            }
            state.copy(messages = byId.values.sortedWith(compareByDescending<ThreadMessage> { it.sentAt }.thenByDescending { it.id }))
        }
    }

    private fun replace(id: String, change: (ThreadMessage) -> ThreadMessage) {
        _uiState.update { state -> state.copy(messages = state.messages.map { if (it.id == id) change(it) else it }) }
    }

    private fun Message.toThread() = ThreadMessage(
        id = id,
        clientId = null,
        isMine = senderId == myId,
        content = content,
        sentAt = sentAt,
        status = status,
    )

    companion object {
        /** Match the property names of ConversationRoute, which is how Navigation stores them. */
        const val ARG_CONVERSATION_ID = "conversationId"
        const val ARG_TITLE = "title"
        const val ARG_OTHER_USER_ID = "otherUserId"
        private const val PAGE_SIZE = 50
    }
}
