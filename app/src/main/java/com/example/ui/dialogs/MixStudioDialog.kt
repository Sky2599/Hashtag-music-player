package com.example.ui.dialogs

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Save
import com.example.data.db.CustomPlaylistEntity
import com.example.data.db.PlaylistTransitionEntity
import com.example.player.AutomixDspEngine
import com.example.player.HarmonicCompatibility
import com.example.data.model.Song
import com.example.ui.components.AlbumArtContainer
import com.example.ui.theme.DeepPurple
import com.example.ui.theme.DeepPurpleCard
import com.example.ui.theme.DeepPurpleDark
import com.example.ui.theme.DeepPurpleSurface
import com.example.ui.theme.DividerPurple
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElectricVioletLight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// Dynamic Theme-Aware Studio Color Palette matching the app's Purple / Dynamic Theme
private val StudioBlack: Color
    @Composable get() = DeepPurpleDark
private val StudioPanelBg: Color
    @Composable get() = DeepPurpleSurface
private val StudioWaveformBoxBg: Color
    @Composable get() = DeepPurpleCard
private val StudioGridLine: Color
    @Composable get() = DividerPurple
private val StudioInactiveWave: Color
    @Composable get() = TextTertiary.copy(alpha = 0.45f)

// 3-Band Frequency Colors (Low = Royal Blue/Violet, Mid = Copper Amber, High = Cream/Gold)
private val WaveLowBlue = Color(0xFF4F46E5)
private val WaveMidAmber = Color(0xFFB45309)
private val WaveHighCream = Color(0xFFFFF4D4)
private val WaveHighGold = Color(0xFFFFCA3A)

// Automation Envelope & Indicator Colors (Themed to match the app's dynamic palette)
private val EnvelopeVolumeBlue = Color(0xFF67C5EA)
private val EnvelopeFilterPurple: Color
    @Composable get() = ElectricVioletLight
private val CrossfadeBoundsYellow = Color(0xFFE5B924)
private val PlayheadSpotifyGreen: Color
    @Composable get() = ElectricViolet

// Camelot Badge Colors
private val CamelotWarmOrange = Color(0xFFE89A74)
private val CamelotVibrantGreen = Color(0xFFC084FC)
private val CamelotCyan = Color(0xFF67E8F9)
private val CamelotViolet = Color(0xFFA855F7)

enum class BeatGridOption(val bars: Int, val label: String) {
    BARS_16(16, "16 bars"),
    BARS_8(8, "8 bars"),
    BARS_4(4, "4 bars"),
    BARS_2(2, "2 bars");

    fun durationSecondsForBpm(bpm: Int): Int {
        val safeBpm = bpm.coerceIn(70, 240)
        // 4 beats per bar -> (bars * 4 * 60) / bpm
        return ((bars * 4 * 60f) / safeBpm.toFloat()).roundToInt().coerceIn(3, 45)
    }
}

enum class MixCurvePreset(
    val title: String,
    val description: String,
    val topFilterDipX: Float,
    val topFilterDipY: Float,
    val bottomFilterEndX: Float,
    val volumeSlope: Float
) {
    STUDIO_DEFAULT(
        title = "Dynamic Automix",
        description = "Resonant low-pass dip on outgoing + diagonal high-pass entry sweep",
        topFilterDipX = 0.12f,
        topFilterDipY = 0.65f,
        bottomFilterEndX = 0.48f,
        volumeSlope = 0.0f
    ),
    HARMONIC_AUTO_BLEND(
        title = "Auto-Blend Harmonic",
        description = "Phase-locked tempo sync with smooth equal-power EQ crossover",
        topFilterDipX = 0.20f,
        topFilterDipY = 0.56f,
        bottomFilterEndX = 0.54f,
        volumeSlope = 0.16f
    ),
    BASS_SWAP(
        title = "Club Bass Swap",
        description = "Instant sub-bass crossover at midpoint with mid-high shimmer blend",
        topFilterDipX = 0.26f,
        topFilterDipY = 0.78f,
        bottomFilterEndX = 0.36f,
        volumeSlope = 0.24f
    ),
    SMOOTH_FADE(
        title = "Long Progressive",
        description = "Gradual full-window filter and gain taper across all bars",
        topFilterDipX = 0.34f,
        topFilterDipY = 0.46f,
        bottomFilterEndX = 0.70f,
        volumeSlope = 0.32f
    )
}

data class AnalyzedMixTrack(
    val song: Song,
    val bpm: Int,
    val camelotKey: String,
    val musicalKeyName: String = "",
    val keyBadgeColor: Color,
    val durationFormatted: String,
    val seed: Int
)

private object TrackAudioAnalyzer {
    private val camelotWheel = listOf(
        "1A" to CamelotVibrantGreen,
        "2A" to CamelotVibrantGreen,
        "3A" to CamelotCyan,
        "4A" to CamelotWarmOrange,
        "5A" to CamelotWarmOrange,
        "6A" to CamelotViolet,
        "7A" to CamelotVibrantGreen,
        "8A" to CamelotCyan,
        "9A" to CamelotWarmOrange,
        "10A" to CamelotViolet,
        "11A" to CamelotCyan,
        "12A" to CamelotWarmOrange,
        "1B" to CamelotVibrantGreen,
        "5B" to CamelotWarmOrange,
        "8B" to CamelotCyan,
        "11B" to CamelotViolet
    )

    fun analyze(song: Song, isOutgoing: Boolean): AnalyzedMixTrack {
        val meta = AutomixDspEngine.analyzeTrack(song)
        val totalSec = (song.durationMs / 1000L).coerceAtLeast(1L)
        val mins = totalSec / 60L
        val secs = totalSec % 60L
        val hash = abs("${song.id}_${song.title}_${song.artist}".hashCode())

        return AnalyzedMixTrack(
            song = song,
            bpm = meta.bpm,
            camelotKey = meta.camelotKey,
            musicalKeyName = meta.musicalKeyName,
            keyBadgeColor = if (isOutgoing) CamelotWarmOrange else CamelotVibrantGreen,
            durationFormatted = String.format("%d:%02d", mins, secs),
            seed = (hash % 9000) + 101
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MixStudioDialog(
    currentSong: Song?,
    queue: List<Song>,
    queueIndex: Int,
    allSongs: List<Song>,
    isPlaying: Boolean,
    currentPositionMs: Long,
    crossfadeProgress: Float,
    crossfadeSeconds: Float,
    playlists: List<CustomPlaylistEntity> = emptyList(),
    onCrossfadeChange: (Float) -> Unit,
    onTogglePlayPause: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onStartTransitionPreview: (Song, Song?, Float, Float, PlaylistTransitionEntity?) -> Unit,
    onSeekTransitionPreview: (Song, Float, Float) -> Unit,
    onStopTransitionPreview: () -> Unit = {},
    onSavePlaylistTransition: (PlaylistTransitionEntity) -> Unit = {},
    onSetAudioMuted: (Boolean) -> Unit,
    onToggleFavorite: (Song) -> Unit = {},
    onApplyBandLevel: (Int, Int) -> Unit = { _, _ -> },
    onDismiss: () -> Unit
) {
    // Initialize Outgoing & Incoming exclusively from the user's real songs (Queue or Library)
    val defaultOutgoing = remember(currentSong, queue, queueIndex, allSongs) {
        currentSong ?: queue.getOrNull(queueIndex) ?: allSongs.firstOrNull()
    }
    val defaultIncoming = remember(defaultOutgoing, queue, queueIndex, allSongs) {
        queue.getOrNull(queueIndex + 1)
            ?: allSongs.firstOrNull { it.id != defaultOutgoing?.id }
            ?: defaultOutgoing
    }

    var selectedOutgoingSong by remember { mutableStateOf(defaultOutgoing) }
    var selectedIncomingSong by remember { mutableStateOf(defaultIncoming) }

    // Keep synced if library finishes scanning while dialog is open
    LaunchedEffect(defaultOutgoing, defaultIncoming) {
        if (selectedOutgoingSong == null && defaultOutgoing != null) {
            selectedOutgoingSong = defaultOutgoing
        }
        if (selectedIncomingSong == null && defaultIncoming != null) {
            selectedIncomingSong = defaultIncoming
        }
    }

    // Keep favorite flag synced with latest allSongs state
    val liveOutgoingSong = remember(selectedOutgoingSong, allSongs) {
        selectedOutgoingSong?.let { sel -> allSongs.find { it.id == sel.id } ?: sel }
    }
    val liveIncomingSong = remember(selectedIncomingSong, allSongs) {
        selectedIncomingSong?.let { sel -> allSongs.find { it.id == sel.id } ?: sel }
    }

    val outgoingAnalyzed = remember(liveOutgoingSong) {
        liveOutgoingSong?.let { TrackAudioAnalyzer.analyze(it, isOutgoing = true) }
    }
    val incomingAnalyzed = remember(liveIncomingSong) {
        liveIncomingSong?.let { TrackAudioAnalyzer.analyze(it, isOutgoing = false) }
    }

    var selectedBeatGrid by remember { mutableStateOf(BeatGridOption.BARS_16) }
    var showBeatGridMenu by remember { mutableStateOf(false) }
    var selectedPreset by remember { mutableStateOf(MixCurvePreset.STUDIO_DEFAULT) }

    val transitionDurationSec = remember(selectedBeatGrid, outgoingAnalyzed) {
        selectedBeatGrid.durationSecondsForBpm(outgoingAnalyzed?.bpm ?: 120)
    }

    // Playhead progress (0.0f .. 1.0f across the transition window)
    var playheadProgress by remember { mutableFloatStateOf(0.08f) }
    var isPreviewActive by remember { mutableStateOf(false) }
    var isAudioMuted by remember { mutableStateOf(false) }
    var isCurveEditMode by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var trackPickerSlot by remember { mutableStateOf<Int?>(null) } // 1 = Outgoing, 2 = Incoming
    var pickerSearchQuery by remember { mutableStateOf("") }

    // Custom interactive filter & volume envelope parameters
    var customFilterDipX by remember(selectedPreset) { mutableFloatStateOf(selectedPreset.topFilterDipX) }
    var customFilterDipY by remember(selectedPreset) { mutableFloatStateOf(selectedPreset.topFilterDipY) }
    var customBottomSweepEndX by remember(selectedPreset) { mutableFloatStateOf(selectedPreset.bottomFilterEndX) }
    var customVolumeSlope by remember(selectedPreset) { mutableFloatStateOf(selectedPreset.volumeSlope) }

    var isBpmLockEnabled by remember { mutableStateOf(true) }
    var showHarmonicInfoSheet by remember { mutableStateOf(false) }
    var showSavePlaylistSheet by remember { mutableStateOf(false) }
    var activeDspHudLabel by remember { mutableStateOf<String?>(null) }

    val harmonicCompatibility = remember(outgoingAnalyzed, incomingAnalyzed) {
        if (outgoingAnalyzed != null && incomingAnalyzed != null) {
            AutomixDspEngine.evaluateHarmonicCompatibility(outgoingAnalyzed.camelotKey, incomingAnalyzed.camelotKey)
        } else {
            HarmonicCompatibility.PERFECT_MATCH
        }
    }

    val currentTransitionEntity = remember(
        selectedBeatGrid, transitionDurationSec, selectedPreset,
        customFilterDipX, customFilterDipY, customBottomSweepEndX, customVolumeSlope,
        outgoingAnalyzed, incomingAnalyzed, isBpmLockEnabled, liveOutgoingSong, liveIncomingSong
    ) {
        val outSong = liveOutgoingSong
        val inSong = liveIncomingSong
        if (outSong != null && inSong != null) {
            val (lpfCut, hpfCut) = AutomixDspEngine.computeFilterCutoffs(
                0.5f, customFilterDipX, customFilterDipY, customBottomSweepEndX
            )
            PlaylistTransitionEntity(
                id = "transition_${outSong.id}_${inSong.id}",
                playlistId = "global_automix",
                outgoingSongId = outSong.id,
                incomingSongId = inSong.id,
                barCount = selectedBeatGrid.bars,
                durationSeconds = transitionDurationSec.toFloat(),
                presetName = selectedPreset.title,
                topFilterDipX = customFilterDipX,
                topFilterDipY = customFilterDipY,
                bottomFilterEndX = customBottomSweepEndX,
                volumeSlope = customVolumeSlope,
                targetBpm = incomingAnalyzed?.bpm ?: outgoingAnalyzed?.bpm ?: 120,
                bpmLockEnabled = isBpmLockEnabled,
                hpfCutoffHz = hpfCut,
                lpfCutoffHz = lpfCut
            )
        } else null
    }

    // Ensure audio un-mutes and pre-fader loop preview stops when leaving Mix Studio
    DisposableEffect(Unit) {
        onDispose {
            onSetAudioMuted(false)
            onStopTransitionPreview()
        }
    }

    // Seamlessly advance the Mix Studio deck when the crossfade transition completes into the incoming track,
    // keeping the music flowing continuously through the rest of the playlist without restarting or stopping.
    LaunchedEffect(currentSong?.id, queue, queueIndex) {
        val activeSong = currentSong ?: return@LaunchedEffect
        if (selectedIncomingSong != null &&
            activeSong.id == selectedIncomingSong?.id &&
            selectedOutgoingSong?.id != activeSong.id
        ) {
            selectedOutgoingSong = activeSong
            val nextIdx = queueIndex + 1
            val nextInQueue = if (nextIdx in queue.indices) {
                queue[nextIdx]
            } else {
                allSongs.firstOrNull { it.id != activeSong.id }
            }
            if (nextInQueue != null && nextInQueue.id != activeSong.id) {
                selectedIncomingSong = nextInQueue
            }
            playheadProgress = 0.0f
            isPreviewActive = false
        }
    }

    // Synchronize the playhead with actual audio playback position without EVER interrupting or restarting playback
    LaunchedEffect(isPlaying, currentPositionMs, crossfadeProgress, liveOutgoingSong, liveIncomingSong, currentSong?.id, transitionDurationSec) {
        val outSong = liveOutgoingSong ?: return@LaunchedEffect
        if (!isPlaying) {
            if (isPreviewActive) isPreviewActive = false
            return@LaunchedEffect
        }

        if (crossfadeProgress > 0.01f) {
            isPreviewActive = true
            playheadProgress = crossfadeProgress.coerceIn(0f, 1f)
        } else if (currentSong?.id == outSong.id && outSong.durationMs > 0L) {
            val transMs = (transitionDurationSec * 1000L).coerceAtMost((outSong.durationMs * 0.75f).toLong().coerceAtLeast(4000L))
            val startMs = (outSong.durationMs - transMs).coerceAtLeast(0L)
            if (currentPositionMs >= startMs) {
                isPreviewActive = true
                val frac = ((currentPositionMs - startMs).toFloat() / transMs.toFloat()).coerceIn(0f, 1f)
                playheadProgress = frac
            } else if (isPreviewActive) {
                val preFrac = (currentPositionMs.toFloat() / startMs.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                playheadProgress = (preFrac * 0.1f).coerceIn(0f, 0.1f)
            }
        }
    }

    // Auto-dismiss status banner
    LaunchedEffect(statusMessage) {
        if (statusMessage != null) {
            delay(3200L)
            statusMessage = null
        }
    }

    val elapsedSeconds = (playheadProgress * transitionDurationSec).toInt().coerceIn(0, transitionDurationSec)
    val elapsedFormatted = String.format("%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60)
    val totalFormatted = String.format("%02d:%02d", transitionDurationSec / 60, transitionDurationSec % 60)

    val triggerPreviewToggle: () -> Unit = {
        val outSong = liveOutgoingSong
        if (outSong == null) {
            trackPickerSlot = 1
        } else {
            val nextState = !isPreviewActive
            isPreviewActive = nextState
            if (nextState) {
                onStartTransitionPreview(
                    outSong,
                    liveIncomingSong,
                    transitionDurationSec.toFloat(),
                    playheadProgress,
                    currentTransitionEntity
                )
                statusMessage = "Pre-Fader Loop Preview Active (Looping ${selectedBeatGrid.label})"
            } else {
                onStopTransitionPreview()
                if (isPlaying) {
                    onTogglePlayPause()
                }
                statusMessage = "Pre-Fader Loop Preview Stopped"
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(StudioBlack)
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag("mix_studio_dialog")
        ) {
            val isWideTablet = maxWidth >= 720.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Top Studio Header Bar
                MixStudioTopBar(
                    outgoingAnalyzed = outgoingAnalyzed,
                    incomingAnalyzed = incomingAnalyzed,
                    harmonicCompatibility = harmonicCompatibility,
                    onBackClick = onDismiss,
                    onAutoBlendClick = {
                        val outTrack = outgoingAnalyzed
                        val inTrack = incomingAnalyzed
                        if (outTrack == null) {
                            trackPickerSlot = 1
                        } else {
                            val recommendation = AutomixDspEngine.calculateAutoBlend(outTrack.song, inTrack?.song)
                            selectedBeatGrid = when (recommendation.barCount) {
                                16 -> BeatGridOption.BARS_16
                                8 -> BeatGridOption.BARS_8
                                4 -> BeatGridOption.BARS_4
                                else -> BeatGridOption.BARS_2
                            }
                            selectedPreset = when {
                                recommendation.presetName.contains("Harmonic", ignoreCase = true) -> MixCurvePreset.HARMONIC_AUTO_BLEND
                                recommendation.presetName.contains("Club", ignoreCase = true) -> MixCurvePreset.BASS_SWAP
                                else -> MixCurvePreset.STUDIO_DEFAULT
                            }
                            customFilterDipX = recommendation.topFilterDipX
                            customFilterDipY = recommendation.topFilterDipY
                            customBottomSweepEndX = recommendation.bottomFilterEndX
                            customVolumeSlope = recommendation.volumeSlope
                            isBpmLockEnabled = true

                            val newDur = selectedBeatGrid.durationSecondsForBpm(outTrack.bpm)
                            playheadProgress = 0.0f
                            isPreviewActive = true
                            onStartTransitionPreview(
                                outTrack.song,
                                inTrack?.song,
                                newDur.toFloat(),
                                0f,
                                currentTransitionEntity
                            )
                            statusMessage = recommendation.explanation
                        }
                    },
                    onSaveClick = { showSavePlaylistSheet = true },
                    onInfoClick = { showInfoSheet = true }
                )

                // Real-time Action / Telemetry Toast Pill
                AnimatedVisibility(
                    visible = statusMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .widthIn(max = 680.dp),
                        color = DeepPurpleSurface,
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PlayheadSpotifyGreen.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = PlayheadSpotifyGreen,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = statusMessage.orEmpty(),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (isWideTablet) 12.dp else 8.dp))

                // 2. Main Spotify Mix Studio Container
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = if (isWideTablet) 920.dp else 640.dp)
                        .background(StudioPanelBg)
                        .padding(vertical = 14.dp)
                ) {
                    // TOP HEADER: Outgoing Track (User's Selected Track 1)
                    SpotifyMixTrackHeader(
                        analyzedTrack = outgoingAnalyzed,
                        emptyPlaceholderTitle = "Select Outgoing Track",
                        emptyPlaceholderSub = "Tap to choose the first song from your library",
                        onClick = {
                            pickerSearchQuery = ""
                            trackPickerSlot = 1
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("mix_outgoing_header")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // CENTER: Dual Stacked Waveform & Spotify Automix Visualizer Engine
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isWideTablet) 400.dp else 350.dp)
                            .testTag("mix_dual_waveform_container"),
                        contentAlignment = Alignment.Center
                    ) {
                        SpotifyMixDualWaveformCanvas(
                            outgoingTrack = outgoingAnalyzed,
                            incomingTrack = incomingAnalyzed,
                            beatGrid = selectedBeatGrid,
                            playheadProgress = playheadProgress,
                            topFilterDipX = customFilterDipX,
                            topFilterDipY = customFilterDipY,
                            bottomFilterEndX = customBottomSweepEndX,
                            volumeSlope = customVolumeSlope,
                            isCurveEditMode = isCurveEditMode,
                            onScrubProgress = { newProgress ->
                                val clamped = newProgress.coerceIn(0f, 1f)
                                playheadProgress = clamped
                                liveOutgoingSong?.let { outSong ->
                                    onSeekTransitionPreview(outSong, transitionDurationSec.toFloat(), clamped)
                                }
                            },
                            onAdjustFilterDip = { newDipX, newDipY ->
                                customFilterDipX = newDipX
                                customFilterDipY = newDipY
                                val eqCutMb = -((customFilterDipY - 0.2f) * 900f).toInt().coerceIn(0, 800)
                                onApplyBandLevel(0, eqCutMb)
                            },
                            onAdjustBottomSweep = { newEndX ->
                                customBottomSweepEndX = newEndX
                            },
                            onAdjustVolumeSlope = { newSlope ->
                                customVolumeSlope = newSlope
                            },
                            onSnapBeatGrid = { snapped ->
                                selectedBeatGrid = snapped
                                val refBpm = outgoingAnalyzed?.bpm ?: 120
                                val secs = snapped.durationSecondsForBpm(refBpm)
                                onCrossfadeChange(secs.toFloat())
                                if (isPreviewActive && liveOutgoingSong != null) {
                                    onStartTransitionPreview(
                                        liveOutgoingSong,
                                        liveIncomingSong,
                                        secs.toFloat(),
                                        playheadProgress,
                                        currentTransitionEntity
                                    )
                                }
                            },
                            onDspTelemetryChange = { label ->
                                activeDspHudLabel = label
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Floating DSP Real-Time Telemetry HUD Pill
                        if (activeDspHudLabel != null) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF1E1F24).copy(alpha = 0.94f),
                                border = BorderStroke(1.dp, PlayheadSpotifyGreen.copy(alpha = 0.6f)),
                                shadowElevation = 8.dp,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = PlayheadSpotifyGreen,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = activeDspHudLabel.orEmpty(),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Center Overlay Play/Stop Preview Pill Button
                        Box(
                            modifier = Modifier
                                .size(width = 56.dp, height = 38.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF4F5056).copy(alpha = 0.92f))
                                .clickable { triggerPreviewToggle() }
                                .testTag("mix_center_preview_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isPreviewActive) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color.White)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Preview Transition",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Spotify Beat Grid Selector Pill ("16 bars v") at Bottom Center of Waveform Box
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .offset(y = 6.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { showBeatGridMenu = true }
                                    .testTag("mix_beat_grid_selector"),
                                color = Color(0xFF37383E),
                                shape = RoundedCornerShape(20.dp),
                                tonalElevation = 4.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = selectedBeatGrid.label,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Select Beat Grid Bars",
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showBeatGridMenu,
                                onDismissRequest = { showBeatGridMenu = false },
                                modifier = Modifier.background(Color(0xFF2A2B30))
                            ) {
                                val refBpm = outgoingAnalyzed?.bpm ?: 120
                                BeatGridOption.values().forEach { option ->
                                    val secs = option.durationSecondsForBpm(refBpm)
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = option.label,
                                                    color = if (option == selectedBeatGrid) PlayheadSpotifyGreen else Color.White,
                                                    fontWeight = if (option == selectedBeatGrid) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 14.sp
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = "${secs}s",
                                                    color = Color(0xFF9E9FA5),
                                                    fontSize = 12.sp
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedBeatGrid = option
                                            showBeatGridMenu = false
                                            onCrossfadeChange(secs.toFloat())
                                            if (isPreviewActive && liveOutgoingSong != null) {
                                                onStartTransitionPreview(
                                                    liveOutgoingSong,
                                                    liveIncomingSong,
                                                    secs.toFloat(),
                                                    playheadProgress,
                                                    currentTransitionEntity
                                                )
                                            }
                                            statusMessage = "Beat Grid set to ${option.label} (${secs}s crossfade window)"
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // BOTTOM HEADER: Incoming Track (User's Selected Track 2)
                    SpotifyMixTrackHeader(
                        analyzedTrack = incomingAnalyzed,
                        emptyPlaceholderTitle = "Select Incoming Track",
                        emptyPlaceholderSub = "Tap to choose the next song to transition into",
                        onClick = {
                            pickerSearchQuery = ""
                            trackPickerSlot = 2
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("mix_incoming_header")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Interactive Transition Curve Strip & Track Picker Shortcuts
                MixEnvelopePresetStrip(
                    selectedPreset = selectedPreset,
                    onSelectPreset = { preset ->
                        selectedPreset = preset
                        customFilterDipX = preset.topFilterDipX
                        customFilterDipY = preset.topFilterDipY
                        customBottomSweepEndX = preset.bottomFilterEndX
                        customVolumeSlope = preset.volumeSlope
                        statusMessage = "Curve Preset: ${preset.title}"
                    },
                    onPickOutgoing = {
                        pickerSearchQuery = ""
                        trackPickerSlot = 1
                    },
                    onPickIncoming = {
                        pickerSearchQuery = ""
                        trackPickerSlot = 2
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = if (isWideTablet) 920.dp else 640.dp)
                        .padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Bottom Preview Transport Scrubber Pill
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = if (isWideTablet) 720.dp else 560.dp)
                        .padding(horizontal = 24.dp)
                        .height(50.dp),
                    color = Color(0xFF17181B),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { triggerPreviewToggle() },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("mix_bottom_play_pause")
                        ) {
                            Icon(
                                imageVector = if (isPreviewActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPreviewActive) "Pause Preview" else "Play Preview",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(14.dp)
                                .background(Color(0xFF3A3B40))
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = elapsedFormatted,
                            color = Color(0xFF9E9FA5),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Slider(
                            value = playheadProgress,
                            onValueChange = { newValue ->
                                val clamped = newValue.coerceIn(0f, 1f)
                                playheadProgress = clamped
                                liveOutgoingSong?.let { outSong ->
                                    onSeekTransitionPreview(outSong, transitionDurationSec.toFloat(), clamped)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 6.dp)
                                .testTag("mix_transition_scrubber"),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color(0xFF45464C)
                            )
                        )

                        Text(
                            text = totalFormatted,
                            color = Color(0xFF9E9FA5),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(14.dp)
                                .background(Color(0xFF3A3B40))
                        )

                        IconButton(
                            onClick = {
                                isAudioMuted = !isAudioMuted
                                onSetAudioMuted(isAudioMuted)
                                statusMessage = if (isAudioMuted) "Transition Preview Audio Muted" else "Transition Preview Audio Unmuted"
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("mix_mute_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isAudioMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (isAudioMuted) "Unmute Audio" else "Mute Audio",
                                tint = if (isAudioMuted) Color(0xFFEF4444) else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 5. Bottom Studio Action Bar (Every button performs a real action)
                val isCurrentFav = liveOutgoingSong?.isFavorite == true
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = if (isWideTablet) 720.dp else 560.dp)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MixBottomActionItem(
                        icon = Icons.Default.SwapVert,
                        label = "Swap",
                        onClick = {
                            val temp = selectedOutgoingSong
                            selectedOutgoingSong = selectedIncomingSong
                            selectedIncomingSong = temp
                            val newOut = selectedOutgoingSong
                            val newIn = selectedIncomingSong
                            if (newOut != null && isPreviewActive) {
                                onStartTransitionPreview(
                                    newOut,
                                    newIn,
                                    transitionDurationSec.toFloat(),
                                    playheadProgress,
                                    currentTransitionEntity
                                )
                            }
                            statusMessage = "Swapped Outgoing & Incoming Songs"
                        }
                    )
                    MixBottomActionItem(
                        icon = if (isCurrentFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        label = "Favourite",
                        activeColor = if (isCurrentFav) PlayheadSpotifyGreen else null,
                        onClick = {
                            val target = liveOutgoingSong
                            if (target != null) {
                                onToggleFavorite(target)
                                statusMessage = if (!target.isFavorite) {
                                    "Added '${target.title}' to Favourites"
                                } else {
                                    "Removed '${target.title}' from Favourites"
                                }
                            } else {
                                trackPickerSlot = 1
                            }
                        }
                    )
                    MixBottomActionItem(
                        icon = Icons.Default.Edit,
                        label = "Edit",
                        activeColor = if (isCurveEditMode) EnvelopeFilterPurple else null,
                        onClick = {
                            isCurveEditMode = !isCurveEditMode
                            statusMessage = if (isCurveEditMode) {
                                "Curve Sculpting Active: Drag purple/blue nodes on the waveform"
                            } else {
                                "Curve Sculpting Locked: Drag waveform to scrub playhead"
                            }
                        }
                    )
                    MixBottomActionItem(
                        icon = Icons.Default.DeleteOutline,
                        label = "Reset",
                        onClick = {
                            selectedPreset = MixCurvePreset.STUDIO_DEFAULT
                            selectedBeatGrid = BeatGridOption.BARS_16
                            customFilterDipX = MixCurvePreset.STUDIO_DEFAULT.topFilterDipX
                            customFilterDipY = MixCurvePreset.STUDIO_DEFAULT.topFilterDipY
                            customBottomSweepEndX = MixCurvePreset.STUDIO_DEFAULT.bottomFilterEndX
                            customVolumeSlope = MixCurvePreset.STUDIO_DEFAULT.volumeSlope
                            playheadProgress = 0.0f
                            onApplyBandLevel(0, 0)
                            liveOutgoingSong?.let { outSong ->
                                onSeekTransitionPreview(outSong, transitionDurationSec.toFloat(), 0f)
                            }
                            statusMessage = "Reset Transition Curves & Rewound Playhead"
                        }
                    )
                    MixBottomActionItem(
                        icon = Icons.Default.QueueMusic,
                        label = "Apply Mix",
                        activeColor = PlayheadSpotifyGreen,
                        onClick = {
                            val outSong = liveOutgoingSong
                            val inSong = liveIncomingSong
                            if (outSong != null) {
                                onCrossfadeChange(transitionDurationSec.toFloat())
                                val updatedQueue = buildList {
                                    add(outSong)
                                    if (inSong != null && inSong.id != outSong.id) {
                                        add(inSong)
                                    }
                                    allSongs.forEach { s ->
                                        if (s.id != outSong.id && s.id != inSong?.id) {
                                            add(s)
                                        }
                                    }
                                }
                                onPlaySong(outSong, updatedQueue)
                                statusMessage = "Applied ${transitionDurationSec}s Transition to Active Queue"
                            } else {
                                trackPickerSlot = 1
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Searchable Library Song Picker Sheet for Selecting Outgoing or Incoming Song
            if (trackPickerSlot != null) {
                val slot = trackPickerSlot!!
                val filteredSongs = remember(allSongs, pickerSearchQuery) {
                    if (pickerSearchQuery.isBlank()) {
                        allSongs
                    } else {
                        allSongs.filter {
                            it.title.contains(pickerSearchQuery, ignoreCase = true) ||
                                it.artist.contains(pickerSearchQuery, ignoreCase = true) ||
                                it.album.contains(pickerSearchQuery, ignoreCase = true)
                        }
                    }
                }

                ModalBottomSheet(
                    onDismissRequest = { trackPickerSlot = null },
                    containerColor = DeepPurpleSurface,
                    contentColor = TextPrimary
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (slot == 1) "Choose Outgoing Song (Top Deck)" else "Choose Incoming Song (Bottom Deck)",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (slot == 1) {
                                "Select the track that plays first and transitions out"
                            } else {
                                "Select the track that blends in after ${liveOutgoingSong?.title ?: "the outgoing track"}"
                            },
                            color = Color(0xFF9E9FA5),
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = pickerSearchQuery,
                            onValueChange = { pickerSearchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text("Search your songs or artists...", color = Color(0xFF7A7C85), fontSize = 13.sp)
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color(0xFF9E9FA5),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (pickerSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { pickerSearchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            tint = Color(0xFF9E9FA5),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF26272C),
                                unfocusedContainerColor = Color(0xFF26272C),
                                focusedBorderColor = PlayheadSpotifyGreen,
                                unfocusedBorderColor = Color(0xFF34353C),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (filteredSongs.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (allSongs.isEmpty()) {
                                        "No songs found on device yet. Scan your media library from the sidebar first."
                                    } else {
                                        "No songs match \"$pickerSearchQuery\""
                                    },
                                    color = Color(0xFF9E9FA5),
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(340.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(filteredSongs, key = { it.id }) { song ->
                                    val analyzed = remember(song.id, slot) {
                                        TrackAudioAnalyzer.analyze(song, isOutgoing = (slot == 1))
                                    }
                                    val isCurrentlyPicked = if (slot == 1) {
                                        liveOutgoingSong?.id == song.id
                                    } else {
                                        liveIncomingSong?.id == song.id
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isCurrentlyPicked) ElectricViolet.copy(alpha = 0.25f) else DeepPurpleDark
                                            )
                                            .clickable {
                                                if (slot == 1) {
                                                    selectedOutgoingSong = song
                                                    if (isPreviewActive) {
                                                        onStartTransitionPreview(
                                                            song,
                                                            liveIncomingSong,
                                                            transitionDurationSec.toFloat(),
                                                            0f,
                                                            currentTransitionEntity
                                                        )
                                                    }
                                                } else {
                                                    selectedIncomingSong = song
                                                    if (isPreviewActive && liveOutgoingSong != null) {
                                                        onStartTransitionPreview(
                                                            liveOutgoingSong,
                                                            song,
                                                            transitionDurationSec.toFloat(),
                                                            playheadProgress,
                                                            currentTransitionEntity
                                                        )
                                                    }
                                                }
                                                trackPickerSlot = null
                                            }
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AlbumArtContainer(
                                            albumArtUri = song.albumArtUri,
                                            size = 44.dp,
                                            cornerRadius = 6.dp,
                                            contentDescription = song.title
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = song.title,
                                                color = if (isCurrentlyPicked) PlayheadSpotifyGreen else Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = song.artist,
                                                color = Color(0xFF9E9FA5),
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "${analyzed.bpm} BPM",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                CamelotKeyBadge(
                                                    keyText = analyzed.camelotKey,
                                                    badgeColor = analyzed.keyBadgeColor
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = analyzed.durationFormatted,
                                                    color = Color(0xFF9E9FA5),
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            // Harmonic Analysis & Live Transition Info Sheet
            if (showInfoSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showInfoSheet = false },
                    containerColor = Color(0xFF1C1D21),
                    contentColor = Color.White
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Mix Studio • Live Transition Analysis",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (outgoingAnalyzed != null && incomingAnalyzed != null) {
                            Text(
                                text = "Outgoing: ${outgoingAnalyzed.song.title} (${outgoingAnalyzed.bpm} BPM • Key ${outgoingAnalyzed.camelotKey})\n" +
                                    "Incoming: ${incomingAnalyzed.song.title} (${incomingAnalyzed.bpm} BPM • Key ${incomingAnalyzed.camelotKey})\n" +
                                    "Active Window: ${selectedBeatGrid.label} (${transitionDurationSec}s crossfade)",
                                color = Color(0xFFD0D2D8),
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        EnvelopeLegendRow(
                            color = PlayheadSpotifyGreen,
                            title = "Synchronized Playhead (Green Line)",
                            desc = "Tracks real audio transition playback across both waveforms. Drag horizontally to seek."
                        )
                        EnvelopeLegendRow(
                            color = EnvelopeVolumeBlue,
                            title = "Volume Envelope (Blue Lines)",
                            desc = "Gain-fading automation boundaries controlling outgoing attenuation and incoming boost."
                        )
                        EnvelopeLegendRow(
                            color = EnvelopeFilterPurple,
                            title = "High/Low Filter Sweeps (Purple Lines)",
                            desc = "Resonant low-pass & high-pass frequency sweeps. Tap 'Edit' to drag nodes directly on the canvas."
                        )
                        EnvelopeLegendRow(
                            color = CrossfadeBoundsYellow,
                            title = "Crossfade Bounds (Yellow Box)",
                            desc = "Active ${selectedBeatGrid.label} beat-matched audio blending window (${transitionDurationSec}s)."
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                    }
                }
            }

            // Save Transition Profile to Specific Playlist or Global Automix
            if (showSavePlaylistSheet) {
                var selectedPlaylistId by remember { mutableStateOf("global_automix") }
                ModalBottomSheet(
                    onDismissRequest = { showSavePlaylistSheet = false },
                    containerColor = Color(0xFF1C1D21),
                    contentColor = Color.White
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = "Save Transition Profile",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Save this custom DSP transition (equal-power curve, smart EQ sweeps, ${selectedBeatGrid.label}) for persistent playback.",
                            color = Color(0xFF9E9FA5),
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Target Destination:",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Option 1: Global Queue Automix
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedPlaylistId == "global_automix") Color(0xFF2E3038) else Color(0xFF16171B))
                                .clickable { selectedPlaylistId = "global_automix" }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.QueueMusic,
                                contentDescription = null,
                                tint = if (selectedPlaylistId == "global_automix") PlayheadSpotifyGreen else Color(0xFF9E9FA5),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Global Queue Automix",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Applies automatically whenever this track pair plays in any queue",
                                    color = Color(0xFF9E9FA5),
                                    fontSize = 11.sp
                                )
                            }
                            if (selectedPlaylistId == "global_automix") {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = PlayheadSpotifyGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Option 2: Custom Playlists
                        playlists.forEach { pl ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedPlaylistId == pl.id) Color(0xFF2E3038) else Color(0xFF16171B))
                                    .clickable { selectedPlaylistId = pl.id }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LibraryMusic,
                                    contentDescription = null,
                                    tint = if (selectedPlaylistId == pl.id) PlayheadSpotifyGreen else Color(0xFF9E9FA5),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pl.name,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Saved Playlist Transition",
                                        color = Color(0xFF9E9FA5),
                                        fontSize = 11.sp
                                    )
                                }
                                if (selectedPlaylistId == pl.id) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = PlayheadSpotifyGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                val outSong = liveOutgoingSong
                                if (outSong != null && currentTransitionEntity != null) {
                                    val targetEntity = currentTransitionEntity.copy(
                                        playlistId = selectedPlaylistId
                                    )
                                    onSavePlaylistTransition(targetEntity)
                                    val destName = if (selectedPlaylistId == "global_automix") "Global Queue Automix" else playlists.firstOrNull { it.id == selectedPlaylistId }?.name ?: "Playlist"
                                    statusMessage = "Transition saved to '$destName'!"
                                    showSavePlaylistSheet = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PlayheadSpotifyGreen,
                                contentColor = Color(0xFF0A140D)
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("mix_confirm_save_transition_button")
                        ) {
                            Text(
                                text = "Save Transition Profile",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MixStudioTopBar(
    outgoingAnalyzed: AnalyzedMixTrack?,
    incomingAnalyzed: AnalyzedMixTrack?,
    harmonicCompatibility: HarmonicCompatibility,
    onBackClick: () -> Unit,
    onAutoBlendClick: () -> Unit,
    onSaveClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    val subTitleText = when {
        outgoingAnalyzed != null && incomingAnalyzed != null ->
            "${outgoingAnalyzed.camelotKey} → ${incomingAnalyzed.camelotKey} • ${outgoingAnalyzed.bpm}/${incomingAnalyzed.bpm} BPM"
        outgoingAnalyzed != null ->
            "${outgoingAnalyzed.camelotKey} • ${outgoingAnalyzed.bpm} BPM"
        else -> "Select two songs below to preview & sculpt transition"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.testTag("mix_studio_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Mix Studio",
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (outgoingAnalyzed != null && incomingAnalyzed != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = harmonicCompatibility.badgeColor.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, harmonicCompatibility.badgeColor.copy(alpha = 0.75f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(harmonicCompatibility.badgeColor)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = harmonicCompatibility.label,
                                    color = harmonicCompatibility.badgeColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                Text(
                    text = subTitleText,
                    color = Color(0xFF9E9FA5),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Auto-Blend Button
            Button(
                onClick = onAutoBlendClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PlayheadSpotifyGreen,
                    contentColor = Color(0xFF0A140D)
                ),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("mix_auto_blend_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Auto-Blend",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            IconButton(
                onClick = onSaveClick,
                modifier = Modifier.size(36.dp).testTag("mix_top_save_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Save Transition Profile",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onInfoClick,
                modifier = Modifier.size(36.dp).testTag("mix_top_info_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Mix Studio Info",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SpotifyMixTrackHeader(
    analyzedTrack: AnalyzedMixTrack?,
    emptyPlaceholderTitle: String,
    emptyPlaceholderSub: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (analyzedTrack != null) {
            AlbumArtContainer(
                albumArtUri = analyzedTrack.song.albumArtUri,
                size = 48.dp,
                cornerRadius = 6.dp,
                contentDescription = analyzedTrack.song.title
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = analyzedTrack.song.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = analyzedTrack.song.artist,
                    color = Color(0xFF9E9FA6),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${analyzedTrack.bpm} BPM",
                    color = Color(0xFFE6E7EC),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CamelotKeyBadge(
                        keyText = analyzedTrack.camelotKey,
                        badgeColor = analyzedTrack.keyBadgeColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = analyzedTrack.durationFormatted,
                        color = Color(0xFF9E9FA6),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        } else {
            // Empty state when user hasn't picked a track yet
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF2B2C32)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = PlayheadSpotifyGreen,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = emptyPlaceholderTitle,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = emptyPlaceholderSub,
                    color = Color(0xFF9E9FA6),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun CamelotKeyBadge(
    keyText: String,
    badgeColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(badgeColor)
            .padding(horizontal = 6.dp, vertical = 1.5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = keyText,
            color = Color(0xFF141518),
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

private enum class WaveformTouchNode {
    NONE,
    TOP_FILTER_DIP,
    BOTTOM_FILTER_SWEEP,
    TOP_VOLUME_KNEE,
    BOTTOM_VOLUME_RISE,
    LEFT_BOUNDARY,
    RIGHT_BOUNDARY,
    PLAYHEAD
}

@Composable
private fun SpotifyMixDualWaveformCanvas(
    outgoingTrack: AnalyzedMixTrack?,
    incomingTrack: AnalyzedMixTrack?,
    beatGrid: BeatGridOption,
    playheadProgress: Float,
    topFilterDipX: Float,
    topFilterDipY: Float,
    bottomFilterEndX: Float,
    volumeSlope: Float,
    isCurveEditMode: Boolean,
    onScrubProgress: (Float) -> Unit,
    onAdjustFilterDip: (Float, Float) -> Unit,
    onAdjustBottomSweep: (Float) -> Unit,
    onAdjustVolumeSlope: (Float) -> Unit,
    onSnapBeatGrid: (BeatGridOption) -> Unit,
    onDspTelemetryChange: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val sampleCount = 220
    val outSeed = outgoingTrack?.seed ?: 101
    val inSeed = incomingTrack?.seed ?: 202

    val outgoingSamples = remember(outSeed) {
        FloatArray(sampleCount) { i ->
            val phase1 = sin(i * 0.37f + outSeed) * 0.45f
            val phase2 = cos(i * 0.83f + outSeed * 0.5f) * 0.30f
            val beatPulse = if (i % 6 == 0) 0.22f else 0.06f
            (0.38f + abs(phase1) + abs(phase2) * 0.5f + beatPulse).coerceIn(0.18f, 0.96f)
        }
    }

    val incomingSamples = remember(inSeed) {
        FloatArray(sampleCount) { i ->
            val phase1 = sin(i * 0.41f + inSeed) * 0.35f
            val phase2 = cos(i * 0.91f + inSeed * 0.3f) * 0.25f
            val buildFactor = if (i > sampleCount * 0.78f) 0.92f else 0.52f
            (0.28f + (abs(phase1) + abs(phase2) * 0.45f) * buildFactor).coerceIn(0.15f, 0.92f)
        }
    }

    val waveformBoxBgColor = StudioWaveformBoxBg
    val gridLineColor = StudioGridLine
    val inactiveWaveColor = StudioInactiveWave
    val filterPurpleColor = EnvelopeFilterPurple
    val playheadAccentColor = PlayheadSpotifyGreen

    var activeNode by remember { mutableStateOf(WaveformTouchNode.NONE) }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val w = size.width.toFloat()
                    val boxLeft = w * 0.135f
                    val boxRight = w * 0.865f
                    val boxW = (boxRight - boxLeft).coerceAtLeast(1f)
                    val normX = ((offset.x - boxLeft) / boxW).coerceIn(0f, 1f)
                    onScrubProgress(normX)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val boxLeft = w * 0.135f
                        val boxRight = w * 0.865f
                        val boxW = (boxRight - boxLeft).coerceAtLeast(1f)
                        val boxTop = h * 0.02f
                        val boxBottom = h * 0.95f
                        val boxH = boxBottom - boxTop
                        val midY = boxTop + boxH * 0.495f
                        val topLaneTop = boxTop + boxH * 0.055f
                        val topCenterY = (topLaneTop + (midY - boxH * 0.015f)) * 0.5f
                        val bottomLaneTop = midY + boxH * 0.025f
                        val bottomCenterY = (bottomLaneTop + (boxBottom - boxH * 0.045f)) * 0.46f

                        val dipX = boxLeft + boxW * topFilterDipX
                        val dipY = topCenterY + (midY - topCenterY) * topFilterDipY
                        val sweepEndX = boxLeft + boxW * bottomFilterEndX
                        val kneeX = boxRight - boxW * volumeSlope
                        val riseX = boxLeft + boxW * volumeSlope
                        val playheadX = boxLeft + boxW * playheadProgress

                        val hitR = 34.dp.toPx()

                        activeNode = when {
                            (offset - Offset(dipX, dipY)).getDistance() < hitR -> WaveformTouchNode.TOP_FILTER_DIP
                            (offset - Offset(sweepEndX, bottomCenterY)).getDistance() < hitR -> WaveformTouchNode.BOTTOM_FILTER_SWEEP
                            (offset - Offset(kneeX, topLaneTop)).getDistance() < hitR -> WaveformTouchNode.TOP_VOLUME_KNEE
                            (offset - Offset(riseX, bottomLaneTop)).getDistance() < hitR -> WaveformTouchNode.BOTTOM_VOLUME_RISE
                            kotlin.math.abs(offset.x - boxLeft) < hitR * 0.7f -> WaveformTouchNode.LEFT_BOUNDARY
                            kotlin.math.abs(offset.x - boxRight) < hitR * 0.7f -> WaveformTouchNode.RIGHT_BOUNDARY
                            kotlin.math.abs(offset.x - playheadX) < hitR * 0.8f -> WaveformTouchNode.PLAYHEAD
                            else -> WaveformTouchNode.PLAYHEAD
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val boxLeft = w * 0.135f
                        val boxRight = w * 0.865f
                        val boxW = (boxRight - boxLeft).coerceAtLeast(1f)
                        val normX = ((change.position.x - boxLeft) / boxW).coerceIn(0f, 1f)
                        val boxTop = h * 0.02f
                        val boxBottom = h * 0.95f
                        val boxH = boxBottom - boxTop
                        val midY = boxTop + boxH * 0.495f
                        val topLaneTop = boxTop + boxH * 0.055f
                        val topCenterY = (topLaneTop + (midY - boxH * 0.015f)) * 0.5f

                        when (activeNode) {
                            WaveformTouchNode.TOP_FILTER_DIP -> {
                                val newDipX = normX.coerceIn(0.06f, 0.65f)
                                val newDipY = ((change.position.y - topCenterY) / (midY - topCenterY).coerceAtLeast(1f)).coerceIn(0.15f, 0.95f)
                                onAdjustFilterDip(newDipX, newDipY)
                                val lpfHz = (20000f - 15000f * newDipY).roundToInt()
                                onDspTelemetryChange("LPF Filter: ${lpfHz} Hz sweep • Outgoing Treble Cut")
                            }
                            WaveformTouchNode.BOTTOM_FILTER_SWEEP -> {
                                val newEndX = normX.coerceIn(0.15f, 0.90f)
                                onAdjustBottomSweep(newEndX)
                                val hpfHz = (280f * (1f - newEndX) + 20f).roundToInt()
                                onDspTelemetryChange("HPF Isolator: < ${hpfHz} Hz roll-off • Incoming Bass Protection")
                            }
                            WaveformTouchNode.TOP_VOLUME_KNEE -> {
                                val slope = (1f - normX).coerceIn(0f, 0.45f)
                                onAdjustVolumeSlope(slope)
                                onDspTelemetryChange("Equal-Power Volume Knee: ${String.format("%.2f", slope)} (0 dB RMS dip)")
                            }
                            WaveformTouchNode.BOTTOM_VOLUME_RISE -> {
                                val slope = normX.coerceIn(0f, 0.45f)
                                onAdjustVolumeSlope(slope)
                                onDspTelemetryChange("Equal-Power Volume Rise: ${String.format("%.2f", slope)} (Constant loudness)")
                            }
                            WaveformTouchNode.LEFT_BOUNDARY, WaveformTouchNode.RIGHT_BOUNDARY -> {
                                val snapped = when {
                                    normX < 0.25f -> BeatGridOption.BARS_16
                                    normX < 0.50f -> BeatGridOption.BARS_8
                                    normX < 0.75f -> BeatGridOption.BARS_4
                                    else -> BeatGridOption.BARS_2
                                }
                                onSnapBeatGrid(snapped)
                                onDspTelemetryChange("Snapped to ${snapped.label}")
                            }
                            WaveformTouchNode.PLAYHEAD -> {
                                onScrubProgress(normX)
                                onDspTelemetryChange("Scrub Playhead: ${(normX * 100).roundToInt()}%")
                            }
                            WaveformTouchNode.NONE -> {}
                        }
                    },
                    onDragEnd = {
                        activeNode = WaveformTouchNode.NONE
                        onDspTelemetryChange(null)
                    },
                    onDragCancel = {
                        activeNode = WaveformTouchNode.NONE
                        onDspTelemetryChange(null)
                    }
                )
            }
    ) {
        val w = size.width
        val h = size.height

        val boxLeft = w * 0.135f
        val boxRight = w * 0.865f
        val boxTop = h * 0.02f
        val boxBottom = h * 0.95f
        val boxW = boxRight - boxLeft
        val boxH = boxBottom - boxTop
        val midY = boxTop + boxH * 0.495f

        // 1. Draw Central Dark Rounded Transition Box
        drawRoundRect(
            color = waveformBoxBgColor,
            topLeft = Offset(boxLeft, boxTop),
            size = Size(boxW, boxH),
            cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx())
        )

        // 2. Draw Vertical Beat Grid Lines inside the Transition Box
        val gridColumns = beatGrid.bars
        for (i in 1 until gridColumns) {
            val gx = boxLeft + (boxW * i / gridColumns.toFloat())
            drawLine(
                color = gridLineColor,
                start = Offset(gx, boxTop + 4.dp.toPx()),
                end = Offset(gx, boxBottom - 4.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )
        }

        val topLaneTop = boxTop + boxH * 0.055f
        val topLaneBottom = midY - boxH * 0.015f
        val topCenterY = (topLaneTop + topLaneBottom) * 0.5f
        val topMaxHalfAmp = (topLaneBottom - topLaneTop) * 0.46f

        val bottomLaneTop = midY + boxH * 0.025f
        val bottomLaneBottom = boxBottom - boxH * 0.045f
        val bottomCenterY = (bottomLaneTop + bottomLaneBottom) * 0.46f
        val bottomMaxHalfAmp = (bottomLaneBottom - bottomLaneTop) * 0.42f

        val stepX = w / sampleCount.toFloat()
        val barWidth = (stepX * 0.85f).coerceAtLeast(2f)

        // 3. Render TOP WAVEFORM (Outgoing Track)
        for (i in 0 until sampleCount) {
            val x = i * stepX + stepX * 0.5f
            val amp = outgoingSamples[i]
            val isActiveRegion = x <= boxRight

            if (!isActiveRegion) {
                val tailHalfH = topMaxHalfAmp * amp * 0.42f
                drawLine(
                    color = inactiveWaveColor,
                    start = Offset(x, topCenterY - tailHalfH),
                    end = Offset(x, topCenterY + tailHalfH),
                    strokeWidth = barWidth,
                    cap = StrokeCap.Round
                )
            } else {
                val lowHalfH = topMaxHalfAmp * amp
                drawLine(
                    color = WaveLowBlue,
                    start = Offset(x, topCenterY - lowHalfH),
                    end = Offset(x, topCenterY + lowHalfH),
                    strokeWidth = barWidth,
                    cap = StrokeCap.Round
                )

                val midHalfH = lowHalfH * 0.58f
                drawLine(
                    color = WaveMidAmber,
                    start = Offset(x, topCenterY - midHalfH),
                    end = Offset(x, topCenterY + midHalfH),
                    strokeWidth = barWidth,
                    cap = StrokeCap.Round
                )

                val highHalfH = lowHalfH * 0.28f
                drawLine(
                    color = WaveHighCream,
                    start = Offset(x, topCenterY - highHalfH),
                    end = Offset(x, topCenterY + highHalfH),
                    strokeWidth = barWidth,
                    cap = StrokeCap.Round
                )
            }
        }

        // 4. Render BOTTOM WAVEFORM (Incoming Track)
        for (i in 0 until sampleCount) {
            val x = i * stepX + stepX * 0.5f
            val amp = incomingSamples[i]
            val isActiveRegion = x >= boxLeft

            if (!isActiveRegion) {
                val preHalfH = bottomMaxHalfAmp * amp * 0.18f
                drawLine(
                    color = inactiveWaveColor.copy(alpha = 0.65f),
                    start = Offset(x, bottomCenterY - preHalfH),
                    end = Offset(x, bottomCenterY + preHalfH),
                    strokeWidth = barWidth,
                    cap = StrokeCap.Round
                )
            } else {
                val isPostTransition = x > boxRight
                val scale = if (isPostTransition) 0.95f else 0.58f
                val lowHalfH = bottomMaxHalfAmp * amp * scale

                drawLine(
                    color = WaveLowBlue,
                    start = Offset(x, bottomCenterY - lowHalfH),
                    end = Offset(x, bottomCenterY + lowHalfH),
                    strokeWidth = barWidth,
                    cap = StrokeCap.Round
                )

                val midHalfH = lowHalfH * 0.62f
                drawLine(
                    color = WaveMidAmber,
                    start = Offset(x, bottomCenterY - midHalfH),
                    end = Offset(x, bottomCenterY + midHalfH),
                    strokeWidth = barWidth,
                    cap = StrokeCap.Round
                )

                val highHalfH = lowHalfH * (if (isPostTransition) 0.25f else 0.16f)
                drawLine(
                    color = if (isPostTransition) WaveHighCream else WaveHighGold,
                    start = Offset(x, bottomCenterY - highHalfH),
                    end = Offset(x, bottomCenterY + highHalfH),
                    strokeWidth = barWidth,
                    cap = StrokeCap.Round
                )
            }
        }

        // Golden horizontal center axis line through incoming waveform
        drawLine(
            color = WaveHighGold.copy(alpha = 0.85f),
            start = Offset(boxLeft, bottomCenterY),
            end = Offset(w, bottomCenterY),
            strokeWidth = 2.dp.toPx()
        )

        val envelopeStroke = 2.2.dp.toPx()

        // 5. Crossfade Bounds (Yellow Box / Boundary Lines)
        drawLine(
            color = CrossfadeBoundsYellow,
            start = Offset(boxLeft, topCenterY),
            end = Offset(boxLeft, midY),
            strokeWidth = envelopeStroke
        )
        drawLine(
            color = CrossfadeBoundsYellow,
            start = Offset(boxLeft, midY),
            end = Offset(boxRight, midY),
            strokeWidth = envelopeStroke
        )

        // 6. Volume Envelope (Light Blue Lines)
        val kneeX = boxRight - boxW * volumeSlope
        val topVolPath = Path().apply {
            moveTo(boxLeft, topLaneTop)
            lineTo(kneeX, topLaneTop)
            lineTo(boxRight, if (volumeSlope > 0f) midY else topLaneTop)
            lineTo(boxRight, midY)
        }
        drawPath(
            path = topVolPath,
            color = EnvelopeVolumeBlue,
            style = Stroke(width = envelopeStroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        val riseX = boxLeft + boxW * volumeSlope
        val bottomVolPath = Path().apply {
            moveTo(boxLeft, boxBottom - 10.dp.toPx())
            lineTo(riseX, bottomLaneTop)
            lineTo(boxRight, bottomLaneTop)
        }
        drawPath(
            path = bottomVolPath,
            color = EnvelopeVolumeBlue,
            style = Stroke(width = envelopeStroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // 7. High/Low Filter Sweeps (Purple Lines)
        val dipX = boxLeft + boxW * topFilterDipX
        val dipY = topCenterY + (midY - topCenterY) * topFilterDipY
        val recoveryX = (dipX + boxW * 0.34f).coerceAtMost(boxRight)
        val topFilterPath = Path().apply {
            moveTo(boxLeft, topCenterY)
            lineTo(dipX, dipY)
            lineTo(recoveryX, topCenterY)
            lineTo(boxRight, topCenterY)
        }
        drawPath(
            path = topFilterPath,
            color = filterPurpleColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        val sweepEndX = boxLeft + boxW * bottomFilterEndX
        val bottomFilterPath = Path().apply {
            moveTo(boxLeft, boxBottom - 10.dp.toPx())
            lineTo(sweepEndX, bottomCenterY)
        }
        drawPath(
            path = bottomFilterPath,
            color = filterPurpleColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Show draggable control handles when Curve Edit Mode is toggled ON
        if (isCurveEditMode) {
            drawCircle(
                color = filterPurpleColor,
                radius = 7.dp.toPx(),
                center = Offset(dipX, dipY)
            )
            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = Offset(dipX, dipY)
            )
            drawCircle(
                color = filterPurpleColor,
                radius = 7.dp.toPx(),
                center = Offset(sweepEndX, bottomCenterY)
            )
            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = Offset(sweepEndX, bottomCenterY)
            )
        }

        // 8. Synchronized Playhead (Themed Accent Line spanning both stacked waveforms)
        val playheadX = (boxLeft + boxW * playheadProgress.coerceIn(0f, 1f))
            .coerceIn(boxLeft + 4.dp.toPx(), boxRight - 4.dp.toPx())

        drawLine(
            color = playheadAccentColor.copy(alpha = 0.28f),
            start = Offset(playheadX, boxTop + 2.dp.toPx()),
            end = Offset(playheadX, boxBottom - 2.dp.toPx()),
            strokeWidth = 7.dp.toPx(),
            cap = StrokeCap.Round
        )

        drawLine(
            color = playheadAccentColor,
            start = Offset(playheadX, boxTop + 2.dp.toPx()),
            end = Offset(playheadX, boxBottom - 2.dp.toPx()),
            strokeWidth = 2.8.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun MixEnvelopePresetStrip(
    selectedPreset: MixCurvePreset,
    onSelectPreset: (MixCurvePreset) -> Unit,
    onPickOutgoing: () -> Unit,
    onPickIncoming: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(color = EnvelopeVolumeBlue, label = "Volume")
                LegendDot(color = EnvelopeFilterPurple, label = "EQ Filter")
                LegendDot(color = CrossfadeBoundsYellow, label = "Crossfade")
                LegendDot(color = PlayheadSpotifyGreen, label = "Playhead")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onPickOutgoing() },
                    color = Color(0xFF25262C),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LibraryMusic,
                            contentDescription = "Change Track 1",
                            tint = CamelotWarmOrange,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Track 1",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onPickIncoming() },
                    color = Color(0xFF25262C),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LibraryMusic,
                            contentDescription = "Change Track 2",
                            tint = CamelotVibrantGreen,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Track 2",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(MixCurvePreset.values()) { preset ->
                val isSelected = preset == selectedPreset
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onSelectPreset(preset) },
                    color = if (isSelected) Color(0xFF2F323B) else Color(0xFF1A1B1F),
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) EnvelopeVolumeBlue else Color(0xFF2D2E34)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = EnvelopeVolumeBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = preset.title,
                            color = if (isSelected) Color.White else Color(0xFF9E9FA5),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(
    color: Color,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = Color(0xFFA5A7AE),
            fontSize = 11.sp
        )
    }
}

@Composable
private fun MixBottomActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    activeColor: Color? = null,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = activeColor ?: Color.White,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = activeColor ?: Color(0xFFD0D2D8),
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
private fun EnvelopeLegendRow(
    color: Color,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = desc,
                color = Color(0xFF9E9FA5),
                fontSize = 12.sp
            )
        }
    }
}
