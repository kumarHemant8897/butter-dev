package com.yourcompany.musicsocial.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yourcompany.musicsocial.core.designsystem.component.Artwork
import com.yourcompany.musicsocial.core.designsystem.component.ArtworkTint
import com.yourcompany.musicsocial.core.designsystem.component.LoadingIndicator
import com.yourcompany.musicsocial.core.designsystem.icon.AppIcons
import com.yourcompany.musicsocial.core.designsystem.theme.AppShapes
import com.yourcompany.musicsocial.core.designsystem.theme.ButterTheme
import com.yourcompany.musicsocial.core.designsystem.theme.Spacing
import com.yourcompany.musicsocial.core.domain.model.Playlist
import com.yourcompany.musicsocial.core.domain.repository.PlaylistRepository
import com.yourcompany.musicsocial.core.ui.R

/** Name a new playlist and choose who sees it. [creating] disables the buttons while it is saved. */
@Composable
fun CreatePlaylistDialog(
    onCreate: (title: String, isPublic: Boolean) -> Unit,
    onDismiss: () -> Unit,
    creating: Boolean = false,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var isPublic by rememberSaveable { mutableStateOf(true) }
    val tooLong = title.trim().length > PlaylistRepository.TITLE_MAX_LENGTH
    val canCreate = title.isNotBlank() && !tooLong && !creating
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.playlist_new)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.playlist_name_hint)) },
                    isError = tooLong,
                    supportingText = if (tooLong) ({ Text(stringResource(R.string.playlist_name_too_long)) }) else null,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.playlist_public), style = MaterialTheme.typography.titleSmall)
                        Text(
                            stringResource(R.string.playlist_public_hint),
                            style = MaterialTheme.typography.labelMedium,
                            color = ButterTheme.colors.textSecondary,
                        )
                    }
                    Switch(checked = isPublic, onCheckedChange = { isPublic = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(title.trim(), isPublic) }, enabled = canCreate) { Text(stringResource(R.string.playlist_create)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !creating) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/**
 * Bottom sheet listing the user's playlists to add a song to, with "New playlist" on top.
 * [playlists] null means still loading.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
    playlists: List<Playlist>?,
    onPick: (Playlist) -> Unit,
    onNewPlaylist: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ButterTheme.colors.surfaceSecondary,
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = Spacing.lg)) {
            Text(
                stringResource(R.string.playlist_add_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm),
            )
            SheetRow(onClick = onNewPlaylist) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Icon(painterResource(AppIcons.Plus), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(Spacing.md))
                Text(stringResource(R.string.playlist_new), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            when {
                playlists == null -> Box(Modifier.fillMaxWidth().padding(Spacing.xl), contentAlignment = Alignment.Center) { LoadingIndicator() }
                playlists.isEmpty() -> Text(
                    stringResource(R.string.playlist_add_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = ButterTheme.colors.textSecondary,
                    modifier = Modifier.padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.md),
                )
                else -> LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(playlists, key = { it.id }) { playlist ->
                        SheetRow(onClick = { onPick(playlist) }) {
                            Artwork(playlist.coverUrl, null, AppShapes.Artwork, ArtworkTint.forKey(playlist.id), Modifier.size(48.dp))
                            Spacer(Modifier.width(Spacing.md))
                            Column(Modifier.weight(1f)) {
                                Text(playlist.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    pluralStringResource(R.plurals.playlist_song_count, playlist.songCount, playlist.songCount),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ButterTheme.colors.textSecondary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetRow(onClick: () -> Unit, content: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}
