package com.yourcompany.musicsocial.data.chat.mapper

import com.yourcompany.musicsocial.core.domain.model.Artist
import com.yourcompany.musicsocial.core.domain.model.CatalogRef
import com.yourcompany.musicsocial.core.domain.model.ConversationSummary
import com.yourcompany.musicsocial.core.domain.model.Song
import com.yourcompany.musicsocial.data.chat.datasource.MessageSongDto
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
    val content = contentOf(kind, body, deletedAt != null, song?.toSong()) ?: return null
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

/** Playlist shares are not supported yet, so such rows are skipped. */
private fun contentOf(kind: String?, body: String?, deleted: Boolean, song: Song?): MessageContent? = when {
    deleted -> MessageContent.Deleted
    kind == MessageKind.TEXT -> body?.let { MessageContent.Text(it) }
    kind == MessageKind.SONG -> song?.let { MessageContent.SharedSong(it, body?.takeIf { text -> text.isNotBlank() }) }
    else -> null
}

/** The stored artist line is display text ("A, B"), so the song gets one display artist. */
fun MessageSongDto.toSong() = Song(
    id = songId,
    title = title,
    artists = listOf(Artist(id = "stored:$songId", name = artistNames, imageUrl = null)),
    albumTitle = albumTitle,
    artworkUrl = artworkUrl,
    durationMs = durationMs,
    previewUrl = null,
    externalUrl = externalUrl,
    catalogRef = songId.substringBefore(':', "").takeIf { it.isNotEmpty() }?.let { CatalogRef(it, songId.substringAfter(':')) },
)

/** Display data to store when sharing [this], cut to what the database CHECK constraints accept. */
fun Song.toMessageSong() = MessageSongDto(
    songId = id,
    title = title.take(300),
    artistNames = artistNames.ifBlank { "Unknown artist" }.take(500),
    albumTitle = albumTitle?.take(300),
    artworkUrl = artworkUrl?.takeIf { it.startsWith("https://") && it.length <= 500 },
    externalUrl = externalUrl?.takeIf { it.startsWith("https://") && it.length <= 500 },
    durationMs = durationMs.coerceAtLeast(0),
)

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
        // The list only shows a shared song's title, so the preview song carries just that.
        val previewSong = if (lastKind == MessageKind.SONG) {
            Song("", lastSongTitle ?: "a song", emptyList(), null, null, 0, null, null, null)
        } else {
            null
        }
        contentOf(lastKind, lastBody, lastDeleted == true, previewSong)?.let { LastMessagePreview(sender, it, lastSenderName) }
    }
    return ConversationSummary(
        id = id,
        type = type,
        title = title,
        otherUser = other,
        lastMessage = last,
        unreadCount = unreadCount.coerceAtLeast(0),
        updatedAt = parseSupabaseTimestamp(lastMessageAt) ?: parseSupabaseTimestamp(updatedAt) ?: return null,
        memberCount = memberCount.coerceAtLeast(1),
    )
}
