package com.yourcompany.musicsocial.core.domain.model

import java.time.Instant

enum class ConversationType { DIRECT, GROUP }

data class Conversation(
    val id: String,
    val type: ConversationType,
    /** Group title. Null for direct conversations, where the UI shows the other member's name. */
    val title: String?,
    val avatarUrl: String?,
    val members: List<UserSummary>,
    val lastMessage: Message?,
    val unreadCount: Int,
    val updatedAt: Instant,
)

data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val content: MessageContent,
    val sentAt: Instant,
    val editedAt: Instant?,
    val status: DeliveryStatus,
)

sealed interface MessageContent {
    data class Text(val body: String) : MessageContent
    /** A catalogue song with display data; it plays through a fresh preview lookup. */
    data class SharedSong(val song: Song, val comment: String?) : MessageContent
    data class SharedPlaylist(val playlistId: String, val comment: String?) : MessageContent

    /** A message that was deleted by its sender. Kept so the timeline does not shift. */
    data object Deleted : MessageContent
}

/**
 * Delivery state from the sender's point of view.
 * SENDING and FAILED are local-only; the others are derived from members' read/delivery cursors.
 */
enum class DeliveryStatus { SENDING, SENT, DELIVERED, READ, FAILED }

/** What the user is sending. Kept separate from [MessageContent] because a send cannot be "Deleted". */
sealed interface OutgoingMessage {
    data class Text(val body: String) : OutgoingMessage
    data class SongShare(val song: Song, val comment: String?) : OutgoingMessage
    data class PlaylistShare(val playlistId: String, val comment: String?) : OutgoingMessage
}
