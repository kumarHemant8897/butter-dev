package com.yourcompany.musicsocial.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.yourcompany.musicsocial.core.designsystem.theme.AppShapes
import com.yourcompany.musicsocial.core.designsystem.theme.ButterTheme
import com.yourcompany.musicsocial.core.designsystem.theme.Dimens
import com.yourcompany.musicsocial.core.designsystem.theme.Spacing

/** Button heights from the Figma action specimens: regular 44 dp, compact 32 dp ("Follow" in rows). */
enum class ButtonSize { Regular, Compact }

/**
 * The one primary accent: violet pill. While [loading] it keeps its size, shows a spinner and
 * ignores taps, which prevents double submissions.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    size: ButtonSize = ButtonSize.Regular,
    @DrawableRes leadingIcon: Int? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = AppShapes.Pill,
        contentPadding = size.padding(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
        ),
        modifier = modifier
            .heightIn(min = size.height())
            .loadingSemantics(loading),
    ) {
        ButtonContent(text, loading, leadingIcon, size, MaterialTheme.colorScheme.onPrimary)
    }
}

/** Quiet action next to a [PrimaryButton]: card-coloured pill with a hairline border. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    size: ButtonSize = ButtonSize.Regular,
    @DrawableRes leadingIcon: Int? = null,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = AppShapes.Pill,
        contentPadding = size.padding(),
        border = BorderStroke(1.dp, ButterTheme.colors.divider),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = ButterTheme.colors.surfaceSecondary,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = modifier
            .heightIn(min = size.height())
            .loadingSemantics(loading),
    ) {
        ButtonContent(text, loading, leadingIcon, size, MaterialTheme.colorScheme.onSurface)
    }
}

private fun ButtonSize.height() = if (this == ButtonSize.Regular) Dimens.buttonHeight else Dimens.buttonHeightCompact

private fun ButtonSize.padding() = if (this == ButtonSize.Regular) {
    PaddingValues(horizontal = Spacing.xl, vertical = Spacing.sm)
} else {
    PaddingValues(horizontal = Spacing.lg, vertical = Spacing.xs)
}

@Composable
private fun ButtonContent(text: String, loading: Boolean, @DrawableRes leadingIcon: Int?, size: ButtonSize, spinnerColor: Color) {
    if (loading) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = spinnerColor, strokeWidth = 2.dp)
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (leadingIcon != null) {
            Icon(painterResource(leadingIcon), contentDescription = null, modifier = Modifier.size(Dimens.iconSmall))
            Spacer(Modifier.width(Spacing.sm))
        }
        Text(
            text = text,
            style = if (size == ButtonSize.Regular) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleSmall,
        )
    }
}

private fun Modifier.loadingSemantics(loading: Boolean): Modifier =
    if (loading) semantics { stateDescription = "Loading" } else this
