package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DeepPurpleDark
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElectricVioletLight
import com.example.ui.theme.TextTertiary
import com.example.ui.util.instantTapDown

data class NavItem(
    val title: String,
    val icon: ImageVector,
    val tag: String
)

/**
 * Bottom Navigation Bar:
 * - Now Playing is placed in the ergonomic central position (Tab 2).
 * - Artists is placed at Tab 3.
 * - Multi-Queues is accessible exclusively from the Side Navigation Drawer.
 * - Uses zero-delay `instantTapDown` touch handlers with hardware haptic feedback.
 */
@Composable
fun AppBottomBar(
    currentTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        NavItem("Home", Icons.Default.Home, "nav_home"),
        NavItem("Albums", Icons.Default.Album, "nav_albums"),
        NavItem("Playing", Icons.Default.PlayCircleFilled, "nav_now_playing"),
        NavItem("Artists", Icons.Default.Person, "nav_artists"),
        NavItem("Songs", Icons.AutoMirrored.Filled.QueueMusic, "nav_songs")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DeepPurpleDark)
            .navigationBarsPadding()
    ) {
        val barScrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .horizontalScroll(barScrollState)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = currentTab == index
                val isCenterPlayTab = index == 2
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .widthIn(min = 64.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .instantTapDown(hapticLightImpact = true) { onTabSelected(index) }
                        .padding(vertical = 4.dp)
                        .testTag(item.tag),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isCenterPlayTab) 38.dp else 32.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                when {
                                    isSelected -> ElectricViolet.copy(alpha = 0.30f)
                                    isCenterPlayTab -> ElectricViolet.copy(alpha = 0.14f)
                                    else -> DeepPurpleDark
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = when {
                                isSelected -> ElectricVioletLight
                                isCenterPlayTab -> ElectricVioletLight.copy(alpha = 0.85f)
                                else -> TextTertiary
                            },
                            modifier = Modifier.size(if (isCenterPlayTab) 25.dp else 21.dp)
                        )
                    }

                    Text(
                        text = item.title,
                        color = if (isSelected) ElectricVioletLight else TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Active indicator bar
                    Box(
                        modifier = Modifier
                            .padding(top = 1.dp)
                            .size(width = 16.dp, height = 2.dp)
                            .background(
                                color = if (isSelected) ElectricVioletLight else DeepPurpleDark,
                                shape = RoundedCornerShape(1.dp)
                            )
                    )
                }
            }
        }
    }
}
