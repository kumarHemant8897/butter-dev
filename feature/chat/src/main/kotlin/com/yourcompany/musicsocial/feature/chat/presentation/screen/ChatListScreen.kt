package com.yourcompany.musicsocial.feature.chat.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourcompany.musicsocial.core.designsystem.component.CircleIconButton
import com.yourcompany.musicsocial.core.designsystem.component.EmptyState
import com.yourcompany.musicsocial.core.designsystem.component.FilterChip
import com.yourcompany.musicsocial.core.designsystem.component.ScreenTitle
import com.yourcompany.musicsocial.core.designsystem.component.SearchField
import com.yourcompany.musicsocial.core.designsystem.icon.AppIcons
import com.yourcompany.musicsocial.core.designsystem.theme.ButterTheme
import com.yourcompany.musicsocial.core.designsystem.theme.Spacing
import com.yourcompany.musicsocial.feature.chat.R
import com.yourcompany.musicsocial.feature.chat.presentation.components.ConversationRow
import com.yourcompany.musicsocial.feature.chat.presentation.components.NowPlayingDock
import com.yourcompany.musicsocial.feature.chat.presentation.state.ChatFilter
import com.yourcompany.musicsocial.feature.chat.presentation.state.ChatListUiState
import com.yourcompany.musicsocial.feature.chat.presentation.viewmodel.ChatListViewModel

@Composable
internal fun ChatListRoute(contentPadding: PaddingValues, viewModel: ChatListViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ChatListScreen(state, contentPadding, viewModel::onQueryChange, viewModel::onFilterChange)
}

@Composable
internal fun ChatListScreen(
    state: ChatListUiState,
    contentPadding: PaddingValues,
    onQueryChange: (String) -> Unit,
    onFilterChange: (ChatFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The mini player's play state is local until the shared playback controller exists (Phase 3).
    var dockPlaying by remember { mutableStateOf(true) }

    Column(modifier.fillMaxSize().padding(contentPadding)) {
        LazyColumn(Modifier.weight(1f)) {
            item(key = "title") {
                ScreenTitle(eyebrow = stringResource(R.string.chat_eyebrow), title = stringResource(R.string.chat_title)) {
                    CircleIconButton(
                        icon = AppIcons.Compose,
                        contentDescription = stringResource(R.string.chat_new),
                        onClick = {},
                        filled = true,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            item(key = "search") {
                SearchField(
                    query = state.query,
                    onQueryChange = onQueryChange,
                    placeholder = stringResource(R.string.chat_search_hint),
                    modifier = Modifier.padding(horizontal = Spacing.screenHorizontal),
                )
            }
            item(key = "filters") {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterChip(stringResource(R.string.chat_filter_all), state.filter == ChatFilter.ALL, { onFilterChange(ChatFilter.ALL) }, count = state.totalCount)
                    FilterChip(stringResource(R.string.chat_filter_friends), state.filter == ChatFilter.FRIENDS, { onFilterChange(ChatFilter.FRIENDS) })
                    FilterChip(stringResource(R.string.chat_filter_groups), state.filter == ChatFilter.GROUPS, { onFilterChange(ChatFilter.GROUPS) })
                    Spacer(Modifier.width(Spacing.xs))
                    Box(Modifier.width(1.dp).height(16.dp).background(ButterTheme.colors.divider))
                    Spacer(Modifier.width(Spacing.xs))
                    Text(
                        stringResource(R.string.chat_friends_online, state.onlineFriends),
                        style = MaterialTheme.typography.labelMedium,
                        color = ButterTheme.colors.textSecondary,
                    )
                }
            }
            if (state.conversations.isEmpty()) {
                item(key = "empty") {
                    val searching = state.query.isNotBlank()
                    EmptyState(
                        icon = AppIcons.Chats,
                        title = stringResource(if (searching) R.string.chat_no_results_title else R.string.chat_empty_title),
                        message = stringResource(if (searching) R.string.chat_no_results_message else R.string.chat_empty_message),
                        modifier = Modifier.height(320.dp),
                    )
                }
            }
            items(state.conversations, key = { it.id }) { item ->
                ConversationRow(item = item, onClick = {})
            }
        }
        state.nowPlaying?.let { song ->
            NowPlayingDock(
                title = song.title,
                artist = song.artistNames,
                tintKey = song.id,
                isPlaying = dockPlaying,
                onTogglePlay = { dockPlaying = !dockPlaying },
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0F, heightDp = 860)
@Composable
private fun ChatListScreenPreview() {
    ButterTheme {
        ChatListScreen(ChatListUiState(totalCount = 0), PaddingValues(), {}, {})
    }
}
