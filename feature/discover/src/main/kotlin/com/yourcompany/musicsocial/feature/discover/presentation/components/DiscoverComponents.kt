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
        Artwork(null, null, AppShapes.Artwork, ArtworkTint.forKey(tintKey), Modifier.width(118.dp).fillMaxHeight(), fallbackIcon = null)
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f).padding(end = Spacing.md)) {
            Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(Spacing.xs))
            Text(stats, style = MaterialTheme.typography.labelMedium, color = ButterTheme.colors.textSecondary)
        }
    }
}

/** Artist avatar with name and genre, laid out three per row. */
@Composable
internal fun ArtistChip(name: String, genre: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        UserAvatar(name = name, imageUrl = null, size = 34.dp)
        Spacer(Modifier.width(Spacing.sm))
        Column {
            Text(name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(genre, style = MaterialTheme.typography.labelMedium, color = ButterTheme.colors.textSecondary, maxLines = 1)
        }
    }
}
