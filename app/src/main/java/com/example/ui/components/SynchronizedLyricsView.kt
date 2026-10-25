package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lyrics.LyricLine
import com.example.ui.theme.DeepPurple
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElectricVioletLight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SynchronizedLyricsView(
    lyrics: List<LyricLine>,
    currentPositionMs: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Find active lyric index: last line where timeMs <= currentPositionMs
    val activeIndex = remember(lyrics, currentPositionMs) {
        val idx = lyrics.indexOfLast { it.timeMs <= currentPositionMs }
        if (idx == -1 && lyrics.isNotEmpty()) 0 else idx
    }

    // Smoothly scroll to keep active lyric line centered
    LaunchedEffect(activeIndex) {
        if (activeIndex in lyrics.indices) {
            val targetScroll = (activeIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(targetScroll)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(DeepPurple.copy(alpha = 0.85f))
    ) {
        if (lyrics.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "(no lyrics found)",
                    color = TextSecondary,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(top = 100.dp, bottom = 120.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(
                    items = lyrics,
                    key = { index, line -> "${line.timeMs}_$index" },
                    contentType = { _, _ -> "lyric_row" }
                ) { index, line ->
                    val isActive = index == activeIndex
                    val animatedAlpha by animateFloatAsState(
                        targetValue = if (isActive) 1.0f else 0.42f,
                        animationSpec = tween(durationMillis = 250),
                        label = "lyric_alpha"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isActive) Color.White else TextSecondary,
                        animationSpec = tween(durationMillis = 250),
                        label = "lyric_color"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSeekTo(line.timeMs) }
                            .padding(horizontal = 18.dp, vertical = if (isActive) 10.dp else 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = line.text,
                            color = textColor.copy(alpha = animatedAlpha),
                            fontSize = if (isActive) 18.sp else 14.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            lineHeight = if (isActive) 24.sp else 18.sp
                        )
                    }
                }
            }

            // Top and bottom edge gradient fades for fluid visuals
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(bottom = 8.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(DeepPurple, Color.Transparent)
                        )
                    )
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            )
        }
    }
}
