package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DeepPurpleDark
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElectricVioletLight
import com.example.ui.util.TactileHaptics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * High-performance interactive scrollbar for LazyColumn lists.
 * Utilizes graphicsLayer translation and derivedStateOf to achieve 0 recompositions
 * during high-speed scrolling and flinging.
 */
@Composable
fun LazyColumnFastScrollbar(
    listState: LazyListState,
    itemCount: Int,
    modifier: Modifier = Modifier,
    getSectionText: ((Int) -> String)? = null
) {
    if (itemCount <= 0) return

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val view = LocalView.current
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var containerHeightPx by remember { mutableFloatStateOf(1f) }
    var lastHapticLetter by remember { mutableStateOf("") }

    // Derive scroll progress strictly for GPU draw consumption
    val listScrollProgress by remember(listState, itemCount) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleCount = layoutInfo.visibleItemsInfo.size
            if (itemCount <= visibleCount || visibleCount == 0) {
                0f
            } else {
                val firstIndex = listState.firstVisibleItemIndex
                val maxScrollIndex = (itemCount - visibleCount).coerceAtLeast(1)
                (firstIndex.toFloat() / maxScrollIndex.toFloat()).coerceIn(0f, 1f)
            }
        }
    }

    val isScrollInProgress by remember {
        derivedStateOf { listState.isScrollInProgress }
    }

    val thumbHeightDp = 48.dp
    val thumbHeightPx = with(density) { thumbHeightDp.toPx() }

    val activeLetter by remember(isDragging, dragFraction, itemCount) {
        derivedStateOf {
            if (!isDragging || getSectionText == null || itemCount <= 0) ""
            else {
                val targetIndex = (dragFraction * (itemCount - 1)).toInt().coerceIn(0, itemCount - 1)
                getSectionText(targetIndex)
            }
        }
    }

    Box(
        modifier = modifier
            .width(36.dp)
            .fillMaxHeight()
            .onSizeChanged { containerHeightPx = it.height.toFloat().coerceAtLeast(1f) }
            .testTag("fast_scrollbar")
    ) {
        // Drag and Tap gestures
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(itemCount, containerHeightPx) {
                    detectTapGestures(
                        onPress = { offset ->
                            isDragging = true
                            TactileHaptics.selectionClick(view)
                            val fraction = (offset.y / containerHeightPx).coerceIn(0f, 1f)
                            dragFraction = fraction
                            val targetIndex = (fraction * (itemCount - 1)).toInt().coerceIn(0, (itemCount - 1).coerceAtLeast(0))
                            scope.launch { listState.scrollToItem(targetIndex) }
                            tryAwaitRelease()
                            isDragging = false
                        }
                    )
                }
                .pointerInput(itemCount, containerHeightPx) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            TactileHaptics.selectionClick(view)
                            val fraction = (offset.y / containerHeightPx).coerceIn(0f, 1f)
                            dragFraction = fraction
                            val targetIndex = (fraction * (itemCount - 1)).toInt().coerceIn(0, (itemCount - 1).coerceAtLeast(0))
                            lastHapticLetter = getSectionText?.invoke(targetIndex) ?: ""
                            scope.launch { listState.scrollToItem(targetIndex) }
                        },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            val fraction = (change.position.y / containerHeightPx).coerceIn(0f, 1f)
                            dragFraction = fraction
                            val targetIndex = (fraction * (itemCount - 1)).toInt().coerceIn(0, (itemCount - 1).coerceAtLeast(0))
                            val sec = getSectionText?.invoke(targetIndex) ?: ""
                            if (sec.isNotEmpty() && sec != lastHapticLetter) {
                                lastHapticLetter = sec
                                TactileHaptics.selectionClick(view)
                            }
                            scope.launch { listState.scrollToItem(targetIndex) }
                        }
                    )
                }
        ) {
            // Track background line - fixed dimensions, constant background
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 5.dp)
                    .width(2.dp)
                    .fillMaxHeight()
                    .graphicsLayer {
                        this.alpha = if (isDragging || isScrollInProgress) 0.5f else 0.2f
                    }
                    .background(DeepPurpleDark, RoundedCornerShape(1.dp))
            )

            // Scrollbar Thumb - Rendered with graphicsLayer for 0-recomposition GPU translation & scaling
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 3.dp)
                    .graphicsLayer {
                        val maxThumbTravel = (containerHeightPx - thumbHeightPx).coerceAtLeast(0f)
                        val progress = if (isDragging) dragFraction else listScrollProgress
                        translationY = progress * maxThumbTravel
                        this.alpha = if (isDragging || isScrollInProgress) 1.0f else 0.4f
                        scaleX = if (isDragging) 1.5f else 1.0f
                    }
                    .size(width = 5.dp, height = thumbHeightDp)
                    .clip(CircleShape)
                    .background(if (isDragging) ElectricVioletLight else ElectricViolet)
            )

            // Floating letter popup bubble when dragging
            AnimatedVisibility(
                visible = isDragging && activeLetter.isNotEmpty(),
                enter = fadeIn(tween(100)),
                exit = fadeOut(tween(150)),
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            val maxThumbTravel = (containerHeightPx - thumbHeightPx).coerceAtLeast(0f)
                            val progress = if (isDragging) dragFraction else listScrollProgress
                            val thumbTop = progress * maxThumbTravel
                            val safeTop = (thumbTop - 8.dp.toPx()).coerceIn(
                                0f,
                                (containerHeightPx - 56.dp.toPx()).coerceAtLeast(0f)
                            )
                            translationX = -52.dp.toPx()
                            translationY = safeTop
                        }
                        .size(46.dp)
                        .shadow(6.dp, CircleShape)
                        .clip(CircleShape)
                        .background(ElectricViolet)
                        .testTag("scrollbar_bubble"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = activeLetter,
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * High-performance interactive scrollbar for LazyVerticalGrid.
 * Utilizes graphicsLayer translation and derivedStateOf to achieve 0 recompositions
 * during fast scroll.
 */
@Composable
fun LazyGridFastScrollbar(
    gridState: LazyGridState,
    itemCount: Int,
    modifier: Modifier = Modifier,
    getSectionText: ((Int) -> String)? = null
) {
    if (itemCount <= 0) return

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var containerHeightPx by remember { mutableFloatStateOf(1f) }

    val gridScrollProgress by remember(gridState, itemCount) {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val visibleCount = layoutInfo.visibleItemsInfo.size
            if (itemCount <= visibleCount || visibleCount == 0) {
                0f
            } else {
                val firstIndex = gridState.firstVisibleItemIndex
                val maxScrollIndex = (itemCount - visibleCount).coerceAtLeast(1)
                (firstIndex.toFloat() / maxScrollIndex.toFloat()).coerceIn(0f, 1f)
            }
        }
    }

    val isScrollInProgress by remember {
        derivedStateOf { gridState.isScrollInProgress }
    }

    val thumbHeightDp = 48.dp
    val thumbHeightPx = with(density) { thumbHeightDp.toPx() }

    val activeLetter by remember(isDragging, dragFraction, itemCount) {
        derivedStateOf {
            if (!isDragging || getSectionText == null || itemCount <= 0) ""
            else {
                val targetIndex = (dragFraction * (itemCount - 1)).toInt().coerceIn(0, itemCount - 1)
                getSectionText(targetIndex)
            }
        }
    }

    Box(
        modifier = modifier
            .width(36.dp)
            .fillMaxHeight()
            .onSizeChanged { containerHeightPx = it.height.toFloat().coerceAtLeast(1f) }
            .testTag("fast_grid_scrollbar")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(itemCount, containerHeightPx) {
                    detectTapGestures(
                        onPress = { offset ->
                            isDragging = true
                            val fraction = (offset.y / containerHeightPx).coerceIn(0f, 1f)
                            dragFraction = fraction
                            val targetIndex = (fraction * (itemCount - 1)).toInt().coerceIn(0, (itemCount - 1).coerceAtLeast(0))
                            scope.launch { gridState.scrollToItem(targetIndex) }
                            tryAwaitRelease()
                            isDragging = false
                        }
                    )
                }
                .pointerInput(itemCount, containerHeightPx) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            val fraction = (offset.y / containerHeightPx).coerceIn(0f, 1f)
                            dragFraction = fraction
                            val targetIndex = (fraction * (itemCount - 1)).toInt().coerceIn(0, (itemCount - 1).coerceAtLeast(0))
                            scope.launch { gridState.scrollToItem(targetIndex) }
                        },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            val fraction = (change.position.y / containerHeightPx).coerceIn(0f, 1f)
                            dragFraction = fraction
                            val targetIndex = (fraction * (itemCount - 1)).toInt().coerceIn(0, (itemCount - 1).coerceAtLeast(0))
                            scope.launch { gridState.scrollToItem(targetIndex) }
                        }
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 5.dp)
                    .width(2.dp)
                    .fillMaxHeight()
                    .graphicsLayer {
                        this.alpha = if (isDragging || isScrollInProgress) 0.5f else 0.2f
                    }
                    .background(DeepPurpleDark, RoundedCornerShape(1.dp))
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 3.dp)
                    .graphicsLayer {
                        val maxThumbTravel = (containerHeightPx - thumbHeightPx).coerceAtLeast(0f)
                        val progress = if (isDragging) dragFraction else gridScrollProgress
                        translationY = progress * maxThumbTravel
                        this.alpha = if (isDragging || isScrollInProgress) 1.0f else 0.4f
                        scaleX = if (isDragging) 1.5f else 1.0f
                    }
                    .size(width = 5.dp, height = thumbHeightDp)
                    .clip(CircleShape)
                    .background(if (isDragging) ElectricVioletLight else ElectricViolet)
            )

            AnimatedVisibility(
                visible = isDragging && activeLetter.isNotEmpty(),
                enter = fadeIn(tween(100)),
                exit = fadeOut(tween(150)),
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            val maxThumbTravel = (containerHeightPx - thumbHeightPx).coerceAtLeast(0f)
                            val progress = if (isDragging) dragFraction else gridScrollProgress
                            val thumbTop = progress * maxThumbTravel
                            val safeTop = (thumbTop - 8.dp.toPx()).coerceIn(
                                0f,
                                (containerHeightPx - 56.dp.toPx()).coerceAtLeast(0f)
                            )
                            translationX = -52.dp.toPx()
                            translationY = safeTop
                        }
                        .size(46.dp)
                        .shadow(6.dp, CircleShape)
                        .clip(CircleShape)
                        .background(ElectricViolet)
                        .testTag("grid_scrollbar_bubble"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = activeLetter,
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
