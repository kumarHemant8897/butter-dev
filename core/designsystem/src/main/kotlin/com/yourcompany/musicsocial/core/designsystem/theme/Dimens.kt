package com.yourcompany.musicsocial.core.designsystem.theme

import androidx.compose.ui.unit.dp

/** Figma rhythm: 4 px base, 20 px screen insets. Use these instead of literal dp values. */
object Spacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp

    /** Horizontal inset of every screen. */
    val screenHorizontal = 20.dp

    /** Gap between the end of one section and the next section heading. */
    val sectionGap = 24.dp
}

/** Fixed component sizes. 44 dp is the Figma minimum tap target. */
object Dimens {
    val minTouchTarget = 44.dp
    val buttonHeight = 44.dp
    val buttonHeightCompact = 32.dp
    val fieldHeight = 44.dp

    val avatarSmall = 32.dp
    val avatarMedium = 42.dp
    val avatarLarge = 52.dp
    val avatarProfile = 84.dp

    val artworkRow = 44.dp
    val artworkTile = 36.dp
    val artworkPlayer = 353.dp

    val iconSmall = 16.dp
    val iconMedium = 21.dp
    val iconLarge = 24.dp

    val navigationBarHeight = 66.dp
    val playButtonLarge = 60.dp
}
