package com.yourcompany.musicsocial.feature.profile.presentation.screen

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourcompany.musicsocial.core.designsystem.component.ArtworkTint
import com.yourcompany.musicsocial.core.designsystem.component.LoadingView
import com.yourcompany.musicsocial.core.designsystem.component.PrimaryButton
import com.yourcompany.musicsocial.core.designsystem.component.ProfileHeader
import com.yourcompany.musicsocial.core.designsystem.component.ProfileStat
import com.yourcompany.musicsocial.core.designsystem.component.RowAction
import com.yourcompany.musicsocial.core.designsystem.component.SecondaryButton
import com.yourcompany.musicsocial.core.designsystem.component.SectionHeader
import com.yourcompany.musicsocial.core.designsystem.component.SongRow
import com.yourcompany.musicsocial.core.designsystem.component.UserAvatar
import com.yourcompany.musicsocial.core.designsystem.icon.AppIcons
import com.yourcompany.musicsocial.core.designsystem.theme.ButterTheme
import com.yourcompany.musicsocial.core.designsystem.theme.Spacing
import com.yourcompany.musicsocial.core.sample.SampleData
import com.yourcompany.musicsocial.core.ui.component.BackendNotConfiguredNotice
import com.yourcompany.musicsocial.core.ui.component.NoticeCard
import com.yourcompany.musicsocial.core.ui.error.AppErrorView
import com.yourcompany.musicsocial.core.ui.format.formatCompactCount
import com.yourcompany.musicsocial.feature.profile.R
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import com.yourcompany.musicsocial.core.designsystem.component.ButtonSize
import com.yourcompany.musicsocial.core.designsystem.component.PersonRow
import com.yourcompany.musicsocial.core.domain.model.LibraryKind
import com.yourcompany.musicsocial.core.domain.model.Playlist
import com.yourcompany.musicsocial.core.ui.component.CreatePlaylistDialog
import com.yourcompany.musicsocial.core.ui.error.toErrorCopy
import com.yourcompany.musicsocial.feature.profile.presentation.components.LibraryLists
import com.yourcompany.musicsocial.feature.profile.presentation.components.librarySections
import com.yourcompany.musicsocial.feature.profile.presentation.state.ProfileUiState
import com.yourcompany.musicsocial.feature.profile.presentation.state.SampleSections
import com.yourcompany.musicsocial.feature.profile.presentation.viewmodel.ProfileViewModel

@Composable
internal fun ProfileRoute(
    contentPadding: PaddingValues,
    onEditProfile: () -> Unit,
    onOpenRequests: () -> Unit,
    onFindPeople: () -> Unit,
    onOpenPlaylist: (Playlist) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uriHandler = LocalUriHandler.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Coming back from Edit profile: reload quietly so the new name appears. The very first resume is
    // skipped because the ViewModel already loads on creation; rememberSaveable keeps the flag across
    // the trip to Edit profile, so the resume on the way back still refreshes.
    var isFirstResume by rememberSaveable { mutableStateOf(true) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (isFirstResume) isFirstResume = false else viewModel.refresh()
    }
    ProfileScreen(
        state = state,
        contentPadding = contentPadding,
        actions = ProfileActions(
            onEditProfile = onEditProfile,
            onSignOut = viewModel::onSignOut,
            onRetry = viewModel::onRetry,
            onToggleLike = viewModel::onToggleLike,
            onSongClick = viewModel::onSongClick,
            onOpenRequests = onOpenRequests,
            onFindPeople = onFindPeople,
            onOpenUrl = uriHandler::openUri,
            onOpenPlaylist = onOpenPlaylist,
            onNewPlaylist = viewModel::onNewPlaylist,
        ),
    )
    (state as? ProfileUiState.Content)?.takeIf { it.isCreatePlaylistOpen }?.let { content ->
        CreatePlaylistDialog(
            onCreate = viewModel::onCreatePlaylist,
            onDismiss = viewModel::onDismissNewPlaylist,
            creating = content.isCreatingPlaylist,
        )
    }
}

internal data class ProfileActions(
    val onEditProfile: () -> Unit = {},
    val onSignOut: () -> Unit = {},
    val onRetry: () -> Unit = {},
    /** TESTING ONLY: hearts on the sample profile. */
    val onToggleLike: (String) -> Unit = {},
    val onSongClick: (LibraryKind, Int) -> Unit = { _, _ -> },
    val onOpenRequests: () -> Unit = {},
    val onFindPeople: () -> Unit = {},
    val onOpenUrl: (String) -> Unit = {},
    val onOpenPlaylist: (Playlist) -> Unit = {},
    val onNewPlaylist: () -> Unit = {},
)

/** The signed-in user's own profile, adapted from the Figma "Listener profile" frame. */
@Composable
internal fun ProfileScreen(
    state: ProfileUiState,
    contentPadding: PaddingValues,
    actions: ProfileActions,
    modifier: Modifier = Modifier,
) {
    when (state) {
        ProfileUiState.Loading -> Box(modifier.fillMaxSize().padding(contentPadding)) { LoadingView() }
        is ProfileUiState.Error -> Box(modifier.fillMaxSize().padding(contentPadding)) {
            AppErrorView(error = state.error, onRetry = actions.onRetry)
        }
        is ProfileUiState.Content -> ProfileContent(state, contentPadding, actions, modifier)
    }
}

@Composable
private fun ProfileContent(
    state: ProfileUiState.Content,
    contentPadding: PaddingValues,
    actions: ProfileActions,
    modifier: Modifier,
) {
    val profile = state.profile
    val sample = state.sample
    LazyColumn(modifier = modifier, contentPadding = contentPadding) {
        item(key = "top-bar") {
            Box(Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.lg), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.profile_eyebrow).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = ButterTheme.colors.textSecondary,
                )
            }
        }
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
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    PrimaryButton(
                        stringResource(R.string.profile_edit),
                        onClick = actions.onEditProfile,
                        leadingIcon = AppIcons.Edit,
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryButton(
                        stringResource(R.string.profile_sign_out),
                        onClick = actions.onSignOut,
                        loading = state.isSigningOut,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        item(key = "divider") {
            Box(
                Modifier
                    .padding(start = Spacing.screenHorizontal, end = Spacing.screenHorizontal, top = Spacing.xl)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(ButterTheme.colors.divider),
            )
        }

        if (sample != null) {
            sampleSections(sample, actions.onToggleLike)
            item(key = "backend") {
                BackendNotConfiguredNotice(Modifier.padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sectionGap))
            }
        } else {
            item(key = "requests") {
                PersonRow(
                    name = stringResource(R.string.profile_requests),
                    subtitle = if (state.requestCount > 0) {
                        pluralStringResource(R.plurals.profile_requests_waiting, state.requestCount, state.requestCount)
                    } else {
                        stringResource(R.string.profile_requests_none)
                    },
                    avatarUrl = null,
                    subtitleColor = if (state.requestCount > 0) MaterialTheme.colorScheme.primary else null,
                    onClick = actions.onOpenRequests,
                    modifier = Modifier.padding(top = Spacing.lg),
                ) {
                    Icon(painterResource(AppIcons.ArrowUpRight), contentDescription = null, tint = ButterTheme.colors.textSecondary, modifier = Modifier.size(16.dp))
                }
            }
            item(key = "find-people") {
                SecondaryButton(
                    stringResource(R.string.profile_find_people),
                    onClick = actions.onFindPeople,
                    leadingIcon = AppIcons.Search,
                    size = ButtonSize.Compact,
                    modifier = Modifier.padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm),
                )
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
            librarySections(
                state.library,
                isOwn = true,
                onSongClick = actions.onSongClick,
                onOpenUrl = actions.onOpenUrl,
                onOpenPlaylist = actions.onOpenPlaylist,
                onNewPlaylist = actions.onNewPlaylist,
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
    }
}

/** TESTING ONLY: sample sections shown when the build has no backend. */
private fun androidx.compose.foundation.lazy.LazyListScope.sampleSections(
    sample: SampleSections,
    onToggleLike: (String) -> Unit,
) {
    if (sample.topArtists.isNotEmpty()) {
        item(key = "rotation-title") { SectionHeader(stringResource(R.string.profile_section_rotation), actionLabel = stringResource(R.string.profile_action_artists)) }
        item(key = "rotation") {
            Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.screenHorizontal), horizontalArrangement = Arrangement.SpaceBetween) {
                sample.topArtists.take(4).forEach { artist ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        UserAvatar(name = artist.name, imageUrl = artist.imageUrl, size = 52.dp)
                        Spacer(Modifier.height(Spacing.sm))
                        Text(
                            artist.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = ButterTheme.colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
    if (sample.favoriteSongs.isNotEmpty()) {
        item(key = "songs-title") { SectionHeader(stringResource(R.string.profile_section_songs), actionLabel = stringResource(R.string.profile_action_favorites)) }
        items(sample.favoriteSongs, key = { "fav-${it.id}" }) { song ->
            val liked = song.id !in sample.unlikedSongIds
            SongRow(
                title = song.title,
                subtitle = song.artistNames,
                artworkUrl = song.artworkUrl,
                tint = ArtworkTint.forKey(song.id),
                action = RowAction(
                    icon = AppIcons.Favorite,
                    contentDescription = stringResource(if (liked) R.string.profile_unlike else R.string.profile_like, song.title),
                    tint = if (liked) MaterialTheme.colorScheme.primary else null,
                    onClick = { onToggleLike(song.id) },
                ),
            )
        }
    }
    if (sample.publicMixes.isNotEmpty()) {
        item(key = "mixes-title") { SectionHeader(stringResource(R.string.profile_section_mixes), actionLabel = stringResource(R.string.profile_action_public_mixes)) }
        items(sample.publicMixes, key = { "mix-${it.id}" }) { mix ->
            SongRow(
                title = mix.title,
                subtitle = stringResource(R.string.profile_mix_stats, mix.songCount),
                artworkUrl = mix.coverUrl,
                tint = ArtworkTint.forKey(mix.id),
                action = RowAction(AppIcons.ArrowUpRight, stringResource(R.string.profile_open, mix.title), onClick = {}),
            )
        }
    }
    sample.lastPlayed?.let { last ->
        item(key = "last-title") {
            SectionHeader(
                stringResource(R.string.profile_section_last),
                actionLabel = stringResource(R.string.profile_action_opted_in),
                actionColor = ButterTheme.colors.live,
            )
        }
        item(key = "last") {
            SongRow(
                title = last.song.title,
                subtitle = stringResource(R.string.profile_minutes_ago, last.song.artistNames, last.minutesAgo),
                artworkUrl = last.song.artworkUrl,
                tint = ArtworkTint.forKey(last.song.id),
                action = RowAction(AppIcons.Clock, stringResource(R.string.profile_history), onClick = {}),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0F, heightDp = 1000)
@Composable
private fun ProfileScreenSamplePreview() {
    ButterTheme {
        ProfileScreen(
            state = ProfileUiState.Content(
                profile = SampleData.me,
                sample = SampleSections(SampleData.myTopArtists, SampleData.myFavoriteSongs, SampleData.myPublicMixes, SampleData.myLastPlayed),
            ),
            contentPadding = PaddingValues(),
            actions = ProfileActions(),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0F, heightDp = 900)
@Composable
private fun ProfileScreenRealPreview() {
    ButterTheme {
        ProfileScreen(
            state = ProfileUiState.Content(
                profile = SampleData.me,
                library = LibraryLists(liked = SampleData.myFavoriteSongs),
                requestCount = 2,
            ),
            contentPadding = PaddingValues(),
            actions = ProfileActions(),
        )
    }
}
