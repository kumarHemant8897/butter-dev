package com.yourcompany.musicsocial.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yourcompany.musicsocial.core.designsystem.icon.AppIcons
import com.yourcompany.musicsocial.core.designsystem.theme.AppShapes
import com.yourcompany.musicsocial.core.designsystem.theme.ButterTheme
import com.yourcompany.musicsocial.core.designsystem.theme.Spacing

/**
 * Screen title block: uppercase eyebrow ("YOUR NIGHT STARTS HERE") over a tight editorial title,
 * with an optional trailing action slot.
 */
@Composable
fun ScreenTitle(
    eyebrow: String,
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = Spacing.screenHorizontal, end = Spacing.screenHorizontal, top = Spacing.lg, bottom = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelSmall, color = ButterTheme.colors.textSecondary)
            Spacer(Modifier.size(6.dp))
            Text(title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
        }
        trailing()
    }
}

/**
 * Section heading with an optional quiet action on the right ("History", "See all").
 * [live] adds the lime presence dot used for real-time sections ("In good company").
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    live: Boolean = false,
    actionIcon: Int? = null,
    actionColor: Color? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = Spacing.screenHorizontal, end = Spacing.screenHorizontal, top = Spacing.sectionGap, bottom = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (live) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(ButterTheme.colors.live))
            Spacer(Modifier.width(Spacing.sm))
        }
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        if (actionLabel != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = if (onAction != null) Modifier.clickable(role = Role.Button, onClick = onAction) else Modifier,
            ) {
                Text(actionLabel, style = MaterialTheme.typography.labelMedium, color = actionColor ?: ButterTheme.colors.textSecondary)
                if (actionIcon != null) {
                    Spacer(Modifier.width(Spacing.xs))
                    Icon(painterResource(actionIcon), null, tint = actionColor ?: ButterTheme.colors.textSecondary, modifier = Modifier.size(11.dp))
                }
            }
        }
    }
}

/** Selectable pill ("Dream pop", "All 3"). Selected = violet outline on a soft violet fill. */
@Composable
fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null,
) {
    Row(
        modifier = modifier
            .heightIn(min = 32.dp)
            .clip(AppShapes.Pill)
            .background(if (selected) ButterTheme.colors.accentSoft else ButterTheme.colors.surfaceSecondary)
            .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else ButterTheme.colors.divider, AppShapes.Pill)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = if (selected) MaterialTheme.colorScheme.primary else ButterTheme.colors.textSecondary,
        )
        if (count != null) {
            Text(
                count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(AppShapes.Pill)
                    .background(ButterTheme.colors.accentSoft)
                    .padding(horizontal = 6.dp, vertical = 1.dp),
            )
        }
    }
}

/** Lime affinity badge from the Figma system ("94% taste match"). */
@Composable
fun TasteMatchBadge(label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(AppShapes.Pill)
            .background(ButterTheme.colors.surfaceSecondary)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(AppIcons.AudioLines), null, tint = ButterTheme.colors.live, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = ButterTheme.colors.live)
    }
}

/**
 * Person row ("Your kind of listeners", "In good company"): avatar, name, a secondary line and
 * an optional trailing slot (Follow button, time).
 */
@Composable
fun PersonRow(
    name: String,
    subtitle: String,
    avatarUrl: String?,
    modifier: Modifier = Modifier,
    isOnline: Boolean = false,
    subtitleColor: Color? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = Spacing.screenHorizontal, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserAvatar(name = name, imageUrl = avatarUrl, isOnline = isOnline)
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = subtitleColor ?: ButterTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailing()
    }
}

/** Round icon button on the card surface (compose, notifications). [badge] shows a violet dot. */
@Composable
fun CircleIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: Boolean = false,
    filled: Boolean = false,
    tint: Color? = null,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .then(if (filled) Modifier.background(ButterTheme.colors.card) else Modifier)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription, tint = tint ?: MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
        if (badge) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}
