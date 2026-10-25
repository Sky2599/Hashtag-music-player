package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DeepPurpleDark
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElectricVioletLight
import com.example.ui.util.TactileHaptics
import kotlin.math.abs

/**
 * Interactive Waveform Seekbar that renders the track's real decoded PCM RMS amplitude envelope
 * across 96–120 vertical bars, allowing both tap-to-seek and smooth horizontal drag scrubbing.
 */
@Composable
fun WaveformSeekBar(
    progress: Float,
    waveformPeaks: FloatArray,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 36.dp,
    activeColor: Color = ElectricVioletLight,
    activeBottomColor: Color = ElectricViolet,
    inactiveColor: Color = DeepPurpleDark
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val view = LocalView.current
    var lastHapticBucket by remember { mutableIntStateOf(-1) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val width = size.width.toFloat().coerceAtLeast(1f)
                    val newProgress = (offset.x / width).coerceIn(0f, 1f)
                    TactileHaptics.lightImpact(view)
                    onSeekChange(newProgress)
                    onSeekFinished()
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        val width = size.width.toFloat().coerceAtLeast(1f)
                        val newProgress = (offset.x / width).coerceIn(0f, 1f)
                        lastHapticBucket = (newProgress * 48).toInt()
                        TactileHaptics.selectionClick(view)
                        onSeekChange(newProgress)
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val width = size.width.toFloat().coerceAtLeast(1f)
                        val newProgress = (change.position.x / width).coerceIn(0f, 1f)
                        val bucket = (newProgress * 48).toInt()
                        if (bucket != lastHapticBucket) {
                            lastHapticBucket = bucket
                            TactileHaptics.selectionClick(view)
                        }
                        onSeekChange(newProgress)
                    },
                    onDragEnd = {
                        onSeekFinished()
                    },
                    onDragCancel = {
                        onSeekFinished()
                    }
                )
            }
    ) {
        val barCount = if (waveformPeaks.isNotEmpty()) waveformPeaks.size.coerceAtMost(96) else 72
        val totalWidth = size.width
        val canvasHeight = size.height
        val centerY = canvasHeight * 0.5f

        val slotWidth = totalWidth / barCount.toFloat()
        val barWidth = (slotWidth * 0.64f).coerceAtLeast(2.dp.toPx())
        val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)

        val playheadX = clampedProgress * totalWidth

        for (i in 0 until barCount) {
            val peakIdx = if (waveformPeaks.isNotEmpty()) {
                ((i.toFloat() / barCount) * waveformPeaks.size).toInt().coerceIn(0, waveformPeaks.lastIndex)
            } else {
                i
            }
            val rawPeak = if (waveformPeaks.isNotEmpty()) {
                waveformPeaks[peakIdx].coerceIn(0.12f, 1.0f)
            } else {
                0.25f
            }

            val barCenterX = i * slotWidth + slotWidth * 0.5f
            val isPlayed = barCenterX <= playheadX
            val distFromPlayhead = abs(barCenterX - playheadX) / totalWidth
            val playheadBoost = if (distFromPlayhead < 0.025f) {
                1.0f + (1.0f - distFromPlayhead / 0.025f) * 0.14f
            } else {
                1.0f
            }

            val barHeight = (rawPeak * playheadBoost * canvasHeight * 0.86f)
                .coerceIn(4.dp.toPx(), canvasHeight * 0.94f)
            val topY = centerY - barHeight * 0.5f

            if (isPlayed) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(activeColor, activeBottomColor),
                        startY = topY,
                        endY = topY + barHeight
                    ),
                    topLeft = Offset(barCenterX - barWidth * 0.5f, topY),
                    size = Size(barWidth, barHeight),
                    cornerRadius = cornerRadius
                )
            } else {
                drawRoundRect(
                    color = inactiveColor.copy(alpha = 0.85f),
                    topLeft = Offset(barCenterX - barWidth * 0.5f, topY),
                    size = Size(barWidth, barHeight),
                    cornerRadius = cornerRadius
                )
                // Subtle inner highlight on unplayed bars for readability on dark themes
                drawRoundRect(
                    color = activeColor.copy(alpha = 0.22f),
                    topLeft = Offset(barCenterX - barWidth * 0.5f, topY),
                    size = Size(barWidth, barHeight),
                    cornerRadius = cornerRadius
                )
            }
        }

        //Glowing Playhead Pill Indicator
        val pillWidth = 3.dp.toPx()
        val pillHeight = canvasHeight * 0.96f
        drawRoundRect(
            color = activeColor,
            topLeft = Offset((playheadX - pillWidth * 0.5f).coerceIn(0f, totalWidth - pillWidth), (canvasHeight - pillHeight) * 0.5f),
            size = Size(pillWidth, pillHeight),
            cornerRadius = CornerRadius(pillWidth, pillWidth)
        )
    }
}
