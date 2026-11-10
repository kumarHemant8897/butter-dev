package com.yourcompany.musicsocial.feature.chat.presentation.state

import androidx.compose.runtime.Immutable
import com.yourcompany.musicsocial.core.domain.model.Song

enum class ChatFilter { ALL, FRIENDS, GROUPS }

/** One row in the conversation list, already formatted for display. */
@Immutable
data class ConversationItem(
    val id: String,
    val title: String,
    val preview: String,
    /** The preview is a shared song or playlist: shown with a music note. */
    val previewIsMusic: Boolean,
    val timeLabel: String,
    val unreadCount: Int,
    val isGroup: Boolean,
    val isOnline: Boolean,
)

@Immutable
data class ChatListUiState(
    val query: String = "",
    val filter: ChatFilter = ChatFilter.ALL,
    val conversations: List<ConversationItem> = emptyList(),
    val totalCount: Int = 0,
    val onlineFriends: Int = 0,
    val nowPlaying: Song? = null,
    val isNowPlayingPaused: Boolean = false,
)
