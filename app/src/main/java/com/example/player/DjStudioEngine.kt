package com.example.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.media.SoundPool
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

data class DjDeckState(
    val deckNumber: Int,
    val song: Song? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bpm: Float = 120.0f,
    val originalBpm: Float = 120.0f,
    val pitchPercent: Float = 0.0f, // -16% .. +16%
    val pitchBend: Float = 0.0f,   // temporary nudge
    val faderVolume: Float = 1.0f,  // 0.0 .. 1.0
    val isSyncActive: Boolean = false,
    val cuePositionMs: Long = 0L,
    val hotCues: Map<Int, Long> = emptyMap(), // slot 1..4 -> positionMs
    val isLoopActive: Boolean = false,
    val loopStartMs: Long = 0L,
    val loopDurationMs: Long = 0L,
    val activeLoopBeats: Float = 4.0f,
    val isRollActive: Boolean = false,
    val rollResumeMs: Long = 0L,
    val isScratching: Boolean = false,
    val platterAngleDeg: Float = 0.0f,
    val pflEnabled: Boolean = false,
    val vuLevel: Float = 0.0f,
    val waveformPeaks: FloatArray = FloatArray(100) { 0.25f }
)

enum class DjTab {
    AUDIO, VIDEO, WAVES, AMP
}

class DjStudioEngine(private val context: Context) {
    private val TAG = "DjStudioEngine"
    private val engineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var deck1Player: MediaPlayer? = null
    private var deck2Player: MediaPlayer? = null

    private val _deck1State = MutableStateFlow(DjDeckState(deckNumber = 1))
    val deck1State: StateFlow<DjDeckState> = _deck1State.asStateFlow()

    private val _deck2State = MutableStateFlow(DjDeckState(deckNumber = 2))
    val deck2State: StateFlow<DjDeckState> = _deck2State.asStateFlow()

    // Master Mixer State
    private val _crossfader = MutableStateFlow(0.0f) // 0.0 = full Deck 1, 0.5 = center, 1.0 = full Deck 2
    val crossfader: StateFlow<Float> = _crossfader.asStateFlow()

    private val _masterGain = MutableStateFlow(1.0f)
    val masterGain: StateFlow<Float> = _masterGain.asStateFlow()

    private val _activeTab = MutableStateFlow(DjTab.AUDIO)
    val activeTab: StateFlow<DjTab> = _activeTab.asStateFlow()

    private val _isAutoFading = MutableStateFlow(false)
    val isAutoFading: StateFlow<Boolean> = _isAutoFading.asStateFlow()

    private val _masterVuLevel = MutableStateFlow(0.0f)
    val masterVuLevel: StateFlow<Float> = _masterVuLevel.asStateFlow()

    private var autoFadeJob: Job? = null
    private var pollJob: Job? = null

    // Sampler SoundPool for DJ drops
    private var soundPool: SoundPool? = null
    private val sampleIds = mutableMapOf<Int, Int>()

    init {
        initSoundPool()
    }

    private fun initSoundPool() {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            soundPool = SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(audioAttributes)
                .build()
        } catch (e: Exception) {
            Log.w(TAG, "SoundPool init failed: ${e.message}")
        }
    }

    fun setActiveTab(tab: DjTab) {
        _activeTab.value = tab
    }

    // Active Queue Tracking & Auto-Advancement
    private var activeQueue: List<Song> = emptyList()
    private var currentQueueIndex: Int = 0
    private var deck1QueueIndex: Int = 0
    private var deck2QueueIndex: Int = 1
    private var activePlaybackDeck: Int = 1

    var onQueueAdvanced: ((newIndex: Int, activeSong: Song) -> Unit)? = null

    private val _queueDisplayInfo = MutableStateFlow("")
    val queueDisplayInfo: StateFlow<String> = _queueDisplayInfo.asStateFlow()

    /**
     * Activates DJ Mode seamlessly:
     * - Queue remains 100% intact
     * - Deck 1 is mapped to currently playing track maintaining exact millisecond position
     * - Deck 2 automatically pre-loads the next upcoming track ready for crossfading
     */
    fun activate(
        currentSong: Song?,
        currentPosMs: Long,
        wasPlaying: Boolean,
        queue: List<Song>,
        queueIndex: Int,
        allSongs: List<Song>
    ) {
        activeQueue = if (queue.isNotEmpty()) queue else allSongs
        currentQueueIndex = queueIndex.coerceIn(0, maxOf(0, activeQueue.size - 1))
        deck1QueueIndex = currentQueueIndex
        deck2QueueIndex = if (activeQueue.size > 1) (currentQueueIndex + 1) % activeQueue.size else currentQueueIndex
        activePlaybackDeck = 1

        val deck1Song = activeQueue.getOrNull(deck1QueueIndex) ?: currentSong
        val deck2Song = activeQueue.getOrNull(deck2QueueIndex) ?: allSongs.firstOrNull { it.id != deck1Song?.id }

        // Synchronously load Deck 1 with the currently active track
        if (deck1Song != null) {
            loadTrack(1, deck1Song, startPlaying = wasPlaying, initialPosMs = currentPosMs)
        }

        // Synchronously pre-load Deck 2 with the next upcoming track ready for instant crossfading
        if (deck2Song != null) {
            loadTrack(2, deck2Song, startPlaying = false, initialPosMs = 0L)
        }

        _crossfader.value = if (wasPlaying) 0.0f else 0.5f
        updateVolumes()
        updateQueueDisplay()
        startTelemetryPolling()
    }

    fun deactivate(): Triple<Song?, Long, Int> {
        stopTelemetryPolling()
        autoFadeJob?.cancel()

        val isDeck2Active = _crossfader.value > 0.5f || (!_deck1State.value.isPlaying && _deck2State.value.isPlaying)

        val activeDeckSong = if (isDeck2Active && _deck2State.value.song != null) {
            _deck2State.value.song
        } else {
            _deck1State.value.song
        }

        val activeDeckPos = if (isDeck2Active) {
            _deck2State.value.currentPositionMs
        } else {
            _deck1State.value.currentPositionMs
        }

        val activeIndex = if (isDeck2Active) deck2QueueIndex else deck1QueueIndex

        releasePlayer(1)
        releasePlayer(2)

        return Triple(activeDeckSong, activeDeckPos, activeIndex)
    }

    /**
     * Synchronously prepares and maps audio with zero delay or buffering.
     */
    fun loadTrack(deckNumber: Int, song: Song, startPlaying: Boolean = false, initialPosMs: Long = 0L) {
        val meta = AutomixDspEngine.analyzeTrack(song)
        val initialBpm = meta.bpm.toFloat()

        // Immediate cached or synthetic waveform peaks for zero-latency UI rendering
        val cachedPeaks = PcmWaveformExtractor.getCachedProfile(song.id)?.seekbarPeaks
            ?: FloatArray(100) { 0.2f + ((it * 17) % 70) / 100f }

        if (deckNumber == 1) {
            _deck1State.value = _deck1State.value.copy(
                song = song,
                durationMs = song.durationMs,
                bpm = initialBpm,
                originalBpm = initialBpm,
                pitchPercent = 0.0f,
                pitchBend = 0.0f,
                cuePositionMs = initialPosMs,
                waveformPeaks = cachedPeaks
            )
        } else {
            _deck2State.value = _deck2State.value.copy(
                song = song,
                durationMs = song.durationMs,
                bpm = initialBpm,
                originalBpm = initialBpm,
                pitchPercent = 0.0f,
                pitchBend = 0.0f,
                cuePositionMs = initialPosMs,
                waveformPeaks = cachedPeaks
            )
        }

        // Synchronously prepare native MediaPlayer without background delay
        preparePlayer(deckNumber, song, startPlaying, initialPosMs)
        updateVolumes()
        updateQueueDisplay()

        // Background update for high-res real audio waveform if not yet in cache
        if (PcmWaveformExtractor.getCachedProfile(song.id) == null) {
            engineScope.launch(Dispatchers.IO) {
                try {
                    val profile = PcmWaveformExtractor.extractWaveformProfile(context, song)
                    engineScope.launch(Dispatchers.Main) {
                        if (deckNumber == 1 && _deck1State.value.song?.id == song.id) {
                            _deck1State.value = _deck1State.value.copy(waveformPeaks = profile.seekbarPeaks)
                        } else if (deckNumber == 2 && _deck2State.value.song?.id == song.id) {
                            _deck2State.value = _deck2State.value.copy(waveformPeaks = profile.seekbarPeaks)
                        }
                    }
                } catch (_: Throwable) {}
            }
        }
    }

    private fun getPlayer(deckNumber: Int): MediaPlayer? =
        if (deckNumber == 1) deck1Player else deck2Player

    private fun preparePlayer(deckNumber: Int, song: Song, startPlaying: Boolean, initialPosMs: Long) {
        val existing = getPlayer(deckNumber)
        try {
            existing?.stop()
            existing?.release()
        } catch (_: Throwable) {}

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                if (song.path.isNotEmpty() && File(song.path).exists()) {
                    setDataSource(song.path)
                } else {
                    setDataSource(context, Uri.parse(song.uri))
                }
                prepare()
                if (initialPosMs > 0L) {
                    seekTo(initialPosMs.toInt().coerceAtMost(duration))
                }
                setOnCompletionListener {
                    if (deckNumber == 1) {
                        _deck1State.value = _deck1State.value.copy(isPlaying = false, currentPositionMs = 0L)
                    } else {
                        _deck2State.value = _deck2State.value.copy(isPlaying = false, currentPositionMs = 0L)
                    }
                    handleDeckCompleted(deckNumber)
                }
            }

            if (deckNumber == 1) {
                deck1Player = player
                if (startPlaying) {
                    player.start()
                    _deck1State.value = _deck1State.value.copy(isPlaying = true)
                }
            } else {
                deck2Player = player
                if (startPlaying) {
                    player.start()
                    _deck2State.value = _deck2State.value.copy(isPlaying = true)
                }
            }
            applyPlaybackParams(deckNumber)
            updateVolumes()
        } catch (e: Exception) {
            Log.e(TAG, "preparePlayer failed for Deck $deckNumber: ${e.message}", e)
        }
    }

    private fun updateQueueDisplay() {
        if (activeQueue.isEmpty()) return
        val nextDeck = if (activePlaybackDeck == 1) 2 else 1
        val nextSong = if (activePlaybackDeck == 1) _deck2State.value.song else _deck1State.value.song
        _queueDisplayInfo.value = "QUEUE #${currentQueueIndex + 1}/${activeQueue.size} • Deck $nextDeck: ${nextSong?.title?.take(22) ?: "Ready"}"
    }

    /**
     * Automatic Queue Advancement:
     * When one deck completes, the idle deck is automatically loaded with the next upcoming
     * track in the playlist queue, keeping the music playing continuously.
     */
    private fun handleDeckCompleted(completedDeck: Int) {
        if (activeQueue.isEmpty()) return

        val otherDeck = if (completedDeck == 1) 2 else 1
        val otherState = if (otherDeck == 1) _deck1State.value else _deck2State.value
        val otherPlayer = getPlayer(otherDeck)

        if (otherState.isPlaying) {
            // Other deck is already taking over the mix.
            activePlaybackDeck = otherDeck
            currentQueueIndex = if (otherDeck == 1) deck1QueueIndex else deck2QueueIndex
            otherState.song?.let { onQueueAdvanced?.invoke(currentQueueIndex, it) }

            // Pull next track down the queue onto the idle deck
            val nextQueueIdx = (currentQueueIndex + 1) % activeQueue.size
            if (completedDeck == 1) {
                deck1QueueIndex = nextQueueIdx
                loadTrack(1, activeQueue[nextQueueIdx], startPlaying = false, initialPosMs = 0L)
            } else {
                deck2QueueIndex = nextQueueIdx
                loadTrack(2, activeQueue[nextQueueIdx], startPlaying = false, initialPosMs = 0L)
            }
        } else {
            // Other deck is pre-loaded but idle: auto-start it so playback never drops out
            if (otherPlayer != null && otherState.song != null) {
                otherPlayer.start()
                applyPlaybackParams(otherDeck)
                if (otherDeck == 1) {
                    _deck1State.value = _deck1State.value.copy(isPlaying = true)
                    _crossfader.value = 0.0f
                } else {
                    _deck2State.value = _deck2State.value.copy(isPlaying = true)
                    _crossfader.value = 1.0f
                }
                updateVolumes()

                activePlaybackDeck = otherDeck
                currentQueueIndex = if (otherDeck == 1) deck1QueueIndex else deck2QueueIndex
                otherState.song?.let { onQueueAdvanced?.invoke(currentQueueIndex, it) }

                // Pull next track onto newly completed deck
                val nextQueueIdx = (currentQueueIndex + 1) % activeQueue.size
                if (completedDeck == 1) {
                    deck1QueueIndex = nextQueueIdx
                    loadTrack(1, activeQueue[nextQueueIdx], startPlaying = false, initialPosMs = 0L)
                } else {
                    deck2QueueIndex = nextQueueIdx
                    loadTrack(2, activeQueue[nextQueueIdx], startPlaying = false, initialPosMs = 0L)
                }
            }
        }
        updateQueueDisplay()
    }

    fun togglePlayPause(deckNumber: Int) {
        val player = if (deckNumber == 1) deck1Player else deck2Player
        val state = if (deckNumber == 1) _deck1State.value else _deck2State.value

        if (player == null || state.song == null) return

        try {
            if (player.isPlaying) {
                player.pause()
                updateDeckState(deckNumber) { it.copy(isPlaying = false) }
            } else {
                player.start()
                applyPlaybackParams(deckNumber)
                updateDeckState(deckNumber) { it.copy(isPlaying = true) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "togglePlayPause error: ${e.message}")
        }
    }

    /**
     * Standard DJ CUE Behavior:
     * - If track is playing: Jumps to Cue point and pauses.
     * - If track is paused: Sets Cue point at current position.
     */
    fun triggerCue(deckNumber: Int) {
        val player = if (deckNumber == 1) deck1Player else deck2Player
        val state = if (deckNumber == 1) _deck1State.value else _deck2State.value

        if (player == null || state.song == null) return

        try {
            if (player.isPlaying) {
                player.pause()
                player.seekTo(state.cuePositionMs.toInt())
                updateDeckState(deckNumber) {
                    it.copy(isPlaying = false, currentPositionMs = state.cuePositionMs)
                }
            } else {
                val newCueMs = player.currentPosition.toLong()
                updateDeckState(deckNumber) { it.copy(cuePositionMs = newCueMs) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "triggerCue error: ${e.message}")
        }
    }

    /**
     * Instant BPM Sync: Matches target deck's tempo to master deck's active BPM.
     */
    fun syncDeck(targetDeck: Int) {
        val masterDeck = if (targetDeck == 1) 2 else 1
        val masterState = if (masterDeck == 1) _deck1State.value else _deck2State.value
        val targetState = if (targetDeck == 1) _deck1State.value else _deck2State.value

        if (masterState.song == null || targetState.song == null) return

        val masterBpm = masterState.bpm
        val targetOrigBpm = targetState.originalBpm

        if (targetOrigBpm <= 0f) return

        // Compute needed pitch percentage to match master BPM
        val ratio = masterBpm / targetOrigBpm
        val neededPitchPercent = ((ratio - 1.0f) * 100f).coerceIn(-16f, 16f)

        updateDeckState(targetDeck) {
            it.copy(
                pitchPercent = neededPitchPercent,
                bpm = targetOrigBpm * (1f + neededPitchPercent / 100f),
                isSyncActive = true
            )
        }
        applyPlaybackParams(targetDeck)
    }

    fun setPitchPercent(deckNumber: Int, percent: Float) {
        val clamped = percent.coerceIn(-16f, 16f)
        updateDeckState(deckNumber) {
            val newBpm = it.originalBpm * (1f + (clamped + it.pitchBend) / 100f)
            it.copy(pitchPercent = clamped, bpm = newBpm, isSyncActive = false)
        }
        applyPlaybackParams(deckNumber)
    }

    fun pitchBendNudge(deckNumber: Int, delta: Float) {
        updateDeckState(deckNumber) {
            val newBend = (it.pitchBend + delta).coerceIn(-4f, 4f)
            val newBpm = it.originalBpm * (1f + (it.pitchPercent + newBend) / 100f)
            it.copy(pitchBend = newBend, bpm = newBpm)
        }
        applyPlaybackParams(deckNumber)
    }

    fun releasePitchBend(deckNumber: Int) {
        updateDeckState(deckNumber) {
            val newBpm = it.originalBpm * (1f + it.pitchPercent / 100f)
            it.copy(pitchBend = 0.0f, bpm = newBpm)
        }
        applyPlaybackParams(deckNumber)
    }

    fun resetPitch(deckNumber: Int) {
        updateDeckState(deckNumber) {
            it.copy(pitchPercent = 0.0f, pitchBend = 0.0f, bpm = it.originalBpm, isSyncActive = false)
        }
        applyPlaybackParams(deckNumber)
    }

    private fun applyPlaybackParams(deckNumber: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

        val player = getPlayer(deckNumber) ?: return
        val state = if (deckNumber == 1) _deck1State.value else _deck2State.value

        try {
            val totalPitchFactor = 1.0f + (state.pitchPercent + state.pitchBend) / 100f
            val safeSpeed = totalPitchFactor.coerceIn(0.70f, 1.40f)

            val params = player.playbackParams ?: PlaybackParams()
            params.speed = safeSpeed
            player.playbackParams = params
        } catch (_: Throwable) {}
    }

    // Hot Cues: slots 1..4
    fun triggerHotCue(deckNumber: Int, slot: Int) {
        val player = getPlayer(deckNumber) ?: return
        val state = if (deckNumber == 1) _deck1State.value else _deck2State.value

        val cueTime = state.hotCues[slot]
        if (cueTime != null) {
            try {
                player.seekTo(cueTime.toInt())
                updateDeckState(deckNumber) { it.copy(currentPositionMs = cueTime) }
                if (!player.isPlaying) {
                    player.start()
                    updateDeckState(deckNumber) { it.copy(isPlaying = true) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "triggerHotCue error: ${e.message}")
            }
        } else {
            // Save current position as hot cue
            setHotCue(deckNumber, slot)
        }
    }

    fun setHotCue(deckNumber: Int, slot: Int) {
        val player = getPlayer(deckNumber) ?: return
        val curPos = player.currentPosition.toLong()
        updateDeckState(deckNumber) {
            val updated = it.hotCues.toMutableMap()
            updated[slot] = curPos
            it.copy(hotCues = updated)
        }
    }

    fun deleteHotCue(deckNumber: Int, slot: Int) {
        updateDeckState(deckNumber) {
            val updated = it.hotCues.toMutableMap()
            updated.remove(slot)
            it.copy(hotCues = updated)
        }
    }

    // Looping Engine
    fun triggerLoopLength(deckNumber: Int, beats: Float) {
        val state = if (deckNumber == 1) _deck1State.value else _deck2State.value
        val player = getPlayer(deckNumber) ?: return

        val curPos = player.currentPosition.toLong()
        val safeBpm = state.bpm.coerceIn(60f, 220f)
        val beatMs = (60000f / safeBpm)
        val loopDurMs = (beatMs * beats).toLong().coerceIn(200L, 60000L)

        // Toggle loop off if already active on same length
        if (state.isLoopActive && abs(state.activeLoopBeats - beats) < 0.05f) {
            updateDeckState(deckNumber) { it.copy(isLoopActive = false) }
        } else {
            updateDeckState(deckNumber) {
                it.copy(
                    isLoopActive = true,
                    loopStartMs = curPos,
                    loopDurationMs = loopDurMs,
                    activeLoopBeats = beats
                )
            }
        }
    }

    fun setLoopIn(deckNumber: Int) {
        val player = getPlayer(deckNumber) ?: return
        val curPos = player.currentPosition.toLong()
        updateDeckState(deckNumber) {
            it.copy(loopStartMs = curPos)
        }
    }

    fun setLoopOut(deckNumber: Int) {
        val player = getPlayer(deckNumber) ?: return
        val curPos = player.currentPosition.toLong()
        updateDeckState(deckNumber) {
            val dur = (curPos - it.loopStartMs).coerceAtLeast(200L)
            it.copy(
                loopDurationMs = dur,
                isLoopActive = true
            )
        }
    }

    fun halveLoop(deckNumber: Int) {
        updateDeckState(deckNumber) {
            val newDur = (it.loopDurationMs / 2).coerceAtLeast(150L)
            val newBeats = (it.activeLoopBeats / 2f).coerceAtLeast(0.125f)
            it.copy(loopDurationMs = newDur, activeLoopBeats = newBeats, isLoopActive = true)
        }
    }

    fun doubleLoop(deckNumber: Int) {
        updateDeckState(deckNumber) {
            val newDur = (it.loopDurationMs * 2).coerceAtMost(60000L)
            val newBeats = (it.activeLoopBeats * 2f).coerceAtMost(32f)
            it.copy(loopDurationMs = newDur, activeLoopBeats = newBeats, isLoopActive = true)
        }
    }

    fun toggleRoll(deckNumber: Int, beats: Float = 1.0f) {
        val player = getPlayer(deckNumber) ?: return
        val state = if (deckNumber == 1) _deck1State.value else _deck2State.value

        if (state.isRollActive) {
            // Exit roll and return to where song would have been
            val elapsed = System.currentTimeMillis() - state.rollResumeMs
            val returnPos = (player.currentPosition + elapsed).coerceAtMost(state.durationMs)
            try {
                player.seekTo(returnPos.toInt())
            } catch (_: Throwable) {}
            updateDeckState(deckNumber) {
                it.copy(isRollActive = false, isLoopActive = false)
            }
        } else {
            // Start roll loop
            val curPos = player.currentPosition.toLong()
            val safeBpm = state.bpm.coerceIn(60f, 220f)
            val dur = ((60000f / safeBpm) * beats).toLong().coerceIn(150L, 10000L)
            updateDeckState(deckNumber) {
                it.copy(
                    isRollActive = true,
                    isLoopActive = true,
                    loopStartMs = curPos,
                    loopDurationMs = dur,
                    activeLoopBeats = beats,
                    rollResumeMs = System.currentTimeMillis()
                )
            }
        }
    }

    // Touch Scratching
    fun startScratch(deckNumber: Int) {
        val player = getPlayer(deckNumber) ?: return
        try {
            player.pause()
        } catch (_: Throwable) {}
        updateDeckState(deckNumber) { it.copy(isScratching = true) }
    }

    fun scratchScrub(deckNumber: Int, deltaDegrees: Float) {
        val player = getPlayer(deckNumber) ?: return
        val state = if (deckNumber == 1) _deck1State.value else _deck2State.value

        // 360 degrees = 1.8 seconds of audio scrub (approx 33 1/3 RPM vinyl)
        val deltaMs = ((deltaDegrees / 360f) * 1800f).toLong()
        val targetPos = (player.currentPosition + deltaMs).coerceIn(0L, state.durationMs)

        try {
            player.seekTo(targetPos.toInt())
        } catch (_: Throwable) {}

        updateDeckState(deckNumber) {
            it.copy(
                currentPositionMs = targetPos,
                platterAngleDeg = (it.platterAngleDeg + deltaDegrees) % 360f
            )
        }
    }

    fun endScratch(deckNumber: Int) {
        val player = getPlayer(deckNumber) ?: return
        val state = if (deckNumber == 1) _deck1State.value else _deck2State.value

        updateDeckState(deckNumber) { it.copy(isScratching = false) }
        if (state.isPlaying) {
            try {
                player.start()
                applyPlaybackParams(deckNumber)
            } catch (_: Throwable) {}
        }
    }

    // Faders & Mixer
    fun setDeckVolume(deckNumber: Int, volume: Float) {
        val clamped = volume.coerceIn(0.0f, 1.0f)
        updateDeckState(deckNumber) { it.copy(faderVolume = clamped) }
        updateVolumes()
    }

    fun setCrossfader(value: Float) {
        autoFadeJob?.cancel()
        _isAutoFading.value = false
        _crossfader.value = value.coerceIn(0.0f, 1.0f)
        updateVolumes()
        checkCrossfaderTakeover()
    }

    fun resetCrossfaderToCenter() {
        setCrossfader(0.5f)
    }

    fun setMasterGain(gain: Float) {
        _masterGain.value = gain.coerceIn(0.0f, 1.5f)
        updateVolumes()
    }

    fun togglePfl(deckNumber: Int) {
        updateDeckState(deckNumber) { it.copy(pflEnabled = !it.pflEnabled) }
    }

    /**
     * Crossfade takeover detector:
     * When the user mixes over to Deck 2 (or Deck 1), the newly idle deck automatically
     * pulls the next upcoming track from the playlist queue ready for the next transition.
     */
    private fun checkCrossfaderTakeover() {
        if (activeQueue.isEmpty()) return
        val cf = _crossfader.value

        if (cf >= 0.85f && _deck2State.value.isPlaying) {
            if (activePlaybackDeck != 2) {
                activePlaybackDeck = 2
                currentQueueIndex = deck2QueueIndex
                _deck2State.value.song?.let { onQueueAdvanced?.invoke(currentQueueIndex, it) }
                updateQueueDisplay()
            }
            if (!_deck1State.value.isPlaying && activeQueue.size > 2) {
                val nextIdx = (deck2QueueIndex + 1) % activeQueue.size
                if (nextIdx != deck1QueueIndex) {
                    deck1QueueIndex = nextIdx
                    loadTrack(1, activeQueue[nextIdx], startPlaying = false, initialPosMs = 0L)
                }
            }
        } else if (cf <= 0.15f && _deck1State.value.isPlaying) {
            if (activePlaybackDeck != 1) {
                activePlaybackDeck = 1
                currentQueueIndex = deck1QueueIndex
                _deck1State.value.song?.let { onQueueAdvanced?.invoke(currentQueueIndex, it) }
                updateQueueDisplay()
            }
            if (!_deck2State.value.isPlaying && activeQueue.size > 2) {
                val nextIdx = (deck1QueueIndex + 1) % activeQueue.size
                if (nextIdx != deck2QueueIndex) {
                    deck2QueueIndex = nextIdx
                    loadTrack(2, activeQueue[nextIdx], startPlaying = false, initialPosMs = 0L)
                }
            }
        }
    }

    /**
     * Equal-Power Logarithmic Gain Calculation:
     * Maintains constant acoustic power (RMS sound pressure) across the crossfader sweep.
     */
    private fun updateVolumes() {
        val cf = _crossfader.value
        val crossfadeAngle = cf * (PI.toFloat() / 2f)

        // Equal power logarithmic curves
        val deck1CfGain = cos(crossfadeAngle)
        val deck2CfGain = sin(crossfadeAngle)

        val mg = _masterGain.value
        val d1Fader = _deck1State.value.faderVolume
        val d2Fader = _deck2State.value.faderVolume

        val d1Vol = (d1Fader * deck1CfGain * mg).coerceIn(0.0f, 1.0f)
        val d2Vol = (d2Fader * deck2CfGain * mg).coerceIn(0.0f, 1.0f)

        try {
            deck1Player?.setVolume(d1Vol, d1Vol)
        } catch (_: Throwable) {}

        try {
            deck2Player?.setVolume(d2Vol, d2Vol)
        } catch (_: Throwable) {}
    }

    /**
     * Timed equal-power auto-fade between decks based on BPM bars.
     */
    fun startAutoFade(bars: Int = 4) {
        autoFadeJob?.cancel()
        _isAutoFading.value = true

        val refBpm = (_deck1State.value.bpm.takeIf { it > 60f } ?: _deck2State.value.bpm).coerceIn(70f, 200f)
        // 4 beats per bar
        val totalDurationMs = ((bars * 4 * 60000f) / refBpm).toLong().coerceIn(3000L, 30000L)

        val startVal = _crossfader.value
        val targetVal = if (startVal < 0.5f) 1.0f else 0.0f

        // Ensure target deck is playing if user triggers auto-fade
        val targetDeck = if (targetVal > 0.5f) 2 else 1
        val targetState = if (targetDeck == 1) _deck1State.value else _deck2State.value
        if (!targetState.isPlaying && targetState.song != null) {
            val targetPlayer = getPlayer(targetDeck)
            targetPlayer?.start()
            applyPlaybackParams(targetDeck)
            updateDeckState(targetDeck) { it.copy(isPlaying = true) }
        }

        autoFadeJob = engineScope.launch {
            val startTime = System.currentTimeMillis()
            while (isActive) {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toFloat() / totalDurationMs.toFloat()).coerceIn(0.0f, 1.0f)

                _crossfader.value = startVal + (targetVal - startVal) * progress
                updateVolumes()

                if (progress >= 1.0f) break
                delay(16L) // 60 FPS fader animation
            }
            _isAutoFading.value = false
            checkCrossfaderTakeover()
        }
    }

    // Real-Time Looping, Position Polling, and VU Meters
    private fun startTelemetryPolling() {
        pollJob?.cancel()
        pollJob = engineScope.launch {
            var rotTick = 0f
            while (isActive) {
                // Deck 1 loop & position check
                val p1 = deck1Player
                if (p1 != null) {
                    try {
                        val pos1 = p1.currentPosition.toLong()
                        val state1 = _deck1State.value

                        if (state1.isLoopActive && state1.loopDurationMs > 0L) {
                            val loopEnd = state1.loopStartMs + state1.loopDurationMs
                            if (pos1 >= loopEnd || pos1 < state1.loopStartMs) {
                                p1.seekTo(state1.loopStartMs.toInt())
                            }
                        }

                        val angleDelta = if (state1.isPlaying && !state1.isScratching) {
                            (state1.bpm / 120f) * 4.2f
                        } else 0f

                        val vu1 = if (state1.isPlaying) {
                            ((state1.faderVolume * 0.7f) + (sin(rotTick * 0.4f) * 0.25f)).coerceIn(0.05f, 0.95f)
                        } else 0.0f

                        _deck1State.value = state1.copy(
                            currentPositionMs = pos1,
                            platterAngleDeg = (state1.platterAngleDeg + angleDelta) % 360f,
                            vuLevel = vu1
                        )
                    } catch (_: Throwable) {}
                }

                // Deck 2 loop & position check
                val p2 = deck2Player
                if (p2 != null) {
                    try {
                        val pos2 = p2.currentPosition.toLong()
                        val state2 = _deck2State.value

                        if (state2.isLoopActive && state2.loopDurationMs > 0L) {
                            val loopEnd = state2.loopStartMs + state2.loopDurationMs
                            if (pos2 >= loopEnd || pos2 < state2.loopStartMs) {
                                p2.seekTo(state2.loopStartMs.toInt())
                            }
                        }

                        val angleDelta2 = if (state2.isPlaying && !state2.isScratching) {
                            (state2.bpm / 120f) * 4.2f
                        } else 0f

                        val vu2 = if (state2.isPlaying) {
                            ((state2.faderVolume * 0.7f) + (cos(rotTick * 0.35f) * 0.25f)).coerceIn(0.05f, 0.95f)
                        } else 0.0f

                        _deck2State.value = state2.copy(
                            currentPositionMs = pos2,
                            platterAngleDeg = (state2.platterAngleDeg + angleDelta2) % 360f,
                            vuLevel = vu2
                        )
                    } catch (_: Throwable) {}
                }

                // Master VU level composite
                val cf = _crossfader.value
                val d1Vu = _deck1State.value.vuLevel * (1.0f - cf)
                val d2Vu = _deck2State.value.vuLevel * cf
                _masterVuLevel.value = (d1Vu + d2Vu).coerceIn(0.0f, 1.0f)

                rotTick += 0.2f
                delay(25L) // 40 Hz polling
            }
        }
    }

    private fun stopTelemetryPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    private inline fun updateDeckState(deckNumber: Int, update: (DjDeckState) -> DjDeckState) {
        if (deckNumber == 1) {
            _deck1State.value = update(_deck1State.value)
        } else {
            _deck2State.value = update(_deck2State.value)
        }
    }

    private fun releasePlayer(deckNumber: Int) {
        val player = if (deckNumber == 1) deck1Player else deck2Player
        try {
            player?.stop()
            player?.release()
        } catch (_: Throwable) {}

        if (deckNumber == 1) {
            deck1Player = null
            _deck1State.value = _deck1State.value.copy(isPlaying = false)
        } else {
            deck2Player = null
            _deck2State.value = _deck2State.value.copy(isPlaying = false)
        }
    }
}
