package com.yourcompany.musicsocial.core.domain.repository

import com.yourcompany.musicsocial.core.domain.model.ConversationSummary
import com.yourcompany.musicsocial.core.domain.model.Message
import com.yourcompany.musicsocial.core.domain.model.OutgoingMessage
import com.yourcompany.musicsocial.core.domain.model.Page
import com.yourcompany.musicsocial.core.domain.model.PageRequest
import com.yourcompany.musicsocial.core.domain.model.UserSummary
import com.yourcompany.musicsocial.core.domain.result.AppResult
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    /** Conversations of the signed-in user, most recently active first. */
    suspend fun getConversations(): AppResult<List<ConversationSummary>>

    /**
     * The direct conversation with [userId], created on first use. Fails with AppError.Auth.Forbidden
     * unless one of the two follows the other (an accepted follow request).
     */
    suspend fun openDirectConversation(userId: String): AppResult<String>

    /** Message history, newest first, for infinite scroll upwards. */
    suspend fun getMessages(conversationId: String, page: PageRequest): AppResult<Page<Message>>

    /** Messages inserted after subscription, delivered through Supabase Realtime. */
    fun observeNewMessages(conversationId: String): Flow<Message>

    /**
     * Sends a message. [clientMessageId] is generated once per user action and reused on retry,
     * so the backend can deduplicate (unique constraint on sender_id + client_id).
     */
    suspend fun sendMessage(
        conversationId: String,
        message: OutgoingMessage,
        clientMessageId: String,
    ): AppResult<Message>

    /** Marks everything in the conversation as read by the signed-in user. */
    suspend fun markRead(conversationId: String): AppResult<Unit>

    /**
     * Creates a group with [title] (1 to 80 characters) and [memberIds]. Only people the signed-in user
     * is connected with (a follow either way, no block) are added; the database skips anyone else.
     */
    suspend fun createGroup(title: String, memberIds: List<String>): AppResult<String>

    /** Everyone in the conversation, including the signed-in user. */
    suspend fun getMembers(conversationId: String): AppResult<List<UserSummary>>

    /** The signed-in user leaves the conversation; it disappears from their chat list. */
    suspend fun leave(conversationId: String): AppResult<Unit>

    companion object {
        /** Matches the conversations.title CHECK constraint. */
        const val GROUP_TITLE_MAX_LENGTH = 80
    }
}
