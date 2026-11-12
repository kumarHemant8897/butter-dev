package com.yourcompany.musicsocial.data.chat.datasource

import com.yourcompany.musicsocial.core.domain.result.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Remote contract for chat.
 *
 * - The chat list comes from the `my_conversations` RPC (one call, RLS-scoped to the caller).
 * - History is read with keyset pagination on (created_at, id), newest first.
 * - New messages arrive through Supabase Realtime postgres_changes on `messages`,
 *   filtered by conversation_id; RLS ensures only members receive them.
 * - Direct conversations are created through `create_direct_conversation`, which also enforces
 *   that the two people are connected by an accepted follow.
 */
interface ChatRemoteDataSource {
    suspend fun getConversations(): AppResult<List<ConversationRowDto>>

    suspend fun createDirectConversation(otherUserId: String): AppResult<String>

    suspend fun getMessages(conversationId: String, before: MessageCursor?, limit: Int): AppResult<List<MessageDto>>

    /** Read cursors of the other members, for "seen" ticks. */
    suspend fun getOtherMembersLastReadAt(conversationId: String): AppResult<List<String?>>

    fun observeInsertedMessages(conversationId: String): Flow<MessageDto>

    suspend fun insertMessage(message: NewMessageDto): AppResult<MessageDto>

    /** Moves the caller's read cursor forward; drives "read" ticks for other members. */
    suspend fun markRead(conversationId: String, upToCreatedAt: String): AppResult<Unit>

    fun currentUserId(): String?
}

data class MessageCursor(val createdAt: String, val id: String)

/** One row of `my_conversations()`. */
@Serializable
data class ConversationRowDto(
    val id: String,
    val type: String,
    val title: String? = null,
    @SerialName("last_message_at") val lastMessageAt: String? = null,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("other_user_id") val otherUserId: String? = null,
    @SerialName("other_username") val otherUsername: String? = null,
    @SerialName("other_display_name") val otherDisplayName: String? = null,
    @SerialName("other_avatar_url") val otherAvatarUrl: String? = null,
    @SerialName("last_sender_id") val lastSenderId: String? = null,
    @SerialName("last_kind") val lastKind: String? = null,
    @SerialName("last_body") val lastBody: String? = null,
    @SerialName("last_deleted") val lastDeleted: Boolean? = null,
    @SerialName("unread_count") val unreadCount: Int = 0,
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
    @SerialName("sender_id") val senderId: String,
    @SerialName("client_id") val clientId: String,
    val kind: String,
    val body: String? = null,
)
