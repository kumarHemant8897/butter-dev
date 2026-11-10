package com.yourcompany.musicsocial.data.chat.datasource

import com.yourcompany.musicsocial.core.domain.result.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Remote contract for chat. Not implemented yet (Phase 4).
 *
 * - History is read with keyset pagination on (created_at, id), newest first.
 * - New messages arrive through Supabase Realtime postgres_changes on `messages`,
 *   filtered by conversation_id; RLS ensures only members receive them.
 * - Conversations are created through the `create_direct_conversation` /
 *   `create_group_conversation` RPCs so members and conversation are written atomically.
 */
interface ChatRemoteDataSource {
    suspend fun getConversations(beforeLastMessageAt: String?, limit: Int): AppResult<List<ConversationDto>>

    suspend fun getMessages(conversationId: String, before: MessageCursor?, limit: Int): AppResult<List<MessageDto>>

    fun observeInsertedMessages(conversationId: String): Flow<MessageDto>

    suspend fun insertMessage(message: NewMessageDto): AppResult<MessageDto>

    /** Moves the caller's read cursor forward; drives "read" ticks for other members. */
    suspend fun markRead(conversationId: String, upToCreatedAt: String): AppResult<Unit>
}

data class MessageCursor(val createdAt: String, val id: String)

@Serializable
data class ConversationDto(
    val id: String,
    val type: String,
    val title: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("last_message_at") val lastMessageAt: String? = null,
    @SerialName("updated_at") val updatedAt: String,
)

/** Row of `public.messages` with its optional share embedded (`message_shares`). */
@Serializable
data class MessageDto(
    val id: String,
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("client_id") val clientId: String,
    val kind: String,
    val body: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("edited_at") val editedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null,
    val share: MessageShareDto? = null,
)

@Serializable
data class MessageShareDto(
    @SerialName("song_id") val songId: String? = null,
    @SerialName("playlist_id") val playlistId: String? = null,
)

@Serializable
data class NewMessageDto(
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("client_id") val clientId: String,
    val kind: String,
    val body: String? = null,
)
