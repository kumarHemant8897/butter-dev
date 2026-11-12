package com.yourcompany.musicsocial.core.domain.playback

import com.yourcompany.musicsocial.core.domain.model.Song
import kotlin.random.Random

/**
 * Order in which songs play: current position, shuffle and repeat. Pure Kotlin so the rules are
 * unit-tested without a player.
 *
 * Shuffle keeps the current song first and shuffles only the rest, so turning it on never skips
 * or restarts what the user is hearing.
 */
class PlaybackQueue(
    private val songs: List<Song>,
    startIndex: Int,
    shuffle: Boolean = false,
    repeatMode: RepeatMode = RepeatMode.OFF,
    private val random: Random = Random.Default,
) {
    init {
        require(songs.isNotEmpty()) { "queue needs at least one song" }
        require(startIndex in songs.indices) { "startIndex $startIndex is outside 0..${songs.lastIndex}" }
    }

    /** Indices into [songs] in play order, and where we are in that order. */
    private var order: List<Int> = buildOrder(startIndex, shuffle)
    private var position: Int = order.indexOf(startIndex)

    var shuffle: Boolean = shuffle
        private set

    var repeatMode: RepeatMode = repeatMode

    val current: Song get() = songs[order[position]]

    val hasNext: Boolean get() = position < order.lastIndex || repeatMode == RepeatMode.ALL
    val hasPrevious: Boolean get() = position > 0 || repeatMode == RepeatMode.ALL

    /**
     * The song to play after the current one, or null when the queue is finished.
     * [auto] is true when the song ended by itself: with [RepeatMode.ONE] the same song comes back,
     * but a manual "next" always moves on.
     */
    fun next(auto: Boolean): Song? {
        if (auto && repeatMode == RepeatMode.ONE) return current
        position = when {
            position < order.lastIndex -> position + 1
            repeatMode == RepeatMode.ALL -> 0
            else -> return null
        }
        return current
    }

    /** The previous song; at the start of the queue it wraps only with [RepeatMode.ALL]. */
    fun previous(): Song {
        position = when {
            position > 0 -> position - 1
            repeatMode == RepeatMode.ALL -> order.lastIndex
            else -> 0
        }
        return current
    }

    fun setShuffle(enabled: Boolean) {
        if (enabled == shuffle) return
        val currentIndex = order[position]
        shuffle = enabled
        order = buildOrder(currentIndex, enabled)
        position = order.indexOf(currentIndex)
    }

    private fun buildOrder(firstIndex: Int, shuffled: Boolean): List<Int> =
        if (shuffled) {
            listOf(firstIndex) + songs.indices.filter { it != firstIndex }.shuffled(random)
        } else {
            songs.indices.toList()
        }
}
