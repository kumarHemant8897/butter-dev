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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourcompany.musicsocial.core.designsystem.component.Artwork
import com.yourcompany.musicsocial.core.designsystem.component.ArtworkTint
import com.yourcompany.musicsocial.core.designsystem.component.UserAvatar
import com.yourcompany.musicsocial.core.designsystem.icon.AppIcons
import com.yourcompany.musicsocial.core.designsystem.theme.AppShapes
import com.yourcompany.musicsocial.core.designsystem.theme.ButterTheme
import com.yourcompany.musicsocial.core.designsystem.theme.Dimens
import com.yourcompany.musicsocial.core.designsystem.theme.Spacing
import com.yourcompany.musicsocial.core.ui.format.formatDuration
import com.yourcompany.musicsocial.feature.player.R
import com.yourcompany.musicsocial.feature.player.presentation.state.PlayerUiState
import com.yourcompany.musicsocial.feature.player.presentation.state.RepeatMode
import com.yourcompany.musicsocial.feature.player.presentation.viewmodel.PlayerViewModel

@Composable
internal fun PlayerRoute(contentPadding: PaddingValues, viewModel: PlayerViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PlayerScreen(
        state = state,
        contentPadding = contentPadding,
        actions = PlayerActions(
            onTogglePlay = viewModel::onTogglePlay,
            onToggleLike = viewModel::onToggleLike,
            onToggleShuffle = viewModel::onToggleShuffle,
            onCycleRepeat = viewModel::onCycleRepeat,
        ),
    )
}

internal data class PlayerActions(
    val onTogglePlay: () -> Unit = {},
    val onToggleLike: () -> Unit = {},
    val onToggleShuffle: () -> Unit = {},
    val onCycleRepeat: () -> Unit = {},
)

@Composable
internal fun PlayerScreen(
    state: PlayerUiState,
    contentPadding: PaddingValues,
    actions: PlayerActions,
    modifier: Modifier = Modifier,
) {
    val secondary = ButterTheme.colors.textSecondary
    val violet = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenHorizontal),
    ) {
        // Header: collapse, source, more.
        Row(Modifier.fillMaxWidth().padding(vertical = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            PlainIcon(AppIcons.ChevronDown, stringResource(R.string.player_collapse), onClick = {})
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.player_playing_from).uppercase(), style = MaterialTheme.typography.labelSmall, color = secondary)
                Text(state.sourceLabel, style = MaterialTheme.typography.titleSmall)
            }
            PlainIcon(AppIcons.More, stringResource(R.string.player_more), onClick = {})
        }
        Spacer(Modifier.height(Spacing.lg))
        Artwork(
            imageUrl = state.song.artworkUrl,
            contentDescription = null,
            shape = AppShapes.ArtworkLarge,
            tint = ArtworkTint.forKey(state.song.id),
            modifier = Modifier.fillMaxWidth().aspectRatio(1.09f),
        )
        Spacer(Modifier.height(Spacing.xxl))

        // Title, artist, like.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(state.song.title, style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.height(Spacing.xs))
                Text(state.song.artistNames, style = MaterialTheme.typography.bodyLarge, color = secondary)
            }
            PlainIcon(
                icon = AppIcons.Favorite,
                contentDescription = stringResource(if (state.isLiked) R.string.player_unlike else R.string.player_like),
                onClick = actions.onToggleLike,
                tint = violet,
                filledBackground = state.isLiked,
            )
        }
        Spacer(Modifier.height(Spacing.xl))

        // Progress.
        ProgressBar(progress = state.progress, label = stringResource(R.string.player_progress))
        Spacer(Modifier.height(Spacing.sm))
        Row(Modifier.fillMaxWidth()) {
            Text(formatDuration(state.positionMs), style = MaterialTheme.typography.labelMedium, color = secondary)
            Spacer(Modifier.weight(1f))
            Text(formatDuration(state.song.durationMs), style = MaterialTheme.typography.labelMedium, color = secondary)
        }
        Spacer(Modifier.height(Spacing.lg))

        // Transport controls.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            ToggleIcon(AppIcons.Shuffle, stringResource(R.string.player_shuffle), state.shuffle, actions.onToggleShuffle)
            PlainIcon(AppIcons.SkipPrevious, stringResource(R.string.player_previous), onClick = {}, size = 26)
            Box(
                Modifier
                    .size(Dimens.playButtonLarge)
                    .clip(CircleShape)
                    .background(violet)
                    .clickable(role = Role.Button, onClick = actions.onTogglePlay),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(if (state.isPlaying) AppIcons.Pause else AppIcons.Play),
                    contentDescription = stringResource(if (state.isPlaying) R.string.player_pause else R.string.player_play),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp),
                )
            }
            PlainIcon(AppIcons.SkipNext, stringResource(R.string.player_next), onClick = {}, size = 26)
            ToggleIcon(AppIcons.Repeat, stringResource(R.string.player_repeat), state.repeatMode != RepeatMode.OFF, actions.onCycleRepeat)
        }
        Spacer(Modifier.height(Spacing.lg))

        // Output + share/queue.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(AppIcons.Headphones), null, tint = ButterTheme.colors.live, modifier = Modifier.size(Dimens.iconSmall))
            Spacer(Modifier.width(Spacing.sm))
            Text(stringResource(R.string.player_output), style = MaterialTheme.typography.labelMedium, color = ButterTheme.colors.live, modifier = Modifier.weight(1f))
            PlainIcon(AppIcons.Share, stringResource(R.string.player_share), onClick = {})
            PlainIcon(AppIcons.Queue, stringResource(R.string.player_queue), onClick = {})
        }
        Spacer(Modifier.height(Spacing.md))

        // Share prompt.
        Row(
            Modifier
                .fillMaxWidth()
                .clip(AppShapes.Card)
                .background(ButterTheme.colors.surfaceSecondary)
                .border(1.dp, ButterTheme.colors.divider, AppShapes.Card)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UserAvatar(name = state.shareWithName, imageUrl = null, size = Dimens.avatarSmall)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.player_share_title), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.player_share_subtitle, state.shareWithName), style = MaterialTheme.typography.labelMedium, color = secondary)
            }
            PlainIcon(AppIcons.Send, stringResource(R.string.player_send), onClick = {}, tint = violet)
        }
        Spacer(Modifier.height(Spacing.xl))
    }
}

@Composable
private fun ProgressBar(progress: Float, label: String) {
    val violet = MaterialTheme.colorScheme.primary
    Box(
        Modifier
            .fillMaxWidth()
            .height(12.dp)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.fillMaxWidth().height(2.dp).background(ButterTheme.colors.divider))
        Box(Modifier.fillMaxWidth(progress).height(2.dp).background(violet))
        Row(Modifier.fillMaxWidth(progress), horizontalArrangement = Arrangement.End) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(violet))
        }
    }
}

@Composable
private fun PlainIcon(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color? = null,
    size: Int = 22,
    filledBackground: Boolean = false,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(Dimens.minTouchTarget)) {
        Icon(
            painterResource(icon),
            contentDescription,
            tint = tint ?: MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .size(size.dp)
                .then(if (filledBackground) Modifier.clip(CircleShape).background(ButterTheme.colors.accentSoft) else Modifier),
        )
    }
}

/** Shuffle/repeat: violet with a dot underneath when on, white when off. */
@Composable
private fun ToggleIcon(@DrawableRes icon: Int, contentDescription: String, active: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .size(Dimens.minTouchTarget)
            .clip(CircleShape)
            .clickable(role = Role.Switch, onClick = onClick)
            .semantics { this.contentDescription = contentDescription + if (active) ", on" else ", off" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(3.dp))
        Box(Modifier.size(3.dp).clip(CircleShape).background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0F, heightDp = 860)
@Composable
private fun PlayerScreenPreview() {
    ButterTheme {
        PlayerScreen(PlayerViewModel().uiState.value.copy(isPlaying = true, shuffle = true), PaddingValues(), PlayerActions())
    }
}
