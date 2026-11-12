package com.yourcompany.musicsocial.feature.chat.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourcompany.musicsocial.core.designsystem.component.CircleIconButton
import com.yourcompany.musicsocial.core.designsystem.component.LoadingIndicator
import com.yourcompany.musicsocial.core.designsystem.component.UserAvatar
import com.yourcompany.musicsocial.core.designsystem.icon.AppIcons
import com.yourcompany.musicsocial.core.designsystem.theme.ButterTheme
import com.yourcompany.musicsocial.core.designsystem.theme.Spacing
import com.yourcompany.musicsocial.core.domain.model.DeliveryStatus
import com.yourcompany.musicsocial.core.domain.model.MessageContent
import com.yourcompany.musicsocial.core.domain.result.AppError
import com.yourcompany.musicsocial.core.ui.error.AppErrorView
import com.yourcompany.musicsocial.core.ui.error.toErrorCopy
import com.yourcompany.musicsocial.feature.chat.R
import com.yourcompany.musicsocial.feature.chat.presentation.viewmodel.ConversationUiState
import com.yourcompany.musicsocial.feature.chat.presentation.viewmodel.ConversationViewModel
import com.yourcompany.musicsocial.feature.chat.presentation.viewmodel.ThreadMessage
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun ConversationRoute(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit,
    viewModel: ConversationViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    // Safety net under Realtime: a dropped socket on Wi-Fi must not leave the thread stale.
    LaunchedEffect(Unit) {
        while (true) {
            delay(REFRESH_EVERY_MS)
            viewModel.refresh()
        }
    }
    ConversationScreen(
        state = state,
        contentPadding = contentPadding,
        actions = ConversationActions(
            onBack = onBack,
            onOpenProfile = { state.otherUserId?.let(onOpenProfile) },
            onDraftChange = viewModel::onDraftChange,
            onSend = viewModel::onSend,
            onRetrySend = viewModel::onRetrySend,
            onRetryLoad = viewModel::onRetryLoad,
        ),
    )
}

private const val REFRESH_EVERY_MS = 5_000L

internal data class ConversationActions(
    val onBack: () -> Unit = {},
    val onOpenProfile: () -> Unit = {},
    val onDraftChange: (String) -> Unit = {},
    val onSend: () -> Unit = {},
    val onRetrySend: (String) -> Unit = {},
    val onRetryLoad: () -> Unit = {},
)

@Composable
internal fun ConversationScreen(
    state: ConversationUiState,
    contentPadding: PaddingValues,
    actions: ConversationActions,
    modifier: Modifier = Modifier,
) {
    // consumeWindowInsets: the navigation bar inset is already in contentPadding, so the keyboard
    // padding adds only what the keyboard covers beyond it.
    Column(modifier.fillMaxSize().padding(contentPadding).consumeWindowInsets(contentPadding).imePadding()) {
        Header(state, actions)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.isLoading && state.messages.isEmpty() -> LoadingIndicator(Modifier.align(Alignment.Center))
                state.loadError != null -> AppErrorView(error = state.loadError, onRetry = actions.onRetryLoad)
                state.messages.isEmpty() -> Text(
                    stringResource(R.string.chat_empty_thread, state.title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ButterTheme.colors.textSecondary,
                    modifier = Modifier.align(Alignment.Center).padding(Spacing.xl),
                )
                // Newest at the bottom, and the list opens there.
                else -> LazyColumn(
                    reverseLayout = true,
                    contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal, vertical = Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    val newestMineReadId = state.messages.firstOrNull { it.isMine && it.status == DeliveryStatus.READ }?.id
                    items(state.messages, key = { it.id }) { message ->
                        Bubble(message, showSeen = message.id == newestMineReadId, onRetry = { actions.onRetrySend(message.id) })
                    }
                }
            }
        }
        state.sendError?.let { error ->
            Text(
                if (error is AppError.Validation) stringResource(R.string.chat_too_long) else stringResource(error.toErrorCopy().message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = Spacing.screenHorizontal),
            )
        }
        Composer(state.draft, actions.onDraftChange, actions.onSend)
    }
}

@Composable
private fun Header(state: ConversationUiState, actions: ConversationActions) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(icon = AppIcons.Back, contentDescription = stringResource(R.string.chat_back), onClick = actions.onBack)
        Spacer(Modifier.width(Spacing.sm))
        val profileLabel = stringResource(R.string.chat_open_profile, state.title)
        Row(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .then(
                    if (state.otherUserId != null) Modifier.clickable(role = Role.Button, onClickLabel = profileLabel, onClick = actions.onOpenProfile) else Modifier,
                )
                .padding(Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UserAvatar(name = state.title, imageUrl = null, size = 36.dp)
            Spacer(Modifier.width(Spacing.sm))
            Text(state.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    Box(Modifier.fillMaxWidth().padding(top = 1.dp).background(ButterTheme.colors.divider).heightIn(min = 1.dp, max = 1.dp))
}

@Composable
private fun Bubble(message: ThreadMessage, showSeen: Boolean, onRetry: () -> Unit) {
    val mine = message.isMine
    val failed = message.status == DeliveryStatus.FAILED
    val shape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = if (mine) 18.dp else 4.dp,
        bottomEnd = if (mine) 4.dp else 18.dp,
    )
    val text = when (val content = message.content) {
        is MessageContent.Text -> content.body
        is MessageContent.SharedSong -> stringResource(R.string.chat_shared_song)
        is MessageContent.SharedPlaylist -> stringResource(R.string.chat_shared_playlist)
        MessageContent.Deleted -> stringResource(R.string.chat_deleted)
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
        Box(
            Modifier
                .widthIn(max = 290.dp)
                .clip(shape)
                .background(if (mine) MaterialTheme.colorScheme.primary else ButterTheme.colors.card)
                .then(if (failed) Modifier.border(1.dp, MaterialTheme.colorScheme.error, shape).clickable(role = Role.Button, onClick = onRetry) else Modifier)
                .padding(horizontal = 14.dp, vertical = 9.dp),
        ) {
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            )
        }
        val meta = when (message.status) {
            DeliveryStatus.SENDING -> stringResource(R.string.chat_sending)
            DeliveryStatus.FAILED -> stringResource(R.string.chat_failed)
            else -> timeLabel(message.sentAt) + if (showSeen) " · " + stringResource(R.string.chat_seen) else ""
        }
        Text(
            meta,
            style = MaterialTheme.typography.labelSmall,
            color = if (failed) MaterialTheme.colorScheme.error else ButterTheme.colors.textSecondary,
            modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 2.dp),
        )
    }
}

@Composable
private fun Composer(draft: String, onDraftChange: (String) -> Unit, onSend: () -> Unit) {
    val canSend = draft.isNotBlank()
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .weight(1f)
                .heightIn(min = 44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(ButterTheme.colors.card)
                .border(1.dp, ButterTheme.colors.divider, RoundedCornerShape(22.dp))
                .padding(horizontal = Spacing.lg, vertical = 11.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (draft.isEmpty()) {
                Text(stringResource(R.string.chat_message_hint), style = MaterialTheme.typography.bodyMedium, color = ButterTheme.colors.textSecondary)
            }
            BasicTextField(
                value = draft,
                onValueChange = onDraftChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.width(Spacing.sm))
        val sendLabel = stringResource(R.string.chat_send)
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (canSend) MaterialTheme.colorScheme.primary else ButterTheme.colors.card)
                .clickable(enabled = canSend, role = Role.Button, onClick = onSend)
                .semantics { contentDescription = sendLabel },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(AppIcons.Send),
                contentDescription = null,
                tint = if (canSend) MaterialTheme.colorScheme.onPrimary else ButterTheme.colors.textSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm", Locale.ROOT)

private fun timeLabel(instant: Instant): String = instant.atZone(ZoneId.systemDefault()).format(TIME)

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0F, heightDp = 700)
@Composable
private fun ConversationScreenPreview() {
    val now = Instant.parse("2026-11-11T10:00:00Z")
    ButterTheme {
        ConversationScreen(
            state = ConversationUiState(
                title = "Luna Waves",
                otherUserId = "u2",
                isLoading = false,
                messages = listOf(
                    ThreadMessage("3", null, true, MessageContent.Text("on my way"), now.plusSeconds(60), DeliveryStatus.READ),
                    ThreadMessage("2", null, false, MessageContent.Text("this song is a vibe"), now.plusSeconds(30), DeliveryStatus.SENT),
                    ThreadMessage("1", null, true, MessageContent.Text("hey!"), now, DeliveryStatus.READ),
                ),
            ),
            contentPadding = PaddingValues(),
            actions = ConversationActions(),
        )
    }
}
