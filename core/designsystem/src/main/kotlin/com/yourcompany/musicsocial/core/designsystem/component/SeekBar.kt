package com.yourcompany.musicsocial.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yourcompany.musicsocial.core.designsystem.theme.ButterTheme

/**
 * Thin playback bar: tap anywhere to jump there, or drag the thumb. While dragging, the bar
 * follows the finger and [onSeek] is called once on release, so the player is not asked to seek
 * on every pixel. Screen readers can set the position too (setProgress).
 *
 * [progress] is 0..1. The touch area is taller than the visible line so it is easy to hit.
 */
@Composable
fun SeekBar(
    progress: Float,
    onSeek: (Float) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    touchHeight: Dp = 24.dp,
    lineWidth: Dp = 3.dp,
    thumbRadius: Dp = 5.dp,
) {
    val currentOnSeek by rememberUpdatedState(onSeek)
    var widthPx by remember { mutableIntStateOf(1) }
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    val shown = (if (dragging) dragFraction else progress).coerceIn(0f, 1f)

    val active = MaterialTheme.colorScheme.primary
    val track = ButterTheme.colors.divider

    Canvas(
        modifier
            .fillMaxWidth()
            .height(touchHeight)
            .onSizeChanged { widthPx = it.width.coerceAtLeast(1) }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset -> currentOnSeek((offset.x / widthPx).coerceIn(0f, 1f)) }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragging = true
                        dragFraction = (offset.x / widthPx).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        currentOnSeek(dragFraction)
                        dragging = false
                    },
                    onDragCancel = { dragging = false },
                ) { change, _ ->
                    change.consume()
                    dragFraction = (change.position.x / widthPx).coerceIn(0f, 1f)
                }
            }
            .semantics {
                this.contentDescription = contentDescription
                progressBarRangeInfo = ProgressBarRangeInfo(shown, 0f..1f)
                if (enabled) {
                    setProgress { target ->
                        currentOnSeek(target.coerceIn(0f, 1f))
                        true
                    }
                }
            },
    ) {
        val y = size.height / 2
        val stroke = lineWidth.toPx()
        val radius = thumbRadius.toPx() * if (dragging) 1.4f else 1f
        // Keep the thumb fully visible at both ends.
        val start = radius
        val end = size.width - radius
        val x = start + (end - start) * shown
        drawLine(track, Offset(start, y), Offset(end, y), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(active, Offset(start, y), Offset(x, y), strokeWidth = stroke, cap = StrokeCap.Round)
        if (enabled) drawCircle(active, radius = radius, center = Offset(x, y))
    }
}
