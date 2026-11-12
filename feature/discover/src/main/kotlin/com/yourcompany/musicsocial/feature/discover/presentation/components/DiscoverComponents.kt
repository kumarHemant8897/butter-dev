package com.yourcompany.musicsocial.feature.discover.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yourcompany.musicsocial.core.designsystem.component.Artwork
import com.yourcompany.musicsocial.core.designsystem.component.ArtworkTint
import com.yourcompany.musicsocial.core.designsystem.component.UserAvatar
import com.yourcompany.musicsocial.core.designsystem.theme.AppShapes
import com.yourcompany.musicsocial.core.designsystem.theme.ButterTheme
import com.yourcompany.musicsocial.core.designsystem.theme.Spacing

/** Wide playlist card: artwork on the left, violet eyebrow, title and stats on the right. */
@Composable
internal fun FeaturedPlaylistCard(
    eyebrow: String,
    title: String,
    stats: String,
    tintKey: String,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(90.dp)
            .clip(AppShapes.Card)
            .background(ButterTheme.colors.card)
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(imageUrl, null, AppShapes.Artwork, ArtworkTint.forKey(tintKey), Modifier.width(118.dp).fillMaxHeight(), fallbackIcon = null)
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f).padding(end = Spacing.md)) {
            Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(Spacing.xs))
            Text(stats, style = MaterialTheme.typography.labelMedium, color = ButterTheme.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * Round artist image with name and genre, for a horizontally scrolling row. The image is album art
 * from the artist's chart song: the catalogue has no artist photos.
 */
@Composable
internal fun ArtistChip(
    name: String,
    genre: String?,
    imageUrl: String?,
    onClickLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(ARTIST_CHIP_WIDTH)
            .clip(AppShapes.Card)
            .clickable(role = Role.Button, onClickLabel = onClickLabel, onClick = onClick)
            .padding(vertical = Spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        UserAvatar(name = name, imageUrl = imageUrl, size = ARTIST_IMAGE)
        Spacer(Modifier.height(Spacing.sm))
        Text(
            name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (genre != null) {
            Text(
                genre,
                style = MaterialTheme.typography.labelMedium,
                color = ButterTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private val ARTIST_CHIP_WIDTH = 92.dp
private val ARTIST_IMAGE = 76.dp
