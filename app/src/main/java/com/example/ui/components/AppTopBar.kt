package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DeepPurple
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.util.TactileHaptics
import androidx.compose.ui.platform.LocalView

@Composable
fun AppTopBar(
    title: String,
    onMenuClick: () -> Unit,
    isScanning: Boolean = false,
    showSearch: Boolean = false,
    onSearchClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val view = LocalView.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeepPurple)
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Menu hamburger button to open sidebar
                IconButton(
                    onClick = {
                        TactileHaptics.lightImpact(view)
                        onMenuClick()
                    },
                    modifier = Modifier.testTag("menu_sidebar_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open Sidebar",
                        tint = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.testTag("screen_title_${title.lowercase()}")
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (trailingContent != null) {
                    trailingContent()
                } else {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(22.dp)
                                .padding(end = 8.dp),
                            color = ElectricViolet,
                            strokeWidth = 2.dp
                        )
                    }

                    if (showSearch && onSearchClick != null) {
                        IconButton(
                            onClick = {
                                TactileHaptics.lightImpact(view)
                                onSearchClick()
                            },
                            modifier = Modifier.testTag("search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search library",
                                tint = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

