package com.yourcompany.musicsocial.feature.discover.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourcompany.musicsocial.core.designsystem.component.ArtworkTint
import com.yourcompany.musicsocial.core.designsystem.component.ButtonSize
import com.yourcompany.musicsocial.core.designsystem.component.FilterChip
import com.yourcompany.musicsocial.core.designsystem.component.PersonRow
import com.yourcompany.musicsocial.core.designsystem.component.PrimaryButton
import com.yourcompany.musicsocial.core.designsystem.component.RowAction
import com.yourcompany.musicsocial.core.designsystem.component.ScreenTitle
import com.yourcompany.musicsocial.core.designsystem.component.SearchField
import com.yourcompany.musicsocial.core.designsystem.component.SecondaryButton
import com.yourcompany.musicsocial.core.designsystem.component.SectionHeader
import com.yourcompany.musicsocial.core.designsystem.component.SongRow
import com.yourcompany.musicsocial.core.designsystem.icon.AppIcons
import com.yourcompany.musicsocial.core.designsystem.theme.ButterTheme
import com.yourcompany.musicsocial.core.designsystem.theme.Spacing
import com.yourcompany.musicsocial.core.ui.format.formatCompactCount
import com.yourcompany.musicsocial.feature.discover.R
import com.yourcompany.musicsocial.feature.discover.presentation.components.ArtistChip
import com.yourcompany.musicsocial.feature.discover.presentation.components.FeaturedPlaylistCard
import com.yourcompany.musicsocial.feature.discover.presentation.state.DiscoverUiState
import com.yourcompany.musicsocial.feature.discover.presentation.viewmodel.DiscoverViewModel

@Composable
internal fun DiscoverRoute(contentPadding: PaddingValues, viewModel: DiscoverViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DiscoverScreen(
        state = state,
        contentPadding = contentPadding,
        onQueryChange = viewModel::onQueryChange,
        onToggleFollow = viewModel::onToggleFollow,
        onSelectFeeling = viewModel::onSelectFeeling,
    )
}

@Composable
internal fun DiscoverScreen(
    state: DiscoverUiState,
    contentPadding: PaddingValues,
    onQueryChange: (String) -> Unit,
    onToggleFollow: (String) -> Unit,
    onSelectFeeling: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier, contentPadding = contentPadding) {
        item(key = "title") {
            ScreenTitle(eyebrow = stringResource(R.string.discover_eyebrow), title = stringResource(R.string.discover_title)) {
                Icon(
                    painterResource(AppIcons.AudioLines),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        item(key = "search") {
            SearchField(
                query = state.query,
                onQueryChange = onQueryChange,
                placeholder = stringResource(R.string.discover_search_hint),
                modifier = Modifier.padding(horizontal = Spacing.screenHorizontal),
            )
        }

        if (state.topSongs.isNotEmpty()) {
            item(key = "top-title") {
                SectionHeader(stringResource(R.string.discover_section_repeat), actionLabel = stringResource(R.string.discover_action_top), actionIcon = AppIcons.ArrowUpRight)
            }
            itemsIndexed(state.topSongs, key = { _, item -> "top-${item.song.id}" }) { index, item ->
                SongRow(
                    title = item.song.title,
                    subtitle = stringResource(R.string.discover_listening, item.song.artistNames, formatCompactCount(item.listeners)),
                    artworkUrl = item.song.artworkUrl,
                    tint = ArtworkTint.forKey(item.song.id),
                    leadingText = (index + 1).toString().padStart(2, '0'),
                    action = RowAction(AppIcons.ArrowUpRight, stringResource(R.string.discover_open, item.song.title), onClick = {}),
                )
            }
        }

        state.featuredPlaylist?.let { featured ->
            item(key = "longer-title") { SectionHeader(stringResource(R.string.discover_section_longer), actionLabel = stringResource(R.string.discover_action_playlists)) }
            item(key = "longer") {
                FeaturedPlaylistCard(
                    eyebrow = stringResource(R.string.discover_community_favorite),
                    title = featured.playlist.title,
                    stats = stringResource(R.string.discover_playlist_stats, featured.playlist.songCount, formatCompactCount(featured.saves)),
                    tintKey = featured.playlist.id,
                    onClick = {},
                    modifier = Modifier.padding(horizontal = Spacing.screenHorizontal),
                )
            }
        }

        if (state.artists.isNotEmpty()) {
            item(key = "voices-title") { SectionHeader(stringResource(R.string.discover_section_voices), actionLabel = stringResource(R.string.discover_action_artists)) }
            item(key = "voices") {
                Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.screenHorizontal), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    state.artists.forEach { ArtistChip(it.artist.name, it.genre, Modifier.weight(1f)) }
                }
            }
        }

        if (state.listeners.isNotEmpty()) {
            item(key = "listeners-title") { SectionHeader(stringResource(R.string.discover_section_listeners), actionLabel = stringResource(R.string.discover_action_see_all)) }
            items(state.listeners, key = { "listener-${it.person.id}" }) { match ->
                val followed = match.person.id in state.followedIds
                PersonRow(
                    name = match.person.displayName,
                    subtitle = stringResource(R.string.discover_listener_match, match.tasteMatchPercent, match.sharedArtists),
                    avatarUrl = match.person.avatarUrl,
                ) {
                    if (followed) {
                        SecondaryButton(stringResource(R.string.discover_following), onClick = { onToggleFollow(match.person.id) }, size = ButtonSize.Compact)
                    } else {
                        PrimaryButton(stringResource(R.string.discover_follow), onClick = { onToggleFollow(match.person.id) }, size = ButtonSize.Compact)
                    }
                }
            }
        }

        if (state.feelings.isNotEmpty()) {
            item(key = "feeling-title") { SectionHeader(stringResource(R.string.discover_section_feeling)) }
            item(key = "feelings") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    items(state.feelings, key = { it }) { feeling ->
                        FilterChip(label = feeling, selected = feeling == state.selectedFeeling, onClick = { onSelectFeeling(feeling) })
                    }
                }
                Spacer(Modifier.height(Spacing.xxl))
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0F, heightDp = 1200)
@Composable
private fun DiscoverScreenPreview() {
    ButterTheme {
        DiscoverScreen(DiscoverViewModel().uiState.value, PaddingValues(), {}, {}, {})
    }
}
