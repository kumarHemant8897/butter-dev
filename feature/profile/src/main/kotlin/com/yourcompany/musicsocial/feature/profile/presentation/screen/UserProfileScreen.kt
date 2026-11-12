package com.yourcompany.musicsocial.feature.profile.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourcompany.musicsocial.core.designsystem.component.CircleIconButton
import com.yourcompany.musicsocial.core.designsystem.component.LoadingIndicator
import com.yourcompany.musicsocial.core.designsystem.component.LoadingView
import com.yourcompany.musicsocial.core.designsystem.component.PrimaryButton
import com.yourcompany.musicsocial.core.designsystem.component.ProfileHeader
import com.yourcompany.musicsocial.core.designsystem.component.ProfileStat
import com.yourcompany.musicsocial.core.designsystem.component.SecondaryButton
import com.yourcompany.musicsocial.core.designsystem.icon.AppIcons
import com.yourcompany.musicsocial.core.designsystem.theme.ButterTheme
import com.yourcompany.musicsocial.core.designsystem.theme.Spacing
import com.yourcompany.musicsocial.core.domain.model.FollowStatus
import com.yourcompany.musicsocial.core.domain.model.LibraryKind
import com.yourcompany.musicsocial.core.domain.model.Playlist
import com.yourcompany.musicsocial.core.domain.model.Relationship
import com.yourcompany.musicsocial.core.domain.model.UserProfile
import com.yourcompany.musicsocial.core.navigation.ConversationRoute
import com.yourcompany.musicsocial.core.ui.component.NoticeCard
import com.yourcompany.musicsocial.core.ui.error.AppErrorView
import com.yourcompany.musicsocial.core.ui.error.toErrorCopy
import com.yourcompany.musicsocial.core.ui.format.formatCompactCount
import com.yourcompany.musicsocial.feature.profile.R
import com.yourcompany.musicsocial.feature.profile.presentation.components.librarySections
import com.yourcompany.musicsocial.feature.profile.presentation.state.UserProfileUiState
import com.yourcompany.musicsocial.feature.profile.presentation.viewmodel.UserProfileViewModel
import com.yourcompany.musicsocial.feature.profile.presentation.viewmodel.UserProfileViewModel.FollowLabel

@Composable
internal fun UserProfileRoute(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenConversation: (ConversationRoute) -> Unit,
    onOpenPlaylist: (Playlist) -> Unit,
    viewModel: UserProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val open = (state as? UserProfileUiState.Content)?.openConversation
    LaunchedEffect(open) {
        if (open != null) {
            viewModel.onConversationOpened()
            onOpenConversation(open)
        }
    }
    // Back from a chat or another app: the other person may have accepted the request meanwhile.
    var isFirstResume by rememberSaveable { mutableStateOf(true) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (isFirstResume) isFirstResume = false else viewModel.refresh()
    }
    // While a request waits, check now and then so "Requested" turns into "Following" on its own.
    val waiting = (state as? UserProfileUiState.Content)?.relationship?.status == FollowStatus.REQUESTED
    LaunchedEffect(waiting) {
        while (waiting) {
            kotlinx.coroutines.delay(REQUEST_CHECK_EVERY_MS)
            viewModel.refresh()
        }
    }
    UserProfileScreen(
        state = state,
        contentPadding = contentPadding,
        actions = UserProfileActions(
            onBack = onBack,
            onRetry = viewModel::onRetry,
            onFollowClick = viewModel::onFollowClick,
            onMessageClick = viewModel::onMessageClick,
            onSongClick = viewModel::onSongClick,
            onOpenUrl = uriHandler::openUri,
            onOpenPlaylist = onOpenPlaylist,
        ),
    )
}

private const val REQUEST_CHECK_EVERY_MS = 6_000L

internal data class UserProfileActions(
    val onBack: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onFollowClick: () -> Unit = {},
    val onMessageClick: () -> Unit = {},
    val onSongClick: (LibraryKind, Int) -> Unit = { _, _ -> },
    val onOpenUrl: (String) -> Unit = {},
    val onOpenPlaylist: (Playlist) -> Unit = {},
)

@Composable
internal fun UserProfileScreen(
    state: UserProfileUiState,
    contentPadding: PaddingValues,
    actions: UserProfileActions,
    modifier: Modifier = Modifier,
) {
    when (state) {
        UserProfileUiState.Loading -> Box(modifier.fillMaxSize().padding(contentPadding)) {
            BackButton(actions.onBack)
            LoadingView()
        }
        is UserProfileUiState.Error -> Box(modifier.fillMaxSize().padding(contentPadding)) {
            AppErrorView(error = state.error, onRetry = actions.onRetry)
            BackButton(actions.onBack)
        }
        is UserProfileUiState.Content -> Content(state, contentPadding, actions, modifier)
    }
}

@Composable
private fun BackButton(onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.sm)) {
        CircleIconButton(icon = AppIcons.Back, contentDescription = stringResource(R.string.profile_back), onClick = onBack)
    }
}

@Composable
private fun Content(state: UserProfileUiState.Content, contentPadding: PaddingValues, actions: UserProfileActions, modifier: Modifier) {
    val profile = state.profile
    val relationship = state.relationship
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = contentPadding) {
        item(key = "back") { BackButton(actions.onBack) }
        item(key = "header") {
            ProfileHeader(
                displayName = profile.displayName,
                username = profile.username,
                avatarUrl = profile.avatarUrl,
                bio = profile.bio,
                stats = listOf(
                    ProfileStat(formatCompactCount(profile.followerCount), pluralStringResource(R.plurals.profile_followers, profile.followerCount)),
                    ProfileStat(formatCompactCount(profile.followingCount), stringResource(R.string.profile_following)),
                ),
                affinityLabel = if (relationship.followsMe) stringResource(R.string.profile_follows_you) else null,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    FollowButton(relationship, state.isUpdating, actions.onFollowClick, Modifier.weight(1f))
                    // Shown only when the database would allow it: someone follows the other.
                    if (relationship.canMessage) {
                        SecondaryButton(
                            stringResource(R.string.profile_message),
                            onClick = actions.onMessageClick,
                            loading = state.isOpeningChat,
                            leadingIcon = AppIcons.Message,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        state.actionError?.let { error ->
            item(key = "action-error") {
                Text(
                    stringResource(error.toErrorCopy().message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm),
                )
            }
        }

        if (relationship.canSeeLibrary) {
            if (state.isLibraryLoading && state.library.liked.isEmpty() && state.library.saved.isEmpty()) {
                item(key = "library-loading") {
                    Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) { LoadingIndicator() }
                }
            } else {
                librarySections(
                    state.library,
                    isOwn = false,
                    onSongClick = actions.onSongClick,
                    onOpenUrl = actions.onOpenUrl,
                    onOpenPlaylist = actions.onOpenPlaylist,
                )
                item(key = "credit") {
                    Text(
                        stringResource(R.string.profile_preview_credit),
                        style = MaterialTheme.typography.labelMedium,
                        color = ButterTheme.colors.textSecondary,
                        modifier = Modifier.padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.lg),
                    )
                }
            }
        } else {
            item(key = "private") {
                val requested = relationship.status == FollowStatus.REQUESTED
                NoticeCard(
                    icon = if (requested) AppIcons.Clock else AppIcons.Favorite,
                    title = stringResource(if (requested) R.string.profile_requested_title else R.string.profile_private_title),
                    message = stringResource(
                        if (requested) R.string.profile_requested_message else R.string.profile_private_message,
                        profile.displayName,
                    ),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sectionGap),
                )
            }
        }
    }
}

/** Follow / Follow back (primary) until a request exists; Requested / Following (quiet) after. */
@Composable
private fun FollowButton(relationship: Relationship, loading: Boolean, onClick: () -> Unit, modifier: Modifier) {
    when (val label = UserProfileViewModel.followLabel(relationship)) {
        FollowLabel.FOLLOW, FollowLabel.FOLLOW_BACK -> PrimaryButton(
            stringResource(if (label == FollowLabel.FOLLOW) R.string.profile_follow else R.string.profile_follow_back),
            onClick = onClick,
            enabled = !loading,
            leadingIcon = AppIcons.Plus,
            modifier = modifier,
        )
        FollowLabel.REQUESTED, FollowLabel.FOLLOWING -> SecondaryButton(
            stringResource(if (label == FollowLabel.REQUESTED) R.string.profile_requested else R.string.profile_following_button),
            onClick = onClick,
            enabled = !loading,
            modifier = modifier,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0F, heightDp = 800)
@Composable
private fun UserProfileScreenPreview() {
    ButterTheme {
        UserProfileScreen(
            state = UserProfileUiState.Content(
                profile = UserProfile("u2", "luna.waves", "Luna Waves", "late night playlists", null, 12, 30),
                relationship = Relationship(FollowStatus.REQUESTED, followsMe = true),
            ),
            contentPadding = PaddingValues(),
            actions = UserProfileActions(),
        )
    }
}
