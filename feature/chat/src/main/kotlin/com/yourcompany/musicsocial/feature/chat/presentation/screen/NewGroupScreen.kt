package com.yourcompany.musicsocial.feature.chat.presentation.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourcompany.musicsocial.core.designsystem.component.ButtonSize
import com.yourcompany.musicsocial.core.designsystem.component.CircleIconButton
import com.yourcompany.musicsocial.core.designsystem.component.LoadingIndicator
import com.yourcompany.musicsocial.core.designsystem.component.PersonRow
import com.yourcompany.musicsocial.core.designsystem.component.PrimaryButton
import com.yourcompany.musicsocial.core.designsystem.component.SectionHeader
import com.yourcompany.musicsocial.core.designsystem.icon.AppIcons
import com.yourcompany.musicsocial.core.designsystem.theme.ButterTheme
import com.yourcompany.musicsocial.core.designsystem.theme.Spacing
import com.yourcompany.musicsocial.core.navigation.ConversationRoute
import com.yourcompany.musicsocial.core.ui.error.AppErrorView
import com.yourcompany.musicsocial.core.ui.error.toErrorCopy
import com.yourcompany.musicsocial.feature.chat.R
import com.yourcompany.musicsocial.feature.chat.presentation.viewmodel.NewGroupUiState
import com.yourcompany.musicsocial.feature.chat.presentation.viewmodel.NewGroupViewModel

@Composable
internal fun NewGroupRoute(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onCreated: (ConversationRoute) -> Unit,
    viewModel: NewGroupViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.created) {
        state.created?.let {
            viewModel.onOpened()
            onCreated(it)
        }
    }
    NewGroupScreen(state, contentPadding, onBack, viewModel::onTitleChange, viewModel::onToggle, viewModel::onCreate, viewModel::onRetry)
}

@Composable
internal fun NewGroupScreen(
    state: NewGroupUiState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onToggle: (String) -> Unit,
    onCreate: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().padding(contentPadding).consumeWindowInsets(contentPadding).imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(icon = AppIcons.Back, contentDescription = stringResource(R.string.chat_back), onClick = onBack)
            Text(
                stringResource(R.string.group_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f).padding(start = Spacing.sm).semantics { heading() },
            )
            PrimaryButton(stringResource(R.string.group_create), onClick = onCreate, enabled = state.canCreate, loading = state.isCreating, size = ButtonSize.Compact)
        }
        OutlinedTextField(
            value = state.title,
            onValueChange = onTitleChange,
            singleLine = true,
            label = { Text(stringResource(R.string.group_name_hint)) },
            isError = state.titleTooLong,
            supportingText = if (state.titleTooLong) ({ Text(stringResource(R.string.group_name_too_long)) }) else null,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screenHorizontal),
        )
        state.error?.takeIf { state.people.isNotEmpty() }?.let { error ->
            Text(
                stringResource(error.toErrorCopy().message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.xs),
            )
        }
        LazyColumn(Modifier.weight(1f)) {
            when {
                state.isLoading -> item(key = "loading") {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { LoadingIndicator() }
                }
                state.error != null && state.people.isEmpty() -> item(key = "error") {
                    AppErrorView(error = state.error, onRetry = onRetry, modifier = Modifier.fillMaxWidth().height(320.dp))
                }
                state.people.isEmpty() -> item(key = "nobody") {
                    Text(
                        stringResource(R.string.group_nobody),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ButterTheme.colors.textSecondary,
                        modifier = Modifier.padding(Spacing.screenHorizontal),
                    )
                }
                else -> {
                    item(key = "title") {
                        SectionHeader(
                            stringResource(R.string.group_pick_people),
                            actionLabel = if (state.selected.isNotEmpty()) pluralStringResource(R.plurals.group_selected, state.selected.size, state.selected.size) else null,
                        )
                    }
                    items(state.people, key = { it.id }) { person ->
                        val checked = person.id in state.selected
                        PersonRow(
                            name = person.displayName,
                            subtitle = "@${person.username}",
                            avatarUrl = person.avatarUrl,
                            onClick = { onToggle(person.id) },
                        ) {
                            Checkbox(checked = checked, onCheckedChange = { onToggle(person.id) })
                        }
                    }
                }
            }
        }
    }
}
