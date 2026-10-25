package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.ui.components.LazyColumnFastScrollbar
import com.example.ui.components.LazyGridFastScrollbar
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import com.example.data.model.Album
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
import com.example.ui.util.instantListClickable

data class ArtistGroup(
    val name: String,
    val songs: List<Song>,
    val albumCount: Int
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    albums: List<Album>,
    allSongs: List<Song>,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlayNext: (List<Song>) -> Unit,
    onAddToQueue: (List<Song>) -> Unit,
    columnsCount: Int = 2,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Albums, 1: Artists
    var selectedAlbumForDetails by remember { mutableStateOf<Album?>(null) }
    var selectedArtistForDetails by remember { mutableStateOf<ArtistGroup?>(null) }
    val sheetState = rememberModalBottomSheetState()

    // Multi-select state for albums
    var selectedAlbumTitles by remember { mutableStateOf(setOf<String>()) }
    val isAlbumMultiSelect = selectedAlbumTitles.isNotEmpty()
    val albumsGridState = rememberLazyGridState()
    val artistsListState = rememberLazyListState()

    // Multi-select state inside the open tracklist sheet
    var sheetSelectedSongIds by remember { mutableStateOf(setOf<Long>()) }

    val sortedAlbums = remember(albums) {
        albums.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title.trim() })
    }

    val artists by androidx.compose.runtime.produceState(
        initialValue = emptyList<ArtistGroup>(),
        key1 = allSongs
    ) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            com.example.data.util.ArtistNameParser.groupSongsBySmartArtist(allSongs)
                .map { (artistName, songList) ->
                    val distinctAlbums = songList.map { it.album }.distinct().size
                    ArtistGroup(artistName, songList, distinctAlbums)
                }
                .sortedBy { it.name.lowercase() }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(DeepPurple)
    ) {
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val isTablet = maxWidth >= 720.dp
        val useSplitView = isTablet || isLandscape
        Column(modifier = Modifier.fillMaxSize()) {
            // View Mode Filter Chips: Albums & Artists
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        selectedAlbumTitles = emptySet()
                    },
                    label = { Text("Albums (${sortedAlbums.size})") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Album,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricViolet,
                        selectedLabelColor = Color.White,
                        containerColor = DeepPurpleDark,
                        labelColor = TextSecondary
                    )
                )

                FilterChip(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        selectedAlbumTitles = emptySet()
                    },
                    label = { Text("Artists (${artists.size})") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricViolet,
                        selectedLabelColor = Color.White,
                        containerColor = DeepPurpleDark,
                        labelColor = TextSecondary
                    )
                )
            }

            // Contextual Multi-Select Action Bar for Albums / Artists
            if (isAlbumMultiSelect) {
                val selectedSongsForBatch = remember(selectedAlbumTitles, allSongs) {
                    allSongs.filter { selectedAlbumTitles.contains(it.album) }
                        .sortedWith(
                            compareBy(
                                { it.album.lowercase() },
                                { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE },
                                { it.title.lowercase() }
                            )
                        )
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DeepPurpleDark,
                    shadowElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${selectedAlbumTitles.size} selected (${selectedSongsForBatch.size} tracks)",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        selectedAlbumTitles = sortedAlbums.map { it.title }.toSet()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SelectAll,
                                        contentDescription = "Select All",
                                        tint = TextSecondary
                                    )
                                }
                                IconButton(onClick = { selectedAlbumTitles = emptySet() }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear Selection",
                                        tint = TextSecondary
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (selectedSongsForBatch.isNotEmpty()) {
                                        onSongClick(selectedSongsForBatch.first(), selectedSongsForBatch)
                                        selectedAlbumTitles = emptySet()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.PlaylistPlay, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Play Batch", fontSize = 12.sp, maxLines = 1)
                            }

                            FilledTonalButton(
                                onClick = {
                                    if (selectedSongsForBatch.isNotEmpty()) {
                                        onPlayNext(selectedSongsForBatch)
                                        selectedAlbumTitles = emptySet()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = DeepPurpleCard),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp), tint = ElectricVioletLight)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Play Next", fontSize = 12.sp, color = TextPrimary, maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = {
                                    if (selectedSongsForBatch.isNotEmpty()) {
                                        onAddToQueue(selectedSongsForBatch)
                                        selectedAlbumTitles = emptySet()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.PlaylistAdd, contentDescription = null, modifier = Modifier.size(18.dp), tint = TextSecondary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Queue", fontSize = 12.sp, color = TextSecondary, maxLines = 1)
                            }
                        }
                    }
                }
            }

            // Main Content: Albums Grid or Artists List
            if (selectedTab == 0) {
                // Albums Grid
                if (sortedAlbums.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No albums found. Tap refresh to scan your audio files.",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                } else if (useSplitView) {
                    val activeAlbum = selectedAlbumForDetails ?: sortedAlbums.firstOrNull()
                    val activeAlbumSongs = remember(activeAlbum, allSongs) {
                        if (activeAlbum == null) emptyList()
                        else allSongs.filter { it.album.equals(activeAlbum.title, ignoreCase = true) }
                            .sortedWith(
                                compareBy(
                                    { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE },
                                    { it.title.lowercase() }
                                )
                            )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1.05f)
                                .fillMaxHeight()
                        ) {
                            LazyVerticalGrid(
                                state = albumsGridState,
                                columns = GridCells.Adaptive(minSize = 140.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(end = 12.dp)
                                    .testTag("albums_grid"),
                                contentPadding = PaddingValues(top = 4.dp, bottom = 90.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(sortedAlbums, key = { it.title }, contentType = { "album_card" }) { album ->
                                    val isSelected = selectedAlbumTitles.contains(album.title) || activeAlbum?.title == album.title

                                    AlbumGridCard(
                                        album = album,
                                        isSelected = isSelected,
                                        isAlbumMultiSelect = isAlbumMultiSelect,
                                        onClick = {
                                            if (isAlbumMultiSelect) {
                                                selectedAlbumTitles = if (selectedAlbumTitles.contains(album.title)) {
                                                    selectedAlbumTitles - album.title
                                                } else {
                                                    selectedAlbumTitles + album.title
                                                }
                                            } else {
                                                selectedAlbumForDetails = album
                                                sheetSelectedSongIds = emptySet()
                                            }
                                        },
                                        onLongClick = {
                                            selectedAlbumTitles = if (selectedAlbumTitles.contains(album.title)) {
                                                selectedAlbumTitles - album.title
                                            } else {
                                                selectedAlbumTitles + album.title
                                            }
                                        }
                                    )
                                }
                            }

                            LazyGridFastScrollbar(
                                gridState = albumsGridState,
                                itemCount = sortedAlbums.size,
                                getSectionText = { index ->
                                    sortedAlbums.getOrNull(index)?.title?.trim()?.take(1)?.uppercase() ?: ""
                                },
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .fillMaxHeight()
                                    .padding(top = 4.dp, bottom = 90.dp)
                            )
                        }

                        Card(
                            modifier = Modifier
                                .weight(0.95f)
                                .fillMaxHeight()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = DeepPurpleSurface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            if (activeAlbum != null) {
                                AlbumTracklistContent(
                                    album = activeAlbum,
                                    albumSongs = activeAlbumSongs,
                                    sheetSelectedSongIds = sheetSelectedSongIds,
                                    onToggleSelectSong = { songId ->
                                        sheetSelectedSongIds = if (sheetSelectedSongIds.contains(songId)) {
                                            sheetSelectedSongIds - songId
                                        } else {
                                            sheetSelectedSongIds + songId
                                        }
                                    },
                                    onSongClick = onSongClick,
                                    onPlayNext = onPlayNext,
                                    onAddToQueue = onAddToQueue,
                                    onClose = null
                                )
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyVerticalGrid(
                            state = albumsGridState,
                            columns = GridCells.Fixed(columnsCount),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 14.dp, end = 16.dp)
                                .testTag("albums_grid"),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                        items(sortedAlbums, key = { it.title }, contentType = { "album_card" }) { album ->
                            val isSelected = selectedAlbumTitles.contains(album.title)

                            AlbumGridCard(
                                album = album,
                                isSelected = isSelected,
                                isAlbumMultiSelect = isAlbumMultiSelect,
                                onClick = {
                                    if (isAlbumMultiSelect) {
                                        selectedAlbumTitles = if (isSelected) {
                                            selectedAlbumTitles - album.title
                                        } else {
                                            selectedAlbumTitles + album.title
                                        }
                                    } else {
                                        selectedAlbumForDetails = album
                                        sheetSelectedSongIds = emptySet()
                                    }
                                },
                                onLongClick = {
                                    selectedAlbumTitles = if (isSelected) {
                                        selectedAlbumTitles - album.title
                                    } else {
                                        selectedAlbumTitles + album.title
                                    }
                                }
                            )
                        }
                    }

                    LazyGridFastScrollbar(
                            gridState = albumsGridState,
                            itemCount = sortedAlbums.size,
                            getSectionText = { index ->
                                sortedAlbums.getOrNull(index)?.title?.trim()?.take(1)?.uppercase() ?: ""
                            },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .padding(top = 8.dp, bottom = 90.dp)
                        )
                    }
                }
            } else {
                // Artists View
                if (artists.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No artists found. Tap refresh to scan your audio files.",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                } else if (useSplitView) {
                    val activeArtist = selectedArtistForDetails ?: artists.firstOrNull()
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(0.42f)
                                .fillMaxHeight()
                        ) {
                            LazyColumn(
                                state = artistsListState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(start = 8.dp, end = 12.dp),
                                contentPadding = PaddingValues(top = 4.dp, bottom = 90.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(artists, key = { it.name }, contentType = { "artist_card" }) { artist ->
                                    val isSelected = activeArtist?.name == artist.name
                                    ArtistListRow(
                                        artist = artist,
                                        onClick = {
                                            selectedArtistForDetails = artist
                                            sheetSelectedSongIds = emptySet()
                                        },
                                        onPlayClick = {
                                            if (artist.songs.isNotEmpty()) {
                                                onSongClick(artist.songs.first(), artist.songs)
                                            }
                                        }
                                    )
                                }
                            }

                            LazyColumnFastScrollbar(
                                listState = artistsListState,
                                itemCount = artists.size,
                                getSectionText = { index ->
                                    artists.getOrNull(index)?.name?.take(1)?.uppercase() ?: ""
                                },
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .fillMaxHeight()
                                    .padding(top = 4.dp, bottom = 90.dp)
                            )
                        }

                        Card(
                            modifier = Modifier
                                .weight(0.58f)
                                .fillMaxHeight()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = DeepPurpleSurface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            if (activeArtist != null) {
                                ArtistTracklistContent(
                                    artist = activeArtist,
                                    onSongClick = onSongClick,
                                    onClose = null
                                )
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = artistsListState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 16.dp, end = 18.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                        items(artists, key = { it.name }, contentType = { "artist_card" }) { artist ->
                            ArtistListRow(
                                artist = artist,
                                onClick = {
                                    selectedArtistForDetails = artist
                                    sheetSelectedSongIds = emptySet()
                                },
                                onPlayClick = {
                                    if (artist.songs.isNotEmpty()) {
                                        onSongClick(artist.songs.first(), artist.songs)
                                    }
                                }
                            )
                        }
                    }

                    LazyColumnFastScrollbar(
                        listState = artistsListState,
                        itemCount = artists.size,
                        getSectionText = { index ->
                            artists.getOrNull(index)?.name?.take(1)?.uppercase() ?: ""
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .padding(top = 8.dp, bottom = 90.dp)
                    )
                }
            }
        }
    }

    // Full Album Tracklist Bottom Sheet for Mobile Handheld
    if (!useSplitView) {
        selectedAlbumForDetails?.let { album ->
            val albumSongs = remember(album, allSongs) {
                allSongs.filter { it.album.equals(album.title, ignoreCase = true) }
                    .sortedWith(
                        compareBy(
                            { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE },
                            { it.title.lowercase() }
                        )
                    )
            }

            ModalBottomSheet(
                onDismissRequest = { selectedAlbumForDetails = null },
                sheetState = sheetState,
                containerColor = DeepPurpleSurface
            ) {
                AlbumTracklistContent(
                    album = album,
                    albumSongs = albumSongs,
                    sheetSelectedSongIds = sheetSelectedSongIds,
                    onToggleSelectSong = { songId ->
                        sheetSelectedSongIds = if (sheetSelectedSongIds.contains(songId)) {
                            sheetSelectedSongIds - songId
                        } else {
                            sheetSelectedSongIds + songId
                        }
                    },
                    onSongClick = { song, list ->
                        onSongClick(song, list)
                        selectedAlbumForDetails = null
                    },
                    onPlayNext = onPlayNext,
                    onAddToQueue = onAddToQueue,
                    onClose = { selectedAlbumForDetails = null }
                )
            }
        }

        // Artist Tracklist Bottom Sheet for Mobile
        selectedArtistForDetails?.let { artist ->
            ModalBottomSheet(
                onDismissRequest = { selectedArtistForDetails = null },
                sheetState = sheetState,
                containerColor = DeepPurpleSurface
            ) {
                ArtistTracklistContent(
                    artist = artist,
                    onSongClick = { song, list ->
                        onSongClick(song, list)
                        selectedArtistForDetails = null
                    },
                    onClose = { selectedArtistForDetails = null }
                )
            }
        }
    }
    }
}

private val AlbumGridShape = RoundedCornerShape(12.dp)
private val AlbumArtRadiusDp = 10.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumGridCard(
    album: Album,
    isSelected: Boolean,
    isAlbumMultiSelect: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AlbumGridShape)
            .instantListClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, ElectricViolet, AlbumGridShape)
                } else Modifier
            )
            .testTag("album_card_${album.title.lowercase().replace(" ", "_")}"),
        shape = AlbumGridShape,
        colors = CardDefaults.cardColors(containerColor = DeepPurpleDark.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(6.dp)
        ) {
            Box {
                AlbumArtContainer(
                    albumArtUri = album.parsedArtUri,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    cornerRadius = AlbumArtRadiusDp,
                    contentDescription = album.title
                )

                if (isAlbumMultiSelect) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) ElectricViolet else DeepPurpleDark.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.Check else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = album.title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Text(
                text = "${album.artist} • ${album.songCount} tracks",
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun ArtistListRow(
    artist: ArtistGroup,
    onClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .instantListClickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DeepPurpleDark.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(DeepPurpleCard),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = artist.name,
                    tint = ElectricViolet,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = artist.name,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${artist.songs.size} tracks • ${artist.albumCount} albums",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            IconButton(onClick = onPlayClick) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play artist",
                    tint = ElectricVioletLight
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumTracklistContent(
    album: Album,
    albumSongs: List<Song>,
    sheetSelectedSongIds: Set<Long>,
    onToggleSelectSong: (Long) -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlayNext: (List<Song>) -> Unit,
    onAddToQueue: (List<Song>) -> Unit,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isSheetMultiSelect = sheetSelectedSongIds.isNotEmpty()
    val tracklistState = rememberLazyListState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        // Header with Artwork, Title, Count & Close
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                AlbumArtContainer(
                    albumArtUri = if (album.albumArtUri != null) android.net.Uri.parse(album.albumArtUri) else null,
                    size = 46.dp,
                    cornerRadius = 8.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = album.title,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${album.artist} • ${albumSongs.size} tracks",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            if (onClose != null) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }
        }

        // Contextual multi-select bar
        if (isSheetMultiSelect) {
            val selectedInSheet = albumSongs.filter { sheetSelectedSongIds.contains(it.id) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (selectedInSheet.isNotEmpty()) {
                            onSongClick(selectedInSheet.first(), selectedInSheet)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Play (${selectedInSheet.size})", fontSize = 12.sp)
                }
                FilledTonalButton(
                    onClick = {
                        if (selectedInSheet.isNotEmpty()) {
                            onPlayNext(selectedInSheet)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Play Next", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = {
                        if (selectedInSheet.isNotEmpty()) {
                            onAddToQueue(selectedInSheet)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Add Queue", fontSize = 12.sp)
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = DeepPurpleDark
        )

        // Fully scrollable Tracklist Container with FastScrollbar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LazyColumn(
                state = tracklistState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 8.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(albumSongs, key = { _, song -> song.id }, contentType = { _, _ -> "album_track" }) { index, song ->
                    val isSongSelected = sheetSelectedSongIds.contains(song.id)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .instantListClickable(
                                onClick = {
                                    if (isSheetMultiSelect) {
                                        onToggleSelectSong(song.id)
                                    } else {
                                        onSongClick(song, albumSongs)
                                    }
                                },
                                onLongClick = {
                                    onToggleSelectSong(song.id)
                                }
                            )
                            .background(if (isSongSelected) DeepPurpleDark else Color.Transparent)
                            .padding(vertical = 6.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSheetMultiSelect) {
                            Icon(
                                imageVector = if (isSongSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (isSongSelected) ElectricViolet else TextSecondary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(end = 4.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            val trackDisplay = if (song.trackNumber > 0) "${song.trackNumber}" else "${index + 1}"
                            Text(
                                text = trackDisplay,
                                color = TextSecondary,
                                fontSize = 13.sp,
                                modifier = Modifier.width(28.dp)
                            )
                        }

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
                                text = "${song.format} • ${song.durationFormatted} • Play Count: ${song.playCount}",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play track",
                            tint = ElectricVioletLight
                        )
                    }
                }
            }

            LazyColumnFastScrollbar(
                listState = tracklistState,
                itemCount = albumSongs.size,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun ArtistTracklistContent(
    artist: ArtistGroup,
    onSongClick: (Song, List<Song>) -> Unit,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val tracklistState = rememberLazyListState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(DeepPurpleDark),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = ElectricViolet,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = artist.name,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${artist.songs.size} tracks across ${artist.albumCount} albums",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            if (onClose != null) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = DeepPurpleDark)

        // Fully scrollable Artist Songs Container with FastScrollbar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LazyColumn(
                state = tracklistState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 8.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(artist.songs, key = { _, song -> song.id }, contentType = { _, _ -> "artist_track" }) { index, song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onSongClick(song, artist.songs)
                            }
                            .padding(vertical = 6.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.width(28.dp)
                        )
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
                                text = "${song.album} • ${song.durationFormatted} • Play Count: ${song.playCount}",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play track",
                            tint = ElectricVioletLight
                        )
                    }
                }
            }

            LazyColumnFastScrollbar(
                listState = tracklistState,
                itemCount = artist.songs.size,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(bottom = 24.dp)
            )
        }
    }
}

