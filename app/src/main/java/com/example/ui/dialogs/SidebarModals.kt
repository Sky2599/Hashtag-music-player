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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Song
import com.example.ui.theme.DeepPurpleCard
import com.example.ui.theme.DeepPurpleDark
import com.example.ui.theme.DeepPurpleSurface
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElectricVioletLight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun FoldersDialog(
    allSongs: List<Song>,
    onSongClick: (Song, List<Song>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFolder by remember { mutableStateOf<String?>(null) }

    // Group songs by parent folder
    val folders = remember(allSongs) {
        allSongs.groupBy { song ->
            val path = song.path
            if (path.contains('/')) {
                path.substringBeforeLast('/')
            } else "Internal Storage"
        }.map { (folderPath, songs) ->
            val folderName = folderPath.substringAfterLast('/')
            Triple(folderName, folderPath, songs)
        }.sortedBy { it.first.lowercase() }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(520.dp)
                .testTag("folders_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DeepPurpleSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (selectedFolder == null) "Audio Folders" else selectedFolder!!.substringAfterLast('/'),
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (selectedFolder == null) "${folders.size} folders found" else "Tap a song to play",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    IconButton(
                        onClick = {
                            if (selectedFolder != null) {
                                selectedFolder = null
                            } else {
                                onDismiss()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = DeepPurpleDark)

                if (selectedFolder == null) {
                    // List of folders
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(folders) { (folderName, fullPath, songs) ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedFolder = fullPath },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = DeepPurpleCard)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = ElectricVioletLight,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = folderName,
                                                color = TextPrimary,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${songs.size} audio files",
                                                color = TextSecondary,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            if (songs.isNotEmpty()) {
                                                onSongClick(songs.first(), songs)
                                                onDismiss()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play folder",
                                            tint = ElectricVioletLight
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Songs in selected folder
                    val folderSongs = folders.firstOrNull { it.second == selectedFolder }?.third ?: emptyList()
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(folderSongs) { song ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onSongClick(song, folderSongs)
                                        onDismiss()
                                    }
                                    .background(DeepPurpleCard)
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${song.artist} • ${song.durationFormatted}",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = ElectricVioletLight
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ListeningStatsDialog(
    allSongs: List<Song>,
    onDismiss: () -> Unit
) {
    val totalSongs = allSongs.size
    val totalDurationMs = allSongs.sumOf { it.durationMs }
    val totalHours = totalDurationMs / (1000 * 60 * 60)
    val totalMinutes = (totalDurationMs / (1000 * 60)) % 60
    val totalPlays = allSongs.sumOf { it.playCount }
    val favoritesCount = allSongs.count { it.isFavorite }
    val hiResCount = allSongs.count { it.bitrateKbps >= 320 || it.format == "FLAC" || it.format == "WAV" }
    val topPlayedSong = allSongs.maxByOrNull { it.playCount }
    val topArtist = com.example.data.util.ArtistNameParser.groupSongsBySmartArtist(allSongs)
        .maxByOrNull { it.value.sumOf { s -> s.playCount } }?.key

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = ElectricVioletLight,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Listening Statistics", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(title = "Total Library Time", value = "${totalHours}h ${totalMinutes}m ($totalSongs songs)")
                StatCard(title = "Total Plays Logged", value = "$totalPlays tracks played")
                if (topPlayedSong != null && topPlayedSong.playCount > 0) {
                    StatCard(title = "Most Played Track", value = "${topPlayedSong.title} (${topPlayedSong.playCount} plays)")
                }
                if (!topArtist.isNullOrBlank()) {
                    StatCard(title = "Top Artist", value = topArtist)
                }
                StatCard(title = "Favorites", value = "$favoritesCount tracks bookmarked")
                StatCard(title = "Hi-Res / Lossless", value = "$hiResCount tracks (320kbps+ / FLAC)")
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Close", color = TextPrimary)
            }
        },
        containerColor = DeepPurpleSurface
    )
}

@Composable
private fun StatCard(title: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DeepPurpleCard)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = title, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text(text = value, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Headphones,
                    contentDescription = null,
                    tint = ElectricVioletLight,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("About Hashtag music player", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Hashtag music player v3.8.0",
                    color = ElectricVioletLight,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "A high-performance offline music player engineered with bit-perfect audio architecture, Liquid Glass fluid theme, full-screen tabbed settings, switchable 5/10-band equalizer, and responsive home screen widgets.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• Audio Engine: Dual MediaPlayer with AAudio / OpenSL ES\n• Dynamic Palette: Chroma-adaptive algorithm\n• Local Database: Room SQLite Persistence\n• 100% Offline & Zero Ads",
                    color = TextTertiary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Got It", color = TextPrimary)
            }
        },
        containerColor = DeepPurpleSurface
    )
}

@Composable
fun DonateDialog(onDismiss: () -> Unit) {
    var donated by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.VolunteerActivism,
                    contentDescription = null,
                    tint = Color(0xFFFF6B81),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Support Development", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            if (donated) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFFF6B81),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Thank you so much for your support!",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Your contribution keeps Hashtag music player 100% ad-free and actively developed with advanced high-fidelity audio features.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Hashtag music player is completely free, 100% offline, and contains zero ads or trackers. If you enjoy using this music player, consider buying a coffee to support continued development!",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { donated = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B81)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("☕ Buy Coffee", color = Color.White, fontSize = 13.sp)
                        }
                        Button(
                            onClick = { donated = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("💖 $5 Tip", color = TextPrimary, fontSize = 13.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = DeepPurpleCard),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (donated) "Close" else "Maybe Later", color = TextPrimary)
            }
        },
        containerColor = DeepPurpleSurface
    )
}
