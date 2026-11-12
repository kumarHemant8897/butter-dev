package com.yourcompany.musicsocial.feature.player.presentation.screen

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourcompany.musicsocial.core.designsystem.component.Artwork
import com.yourcompany.musicsocial.core.designsystem.component.ArtworkTint
import com.yourcompany.musicsocial.core.designsystem.component.ButtonSize
import com.yourcompany.musicsocial.core.designsystem.component.EmptyState
import com.yourcompany.musicsocial.core.designsystem.component.SecondaryButton
import com.yourcompany.musicsocial.core.designsystem.component.SeekBar
import com.yourcompany.musicsocial.core.designsystem.icon.AppIcons
import com.yourcompany.musicsocial.core.designsystem.theme.AppShapes
import com.yourcompany.musicsocial.core.designsystem.theme.ButterTheme
import com.yourcompany.musicsocial.core.designsystem.theme.Dimens
import com.yourcompany.musicsocial.core.designsystem.theme.Spacing
import androidx.compose.ui.semantics.stateDescription
import com.yourcompany.musicsocial.core.domain.model.LibraryKind
import com.yourcompany.musicsocial.core.domain.model.Song
import com.yourcompany.musicsocial.core.domain.result.AppError
import androidx.compose.runtime.LaunchedEffect
import com.yourcompany.musicsocial.core.ui.component.AddToPlaylistSheet
import com.yourcompany.musicsocial.core.ui.component.CreatePlaylistDialog
import com.yourcompany.musicsocial.core.domain.playback.PlaybackState
import com.yourcompany.musicsocial.core.domain.playback.RepeatMode
import com.yourcompany.musicsocial.core.ui.error.toErrorCopy
import com.yourcompany.musicsocial.core.ui.format.formatDuration
import com.yourcompany.musicsocial.feature.player.R
import com.yourcompany.musicsocial.feature.player.presentation.viewmodel.PlayerViewModel

@Composable
internal fun PlayerRoute(contentPadding: PaddingValues, viewModel: PlayerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val librarySongIds by viewModel.librarySongIds.collectAsStateWithLifecycle()
    val libraryError by viewModel.libraryError.collectAsStateWithLifecycle()
    val playlistUi by viewModel.playlistUi.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val songId = state.song?.id

    if (playlistUi.isSheetOpen) {
        AddToPlaylistSheet(
            playlists = playlistUi.playlists,
            onPick = viewModel::onPickPlaylist,
            onNewPlaylist = viewModel::onNewPlaylistFromSheet,
            onDismiss = viewModel::onDismissPlaylistSheet,
        )
    }
    if (playlistUi.isCreateOpen) {
        CreatePlaylistDialog(
            onCreate = viewModel::onCreatePlaylist,
            onDismiss = viewModel::onDismissCreatePlaylist,
            creating = playlistUi.isCreating,
        )
    }
    // The confirmation is a short note, not a permanent line.
    LaunchedEffect(playlistUi.addedTo) {
        if (playlistUi.addedTo != null) {
            kotlinx.coroutines.delay(ADDED_NOTE_MS)
            viewModel.onPlaylistMessageShown()
        }
    }

    PlayerScreen(
        state = state,
        contentPadding = contentPadding,
        library = PlayerLibraryState(
            isLiked = songId != null && songId in librarySongIds[LibraryKind.LIKED].orEmpty(),
            isSaved = songId != null && songId in librarySongIds[LibraryKind.SAVED].orEmpty(),
            error = libraryError ?: playlistUi.error,
            addedTo = playlistUi.addedTo,
        ),
        actions = PlayerActions(
            onToggleLike = viewModel::onToggleLike,
            onToggleSave = viewModel::onToggleSave,
            onAddToPlaylist = viewModel::onOpenAddToPlaylist,
            onTogglePlay = viewModel::onTogglePlay,
            onNext = viewModel::onNext,
            onPrevious = viewModel::onPrevious,
            onToggleShuffle = viewModel::onToggleShuffle,
            onCycleRepeat = viewModel::onCycleRepeat,
            onSeek = viewModel::onSeek,
            onOpenUrl = uriHandler::openUri,
        ),
    )
}

private const val ADDED_NOTE_MS = 2_500L

internal data class PlayerActions(
    val onTogglePlay: () -> Unit = {},
    val onNext: () -> Unit = {},
    val onPrevious: () -> Unit = {},
    val onToggleShuffle: () -> Unit = {},
    val onCycleRepeat: () -> Unit = {},
    val onSeek: (Float) -> Unit = {},
    val onOpenUrl: (String) -> Unit = {},
    val onToggleLike: () -> Unit = {},
    val onToggleSave: () -> Unit = {},
    val onAddToPlaylist: () -> Unit = {},
)

/** Whether the loaded song is in the user's liked and saved lists. */
internal data class PlayerLibraryState(
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val error: AppError? = null,
    /** Title of the playlist the song was just added to. */
    val addedTo: String? = null,
)

@Composable
internal fun PlayerScreen(
    state: PlaybackState,
    contentPadding: PaddingValues,
    actions: PlayerActions,
    modifier: Modifier = Modifier,
    library: PlayerLibraryState = PlayerLibraryState(),
) {
    val song = state.song
    if (song == null) {
        EmptyState(
            icon = AppIcons.Player,
            title = stringResource(R.string.player_idle_title),
            message = stringResource(R.string.player_idle_message),
            modifier = modifier.padding(contentPadding),
        )
        return
    }

    val secondary = ButterTheme.colors.textSecondary
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenHorizontal),
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = Spacing.lg), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.player_eyebrow).uppercase(), style = MaterialTheme.typography.labelSmall, color = secondary)
            song.albumTitle?.let {
                Text(it, style = MaterialTheme.typography.titleSmall, maxLines = 1, textAlign = TextAlign.Center)
            }
        }
        Artwork(
            imageUrl = song.artworkUrl,
            contentDescription = null,
            shape = AppShapes.ArtworkLarge,
            tint = ArtworkTint.forKey(song.id),
            modifier = Modifier.fillMaxWidth().aspectRatio(1.09f),
        )
        Spacer(Modifier.height(Spacing.xxl))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(song.title, style = MaterialTheme.typography.displaySmall, maxLines = 2)
                Spacer(Modifier.height(Spacing.xs))
                Text(song.artistNames, style = MaterialTheme.typography.bodyLarge, color = secondary, maxLines = 2)
            }
            IconButton(onClick = actions.onAddToPlaylist, modifier = Modifier.size(Dimens.minTouchTarget)) {
                Icon(
                    painterResource(AppIcons.Queue),
                    contentDescription = stringResource(R.string.player_add_to_playlist),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp),
                )
            }
            LibraryButton(
                icon = AppIcons.Plus,
                label = stringResource(if (library.isSaved) R.string.player_unsave else R.string.player_save),
                active = library.isSaved,
                onClick = actions.onToggleSave,
            )
            LibraryButton(
                icon = AppIcons.Favorite,
                label = stringResource(if (library.isLiked) R.string.player_unlike else R.string.player_like),
                active = library.isLiked,
                onClick = actions.onToggleLike,
            )
        }
        library.error?.let { error ->
            Text(
                stringResource(error.toErrorCopy().message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        library.addedTo?.let { title ->
            Text(
                stringResource(com.yourcompany.musicsocial.core.ui.R.string.playlist_added, title),
                style = MaterialTheme.typography.bodySmall,
                color = ButterTheme.colors.live,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        Spacer(Modifier.height(Spacing.xl))

        SeekBar(
            progress = state.progress,
            onSeek = actions.onSeek,
            contentDescription = stringResource(R.string.player_progress),
            enabled = !state.isLoading,
        )
        Spacer(Modifier.height(Spacing.sm))
        Row(Modifier.fillMaxWidth()) {
            Text(formatDuration(state.positionMs), style = MaterialTheme.typography.labelMedium, color = secondary)
            Spacer(Modifier.weight(1f))
            Text(formatDuration(state.durationMs), style = MaterialTheme.typography.labelMedium, color = secondary)
        }
        Spacer(Modifier.height(Spacing.lg))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            ToggleIcon(AppIcons.Shuffle, stringResource(R.string.player_shuffle), state.shuffle, actions.onToggleShuffle)
            ControlIcon(AppIcons.SkipPrevious, stringResource(R.string.player_previous), actions.onPrevious)
            PlayPauseButton(state, actions.onTogglePlay)
            ControlIcon(AppIcons.SkipNext, stringResource(R.string.player_next), actions.onNext, enabled = state.hasNext)
            ToggleIcon(AppIcons.Repeat, stringResource(R.string.player_repeat), state.repeatMode != RepeatMode.OFF, actions.onCycleRepeat, badge = if (state.repeatMode == RepeatMode.ONE) "1" else null)
        }
        Spacer(Modifier.height(Spacing.lg))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(AppIcons.Headphones), null, tint = ButterTheme.colors.live, modifier = Modifier.size(Dimens.iconSmall))
            Spacer(Modifier.width(Spacing.sm))
            Text(stringResource(R.string.player_preview_notice), style = MaterialTheme.typography.labelMedium, color = ButterTheme.colors.live)
        }

        state.error?.let { error ->
            Spacer(Modifier.height(Spacing.md))
            Text(
                stringResource(error.toErrorCopy().message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }

        Spacer(Modifier.height(Spacing.lg))
        FullSongCard(song, actions.onOpenUrl)
        Spacer(Modifier.height(Spacing.xl))
    }
}

/** Previews are for promoting the song: a link to it in Apple Music and the provider credit sit with the player. */
@Composable
private fun FullSongCard(song: Song, onOpenUrl: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(AppShapes.Card)
            .background(ButterTheme.colors.surfaceSecondary)
            .border(1.dp, ButterTheme.colors.divider, AppShapes.Card)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(stringResource(R.string.player_full_song_title), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(R.string.player_credit),
            style = MaterialTheme.typography.labelMedium,
            color = ButterTheme.colors.textSecondary,
        )
        song.externalUrl?.let { url ->
            SecondaryButton(
                text = stringResource(R.string.player_open_apple_music),
                onClick = { onOpenUrl(url) },
                leadingIcon = AppIcons.ArrowUpRight,
                size = ButtonSize.Compact,
            )
        }
    }
}

@Composable
private fun PlayPauseButton(state: PlaybackState, onClick: () -> Unit) {
    val description = stringResource(
        when {
            state.isLoading -> R.string.player_loading
            state.isPlaying -> R.string.player_pause
            else -> R.string.player_play
        },
    )
    Box(
        Modifier
            .size(Dimens.playButtonLarge)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(enabled = !state.isLoading, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(Modifier.size(26.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.5.dp)
        } else {
            Icon(
                painterResource(if (state.isPlaying) AppIcons.Pause else AppIcons.Play),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

/** Heart / save toggle: violet when the song is in the list. [label] describes what a tap does. */
@Composable
private fun LibraryButton(@DrawableRes icon: Int, label: String, active: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(Dimens.minTouchTarget).semantics { stateDescription = if (active) "on" else "off" }) {
        Icon(
            painterResource(icon),
            contentDescription = label,
            tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun ControlIcon(@DrawableRes icon: Int, contentDescription: String, onClick: () -> Unit, enabled: Boolean = true) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(Dimens.minTouchTarget)) {
        Icon(
            painterResource(icon),
            contentDescription,
            tint = if (enabled) MaterialTheme.colorScheme.onSurface else ButterTheme.colors.textSecondary.copy(alpha = 0.4f),
            modifier = Modifier.size(26.dp),
        )
    }
}

/** Shuffle/repeat: violet with a dot underneath when on, white when off. [badge] is a small label such as "1". */
@Composable
private fun ToggleIcon(@DrawableRes icon: Int, contentDescription: String, active: Boolean, onClick: () -> Unit, badge: String? = null) {
    val tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Column(
        Modifier
            .size(Dimens.minTouchTarget)
            .clip(CircleShape)
            .clickable(role = Role.Switch, onClick = onClick)
            .semantics { this.contentDescription = contentDescription + if (active) ", on" else ", off" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            if (badge != null) Text(badge, style = MaterialTheme.typography.labelSmall, color = tint)
        }
        Spacer(Modifier.height(3.dp))
        Box(Modifier.size(3.dp).clip(CircleShape).background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0F, heightDp = 900)
@Composable
private fun PlayerScreenPreview() {
    ButterTheme {
        PlayerScreen(
            state = PlaybackState(
                song = Song("itunes:1", "Sample Song", listOf(), "Sample Album", null, 200_000, null, "https://music.apple.com/in/album/x", null),
                status = PlaybackState.Status.PLAYING,
                positionMs = 12_000,
                durationMs = 30_000,
                hasNext = true,
            ),
            contentPadding = PaddingValues(),
            actions = PlayerActions(),
        )
    }
}
