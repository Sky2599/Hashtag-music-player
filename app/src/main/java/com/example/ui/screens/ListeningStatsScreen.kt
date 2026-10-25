package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Song
import com.example.data.util.ArtistNameParser
import com.example.ui.components.AlbumArtContainer
import com.example.ui.theme.DeepPurple
import com.example.ui.theme.DeepPurpleCard
import com.example.ui.theme.DeepPurpleDark
import com.example.ui.theme.DeepPurpleSurface
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElectricVioletLight
import com.example.ui.theme.OnPrimaryAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.util.TactileHaptics
import com.example.ui.util.instantListClickable
import com.example.ui.util.instantTapDown
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Computed Analytics Model for the Standalone Listening Stats & Music DNA Screen.
 */
data class DetailedListeningAnalytics(
    val totalTracks: Int = 0,
    val totalAlbums: Int = 0,
    val totalArtists: Int = 0,
    val totalLibraryDurationMs: Long = 0L,
    val totalPlayCount: Int = 0,
    val totalTimeListenedMs: Long = 0L,
    val totalStorageBytes: Long = 0L,
    val favoritesCount: Int = 0,
    val hiResLosslessCount: Int = 0,
    val audiophileScore: Int = 0,
    val sonicDnaPersona: String = "Sonic Explorer",
    val sonicDnaDescription: String = "",
    val averageBitrateKbps: Int = 0,
    val averageSampleRateHz: Int = 44100,
    val averageTrackDurationMs: Long = 0L,
    val formatBreakdown: List<StatDistributionBucket> = emptyList(),
    val sampleRateBreakdown: List<StatDistributionBucket> = emptyList(),
    val bitDepthBreakdown: List<StatDistributionBucket> = emptyList(),
    val durationVibeBreakdown: List<StatDistributionBucket> = emptyList(),
    val rotationTiers: List<StatDistributionBucket> = emptyList(),
    val topPlayedSongs: List<Song> = emptyList(),
    val topArtists: List<ArtistListeningStat> = emptyList(),
    val topAlbums: List<AlbumListeningStat> = emptyList(),
    val rediscoverGems: List<Song> = emptyList()
)

data class StatDistributionBucket(
    val label: String,
    val subtitle: String,
    val count: Int,
    val fraction: Float,
    val accentColor: Color
)

data class ArtistListeningStat(
    val name: String,
    val trackCount: Int,
    val albumCount: Int,
    val totalPlays: Int,
    val totalListenedMinutes: Long,
    val artworkUri: android.net.Uri?,
    val songs: List<Song>
)

data class AlbumListeningStat(
    val title: String,
    val artist: String,
    val trackCount: Int,
    val totalPlays: Int,
    val primaryFormat: String,
    val artworkUri: android.net.Uri?,
    val songs: List<Song>
)

/**
 * Standalone Full-Screen Listening Stats & Sonic DNA Analytics Page.
 * Replaces the basic popup dialog with a rich, multi-section analytics dashboard
 * detailing the exact kind of music the user plays, audio fidelity distribution,
 * rotation habits, top artists/albums/tracks, and playable actions.
 */
@Composable
fun ListeningStatsScreen(
    allSongs: List<Song>,
    onSongClick: (Song, List<Song>) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)
    val view = LocalView.current

    // Filter tabs: 0 = Overview & DNA, 1 = Top Rotation, 2 = Audio Fidelity & Formats
    var selectedSectionTab by remember { mutableIntStateOf(0) }

    val analytics by produceState(
        initialValue = DetailedListeningAnalytics(),
        key1 = allSongs
    ) {
        value = withContext(Dispatchers.Default) {
            computeDetailedAnalytics(allSongs)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(DeepPurple)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("listening_stats_screen")
    ) {
        val isWideTablet = maxWidth >= 720.dp

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top App Bar with Instant Back & Play Heavy Rotation Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepPurpleSurface)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = {
                            TactileHaptics.lightImpact(view)
                            onBackClick()
                        },
                        modifier = Modifier.testTag("stats_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Column {
                        Text(
                            text = "Listening Stats & Sonic DNA",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${analytics.totalTracks} tracks analyzed • ${analytics.sonicDnaPersona}",
                            color = ElectricVioletLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (analytics.topPlayedSongs.isNotEmpty()) {
                    Button(
                        onClick = {
                            TactileHaptics.lightImpact(view)
                            val topList = analytics.topPlayedSongs
                            onSongClick(topList.first(), topList)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("play_top_rotation_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = OnPrimaryAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Play Top Mix",
                            color = OnPrimaryAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 2. Zero-Latency Tactile Filter Pill Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepPurpleDark)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tabs = listOf(
                    0 to "Sonic DNA & Overview",
                    1 to "Top Artists & Tracks",
                    2 to "Fidelity & Formats"
                )
                tabs.forEach { (idx, title) ->
                    val isSelected = selectedSectionTab == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) ElectricViolet else DeepPurpleSurface)
                            .then(
                                if (isSelected) {
                                    Modifier.border(1.dp, ElectricVioletLight, RoundedCornerShape(12.dp))
                                } else Modifier
                            )
                            .instantTapDown(hapticLightImpact = false) {
                                selectedSectionTab = idx
                            }
                            .padding(vertical = 9.dp, horizontal = 6.dp)
                            .testTag("stats_tab_$idx"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) OnPrimaryAccent else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            if (allSongs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No songs indexed yet. Scan your device audio files to generate Listening Stats.",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else {
                // 3. Main Analytics Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = if (isWideTablet) 960.dp else 640.dp)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 14.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (selectedSectionTab) {
                        0 -> {
                            // TAB 0: SONIC DNA & OVERVIEW
                            item(key = "sonic_dna_hero") {
                                SonicDnaHeroCard(analytics = analytics)
                            }

                            item(key = "kpi_grid") {
                                ListeningKpiGrid(analytics = analytics)
                            }

                            item(key = "rotation_habit_breakdown") {
                                DistributionSectionCard(
                                    title = "Listening Rotation Habits",
                                    subtitle = "How deeply you explore your library vs repeating favorites",
                                    icon = Icons.Default.Psychology,
                                    buckets = analytics.rotationTiers
                                )
                            }

                            item(key = "vibe_duration_breakdown") {
                                DistributionSectionCard(
                                    title = "Track Length & Pacing Profile",
                                    subtitle = "Distribution of concise cuts, standard songs, and extended epics",
                                    icon = Icons.Default.Timer,
                                    buckets = analytics.durationVibeBreakdown
                                )
                            }

                            if (analytics.topArtists.isNotEmpty()) {
                                item(key = "overview_top_artists_preview") {
                                    TopArtistsLeaderboardCard(
                                        artists = analytics.topArtists.take(4),
                                        onPlayArtist = { artist ->
                                            if (artist.songs.isNotEmpty()) {
                                                onSongClick(artist.songs.first(), artist.songs)
                                            }
                                        },
                                        onSeeAllClick = { selectedSectionTab = 1 }
                                    )
                                }
                            }

                            if (analytics.rediscoverGems.isNotEmpty()) {
                                item(key = "unplayed_gems_card") {
                                    RediscoverGemsCard(
                                        gems = analytics.rediscoverGems,
                                        onSongClick = onSongClick
                                    )
                                }
                            }
                        }

                        1 -> {
                            // TAB 1: TOP ARTISTS, ALBUMS & MOST PLAYED TRACKS
                            if (analytics.topArtists.isNotEmpty()) {
                                item(key = "full_top_artists") {
                                    TopArtistsLeaderboardCard(
                                        artists = analytics.topArtists,
                                        onPlayArtist = { artist ->
                                            if (artist.songs.isNotEmpty()) {
                                                onSongClick(artist.songs.first(), artist.songs)
                                            }
                                        },
                                        onSeeAllClick = null
                                    )
                                }
                            }

                            if (analytics.topAlbums.isNotEmpty()) {
                                item(key = "full_top_albums") {
                                    TopAlbumsLeaderboardCard(
                                        albums = analytics.topAlbums,
                                        onPlayAlbum = { album ->
                                            if (album.songs.isNotEmpty()) {
                                                onSongClick(album.songs.first(), album.songs)
                                            }
                                        }
                                    )
                                }
                            }

                            item(key = "heavy_rotation_header") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Heavy Rotation Tracks",
                                            color = TextPrimary,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Tap any track to play immediately in Heavy Rotation order",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            TactileHaptics.lightImpact(view)
                                            val shuffled = analytics.topPlayedSongs.shuffled()
                                            if (shuffled.isNotEmpty()) {
                                                onSongClick(shuffled.first(), shuffled)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Shuffle,
                                            contentDescription = "Shuffle Top Tracks",
                                            tint = ElectricVioletLight
                                        )
                                    }
                                }
                            }

                            itemsIndexed(
                                items = analytics.topPlayedSongs,
                                key = { _, song -> "top_song_${song.id}" },
                                contentType = { _, _ -> "top_song_row" }
                            ) { index, song ->
                                TopTrackRankRow(
                                    rank = index + 1,
                                    song = song,
                                    maxPlays = analytics.topPlayedSongs.firstOrNull()?.playCount?.coerceAtLeast(1) ?: 1,
                                    onClick = {
                                        onSongClick(song, analytics.topPlayedSongs)
                                    }
                                )
                            }
                        }

                        2 -> {
                            // TAB 2: AUDIO FIDELITY, CODECS, SAMPLE RATES & BIT DEPTH
                            item(key = "audiophile_grade_card") {
                                AudiophileFidelityMeterCard(analytics = analytics)
                            }

                            item(key = "codec_format_breakdown") {
                                DistributionSectionCard(
                                    title = "Audio Codec & Container Formats",
                                    subtitle = "Lossless (FLAC / WAV / ALAC) vs Compressed (MP3 / AAC / OGG) files",
                                    icon = Icons.Default.AudioFile,
                                    buckets = analytics.formatBreakdown
                                )
                            }

                            item(key = "sample_rate_breakdown") {
                                DistributionSectionCard(
                                    title = "Sample Rate Spectrum (Hz / kHz)",
                                    subtitle = "Redbook CD (44.1 kHz), Studio (48 kHz), and Hi-Res (96–192 kHz)",
                                    icon = Icons.Default.GraphicEq,
                                    buckets = analytics.sampleRateBreakdown
                                )
                            }

                            item(key = "bit_depth_breakdown") {
                                DistributionSectionCard(
                                    title = "Bit-Depth & Dynamic Range",
                                    subtitle = "16-bit Standard vs 24-bit / 32-bit Studio Master headroom",
                                    icon = Icons.Default.HighQuality,
                                    buckets = analytics.bitDepthBreakdown
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
private fun SonicDnaHeroCard(analytics: DetailedListeningAnalytics) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sonic_dna_hero_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DeepPurpleCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            ElectricViolet.copy(alpha = 0.28f),
                            DeepPurpleCard,
                            DeepPurpleSurface
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(ElectricViolet),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = OnPrimaryAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "YOUR LISTENING PERSONA",
                            color = ElectricVioletLight,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = analytics.sonicDnaPersona,
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Audiophile Fidelity Badge
                Surface(
                    color = DeepPurpleDark,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.border(1.dp, ElectricVioletLight.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${analytics.audiophileScore}/100",
                            color = ElectricVioletLight,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "FIDELITY",
                            color = TextSecondary,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = analytics.sonicDnaDescription,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = DeepPurpleDark)
            Spacer(modifier = Modifier.height(12.dp))

            // Quick Technical Averages Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DnaMiniPill(
                    label = "Avg Bitrate",
                    value = "${analytics.averageBitrateKbps} kbps"
                )
                DnaMiniPill(
                    label = "Avg Sample Rate",
                    value = String.format("%.1f kHz", analytics.averageSampleRateHz / 1000f)
                )
                DnaMiniPill(
                    label = "Avg Track Length",
                    value = formatShortDuration(analytics.averageTrackDurationMs)
                )
                DnaMiniPill(
                    label = "Hi-Res Share",
                    value = "${if (analytics.totalTracks > 0) (analytics.hiResLosslessCount * 100) / analytics.totalTracks else 0}%"
                )
            }
        }
    }
}

@Composable
private fun DnaMiniPill(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = TextTertiary,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun ListeningKpiGrid(analytics: DetailedListeningAnalytics) {
    val listenedHours = analytics.totalTimeListenedMs / (1000L * 60L * 60L)
    val listenedMins = (analytics.totalTimeListenedMs / (1000L * 60L)) % 60L
    val libHours = analytics.totalLibraryDurationMs / (1000L * 60L * 60L)
    val libMins = (analytics.totalLibraryDurationMs / (1000L * 60L)) % 60L
    val storageGb = analytics.totalStorageBytes / (1024f * 1024f * 1024f)
    val storageMb = analytics.totalStorageBytes / (1024f * 1024f)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatKpiTile(
                icon = Icons.Default.Headphones,
                label = "Total Listening Time",
                value = if (listenedHours > 0) "${listenedHours}h ${listenedMins}m" else "${listenedMins}m",
                subvalue = "${analytics.totalPlayCount} total plays logged",
                modifier = Modifier.weight(1f)
            )
            StatKpiTile(
                icon = Icons.Default.Timer,
                label = "Library Duration",
                value = "${libHours}h ${libMins}m",
                subvalue = "${analytics.totalTracks} tracks indexed",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatKpiTile(
                icon = Icons.Default.Person,
                label = "Artists & Albums",
                value = "${analytics.totalArtists} Artists",
                subvalue = "Across ${analytics.totalAlbums} albums",
                modifier = Modifier.weight(1f)
            )
            StatKpiTile(
                icon = Icons.Default.Storage,
                label = "Offline Audio Footprint",
                value = if (storageGb >= 1.0f) String.format("%.2f GB", storageGb) else String.format("%.0f MB", storageMb),
                subvalue = "${analytics.favoritesCount} favorites • ${analytics.hiResLosslessCount} lossless",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatKpiTile(
    icon: ImageVector,
    label: String,
    value: String,
    subvalue: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DeepPurpleSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ElectricVioletLight,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subvalue,
                color = TextTertiary,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DistributionSectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    buckets: List<StatDistributionBucket>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DeepPurpleCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ElectricVioletLight,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Multi-segment stacked bar
            if (buckets.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(DeepPurpleDark)
                ) {
                    buckets.filter { it.fraction > 0f }.forEach { bucket ->
                        Box(
                            modifier = Modifier
                                .weight(bucket.fraction.coerceAtLeast(0.02f))
                                .fillMaxHeight()
                                .background(bucket.accentColor)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                buckets.forEach { bucket ->
                    val pct = (bucket.fraction * 100f).roundToInt()
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(bucket.accentColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = bucket.label,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = bucket.subtitle,
                                        color = TextTertiary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            Text(
                                text = "${bucket.count} tracks ($pct%)",
                                color = ElectricVioletLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { bucket.fraction.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = bucket.accentColor,
                            trackColor = DeepPurpleDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopArtistsLeaderboardCard(
    artists: List<ArtistListeningStat>,
    onPlayArtist: (ArtistListeningStat) -> Unit,
    onSeeAllClick: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = ElectricVioletLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Most Played Artists",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ranked by cumulative plays & listening time",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                if (onSeeAllClick != null) {
                    Text(
                        text = "See All",
                        color = ElectricVioletLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .instantTapDown(hapticLightImpact = false) { onSeeAllClick() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val maxArtistPlays = artists.maxOfOrNull { it.totalPlays.coerceAtLeast(1) } ?: 1

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                artists.forEachIndexed { idx, artist ->
                    val barFraction = (artist.totalPlays.toFloat() / maxArtistPlays.toFloat()).coerceIn(0.08f, 1f)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DeepPurpleSurface)
                            .instantListClickable { onPlayArtist(artist) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "#${idx + 1}",
                            color = if (idx == 0) ElectricVioletLight else TextTertiary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.width(28.dp)
                        )

                        AlbumArtContainer(
                            albumArtUri = artist.artworkUri,
                            size = 44.dp,
                            cornerRadius = 22.dp,
                            contentDescription = artist.name
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = artist.name,
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${artist.totalPlays} plays",
                                    color = ElectricVioletLight,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "${artist.trackCount} tracks • ${artist.albumCount} albums • ~${artist.totalListenedMinutes} min played",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            LinearProgressIndicator(
                                progress = { barFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(1.5.dp)),
                                color = ElectricVioletLight,
                                trackColor = DeepPurpleDark
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play ${artist.name}",
                            tint = ElectricVioletLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopAlbumsLeaderboardCard(
    albums: List<AlbumListeningStat>,
    onPlayAlbum: (AlbumListeningStat) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DeepPurpleCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Album,
                    contentDescription = null,
                    tint = ElectricVioletLight,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Top Albums by Play Volume",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Your most spun full-length releases & EPs",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                albums.forEachIndexed { idx, album ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DeepPurpleSurface)
                            .instantListClickable { onPlayAlbum(album) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "#${idx + 1}",
                            color = if (idx == 0) ElectricVioletLight else TextTertiary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.width(28.dp)
                        )

                        AlbumArtContainer(
                            albumArtUri = album.artworkUri,
                            size = 44.dp,
                            cornerRadius = 8.dp,
                            contentDescription = album.title
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = album.title,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${album.artist} • ${album.trackCount} tracks • ${album.primaryFormat}",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Surface(
                            color = DeepPurpleDark,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${album.totalPlays} plays",
                                color = ElectricVioletLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopTrackRankRow(
    rank: Int,
    song: Song,
    maxPlays: Int,
    onClick: () -> Unit
) {
    val fraction = (song.playCount.toFloat() / maxPlays.toFloat()).coerceIn(0.05f, 1f)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .instantListClickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DeepPurpleSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#$rank",
                color = if (rank <= 3) ElectricVioletLight else TextTertiary,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.width(30.dp)
            )

            AlbumArtContainer(
                albumArtUri = song.albumArtUri,
                size = 44.dp,
                cornerRadius = 8.dp,
                contentDescription = song.title
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${song.artist} • ${song.techSpecsTag}",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp)),
                    color = ElectricViolet,
                    trackColor = DeepPurpleDark
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${song.playCount}x",
                    color = ElectricVioletLight,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = song.durationFormatted,
                    color = TextTertiary,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun RediscoverGemsCard(
    gems: List<Song>,
    onSongClick: (Song, List<Song>) -> Unit
) {
    val view = LocalView.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DeepPurpleCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Unplayed & Forgotten Gems",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "High-quality tracks in your library waiting to be spun",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Button(
                    onClick = {
                        TactileHaptics.lightImpact(view)
                        onSongClick(gems.first(), gems)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Spin Gems", color = OnPrimaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                gems.take(5).forEach { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DeepPurpleSurface)
                            .instantListClickable { onSongClick(song, gems) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AlbumArtContainer(
                            albumArtUri = song.albumArtUri,
                            size = 38.dp,
                            cornerRadius = 6.dp,
                            contentDescription = song.title
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${song.artist} • ${song.techSpecsTag}",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = ElectricVioletLight,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AudiophileFidelityMeterCard(analytics: DetailedListeningAnalytics) {
    val accentPrimary = ElectricViolet
    val accentLight = ElectricVioletLight
    val darkTrack = DeepPurpleDark

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DeepPurpleCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(96.dp),
                contentAlignment = Alignment.Center
            ) {
                val sweep = (analytics.audiophileScore / 100f).coerceIn(0f, 1f) * 270f
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 9.dp.toPx()
                    drawArc(
                        color = darkTrack,
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        brush = Brush.sweepGradient(listOf(accentPrimary, accentLight)),
                        startAngle = 135f,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${analytics.audiophileScore}",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "SCORE",
                        color = ElectricVioletLight,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Acoustic Library Grade",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Evaluated from your library's lossless FLAC/WAV ratio (${analytics.hiResLosslessCount}/${analytics.totalTracks}), average bitrate (${analytics.averageBitrateKbps} kbps), and sample rate headroom (${String.format("%.1f kHz", analytics.averageSampleRateHz / 1000f)}).",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

private fun formatShortDuration(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return String.format("%d:%02d", mins, secs)
}

/**
 * Pure background computation of the user's Listening Stats & Sonic DNA.
 */
private fun computeDetailedAnalytics(allSongs: List<Song>): DetailedListeningAnalytics {
    if (allSongs.isEmpty()) return DetailedListeningAnalytics()

    val totalTracks = allSongs.size
    val totalAlbums = allSongs.map { it.album.trim().lowercase() }.distinct().size
    val smartArtistMap = ArtistNameParser.groupSongsBySmartArtist(allSongs)
        .filterKeys { it.isNotBlank() && !it.equals("Unknown Artist", ignoreCase = true) }
    val totalArtists = smartArtistMap.size.coerceAtLeast(1)

    val totalLibraryDurationMs = allSongs.sumOf { it.durationMs }
    val totalPlayCount = allSongs.sumOf { it.playCount }
    val totalTimeListenedMs = allSongs.sumOf { it.durationMs * it.playCount.toLong() }
    val totalStorageBytes = allSongs.sumOf { it.sizeBytes }
    val favoritesCount = allSongs.count { it.isFavorite }

    val losslessCount = allSongs.count {
        it.format.equals("FLAC", ignoreCase = true) ||
            it.format.equals("WAV", ignoreCase = true) ||
            it.format.equals("ALAC", ignoreCase = true) ||
            it.bitrateKbps >= 700
    }
    val hiResCount = allSongs.count {
        it.sampleRateHz >= 88200 || it.bitDepth >= 24 || it.bitrateKbps >= 320 ||
            it.format.equals("FLAC", ignoreCase = true) || it.format.equals("WAV", ignoreCase = true)
    }

    val avgBitrate = (allSongs.sumOf { it.bitrateKbps.toLong() } / totalTracks).toInt()
    val avgSampleRate = (allSongs.sumOf { it.sampleRateHz.toLong() } / totalTracks).toInt()
    val avgDurationMs = totalLibraryDurationMs / totalTracks

    // Compute Audiophile Score (0..100)
    val losslessRatio = losslessCount.toFloat() / totalTracks.toFloat()
    val hiResSampleRatio = allSongs.count { it.sampleRateHz > 44100 }.toFloat() / totalTracks.toFloat()
    val bitrateScore = ((avgBitrate - 128).coerceAtLeast(0) / 800f).coerceIn(0f, 1f)
    val audiophileScore = ((losslessRatio * 45f) + (hiResSampleRatio * 25f) + (bitrateScore * 30f))
        .roundToInt()
        .coerceIn(18, 100)

    // 1. Format Breakdown
    val flacWav = allSongs.count { it.format.equals("FLAC", true) || it.format.equals("WAV", true) || it.format.equals("ALAC", true) }
    val highMp3Aac = allSongs.count { !it.format.equals("FLAC", true) && !it.format.equals("WAV", true) && !it.format.equals("ALAC", true) && it.bitrateKbps >= 256 }
    val standardCompressed = (totalTracks - flacWav - highMp3Aac).coerceAtLeast(0)

    val formatBreakdown = listOf(
        StatDistributionBucket(
            label = "Lossless Master (FLAC / WAV / ALAC)",
            subtitle = "Bit-perfect uncompressed or lossless archival audio",
            count = flacWav,
            fraction = flacWav.toFloat() / totalTracks,
            accentColor = Color(0xFF10B981)
        ),
        StatDistributionBucket(
            label = "High-Bitrate Mastering (256–320 kbps)",
            subtitle = "High-efficiency transparent MP3 / AAC / OGG streams",
            count = highMp3Aac,
            fraction = highMp3Aac.toFloat() / totalTracks,
            accentColor = Color(0xFF8B5CF6)
        ),
        StatDistributionBucket(
            label = "Standard Compressed (< 256 kbps)",
            subtitle = "Compact portable audio encodings",
            count = standardCompressed,
            fraction = standardCompressed.toFloat() / totalTracks,
            accentColor = Color(0xFF38BDF8)
        )
    )

    // 2. Sample Rate Breakdown
    val ultraHiResSr = allSongs.count { it.sampleRateHz >= 88200 }
    val studio48Sr = allSongs.count { it.sampleRateHz in 48000..88199 }
    val cd44Sr = (totalTracks - ultraHiResSr - studio48Sr).coerceAtLeast(0)

    val sampleRateBreakdown = listOf(
        StatDistributionBucket(
            label = "Hi-Res Ultrasonic (88.2 kHz – 192 kHz)",
            subtitle = "Studio master ultrasonic frequency response",
            count = ultraHiResSr,
            fraction = ultraHiResSr.toFloat() / totalTracks,
            accentColor = Color(0xFFF59E0B)
        ),
        StatDistributionBucket(
            label = "Studio Reference (48.0 kHz)",
            subtitle = "Native Android hardware & digital cinema clock rate",
            count = studio48Sr,
            fraction = studio48Sr.toFloat() / totalTracks,
            accentColor = Color(0xFF10B981)
        ),
        StatDistributionBucket(
            label = "Redbook CD Standard (44.1 kHz)",
            subtitle = "Classic full-spectrum 20Hz–20kHz stereo mastering",
            count = cd44Sr,
            fraction = cd44Sr.toFloat() / totalTracks,
            accentColor = Color(0xFF8B5CF6)
        )
    )

    // 3. Bit Depth Breakdown
    val depth24Plus = allSongs.count { it.bitDepth >= 24 }
    val depth16 = (totalTracks - depth24Plus).coerceAtLeast(0)
    val bitDepthBreakdown = listOf(
        StatDistributionBucket(
            label = "24-Bit / 32-Bit Studio Headroom",
            subtitle = "144 dB+ dynamic range for zero noise floor",
            count = depth24Plus,
            fraction = depth24Plus.toFloat() / totalTracks,
            accentColor = Color(0xFFEC4899)
        ),
        StatDistributionBucket(
            label = "16-Bit Standard PCM",
            subtitle = "96 dB dynamic range reference",
            count = depth16,
            fraction = depth16.toFloat() / totalTracks,
            accentColor = Color(0xFF6366F1)
        )
    )

    // 4. Track Duration & Pacing Breakdown
    val shortCuts = allSongs.count { it.durationMs < 150_000L } // < 2:30
    val standardGrooves = allSongs.count { it.durationMs in 150_000L..270_000L } // 2:30 .. 4:30
    val extendedEpics = allSongs.count { it.durationMs > 270_000L } // > 4:30

    val durationVibeBreakdown = listOf(
        StatDistributionBucket(
            label = "Punchy & Concise (< 2:30)",
            subtitle = "Fast-paced intros, interludes, and high-energy cuts",
            count = shortCuts,
            fraction = shortCuts.toFloat() / totalTracks,
            accentColor = Color(0xFF38BDF8)
        ),
        StatDistributionBucket(
            label = "Core Studio Grooves (2:30 – 4:30)",
            subtitle = "Classic verse-chorus structure & radio-length tracks",
            count = standardGrooves,
            fraction = standardGrooves.toFloat() / totalTracks,
            accentColor = Color(0xFF8B5CF6)
        ),
        StatDistributionBucket(
            label = "Extended Sessions & Epics (> 4:30)",
            subtitle = "Progressive arrangements, live cuts, and deep mixes",
            count = extendedEpics,
            fraction = extendedEpics.toFloat() / totalTracks,
            accentColor = Color(0xFFF59E0B)
        )
    )

    // 5. Rotation Habits Breakdown
    val heavyRotation = allSongs.count { it.playCount >= 10 }
    val regularRotation = allSongs.count { it.playCount in 3..9 }
    val casualSpins = allSongs.count { it.playCount in 1..2 }
    val unplayedVault = allSongs.count { it.playCount <= 0 }

    val rotationTiers = listOf(
        StatDistributionBucket(
            label = "Obsessive Heavy Rotation (10+ plays)",
            subtitle = "Your signature anthems on constant repeat",
            count = heavyRotation,
            fraction = heavyRotation.toFloat() / totalTracks,
            accentColor = Color(0xFFEC4899)
        ),
        StatDistributionBucket(
            label = "Active Rotation (3 – 9 plays)",
            subtitle = "Go-to tracks in your weekly queues",
            count = regularRotation,
            fraction = regularRotation.toFloat() / totalTracks,
            accentColor = Color(0xFF8B5CF6)
        ),
        StatDistributionBucket(
            label = "Sampled Cuts (1 – 2 plays)",
            subtitle = "Lightly explored tracks",
            count = casualSpins,
            fraction = casualSpins.toFloat() / totalTracks,
            accentColor = Color(0xFF10B981)
        ),
        StatDistributionBucket(
            label = "Unplayed Vault (0 plays)",
            subtitle = "Fresh additions & deep cuts waiting in your library",
            count = unplayedVault,
            fraction = unplayedVault.toFloat() / totalTracks,
            accentColor = Color(0xFF64748B)
        )
    )

    // 6. Top Artists Leaderboard
    val topArtists = smartArtistMap.entries
        .map { (artistName, songs) ->
            val plays = songs.sumOf { it.playCount }
            val listenedMin = songs.sumOf { (it.durationMs * it.playCount.coerceAtLeast(1)) } / 60_000L
            ArtistListeningStat(
                name = artistName,
                trackCount = songs.size,
                albumCount = songs.map { it.album }.distinct().size,
                totalPlays = plays,
                totalListenedMinutes = listenedMin,
                artworkUri = songs.firstOrNull { it.albumArtUri != null }?.albumArtUri,
                songs = songs.sortedByDescending { it.playCount }
            )
        }
        .sortedWith(compareByDescending<ArtistListeningStat> { it.totalPlays }.thenByDescending { it.trackCount })
        .take(12)

    // 7. Top Albums Leaderboard
    val topAlbums = allSongs.groupBy { it.album.ifBlank { "Unknown Album" } }
        .map { (albumTitle, songs) ->
            AlbumListeningStat(
                title = albumTitle,
                artist = songs.firstOrNull()?.artist ?: "Unknown Artist",
                trackCount = songs.size,
                totalPlays = songs.sumOf { it.playCount },
                primaryFormat = songs.firstOrNull()?.format ?: "Audio",
                artworkUri = songs.firstOrNull { it.albumArtUri != null }?.albumArtUri,
                songs = songs.sortedBy { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE }
            )
        }
        .sortedWith(compareByDescending<AlbumListeningStat> { it.totalPlays }.thenByDescending { it.trackCount })
        .take(10)

    // 8. Top Played Songs
    val topPlayedSongs = allSongs
        .sortedWith(compareByDescending<Song> { it.playCount }.thenByDescending { it.isFavorite }.thenByDescending { it.bitrateKbps })
        .take(25)

    // 9. Rediscover Gems (0 or 1 play, prioritized by lossless/bitrate)
    val rediscoverGems = allSongs
        .filter { it.playCount <= 1 }
        .sortedWith(compareByDescending<Song> { it.bitrateKbps }.thenByDescending { it.sampleRateHz })
        .take(10)

    // Determine Sonic DNA Persona
    val topArtistName = topArtists.firstOrNull()?.name ?: "Eclectic Artists"
    val (persona, description) = when {
        losslessRatio >= 0.45f && avgSampleRate >= 48000 ->
            "Hi-Res Purist & Master Curator" to
                "Over ${(losslessRatio * 100).roundToInt()}% of your collection is lossless/master grade. You gravitate toward high-dynamic-range acoustics led by $topArtistName."
        heavyRotation >= (totalTracks * 0.2f).coerceAtLeast(3f) ->
            "Heavy-Rotation Loyalist" to
                "When you lock into a groove, you replay it deeply. Your queues center on anthem-level tracks from $topArtistName with ${totalPlayCount} total spins logged."
        extendedEpics >= (totalTracks * 0.35f) ->
            "Deep-Cut & Long-Form Explorer" to
                "You favor immersive, extended arrangements (> 4:30) and full album journeys over short radio edits, anchored by $topArtistName."
        totalArtists >= 25 ->
            "Wide-Spectrum Crate Digger" to
                "Your offline vault spans $totalArtists distinct artists and $totalAlbums albums, blending diverse genres and colaborations around $topArtistName."
        else ->
            "Focused Sonic Collector" to
                "A tightly curated offline catalog of $totalTracks tracks across $totalAlbums releases, featuring $topArtistName at ${avgBitrate} kbps average fidelity."
    }

    return DetailedListeningAnalytics(
        totalTracks = totalTracks,
        totalAlbums = totalAlbums,
        totalArtists = totalArtists,
        totalLibraryDurationMs = totalLibraryDurationMs,
        totalPlayCount = totalPlayCount,
        totalTimeListenedMs = totalTimeListenedMs,
        totalStorageBytes = totalStorageBytes,
        favoritesCount = favoritesCount,
        hiResLosslessCount = hiResCount,
        audiophileScore = audiophileScore,
        sonicDnaPersona = persona,
        sonicDnaDescription = description,
        averageBitrateKbps = avgBitrate,
        averageSampleRateHz = avgSampleRate,
        averageTrackDurationMs = avgDurationMs,
        formatBreakdown = formatBreakdown,
        sampleRateBreakdown = sampleRateBreakdown,
        bitDepthBreakdown = bitDepthBreakdown,
        durationVibeBreakdown = durationVibeBreakdown,
        rotationTiers = rotationTiers,
        topPlayedSongs = topPlayedSongs,
        topArtists = topArtists,
        topAlbums = topAlbums,
        rediscoverGems = rediscoverGems
    )
}
