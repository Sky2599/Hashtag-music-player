package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.CustomPlaylistEntity
import com.example.data.engine.RadioGenerator
import com.example.data.model.RadioMix
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
import java.util.Calendar

data class SmartPlaylistItem(
    val title: String,
    val count: Int,
    val icon: ImageVector,
    val songs: List<Song>
)

@Composable
fun HomeScreen(
    allSongs: List<Song>,
    recentlyAdded: List<Song>,
    mostPlayed: List<Song>,
    favorites: List<Song>,
    customPlaylists: List<CustomPlaylistEntity> = emptyList(),
    currentSong: Song? = null,
    isPlaying: Boolean = false,
    isScanning: Boolean = false,
    currentPositionMsProvider: () -> Long = { 0L },
    durationMs: Long = 1L,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlayPauseClick: () -> Unit = {},
    onCreatePlaylistClick: () -> Unit = {},
    onPlaylistClick: (CustomPlaylistEntity) -> Unit = {},
    onQuickShuffleClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val greetingData = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> Triple("Good morning", "Start your day with your favorites", Icons.Default.WbSunny)
            in 12..16 -> Triple("Good afternoon", "Keep the groove flowing", Icons.Default.WbSunny)
            in 17..21 -> Triple("Good evening", "Unwind and enjoy your music", Icons.Default.WbTwilight)
            else -> Triple("Good night", "Late night listening sanctuary", Icons.Default.Bedtime)
        }
    }

    val smartPlaylists = remember(allSongs, recentlyAdded, mostPlayed, favorites) {
        listOf(
            SmartPlaylistItem("Recently Added", recentlyAdded.size, Icons.Default.History, recentlyAdded),
            SmartPlaylistItem("Most Played", mostPlayed.size, Icons.Default.TrendingUp, mostPlayed),
            SmartPlaylistItem("Favourites", favorites.size, Icons.Default.Favorite, favorites),
            SmartPlaylistItem("All Tracks", allSongs.size, Icons.Default.QueueMusic, allSongs)
        )
    }

    val radioMixes by androidx.compose.runtime.produceState(
        initialValue = emptyList<RadioMix>(),
        key1 = allSongs,
        key2 = mostPlayed,
        key3 = favorites
    ) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            RadioGenerator.generateRadioMixes(allSongs, mostPlayed, recentlyAdded, favorites)
        }
    }

    val filteredMixes = radioMixes
    val resumeTrack = currentSong ?: recentlyAdded.firstOrNull() ?: allSongs.firstOrNull()

    if (allSongs.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DeepPurple)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(DeepPurpleDark),
                    contentAlignment = Alignment.Center
                ) {
                    if (isScanning) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = ElectricViolet,
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = "No music found",
                            tint = ElectricViolet,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isScanning) "Loading Music Archive..." else "No Audio Files Found",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isScanning)
                        "Indexing audio tracks and preparing your library..."
                    else
                        "No music scanned on device storage yet.\nTap the refresh button in the top bar to index your music files.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        return
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(DeepPurple)
    ) {
        val isTabletWide = maxWidth >= 720.dp

        if (isTabletWide) {
            // Adaptive 2-column split dashboard on tablets
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Left Column: Greetings, Jump Back In, Moods, Dynamic Radios
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        HomeGreetingCard(
                            greetingData = greetingData,
                            allSongsCount = allSongs.size,
                            smartPlaylists = smartPlaylists,
                            onQuickShuffleClick = onQuickShuffleClick
                        )
                    }

                    if (resumeTrack != null) {
                        item {
                            HomeJumpBackInCard(
                                resumeTrack = resumeTrack,
                                allSongs = allSongs,
                                currentSong = currentSong,
                                isPlaying = isPlaying,
                                durationMs = durationMs,
                                currentPositionMsProvider = currentPositionMsProvider,
                                onSongClick = onSongClick,
                                onPlayPauseClick = onPlayPauseClick
                            )
                        }
                    }



                    if (filteredMixes.isNotEmpty()) {
                        item {
                            HomeSmartRadiosSection(
                                filteredMixes = filteredMixes,
                                onSongClick = onSongClick
                            )
                        }
                    }
                }

                // Vertical Divider between columns
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(DeepPurpleDark)
                )

                // Right Column: Smart Playlists, Custom Playlists, Top Songs
                LazyColumn(
                    modifier = Modifier
                        .weight(1.15f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        HomeSmartAndCustomPlaylistsSection(
                            customPlaylists = customPlaylists,
                            smartPlaylists = smartPlaylists,
                            allSongs = allSongs,
                            onCreatePlaylistClick = onCreatePlaylistClick,
                            onPlaylistClick = onPlaylistClick,
                            onSmartPlaylistClick = { playlist ->
                                if (playlist.songs.isNotEmpty()) {
                                    onSongClick(playlist.songs.first(), playlist.songs)
                                }
                            }
                        )
                    }

                    item {
                        HomeTopSongsSection(
                            mostPlayed = mostPlayed,
                            allSongs = allSongs,
                            onSongClick = onSongClick
                        )
                    }
                }
            }
        } else {
            // Standard single-column phone layout
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    HomeGreetingCard(
                        greetingData = greetingData,
                        allSongsCount = allSongs.size,
                        smartPlaylists = smartPlaylists,
                        onQuickShuffleClick = onQuickShuffleClick
                    )
                }

                if (resumeTrack != null) {
                    item {
                        HomeJumpBackInCard(
                            resumeTrack = resumeTrack,
                            allSongs = allSongs,
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            durationMs = durationMs,
                            currentPositionMsProvider = currentPositionMsProvider,
                            onSongClick = onSongClick,
                            onPlayPauseClick = onPlayPauseClick
                        )
                    }
                }



                item {
                    HomeSmartAndCustomPlaylistsSection(
                        customPlaylists = customPlaylists,
                        smartPlaylists = smartPlaylists,
                        allSongs = allSongs,
                        onCreatePlaylistClick = onCreatePlaylistClick,
                        onPlaylistClick = onPlaylistClick,
                        onSmartPlaylistClick = { playlist ->
                            if (playlist.songs.isNotEmpty()) {
                                onSongClick(playlist.songs.first(), playlist.songs)
                            }
                        }
                    )
                }

                if (filteredMixes.isNotEmpty()) {
                    item {
                        HomeSmartRadiosSection(
                            filteredMixes = filteredMixes,
                            onSongClick = onSongClick
                        )
                    }
                }

                item {
                    HomeTopSongsSection(
                        mostPlayed = mostPlayed,
                        allSongs = allSongs,
                        onSongClick = onSongClick
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeGreetingCard(
    greetingData: Triple<String, String, ImageVector>,
    allSongsCount: Int,
    smartPlaylists: List<SmartPlaylistItem>,
    onQuickShuffleClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_greeting_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DeepPurpleCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = greetingData.third,
                        contentDescription = null,
                        tint = ElectricVioletLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = greetingData.first,
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = greetingData.second,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$allSongsCount tracks • ${smartPlaylists.sumOf { it.count } / 2} played",
                    color = ElectricVioletLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(ElectricViolet)
                    .clickable { onQuickShuffleClick() }
                    .padding(12.dp)
                    .testTag("home_quick_shuffle_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Quick Shuffle",
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun HomeJumpBackInCard(
    resumeTrack: Song,
    allSongs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    durationMs: Long,
    currentPositionMsProvider: () -> Long,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlayPauseClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onSongClick(resumeTrack, allSongs) }
            .testTag("jump_back_in_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DeepPurpleSurface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlbumArtContainer(
                    song = resumeTrack,
                    albumArtUri = resumeTrack.albumArtUri,
                    size = 52.dp,
                    cornerRadius = 10.dp,
                    contentDescription = resumeTrack.title
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isPlaying && currentSong != null) "NOW PLAYING" else "JUMP BACK IN",
                        color = ElectricVioletLight,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = resumeTrack.title,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${resumeTrack.artist} • ${resumeTrack.album}",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(ElectricViolet)
                        .clickable {
                            if (currentSong != null) {
                                onPlayPauseClick()
                            } else {
                                onSongClick(resumeTrack, allSongs)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying && currentSong?.id == resumeTrack.id) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play or Resume",
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            if (currentSong?.id == resumeTrack.id && durationMs > 0L) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = {
                        (currentPositionMsProvider().toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp)),
                    color = ElectricVioletLight,
                    trackColor = DeepPurpleDark,
                )
            }
        }
    }
}



@Composable
private fun HomeSmartAndCustomPlaylistsSection(
    customPlaylists: List<CustomPlaylistEntity>,
    smartPlaylists: List<SmartPlaylistItem>,
    allSongs: List<Song>,
    onCreatePlaylistClick: () -> Unit,
    onPlaylistClick: (CustomPlaylistEntity) -> Unit,
    onSmartPlaylistClick: (SmartPlaylistItem) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("custom_playlists_section"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DeepPurpleCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlaylistPlay,
                        contentDescription = null,
                        tint = ElectricVioletLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Smart & Custom Playlists",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DeepPurpleDark)
                        .clickable { onCreatePlaylistClick() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("create_playlist_home_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Playlist",
                        tint = ElectricVioletLight,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "New / Rule",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                // 1. New Smart Rule Creator Action Card
                item {
                    Card(
                        modifier = Modifier
                            .width(128.dp)
                            .clickable { onCreatePlaylistClick() },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DeepPurpleSurface)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ElectricViolet.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Create",
                                    tint = ElectricVioletLight,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "New Smart Rule",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Rule Builder",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // 2. Custom & Smart Rule Playlists
                items(customPlaylists, key = { it.id }) { playlist ->
                    val resolvedCount = remember(playlist.songIdsCsv, allSongs) {
                        playlist.resolveSongs(allSongs).size
                    }
                    Card(
                        modifier = Modifier
                            .width(128.dp)
                            .clickable { onPlaylistClick(playlist) }
                            .testTag("custom_playlist_item_${playlist.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DeepPurpleSurface)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(ElectricViolet.copy(alpha = 0.45f), DeepPurpleDark)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlaylistPlay,
                                    contentDescription = null,
                                    tint = ElectricVioletLight,
                                    modifier = Modifier.size(38.dp)
                                )
                                if (playlist.isSmartPlaylist) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(DeepPurpleDark.copy(alpha = 0.85f))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "SMART",
                                            color = ElectricVioletLight,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = playlist.name,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = "$resolvedCount tracks" + if (playlist.isSmartPlaylist) " • Dynamic" else "",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // 3. Built-In Smart Playlists (Favorites, Most Played, Recently Added, Hi-Res)
                items(smartPlaylists, key = { it.title }) { playlist ->
                    Card(
                        modifier = Modifier
                            .width(128.dp)
                            .clickable { onSmartPlaylistClick(playlist) }
                            .testTag("smart_playlist_item_${playlist.title.lowercase().replace(" ", "_")}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DeepPurpleSurface)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(DeepPurpleSurface, DeepPurpleDark)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = playlist.icon,
                                    contentDescription = playlist.title,
                                    tint = ElectricVioletLight,
                                    modifier = Modifier.size(34.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(DeepPurpleDark.copy(alpha = 0.85f))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "AUTO",
                                        color = ElectricVioletLight,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = playlist.title,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = "${playlist.count} tracks • Smart",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSmartRadiosSection(
    filteredMixes: List<RadioMix>,
    onSongClick: (Song, List<Song>) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("smart_radios_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DeepPurpleCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Radio,
                    contentDescription = "Radio",
                    tint = ElectricViolet,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "smart radios & mixes",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "YouTube Music-inspired dynamic mixes and curated flow",
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(filteredMixes, key = { it.id }, contentType = { "radio_mix" }) { mix ->
                    Card(
                        modifier = Modifier
                            .width(160.dp)
                            .clickable {
                                if (mix.tracks.isNotEmpty()) {
                                    onSongClick(mix.tracks.first(), mix.tracks)
                                }
                            }
                            .testTag("radio_item_${mix.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DeepPurpleSurface)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                AlbumArtContainer(
                                    albumArtUri = if (mix.coverArtUri != null) android.net.Uri.parse(mix.coverArtUri) else null,
                                    modifier = Modifier.fillMaxSize(),
                                    cornerRadius = 8.dp,
                                    contentDescription = mix.title
                                )

                                mix.badgeLabel?.let { badge ->
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(6.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.Black.copy(alpha = 0.7f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = badge,
                                            color = ElectricVioletLight,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(6.dp)
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(ElectricVioletLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play Radio",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = mix.title,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = mix.subtitle,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSmartPlaylistsSection(
    smartPlaylists: List<SmartPlaylistItem>,
    onSongClick: (Song, List<Song>) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("smart_playlist_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DeepPurpleCard)
    ) {
        LazyRow(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(smartPlaylists, key = { it.title }, contentType = { "smart_playlist" }) { playlist ->
                Card(
                    modifier = Modifier
                        .width(140.dp)
                        .clickable {
                            if (playlist.songs.isNotEmpty()) {
                                onSongClick(playlist.songs.first(), playlist.songs)
                            }
                        }
                        .testTag("playlist_item_${playlist.title.lowercase().replace(" ", "_")}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepPurpleSurface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ElectricViolet.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = playlist.icon,
                                contentDescription = playlist.title,
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = playlist.title,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "${playlist.count} tracks",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTopSongsSection(
    mostPlayed: List<Song>,
    allSongs: List<Song>,
    onSongClick: (Song, List<Song>) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("top_songs_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DeepPurpleCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "top songs",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Most played in library",
                    color = TextTertiary,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val topList = remember(mostPlayed, allSongs) {
                val played = mostPlayed.filter { it.playCount > 0 }.sortedByDescending { it.playCount }.take(8)
                played.ifEmpty { allSongs.take(5) }
            }

            topList.forEachIndexed { index, song ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSongClick(song, topList) }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "#${index + 1}",
                        color = if (index < 3) ElectricVioletLight else TextTertiary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(28.dp)
                    )

                    AlbumArtContainer(
                        albumArtUri = song.albumArtUri,
                        size = 42.dp,
                        cornerRadius = 6.dp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

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
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (song.playCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(ElectricViolet.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${song.playCount} ${if (song.playCount == 1) "play" else "plays"}",
                                color = ElectricVioletLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(DeepPurpleDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play song",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
