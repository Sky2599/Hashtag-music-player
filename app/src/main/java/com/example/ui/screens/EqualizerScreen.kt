package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettings
import com.example.player.EqualizerState
import com.example.player.ReverbEngine
import com.example.ui.components.VisualizerView
import com.example.ui.theme.DeepPurple
import com.example.ui.theme.DeepPurpleCard
import com.example.ui.theme.DeepPurpleDark
import com.example.ui.theme.DeepPurpleSurface
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElectricVioletLight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.util.TactileHaptics
import com.example.ui.util.instantTapDown
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import com.example.ui.theme.LocalAppTheme
import com.example.ui.util.LiquidGlassSheen

@Composable
private fun ReflectiveEqualizerCard(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp),
    containerColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val theme = LocalAppTheme.current
    val isLiquidGlass = theme.toneProfileLabel.contains("Liquid Glass", ignoreCase = true)

    val actualBg = containerColor ?: if (isLiquidGlass) Color(0x220D1B2D) else DeepPurpleCard

    Card(
        modifier = modifier
            .border(
                width = 1.dp,
                brush = if (isLiquidGlass) {
                    LiquidGlassSheen.GlassBorderGradient
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            theme.primaryLight.copy(alpha = 0.45f),
                            Color(0x18FFFFFF),
                            theme.primary.copy(alpha = 0.25f)
                        )
                    )
                },
                shape = shape
            )
            .drawWithContent {
                drawContent()
                if (isLiquidGlass) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(
                                Color(0x18FFFFFF),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = size.height * 0.35f
                        )
                    )
                }
            },
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = actualBg),
        content = content
    )
}

@Composable
fun EqualizerScreen(
    equalizerState: EqualizerState,
    visualizerFft: FloatArray = FloatArray(21) { 0.05f },
    visualizerFftProvider: (() -> FloatArray)? = null,
    appSettings: AppSettings = AppSettings(),
    onSettingsChanged: (AppSettings) -> Unit = {},
    onToggleEqualizer: (Boolean) -> Unit,
    onBandLevelChange: (Int, Int) -> Unit,
    onPresetSelect: (String) -> Unit,
    onToggleBassBoost: (Boolean) -> Unit,
    onBassBoostStrengthChange: (Int) -> Unit,
    onToggleStereoWidth: (Boolean) -> Unit = {},
    onStereoWidthChange: (Int) -> Unit = {},
    onCrossfadeChange: (Float) -> Unit,
    onToggleGapless: (Boolean) -> Unit,
    onToggleReplayGain: (Boolean) -> Unit,
    onReplayGainPreampChange: (Float) -> Unit = {},
    onToggleParametricEq: (Boolean) -> Unit = {},
    onApplyAutoEqProfile: (com.example.player.AutoEqDatabase.AutoEqProfile) -> Unit = {},
    onToggleReverb: (Boolean) -> Unit = {},
    onReverbPresetSelect: (String) -> Unit = {},
    onReverbRoomSizeChange: (Float) -> Unit = {},
    onReverbDecayChange: (Int) -> Unit = {},
    onReverbDampingChange: (Float) -> Unit = {},
    onReverbWetLevelChange: (Float) -> Unit = {},
    onToggleReverbLoudspeakerBass: (Boolean) -> Unit = {},
    onReverbBassPunchChange: (Int) -> Unit = {},
    onToggleVirtualizer: ((Boolean) -> Unit)? = null,
    onVirtualizerStrengthChange: ((Int) -> Unit)? = null,
    onBandModeChange: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val presets = listOf("Flat", "V-Line", "Rock", "Pop", "Jazz", "Bass Boost", "Electronic", "Vocal", "Treble", "Acoustic")

    val theme = LocalAppTheme.current
    val isLiquidGlass = theme.toneProfileLabel.contains("Liquid Glass", ignoreCase = true)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(if (isLiquidGlass) Color.Transparent else DeepPurple)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Equalizer Header & Master Toggle
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Equaliser",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Switch(
                    checked = equalizerState.isEnabled,
                    onCheckedChange = {
                        TactileHaptics.toggleClick(view)
                        onToggleEqualizer(it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TextPrimary,
                        checkedTrackColor = ElectricViolet,
                        uncheckedThumbColor = TextTertiary,
                        uncheckedTrackColor = DeepPurpleDark
                    ),
                    modifier = Modifier.testTag("master_equalizer_switch")
                )
            }
        }

        // Preset Selector Pills
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(presets) { preset ->
                    val isSelected = equalizerState.selectedPreset.equals(preset, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) {
                                    if (isLiquidGlass) Color(0x3500F0FF) else ElectricViolet
                                } else {
                                    if (isLiquidGlass) Color(0x18FFFFFF) else DeepPurpleDark
                                }
                            )
                            .border(
                                width = 1.dp,
                                brush = if (isSelected) {
                                    if (isLiquidGlass) {
                                        Brush.linearGradient(
                                            listOf(Color(0xE0FFFFFF), Color(0x8000F0FF), Color(0xC0FFFFFF))
                                        )
                                    } else {
                                        Brush.linearGradient(listOf(ElectricVioletLight, ElectricVioletLight))
                                    }
                                } else {
                                    if (isLiquidGlass) {
                                        Brush.linearGradient(listOf(Color(0x28FFFFFF), Color(0x10FFFFFF)))
                                    } else {
                                        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                    }
                                },
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                TactileHaptics.selectionClick(view)
                                onPresetSelect(preset)
                            }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                            .testTag("preset_$preset")
                    ) {
                        Text(
                            text = preset,
                            color = if (isSelected) (if (isLiquidGlass) Color.White else TextPrimary) else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // Graphic Equalizer Sliders Card (Switchable 5 and 10 Bands)
        item {
            ReflectiveEqualizerCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("graphic_equalizer_card"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Top controls: 5 vs 10 Band Switcher and Quick Reset
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Segmented control: 5 Bands | 10 Bands
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isLiquidGlass) Color(0x1C0D1B2D) else DeepPurpleDark)
                                .border(
                                    1.dp,
                                    if (isLiquidGlass) Color(0x30FFFFFF) else Color.Transparent,
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (equalizerState.bandMode == 5) {
                                            if (isLiquidGlass) Color(0x3500F0FF) else ElectricViolet
                                        } else Color.Transparent
                                    )
                                    .border(
                                        1.dp,
                                        if (equalizerState.bandMode == 5 && isLiquidGlass) Color(0x8000F0FF) else Color.Transparent,
                                        RoundedCornerShape(16.dp)
                                    )
                                    .instantTapDown(hapticLightImpact = false) { onBandModeChange(5) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .testTag("band_mode_5")
                            ) {
                                Text(
                                    text = "5 Bands",
                                    color = if (equalizerState.bandMode == 5) (if (isLiquidGlass) Color.White else TextPrimary) else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (equalizerState.bandMode == 5) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (equalizerState.bandMode == 10) {
                                            if (isLiquidGlass) Color(0x3500F0FF) else ElectricViolet
                                        } else Color.Transparent
                                    )
                                    .border(
                                        1.dp,
                                        if (equalizerState.bandMode == 10 && isLiquidGlass) Color(0x8000F0FF) else Color.Transparent,
                                        RoundedCornerShape(16.dp)
                                    )
                                    .instantTapDown(hapticLightImpact = false) { onBandModeChange(10) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .testTag("band_mode_10")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "10 Bands",
                                        color = if (equalizerState.bandMode == 10) (if (isLiquidGlass) Color.White else TextPrimary) else TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = if (equalizerState.bandMode == 10) FontWeight.Bold else FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (equalizerState.bandMode == 10) Color(0xFFFF6B81) else Color(0x44FF6B81))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "PRO",
                                            color = TextPrimary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }
                        }

                        // Reset Flat Button
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isLiquidGlass) Color(0x1C0D1B2D) else DeepPurpleDark)
                                .border(
                                    1.dp,
                                    if (isLiquidGlass) Color(0x30FFFFFF) else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .instantTapDown(hapticLightImpact = true) { onPresetSelect("Flat") }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("eq_reset_flat"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Flat EQ",
                                tint = if (isLiquidGlass) Color(0xFF00F0FF) else ElectricVioletLight,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Flat",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    val eqPrimaryColor = ElectricViolet
                    val eqPrimaryLightColor = ElectricVioletLight
                    val eqTertiaryColor = TextTertiary

                    // Interactive EQ Response Spectrum Spline Curve
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF130D24))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val midY = h / 2f
                            val bandsCount = equalizerState.bandLevels.size.coerceAtLeast(1)
                            val rangeMb = (equalizerState.maxLevelMb - equalizerState.minLevelMb).toFloat().coerceAtLeast(1f)

                            val points = equalizerState.bandLevels.mapIndexed { idx, lvl ->
                                val x = if (bandsCount > 1) (idx.toFloat() / (bandsCount - 1)) * w else w / 2f
                                val fraction = ((lvl - equalizerState.minLevelMb).toFloat() / rangeMb).coerceIn(0f, 1f)
                                val y = (1f - fraction) * (h - 8.dp.toPx()) + 4.dp.toPx()
                                Offset(x, y)
                            }

                            // 0 dB center reference line
                            drawLine(
                                color = Color(0x33FFFFFF),
                                start = Offset(0f, midY),
                                end = Offset(w, midY),
                                strokeWidth = 1.dp.toPx()
                            )

                            if (points.size >= 2) {
                                val path = Path().apply {
                                    moveTo(points[0].x, points[0].y)
                                    for (i in 0 until points.size - 1) {
                                        val p0 = points[i]
                                        val p1 = points[i + 1]
                                        val cx = (p0.x + p1.x) / 2f
                                        cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                                    }
                                }

                                val fillPath = Path().apply {
                                    addPath(path)
                                    lineTo(w, h)
                                    lineTo(0f, h)
                                    close()
                                }

                                drawPath(
                                    fillPath,
                                    brush = Brush.verticalGradient(
                                        listOf(
                                            if (equalizerState.isEnabled) eqPrimaryColor.copy(alpha = 0.35f) else Color.Transparent,
                                            Color.Transparent
                                        )
                                    )
                                )

                                drawPath(
                                    path,
                                    color = if (equalizerState.isEnabled) eqPrimaryLightColor else eqTertiaryColor,
                                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                                )

                                points.forEach { pt ->
                                    drawCircle(
                                        color = if (equalizerState.isEnabled) Color(0xFFFF6B81) else eqTertiaryColor,
                                        radius = 2.5.dp.toPx(),
                                        center = pt
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "+12 dB",
                            color = TextTertiary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = if (equalizerState.bandMode == 10) "10-Band Studio Equaliser" else "5-Band Graphic Equaliser",
                            color = ElectricVioletLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "-12 dB",
                            color = TextTertiary,
                            fontSize = 11.sp
                        )
                    }

                    // Sliders Container: dynamic for 5 vs 10 bands
                    if (equalizerState.bandMode == 10) {
                        val tenBandFreqs = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .height(210.dp)
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (i in 0 until 10) {
                                val freq = equalizerState.bandFrequencies.getOrElse(i) {
                                    tenBandFreqs.getOrElse(i) { 1000 }
                                }
                                val freqLabel = if (freq >= 1000) "${freq / 1000}k" else "$freq"
                                val levelMb = equalizerState.bandLevels.getOrElse(i) { 0 }

                                VerticalBandSlider(
                                    frequencyLabel = "${freqLabel}Hz",
                                    levelMb = levelMb,
                                    minMb = equalizerState.minLevelMb,
                                    maxMb = equalizerState.maxLevelMb,
                                    enabled = equalizerState.isEnabled,
                                    onLevelChange = { newMb ->
                                        onBandLevelChange(i, newMb)
                                    },
                                    testTag = "equalizer_band_$i",
                                    modifier = Modifier
                                        .width(54.dp)
                                        .fillMaxHeight()
                                )
                            }
                        }
                    } else {
                        val fiveBandFreqs = listOf(60, 230, 910, 3600, 14000)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (i in 0 until 5) {
                                val freq = equalizerState.bandFrequencies.getOrElse(i) {
                                    fiveBandFreqs.getOrElse(i) { 1000 }
                                }
                                val freqLabel = if (freq >= 1000) "${freq / 1000}k" else "$freq"
                                val levelMb = equalizerState.bandLevels.getOrElse(i) { 0 }

                                VerticalBandSlider(
                                    frequencyLabel = "${freqLabel}Hz",
                                    levelMb = levelMb,
                                    minMb = equalizerState.minLevelMb,
                                    maxMb = equalizerState.maxLevelMb,
                                    enabled = equalizerState.isEnabled,
                                    onLevelChange = { newMb ->
                                        onBandLevelChange(i, newMb)
                                    },
                                    testTag = "equalizer_band_$i",
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                )
                            }
                        }
                    }
                }
            }
        }

        // bass & Stereo Width Dual Cards
        item {
            val effectiveToggleStereoWidth: (Boolean) -> Unit = { enabled: Boolean ->
                TactileHaptics.toggleClick(view)
                onToggleStereoWidth(enabled)
                onToggleVirtualizer?.invoke(enabled)
            }
            val effectiveStereoWidthChange: (Int) -> Unit = { percent: Int ->
                onStereoWidthChange(percent)
                onVirtualizerStrengthChange?.invoke((((percent - 100).coerceAtLeast(0) / 100f) * 1000).toInt())
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Bass Boost Card
                ReflectiveEqualizerCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("bass_boost_card"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = "Bass Boost",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (equalizerState.bassBoostEnabled) "${equalizerState.bassBoostStrength / 10}% Punch" else "Bypassed",
                                    color = if (equalizerState.bassBoostEnabled) ElectricVioletLight else TextTertiary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Switch(
                                checked = equalizerState.bassBoostEnabled,
                                onCheckedChange = {
                                    TactileHaptics.toggleClick(view)
                                    onToggleBassBoost(it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = TextPrimary,
                                    checkedTrackColor = ElectricViolet,
                                    uncheckedThumbColor = TextTertiary,
                                    uncheckedTrackColor = DeepPurpleDark
                                ),
                                modifier = Modifier.testTag("bass_boost_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Rotary Knob Representation for Bass
                        var lastBassHapticBucket by remember { mutableIntStateOf(-1) }
                        Box(
                            modifier = Modifier
                                .size(92.dp)
                                .testTag("bass_boost_dial")
                                .pointerInput(equalizerState.bassBoostEnabled) {
                                    if (!equalizerState.bassBoostEnabled) return@pointerInput
                                    detectVerticalDragGestures { _, dragAmount ->
                                        val delta = (-dragAmount * 5f).toInt()
                                        val nextVal = (equalizerState.bassBoostStrength + delta).coerceIn(0, 1000)
                                        val bucket = nextVal / 50
                                        if (bucket != lastBassHapticBucket) {
                                            lastBassHapticBucket = bucket
                                            TactileHaptics.selectionClick(view)
                                        }
                                        onBassBoostStrengthChange(nextVal)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val bassFraction = (equalizerState.bassBoostStrength / 1000f).coerceIn(0f, 1f)
                            val currentAngleDeg = 135f + (bassFraction * 270f)
                            val dialPrimary = ElectricViolet
                            val dialPrimaryLight = ElectricVioletLight
                            val dialDarkTrack = DeepPurpleDark
                            val dialTertiary = TextTertiary

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokeWidth = 7.dp.toPx()
                                val radius = (size.minDimension - strokeWidth) / 2f
                                val centerOffset = Offset(size.width / 2f, size.height / 2f)

                                drawArc(
                                    color = Color(0xFF24183E),
                                    startAngle = 135f,
                                    sweepAngle = 270f,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )

                                if (equalizerState.bassBoostEnabled && bassFraction > 0f) {
                                    val activeBrush = Brush.sweepGradient(
                                        0.0f to dialPrimary,
                                        1.0f to dialPrimaryLight,
                                        center = centerOffset
                                    )
                                    drawArc(
                                        brush = activeBrush,
                                        startAngle = 135f,
                                        sweepAngle = bassFraction * 270f,
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )
                                }

                                val knobRadius = radius * 0.70f
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        listOf(Color(0xFF2C1E4A), Color(0xFF1E1435)),
                                        center = centerOffset,
                                        radius = knobRadius
                                    ),
                                    radius = knobRadius,
                                    center = centerOffset
                                )
                                drawCircle(
                                    color = if (equalizerState.bassBoostEnabled) dialPrimary.copy(alpha = 0.5f) else dialDarkTrack,
                                    radius = knobRadius,
                                    center = centerOffset,
                                    style = Stroke(width = 1.5.dp.toPx())
                                )

                                val pointerRad = Math.toRadians(currentAngleDeg.toDouble())
                                val pointerPos = Offset(
                                    centerOffset.x + (knobRadius - 5.dp.toPx()) * cos(pointerRad).toFloat(),
                                    centerOffset.y + (knobRadius - 5.dp.toPx()) * sin(pointerRad).toFloat()
                                )
                                drawCircle(
                                    color = if (equalizerState.bassBoostEnabled) dialPrimaryLight else dialTertiary,
                                    radius = 3.dp.toPx(),
                                    center = pointerPos
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "${equalizerState.bassBoostStrength / 10}%",
                                    color = if (equalizerState.bassBoostEnabled) TextPrimary else TextTertiary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "BASS",
                                    color = if (equalizerState.bassBoostEnabled) ElectricVioletLight else TextTertiary,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Slider(
                            value = (equalizerState.bassBoostStrength / 1000f).coerceIn(0f, 1f),
                            onValueChange = {
                                val nextVal = (it * 1000).toInt()
                                val bucket = nextVal / 50
                                if (bucket != lastBassHapticBucket) {
                                    lastBassHapticBucket = bucket
                                    TactileHaptics.selectionClick(view)
                                }
                                onBassBoostStrengthChange(nextVal)
                            },
                            enabled = equalizerState.bassBoostEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = ElectricVioletLight,
                                activeTrackColor = ElectricViolet,
                                inactiveTrackColor = DeepPurpleDark
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("0%", color = TextSecondary, fontSize = 9.sp)
                            Text("50%", color = TextTertiary, fontSize = 9.sp)
                            Text("100%", color = TextSecondary, fontSize = 9.sp)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(0 to "0%", 500 to "50%", 1000 to "Max").forEach { (strengthVal, label) ->
                                val isSelected = equalizerState.bassBoostStrength == strengthVal
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected && equalizerState.bassBoostEnabled) ElectricViolet.copy(alpha = 0.35f) else Color(0xFF1E1435))
                                        .clickable(enabled = equalizerState.bassBoostEnabled) {
                                            TactileHaptics.selectionClick(view)
                                            onBassBoostStrengthChange(strengthVal)
                                        }
                                        .padding(vertical = 3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected && equalizerState.bassBoostEnabled) TextPrimary else TextSecondary,
                                        fontSize = 9.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                // Stereo Width Processing Dial Card (Operational 0% Mono to 200% Ultra-Wide)
                ReflectiveEqualizerCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stereo_width_card")
                        .testTag("virtualizer_card"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = "Stereo Width",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = when {
                                        !equalizerState.stereoWidthEnabled -> "Bypassed"
                                        equalizerState.stereoWidthPercent == 0 -> "Mono (0%)"
                                        equalizerState.stereoWidthPercent < 100 -> "Narrow (${equalizerState.stereoWidthPercent}%)"
                                        equalizerState.stereoWidthPercent == 100 -> "Normal (100%)"
                                        equalizerState.stereoWidthPercent <= 150 -> "Wide (${equalizerState.stereoWidthPercent}%)"
                                        else -> "Ultra-Wide (${equalizerState.stereoWidthPercent}%)"
                                    },
                                    color = if (equalizerState.stereoWidthEnabled) ElectricVioletLight else TextTertiary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Switch(
                                checked = equalizerState.stereoWidthEnabled,
                                onCheckedChange = effectiveToggleStereoWidth,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = TextPrimary,
                                    checkedTrackColor = ElectricViolet,
                                    uncheckedThumbColor = TextTertiary,
                                    uncheckedTrackColor = DeepPurpleDark
                                ),
                                modifier = Modifier.testTag("stereo_width_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Rotary Dial Component (0% Mono .. 100% Normal .. 200% Ultra-Wide)
                        var lastStereoHapticBucket by remember { mutableIntStateOf(-1) }
                        Box(
                            modifier = Modifier
                                .size(92.dp)
                                .testTag("stereo_width_dial")
                                .pointerInput(equalizerState.stereoWidthEnabled) {
                                    if (!equalizerState.stereoWidthEnabled) return@pointerInput
                                    detectVerticalDragGestures { _, dragAmount ->
                                        val delta = (-dragAmount * 1.0f).toInt()
                                        val nextVal = (equalizerState.stereoWidthPercent + delta).coerceIn(0, 200)
                                        val bucket = nextVal / 10
                                        if (bucket != lastStereoHapticBucket) {
                                            lastStereoHapticBucket = bucket
                                            TactileHaptics.selectionClick(view)
                                        }
                                        effectiveStereoWidthChange(nextVal)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val sweepFraction = (equalizerState.stereoWidthPercent / 200f).coerceIn(0f, 1f)
                            val currentAngleDeg = 135f + (sweepFraction * 270f)
                            val dialPrimary = ElectricViolet
                            val dialPrimaryLight = ElectricVioletLight
                            val dialDarkTrack = DeepPurpleDark
                            val dialTertiary = TextTertiary

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokeWidth = 7.dp.toPx()
                                val radius = (size.minDimension - strokeWidth) / 2f
                                val centerOffset = Offset(size.width / 2f, size.height / 2f)

                                // Track arc
                                drawArc(
                                    color = Color(0xFF24183E),
                                    startAngle = 135f,
                                    sweepAngle = 270f,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )

                                // Normal stereo center tick mark (top center at 270° = 100%)
                                val tickLen = 5.dp.toPx()
                                val normalAngleRad = Math.toRadians(270.0)
                                val tickStart = Offset(
                                    centerOffset.x + (radius - tickLen) * cos(normalAngleRad).toFloat(),
                                    centerOffset.y + (radius - tickLen) * sin(normalAngleRad).toFloat()
                                )
                                val tickEnd = Offset(
                                    centerOffset.x + (radius + tickLen) * cos(normalAngleRad).toFloat(),
                                    centerOffset.y + (radius + tickLen) * sin(normalAngleRad).toFloat()
                                )
                                drawLine(
                                    color = if (equalizerState.stereoWidthEnabled) dialPrimaryLight.copy(alpha = 0.8f) else dialTertiary,
                                    start = tickStart,
                                    end = tickEnd,
                                    strokeWidth = 2.dp.toPx()
                                )

                                // Active sweep arc
                                if (equalizerState.stereoWidthEnabled && sweepFraction > 0f) {
                                    val activeBrush = Brush.sweepGradient(
                                        0.0f to dialPrimary,
                                        0.5f to dialPrimaryLight,
                                        1.0f to Color(0xFFF472B6),
                                        center = centerOffset
                                    )
                                    drawArc(
                                        brush = activeBrush,
                                        startAngle = 135f,
                                        sweepAngle = sweepFraction * 270f,
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )
                                }

                                // Center knob disc
                                val knobRadius = radius * 0.70f
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        listOf(Color(0xFF2C1E4A), Color(0xFF1E1435)),
                                        center = centerOffset,
                                        radius = knobRadius
                                    ),
                                    radius = knobRadius,
                                    center = centerOffset
                                )
                                drawCircle(
                                    color = if (equalizerState.stereoWidthEnabled) dialPrimary.copy(alpha = 0.5f) else dialDarkTrack,
                                    radius = knobRadius,
                                    center = centerOffset,
                                    style = Stroke(width = 1.5.dp.toPx())
                                )

                                // Pointer notch
                                val pointerRad = Math.toRadians(currentAngleDeg.toDouble())
                                val pointerPos = Offset(
                                    centerOffset.x + (knobRadius - 5.dp.toPx()) * cos(pointerRad).toFloat(),
                                    centerOffset.y + (knobRadius - 5.dp.toPx()) * sin(pointerRad).toFloat()
                                )
                                drawCircle(
                                    color = if (equalizerState.stereoWidthEnabled) dialPrimaryLight else dialTertiary,
                                    radius = 3.dp.toPx(),
                                    center = pointerPos
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "${equalizerState.stereoWidthPercent}%",
                                    color = if (equalizerState.stereoWidthEnabled) TextPrimary else TextTertiary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("stereo_width_value")
                                )
                                Text(
                                    text = when {
                                        equalizerState.stereoWidthPercent == 0 -> "MONO"
                                        equalizerState.stereoWidthPercent < 100 -> "NARROW"
                                        equalizerState.stereoWidthPercent == 100 -> "NORMAL"
                                        equalizerState.stereoWidthPercent <= 150 -> "WIDE"
                                        else -> "ULTRA"
                                    },
                                    color = if (equalizerState.stereoWidthEnabled) ElectricVioletLight else TextTertiary,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Precision Slider (0% to 200%)
                        Slider(
                            value = (equalizerState.stereoWidthPercent / 200f).coerceIn(0f, 1f),
                            onValueChange = {
                                val nextVal = (it * 200).toInt()
                                val bucket = nextVal / 10
                                if (bucket != lastStereoHapticBucket) {
                                    lastStereoHapticBucket = bucket
                                    TactileHaptics.selectionClick(view)
                                }
                                effectiveStereoWidthChange(nextVal)
                            },
                            enabled = equalizerState.stereoWidthEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = ElectricVioletLight,
                                activeTrackColor = ElectricViolet,
                                inactiveTrackColor = DeepPurpleDark
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("stereo_width_slider")
                        )

                        // Calibration Scale
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("0% Mono", color = TextSecondary, fontSize = 9.sp)
                            Text(
                                "100%",
                                color = if (equalizerState.stereoWidthPercent == 100) ElectricVioletLight else TextTertiary,
                                fontSize = 9.sp,
                                fontWeight = if (equalizerState.stereoWidthPercent == 100) FontWeight.Bold else FontWeight.Normal
                            )
                            Text("200% Ultra", color = TextSecondary, fontSize = 9.sp)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Quick Presets Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(0 to "Mono", 100 to "Normal", 200 to "Ultra").forEach { (presetVal, label) ->
                                val isSelected = equalizerState.stereoWidthPercent == presetVal
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected && equalizerState.stereoWidthEnabled) ElectricViolet.copy(alpha = 0.35f) else Color(0xFF1E1435))
                                        .clickable(enabled = equalizerState.stereoWidthEnabled) {
                                            TactileHaptics.selectionClick(view)
                                            effectiveStereoWidthChange(presetVal)
                                        }
                                        .padding(vertical = 3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected && equalizerState.stereoWidthEnabled) TextPrimary else TextSecondary,
                                        fontSize = 9.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }


        // Crossfade & Audio Processing Card
        item {
            ReflectiveEqualizerCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("crossfade_section_card"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Crossfade",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Transition Duration",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "${equalizerState.crossfadeSeconds}s",
                            color = ElectricVioletLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    var lastCrossfadeHapticStep by remember { mutableIntStateOf(-1) }
                    Slider(
                        value = equalizerState.crossfadeSeconds,
                        onValueChange = {
                            val stepBucket = (it * 2).toInt()
                            if (stepBucket != lastCrossfadeHapticStep) {
                                lastCrossfadeHapticStep = stepBucket
                                TactileHaptics.selectionClick(view)
                            }
                            onCrossfadeChange(it)
                        },
                        valueRange = 0f..12f,
                        steps = 23,
                        colors = SliderDefaults.colors(
                            thumbColor = ElectricVioletLight,
                            activeTrackColor = ElectricViolet,
                            inactiveTrackColor = DeepPurpleDark
                        ),
                        modifier = Modifier.testTag("crossfade_slider")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Gapless Playback",
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Eliminate silence between tracks",
                                color = TextTertiary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = equalizerState.gaplessEnabled,
                            onCheckedChange = {
                                TactileHaptics.toggleClick(view)
                                onToggleGapless(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextPrimary,
                                checkedTrackColor = ElectricViolet,
                                uncheckedThumbColor = TextTertiary,
                                uncheckedTrackColor = DeepPurpleDark
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Replay Gain Normalization",
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "EBU R128 / ReplayGain peak leveling",
                                color = TextTertiary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = equalizerState.replayGainEnabled,
                            onCheckedChange = {
                                TactileHaptics.toggleClick(view)
                                onToggleReplayGain(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextPrimary,
                                checkedTrackColor = ElectricViolet,
                                uncheckedThumbColor = TextTertiary,
                                uncheckedTrackColor = DeepPurpleDark
                            )
                        )
                    }

                    if (equalizerState.replayGainEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Pre-amp Gain",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = String.format("%+.1f dB", equalizerState.replayGainPreampDb),
                                color = ElectricVioletLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = equalizerState.replayGainPreampDb,
                            onValueChange = {
                                TactileHaptics.selectionClick(view)
                                onReplayGainPreampChange(it)
                            },
                            valueRange = -6f..6f,
                            steps = 11,
                            colors = SliderDefaults.colors(
                                thumbColor = ElectricVioletLight,
                                activeTrackColor = ElectricViolet,
                                inactiveTrackColor = DeepPurpleDark
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Parametric EQ & AutoEQ Headphone Correction Section
        item {
            ReflectiveEqualizerCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("peq_section_card"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                TactileHaptics.toggleClick(view)
                                onToggleParametricEq(!equalizerState.isParametricEnabled)
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "AutoEQ / Parametric EQ",
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(ElectricViolet)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "PRO REFERENCE",
                                        color = TextPrimary,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (equalizerState.isParametricEnabled)
                                    "Active: ${equalizerState.activeAutoEqProfile}"
                                else
                                    "Acoustic headphone target calibration",
                                color = if (equalizerState.isParametricEnabled) ElectricVioletLight else TextTertiary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Switch(
                            checked = equalizerState.isParametricEnabled,
                            onCheckedChange = {
                                TactileHaptics.toggleClick(view)
                                onToggleParametricEq(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextPrimary,
                                checkedTrackColor = ElectricViolet,
                                uncheckedThumbColor = TextTertiary,
                                uncheckedTrackColor = DeepPurpleDark
                            ),
                            modifier = Modifier.testTag("parametric_eq_switch")
                        )
                    }

                    if (equalizerState.isParametricEnabled) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "AutoEQ Headphone Profiles:",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "10-Band Harman Calibration",
                                color = TextTertiary,
                                fontSize = 10.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            com.example.player.AutoEqDatabase.PROFILES.forEach { profile ->
                                val isSelected = equalizerState.activeAutoEqProfile == profile.name
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            TactileHaptics.selectionClick(view)
                                            onApplyAutoEqProfile(profile)
                                        }
                                        .testTag("autoeq_profile_${profile.name.replace(" ", "_")}"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) {
                                            if (isLiquidGlass) Color(0x3500F0FF) else ElectricViolet.copy(alpha = 0.28f)
                                        } else {
                                            if (isLiquidGlass) Color(0x180D1B2D) else DeepPurpleDark
                                        }
                                    ),
                                    border = if (isSelected) {
                                        androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isLiquidGlass) Color(0xFF00F0FF) else ElectricViolet
                                        )
                                    } else if (isLiquidGlass) {
                                        androidx.compose.foundation.BorderStroke(1.dp, Color(0x25FFFFFF))
                                    } else null
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = androidx.compose.material.icons.Icons.Default.Check,
                                                        contentDescription = "Active Profile",
                                                        tint = if (isLiquidGlass) Color(0xFF80F7FF) else ElectricVioletLight,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                Text(
                                                    text = profile.name,
                                                    color = if (isSelected) (if (isLiquidGlass) Color(0xFF80F7FF) else ElectricVioletLight) else TextPrimary,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(
                                                        if (isSelected) {
                                                            if (isLiquidGlass) Color(0x5000F0FF) else ElectricViolet.copy(alpha = 0.4f)
                                                        } else {
                                                            if (isLiquidGlass) Color(0x25FFFFFF) else DeepPurpleCard
                                                        }
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = profile.targetCurve,
                                                    color = if (isSelected) TextPrimary else TextTertiary,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = profile.description,
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Advanced Reverb & Loudspeaker Room Acoustic Simulation Engine
        item {
            ReflectiveEqualizerCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reverb_acoustic_room_card"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header with Master Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (equalizerState.reverbEnabled) Color(0xFF00E676) else TextTertiary)
                                )
                                Text(
                                    text = "Reverb & Room Engine",
                                    color = TextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = if (equalizerState.reverbEnabled)
                                    "Loudspeaker in Room • ${equalizerState.reverbPreset}"
                                else
                                    "Bypassed (Dry Bit-Perfect Signal)",
                                color = if (equalizerState.reverbEnabled) ElectricVioletLight else TextSecondary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Switch(
                            checked = equalizerState.reverbEnabled,
                            onCheckedChange = {
                                TactileHaptics.toggleClick(view)
                                onToggleReverb(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextPrimary,
                                checkedTrackColor = ElectricViolet,
                                uncheckedThumbColor = TextTertiary,
                                uncheckedTrackColor = DeepPurpleDark
                            ),
                            modifier = Modifier.testTag("reverb_master_switch")
                        )
                    }

                    if (equalizerState.reverbEnabled) {
                        Spacer(modifier = Modifier.height(14.dp))

                        // JBL Boombox Loudspeaker Simulation Feature Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = 1.dp,
                                    color = if (equalizerState.reverbLoudspeakerBass) {
                                        Color(0xFFFF9800)
                                    } else if (isLiquidGlass) {
                                        Color(0x35FFFFFF)
                                    } else {
                                        DeepPurpleDark
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (equalizerState.reverbLoudspeakerBass) {
                                    if (isLiquidGlass) Color(0x33FF9800) else Color(0xFFFF9800).copy(alpha = 0.12f)
                                } else {
                                    if (isLiquidGlass) Color(0x180D1B2D) else DeepPurpleDark
                                }
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "JBL Boombox Loudspeaker Mode",
                                            color = if (equalizerState.reverbLoudspeakerBass) Color(0xFFFFB74D) else TextPrimary,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Simulates dual passive radiators (45Hz..80Hz) + room boundary bass coupling",
                                            color = TextSecondary,
                                            fontSize = 10.5.sp,
                                            lineHeight = 14.sp
                                        )
                                    }

                                    Switch(
                                        checked = equalizerState.reverbLoudspeakerBass,
                                        onCheckedChange = {
                                            TactileHaptics.toggleClick(view)
                                            onToggleReverbLoudspeakerBass(it)
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFFFF9800),
                                            checkedTrackColor = Color(0xFFFFB74D).copy(alpha = 0.6f),
                                            uncheckedThumbColor = TextTertiary,
                                            uncheckedTrackColor = DeepPurpleCard
                                        ),
                                        modifier = Modifier.testTag("jbl_boombox_switch")
                                    )
                                }

                                if (equalizerState.reverbLoudspeakerBass) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Sub-Bass Punch Intensity",
                                            color = TextSecondary,
                                            fontSize = 11.5.sp
                                        )
                                        Text(
                                            text = "+${(equalizerState.reverbBassPunchStrength / 10)}% JBL Thump",
                                            color = Color(0xFFFFB74D),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Slider(
                                        value = equalizerState.reverbBassPunchStrength.toFloat(),
                                        onValueChange = {
                                            onReverbBassPunchChange(it.toInt())
                                        },
                                        valueRange = 0f..1000f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color(0xFFFF9800),
                                            activeTrackColor = Color(0xFFFF9800),
                                            inactiveTrackColor = DeepPurpleDark
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("jbl_punch_slider")
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Interactive Real-Time Acoustic Room Canvas
                        // Depicts loudspeaker in the room with expanding reverb wavefronts & early reflections
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isLiquidGlass) Color(0x90050810) else Color(0xFF090712))
                                .border(1.dp, if (isLiquidGlass) Color(0x3500F0FF) else DeepPurpleDark, RoundedCornerShape(12.dp))
                                .padding(8.dp)
                        ) {
                            val decay = equalizerState.reverbDecayMs
                            val roomSize = equalizerState.reverbRoomSize
                            val damping = equalizerState.reverbDamping
                            val wet = equalizerState.reverbWetLevel
                            val isJbl = equalizerState.reverbLoudspeakerBass
                            val speakerBorderColor = if (isJbl) Color(0xFFFF9800) else ElectricVioletLight

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height

                                // Draw simulated Room 3D perspective wireframe
                                val insetX = w * (1f - roomSize) * 0.18f
                                val insetY = h * (1f - roomSize) * 0.18f
                                val roomLeft = insetX + 8f
                                val roomRight = w - insetX - 8f
                                val roomTop = insetY + 6f
                                val roomBottom = h - insetY - 6f

                                // Room floor & back wall gradient
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color(0xFF140F24), Color(0xFF0B0816)),
                                        startY = roomTop,
                                        endY = roomBottom
                                    ),
                                    topLeft = Offset(roomLeft, roomTop),
                                    size = androidx.compose.ui.geometry.Size(roomRight - roomLeft, roomBottom - roomTop)
                                )

                                // Room boundary lines (walls)
                                drawRect(
                                    color = Color(0xFF382C5E).copy(alpha = 0.5f),
                                    topLeft = Offset(roomLeft, roomTop),
                                    size = androidx.compose.ui.geometry.Size(roomRight - roomLeft, roomBottom - roomTop),
                                    style = Stroke(width = 1.5f)
                                )

                                // 3D perspective corner rays
                                drawLine(Color(0xFF281F43), Offset(0f, 0f), Offset(roomLeft, roomTop), 1.5f)
                                drawLine(Color(0xFF281F43), Offset(w, 0f), Offset(roomRight, roomTop), 1.5f)
                                drawLine(Color(0xFF281F43), Offset(0f, h), Offset(roomLeft, roomBottom), 1.5f)
                                drawLine(Color(0xFF281F43), Offset(w, h), Offset(roomRight, roomBottom), 1.5f)

                                // Center Loudspeaker Location
                                val speakerX = w * 0.5f
                                val speakerY = h * 0.58f

                                // Reverberation wavefront arcs expanding outwards from the loudspeaker
                                val maxRadius = (w * 0.46f).coerceAtLeast(30f)
                                val arcCount = 4
                                for (i in 1..arcCount) {
                                    val r = (maxRadius * (i / arcCount.toFloat()) * (0.4f + roomSize * 0.6f))
                                    val waveAlpha = ((1f - (i / (arcCount + 1f))) * wet * (1f - damping * 0.35f)).coerceIn(0.1f, 0.85f)
                                    val waveColor = if (isJbl) {
                                        Color(0xFFFF9800).copy(alpha = waveAlpha)
                                    } else {
                                        Color(0xFF7C4DFF).copy(alpha = waveAlpha)
                                    }
                                    drawCircle(
                                        color = waveColor,
                                        radius = r,
                                        center = Offset(speakerX, speakerY),
                                        style = Stroke(width = 2.2f)
                                    )
                                }

                                // Bouncing wall reflection dots
                                val dotAlpha = (wet * 0.8f).coerceIn(0.2f, 1f)
                                val dotColor = if (isJbl) Color(0xFFFFB74D) else Color(0xFF00E5FF)
                                drawCircle(dotColor.copy(alpha = dotAlpha), radius = 3.5f, center = Offset(roomLeft + 14f, roomTop + 20f))
                                drawCircle(dotColor.copy(alpha = dotAlpha), radius = 3.5f, center = Offset(roomRight - 14f, roomTop + 20f))
                                drawCircle(dotColor.copy(alpha = dotAlpha * 0.7f), radius = 3.0f, center = Offset(roomLeft + 26f, roomBottom - 18f))
                                drawCircle(dotColor.copy(alpha = dotAlpha * 0.7f), radius = 3.0f, center = Offset(roomRight - 26f, roomBottom - 18f))

                                // Draw Loudspeaker (JBL Boombox style cabinet) in the center
                                val spkW = 44f
                                val spkH = 22f
                                val spkRect = androidx.compose.ui.geometry.Rect(
                                    speakerX - spkW / 2f,
                                    speakerY - spkH / 2f,
                                    speakerX + spkW / 2f,
                                    speakerY + spkH / 2f
                                )

                                // Speaker shadow
                                drawOval(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    topLeft = Offset(speakerX - spkW * 0.6f, speakerY + spkH * 0.4f),
                                    size = androidx.compose.ui.geometry.Size(spkW * 1.2f, 8f)
                                )

                                // Cabinet body
                                drawRoundRect(
                                    color = if (isJbl) Color(0xFF261D15) else Color(0xFF1E1736),
                                    topLeft = Offset(spkRect.left, spkRect.top),
                                    size = androidx.compose.ui.geometry.Size(spkW, spkH),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                                )
                                drawRoundRect(
                                    color = speakerBorderColor,
                                    topLeft = Offset(spkRect.left, spkRect.top),
                                    size = androidx.compose.ui.geometry.Size(spkW, spkH),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                                    style = Stroke(width = 1.6f)
                                )

                                // Dual active woofers / passive radiators
                                val wooferRadius = 6.0f
                                drawCircle(
                                    color = if (isJbl) Color(0xFFFFB74D) else Color(0xFFB388FF),
                                    radius = wooferRadius,
                                    center = Offset(speakerX - 11f, speakerY)
                                )
                                drawCircle(
                                    color = if (isJbl) Color(0xFFFFB74D) else Color(0xFFB388FF),
                                    radius = wooferRadius,
                                    center = Offset(speakerX + 11f, speakerY)
                                )
                                // Woofer center dustcaps
                                drawCircle(Color(0xFF110B22), radius = 2.2f, center = Offset(speakerX - 11f, speakerY))
                                drawCircle(Color(0xFF110B22), radius = 2.2f, center = Offset(speakerX + 11f, speakerY))
                            }

                            // Telemetry Badge on bottom right of Canvas
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.75f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${decay}ms DECAY",
                                    color = ElectricVioletLight,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "•",
                                    color = TextTertiary,
                                    fontSize = 9.5.sp
                                )
                                Text(
                                    text = "~${(40 + Math.pow(roomSize.toDouble(), 2.8) * 17960).toInt()} m³",
                                    color = TextSecondary,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Presets Row
                        Text(
                            text = "Acoustic Room Presets",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val reverbPresets = listOf(
                            ReverbEngine.PRESET_JBL_LARGE_ROOM to "JBL Large Room",
                            ReverbEngine.PRESET_JBL_ARENA to "JBL Arena / Stadium",
                            ReverbEngine.PRESET_CLUB_PA to "Club PA Stack",
                            ReverbEngine.PRESET_LIVING_ROOM to "Living Room Party",
                            ReverbEngine.PRESET_CONCERT_HALL to "Concert Hall",
                            ReverbEngine.PRESET_CATHEDRAL to "Cathedral",
                            ReverbEngine.PRESET_STUDIO_ROOM to "Acoustic Studio",
                            ReverbEngine.PRESET_CUSTOM to "Custom Lab"
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(reverbPresets) { (presetKey, presetLabel) ->
                                val isSelected = equalizerState.reverbPreset == presetKey
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) {
                                                if (isLiquidGlass) {
                                                    if (presetKey.startsWith("JBL")) Color(0x40FF9800) else Color(0x3500F0FF)
                                                } else {
                                                    if (presetKey.startsWith("JBL")) Color(0xFFFF9800) else ElectricViolet
                                                }
                                            } else {
                                                if (isLiquidGlass) Color(0x18FFFFFF) else DeepPurpleDark
                                            }
                                        )
                                        .border(
                                            width = 1.dp,
                                            brush = if (isSelected) {
                                                if (isLiquidGlass) {
                                                    if (presetKey.startsWith("JBL")) {
                                                        Brush.linearGradient(listOf(Color(0xFFFFB74D), Color(0xFFFF9800)))
                                                    } else {
                                                        Brush.linearGradient(listOf(Color(0xE0FFFFFF), Color(0x8000F0FF), Color(0xC0FFFFFF)))
                                                    }
                                                } else {
                                                    Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                                }
                                            } else {
                                                if (isLiquidGlass) {
                                                    Brush.linearGradient(listOf(Color(0x28FFFFFF), Color(0x10FFFFFF)))
                                                } else {
                                                    Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            TactileHaptics.selectionClick(view)
                                            onReverbPresetSelect(presetKey)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                        .testTag("reverb_preset_${presetKey.replace(" ", "_")}")
                                ) {
                                    Text(
                                        text = presetLabel,
                                        color = if (isSelected) (if (isLiquidGlass) Color.White else TextPrimary) else TextSecondary,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Fine-Tuning Sliders
                        // 1. Room Size / Volume
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Room Volume / Size",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = when {
                                    equalizerState.reverbRoomSize < 0.35f -> "Small Chamber (~350 m³)"
                                    equalizerState.reverbRoomSize < 0.65f -> "Medium Hall (~1,800 m³)"
                                    equalizerState.reverbRoomSize < 0.90f -> "Large Room (~5,500 m³)"
                                    else -> "Mega Arena (~18,000 m³)"
                                },
                                color = ElectricVioletLight,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Slider(
                            value = equalizerState.reverbRoomSize,
                            onValueChange = { onReverbRoomSizeChange(it) },
                            valueRange = 0.05f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = ElectricVioletLight,
                                activeTrackColor = ElectricViolet,
                                inactiveTrackColor = DeepPurpleDark
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reverb_room_size_slider")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 2. Reverb Wet Mix
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Reverberation Wet Mix",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${(equalizerState.reverbWetLevel * 100).toInt()}% Mix",
                                color = ElectricVioletLight,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Slider(
                            value = equalizerState.reverbWetLevel,
                            onValueChange = { onReverbWetLevelChange(it) },
                            valueRange = 0.0f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = ElectricVioletLight,
                                activeTrackColor = ElectricViolet,
                                inactiveTrackColor = DeepPurpleDark
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reverb_wet_slider")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 3. Air Absorption / HF Damping
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "High-Frequency Air Damping",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (equalizerState.reverbDamping > 0.5f) "Warm Room Damping" else "Bright Reflective Walls",
                                color = TextTertiary,
                                fontSize = 11.sp
                            )
                        }
                        Slider(
                            value = equalizerState.reverbDamping,
                            onValueChange = { onReverbDampingChange(it) },
                            valueRange = 0.0f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = ElectricVioletLight,
                                activeTrackColor = ElectricViolet,
                                inactiveTrackColor = DeepPurpleDark
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reverb_damping_slider")
                        )
                    }
                }
            }
        }

        // 7. Visualiser & Customization Engine Card (Shifted to bottom for effortless customization)
        item {
            ReflectiveEqualizerCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("visualizer_customization_card"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header & Reset Action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "visualiser",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Live FFT & Customization Engine",
                                color = ElectricVioletLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        IconButton(
                            onClick = {
                                TactileHaptics.selectionClick(view)
                                onSettingsChanged(
                                    appSettings.copy(
                                        visualizerStyle = AppSettings.VIS_STYLE_FL_BARS,
                                        visualizerColorScheme = AppSettings.VIS_COLOR_ALBUM_ADAPTIVE,
                                        visualizerSensitivity = 1.0f,
                                        visualizerBarCount = 21,
                                        visualizerPeakCaps = true,
                                        visualizerShowLabels = true
                                    )
                                )
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset Visualizer Settings",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Live Visualizer displaying current custom styling instantly
                    VisualizerView(
                        fftValues = visualizerFft,
                        fftValuesProvider = visualizerFftProvider,
                        height = 125.dp,
                        style = appSettings.visualizerStyle,
                        colorScheme = appSettings.visualizerColorScheme,
                        sensitivity = appSettings.visualizerSensitivity,
                        barCount = appSettings.visualizerBarCount,
                        showPeakCaps = appSettings.visualizerPeakCaps,
                        showLabels = appSettings.visualizerShowLabels,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("equalizer_live_custom_visualizer")
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = if (isLiquidGlass) Color(0x30FFFFFF) else DeepPurpleDark)
                    Spacer(modifier = Modifier.height(12.dp))

                    // 1. VISUALIZER RENDER STYLE SELECTOR
                    Text(
                        text = "Visualiser Render Style",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val styles = listOf(
                        AppSettings.VIS_STYLE_FL_BARS to "FL Studio DAW",
                        AppSettings.VIS_STYLE_NEON_BARS to "Neon Solid",
                        AppSettings.VIS_STYLE_WAVE_LINE to "Oscilloscope",
                        AppSettings.VIS_STYLE_SYMMETRIC to "Symmetric",
                        AppSettings.VIS_STYLE_DOT_MATRIX to "Dot Matrix"
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(styles) { (styleKey, styleName) ->
                            val isSelected = appSettings.visualizerStyle == styleKey
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) {
                                            if (isLiquidGlass) Color(0x3500F0FF) else ElectricViolet
                                        } else {
                                            if (isLiquidGlass) Color(0x18FFFFFF) else DeepPurpleDark
                                        }
                                    )
                                    .border(
                                        width = 1.dp,
                                        brush = if (isSelected) {
                                            if (isLiquidGlass) {
                                                Brush.linearGradient(listOf(Color(0xE0FFFFFF), Color(0x8000F0FF), Color(0xC0FFFFFF)))
                                            } else {
                                                Brush.linearGradient(listOf(ElectricVioletLight, ElectricVioletLight))
                                            }
                                        } else {
                                            if (isLiquidGlass) {
                                                Brush.linearGradient(listOf(Color(0x28FFFFFF), Color(0x10FFFFFF)))
                                            } else {
                                                Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        TactileHaptics.selectionClick(view)
                                        onSettingsChanged(appSettings.copy(visualizerStyle = styleKey))
                                    }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                                    .testTag("vis_style_$styleKey")
                            ) {
                                Text(
                                    text = styleName,
                                    color = if (isSelected) (if (isLiquidGlass) Color.White else TextPrimary) else TextSecondary,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. COLOR PALETTE THEME
                    Text(
                        text = "Visualiser Color Palette",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val palettes = listOf(
                        Triple(AppSettings.VIS_COLOR_ALBUM_ADAPTIVE, "Album Adaptive", ElectricVioletLight),
                        Triple(AppSettings.VIS_COLOR_CITIVIEW, "Citiview Neon", Color(0xFF00E5FF)),
                        Triple(AppSettings.VIS_COLOR_SUNSET, "Sunset Blaze", Color(0xFFFF9800)),
                        Triple(AppSettings.VIS_COLOR_EMERALD, "Emerald Glow", Color(0xFF10B981)),
                        Triple(AppSettings.VIS_COLOR_ICE, "Ice Pearl", Color(0xFFF8FAFC))
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(palettes) { (colorKey, colorName, previewColor) ->
                            val isSelected = appSettings.visualizerColorScheme == colorKey
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) {
                                            if (isLiquidGlass) Color(0x3500F0FF) else ElectricViolet.copy(alpha = 0.35f)
                                        } else {
                                            if (isLiquidGlass) Color(0x18FFFFFF) else DeepPurpleDark
                                        }
                                    )
                                    .border(
                                        width = 1.dp,
                                        brush = if (isSelected) {
                                            if (isLiquidGlass) {
                                                Brush.linearGradient(listOf(Color(0xE0FFFFFF), Color(0x8000F0FF), Color(0xC0FFFFFF)))
                                            } else {
                                                Brush.linearGradient(listOf(ElectricVioletLight, ElectricVioletLight))
                                            }
                                        } else {
                                            if (isLiquidGlass) {
                                                Brush.linearGradient(listOf(Color(0x28FFFFFF), Color(0x10FFFFFF)))
                                            } else {
                                                Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        TactileHaptics.selectionClick(view)
                                        onSettingsChanged(appSettings.copy(visualizerColorScheme = colorKey))
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("vis_color_$colorKey")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .clip(CircleShape)
                                            .background(previewColor)
                                    )
                                    Text(
                                        text = colorName,
                                        color = if (isSelected) (if (isLiquidGlass) Color.White else TextPrimary) else TextSecondary,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. FREQUENCY BAND RESOLUTION
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Frequency Band Resolution",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isLiquidGlass) Color(0x20FFFFFF) else DeepPurpleDark)
                                .border(1.dp, if (isLiquidGlass) Color(0x35FFFFFF) else Color.Transparent, RoundedCornerShape(16.dp))
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            listOf(7, 14, 21).forEach { count ->
                                val isSelected = appSettings.visualizerBarCount == count
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (isSelected) {
                                                if (isLiquidGlass) Color(0x4000F0FF) else ElectricViolet
                                            } else {
                                                Color.Transparent
                                            }
                                        )
                                        .clickable {
                                            TactileHaptics.selectionClick(view)
                                            onSettingsChanged(appSettings.copy(visualizerBarCount = count))
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .testTag("vis_bars_$count")
                                ) {
                                    Text(
                                        text = "$count Bands",
                                        color = if (isSelected) (if (isLiquidGlass) Color.White else TextPrimary) else TextSecondary,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4. DYNAMIC SENSITIVITY SLIDER
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Dynamic Sensitivity",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val sensLabel = when {
                            appSettings.visualizerSensitivity <= 0.7f -> "Subtle (${String.format("%.1f", appSettings.visualizerSensitivity)}x)"
                            appSettings.visualizerSensitivity <= 1.2f -> "Standard (${String.format("%.1f", appSettings.visualizerSensitivity)}x)"
                            appSettings.visualizerSensitivity <= 1.6f -> "Punchy (${String.format("%.1f", appSettings.visualizerSensitivity)}x)"
                            else -> "Hyper (${String.format("%.1f", appSettings.visualizerSensitivity)}x)"
                        }
                        Text(
                            text = sensLabel,
                            color = ElectricVioletLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    var lastSensHapticStep by remember { mutableIntStateOf(-1) }
                    Slider(
                        value = appSettings.visualizerSensitivity,
                        onValueChange = {
                            val bucket = (it * 10).toInt()
                            if (bucket != lastSensHapticStep) {
                                lastSensHapticStep = bucket
                                TactileHaptics.selectionClick(view)
                            }
                            onSettingsChanged(appSettings.copy(visualizerSensitivity = it))
                        },
                        valueRange = 0.5f..2.0f,
                        steps = 14,
                        colors = SliderDefaults.colors(
                            thumbColor = ElectricVioletLight,
                            activeTrackColor = ElectricViolet,
                            inactiveTrackColor = DeepPurpleDark
                        ),
                        modifier = Modifier.testTag("vis_sensitivity_slider")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 5. TOGGLES: FLOATING PEAK NEEDLES & FREQUENCY LABELS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Floating Peak Needles",
                                color = TextPrimary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Studio gravity falloff needle caps",
                                color = TextTertiary,
                                fontSize = 10.5.sp
                            )
                        }
                        Switch(
                            checked = appSettings.visualizerPeakCaps,
                            onCheckedChange = {
                                TactileHaptics.toggleClick(view)
                                onSettingsChanged(appSettings.copy(visualizerPeakCaps = it))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextPrimary,
                                checkedTrackColor = ElectricViolet,
                                uncheckedThumbColor = TextTertiary,
                                uncheckedTrackColor = DeepPurpleDark
                            ),
                            modifier = Modifier.testTag("vis_peak_caps_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Frequency Band Labels (Hz)",
                                color = TextPrimary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "SUB, BASS, MID, PRES, BRILL readouts",
                                color = TextTertiary,
                                fontSize = 10.5.sp
                            )
                        }
                        Switch(
                            checked = appSettings.visualizerShowLabels,
                            onCheckedChange = {
                                TactileHaptics.toggleClick(view)
                                onSettingsChanged(appSettings.copy(visualizerShowLabels = it))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextPrimary,
                                checkedTrackColor = ElectricViolet,
                                uncheckedThumbColor = TextTertiary,
                                uncheckedTrackColor = DeepPurpleDark
                            ),
                            modifier = Modifier.testTag("vis_labels_switch")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VerticalBandSlider(
    frequencyLabel: String,
    levelMb: Int,
    minMb: Int,
    maxMb: Int,
    enabled: Boolean,
    onLevelChange: (Int) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val levelDb = levelMb / 100f
    val range = (maxMb - minMb).toFloat().coerceAtLeast(1f)
    val fraction = ((levelMb - minMb) / range).coerceIn(0f, 1f)

    val currentOnLevelChange by rememberUpdatedState(onLevelChange)
    val currentLevelMb by rememberUpdatedState(levelMb)
    var lastHapticStep by remember { mutableIntStateOf(-999) }

    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // dB readout at top
        Text(
            text = String.format("%+.1f dB", levelDb),
            color = if (enabled) ElectricVioletLight else TextTertiary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Quick +1 dB increment button
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (enabled) DeepPurpleDark else Color.Transparent)
                .clickable(enabled = enabled) {
                    TactileHaptics.selectionClick(view)
                    val step = (maxMb - minMb) / 24
                    val next = (currentLevelMb + step).coerceAtMost(maxMb)
                    currentOnLevelChange(next)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Increase $frequencyLabel",
                tint = if (enabled) TextPrimary else TextTertiary,
                modifier = Modifier.size(14.dp)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Vertical physical slider track (Instantaneous, smooth, 1:1 hardware drag)
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .width(48.dp)
                .testTag(testTag)
                .pointerInput(enabled, minMb, maxMb) {
                    if (!enabled) return@pointerInput
                    detectTapGestures(
                        onDoubleTap = {
                            TactileHaptics.lightImpact(view)
                            currentOnLevelChange(0)
                        },
                        onTap = { offset ->
                            TactileHaptics.selectionClick(view)
                            val trackHeight = size.height.toFloat().coerceAtLeast(1f)
                            val normFromBottom = (1f - (offset.y / trackHeight)).coerceIn(0f, 1f)
                            val newMb = (minMb + normFromBottom * (maxMb - minMb)).toInt()
                            currentOnLevelChange(newMb)
                        }
                    )
                }
                .pointerInput(enabled, minMb, maxMb) {
                    if (!enabled) return@pointerInput
                    detectDragGestures(
                        onDragStart = { offset ->
                            val trackHeight = size.height.toFloat().coerceAtLeast(1f)
                            val normFromBottom = (1f - (offset.y / trackHeight)).coerceIn(0f, 1f)
                            val newMb = (minMb + normFromBottom * (maxMb - minMb)).toInt()
                            lastHapticStep = newMb / 100
                            TactileHaptics.selectionClick(view)
                            currentOnLevelChange(newMb)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val trackHeight = size.height.toFloat().coerceAtLeast(1f)
                            val normFromBottom = (1f - (change.position.y / trackHeight)).coerceIn(0f, 1f)
                            val newMb = (minMb + normFromBottom * (maxMb - minMb)).toInt()
                            val stepDb = newMb / 100
                            if (stepDb != lastHapticStep) {
                                lastHapticStep = stepDb
                                TactileHaptics.selectionClick(view)
                            }
                            currentOnLevelChange(newMb)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            val trackHeightPx = constraints.maxHeight.toFloat()
            val thumbHeightDp = 24.dp
            val thumbWidthDp = 38.dp

            // Background vertical groove
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(DeepPurpleDark)
            )

            // Center 0 dB guide line
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .height(2.dp)
                    .background(TextTertiary.copy(alpha = 0.5f))
            )

            // Vertical position of knob
            val thumbOffset = ((1f - fraction) * trackHeightPx).coerceIn(0f, trackHeightPx)

            // Tactile hardware fader knob
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        translationY = (thumbOffset - 12.dp.toPx()).coerceIn(
                            0f,
                            (trackHeightPx - 24.dp.toPx()).coerceAtLeast(0f)
                        )
                    }
                    .width(thumbWidthDp)
                    .height(thumbHeightDp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (enabled) {
                            Brush.verticalGradient(
                                listOf(ElectricVioletLight, ElectricViolet)
                            )
                        } else {
                            Brush.verticalGradient(
                                listOf(TextTertiary, DeepPurpleDark)
                            )
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Center metallic groove on knob
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(2.dp)
                        .background(if (enabled) Color.White.copy(alpha = 0.95f) else Color.Gray)
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Quick -1 dB decrement button
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (enabled) DeepPurpleDark else Color.Transparent)
                .clickable(enabled = enabled) {
                    TactileHaptics.selectionClick(view)
                    val step = (maxMb - minMb) / 24
                    val next = (currentLevelMb - step).coerceAtLeast(minMb)
                    currentOnLevelChange(next)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Decrease $frequencyLabel",
                tint = if (enabled) TextPrimary else TextTertiary,
                modifier = Modifier.size(14.dp)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Frequency Label at bottom
        Text(
            text = frequencyLabel,
            color = if (enabled) TextPrimary else TextTertiary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
