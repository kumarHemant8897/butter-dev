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
import com.yourcompany.musicsocial.core.domain.model.ReportReason
import com.yourcompany.musicsocial.core.domain.model.UserProfile
import com.yourcompany.musicsocial.feature.profile.presentation.state.ProfileDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.Role
import com.yourcompany.musicsocial.core.navigation.ConversationRoute
import com.yourcompany.musicsocial.core.navigation.FollowListKind
import com.yourcompany.musicsocial.core.navigation.FollowListRoute
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
    onOpenFollowList: (FollowListRoute) -> Unit,
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
            onOpenFollowList = onOpenFollowList,
            onShowDialog = viewModel::onShowDialog,
            onDismissDialog = viewModel::onDismissDialog,
            onConfirmBlock = viewModel::onConfirmBlock,
            onConfirmUnblock = viewModel::onConfirmUnblock,
            onSendReport = viewModel::onSendReport,
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
    val onOpenFollowList: (FollowListRoute) -> Unit = {},
    val onShowDialog: (ProfileDialog) -> Unit = {},
    val onDismissDialog: () -> Unit = {},
    val onConfirmBlock: () -> Unit = {},
    val onConfirmUnblock: () -> Unit = {},
    val onSendReport: (ReportReason, String) -> Unit = { _, _ -> },
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
    SafetyDialogs(state, actions)
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = contentPadding) {
        item(key = "back") {
            Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                CircleIconButton(icon = AppIcons.Back, contentDescription = stringResource(R.string.profile_back), onClick = actions.onBack)
                Spacer(Modifier.weight(1f))
                SafetyMenu(blocked = relationship.blockedByMe, name = profile.displayName, actions = actions)
            }
        }
        item(key = "header") {
            ProfileHeader(
                displayName = profile.displayName,
                username = profile.username,
                avatarUrl = profile.avatarUrl,
                bio = profile.bio,
                stats = listOf(
                    ProfileStat(
                        formatCompactCount(profile.followerCount),
                        pluralStringResource(R.plurals.profile_followers, profile.followerCount),
                        onClick = { actions.onOpenFollowList(FollowListRoute(profile.id, FollowListKind.FOLLOWERS, profile.displayName)) },
                    ),
                    ProfileStat(
                        formatCompactCount(profile.followingCount),
                        stringResource(R.string.profile_following),
                        onClick = { actions.onOpenFollowList(FollowListRoute(profile.id, FollowListKind.FOLLOWING, profile.displayName)) },
                    ),
                ),
                affinityLabel = if (relationship.followsMe) stringResource(R.string.profile_follows_you) else null,
            ) {
                if (relationship.blockedByMe) {
                    SecondaryButton(
                        stringResource(R.string.profile_unblock),
                        onClick = { actions.onShowDialog(ProfileDialog.UNBLOCK) },
                        enabled = !state.isUpdating,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
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

        if (state.reportSent) {
            item(key = "report-sent") {
                Text(
                    stringResource(R.string.profile_report_sent),
                    style = MaterialTheme.typography.bodySmall,
                    color = ButterTheme.colors.live,
                    modifier = Modifier.padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm),
                )
            }
        }

        if (relationship.blockedByMe) {
            item(key = "blocked") {
                NoticeCard(
                    icon = AppIcons.Error,
                    title = stringResource(R.string.profile_blocked_title, profile.displayName),
                    message = stringResource(R.string.profile_blocked_message),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sectionGap),
                )
            }
        } else if (relationship.canSeeLibrary) {
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

/** ⋯ in the top bar: Block / Unblock and Report. */
@Composable
private fun SafetyMenu(blocked: Boolean, name: String, actions: UserProfileActions) {
    var open by remember { mutableStateOf(false) }
    Box {
        CircleIconButton(icon = AppIcons.More, contentDescription = stringResource(R.string.profile_more, name), onClick = { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(if (blocked) R.string.profile_unblock else R.string.profile_block)) },
                onClick = {
                    open = false
                    actions.onShowDialog(if (blocked) ProfileDialog.UNBLOCK else ProfileDialog.BLOCK)
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.profile_report)) },
                onClick = {
                    open = false
                    actions.onShowDialog(ProfileDialog.REPORT)
                },
            )
        }
    }
}

@Composable
private fun SafetyDialogs(state: UserProfileUiState.Content, actions: UserProfileActions) {
    val name = state.profile.displayName
    when (state.dialog) {
        ProfileDialog.BLOCK -> AlertDialog(
            onDismissRequest = actions.onDismissDialog,
            title = { Text(stringResource(R.string.profile_block_title, name)) },
            text = { Text(stringResource(R.string.profile_block_message)) },
            confirmButton = {
                TextButton(onClick = actions.onConfirmBlock) { Text(stringResource(R.string.profile_block), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = actions.onDismissDialog) { Text(stringResource(R.string.profile_cancel)) } },
        )
        ProfileDialog.UNBLOCK -> AlertDialog(
            onDismissRequest = actions.onDismissDialog,
            title = { Text(stringResource(R.string.profile_unblock_title, name)) },
            text = { Text(stringResource(R.string.profile_unblock_message)) },
            confirmButton = { TextButton(onClick = actions.onConfirmUnblock) { Text(stringResource(R.string.profile_unblock)) } },
            dismissButton = { TextButton(onClick = actions.onDismissDialog) { Text(stringResource(R.string.profile_cancel)) } },
        )
        ProfileDialog.REPORT -> ReportDialog(name, state.isReporting, actions)
        null -> Unit
    }
}

@Composable
private fun ReportDialog(name: String, sending: Boolean, actions: UserProfileActions) {
    var reason by rememberSaveable { mutableStateOf(ReportReason.SPAM) }
    var details by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = actions.onDismissDialog,
        title = { Text(stringResource(R.string.profile_report_title, name)) },
        text = {
            Column {
                ReportReason.entries.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = option == reason, role = Role.RadioButton, onClick = { reason = option })
                            .padding(vertical = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option == reason, onClick = null)
                        Spacer(Modifier.width(Spacing.sm))
                        Text(stringResource(option.label()))
                    }
                }
                OutlinedTextField(
                    value = details,
                    onValueChange = { if (it.length <= REPORT_DETAILS_MAX) details = it },
                    label = { Text(stringResource(R.string.profile_report_details)) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { actions.onSendReport(reason, details) }, enabled = !sending) { Text(stringResource(R.string.profile_report_send)) }
        },
        dismissButton = { TextButton(onClick = actions.onDismissDialog, enabled = !sending) { Text(stringResource(R.string.profile_cancel)) } },
    )
}

private const val REPORT_DETAILS_MAX = 1000

private fun ReportReason.label(): Int = when (this) {
    ReportReason.SPAM -> R.string.report_reason_spam
    ReportReason.HARASSMENT -> R.string.report_reason_harassment
    ReportReason.INAPPROPRIATE -> R.string.report_reason_inappropriate
    ReportReason.IMPERSONATION -> R.string.report_reason_impersonation
    ReportReason.OTHER -> R.string.report_reason_other
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
