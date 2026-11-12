package com.yourcompany.musicsocial.feature.home.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourcompany.musicsocial.core.designsystem.component.ArtworkTint
import com.yourcompany.musicsocial.core.designsystem.component.ButtonSize
import com.yourcompany.musicsocial.core.designsystem.component.LoadingIndicator
import com.yourcompany.musicsocial.core.designsystem.component.MusicCard
import com.yourcompany.musicsocial.core.designsystem.component.MusicTile
import com.yourcompany.musicsocial.core.designsystem.component.RowAction
import com.yourcompany.musicsocial.core.designsystem.component.ScreenTitle
import com.yourcompany.musicsocial.core.designsystem.component.SecondaryButton
import com.yourcompany.musicsocial.core.designsystem.component.SectionHeader
import com.yourcompany.musicsocial.core.designsystem.component.SongRow
import com.yourcompany.musicsocial.core.designsystem.icon.AppIcons
import com.yourcompany.musicsocial.core.designsystem.theme.AppShapes
import com.yourcompany.musicsocial.core.designsystem.theme.ButterTheme
import com.yourcompany.musicsocial.core.designsystem.theme.Dimens
import com.yourcompany.musicsocial.core.designsystem.theme.Spacing
import com.yourcompany.musicsocial.core.domain.model.Artist
import com.yourcompany.musicsocial.core.domain.model.Song
import com.yourcompany.musicsocial.core.navigation.SongListKind
import com.yourcompany.musicsocial.core.navigation.SongListRoute
import com.yourcompany.musicsocial.core.ui.component.OfflineBanner
import com.yourcompany.musicsocial.core.ui.error.toErrorCopy
import com.yourcompany.musicsocial.feature.home.R
import com.yourcompany.musicsocial.feature.home.presentation.state.ChartState
import com.yourcompany.musicsocial.feature.home.presentation.state.HomeUiState
import com.yourcompany.musicsocial.feature.home.presentation.state.TimeOfDay
import com.yourcompany.musicsocial.feature.home.presentation.viewmodel.HomeViewModel

/** Stateful entry point used by navigation. */
@Composable
internal fun HomeRoute(
    contentPadding: PaddingValues,
    onOpenSearch: () -> Unit,
    onOpenSongList: (SongListRoute) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    // The first resume is skipped (the ViewModel already loads); later ones pick up profile edits.
    var isFirstResume by rememberSaveable { mutableStateOf(true) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (isFirstResume) isFirstResume = false else viewModel.refresh()
    }
    HomeScreen(
        state = state,
        contentPadding = contentPadding,
        actions = HomeActions(
            onOpenSearch = onOpenSearch,
            onChartSongClick = viewModel::onChartSongClick,
            onRecentSongClick = viewModel::onRecentSongClick,
            onRetryChart = viewModel::onRetryChart,
            onOpenUrl = uriHandler::openUri,
            onOpenChart = { title -> onOpenSongList(SongListRoute(kind = SongListKind.CHART, title = title)) },
        ),
    )
}

internal data class HomeActions(
    val onOpenSearch: () -> Unit = {},
    val onChartSongClick: (Int) -> Unit = {},
    val onRecentSongClick: (Int) -> Unit = {},
    val onRetryChart: () -> Unit = {},
    val onOpenUrl: (String) -> Unit = {},
    /** Opens the whole chart; the argument is the list title. */
    val onOpenChart: (String) -> Unit = {},
)

/** Stateless and previewable: everything it shows comes from [state]. */
@Composable
internal fun HomeScreen(
    state: HomeUiState,
    contentPadding: PaddingValues,
    actions: HomeActions,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier, contentPadding = contentPadding) {
        item(key = "offline") { OfflineBanner(visible = state.isOffline) }
        item(key = "title") {
            ScreenTitle(
                eyebrow = stringResource(state.timeOfDay.eyebrowRes()),
                title = if (state.firstName != null) stringResource(R.string.home_greeting, state.firstName) else stringResource(R.string.home_greeting_unknown),
            )
        }
        item(key = "search") {
            SearchEntry(
                hint = stringResource(R.string.home_search_hint),
                onClick = actions.onOpenSearch,
                modifier = Modifier.padding(horizontal = Spacing.screenHorizontal),
            )
        }

        if (state.recentlyPlayed.isNotEmpty()) {
            item(key = "orbit-title") { SectionHeader(stringResource(R.string.home_section_orbit)) }
            item(key = "orbit") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
                ) {
                    itemsIndexed(state.recentlyPlayed, key = { _, song -> "recent-${song.id}" }) { index, song ->
                        MusicTile(
                            title = song.title,
                            subtitle = song.artistNames,
                            imageUrl = song.artworkUrl,
                            tint = ArtworkTint.forKey(song.id),
                            modifier = Modifier.width(150.dp),
                            onClick = { actions.onRecentSongClick(index) },
                        )
                    }
                }
            }
        }

        chartSections(state, actions)
    }
}

private fun LazyListScope.chartSections(state: HomeUiState, actions: HomeActions) {
    when (val chart = state.chart) {
        ChartState.Loading -> item(key = "chart-loading") {
            Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) { LoadingIndicator() }
        }
        is ChartState.Error -> item(key = "chart-error") {
            Column(
                Modifier.padding(start = Spacing.screenHorizontal, end = Spacing.screenHorizontal, top = Spacing.sectionGap),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text(
                    stringResource(chart.error.toErrorCopy().message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                SecondaryButton(stringResource(R.string.home_retry), onClick = actions.onRetryChart, size = ButtonSize.Compact)
            }
        }
        is ChartState.Content -> {
            val songs = chart.songs
            if (songs.isEmpty()) {
                item(key = "chart-empty") {
                    Text(
                        stringResource(R.string.home_chart_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = ButterTheme.colors.textSecondary,
                        modifier = Modifier.padding(start = Spacing.screenHorizontal, end = Spacing.screenHorizontal, top = Spacing.sectionGap),
                    )
                }
                return
            }

            // 1-5: the top of the chart as numbered rows.
            item(key = "trending-title") {
                val title = stringResource(R.string.home_section_trending)
                SectionHeader(
                    title,
                    actionLabel = stringResource(R.string.home_action_see_all),
                    actionColor = MaterialTheme.colorScheme.primary,
                    onAction = { actions.onOpenChart(title) },
                )
            }
            songs.take(TOP_ROWS).forEachIndexed { index, song ->
                item(key = "chart-${song.id}") { ChartRow(song, index, state, actions, leadingText = (index + 1).toString().padStart(2, '0')) }
            }

            // 6+: the rest of the chart as a carousel you swipe left and right.
            val more = songs.drop(TOP_ROWS)
            if (more.isNotEmpty()) {
                item(key = "speed-title") { SectionHeader(stringResource(R.string.home_section_speed)) }
                item(key = "speed") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        itemsIndexed(more, key = { _, song -> "card-${song.id}" }) { offset, song ->
                            val index = TOP_ROWS + offset
                            MusicCard(
                                title = song.title,
                                subtitle = stringResource(R.string.home_rank_card, index + 1, song.artistNames),
                                imageUrl = song.artworkUrl,
                                tint = ArtworkTint.forKey(song.id),
                                aspectRatio = 1f,
                                modifier = Modifier.width(CARD_WIDTH),
                                onClick = { actions.onChartSongClick(index) },
                            )
                        }
                    }
                }
            }
            item(key = "credit") {
                Text(
                    stringResource(R.string.home_preview_credit),
                    style = MaterialTheme.typography.labelMedium,
                    color = ButterTheme.colors.textSecondary,
                    modifier = Modifier.padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.lg),
                )
            }
        }
    }
}

@Composable
private fun ChartRow(song: Song, index: Int, state: HomeUiState, actions: HomeActions, leadingText: String) {
    val isActive = song.id == state.activeSongId
    val isPlaying = isActive && state.isActiveSongPlaying
    SongRow(
        title = song.title,
        subtitle = song.artistNames,
        artworkUrl = song.artworkUrl,
        tint = ArtworkTint.forKey(song.id),
        leadingText = leadingText,
        isActive = isActive,
        onClick = { actions.onChartSongClick(index) },
        action = RowAction(
            icon = if (isPlaying) AppIcons.Pause else AppIcons.Play,
            contentDescription = stringResource(if (isPlaying) R.string.home_pause else R.string.home_play, song.title),
            tint = MaterialTheme.colorScheme.primary,
            onClick = { actions.onChartSongClick(index) },
        ),
        // Apple's terms require a link to the song in Apple Music next to every preview.
        secondaryAction = song.externalUrl?.let { url ->
            RowAction(AppIcons.ArrowUpRight, stringResource(R.string.home_open_apple_music, song.title), onClick = { actions.onOpenUrl(url) })
        },
    )
}

/** Looks like a search field; tapping it opens Discover, where searching happens. */
@Composable
private fun SearchEntry(hint: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.home_open_search)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.fieldHeight)
            .clip(AppShapes.Field)
            .background(ButterTheme.colors.card)
            .border(1.dp, ButterTheme.colors.divider.copy(alpha = 0.6f), AppShapes.Field)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .padding(horizontal = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(AppIcons.Search), contentDescription = null, tint = ButterTheme.colors.textSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(Spacing.sm + 2.dp))
        Text(hint, style = MaterialTheme.typography.bodyMedium, color = ButterTheme.colors.textSecondary, maxLines = 1)
    }
}

private const val TOP_ROWS = 5
private val CARD_WIDTH = 156.dp

private fun TimeOfDay.eyebrowRes(): Int = when (this) {
    TimeOfDay.MORNING -> R.string.home_eyebrow_morning
    TimeOfDay.AFTERNOON -> R.string.home_eyebrow_afternoon
    TimeOfDay.EVENING -> R.string.home_eyebrow_evening
    TimeOfDay.NIGHT -> R.string.home_eyebrow_night
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0F, heightDp = 1400)
@Composable
private fun HomeScreenPreview() {
    val songs = (1..14).map { Song("itunes:$it", "Preview song $it", listOf(Artist("a$it", "Artist $it", null)), null, null, 200_000, null, null, null) }
    ButterTheme {
        HomeScreen(
            state = HomeUiState(
                firstName = "Hemant",
                timeOfDay = TimeOfDay.NIGHT,
                isOffline = false,
                chart = ChartState.Content(songs),
                recentlyPlayed = songs.take(3),
                activeSongId = songs[1].id,
                isActiveSongPlaying = true,
            ),
            contentPadding = PaddingValues(),
            actions = HomeActions(),
        )
    }
}
