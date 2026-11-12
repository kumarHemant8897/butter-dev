package com.yourcompany.musicsocial.feature.profile.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.res.pluralStringResource
import com.yourcompany.musicsocial.core.domain.model.Playlist
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.yourcompany.musicsocial.core.designsystem.component.ArtworkTint
import com.yourcompany.musicsocial.core.designsystem.component.RowAction
import com.yourcompany.musicsocial.core.designsystem.component.SectionHeader
import com.yourcompany.musicsocial.core.designsystem.component.SongRow
import com.yourcompany.musicsocial.core.designsystem.icon.AppIcons
import com.yourcompany.musicsocial.core.designsystem.theme.ButterTheme
import com.yourcompany.musicsocial.core.designsystem.theme.Spacing
import com.yourcompany.musicsocial.core.domain.model.LibraryKind
import com.yourcompany.musicsocial.core.domain.model.Song
import com.yourcompany.musicsocial.feature.profile.R

/** A user's liked and saved songs, plus what the player is doing, for highlighting rows. */
@Immutable
data class LibraryLists(
    val liked: List<Song> = emptyList(),
    val saved: List<Song> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val activeSongId: String? = null,
    val isActiveSongPlaying: Boolean = false,
) {
    fun songs(kind: LibraryKind): List<Song> = if (kind == LibraryKind.LIKED) liked else saved
}

/**
 * "Liked songs" and "Saved songs" sections. [isOwn] picks the empty-state copy ("you" vs "they").
 * Tapping a row plays that list from the song, or pauses/resumes it when it is already loaded.
 */
internal fun LazyListScope.librarySections(
    lists: LibraryLists,
    isOwn: Boolean,
    onSongClick: (LibraryKind, Int) -> Unit,
    onOpenUrl: (String) -> Unit,
    onOpenPlaylist: (Playlist) -> Unit = {},
    onNewPlaylist: (() -> Unit)? = null,
) {
    // Playlists first: a stranger's private playlists are never returned, so an empty list on someone
    // else's profile simply hides the section.
    if (isOwn || lists.playlists.isNotEmpty()) {
        item(key = "playlists-title") {
            SectionHeader(
                stringResource(R.string.profile_section_playlists),
                actionLabel = if (onNewPlaylist != null) stringResource(R.string.profile_new_playlist) else null,
                actionIcon = if (onNewPlaylist != null) AppIcons.Plus else null,
                actionColor = MaterialTheme.colorScheme.primary,
                onAction = onNewPlaylist,
            )
        }
        if (lists.playlists.isEmpty()) {
            item(key = "playlists-empty") {
                Text(
                    stringResource(R.string.profile_playlists_empty_own),
                    style = MaterialTheme.typography.bodySmall,
                    color = ButterTheme.colors.textSecondary,
                    modifier = Modifier.padding(horizontal = Spacing.screenHorizontal),
                )
            }
        }
        items(lists.playlists, key = { "playlist-${it.id}" }) { playlist ->
            SongRow(
                title = playlist.title,
                subtitle = pluralStringResource(R.plurals.profile_playlist_songs, playlist.songCount, playlist.songCount) +
                    if (isOwn && !playlist.isPublic) " · " + stringResource(R.string.profile_playlist_private) else "",
                artworkUrl = playlist.coverUrl,
                tint = ArtworkTint.forKey(playlist.id),
                onClick = { onOpenPlaylist(playlist) },
                action = RowAction(AppIcons.ArrowUpRight, stringResource(R.string.profile_open, playlist.title), onClick = { onOpenPlaylist(playlist) }),
            )
        }
    }
    LibraryKind.entries.forEach { kind ->
        val songs = lists.songs(kind)
        item(key = "lib-title-$kind") {
            SectionHeader(stringResource(if (kind == LibraryKind.LIKED) R.string.profile_section_liked else R.string.profile_section_saved))
        }
        if (songs.isEmpty()) {
            item(key = "lib-empty-$kind") {
                Text(
                    stringResource(emptyCopy(kind, isOwn)),
                    style = MaterialTheme.typography.bodySmall,
                    color = ButterTheme.colors.textSecondary,
                    modifier = Modifier.padding(horizontal = Spacing.screenHorizontal),
                )
            }
        }
        itemsIndexed(songs, key = { _, song -> "lib-$kind-${song.id}" }) { index, song ->
            LibrarySongRow(song, lists, onClick = { onSongClick(kind, index) }, onOpenUrl = onOpenUrl)
        }
    }
}

private fun emptyCopy(kind: LibraryKind, isOwn: Boolean): Int = when (kind) {
    LibraryKind.LIKED -> if (isOwn) R.string.profile_liked_empty_own else R.string.profile_liked_empty_other
    LibraryKind.SAVED -> if (isOwn) R.string.profile_saved_empty_own else R.string.profile_saved_empty_other
}

@Composable
private fun LibrarySongRow(song: Song, lists: LibraryLists, onClick: () -> Unit, onOpenUrl: (String) -> Unit) {
    val isActive = song.id == lists.activeSongId
    val isPlaying = isActive && lists.isActiveSongPlaying
    SongRow(
        title = song.title,
        subtitle = song.artistNames,
        artworkUrl = song.artworkUrl,
        tint = ArtworkTint.forKey(song.id),
        isActive = isActive,
        onClick = onClick,
        action = RowAction(
            icon = if (isPlaying) AppIcons.Pause else AppIcons.Play,
            contentDescription = stringResource(if (isPlaying) R.string.profile_pause else R.string.profile_play, song.title),
            tint = MaterialTheme.colorScheme.primary,
            onClick = onClick,
        ),
        // Apple's terms require a link to the song in Apple Music next to every preview.
        secondaryAction = song.externalUrl?.let { url ->
            RowAction(AppIcons.ArrowUpRight, stringResource(R.string.profile_open_apple_music, song.title), onClick = { onOpenUrl(url) })
        },
    )
}
