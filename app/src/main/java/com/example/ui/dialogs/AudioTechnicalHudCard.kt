package com.example.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Song
import com.example.player.AudioDeviceSpecs
import com.example.ui.theme.DeepPurpleDark
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElectricVioletLight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun AudioTechnicalHudCard(
    song: Song,
    deviceSpecs: AudioDeviceSpecs,
    isBitPerfect: Boolean,
    isGaplessActive: Boolean = false,
    isGaplessEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, ElectricViolet.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(containerColor = DeepPurpleDark.copy(alpha = 0.92f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Header: Source -> Output Engine Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isBitPerfect) Color(0xFF00E676) else ElectricVioletLight)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBitPerfect) "BIT-PERFECT DIRECT" else "HI-RES AUDIO PIPELINE",
                        color = if (isBitPerfect) Color(0xFF00E676) else ElectricVioletLight,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Codec pill tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF241640))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = song.format,
                        color = TextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Primary Stream Specs Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Source specs
                Column {
                    Text(
                        text = "SOURCE FILE",
                        color = TextTertiary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${song.sampleRateHz / 1000.0} kHz • ${song.bitDepth}-bit",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }

                // Bitrate
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "BITRATE",
                        color = TextTertiary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${song.bitrateKbps} kbps",
                        color = ElectricVioletLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }

                // Output Destination
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "HARDWARE SINK",
                        color = TextTertiary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = deviceSpecs.deviceType,
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0x33FFFFFF))
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    HudDetailRow(
                        icon = Icons.Default.Usb,
                        title = "Active Device",
                        value = deviceSpecs.deviceName
                    )
                    HudDetailRow(
                        icon = Icons.Default.Memory,
                        title = "Audio Routing",
                        value = deviceSpecs.routingPath
                    )
                    HudDetailRow(
                        icon = Icons.Default.Headphones,
                        title = "Active Codec",
                        value = deviceSpecs.activeCodec
                    )
                    HudDetailRow(
                        icon = Icons.Default.GraphicEq,
                        title = "DAC Resampling",
                        value = if (isBitPerfect) "Bypassed (Native ${song.sampleRateHz / 1000.0} kHz Output)" else "Direct AudioTrack Sync"
                    )
                    HudDetailRow(
                        icon = Icons.Default.Sync,
                        title = "Gapless Engine",
                        value = if (isGaplessActive) "0ms HW Buffer (Armed)" else if (isGaplessEnabled) "Active (Continuous)" else "Crossfade Mode"
                    )
                    HudDetailRow(
                        icon = Icons.Default.Speed,
                        title = "Channels",
                        value = deviceSpecs.channelConfiguration
                    )
                }
            }
        }
    }
}

@Composable
private fun HudDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(0.45f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ElectricVioletLight,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = title, color = TextTertiary, fontSize = 11.sp)
        }
        Text(
            text = value,
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(0.55f),
            maxLines = 1
        )
    }
}
