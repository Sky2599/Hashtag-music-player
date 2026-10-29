package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import com.example.ui.theme.LocalAppTheme
import com.example.ui.util.LiquidGlassSheen
import com.example.ui.util.glassPressSheen
import com.example.ui.theme.DeepPurpleCard
import com.example.ui.theme.DeepPurpleDark
import com.example.ui.theme.DeepPurpleSurface
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElectricVioletLight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

data class ChangelogCategory(
    val categoryName: String,
    val categoryColor: Color,
    val items: List<String>
)

data class ReleaseVersion(
    val version: String,
    val date: String,
    val isLatest: Boolean = false,
    val headline: String,
    val categories: List<ChangelogCategory>
)

val AppChangelogHistory = listOf(
    ReleaseVersion(
        version = "v3.9.0",
        date = "October 2026",
        isLatest = true,
        headline = "Smart Playlist Rule Builder, Interactive PCM Waveform Seekbar, 32-Bit Biquad DSP & Deep-Tone Palette Engine",
        categories = listOf(
            ChangelogCategory(
                categoryName = "Dynamic Smart Playlists (Rule Builder)",
                categoryColor = Color(0xFFE040FB),
                items = listOf(
                    "Added a full Dynamic Smart Playlist Rule Builder with multi-condition AND/OR logic, live matching track count preview, and one-tap templates (e.g., 'Format = FLAC AND Sample Rate >= 96kHz', 'Play Count > 10 AND Added in Last 30 Days', 'Artist contains 21 Savage AND Duration < 4:00').",
                    "Supports rules across Format, Sample Rate, Bitrate, Bit Depth, Play Count, Added Within Days, Artist, Title, Album, Duration, Hi-Res status, and Favorites."
                )
            ),
            ChangelogCategory(
                categoryName = "Decoded PCM Waveform Seekbar & Visualizer Fallback",
                categoryColor = Color(0xFF00E5FF),
                items = listOf(
                    "Added background MediaExtractor/PCM waveform amplitude and 3-band spectral energy extraction (PcmWaveformExtractor) with LRU caching.",
                    "Replaced the standard seek slider on Now Playing (both portrait and landscape) with an interactive 120-bar Waveform Seekbar supporting tap-to-seek and drag scrubbing.",
                    "Upgraded the Visualizer fallback when OEM/Bluetooth HAL blocks Android's hardware Visualizer so all 21 spectrum bands are driven by the track's real decoded Low/Mid/High PCM energy envelope at the exact playback timestamp."
                )
            ),
            ChangelogCategory(
                categoryName = "32-Bit Biquad Parametric DSP & Deep-Tone Palette Engine",
                categoryColor = Color(0xFF76FF03),
                items = listOf(
                    "Added a 32-bit float RBJ Biquad Parametric Filter cascade & soft-knee mastering limiter processor (PcmBiquadDspProcessor) synchronized with all Graphic, Parametric, and AutoEQ settings.",
                    "Expanded DynamicPaletteEngine to 64 unfiltered swatches plus direct 2D pixel hue/luminance distribution analysis—adapting to pure white covers, dark emerald/forest greens, and deep darker covers without falling back to the default app color."
                )
            )
        )
    ),
    ReleaseVersion(
        version = "v3.8.0",
        date = "October 2026",
        isLatest = false,
        headline = "Swappable Pipeline/Visualizer HUD, Seamless Automix Flow, Smart Artist Parser & Auto-Saved 4-Slot Queues",
        categories = listOf(
            ChangelogCategory(
                categoryName = "Now Playing: Swappable Pipeline HUD & Visualizer",
                categoryColor = Color(0xFFE040FB),
                items = listOf(
                    "Added an interactive 'Pipeline Info ↔ Live Visualizer' toggle button in the Now Playing screen so the Hi-Res Audio Pipeline stats card and the 21-Band FFT Visualizer share a dedicated swappable slot.",
                    "Anchored all music playback controls (Seekbar, Play/Pause, Skip, Shuffle, Repeat, Queue, A-B/Speed, Sleep Timer, Tag Editor, and EQ) in their designated bottom position so they are always visible and never pushed off-screen."
                )
            ),
            ChangelogCategory(
                categoryName = "Mix Studio: Seamless Continuous Playlist Integration",
                categoryColor = Color(0xFF38BDF8),
                items = listOf(
                    "Fixed Mix Studio transitions so playback no longer stops after the transition window—the outgoing track crossfades smoothly into the incoming track and continues playing through the entire rest of your playlist without any gap or interruption.",
                    "Live deck auto-advancement: when a transition completes, the incoming track automatically becomes the active deck and loads the next song in your playlist."
                )
            ),
            ChangelogCategory(
                categoryName = "Smart Artist Metadata & Feature Resolution",
                categoryColor = Color(0xFF76FF03),
                items = listOf(
                    "Upgraded the Artist metadata engine (ArtistNameParser) to intelligently parse collaborations, comma-separated artists, and featured artists (e.g., '21 Savage, Tyler, The Creator' and '21 Savage, Kendrick Lamar') while preserving legitimate comma-containing names like 'Tyler, The Creator'.",
                    "Cross-matches known artists across the database so any song featuring or crediting an artist is automatically grouped under that single artist's profile."
                )
            ),
            ChangelogCategory(
                categoryName = "Multi-Queue Auto-Save & Sidebar Navigation",
                categoryColor = Color(0xFF00E5FF),
                items = listOf(
                    "Removed the Queues icon from the bottom navigation bar and tablet rail, keeping Multi-Queues cleanly accessible from the Side Navigation Drawer.",
                    "Automatic 4-Slot Queue Preservation: starting a new song or playlist that is not part of the active queue automatically saves the previous queue in its slot and switches to an open slot—or replaces the oldest saved queue when all 4 slots are full."
                )
            )
        )
    ),
    ReleaseVersion(
        version = "v3.7.0",
        date = "October 2026",
        isLatest = false,
        headline = "4-Slot Multi-Queue Setter, Tabbed Settings Page, Live Widget Sync & Smooth Pager",
        categories = listOf(
            ChangelogCategory(
                categoryName = "Home Screen Widgets & Live Sync",
                categoryColor = Color(0xFFFFB700),
                items = listOf(
                    "Fixed widget playback state accuracy so home screen widgets only show 'Playing' when audio is actively playing, and immediately reset to paused when playback stops or pauses.",
                    "Live Progress Bar Sync: MusicPlaybackService now pushes real-time 1-second progress updates to all widgets during playback so the progress bar and timestamps advance continuously without requiring a play/pause tap.",
                    "Added unified ACTION_TOGGLE_PLAY_PAUSE intent handling and in-memory album art bitmap caching to eliminate Android AppWidget binder transaction stalls."
                )
            ),
            ChangelogCategory(
                categoryName = "Navigation, Centered Playing Tab & Smooth Pager",
                categoryColor = Color(0xFF76FF03),
                items = listOf(
                    "Moved the 'Playing' (Now Playing) tab to the ergonomic center position on the bottom navigation bar and tablet navigation rail (Home • Albums • Playing • Artists • Songs), swapping Artists to the adjacent slot.",
                    "Added smooth horizontal scrolling animations (HorizontalPager) between main tabs with synchronized bottom bar and sidebar navigation."
                )
            ),
            ChangelogCategory(
                categoryName = "4-Slot Multi-Queue Setter",
                categoryColor = Color(0xFF00E5FF),
                items = listOf(
                    "Added a dedicated 'Multi-Queues & Setter' sidebar destination for full multi-queue management.",
                    "Independently inspect, edit, reorder (move up/down), shuffle, rename, duplicate, clear, or add tracks from the library to any of the 4 Queue Slots (Q1–Q4) without interrupting the currently playing queue.",
                    "Added persistent storage for all 4 queue slots, custom slot names, track lists, and active playback indices across app restarts."
                )
            ),
            ChangelogCategory(
                categoryName = "Full-Screen Tabbed Settings",
                categoryColor = Color(0xFFE040FB),
                items = listOf(
                    "Replaced the slide-up settings sheet with a dedicated full-screen, swipeable 4-tab Settings Page (Appearance, Now Playing, Audio & DSP, and Library).",
                    "Every setting is wired to real engine & UI functionality: 3-Way Theme Engine (Material You, Dynamic Album Art, High Contrast), Pure AMOLED (#000000) vs. Onyx Dark surfaces, Album Grid Columns (2/3/4), Now Playing Technical HUD & Visualizer defaults, Crossfade Duration, Gapless 0ms Playback, ReplayGain Normalization, Speed/Pitch Reset, and MediaStore Rescan & Folder Filters."
                )
            ),
            ChangelogCategory(
                categoryName = "Dynamic Theming & Anti-Crash Stability",
                categoryColor = Color(0xFF38BDF8),
                items = listOf(
                    "Unified the Mix Studio (DJ Transitions) palette with the app's dynamic purple/Material You/album-art theme so it blends seamlessly with the rest of the interface.",
                    "Serialized hardware Equalizer, BassBoost, Virtualizer, and crossfade/gapless MediaPlayer transitions onto a dedicated single-thread transition dispatcher to eliminate race conditions and crashes under rapid user input.",
                    "Throttled background FFT visualizer emissions and isolated high-frequency playback state flows so library lists scroll smoothly without recomposition lag."
                )
            )
        )
    ),
    ReleaseVersion(
        version = "v3.6.0",
        date = "October 2026",
        isLatest = false,
        headline = "Mix Studio: DJ Transition & Automix Visualizer Engine",
        categories = listOf(
            ChangelogCategory(
                categoryName = "DJ Transition & Automix Studio",
                categoryColor = Color(0xFFE040FB),
                items = listOf(
                    "Added 'Mix Studio / DJ Transitions' directly in the Side Navigation Drawer below Equaliser & DSP.",
                    "Standalone real-library track selection: dynamically loads your actual outgoing and incoming songs from the active queue or lets you pick any track from your device library—with zero hardcoded or preset song titles.",
                    "Dynamic Track Metadata & Harmonic Key Analysis Header displaying real artwork, title, artist, exact BPM detection, Camelot key badges (e.g., 5A, 2A), and duration/timestamp offsets."
                )
            ),
            ChangelogCategory(
                categoryName = "Dual Waveform & Automation Curves",
                categoryColor = Color(0xFF38BDF8),
                items = listOf(
                    "Stacked Dual Waveforms for outgoing (top) and incoming (bottom) tracks with a synchronized real-time playhead and interactive scrubbing.",
                    "Interactive Beat Grid Selector pill (16 bars, 8 bars, 4 bars, 2 bars) to align transition windows.",
                    "Visual Volume Envelope curves, High/Low Filter Sweep automation curves, and highlighted Crossfade Bounds box.",
                    "Interactive Mix Preview Play/Pause overlay button and 'Auto-Blend' harmonic beat-alignment action."
                )
            )
        )
    ),
    ReleaseVersion(
        version = "v3.5.0",
        date = "September 2026",
        isLatest = false,
        headline = "Material You Dynamic Engine, Reactive FFT Visualizer & Widgets",
        categories = listOf(
            ChangelogCategory(
                categoryName = "Material You & Theming",
                categoryColor = Color(0xFF38BDF8),
                items = listOf(
                    "Implemented Android Material You (Monet) dynamic engine extracting system wallpaper accent colors to tint UI controls, sliders, and navigation.",
                    "Added seamless 3-way Theme Switcher: Material You (System Accent), Default Dark Contrast (#1A002C + #FFB700), and Dynamic Album Art.",
                    "High-contrast color harmonizer preserving legibility across dark wallpapers and vibrant artwork."
                )
            ),
            ChangelogCategory(
                categoryName = "Real-Time FFT Visualizer",
                categoryColor = Color(0xFFE040FB),
                items = listOf(
                    "Album-Adaptive Spectrum Tinting: custom painter dynamically binds 7 frequency bands directly to dominant artwork tones.",
                    "Sub-bass to brilliance progression with physical gravity needle falloff and glowing peak hold indicators.",
                    "Integrated real-time FFT visualization in both Now Playing and Equalizer views."
                )
            ),
            ChangelogCategory(
                categoryName = "Home Screen Widgets",
                categoryColor = Color(0xFFFFB700),
                items = listOf(
                    "Compact Player Widget: Artwork thumbnail, song title, artist, and responsive play/pause/skip controls.",
                    "Detailed Audiophile Widget: Live seek progress bar, elapsed/remaining time, and technical format tags (e.g., FLAC • 980 kbps • 32-bit DSP).",
                    "Quick Queue Widget: Interactive mini playlist with direct track switching directly from the home screen."
                )
            ),
            ChangelogCategory(
                categoryName = "UI & Sidebar Navigation",
                categoryColor = Color(0xFF76FF03),
                items = listOf(
                    "Added dedicated 'Changelog & Updates' drawer item positioned above the version footer.",
                    "Structured release note browser with version tags and categorized change logs."
                )
            )
        )
    ),
    ReleaseVersion(
        version = "v3.4.0",
        date = "August 2026",
        headline = "32-bit Hi-Res DSP Audio Engine & AutoEQ Database",
        categories = listOf(
            ChangelogCategory(
                categoryName = "Audio Engine & DSP",
                categoryColor = Color(0xFF2979FF),
                items = listOf(
                    "Built 32-bit float internal audio processing pipeline with AAudio low-latency architecture.",
                    "Dual-mode 5-band and 10-band studio graphic equalizer with ±15 dB range.",
                    "AutoEQ database integration with target curves for over 1,500 headphone models.",
                    "Bit-perfect direct PCM output mode for audiophile DACs."
                )
            ),
            ChangelogCategory(
                categoryName = "Playback Features",
                categoryColor = Color(0xFFFF6D00),
                items = listOf(
                    "Smart sleep timer with logarithmic volume fade-out and stop-at-track-end.",
                    "Precision A-B looper with millisecond scrub precision and tempo adjustment.",
                    "Sample-accurate gapless playback with customizable crossfade curves."
                )
            )
        )
    ),
    ReleaseVersion(
        version = "v3.2.0",
        date = "July 2026",
        headline = "Multi-Queue Manager & Fast Media Indexer",
        categories = listOf(
            ChangelogCategory(
                categoryName = "Library & Tag Management",
                categoryColor = Color(0xFF00E5FF),
                items = listOf(
                    "Multi-Queue Switcher allowing instantaneous swapping between independent listening sessions.",
                    "Embedded ID3v2 tag editor supporting title, artist, album, and track number edits.",
                    "Zero-recomposition GPU fast scrollbar with alphabetic bubble indexing."
                )
            ),
            ChangelogCategory(
                categoryName = "Optimizations",
                categoryColor = Color(0xFF76FF03),
                items = listOf(
                    "Decoupled 60fps playback streams from library lists to ensure butter-smooth 120 FPS scrolling.",
                    "Optimized MediaStore indexing query pipeline for libraries with 50,000+ tracks."
                )
            )
        )
    ),
    ReleaseVersion(
        version = "v3.0.0",
        date = "May 2026",
        headline = "Initial Offline Music Player Architecture",
        categories = listOf(
            ChangelogCategory(
                categoryName = "Core Playback",
                categoryColor = Color(0xFF38BDF8),
                items = listOf(
                    "Local storage playback for MP3, FLAC, WAV, AAC, and OGG formats.",
                    "Foreground media playback service with persistent system notification controls.",
                    "Smart playlist management: Favorites, Most Played, Recently Added, and Custom Playlists."
                )
            )
        )
    )
)

@Composable
fun ChangelogDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    val isLiquidGlass = theme.toneProfileLabel.contains("Liquid Glass", ignoreCase = true)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(
                    width = 1.2.dp,
                    brush = if (isLiquidGlass) LiquidGlassSheen.GlassBorderGradient else SolidColor(DeepPurpleCard),
                    shape = RoundedCornerShape(24.dp)
                ),
            color = if (isLiquidGlass) Color(0xEB0A1324) else DeepPurpleSurface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isLiquidGlass) {
                                        Brush.linearGradient(
                                            listOf(Color(0x4000F0FF), Color(0x2038BDF8))
                                        )
                                    } else {
                                        SolidColor(ElectricViolet.copy(alpha = 0.2f))
                                    }
                                )
                                .border(
                                    1.dp,
                                    if (isLiquidGlass) {
                                        Brush.horizontalGradient(
                                            listOf(Color(0x80FFFFFF), Color(0x6000F0FF))
                                        )
                                    } else {
                                        SolidColor(Color.Transparent)
                                    },
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NewReleases,
                                contentDescription = null,
                                tint = if (isLiquidGlass) Color(0xFF00F0FF) else ElectricViolet,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Changelog & Updates",
                                color = if (isLiquidGlass) Color.White else TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Version release history and features",
                                color = if (isLiquidGlass) Color(0xFFB0C5DB) else TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isLiquidGlass) Color(0x20FFFFFF) else Color.Transparent
                            )
                            .border(
                                1.dp,
                                if (isLiquidGlass) Color(0x40FFFFFF) else Color.Transparent,
                                CircleShape
                            )
                            .testTag("close_changelog_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close changelog",
                            tint = if (isLiquidGlass) Color.White else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(
                    color = if (isLiquidGlass) Color(0x2500F0FF) else DeepPurpleDark,
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable version cards
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(AppChangelogHistory) { release ->
                        VersionReleaseCard(release = release, isLiquidGlass = isLiquidGlass)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Footer
                Text(
                    text = "Hashtag Music Player • Material You & 32-bit DSP Edition",
                    color = if (isLiquidGlass) Color(0x80B0C5DB) else TextTertiary,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
private fun VersionReleaseCard(
    release: ReleaseVersion,
    isLiquidGlass: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLiquidGlass) Color(0x220D1B2D) else DeepPurpleCard
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = if (isLiquidGlass) LiquidGlassSheen.SubtleGlassBorder else SolidColor(Color.Transparent),
                shape = RoundedCornerShape(16.dp)
            )
            .glassPressSheen(isLiquidGlass = isLiquidGlass, isPressed = isPressed)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { /* Tactile tap response */ }
            .testTag("changelog_card_${release.version}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Version and Date Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = release.version,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (release.isLatest) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isLiquidGlass) {
                                        Brush.horizontalGradient(
                                            listOf(Color(0x4500F0FF), Color(0x3038BDF8))
                                        )
                                    } else {
                                        SolidColor(ElectricViolet.copy(alpha = 0.25f))
                                    }
                                )
                                .border(
                                    0.8.dp,
                                    if (isLiquidGlass) Color(0x8000F0FF) else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = if (isLiquidGlass) Color(0xFF00F0FF) else ElectricViolet,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "LATEST",
                                    color = if (isLiquidGlass) Color(0xFFE0FFFF) else ElectricVioletLight,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Text(
                    text = release.date,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = release.headline,
                color = if (isLiquidGlass) Color(0xFF00F0FF) else ElectricVioletLight,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Categories with structured bullet points
            release.categories.forEach { category ->
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(category.categoryColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = category.categoryName,
                            color = category.categoryColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    category.items.forEach { bullet ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, top = 2.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "• ",
                                color = TextTertiary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = bullet,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
