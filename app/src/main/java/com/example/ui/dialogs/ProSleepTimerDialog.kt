package com.example.ui.dialogs

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.player.SleepTimerState
import com.example.ui.theme.DeepPurpleCard
import com.example.ui.theme.DeepPurpleDark
import com.example.ui.theme.DeepPurpleSurface
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElectricVioletLight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun ProSleepTimerDialog(
    sleepTimerState: SleepTimerState,
    onStartTimer: (minutes: Int, stopAtTrackEnd: Boolean, fadeDurationSec: Int) -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMinutes by remember { mutableIntStateOf(30) }
    var stopAtEnd by remember { mutableStateOf(sleepTimerState.stopAtEndOfTrack) }
    var fadeDurationSec by remember { mutableIntStateOf(30) }

    val presetMinutes = listOf(15, 30, 45, 60, 90)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DeepPurpleSurface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = null,
                            tint = ElectricVioletLight,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Sleep Timer",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (sleepTimerState.isActive) {
                    // Active Timer Status
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DeepPurpleDark),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Timer Active",
                                color = ElectricVioletLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            val remainingMinutes = sleepTimerState.remainingSeconds / 60
                            val remainingSecs = sleepTimerState.remainingSeconds % 60
                            Text(
                                text = String.format("%02d:%02d", remainingMinutes, remainingSecs),
                                color = TextPrimary,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold
                            )

                            if (sleepTimerState.currentVolumeMultiplier < 1.0f) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Fading out volume: ${(sleepTimerState.currentVolumeMultiplier * 100).toInt()}%",
                                    color = ElectricVioletLight,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            onCancelTimer()
                            onDismiss()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cancel Timer")
                    }
                } else {
                    // Timer Configuration
                    Text(
                        text = "Turn off playback after:",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Preset chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetMinutes.forEach { mins ->
                            val isSelected = selectedMinutes == mins
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) ElectricViolet else DeepPurpleCard)
                                    .clickable { selectedMinutes = mins }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${mins}m",
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Playback Options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Wait for track to end", color = TextPrimary, fontSize = 13.sp)
                            Text("Avoid cutting off mid-song", color = TextTertiary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = stopAtEnd,
                            onCheckedChange = { stopAtEnd = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = ElectricViolet)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Smooth Volume Fade-Out", color = TextPrimary, fontSize = 13.sp)
                            Text("Gentle 30-second analog fade", color = TextTertiary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = fadeDurationSec > 0,
                            onCheckedChange = { fadeDurationSec = if (it) 30 else 0 },
                            colors = SwitchDefaults.colors(checkedTrackColor = ElectricViolet)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            onStartTimer(selectedMinutes, stopAtEnd, fadeDurationSec)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = TextPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Timer ($selectedMinutes min)", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
