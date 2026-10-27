package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import com.example.ui.theme.LocalAppTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.ui.util.TactileHaptics
import androidx.compose.ui.platform.LocalView

enum class SidebarItem(
    val title: String,
    val icon: ImageVector,
    val tag: String,
    val subtitle: String? = null
) {
    LIBRARY("Library", Icons.Default.LibraryMusic, "sidebar_library"),
    FOLDERS("Folders", Icons.Default.Folder, "sidebar_folders"),
    EQUALISER("Equaliser & DSP", Icons.Default.GraphicEq, "sidebar_equaliser"),
    LISTENING_STATS("Listening Stats", Icons.Default.BarChart, "sidebar_stats", "Sonic DNA, Rotation & Fidelity Page"),
    SCAN_MEDIA("Scan Media", Icons.Default.Refresh, "sidebar_scan_media"),
    SETTINGS("Settings", Icons.Default.Settings, "sidebar_settings"),
    CHANGELOG("Changelog & Updates", Icons.Default.NewReleases, "sidebar_changelog"),
    ABOUT("About", Icons.Default.Info, "sidebar_about")
}

@Composable
fun AppSidebar(
    isScanning: Boolean,
    currentSong: Song? = null,
    onSelectItem: (SidebarItem) -> Unit,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val theme = LocalAppTheme.current
    val isLiquidGlass = theme.toneProfileLabel.contains("Liquid Glass", ignoreCase = true)

    val infiniteTransition = rememberInfiniteTransition(label = "sidebar_sheen")
    val sheenProgress by infiniteTransition.animateFloat(
        initialValue = -1.2f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sheen_sweep"
    )

    ModalDrawerSheet(
        modifier = modifier
            .width(308.dp)
            .fillMaxHeight(),
        drawerContainerColor = if (isLiquidGlass) Color(0xF6070C18) else DeepPurpleSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header: Glass Pill Now Playing Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        if (isLiquidGlass) {
                            Brush.linearGradient(
                                listOf(
                                    Color(0x3500F0FF),
                                    Color(0x16FFFFFF),
                                    Color(0x280D1D34)
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                listOf(
                                    ElectricViolet.copy(alpha = 0.25f),
                                    DeepPurpleDark.copy(alpha = 0.75f)
                                )
                            )
                        }
                    )
                    .border(
                        width = 1.2.dp,
                        brush = if (isLiquidGlass) {
                            Brush.linearGradient(
                                listOf(
                                    Color(0xCCFFFFFF),
                                    Color(0x6000F0FF),
                                    Color(0x20FFFFFF),
                                    Color(0x99FFFFFF)
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                listOf(
                                    ElectricVioletLight.copy(alpha = 0.6f),
                                    DeepPurpleSurface
                                )
                            )
                        },
                        shape = RoundedCornerShape(24.dp)
                    )
                    .drawWithContent {
                        drawContent()
                        if (isLiquidGlass) {
                            val sweepX = size.width * sheenProgress
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0x18FFFFFF),
                                        Color(0x85FFFFFF),
                                        Color(0x22FFFFFF),
                                        Color.Transparent
                                    ),
                                    start = Offset(sweepX, 0f),
                                    end = Offset(sweepX + size.width * 0.40f, size.height)
                                ),
                                blendMode = BlendMode.Screen
                            )
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentSong != null) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(
                                        width = 1.dp,
                                        color = if (isLiquidGlass) Color(0x7000F0FF) else ElectricViolet.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                            ) {
                                AlbumArtContainer(
                                    albumArtUri = currentSong.albumArtUri,
                                    size = 46.dp,
                                    cornerRadius = 14.dp,
                                    contentDescription = "Now Playing Artwork"
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isLiquidGlass) Color(0x3500F0FF) else ElectricViolet
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isLiquidGlass) Color(0x80FFFFFF) else ElectricVioletLight,
                                        shape = RoundedCornerShape(14.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Headphones,
                                    contentDescription = null,
                                    tint = if (isLiquidGlass) Color.White else TextPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            // Frosted NOW PLAYING label
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isLiquidGlass) Color(0xFF00F0FF) else ElectricVioletLight)
                                )
                                Text(
                                    text = "NOW PLAYING",
                                    color = if (isLiquidGlass) Color(0xFFBAF7FF) else ElectricVioletLight,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = currentSong?.title ?: "No Song Playing",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (currentSong != null) currentSong.artist else "Select a song to play",
                                color = if (isLiquidGlass) Color(0xFFB0C5DB) else ElectricVioletLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onCloseDrawer,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (isLiquidGlass) Color(0x25FFFFFF) else DeepPurpleDark
                            )
                            .border(
                                width = 1.dp,
                                color = if (isLiquidGlass) Color(0x50FFFFFF) else Color.Transparent,
                                shape = CircleShape
                            )
                            .testTag("close_drawer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close drawer",
                            tint = if (isLiquidGlass) Color.White else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                color = if (isLiquidGlass) Color(0x2000F0FF) else DeepPurpleDark,
                thickness = 1.dp
            )

            // Menu Items List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(SidebarItem.values()) { item ->
                    val isScan = item == SidebarItem.SCAN_MEDIA

                    NavigationDrawerItem(
                        icon = {
                            if (isScan && isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = ElectricViolet,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = ElectricVioletLight,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        },
                        label = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        color = TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (item.subtitle != null) {
                                        Text(
                                            text = item.subtitle,
                                            color = ElectricVioletLight.copy(alpha = 0.85f),
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (isScan && isScanning) {
                                    Text(
                                        text = "Scanning...",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        },
                        selected = false,
                        onClick = {
                            TactileHaptics.selectionClick(view)
                            onSelectItem(item)
                            if (!isScan) {
                                onCloseDrawer()
                            }
                        },
                        modifier = Modifier
                            .padding(vertical = 2.dp)
                            .testTag(item.tag),
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Footer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "v3.9.5 • Zero-Latency Tactile & 32-bit Hi-Res DSP",
                    color = TextTertiary,
                    fontSize = 12.sp
                )
            }
        }
    }
}
