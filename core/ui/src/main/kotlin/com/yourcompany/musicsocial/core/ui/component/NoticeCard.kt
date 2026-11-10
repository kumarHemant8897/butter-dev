package com.yourcompany.musicsocial.core.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.yourcompany.musicsocial.core.designsystem.icon.AppIcons
import com.yourcompany.musicsocial.core.designsystem.theme.AppShapes
import com.yourcompany.musicsocial.core.designsystem.theme.Dimens
import com.yourcompany.musicsocial.core.designsystem.theme.ButterTheme
import com.yourcompany.musicsocial.core.designsystem.theme.Spacing
import com.yourcompany.musicsocial.core.ui.R

/** Compact inline notice: an icon, a title and a message on a tinted surface. */
@Composable
fun NoticeCard(
    @DrawableRes icon: Int,
    title: String,
    message: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.Card)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(Spacing.lg),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(Dimens.iconMedium))
        Spacer(Modifier.width(Spacing.md))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(Spacing.xxs))
            Text(message, style = MaterialTheme.typography.bodySmall, color = ButterTheme.colors.textSecondary)
        }
    }
}

/** Shown wherever a feature needs the backend but this build has no Supabase credentials. */
@Composable
fun BackendNotConfiguredNotice(modifier: Modifier = Modifier) {
    NoticeCard(
        icon = AppIcons.Error,
        title = stringResource(R.string.error_backend_not_configured_title),
        message = stringResource(R.string.error_backend_not_configured_message),
        tint = MaterialTheme.colorScheme.error,
        modifier = modifier,
    )
}
