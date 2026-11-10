package com.yourcompany.musicsocial.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.yourcompany.musicsocial.core.designsystem.icon.AppIcons
import com.yourcompany.musicsocial.core.designsystem.theme.ButterTheme
import com.yourcompany.musicsocial.core.designsystem.theme.Dimens
import com.yourcompany.musicsocial.core.designsystem.theme.Spacing

/** A labelled number in the profile header, e.g. "1.2k" followers. */
@Immutable
data class ProfileStat(val value: String, val label: String)

/**
 * Profile header from the Figma "User Profile" frame: avatar left, name, violet handle and bio
 * right; inline stats; an optional lime affinity label; an action row supplied by the caller.
 */
@Composable
fun ProfileHeader(
    displayName: String,
    username: String,
    avatarUrl: String?,
    stats: List<ProfileStat>,
    modifier: Modifier = Modifier,
    bio: String? = null,
    affinityLabel: String? = null,
    actions: @Composable () -> Unit = {},
) {
    Column(modifier.fillMaxWidth().padding(horizontal = Spacing.screenHorizontal)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(name = displayName, imageUrl = avatarUrl, size = Dimens.avatarProfile)
            Spacer(Modifier.width(Spacing.lg))
            Column(Modifier.weight(1f)) {
                Text(displayName, style = MaterialTheme.typography.headlineLarge)
                Text("@$username", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                if (!bio.isNullOrBlank()) {
                    Spacer(Modifier.height(Spacing.xs))
                    Text(bio, style = MaterialTheme.typography.bodySmall, color = ButterTheme.colors.textSecondary)
                }
            }
        }
        Spacer(Modifier.height(Spacing.xl))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xl), verticalAlignment = Alignment.CenterVertically) {
            stats.forEach { stat ->
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.clearAndSetSemantics { contentDescription = "${stat.value} ${stat.label}" },
                ) {
                    Text(stat.value, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(6.dp))
                    Text(stat.label, style = MaterialTheme.typography.bodySmall, color = ButterTheme.colors.textSecondary)
                }
            }
            if (affinityLabel != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(AppIcons.ArrowUpRight), null, tint = ButterTheme.colors.live, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(Spacing.xs))
                    Text(affinityLabel, style = MaterialTheme.typography.labelMedium, color = ButterTheme.colors.live)
                }
            }
        }
        Spacer(Modifier.height(Spacing.xl))
        actions()
    }
}
