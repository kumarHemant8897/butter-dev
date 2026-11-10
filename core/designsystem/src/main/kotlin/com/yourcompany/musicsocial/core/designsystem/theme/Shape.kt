package com.yourcompany.musicsocial.core.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Figma radius scale: 8 / 12 / 18. Circles are reserved for people and controls. */
internal val ButterShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(18.dp),
)

/** Shapes with a specific role, so the same element looks identical everywhere. */
object AppShapes {
    val Artwork = RoundedCornerShape(8.dp)
    val ArtworkLarge = RoundedCornerShape(12.dp)
    val Card = RoundedCornerShape(12.dp)
    val Sheet = RoundedCornerShape(18.dp)
    val Field = RoundedCornerShape(12.dp)
    val Pill = RoundedCornerShape(percent = 50)
    /** People and round controls. */
    val Circle = CircleShape
    /** Group conversations use a rounded square so they read differently from people. */
    val GroupAvatar = RoundedCornerShape(14.dp)
}
