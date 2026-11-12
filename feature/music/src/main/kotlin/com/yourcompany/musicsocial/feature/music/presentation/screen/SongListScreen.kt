package com.yourcompany.musicsocial.feature.music.presentation.screen

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import com.yourcompany.musicsocial.core.ui.error.toErrorCopy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourcompany.musicsocial.core.designsystem.component.Artwork
import com.yourcompany.musicsocial.core.designsystem.component.ArtworkTint
import com.yourcompany.musicsocial.core.designsystem.component.CircleIconButton
import com.yourcompany.musicsocial.core.designsystem.component.LoadingIndicator
import com.yourcompany.musicsocial.core.designsystem.component.PrimaryButton
import com.yourcompany.musicsocial.core.designsystem.component.RowAction
import com.yourcompany.musicsocial.core.designsystem.component.SecondaryButton
import com.yourcompany.musicsocial.core.designsystem.component.SongRow
import com.yourcompany.musicsocial.core.designsystem.icon.AppIcons
import com.yourcompany.musicsocial.core.designsystem.theme.AppShapes
import com.yourcompany.musicsocial.core.designsystem.theme.ButterTheme
import com.yourcompany.musicsocial.core.designsystem.theme.Spacing
import com.yourcompany.musicsocial.core.domain.model.Artist
import com.yourcompany.musicsocial.core.domain.model.Song
import com.yourcompany.musicsocial.core.ui.error.AppErrorView
import com.yourcompany.musicsocial.feature.music.R
import com.yourcompany.musicsocial.feature.music.presentation.state.SongListContent
import com.yourcompany.musicsocial.feature.music.presentation.state.SongListType
import com.yourcompany.musicsocial.feature.music.presentation.state.SongListUiState
import com.yourcompany.musicsocial.feature.music.presentation.viewmodel.SongListViewModel

@Composable
internal fun SongListRoute(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    viewModel: SongListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    LaunchedEffect(state.isDeleted) { if (state.isDeleted) onBack() }
    state.pendingRemoval?.let { song ->
        ConfirmDialog(
            title = stringResource(R.string.music_remove_title),
            message = stringResource(R.string.music_remove_message, song.title),
            confirmLabel = stringResource(R.string.music_remove),
            onConfirm = viewModel::onConfirmRemove,
            onDismiss = viewModel::onDismissRemove,
        )
    }
    if (state.isConfirmingDelete) {
        ConfirmDialog(
            title = stringResource(R.string.music_delete_title),
            message = stringResource(R.string.music_delete_message, state.title),
            confirmLabel = stringResource(R.string.music_delete),
            onConfirm = viewModel::onConfirmDelete,
            onDismiss = viewModel::onDismissDelete,
        )
    }
    SongListScreen(
        state = state,
        contentPadding = contentPadding,
        actions = SongListActions(
            onBack = onBack,
            onSongClick = viewModel::onSongClick,
            onPlayAll = viewModel::onPlayAll,
            onShuffle = viewModel::onShuffle,
            onRetry = viewModel::onRetry,
            onOpenUrl = uriHandler::openUri,
            onRemoveSong = viewModel::onRequestRemove,
            onDeletePlaylist = viewModel::onRequestDelete,
        ),
    )
}

@Composable
private fun ConfirmDialog(title: String, message: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel, color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.music_cancel)) } },
    )
}

internal data class SongListActions(
    val onBack: () -> Unit = {},
    val onSongClick: (Int) -> Unit = {},
    val onPlayAll: () -> Unit = {},
    val onShuffle: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onOpenUrl: (String) -> Unit = {},
    val onRemoveSong: (Int) -> Unit = {},
    val onDeletePlaylist: () -> Unit = {},
)

@Composable
internal fun SongListScreen(
    state: SongListUiState,
    contentPadding: PaddingValues,
    actions: SongListActions,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = contentPadding) {
        item(key = "back") {
            Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.sm)) {
                CircleIconButton(icon = AppIcons.Back, contentDescription = stringResource(R.string.music_back), onClick = actions.onBack)
            }
        }
        item(key = "header") { Header(state, actions) }
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

        when (val content = state.content) {
            SongListContent.Loading -> item(key = "loading") {
                Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) { LoadingIndicator() }
            }
            is SongListContent.Error -> item(key = "error") {
                AppErrorView(error = content.error, onRetry = actions.onRetry, modifier = Modifier.fillMaxWidth().height(320.dp))
            }
            is SongListContent.Songs -> {
                if (content.songs.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            stringResource(if (state.type == SongListType.PLAYLIST) R.string.music_playlist_empty else R.string.music_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = ButterTheme.colors.textSecondary,
                            modifier = Modifier.padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.xl),
                        )
                    }
                }
                itemsIndexed(content.songs, key = { _, song -> song.id }) { index, song ->
                    SongListRow(song, index, state, actions)
                }
                item(key = "credit") {
                    Text(
                        stringResource(R.string.music_preview_credit),
                        style = MaterialTheme.typography.labelMedium,
                        color = ButterTheme.colors.textSecondary,
                        modifier = Modifier.padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.lg),
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(state: SongListUiState, actions: SongListActions) {
    val eyebrow = stringResource(
        when (state.type) {
            SongListType.CHART -> R.string.music_eyebrow_chart
            SongListType.GENRE -> R.string.music_eyebrow_genre
            SongListType.ARTIST -> R.string.music_eyebrow_artist
            SongListType.PLAYLIST -> if (state.isPublic) R.string.music_eyebrow_playlist else R.string.music_eyebrow_playlist_private
        },
    )
    val songCount = state.songs.size
    Column(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.screenHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Artwork(
            imageUrl = state.artworkUrl,
            contentDescription = null,
            shape = if (state.type == SongListType.ARTIST) androidx.compose.foundation.shape.CircleShape else AppShapes.Card,
            tint = ArtworkTint.forKey(state.title),
            modifier = Modifier.size(HEADER_ART),
        )
        Spacer(Modifier.height(Spacing.lg))
        Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(Spacing.xs))
        Text(
            state.title,
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { heading() },
        )
        val details = listOfNotNull(
            state.subtitle,
            if (songCount > 0) androidx.compose.ui.res.pluralStringResource(R.plurals.music_song_count, songCount, songCount) else null,
        ).joinToString(" · ")
        if (details.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.xs))
            Text(details, style = MaterialTheme.typography.labelMedium, color = ButterTheme.colors.textSecondary)
        }
        if (songCount > 0) {
            Spacer(Modifier.height(Spacing.lg))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                PrimaryButton(stringResource(R.string.music_play_all), onClick = actions.onPlayAll, leadingIcon = AppIcons.Play)
                SecondaryButton(stringResource(R.string.music_shuffle), onClick = actions.onShuffle, leadingIcon = AppIcons.Shuffle)
            }
        }
        if (state.isOwner) {
            TextButton(onClick = actions.onDeletePlaylist) {
                Text(stringResource(R.string.music_delete_playlist), color = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

@Composable
private fun SongListRow(song: Song, index: Int, state: SongListUiState, actions: SongListActions) {
    val isActive = song.id == state.activeSongId
    val isPlaying = isActive && state.isActiveSongPlaying
    SongRow(
        title = song.title,
        // On an artist page every row is the same artist; the album says more.
        subtitle = if (state.type == SongListType.ARTIST) song.albumTitle ?: song.artistNames else song.artistNames,
        artworkUrl = song.artworkUrl,
        tint = ArtworkTint.forKey(song.id),
        leadingText = (index + 1).toString().padStart(2, '0'),
        isActive = isActive,
        onClick = { actions.onSongClick(index) },
        // On your own playlist the row button removes the song; tapping the row still plays it.
        action = if (state.isOwner) {
            RowAction(
                icon = AppIcons.More,
                contentDescription = stringResource(R.string.music_remove_from, song.title),
                onClick = { actions.onRemoveSong(index) },
            )
        } else {
            RowAction(
                icon = if (isPlaying) AppIcons.Pause else AppIcons.Play,
                contentDescription = stringResource(if (isPlaying) R.string.music_pause else R.string.music_play, song.title),
                tint = MaterialTheme.colorScheme.primary,
                onClick = { actions.onSongClick(index) },
            )
        },
        // Apple's terms require a link to the song in Apple Music next to every preview.
        secondaryAction = song.externalUrl?.let { url ->
            RowAction(AppIcons.ArrowUpRight, stringResource(R.string.music_open_apple_music, song.title), onClick = { actions.onOpenUrl(url) })
        },
    )
}

private val HEADER_ART = 168.dp

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0F, heightDp = 1000)
@Composable
private fun SongListScreenPreview() {
    val songs = (1..8).map { Song("itunes:$it", "Preview song $it", listOf(Artist("a", "Arijit Singh", null)), "Album $it", null, 200_000, null, null, null) }
    ButterTheme {
        SongListScreen(
            state = SongListUiState(
                type = SongListType.ARTIST,
                title = "Arijit Singh",
                subtitle = "Bollywood",
                content = SongListContent.Songs(songs),
                activeSongId = songs[1].id,
                isActiveSongPlaying = true,
            ),
            contentPadding = PaddingValues(),
            actions = SongListActions(),
        )
    }
}
