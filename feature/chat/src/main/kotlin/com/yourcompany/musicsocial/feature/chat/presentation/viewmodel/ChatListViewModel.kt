package com.yourcompany.musicsocial.feature.chat.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourcompany.musicsocial.core.domain.model.Conversation
import com.yourcompany.musicsocial.core.domain.model.ConversationType
import com.yourcompany.musicsocial.core.domain.model.MessageContent
import com.yourcompany.musicsocial.core.sample.SampleData
import com.yourcompany.musicsocial.feature.chat.presentation.state.ChatFilter
import com.yourcompany.musicsocial.feature.chat.presentation.state.ChatListUiState
import com.yourcompany.musicsocial.feature.chat.presentation.state.ConversationItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/**
 * Conversation list. Conversations come from SampleData (TESTING ONLY); in Phase 4 they come
 * from ChatRepository.getConversations plus Realtime updates. Filtering and search are real
 * logic and stay as they are.
 */
@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val clock: Clock,
) : ViewModel() {

    private val myId = SampleData.me.id
    private val source: List<Conversation> = SampleData.conversations.sortedByDescending { it.updatedAt }
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(ChatFilter.ALL)

    val uiState: StateFlow<ChatListUiState> = combine(query, filter) { q, f ->
        ChatListUiState(
            query = q,
            filter = f,
            conversations = source.filter { it.matches(f) && it.matches(q) }.map { it.toItem() },
            totalCount = source.size,
            onlineFriends = SampleData.onlineUserIds.size,
            nowPlaying = SampleData.nowPlaying.song,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatListUiState(totalCount = source.size))

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onFilterChange(value: ChatFilter) {
        filter.value = value
    }

    private fun Conversation.matches(f: ChatFilter): Boolean = when (f) {
        ChatFilter.ALL -> true
        ChatFilter.FRIENDS -> type == ConversationType.DIRECT
        ChatFilter.GROUPS -> type == ConversationType.GROUP
    }

    private fun Conversation.matches(q: String): Boolean =
        q.isBlank() || displayTitle().contains(q.trim(), ignoreCase = true)

    private fun Conversation.displayTitle(): String =
        title ?: members.firstOrNull { it.id != myId }?.displayName.orEmpty()

    private fun Conversation.toItem(): ConversationItem {
        val other = members.firstOrNull { it.id != myId }
        val last = lastMessage
        val senderName = when {
            last == null -> null
            last.senderId == myId -> "You"
            type == ConversationType.GROUP -> SampleData.peopleById[last.senderId]?.displayName?.substringBefore(' ')
            else -> null
        }
        val prefix = senderName?.let { "$it: " }.orEmpty()
        val (preview, isMusic) = when (val content = last?.content) {
            is MessageContent.Text -> prefix + content.body to false
            is MessageContent.SharedSong -> "Sent you a song · " + (SampleData.songsById[content.songId]?.title ?: "a song") to true
            is MessageContent.SharedPlaylist ->
                listOfNotNull(senderName, content.comment ?: "shared a playlist").joinToString(" ") + " to the playlist" to true
            MessageContent.Deleted -> "Message deleted" to false
            null -> "" to false
        }
        return ConversationItem(
            id = id,
            title = displayTitle(),
            preview = preview,
            previewIsMusic = isMusic && type == ConversationType.DIRECT,
            timeLabel = timeLabel(updatedAt),
            unreadCount = unreadCount,
            isGroup = type == ConversationType.GROUP,
            isOnline = type == ConversationType.DIRECT && other?.id in SampleData.onlineUserIds,
        )
    }

    /** Today -> "21:39", yesterday -> "Yesterday", older -> "Nov 3". */
    internal fun timeLabel(instant: java.time.Instant): String {
        val zone: ZoneId = clock.zone
        val date = instant.atZone(zone).toLocalDate()
        val today = LocalDate.now(clock)
        return when (date) {
            today -> instant.atZone(zone).format(TIME)
            today.minusDays(1) -> "Yesterday"
            else -> instant.atZone(zone).format(DATE)
        }
    }

    private companion object {
        val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm", Locale.ROOT)
        val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
    }
}
