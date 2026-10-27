package com.example.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.CustomPlaylistEntity
import com.example.data.model.Album
import com.example.data.model.AppSettings
import com.example.data.model.Song
import com.example.data.repository.MusicRepository
import com.example.player.EqualizerState
import com.example.service.MusicPlaybackService
import com.example.theme.AppThemeColors
import com.example.theme.DynamicPaletteEngine
import com.example.player.AbLoopState
import com.example.player.AudioDeviceInspector
import com.example.player.AudioDeviceSpecs
import com.example.player.ProSleepTimer
import com.example.player.MultiQueueManager
import com.example.player.PrecisionPlaybackController
import com.example.player.QueueSlot
import com.example.player.SleepTimerState
import com.example.player.TagEditorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = MusicRepository(db.songDao(), db.playlistDao())
    val playlistTransitionDao = db.playlistTransitionDao()
    private val settingsManager = com.example.data.util.SettingsManager(application)

    val allSongs: StateFlow<List<Song>> = repository.allSongs
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val recentlyAdded: StateFlow<List<Song>> = repository.recentlyAdded
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val mostPlayed: StateFlow<List<Song>> = repository.mostPlayed
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val favorites: StateFlow<List<Song>> = repository.favorites
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val hiResSongs: StateFlow<List<Song>> = repository.hiResSongs
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val albums: StateFlow<List<Album>> = repository.albums
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val customPlaylists: StateFlow<List<CustomPlaylistEntity>> = repository.customPlaylists
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _showCreatePlaylistDialog = MutableStateFlow(false)
    val showCreatePlaylistDialog: StateFlow<Boolean> = _showCreatePlaylistDialog.asStateFlow()

    private val _songForAddToPlaylist = MutableStateFlow<Song?>(null)
    val songForAddToPlaylist: StateFlow<Song?> = _songForAddToPlaylist.asStateFlow()

    private val _selectedPlaylistForDetail = MutableStateFlow<CustomPlaylistEntity?>(null)
    val selectedPlaylistForDetail: StateFlow<CustomPlaylistEntity?> = _selectedPlaylistForDetail.asStateFlow()

    private val _currentTab = MutableStateFlow(settingsManager.getLastTab().coerceIn(0, 5)) // 0: Home, 1: Albums, 2: Now Playing, 3: Artists, 4: Songs, 5: Multi-Queues
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _queueIndex = MutableStateFlow(0)
    val queueIndex: StateFlow<Int> = _queueIndex.asStateFlow()

    private val _isShuffle = MutableStateFlow(settingsManager.getLastShuffle())
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _isRepeat = MutableStateFlow(settingsManager.getLastRepeat())
    val isRepeat: StateFlow<Boolean> = _isRepeat.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    private val _showQueueSheet = MutableStateFlow(false)
    val showQueueSheet: StateFlow<Boolean> = _showQueueSheet.asStateFlow()

    private val _showFoldersDialog = MutableStateFlow(false)
    val showFoldersDialog: StateFlow<Boolean> = _showFoldersDialog.asStateFlow()

    private val _showStatsDialog = MutableStateFlow(false)
    val showStatsDialog: StateFlow<Boolean> = _showStatsDialog.asStateFlow()

    private val _showAboutDialog = MutableStateFlow(false)
    val showAboutDialog: StateFlow<Boolean> = _showAboutDialog.asStateFlow()

    private val _showChangelogDialog = MutableStateFlow(false)
    val showChangelogDialog: StateFlow<Boolean> = _showChangelogDialog.asStateFlow()

    private val _showEqualizerDialog = MutableStateFlow(false)
    val showEqualizerDialog: StateFlow<Boolean> = _showEqualizerDialog.asStateFlow()

    private val _isGaplessActive = MutableStateFlow(false)
    val isGaplessActive: StateFlow<Boolean> = _isGaplessActive.asStateFlow()

    // Pro Dialogs & Feature States
    private val _showPrecisionPlaybackDialog = MutableStateFlow(false)
    val showPrecisionPlaybackDialog: StateFlow<Boolean> = _showPrecisionPlaybackDialog.asStateFlow()

    private val _showSleepTimerDialog = MutableStateFlow(false)
    val showSleepTimerDialog: StateFlow<Boolean> = _showSleepTimerDialog.asStateFlow()

    private val _showTagEditorDialog = MutableStateFlow(false)
    val showTagEditorDialog: StateFlow<Boolean> = _showTagEditorDialog.asStateFlow()

    // Professional Audio Engine Managers
    private val audioDeviceInspector = AudioDeviceInspector(application)
    val audioDeviceSpecs: StateFlow<AudioDeviceSpecs> = audioDeviceInspector.currentDeviceSpecs

    private val precisionPlaybackController = PrecisionPlaybackController()
    val loopState: StateFlow<AbLoopState> = precisionPlaybackController.loopState

    private val proSleepTimer = ProSleepTimer(
        onVolumeFadeUpdate = { multiplier: Float ->
            playbackService?.playerEngine?.setSleepTimerVolumeMultiplier(multiplier)
        },
        onTimerFinished = {
            playbackService?.playerEngine?.pause()
            _isPlaying.value = false
        }
    )
    val sleepTimerState: StateFlow<SleepTimerState> = proSleepTimer.timerState

    private val multiQueueManager = MultiQueueManager()
    val queueSlots: StateFlow<List<QueueSlot>> = multiQueueManager.queues
    val activeQueueSlotIndex: StateFlow<Int> = multiQueueManager.activeQueueSlotIndex

    private val _selectedAlbum = MutableStateFlow<Album?>(null)
    val selectedAlbum: StateFlow<Album?> = _selectedAlbum.asStateFlow()

    private val _appSettings = MutableStateFlow(settingsManager.loadSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    private val _dynamicTheme = MutableStateFlow<AppThemeColors>(DynamicPaletteEngine.defaultTheme)
    val dynamicTheme: StateFlow<AppThemeColors> = _dynamicTheme.asStateFlow()

    private val _dynamicAccentColor = MutableStateFlow<Color?>(null)
    val dynamicAccentColor: StateFlow<Color?> = _dynamicAccentColor.asStateFlow()

    // Service binding
    private var playbackService: MusicPlaybackService? = null
    private var isServiceBound = false

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _equalizerState = MutableStateFlow(EqualizerState())
    val equalizerState: StateFlow<EqualizerState> = _equalizerState.asStateFlow()

    private val _visualizerFft = MutableStateFlow<FloatArray>(FloatArray(24) { 0.1f })
    val visualizerFft: StateFlow<FloatArray> = _visualizerFft.asStateFlow()

    private val _crossfadeProgress = MutableStateFlow(0f)
    val crossfadeProgress: StateFlow<Float> = _crossfadeProgress.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as? MusicPlaybackService.LocalBinder
            playbackService = localBinder?.getService()
            isServiceBound = true

            playbackService?.let { service ->
                // 1. Immediately sync active playback state if service was playing!
                val activeSong = service.currentSong
                if (activeSong != null) {
                    _currentSong.value = activeSong
                    _isPlaying.value = service.playerEngine.isPlaying.value
                    _currentPositionMs.value = service.playerEngine.currentPositionMs.value
                    _durationMs.value = service.playerEngine.durationMs.value
                    viewModelScope.launch {
                        extractDynamicColorIfNeeded(activeSong)
                    }
                } else {
                    // Restore last played song from persistent store
                    restoreLastPlaybackState()
                }

                viewModelScope.launch {
                    service.playerEngine.isPlaying.collect { _isPlaying.value = it }
                }
                viewModelScope.launch {
                    service.playerEngine.currentPositionMs.collect { pos ->
                        _currentPositionMs.value = pos
                        if (pos > 0 && pos % 5000 < 500) {
                            val cur = _currentSong.value
                            if (cur != null) {
                                settingsManager.saveLastPlaybackState(
                                    cur.id,
                                    pos,
                                    _queue.value.map { it.id },
                                    _queueIndex.value,
                                    _isShuffle.value,
                                    _isRepeat.value
                                )
                            }
                        }
                    }
                }
                viewModelScope.launch {
                    service.playerEngine.durationMs.collect { _durationMs.value = it }
                }
                viewModelScope.launch {
                    service.playerEngine.equalizerState.collect { _equalizerState.value = it }
                }
                viewModelScope.launch {
                    service.playerEngine.visualizerFft.collect { _visualizerFft.value = it }
                }
                viewModelScope.launch {
                    service.playerEngine.crossfadeProgress.collect { _crossfadeProgress.value = it }
                }
                viewModelScope.launch {
                    service.playerEngine.isGaplessActive.collect { _isGaplessActive.value = it }
                }

                // Do not call synchronous MediaPlayer.prepare() on service connect;
                // prepare will occur on-demand when the user taps Play.

                service.playerEngine.onCompletionListener = {
                    if (!proSleepTimer.handleTrackCompleted()) {
                        skipNext()
                    }
                }

                service.playerEngine.loopCheckProvider = { currentPos ->
                    precisionPlaybackController.checkLoopTrigger(currentPos)
                }

                service.nextSongCallback = {
                    val q = _queue.value
                    val curIdx = _queueIndex.value
                    if (q.isNotEmpty()) {
                        val nextIdx = curIdx + 1
                        if (nextIdx < q.size) {
                            q[nextIdx]
                        } else if (_isRepeat.value) {
                            q.firstOrNull()
                        } else null
                    } else null
                }

                service.onTrackTransitioned = { transitionedSong ->
                    _currentSong.value = transitionedSong
                    val q = _queue.value
                    val newIdx = q.indexOfFirst { it.id == transitionedSong.id }
                    if (newIdx >= 0) {
                        _queueIndex.value = newIdx
                    }
                    viewModelScope.launch {
                        repository.incrementPlayCount(transitionedSong.id)
                        extractDynamicColorIfNeeded(transitionedSong)

                        // Seamless Automix Queue Integration: preload custom saved profile for upcoming track
                        val nextSong = if (newIdx + 1 < q.size) q[newIdx + 1] else if (_isRepeat.value) q.firstOrNull() else null
                        if (nextSong != null) {
                            val profile = playlistTransitionDao.findTransitionForPair(transitionedSong.id, nextSong.id)
                            service.playerEngine.activeTransitionProfile = profile
                        } else {
                            service.playerEngine.activeTransitionProfile = null
                        }

                        settingsManager.saveLastPlaybackState(
                            transitionedSong.id,
                            0L,
                            q.map { it.id },
                            _queueIndex.value,
                            _isShuffle.value,
                            _isRepeat.value
                        )
                    }
                }

                service.onSkipNextCallback = { skipNext() }
                service.onSkipPrevCallback = { skipPrevious() }
                service.onPlayQueueIndexCallback = { targetIndex ->
                    val q = _queue.value
                    if (targetIndex in q.indices) {
                        _queueIndex.value = targetIndex
                        playSong(q[targetIndex])
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playbackService = null
            isServiceBound = false
        }
    }

    init {
        restoreLastPlaybackStateImmediate()
        bindPlaybackService()
        initialLibraryScan()
        registerMediaStoreObserver()
        restoreLastPlaybackState()
    }

    private var hasRestoredPlaybackState = false

    private fun restoreLastPlaybackStateImmediate() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val lastSongId = settingsManager.getLastSongId()
                val cached = db.songDao().getAllSongsOnce()
                if (cached.isNotEmpty()) {
                    val songs = cached.map { it.toSong() }
                    val songById = songs.associateBy { it.id }
                    val found = if (lastSongId != -1L) songById[lastSongId] else songs.firstOrNull()
                    val savedPos = settingsManager.getLastPositionMs()
                    val qIds = settingsManager.getLastQueueIds()
                    val savedQIndex = settingsManager.getLastQueueIndex()
                    val restoredQ = if (qIds.isNotEmpty()) {
                        qIds.mapNotNull { id -> songById[id] }.ifEmpty { if (found != null) listOf(found) else emptyList() }
                    } else if (found != null) {
                        listOf(found)
                    } else {
                        emptyList()
                    }
                    val validQIndex = savedQIndex.coerceIn(0, (restoredQ.size - 1).coerceAtLeast(0))

                    val savedActiveSlot = settingsManager.getSavedActiveQueueSlot()
                    val defaultSlotNames = listOf(
                        "Queue 1 (Main)",
                        "Queue 2 (Lossless)",
                        "Queue 3 (Focus/Acoustic)",
                        "Queue 4 (Party/Energy)"
                    )
                    val restoredSlots = (0 until 4).map { slotIdx ->
                        val sName = settingsManager.getSavedQueueSlotName(slotIdx, defaultSlotNames[slotIdx])
                        val sIds = settingsManager.getSavedQueueSlotIds(slotIdx)
                        val sActiveIdx = settingsManager.getSavedQueueSlotActiveIndex(slotIdx)
                        val slotSongs = if (sIds.isNotEmpty()) {
                            sIds.mapNotNull { id -> songById[id] }
                        } else if (slotIdx == savedActiveSlot) {
                            restoredQ
                        } else {
                            emptyList()
                        }
                        QueueSlot(
                            id = slotIdx + 1,
                            name = sName,
                            items = slotSongs,
                            activeIndex = sActiveIdx.coerceIn(0, (slotSongs.size - 1).coerceAtLeast(0))
                        )
                    }

                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        if (_currentSong.value == null && found != null) {
                            hasRestoredPlaybackState = true
                            _currentSong.value = found
                            _currentPositionMs.value = savedPos
                            _durationMs.value = found.durationMs
                            _queue.value = restoredQ
                            _queueIndex.value = validQIndex
                            multiQueueManager.restoreQueues(restoredSlots, savedActiveSlot)
                            multiQueueManager.updateCurrentQueue(restoredQ, validQIndex)
                            extractDynamicColorIfNeeded(found)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MusicViewModel", "restoreLastPlaybackStateImmediate failed: ${e.message}")
            }
        }
    }

    private fun restoreLastPlaybackState() {
        if (hasRestoredPlaybackState) return
        val lastSongId = settingsManager.getLastSongId()
        if (lastSongId == -1L) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            allSongs.collect { songs ->
                if (!hasRestoredPlaybackState && songs.isNotEmpty() && _currentSong.value == null) {
                    val songById = songs.associateBy { it.id }
                    val found = songById[lastSongId]
                    if (found != null) {
                        hasRestoredPlaybackState = true
                        val savedPos = settingsManager.getLastPositionMs()
                        val qIds = settingsManager.getLastQueueIds()
                        val savedQIndex = settingsManager.getLastQueueIndex()
                        val restoredQ = if (qIds.isNotEmpty()) {
                            qIds.mapNotNull { id -> songById[id] }.ifEmpty { listOf(found) }
                        } else {
                            listOf(found)
                        }
                        val validQIndex = savedQIndex.coerceIn(0, (restoredQ.size - 1).coerceAtLeast(0))

                        // Restore Multi-Queue Slots from persistent store using O(1) map lookup
                        val savedActiveSlot = settingsManager.getSavedActiveQueueSlot()
                        val defaultSlotNames = listOf(
                            "Queue 1 (Main)",
                            "Queue 2 (Lossless)",
                            "Queue 3 (Focus/Acoustic)",
                            "Queue 4 (Party/Energy)"
                        )
                        val restoredSlots = (0 until 4).map { slotIdx ->
                            val sName = settingsManager.getSavedQueueSlotName(slotIdx, defaultSlotNames[slotIdx])
                            val sIds = settingsManager.getSavedQueueSlotIds(slotIdx)
                            val sActiveIdx = settingsManager.getSavedQueueSlotActiveIndex(slotIdx)
                            val slotSongs = if (sIds.isNotEmpty()) {
                                sIds.mapNotNull { id -> songById[id] }
                            } else if (slotIdx == savedActiveSlot) {
                                restoredQ
                            } else {
                                emptyList()
                            }
                            QueueSlot(
                                id = slotIdx + 1,
                                name = sName,
                                items = slotSongs,
                                activeIndex = sActiveIdx.coerceIn(0, (slotSongs.size - 1).coerceAtLeast(0))
                            )
                        }

                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            _currentSong.value = found
                            _currentPositionMs.value = savedPos
                            _durationMs.value = found.durationMs
                            _queue.value = restoredQ
                            _queueIndex.value = validQIndex
                            multiQueueManager.restoreQueues(restoredSlots, savedActiveSlot)
                            multiQueueManager.updateCurrentQueue(_queue.value, _queueIndex.value)
                        }
                        extractDynamicColorIfNeeded(found)
                    }
                }
            }
        }
    }

    fun persistCurrentPlaybackState() {
        val cur = _currentSong.value ?: return
        settingsManager.saveLastPlaybackState(
            songId = cur.id,
            positionMs = _currentPositionMs.value,
            queueIds = _queue.value.map { it.id },
            queueIndex = _queueIndex.value,
            isShuffle = _isShuffle.value,
            isRepeat = _isRepeat.value
        )
    }

    private fun bindPlaybackService() {
        val context = getApplication<Application>()
        val intent = Intent(context, MusicPlaybackService::class.java)
        try {
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            android.util.Log.e("MusicViewModel", "bindService failed: ${e.message}")
        }
    }

    private fun initialLibraryScan() {
        if (!_appSettings.value.autoUpdateLibrary) {
            return
        }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // If library is already cached in Room, delay background scan so UI renders instant warm start
                val cachedCount = db.songDao().getSongCount()
                if (cachedCount > 0) {
                    kotlinx.coroutines.delay(1500L)
                } else {
                    kotlinx.coroutines.delay(100L)
                }
                _isScanning.value = true
                repository.refreshLibrary(getApplication(), _appSettings.value.excludedFolders, forceDeepScan = false)
            } catch (e: Exception) {
                android.util.Log.e("MusicViewModel", "initialLibraryScan failed: ${e.message}", e)
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun selectTab(index: Int) {
        _currentTab.value = index
        settingsManager.saveLastTab(index)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun playSong(song: Song, newQueue: List<Song>? = null, switchSlotIfNeeded: Boolean = true) {
        val currentQ = _queue.value
        if (newQueue != null) {
            val targetIdx = newQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
            val isSameAsCurrent = multiQueueManager.isSameQueueList(currentQ, newQueue)
            if (!isSameAsCurrent && switchSlotIfNeeded && currentQ.isNotEmpty()) {
                // Save current queue & playback index in its current slot first
                multiQueueManager.updateCurrentQueue(currentQ, _queueIndex.value)
                // Rotate to an empty slot or overwrite the oldest of the 4 slots
                val label = if (newQueue.size == 1) song.title else song.album.ifBlank { "Queue" }
                multiQueueManager.allocateSlotForNewQueue(newQueue, targetIdx, label)
            } else {
                multiQueueManager.updateCurrentQueue(newQueue, targetIdx)
            }
            _queue.value = newQueue
            _queueIndex.value = targetIdx
        } else {
            val idxInCurrent = currentQ.indexOfFirst { it.id == song.id }
            if (idxInCurrent >= 0) {
                // Song is part of the current active queue
                _queueIndex.value = idxInCurrent
                multiQueueManager.updateCurrentQueue(currentQ, idxInCurrent)
            } else if (currentQ.isEmpty()) {
                val singleQueue = listOf(song)
                _queue.value = singleQueue
                _queueIndex.value = 0
                multiQueueManager.updateCurrentQueue(singleQueue, 0)
            } else if (switchSlotIfNeeded) {
                // Song is NOT part of the current queue: preserve current queue in its slot and rotate to another slot!
                multiQueueManager.updateCurrentQueue(currentQ, _queueIndex.value)
                val singleQueue = listOf(song)
                multiQueueManager.allocateSlotForNewQueue(singleQueue, 0, song.title)
                _queue.value = singleQueue
                _queueIndex.value = 0
            } else {
                multiQueueManager.updateCurrentQueue(currentQ, _queueIndex.value)
            }
        }

        _currentSong.value = song
        saveMultiQueuesToPrefs()
        playbackService?.playSong(song)

        viewModelScope.launch {
            repository.incrementPlayCount(song.id)
            extractDynamicColorIfNeeded(song)

            // Preload custom saved transition profile for upcoming track
            val q = _queue.value
            val idx = _queueIndex.value
            val nextSong = if (idx + 1 < q.size) q[idx + 1] else if (_isRepeat.value) q.firstOrNull() else null
            if (nextSong != null) {
                val profile = playlistTransitionDao.findTransitionForPair(song.id, nextSong.id)
                playbackService?.playerEngine?.activeTransitionProfile = profile
            } else {
                playbackService?.playerEngine?.activeTransitionProfile = null
            }

            settingsManager.saveLastPlaybackState(
                song.id,
                0L,
                _queue.value.map { it.id },
                _queueIndex.value,
                _isShuffle.value,
                _isRepeat.value
            )
        }
    }

    private suspend fun extractDynamicColorIfNeeded(song: Song?) {
        val settings = _appSettings.value
        val colors = DynamicPaletteEngine.resolveTheme(
            context = getApplication(),
            themeEngine = settings.themeEngine,
            song = song,
            darkThemeMode = settings.darkThemeMode
        )
        _dynamicTheme.value = colors
        _dynamicAccentColor.value = colors.primary
    }

    private fun saveMultiQueuesToPrefs() {
        val slots = multiQueueManager.queues.value
        settingsManager.saveMultiQueueState(
            activeSlotIndex = multiQueueManager.activeQueueSlotIndex.value,
            slotNames = slots.map { it.name },
            slotSongIds = slots.map { slot -> slot.items.map { it.id } },
            slotActiveIndices = slots.map { it.activeIndex }
        )
    }

    private var lastPlaybackActionTimestamp = 0L

    fun togglePlayPause() {
        val now = System.currentTimeMillis()
        if (now - lastPlaybackActionTimestamp < 250L) return
        lastPlaybackActionTimestamp = now

        if (_isPlaying.value) {
            playbackService?.playerEngine?.pause()
            persistCurrentPlaybackState()
        } else {
            val cur = _currentSong.value
            if (cur == null && allSongs.value.isNotEmpty()) {
                playSong(allSongs.value.first(), allSongs.value)
            } else if (cur != null) {
                val service = playbackService
                if (service != null && service.currentSong?.id == cur.id && service.playerEngine.hasActivePlayer()) {
                    service.playerEngine.resume()
                } else {
                    service?.playSong(cur, _currentPositionMs.value)
                }
            }
        }
    }

    fun skipNext() {
        val now = System.currentTimeMillis()
        if (now - lastPlaybackActionTimestamp < 80L) return
        lastPlaybackActionTimestamp = now

        val q = _queue.value
        if (q.isEmpty()) return

        var nextIdx = _queueIndex.value + 1
        if (nextIdx >= q.size) {
            nextIdx = if (_isRepeat.value) 0 else return
        }

        _queueIndex.value = nextIdx.coerceIn(0, q.size - 1)
        val nextSong = q.getOrNull(_queueIndex.value) ?: return
        playSong(nextSong)
    }

    fun skipPrevious() {
        val now = System.currentTimeMillis()
        if (now - lastPlaybackActionTimestamp < 80L) return
        lastPlaybackActionTimestamp = now

        val q = _queue.value
        if (q.isEmpty()) return

        if (_currentPositionMs.value > 3000) {
            seekTo(0)
            return
        }

        var prevIdx = _queueIndex.value - 1
        if (prevIdx < 0) {
            prevIdx = (q.size - 1).coerceAtLeast(0)
        }
        _queueIndex.value = prevIdx.coerceIn(0, q.size - 1)
        val prevSong = q.getOrNull(_queueIndex.value) ?: return
        playSong(prevSong)
    }

    fun seekTo(positionMs: Long) {
        _currentPositionMs.value = positionMs
        playbackService?.playerEngine?.seekTo(positionMs)
        persistCurrentPlaybackState()
    }

    fun playNext(song: Song) {
        playNext(listOf(song))
    }

    fun playNext(songs: List<Song>) {
        if (songs.isEmpty()) return
        val currentQ = _queue.value.toMutableList()
        if (currentQ.isEmpty()) {
            setupQueue(songs, playFirst = true)
            return
        }
        val insertIndex = (_queueIndex.value + 1).coerceAtMost(currentQ.size)
        currentQ.addAll(insertIndex, songs)
        _queue.value = currentQ
        multiQueueManager.updateCurrentQueue(_queue.value, _queueIndex.value)
        saveMultiQueuesToPrefs()
        playbackService?.playerEngine?.invalidateGaplessNextTrack()
    }

    fun addToQueue(song: Song) {
        addToQueue(listOf(song))
    }

    fun addToQueue(songs: List<Song>) {
        if (songs.isEmpty()) return
        val currentQ = _queue.value.toMutableList()
        if (currentQ.isEmpty()) {
            setupQueue(songs, playFirst = true)
            return
        }
        currentQ.addAll(songs)
        _queue.value = currentQ
        multiQueueManager.updateCurrentQueue(_queue.value, _queueIndex.value)
        saveMultiQueuesToPrefs()
        playbackService?.playerEngine?.invalidateGaplessNextTrack()
    }

    fun setupQueue(songs: List<Song>, playFirst: Boolean = true) {
        if (songs.isEmpty()) return
        val currentQ = _queue.value
        if (currentQ.isNotEmpty() && !multiQueueManager.isSameQueueList(currentQ, songs)) {
            multiQueueManager.updateCurrentQueue(currentQ, _queueIndex.value)
            val label = songs.firstOrNull()?.album?.ifBlank { "Custom Queue" } ?: "Custom Queue"
            multiQueueManager.allocateSlotForNewQueue(songs, 0, label)
        } else {
            multiQueueManager.updateCurrentQueue(songs, 0)
        }
        _queue.value = songs
        _queueIndex.value = 0
        saveMultiQueuesToPrefs()
        if (playFirst) {
            playSong(songs.first(), songs, switchSlotIfNeeded = false)
        }
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
        if (_isShuffle.value) {
            _queue.value = _queue.value.shuffled()
        }
        playbackService?.playerEngine?.invalidateGaplessNextTrack()
        persistCurrentPlaybackState()
    }

    fun toggleRepeat() {
        _isRepeat.value = !_isRepeat.value
        playbackService?.playerEngine?.invalidateGaplessNextTrack()
        persistCurrentPlaybackState()
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            val newFav = !song.isFavorite
            repository.toggleFavorite(song.id, newFav)
            if (_currentSong.value?.id == song.id) {
                _currentSong.value = _currentSong.value?.copy(isFavorite = newFav)
            }
        }
    }

    // Equalizer controls
    fun setEqualizerEnabled(enabled: Boolean) {
        playbackService?.playerEngine?.setEqualizerEnabled(enabled)
    }

    fun setBandLevel(bandIndex: Int, levelMb: Int) {
        playbackService?.playerEngine?.setBandLevel(bandIndex, levelMb)
    }

    fun setEqualizerBandMode(bandMode: Int) {
        playbackService?.playerEngine?.setBandMode(bandMode)
    }

    fun applyEqualizerPreset(preset: String) {
        playbackService?.playerEngine?.applyPreset(preset)
    }

    fun setBassBoost(enabled: Boolean, strength: Int) {
        playbackService?.playerEngine?.setBassBoost(enabled, strength)
    }

    fun setStereoWidth(enabled: Boolean, percent: Int) {
        playbackService?.playerEngine?.setStereoWidth(enabled, percent)
    }

    fun setVirtualizer(enabled: Boolean, strength: Int) {
        playbackService?.playerEngine?.setVirtualizer(enabled, strength)
    }

    fun setCrossfade(seconds: Float) {
        playbackService?.playerEngine?.setCrossfadeSeconds(seconds)
        _appSettings.value = _appSettings.value.copy(crossfadeDurationSec = seconds)
    }

    fun setParametricEqEnabled(enabled: Boolean) {
        playbackService?.playerEngine?.setParametricEnabled(enabled)
    }

    fun applyAutoEqProfile(profile: com.example.player.AutoEqDatabase.AutoEqProfile) {
        playbackService?.playerEngine?.applyAutoEqProfile(profile)
    }

    fun setReverbEnabled(enabled: Boolean) {
        playbackService?.playerEngine?.setReverbEnabled(enabled)
    }

    fun setReverbPreset(preset: String) {
        playbackService?.playerEngine?.setReverbPreset(preset)
    }

    fun setReverbRoomSize(size: Float) {
        playbackService?.playerEngine?.setReverbRoomSize(size)
    }

    fun setReverbDecayMs(decayMs: Int) {
        playbackService?.playerEngine?.setReverbDecayMs(decayMs)
    }

    fun setReverbDamping(damping: Float) {
        playbackService?.playerEngine?.setReverbDamping(damping)
    }

    fun setReverbWetLevel(wetLevel: Float) {
        playbackService?.playerEngine?.setReverbWetLevel(wetLevel)
    }

    fun setReverbLoudspeakerBass(enabled: Boolean, punchStrength: Int) {
        playbackService?.playerEngine?.setReverbLoudspeakerBass(enabled, punchStrength)
    }

    fun startMixTransitionPreview(
        outgoingSong: Song,
        incomingSong: Song?,
        transitionDurationSec: Float,
        startProgress: Float = 0f,
        transitionProfile: com.example.data.db.PlaylistTransitionEntity? = null
    ) {
        val baseQueue = _queue.value.ifEmpty { allSongs.value }
        val outgoingIdxInQueue = baseQueue.indexOfFirst { it.id == outgoingSong.id }
        val pairedQueue = buildList {
            if (outgoingIdxInQueue >= 0) {
                // Keep everything before outgoingSong so queue history is intact
                for (i in 0..outgoingIdxInQueue) {
                    add(baseQueue[i])
                }
                if (incomingSong != null && incomingSong.id != outgoingSong.id) {
                    add(incomingSong)
                }
                // Append the remainder of the active playlist/queue in its exact order
                for (i in (outgoingIdxInQueue + 1) until baseQueue.size) {
                    val s = baseQueue[i]
                    if (s.id != outgoingSong.id && s.id != incomingSong?.id) {
                        add(s)
                    }
                }
            } else {
                add(outgoingSong)
                if (incomingSong != null && incomingSong.id != outgoingSong.id) {
                    add(incomingSong)
                }
                baseQueue.forEach { song ->
                    if (song.id != outgoingSong.id && song.id != incomingSong?.id) {
                        add(song)
                    }
                }
            }
            // Ensure any remaining library tracks are available if baseQueue was tiny
            if (size < 3) {
                allSongs.value.forEach { song ->
                    if (none { it.id == song.id }) {
                        add(song)
                    }
                }
            }
        }
        val targetOutIndex = pairedQueue.indexOfFirst { it.id == outgoingSong.id }.coerceAtLeast(0)
        val effectiveCrossfade = transitionDurationSec.coerceIn(2f, 45f)
        setCrossfade(effectiveCrossfade)
        _queue.value = pairedQueue
        _queueIndex.value = targetOutIndex
        multiQueueManager.updateCurrentQueue(_queue.value, _queueIndex.value)
        saveMultiQueuesToPrefs()

        val transitionMs = (effectiveCrossfade * 1000L).toLong()
            .coerceAtMost((outgoingSong.durationMs * 0.75f).toLong().coerceAtLeast(4000L))
        val transitionStartMs = (outgoingSong.durationMs - transitionMs).coerceAtLeast(0L)
        val targetSeekMs = (transitionStartMs + (transitionMs * startProgress.coerceIn(0f, 0.92f)).toLong())
            .coerceIn(0L, (outgoingSong.durationMs - 1000L).coerceAtLeast(0L))

        // Set transition profile and activate zero-latency pre-fader loop preview mode
        playbackService?.playerEngine?.activeTransitionProfile = transitionProfile
        playbackService?.playerEngine?.setPreFaderLoopMode(true, transitionStartMs, transitionMs)

        if (_currentSong.value?.id != outgoingSong.id) {
            _currentSong.value = outgoingSong
            playbackService?.playSong(outgoingSong)
            viewModelScope.launch {
                kotlinx.coroutines.delay(160L)
                seekTo(targetSeekMs)
            }
        } else {
            seekTo(targetSeekMs)
            if (!_isPlaying.value) {
                playbackService?.playerEngine?.resume()
            }
        }
    }

    fun stopMixTransitionPreview() {
        playbackService?.playerEngine?.setPreFaderLoopMode(false)
    }

    fun savePlaylistTransition(transition: com.example.data.db.PlaylistTransitionEntity) {
        viewModelScope.launch {
            playlistTransitionDao.saveTransition(transition)
            playbackService?.playerEngine?.activeTransitionProfile = transition
        }
    }

    fun seekMixTransitionPreview(
        outgoingSong: Song,
        transitionDurationSec: Float,
        progress: Float
    ) {
        if (_currentSong.value?.id == outgoingSong.id) {
            val effectiveCrossfade = transitionDurationSec.coerceIn(2f, 45f)
            val transitionMs = (effectiveCrossfade * 1000L).toLong()
                .coerceAtMost((outgoingSong.durationMs * 0.75f).toLong().coerceAtLeast(4000L))
            val transitionStartMs = (outgoingSong.durationMs - transitionMs).coerceAtLeast(0L)
            val targetSeekMs = (transitionStartMs + (transitionMs * progress.coerceIn(0f, 0.96f)).toLong())
                .coerceIn(0L, (outgoingSong.durationMs - 500L).coerceAtLeast(0L))
            seekTo(targetSeekMs)
        }
    }

    fun setAudioMuted(muted: Boolean) {
        playbackService?.playerEngine?.setSleepTimerVolumeMultiplier(if (muted) 0f else 1f)
    }

    fun setGapless(enabled: Boolean) {
        playbackService?.playerEngine?.setGapless(enabled)
        _appSettings.value = _appSettings.value.copy(gaplessPlayback = enabled)
    }

    fun setReplayGain(enabled: Boolean) {
        playbackService?.playerEngine?.setReplayGain(enabled)
        _appSettings.value = _appSettings.value.copy(replayGain = enabled)
    }

    // Settings
    fun setSettingsDialogVisible(visible: Boolean) {
        _showSettingsDialog.value = visible
    }

    fun setQueueSheetVisible(visible: Boolean) {
        _showQueueSheet.value = visible
    }

    fun setFoldersDialogVisible(visible: Boolean) {
        _showFoldersDialog.value = visible
    }

    fun setStatsDialogVisible(visible: Boolean) {
        _showStatsDialog.value = visible
    }

    fun setAboutDialogVisible(visible: Boolean) {
        _showAboutDialog.value = visible
    }

    fun setChangelogDialogVisible(visible: Boolean) {
        _showChangelogDialog.value = visible
    }

    fun setEqualizerDialogVisible(visible: Boolean) {
        _showEqualizerDialog.value = visible
    }

    // Dialog Controls
    fun setPrecisionPlaybackDialogVisible(visible: Boolean) {
        _showPrecisionPlaybackDialog.value = visible
    }

    fun setSleepTimerDialogVisible(visible: Boolean) {
        _showSleepTimerDialog.value = visible
    }

    fun setTagEditorDialogVisible(visible: Boolean) {
        _showTagEditorDialog.value = visible
    }

    // Precision Playback: Speed & Pitch
    fun setPlaybackSpeed(speed: Float) {
        precisionPlaybackController.setPlaybackSpeed(speed)
        playbackService?.playerEngine?.setPlaybackSpeedAndPitch(
            speed = speed,
            pitchSemiTones = loopState.value.pitchSemiTones
        )
    }

    fun setPlaybackPitch(pitch: Float) {
        precisionPlaybackController.setPitch(pitch)
        playbackService?.playerEngine?.setPlaybackSpeedAndPitch(
            speed = loopState.value.playbackSpeed,
            pitchSemiTones = pitch
        )
    }

    // Precision Playback: A-B Loop
    fun setLoopPointA(ms: Long) {
        precisionPlaybackController.setPointA(ms)
    }

    fun setLoopPointB(ms: Long) {
        precisionPlaybackController.setPointB(ms)
    }

    fun setAbLoopEnabled(enabled: Boolean) {
        precisionPlaybackController.toggleLoop(enabled)
    }

    fun clearAbLoop() {
        precisionPlaybackController.clearLoop()
    }

    // Smart Sleep Timer
    fun startSleepTimer(minutes: Int, fadeDurationSec: Int, stopAtTrackEnd: Boolean = false) {
        proSleepTimer.startTimer(minutes = minutes, stopAtTrackEnd = stopAtTrackEnd, fadeSecs = fadeDurationSec)
    }

    fun cancelSleepTimer() {
        proSleepTimer.cancelTimer()
        playbackService?.playerEngine?.setSleepTimerVolumeMultiplier(1.0f)
    }

    // Custom Playlists Operations
    fun setCreatePlaylistDialogVisible(visible: Boolean) {
        _showCreatePlaylistDialog.value = visible
    }

    fun openAddToPlaylist(song: Song) {
        _songForAddToPlaylist.value = song
    }

    fun closeAddToPlaylist() {
        _songForAddToPlaylist.value = null
    }

    fun openPlaylistDetail(playlist: CustomPlaylistEntity) {
        _selectedPlaylistForDetail.value = playlist
    }

    fun closePlaylistDetail() {
        _selectedPlaylistForDetail.value = null
    }

    fun createPlaylist(name: String, initialSongs: List<Song> = emptyList()) {
        viewModelScope.launch {
            val id = "playlist_${System.currentTimeMillis()}"
            val csv = initialSongs.joinToString(",") { it.id.toString() }
            repository.savePlaylist(CustomPlaylistEntity(id = id, name = name, songIdsCsv = csv))
        }
    }

    fun createSmartPlaylist(name: String, config: com.example.data.engine.SmartPlaylistConfig) {
        viewModelScope.launch {
            val id = "smart_playlist_${System.currentTimeMillis()}"
            val json = com.example.data.engine.SmartPlaylistEngine.serializeConfig(config)
            repository.savePlaylist(
                CustomPlaylistEntity(
                    id = id,
                    name = name,
                    songIdsCsv = "SMART_RULES:$json"
                )
            )
        }
    }

    fun addSongToPlaylist(playlistId: String, song: Song) {
        viewModelScope.launch {
            val current = customPlaylists.value.find { it.id == playlistId } ?: return@launch
            val ids = current.getSongIds().toMutableList()
            if (!ids.contains(song.id)) {
                ids.add(song.id)
                repository.savePlaylist(current.copy(songIdsCsv = ids.joinToString(",")))
            }
        }
    }

    fun removeSongFromPlaylist(playlistId: String, songId: Long) {
        viewModelScope.launch {
            val current = customPlaylists.value.find { it.id == playlistId } ?: return@launch
            val ids = current.getSongIds().filter { it != songId }
            val updated = current.copy(songIdsCsv = ids.joinToString(","))
            repository.savePlaylist(updated)
            if (_selectedPlaylistForDetail.value?.id == playlistId) {
                _selectedPlaylistForDetail.value = updated
            }
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            if (_selectedPlaylistForDetail.value?.id == playlistId) {
                _selectedPlaylistForDetail.value = null
            }
        }
    }

    fun playPlaylist(playlist: CustomPlaylistEntity, startSong: Song? = null, shuffle: Boolean = false) {
        val tracks = playlist.resolveSongs(allSongs.value)
        if (tracks.isEmpty()) return

        if (shuffle) {
            val shuffled = tracks.shuffled()
            playSong(shuffled.first(), shuffled)
        } else {
            val songToPlay = startSong ?: tracks.first()
            playSong(songToPlay, tracks)
        }
    }

    // Multi-Queue Management
    fun switchQueueSlot(slotIndex: Int, startSongIndex: Int? = null) {
        // Save current queue into current slot first
        val currentSlot = multiQueueManager.activeQueueSlotIndex.value
        if (_queue.value.isNotEmpty()) {
            multiQueueManager.updateQueue(currentSlot, _queue.value, _queueIndex.value)
        }

        // Switch to new slot
        multiQueueManager.switchToSlot(slotIndex)
        val targetQueue = multiQueueManager.getCurrentQueue()
        val targetIndex = startSongIndex ?: multiQueueManager.getCurrentIndex()
        if (targetQueue.isNotEmpty()) {
            _queue.value = targetQueue
            _queueIndex.value = targetIndex.coerceIn(0, targetQueue.size - 1)
            multiQueueManager.updateCurrentQueue(_queue.value, _queueIndex.value)
            val songToPlay = targetQueue[_queueIndex.value]
            playSong(songToPlay, switchSlotIfNeeded = false)
        } else {
            _queue.value = emptyList()
            _queueIndex.value = 0
        }
        saveMultiQueuesToPrefs()
    }

    fun addSongsToQueueSlot(slotIndex: Int, songs: List<Song>) {
        if (songs.isEmpty()) return
        if (slotIndex == multiQueueManager.activeQueueSlotIndex.value && _queue.value.isNotEmpty()) {
            multiQueueManager.updateQueue(slotIndex, _queue.value, _queueIndex.value)
        }
        multiQueueManager.addSongsToSlot(slotIndex, songs)
        if (slotIndex == multiQueueManager.activeQueueSlotIndex.value) {
            val updated = multiQueueManager.getCurrentQueue()
            _queue.value = updated
            if (_currentSong.value == null && updated.isNotEmpty()) {
                _queueIndex.value = 0
                playSong(updated.first(), updated)
            }
        }
        saveMultiQueuesToPrefs()
    }

    fun removeSongFromQueueSlot(slotIndex: Int, itemIndex: Int) {
        if (slotIndex == multiQueueManager.activeQueueSlotIndex.value && _queue.value.isNotEmpty()) {
            multiQueueManager.updateQueue(slotIndex, _queue.value, _queueIndex.value)
        }
        multiQueueManager.removeSongAtFromSlot(slotIndex, itemIndex)
        if (slotIndex == multiQueueManager.activeQueueSlotIndex.value) {
            _queue.value = multiQueueManager.getCurrentQueue()
            _queueIndex.value = multiQueueManager.getCurrentIndex()
        }
        saveMultiQueuesToPrefs()
    }

    fun moveSongInQueueSlot(slotIndex: Int, fromIndex: Int, toIndex: Int) {
        if (slotIndex == multiQueueManager.activeQueueSlotIndex.value && _queue.value.isNotEmpty()) {
            multiQueueManager.updateQueue(slotIndex, _queue.value, _queueIndex.value)
        }
        multiQueueManager.moveSongInSlot(slotIndex, fromIndex, toIndex)
        if (slotIndex == multiQueueManager.activeQueueSlotIndex.value) {
            _queue.value = multiQueueManager.getCurrentQueue()
            _queueIndex.value = multiQueueManager.getCurrentIndex()
        }
        saveMultiQueuesToPrefs()
    }

    fun shuffleQueueSlot(slotIndex: Int) {
        if (slotIndex == multiQueueManager.activeQueueSlotIndex.value && _queue.value.isNotEmpty()) {
            multiQueueManager.updateQueue(slotIndex, _queue.value, _queueIndex.value)
        }
        multiQueueManager.shuffleSlot(slotIndex)
        if (slotIndex == multiQueueManager.activeQueueSlotIndex.value) {
            _queue.value = multiQueueManager.getCurrentQueue()
            _queueIndex.value = multiQueueManager.getCurrentIndex()
        }
        saveMultiQueuesToPrefs()
    }

    fun duplicateQueueSlot(fromSlotIndex: Int, toSlotIndex: Int) {
        if (fromSlotIndex == multiQueueManager.activeQueueSlotIndex.value && _queue.value.isNotEmpty()) {
            multiQueueManager.updateQueue(fromSlotIndex, _queue.value, _queueIndex.value)
        }
        multiQueueManager.duplicateSlot(fromSlotIndex, toSlotIndex)
        if (toSlotIndex == multiQueueManager.activeQueueSlotIndex.value) {
            _queue.value = multiQueueManager.getCurrentQueue()
            _queueIndex.value = multiQueueManager.getCurrentIndex()
        }
        saveMultiQueuesToPrefs()
    }

    fun renameQueueSlot(slotIndex: Int, newName: String) {
        multiQueueManager.renameQueue(slotIndex, newName)
        saveMultiQueuesToPrefs()
    }

    fun clearQueueSlot(slotIndex: Int) {
        multiQueueManager.clearQueue(slotIndex)
        if (multiQueueManager.activeQueueSlotIndex.value == slotIndex) {
            _queue.value = emptyList()
            _queueIndex.value = 0
        }
        saveMultiQueuesToPrefs()
    }

    // Tag Editor
    fun saveSongTags(songId: Long, newTitle: String, newArtist: String, newAlbum: String, newTrackNumber: Int?) {
        viewModelScope.launch {
            TagEditorManager.updateSongTags(
                context = getApplication(),
                songDao = db.songDao(),
                songId = songId,
                newTitle = newTitle,
                newArtist = newArtist,
                newAlbum = newAlbum,
                newTrackNumber = newTrackNumber
            )
            // If currently playing song is updated, sync current song state
            val current = _currentSong.value
            if (current != null && current.id == songId) {
                _currentSong.value = current.copy(
                    title = newTitle,
                    artist = newArtist,
                    album = newAlbum,
                    trackNumber = newTrackNumber ?: current.trackNumber
                )
            }
        }
    }

    fun selectAlbum(album: Album?) {
        _selectedAlbum.value = album
    }

    fun updateSettings(newSettings: AppSettings) {
        val oldGapless = _appSettings.value.gaplessPlayback
        val oldCrossfade = _appSettings.value.crossfadeDurationSec
        val oldReplayGain = _appSettings.value.replayGain
        _appSettings.value = newSettings
        settingsManager.saveSettings(newSettings)
        if (oldGapless != newSettings.gaplessPlayback) {
            playbackService?.playerEngine?.setGapless(newSettings.gaplessPlayback)
        }
        if (oldCrossfade != newSettings.crossfadeDurationSec) {
            playbackService?.playerEngine?.setCrossfadeSeconds(newSettings.crossfadeDurationSec)
        }
        if (oldReplayGain != newSettings.replayGain) {
            playbackService?.playerEngine?.setReplayGain(newSettings.replayGain)
        }
        viewModelScope.launch {
            extractDynamicColorIfNeeded(_currentSong.value)
        }
    }

    fun triggerRescan(forceDeepScan: Boolean = true) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                _isScanning.value = true
                repository.refreshLibrary(getApplication(), _appSettings.value.excludedFolders, forceDeepScan)
            } catch (e: Exception) {
                android.util.Log.e("MusicViewModel", "triggerRescan failed: ${e.message}", e)
            } finally {
                _isScanning.value = false
            }
        }
    }

    private var mediaStoreObserver: ContentObserver? = null

    private fun registerMediaStoreObserver() {
        try {
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                private var debounceJob: kotlinx.coroutines.Job? = null
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    super.onChange(selfChange, uri)
                    debounceJob?.cancel()
                    debounceJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        kotlinx.coroutines.delay(1200L) // Debounce rapid multi-file copy/download additions
                        triggerRescan(forceDeepScan = false)
                    }
                }
            }
            mediaStoreObserver = observer
            getApplication<Application>().contentResolver.registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true,
                observer
            )
        } catch (e: Exception) {
            android.util.Log.w("MusicViewModel", "Failed to register MediaStore ContentObserver: ${e.message}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        persistCurrentPlaybackState()
        mediaStoreObserver?.let {
            try {
                getApplication<Application>().contentResolver.unregisterContentObserver(it)
            } catch (_: Throwable) {}
        }
        if (isServiceBound) {
            getApplication<Application>().unbindService(serviceConnection)
            isServiceBound = false
        }
    }
}
