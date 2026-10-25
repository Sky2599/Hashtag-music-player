package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DeepPurpleDark
import com.example.ui.theme.DeepPurpleSurface
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElectricVioletLight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.util.TactileHaptics
import androidx.compose.ui.platform.LocalView

@Composable
fun AppNavigationRail(
    currentTab: Int,
    onTabSelected: (Int) -> Unit,
    onMenuClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val items = listOf(
        NavItem("Home", Icons.Default.Home, "rail_nav_home"),
        NavItem("Albums", Icons.Default.Album, "rail_nav_albums"),
        NavItem("Playing", Icons.Default.PlayCircleFilled, "rail_nav_now_playing"),
        NavItem("Artists", Icons.Default.Person, "rail_nav_artists"),
        NavItem("Songs", Icons.AutoMirrored.Filled.QueueMusic, "rail_nav_songs")
    )

    NavigationRail(
        modifier = modifier
            .fillMaxHeight()
            .statusBarsPadding(),
        containerColor = DeepPurpleDark,
        contentColor = TextPrimary,
        header = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
            ) {
                IconButton(
                    onClick = {
                        TactileHaptics.lightImpact(view)
                        onMenuClick()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DeepPurpleSurface)
                        .testTag("rail_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open Navigation Menu",
                        tint = ElectricVioletLight,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    ) {
        val railScrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(railScrollState)
                .padding(vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = currentTab == index
                NavigationRailItem(
                    selected = isSelected,
                    onClick = {
                        TactileHaptics.lightImpact(view)
                        onTabSelected(index)
                    },
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = item.title,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = TextPrimary,
                        selectedTextColor = ElectricVioletLight,
                        indicatorColor = ElectricViolet,
                        unselectedIconColor = TextTertiary,
                        unselectedTextColor = TextTertiary
                    ),
                    modifier = Modifier
                        .padding(vertical = 1.dp)
                        .testTag(item.tag)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            IconButton(
                onClick = {
                    TactileHaptics.lightImpact(view)
                    onSettingsClick()
                },
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .size(38.dp)
                    .testTag("rail_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
