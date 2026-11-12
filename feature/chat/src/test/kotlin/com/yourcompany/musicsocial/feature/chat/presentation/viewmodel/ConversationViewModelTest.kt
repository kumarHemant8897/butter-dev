package com.yourcompany.musicsocial.feature.chat.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.yourcompany.musicsocial.core.domain.model.AuthState
import com.yourcompany.musicsocial.core.domain.model.DeliveryStatus
import com.yourcompany.musicsocial.core.domain.model.Message
import com.yourcompany.musicsocial.core.domain.model.MessageContent
import com.yourcompany.musicsocial.core.domain.model.OutgoingMessage
import com.yourcompany.musicsocial.core.domain.result.AppError
import com.yourcompany.musicsocial.core.domain.usecase.SendTextMessageUseCase
import com.yourcompany.musicsocial.core.testing.MainDispatcherRule
import com.yourcompany.musicsocial.core.testing.fake.FakeAuthRepository
import com.yourcompany.musicsocial.core.testing.fake.FakeChatRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ConversationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-11-11T10:05:00Z"), ZoneOffset.UTC)
    private val chats = FakeChatRepository(meId = "me")

    private fun message(id: String, sender: String, body: String, at: String) =
        Message(id, "c1", sender, MessageContent.Text(body), Instant.parse(at), null, DeliveryStatus.SENT)

    private fun viewModel() = ConversationViewModel(
        SavedStateHandle(
            mapOf(
                ConversationViewModel.ARG_CONVERSATION_ID to "c1",
                ConversationViewModel.ARG_TITLE to "Luna Waves",
                ConversationViewModel.ARG_OTHER_USER_ID to "u2",
            ),
        ),
        FakeAuthRepository(AuthState.SignedIn("me", null)),
        chats,
        SendTextMessageUseCase(chats),
        clock,
    )

    @Test
    fun `history loads newest first, marks the thread read and knows whose messages are whose`() {
        chats.messages["c1"] = mutableListOf(
            message("m1", "u2", "hey", "2026-11-11T10:00:00Z"),
            message("m2", "me", "hi!", "2026-11-11T10:01:00Z"),
        )

        val state = viewModel().uiState.value

        assertEquals(listOf("m2", "m1"), state.messages.map { it.id })
        assertEquals(listOf(true, false), state.messages.map { it.isMine })
        assertEquals("Luna Waves", state.title)
        assertEquals(listOf("c1"), chats.markedRead)
    }

    @Test
    fun `a live message from the other person appears once and is marked read`() = runTest {
        val viewModel = viewModel()
        val live = message("m9", "u2", "you there?", "2026-11-11T10:04:00Z")

        chats.emitLive(live)
        chats.emitLive(live)

        assertEquals(listOf("m9"), viewModel.uiState.value.messages.map { it.id })
        assertTrue(chats.markedRead.size >= 2)
    }

    @Test
    fun `sending clears the box and shows the stored message`() {
        val viewModel = viewModel()

        viewModel.onDraftChange("  this song is a vibe ")
        viewModel.onSend()

        val state = viewModel.uiState.value
        assertEquals("", state.draft)
        assertEquals(listOf("c1" to OutgoingMessage.Text("this song is a vibe")), chats.sent)
        val bubble = state.messages.single()
        assertTrue(bubble.isMine)
        assertEquals(DeliveryStatus.SENT, bubble.status)
    }

    @Test
    fun `a failed send stays as a failed bubble and retry sends it`() {
        val viewModel = viewModel()
        chats.failWith = AppError.Network.Unavailable

        viewModel.onDraftChange("hello")
        viewModel.onSend()
        val failed = viewModel.uiState.value.messages.single()
        assertEquals(DeliveryStatus.FAILED, failed.status)
        assertEquals(AppError.Network.Unavailable, viewModel.uiState.value.sendError)

        chats.failWith = null
        viewModel.onRetrySend(failed.id)

        val after = viewModel.uiState.value.messages.single()
        assertEquals(DeliveryStatus.SENT, after.status)
        assertEquals(1, chats.sent.size)
    }

    @Test
    fun `blank drafts are not sent`() {
        val viewModel = viewModel()

        viewModel.onDraftChange("   ")
        viewModel.onSend()

        assertTrue(chats.sent.isEmpty())
        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }
}
