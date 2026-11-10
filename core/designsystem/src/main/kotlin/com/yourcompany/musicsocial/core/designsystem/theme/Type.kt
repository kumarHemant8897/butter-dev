package com.yourcompany.musicsocial.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.yourcompany.musicsocial.core.designsystem.R

/**
 * Inter (SIL Open Font License, docs/licenses/Inter-OFL.txt), bundled as one variable font file.
 * Figma scale: Display 44/500, Title 28/600, Section 17/600, Body 14/400, Caption 10/500 (caps).
 * "Sentence case. Tight editorial headings; comfortable body text."
 */
private fun inter(weight: FontWeight) = Font(
    resId = R.font.inter,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Inter = FontFamily(
    inter(FontWeight.Normal),
    inter(FontWeight.Medium),
    inter(FontWeight.SemiBold),
    inter(FontWeight.Bold),
)

private fun style(size: Double, line: Double, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
    fontFamily = Inter,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.em,
)

internal val ButterTypography = Typography(
    // Display: hero statements.
    displayLarge = style(44.0, 52.0, FontWeight.Medium, -0.03),
    displayMedium = style(36.0, 44.0, FontWeight.SemiBold, -0.03),
    // Player song title.
    displaySmall = style(32.0, 38.0, FontWeight.SemiBold, -0.03),
    // Title: screen titles ("Hey, Hemant.", "Find your frequency.").
    headlineLarge = style(28.0, 34.0, FontWeight.SemiBold, -0.03),
    headlineMedium = style(24.0, 30.0, FontWeight.SemiBold, -0.025),
    headlineSmall = style(20.0, 26.0, FontWeight.SemiBold, -0.02),
    // Section headings ("Back in your orbit").
    titleLarge = style(17.0, 22.0, FontWeight.SemiBold, -0.01),
    // Row titles.
    titleMedium = style(15.0, 20.0, FontWeight.SemiBold, -0.005),
    titleSmall = style(13.0, 18.0, FontWeight.SemiBold),
    // Body.
    bodyLarge = style(15.0, 22.0, FontWeight.Normal),
    bodyMedium = style(14.0, 20.0, FontWeight.Normal),
    bodySmall = style(12.0, 16.0, FontWeight.Normal),
    // Labels: buttons, section actions, navigation.
    labelLarge = style(14.0, 18.0, FontWeight.SemiBold),
    labelMedium = style(11.0, 14.0, FontWeight.Medium),
    // Caption / eyebrow, used uppercase: "YOUR NIGHT STARTS HERE".
    labelSmall = style(10.0, 14.0, FontWeight.Medium, 0.16),
)
