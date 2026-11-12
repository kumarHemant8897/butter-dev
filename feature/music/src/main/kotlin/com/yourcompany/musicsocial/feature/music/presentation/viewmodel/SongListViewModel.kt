package com.yourcompany.musicsocial.feature.music.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourcompany.musicsocial.core.domain.model.AuthState
import com.yourcompany.musicsocial.core.domain.model.ChartGenre
import com.yourcompany.musicsocial.core.domain.repository.AuthRepository
import com.yourcompany.musicsocial.core.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.first
import com.yourcompany.musicsocial.core.domain.model.Song
import com.yourcompany.musicsocial.core.domain.playback.PlaybackController
import com.yourcompany.musicsocial.core.domain.repository.MusicRepository
import com.yourcompany.musicsocial.core.domain.result.AppError
import com.yourcompany.musicsocial.core.domain.result.AppResult
import com.yourcompany.musicsocial.core.navigation.SongListKind
import com.yourcompany.musicsocial.feature.music.presentation.state.SongListContent
import com.yourcompany.musicsocial.feature.music.presentation.state.SongListType
import com.yourcompany.musicsocial.feature.music.presentation.state.SongListUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

/**
 * One list of songs: the full India chart, a genre chart or an artist's songs. The arguments of
 * [com.yourcompany.musicsocial.core.navigation.SongListRoute] are read from [SavedStateHandle] by key.
 */
@HiltViewModel
class SongListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    private val authRepository: AuthRepository,
    private val playbackController: PlaybackController,
) : ViewModel() {

    private val kind: String = savedStateHandle.get<String>(ARG_KIND).orEmpty()
    private val id: String? = savedStateHandle.get<String>(ARG_ID)

    private val _uiState = MutableStateFlow(
        SongListUiState(
            type = when (kind) {
                SongListKind.ARTIST -> SongListType.ARTIST
                SongListKind.GENRE -> SongListType.GENRE
                SongListKind.PLAYLIST -> SongListType.PLAYLIST
                else -> SongListType.CHART
            },
            title = savedStateHandle.get<String>(ARG_TITLE).orEmpty(),
        ),
    )
    val uiState: StateFlow<SongListUiState> = _uiState.asStateFlow()

    /** Randomness for shuffle; replaceable in tests. */
    internal var random: Random = Random.Default

    private var loadJob: Job? = null

    init {
        load()
        viewModelScope.launch {
            playbackController.state
                .map { it.song?.id to it.isPlaying }
                .distinctUntilChanged()
                .collect { (songId, playing) -> _uiState.update { it.copy(activeSongId = songId, isActiveSongPlaying = playing) } }
        }
    }

    fun onRetry() = load()

    /** Plays the list from the tapped song; tapping the loaded song pauses or resumes it. */
    fun onSongClick(index: Int) {
        val songs = _uiState.value.songs
        val song = songs.getOrNull(index) ?: return
        if (song.id == playbackController.state.value.song?.id) {
            playbackController.togglePlayPause()
        } else {
            playbackController.play(songs, index)
        }
    }

    fun onPlayAll() {
        val songs = _uiState.value.songs
        if (songs.isEmpty()) return
        playbackController.setShuffle(false)
        playbackController.play(songs, 0)
    }

    fun onShuffle() {
        val songs = _uiState.value.songs
        if (songs.isEmpty()) return
        playbackController.setShuffle(true)
        playbackController.play(songs, random.nextInt(songs.size))
    }

    private fun load() {
        loadJob?.cancel()
        _uiState.update { it.copy(content = SongListContent.Loading) }
        loadJob = viewModelScope.launch {
            when (_uiState.value.type) {
                SongListType.CHART -> showSongs(musicRepository.getTopSongs(LIST_SIZE))
                SongListType.GENRE -> {
                    val genre = ChartGenre.fromId(id)
                    if (genre == null) showError(AppError.NotFound) else showSongs(musicRepository.getGenreChart(genre, LIST_SIZE))
                }
                SongListType.PLAYLIST -> {
                    val playlistId = id
                    if (playlistId == null) showError(AppError.NotFound) else loadPlaylist(playlistId)
                }
                SongListType.ARTIST -> {
                    val artistId = id
                    if (artistId == null) {
                        showError(AppError.NotFound)
                    } else {
                        when (val result = musicRepository.getArtist(artistId)) {
                            is AppResult.Failure -> showError(result.error)
                            is AppResult.Success -> {
                                val artist = result.data.artist
                                _uiState.update {
                                    it.copy(
                                        title = artist.name,
                                        subtitle = artist.genre,
                                        artworkUrl = artist.imageUrl,
                                        content = SongListContent.Songs(result.data.songs),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ---- playlist owner actions ------------------------------------------------------------------

    fun onRequestRemove(index: Int) {
        val state = _uiState.value
        if (!state.isOwner) return
        _uiState.update { it.copy(pendingRemoval = state.songs.getOrNull(index), actionError = null) }
    }

    fun onDismissRemove() = _uiState.update { it.copy(pendingRemoval = null) }

    /** Removes the song at once; puts it back where it was if the backend refuses. */
    fun onConfirmRemove() {
        val playlistId = id ?: return
        val song = _uiState.value.pendingRemoval ?: return
        val before = _uiState.value.songs
        _uiState.update { it.copy(pendingRemoval = null, content = SongListContent.Songs(before - song)) }
        viewModelScope.launch {
            val result = playlistRepository.removeSong(playlistId, song.id)
            if (result is AppResult.Failure) {
                _uiState.update { it.copy(content = SongListContent.Songs(before), actionError = result.error) }
            } else {
                _uiState.update { it.copy(artworkUrl = it.songs.firstNotNullOfOrNull { s -> s.artworkUrl }) }
            }
        }
    }

    fun onRequestDelete() = _uiState.update { if (it.isOwner) it.copy(isConfirmingDelete = true, actionError = null) else it }

    fun onDismissDelete() = _uiState.update { it.copy(isConfirmingDelete = false) }

    fun onConfirmDelete() {
        val playlistId = id ?: return
        _uiState.update { it.copy(isConfirmingDelete = false) }
        viewModelScope.launch {
            when (val result = playlistRepository.deletePlaylist(playlistId)) {
                is AppResult.Success -> _uiState.update { it.copy(isDeleted = true) }
                is AppResult.Failure -> _uiState.update { it.copy(actionError = result.error) }
            }
        }
    }

    private suspend fun loadPlaylist(playlistId: String) {
        when (val result = playlistRepository.getPlaylist(playlistId)) {
            is AppResult.Failure -> showError(result.error)
            is AppResult.Success -> {
                val me = (authRepository.authState.first { it !is AuthState.Initializing } as? AuthState.SignedIn)?.userId
                val playlist = result.data.playlist
                _uiState.update {
                    it.copy(
                        title = playlist.title,
                        artworkUrl = playlist.coverUrl ?: result.data.songs.firstNotNullOfOrNull { s -> s.artworkUrl },
                        isOwner = me != null && me == playlist.ownerId,
                        isPublic = playlist.isPublic,
                        content = SongListContent.Songs(result.data.songs),
                    )
                }
            }
        }
    }

    private fun showSongs(result: AppResult<List<Song>>) {
        when (result) {
            is AppResult.Failure -> showError(result.error)
            is AppResult.Success -> _uiState.update {
                it.copy(
                    artworkUrl = result.data.firstNotNullOfOrNull { song -> song.artworkUrl },
                    content = SongListContent.Songs(result.data),
                )
            }
        }
    }

    private fun showError(error: AppError) {
        _uiState.update { it.copy(content = SongListContent.Error(error)) }
    }

    companion object {
        /** Keys match the property names of SongListRoute, which is how Navigation stores them. */
        const val ARG_KIND = "kind"
        const val ARG_ID = "id"
        const val ARG_TITLE = "title"

        /** The chart feeds offer 25 songs; that is the whole list. */
        private const val LIST_SIZE = 25
    }
}
