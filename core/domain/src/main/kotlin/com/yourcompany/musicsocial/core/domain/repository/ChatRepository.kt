package com.yourcompany.musicsocial.core.domain.repository

import com.yourcompany.musicsocial.core.domain.model.Conversation
import com.yourcompany.musicsocial.core.domain.model.Message
import com.yourcompany.musicsocial.core.domain.model.OutgoingMessage
import com.yourcompany.musicsocial.core.domain.model.Page
import com.yourcompany.musicsocial.core.domain.model.PageRequest
import com.yourcompany.musicsocial.core.domain.result.AppResult
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    /** Conversations of the signed-in user, most recently active first. */
    suspend fun getConversations(page: PageRequest): AppResult<Page<Conversation>>

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
}
