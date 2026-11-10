package com.yourcompany.musicsocial.core.ui.format

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {

    @Test
    fun `durations use minutes and zero-padded seconds`() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("0:09", formatDuration(9_999))
        assertEquals("3:35", formatDuration(215_000))
        assertEquals("1:02:05", formatDuration(3_725_000))
        assertEquals("0:00", formatDuration(-5_000))
    }

    @Test
    fun `counts are compacted and never rounded up`() {
        assertEquals("999", formatCompactCount(999))
        assertEquals("1k", formatCompactCount(1_000))
        assertEquals("1.2k", formatCompactCount(1_299))
        assertEquals("12.5k", formatCompactCount(12_599))
        // From 100K upwards the decimal is dropped to keep labels short.
        assertEquals("999k", formatCompactCount(999_999))
        assertEquals("3.4m", formatCompactCount(3_499_999))
        assertEquals("0", formatCompactCount(-3))
    }
}
