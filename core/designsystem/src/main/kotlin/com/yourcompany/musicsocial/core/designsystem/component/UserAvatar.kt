package com.yourcompany.musicsocial.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.yourcompany.musicsocial.core.designsystem.theme.ButterTheme
import com.yourcompany.musicsocial.core.designsystem.theme.Dimens

/**
 * Person avatar: circular photo, or initials on a gradient when there is no photo.
 * [isOnline] draws the lime presence dot from the Figma "Presence pattern".
 * Pass [shape] = AppShapes.GroupAvatar for group conversations.
 */
@Composable
fun UserAvatar(
    name: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = Dimens.avatarMedium,
    isOnline: Boolean = false,
    shape: Shape = CircleShape,
    tint: ArtworkTint = ArtworkTint.forKey(name),
) {
    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(Brush.linearGradient(listOf(tint.start, tint.end))),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initialsOf(name),
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = (size.value * 0.36f).sp,
                style = MaterialTheme.typography.titleMedium,
            )
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "$name's photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        if (isOnline) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size((size.value * 0.24f).coerceAtLeast(8f).dp)
                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                    .clip(CircleShape)
                    .background(ButterTheme.colors.live),
            )
        }
    }
}

/** "Hemant Kumar" -> "HK", "nova" -> "N", "" -> "?" */
fun initialsOf(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(1).uppercase()
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}
