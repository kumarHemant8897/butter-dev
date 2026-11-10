package com.yourcompany.musicsocial.core.domain.usecase

import com.yourcompany.musicsocial.core.domain.model.DeliveryStatus
import com.yourcompany.musicsocial.core.domain.model.Message
import com.yourcompany.musicsocial.core.domain.model.MessageContent
import com.yourcompany.musicsocial.core.domain.model.OutgoingMessage
import com.yourcompany.musicsocial.core.domain.repository.ChatRepository
import com.yourcompany.musicsocial.core.domain.result.AppError
import com.yourcompany.musicsocial.core.domain.result.AppResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class SendTextMessageUseCaseTest {

    private val chatRepository = mockk<ChatRepository>()
    private val sendTextMessage = SendTextMessageUseCase(chatRepository)

    @Test
    fun `blank text is rejected without calling the repository`() = runTest {
        val result = sendTextMessage(conversationId = "c1", rawText = "   \n ", clientMessageId = "client-1")

        assertEquals(
            AppResult.Failure(AppError.Validation(SendTextMessageUseCase.FIELD_BODY, SendTextMessageUseCase.RULE_BLANK)),
            result,
        )
        coVerify(exactly = 0) { chatRepository.sendMessage(any(), any(), any()) }
    }

    @Test
    fun `text longer than the database limit is rejected`() = runTest {
        val tooLong = "a".repeat(SendTextMessageUseCase.MAX_LENGTH + 1)

        val result = sendTextMessage("c1", tooLong, "client-1")

        assertEquals(
            AppResult.Failure(AppError.Validation(SendTextMessageUseCase.FIELD_BODY, SendTextMessageUseCase.RULE_TOO_LONG)),
            result,
        )
        coVerify(exactly = 0) { chatRepository.sendMessage(any(), any(), any()) }
    }

    @Test
    fun `valid text is trimmed and sent with the caller's idempotency key`() = runTest {
        val sent = Message(
            id = "m1",
            conversationId = "c1",
            senderId = "u1",
            content = MessageContent.Text("hey, listen to this"),
            sentAt = Instant.parse("2026-11-09T10:00:00Z"),
            editedAt = null,
            status = DeliveryStatus.SENT,
        )
        coEvery { chatRepository.sendMessage(any(), any(), any()) } returns AppResult.Success(sent)

        val result = sendTextMessage("c1", "  hey, listen to this  ", "client-1")

        assertEquals(AppResult.Success(sent), result)
        coVerify(exactly = 1) {
            chatRepository.sendMessage("c1", OutgoingMessage.Text("hey, listen to this"), "client-1")
        }
    }

    @Test
    fun `repository failure is passed through unchanged`() = runTest {
        coEvery { chatRepository.sendMessage(any(), any(), any()) } returns AppResult.Failure(AppError.Network.Unavailable)

        val result = sendTextMessage("c1", "hello", "client-1")

        assertEquals(AppResult.Failure(AppError.Network.Unavailable), result)
    }
}
