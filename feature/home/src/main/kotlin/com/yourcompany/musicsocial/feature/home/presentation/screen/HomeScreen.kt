package com.yourcompany.musicsocial.feature.home.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourcompany.musicsocial.core.designsystem.component.ArtworkTint
import com.yourcompany.musicsocial.core.designsystem.component.CircleIconButton
import com.yourcompany.musicsocial.core.designsystem.component.MusicCard
import com.yourcompany.musicsocial.core.designsystem.component.MusicTile
import com.yourcompany.musicsocial.core.designsystem.component.PersonRow
import com.yourcompany.musicsocial.core.designsystem.component.RowAction
import com.yourcompany.musicsocial.core.designsystem.component.ScreenTitle
import com.yourcompany.musicsocial.core.designsystem.component.SearchField
import com.yourcompany.musicsocial.core.designsystem.component.SectionHeader
import com.yourcompany.musicsocial.core.designsystem.component.SongRow
import com.yourcompany.musicsocial.core.designsystem.icon.AppIcons
import com.yourcompany.musicsocial.core.designsystem.theme.ButterTheme
import com.yourcompany.musicsocial.core.designsystem.theme.Spacing
import com.yourcompany.musicsocial.core.ui.component.BackendNotConfiguredNotice
import com.yourcompany.musicsocial.core.ui.component.OfflineBanner
import com.yourcompany.musicsocial.core.ui.format.formatCompactCount
import com.yourcompany.musicsocial.feature.home.R
import com.yourcompany.musicsocial.feature.home.presentation.state.HomeUiState
import com.yourcompany.musicsocial.feature.home.presentation.state.TimeOfDay
import com.yourcompany.musicsocial.feature.home.presentation.state.sampleHomeContent
import com.yourcompany.musicsocial.feature.home.presentation.viewmodel.HomeViewModel

/** Stateful entry point used by navigation. */
@Composable
internal fun HomeRoute(
    contentPadding: PaddingValues,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(state = state, contentPadding = contentPadding)
}

/** Stateless and previewable: everything it shows comes from [state]. */
@Composable
internal fun HomeScreen(
    state: HomeUiState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val content = state.content
    // Search is local to the field until the search feature exists.
    var query by remember { mutableStateOf("") }

    LazyColumn(modifier = modifier, contentPadding = contentPadding) {
        item(key = "offline") { OfflineBanner(visible = state.isOffline) }
        item(key = "title") {
            ScreenTitle(
                eyebrow = stringResource(state.timeOfDay.eyebrowRes()),
                title = stringResource(R.string.home_greeting, state.firstName),
            ) {
                CircleIconButton(
                    icon = AppIcons.Notifications,
                    contentDescription = stringResource(R.string.home_notifications),
                    onClick = {},
                    badge = true,
                )
            }
        }
        item(key = "search") {
            SearchField(
                query = query,
                onQueryChange = { query = it },
                placeholder = stringResource(R.string.home_search_hint),
                modifier = Modifier.padding(horizontal = Spacing.screenHorizontal),
            )
        }

        if (content.recentlyPlayed.isNotEmpty()) {
            item(key = "orbit-title") { SectionHeader(stringResource(R.string.home_section_orbit), actionLabel = stringResource(R.string.home_action_history)) }
            item(key = "orbit") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
                ) {
                    items(content.recentlyPlayed, key = { it.id }) { song ->
                        MusicTile(song.title, song.artistNames, song.artworkUrl, ArtworkTint.forKey(song.id), Modifier.width(112.dp), onClick = {})
                    }
                }
            }
        }

        if (content.recommendedAlbums.isNotEmpty()) {
            item(key = "speed-title") { SectionHeader(stringResource(R.string.home_section_speed), actionLabel = stringResource(R.string.home_action_for_you)) }
            item(key = "speed") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screenHorizontal),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    content.recommendedAlbums.take(2).forEach { album ->
                        MusicCard(
                            title = album.title,
                            subtitle = stringResource(R.string.home_album_subtitle, album.artistName),
                            imageUrl = null,
                            tint = ArtworkTint.forKey(album.id),
                            modifier = Modifier.weight(1f),
                            onClick = {},
                        )
                    }
                }
            }
        }

        content.pickedForYou?.let { song ->
            item(key = "picked") {
                Spacer(Modifier.height(Spacing.md))
                SongRow(
                    title = song.title,
                    subtitle = stringResource(R.string.home_picked_subtitle, song.artistNames),
                    artworkUrl = song.artworkUrl,
                    tint = ArtworkTint.forKey(song.id),
                    action = RowAction(AppIcons.Play, stringResource(R.string.home_play, song.title), onClick = {}),
                )
            }
        }

        content.trending?.let { trending ->
            item(key = "repeat-title") {
                SectionHeader(stringResource(R.string.home_section_repeat), actionLabel = stringResource(R.string.home_action_trending), actionIcon = AppIcons.ArrowUpRight)
            }
            item(key = "repeat") {
                SongRow(
                    title = trending.song.title,
                    subtitle = stringResource(R.string.home_listening_tonight, trending.song.artistNames, formatCompactCount(trending.listeners)),
                    artworkUrl = trending.song.artworkUrl,
                    tint = ArtworkTint.forKey(trending.song.id),
                    action = RowAction(AppIcons.ArrowUpRight, stringResource(R.string.home_open, trending.song.title), onClick = {}),
                )
            }
        }

        if (content.friendsListening.isNotEmpty()) {
            item(key = "company-title") {
                SectionHeader(stringResource(R.string.home_section_company), actionLabel = stringResource(R.string.home_action_live), live = true)
            }
            item(key = "company") {
                Row(Modifier.fillMaxWidth()) {
                    content.friendsListening.take(2).forEach { activity ->
                        PersonRow(
                            name = activity.friend.displayName.substringBefore(' '),
                            subtitle = activity.song.title,
                            avatarUrl = activity.friend.avatarUrl,
                            isOnline = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        content.sharedWithYou?.let { shared ->
            item(key = "passed-title") { SectionHeader(stringResource(R.string.home_section_passed), actionLabel = stringResource(R.string.home_action_open_chats)) }
            item(key = "passed") {
                SongRow(
                    title = "\u201C${shared.quote}\u201D",
                    subtitle = stringResource(R.string.home_shared_subtitle, shared.senderName, shared.song.title, shared.minutesAgo),
                    artworkUrl = shared.song.artworkUrl,
                    tint = ArtworkTint.forKey(shared.song.id),
                    action = RowAction(AppIcons.ArrowUpRight, stringResource(R.string.home_open, shared.song.title), tint = MaterialTheme.colorScheme.primary, onClick = {}),
                )
            }
        }

        content.madeForYou?.let { mix ->
            item(key = "mix-title") { SectionHeader(stringResource(R.string.home_section_after_hours)) }
            item(key = "mix") {
                SongRow(
                    title = mix.title,
                    subtitle = stringResource(R.string.home_mix_subtitle, mix.songCount, mix.description.orEmpty()),
                    artworkUrl = mix.coverUrl,
                    tint = ArtworkTint.forKey(mix.id),
                    action = RowAction(AppIcons.Play, stringResource(R.string.home_play, mix.title), tint = MaterialTheme.colorScheme.primary, onClick = {}),
                )
            }
        }

        if (!state.isBackendConfigured) {
            item(key = "backend") {
                BackendNotConfiguredNotice(Modifier.padding(start = Spacing.screenHorizontal, end = Spacing.screenHorizontal, top = Spacing.sectionGap))
            }
        }
        if (content.isSample) {
            item(key = "sample") {
                Text(
                    stringResource(R.string.home_sample_notice),
                    style = MaterialTheme.typography.labelMedium,
                    color = ButterTheme.colors.textSecondary,
                    modifier = Modifier.padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.lg),
                )
            }
        }
    }
}

private fun TimeOfDay.eyebrowRes(): Int = when (this) {
    TimeOfDay.MORNING -> R.string.home_eyebrow_morning
    TimeOfDay.AFTERNOON -> R.string.home_eyebrow_afternoon
    TimeOfDay.EVENING -> R.string.home_eyebrow_evening
    TimeOfDay.NIGHT -> R.string.home_eyebrow_night
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0F, heightDp = 1400)
@Composable
private fun HomeScreenPreview() {
    ButterTheme {
        HomeScreen(
            state = HomeUiState(
                firstName = "Hemant",
                timeOfDay = TimeOfDay.NIGHT,
                isBackendConfigured = true,
                isOffline = false,
                content = sampleHomeContent(),
            ),
            contentPadding = PaddingValues(),
        )
    }
}
