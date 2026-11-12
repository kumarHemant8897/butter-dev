package com.yourcompany.musicsocial.data.chat.mapper

import com.yourcompany.musicsocial.core.domain.model.DeliveryStatus
import com.yourcompany.musicsocial.core.domain.model.MessageContent
import com.yourcompany.musicsocial.data.chat.datasource.MessageDto
import com.yourcompany.musicsocial.data.chat.datasource.MessageShareDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class MessageMappersTest {

    private val sentAt = "2026-11-09T10:00:00Z"

    @Test
    fun `text message maps to text content`() {
        val message = dto(kind = MessageKind.TEXT, body = "this song is a vibe").toDomainOrNull(emptyList())!!

        assertEquals(MessageContent.Text("this song is a vibe"), message.content)
        assertEquals(Instant.parse(sentAt), message.sentAt)
    }

    @Test
    fun `song share keeps the optional comment`() {
        val message = dto(kind = MessageKind.SONG, body = "on repeat", share = MessageShareDto(songId = "song-1"))
            .toDomainOrNull(emptyList())!!

        assertEquals(MessageContent.SharedSong("song-1", "on repeat"), message.content)
    }

    @Test
    fun `deleted messages hide their content`() {
        val message = dto(kind = MessageKind.TEXT, body = "oops", deletedAt = "2026-11-09T10:01:00Z")
            .toDomainOrNull(emptyList())!!

        assertEquals(MessageContent.Deleted, message.content)
    }

    @Test
    fun `rows the client cannot represent are skipped instead of crashing`() {
        assertNull(dto(kind = "voice_note", body = null).toDomainOrNull(emptyList()))
        assertNull(dto(kind = MessageKind.SONG, body = null, share = null).toDomainOrNull(emptyList()))
    }

    @Test
    fun `status is READ only once another member's read cursor reaches the message`() {
        val before = Instant.parse("2026-11-09T09:59:59Z")
        val exactly = Instant.parse(sentAt)

        assertEquals(DeliveryStatus.SENT, dto().toDomainOrNull(listOf(null, before))!!.status)
        assertEquals(DeliveryStatus.READ, dto().toDomainOrNull(listOf(before, exactly))!!.status)
    }

    private fun dto(
        kind: String = MessageKind.TEXT,
        body: String? = "hi",
        share: MessageShareDto? = null,
        deletedAt: String? = null,
    ) = MessageDto(
        id = "m1",
        conversationId = "c1",
        senderId = "u1",
        clientId = "client-1",
        kind = kind,
        body = body,
        createdAt = sentAt,
        deletedAt = deletedAt,
        share = share,
    )
}

class ConversationRowMappingTest {

    private fun row(
        type: String = "direct",
        lastAt: String? = "2026-11-11T10:00:00.5+00:00",
        kind: String? = "text",
        body: String? = "hey",
    ) = com.yourcompany.musicsocial.data.chat.datasource.ConversationRowDto(
        id = "c1",
        type = type,
        lastMessageAt = lastAt,
        updatedAt = "2026-11-10T09:00:00+00:00",
        otherUserId = "u2",
        otherUsername = "luna.waves",
        otherDisplayName = "Luna Waves",
        lastSenderId = if (kind == null) null else "u2",
        lastKind = kind,
        lastBody = body,
        lastDeleted = false,
        unreadCount = 2,
    )

    @Test
    fun `a direct chat is named by the other person and keeps the latest message`() {
        val summary = row().toDomainOrNull()!!

        assertEquals("Luna Waves", summary.otherUser?.displayName)
        assertEquals(MessageContent.Text("hey"), summary.lastMessage?.content)
        assertEquals(2, summary.unreadCount)
        assertEquals(Instant.parse("2026-11-11T10:00:00.5Z"), summary.updatedAt)
    }

    @Test
    fun `a chat without messages uses its creation time and has no preview`() {
        val summary = row(lastAt = null, kind = null, body = null).toDomainOrNull()!!

        assertNull(summary.lastMessage)
        assertEquals(Instant.parse("2026-11-10T09:00:00Z"), summary.updatedAt)
    }

    @Test
    fun `unknown conversation types are skipped`() {
        assertNull(row(type = "broadcast").toDomainOrNull())
    }

    @Test
    fun `realtime timestamps with a short offset or a space are read`() {
        val dto = MessageDto("m1", "c1", "u1", "k1", MessageKind.TEXT, "hi", createdAt = "2026-11-11 10:00:00.123+00")

        assertEquals(Instant.parse("2026-11-11T10:00:00.123Z"), dto.toDomainOrNull(emptyList())!!.sentAt)
    }
}
