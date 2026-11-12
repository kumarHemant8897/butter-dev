package com.yourcompany.musicsocial.data.chat.repository

import com.yourcompany.musicsocial.core.domain.model.ConversationSummary
import com.yourcompany.musicsocial.core.domain.model.Message
import com.yourcompany.musicsocial.core.domain.model.OutgoingMessage
import com.yourcompany.musicsocial.core.domain.model.Page
import com.yourcompany.musicsocial.core.domain.model.PageRequest
import com.yourcompany.musicsocial.core.domain.repository.ChatRepository
import com.yourcompany.musicsocial.core.domain.result.AppError
import com.yourcompany.musicsocial.core.domain.result.AppResult
import com.yourcompany.musicsocial.core.domain.result.flatMap
import com.yourcompany.musicsocial.core.domain.result.map
import com.yourcompany.musicsocial.core.network.parseSupabaseTimestamp
import com.yourcompany.musicsocial.data.chat.datasource.ChatRemoteDataSource
import com.yourcompany.musicsocial.data.chat.datasource.MessageCursor
import com.yourcompany.musicsocial.data.chat.datasource.NewMessageDto
import com.yourcompany.musicsocial.data.chat.mapper.MessageKind
import com.yourcompany.musicsocial.data.chat.mapper.toDomainOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import java.time.Clock
import javax.inject.Inject

internal class ChatRepositoryImpl @Inject constructor(
    private val remote: ChatRemoteDataSource,
    private val clock: Clock,
) : ChatRepository {

    override suspend fun getConversations(): AppResult<List<ConversationSummary>> =
        remote.getConversations().map { rows -> rows.mapNotNull { it.toDomainOrNull() } }

    override suspend fun openDirectConversation(userId: String): AppResult<String> = remote.createDirectConversation(userId)

    override suspend fun getMessages(conversationId: String, page: PageRequest): AppResult<Page<Message>> {
        val cursor = page.cursor?.let(::decodeCursor)
        val readCursors = remote.getOtherMembersLastReadAt(conversationId).map { list -> list.map(::parseSupabaseTimestamp) }
        return readCursors.flatMap { lastReads ->
            remote.getMessages(conversationId, cursor, page.limit).map { rows ->
                val next = if (rows.size == page.limit) rows.last().let { encodeCursor(it.createdAt, it.id) } else null
                Page(rows.mapNotNull { it.toDomainOrNull(lastReads) }, nextCursor = next)
            }
        }
    }

    /** Realtime does not carry read cursors; a live message is new, so nobody has read it yet. */
    override fun observeNewMessages(conversationId: String): Flow<Message> =
        remote.observeInsertedMessages(conversationId).mapNotNull { it.toDomainOrNull(emptyList()) }

    override suspend fun sendMessage(conversationId: String, message: OutgoingMessage, clientMessageId: String): AppResult<Message> {
        val text = (message as? OutgoingMessage.Text)
            // Song and playlist shares need message_shares rows; they arrive with song sharing.
            ?: return AppResult.Failure(AppError.Validation(field = "kind", rule = "unsupported"))
        val row = NewMessageDto(
            conversationId = conversationId,
            senderId = "",
            clientId = clientMessageId,
            kind = MessageKind.TEXT,
            body = text.body,
        )
        return remote.insertMessage(row).flatMap { dto ->
            dto.toDomainOrNull(emptyList())?.let { AppResult.Success(it) } ?: AppResult.Failure(AppError.Unknown)
        }
    }

    /** "Now" from the device clock; the database compares it with message times. */
    override suspend fun markRead(conversationId: String): AppResult<Unit> =
        remote.markRead(conversationId, clock.instant().toString())

    private companion object {
        const val SEPARATOR = "|"

        fun encodeCursor(createdAt: String, id: String) = createdAt + SEPARATOR + id

        fun decodeCursor(value: String): MessageCursor? {
            val createdAt = value.substringBeforeLast(SEPARATOR, "")
            val id = value.substringAfterLast(SEPARATOR, "")
            return if (createdAt.isEmpty() || id.isEmpty()) null else MessageCursor(createdAt, id)
        }
    }
}
