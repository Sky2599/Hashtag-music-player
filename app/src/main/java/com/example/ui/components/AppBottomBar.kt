package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DeepPurpleDark
import com.example.ui.theme.ElectricVioletLight
import com.example.ui.theme.LocalAppTheme
import com.example.ui.theme.TextTertiary
import com.example.ui.util.instantTapDown
import kotlin.math.cos
import kotlin.math.sin

data class NavItem(
    val title: String,
    val icon: ImageVector,
    val tag: String
)

/**
 * Bottom Navigation Bar:
 * - Liquid Glass Theme support:
 *   1. Reflective pill that moves to the currently open tab on the bottom tab bar with
 *      spring physics and an animated specular reflection sweep.
 *   2. An ambient liquid glass pill that smoothly glides around across the bar using
 *      GPU-accelerated graphicsLayer transformations for maximum efficiency.
 *   3. 5 ergonomically placed tabs: Home (0), Albums (1), Playing (2), Artists (3), Songs (4).
 */
@Composable
fun AppBottomBar(
    currentTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    val isLiquidGlass = theme.toneProfileLabel.contains("Liquid Glass", ignoreCase = true)

    val items = listOf(
        NavItem("Home", Icons.Default.Home, "nav_home"),
        NavItem("Albums", Icons.Default.Album, "nav_albums"),
        NavItem("Playing", Icons.Default.PlayCircleFilled, "nav_now_playing"),
        NavItem("Artists", Icons.Default.Person, "nav_artists"),
        NavItem("Songs", Icons.AutoMirrored.Filled.QueueMusic, "nav_songs")
    )

    // Infinite transition for fluid specular reflections and ambient moving pill
    val infiniteTransition = rememberInfiniteTransition(label = "bottom_bar_fluid_anims")

    // 1. Ambient floating pill smooth phase (GPU-accelerated lissajous motion for peak efficiency)
    val floatPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ambient_pill_phase"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isLiquidGlass) {
                    Brush.verticalGradient(
                        listOf(
                            Color(0xDD090F1E),
                            Color(0xF5050811)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            theme.backgroundDark.copy(alpha = 0.95f),
                            theme.backgroundDark
                        )
                    )
                }
            )
            .navigationBarsPadding()
    ) {
        // Specular top rim line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    if (isLiquidGlass) {
                        Brush.horizontalGradient(
                            listOf(
                                Color(0x1500F0FF),
                                Color(0x6000F0FF),
                                Color(0xCCFFFFFF),
                                Color(0x6000F0FF),
                                Color(0x1500F0FF)
                            )
                        )
                    } else {
                        Brush.horizontalGradient(
                            listOf(
                                theme.divider.copy(alpha = 0.2f),
                                theme.primary.copy(alpha = 0.6f),
                                theme.divider.copy(alpha = 0.2f)
                            )
                        )
                    }
                )
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            val totalWidth = maxWidth
            val tabCount = items.size
            val tabWidth = totalWidth / tabCount
            val pillWidth = (tabWidth - 8.dp).coerceIn(48.dp, 68.dp)
            val pillHeight = 50.dp

            // Active Tab Pill Target Position
            val targetPillX = tabWidth * currentTab + (tabWidth - pillWidth) / 2
            val animatedPillX by animateDpAsState(
                targetValue = targetPillX,
                animationSpec = spring(
                    dampingRatio = 0.78f,
                    stiffness = 380f
                ),
                label = "active_pill_x"
            )

            // -------------------------------------------------------------
            // LAYER 1: Ambient Floating Liquid Glass Pill
            // "also add a pill that moves around in a smooth animation in about 25fps or the most efficient one"
            // Uses graphicsLayer translation & rotation purely on the GPU for peak performance
            // -------------------------------------------------------------
            val ambientPillWidth = 38.dp
            val ambientPillHeight = 18.dp
            val amplitudeX = (totalWidth.value - 60f) * 0.44f
            val centerX = (totalWidth.value - ambientPillWidth.value) / 2f
            val offsetX = centerX + amplitudeX * sin(floatPhase)
            val offsetY = 22f + 14f * sin(floatPhase * 2f)
            val rotationDeg = sin(floatPhase) * 14f

            Box(
                modifier = Modifier
                    .size(width = ambientPillWidth, height = ambientPillHeight)
                    .graphicsLayer {
                        translationX = offsetX * density
                        translationY = offsetY * density
                        rotationZ = rotationDeg
                        alpha = if (isLiquidGlass) 0.72f else 0.45f
                    }
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        if (isLiquidGlass) {
                            Brush.linearGradient(
                                listOf(
                                    Color(0x3500F0FF),
                                    Color(0x18FFFFFF),
                                    Color(0x2838BDF8)
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                listOf(
                                    theme.primary.copy(alpha = 0.25f),
                                    theme.primaryLight.copy(alpha = 0.15f)
                                )
                            )
                        }
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color(0x60FFFFFF),
                                if (isLiquidGlass) Color(0x4000F0FF) else theme.primary.copy(alpha = 0.4f),
                                Color(0x75FFFFFF)
                            )
                        ),
                        shape = RoundedCornerShape(9.dp)
                    )
            ) {
                // Internal luminous specular reflection dot
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(Color(0x99FFFFFF))
                )
            }

            // -------------------------------------------------------------
            // LAYER 2: Reflective Pill that moves to the currently open tab
            // "a reflective pill that moves to the currently open tab on the bottom tab bar"
            // -------------------------------------------------------------
            Box(
                modifier = Modifier
                    .offset(x = animatedPillX, y = (64.dp - pillHeight) / 2)
                    .size(width = pillWidth, height = pillHeight)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        if (isLiquidGlass) {
                            Brush.verticalGradient(
                                listOf(
                                    Color(0x3200F0FF),
                                    Color(0x14FFFFFF),
                                    Color(0x240A283C)
                                )
                            )
                        } else {
                            Brush.verticalGradient(
                                listOf(
                                    theme.primary.copy(alpha = 0.28f),
                                    theme.surfaceVariant.copy(alpha = 0.45f)
                                )
                            )
                        }
                    )
                    .border(
                        border = BorderStroke(
                            width = 1.2.dp,
                            brush = if (isLiquidGlass) {
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xCCFFFFFF),
                                        Color(0x6600F0FF),
                                        Color(0x22FFFFFF),
                                        Color(0x99FFFFFF)
                                    )
                                )
                            } else {
                                Brush.linearGradient(
                                    listOf(
                                        theme.primaryLight.copy(alpha = 0.8f),
                                        theme.primary.copy(alpha = 0.4f)
                                    )
                                )
                            }
                        ),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .drawWithContent {
                        drawContent()
                        if (isLiquidGlass) {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x35FFFFFF),
                                        Color.Transparent
                                    ),
                                    startY = 0f,
                                    endY = size.height * 0.45f
                                )
                            )
                        }
                    }
            )

            // -------------------------------------------------------------
            // LAYER 3: Tab Items (Icons + Labels)
            // -------------------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val isSelected = currentTab == index
                    val isCenterPlayTab = index == 2

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .instantTapDown(hapticLightImpact = true) { onTabSelected(index) }
                            .padding(vertical = 4.dp)
                            .testTag(item.tag),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isCenterPlayTab) 34.dp else 28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = when {
                                    isSelected -> if (isLiquidGlass) Color(0xFFFFFFFF) else ElectricVioletLight
                                    isCenterPlayTab -> if (isLiquidGlass) Color(0xFFB0F4FF) else ElectricVioletLight.copy(alpha = 0.85f)
                                    else -> TextTertiary
                                },
                                modifier = Modifier.size(if (isCenterPlayTab) 25.dp else 21.dp)
                            )
                        }

                        Text(
                            text = item.title,
                            color = when {
                                isSelected -> if (isLiquidGlass) Color(0xFFFFFFFF) else ElectricVioletLight
                                else -> TextTertiary
                            },
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
