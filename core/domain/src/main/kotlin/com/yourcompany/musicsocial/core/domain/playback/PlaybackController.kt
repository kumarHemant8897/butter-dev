package com.yourcompany.musicsocial.core.domain.playback

import com.yourcompany.musicsocial.core.domain.model.Song
import com.yourcompany.musicsocial.core.domain.result.AppError
import kotlinx.coroutines.flow.StateFlow

enum class RepeatMode { OFF, ALL, ONE }

/** What the player is doing, as the UI needs to show it. */
data class PlaybackState(
    /** The song that is loaded, or null when nothing was started yet. */
    val song: Song? = null,
    val status: Status = Status.IDLE,
    val positionMs: Long = 0,
    /** Length of what is actually playing (a 30-second preview is 30000, not the song's full length). */
    val durationMs: Long = 0,
    val shuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    /** The last attempt to load or play failed. Cleared when another song starts. */
    val error: AppError? = null,
) {
    enum class Status { IDLE, LOADING, PLAYING, PAUSED }

    val isPlaying: Boolean get() = status == Status.PLAYING

    /** Resolving the audio address or buffering. */
    val isLoading: Boolean get() = status == Status.LOADING

    val progress: Float get() = if (durationMs <= 0) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
}

/**
 * The one player for the whole app. Screens only describe what they want ("play these songs from
 * this one"); they never see ExoPlayer. That keeps screens testable and lets the implementation
 * change (for example to a MediaSession service for full licensed songs) without touching them.
 *
 * Calls may come from any thread; the implementation moves to the main thread itself.
 */
interface PlaybackController {
    val state: StateFlow<PlaybackState>

    /** Songs started in this app session, most recent first, without duplicates. Not saved across restarts. */
    val recentlyPlayed: StateFlow<List<Song>>

    /** Replaces the queue with [songs] and starts playing the one at [startIndex]. */
    fun play(songs: List<Song>, startIndex: Int)

    fun togglePlayPause()

    fun pause()

    fun next()

    /** Restarts the song if it has been playing for a few seconds, otherwise goes to the previous one. */
    fun previous()

    fun seekTo(positionMs: Long)

    fun setShuffle(enabled: Boolean)

    /** OFF -> ALL -> ONE -> OFF. */
    fun cycleRepeatMode()

    /** Stops playback and forgets the queue. */
    fun stop()
}
