package com.yourcompany.musicsocial.feature.chat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yourcompany.musicsocial.core.designsystem.component.Artwork
import com.yourcompany.musicsocial.core.designsystem.component.ArtworkTint
import com.yourcompany.musicsocial.core.designsystem.component.UserAvatar
import com.yourcompany.musicsocial.core.designsystem.icon.AppIcons
import com.yourcompany.musicsocial.core.designsystem.theme.AppShapes
import com.yourcompany.musicsocial.core.designsystem.theme.ButterTheme
import com.yourcompany.musicsocial.core.designsystem.theme.Dimens
import com.yourcompany.musicsocial.core.designsystem.theme.Spacing
import com.yourcompany.musicsocial.feature.chat.R
import com.yourcompany.musicsocial.feature.chat.presentation.state.ConversationItem

/** Figma "Messaging / Conversation row": avatar, name, preview, time and an unread counter. */
@Composable
internal fun ConversationRow(item: ConversationItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val unread = item.unreadCount > 0
    Column(modifier.clickable(onClick = onClick)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Spacing.screenHorizontal, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UserAvatar(
                name = item.title,
                imageUrl = null,
                size = Dimens.avatarLarge - 8.dp,
                isOnline = item.isOnline,
                shape = if (item.isGroup) AppShapes.GroupAvatar else CircleShape,
            )
            Spacer(Modifier.width(Spacing.md + 1.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(Spacing.xs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.previewIsMusic) {
                        Icon(painterResource(AppIcons.MusicNote), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        item.preview,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (unread) MaterialTheme.colorScheme.onSurface else ButterTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(Spacing.sm))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    item.timeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (unread) MaterialTheme.colorScheme.primary else ButterTheme.colors.textSecondary,
                )
                Spacer(Modifier.height(Spacing.sm))
                if (unread) {
                    val description = stringResource(R.string.chat_unread, item.unreadCount)
                    Box(
                        Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clearAndSetSemantics { contentDescription = description },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            item.unreadCount.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                } else {
                    Spacer(Modifier.height(18.dp))
                }
            }
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = Spacing.screenHorizontal).height(1.dp).background(ButterTheme.colors.divider))
    }
}

/** Mini player shown above the navigation bar while something is loaded in the player. */
@Composable
internal fun NowPlayingDock(
    title: String,
    artist: String,
    tintKey: String,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm)
            .clip(AppShapes.Card)
            .background(ButterTheme.colors.card)
            .border(1.dp, ButterTheme.colors.divider, AppShapes.Card)
            .padding(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(null, null, AppShapes.Artwork, ArtworkTint.forKey(tintKey), Modifier.size(40.dp), fallbackIcon = null)
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Text(artist, style = MaterialTheme.typography.labelMedium, color = ButterTheme.colors.textSecondary, maxLines = 1)
        }
        IconButton(onClick = onTogglePlay) {
            Icon(
                painterResource(if (isPlaying) AppIcons.Pause else AppIcons.Play),
                contentDescription = stringResource(if (isPlaying) R.string.chat_pause else R.string.chat_play),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
