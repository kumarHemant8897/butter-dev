package com.yourcompany.musicsocial.core.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yourcompany.musicsocial.core.designsystem.component.Artwork
import com.yourcompany.musicsocial.core.designsystem.component.ArtworkTint
import com.yourcompany.musicsocial.core.designsystem.component.SeekBar
import com.yourcompany.musicsocial.core.designsystem.icon.AppIcons
import com.yourcompany.musicsocial.core.designsystem.theme.AppShapes
import com.yourcompany.musicsocial.core.designsystem.theme.ButterTheme
import com.yourcompany.musicsocial.core.designsystem.theme.Spacing
import com.yourcompany.musicsocial.core.ui.R

/**
 * Compact player shown above the bottom bar on every tab except Player while a song is loaded:
 * artwork, title, previous / play-pause / next, and a seek bar you can tap or drag.
 * Tapping the artwork or title opens the full player.
 */
@Composable
fun MiniPlayer(
    title: String,
    artist: String,
    artworkUrl: String?,
    tintKey: String,
    isPlaying: Boolean,
    isLoading: Boolean,
    progress: Float,
    hasNext: Boolean,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Float) -> Unit,
    onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm)
            .clip(AppShapes.Card)
            .background(ButterTheme.colors.card)
            .border(1.dp, ButterTheme.colors.divider, AppShapes.Card)
            .padding(start = Spacing.sm, end = Spacing.xs, top = Spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier
                    .weight(1f)
                    .clip(AppShapes.Artwork)
                    .clickable(role = Role.Button, onClickLabel = stringResource(R.string.mini_player_open), onClick = onOpenPlayer),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Artwork(artworkUrl, null, AppShapes.Artwork, ArtworkTint.forKey(tintKey), Modifier.size(42.dp), fallbackIcon = null)
                Spacer(Modifier.width(Spacing.md))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        artist,
                        style = MaterialTheme.typography.labelMedium,
                        color = ButterTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            ControlButton(AppIcons.SkipPrevious, stringResource(R.string.mini_player_previous), onPrevious)
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                } else {
                    ControlButton(
                        icon = if (isPlaying) AppIcons.Pause else AppIcons.Play,
                        contentDescription = stringResource(if (isPlaying) R.string.mini_player_pause else R.string.mini_player_play),
                        onClick = onTogglePlay,
                        tint = MaterialTheme.colorScheme.primary,
                        iconSize = 26,
                    )
                }
            }
            ControlButton(AppIcons.SkipNext, stringResource(R.string.mini_player_next), onNext, enabled = hasNext)
        }
        SeekBar(
            progress = progress,
            onSeek = onSeek,
            contentDescription = stringResource(R.string.mini_player_position),
            enabled = !isLoading,
            touchHeight = 20.dp,
            lineWidth = 2.5.dp,
            thumbRadius = 4.5.dp,
            modifier = Modifier.padding(end = Spacing.xs),
        )
    }
}

@Composable
private fun ControlButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: Color? = null,
    iconSize: Int = 22,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(44.dp)) {
        Icon(
            painterResource(icon),
            contentDescription = contentDescription,
            tint = when {
                !enabled -> ButterTheme.colors.textSecondary.copy(alpha = 0.4f)
                tint != null -> tint
                else -> MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.size(iconSize.dp),
        )
    }
}
