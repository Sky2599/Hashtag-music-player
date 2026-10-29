package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.ui.components.LazyColumnFastScrollbar
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
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
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Song
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
import androidx.compose.ui.platform.LocalView

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SongsScreen(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    isScanning: Boolean = false,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlayNext: (List<Song>) -> Unit,
    onAddToQueue: (List<Song>) -> Unit,
    onSetupQueue: (List<Song>, Boolean) -> Unit,
    onAddToPlaylist: (Song) -> Unit = {},
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSongIds by remember { mutableStateOf(setOf<Long>()) }
    var songForDetails by remember { mutableStateOf<Song?>(null) }
    val isMultiSelectMode = selectedSongIds.isNotEmpty()
    val listState = rememberLazyListState()

    val filteredSongs = remember(songs, searchQuery) {
        if (searchQuery.isBlank()) {
            songs
        } else {
            songs.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                    it.artist.contains(searchQuery, ignoreCase = true) ||
                    it.album.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val selectedSongs = remember(selectedSongIds, songs) {
        songs.filter { it.id in selectedSongIds }
    }

    val songsListContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        // Multi-Select Action Header Bar (Musicolet style) or Normal Search Field
        if (isMultiSelectMode) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                color = DeepPurpleSurface,
                tonalElevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { selectedSongIds = emptySet() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel Selection",
                                    tint = TextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${selectedSongIds.size} Selected",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = {
                                    selectedSongIds = if (selectedSongIds.size == filteredSongs.size) {
                                        emptySet()
                                    } else {
                                        filteredSongs.map { it.id }.toSet()
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricViolet)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SelectAll,
                                    contentDescription = null,
                                    tint = ElectricVioletLight,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (selectedSongIds.size == filteredSongs.size) "Clear" else "All",
                                    color = ElectricVioletLight,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Queue actions row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onSetupQueue(selectedSongs, true)
                                selectedSongIds = emptySet()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("multi_select_play_now"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Play", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                onPlayNext(selectedSongs)
                                selectedSongIds = emptySet()
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .height(38.dp)
                                .testTag("multi_select_play_next"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepPurpleCard),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlaylistPlay,
                                contentDescription = null,
                                tint = ElectricVioletLight,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Play Next", color = ElectricVioletLight, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                onAddToQueue(selectedSongs)
                                selectedSongIds = emptySet()
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .height(38.dp)
                                .testTag("multi_select_add_queue"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepPurpleCard),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlaylistAdd,
                                contentDescription = null,
                                tint = ElectricVioletLight,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Queue", color = ElectricVioletLight, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        } else {
            // Regular Search Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("songs_search_field"),
                placeholder = { Text("Search songs, artists, albums...", color = TextTertiary, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DeepPurpleSurface,
                    unfocusedContainerColor = DeepPurpleDark,
                    focusedBorderColor = ElectricViolet,
                    unfocusedBorderColor = DeepPurpleSurface,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
        }

        if (filteredSongs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isScanning && songs.isEmpty())
                        "Indexing audio archive..."
                    else if (searchQuery.isNotEmpty())
                        "No songs match '$searchQuery'"
                    else
                        "No songs found.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 14.dp, end = 16.dp)
                        .testTag("songs_list"),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                items(filteredSongs, key = { it.id }, contentType = { "song_row" }) { song ->
                    val isCurrent = currentSong?.id == song.id
                    val isSelected = song.id in selectedSongIds

                    SongItemRow(
                        song = song,
                        isCurrent = isCurrent,
                        isSelected = isSelected,
                        isMultiSelectMode = isMultiSelectMode,
                        isPlaying = isPlaying,
                        onClick = {
                            if (isMultiSelectMode) {
                                selectedSongIds = if (isSelected) {
                                    selectedSongIds - song.id
                                } else {
                                    selectedSongIds + song.id
                                }
                            } else {
                                onSongClick(song, filteredSongs)
                            }
                        },
                        onLongClick = {
                            if (isMultiSelectMode) {
                                selectedSongIds = if (isSelected) {
                                    selectedSongIds - song.id
                                } else {
                                    selectedSongIds + song.id
                                }
                            } else {
                                // Musicolet long-press: Show Song Info & Queue Options Dialog
                                songForDetails = song
                            }
                        },
                        onInfoClick = { songForDetails = song }
                    )
                }
            }

            LazyColumnFastScrollbar(
                    listState = listState,
                    itemCount = filteredSongs.size,
                    getSectionText = { index ->
                        filteredSongs.getOrNull(index)?.title?.take(1)?.uppercase() ?: ""
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(top = 4.dp, bottom = 90.dp)
                )
            }
        }
    }
    }

    val activeInspectSong = songForDetails ?: currentSong ?: filteredSongs.firstOrNull()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(DeepPurple)
    ) {
        val isTablet = maxWidth >= 720.dp

        if (isTablet) {
            // Adaptive 2-Column Split View on Tablets (Left: Song List, Right: Track Inspector & Audio Specs)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Column: Songs List
                Box(
                    modifier = Modifier
                        .weight(1.15f)
                        .fillMaxHeight()
                ) {
                    songsListContent()
                }

                // Right Column: Track Inspector Pane
                Card(
                    modifier = Modifier
                        .weight(0.85f)
                        .fillMaxHeight()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepPurpleSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    if (activeInspectSong != null) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 8.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            item {
                                SongInfoBottomSheetContent(
                                    song = activeInspectSong,
                                    onPlayNow = { onSongClick(activeInspectSong, filteredSongs) },
                                    onPlayNext = { onPlayNext(listOf(activeInspectSong)) },
                                    onAddToQueue = { onAddToQueue(listOf(activeInspectSong)) },
                                    onAddToPlaylist = { onAddToPlaylist(activeInspectSong) },
                                    onStartMultiSelect = {
                                        selectedSongIds = selectedSongIds + activeInspectSong.id
                                    }
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Select any track from your library to inspect its audiophile specs, metadata, and lyrics.",
                                color = TextSecondary,
                                fontSize = 14.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        } else {
            // Mobile Single-Column View
            songsListContent()

            // Musicolet Long-Press Song Info & Queue Options Bottom Sheet for Mobile
            songForDetails?.let { detailSong ->
                ModalBottomSheet(
                    onDismissRequest = { songForDetails = null },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = DeepPurpleSurface,
                    contentColor = TextPrimary
                ) {
                    SongInfoBottomSheetContent(
                        song = detailSong,
                        onPlayNow = {
                            onSongClick(detailSong, filteredSongs)
                            songForDetails = null
                        },
                        onPlayNext = {
                            onPlayNext(listOf(detailSong))
                            songForDetails = null
                        },
                        onAddToQueue = {
                            onAddToQueue(listOf(detailSong))
                            songForDetails = null
                        },
                        onAddToPlaylist = {
                            onAddToPlaylist(detailSong)
                            songForDetails = null
                        },
                        onStartMultiSelect = {
                            selectedSongIds = selectedSongIds + detailSong.id
                            songForDetails = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SongInfoBottomSheetContent(
    song: Song,
    onPlayNow: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onStartMultiSelect: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("song_info_sheet")
    ) {
        // Track Header Card
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlbumArtContainer(
                albumArtUri = song.albumArtUri,
                song = song,
                size = 64.dp,
                cornerRadius = 10.dp,
                contentDescription = song.title
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    color = ElectricVioletLight,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.album,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onPlayNow,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("action_play_now"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Play Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            FilledTonalButton(
                onClick = onPlayNext,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("action_play_next"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = DeepPurpleCard,
                    contentColor = ElectricVioletLight
                )
            ) {
                Icon(Icons.Default.PlaylistPlay, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Play Next", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            FilledTonalButton(
                onClick = onAddToQueue,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("action_add_to_queue"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = DeepPurpleCard,
                    contentColor = ElectricVioletLight
                )
            ) {
                Icon(Icons.Default.PlaylistAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Queue", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilledTonalButton(
                onClick = onAddToPlaylist,
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .testTag("action_add_to_playlist"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = DeepPurpleCard,
                    contentColor = ElectricVioletLight
                )
            ) {
                Icon(Icons.Default.PlaylistAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add to Playlist", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = onStartMultiSelect,
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .testTag("action_select_song"),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DeepPurpleCard)
            ) {
                Icon(Icons.Default.SelectAll, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Multi-Select", color = TextSecondary, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = DeepPurpleDark)
        Spacer(modifier = Modifier.height(12.dp))

        // Detailed Track & Audio Information (Musicolet style)
        Text(
            text = "Track Information",
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        InfoRow(label = "Duration", value = song.durationFormatted)
        InfoRow(label = "Format & Codec", value = song.format)
        InfoRow(label = "Bitrate", value = "${song.bitrateKbps} kbps")
        InfoRow(label = "Sample Rate", value = "${song.sampleRateHz / 1000.0} kHz")
        InfoRow(label = "Bit Depth", value = "${song.bitDepth}-bit")
        InfoRow(label = "MIME Type", value = song.mimeType)
        if (song.sizeBytes > 0) {
            val sizeMb = song.sizeBytes / (1024f * 1024f)
            InfoRow(label = "File Size", value = String.format("%.2f MB", sizeMb))
        }
        if (song.path.isNotBlank()) {
            InfoRow(label = "Location", value = song.path)
        }
        InfoRow(label = "Play Count", value = "${song.playCount} times")

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            color = TextTertiary,
            fontSize = 12.sp,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private val SongItemCardShape = RoundedCornerShape(10.dp)
private val SongThumbCornerDp = 8.dp
private val TechSpecsTagShape = RoundedCornerShape(4.dp)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongItemRow(
    song: Song,
    isCurrent: Boolean,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(SongItemCardShape)
            .then(
                if (isSelected) {
                    Modifier.border(1.5.dp, ElectricVioletLight, SongItemCardShape)
                } else Modifier
            )
            .instantListClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("song_item_${song.id}"),
        shape = SongItemCardShape,
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> ElectricViolet.copy(alpha = 0.28f)
                isCurrent -> DeepPurpleSurface
                else -> DeepPurpleDark.copy(alpha = 0.7f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox indicator in multi-select mode
            if (isMultiSelectMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (isSelected) "Selected" else "Unselected",
                    tint = if (isSelected) ElectricVioletLight else TextTertiary,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 4.dp)
                )
            }

            // Album thumbnail
            AlbumArtContainer(
                albumArtUri = song.albumArtUri,
                song = song,
                size = 48.dp,
                cornerRadius = SongThumbCornerDp,
                contentDescription = song.title
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Track metadata inline
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.title,
                    color = if (isCurrent) ElectricVioletLight else TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${song.artist} • ${song.album}",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Technical specs tag
                Box(
                    modifier = Modifier
                        .clip(TechSpecsTagShape)
                        .background(DeepPurpleDark)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = song.techSpecsTag,
                        color = ElectricVioletLight,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isCurrent && isPlaying) {
                Icon(
                    imageVector = Icons.Default.Equalizer,
                    contentDescription = "Playing",
                    tint = ElectricVioletLight,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Text(
                    text = song.durationFormatted,
                    color = TextTertiary,
                    fontSize = 12.sp
                )
            }

            // More info icon button
            IconButton(
                onClick = onInfoClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Song Info",
                    tint = TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

