package com.yourcompany.musicsocial.core.ui.format

import java.util.Locale

/** 215_000 ms -> "3:35"; 3_725_000 ms -> "1:02:05". Negative input is treated as zero. */
fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs.coerceAtLeast(0) / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
    }
}

/** Compact social counts: 999 -> "999", 1_250 -> "1.2k", 3_400_000 -> "3.4m". Rounds down so counts never overstate. */
fun formatCompactCount(count: Int): String {
    val value = count.coerceAtLeast(0)
    return when {
        value < 1_000 -> value.toString()
        value < 1_000_000 -> compact(value, 1_000, "k")
        else -> compact(value, 1_000_000, "m")
    }
}

private fun compact(value: Int, unit: Int, suffix: String): String {
    val tenths = (value.toLong() * 10 / unit)
    val whole = tenths / 10
    val fraction = tenths % 10
    return if (fraction == 0L || whole >= 100) "$whole$suffix" else "$whole.$fraction$suffix"
}
