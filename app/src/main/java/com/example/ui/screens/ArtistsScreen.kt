package com.example.ui.screens

import android.net.Uri
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.ui.components.LazyColumnFastScrollbar
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.ui.util.instantListClickable

data class ArtistDetailData(
    val name: String,
    val allSongs: List<Song>,
    val albums: List<ArtistAlbumItem>,
    val singles: List<ArtistAlbumItem>
)

data class ArtistAlbumItem(
    val title: String,
    val songs: List<Song>,
    val artworkUri: String?,
    val isSingle: Boolean
)

@Composable
fun ArtistsScreen(
    allSongs: List<Song>,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onToggleFavorite: ((Song) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedArtistName by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val artistsListState = rememberLazyListState()

    // Group songs into artists on background thread using intelligent multi-artist & feature parser
    val artistGroups by androidx.compose.runtime.produceState(
        initialValue = emptyList<Triple<String, List<Song>, Int>>(),
        key1 = allSongs
    ) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            com.example.data.util.ArtistNameParser.groupSongsBySmartArtist(allSongs)
                .filterKeys { it.isNotBlank() && !it.equals("Unknown Artist", ignoreCase = true) }
                .map { (artistName, songs) ->
                    val albumsCount = songs.map { it.album }.distinct().size
                    Triple(artistName, songs, albumsCount)
                }
                .sortedBy { it.first.lowercase() }
        }
    }

    val filteredArtists = remember(artistGroups, searchQuery) {
        if (searchQuery.isBlank()) artistGroups
        else artistGroups.filter { it.first.contains(searchQuery, ignoreCase = true) }
    }

    val selectedArtistData = remember(selectedArtistName, artistGroups) {
        selectedArtistName?.let { artistName ->
            val songs = artistGroups.firstOrNull { it.first == artistName }?.second ?: emptyList()
            val groupedByAlbum = songs.groupBy { it.album }
            val albumsList = mutableListOf<ArtistAlbumItem>()
            val singlesList = mutableListOf<ArtistAlbumItem>()

            groupedByAlbum.forEach { (albumTitle, albumSongs) ->
                val sortedSongs = albumSongs.sortedWith(
                    compareBy(
                        { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE },
                        { it.title.lowercase() }
                    )
                )
                val isSingle = albumSongs.size <= 2 ||
                        albumTitle.contains("single", ignoreCase = true) ||
                        albumTitle.contains("ep", ignoreCase = true)

                val item = ArtistAlbumItem(
                    title = albumTitle,
                    songs = sortedSongs,
                    artworkUri = sortedSongs.firstOrNull()?.albumArtUri?.toString(),
                    isSingle = isSingle
                )
                if (isSingle) {
                    singlesList.add(item)
                } else {
                    albumsList.add(item)
                }
            }

            ArtistDetailData(
                name = artistName,
                allSongs = songs.sortedByDescending { it.playCount },
                albums = albumsList.sortedBy { it.title },
                singles = singlesList.sortedBy { it.title }
            )
        }
    }

    val artistsListContent: @Composable (Modifier) -> Unit = { listMod ->
        Column(
            modifier = listMod
                .padding(horizontal = 16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp)
                    .testTag("artists_search_field"),
                placeholder = { Text("Search artists...", color = TextTertiary, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary)
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricViolet,
                    unfocusedBorderColor = DeepPurpleDark,
                    focusedContainerColor = DeepPurpleCard,
                    unfocusedContainerColor = DeepPurpleCard,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )

            if (filteredArtists.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) "No artists detected" else "No artists matching \"$searchQuery\"",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = artistsListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 0.dp, end = 16.dp),
                        contentPadding = PaddingValues(bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredArtists, key = { it.first }, contentType = { "artist_item" }) { (artistName, songs, albumCount) ->
                            ArtistListItem(
                                name = artistName,
                                songs = songs,
                                albumCount = albumCount,
                                onClick = { selectedArtistName = artistName },
                                onShuffle = {
                                    val shuffled = songs.shuffled()
                                    if (shuffled.isNotEmpty()) {
                                        onSongClick(shuffled.first(), shuffled)
                                    }
                                }
                            )
                        }
                    }

                    LazyColumnFastScrollbar(
                        listState = artistsListState,
                        itemCount = filteredArtists.size,
                        getSectionText = { index ->
                            filteredArtists.getOrNull(index)?.first?.take(1)?.uppercase() ?: ""
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .padding(bottom = 90.dp)
                    )
                }
            }
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

        if (useSplitView) {
            val tabletSelectedArtist = selectedArtistData ?: filteredArtists.firstOrNull()?.let { firstGroup ->
                val songs = firstGroup.second
                val groupedByAlbum = songs.groupBy { it.album }
                val albumsList = mutableListOf<ArtistAlbumItem>()
                val singlesList = mutableListOf<ArtistAlbumItem>()

                groupedByAlbum.forEach { (albumTitle, albumSongs) ->
                    val sortedSongs = albumSongs.sortedWith(
                        compareBy(
                            { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE },
                            { it.title.lowercase() }
                        )
                    )
                    val isSingle = albumSongs.size <= 2 ||
                            albumTitle.contains("single", ignoreCase = true) ||
                            albumTitle.contains("ep", ignoreCase = true)

                    val item = ArtistAlbumItem(
                        title = albumTitle,
                        songs = sortedSongs,
                        artworkUri = sortedSongs.firstOrNull()?.albumArtUri?.toString(),
                        isSingle = isSingle
                    )
                    if (isSingle) {
                        singlesList.add(item)
                    } else {
                        albumsList.add(item)
                    }
                }

                ArtistDetailData(
                    name = firstGroup.first,
                    allSongs = songs.sortedByDescending { it.playCount },
                    albums = albumsList.sortedBy { it.title },
                    singles = singlesList.sortedBy { it.title }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Column: Artists List (weight 0.40f)
                Box(
                    modifier = Modifier
                        .weight(0.40f)
                        .fillMaxHeight()
                ) {
                    artistsListContent(Modifier.fillMaxSize())
                }

                // Right Column: Artist Discography & Songs Detail (weight 0.60f)
                Card(
                    modifier = Modifier
                        .weight(0.60f)
                        .fillMaxHeight()
                        .padding(end = 16.dp, bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepPurpleSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    if (tabletSelectedArtist != null) {
                        SpotifyArtistDetailView(
                            data = tabletSelectedArtist,
                            onBack = { /* In split view, back button is omitted or no-op */ },
                            onSongClick = onSongClick,
                            onPlayNext = onPlayNext,
                            onAddToQueue = onAddToQueue,
                            onToggleFavorite = onToggleFavorite
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Select an artist to view their full discography and tracks.",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        } else {
            // Handheld Mobile View
            if (selectedArtistData != null) {
                SpotifyArtistDetailView(
                    data = selectedArtistData,
                    onBack = { selectedArtistName = null },
                    onSongClick = onSongClick,
                    onPlayNext = onPlayNext,
                    onAddToQueue = onAddToQueue,
                    onToggleFavorite = onToggleFavorite
                )
            } else {
                artistsListContent(Modifier.fillMaxSize())
            }
        }
    }
}

private val ArtistCardShape = RoundedCornerShape(14.dp)

@Composable
fun ArtistListItem(
    name: String,
    songs: List<Song>,
    albumCount: Int,
    onClick: () -> Unit,
    onShuffle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("artist_card_${name.lowercase().replace(" ", "_")}"),
        shape = ArtistCardShape,
        colors = CardDefaults.cardColors(containerColor = DeepPurpleCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Circular artist avatar with first album art or initial
                val firstArt = songs.firstOrNull()?.albumArtUri
                if (firstArt != null) {
                    AlbumArtContainer(
                        albumArtUri = firstArt,
                        size = 52.dp,
                        cornerRadius = 26.dp
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(DeepPurpleDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name.take(1).uppercase(),
                            color = ElectricVioletLight,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = name,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${songs.size} songs • $albumCount ${if (albumCount == 1) "album" else "albums"}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // Quick Shuffle Button
            IconButton(
                onClick = onShuffle,
                modifier = Modifier.testTag("shuffle_artist_$name")
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle artist songs",
                    tint = ElectricVioletLight
                )
            }
        }
    }
}

@Composable
fun SpotifyArtistDetailView(
    data: ArtistDetailData,
    onBack: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onToggleFavorite: ((Song) -> Unit)? = null
) {
    val firstArtwork = data.allSongs.firstOrNull()?.albumArtUri
    val detailListState = rememberLazyListState()
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val headerHeight = if (isLandscape) 130.dp else 240.dp
    val avatarSize = if (isLandscape) 52.dp else 80.dp

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = detailListState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("spotify_artist_details_page"),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
        // Hero Header (Spotify-style)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight)
            ) {
                // Background Gradient or Art
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(ElectricViolet.copy(alpha = 0.65f), DeepPurple)
                            )
                        )
                )

                // Back Button
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .padding(top = if (isLandscape) 6.dp else 40.dp, start = 8.dp)
                        .align(Alignment.TopStart)
                        .testTag("artist_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                // Artist Avatar & Name
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = if (isLandscape) 8.dp else 16.dp, end = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (firstArtwork != null) {
                            AlbumArtContainer(
                                albumArtUri = firstArtwork,
                                size = avatarSize,
                                cornerRadius = avatarSize / 2
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(avatarSize)
                                    .clip(CircleShape)
                                    .background(DeepPurpleDark),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = ElectricVioletLight,
                                    modifier = Modifier.size(if (isLandscape) 28.dp else 44.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = "ARTIST",
                                color = ElectricVioletLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = data.name,
                                color = TextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${data.allSongs.size} tracks • ${data.albums.size + data.singles.size} releases",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Action Row: Shuffle Play & Sequential Play (Spotify style)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Prominent Shuffle Button (Spotify main action)
                Button(
                    onClick = {
                        val shuffled = data.allSongs.shuffled()
                        if (shuffled.isNotEmpty()) {
                            onSongClick(shuffled.first(), shuffled)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("artist_shuffle_all_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Shuffle",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Play Sequential Button
                FilledTonalButton(
                    onClick = {
                        if (data.allSongs.isNotEmpty()) {
                            onSongClick(data.allSongs.first(), data.allSongs)
                        }
                    },
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("artist_play_all_button"),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = DeepPurpleCard),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = ElectricVioletLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Play", color = TextPrimary, fontSize = 14.sp)
                }
            }
        }

        // Section 1: Popular / Songs
        item {
            SectionHeader(title = "Popular Songs", count = data.allSongs.size)
        }

        itemsIndexed(data.allSongs, key = { _, song -> song.id }, contentType = { _, _ -> "artist_song" }) { index, song ->
            ArtistSongRow(
                index = index + 1,
                song = song,
                allSongs = data.allSongs,
                onSongClick = onSongClick,
                onPlayNext = onPlayNext,
                onAddToQueue = onAddToQueue,
                onToggleFavorite = onToggleFavorite
            )
        }

        // Section 2: Albums
        if (data.albums.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(title = "Albums", count = data.albums.size)
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(data.albums, key = { it.title }, contentType = { "artist_album" }) { albumItem ->
                        ReleaseCard(
                            release = albumItem,
                            onClick = {
                                if (albumItem.songs.isNotEmpty()) {
                                    onSongClick(albumItem.songs.first(), albumItem.songs)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Section 3: Singles & EPs (Spotify feature: separate singles section!)
        if (data.singles.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(title = "Singles & EPs", count = data.singles.size)
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(data.singles, key = { it.title }, contentType = { "artist_single" }) { singleItem ->
                        ReleaseCard(
                            release = singleItem,
                            onClick = {
                                if (singleItem.songs.isNotEmpty()) {
                                    onSongClick(singleItem.songs.first(), singleItem.songs)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

        LazyColumnFastScrollbar(
            listState = detailListState,
            itemCount = data.allSongs.size,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(bottom = 90.dp)
        )
    }
}

@Composable
fun SectionHeader(title: String, count: Int? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        if (count != null) {
            Text(
                text = "$count",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ArtistSongRow(
    index: Int,
    song: Song,
    allSongs: List<Song>,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onToggleFavorite: ((Song) -> Unit)? = null
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .instantListClickable { onSongClick(song, allSongs) }
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("artist_song_row_${song.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track Rank / Number
        Text(
            text = "$index",
            color = TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(28.dp)
        )

        // Artwork thumbnail
        AlbumArtContainer(
            albumArtUri = song.albumArtUri,
            size = 44.dp,
            cornerRadius = 6.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Title and meta
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
                text = "${song.album} • ${song.durationFormatted}",
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Heart favorite toggle
        if (onToggleFavorite != null) {
            IconButton(onClick = { onToggleFavorite(song) }) {
                Icon(
                    imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (song.isFavorite) ElectricViolet else TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // More options dropdown
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(DeepPurpleCard)
            ) {
                DropdownMenuItem(
                    text = { Text("Play Next", color = TextPrimary) },
                    onClick = {
                        onPlayNext(song)
                        menuExpanded = false
                    },
                    leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = ElectricVioletLight) }
                )
                DropdownMenuItem(
                    text = { Text("Add to Queue", color = TextPrimary) },
                    onClick = {
                        onAddToQueue(song)
                        menuExpanded = false
                    },
                    leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = TextSecondary) }
                )
            }
        }
    }
}

@Composable
fun ReleaseCard(
    release: ArtistAlbumItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .instantListClickable { onClick() }
            .testTag("release_card_${release.title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DeepPurpleCard)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            val artUri = if (release.artworkUri != null) Uri.parse(release.artworkUri) else null
            AlbumArtContainer(
                albumArtUri = artUri,
                size = 124.dp,
                cornerRadius = 8.dp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = release.title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${release.songs.size} ${if (release.songs.size == 1) "track" else "tracks"}",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}
