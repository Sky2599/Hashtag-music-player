package com.example.ui.dialogs

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.player.AbLoopState
import com.example.ui.theme.DeepPurpleDark
import com.example.ui.theme.DeepPurpleSurface
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElectricVioletLight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun PrecisionPlaybackDialog(
    currentPosMs: Long,
    loopState: AbLoopState,
    onSetPointA: (Long) -> Unit,
    onSetPointB: (Long) -> Unit,
    onClearLoop: () -> Unit,
    onToggleLoop: (Boolean) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = !isLandscape)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DeepPurpleSurface),
            modifier = Modifier
                .then(
                    if (isLandscape) Modifier.widthIn(min = 540.dp, max = 680.dp).padding(horizontal = 16.dp, vertical = 8.dp)
                    else Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                )
                .testTag("precision_playback_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(if (isLandscape) 14.dp else 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Loop,
                            contentDescription = null,
                            tint = ElectricVioletLight,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "A-B Looper & Pitch Control",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (isLandscape) {
                    // Landscape 2-column layout to prevent crunched sliders
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Left Column: A-B Repeat Loop Card
                        Box(modifier = Modifier.weight(1f)) {
                            AbLoopCard(
                                loopState = loopState,
                                currentPosMs = currentPosMs,
                                onToggleLoop = onToggleLoop,
                                onSetPointA = onSetPointA,
                                onSetPointB = onSetPointB,
                                onClearLoop = onClearLoop
                            )
                        }

                        // Right Column: Speed and Pitch Controls with plenty of room
                        Column(modifier = Modifier.weight(1.1f)) {
                            SpeedPitchControls(
                                loopState = loopState,
                                onSpeedChange = onSpeedChange,
                                onPitchChange = onPitchChange
                            )
                        }
                    }
                } else {
                    // Portrait single-column layout
                    AbLoopCard(
                        loopState = loopState,
                        currentPosMs = currentPosMs,
                        onToggleLoop = onToggleLoop,
                        onSetPointA = onSetPointA,
                        onSetPointB = onSetPointB,
                        onClearLoop = onClearLoop
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    SpeedPitchControls(
                        loopState = loopState,
                        onSpeedChange = onSpeedChange,
                        onPitchChange = onPitchChange
                    )
                }
            }
        }
    }
}

@Composable
private fun AbLoopCard(
    loopState: AbLoopState,
    currentPosMs: Long,
    onToggleLoop: (Boolean) -> Unit,
    onSetPointA: (Long) -> Unit,
    onSetPointB: (Long) -> Unit,
    onClearLoop: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DeepPurpleDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("A-B Repeat Loop", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (loopState.isEnabled) "Loop Active" else "Inactive",
                        color = if (loopState.isEnabled) ElectricVioletLight else TextTertiary,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = loopState.isEnabled,
                    onCheckedChange = onToggleLoop,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = ElectricViolet,
                        checkedThumbColor = TextPrimary
                    ),
                    modifier = Modifier.testTag("ab_loop_switch")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Point A Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (loopState.pointA != null) ElectricViolet.copy(alpha = 0.35f) else DeepPurpleSurface)
                        .clickable { onSetPointA(currentPosMs) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Set [A]", color = ElectricVioletLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = loopState.pointA?.let { formatTimeMs(it) } ?: "--:--",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Point B Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (loopState.pointB != null) ElectricViolet.copy(alpha = 0.35f) else DeepPurpleSurface)
                        .clickable { onSetPointB(currentPosMs) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Set [B]", color = ElectricVioletLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = loopState.pointB?.let { formatTimeMs(it) } ?: "--:--",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Clear button
                IconButton(
                    onClick = onClearLoop,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.RestartAlt,
                        contentDescription = "Clear loop",
                        tint = TextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeedPitchControls(
    loopState: AbLoopState,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DeepPurpleDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Playback Speed Slider (0.5x to 2.0x)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tempo / Speed", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(
                    text = String.format("%.2fx", loopState.playbackSpeed),
                    color = ElectricVioletLight,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Slider(
                value = loopState.playbackSpeed,
                onValueChange = onSpeedChange,
                valueRange = 0.5f..2.0f,
                steps = 14,
                colors = SliderDefaults.colors(
                    thumbColor = ElectricVioletLight,
                    activeTrackColor = ElectricViolet,
                    inactiveTrackColor = DeepPurpleSurface
                ),
                modifier = Modifier.fillMaxWidth().testTag("speed_slider")
            )

            // Speed Quick Preset Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(0.75f to "0.75x", 1.0f to "1.0x", 1.25f to "1.25x", 1.5f to "1.5x").forEach { (speedVal, label) ->
                    val isSelected = Math.abs(loopState.playbackSpeed - speedVal) < 0.05f
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) ElectricViolet.copy(alpha = 0.35f) else DeepPurpleSurface)
                            .clickable { onSpeedChange(speedVal) }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pitch Tuning Slider (-6st to +6st)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pitch Tuning", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = String.format("%+.1f st", loopState.pitchSemiTones),
                        color = ElectricVioletLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    if (loopState.pitchSemiTones != 0.0f) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Reset",
                            color = TextTertiary,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(DeepPurpleSurface)
                                .clickable { onPitchChange(0f) }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Slider(
                value = loopState.pitchSemiTones,
                onValueChange = onPitchChange,
                valueRange = -6f..6f,
                steps = 23,
                colors = SliderDefaults.colors(
                    thumbColor = ElectricVioletLight,
                    activeTrackColor = ElectricViolet,
                    inactiveTrackColor = DeepPurpleSurface
                ),
                modifier = Modifier.fillMaxWidth().testTag("pitch_slider")
            )
        }
    }
}

private fun formatTimeMs(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return String.format("%02d:%02d", m, s)
}
