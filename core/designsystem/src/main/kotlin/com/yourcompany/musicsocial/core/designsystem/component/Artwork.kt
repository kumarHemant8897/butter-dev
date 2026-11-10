package com.yourcompany.musicsocial.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import com.yourcompany.musicsocial.core.designsystem.icon.AppIcons

/**
 * Two-colour gradient drawn when an item has no image (or while it loads). Keeps rows and
 * cards from looking empty without shipping stock photos in the app.
 */
@Immutable
data class ArtworkTint(val start: Color, val end: Color) {
    companion object {
        /** Night palette that sits well on the Afterglow background. Index with a stable key. */
        val Presets = listOf(
            ArtworkTint(Color(0xFF1E2A5A), Color(0xFF4B6CB7)),
            ArtworkTint(Color(0xFF3A1C71), Color(0xFFA970FF)),
            ArtworkTint(Color(0xFF5B1A2E), Color(0xFFD7516B)),
            ArtworkTint(Color(0xFF0F3443), Color(0xFF34E89E)),
            ArtworkTint(Color(0xFF2C2A4A), Color(0xFF8E7DBE)),
            ArtworkTint(Color(0xFF42275A), Color(0xFFF2A07B)),
            ArtworkTint(Color(0xFF0B486B), Color(0xFFF56217)),
            ArtworkTint(Color(0xFF1F1C2C), Color(0xFF928DAB)),
        )

        /** Deterministic tint for an id, so the same song always gets the same colours. */
        fun forKey(key: String): ArtworkTint = Presets[Math.floorMod(key.hashCode(), Presets.size)]
    }
}

/**
 * Remote image over a gradient fallback. The fallback shows while loading, when [imageUrl] is
 * null and when the request fails, without layout shifts. Coil caches to memory and disk.
 */
@Composable
fun Artwork(
    imageUrl: String?,
    contentDescription: String?,
    shape: Shape,
    tint: ArtworkTint,
    modifier: Modifier = Modifier,
    @DrawableRes fallbackIcon: Int? = AppIcons.MusicNote,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(Brush.linearGradient(listOf(tint.start, tint.end))),
        contentAlignment = Alignment.Center,
    ) {
        if (fallbackIcon != null) {
            Icon(
                painter = painterResource(fallbackIcon),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(0.38f),
            )
        }
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
