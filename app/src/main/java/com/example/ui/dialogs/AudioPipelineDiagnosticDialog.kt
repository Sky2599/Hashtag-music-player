package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Song
import com.example.player.AudioDeviceSpecs
import com.example.ui.theme.DeepPurpleDark
import com.example.ui.theme.DeepPurpleSurface
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElectricVioletLight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

/**
 * Dedicated, full-screen diagnostic dialog displaying complete technical specifications
 * for the audio source file, DSP processing chain, and hardware output sink.
 */
@Composable
fun AudioPipelineDiagnosticDialog(
    song: Song?,
    deviceSpecs: AudioDeviceSpecs,
    isBitPerfect: Boolean,
    isGaplessActive: Boolean = false,
    isGaplessEnabled: Boolean = true,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DeepPurpleSurface),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 620.dp)
                .testTag("audio_pipeline_diagnostic_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ElectricViolet.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = ElectricVioletLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Audio Pipeline Diagnostic",
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isBitPerfect) "Direct Bit-Perfect HW Mode" else "Low-Latency AAudio Engine",
                                color = if (isBitPerfect) Color(0xFF00E676) else ElectricVioletLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Source Track Specification Section
                    item {
                        DiagnosticSectionCard(title = "SOURCE AUDIO STREAM") {
                            if (song != null) {
                                DiagnosticMetricRow("Title", song.title)
                                DiagnosticMetricRow("Format / Codec", song.format)
                                DiagnosticMetricRow("Sample Rate", "${song.sampleRateHz} Hz (${song.sampleRateHz / 1000.0} kHz)")
                                DiagnosticMetricRow("Bit Depth", "${song.bitDepth}-Bit " + if (song.bitDepth >= 24) "Lossless Hi-Res" else "Standard PCM")
                                DiagnosticMetricRow("Bitrate", "${song.bitrateKbps} kbps")
                                DiagnosticMetricRow("Channels", "2 Channels (Stereo)")
                                if (song.path.isNotEmpty()) {
                                    DiagnosticMetricRow("File Path", song.path.substringAfterLast('/'))
                                }
                            } else {
                                Text(
                                    text = "No track currently loaded",
                                    color = TextTertiary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // 2. Real-Time DSP Processing Pipeline Section
                    item {
                        DiagnosticSectionCard(title = "32-BIT FLOAT DSP ENGINE") {
                            DiagnosticMetricRow("Processing Architecture", "32-Bit Floating Point Pipeline")
                            DiagnosticMetricRow("Equalizer Engine", "Biquad Parametric Filters (Direct PCM)")
                            DiagnosticMetricRow("Limiter & Headroom", "Adaptive Soft-Knee Limiter (0 dBFS)")
                            DiagnosticMetricRow(
                                "Gapless Engine",
                                if (isGaplessActive) "Armed (0ms Hardware Transition)" else if (isGaplessEnabled) "Continuous Buffer Pre-roll" else "Crossfade Enabled"
                            )
                            DiagnosticMetricRow("Waveform Extractor", "Hardware Decoded RMS Envelope")
                        }
                    }

                    // 3. Hardware Sink & Output Destination Section
                    item {
                        DiagnosticSectionCard(title = "OUTPUT HARDWARE SINK") {
                            DiagnosticMetricRow("Active Device", deviceSpecs.deviceName)
                            DiagnosticMetricRow("Sink Type", deviceSpecs.deviceType)
                            DiagnosticMetricRow("Audio Routing", deviceSpecs.routingPath)
                            DiagnosticMetricRow("Active Codec Profile", deviceSpecs.activeCodec)
                            DiagnosticMetricRow("Max HW Sample Rate", deviceSpecs.maxSampleRate)
                            DiagnosticMetricRow(
                                "DAC Resampling",
                                if (isBitPerfect) "Bypassed (Bit-Perfect Direct)" else "Synchronized AAudio Sink"
                            )
                            DiagnosticMetricRow("Channel Mode", deviceSpecs.channelConfiguration)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Close Diagnostic",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DeepPurpleDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                color = ElectricVioletLight,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun DiagnosticMetricRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal
        )
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
    }
}
