package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettings
import com.example.theme.DynamicPaletteEngine
import com.example.theme.VisualizerSpectrumPalette
import com.example.ui.theme.AppTheme

private val FlBandLabels = listOf(
    "SUB\n30Hz", "BASS\n100Hz", "L-MID\n350Hz", "MID\n1kHz", "H-MID\n3kHz", "PRES\n6kHz", "BRILL\n14kHz"
)

/**
 * Visualizer Customization Engine Component:
 * Renders real-time reactive audio FFT spectrum across 5 selectable render styles:
 * 1. FL Studio DAW LED segmented bars with studio grid & floating needles
 * 2. Neon Solid continuous luminous gradient bars
 * 3. Analog Oscilloscope continuous waveform curve
 * 4. Symmetric Mirror center-expanding bipolar bars
 * 5. Dot Matrix LED matrix frequency display
 *
 * Supports 5 color schemes (Album Adaptive, Citiview Neon, Sunset Blaze, Emerald Glow, Ice Pearl),
 * dynamic sensitivity scaling (0.5x .. 2.0x), and switchable 7, 14, or 21 band resolutions.
 */
@Composable
fun VisualizerView(
    fftValues: FloatArray = FloatArray(21) { 0.05f },
    fftValuesProvider: (() -> FloatArray)? = null,
    modifier: Modifier = Modifier,
    height: Dp? = 110.dp,
    style: String = AppSettings.VIS_STYLE_FL_BARS,
    colorScheme: String = AppSettings.VIS_COLOR_ALBUM_ADAPTIVE,
    sensitivity: Float = 1.0f,
    barCount: Int = 21,
    showPeakCaps: Boolean = true,
    showLabels: Boolean = true,
    backgroundColor: Color = Color(0xFF0D0C14),
    spectrumPalette: VisualizerSpectrumPalette? = null,
    onClick: (() -> Unit)? = null
) {
    val currentTheme = AppTheme.colors

    val internalFft by androidx.compose.runtime.produceState(
        initialValue = fftValuesProvider?.invoke() ?: fftValues,
        key1 = fftValuesProvider
    ) {
        if (fftValuesProvider != null) {
            while (true) {
                androidx.compose.runtime.withFrameMillis {
                    value = fftValuesProvider.invoke()
                }
            }
        }
    }

    val effectiveFft = if (fftValuesProvider != null) internalFft else fftValues

    // Resolve color scheme palette
    val (topBandColors, bottomBandColors) = remember(colorScheme, currentTheme, spectrumPalette) {
        when (colorScheme) {
            AppSettings.VIS_COLOR_CITIVIEW -> {
                val tops = listOf(
                    Color(0xFF00E5FF), // SUB: Neon Cyan
                    Color(0xFF06B6D4), // BASS: Cyan
                    Color(0xFF14B8A6), // L-MID: Teal
                    Color(0xFF38BDF8), // MID: Sky Blue
                    Color(0xFF6366F1), // H-MID: Indigo
                    Color(0xFF8B5CF6), // PRES: Violet
                    Color(0xFFA855F7)  // BRILL: Purple
                )
                val bottoms = tops.map { it.copy(alpha = 0.22f) }
                tops to bottoms
            }
            AppSettings.VIS_COLOR_SUNSET -> {
                val tops = listOf(
                    Color(0xFFFF5722), // SUB: Amber-Red
                    Color(0xFFFF9800), // BASS: Orange
                    Color(0xFFFFB700), // L-MID: Warm Amber
                    Color(0xFFFFC107), // MID: Gold
                    Color(0xFFFF7043), // H-MID: Coral
                    Color(0xFFF4511E), // PRES: Crimson
                    Color(0xFFE53935)  // BRILL: Red
                )
                val bottoms = tops.map { it.copy(alpha = 0.22f) }
                tops to bottoms
            }
            AppSettings.VIS_COLOR_EMERALD -> {
                val tops = listOf(
                    Color(0xFF84CC16), // SUB: Electric Lime
                    Color(0xFF22C55E), // BASS: Spring Green
                    Color(0xFF10B981), // L-MID: Emerald
                    Color(0xFF059669), // MID: Deep Mint
                    Color(0xFF14B8A6), // H-MID: Teal Green
                    Color(0xFF34D399), // PRES: Mint
                    Color(0xFF6EE7B7)  // BRILL: Seafoam
                )
                val bottoms = tops.map { it.copy(alpha = 0.22f) }
                tops to bottoms
            }
            AppSettings.VIS_COLOR_ICE -> {
                val tops = listOf(
                    Color(0xFFF8FAFC), // SUB: Pure White
                    Color(0xFFE2E8F0), // BASS: Platinum
                    Color(0xFFCBD5E1), // L-MID: Silver
                    Color(0xFF94A3B8), // MID: Slate
                    Color(0xFF38BDF8), // H-MID: Ice Blue
                    Color(0xFF7DD3FC), // PRES: Sky Frost
                    Color(0xFFE0F2FE)  // BRILL: Glacier White
                )
                val bottoms = tops.map { it.copy(alpha = 0.25f) }
                tops to bottoms
            }
            else -> {
                val activePalette = spectrumPalette ?: DynamicPaletteEngine.generateSpectrumColors(currentTheme)
                activePalette.bandColors to activePalette.bandBottomColors
            }
        }
    }

    // Resample/map fftValues to requested barCount (7, 14, 21) & apply sensitivity
    val displayValues = remember(effectiveFft, barCount, sensitivity) {
        val safeSens = sensitivity.coerceIn(0.5f, 2.5f)
        val source = if (effectiveFft.isNotEmpty()) effectiveFft else FloatArray(21) { 0.05f }
        when (barCount) {
            7 -> {
                FloatArray(7) { i ->
                    val start = i * 3
                    var max = 0.04f
                    for (k in 0 until 3) {
                        val v = source.getOrElse(start + k) { 0.04f }
                        if (v > max) max = v
                    }
                    (max * safeSens).coerceIn(0.04f, 1.0f)
                }
            }
            14 -> {
                FloatArray(14) { i ->
                    val srcIdx = (i * 20) / 13
                    val v = source.getOrElse(srcIdx) { 0.04f }
                    (v * safeSens).coerceIn(0.04f, 1.0f)
                }
            }
            else -> {
                // Default 21 bands
                FloatArray(source.size) { i ->
                    (source[i] * safeSens).coerceIn(0.04f, 1.0f)
                }
            }
        }
    }

    val peakCaps = remember { mutableStateListOf<Float>() }
    val peakHoldFrames = remember { mutableStateListOf<Int>() }

    // Physical gravity falloff & peak hold ballistics
    LaunchedEffect(displayValues) {
        while (peakCaps.size < displayValues.size) {
            peakCaps.add(0.04f)
            peakHoldFrames.add(0)
        }
        while (peakCaps.size > displayValues.size) {
            peakCaps.removeAt(peakCaps.lastIndex)
            peakHoldFrames.removeAt(peakHoldFrames.lastIndex)
        }
        for (i in displayValues.indices) {
            val currentVal = displayValues[i].coerceIn(0.04f, 1.0f)
            val currentPeak = peakCaps.getOrElse(i) { 0.04f }
            val currentHold = peakHoldFrames.getOrElse(i) { 0 }

            if (currentVal >= currentPeak) {
                peakCaps[i] = currentVal
                peakHoldFrames[i] = if (i < 6) 1 else 2 // Snappy peak release on bass so peaks don't linger at top
            } else {
                if (currentHold > 0) {
                    peakHoldFrames[i] = currentHold - 1
                } else {
                    val falloff = if (i < 6) 0.78f else 0.85f // Responsive gravity falloff on lower frequencies
                    peakCaps[i] = (currentPeak * falloff).coerceAtLeast(currentVal)
                }
            }
        }
    }

    val baseModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .background(backgroundColor)
            .padding(top = 8.dp, bottom = 6.dp, start = 8.dp, end = 8.dp)
            .testTag("fl_studio_visualizer_container")
    } else {
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(top = 8.dp, bottom = 6.dp, start = 8.dp, end = 8.dp)
            .testTag("fl_studio_visualizer_container")
    }

    val containerModifier = if (height == null) {
        baseModifier.fillMaxHeight()
    } else {
        baseModifier
    }

    Column(modifier = containerModifier) {
        val canvasModifier = if (height != null) {
            Modifier
                .fillMaxWidth()
                .height(height)
                .testTag("audio_visualizer_canvas")
        } else {
            Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
                .testTag("audio_visualizer_canvas")
        }

        Canvas(modifier = canvasModifier) {
            val count = displayValues.size.coerceAtLeast(1)
            val canvasW = size.width
            val canvasH = size.height

            when (style) {
                AppSettings.VIS_STYLE_WAVE_LINE -> {
                    // STYLE 3: Analog Oscilloscope continuous waveform curve
                    val gridColor = Color(0xFF1E1C2B)
                    val gridLevels = listOf(0.25f, 0.50f, 0.75f)
                    for (level in gridLevels) {
                        val lineY = canvasH * level
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, lineY),
                            end = Offset(canvasW, lineY),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    val path = Path()
                    val fillPath = Path()
                    val stepX = canvasW / (count - 1).coerceAtLeast(1)

                    val points = ArrayList<Offset>(count)
                    for (i in 0 until count) {
                        val amp = displayValues[i].coerceIn(0.04f, 1.0f)
                        val x = i * stepX
                        val y = canvasH - (canvasH - 8.dp.toPx()) * amp
                        points.add(Offset(x, y))
                    }

                    if (points.isNotEmpty()) {
                        path.moveTo(points[0].x, points[0].y)
                        fillPath.moveTo(points[0].x, canvasH)
                        fillPath.lineTo(points[0].x, points[0].y)

                        for (i in 0 until points.size - 1) {
                            val p0 = points[i]
                            val p1 = points[i + 1]
                            val midX = (p0.x + p1.x) / 2f
                            val midY = (p0.y + p1.y) / 2f
                            path.quadraticTo(p0.x, p0.y, midX, midY)
                            fillPath.quadraticTo(p0.x, p0.y, midX, midY)
                        }
                        path.lineTo(points.last().x, points.last().y)
                        fillPath.lineTo(points.last().x, points.last().y)
                        fillPath.lineTo(points.last().x, canvasH)
                        fillPath.close()

                        // Draw glowing area fill beneath oscilloscope curve
                        val primaryTop = topBandColors.firstOrNull() ?: Color(0xFF38BDF8)
                        val primaryMid = topBandColors.getOrElse(3) { Color(0xFF8B5CF6) }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(primaryTop.copy(alpha = 0.35f), primaryMid.copy(alpha = 0.05f), Color.Transparent),
                                startY = 0f,
                                endY = canvasH
                            )
                        )

                        // Outer luminous stroke glow
                        drawPath(
                            path = path,
                            brush = Brush.horizontalGradient(topBandColors),
                            style = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Sharp core stroke
                        drawPath(
                            path = path,
                            color = Color.White.copy(alpha = 0.85f),
                            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Floating peak points along the curve
                        if (showPeakCaps) {
                            for (i in 0 until count) {
                                val pAmp = peakCaps.getOrElse(i) { displayValues[i] }
                                val pY = (canvasH - (canvasH - 8.dp.toPx()) * pAmp).coerceAtLeast(2.dp.toPx())
                                val pX = points[i].x
                                drawCircle(
                                    color = topBandColors.getOrElse((i * topBandColors.size) / count) { Color.White },
                                    radius = 3.dp.toPx(),
                                    center = Offset(pX, pY)
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 1.5.dp.toPx(),
                                    center = Offset(pX, pY)
                                )
                            }
                        }
                    }
                }

                AppSettings.VIS_STYLE_SYMMETRIC -> {
                    // STYLE 4: Center-Expanding Symmetric Mirror Bars
                    val centerY = canvasH / 2f
                    drawLine(
                        color = Color(0xFF2A2838),
                        start = Offset(0f, centerY),
                        end = Offset(canvasW, centerY),
                        strokeWidth = 1.5.dp.toPx()
                    )

                    val spacing = 3.dp.toPx()
                    val totalSpacing = spacing * (count + 1)
                    val barWidth = ((canvasW - totalSpacing) / count).coerceAtLeast(2.dp.toPx())

                    for (i in displayValues.indices) {
                        val amp = displayValues[i].coerceIn(0.04f, 1.0f)
                        val halfH = ((centerY - 4.dp.toPx()) * amp).coerceAtLeast(2.dp.toPx())
                        val x = spacing + (i * (barWidth + spacing))

                        val zoneIndex = ((i * topBandColors.size) / count).coerceIn(0, topBandColors.lastIndex)
                        val topColor = topBandColors[zoneIndex]
                        val bottomColor = bottomBandColors.getOrElse(zoneIndex) { topColor.copy(alpha = 0.25f) }

                        // Draw upward half
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(topColor, bottomColor),
                                startY = centerY - halfH,
                                endY = centerY
                            ),
                            topLeft = Offset(x, centerY - halfH),
                            size = Size(barWidth, halfH),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )

                        // Draw downward mirrored half
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(bottomColor, topColor),
                                startY = centerY,
                                endY = centerY + halfH
                            ),
                            topLeft = Offset(x, centerY),
                            size = Size(barWidth, halfH),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )

                        // Mirrored peak caps
                        if (showPeakCaps) {
                            val peakAmp = peakCaps.getOrElse(i) { amp }.coerceIn(0.04f, 1.0f)
                            val peakHalfH = ((centerY - 4.dp.toPx()) * peakAmp).coerceAtLeast(2.dp.toPx())
                            // Top peak cap
                            drawRoundRect(
                                color = Color.White,
                                topLeft = Offset(x, centerY - peakHalfH - 2.dp.toPx()),
                                size = Size(barWidth, 2.dp.toPx()),
                                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                            )
                            // Bottom peak cap
                            drawRoundRect(
                                color = Color.White,
                                topLeft = Offset(x, centerY + peakHalfH),
                                size = Size(barWidth, 2.dp.toPx()),
                                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                            )
                        }
                    }
                }

                AppSettings.VIS_STYLE_DOT_MATRIX -> {
                    // STYLE 5: Retro LED Dot Matrix
                    val dotRows = 10
                    val spacingX = 3.dp.toPx()
                    val totalSpacingX = spacingX * (count + 1)
                    val dotW = ((canvasW - totalSpacingX) / count).coerceAtLeast(3.dp.toPx())
                    val dotH = ((canvasH - 12.dp.toPx()) / dotRows).coerceAtLeast(3.dp.toPx())
                    val dotSpacingY = 2.dp.toPx()

                    for (i in displayValues.indices) {
                        val amp = displayValues[i].coerceIn(0.04f, 1.0f)
                        val litDots = (amp * dotRows).toInt().coerceIn(1, dotRows)
                        val peakDot = (peakCaps.getOrElse(i) { amp } * dotRows).toInt().coerceIn(1, dotRows)
                        val x = spacingX + (i * (dotW + spacingX))

                        val zoneIndex = ((i * topBandColors.size) / count).coerceIn(0, topBandColors.lastIndex)
                        val activeColor = topBandColors[zoneIndex]

                        for (r in 0 until dotRows) {
                            val dotIndexFromBottom = dotRows - 1 - r
                            val y = r * (dotH + dotSpacingY) + 4.dp.toPx()
                            val isLit = dotIndexFromBottom < litDots
                            val isPeak = showPeakCaps && (dotIndexFromBottom == peakDot - 1)

                            val color = when {
                                isPeak -> Color.White
                                isLit -> activeColor
                                else -> Color(0x18374151)
                            }

                            drawRoundRect(
                                color = color,
                                topLeft = Offset(x, y),
                                size = Size(dotW, dotH - dotSpacingY),
                                cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                            )
                        }
                    }
                }

                AppSettings.VIS_STYLE_NEON_BARS -> {
                    // STYLE 2: Modern Smooth Solid Neon Bars with Glowing Floating Caps
                    val spacing = 3.dp.toPx()
                    val totalSpacing = spacing * (count + 1)
                    val barWidth = ((canvasW - totalSpacing) / count).coerceAtLeast(3.dp.toPx())

                    for (i in displayValues.indices) {
                        val amp = displayValues[i].coerceIn(0.04f, 1.0f)
                        val barH = ((canvasH - 6.dp.toPx()) * amp).coerceAtLeast(3.dp.toPx())
                        val x = spacing + (i * (barWidth + spacing))
                        val y = canvasH - barH

                        val zoneIndex = ((i * topBandColors.size) / count).coerceIn(0, topBandColors.lastIndex)
                        val topColor = topBandColors[zoneIndex]
                        val bottomColor = bottomBandColors.getOrElse(zoneIndex) { topColor.copy(alpha = 0.20f) }

                        // Draw solid continuous rounded neon bar
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(topColor, bottomColor),
                                startY = y,
                                endY = canvasH
                            ),
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barH),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )

                        // Subtle top highlight
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.55f),
                            topLeft = Offset(x, y),
                            size = Size(barWidth, 3.dp.toPx()),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                        )

                        // Floating peak needle cap
                        if (showPeakCaps) {
                            val peakAmp = peakCaps.getOrElse(i) { amp }.coerceIn(0.04f, 1.0f)
                            val peakY = (canvasH - (canvasH - 6.dp.toPx()) * peakAmp - 3.dp.toPx()).coerceAtLeast(1.dp.toPx())

                            drawRoundRect(
                                color = topColor.copy(alpha = 0.55f),
                                topLeft = Offset(x - 0.5.dp.toPx(), peakY - 1.dp.toPx()),
                                size = Size(barWidth + 1.dp.toPx(), 4.dp.toPx()),
                                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                            )
                            drawRoundRect(
                                color = Color.White,
                                topLeft = Offset(x, peakY),
                                size = Size(barWidth, 2.5.dp.toPx()),
                                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                            )
                        }
                    }
                }

                else -> {
                    // STYLE 1: FL Studio DAW LED Segmented Bars with Studio Grid & Floating Peak Needles
                    val gridColor = Color(0xFF2A2838)
                    val gridLevels = listOf(0.18f, 0.38f, 0.60f, 0.82f)
                    for (level in gridLevels) {
                        val lineY = canvasH * level
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, lineY),
                            end = Offset(canvasW, lineY),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    val spacing = 2.5.dp.toPx()
                    val totalSpacing = spacing * (count + 1)
                    val barWidth = ((canvasW - totalSpacing) / count).coerceAtLeast(2.dp.toPx())

                    for (i in displayValues.indices) {
                        val amp = displayValues[i].coerceIn(0.04f, 1.0f)
                        val barH = ((canvasH - 6.dp.toPx()) * amp).coerceAtLeast(2.dp.toPx())
                        val x = spacing + (i * (barWidth + spacing))
                        val y = canvasH - barH

                        val zoneIndex = ((i * topBandColors.size) / count).coerceIn(0, topBandColors.lastIndex)
                        val topColor = topBandColors[zoneIndex]
                        val bottomColor = bottomBandColors.getOrElse(zoneIndex) { topColor.copy(alpha = 0.25f) }

                        // Draw vertical frequency bar with album-adaptive gradient
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(topColor, bottomColor),
                                startY = y,
                                endY = canvasH
                            ),
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barH),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )

                        // Bright top cap highlight
                        if (barH > 4.dp.toPx()) {
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.45f),
                                topLeft = Offset(x, y),
                                size = Size(barWidth, 2.5.dp.toPx()),
                                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                            )
                        }

                        // Subtle DAW LED segmentation notches for authentic FL Studio look
                        val segmentHeight = 4.dp.toPx()
                        val segmentGap = 1.dp.toPx()
                        var segY = canvasH - segmentHeight
                        val segmentDarkenColor = Color(0x35000000)
                        while (segY > y) {
                            drawLine(
                                color = segmentDarkenColor,
                                start = Offset(x, segY),
                                end = Offset(x + barWidth, segY),
                                strokeWidth = segmentGap
                            )
                            segY -= (segmentHeight + segmentGap)
                        }

                        // Floating peak falloff needle cap
                        if (showPeakCaps) {
                            val peakAmp = peakCaps.getOrElse(i) { amp }.coerceIn(0.04f, 1.0f)
                            val peakY = (canvasH - (canvasH - 6.dp.toPx()) * peakAmp - 3.dp.toPx()).coerceAtLeast(1.dp.toPx())

                            drawRoundRect(
                                color = topColor.copy(alpha = 0.45f),
                                topLeft = Offset(x - 0.5.dp.toPx(), peakY - 1.dp.toPx()),
                                size = Size(barWidth + 1.dp.toPx(), 4.5.dp.toPx()),
                                cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                            )
                            drawRoundRect(
                                color = Color.White,
                                topLeft = Offset(x, peakY),
                                size = Size(barWidth, 2.5.dp.toPx()),
                                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                            )
                        }
                    }
                }
            }
        }

        if (showLabels) {
            Spacer(modifier = Modifier.height(4.dp))

            // Frequency band identifiers tinted with active spectrum colors
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlBandLabels.forEachIndexed { index, label ->
                    val bandColor = topBandColors.getOrElse(index) { Color.White }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(bandColor.copy(alpha = 0.14f))
                            .padding(horizontal = 3.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = bandColor,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 10.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
