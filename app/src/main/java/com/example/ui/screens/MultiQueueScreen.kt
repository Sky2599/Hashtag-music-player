package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.Song
import com.example.player.QueueSlot
import com.example.ui.components.AlbumArtContainer
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
import com.example.ui.util.instantListClickable
import com.example.ui.util.instantTapDown
import androidx.compose.ui.platform.LocalView

/**
 * Multi-Queue Setter Screen:
 * Allows inspecting, editing, reordering, adding songs to, renaming, duplicating,
 * and activating any of the 4 independent Queue Slots (Q1 - Q4) without being
 * forced to only see the currently playing queue.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiQueueScreen(
    queues: List<QueueSlot>,
    activeSlotIndex: Int,
    allSongs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    onActivateQueueSlot: (Int, Int?) -> Unit,
    onAddSongsToSlot: (Int, List<Song>) -> Unit,
    onRemoveSongFromSlot: (Int, Int) -> Unit,
    onMoveSongInSlot: (Int, Int, Int) -> Unit,
    onShuffleSlot: (Int) -> Unit,
    onClearSlot: (Int) -> Unit,
    onRenameSlot: (Int, String) -> Unit,
    onDuplicateSlot: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    // Allow inspecting and editing ANY slot independently of which slot is currently playing!
    var inspectedSlotIndex by remember { mutableIntStateOf(activeSlotIndex.coerceIn(0, 3)) }
    var showAddTracksSheet by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showCopyDialog by remember { mutableStateOf(false) }
    var addSearchQuery by remember { mutableStateOf("") }
    var selectedToAddIds by remember { mutableStateOf(setOf<Long>()) }

    val inspectedSlot = queues.getOrNull(inspectedSlotIndex) ?: queues.firstOrNull() ?: QueueSlot(1, "Queue 1")
    val isThisSlotActivePlaying = inspectedSlotIndex == activeSlotIndex

    val totalDurationMs = remember(inspectedSlot.items) {
        inspectedSlot.items.sumOf { it.durationMs }
    }
    val totalDurationFormatted = remember(totalDurationMs) {
        val totalSec = totalDurationMs / 1000L
        val hrs = totalSec / 3600L
        val mins = (totalSec % 3600L) / 60L
        val secs = totalSec % 60L
        if (hrs > 0) String.format("%dh %02dm", hrs, mins) else String.format("%dm %02ds", mins, secs)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(DeepPurple)
            .testTag("multi_queue_screen")
    ) {
        val isWideTablet = maxWidth >= 720.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isWideTablet) 20.dp else 14.dp, vertical = 6.dp)
        ) {
            // 1. 4-Queue Slot Tabs (Inspect & Switch independently)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                itemsIndexed(queues) { idx, slot ->
                    val isInspected = idx == inspectedSlotIndex
                    val isLivePlaying = idx == activeSlotIndex
                    Card(
                        modifier = Modifier
                            .width(if (isWideTablet) 190.dp else 148.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .then(
                                if (isInspected) {
                                    Modifier.border(1.5.dp, ElectricVioletLight, RoundedCornerShape(14.dp))
                                } else Modifier
                            )
                            .instantTapDown(hapticLightImpact = false) { inspectedSlotIndex = idx }
                            .testTag("queue_slot_tab_$idx"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isInspected -> ElectricViolet.copy(alpha = 0.25f)
                                isLivePlaying -> DeepPurpleSurface
                                else -> DeepPurpleDark
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isLivePlaying) ElectricViolet else DeepPurpleCard)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Q${idx + 1}",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                    if (isLivePlaying) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "ACTIVE",
                                            color = ElectricVioletLight,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                if (isLivePlaying && isPlaying) {
                                    Icon(
                                        imageVector = Icons.Default.Equalizer,
                                        contentDescription = "Playing",
                                        tint = ElectricVioletLight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = slot.name,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            val activeTrackTitle = slot.items.getOrNull(slot.activeIndex)?.title ?: "Empty slot"
                            Text(
                                text = "${slot.items.size} tracks • $activeTrackTitle",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Selected Queue Control Header Card (Musicolet Queue Setter Toolbar)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DeepPurpleSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = inspectedSlot.name,
                                    color = TextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                if (isThisSlotActivePlaying) {
                                    Surface(
                                        color = ElectricViolet.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "Active Queue",
                                            color = ElectricVioletLight,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${inspectedSlot.items.size} songs • Total time: $totalDurationFormatted • Resume track #${if (inspectedSlot.items.isEmpty()) 0 else inspectedSlot.activeIndex + 1}",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { showRenameDialog = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Rename Queue",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { showCopyDialog = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Duplicate Queue to Another Slot",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { onClearSlot(inspectedSlotIndex) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Clear Queue",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action Buttons Row: Activate/Play Slot, Add Songs to Slot, Shuffle Slot
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onActivateQueueSlot(inspectedSlotIndex, null)
                            },
                            enabled = inspectedSlot.items.isNotEmpty(),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(40.dp)
                                .testTag("activate_queue_slot_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricViolet,
                                disabledContainerColor = DeepPurpleDark
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isThisSlotActivePlaying) "Play Active" else "Switch & Play Q${inspectedSlotIndex + 1}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }

                        FilledTonalButton(
                            onClick = {
                                addSearchQuery = ""
                                selectedToAddIds = emptySet()
                                showAddTracksSheet = true
                            },
                            modifier = Modifier
                                .weight(1.1f)
                                .height(40.dp)
                                .testTag("add_songs_to_slot_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = DeepPurpleCard,
                                contentColor = ElectricVioletLight
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlaylistAdd,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Songs", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = { onShuffleSlot(inspectedSlotIndex) },
                            enabled = inspectedSlot.items.size > 1,
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("shuffle_slot_btn"),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Shuffle Queue",
                                tint = ElectricVioletLight,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Track List for Inspected Queue Slot
            if (inspectedSlot.items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(DeepPurpleSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = ElectricVioletLight,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "${inspectedSlot.name} is Empty",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Build an independent queue in Q${inspectedSlotIndex + 1} without interrupting your current playback.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                addSearchQuery = ""
                                selectedToAddIds = emptySet()
                                showAddTracksSheet = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Songs to Q${inspectedSlotIndex + 1}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(
                        items = inspectedSlot.items,
                        key = { index, song -> "${inspectedSlotIndex}_${index}_${song.id}" },
                        contentType = { _, _ -> "queue_slot_song_row" }
                    ) { index, song ->
                        val isSlotActiveTrack = index == inspectedSlot.activeIndex
                        val isActuallyPlayingNow = isThisSlotActivePlaying && currentSong?.id == song.id && isPlaying

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(62.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .instantListClickable {
                                    // Tap any song in any queue slot to make that slot active and play from this index!
                                    onActivateQueueSlot(inspectedSlotIndex, index)
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isSlotActiveTrack -> ElectricViolet.copy(alpha = 0.22f)
                                    else -> DeepPurpleDark.copy(alpha = 0.85f)
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Track position number or Equalizer icon
                                Box(
                                    modifier = Modifier.width(28.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isActuallyPlayingNow) {
                                        Icon(
                                            imageVector = Icons.Default.Equalizer,
                                            contentDescription = "Playing",
                                            tint = ElectricVioletLight,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "${index + 1}",
                                            color = if (isSlotActiveTrack) ElectricVioletLight else TextTertiary,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSlotActiveTrack) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }

                                AlbumArtContainer(
                                    albumArtUri = song.albumArtUri,
                                    size = 44.dp,
                                    cornerRadius = 8.dp,
                                    contentDescription = song.title
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        color = if (isSlotActiveTrack) ElectricVioletLight else TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSlotActiveTrack) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${song.artist} • ${song.durationFormatted}",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Reorder Up / Down & Remove buttons
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                TactileHaptics.selectionClick(view)
                                                onMoveSongInSlot(inspectedSlotIndex, index, index - 1)
                                            }
                                        },
                                        enabled = index > 0,
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = "Move Up",
                                            tint = if (index > 0) TextSecondary else TextTertiary.copy(alpha = 0.3f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            if (index < inspectedSlot.items.size - 1) {
                                                TactileHaptics.selectionClick(view)
                                                onMoveSongInSlot(inspectedSlotIndex, index, index + 1)
                                            }
                                        },
                                        enabled = index < inspectedSlot.items.size - 1,
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = "Move Down",
                                            tint = if (index < inspectedSlot.items.size - 1) TextSecondary else TextTertiary.copy(alpha = 0.3f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            TactileHaptics.lightImpact(view)
                                            onRemoveSongFromSlot(inspectedSlotIndex, index)
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Remove from Queue",
                                            tint = TextTertiary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Songs to Selected Queue Slot Sheet
        if (showAddTracksSheet) {
            val filteredLibrary = remember(allSongs, addSearchQuery) {
                if (addSearchQuery.isBlank()) {
                    allSongs
                } else {
                    allSongs.filter {
                        it.title.contains(addSearchQuery, ignoreCase = true) ||
                            it.artist.contains(addSearchQuery, ignoreCase = true) ||
                            it.album.contains(addSearchQuery, ignoreCase = true)
                    }
                }
            }

            ModalBottomSheet(
                onDismissRequest = { showAddTracksSheet = false },
                containerColor = DeepPurpleSurface,
                contentColor = TextPrimary
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Add Songs to ${inspectedSlot.name}",
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${selectedToAddIds.size} selected to append",
                                color = ElectricVioletLight,
                                fontSize = 12.sp
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = {
                                    selectedToAddIds = if (selectedToAddIds.size == filteredLibrary.size) {
                                        emptySet()
                                    } else {
                                        filteredLibrary.map { it.id }.toSet()
                                    }
                                }
                            ) {
                                Text(
                                    text = if (selectedToAddIds.size == filteredLibrary.size) "Clear" else "Select All",
                                    color = ElectricVioletLight,
                                    fontSize = 12.sp
                                )
                            }

                            Button(
                                onClick = {
                                    val songsToAdd = allSongs.filter { it.id in selectedToAddIds }
                                    onAddSongsToSlot(inspectedSlotIndex, songsToAdd)
                                    showAddTracksSheet = false
                                },
                                enabled = selectedToAddIds.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Add (${selectedToAddIds.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = addSearchQuery,
                        onValueChange = { addSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search tracks to add...", color = TextTertiary, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricViolet,
                            unfocusedBorderColor = DeepPurpleCard,
                            focusedContainerColor = DeepPurpleDark,
                            unfocusedContainerColor = DeepPurpleDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredLibrary, key = { it.id }, contentType = { "library_row" }) { song ->
                            val isSelected = song.id in selectedToAddIds
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) ElectricViolet.copy(alpha = 0.25f) else DeepPurpleDark)
                                    .clickable {
                                        selectedToAddIds = if (isSelected) {
                                            selectedToAddIds - song.id
                                        } else {
                                            selectedToAddIds + song.id
                                        }
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AlbumArtContainer(
                                    albumArtUri = song.albumArtUri,
                                    size = 42.dp,
                                    cornerRadius = 6.dp,
                                    contentDescription = song.title
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${song.artist} • ${song.durationFormatted}",
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = ElectricVioletLight
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Rename Queue Slot Dialog
        if (showRenameDialog) {
            var newNameText by remember(inspectedSlot.name) { mutableStateOf(inspectedSlot.name) }
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                containerColor = DeepPurpleSurface,
                title = {
                    Text("Rename Queue Q${inspectedSlotIndex + 1}", color = TextPrimary, fontWeight = FontWeight.Bold)
                },
                text = {
                    OutlinedTextField(
                        value = newNameText,
                        onValueChange = { newNameText = it },
                        singleLine = true,
                        label = { Text("Queue Name", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricViolet,
                            unfocusedBorderColor = DeepPurpleCard,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newNameText.isNotBlank()) {
                                onRenameSlot(inspectedSlotIndex, newNameText.trim())
                            }
                            showRenameDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }

        // Copy / Duplicate Queue Slot Dialog
        if (showCopyDialog) {
            AlertDialog(
                onDismissRequest = { showCopyDialog = false },
                containerColor = DeepPurpleSurface,
                title = {
                    Text(
                        text = "Copy Q${inspectedSlotIndex + 1} to Another Slot",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Choose which queue slot should receive a copy of ${inspectedSlot.name} (${inspectedSlot.items.size} tracks):",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        queues.forEachIndexed { targetIdx, targetSlot ->
                            if (targetIdx != inspectedSlotIndex) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onDuplicateSlot(inspectedSlotIndex, targetIdx)
                                            showCopyDialog = false
                                            inspectedSlotIndex = targetIdx
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = DeepPurpleDark)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Q${targetIdx + 1}: ${targetSlot.name}",
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${targetSlot.items.size} tracks",
                                            color = ElectricVioletLight,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showCopyDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }
    }
}
