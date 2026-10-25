package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Song
import com.example.ui.components.AlbumArtContainer
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElectricVioletLight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.util.TactileHaptics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Display mode for the right-hand side of the Landscape Standby Always-On screen.
 */
enum class LandscapeSidePanel {
    ALBUM_ART,
    UP_NEXT_QUEUE,
    WORLD_CLOCK
}

/**
 * Landscape Standby Album Side:
 *
 * Provides the interactive right-hand side of Landscape Standby (Always-On) mode:
 * - Slide right on album cover: Play / Restart the song (previous/restart)
 * - Swipe left on album cover: Plays next song
 * - Tap album cover: Toggles pause / play
 * - Swipe up on album cover: Shows the songs playing next in a list that stays strictly to the right side
 *   and rises smoothly from the bottom
 * - Swipe down on album cover: Shows a real-time Analogue Clock, which can be swiped horizontally
 *   to display a Digital Clock
 * - At the bottom: Displays the exact real-world time the currently playing queue is expected to end
 */
@Composable
fun LandscapeStandbyAlbumSide(
    currentSong: Song,
    isPlaying: Boolean,
    currentPositionMs: Long,
    queue: List<Song>,
    queueIndex: Int,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onSelectQueueItem: (Song) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    var activePanel by remember { mutableStateOf(LandscapeSidePanel.ALBUM_ART) }
    var gestureFeedbackText by remember { mutableStateOf<String?>(null) }
    var gestureFeedbackIcon by remember { mutableStateOf<ImageVector?>(null) }

    // Real-time world clock ticker (updates every second)
    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMs = System.currentTimeMillis()
            delay(1000L)
        }
    }

    // Expected Queue End Time calculation
    val (queueEndTimeStr, queueDurationLeftStr) = remember(
        queue,
        queueIndex,
        currentPositionMs,
        currentSong.id,
        currentSong.durationMs,
        currentTimeMs / 30000L // Recalculate every ~30 seconds or on track change
    ) {
        val currentRemainingMs = (currentSong.durationMs - currentPositionMs).coerceAtLeast(0L)
        val upcomingRemainingMs = if (queueIndex in queue.indices) {
            queue.drop(queueIndex + 1).sumOf { it.durationMs }
        } else 0L
        val totalRemainingMs = currentRemainingMs + upcomingRemainingMs
        val expectedEndTimestamp = currentTimeMs + totalRemainingMs

        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val endTimeFormatted = timeFormat.format(Date(expectedEndTimestamp))

        val totalMinutes = totalRemainingMs / 60000L
        val hours = totalMinutes / 60L
        val mins = totalMinutes % 60L
        val durationLeft = when {
            hours > 0 -> "${hours}h ${mins}m left"
            mins > 0 -> "${mins}m left"
            else -> "< 1m left"
        }
        endTimeFormatted to durationLeft
    }

    fun showFeedback(text: String, icon: ImageVector) {
        gestureFeedbackText = text
        gestureFeedbackIcon = icon
        coroutineScope.launch {
            delay(1200L)
            if (gestureFeedbackText == text) {
                gestureFeedbackText = null
                gestureFeedbackIcon = null
            }
        }
    }

    BackHandler(enabled = activePanel != LandscapeSidePanel.ALBUM_ART) {
        activePanel = LandscapeSidePanel.ALBUM_ART
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0D0B16)),
        contentAlignment = Alignment.Center
    ) {
        // Base content: Album Art with gesture handling
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            // Album Art with multi-directional touch gesture listener
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.88f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .pointerInput(isPlaying) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val startPos = down.position
                            val startTime = System.currentTimeMillis()
                            var totalDeltaX = 0f
                            var totalDeltaY = 0f

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change == null || !change.pressed) break
                                change.consume() // Consume all pointer drag events so parent pager never intercepts
                                totalDeltaX += change.position.x - change.previousPosition.x
                                totalDeltaY += change.position.y - change.previousPosition.y
                            }

                            val elapsed = System.currentTimeMillis() - startTime
                            val distSq = totalDeltaX * totalDeltaX + totalDeltaY * totalDeltaY
                            val tapThresholdPx = with(density) { 24.dp.toPx() }
                            val swipeThresholdPx = with(density) { 40.dp.toPx() }

                            if (distSq < tapThresholdPx * tapThresholdPx && elapsed < 350L) {
                                // Tap Album Cover -> Pause / Play
                                TactileHaptics.lightImpact(view)
                                onPlayPauseClick()
                                showFeedback(
                                    text = if (isPlaying) "Paused" else "Playing",
                                    icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow
                                )
                            } else if (distSq >= swipeThresholdPx * swipeThresholdPx) {
                                // Swipe gesture detected
                                if (abs(totalDeltaX) > abs(totalDeltaY)) {
                                    if (totalDeltaX > 0) {
                                        // Slide Right -> Play / Restart song
                                        TactileHaptics.lightImpact(view)
                                        onPreviousClick()
                                        showFeedback("Restart / Previous", Icons.Default.SkipPrevious)
                                    } else {
                                        // Swipe Left -> Next song
                                        TactileHaptics.lightImpact(view)
                                        onNextClick()
                                        showFeedback("Next Song", Icons.Default.SkipNext)
                                    }
                                } else {
                                    if (totalDeltaY < 0) {
                                        // Swipe Up -> Rise Up Next queue panel
                                        TactileHaptics.lightImpact(view)
                                        activePanel = LandscapeSidePanel.UP_NEXT_QUEUE
                                    } else {
                                        // Swipe Down -> Show World Clock
                                        TactileHaptics.lightImpact(view)
                                        activePanel = LandscapeSidePanel.WORLD_CLOCK
                                    }
                                }
                            }
                        }
                    }
                    .testTag("fullscreen_visualizer_album_art"),
                contentAlignment = Alignment.Center
            ) {
                AlbumArtContainer(
                    song = currentSong,
                    modifier = Modifier.fillMaxSize()
                )

                // Overlay gradient for subtle depth
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0x66000000))
                            )
                        )
                )

                // Floating transient gesture HUD badge (Appears on tap/swipe)
                AnimatedVisibility(
                    visible = gestureFeedbackText != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xCC131024))
                            .border(1.dp, ElectricVioletLight.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            gestureFeedbackIcon?.let { icon ->
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = ElectricVioletLight,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Text(
                                text = gestureFeedbackText.orEmpty(),
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Quick Gesture Navigation Indicators around Album Cover
            // Top Indicator: Swipe Down for Clock
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x80131024))
                    .clickable {
                        TactileHaptics.lightImpact(view)
                        activePanel = LandscapeSidePanel.WORLD_CLOCK
                    }
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = "Show Clock",
                    tint = ElectricVioletLight,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "Swipe down for Clock",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(14.dp)
                )
            }

            // Bottom Bar: Expected Queue End Time + Swipe Up for Queue Indicator
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x99131024))
                    .clickable {
                        TactileHaptics.lightImpact(view)
                        activePanel = LandscapeSidePanel.UP_NEXT_QUEUE
                    }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    tint = ElectricVioletLight,
                    modifier = Modifier.size(14.dp)
                )
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "Queue end time",
                    tint = ElectricVioletLight,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "Queue ends at $queueEndTimeStr ($queueDurationLeftStr)",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "• Up Next ↑",
                    color = ElectricVioletLight,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // PANEL 1: Up Next Queue Panel (Stays strictly on the right side, rises smoothly from bottom)
        AnimatedVisibility(
            visible = activePanel == LandscapeSidePanel.UP_NEXT_QUEUE,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            UpNextQueueSideView(
                queue = queue,
                queueIndex = queueIndex,
                currentSong = currentSong,
                queueEndTimeStr = queueEndTimeStr,
                queueDurationLeftStr = queueDurationLeftStr,
                onSelectSong = { song ->
                    onSelectQueueItem(song)
                },
                onDismiss = {
                    TactileHaptics.lightImpact(view)
                    activePanel = LandscapeSidePanel.ALBUM_ART
                }
            )
        }

        // PANEL 2: Real-time World Clock Panel (Analog / Digital swappable via horizontal swipe)
        AnimatedVisibility(
            visible = activePanel == LandscapeSidePanel.WORLD_CLOCK,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            WorldClockSideView(
                currentTimeMs = currentTimeMs,
                queueEndTimeStr = queueEndTimeStr,
                queueDurationLeftStr = queueDurationLeftStr,
                onDismiss = {
                    TactileHaptics.lightImpact(view)
                    activePanel = LandscapeSidePanel.ALBUM_ART
                }
            )
        }

        // Top right corner exit "x" sign to exit panel or exit Fullscreen Landscape mode
        IconButton(
            onClick = {
                TactileHaptics.lightImpact(view)
                if (activePanel != LandscapeSidePanel.ALBUM_ART) {
                    activePanel = LandscapeSidePanel.ALBUM_ART
                } else {
                    onClose()
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0x991E192E))
                .testTag("visualizer_fullscreen_exit_button")
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Exit Fullscreen Visualizer",
                tint = TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Up Next Queue panel staying strictly on the right half of landscape mode.
 * Rises from the bottom and displays upcoming queue items with expected end time.
 */
@Composable
private fun UpNextQueueSideView(
    queue: List<Song>,
    queueIndex: Int,
    currentSong: Song,
    queueEndTimeStr: String,
    queueDurationLeftStr: String,
    onSelectSong: (Song) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val listState = rememberLazyListState()

    // Auto-scroll to current playing item when panel opens
    LaunchedEffect(queueIndex) {
        if (queueIndex in queue.indices) {
            listState.animateScrollToItem(queueIndex.coerceAtLeast(0))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0D1C))
            .padding(top = 8.dp, bottom = 6.dp, start = 12.dp, end = 12.dp)
    ) {
        // Drag handle & Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onDismiss() }
                .pointerInput(Unit) {
                    var dragY = 0f
                    detectVerticalDragGestures(
                        onDragStart = { dragY = 0f },
                        onVerticalDrag = { change, dragAmount ->
                            dragY += dragAmount
                            if (dragY > 30f) {
                                change.consume()
                                onDismiss()
                            }
                        }
                    )
                }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF38334E))
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QueueMusic,
                    contentDescription = null,
                    tint = ElectricVioletLight,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "UP NEXT QUEUE",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "(${queue.size} songs)",
                    color = TextTertiary,
                    fontSize = 11.sp
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close queue",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Lazy list of upcoming songs
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(
                items = queue,
                key = { idx, song -> "${song.id}_$idx" },
                contentType = { _, _ -> "queue_item" }
            ) { idx, song ->
                val isCurrent = idx == queueIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isCurrent) ElectricViolet.copy(alpha = 0.28f) else Color(0x331E192E))
                        .border(
                            width = if (isCurrent) 1.dp else 0.dp,
                            color = if (isCurrent) ElectricVioletLight else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            TactileHaptics.lightImpact(view)
                            onSelectSong(song)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${idx + 1}",
                        color = if (isCurrent) ElectricVioletLight else TextTertiary,
                        fontSize = 11.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.width(22.dp)
                    )

                    AlbumArtContainer(
                        song = song,
                        size = 34.dp,
                        cornerRadius = 6.dp
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            color = if (isCurrent) ElectricVioletLight else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.artist,
                            color = TextSecondary,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = song.durationFormatted,
                        color = if (isCurrent) ElectricVioletLight else TextTertiary,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Expected Queue End Time pill at the bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF18152B))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = ElectricVioletLight,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Queue ends at $queueEndTimeStr ($queueDurationLeftStr)",
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * World Clock Panel with switchable Analogue Clock and Digital Clock via horizontal swipe.
 * At the bottom it displays the expected time the current queue will end.
 */
@Composable
private fun WorldClockSideView(
    currentTimeMs: Long,
    queueEndTimeStr: String,
    queueDurationLeftStr: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    // Page 0: Analogue Clock, Page 1: Digital Clock
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })

    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 36.dp.toPx() }
    var totalVerticalDrag by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0A17))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { totalVerticalDrag = 0f },
                    onDragEnd = { totalVerticalDrag = 0f },
                    onDragCancel = { totalVerticalDrag = 0f },
                    onVerticalDrag = { change, dragAmount ->
                        totalVerticalDrag += dragAmount
                        if (totalVerticalDrag < -swipeThresholdPx) {
                            change.consume()
                            TactileHaptics.lightImpact(view)
                            onDismiss()
                        }
                    }
                )
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = ElectricVioletLight,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "WORLD TIME CLOCK",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }

            // Mode Selector Pill (Analogue vs Digital)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E192E))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (pagerState.currentPage == 0) ElectricViolet else Color.Transparent)
                        .clickable {
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                        }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Analog",
                        color = if (pagerState.currentPage == 0) Color.White else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (pagerState.currentPage == 1) ElectricViolet else Color.Transparent)
                        .clickable {
                            coroutineScope.launch { pagerState.animateScrollToPage(1) }
                        }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Digital",
                        color = if (pagerState.currentPage == 1) Color.White else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close clock",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Horizontal Pager: Page 0 = Analogue, Page 1 = Digital
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { totalVerticalDrag = 0f },
                        onDragEnd = { totalVerticalDrag = 0f },
                        onDragCancel = { totalVerticalDrag = 0f },
                        onVerticalDrag = { change, dragAmount ->
                            totalVerticalDrag += dragAmount
                            if (totalVerticalDrag < -swipeThresholdPx) {
                                change.consume()
                                TactileHaptics.lightImpact(view)
                                onDismiss()
                            }
                        }
                    )
                }
        ) { page ->
            if (page == 0) {
                AnalogClockView(currentTimeMs = currentTimeMs)
            } else {
                DigitalClockView(currentTimeMs = currentTimeMs)
            }
        }

        // Swipe Hint Indicator
        Text(
            text = if (pagerState.currentPage == 0) "Swipe left for Digital Clock • Swipe up to return to Album" else "Swipe right for Analog Clock • Swipe up to return to Album",
            color = TextTertiary,
            fontSize = 9.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Bottom Pill: Expected Queue End Time
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF171329))
                .border(1.dp, Color(0xFF2B2544), RoundedCornerShape(10.dp))
                .clickable { onDismiss() }
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = ElectricVioletLight,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Queue ends at $queueEndTimeStr ($queueDurationLeftStr)",
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Beautiful Audiophile Analog Clock Canvas:
 * Features high-precision hour, minute, and second hands, dial ticks,
 * elegant hour markers, and real-time world date badge.
 */
@Composable
private fun AnalogClockView(
    currentTimeMs: Long,
    modifier: Modifier = Modifier
) {
    val calendar = remember(currentTimeMs) {
        Calendar.getInstance().apply { timeInMillis = currentTimeMs }
    }

    val hours = calendar.get(Calendar.HOUR)
    val minutes = calendar.get(Calendar.MINUTE)
    val seconds = calendar.get(Calendar.SECOND)

    val hourAngle = (hours + minutes / 60f) * 30f - 90f
    val minuteAngle = (minutes + seconds / 60f) * 6f - 90f
    val secondAngle = seconds * 6f - 90f

    val dateFormat = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }
    val dateStr = remember(currentTimeMs) { dateFormat.format(Date(currentTimeMs)).uppercase(Locale.getDefault()) }

    val accentColor = ElectricVioletLight
    val primaryTextColor = TextPrimary

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxHeight(0.85f)
                .aspectRatio(1f)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.width / 2f).coerceAtMost(size.height / 2f) - 6.dp.toPx()

            // Outer dial glow and face background
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF1E1A33), Color(0xFF100D1E)),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )

            // Outer metallic bezel rim
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFF3B335E),
                        accentColor.copy(alpha = 0.5f),
                        Color(0xFF25203D),
                        accentColor.copy(alpha = 0.5f),
                        Color(0xFF3B335E)
                    ),
                    center = center
                ),
                radius = radius,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )

            // Inner subtle ring
            drawCircle(
                color = Color(0x334E4575),
                radius = radius * 0.72f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // 60 Tick marks around the dial
            for (i in 0 until 60) {
                val angleRad = (i * 6f - 90f) * (PI.toFloat() / 180f)
                val isHour = i % 5 == 0
                val tickLen = if (isHour) 9.dp.toPx() else 4.dp.toPx()
                val tickWidth = if (isHour) 2.5.dp.toPx() else 1.dp.toPx()
                val tickColor = if (isHour) accentColor else Color(0x669CA3AF)

                val startR = radius - tickLen - 3.dp.toPx()
                val endR = radius - 3.dp.toPx()

                val startOffset = Offset(
                    center.x + startR * cos(angleRad),
                    center.y + startR * sin(angleRad)
                )
                val endOffset = Offset(
                    center.x + endR * cos(angleRad),
                    center.y + endR * sin(angleRad)
                )

                drawLine(
                    color = tickColor,
                    start = startOffset,
                    end = endOffset,
                    strokeWidth = tickWidth,
                    cap = StrokeCap.Round
                )
            }

            // Hour Hand
            val hourRad = hourAngle * (PI.toFloat() / 180f)
            val hourLen = radius * 0.50f
            val hourEnd = Offset(
                center.x + hourLen * cos(hourRad),
                center.y + hourLen * sin(hourRad)
            )
            val hourBack = Offset(
                center.x - 8.dp.toPx() * cos(hourRad),
                center.y - 8.dp.toPx() * sin(hourRad)
            )
            drawLine(
                color = primaryTextColor,
                start = hourBack,
                end = hourEnd,
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Minute Hand
            val minRad = minuteAngle * (PI.toFloat() / 180f)
            val minLen = radius * 0.72f
            val minEnd = Offset(
                center.x + minLen * cos(minRad),
                center.y + minLen * sin(minRad)
            )
            val minBack = Offset(
                center.x - 10.dp.toPx() * cos(minRad),
                center.y - 10.dp.toPx() * sin(minRad)
            )
            drawLine(
                color = Color(0xFFE2E8F0),
                start = minBack,
                end = minEnd,
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Second Hand (Electric Violet needle with tail)
            val secRad = secondAngle * (PI.toFloat() / 180f)
            val secLen = radius * 0.82f
            val secEnd = Offset(
                center.x + secLen * cos(secRad),
                center.y + secLen * sin(secRad)
            )
            val secTail = Offset(
                center.x - 14.dp.toPx() * cos(secRad),
                center.y - 14.dp.toPx() * sin(secRad)
            )
            drawLine(
                color = accentColor,
                start = secTail,
                end = secEnd,
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Center Pin Jewel
            drawCircle(
                color = accentColor,
                radius = 5.dp.toPx(),
                center = center
            )
            drawCircle(
                color = Color.White,
                radius = 2.dp.toPx(),
                center = center
            )
        }

        // Live Date Badge placed cleanly beneath center
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 70.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x99171329))
                .border(1.dp, Color(0x338B5CF6), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = dateStr,
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

/**
 * Modern Digital Clock Display:
 * Large luminous digits, seconds readout, full date and timezone.
 */
@Composable
private fun DigitalClockView(
    currentTimeMs: Long,
    modifier: Modifier = Modifier
) {
    val timeFormat = remember { SimpleDateFormat("hh:mm", Locale.getDefault()) }
    val secondsFormat = remember { SimpleDateFormat(":ss", Locale.getDefault()) }
    val amPmFormat = remember { SimpleDateFormat("a", Locale.getDefault()) }
    val fullDateFormat = remember { SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()) }

    val date = remember(currentTimeMs) { Date(currentTimeMs) }
    val timeText = remember(currentTimeMs) { timeFormat.format(date) }
    val secondsText = remember(currentTimeMs) { secondsFormat.format(date) }
    val amPmText = remember(currentTimeMs) { amPmFormat.format(date).uppercase(Locale.getDefault()) }
    val fullDateText = remember(currentTimeMs) { fullDateFormat.format(date) }
    val tzName = remember { TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Large Glowing Digital Time
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = timeText,
                color = TextPrimary,
                fontSize = 54.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1).sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = secondsText,
                color = ElectricVioletLight,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = amPmText,
                color = TextSecondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Full Date Card
        Text(
            text = fullDateText,
            color = TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Timezone Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1E192E))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = "$tzName • REAL WORLD TIME",
                color = TextTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
        }
    }
}
