package com.yourcompany.musicsocial.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yourcompany.musicsocial.core.designsystem.theme.AppShapes
import com.yourcompany.musicsocial.core.designsystem.theme.ButterTheme
import com.yourcompany.musicsocial.core.designsystem.theme.Dimens
import com.yourcompany.musicsocial.core.designsystem.theme.Spacing

/** Optional trailing icon action on a row (more, like, open, play). */
data class RowAction(
    @DrawableRes val icon: Int,
    val contentDescription: String,
    val tint: Color? = null,
    val onClick: () -> Unit,
)

/**
 * "Music, not tiles": an open row where artwork, title and artist carry the hierarchy.
 * [leadingText] shows a chart position ("01"). [isActive] highlights the song in the player.
 */
@Composable
fun SongRow(
    title: String,
    subtitle: String,
    artworkUrl: String?,
    tint: ArtworkTint,
    modifier: Modifier = Modifier,
    leadingText: String? = null,
    isActive: Boolean = false,
    artworkShape: androidx.compose.ui.graphics.Shape = AppShapes.Artwork,
    action: RowAction? = null,
    /** Shown before [action]; used for a quiet extra such as "Open in Apple Music". */
    secondaryAction: RowAction? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = Spacing.screenHorizontal, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingText != null) {
            Text(
                leadingText,
                style = MaterialTheme.typography.bodyMedium,
                color = ButterTheme.colors.textSecondary,
                modifier = Modifier.widthIn(min = 24.dp),
            )
            Spacer(Modifier.width(Spacing.xs))
        }
        Artwork(artworkUrl, null, artworkShape, tint, Modifier.size(Dimens.artworkRow), fallbackIcon = null)
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = ButterTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (secondaryAction != null) {
            IconButton(onClick = secondaryAction.onClick, modifier = Modifier.size(Dimens.minTouchTarget)) {
                Icon(
                    painterResource(secondaryAction.icon),
                    contentDescription = secondaryAction.contentDescription,
                    tint = secondaryAction.tint ?: ButterTheme.colors.textSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        if (action != null) {
            IconButton(onClick = action.onClick, modifier = Modifier.size(Dimens.minTouchTarget)) {
                Icon(
                    painterResource(action.icon),
                    contentDescription = action.contentDescription,
                    tint = action.tint ?: ButterTheme.colors.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
