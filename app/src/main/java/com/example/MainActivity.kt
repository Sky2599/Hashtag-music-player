package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ui.theme.LocalAppTheme
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import com.example.ui.components.AppBottomBar
import com.example.ui.components.AppNavigationRail
import com.example.ui.components.AppSidebar
import com.example.ui.components.AppTopBar
import com.example.ui.components.MiniPlayer
import com.example.ui.components.SidebarItem
import com.example.ui.dialogs.CreatePlaylistDialog
import com.example.ui.dialogs.AddToPlaylistDialog
import com.example.ui.dialogs.PlaylistDetailDialog
import com.example.ui.dialogs.AboutDialog
import com.example.ui.dialogs.ChangelogDialog
import com.example.ui.dialogs.ProSleepTimerDialog
import com.example.ui.dialogs.EqualizerDialog
import com.example.ui.dialogs.FoldersDialog
import com.example.ui.dialogs.PlayingQueueDialog
import com.example.ui.dialogs.PrecisionPlaybackDialog
import com.example.ui.dialogs.TagEditorDialog
import com.example.ui.screens.AlbumsScreen
import com.example.ui.screens.ArtistsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ListeningStatsScreen
import com.example.ui.screens.NowPlayingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SongsScreen
import com.example.ui.theme.DeepPurple
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MusicViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val musicViewModel: MusicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val dynamicTheme by musicViewModel.dynamicTheme.collectAsState()

            // Request Audio & Notification & Record Audio (for hardware visualizer) Permissions
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { results ->
                val audioGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    results[Manifest.permission.READ_MEDIA_AUDIO] == true
                } else {
                    results[Manifest.permission.READ_EXTERNAL_STORAGE] == true
                }
                if (audioGranted) {
                    musicViewModel.triggerRescan()
                }
            }

            LaunchedEffect(Unit) {
                val permissionsToRequest = mutableListOf<String>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_MEDIA_AUDIO)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
                    }
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                } else {
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }
                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
                }
                if (permissionsToRequest.isNotEmpty()) {
                    permissionLauncher.launch(permissionsToRequest.toTypedArray())
                }
            }

            MyApplicationTheme(themeColors = dynamicTheme) {
                MusicApp(viewModel = musicViewModel)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        musicViewModel.persistCurrentPlaybackState()
    }

    override fun onStop() {
        super.onStop()
        musicViewModel.persistCurrentPlaybackState()
    }
@Composable
private fun ShiftingAmbientBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val theme = LocalAppTheme.current
    val isLiquidGlass = theme.toneProfileLabel.contains("Liquid Glass", ignoreCase = true)

    val animSpec = tween<Color>(durationMillis = 1400, easing = LinearOutSlowInEasing)
    val animPrimary by animateColorAsState(theme.primary, animSpec, label = "bg_primary")
    val animPrimaryDim by animateColorAsState(theme.primaryDim, animSpec, label = "bg_primary_dim")
    val animBgDark by animateColorAsState(theme.backgroundDark, animSpec, label = "bg_dark")

    val bgAnim = rememberInfiniteTransition(label = "ambient_bg_shift")
    val shiftPhase by bgAnim.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shift_phase"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val w = size.width
                val h = size.height

                // Base deep dark tone
                drawRect(color = animBgDark)

                // 1. Primary atmospheric chromatic bloom shifting based on album art palette
                val orb1X = w * (0.32f + 0.18f * sin(shiftPhase))
                val orb1Y = h * (0.24f + 0.12f * cos(shiftPhase))
                val radius1 = max(w, h) * 0.85f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            animPrimaryDim.copy(alpha = if (isLiquidGlass) 0.40f else 0.22f),
                            animPrimary.copy(alpha = if (isLiquidGlass) 0.20f else 0.09f),
                            Color.Transparent
                        ),
                        center = Offset(orb1X, orb1Y),
                        radius = radius1
                    )
                )

                // 2. Secondary subtle ambient accent bloom from bottom-right
                val orb2X = w * (0.75f - 0.15f * cos(shiftPhase))
                val orb2Y = h * (0.76f - 0.10f * sin(shiftPhase))
                val radius2 = max(w, h) * 0.70f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            animPrimary.copy(alpha = if (isLiquidGlass) 0.22f else 0.12f),
                            if (isLiquidGlass) Color(0x1800F0FF) else animPrimaryDim.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        center = Offset(orb2X, orb2Y),
                        radius = radius2
                    )
                )
            }
    ) {
        content()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MusicApp(viewModel: MusicViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val allSongs by viewModel.allSongs.collectAsState()
    val recentlyAdded by viewModel.recentlyAdded.collectAsState()
    val mostPlayed by viewModel.mostPlayed.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val albums by viewModel.albums.collectAsState()

    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val queueIndex by viewModel.queueIndex.collectAsState()
    val isShuffle by viewModel.isShuffle.collectAsState()
    val isRepeat by viewModel.isRepeat.collectAsState()

    val equalizerState by viewModel.equalizerState.collectAsState()
    val showEqualizerDialog by viewModel.showEqualizerDialog.collectAsState()
    val showPrecisionPlaybackDialog by viewModel.showPrecisionPlaybackDialog.collectAsState()

    // High-frequency playback streams: only observe when Now Playing (tab 2) or Equalizer is visible
    // to prevent 60fps/25fps recomposition storms while scrolling library tabs
    val currentPositionMs by if (currentTab == 2 || showPrecisionPlaybackDialog) {
        viewModel.currentPositionMs.collectAsState()
    } else {
        remember { mutableLongStateOf(0L) }
    }

    val visualizerFft by if (currentTab == 2) {
        viewModel.visualizerFft.collectAsState()
    } else {
        remember { mutableStateOf(FloatArray(21) { 0.05f }) }
    }

    val crossfadeProgress by if (currentTab == 2) {
        viewModel.crossfadeProgress.collectAsState()
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    val isGaplessActive by viewModel.isGaplessActive.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    // Dialog state flows
    val showSettingsDialog by viewModel.showSettingsDialog.collectAsState()
    val showQueueSheet by viewModel.showQueueSheet.collectAsState()
    val showFoldersDialog by viewModel.showFoldersDialog.collectAsState()
    val showStatsDialog by viewModel.showStatsDialog.collectAsState()
    val showAboutDialog by viewModel.showAboutDialog.collectAsState()
    val showChangelogDialog by viewModel.showChangelogDialog.collectAsState()
    val showSleepTimerDialog by viewModel.showSleepTimerDialog.collectAsState()
    val showTagEditorDialog by viewModel.showTagEditorDialog.collectAsState()

    // Custom Playlists State
    val customPlaylists by viewModel.customPlaylists.collectAsState()
    val showCreatePlaylistDialog by viewModel.showCreatePlaylistDialog.collectAsState()
    val songForAddToPlaylist by viewModel.songForAddToPlaylist.collectAsState()
    val selectedPlaylistForDetail by viewModel.selectedPlaylistForDetail.collectAsState()

    // Pro audio flows
    val audioDeviceSpecs by viewModel.audioDeviceSpecs.collectAsState()
    val loopState by viewModel.loopState.collectAsState()
    val sleepTimerState by viewModel.sleepTimerState.collectAsState()
    val queueSlots by viewModel.queueSlots.collectAsState()
    val activeQueueSlotIndex by viewModel.activeQueueSlotIndex.collectAsState()

    val appSettings by viewModel.appSettings.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val titles = listOf("LIBRARY", "Albums", "Now Playing", "Artists", "Songs")

    // Zero-latency Horizontal Pager for Main Bottom Tabs (0..4)
    val pagerState = rememberPagerState(
        initialPage = currentTab.coerceIn(0, 4),
        pageCount = { 5 }
    )

    // Sync ViewModel tab -> Pager with instantaneous 0ms page switch on tab tap
    LaunchedEffect(currentTab) {
        if (currentTab in 0..4) {
            if (pagerState.currentPage != currentTab && !pagerState.isScrollInProgress) {
                pagerState.scrollToPage(currentTab)
            }
        }
    }

    // Sync Pager 1:1 finger swipe -> ViewModel tab immediately
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (currentTab in 0..4 && !pagerState.isScrollInProgress && currentTab != pagerState.currentPage) {
            viewModel.selectTab(pagerState.currentPage)
        }
    }

    BackHandler(enabled = drawerState.isOpen || showSettingsDialog || showStatsDialog || currentTab != 0) {
        when {
            drawerState.isOpen -> coroutineScope.launch { drawerState.close() }
            showStatsDialog -> viewModel.setStatsDialogVisible(false)
            showSettingsDialog -> viewModel.setSettingsDialogVisible(false)
            currentTab != 0 -> viewModel.selectTab(0)
        }
    }

    val activeTabContent: @Composable () -> Unit = {
        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                userScrollEnabled = !(currentTab == 2 && isLandscape),
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                    when (pageIndex) {
                    0 -> HomeScreen(
                        allSongs = allSongs,
                        recentlyAdded = recentlyAdded,
                        mostPlayed = mostPlayed,
                        favorites = favorites,
                        customPlaylists = customPlaylists,
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        isScanning = isScanning,
                        currentPositionMsProvider = { viewModel.currentPositionMs.value },
                        durationMs = durationMs,
                        onSongClick = { song, queueList -> viewModel.playSong(song, queueList) },
                        onPlayPauseClick = { viewModel.togglePlayPause() },
                        onCreatePlaylistClick = { viewModel.setCreatePlaylistDialogVisible(true) },
                        onPlaylistClick = { viewModel.openPlaylistDetail(it) },
                        onQuickShuffleClick = {
                            if (allSongs.isNotEmpty()) {
                                val shuffled = allSongs.shuffled()
                                viewModel.playSong(shuffled.first(), shuffled)
                            }
                        }
                    )
                    1 -> AlbumsScreen(
                        albums = albums,
                        allSongs = allSongs,
                        onSongClick = { song, queueList -> viewModel.playSong(song, queueList) },
                        onPlayNext = { viewModel.playNext(it) },
                        onAddToQueue = { viewModel.addToQueue(it) },
                        columnsCount = appSettings.albumGridColumns
                    )
                    2 -> NowPlayingScreen(
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        currentPositionMs = currentPositionMs,
                        durationMs = durationMs,
                        queue = queue,
                        queueIndex = queueIndex,
                        isShuffle = isShuffle,
                        isRepeat = isRepeat,
                        visualizerFft = visualizerFft,
                        appSettings = appSettings,
                        crossfadeProgress = crossfadeProgress,
                        crossfadeSeconds = equalizerState.crossfadeSeconds,
                        onPlayPauseClick = { viewModel.togglePlayPause() },
                        onNextClick = { viewModel.skipNext() },
                        onPreviousClick = { viewModel.skipPrevious() },
                        onSeekTo = { viewModel.seekTo(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onToggleShuffle = { viewModel.toggleShuffle() },
                        onToggleRepeat = { viewModel.toggleRepeat() },
                        onSelectQueueItem = { viewModel.playSong(it, switchSlotIfNeeded = false) },
                        onBackClick = { viewModel.selectTab(0) },
                        audioDeviceSpecs = audioDeviceSpecs,
                        isBitPerfect = audioDeviceSpecs.isBitPerfectDirect,
                        isGapless = equalizerState.gaplessEnabled,
                        isGaplessActive = isGaplessActive,
                        showTechnicalChips = appSettings.showTechnicalChips,
                        showVisualizerByDefault = appSettings.highResolutionVisualizer,
                        showAudioPipelineInNowPlaying = appSettings.showAudioPipelineInNowPlaying,
                        onOpenPrecisionPlayback = { viewModel.setPrecisionPlaybackDialogVisible(true) },
                        onOpenSleepTimer = { viewModel.setSleepTimerDialogVisible(true) },
                        onOpenTagEditor = { viewModel.setTagEditorDialogVisible(true) },
                        onOpenEqualizer = { viewModel.setEqualizerDialogVisible(true) }
                    )
                    3 -> ArtistsScreen(
                        allSongs = allSongs,
                        onSongClick = { song, queueList -> viewModel.playSong(song, queueList) },
                        onPlayNext = { viewModel.playNext(it) },
                        onAddToQueue = { viewModel.addToQueue(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) }
                    )
                    4 -> SongsScreen(
                        songs = allSongs,
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        isScanning = isScanning,
                        onSongClick = { song, queueList -> viewModel.playSong(song, queueList) },
                        onPlayNext = { viewModel.playNext(it) },
                        onAddToQueue = { viewModel.addToQueue(it) },
                        onSetupQueue = { songs, playFirst -> viewModel.setupQueue(songs, playFirst) },
                        onAddToPlaylist = { viewModel.openAddToPlaylist(it) },
                        searchQuery = searchQuery,
                        onSearchQueryChange = { viewModel.setSearchQuery(it) }
                    )
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppSidebar(
                isScanning = isScanning,
                currentSong = currentSong,
                onSelectItem = { item ->
                    when (item) {
                        SidebarItem.LIBRARY -> viewModel.selectTab(0)
                        SidebarItem.FOLDERS -> viewModel.setFoldersDialogVisible(true)
                        SidebarItem.EQUALISER -> viewModel.setEqualizerDialogVisible(true)
                        SidebarItem.LISTENING_STATS -> viewModel.setStatsDialogVisible(true)
                        SidebarItem.SCAN_MEDIA -> viewModel.triggerRescan()
                        SidebarItem.SETTINGS -> viewModel.setSettingsDialogVisible(true)
                        SidebarItem.CHANGELOG -> viewModel.setChangelogDialogVisible(true)
                        SidebarItem.ABOUT -> viewModel.setAboutDialogVisible(true)
                    }
                },
                onCloseDrawer = {
                    coroutineScope.launch { drawerState.close() }
                }
            )
        }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            ShiftingAmbientBackground {
                val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val isTablet = maxWidth >= 720.dp
            val useSideRail = isTablet || isLandscape

            if (useSideRail) {
                // Adaptive Landscape / Tablet Layout: Scrollable Side Navigation Rail + Main Content + Docked MiniPlayer
                Row(modifier = Modifier.fillMaxSize()) {
                    AppNavigationRail(
                        currentTab = currentTab,
                        onTabSelected = { tab -> viewModel.selectTab(tab) },
                        onMenuClick = {
                            coroutineScope.launch { drawerState.open() }
                        },
                        onSettingsClick = {
                            viewModel.setSettingsDialogVisible(true)
                        }
                    )

                    Scaffold(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        topBar = {
                            if (currentTab != 2) {
                                AppTopBar(
                                    title = titles.getOrElse(currentTab) { "LIBRARY" },
                                    onMenuClick = {
                                        coroutineScope.launch { drawerState.open() }
                                    },
                                    isScanning = isScanning,
                                    showSearch = currentTab == 4
                                )
                            }
                        },
                        bottomBar = {
                            if (currentSong != null && currentTab != 2) {
                                MiniPlayer(
                                    currentSong = currentSong,
                                    isPlaying = isPlaying,
                                    onPlayPauseClick = { viewModel.togglePlayPause() },
                                    onMiniPlayerClick = { viewModel.selectTab(2) }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Transparent)
                                .padding(innerPadding)
                        ) {
                            activeTabContent()
                        }
                    }
                }
            } else {
                // Mobile Handheld Layout: Top Bar + Content + Bottom Bar with docked MiniPlayer
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    topBar = {
                        if (currentTab != 2) {
                            AppTopBar(
                                title = titles.getOrElse(currentTab) { "LIBRARY" },
                                onMenuClick = {
                                    coroutineScope.launch { drawerState.open() }
                                },
                                isScanning = isScanning,
                                showSearch = currentTab == 4
                            )
                        }
                    },
                    bottomBar = {
                        val configuration = LocalConfiguration.current
                        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                        if (!isLandscape) {
                            Column(modifier = Modifier.background(Color.Transparent)) {
                                if (currentSong != null && currentTab != 2) {
                                    MiniPlayer(
                                        currentSong = currentSong,
                                        isPlaying = isPlaying,
                                        onPlayPauseClick = { viewModel.togglePlayPause() },
                                        onMiniPlayerClick = { viewModel.selectTab(2) }
                                    )
                                }
                                AppBottomBar(
                                    currentTab = currentTab,
                                    onTabSelected = { tab -> viewModel.selectTab(tab) }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Transparent)
                            .padding(innerPadding)
                    ) {
                        activeTabContent()
                    }
                }
            }
        }
    }

                // Full-Screen Tabbed Settings Page
                if (showSettingsDialog) {
                    SettingsScreen(
                        settings = appSettings,
                        isScanning = isScanning,
                        songsCount = allSongs.size,
                        albumsCount = albums.size,
                        audioDeviceSpecs = audioDeviceSpecs,
                        currentSong = currentSong,
                        isBitPerfect = audioDeviceSpecs.isBitPerfectDirect,
                        onSettingsChanged = { viewModel.updateSettings(it) },
                        onManualRescan = { viewModel.triggerRescan() },
                        onOpenEqualizer = {
                            viewModel.setSettingsDialogVisible(false)
                            viewModel.setEqualizerDialogVisible(true)
                        },
                        onOpenFolders = {
                            viewModel.setSettingsDialogVisible(false)
                            viewModel.setFoldersDialogVisible(true)
                        },
                        onResetPlaybackSpeedAndPitch = {
                            viewModel.setPlaybackSpeed(1.0f)
                            viewModel.setPlaybackPitch(0.0f)
                        },
                        onBackClick = { viewModel.setSettingsDialogVisible(false) }
                    )
                }

                // Folders Dialog
                if (showFoldersDialog) {
                    FoldersDialog(
                        allSongs = allSongs,
                        onSongClick = { song, queueList -> viewModel.playSong(song, queueList) },
                        onDismiss = { viewModel.setFoldersDialogVisible(false) }
                    )
                }

                // Playing Queue Dialog
                if (showQueueSheet) {
                    PlayingQueueDialog(
                        queue = queue,
                        queueIndex = queueIndex,
                        onSongClick = { viewModel.playSong(it) },
                        onDismiss = { viewModel.setQueueSheetVisible(false) }
                    )
                }

                // Standalone Full-Screen Listening Statistics & Sonic DNA Page
                if (showStatsDialog) {
                    ListeningStatsScreen(
                        allSongs = allSongs,
                        onSongClick = { song, queueList ->
                            viewModel.playSong(song, queueList)
                        },
                        onBackClick = { viewModel.setStatsDialogVisible(false) }
                    )
                }

                // Equalizer Dialog (invoked from Sidebar)
                if (showEqualizerDialog) {
                    EqualizerDialog(
                        equalizerState = equalizerState,
                        visualizerFftProvider = { viewModel.visualizerFft.value },
                        appSettings = appSettings,
                        onSettingsChanged = { viewModel.updateSettings(it) },
                        onToggleEqualizer = { viewModel.setEqualizerEnabled(it) },
                        onBandLevelChange = { index, levelMb -> viewModel.setBandLevel(index, levelMb) },
                        onBandModeChange = { mode -> viewModel.setEqualizerBandMode(mode) },
                        onPresetSelect = { viewModel.applyEqualizerPreset(it) },
                        onToggleBassBoost = { viewModel.setBassBoost(it, equalizerState.bassBoostStrength) },
                        onBassBoostStrengthChange = { viewModel.setBassBoost(equalizerState.bassBoostEnabled, it) },
                        onToggleStereoWidth = { viewModel.setStereoWidth(it, equalizerState.stereoWidthPercent) },
                        onStereoWidthChange = { viewModel.setStereoWidth(equalizerState.stereoWidthEnabled, it) },
                        onCrossfadeChange = { viewModel.setCrossfade(it) },
                        onToggleGapless = { viewModel.setGapless(it) },
                        onToggleReplayGain = { viewModel.setReplayGain(it) },
                        onToggleParametricEq = { viewModel.setParametricEqEnabled(it) },
                        onApplyAutoEqProfile = { viewModel.applyAutoEqProfile(it) },
                        onToggleReverb = { viewModel.setReverbEnabled(it) },
                        onReverbPresetSelect = { viewModel.setReverbPreset(it) },
                        onReverbRoomSizeChange = { viewModel.setReverbRoomSize(it) },
                        onReverbDecayChange = { viewModel.setReverbDecayMs(it) },
                        onReverbDampingChange = { viewModel.setReverbDamping(it) },
                        onReverbWetLevelChange = { viewModel.setReverbWetLevel(it) },
                        onToggleReverbLoudspeakerBass = { viewModel.setReverbLoudspeakerBass(it, equalizerState.reverbBassPunchStrength) },
                        onReverbBassPunchChange = { viewModel.setReverbLoudspeakerBass(equalizerState.reverbLoudspeakerBass, it) },
                        onDismiss = { viewModel.setEqualizerDialogVisible(false) }
                    )
                }

                // About Dialog
                if (showAboutDialog) {
                    AboutDialog(onDismiss = { viewModel.setAboutDialogVisible(false) })
                }

                // Changelog Dialog
                if (showChangelogDialog) {
                    ChangelogDialog(onDismiss = { viewModel.setChangelogDialogVisible(false) })
                }

                // Precision Playback Dialog (A-B Repeat, Speed, Pitch)
                if (showPrecisionPlaybackDialog) {
                    PrecisionPlaybackDialog(
                        currentPosMs = currentPositionMs,
                        loopState = loopState,
                        onSetPointA = { viewModel.setLoopPointA(it) },
                        onSetPointB = { viewModel.setLoopPointB(it) },
                        onClearLoop = { viewModel.clearAbLoop() },
                        onToggleLoop = { viewModel.setAbLoopEnabled(it) },
                        onSpeedChange = { viewModel.setPlaybackSpeed(it) },
                        onPitchChange = { viewModel.setPlaybackPitch(it) },
                        onDismiss = { viewModel.setPrecisionPlaybackDialogVisible(false) }
                    )
                }

                // Pro Sleep Timer Dialog
                if (showSleepTimerDialog) {
                    ProSleepTimerDialog(
                        sleepTimerState = sleepTimerState,
                        onStartTimer = { mins, stopAtEnd, fadeSec ->
                            viewModel.startSleepTimer(mins, fadeSec, stopAtEnd)
                        },
                        onCancelTimer = { viewModel.cancelSleepTimer() },
                        onDismiss = { viewModel.setSleepTimerDialogVisible(false) }
                    )
                }

                // Tag Editor Dialog
                if (showTagEditorDialog && currentSong != null) {
                    TagEditorDialog(
                        song = currentSong!!,
                        onSaveTags = { tags ->
                            viewModel.saveSongTags(
                                songId = currentSong!!.id,
                                newTitle = tags.title,
                                newArtist = tags.artist,
                                newAlbum = tags.album,
                                newTrackNumber = tags.trackNumber.toIntOrNull()
                            )
                        },
                        onDismiss = { viewModel.setTagEditorDialogVisible(false) }
                    )
                }

                // Create Playlist Dialog (Static & Dynamic Smart Rule Builder)
                if (showCreatePlaylistDialog) {
                    CreatePlaylistDialog(
                        allSongs = allSongs,
                        onConfirm = { name ->
                            viewModel.createPlaylist(name)
                            viewModel.setCreatePlaylistDialogVisible(false)
                        },
                        onConfirmSmartPlaylist = { name, smartConfig ->
                            viewModel.createSmartPlaylist(name, smartConfig)
                            viewModel.setCreatePlaylistDialogVisible(false)
                        },
                        onDismiss = { viewModel.setCreatePlaylistDialogVisible(false) }
                    )
                }

                // Add to Playlist Dialog
                songForAddToPlaylist?.let { song ->
                    AddToPlaylistDialog(
                        song = song,
                        playlists = customPlaylists,
                        onSelectPlaylist = { playlist ->
                            viewModel.addSongToPlaylist(playlist.id, song)
                            viewModel.closeAddToPlaylist()
                        },
                        onCreateNewPlaylist = {
                            viewModel.closeAddToPlaylist()
                            viewModel.setCreatePlaylistDialogVisible(true)
                        },
                        onDismiss = { viewModel.closeAddToPlaylist() }
                    )
                }

                // Playlist Detail Dialog
                selectedPlaylistForDetail?.let { playlist ->
                    PlaylistDetailDialog(
                        playlist = playlist,
                        allSongs = allSongs,
                        onPlayAll = { songs -> viewModel.playPlaylist(playlist, shuffle = false) },
                        onShufflePlay = { songs -> viewModel.playPlaylist(playlist, shuffle = true) },
                        onPlaySong = { song, songs -> viewModel.playPlaylist(playlist, startSong = song) },
                        onRemoveSong = { songId -> viewModel.removeSongFromPlaylist(playlist.id, songId) },
                        onDeletePlaylist = { viewModel.deletePlaylist(playlist.id) },
                        onDismiss = { viewModel.closePlaylistDetail() }
                    )
                }
            }
        }
    }
