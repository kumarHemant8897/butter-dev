package com.yourcompany.musicsocial.core.domain.usecase

import com.yourcompany.musicsocial.core.domain.model.Message
import com.yourcompany.musicsocial.core.domain.model.OutgoingMessage
import com.yourcompany.musicsocial.core.domain.repository.ChatRepository
import com.yourcompany.musicsocial.core.domain.result.AppError
import com.yourcompany.musicsocial.core.domain.result.AppResult
import javax.inject.Inject

/**
 * Validates and sends a text message.
 * The length limit matches the `messages_body_length` CHECK constraint in the database.
 */
class SendTextMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {

    suspend operator fun invoke(
        conversationId: String,
        rawText: String,
        clientMessageId: String,
    ): AppResult<Message> {
        val body = rawText.trim()
        if (body.isEmpty()) {
            return AppResult.Failure(AppError.Validation(field = FIELD_BODY, rule = RULE_BLANK))
        }
        if (body.length > MAX_LENGTH) {
            return AppResult.Failure(AppError.Validation(field = FIELD_BODY, rule = RULE_TOO_LONG))
        }
        return chatRepository.sendMessage(
            conversationId = conversationId,
            message = OutgoingMessage.Text(body),
            clientMessageId = clientMessageId,
        )
    }

    companion object {
        const val MAX_LENGTH = 4000
        const val FIELD_BODY = "body"
        const val RULE_BLANK = "blank"
        const val RULE_TOO_LONG = "too_long"
    }
}
