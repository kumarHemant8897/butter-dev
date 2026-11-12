package com.yourcompany.musicsocial.data.chat.mapper

import com.yourcompany.musicsocial.core.domain.model.ConversationSummary
import com.yourcompany.musicsocial.core.domain.model.ConversationType
import com.yourcompany.musicsocial.core.domain.model.DeliveryStatus
import com.yourcompany.musicsocial.core.domain.model.LastMessagePreview
import com.yourcompany.musicsocial.core.domain.model.Message
import com.yourcompany.musicsocial.core.domain.model.MessageContent
import com.yourcompany.musicsocial.core.domain.model.UserSummary
import com.yourcompany.musicsocial.core.network.parseSupabaseTimestamp
import com.yourcompany.musicsocial.data.chat.datasource.ConversationRowDto
import com.yourcompany.musicsocial.data.chat.datasource.MessageDto
import java.time.Instant

/** Values of the `messages.kind` CHECK constraint. */
object MessageKind {
    const val TEXT = "text"
    const val SONG = "song"
    const val PLAYLIST = "playlist"
}

/**
 * Maps a message row to the domain model.
 *
 * Returns null for rows the client cannot represent (unknown kind from a newer backend, a share
 * without its target, an unreadable time) so one bad row is skipped instead of breaking the thread.
 *
 * Delivery status is computed from read cursors: a message is READ once another member's
 * last_read_at is at or after its created_at. Computing it this way avoids one receipt row per
 * message per member, which grows quadratically in group chats.
 */
fun MessageDto.toDomainOrNull(otherMembersLastReadAt: List<Instant?>): Message? {
    val sentAt = parseSupabaseTimestamp(createdAt) ?: return null
    val content = contentOf(kind, body, deletedAt != null, share?.songId, share?.playlistId) ?: return null
    val readByAnyone = otherMembersLastReadAt.any { it != null && !it.isBefore(sentAt) }
    return Message(
        id = id,
        conversationId = conversationId,
        senderId = senderId,
        content = content,
        sentAt = sentAt,
        editedAt = parseSupabaseTimestamp(editedAt),
        status = if (readByAnyone) DeliveryStatus.READ else DeliveryStatus.SENT,
    )
}

private fun contentOf(kind: String?, body: String?, deleted: Boolean, songId: String?, playlistId: String?): MessageContent? = when {
    deleted -> MessageContent.Deleted
    kind == MessageKind.TEXT -> body?.let { MessageContent.Text(it) }
    kind == MessageKind.SONG -> songId?.let { MessageContent.SharedSong(it, body) }
    kind == MessageKind.PLAYLIST -> playlistId?.let { MessageContent.SharedPlaylist(it, body) }
    else -> null
}

/** Returns null for rows without a readable time or with an unknown type. */
fun ConversationRowDto.toDomainOrNull(): ConversationSummary? {
    val type = when (type) {
        "direct" -> ConversationType.DIRECT
        "group" -> ConversationType.GROUP
        else -> return null
    }
    val other = if (otherUserId != null && otherUsername != null) {
        UserSummary(otherUserId, otherUsername, otherDisplayName ?: otherUsername, otherAvatarUrl)
    } else {
        null
    }
    val last = lastSenderId?.let { sender ->
        // Song and playlist shares are previewed without their target id: the list only needs the kind.
        contentOf(lastKind, lastBody, lastDeleted == true, songId = "", playlistId = "")?.let { LastMessagePreview(sender, it) }
    }
    return ConversationSummary(
        id = id,
        type = type,
        title = title,
        otherUser = other,
        lastMessage = last,
        unreadCount = unreadCount.coerceAtLeast(0),
        updatedAt = parseSupabaseTimestamp(lastMessageAt) ?: parseSupabaseTimestamp(updatedAt) ?: return null,
    )
}
