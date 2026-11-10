package com.yourcompany.musicsocial.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.yourcompany.musicsocial.core.designsystem.icon.AppIcons
import com.yourcompany.musicsocial.core.designsystem.theme.AppShapes
import com.yourcompany.musicsocial.core.designsystem.theme.ButterTheme
import com.yourcompany.musicsocial.core.designsystem.theme.Dimens
import com.yourcompany.musicsocial.core.designsystem.theme.Spacing

/**
 * Form input (sign-in, edit profile). Validation lives in use cases; this component only shows
 * [errorText] when one is supplied.
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    errorText: String? = null,
    supportingText: String? = null,
    @DrawableRes leadingIcon: Int? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    isPassword: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val secondary = ButterTheme.colors.textSecondary
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        singleLine = singleLine,
        shape = AppShapes.Field,
        textStyle = MaterialTheme.typography.bodyMedium,
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it, color = secondary) } },
        leadingIcon = leadingIcon?.let { { Icon(painterResource(it), contentDescription = null) } },
        isError = errorText != null,
        supportingText = (errorText ?: supportingText)?.let { { Text(it) } },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = ButterTheme.colors.surfaceSecondary,
            unfocusedContainerColor = ButterTheme.colors.surfaceSecondary,
            disabledContainerColor = ButterTheme.colors.surfaceSecondary,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = ButterTheme.colors.divider,
            unfocusedLeadingIconColor = secondary,
            unfocusedLabelColor = secondary,
        ),
    )
}

/**
 * Search bar from the Figma "Search / ..." components: card surface, 12 dp corners,
 * magnifier and a quiet placeholder.
 */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearch: () -> Unit = {},
) {
    val secondary = ButterTheme.colors.textSecondary
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.fieldHeight)
            .clip(AppShapes.Field)
            .background(ButterTheme.colors.card)
            .border(1.dp, ButterTheme.colors.divider.copy(alpha = 0.6f), AppShapes.Field)
            .padding(horizontal = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(AppIcons.Search), contentDescription = null, tint = secondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(Spacing.sm + 2.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = secondary, maxLines = 1)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
