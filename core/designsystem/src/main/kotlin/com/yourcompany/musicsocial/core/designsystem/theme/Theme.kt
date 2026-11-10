package com.yourcompany.musicsocial.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = Palette.Violet,
    onPrimary = Palette.Background,
    primaryContainer = Palette.Violet.copy(alpha = 0.16f),
    onPrimaryContainer = Palette.Violet,
    secondary = Palette.Lime,
    onSecondary = Palette.Background,
    secondaryContainer = Palette.Lime.copy(alpha = 0.14f),
    onSecondaryContainer = Palette.Lime,
    tertiary = Palette.Violet,
    background = Palette.Background,
    onBackground = Palette.TextPrimary,
    surface = Palette.Background,
    onSurface = Palette.TextPrimary,
    surfaceVariant = Palette.SurfaceCard,
    onSurfaceVariant = Palette.TextSecondary,
    surfaceContainerLowest = Palette.Background,
    surfaceContainerLow = Palette.SurfaceSecondary,
    surfaceContainer = Palette.SurfaceCard,
    surfaceContainerHigh = Palette.SurfaceCard,
    surfaceContainerHighest = Palette.Divider,
    outline = Palette.Divider,
    outlineVariant = Palette.Divider,
    error = Palette.Error,
    onError = Palette.Background,
    errorContainer = Palette.Error.copy(alpha = 0.14f),
    onErrorContainer = Palette.Error,
    inverseSurface = Palette.TextPrimary,
    inverseOnSurface = Palette.Background,
    scrim = Palette.Background,
)

/** Not designed yet. Defined so the preference switch works; dark is the product default. */
private val LightColorScheme: ColorScheme = lightColorScheme(
    primary = Palette.VioletDeep,
    onPrimary = Palette.LightCard,
    primaryContainer = Palette.VioletDeep.copy(alpha = 0.12f),
    onPrimaryContainer = Palette.VioletDeep,
    secondary = Palette.LimeDeep,
    onSecondary = Palette.LightCard,
    background = Palette.LightBackground,
    onBackground = Palette.LightTextPrimary,
    surface = Palette.LightBackground,
    onSurface = Palette.LightTextPrimary,
    surfaceVariant = Palette.LightCard,
    onSurfaceVariant = Palette.LightTextSecondary,
    surfaceContainerLow = Palette.LightSurfaceSecondary,
    surfaceContainer = Palette.LightCard,
    surfaceContainerHigh = Palette.LightCard,
    surfaceContainerHighest = Palette.LightDivider,
    outline = Palette.LightDivider,
    outlineVariant = Palette.LightDivider,
)

private val LocalExtendedColors = staticCompositionLocalOf { DarkExtendedColors }

/**
 * Butter theme (Afterglow visual language). [darkTheme] defaults to true rather than following the
 * system, because dark is the product default; the saved preference in :core:datastore drives it.
 * Dynamic colour (Material You) is not used: the palette is part of the brand.
 */
@Composable
fun ButterTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalExtendedColors provides if (darkTheme) DarkExtendedColors else LightExtendedColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = ButterTypography,
            shapes = ButterShapes,
            content = content,
        )
    }
}

/** Access to tokens that Material 3 has no slot for. */
object ButterTheme {
    val colors: ExtendedColors
        @Composable @ReadOnlyComposable
        get() = LocalExtendedColors.current
}
