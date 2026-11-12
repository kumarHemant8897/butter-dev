package com.yourcompany.musicsocial.core.domain.playback

import com.yourcompany.musicsocial.core.domain.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PlaybackQueueTest {

    private fun song(id: String) = Song(id, id, emptyList(), null, null, 0, null, null, null)
    private val songs = listOf(song("a"), song("b"), song("c"))

    @Test
    fun `plays in order and ends when repeat is off`() {
        val queue = PlaybackQueue(songs, startIndex = 0)

        assertEquals("a", queue.current.id)
        assertEquals("b", queue.next(auto = true)?.id)
        assertEquals("c", queue.next(auto = true)?.id)
        assertNull(queue.next(auto = true))
        assertEquals("c", queue.current.id)
    }

    @Test
    fun `starting in the middle keeps the rest of the list`() {
        val queue = PlaybackQueue(songs, startIndex = 1)

        assertEquals("b", queue.current.id)
        assertTrue(queue.hasPrevious)
        assertEquals("c", queue.next(auto = false)?.id)
        assertFalse(queue.hasNext)
    }

    @Test
    fun `repeat all wraps in both directions`() {
        val queue = PlaybackQueue(songs, startIndex = 2, repeatMode = RepeatMode.ALL)

        assertTrue(queue.hasNext)
        assertEquals("a", queue.next(auto = true)?.id)
        assertEquals("c", queue.previous().id)
    }

    @Test
    fun `repeat one replays when a song ends but a manual next still moves on`() {
        val queue = PlaybackQueue(songs, startIndex = 0, repeatMode = RepeatMode.ONE)

        assertEquals("a", queue.next(auto = true)?.id)
        assertEquals("a", queue.next(auto = true)?.id)
        assertEquals("b", queue.next(auto = false)?.id)
    }

    @Test
    fun `previous stops at the first song when repeat is off`() {
        val queue = PlaybackQueue(songs, startIndex = 0)

        assertEquals("a", queue.previous().id)
        assertFalse(queue.hasPrevious)
    }

    @Test
    fun `turning shuffle on keeps the current song and plays every song once`() {
        val many = (0 until 20).map { song("s$it") }
        val queue = PlaybackQueue(many, startIndex = 7, random = Random(42))

        queue.setShuffle(true)

        assertEquals("s7", queue.current.id)
        val played = mutableListOf(queue.current.id)
        while (true) played += queue.next(auto = true)?.id ?: break
        assertEquals(20, played.size)
        assertEquals(many.map { it.id }.toSet(), played.toSet())
        assertFalse("a shuffled order should differ from the plain one", played == many.map { it.id }.drop(7) + many.map { it.id }.take(7))
    }

    @Test
    fun `turning shuffle off returns to list order from the current song`() {
        val queue = PlaybackQueue(songs, startIndex = 1, shuffle = true, random = Random(1))
        val current = queue.current.id

        queue.setShuffle(false)

        assertEquals(current, queue.current.id)
        val expectedNext = songs.getOrNull(songs.indexOfFirst { it.id == current } + 1)?.id
        assertEquals(expectedNext, queue.next(auto = false)?.id)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `an empty queue is rejected`() {
        PlaybackQueue(emptyList(), startIndex = 0)
    }
}
