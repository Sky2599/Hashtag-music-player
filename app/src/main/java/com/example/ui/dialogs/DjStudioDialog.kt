package com.example.ui.dialogs

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Song
import com.example.player.DjDeckState
import com.example.player.DjStudioEngine
import com.example.player.DjTab
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// DJ Console Color Tokens matching image_6.png
private val ConsoleDarkBg = Color(0xFF0F1013)
private val ConsolePanelBg = Color(0xFF16171B)
private val ConsoleBevelBorder = Color(0xFF2C2E35)
private val ConsoleInsetDark = Color(0xFF0A0B0D)

// Deck Accents
private val Deck1Cyan = Color(0xFF00D2FF)
private val Deck1CyanDim = Color(0xFF007A99)
private val Deck2Red = Color(0xFFFF2A2A)
private val Deck2RedDim = Color(0xFF991919)
private val SyncAmber = Color(0xFFFFB300)
private val PlayGreen = Color(0xFF00E676)
private val CueOrange = Color(0xFFFF7043)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DjStudioDialog(
    djEngine: DjStudioEngine,
    allSongs: List<Song>,
    onDismiss: () -> Unit
) {
    val view = LocalView.current
    val context = LocalContext.current
    val activity = context as? Activity

    // 1. Keep Screen On (WakeLock) during active DJ session
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = false
        }
    }

    val deck1State by djEngine.deck1State.collectAsState()
    val deck2State by djEngine.deck2State.collectAsState()
    val crossfader by djEngine.crossfader.collectAsState()
    val masterGain by djEngine.masterGain.collectAsState()
    val activeTab by djEngine.activeTab.collectAsState()
    val masterVuLevel by djEngine.masterVuLevel.collectAsState()
    val isAutoFading by djEngine.isAutoFading.collectAsState()
    val queueDisplayInfo by djEngine.queueDisplayInfo.collectAsState()

    var trackPickerDeck by remember { mutableStateOf<Int?>(null) }
    var trackSearchQuery by remember { mutableStateOf("") }
    var hotCueDeleteMode by remember { mutableStateOf(false) }

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
                .background(ConsoleDarkBg)
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag("dj_studio_console")
        ) {
            val isWide = maxWidth >= 680.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Mini Header & Utility Strip (Close, Landscape orientation toggle)
                DjTopUtilityBar(
                    queueDisplayInfo = queueDisplayInfo,
                    onClose = onDismiss,
                    onToggleOrientation = {
                        activity?.let { act ->
                            val current = act.requestedOrientation
                            act.requestedOrientation = if (current == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            } else {
                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            }
                        }
                    }
                )

                // 1. Topmost Waveform Overview & Deck Waveform Boxes (1:1 with blueprint top)
                DjTopWaveformSection(
                    deck1State = deck1State,
                    deck2State = deck2State,
                    onDeck1HeaderClick = {
                        trackSearchQuery = ""
                        trackPickerDeck = 1
                    },
                    onDeck2HeaderClick = {
                        trackSearchQuery = ""
                        trackPickerDeck = 2
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 2. Middle Main Console Area: Deck 1 (Platter + Pitch) | Center Mixer | Deck 2 (Platter + Cues)
                if (isWide) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        // Left Deck (Deck 1)
                        DjPlatterDeckView(
                            deckState = deck1State,
                            accentColor = Deck1Cyan,
                            isLeftDeck = true,
                            djEngine = djEngine,
                            modifier = Modifier.weight(1.35f)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        // Center Mixer Section
                        DjCenterMixerView(
                            activeTab = activeTab,
                            deck1State = deck1State,
                            deck2State = deck2State,
                            crossfader = crossfader,
                            masterGain = masterGain,
                            masterVuLevel = masterVuLevel,
                            isAutoFading = isAutoFading,
                            djEngine = djEngine,
                            modifier = Modifier.weight(0.95f)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        // Right Deck (Deck 2)
                        DjPlatterDeckView(
                            deckState = deck2State,
                            accentColor = Deck2Red,
                            isLeftDeck = false,
                            djEngine = djEngine,
                            hotCueDeleteMode = hotCueDeleteMode,
                            onToggleHotCueDelete = { hotCueDeleteMode = !hotCueDeleteMode },
                            modifier = Modifier.weight(1.35f)
                        )
                    }
                } else {
                    // Mobile stacked layout with Center Mixer prominent
                    DjCenterMixerView(
                        activeTab = activeTab,
                        deck1State = deck1State,
                        deck2State = deck2State,
                        crossfader = crossfader,
                        masterGain = masterGain,
                        masterVuLevel = masterVuLevel,
                        isAutoFading = isAutoFading,
                        djEngine = djEngine,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        DjPlatterDeckView(
                            deckState = deck1State,
                            accentColor = Deck1Cyan,
                            isLeftDeck = true,
                            djEngine = djEngine,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        DjPlatterDeckView(
                            deckState = deck2State,
                            accentColor = Deck2Red,
                            isLeftDeck = false,
                            djEngine = djEngine,
                            hotCueDeleteMode = hotCueDeleteMode,
                            onToggleHotCueDelete = { hotCueDeleteMode = !hotCueDeleteMode },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Bottom Looping & Beat Jump Section (1:1 with blueprint bottom)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DjLoopSectionView(
                        deckState = deck1State,
                        accentColor = Deck1Cyan,
                        djEngine = djEngine,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    DjLoopSectionView(
                        deckState = deck2State,
                        accentColor = Deck2Red,
                        djEngine = djEngine,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Track Picker Modal
    if (trackPickerDeck != null) {
        val targetDeck = trackPickerDeck!!
        ModalBottomSheet(
            onDismissRequest = { trackPickerDeck = null },
            containerColor = ConsolePanelBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Load Track into Deck $targetDeck",
                    color = if (targetDeck == 1) Deck1Cyan else Deck2Red,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = trackSearchQuery,
                    onValueChange = { trackSearchQuery = it },
                    placeholder = { Text("Search songs or artists...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.LightGray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = if (targetDeck == 1) Deck1Cyan else Deck2Red,
                        unfocusedBorderColor = ConsoleBevelBorder
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                val filtered = remember(allSongs, trackSearchQuery) {
                    if (trackSearchQuery.isBlank()) allSongs
                    else allSongs.filter {
                        it.title.contains(trackSearchQuery, ignoreCase = true) ||
                        it.artist.contains(trackSearchQuery, ignoreCase = true)
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                ) {
                    items(filtered) { song ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    djEngine.loadTrack(targetDeck, song, startPlaying = false)
                                    trackPickerDeck = null
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ConsoleInsetDark),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = if (targetDeck == 1) Deck1Cyan else Deck2Red,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artist,
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = String.format("%02d:%02d", (song.durationMs / 1000) / 60, (song.durationMs / 1000) % 60),
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TOP UTILITY BAR (Close & Screen Orientation Helper)
// -------------------------------------------------------------
@Composable
private fun DjTopUtilityBar(
    queueDisplayInfo: String,
    onClose: () -> Unit,
    onToggleOrientation: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(PlayGreen)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = queueDisplayInfo.ifEmpty { "PRO DJ STUDIO • DUAL-DECK CONSOLE" },
                color = Color.White.copy(alpha = 0.90f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Orientation Toggle
            IconButton(
                onClick = onToggleOrientation,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ScreenRotation,
                    contentDescription = "Rotate Screen",
                    tint = Color.LightGray,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            // Exit Button
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close DJ Studio",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 1. TOP WAVEFORM SECTION (Dual stacked overview + Deck displays)
// -------------------------------------------------------------
@Composable
private fun DjTopWaveformSection(
    deck1State: DjDeckState,
    deck2State: DjDeckState,
    onDeck1HeaderClick: () -> Unit,
    onDeck2HeaderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Dual mini overview waveform strip across very top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(ConsoleInsetDark)
                .border(1.dp, ConsoleBevelBorder, RoundedCornerShape(4.dp))
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // WAVE button
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(44.dp)
                        .background(Color(0xFF2B2D33))
                        .border(1.dp, Color(0xFF3E4048)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "WAVE",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Dual Stacked Overview Waveform Canvas
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    val w = size.width
                    val h = size.height
                    val midY = h / 2f

                    // Draw Deck 1 Overview (Top Half - Blue)
                    val d1Prog = if (deck1State.durationMs > 0L) {
                        (deck1State.currentPositionMs.toFloat() / deck1State.durationMs.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    for (i in 0 until 120) {
                        val x = (i / 120f) * w
                        val amp = ((i * 13) % 40) / 40f * (midY - 2f)
                        drawLine(
                            color = if (x / w <= d1Prog) Deck1Cyan else Deck1CyanDim.copy(alpha = 0.4f),
                            start = Offset(x, midY),
                            end = Offset(x, midY - amp),
                            strokeWidth = 2.dp.toPx()
                        )
                    }

                    // Draw Deck 2 Overview (Bottom Half - Red)
                    val d2Prog = if (deck2State.durationMs > 0L) {
                        (deck2State.currentPositionMs.toFloat() / deck2State.durationMs.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    for (i in 0 until 120) {
                        val x = (i / 120f) * w
                        val amp = ((i * 19) % 40) / 40f * (midY - 2f)
                        drawLine(
                            color = if (x / w <= d2Prog) Deck2Red else Deck2RedDim.copy(alpha = 0.4f),
                            start = Offset(x, midY),
                            end = Offset(x, midY + amp),
                            strokeWidth = 2.dp.toPx()
                        )
                    }

                    // Deck 1 Playhead marker
                    drawLine(
                        color = Color.White,
                        start = Offset(d1Prog * w, 0f),
                        end = Offset(d1Prog * w, midY),
                        strokeWidth = 2.dp.toPx()
                    )

                    // Deck 2 Playhead marker
                    drawLine(
                        color = Color.White,
                        start = Offset(d2Prog * w, midY),
                        end = Offset(d2Prog * w, h),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Side-by-side Deck Waveform Boxes (1:1 with blueprint)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Deck 1 Waveform Box (Left)
            DjDeckWaveformBox(
                deckState = deck1State,
                accentColor = Deck1Cyan,
                onHeaderClick = onDeck1HeaderClick,
                modifier = Modifier.weight(1f)
            )

            // Deck 2 Waveform Box (Right)
            DjDeckWaveformBox(
                deckState = deck2State,
                accentColor = Deck2Red,
                onHeaderClick = onDeck2HeaderClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DjDeckWaveformBox(
    deckState: DjDeckState,
    accentColor: Color,
    onHeaderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val song = deckState.song
    val titleText = song?.title ?: "TAP TO LOAD TRACK"
    val bpmText = String.format("%.1f", deckState.bpm)
    val remMs = (deckState.durationMs - deckState.currentPositionMs).coerceAtLeast(0L)
    val timeFormatted = String.format("%02d:%02d.%d", (remMs / 1000) / 60, (remMs / 1000) % 60, (remMs % 1000) / 100)

    Box(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(ConsoleInsetDark)
            .border(1.dp, ConsoleBevelBorder, RoundedCornerShape(4.dp))
            .clickable { onHeaderClick() }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Info Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = titleText,
                    color = accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = timeFormatted,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = bpmText,
                    color = accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // Real-Time Scrolling Waveform Canvas with Beat Grid Lines
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val w = size.width
                val h = size.height
                val midY = h / 2f
                val centerX = w / 2f

                // Draw background grid lines (beat markers)
                val beatWidthPx = (deckState.bpm / 120f) * 36.dp.toPx()
                val scrollOffset = ((deckState.currentPositionMs % 10000) / 1000f) * beatWidthPx

                var gridX = (centerX - scrollOffset) % beatWidthPx
                while (gridX < w) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.25f),
                        start = Offset(gridX, 0f),
                        end = Offset(gridX, h),
                        strokeWidth = 1.dp.toPx()
                    )
                    gridX += beatWidthPx
                }

                // Draw Waveform Envelope
                val numBars = 70
                val barSpacing = w / numBars
                val playFrac = if (deckState.durationMs > 0L) {
                    deckState.currentPositionMs.toFloat() / deckState.durationMs.toFloat()
                } else 0f

                for (i in 0 until numBars) {
                    val barX = i * barSpacing
                    val sampleIdx = ((playFrac * 100f).toInt() + i - numBars / 2).coerceIn(0, 99)
                    val rawAmp = deckState.waveformPeaks.getOrElse(sampleIdx) { 0.25f }
                    val barH = (rawAmp * (midY - 2f)).coerceIn(3.dp.toPx(), midY)

                    // Dual frequency color bars (outer gradient + inner bright peak)
                    drawLine(
                        color = accentColor.copy(alpha = if (barX <= centerX) 0.95f else 0.55f),
                        start = Offset(barX, midY - barH),
                        end = Offset(barX, midY + barH),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.85f),
                        start = Offset(barX, midY - barH * 0.35f),
                        end = Offset(barX, midY + barH * 0.35f),
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Center Red Playhead Line
                drawLine(
                    color = Color.Red,
                    start = Offset(centerX, 0f),
                    end = Offset(centerX, h),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 2. CENTER MIXER VIEW (Tabs, PFL, VU Meters, Volume Faders, Crossfader)
// -------------------------------------------------------------
@Composable
private fun DjCenterMixerView(
    activeTab: DjTab,
    deck1State: DjDeckState,
    deck2State: DjDeckState,
    crossfader: Float,
    masterGain: Float,
    masterVuLevel: Float,
    isAutoFading: Boolean,
    djEngine: DjStudioEngine,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, ConsoleBevelBorder, RoundedCornerShape(8.dp)),
        color = ConsolePanelBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mode Tabs: AUDIO, VIDEO, WAVES, AMP
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(ConsoleInsetDark),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DjTab.values().forEach { tab ->
                    val isSelected = tab == activeTab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(if (isSelected) Color(0xFF383A42) else Color.Transparent)
                            .clickable { djEngine.setActiveTab(tab) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.name,
                            color = if (isSelected) Color.White else Color.Gray,
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // PFL & DB Level Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Deck 1 PFL
                Row(
                    modifier = Modifier.clickable { djEngine.togglePfl(1) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = "PFL Deck 1",
                        tint = if (deck1State.pflEnabled) Deck1Cyan else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "0.0 db",
                        color = Deck1Cyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Center GAIN Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF282A30))
                        .border(1.dp, Color(0xFF3C3E48), RoundedCornerShape(3.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "GAIN",
                        color = Color.LightGray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Deck 2 PFL
                Row(
                    modifier = Modifier.clickable { djEngine.togglePfl(2) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "0.0 db",
                        color = Deck2Red,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = "PFL Deck 2",
                        tint = if (deck2State.pflEnabled) Deck2Red else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Volume Faders & Center VU Meters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Deck 1 Vertical Fader
                DjVerticalFader(
                    value = deck1State.faderVolume,
                    onValueChange = { djEngine.setDeckVolume(1, it) },
                    accentColor = Deck1Cyan,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Center Stereo LED VU Meters with MIX Button
                DjCenterVuLadderWithMixButton(
                    deck1Level = deck1State.vuLevel,
                    deck2Level = deck2State.vuLevel,
                    isAutoFading = isAutoFading,
                    onMixClick = { djEngine.startAutoFade(bars = 4) },
                    modifier = Modifier.weight(1.2f)
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Deck 2 Vertical Fader
                DjVerticalFader(
                    value = deck2State.faderVolume,
                    onValueChange = { djEngine.setDeckVolume(2, it) },
                    accentColor = Deck2Red,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Center '0' reset button for Crossfader
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF26282E))
                    .border(1.dp, Color(0xFF40424A), CircleShape)
                    .clickable { djEngine.resetCrossfaderToCenter() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "0",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Master Horizontal Crossfader (Equal-Power Gain Curve)
            DjMasterCrossfader(
                value = crossfader,
                onValueChange = { djEngine.setCrossfader(it) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DjVerticalFader(
    value: Float,
    onValueChange: (Float) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(4.dp))
            .background(ConsoleInsetDark)
            .border(1.dp, ConsoleBevelBorder, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val centerX = w / 2f

            // Draw ladder tick marks
            for (i in 0..14) {
                val y = (i / 14f) * (h - 20.dp.toPx()) + 10.dp.toPx()
                drawLine(
                    color = accentColor.copy(alpha = 0.45f),
                    start = Offset(centerX - 8.dp.toPx(), y),
                    end = Offset(centerX + 8.dp.toPx(), y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Draw center travel slot
            drawLine(
                color = Color.Black,
                start = Offset(centerX, 10.dp.toPx()),
                end = Offset(centerX, h - 10.dp.toPx()),
                strokeWidth = 4.dp.toPx()
            )

            // Draw Fader Knob
            val thumbY = (1.0f - value) * (h - 28.dp.toPx()) + 14.dp.toPx()
            drawRoundRect(
                color = Color(0xFF444750),
                topLeft = Offset(centerX - 14.dp.toPx(), thumbY - 10.dp.toPx()),
                size = Size(28.dp.toPx(), 20.dp.toPx()),
                cornerRadius = CornerRadius(3.dp.toPx())
            )
            // Knob center white line
            drawLine(
                color = Color.White,
                start = Offset(centerX - 10.dp.toPx(), thumbY),
                end = Offset(centerX + 10.dp.toPx(), thumbY),
                strokeWidth = 2.dp.toPx()
            )
        }

        // Gesture detector for vertical drag
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val deltaFraction = dragAmount.y / size.height
                        val next = (value - deltaFraction).coerceIn(0.0f, 1.0f)
                        onValueChange(next)
                    }
                }
        )
    }
}

@Composable
private fun DjCenterVuLadderWithMixButton(
    deck1Level: Float,
    deck2Level: Float,
    isAutoFading: Boolean,
    onMixClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(4.dp))
            .background(ConsoleInsetDark)
            .border(1.dp, ConsoleBevelBorder, RoundedCornerShape(4.dp))
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Dual LED VU Meter Bars
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val w = size.width
            val h = size.height
            val barW = w * 0.35f
            val segments = 18
            val segH = (h / segments) - 2.dp.toPx()

            for (i in 0 until segments) {
                val frac = (segments - 1 - i) / segments.toFloat()
                val y = i * (segH + 2.dp.toPx())

                val color = when {
                    frac > 0.85f -> Color.Red
                    frac > 0.65f -> Color(0xFFFFB300)
                    else -> PlayGreen
                }

                // Deck 1 Segment (Left)
                val d1Active = deck1Level >= frac
                drawRoundRect(
                    color = if (d1Active) color else color.copy(alpha = 0.12f),
                    topLeft = Offset(4.dp.toPx(), y),
                    size = Size(barW, segH),
                    cornerRadius = CornerRadius(1.dp.toPx())
                )

                // Deck 2 Segment (Right)
                val d2Active = deck2Level >= frac
                drawRoundRect(
                    color = if (d2Active) color else color.copy(alpha = 0.12f),
                    topLeft = Offset(w - barW - 4.dp.toPx(), y),
                    size = Size(barW, segH),
                    cornerRadius = CornerRadius(1.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Center 'MIX' (Auto Fade) Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(if (isAutoFading) SyncAmber else Color(0xFF32343C))
                .border(1.dp, Color(0xFF4A4D58), RoundedCornerShape(3.dp))
                .clickable { onMixClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isAutoFading) "FADING" else "MIX",
                color = if (isAutoFading) Color.Black else Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun DjMasterCrossfader(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(ConsoleInsetDark)
            .border(1.dp, ConsoleBevelBorder, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val midY = h / 2f

            // Travel slot
            drawLine(
                color = Color.Black,
                start = Offset(14.dp.toPx(), midY),
                end = Offset(w - 14.dp.toPx(), midY),
                strokeWidth = 6.dp.toPx()
            )

            // Center mark
            drawLine(
                color = Color.White.copy(alpha = 0.5f),
                start = Offset(w / 2f, 4.dp.toPx()),
                end = Offset(w / 2f, h - 4.dp.toPx()),
                strokeWidth = 1.5.dp.toPx()
            )

            // Slider Knob
            val knobX = value * (w - 32.dp.toPx()) + 16.dp.toPx()
            drawRoundRect(
                color = Color(0xFF4C4F5A),
                topLeft = Offset(knobX - 12.dp.toPx(), 4.dp.toPx()),
                size = Size(24.dp.toPx(), h - 8.dp.toPx()),
                cornerRadius = CornerRadius(3.dp.toPx())
            )
            // Center notch line
            drawLine(
                color = Color.White,
                start = Offset(knobX, 6.dp.toPx()),
                end = Offset(knobX, h - 6.dp.toPx()),
                strokeWidth = 2.dp.toPx()
            )
        }

        // Gesture detector for horizontal crossfade drag
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val deltaFraction = dragAmount.x / size.width
                        val next = (value + deltaFraction).coerceIn(0.0f, 1.0f)
                        onValueChange(next)
                    }
                }
        )
    }
}

// -------------------------------------------------------------
// 3. PLATTER DECK VIEW (Vinyl Disc + Pitch Slider / Hot Cues + Transport)
// -------------------------------------------------------------
@Composable
private fun DjPlatterDeckView(
    deckState: DjDeckState,
    accentColor: Color,
    isLeftDeck: Boolean,
    djEngine: DjStudioEngine,
    hotCueDeleteMode: Boolean = false,
    onToggleHotCueDelete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, ConsoleBevelBorder, RoundedCornerShape(8.dp)),
        color = ConsolePanelBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main Deck Platter Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // If Deck 2 (Right), Hot Cues Column is placed to the left of the platter
                if (!isLeftDeck) {
                    DjHotCuesColumn(
                        deckState = deckState,
                        hotCueDeleteMode = hotCueDeleteMode,
                        onTriggerCue = { slot ->
                            if (hotCueDeleteMode) djEngine.deleteHotCue(deckState.deckNumber, slot)
                            else djEngine.triggerHotCue(deckState.deckNumber, slot)
                        },
                        onToggleDeleteMode = onToggleHotCueDelete,
                        modifier = Modifier.width(38.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }

                // Interactive Rotating Vinyl Platter with Scratching
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    DjInteractivePlatter(
                        deckState = deckState,
                        accentColor = accentColor,
                        onScratchStart = { djEngine.startScratch(deckState.deckNumber) },
                        onScratchMove = { deltaDeg -> djEngine.scratchScrub(deckState.deckNumber, deltaDeg) },
                        onScratchEnd = { djEngine.endScratch(deckState.deckNumber) },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // If Deck 1 (Left), Pitch Slider is placed to the right of the platter
                if (isLeftDeck) {
                    Spacer(modifier = Modifier.width(6.dp))
                    DjPitchSliderSection(
                        pitchPercent = deckState.pitchPercent,
                        onPitchChange = { djEngine.setPitchPercent(deckState.deckNumber, it) },
                        onNudgeUp = { djEngine.pitchBendNudge(deckState.deckNumber, +1.0f) },
                        onNudgeDown = { djEngine.pitchBendNudge(deckState.deckNumber, -1.0f) },
                        onReset = { djEngine.resetPitch(deckState.deckNumber) },
                        modifier = Modifier.width(42.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Transport Control Buttons (CUE, PLAY/PAUSE, SYNC)
            DjTransportButtonsRow(
                deckState = deckState,
                accentColor = accentColor,
                onCueClick = { djEngine.triggerCue(deckState.deckNumber) },
                onPlayPauseClick = { djEngine.togglePlayPause(deckState.deckNumber) },
                onSyncClick = { djEngine.syncDeck(deckState.deckNumber) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DjInteractivePlatter(
    deckState: DjDeckState,
    accentColor: Color,
    onScratchStart: () -> Unit,
    onScratchMove: (Float) -> Unit,
    onScratchEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var previousTouchAngle by remember { mutableFloatStateOf(0f) }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val angle = (atan2(offset.y - center.y, offset.x - center.x) * (180f / PI.toFloat()))
                        previousTouchAngle = angle
                        onScratchStart()
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val currentAngle = (atan2(change.position.y - center.y, change.position.x - center.x) * (180f / PI.toFloat()))
                        var diff = currentAngle - previousTouchAngle
                        if (diff > 180f) diff -= 360f
                        if (diff < -180f) diff += 360f

                        previousTouchAngle = currentAngle
                        onScratchMove(diff)
                    },
                    onDragEnd = { onScratchEnd() },
                    onDragCancel = { onScratchEnd() }
                )
            }
    ) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)
        val radius = (minOf(w, h) / 2f) - 4.dp.toPx()

        // 1. Outer Metallic Bezel Ring
        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(Color(0xFF3A3D46), Color(0xFF222429), Color(0xFF4A4E5A), Color(0xFF222429), Color(0xFF3A3D46)),
                center = center
            ),
            radius = radius,
            center = center
        )

        // 2. Vinyl Grooves Disc
        val vinylRadius = radius - 8.dp.toPx()
        drawCircle(
            color = Color(0xFF111215),
            radius = vinylRadius,
            center = center
        )

        // Concentric micro-grooves
        for (r in 1..7) {
            val grooveR = vinylRadius * (0.35f + r * 0.085f)
            drawCircle(
                color = Color(0xFF1E2026),
                radius = grooveR,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // 3. Center Spindle Hub
        val hubRadius = vinylRadius * 0.36f
        drawCircle(
            color = Color(0xFF0A0B0E),
            radius = hubRadius,
            center = center
        )
        drawCircle(
            color = accentColor.copy(alpha = 0.8f),
            radius = hubRadius,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )

        // 4. Rotating Vinyl Position Marker & Spindle Label
        val angleRad = (deckState.platterAngleDeg * (PI / 180f)).toFloat()

        // Outer rotating red needle / position line
        val markerStart = Offset(
            center.x + cos(angleRad) * (vinylRadius * 0.72f),
            center.y + sin(angleRad) * (vinylRadius * 0.72f)
        )
        val markerEnd = Offset(
            center.x + cos(angleRad) * (vinylRadius - 2.dp.toPx()),
            center.y + sin(angleRad) * (vinylRadius - 2.dp.toPx())
        )
        drawLine(
            color = Color.Red,
            start = markerStart,
            end = markerEnd,
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Center Spindle Dot
        drawCircle(
            color = Color.White,
            radius = 3.dp.toPx(),
            center = center
        )
    }
}

// -------------------------------------------------------------
// PITCH SLIDER SECTION (with + / - Pitch Bend and 0 Reset)
// -------------------------------------------------------------
@Composable
private fun DjPitchSliderSection(
    pitchPercent: Float,
    onPitchChange: (Float) -> Unit,
    onNudgeUp: () -> Unit,
    onNudgeDown: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(4.dp))
            .background(ConsoleInsetDark)
            .border(1.dp, ConsoleBevelBorder, RoundedCornerShape(4.dp))
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "PITCH",
            color = Color.LightGray,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold
        )

        // Nudge '+' Button
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Color(0xFF2A2B32))
                .clickable { onNudgeUp() },
            contentAlignment = Alignment.Center
        ) {
            Text("+", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        // Center Pitch Fader
        Box(
            modifier = Modifier
                .weight(1f)
                .width(22.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val midX = w / 2f
                val midY = h / 2f

                // Travel slot
                drawLine(
                    color = Color.Black,
                    start = Offset(midX, 4.dp.toPx()),
                    end = Offset(midX, h - 4.dp.toPx()),
                    strokeWidth = 3.dp.toPx()
                )

                // Zero Center Line
                drawLine(
                    color = Color.White.copy(alpha = 0.6f),
                    start = Offset(2.dp.toPx(), midY),
                    end = Offset(w - 2.dp.toPx(), midY),
                    strokeWidth = 1.dp.toPx()
                )

                // Thumb Knob (-16% to +16%)
                val normalized = (pitchPercent / 16f).coerceIn(-1f, 1f)
                val thumbY = midY - (normalized * (midY - 8.dp.toPx()))

                drawRoundRect(
                    color = Color(0xFF4A4D58),
                    topLeft = Offset(2.dp.toPx(), thumbY - 6.dp.toPx()),
                    size = Size(w - 4.dp.toPx(), 12.dp.toPx()),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
                drawLine(
                    color = Color.White,
                    start = Offset(4.dp.toPx(), thumbY),
                    end = Offset(w - 4.dp.toPx(), thumbY),
                    strokeWidth = 1.5.dp.toPx()
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaFraction = dragAmount.y / size.height
                            val next = (pitchPercent - deltaFraction * 32f).coerceIn(-16f, 16f)
                            onPitchChange(next)
                        }
                    }
            )
        }

        // Reset '0' Button
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(Color(0xFF222328))
                .clickable { onReset() },
            contentAlignment = Alignment.Center
        ) {
            Text("0", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }

        // Nudge '-' Button
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Color(0xFF2A2B32))
                .clickable { onNudgeDown() },
            contentAlignment = Alignment.Center
        ) {
            Text("-", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// -------------------------------------------------------------
// HOT CUES COLUMN (Slots 1..4, S = Set, DEL = Delete, WAVE)
// -------------------------------------------------------------
@Composable
private fun DjHotCuesColumn(
    deckState: DjDeckState,
    hotCueDeleteMode: Boolean,
    onTriggerCue: (Int) -> Unit,
    onToggleDeleteMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(4.dp))
            .background(ConsoleInsetDark)
            .border(1.dp, ConsoleBevelBorder, RoundedCornerShape(4.dp))
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "HOT CUES",
            color = Color.LightGray,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold
        )

        // Circular Hot Cue Buttons 1..4
        for (slot in 1..4) {
            val isSet = deckState.hotCues.containsKey(slot)
            val ringColor = if (hotCueDeleteMode && isSet) Color.Red else if (isSet) PlayGreen else Color(0xFF383A42)

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E2026))
                    .border(2.dp, ringColor, CircleShape)
                    .clickable { onTriggerCue(slot) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$slot",
                    color = if (isSet) Color.White else Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // DEL Mode Toggle Button
        Box(
            modifier = Modifier
                .size(width = 30.dp, height = 18.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(if (hotCueDeleteMode) Color.Red else Color(0xFF282A32))
                .clickable { onToggleDeleteMode() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "DEL",
                color = Color.White,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// -------------------------------------------------------------
// TRANSPORT BUTTONS ROW (CUE, PLAY/PAUSE, SYNC)
// -------------------------------------------------------------
@Composable
private fun DjTransportButtonsRow(
    deckState: DjDeckState,
    accentColor: Color,
    onCueClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // CUE Button
        Box(
            modifier = Modifier
                .weight(1f)
                .height(36.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF26282E))
                .border(1.dp, Color(0xFF42454E), RoundedCornerShape(4.dp))
                .clickable { onCueClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "CUE",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        // PLAY / PAUSE Button
        Box(
            modifier = Modifier
                .weight(1.3f)
                .height(36.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (deckState.isPlaying) PlayGreen.copy(alpha = 0.25f) else Color(0xFF26282E))
                .border(1.dp, if (deckState.isPlaying) PlayGreen else Color(0xFF42454E), RoundedCornerShape(4.dp))
                .clickable { onPlayPauseClick() },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (deckState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play Pause",
                    tint = if (deckState.isPlaying) PlayGreen else Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // SYNC Button (with bright glowing amber background)
        Box(
            modifier = Modifier
                .weight(1.1f)
                .height(36.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (deckState.isSyncActive) SyncAmber else Color(0xFF26282E))
                .border(1.dp, if (deckState.isSyncActive) SyncAmber else Color(0xFF42454E), RoundedCornerShape(4.dp))
                .clickable { onSyncClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "SYNC",
                color = if (deckState.isSyncActive) Color.Black else SyncAmber,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

// -------------------------------------------------------------
// 4. BOTTOM LOOP SECTION (ROLL, IN, OUT, 4, /2, x2 + Beat Pads)
// -------------------------------------------------------------
@Composable
private fun DjLoopSectionView(
    deckState: DjDeckState,
    accentColor: Color,
    djEngine: DjStudioEngine,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, ConsoleBevelBorder, RoundedCornerShape(6.dp)),
        color = ConsolePanelBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
        ) {
            // Header
            Text(
                text = "LOOP",
                color = Color.LightGray,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Top Row of Rectangular Pads: ROLL, IN, OUT, 4, /2, x2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DjLoopRectPad(
                    text = "ROLL",
                    isActive = deckState.isRollActive,
                    activeColor = SyncAmber,
                    onClick = { djEngine.toggleRoll(deckState.deckNumber, 1.0f) },
                    modifier = Modifier.weight(1f)
                )
                DjLoopRectPad(
                    text = "IN",
                    isActive = false,
                    activeColor = accentColor,
                    onClick = { djEngine.setLoopIn(deckState.deckNumber) },
                    modifier = Modifier.weight(1f)
                )
                DjLoopRectPad(
                    text = "OUT",
                    isActive = deckState.isLoopActive,
                    activeColor = accentColor,
                    onClick = { djEngine.setLoopOut(deckState.deckNumber) },
                    modifier = Modifier.weight(1f)
                )
                DjLoopRectPad(
                    text = "4",
                    isActive = deckState.isLoopActive && deckState.activeLoopBeats == 4f,
                    activeColor = accentColor,
                    onClick = { djEngine.triggerLoopLength(deckState.deckNumber, 4f) },
                    modifier = Modifier.weight(1f)
                )
                DjLoopRectPad(
                    text = "/2",
                    isActive = false,
                    activeColor = accentColor,
                    onClick = { djEngine.halveLoop(deckState.deckNumber) },
                    modifier = Modifier.weight(1f)
                )
                DjLoopRectPad(
                    text = "x2",
                    isActive = false,
                    activeColor = accentColor,
                    onClick = { djEngine.doubleLoop(deckState.deckNumber) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bottom Row of 8 Circular Beat Jump/Loop Pads: 1/8, 1/4, 1/2, 1, 2, 4, 8, 16
            val loopLengths = listOf(
                "1/8" to 0.125f,
                "1/4" to 0.25f,
                "1/2" to 0.5f,
                "1" to 1.0f,
                "2" to 2.0f,
                "4" to 4.0f,
                "8" to 8.0f,
                "16" to 16.0f
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                loopLengths.forEach { (label, beats) ->
                    val isActive = deckState.isLoopActive && kotlin.math.abs(deckState.activeLoopBeats - beats) < 0.05f
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (isActive) accentColor else Color(0xFF22242B))
                            .border(1.dp, if (isActive) Color.White else Color(0xFF383B44), CircleShape)
                            .clickable { djEngine.triggerLoopLength(deckState.deckNumber, beats) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isActive) Color.Black else Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DjLoopRectPad(
    text: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(26.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(if (isActive) activeColor else Color(0xFF26282F))
            .border(1.dp, if (isActive) Color.White else Color(0xFF3E414A), RoundedCornerShape(3.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isActive) Color.Black else Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
