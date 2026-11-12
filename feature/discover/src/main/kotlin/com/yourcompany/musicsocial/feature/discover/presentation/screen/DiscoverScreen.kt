package com.yourcompany.musicsocial.feature.discover.presentation.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.platform.LocalUriHandler
import com.yourcompany.musicsocial.core.designsystem.component.LoadingIndicator
import com.yourcompany.musicsocial.feature.discover.presentation.state.SearchState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
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
import com.yourcompany.musicsocial.core.ui.component.NoticeCard
import com.yourcompany.musicsocial.core.ui.error.toErrorCopy
import com.yourcompany.musicsocial.feature.discover.presentation.state.PersonDetail
import com.yourcompany.musicsocial.core.sample.SampleData
import com.yourcompany.musicsocial.feature.discover.presentation.state.DiscoverPerson
import com.yourcompany.musicsocial.feature.discover.presentation.state.CommunityChart
import com.yourcompany.musicsocial.core.domain.model.ArtistProfile
import com.yourcompany.musicsocial.core.domain.model.ChartGenre
import com.yourcompany.musicsocial.core.domain.model.FollowStatus
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.yourcompany.musicsocial.core.navigation.SongListKind
import com.yourcompany.musicsocial.core.navigation.SongListRoute
import com.yourcompany.musicsocial.feature.discover.R
import com.yourcompany.musicsocial.feature.discover.presentation.components.ArtistChip
import com.yourcompany.musicsocial.feature.discover.presentation.components.FeaturedPlaylistCard
import com.yourcompany.musicsocial.feature.discover.presentation.state.DiscoverUiState
import com.yourcompany.musicsocial.feature.discover.presentation.viewmodel.DiscoverViewModel

@Composable
internal fun DiscoverRoute(
    contentPadding: PaddingValues,
    onOpenSongList: (SongListRoute) -> Unit,
    onOpenProfile: (String) -> Unit,
    onFindPeople: () -> Unit,
    viewModel: DiscoverViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    // Back from a profile: a request may have been sent or cancelled there.
    var isFirstResume by rememberSaveable { mutableStateOf(true) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (isFirstResume) isFirstResume = false else viewModel.refreshPeople()
    }
    DiscoverScreen(
        state = state,
        contentPadding = contentPadding,
        onQueryChange = viewModel::onQueryChange,
        onToggleFollow = viewModel::onToggleFollow,
        onRetryPeople = viewModel::onRetryPeople,
        onSelectFeeling = viewModel::onSelectFeeling,
        onSongClick = viewModel::onSongClick,
        onTopSongClick = viewModel::onTopSongClick,
        onRetrySearch = viewModel::onRetrySearch,
        onOpenUrl = uriHandler::openUri,
        onOpenSongList = onOpenSongList,
        onOpenProfile = onOpenProfile,
        onFindPeople = onFindPeople,
    )
}

@Composable
internal fun DiscoverScreen(
    state: DiscoverUiState,
    contentPadding: PaddingValues,
    onQueryChange: (String) -> Unit,
    onToggleFollow: (String) -> Unit,
    onRetryPeople: () -> Unit,
    onSelectFeeling: (String) -> Unit,
    onSongClick: (Int) -> Unit,
    onTopSongClick: (Int) -> Unit,
    onRetrySearch: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onOpenSongList: (SongListRoute) -> Unit,
    modifier: Modifier = Modifier,
    onOpenProfile: (String) -> Unit = {},
    onFindPeople: () -> Unit = {},
) {
    // Read here: the LazyColumn builder below is not composable.
    val chartTitle = stringResource(R.string.discover_chart_title)
    val communityTitle = stringResource(R.string.discover_community_title)
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

        if (state.search !is SearchState.Idle) {
            searchSection(state, onSongClick, onRetrySearch, onOpenUrl)
            return@LazyColumn
        }

        if (state.topSongs.isNotEmpty()) {
            item(key = "top-title") {
                SectionHeader(
                    stringResource(R.string.discover_section_repeat),
                    actionLabel = stringResource(R.string.discover_action_see_all),
                    actionColor = MaterialTheme.colorScheme.primary,
                    onAction = { onOpenSongList(SongListRoute(kind = SongListKind.CHART, title = chartTitle)) },
                )
            }
            itemsIndexed(state.topSongs, key = { _, song -> "top-${song.id}" }) { index, song ->
                val isActive = song.id == state.activeSongId
                val isPlaying = isActive && state.isActiveSongPlaying
                SongRow(
                    title = song.title,
                    subtitle = song.artistNames,
                    artworkUrl = song.artworkUrl,
                    tint = ArtworkTint.forKey(song.id),
                    leadingText = (index + 1).toString().padStart(2, '0'),
                    isActive = isActive,
                    onClick = { onTopSongClick(index) },
                    action = RowAction(
                        icon = if (isPlaying) AppIcons.Pause else AppIcons.Play,
                        contentDescription = stringResource(if (isPlaying) R.string.discover_pause_song else R.string.discover_play_preview, song.title),
                        tint = MaterialTheme.colorScheme.primary,
                        onClick = { onTopSongClick(index) },
                    ),
                    secondaryAction = song.externalUrl?.let { url ->
                        RowAction(AppIcons.ArrowUpRight, stringResource(R.string.discover_open_apple_music, song.title), onClick = { onOpenUrl(url) })
                    },
                )
            }
        }
        state.communityChart?.let { community ->
            val title = communityTitle
            val open = { onOpenSongList(SongListRoute(kind = SongListKind.GENRE, title = title, id = community.genre.id)) }
            item(key = "longer-title") {
                SectionHeader(
                    stringResource(R.string.discover_section_longer),
                    actionLabel = stringResource(R.string.discover_action_see_all),
                    actionColor = MaterialTheme.colorScheme.primary,
                    onAction = open,
                )
            }
            item(key = "longer") {
                FeaturedPlaylistCard(
                    eyebrow = stringResource(R.string.discover_community_favorite),
                    title = title,
                    stats = stringResource(R.string.discover_community_stats, community.songs.size, community.songs.first().artists.firstOrNull()?.name.orEmpty()),
                    tintKey = community.genre.id,
                    imageUrl = community.artworkUrl,
                    onClick = open,
                    modifier = Modifier.padding(horizontal = Spacing.screenHorizontal),
                )
            }
        }

        if (state.artists.isNotEmpty()) {
            item(key = "voices-title") { SectionHeader(stringResource(R.string.discover_section_voices)) }
            item(key = "voices") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    items(state.artists, key = { "artist-${it.id}" }) { artist ->
                        ArtistChip(
                            name = artist.name,
                            genre = artist.genre,
                            imageUrl = artist.imageUrl,
                            onClickLabel = stringResource(R.string.discover_open_artist, artist.name),
                            onClick = { onOpenSongList(SongListRoute(kind = SongListKind.ARTIST, title = artist.name, id = artist.id)) },
                        )
                    }
                }
            }
        }

        item(key = "listeners-title") {
            SectionHeader(
                stringResource(if (state.arePeopleSample) R.string.discover_section_listeners else R.string.discover_section_people),
                actionLabel = stringResource(R.string.discover_action_see_all),
                actionColor = MaterialTheme.colorScheme.primary,
                onAction = if (state.arePeopleSample) null else onFindPeople,
            )
        }
        if (state.peopleError != null) {
            item(key = "people-error") {
                Column(Modifier.padding(horizontal = Spacing.screenHorizontal), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(
                        stringResource(state.peopleError.toErrorCopy().message),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    SecondaryButton(stringResource(R.string.discover_retry), onClick = onRetryPeople, size = ButtonSize.Compact)
                }
            }
        } else if (state.people.isEmpty() && !state.isLoadingPeople) {
            item(key = "people-empty") {
                NoticeCard(
                    icon = AppIcons.Profile,
                    title = stringResource(R.string.discover_people_empty_title),
                    message = stringResource(R.string.discover_people_empty_message),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = Spacing.screenHorizontal),
                )
            }
        }
        items(state.people, key = { "person-${it.id}" }) { person ->
            PersonRow(
                name = person.name,
                subtitle = when (val detail = person.detail) {
                    is PersonDetail.TasteMatch -> stringResource(R.string.discover_listener_match, detail.percent, detail.sharedArtists)
                    is PersonDetail.Handle -> pluralStringResource(R.plurals.discover_person_followers, detail.followerCount, detail.username, formatCompactCount(detail.followerCount))
                },
                avatarUrl = person.avatarUrl,
                // Sample people have no profile to open.
                onClick = if (state.arePeopleSample) null else ({ onOpenProfile(person.id) }),
            ) {
                when (person.status) {
                    FollowStatus.NONE ->
                        PrimaryButton(stringResource(R.string.discover_follow), onClick = { onToggleFollow(person.id) }, size = ButtonSize.Compact)
                    FollowStatus.REQUESTED ->
                        SecondaryButton(stringResource(R.string.discover_requested), onClick = { onToggleFollow(person.id) }, size = ButtonSize.Compact)
                    // Unfollowing happens on the profile, one deliberate step away.
                    FollowStatus.FOLLOWING ->
                        SecondaryButton(stringResource(R.string.discover_following), onClick = { onOpenProfile(person.id) }, size = ButtonSize.Compact)
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
        DiscoverScreen(previewState(), PaddingValues(), {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}

private fun previewState() = DiscoverUiState(
    topSongs = SampleData.trending.map { it.song },
    communityChart = CommunityChart(ChartGenre.BOLLYWOOD, SampleData.trending.map { it.song }),
    artists = SampleData.artists.map { ArtistProfile(it.id, it.name, SampleData.artistGenres[it.id], it.imageUrl) },
    people = SampleData.listenerMatches.map {
        DiscoverPerson(
            it.person.id,
            it.person.displayName,
            null,
            PersonDetail.TasteMatch(it.tasteMatchPercent, it.sharedArtists),
            status = if (it.tasteMatchPercent < 92) FollowStatus.FOLLOWING else FollowStatus.NONE,
        )
    },
    arePeopleSample = true,
    feelings = SampleData.feelings,
    selectedFeeling = SampleData.feelings.first(),
)
/** Replaces the browse sections while the user is searching. */
private fun LazyListScope.searchSection(
    state: DiscoverUiState,
    onSongClick: (Int) -> Unit,
    onRetrySearch: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    when (val search = state.search) {
        SearchState.Idle -> Unit
        SearchState.Loading -> item(key = "search-loading") {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { LoadingIndicator() }
        }
        SearchState.Empty -> item(key = "search-empty") {
            NoticeCard(
                icon = AppIcons.Search,
                title = stringResource(R.string.discover_search_empty_title),
                message = stringResource(R.string.discover_search_empty_message, state.query.trim()),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = Spacing.screenHorizontal, end = Spacing.screenHorizontal, top = Spacing.xl),
            )
        }
        is SearchState.Error -> item(key = "search-error") {
            Column(
                Modifier.padding(start = Spacing.screenHorizontal, end = Spacing.screenHorizontal, top = Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text(
                    stringResource(search.error.toErrorCopy().message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                SecondaryButton(stringResource(R.string.discover_retry), onClick = onRetrySearch, size = ButtonSize.Compact)
            }
        }
        is SearchState.Results -> {
            item(key = "search-title") { SectionHeader(stringResource(R.string.discover_search_results)) }
            itemsIndexed(search.songs, key = { _, song -> "result-${song.id}" }) { index, song ->
                val isActive = song.id == state.activeSongId
                SongRow(
                    title = song.title,
                    subtitle = song.artistNames,
                    artworkUrl = song.artworkUrl,
                    tint = ArtworkTint.forKey(song.id),
                    isActive = isActive,
                    onClick = { onSongClick(index) },
                    action = RowAction(
                        icon = if (isActive && state.isActiveSongPlaying) AppIcons.Pause else AppIcons.Play,
                        contentDescription = stringResource(
                            if (isActive && state.isActiveSongPlaying) R.string.discover_pause_song else R.string.discover_play_preview,
                            song.title,
                        ),
                        tint = MaterialTheme.colorScheme.primary,
                        onClick = { onSongClick(index) },
                    ),
                    // Apple's terms require a link to the song in Apple Music next to every preview.
                    secondaryAction = song.externalUrl?.let { url ->
                        RowAction(
                            icon = AppIcons.ArrowUpRight,
                            contentDescription = stringResource(R.string.discover_open_apple_music, song.title),
                            onClick = { onOpenUrl(url) },
                        )
                    },
                )
            }
            item(key = "search-credit") {
                Text(
                    stringResource(R.string.discover_preview_credit),
                    style = MaterialTheme.typography.labelMedium,
                    color = ButterTheme.colors.textSecondary,
                    modifier = Modifier.padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.lg),
                )
            }
        }
    }
}
