package com.yourcompany.musicsocial.core.playback

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.yourcompany.musicsocial.core.common.coroutines.MainDispatcher
import com.yourcompany.musicsocial.core.common.logging.AppLogger
import com.yourcompany.musicsocial.core.common.logging.LogCategory
import com.yourcompany.musicsocial.core.domain.model.Song
import com.yourcompany.musicsocial.core.domain.playback.PlaybackController
import com.yourcompany.musicsocial.core.domain.playback.PlaybackQueue
import com.yourcompany.musicsocial.core.domain.playback.PlaybackState
import com.yourcompany.musicsocial.core.domain.playback.PlaybackState.Status
import com.yourcompany.musicsocial.core.domain.playback.RepeatMode
import com.yourcompany.musicsocial.core.domain.repository.MusicRepository
import com.yourcompany.musicsocial.core.domain.result.AppError
import com.yourcompany.musicsocial.core.domain.result.AppResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ExoPlayer-backed [PlaybackController].
 *
 * Everything runs on the main thread (ExoPlayer requires it): public methods hop there with
 * [scope], and player callbacks arrive there.
 *
 * Preview rules (provider terms): audio is streamed only, never saved (no disk cache is configured).
 * Playback continues in the background through [PlaybackService], which also provides the media
 * notification and lock-screen controls.
 */
@Singleton
internal class ExoPlaybackController @Inject constructor(
    @ApplicationContext private val context: Context,
    @MainDispatcher mainDispatcher: CoroutineDispatcher,
    private val musicRepository: MusicRepository,
    private val logger: AppLogger,
) : PlaybackController {

    private val scope = CoroutineScope(SupervisorJob() + mainDispatcher)
    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val _recentlyPlayed = MutableStateFlow<List<Song>>(emptyList())
    override val recentlyPlayed: StateFlow<List<Song>> = _recentlyPlayed.asStateFlow()

    private var exo: ExoPlayer? = null
    private var queue: PlaybackQueue? = null
    private var loadJob: Job? = null
    private var tickerJob: Job? = null
    private var shuffleEnabled = false
    private var repeatMode = RepeatMode.OFF

    /** True from choosing a song until its audio address is resolved and handed to the player. */
    private var resolving = false

    /** [PlaybackService] is running and publishing the session. */
    private var serviceStarted = false

    /**
     * The player as the media session sees it. ExoPlayer holds one song at a time (the queue lives
     * in [PlaybackQueue]), so next/previous from the notification, lock screen or headphones are
     * routed to this controller instead of the player's own one-item playlist.
     */
    internal fun sessionPlayer(): Player = object : ForwardingPlayer(player()) {
        override fun getAvailableCommands(): Player.Commands = super.getAvailableCommands().buildUpon()
            .addAll(Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_PREVIOUS, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            .build()

        override fun isCommandAvailable(command: Int): Boolean = availableCommands.contains(command)

        override fun seekToNext() = next()

        override fun seekToNextMediaItem() = next()

        override fun seekToPrevious() = previous()

        override fun seekToPreviousMediaItem() = previous()
    }

    internal fun onServiceDestroyed() {
        serviceStarted = false
    }

    /**
     * Starts the service with the first song. Starting a service is only allowed while the app is in
     * the foreground, which it is whenever a user picks a song; afterwards the service stays up.
     */
    private fun ensureServiceStarted() {
        if (serviceStarted) return
        try {
            context.startService(Intent(context, PlaybackService::class.java))
            serviceStarted = true
        } catch (e: IllegalStateException) {
            // Not allowed from the background; playback still works, only without system controls.
            logger.warn(LogCategory.Playback, "Could not start the playback service", e)
        }
    }

    override fun play(songs: List<Song>, startIndex: Int) {
        scope.launch {
            if (songs.isEmpty() || startIndex !in songs.indices) return@launch
            queue = PlaybackQueue(songs, startIndex, shuffleEnabled, repeatMode)
            loadCurrent()
        }
    }

    override fun togglePlayPause() {
        scope.launch {
            val current = _state.value
            when (current.status) {
                Status.PLAYING -> exo?.pause()
                Status.PAUSED -> if (current.error != null) loadCurrent() else exo?.play()
                Status.LOADING, Status.IDLE -> Unit
            }
        }
    }

    override fun pause() {
        scope.launch { exo?.pause() }
    }

    override fun next() {
        scope.launch {
            if (queue?.next(auto = false) != null) loadCurrent()
        }
    }

    override fun previous() {
        scope.launch {
            val q = queue ?: return@launch
            if ((exo?.currentPosition ?: 0) > RESTART_THRESHOLD_MS) {
                exo?.seekTo(0)
                updateProgress()
            } else {
                q.previous()
                loadCurrent()
            }
        }
    }

    override fun seekTo(positionMs: Long) {
        scope.launch {
            exo?.seekTo(positionMs.coerceAtLeast(0))
            updateProgress()
        }
    }

    override fun setShuffle(enabled: Boolean) {
        scope.launch {
            shuffleEnabled = enabled
            queue?.setShuffle(enabled)
            publishQueueFlags()
        }
    }

    override fun cycleRepeatMode() {
        scope.launch {
            repeatMode = when (repeatMode) {
                RepeatMode.OFF -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.OFF
            }
            queue?.repeatMode = repeatMode
            publishQueueFlags()
        }
    }

    override fun stop() {
        scope.launch {
            loadJob?.cancel()
            resolving = false
            exo?.stop()
            exo?.clearMediaItems()
            stopTicker()
            queue = null
            _state.value = PlaybackState(shuffle = shuffleEnabled, repeatMode = repeatMode)
        }
    }

    /** Starts the queue's current song: stop what is playing, resolve the audio address, then play. */
    private fun loadCurrent() {
        val song = queue?.current ?: return
        _recentlyPlayed.update { recent -> (listOf(song) + recent.filterNot { it.id == song.id }).take(RECENT_LIMIT) }
        loadJob?.cancel()
        resolving = true
        // Pause, but keep the current item until the next one is ready. An empty player makes Media3
        // treat the session as finished and stop the background service between two songs.
        exo?.pause()
        stopTicker()
        _state.update {
            it.copy(song = song, status = Status.LOADING, positionMs = 0, durationMs = 0, error = null)
        }
        publishQueueFlags()

        loadJob = scope.launch {
            // A URL fresh from this session's search can be used directly; otherwise ask the provider.
            val url = song.previewUrl ?: when (val result = musicRepository.getPreviewUrl(song.id)) {
                is AppResult.Success -> result.data
                is AppResult.Failure -> {
                    fail(result.error, "no preview address")
                    return@launch
                }
            }
            val player = player()
            ensureServiceStarted()
            // Title, artist and art are what the notification and lock screen show.
            val metadata = MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artistNames)
                .setAlbumTitle(song.albumTitle)
                .setArtworkUri(song.artworkUrl?.let(Uri::parse))
                .build()
            player.setMediaItem(MediaItem.Builder().setMediaId(song.id).setUri(url).setMediaMetadata(metadata).build())
            player.prepare()
            player.playWhenReady = true
            resolving = false
            syncStatus()
        }
    }

    private fun player(): ExoPlayer = exo ?: ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            /* handleAudioFocus = */ true,
        )
        .setHandleAudioBecomingNoisy(true)
        .build()
        .also {
            it.addListener(listener)
            exo = it
        }

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                onEnded()
            } else {
                updateProgress()
                syncStatus()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) startTicker() else stopTicker()
            syncStatus()
        }

        override fun onPlayerError(error: PlaybackException) {
            val mapped = when (error.errorCode) {
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> AppError.Network.Unavailable
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> AppError.Network.Timeout
                else -> AppError.Unknown
            }
            fail(mapped, "player error ${error.errorCodeName}")
        }
    }

    private fun onEnded() {
        val q = queue ?: return
        val player = exo ?: return
        when {
            q.repeatMode == RepeatMode.ONE -> {
                player.seekTo(0)
                player.play()
            }
            q.next(auto = true) != null -> loadCurrent()
            else -> {
                // End of the queue: rewind so pressing play starts the last song again.
                player.seekTo(0)
                player.pause()
                updateProgress()
                syncStatus()
            }
        }
    }

    private fun fail(error: AppError, detail: String) {
        logger.warn(LogCategory.Playback, "Playback failed ($detail): $error")
        resolving = false
        stopTicker()
        _state.update { it.copy(status = Status.PAUSED, error = error) }
    }

    private fun syncStatus() {
        val player = exo
        _state.update { current ->
            val status = when {
                current.song == null -> Status.IDLE
                resolving -> Status.LOADING
                player == null -> current.status
                player.playbackState == Player.STATE_BUFFERING && player.playWhenReady -> Status.LOADING
                player.isPlaying -> Status.PLAYING
                else -> Status.PAUSED
            }
            current.copy(status = status)
        }
    }

    private fun publishQueueFlags() {
        val q = queue
        _state.update {
            it.copy(
                shuffle = shuffleEnabled,
                repeatMode = repeatMode,
                hasNext = q?.hasNext ?: false,
                hasPrevious = q?.hasPrevious ?: false,
            )
        }
    }

    private fun updateProgress() {
        val player = exo ?: return
        val duration = player.duration.takeIf { it != C.TIME_UNSET && it > 0 }
        _state.update {
            it.copy(positionMs = player.currentPosition.coerceAtLeast(0), durationMs = duration ?: it.durationMs)
        }
    }

    private fun startTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = scope.launch {
            while (isActive) {
                updateProgress()
                delay(TICK_MS)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private companion object {
        const val TICK_MS = 250L
        const val RECENT_LIMIT = 12

        /** Past this point "previous" restarts the song instead of going back, like most players. */
        const val RESTART_THRESHOLD_MS = 3_000L
    }
}
