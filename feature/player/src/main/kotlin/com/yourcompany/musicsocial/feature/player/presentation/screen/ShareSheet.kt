package com.yourcompany.musicsocial.feature.player.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yourcompany.musicsocial.core.designsystem.component.LoadingIndicator
import com.yourcompany.musicsocial.core.designsystem.component.UserAvatar
import com.yourcompany.musicsocial.core.designsystem.theme.AppShapes
import com.yourcompany.musicsocial.core.designsystem.theme.ButterTheme
import com.yourcompany.musicsocial.core.designsystem.theme.Spacing
import com.yourcompany.musicsocial.core.domain.model.ConversationSummary
import com.yourcompany.musicsocial.core.domain.model.ConversationType
import com.yourcompany.musicsocial.feature.player.R

/** "Share with": the user's chats, most recent first. [conversations] null while loading. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShareSheet(conversations: List<ConversationSummary>?, onPick: (ConversationSummary) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ButterTheme.colors.surfaceSecondary,
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = Spacing.lg)) {
            Text(
                stringResource(R.string.player_share_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm),
            )
            when {
                conversations == null -> Box(Modifier.fillMaxWidth().padding(Spacing.xl), contentAlignment = Alignment.Center) { LoadingIndicator() }
                conversations.isEmpty() -> Text(
                    stringResource(R.string.player_share_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = ButterTheme.colors.textSecondary,
                    modifier = Modifier.padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.md),
                )
                else -> LazyColumn(Modifier.heightIn(max = 460.dp)) {
                    items(conversations, key = { it.id }) { conversation ->
                        val isGroup = conversation.type == ConversationType.GROUP
                        val name = conversation.title ?: conversation.otherUser?.displayName.orEmpty()
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.Button) { onPick(conversation) }
                                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            UserAvatar(
                                name = name,
                                imageUrl = conversation.otherUser?.avatarUrl,
                                size = 44.dp,
                                shape = if (isGroup) AppShapes.GroupAvatar else androidx.compose.foundation.shape.CircleShape,
                            )
                            Spacer(Modifier.width(Spacing.md))
                            Column(Modifier.weight(1f)) {
                                Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    if (isGroup) stringResource(R.string.player_share_group) else "@" + conversation.otherUser?.username.orEmpty(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ButterTheme.colors.textSecondary,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
