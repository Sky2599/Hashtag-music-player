package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettings
import com.example.data.model.Song
import com.example.lyrics.LyricEngine
import com.example.player.AudioDeviceSpecs
import com.example.player.PcmWaveformExtractor
import com.example.ui.components.AlbumArtContainer
import com.example.ui.components.SynchronizedLyricsView
import com.example.ui.components.VisualizerView
import com.example.ui.components.WaveformSeekBar
import com.example.ui.dialogs.AudioPipelineDiagnosticDialog
import com.example.ui.dialogs.AudioTechnicalHudCard
import com.example.ui.theme.DeepPurple
import com.example.ui.theme.DeepPurpleDark
import com.example.ui.theme.DeepPurpleSurface
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElectricVioletLight
import com.example.ui.theme.OnPrimaryAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.util.TactileHaptics
import com.example.ui.util.instantTapDown
import androidx.compose.ui.platform.LocalView

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    currentSong: Song?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    queue: List<Song>,
    queueIndex: Int,
    isShuffle: Boolean,
    isRepeat: Boolean,
    visualizerFft: FloatArray,
    appSettings: AppSettings = AppSettings(),
    crossfadeProgress: Float = 0f,
    crossfadeSeconds: Float = 3.0f,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onSelectQueueItem: (Song) -> Unit,
    onBackClick: (() -> Unit)? = null,
    audioDeviceSpecs: AudioDeviceSpecs? = null,
    isBitPerfect: Boolean = false,
    isGapless: Boolean = true,
    isGaplessActive: Boolean = false,
    showTechnicalChips: Boolean = true,
    showVisualizerByDefault: Boolean = false,
    showAudioPipelineInNowPlaying: Boolean = false,
    onOpenPrecisionPlayback: (() -> Unit)? = null,
    onOpenSleepTimer: (() -> Unit)? = null,
    onOpenMultiQueue: (() -> Unit)? = null,
    onOpenTagEditor: (() -> Unit)? = null,
    onOpenEqualizer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isSeeking by remember { mutableStateOf(false) }
    var seekProgress by remember { mutableFloatStateOf(0f) }
    var showAudioPipelineDiagnosticDialog by remember { mutableStateOf(false) }

    // Dedicated Swappable Slot Mode:
    // 0 = Hi-Res Audio Pipeline Stats, 1 = Real-Time Spectrum Visualizer
    // Swapping between them in a single bounded slot guarantees transport controls never get pushed off screen!
    var activeModuleMode by remember(showVisualizerByDefault, showTechnicalChips) {
        mutableIntStateOf(if (showVisualizerByDefault || !showTechnicalChips) 1 else 0)
    }

    var showLyricsView by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current
    val view = LocalView.current

    val totalDuration = if (durationMs > 0) durationMs else currentSong?.durationMs ?: 1L
    val effectivePosition = if (isSeeking) (seekProgress * totalDuration).toLong() else currentPositionMs
    val nextSong = queue.getOrNull(queueIndex + 1) ?: queue.firstOrNull()

    val lyrics by androidx.compose.runtime.produceState(
        initialValue = emptyList<com.example.lyrics.LyricLine>(),
        key1 = currentSong?.id,
        key2 = showLyricsView
    ) {
        val song = currentSong
        value = if (song != null && showLyricsView) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                LyricEngine.getLyricsForSong(context, song)
            }
        } else {
            emptyList()
        }
    }

    val waveformPeaks by androidx.compose.runtime.produceState(
        initialValue = currentSong?.let { PcmWaveformExtractor.getCachedProfile(it.id)?.seekbarPeaks }
            ?: FloatArray(120) { 0.24f },
        key1 = currentSong?.id
    ) {
        val song = currentSong
        if (song != null) {
            val profile = PcmWaveformExtractor.extractWaveformProfile(context, song)
            value = profile.seekbarPeaks
        }
    }

    // Calculate dynamic crossfade progress (0.0 to 1.0)
    val crossfadeMs = ((if (crossfadeSeconds > 0f) crossfadeSeconds else 3.0f) * 1000).toLong()
    val crossfadeStartMs = (totalDuration - crossfadeMs).coerceAtLeast(0L)
    val computedCrossfadeProgress = if (isSeeking) {
        if (crossfadeMs > 0 && totalDuration > crossfadeMs && effectivePosition >= crossfadeStartMs) {
            ((effectivePosition - crossfadeStartMs).toFloat() / crossfadeMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    } else {
        if (crossfadeProgress > 0f) {
            crossfadeProgress
        } else if (crossfadeMs > 0 && totalDuration > crossfadeMs && effectivePosition >= crossfadeStartMs) {
            ((effectivePosition - crossfadeStartMs).toFloat() / crossfadeMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    var isFullScreenVisualizer by remember { mutableStateOf(false) }

    DisposableEffect(isFullScreenVisualizer) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        if (isFullScreenVisualizer) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }

    BackHandler(enabled = isFullScreenVisualizer) {
        isFullScreenVisualizer = false
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(DeepPurple)
            .statusBarsPadding()
    ) {
        if (isFullScreenVisualizer && currentSong != null) {
            FullScreenVisualizerLandscapeView(
                currentSong = currentSong,
                isPlaying = isPlaying,
                currentPositionMs = effectivePosition,
                queue = queue,
                queueIndex = queueIndex,
                visualizerFft = visualizerFft,
                isBitPerfect = isBitPerfect,
                audioDeviceSpecs = audioDeviceSpecs,
                appSettings = appSettings,
                isGapless = isGapless,
                isGaplessActive = isGaplessActive,
                onPlayPauseClick = onPlayPauseClick,
                onNextClick = onNextClick,
                onPreviousClick = onPreviousClick,
                onSelectQueueItem = onSelectQueueItem,
                onClose = { isFullScreenVisualizer = false }
            )
        } else {
            val isWideScreen = maxWidth >= 600.dp
            val useSplitLayout = isLandscape || isWideScreen

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = if (useSplitLayout) 16.dp else 0.dp,
                        vertical = 0.dp
                    )
            ) {
                if (currentSong == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No track selected. Pick a song from Songs or Albums tab.",
                            color = TextSecondary,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (useSplitLayout) {
                NowPlayingLandscapeLayout(
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    effectivePosition = effectivePosition,
                    totalDuration = totalDuration,
                    isSeeking = isSeeking,
                    seekProgress = seekProgress,
                    onSeekChange = { progress ->
                        isSeeking = true
                        seekProgress = progress
                    },
                    onSeekFinished = {
                        isSeeking = false
                        onSeekTo((seekProgress * totalDuration).toLong())
                    },
                    onSeekTo = onSeekTo,
                    isShuffle = isShuffle,
                    isRepeat = isRepeat,
                    visualizerFft = visualizerFft,
                    waveformPeaks = waveformPeaks,
                    activeModuleMode = activeModuleMode,
                    onSwapModule = { activeModuleMode = if (activeModuleMode == 0) 1 else 0 },
                    showLyricsView = showLyricsView,
                    onToggleLyrics = { showLyricsView = !showLyricsView },
                    lyrics = lyrics,
                    computedCrossfadeProgress = computedCrossfadeProgress,
                    crossfadeSeconds = crossfadeSeconds,
                    isGapless = isGapless,
                    isGaplessActive = isGaplessActive,
                    audioDeviceSpecs = audioDeviceSpecs,
                    isBitPerfect = isBitPerfect,
                    nextSong = nextSong,
                    onPlayPauseClick = onPlayPauseClick,
                    onNextClick = onNextClick,
                    onPreviousClick = onPreviousClick,
                    onToggleShuffle = onToggleShuffle,
                    onToggleRepeat = onToggleRepeat,
                    onToggleFavorite = onToggleFavorite,
                    onBackClick = onBackClick,
                    onOpenQueue = { showQueueSheet = true },
                    onOpenPrecisionPlayback = onOpenPrecisionPlayback,
                    onOpenSleepTimer = onOpenSleepTimer,
                    onOpenMultiQueue = onOpenMultiQueue,
                    onOpenTagEditor = onOpenTagEditor,
                    onOpenEqualizer = onOpenEqualizer,
                    onOpenFullScreenVisualizer = { isFullScreenVisualizer = true }
                )
            } else {
                // Portrait Handheld Layout:
                // Hero Album Art is expansive and prominent, scaling to match phone screen width.
                // Large touch targets for playback controls, bold typography, and clean status indicators.
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. Top Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onBackClick?.invoke() },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Collapse",
                                tint = TextSecondary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = "NOW PLAYING",
                                color = TextTertiary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = currentSong.album,
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { showLyricsView = !showLyricsView },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Subtitles,
                                    contentDescription = "Lyrics",
                                    tint = if (showLyricsView) ElectricVioletLight else TextSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Box {
                                var showOptionsMenu by remember { mutableStateOf(false) }

                                IconButton(
                                    onClick = { showOptionsMenu = true },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Options",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showOptionsMenu,
                                    onDismissRequest = { showOptionsMenu = false },
                                    modifier = Modifier.background(DeepPurpleSurface)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Audio Pipeline Diagnostic", color = TextPrimary) },
                                        leadingIcon = { Icon(Icons.Default.Memory, contentDescription = null, tint = ElectricVioletLight) },
                                        onClick = {
                                            showOptionsMenu = false
                                            showAudioPipelineDiagnosticDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("A-B Repeat & Pitch", color = TextPrimary) },
                                        leadingIcon = { Icon(Icons.Default.Loop, contentDescription = null, tint = ElectricVioletLight) },
                                        onClick = {
                                            showOptionsMenu = false
                                            onOpenPrecisionPlayback?.invoke()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Sleep Timer", color = TextPrimary) },
                                        leadingIcon = { Icon(Icons.Default.Bedtime, contentDescription = null, tint = ElectricVioletLight) },
                                        onClick = {
                                            showOptionsMenu = false
                                            onOpenSleepTimer?.invoke()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Multi-Queue Switcher", color = TextPrimary) },
                                        leadingIcon = { Icon(Icons.Default.Layers, contentDescription = null, tint = ElectricVioletLight) },
                                        onClick = {
                                            showOptionsMenu = false
                                            onOpenMultiQueue?.invoke()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Edit Metadata / Tags", color = TextPrimary) },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = ElectricVioletLight) },
                                        onClick = {
                                            showOptionsMenu = false
                                            onOpenTagEditor?.invoke()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Parametric Equalizer", color = TextPrimary) },
                                        leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, tint = ElectricVioletLight) },
                                        onClick = {
                                            showOptionsMenu = false
                                            onOpenEqualizer?.invoke()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Fullscreen Visualizer (Landscape)", color = TextPrimary) },
                                        leadingIcon = { Icon(Icons.Default.GraphicEq, contentDescription = null, tint = ElectricVioletLight) },
                                        onClick = {
                                            showOptionsMenu = false
                                            isFullScreenVisualizer = true
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 2. Large Hero Album Artwork / Synchronized Lyrics (Scales to phone screen width)
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val artDimension = minOf(maxWidth, maxHeight)
                        Box(
                            modifier = Modifier
                                .size(artDimension)
                                .clip(RoundedCornerShape(22.dp))
                        ) {
                            if (showLyricsView) {
                                SynchronizedLyricsView(
                                    lyrics = lyrics,
                                    currentPositionMs = effectivePosition,
                                    onSeekTo = onSeekTo,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                AlbumArtContainer(
                                    song = currentSong,
                                    albumArtUri = currentSong.albumArtUri,
                                    modifier = Modifier.fillMaxSize(),
                                    cornerRadius = 22.dp,
                                    contentDescription = "Now Playing Artwork"
                                )
                            }
                        }
                    }

                    // 3. Title, Artist & Favorite Heart
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text(
                                text = currentSong.title,
                                color = TextPrimary,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${currentSong.artist} • ${currentSong.album}",
                                color = TextSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = { onToggleFavorite(currentSong) },
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("now_playing_favorite_button")
                        ) {
                            Icon(
                                imageVector = if (currentSong.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (currentSong.isFavorite) Color(0xFFFF3366) else TextSecondary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // 4. Subtle Audio Quality Badge & Transition Status
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(DeepPurpleDark)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = currentSong.techSpecsTag.ifEmpty { "PCM 44.1kHz • 16-bit" },
                                color = ElectricVioletLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (crossfadeSeconds >= 2.0f && computedCrossfadeProgress > 0f) {
                            Text(
                                text = "Crossfading • ${(computedCrossfadeProgress * 100).toInt()}%",
                                color = ElectricVioletLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else if (isGapless && isGaplessActive) {
                            Text(
                                text = "Gapless (Armed)",
                                color = Color(0xFF00E676),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                text = nextSong?.let { "Next: ${it.title}" } ?: "Queue End",
                                color = TextTertiary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Optional Audio Pipeline / Visualizer slot (ONLY rendered if user enables it in Settings!)
                    if (showAudioPipelineInNowPlaying) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (activeModuleMode == 0 && audioDeviceSpecs != null) {
                                AudioTechnicalHudCard(
                                    song = currentSong,
                                    deviceSpecs = audioDeviceSpecs,
                                    isBitPerfect = isBitPerfect,
                                    isGaplessActive = isGaplessActive,
                                    isGaplessEnabled = isGapless,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            TactileHaptics.toggleClick(view)
                                            activeModuleMode = 1
                                        }
                                )
                            } else {
                                VisualizerView(
                                    fftValues = visualizerFft,
                                    height = 54.dp,
                                    style = appSettings.visualizerStyle,
                                    colorScheme = appSettings.visualizerColorScheme,
                                    sensitivity = appSettings.visualizerSensitivity,
                                    barCount = appSettings.visualizerBarCount,
                                    showPeakCaps = appSettings.visualizerPeakCaps,
                                    showLabels = false,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("now_playing_visualizer_card"),
                                    onClick = {
                                        TactileHaptics.lightImpact(view)
                                        isFullScreenVisualizer = true
                                    }
                                )
                            }
                        }
                    }

                    // 5. Interactive Decoded PCM Waveform Seek Bar & Timestamps
                    Column(modifier = Modifier.fillMaxWidth()) {
                        WaveformSeekBar(
                            progress = if (totalDuration > 0) (effectivePosition.toFloat() / totalDuration).coerceIn(0f, 1f) else 0f,
                            waveformPeaks = waveformPeaks,
                            onSeekChange = {
                                isSeeking = true
                                seekProgress = it
                            },
                            onSeekFinished = {
                                isSeeking = false
                                onSeekTo((seekProgress * totalDuration).toLong())
                            },
                            height = 36.dp,
                            activeColor = ElectricVioletLight,
                            activeBottomColor = ElectricViolet,
                            inactiveColor = DeepPurpleDark,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("now_playing_seek_bar")
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatTime(effectivePosition),
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = formatTime(totalDuration),
                                color = TextTertiary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // 6. Prominent Transport Controls (Shuffle, Previous, Play/Pause, Next, Repeat)
                    val npView = LocalView.current
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                TactileHaptics.toggleClick(npView)
                                onToggleShuffle()
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (isShuffle) ElectricVioletLight else TextTertiary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                TactileHaptics.lightImpact(npView)
                                onPreviousClick()
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .testTag("now_playing_previous_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                tint = TextPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Central Play/Pause button: 72dp with 40dp icon
                        IconButton(
                            onClick = {
                                TactileHaptics.lightImpact(npView)
                                onPlayPauseClick()
                            },
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(ElectricViolet)
                                .testTag("now_playing_play_pause_button")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = OnPrimaryAccent,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                TactileHaptics.lightImpact(npView)
                                onNextClick()
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .testTag("now_playing_next_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = TextPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                TactileHaptics.toggleClick(npView)
                                onToggleRepeat()
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = "Repeat",
                                tint = if (isRepeat) ElectricVioletLight else TextTertiary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // 7. Bottom Queue Bar
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                TactileHaptics.lightImpact(npView)
                                showQueueSheet = true
                            }
                            .testTag("now_playing_bottom_queue_bar"),
                        colors = CardDefaults.cardColors(containerColor = DeepPurpleDark)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QueueMusic,
                                    contentDescription = "Queue sheet",
                                    tint = ElectricVioletLight,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = nextSong?.let { "Up next: ${it.title}" } ?: "Playing Queue",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Text(
                                text = "${queue.size} songs",
                                color = ElectricVioletLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Queue Modal Bottom Sheet
            if (showQueueSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showQueueSheet = false },
                    sheetState = sheetState,
                    containerColor = DeepPurpleSurface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Current Queue (${queue.size} songs)",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(bottom = 30.dp)
                        ) {
                            itemsIndexed(queue, key = { idx, song -> "${song.id}_$idx" }, contentType = { _, _ -> "queue_item" }) { idx, song ->
                                val isCurrent = idx == queueIndex
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isCurrent) ElectricViolet.copy(alpha = 0.25f) else Color.Transparent)
                                        .clickable {
                                            onSelectQueueItem(song)
                                            showQueueSheet = false
                                        }
                                        .padding(vertical = 8.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${idx + 1}",
                                        color = if (isCurrent) ElectricVioletLight else TextTertiary,
                                        fontSize = 12.sp,
                                        modifier = Modifier.width(24.dp)
                                    )

                                    AlbumArtContainer(
                                        albumArtUri = song.albumArtUri,
                                        size = 36.dp,
                                        cornerRadius = 6.dp
                                    )

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.title,
                                            color = if (isCurrent) ElectricVioletLight else TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${song.artist} • ${song.durationFormatted}",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }

                                    if (isCurrent) {
                                        Icon(
                                            imageVector = Icons.Default.Equalizer,
                                            contentDescription = "Currently playing",
                                            tint = ElectricVioletLight,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Audio Pipeline Diagnostic Dialog
            if (showAudioPipelineDiagnosticDialog && audioDeviceSpecs != null) {
                AudioPipelineDiagnosticDialog(
                    song = currentSong,
                    deviceSpecs = audioDeviceSpecs,
                    isBitPerfect = isBitPerfect,
                    isGaplessActive = isGaplessActive,
                    isGaplessEnabled = isGapless,
                    onDismiss = { showAudioPipelineDiagnosticDialog = false }
                )
            }
        }
        }
    }
}

@Composable
private fun NowPlayingLandscapeLayout(
    currentSong: Song,
    isPlaying: Boolean,
    effectivePosition: Long,
    totalDuration: Long,
    isSeeking: Boolean,
    seekProgress: Float,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onSeekTo: (Long) -> Unit,
    isShuffle: Boolean,
    isRepeat: Boolean,
    visualizerFft: FloatArray,
    waveformPeaks: FloatArray,
    activeModuleMode: Int,
    onSwapModule: () -> Unit,
    showLyricsView: Boolean,
    onToggleLyrics: () -> Unit,
    lyrics: List<com.example.lyrics.LyricLine>,
    computedCrossfadeProgress: Float,
    crossfadeSeconds: Float,
    isGapless: Boolean,
    isGaplessActive: Boolean,
    audioDeviceSpecs: AudioDeviceSpecs?,
    isBitPerfect: Boolean,
    nextSong: Song?,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onBackClick: (() -> Unit)?,
    onOpenQueue: () -> Unit,
    onOpenPrecisionPlayback: (() -> Unit)?,
    onOpenSleepTimer: (() -> Unit)?,
    onOpenMultiQueue: (() -> Unit)?,
    onOpenTagEditor: (() -> Unit)?,
    onOpenEqualizer: (() -> Unit)?,
    onOpenFullScreenVisualizer: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Column (Artwork, Compact Header & Info)
        Column(
            modifier = Modifier
                .weight(0.42f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row in Left Column
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onBackClick?.invoke() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = currentSong.album,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    textAlign = TextAlign.Center
                )

                Box {
                    var showOptionsMenu by remember { mutableStateOf(false) }

                    IconButton(
                        onClick = { showOptionsMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showOptionsMenu,
                        onDismissRequest = { showOptionsMenu = false },
                        modifier = Modifier.background(DeepPurpleSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("A-B Repeat & Pitch", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Loop, contentDescription = null, tint = ElectricVioletLight) },
                            onClick = {
                                showOptionsMenu = false
                                onOpenPrecisionPlayback?.invoke()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sleep Timer", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Bedtime, contentDescription = null, tint = ElectricVioletLight) },
                            onClick = {
                                showOptionsMenu = false
                                onOpenSleepTimer?.invoke()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Multi-Queue Switcher", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Layers, contentDescription = null, tint = ElectricVioletLight) },
                            onClick = {
                                showOptionsMenu = false
                                onOpenMultiQueue?.invoke()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit Metadata / Tags", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = ElectricVioletLight) },
                            onClick = {
                                showOptionsMenu = false
                                onOpenTagEditor?.invoke()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Parametric Equalizer", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, tint = ElectricVioletLight) },
                            onClick = {
                                showOptionsMenu = false
                                onOpenEqualizer?.invoke()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Fullscreen Visualizer", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.GraphicEq, contentDescription = null, tint = ElectricVioletLight) },
                            onClick = {
                                showOptionsMenu = false
                                onOpenFullScreenVisualizer?.invoke()
                            }
                        )
                    }
                }
            }

            // Central Square Art or Lyrics
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f, matchHeightConstraintsFirst = true)
                    .padding(vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                if (showLyricsView) {
                    SynchronizedLyricsView(
                        lyrics = lyrics,
                        currentPositionMs = effectivePosition,
                        onSeekTo = onSeekTo,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AlbumArtContainer(
                        albumArtUri = currentSong.albumArtUri,
                        modifier = Modifier.fillMaxSize(),
                        cornerRadius = 12.dp,
                        contentDescription = "Now Playing Artwork"
                    )
                }

                // Hi-Res Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(DeepPurpleDark.copy(alpha = 0.85f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = currentSong.techSpecsTag,
                        color = ElectricVioletLight,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Favorite Heart Button
                IconButton(
                    onClick = { onToggleFavorite(currentSong) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(DeepPurpleDark.copy(alpha = 0.7f))
                        .testTag("now_playing_landscape_fav")
                ) {
                    Icon(
                        imageVector = if (currentSong.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (currentSong.isFavorite) Color.Red else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Stream Info Chip
            if (audioDeviceSpecs != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(DeepPurpleDark)
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${audioDeviceSpecs.deviceName} • ${if (isBitPerfect) "Direct HW" else "AudioTrack"} • ${audioDeviceSpecs.maxSampleRate}",
                        color = ElectricVioletLight,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Right Column (Metadata, Swappable Module, Seekbar, Transport Controls, Quick Bar)
        Column(
            modifier = Modifier
                .weight(0.58f)
                .fillMaxHeight()
                .padding(vertical = 2.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Track Title & Quick Action Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSong.title.uppercase(),
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${currentSong.artist.uppercase()} • ${currentSong.album.uppercase()}",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleLyrics,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Subtitles,
                            contentDescription = "Toggle Lyrics",
                            tint = if (showLyricsView) ElectricVioletLight else TextTertiary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    IconButton(
                        onClick = onSwapModule,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Swap Visualizer & Pipeline",
                            tint = ElectricVioletLight,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenQueue,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = "Queue Sheet",
                            tint = ElectricVioletLight,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            // Crossfade / Gapless Status Pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DeepPurpleDark),
                contentAlignment = Alignment.CenterStart
            ) {
                if (crossfadeSeconds >= 2.0f && computedCrossfadeProgress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = computedCrossfadeProgress.coerceIn(0.01f, 1f))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        ElectricVioletLight.copy(alpha = 0.25f),
                                        ElectricVioletLight.copy(alpha = 0.55f)
                                    )
                                )
                            )
                    )
                }

                val nextTitle = nextSong?.title ?: "End of queue"
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    if (crossfadeSeconds >= 2.0f) {
                        val progressInt = (computedCrossfadeProgress * 100).toInt()
                        Text(
                            text = "Crossfade • $progressInt% • Next: $nextTitle",
                            color = if (computedCrossfadeProgress > 0f) Color.White else TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (isGapless) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isGaplessActive) Color(0xFF00E676) else ElectricVioletLight)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isGaplessActive) "Gapless 0ms (Armed) • Next: $nextTitle" else "Gapless • Next: $nextTitle",
                            color = if (isGaplessActive) Color(0xFF00E676) else TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = "Next: $nextTitle",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Designated Swappable Area in Landscape
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(66.dp),
                contentAlignment = Alignment.Center
            ) {
                if (activeModuleMode == 0 && audioDeviceSpecs != null) {
                    AudioTechnicalHudCard(
                        song = currentSong,
                        deviceSpecs = audioDeviceSpecs,
                        isBitPerfect = isBitPerfect,
                        isGaplessActive = isGaplessActive,
                        isGaplessEnabled = isGapless,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSwapModule() }
                    )
                } else {
                    VisualizerView(
                        fftValues = visualizerFft,
                        height = 60.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("landscape_visualizer_card"),
                        onClick = {
                            onOpenFullScreenVisualizer?.invoke()
                        }
                    )
                }
            }

            // Interactive Decoded PCM Waveform Seek Bar & Timestamps
            Column(modifier = Modifier.fillMaxWidth()) {
                WaveformSeekBar(
                    progress = if (totalDuration > 0) (effectivePosition.toFloat() / totalDuration).coerceIn(0f, 1f) else 0f,
                    waveformPeaks = waveformPeaks,
                    onSeekChange = onSeekChange,
                    onSeekFinished = onSeekFinished,
                    height = 28.dp,
                    activeColor = ElectricVioletLight,
                    activeBottomColor = ElectricViolet,
                    inactiveColor = DeepPurpleDark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("now_playing_landscape_seek_bar")
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = formatTime(effectivePosition), color = TextTertiary, fontSize = 10.sp)
                    Text(text = "${currentSong.techSpecsTag} • 32-Bit Biquad PCM", color = ElectricVioletLight.copy(alpha = 0.8f), fontSize = 9.sp)
                    Text(text = formatTime(totalDuration), color = TextTertiary, fontSize = 10.sp)
                }
            }

            // Transport Controls: Shuffle, Previous, Play/Pause, Next, Repeat
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) ElectricVioletLight else TextTertiary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(
                    onClick = onPreviousClick,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("now_playing_landscape_previous")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = TextPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Central Play/Pause FAB
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(ElectricViolet)
                        .clickable { onPlayPauseClick() }
                        .testTag("now_playing_landscape_play_pause"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = OnPrimaryAccent,
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(
                    onClick = onNextClick,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("now_playing_landscape_next")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = TextPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(
                    onClick = onToggleRepeat,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = if (isRepeat) ElectricVioletLight else TextTertiary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            // Bottom Quick Utility Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DeepPurpleDark)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onOpenEqualizer?.invoke() }
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Tune, contentDescription = "Equalizer", tint = ElectricVioletLight, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Equalizer", color = TextSecondary, fontSize = 10.sp)
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onOpenPrecisionPlayback?.invoke() }
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Loop, contentDescription = "Speed & Pitch", tint = ElectricVioletLight, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Speed / Pitch", color = TextSecondary, fontSize = 10.sp)
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onOpenSleepTimer?.invoke() }
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Bedtime, contentDescription = "Sleep Timer", tint = ElectricVioletLight, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sleep Timer", color = TextSecondary, fontSize = 10.sp)
                }
            }
        }
    }
}

/**
 * Fullscreen Landscape Visualizer:
 * Takes over the entire landscape display with a 50/50 split:
 * - Half the screen features the live, punchy 21-band FFT spectrum visualizer filling full vertical height.
 * - The other half displays only the album cover artwork, with an exit 'x' icon button in the top-right corner.
 */
/**
 * Fullscreen Landscape Visualizer:
 * Takes over the entire landscape display with a 50/50 split:
 * - Half 1: Live, punchy 21-band FFT spectrum visualizer, scrollable via simple gesture into the detailed Audio Pipeline HUD.
 * - Half 2: Only the album cover artwork, with an exit 'x' icon button in the top-right corner.
 * - Standby mode keeps screen awake while music is playing.
 */
@Composable
private fun FullScreenVisualizerLandscapeView(
    currentSong: Song,
    isPlaying: Boolean,
    currentPositionMs: Long = 0L,
    queue: List<Song> = emptyList(),
    queueIndex: Int = 0,
    visualizerFft: FloatArray,
    isBitPerfect: Boolean,
    audioDeviceSpecs: AudioDeviceSpecs?,
    appSettings: AppSettings = AppSettings(),
    isGapless: Boolean = true,
    isGaplessActive: Boolean = false,
    onPlayPauseClick: () -> Unit = {},
    onNextClick: () -> Unit = {},
    onPreviousClick: () -> Unit = {},
    onSelectQueueItem: (Song) -> Unit = {},
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Standby Mode: Keep screen on while music is actively playing in Fullscreen mode without WindowManager sync storms
    DisposableEffect(isPlaying) {
        view.keepScreenOn = isPlaying
        onDispose {
            view.keepScreenOn = false
        }
    }

    // Scrollable Pager: Page 0 = Live Visualizer, Page 1 = Audio Pipeline Info
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF07050C))
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("fullscreen_visualizer_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // HALF 1: Live Reactive Visualizer / Audio Pipeline Info (50% of screen)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(end = 10.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0D0B16))
            ) {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    if (page == 0) {
                        // Page 0: Full-height Live Spectrum Visualizer (Lowered baseline, tall dynamic bands)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            // Header Badge & Telemetry
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isPlaying) Color(0xFF00E676) else TextTertiary)
                                    )
                                    Text(
                                        text = "LIVE SPECTRUM VISUALIZER",
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }

                                Text(
                                    text = if (isBitPerfect) "BIT-PERFECT DIRECT" else "${appSettings.visualizerBarCount}-BAND FFT • HI-RES",
                                    color = ElectricVioletLight,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Full-height VisualizerView with lowered base and full dynamic height
                            VisualizerView(
                                fftValues = visualizerFft,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                height = null,
                                style = appSettings.visualizerStyle,
                                colorScheme = appSettings.visualizerColorScheme,
                                sensitivity = appSettings.visualizerSensitivity,
                                barCount = appSettings.visualizerBarCount,
                                showPeakCaps = appSettings.visualizerPeakCaps,
                                showLabels = appSettings.visualizerShowLabels,
                                backgroundColor = Color.Transparent
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Scroll indicator to flip to Audio Pipeline Info
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        TactileHaptics.lightImpact(view)
                                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                                    }
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Scroll to Audio Pipeline Info",
                                    tint = ElectricVioletLight,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Scroll down for Audio Pipeline Info",
                                    color = TextTertiary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        // Page 1: Audio Pipeline Information HUD
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            // Header Badge & Codec Pill
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isBitPerfect) Color(0xFF00E676) else ElectricVioletLight)
                                    )
                                    Text(
                                        text = "AUDIO PIPELINE ARCHITECTURE",
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF241640))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = currentSong.format,
                                        color = TextPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Telemetry Rows inside smooth scrollable container
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Source Specs Card
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(DeepPurpleDark)
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("SOURCE FILE DECODER", color = TextTertiary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            "${currentSong.sampleRateHz / 1000.0} kHz • ${currentSong.bitDepth}-bit PCM",
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("BITRATE", color = TextTertiary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            "${currentSong.bitrateKbps} kbps",
                                            color = ElectricVioletLight,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // DSP Pipeline & Sink
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(DeepPurpleDark)
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("DSP ENGINE PIPELINE", color = TextTertiary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            if (isBitPerfect) "Direct Bit-Perfect Bypass" else "32-bit Floating Pt DSP • AutoEQ Active",
                                            color = if (isBitPerfect) Color(0xFF00E676) else TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("HARDWARE SINK", color = TextTertiary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            audioDeviceSpecs?.deviceType ?: "AudioTrack Direct",
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                // Audio Device & Buffer
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(DeepPurpleDark)
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("OUTPUT DEVICE", color = TextTertiary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            audioDeviceSpecs?.deviceName ?: "System Audio Sink",
                                            color = TextPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("PLAYBACK ENGINE", color = TextTertiary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            if (isGapless && isGaplessActive) "Gapless Seamless" else (audioDeviceSpecs?.activeCodec ?: "Direct Audio HAL"),
                                            color = if (isGapless && isGaplessActive) Color(0xFF00E676) else ElectricVioletLight,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Scroll indicator to flip back to Visualizer
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        TactileHaptics.lightImpact(view)
                                        coroutineScope.launch { pagerState.animateScrollToPage(0) }
                                    }
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Scroll to Visualizer",
                                    tint = ElectricVioletLight,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Scroll up for Spectrum Visualizer",
                                    color = TextTertiary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // HALF 2: Landscape Standby Album Side with quick gestures, rising Up Next queue panel, and world clock
            LandscapeStandbyAlbumSide(
                currentSong = currentSong,
                isPlaying = isPlaying,
                currentPositionMs = currentPositionMs,
                queue = queue,
                queueIndex = queueIndex,
                onPlayPauseClick = onPlayPauseClick,
                onNextClick = onNextClick,
                onPreviousClick = onPreviousClick,
                onSelectQueueItem = onSelectQueueItem,
                onClose = onClose,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 10.dp)
            )
        }
    }
}

