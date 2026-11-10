package com.yourcompany.musicsocial.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yourcompany.musicsocial.core.designsystem.icon.AppIcons
import com.yourcompany.musicsocial.core.designsystem.theme.ButterTheme
import com.yourcompany.musicsocial.core.designsystem.theme.Spacing

/**
 * Animated equaliser bars: a music-themed loading indicator.
 * Animation values are read inside graphicsLayer, so frames trigger only a redraw,
 * never a recomposition.
 */
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    contentDescription: String = "Loading",
) {
    val transition = rememberInfiniteTransition(label = "equaliser")
    val color = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .height(28.dp)
            .semantics { this.contentDescription = contentDescription },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        BAR_DURATIONS_MS.forEachIndexed { index, duration ->
            val fraction by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(duration), RepeatMode.Reverse),
                label = "bar$index",
            )
            Box(
                Modifier
                    .width(5.dp)
                    .height(28.dp)
                    .graphicsLayer {
                        scaleY = fraction
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                    }
                    .clip(RoundedCornerShape(3.dp))
                    .background(color),
            )
        }
    }
}

private val BAR_DURATIONS_MS = listOf(420, 560, 380, 500)

/** Full-area loading state. */
@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LoadingIndicator()
    }
}

/**
 * Full-area error state. Pass [onRetry] only when retrying can actually help
 * (network or server errors, not validation errors).
 */
@Composable
fun ErrorView(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int = AppIcons.Error,
    retryLabel: String = "Try again",
    onRetry: (() -> Unit)? = null,
) {
    StatusLayout(
        icon = icon,
        iconTint = MaterialTheme.colorScheme.error,
        title = title,
        message = message,
        modifier = modifier,
    ) {
        if (onRetry != null) {
            SecondaryButton(text = retryLabel, onClick = onRetry)
        }
    }
}

/** Full-area state for "nothing here yet", optionally with one action. */
@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int = AppIcons.MusicNote,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    StatusLayout(
        icon = icon,
        iconTint = MaterialTheme.colorScheme.primary,
        title = title,
        message = message,
        modifier = modifier,
    ) {
        if (actionLabel != null && onAction != null) {
            PrimaryButton(text = actionLabel, onClick = onAction)
        }
    }
}

@Composable
private fun StatusLayout(
    @DrawableRes icon: Int,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    message: String,
    modifier: Modifier,
    action: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = iconTint, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(Spacing.xl))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Spacing.sm))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = ButterTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.xl))
        action()
    }
}
