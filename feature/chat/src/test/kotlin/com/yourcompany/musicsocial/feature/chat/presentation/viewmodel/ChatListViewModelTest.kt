package com.yourcompany.musicsocial.feature.chat.presentation.viewmodel

import app.cash.turbine.test
import com.yourcompany.musicsocial.core.domain.model.AuthState
import com.yourcompany.musicsocial.core.domain.model.ConversationSummary
import com.yourcompany.musicsocial.core.domain.model.ConversationType
import com.yourcompany.musicsocial.core.domain.model.LastMessagePreview
import com.yourcompany.musicsocial.core.domain.model.MessageContent
import com.yourcompany.musicsocial.core.domain.model.UserSummary
import com.yourcompany.musicsocial.core.domain.result.AppError
import com.yourcompany.musicsocial.core.testing.MainDispatcherRule
import com.yourcompany.musicsocial.core.testing.fake.FakeAuthRepository
import com.yourcompany.musicsocial.core.testing.fake.FakeChatRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ChatListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-11-09T22:00:00Z"), ZoneOffset.UTC)
    private val chats = FakeChatRepository(meId = "me")

    private fun conversation(id: String, name: String, at: String, last: LastMessagePreview? = null, unread: Int = 0) = ConversationSummary(
        id = id,
        type = ConversationType.DIRECT,
        title = null,
        otherUser = UserSummary("u-$id", name.lowercase(), name, null),
        lastMessage = last,
        unreadCount = unread,
        updatedAt = Instant.parse(at),
    )

    private fun viewModel(auth: AuthState = AuthState.SignedIn("me", null)) = ChatListViewModel(FakeAuthRepository(auth), chats, clock)

    @Test
    fun `a new user has no conversations, which the screen shows as Find your people`() = runTest {
        viewModel().uiState.test {
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertNull(state.error)
            assertEquals(0, state.totalCount)
        }
    }

    @Test
    fun `real conversations are listed newest first with who wrote last`() = runTest {
        chats.conversations = listOf(
            conversation("1", "Luna Waves", "2026-11-08T18:10:00Z", LastMessagePreview("u-1", MessageContent.Text("this song is a vibe")), unread = 2),
            conversation("2", "Eli Chen", "2026-11-09T21:39:00Z", LastMessagePreview("me", MessageContent.Text("on my way"))),
        )

        viewModel().uiState.test {
            val items = expectMostRecentItem().conversations
            assertEquals(listOf("Eli Chen", "Luna Waves"), items.map { it.title })
            assertEquals("You: on my way", items[0].preview)
            assertEquals("this song is a vibe", items[1].preview)
            assertEquals(2, items[1].unreadCount)
            assertEquals("u-1", items[1].otherUserId)
        }
    }

    @Test
    fun `search matches names and usernames`() = runTest {
        chats.conversations = listOf(conversation("1", "Luna Waves", "2026-11-09T21:00:00Z"), conversation("2", "Eli Chen", "2026-11-09T20:00:00Z"))
        val viewModel = viewModel()

        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onQueryChange("  @eli")
            assertEquals(listOf("Eli Chen"), expectMostRecentItem().conversations.map { it.title })
        }
    }

    @Test
    fun `a failed first load shows the error, a failed refresh keeps the list`() = runTest {
        chats.failWith = AppError.Network.Unavailable
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertEquals(AppError.Network.Unavailable, expectMostRecentItem().error)

            chats.failWith = null
            chats.conversations = listOf(conversation("1", "Luna Waves", "2026-11-09T21:00:00Z"))
            viewModel.onRetry()
            assertEquals(1, expectMostRecentItem().totalCount)

            chats.failWith = AppError.Network.Timeout
            viewModel.refresh()
            // Nothing changes: the list stays and no error replaces it.
            expectNoEvents()
            assertEquals(1, viewModel.uiState.value.totalCount)
            assertNull(viewModel.uiState.value.error)
        }
    }

    @Test
    fun `time labels show clock time today and Yesterday for the previous day`() {
        val viewModel = viewModel()

        assertEquals("21:39", viewModel.timeLabel(Instant.parse("2026-11-09T21:39:00Z")))
        assertEquals("Yesterday", viewModel.timeLabel(Instant.parse("2026-11-08T18:10:00Z")))
        assertEquals("Nov 1", viewModel.timeLabel(Instant.parse("2026-11-01T09:00:00Z")))
    }
}
