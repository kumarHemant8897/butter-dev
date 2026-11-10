package com.yourcompany.musicsocial.feature.profile.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourcompany.musicsocial.core.designsystem.component.ArtworkTint
import com.yourcompany.musicsocial.core.designsystem.component.CircleIconButton
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
import com.yourcompany.musicsocial.core.ui.format.formatCompactCount
import com.yourcompany.musicsocial.feature.profile.R
import com.yourcompany.musicsocial.feature.profile.presentation.state.AccountStatus
import com.yourcompany.musicsocial.feature.profile.presentation.state.ProfileUiState
import com.yourcompany.musicsocial.feature.profile.presentation.viewmodel.ProfileViewModel

@Composable
internal fun ProfileRoute(
    contentPadding: PaddingValues,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileScreen(state = state, contentPadding = contentPadding, onToggleLike = viewModel::onToggleLike)
}

/**
 * The signed-in user's own profile, adapted from the Figma "Listener profile" frame:
 * Edit profile / Share replace Follow / Message, and the affinity label is omitted.
 */
@Composable
internal fun ProfileScreen(
    state: ProfileUiState,
    contentPadding: PaddingValues,
    onToggleLike: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val profile = state.profile
    LazyColumn(modifier = modifier, contentPadding = contentPadding) {
        item(key = "top-bar") {
            // Figma: small centred caps label in the top bar, action on the right.
            Box(Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.sm), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.profile_eyebrow).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = ButterTheme.colors.textSecondary,
                )
                CircleIconButton(
                    icon = AppIcons.Settings,
                    contentDescription = stringResource(R.string.profile_settings),
                    onClick = {},
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
            Spacer(Modifier.height(Spacing.md))
        }
        item(key = "header") {
            ProfileHeader(
                displayName = profile.displayName,
                username = profile.username,
                avatarUrl = profile.avatarUrl,
                bio = profile.bio,
                stats = listOf(
                    ProfileStat(formatCompactCount(profile.followerCount), stringResource(R.string.profile_followers)),
                    ProfileStat(formatCompactCount(profile.followingCount), stringResource(R.string.profile_following)),
                ),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    PrimaryButton(stringResource(R.string.profile_edit), onClick = {}, leadingIcon = AppIcons.Edit, modifier = Modifier.weight(1f))
                    SecondaryButton(stringResource(R.string.profile_share), onClick = {}, leadingIcon = AppIcons.Share, modifier = Modifier.weight(1f))
                }
            }
        }
        item(key = "divider") {
            Box(Modifier.padding(start = Spacing.screenHorizontal, end = Spacing.screenHorizontal, top = Spacing.xl).fillMaxWidth().height(1.dp).background(ButterTheme.colors.divider))
        }

        if (state.topArtists.isNotEmpty()) {
            item(key = "rotation-title") { SectionHeader(stringResource(R.string.profile_section_rotation), actionLabel = stringResource(R.string.profile_action_artists)) }
            item(key = "rotation") {
                Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.screenHorizontal), horizontalArrangement = Arrangement.SpaceBetween) {
                    state.topArtists.take(4).forEach { artist ->
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

        if (state.favoriteSongs.isNotEmpty()) {
            item(key = "songs-title") { SectionHeader(stringResource(R.string.profile_section_songs), actionLabel = stringResource(R.string.profile_action_favorites)) }
            items(state.favoriteSongs, key = { "fav-${it.id}" }) { song ->
                val liked = song.id !in state.unlikedSongIds
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

        if (state.publicMixes.isNotEmpty()) {
            item(key = "mixes-title") { SectionHeader(stringResource(R.string.profile_section_mixes), actionLabel = stringResource(R.string.profile_action_public_mixes)) }
            items(state.publicMixes, key = { "mix-${it.id}" }) { mix ->
                SongRow(
                    title = mix.title,
                    subtitle = stringResource(R.string.profile_mix_stats, mix.songCount),
                    artworkUrl = mix.coverUrl,
                    tint = ArtworkTint.forKey(mix.id),
                    action = RowAction(AppIcons.ArrowUpRight, stringResource(R.string.profile_open, mix.title), onClick = {}),
                )
            }
        }

        state.lastPlayed?.let { last ->
            item(key = "last-title") {
                SectionHeader(
                    stringResource(R.string.profile_section_last),
                    actionLabel = stringResource(R.string.profile_action_opted_in),
                    actionColor = ButterTheme.colors.live,
                    live = false,
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

        item(key = "account") {
            Column(Modifier.padding(start = Spacing.screenHorizontal, end = Spacing.screenHorizontal, top = Spacing.sectionGap, bottom = Spacing.lg)) {
                when (state.account) {
                    AccountStatus.BackendUnavailable -> BackendNotConfiguredNotice()
                    AccountStatus.SignedOut -> Text(
                        stringResource(R.string.profile_signed_out),
                        style = MaterialTheme.typography.labelMedium,
                        color = ButterTheme.colors.textSecondary,
                    )
                    AccountStatus.Loading, is AccountStatus.SignedIn -> Unit
                }
                if (state.isSample) {
                    Spacer(Modifier.height(Spacing.md))
                    Text(
                        stringResource(R.string.profile_sample_notice),
                        style = MaterialTheme.typography.labelMedium,
                        color = ButterTheme.colors.textSecondary,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0F, heightDp = 1100)
@Composable
private fun ProfileScreenPreview() {
    ButterTheme {
        ProfileScreen(
            state = ProfileUiState(
                account = AccountStatus.SignedIn("preview"),
                profile = SampleData.me,
                topArtists = SampleData.myTopArtists,
                favoriteSongs = SampleData.myFavoriteSongs,
                publicMixes = SampleData.myPublicMixes,
                lastPlayed = SampleData.myLastPlayed,
                isSample = true,
            ),
            contentPadding = PaddingValues(),
            onToggleLike = {},
        )
    }
}
