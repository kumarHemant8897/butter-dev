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
