package com.yourcompany.musicsocial.feature.profile.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourcompany.musicsocial.core.designsystem.component.ButtonSize
import com.yourcompany.musicsocial.core.designsystem.component.CircleIconButton
import com.yourcompany.musicsocial.core.designsystem.component.EmptyState
import com.yourcompany.musicsocial.core.designsystem.component.LoadingIndicator
import com.yourcompany.musicsocial.core.designsystem.component.PersonRow
import com.yourcompany.musicsocial.core.designsystem.component.PrimaryButton
import com.yourcompany.musicsocial.core.designsystem.component.SearchField
import com.yourcompany.musicsocial.core.designsystem.component.SecondaryButton
import com.yourcompany.musicsocial.core.designsystem.component.SectionHeader
import com.yourcompany.musicsocial.core.designsystem.icon.AppIcons
import com.yourcompany.musicsocial.core.designsystem.theme.Spacing
import com.yourcompany.musicsocial.core.domain.model.FollowStatus
import com.yourcompany.musicsocial.core.domain.result.AppError
import com.yourcompany.musicsocial.core.ui.error.AppErrorView
import com.yourcompany.musicsocial.core.ui.error.toErrorCopy
import com.yourcompany.musicsocial.core.ui.format.formatCompactCount
import com.yourcompany.musicsocial.feature.profile.R
import com.yourcompany.musicsocial.feature.profile.presentation.viewmodel.FollowRequestsUiState
import com.yourcompany.musicsocial.feature.profile.presentation.viewmodel.FollowRequestsViewModel
import com.yourcompany.musicsocial.feature.profile.presentation.viewmodel.PeopleSearchUiState
import com.yourcompany.musicsocial.feature.profile.presentation.viewmodel.PeopleSearchViewModel

/** Title bar with a back button, shared by the people screens. */
@Composable
private fun TitleBar(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(icon = AppIcons.Back, contentDescription = stringResource(R.string.profile_back), onClick = onBack)
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = Spacing.sm).semantics { heading() },
        )
    }
}

@Composable
private fun ErrorLine(error: AppError) {
    Text(
        stringResource(error.toErrorCopy().message),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm),
    )
}

// ---- Find your people ---------------------------------------------------------------------------

@Composable
internal fun PeopleSearchRoute(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit,
    viewModel: PeopleSearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var isFirstResume by rememberSaveable { mutableStateOf(true) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (isFirstResume) isFirstResume = false else viewModel.refresh()
    }
    PeopleSearchScreen(state, contentPadding, onBack, viewModel::onQueryChange, viewModel::onFollowClick, onOpenProfile, viewModel::onRetry)
}

@Composable
internal fun PeopleSearchScreen(
    state: PeopleSearchUiState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onFollowClick: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    // Opened to search: the keyboard comes up straight away (once, not again after rotation).
    var focused by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!focused) {
            runCatching { focusRequester.requestFocus() }
            focused = true
        }
    }
    Column(modifier.fillMaxSize().padding(contentPadding).consumeWindowInsets(contentPadding).imePadding()) {
        TitleBar(stringResource(R.string.people_title), onBack)
        SearchField(
            query = state.query,
            onQueryChange = onQueryChange,
            placeholder = stringResource(R.string.people_search_hint),
            focusRequester = focusRequester,
            modifier = Modifier.padding(horizontal = Spacing.screenHorizontal),
        )
        LazyColumn(Modifier.weight(1f)) {
            state.actionError?.let { error -> item(key = "action-error") { ErrorLine(error) } }
            when {
                state.error != null -> item(key = "error") {
                    AppErrorView(error = state.error, onRetry = onRetry, modifier = Modifier.fillMaxWidth().height(320.dp))
                }
                state.isSearching && state.people.isEmpty() -> item(key = "loading") {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { LoadingIndicator() }
                }
                state.people.isEmpty() -> item(key = "empty") {
                    val nobody = state.isShowingSuggestions
                    EmptyState(
                        icon = AppIcons.Profile,
                        title = stringResource(if (nobody) R.string.people_nobody_title else R.string.people_empty_title),
                        message = stringResource(if (nobody) R.string.people_nobody_message else R.string.people_empty_message),
                        modifier = Modifier.height(320.dp),
                    )
                }
                else -> {
                    item(key = "title") {
                        SectionHeader(stringResource(if (state.isShowingSuggestions) R.string.people_suggested else R.string.people_results))
                    }
                    items(state.people, key = { it.profile.id }) { person ->
                        val profile = person.profile
                        PersonRow(
                            name = profile.displayName,
                            subtitle = pluralStringResource(
                                R.plurals.people_handle_followers,
                                profile.followerCount,
                                profile.username,
                                formatCompactCount(profile.followerCount),
                            ),
                            avatarUrl = profile.avatarUrl,
                            onClick = { onOpenProfile(profile.id) },
                        ) {
                            when (person.status) {
                                FollowStatus.NONE -> PrimaryButton(
                                    stringResource(R.string.profile_follow),
                                    onClick = { onFollowClick(profile.id) },
                                    size = ButtonSize.Compact,
                                )
                                FollowStatus.REQUESTED -> SecondaryButton(
                                    stringResource(R.string.profile_requested),
                                    onClick = { onFollowClick(profile.id) },
                                    size = ButtonSize.Compact,
                                )
                                // Unfollowing is a bigger step: it happens on the person's profile.
                                FollowStatus.FOLLOWING -> SecondaryButton(
                                    stringResource(R.string.profile_following_button),
                                    onClick = { onOpenProfile(profile.id) },
                                    size = ButtonSize.Compact,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---- Follow requests ----------------------------------------------------------------------------

@Composable
internal fun FollowRequestsRoute(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onFindPeople: () -> Unit,
    viewModel: FollowRequestsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    FollowRequestsScreen(state, contentPadding, onBack, viewModel::onAccept, viewModel::onDecline, onOpenProfile, onFindPeople, viewModel::onRetry)
}

@Composable
internal fun FollowRequestsScreen(
    state: FollowRequestsUiState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
    onFindPeople: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = contentPadding) {
        item(key = "title") { TitleBar(stringResource(R.string.profile_requests), onBack) }
        state.actionError?.let { error -> item(key = "action-error") { ErrorLine(error) } }
        when {
            state.loadError != null -> item(key = "error") {
                AppErrorView(error = state.loadError, onRetry = onRetry, modifier = Modifier.fillMaxWidth().height(320.dp))
            }
            state.isLoading -> item(key = "loading") {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { LoadingIndicator() }
            }
            state.requests.isEmpty() -> item(key = "empty") {
                EmptyState(
                    icon = AppIcons.Profile,
                    title = stringResource(R.string.profile_requests_empty_title),
                    message = stringResource(R.string.profile_requests_empty_message),
                    actionLabel = stringResource(R.string.profile_find_people),
                    onAction = onFindPeople,
                    modifier = Modifier.height(360.dp),
                )
            }
            else -> items(state.requests, key = { it.requester.id }) { request ->
                val person = request.requester
                PersonRow(
                    name = person.displayName,
                    subtitle = "@${person.username}",
                    avatarUrl = person.avatarUrl,
                    onClick = { onOpenProfile(person.id) },
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        SecondaryButton(stringResource(R.string.profile_decline), onClick = { onDecline(person.id) }, size = ButtonSize.Compact)
                        PrimaryButton(stringResource(R.string.profile_accept), onClick = { onAccept(person.id) }, size = ButtonSize.Compact)
                    }
                }
            }
        }
    }
}
