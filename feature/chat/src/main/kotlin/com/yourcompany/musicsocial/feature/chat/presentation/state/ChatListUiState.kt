package com.yourcompany.musicsocial.feature.chat.presentation.state

import androidx.compose.runtime.Immutable
import com.yourcompany.musicsocial.core.domain.result.AppError

enum class ChatFilter { ALL, FRIENDS, GROUPS }

/** One row in the conversation list, already formatted for display. */
@Immutable
data class ConversationItem(
    val id: String,
    val title: String,
    val avatarUrl: String? = null,
    /** The other person in a direct chat; opens their profile from the conversation header. */
    val otherUserId: String? = null,
    val preview: String,
    /** The preview is a shared song or playlist: shown with a music note. */
    val previewIsMusic: Boolean,
    val timeLabel: String,
    val unreadCount: Int,
    val isGroup: Boolean,
    val isOnline: Boolean = false,
)

@Immutable
data class ChatListUiState(
    val query: String = "",
    val filter: ChatFilter = ChatFilter.ALL,
    val conversations: List<ConversationItem> = emptyList(),
    val totalCount: Int = 0,
    val isLoading: Boolean = true,
    /** Loading failed and there is nothing to show. A failed background refresh keeps the list. */
    val error: AppError? = null,
)
