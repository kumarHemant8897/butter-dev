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
import org.junit.Assert.assertFalse
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
        playback,
        clock,
    )

    private val playback = com.yourcompany.musicsocial.core.testing.fake.FakePlaybackController()

    private fun shared(id: String, songId: String, at: String) = Message(
        id, "c1", "u2",
        MessageContent.SharedSong(com.yourcompany.musicsocial.core.domain.model.Song(songId, "Song $songId", emptyList(), null, null, 0, null, null, null), null),
        Instant.parse(at), null, DeliveryStatus.SENT,
    )

    @Test
    fun `tapping a shared song plays the thread's shared songs from it, oldest first`() {
        chats.messages["c1"] = mutableListOf(
            shared("m1", "itunes:1", "2026-11-11T10:00:00Z"),
            message("m2", "me", "nice", "2026-11-11T10:01:00Z"),
            shared("m3", "itunes:2", "2026-11-11T10:02:00Z"),
        )
        val viewModel = viewModel()

        viewModel.onPlaySong("itunes:2")

        val (queue, start) = playback.playRequests.single()
        assertEquals(listOf("itunes:1", "itunes:2"), queue.map { it.id })
        assertEquals(1, start)

        viewModel.onPlaySong("itunes:2")
        assertEquals(1, playback.toggleCount)
    }

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

class GroupChatTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val chats = FakeChatRepository(meId = "me")
    private val users = com.yourcompany.musicsocial.core.testing.fake.FakeUserRepository(meId = "me")
    private val auth = FakeAuthRepository(AuthState.SignedIn("me", null))
    private fun person(id: String, name: String) = com.yourcompany.musicsocial.core.domain.model.UserProfile(id, id, name, null, null, 0, 0)

    @Test
    fun `new group lists the people I am connected with once each and creates the group`() {
        users.followerLists["me"] = listOf(person("b", "Ben"), person("c", "Cara"))
        users.followingLists["me"] = listOf(person("b", "Ben"), person("a", "Asha"))
        val viewModel = NewGroupViewModel(auth, users, chats)
        assertEquals(listOf("Asha", "Ben", "Cara"), viewModel.uiState.value.people.map { it.displayName })

        viewModel.onTitleChange("  Weekend ")
        assertFalse(viewModel.uiState.value.canCreate)
        viewModel.onToggle("a")
        viewModel.onToggle("c")
        viewModel.onCreate()

        assertEquals(listOf("Weekend" to listOf("a", "c")), chats.createdGroups.map { it.first to it.second.sorted() })
        val opened = viewModel.uiState.value.created!!
        assertTrue(opened.isGroup)
        assertEquals("Weekend", opened.title)
    }

    @Test
    fun `a group conversation names senders and can be left`() {
        chats.members["g1"] = listOf(
            com.yourcompany.musicsocial.core.domain.model.UserSummary("me", "me", "Hemant Kumar", null),
            com.yourcompany.musicsocial.core.domain.model.UserSummary("u2", "riya", "Riya Sharma", null),
        )
        chats.messages["g1"] = mutableListOf(
            Message("m1", "g1", "u2", MessageContent.Text("see you there"), Instant.parse("2026-11-11T10:00:00Z"), null, DeliveryStatus.SENT),
        )
        val viewModel = ConversationViewModel(
            SavedStateHandle(
                mapOf(
                    ConversationViewModel.ARG_CONVERSATION_ID to "g1",
                    ConversationViewModel.ARG_TITLE to "Weekend",
                    ConversationViewModel.ARG_IS_GROUP to true,
                ),
            ),
            auth,
            chats,
            SendTextMessageUseCase(chats),
            com.yourcompany.musicsocial.core.testing.fake.FakePlaybackController(),
            Clock.fixed(Instant.parse("2026-11-11T10:05:00Z"), ZoneOffset.UTC),
        )

        val state = viewModel.uiState.value
        assertTrue(state.isGroup)
        assertEquals(2, state.members.size)
        assertEquals("Riya", state.senderName(state.messages.single().senderId))

        viewModel.onRequestLeave()
        assertTrue(viewModel.uiState.value.isConfirmingLeave)
        viewModel.onConfirmLeave()
        assertEquals(listOf("g1"), chats.left)
        assertTrue(viewModel.uiState.value.hasLeft)
    }
}
