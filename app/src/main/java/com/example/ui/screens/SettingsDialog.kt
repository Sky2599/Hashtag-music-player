package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import com.example.ui.theme.LocalAppTheme
import kotlin.math.sin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettings
import com.example.data.model.Song
import com.example.player.AudioDeviceSpecs
import com.example.ui.dialogs.AudioPipelineDiagnosticDialog
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
import com.example.ui.util.glassPressSheen
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

private data class SettingsCategoryTab(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

/**
 * Full-Screen Tabbed Settings Page:
 * - Replaces the old slide-up sheet with a dedicated full-screen page with smooth swipeable tabs:
 *   1. Appearance & Theme
 *   2. Now Playing & Visualizer
 *   3. Audio & Playback
 *   4. Library & Storage
 * - Every single control on every tab performs a real, verifiable action in the app or audio engine.
 *   No fake or aesthetic-only settings are included.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    isScanning: Boolean = false,
    songsCount: Int = 0,
    albumsCount: Int = 0,
    audioDeviceSpecs: AudioDeviceSpecs? = null,
    currentSong: Song? = null,
    isBitPerfect: Boolean = false,
    onSettingsChanged: (AppSettings) -> Unit,
    onManualRescan: () -> Unit,
    onOpenEqualizer: () -> Unit = {},
    onOpenFolders: () -> Unit = {},
    onResetPlaybackSpeedAndPitch: () -> Unit = {},
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)
    val view = LocalView.current
    var showDiagnosticDialog by remember { mutableStateOf(false) }

    val tabs = listOf(
        SettingsCategoryTab("Appearance", "Themes, AMOLED & Grid", Icons.Default.Palette),
        SettingsCategoryTab("Now Playing", "Visualizer & Tech HUD", Icons.Default.Equalizer),
        SettingsCategoryTab("Audio & DSP", "Crossfade, Gapless & Gain", Icons.Default.Headphones),
        SettingsCategoryTab("Library", "Scanner & Folder Filter", Icons.Default.LibraryMusic)
    )

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    val theme = LocalAppTheme.current
    val isLiquidGlass = theme.toneProfileLabel.contains("Liquid Glass", ignoreCase = true)

    val infiniteTransition = rememberInfiniteTransition(label = "settings_ambient_pill")
    val ambientPillPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ambient_pill_phase"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(if (isLiquidGlass) Color(0xEB0A1324) else DeepPurple)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("settings_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            TactileHaptics.lightImpact(view)
                            onBackClick()
                        },
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isLiquidGlass) Color.White else TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Settings",
                            color = if (isLiquidGlass) Color.White else TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tabs[pagerState.currentPage].subtitle,
                            color = if (isLiquidGlass) Color(0xFF00F0FF) else ElectricVioletLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 2. Liquid Glass Tab Bar with moving reflective pill and ambient floating pill
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isLiquidGlass) {
                            Brush.verticalGradient(
                                listOf(
                                    Color(0x350C1A2E),
                                    Color(0x22060D1A)
                                )
                            )
                        } else {
                            Brush.verticalGradient(
                                listOf(
                                    DeepPurpleDark,
                                    DeepPurpleDark
                                )
                            )
                        }
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color(0x30FFFFFF),
                                if (isLiquidGlass) Color(0x7000F0FF) else theme.primary.copy(alpha = 0.5f),
                                Color(0x30FFFFFF)
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .height(52.dp)
            ) {
                val totalWidth = maxWidth
                val tabCount = tabs.size
                val tabWidth = totalWidth / tabCount
                val pillWidth = tabWidth - 6.dp
                val pillHeight = 42.dp

                // Active Tab Target X position with smooth spring physics
                val targetPillX = tabWidth * pagerState.currentPage + 3.dp
                val animatedPillX by animateDpAsState(
                    targetValue = targetPillX,
                    animationSpec = spring(
                        dampingRatio = 0.8f,
                        stiffness = 380f
                    ),
                    label = "settings_tab_pill_x"
                )

                // 1. Ambient Floating Liquid Glass Pill
                // "in settings add that pill that moves around the tabs as well"
                val ambientPillWidth = 36.dp
                val ambientPillHeight = 16.dp
                val amplitudeX = (totalWidth.value - 50f) * 0.42f
                val centerX = (totalWidth.value - ambientPillWidth.value) / 2f
                val offsetX = centerX + amplitudeX * sin(ambientPillPhase)
                val offsetY = 18f + 10f * sin(ambientPillPhase * 2f)
                val rotationDeg = sin(ambientPillPhase) * 12f

                Box(
                    modifier = Modifier
                        .size(width = ambientPillWidth, height = ambientPillHeight)
                        .graphicsLayer {
                            translationX = offsetX * density
                            translationY = offsetY * density
                            rotationZ = rotationDeg
                            alpha = if (isLiquidGlass) 0.65f else 0.35f
                        }
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0x4000F0FF),
                                    Color(0x25FFFFFF),
                                    Color(0x3038BDF8)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0x60FFFFFF),
                                    Color(0x5000F0FF),
                                    Color(0x70FFFFFF)
                                )
                            ),
                            RoundedCornerShape(8.dp)
                        )
                )

                // 2. Reflective Active Tab Pill
                Box(
                    modifier = Modifier
                        .offset(x = animatedPillX, y = (52.dp - pillHeight) / 2)
                        .size(width = pillWidth, height = pillHeight)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isLiquidGlass) {
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0x4000F0FF),
                                        Color(0x18FFFFFF),
                                        Color(0x280B2233)
                                    )
                                )
                            } else {
                                Brush.verticalGradient(
                                    listOf(
                                        theme.primary.copy(alpha = 0.32f),
                                        theme.surfaceVariant.copy(alpha = 0.45f)
                                    )
                                )
                            }
                        )
                        .border(
                            width = 1.2.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    Color(0x90FFFFFF),
                                    if (isLiquidGlass) Color(0x6000F0FF) else theme.primaryLight.copy(alpha = 0.6f),
                                    Color(0x40FFFFFF)
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .drawWithContent {
                            drawContent()
                            if (isLiquidGlass) {
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        listOf(
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

                // 3. Tab Labels and Icons
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, tab ->
                        val isSelected = pagerState.currentPage == index
                        val tabInteractionSource = remember { MutableInteractionSource() }
                        val isTabPressed by tabInteractionSource.collectIsPressedAsState()

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(16.dp))
                                .glassPressSheen(isLiquidGlass = isLiquidGlass, isPressed = isTabPressed)
                                .clickable(
                                    interactionSource = tabInteractionSource,
                                    indication = null
                                ) {
                                    TactileHaptics.selectionClick(view)
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                }
                                .testTag("settings_tab_$index"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) {
                                        if (isLiquidGlass) Color(0xFF00F0FF) else ElectricVioletLight
                                    } else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = tab.title,
                                    color = if (isSelected) {
                                        if (isLiquidGlass) Color.White else ElectricVioletLight
                                    } else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // 3. Swipeable HorizontalPager for Tab Pages
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { pageIndex ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 700.dp)
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        when (pageIndex) {
                            0 -> {
                                // TAB 0: APPEARANCE & THEME (Oto Music Style)
                                item {
                                    SettingsSectionCard(
                                        title = "Color Theme Engine",
                                        subtitle = "Choose how the app dynamically tints backgrounds, controls, and visualizers"
                                    ) {
                                        val themeOptions = listOf(
                                            Triple(
                                                AppSettings.THEME_LIQUID_GLASS,
                                                "Liquid Glass (Apple Fluid)",
                                                "Translucent frosted glass aesthetics with fluid specular reflections, chromatic glow, and organic physics"
                                            ),
                                            Triple(
                                                AppSettings.THEME_MATERIAL_YOU,
                                                "Material You (Monet System Accent)",
                                                "Uses Android 12+ wallpaper colors across bars, sliders, and visualizers"
                                            ),
                                            Triple(
                                                AppSettings.THEME_DARK_CONTRAST,
                                                "Hashtag theme",
                                                "Signature Hashtag theme: Deep Purple (#1A002C) canvas with vibrant Warm Orange (#FFB700) accents"
                                            ),
                                            Triple(
                                                AppSettings.THEME_ALBUM_ART,
                                                "Dynamic Album Artwork Palette",
                                                "Extracts dominant & vibrant colors from the currently playing track"
                                            ),
                                            Triple(
                                                AppSettings.THEME_CITIVIEW,
                                                "citiview theme",
                                                "Modern metropolitan aesthetic with sleek skyline cyan accents"
                                            )
                                        )

                                        themeOptions.forEach { (engineKey, title, description) ->
                                            val isSelected = settings.themeEngine == engineKey
                                            val cardInteractionSource = remember { MutableInteractionSource() }
                                            val isCardPressed by cardInteractionSource.collectIsPressedAsState()

                                            Card(
                                                shape = RoundedCornerShape(14.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected) {
                                                        if (isLiquidGlass) Color(0x3500F0FF) else ElectricViolet.copy(alpha = 0.20f)
                                                    } else {
                                                        if (isLiquidGlass) Color(0x180B1C2E) else DeepPurpleDark
                                                    }
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .border(
                                                        width = if (isSelected) 1.2.dp else 0.8.dp,
                                                        brush = if (isSelected) {
                                                            if (isLiquidGlass) {
                                                                Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFF00F0FF), Color(0xAAFFFFFF)))
                                                            } else Brush.linearGradient(listOf(ElectricVioletLight, ElectricViolet))
                                                        } else {
                                                            if (isLiquidGlass) Brush.linearGradient(listOf(Color(0x25FFFFFF), Color(0x1800F0FF)))
                                                            else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                                        },
                                                        shape = RoundedCornerShape(14.dp)
                                                    )
                                                    .glassPressSheen(isLiquidGlass = isLiquidGlass, isPressed = isCardPressed)
                                                    .clickable(
                                                        interactionSource = cardInteractionSource,
                                                        indication = null
                                                    ) {
                                                        onSettingsChanged(
                                                            settings.copy(
                                                                themeEngine = engineKey,
                                                                dynamicThemeFromAlbumArt = engineKey == AppSettings.THEME_ALBUM_ART
                                                            )
                                                        )
                                                    }
                                                    .testTag("theme_option_$engineKey")
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                when (engineKey) {
                                                                    AppSettings.THEME_LIQUID_GLASS -> Color(0xFF00F0FF)
                                                                    AppSettings.THEME_MATERIAL_YOU -> Color(0xFF38BDF8)
                                                                    AppSettings.THEME_DARK_CONTRAST -> Color(0xFFFFB700)
                                                                    AppSettings.THEME_CITIVIEW -> Color(0xFF00E5FF)
                                                                    else -> ElectricViolet
                                                                }
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (isSelected) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(10.dp)
                                                                    .clip(CircleShape)
                                                                    .background(Color.White)
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = title,
                                                            color = TextPrimary,
                                                            fontSize = 14.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                                        )
                                                        Text(
                                                            text = description,
                                                            color = TextSecondary,
                                                            fontSize = 11.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                item {
                                    SettingsSectionCard(
                                        title = "Dark Surface Contrast & Layout",
                                        subtitle = "Customize background black levels and library grid density"
                                    ) {
                                        val isPureAmoled = settings.darkThemeMode.equals("Pure AMOLED", ignoreCase = true)
                                        SettingsToggleRow(
                                            title = "Pure AMOLED Pitch Black Mode",
                                            subtitle = "Use #000000 pitch-black background surfaces to save OLED battery",
                                            checked = isPureAmoled,
                                            onCheckedChange = { enabled ->
                                                onSettingsChanged(
                                                    settings.copy(darkThemeMode = if (enabled) "Pure AMOLED" else "Onyx Dark")
                                                )
                                            }
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))
                                        HorizontalDivider(color = DeepPurpleDark)
                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Albums Grid Columns",
                                                    color = TextPrimary,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = "Adjust artwork card size on the Albums tab",
                                                    color = TextSecondary,
                                                    fontSize = 11.sp
                                                )
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                listOf(2, 3).forEach { cols ->
                                                    val selected = settings.albumGridColumns == cols
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .background(if (selected) ElectricViolet else DeepPurpleDark)
                                                            .clickable {
                                                                onSettingsChanged(settings.copy(albumGridColumns = cols))
                                                            }
                                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                                    ) {
                                                        Text(
                                                            text = "$cols Cols",
                                                            color = TextPrimary,
                                                            fontSize = 12.sp,
                                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            1 -> {
                                // TAB 1: NOW PLAYING & VISUALIZER
                                item {
                                    SettingsSectionCard(
                                        title = "Now Playing Layout & Clean Mode",
                                        subtitle = "Configure screen density, album art prominence, and visualizer modules"
                                    ) {
                                        SettingsToggleRow(
                                            title = "Show Visualizer & DSP in Now Playing",
                                            subtitle = "When disabled (recommended), Now Playing features a clean, uncluttered layout with maximum hero album art prominence like Musicolet",
                                            checked = settings.showAudioPipelineInNowPlaying,
                                            onCheckedChange = {
                                                onSettingsChanged(settings.copy(showAudioPipelineInNowPlaying = it))
                                            }
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))
                                        HorizontalDivider(color = DeepPurpleDark)
                                        Spacer(modifier = Modifier.height(12.dp))

                                        SettingsToggleRow(
                                            title = "High-Resolution Spectrum Visualizer",
                                            subtitle = "Enable 21-band reactive FFT frequency spectrum in Now Playing & Equalizer",
                                            checked = settings.highResolutionVisualizer,
                                            onCheckedChange = {
                                                onSettingsChanged(settings.copy(highResolutionVisualizer = it))
                                            }
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))
                                        HorizontalDivider(color = DeepPurpleDark)
                                        Spacer(modifier = Modifier.height(12.dp))

                                        SettingsToggleRow(
                                            title = "Audiophile Technical Specs Badges",
                                            subtitle = "Display FLAC/MP3 codec, bitrate (kbps), and sample rate (kHz) chips on song rows",
                                            checked = settings.showTechnicalChips,
                                            onCheckedChange = {
                                                onSettingsChanged(settings.copy(showTechnicalChips = it))
                                            }
                                        )
                                    }
                                }

                                item {
                                    SettingsSectionCard(
                                        title = "Audio Pipeline & Hardware Diagnostic",
                                        subtitle = "Detailed technical inspector for active DAC, sample rates, AAudio sink, and DSP filters"
                                    ) {
                                        Button(
                                            onClick = {
                                                TactileHaptics.selectionClick(view)
                                                showDiagnosticDialog = true
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.Memory, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("View Audio Pipeline Diagnostic", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            2 -> {
                                // TAB 2: AUDIO & PLAYBACK ENGINE
                                item {
                                    SettingsSectionCard(
                                        title = "Crossfade & Gapless Engine",
                                        subtitle = "Directly controls the dual-player transition engine during track changes"
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Crossfade Duration",
                                                color = TextPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = if (settings.crossfadeDurationSec < 2f) "Off (0s)" else "${settings.crossfadeDurationSec.roundToInt()}s",
                                                color = ElectricVioletLight,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Slider(
                                            value = settings.crossfadeDurationSec,
                                            onValueChange = {
                                                onSettingsChanged(settings.copy(crossfadeDurationSec = it))
                                            },
                                            valueRange = 0f..12f,
                                            steps = 11,
                                            colors = SliderDefaults.colors(
                                                thumbColor = ElectricVioletLight,
                                                activeTrackColor = ElectricViolet
                                            )
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))
                                        HorizontalDivider(color = DeepPurpleDark)
                                        Spacer(modifier = Modifier.height(12.dp))

                                        SettingsToggleRow(
                                            title = "Bit-Perfect Gapless Playback",
                                            subtitle = "Pre-buffers the next track in queue for zero-silence album transitions when crossfade is < 2s",
                                            checked = settings.gaplessPlayback,
                                            onCheckedChange = {
                                                onSettingsChanged(settings.copy(gaplessPlayback = it))
                                            }
                                        )
                                    }
                                }

                                item {
                                    SettingsSectionCard(
                                        title = "Headphone & Bluetooth Disconnect",
                                        subtitle = "Safeguard against accidental audio blasting when devices disconnect"
                                    ) {
                                        SettingsToggleRow(
                                            title = "Pause on Disconnect",
                                            subtitle = "Automatically pause music playback immediately when wired 3.5mm headphones, Bluetooth headphones/speakers, or USB-C audio devices are disconnected",
                                            checked = settings.pauseOnHeadphoneDisconnect,
                                            onCheckedChange = {
                                                onSettingsChanged(settings.copy(pauseOnHeadphoneDisconnect = it))
                                            }
                                        )
                                    }
                                }

                                item {
                                    SettingsSectionCard(
                                        title = "ReplayGain & DSP Mastering",
                                        subtitle = "Real-time volume normalization and hardware Equalizer shortcuts"
                                    ) {
                                        SettingsToggleRow(
                                            title = "ReplayGain Volume Normalization",
                                            subtitle = "Apply EBU R128 loudness headroom normalization across tracks",
                                            checked = settings.replayGain,
                                            onCheckedChange = {
                                                onSettingsChanged(settings.copy(replayGain = it))
                                            }
                                        )

                                        if (settings.replayGain) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("ReplayGain Preamp", color = TextSecondary, fontSize = 12.sp)
                                                Text(
                                                    text = String.format("%+.1f dB", settings.replayGainPreampDb),
                                                    color = ElectricVioletLight,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Slider(
                                                value = settings.replayGainPreampDb,
                                                onValueChange = {
                                                    onSettingsChanged(settings.copy(replayGainPreampDb = it))
                                                },
                                                valueRange = -6f..6f,
                                                colors = SliderDefaults.colors(
                                                    thumbColor = ElectricVioletLight,
                                                    activeTrackColor = ElectricViolet
                                                )
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Button(
                                            onClick = onOpenEqualizer,
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Open Equalizer & DSP", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        OutlinedButton(
                                            onClick = onResetPlaybackSpeedAndPitch,
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, DeepPurpleDark)
                                        ) {
                                            Icon(Icons.Default.RestartAlt, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Reset Playback Speed (1.0x) & Pitch (0 st)", color = TextSecondary, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }

                            3 -> {
                                // TAB 3: LIBRARY & STORAGE
                                item {
                                    SettingsSectionCard(
                                        title = "MediaStore Library Scanner",
                                        subtitle = "Currently indexed: $songsCount songs across $albumsCount albums"
                                    ) {
                                        Button(
                                            onClick = onManualRescan,
                                            enabled = !isScanning,
                                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (isScanning) "Scanning Device Storage..." else "Rescan Device Music Library Now",
                                                color = TextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))
                                        HorizontalDivider(color = DeepPurpleDark)
                                        Spacer(modifier = Modifier.height(12.dp))

                                        SettingsToggleRow(
                                            title = "Auto-Scan Library on Launch",
                                            subtitle = "Automatically index newly added audio files when opening the app",
                                            checked = settings.autoUpdateLibrary,
                                            onCheckedChange = {
                                                onSettingsChanged(settings.copy(autoUpdateLibrary = it))
                                            }
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        FilledTonalButton(
                                            onClick = onOpenFolders,
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = DeepPurpleDark,
                                                contentColor = ElectricVioletLight
                                            ),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Browse Storage Folders Hierarchy", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showDiagnosticDialog && audioDeviceSpecs != null) {
            AudioPipelineDiagnosticDialog(
                song = currentSong,
                deviceSpecs = audioDeviceSpecs,
                isBitPerfect = isBitPerfect,
                onDismiss = { showDiagnosticDialog = false }
            )
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    val theme = LocalAppTheme.current
    val isLiquidGlass = theme.toneProfileLabel.contains("Liquid Glass", ignoreCase = true)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLiquidGlass) Color(0x220D1B2D) else DeepPurpleCard
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.2.dp,
            brush = if (isLiquidGlass) {
                Brush.linearGradient(
                    listOf(
                        Color(0x80FFFFFF),
                        Color(0x4000F0FF),
                        Color(0x18FFFFFF),
                        Color(0x5500F0FF),
                        Color(0x75FFFFFF)
                    )
                )
            } else {
                Brush.linearGradient(
                    listOf(
                        theme.divider.copy(alpha = 0.4f),
                        theme.surfaceVariant.copy(alpha = 0.2f)
                    )
                )
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .drawWithContent {
                drawContent()
                if (isLiquidGlass) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(
                                Color(0x18FFFFFF),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = size.height * 0.35f
                        )
                    )
                }
            }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = if (isLiquidGlass) Color.White else TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = if (isLiquidGlass) Color(0xB300F0FF) else TextTertiary,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = ElectricViolet)
        )
    }
}
