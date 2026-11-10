package com.yourcompany.musicsocial.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yourcompany.musicsocial.core.designsystem.theme.AppShapes
import com.yourcompany.musicsocial.core.designsystem.theme.ButterTheme
import com.yourcompany.musicsocial.core.designsystem.theme.Dimens
import com.yourcompany.musicsocial.core.designsystem.theme.Spacing

/**
 * Large artwork card with title and subtitle ("A little more your speed").
 * Width comes from [modifier] so callers can lay cards out in a row or a grid.
 */
@Composable
fun MusicCard(
    title: String,
    subtitle: String,
    imageUrl: String?,
    tint: ArtworkTint,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 1.6f,
    onClick: (() -> Unit)? = null,
) {
    // No clip on the column: clipping to the card shape cut off glyphs at the text edge.
    Column(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
    ) {
        Artwork(
            imageUrl = imageUrl,
            contentDescription = null,
            shape = AppShapes.ArtworkLarge,
            tint = tint,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio),
        )
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(Spacing.xxs))
        Text(
            subtitle,
            style = MaterialTheme.typography.labelMedium,
            color = ButterTheme.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Compact artwork + two lines, used in the horizontal "Back in your orbit" strip. */
@Composable
fun MusicTile(
    title: String,
    subtitle: String,
    imageUrl: String?,
    tint: ArtworkTint,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .clip(AppShapes.Artwork)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Artwork(imageUrl, null, AppShapes.Artwork, tint, Modifier.size(Dimens.artworkTile), fallbackIcon = null)
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = ButterTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
