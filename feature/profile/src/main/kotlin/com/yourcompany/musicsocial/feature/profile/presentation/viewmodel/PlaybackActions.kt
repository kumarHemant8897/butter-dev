package com.yourcompany.musicsocial.feature.profile.presentation.viewmodel

import com.yourcompany.musicsocial.core.domain.model.Song
import com.yourcompany.musicsocial.core.domain.playback.PlaybackController
import com.yourcompany.musicsocial.feature.profile.presentation.components.LibraryLists
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Plays [songs] from [index]; tapping the song that is already loaded pauses or resumes it. */
internal fun PlaybackController.playOrToggle(songs: List<Song>, index: Int) {
    val song = songs.getOrNull(index) ?: return
    if (song.id == state.value.song?.id) togglePlayPause() else play(songs, index)
}

/** The loaded song and whether it plays, for highlighting rows. */
internal fun PlaybackController.activeSong(): Flow<Pair<String?, Boolean>> =
    state.map { it.song?.id to it.isPlaying }.distinctUntilChanged()

internal fun LibraryLists.withActive(active: Pair<String?, Boolean>) =
    copy(activeSongId = active.first, isActiveSongPlaying = active.second)
