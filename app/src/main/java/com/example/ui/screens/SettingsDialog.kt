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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    onOpenMixStudio: () -> Unit = {},
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

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(DeepPurple)
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
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Settings",
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tabs[pagerState.currentPage].subtitle,
                            color = ElectricVioletLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 2. Tab Bar with Instant Pager Sync
            PrimaryScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = DeepPurpleDark,
                contentColor = TextPrimary,
                edgePadding = 12.dp,
                divider = { HorizontalDivider(color = DeepPurpleSurface) }
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = pagerState.currentPage == index
                    Tab(
                        selected = isSelected,
                        onClick = {
                            TactileHaptics.selectionClick(view)
                            coroutineScope.launch {
                                pagerState.scrollToPage(index)
                            }
                        },
                        modifier = Modifier.testTag("settings_tab_$index"),
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) ElectricVioletLight else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = tab.title,
                                    color = if (isSelected) ElectricVioletLight else TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    )
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
                                        subtitle = "Choose how the app dynamically tints backgrounds, controls, and Mix Studio"
                                    ) {
                                        val themeOptions = listOf(
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
                                            Card(
                                                shape = RoundedCornerShape(12.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected) ElectricViolet.copy(alpha = 0.20f) else DeepPurpleDark
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .then(
                                                        if (isSelected) {
                                                            Modifier.border(1.dp, ElectricVioletLight, RoundedCornerShape(12.dp))
                                                        } else Modifier
                                                    )
                                                    .clickable {
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

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = onOpenEqualizer,
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Open Equalizer", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }

                                            FilledTonalButton(
                                                onClick = onOpenMixStudio,
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.filledTonalButtonColors(
                                                    containerColor = DeepPurpleDark,
                                                    contentColor = ElectricVioletLight
                                                ),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text("Mix Studio", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
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
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DeepPurpleCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextTertiary,
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
