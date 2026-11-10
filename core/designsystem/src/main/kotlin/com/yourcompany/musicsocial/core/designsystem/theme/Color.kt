package com.yourcompany.musicsocial.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Afterglow palette, taken from the Figma variables ("afterglow / ..."):
 * near-black foundations, violet for action, lime only for presence, affinity and live signals.
 *
 * Composables must not use these directly; read them from MaterialTheme.colorScheme or
 * ButterTheme.colors so the light theme can be added later without touching features.
 */
internal object Palette {
    val Background = Color(0xFF0B0B0F)
    val SurfaceSecondary = Color(0xFF12121A)
    val SurfaceCard = Color(0xFF1B1B26)
    val Violet = Color(0xFFA970FF)
    val Lime = Color(0xFFC5FF6B)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFA1A1AA)
    val Divider = Color(0xFF292934)

    /** Not part of the Figma palette: errors need a colour that is neither action nor presence. */
    val Error = Color(0xFFFF6B81)

    // Light theme (not designed yet; kept so the theme switch works end to end).
    val LightBackground = Color(0xFFF7F7FA)
    val LightCard = Color(0xFFFFFFFF)
    val LightSurfaceSecondary = Color(0xFFEDEDF3)
    val LightTextPrimary = Color(0xFF111117)
    val LightTextSecondary = Color(0xFF5B5B66)
    val LightDivider = Color(0xFFDCDCE5)
    val VioletDeep = Color(0xFF7A3FE0)
    val LimeDeep = Color(0xFF4F7A12)
}

/** Colours Material 3 has no slot for. Exposed through ButterTheme.colors. */
@Immutable
data class ExtendedColors(
    val textSecondary: Color,
    val surfaceSecondary: Color,
    val card: Color,
    val divider: Color,
    /** Lime: presence (online), affinity (taste match) and live signals only. */
    val live: Color,
    /** Violet at low opacity, for selected chips and soft action backgrounds. */
    val accentSoft: Color,
)

internal val DarkExtendedColors = ExtendedColors(
    textSecondary = Palette.TextSecondary,
    surfaceSecondary = Palette.SurfaceSecondary,
    card = Palette.SurfaceCard,
    divider = Palette.Divider,
    live = Palette.Lime,
    accentSoft = Palette.Violet.copy(alpha = 0.16f),
)

internal val LightExtendedColors = ExtendedColors(
    textSecondary = Palette.LightTextSecondary,
    surfaceSecondary = Palette.LightSurfaceSecondary,
    card = Palette.LightCard,
    divider = Palette.LightDivider,
    live = Palette.LimeDeep,
    accentSoft = Palette.VioletDeep.copy(alpha = 0.12f),
)
