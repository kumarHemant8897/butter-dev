package com.yourcompany.musicsocial.feature.chat.presentation.viewmodel

import app.cash.turbine.test
import com.yourcompany.musicsocial.core.testing.MainDispatcherRule
import com.yourcompany.musicsocial.feature.chat.presentation.state.ChatFilter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ChatListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-11-09T22:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `filters split direct and group conversations`() = runTest {
        val viewModel = ChatListViewModel(clock)

        viewModel.uiState.test {
            val all = awaitItem().conversations
            viewModel.onFilterChange(ChatFilter.GROUPS)
            val groups = awaitItem().conversations
            viewModel.onFilterChange(ChatFilter.FRIENDS)
            val friends = awaitItem().conversations

            assertTrue(groups.all { it.isGroup })
            assertTrue(friends.none { it.isGroup })
            assertEquals(all.size, groups.size + friends.size)
        }
    }

    @Test
    fun `search matches names case-insensitively`() = runTest {
        val viewModel = ChatListViewModel(clock)

        viewModel.uiState.test {
            awaitItem()
            viewModel.onQueryChange("  eli ")
            assertEquals(listOf("Eli Chen"), awaitItem().conversations.map { it.title })
        }
    }

    @Test
    fun `time labels show clock time today and Yesterday for the previous day`() {
        val viewModel = ChatListViewModel(clock)

        assertEquals("21:39", viewModel.timeLabel(Instant.parse("2026-11-09T21:39:00Z")))
        assertEquals("Yesterday", viewModel.timeLabel(Instant.parse("2026-11-08T18:10:00Z")))
        assertEquals("Nov 1", viewModel.timeLabel(Instant.parse("2026-11-01T09:00:00Z")))
    }
}
