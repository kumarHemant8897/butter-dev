package com.yourcompany.musicsocial.data.chat.mapper

import com.yourcompany.musicsocial.core.domain.model.DeliveryStatus
import com.yourcompany.musicsocial.core.domain.model.Message
import com.yourcompany.musicsocial.core.domain.model.MessageContent
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
 * Returns null for rows the client cannot represent (unknown kind from a newer backend, or a
 * share without its target) so one bad row is skipped instead of breaking the whole thread.
 *
 * Delivery status is computed from read cursors: a message is READ once another member's
 * last_read_at is at or after its created_at. Computing it this way avoids one receipt row per
 * message per member, which grows quadratically in group chats.
 */
fun MessageDto.toDomainOrNull(otherMembersLastReadAt: List<Instant?>): Message? {
    val sentAt = Instant.parse(createdAt)
    val content: MessageContent = when {
        deletedAt != null -> MessageContent.Deleted
        kind == MessageKind.TEXT -> MessageContent.Text(body ?: return null)
        kind == MessageKind.SONG -> MessageContent.SharedSong(share?.songId ?: return null, body)
        kind == MessageKind.PLAYLIST -> MessageContent.SharedPlaylist(share?.playlistId ?: return null, body)
        else -> return null
    }
    val readByAnyone = otherMembersLastReadAt.any { it != null && !it.isBefore(sentAt) }
    return Message(
        id = id,
        conversationId = conversationId,
        senderId = senderId,
        content = content,
        sentAt = sentAt,
        editedAt = editedAt?.let(Instant::parse),
        status = if (readByAnyone) DeliveryStatus.READ else DeliveryStatus.SENT,
    )
}
