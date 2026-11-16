package com.yourcompany.musicsocial.feature.chat.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourcompany.musicsocial.core.designsystem.component.CircleIconButton
import com.yourcompany.musicsocial.core.designsystem.component.EmptyState
import com.yourcompany.musicsocial.core.designsystem.component.FilterChip
import com.yourcompany.musicsocial.core.designsystem.component.LoadingIndicator
import com.yourcompany.musicsocial.core.designsystem.component.ScreenTitle
import com.yourcompany.musicsocial.core.designsystem.component.SearchField
import com.yourcompany.musicsocial.core.designsystem.icon.AppIcons
import com.yourcompany.musicsocial.core.designsystem.theme.ButterTheme
import com.yourcompany.musicsocial.core.designsystem.theme.Spacing
import com.yourcompany.musicsocial.core.navigation.ConversationRoute
import com.yourcompany.musicsocial.core.ui.error.AppErrorView
import com.yourcompany.musicsocial.feature.chat.R
import com.yourcompany.musicsocial.feature.chat.presentation.components.ConversationRow
import com.yourcompany.musicsocial.feature.chat.presentation.state.ChatFilter
import com.yourcompany.musicsocial.feature.chat.presentation.state.ChatListUiState
import com.yourcompany.musicsocial.feature.chat.presentation.viewmodel.ChatListViewModel
import kotlinx.coroutines.delay

@Composable
internal fun ChatListRoute(
    contentPadding: PaddingValues,
    onFindPeople: () -> Unit,
    onOpenConversation: (ConversationRoute) -> Unit,
    onNewGroup: () -> Unit,
    viewModel: ChatListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    // While the list is on screen, pick up new chats and unread counts from the other phone.
    LaunchedEffect(Unit) {
        while (true) {
            delay(REFRESH_EVERY_MS)
            viewModel.refresh()
        }
    }
    ChatListScreen(
        state = state,
        contentPadding = contentPadding,
        actions = ChatListActions(
            onQueryChange = viewModel::onQueryChange,
            onFilterChange = viewModel::onFilterChange,
            onFindPeople = onFindPeople,
            onRetry = viewModel::onRetry,
            onOpenConversation = onOpenConversation,
            onNewGroup = onNewGroup,
        ),
    )
}

private const val REFRESH_EVERY_MS = 8_000L

internal data class ChatListActions(
    val onQueryChange: (String) -> Unit = {},
    val onFilterChange: (ChatFilter) -> Unit = {},
    val onFindPeople: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onOpenConversation: (ConversationRoute) -> Unit = {},
    val onNewGroup: () -> Unit = {},
)

@Composable
internal fun ChatListScreen(
    state: ChatListUiState,
    contentPadding: PaddingValues,
    actions: ChatListActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().padding(contentPadding)) {
        LazyColumn(Modifier.weight(1f)) {
            item(key = "title") {
                ScreenTitle(eyebrow = stringResource(R.string.chat_eyebrow), title = stringResource(R.string.chat_title)) {
                    // New message (find the person) or new group.
                    var menuOpen by remember { mutableStateOf(false) }
                    Box {
                        CircleIconButton(
                            icon = AppIcons.Compose,
                            contentDescription = stringResource(R.string.chat_new),
                            onClick = { menuOpen = true },
                            filled = true,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.chat_new_message)) },
                                onClick = {
                                    menuOpen = false
                                    actions.onFindPeople()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.chat_new_group)) },
                                onClick = {
                                    menuOpen = false
                                    actions.onNewGroup()
                                },
                            )
                        }
                    }
                }
            }
            when {
                state.isLoading && state.totalCount == 0 -> item(key = "loading") {
                    Box(Modifier.fillMaxWidth().height(320.dp), contentAlignment = Alignment.Center) { LoadingIndicator() }
                }
                state.error != null -> item(key = "error") {
                    AppErrorView(error = state.error, onRetry = actions.onRetry, modifier = Modifier.fillMaxWidth().height(360.dp))
                }
                // A new user: no chats at all. Show the way to people instead of an empty list.
                state.totalCount == 0 -> item(key = "find-people") {
                    EmptyState(
                        icon = AppIcons.Profile,
                        title = stringResource(R.string.chat_find_people_title),
                        message = stringResource(R.string.chat_find_people_message),
                        actionLabel = stringResource(R.string.chat_find_people_action),
                        onAction = actions.onFindPeople,
                        modifier = Modifier.height(420.dp),
                    )
                }
                else -> {
                    item(key = "search") {
                        SearchField(
                            query = state.query,
                            onQueryChange = actions.onQueryChange,
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
                            FilterChip(stringResource(R.string.chat_filter_all), state.filter == ChatFilter.ALL, { actions.onFilterChange(ChatFilter.ALL) }, count = state.totalCount)
                            FilterChip(stringResource(R.string.chat_filter_friends), state.filter == ChatFilter.FRIENDS, { actions.onFilterChange(ChatFilter.FRIENDS) })
                            FilterChip(stringResource(R.string.chat_filter_groups), state.filter == ChatFilter.GROUPS, { actions.onFilterChange(ChatFilter.GROUPS) })
                        }
                    }
                    if (state.conversations.isEmpty()) {
                        item(key = "no-results") {
                            EmptyState(
                                icon = AppIcons.Chats,
                                title = stringResource(R.string.chat_no_results_title),
                                message = stringResource(R.string.chat_no_results_message),
                                modifier = Modifier.height(280.dp),
                            )
                        }
                    }
                    items(state.conversations, key = { it.id }) { item ->
                        ConversationRow(
                            item = item,
                            onClick = { actions.onOpenConversation(ConversationRoute(item.id, item.title, item.otherUserId, isGroup = item.isGroup)) },
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0F, heightDp = 860)
@Composable
private fun ChatListScreenEmptyPreview() {
    ButterTheme {
        ChatListScreen(ChatListUiState(isLoading = false), PaddingValues(), ChatListActions())
    }
}
