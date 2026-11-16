package com.yourcompany.musicsocial.feature.player.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourcompany.musicsocial.core.domain.model.LibraryKind
import com.yourcompany.musicsocial.core.domain.playback.PlaybackController
import com.yourcompany.musicsocial.core.domain.playback.PlaybackState
import com.yourcompany.musicsocial.core.domain.model.ConversationSummary
import com.yourcompany.musicsocial.core.domain.model.OutgoingMessage
import com.yourcompany.musicsocial.core.domain.model.Playlist
import com.yourcompany.musicsocial.core.domain.repository.ChatRepository
import java.util.UUID
import com.yourcompany.musicsocial.core.domain.repository.LibraryRepository
import com.yourcompany.musicsocial.core.domain.repository.PlaylistRepository
import com.yourcompany.musicsocial.core.domain.result.AppError
import com.yourcompany.musicsocial.core.domain.result.AppResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The "Add to playlist" flow on the Player. [playlists] null while loading. */
data class AddToPlaylistUi(
    val isSheetOpen: Boolean = false,
    val playlists: List<Playlist>? = null,
    val isCreateOpen: Boolean = false,
    val isCreating: Boolean = false,
    /** Title of the playlist the song was just added to, for a short confirmation. */
    val addedTo: String? = null,
    /** Name of the chat the song was just shared into, for a short confirmation. */
    val sharedWith: String? = null,
    val error: AppError? = null,
)

/** The "Share with" sheet. [conversations] null while loading. */
data class ShareUi(
    val isSheetOpen: Boolean = false,
    val conversations: List<ConversationSummary>? = null,
)

/**
 * Mirrors [PlaybackController] and forwards taps to it. The heart and save buttons write to the
 * user's library, which approved followers see on their profile.
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playback: PlaybackController,
    private val library: LibraryRepository,
    private val playlists: PlaylistRepository,
    private val chats: ChatRepository,
) : ViewModel() {
    val state: StateFlow<PlaybackState> = playback.state

    /** The signed-in user's liked and saved song ids. */
    val librarySongIds: StateFlow<Map<LibraryKind, Set<String>>> = library.mySongIds

    private val _libraryError = MutableStateFlow<AppError?>(null)
    val libraryError: StateFlow<AppError?> = _libraryError.asStateFlow()

    init {
        viewModelScope.launch { library.refreshMine() }
    }

    fun onTogglePlay() = playback.togglePlayPause()
    fun onNext() = playback.next()
    fun onPrevious() = playback.previous()
    fun onToggleShuffle() = playback.setShuffle(!playback.state.value.shuffle)
    fun onCycleRepeat() = playback.cycleRepeatMode()

    /** [fraction] is 0..1 of what is playing (the preview length, not the full song). */
    fun onSeek(fraction: Float) {
        val duration = playback.state.value.durationMs
        if (duration > 0) playback.seekTo((duration * fraction.coerceIn(0f, 1f)).toLong())
    }

    private val _playlistUi = MutableStateFlow(AddToPlaylistUi())
    val playlistUi: StateFlow<AddToPlaylistUi> = _playlistUi.asStateFlow()

    /** Opens the sheet and loads the user's playlists (fresh each time: they change on the profile). */
    fun onOpenAddToPlaylist() {
        if (playback.state.value.song == null) return
        _playlistUi.value = AddToPlaylistUi(isSheetOpen = true, playlists = null)
        viewModelScope.launch {
            when (val result = playlists.getMyPlaylists()) {
                is AppResult.Success -> _playlistUi.update { it.copy(playlists = result.data) }
                is AppResult.Failure -> _playlistUi.update { it.copy(isSheetOpen = false, error = result.error) }
            }
        }
    }

    fun onDismissPlaylistSheet() = _playlistUi.update { it.copy(isSheetOpen = false) }

    fun onPickPlaylist(playlist: Playlist) {
        val song = playback.state.value.song ?: return
        _playlistUi.update { it.copy(isSheetOpen = false) }
        viewModelScope.launch {
            when (val result = playlists.addSong(playlist.id, song)) {
                is AppResult.Success -> _playlistUi.update { it.copy(addedTo = playlist.title, error = null) }
                is AppResult.Failure -> _playlistUi.update { it.copy(error = result.error) }
            }
        }
    }

    fun onNewPlaylistFromSheet() = _playlistUi.update { it.copy(isSheetOpen = false, isCreateOpen = true) }

    fun onDismissCreatePlaylist() = _playlistUi.update { it.copy(isCreateOpen = false) }

    /** Creates the playlist and puts the playing song in it straight away. */
    fun onCreatePlaylist(title: String, isPublic: Boolean) {
        val song = playback.state.value.song ?: return
        if (_playlistUi.value.isCreating) return
        _playlistUi.update { it.copy(isCreating = true) }
        viewModelScope.launch {
            val created = playlists.createPlaylist(title, isPublic)
            val result = if (created is AppResult.Success) playlists.addSong(created.data.id, song) else created
            _playlistUi.update {
                when (result) {
                    is AppResult.Success -> it.copy(isCreating = false, isCreateOpen = false, addedTo = (created as AppResult.Success).data.title, error = null)
                    is AppResult.Failure -> it.copy(isCreating = false, error = result.error)
                }
            }
        }
    }

    /** The "Added to …" note was shown. */
    fun onPlaylistMessageShown() = _playlistUi.update { it.copy(addedTo = null, sharedWith = null) }

    // ---- share in chat ---------------------------------------------------------------------------

    private val _shareUi = MutableStateFlow(ShareUi())
    val shareUi: StateFlow<ShareUi> = _shareUi.asStateFlow()

    /** Opens the "Share with" sheet with the user's chats, most recent first. */
    fun onOpenShare() {
        if (playback.state.value.song == null) return
        _shareUi.value = ShareUi(isSheetOpen = true, conversations = null)
        viewModelScope.launch {
            when (val result = chats.getConversations()) {
                is AppResult.Success -> _shareUi.update { it.copy(conversations = result.data) }
                is AppResult.Failure -> {
                    _shareUi.update { it.copy(isSheetOpen = false) }
                    _playlistUi.update { it.copy(error = result.error) }
                }
            }
        }
    }

    fun onDismissShare() = _shareUi.update { it.copy(isSheetOpen = false) }

    /** Sends the playing song into [conversation]. One client id per tap, so a retry is never stored twice. */
    fun onShareTo(conversation: ConversationSummary) {
        val song = playback.state.value.song ?: return
        _shareUi.update { it.copy(isSheetOpen = false) }
        val name = conversation.title ?: conversation.otherUser?.displayName.orEmpty()
        viewModelScope.launch {
            when (val result = chats.sendMessage(conversation.id, OutgoingMessage.SongShare(song, comment = null), UUID.randomUUID().toString())) {
                is AppResult.Success -> _playlistUi.update { it.copy(sharedWith = name, error = null) }
                is AppResult.Failure -> _playlistUi.update { it.copy(error = result.error) }
            }
        }
    }

    fun onToggleLike() = toggle(LibraryKind.LIKED)

    fun onToggleSave() = toggle(LibraryKind.SAVED)

    private fun toggle(kind: LibraryKind) {
        val song = playback.state.value.song ?: return
        val inList = song.id in library.mySongIds.value[kind].orEmpty()
        _libraryError.value = null
        viewModelScope.launch {
            // The repository updates the heart at once and puts it back if the backend refuses.
            val result = if (inList) library.remove(kind, song.id) else library.add(kind, song)
            if (result is AppResult.Failure) _libraryError.value = result.error
        }
    }
}
