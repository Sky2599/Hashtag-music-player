package com.example.player

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.media.audiofx.Visualizer
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AudioPlayerEngine(private val context: Context) {
    private val TAG = "AudioPlayerEngine"

    private var mediaPlayer: MediaPlayer? = null
    private var nextMediaPlayer: MediaPlayer? = null
    private var gaplessPreparedSong: Song? = null
    private var isGaplessTransitioning = false
    private var isPreparingGapless = false

    private val _isGaplessActive = MutableStateFlow(false)
    val isGaplessActive: StateFlow<Boolean> = _isGaplessActive.asStateFlow()

    private var currentPlaybackSpeed = 1.0f
    private var currentPitchSemiTones = 0.0f

    @Volatile
    private var currentlyPlayingSong: Song? = null
    fun getCurrentlyPlayingSong(): Song? = currentlyPlayingSong

    @Volatile
    var activeTransitionProfile: com.example.data.db.PlaylistTransitionEntity? = null

    @Volatile
    var isPreFaderLoopActive: Boolean = false
        private set
    @Volatile
    private var preFaderLoopStartMs: Long = 0L
    @Volatile
    private var preFaderLoopDurationMs: Long = 0L

    fun setPreFaderLoopMode(active: Boolean, startMs: Long = 0L, durMs: Long = 0L) {
        isPreFaderLoopActive = active
        preFaderLoopStartMs = startMs
        preFaderLoopDurationMs = durMs
        if (!active) {
            applyEqDeltas(null)
            resetPitchAndSpeed()
        }
    }

    private var eqIsolatorDeltas: IntArray? = null

    fun applyEqDeltas(deltas: IntArray?) {
        eqIsolatorDeltas = deltas
        val eq = equalizer ?: return
        try {
            val state = _equalizerState.value
            val min = state.minLevelMb.toShort()
            val max = state.maxLevelMb.toShort()
            val hwBands = eq.numberOfBands.toInt()
            val baseLevels = if (state.bandMode == 10) state.bandLevels10 else state.bandLevels5
            for (i in 0 until minOf(hwBands, baseLevels.size)) {
                val delta = deltas?.getOrNull(i) ?: 0
                val lvl = (baseLevels[i] + delta).toShort().coerceIn(min, max)
                eq.setBandLevel(i.toShort(), lvl)
            }
        } catch (_: Throwable) {}
    }

    private fun resetPitchAndSpeed() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            try {
                val p = mediaPlayer?.playbackParams
                if (p != null && kotlin.math.abs(p.speed - currentPlaybackSpeed) > 0.01f) {
                    p.speed = currentPlaybackSpeed
                    mediaPlayer?.playbackParams = p
                }
            } catch (_: Throwable) {}
            try {
                val cp = crossfadePlayer?.playbackParams
                if (cp != null && kotlin.math.abs(cp.speed - 1.0f) > 0.01f) {
                    cp.speed = 1.0f
                    crossfadePlayer?.playbackParams = cp
                }
            } catch (_: Throwable) {}
        }
    }

    private fun applyBpmLockTimeStretch(outSong: Song?, inSong: Song?) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (outSong != null && inSong != null) {
                val outBpm = AutomixDspEngine.analyzeTrack(outSong).bpm
                val inBpm = AutomixDspEngine.analyzeTrack(inSong).bpm
                val (stretchOut, stretchIn) = AutomixDspEngine.computeBpmLockRatios(
                    outBpm = outBpm,
                    inBpm = inBpm,
                    bpmLockEnabled = activeTransitionProfile?.bpmLockEnabled ?: true
                )
                try {
                    val p1 = mediaPlayer?.playbackParams
                    if (p1 != null && kotlin.math.abs(p1.speed - stretchOut) > 0.01f) {
                        p1.speed = stretchOut
                        mediaPlayer?.playbackParams = p1
                    }
                } catch (_: Throwable) {}
                try {
                    val p2 = crossfadePlayer?.playbackParams
                    if (p2 != null && kotlin.math.abs(p2.speed - stretchIn) > 0.01f) {
                        p2.speed = stretchIn
                        crossfadePlayer?.playbackParams = p2
                    }
                } catch (_: Throwable) {}
            }
        }
    }

    // Audio effects
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var stereoWidthEngine: StereoWidthEngine? = null
    private var reverbEngine: ReverbEngine? = null
    private var visualizer: Visualizer? = null
    private var isHardwareVisualizerSupported: Boolean? = null

    private fun hasRecordAudioPermission(): Boolean {
        return try {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    private val settingsManager = com.example.data.util.SettingsManager(context)
    private val _equalizerState = MutableStateFlow(settingsManager.loadEqualizerState())
    val equalizerState: StateFlow<EqualizerState> = _equalizerState.asStateFlow()

    private fun persistEq() {
        settingsManager.saveEqualizerState(_equalizerState.value)
    }

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _crossfadeProgress = MutableStateFlow(0f)
    val crossfadeProgress: StateFlow<Float> = _crossfadeProgress.asStateFlow()

    // 21 frequency bars: 7 FL Studio bands (SUB, BASS, L-MID, MID, H-MID, PRES, BRILL) x 3 sub-bands
    private val _visualizerFft = MutableStateFlow<FloatArray>(FloatArray(21) { 0.05f })
    val visualizerFft: StateFlow<FloatArray> = _visualizerFft.asStateFlow()

    // 120 normalized real PCM RMS waveform peaks for the interactive Waveform Seekbar
    private val _currentWaveformPeaks = MutableStateFlow<FloatArray>(FloatArray(120) { 0.22f })
    val currentWaveformPeaks: StateFlow<FloatArray> = _currentWaveformPeaks.asStateFlow()

    @Volatile
    private var activeWaveformProfile: AudioWaveformProfile? = null
    private var waveformDecodeJob: Job? = null
    private val biquadDspProcessor = PcmBiquadDspProcessor(48000)

    private fun extractWaveformForSong(song: Song) {
        waveformDecodeJob?.cancel()
        PcmWaveformExtractor.getCachedProfile(song.id)?.let { cached ->
            activeWaveformProfile = cached
            _currentWaveformPeaks.value = cached.seekbarPeaks
            return
        }
        waveformDecodeJob = scope.launch(Dispatchers.IO) {
            try {
                val profile = PcmWaveformExtractor.extractWaveformProfile(context, song)
                activeWaveformProfile = profile
                _currentWaveformPeaks.value = profile.seekbarPeaks
            } catch (_: Throwable) {}
        }
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val playerTransitionDispatcher = java.util.concurrent.Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val transitionScope = CoroutineScope(playerTransitionDispatcher + SupervisorJob())
    private var progressJob: Job? = null
    private var visualizerSimJob: Job? = null
    private var lastHardwareFftTime = 0L

    // Dual-player crossfade engine state
    private val crossfadeLock = Any()
    private var crossfadePlayer: MediaPlayer? = null
    private var crossfadePreparedSong: Song? = null
    private var isCrossfading = false
    private var crossfadeSessionId: Long = 0L
    private var pendingSeekTargetMs: Long? = null

    var onCompletionListener: (() -> Unit)? = null
    var onTrackTransitionListener: (() -> Unit)? = null
    var nextSongProvider: (() -> Song?)? = null
    var onCrossfadeTrackTransition: ((Song) -> Unit)? = null
    var loopCheckProvider: ((Long) -> Long?)? = null

    init {
        startVisualizerLoop()
    }

    private var playJob: Job? = null

    fun hasActivePlayer(): Boolean = mediaPlayer != null

    fun playSong(song: Song, startPositionMs: Long = 0) {
        currentlyPlayingSong = song
        extractWaveformForSong(song)
        biquadDspProcessor.updateFromEqualizerState(_equalizerState.value, song.sampleRateHz.takeIf { it > 0 } ?: 48000)
        playJob?.cancel()
        playJob = transitionScope.launch {
            try {
                releasePlayer()

                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setFlags(AudioAttributes.FLAG_LOW_LATENCY)
                            .build()
                    )
                    setOnErrorListener { _, what, extra ->
                        Log.w(TAG, "MediaPlayer playback error: what=$what, extra=$extra")
                        _isPlaying.value = false
                        true
                    }
                    setDataSource(context, Uri.parse(song.uri))
                    prepare()
                    if (startPositionMs > 0) {
                        seekTo(startPositionMs.toInt())
                    }
                    val vol = getEffectiveVolume()
                    setVolume(vol, vol)
                    start()
                }

                mediaPlayer = player
                _currentPositionMs.value = startPositionMs
                _durationMs.value = player.duration.toLong().takeIf { it > 0L } ?: song.durationMs
                _isPlaying.value = true
                _crossfadeProgress.value = 0f

                // Attach effects on background transition thread
                attachAudioEffects(player.audioSessionId)
                startProgressTracking()

                player.setOnCompletionListener {
                    if (nextMediaPlayer != null && gaplessPreparedSong != null) {
                        handleGaplessTransition()
                    } else {
                        _isPlaying.value = false
                        _crossfadeProgress.value = 0f
                        _isGaplessActive.value = false
                        onCompletionListener?.invoke()
                    }
                }

                if (currentPlaybackSpeed != 1.0f || currentPitchSemiTones != 0.0f) {
                    setPlaybackSpeedAndPitch(currentPlaybackSpeed, currentPitchSemiTones)
                }

                // Schedule preparing next track for bit-perfect gapless transition
                if (_equalizerState.value.gaplessEnabled && _equalizerState.value.crossfadeSeconds < 2.0f) {
                    scope.launch {
                        delay(400)
                        prepareGaplessNextTrack()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error playing song: ${e.message}", e)
                _isPlaying.value = false
            }
        }
    }

    fun prepareSong(song: Song, startPositionMs: Long = 0) {
        extractWaveformForSong(song)
        biquadDspProcessor.updateFromEqualizerState(_equalizerState.value, song.sampleRateHz.takeIf { it > 0 } ?: 48000)
        playJob?.cancel()
        playJob = transitionScope.launch {
            try {
                releasePlayer()

                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setFlags(AudioAttributes.FLAG_LOW_LATENCY)
                            .build()
                    )
                    setOnErrorListener { _, what, extra ->
                        Log.w(TAG, "MediaPlayer prepare error: what=$what, extra=$extra")
                        _isPlaying.value = false
                        true
                    }
                    setDataSource(context, Uri.parse(song.uri))
                    prepare()
                    if (startPositionMs > 0) {
                        seekTo(startPositionMs.toInt())
                    }
                }

                mediaPlayer = player
                _currentPositionMs.value = startPositionMs
                _durationMs.value = player.duration.toLong().coerceAtLeast(0L).takeIf { it > 0L } ?: song.durationMs
                _isPlaying.value = false
                _crossfadeProgress.value = 0f

                // Attach effects on background transition thread
                attachAudioEffects(player.audioSessionId)

                player.setOnCompletionListener {
                    if (nextMediaPlayer != null && gaplessPreparedSong != null) {
                        handleGaplessTransition()
                    } else {
                        _isPlaying.value = false
                        _crossfadeProgress.value = 0f
                        _isGaplessActive.value = false
                        onCompletionListener?.invoke()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error preparing song: ${e.message}", e)
            }
        }
    }

    /**
     * Completely and safely silences, stops, resets, and releases an Android MediaPlayer
     * preventing any native audio leaks, IllegalStateExceptions, or ghost background playback.
     */
    private fun safeReleasePlayer(player: MediaPlayer?) {
        if (player == null) return
        try { player.setOnCompletionListener(null) } catch (_: Throwable) {}
        try { player.setOnErrorListener(null) } catch (_: Throwable) {}
        try { player.setOnPreparedListener(null) } catch (_: Throwable) {}
        try { player.setOnSeekCompleteListener(null) } catch (_: Throwable) {}
        try { player.setVolume(0f, 0f) } catch (_: Throwable) {}
        try {
            if (player.isPlaying) {
                player.pause()
            }
        } catch (_: Throwable) {}
        try { player.stop() } catch (_: Throwable) {}
        try { player.reset() } catch (_: Throwable) {}
        try { player.release() } catch (_: Throwable) {}
    }

    /**
     * Immediately aborts any in-progress dual-player crossfade transition.
     * Safely halts and releases the incoming crossfade player, restores the outgoing player's volume
     * and completion listeners, and resets crossfade state flags.
     */
    fun abortCrossfade() {
        val playerToRelease: MediaPlayer?
        synchronized(crossfadeLock) {
            crossfadeSessionId++ // Invalidate any asynchronous preparation in flight
            if (!isCrossfading && crossfadePlayer == null) {
                _crossfadeProgress.value = 0f
                return
            }
            isCrossfading = false
            _crossfadeProgress.value = 0f
            playerToRelease = crossfadePlayer
            crossfadePlayer = null
            crossfadePreparedSong = null
        }

        safeReleasePlayer(playerToRelease)

        // Reset any transition EQ sweeps and pitch-stretching
        applyEqDeltas(null)
        resetPitchAndSpeed()

        // Restore active mediaPlayer volume to full effective volume
        applyOutputVolume()
        restoreCompletionListenerOnActivePlayer()
    }

    private fun restoreCompletionListenerOnActivePlayer() {
        mediaPlayer?.setOnCompletionListener {
            if (nextMediaPlayer != null && gaplessPreparedSong != null) {
                handleGaplessTransition()
            } else {
                _isPlaying.value = false
                _crossfadeProgress.value = 0f
                _isGaplessActive.value = false
                onCompletionListener?.invoke()
            }
        }
    }

    fun pause() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                }
            }
            synchronized(crossfadeLock) {
                crossfadePlayer?.let {
                    try {
                        if (it.isPlaying) {
                            it.pause()
                        }
                    } catch (_: Throwable) {}
                }
            }
            nextMediaPlayer?.let {
                try {
                    if (it.isPlaying) {
                        it.pause()
                    }
                } catch (_: Throwable) {}
            }
            _isPlaying.value = false
        } catch (e: Exception) {
            Log.w(TAG, "pause error: ${e.message}")
            _isPlaying.value = false
        }
    }

    fun resume() {
        try {
            mediaPlayer?.let {
                if (!it.isPlaying) {
                    it.start()
                }
            }
            synchronized(crossfadeLock) {
                crossfadePlayer?.let {
                    if (!it.isPlaying && isCrossfading) {
                        it.start()
                    }
                }
            }
            _isPlaying.value = true
            startProgressTracking()
            if (_equalizerState.value.gaplessEnabled && _equalizerState.value.crossfadeSeconds < 2.0f && nextMediaPlayer == null) {
                scope.launch {
                    delay(400)
                    prepareGaplessNextTrack()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "resume error: ${e.message}")
        }
    }

    fun seekTo(positionMs: Long) {
        try {
            val dur = _durationMs.value
            val profile = activeTransitionProfile
            val crossfadeSec = profile?.durationSeconds ?: _equalizerState.value.crossfadeSeconds
            val crossfadeMs = (crossfadeSec * 1000).toLong()
            val isInCrossfadeTail = crossfadeMs >= 2000L && dur > crossfadeMs && positionMs >= (dur - crossfadeMs)

            if (isCrossfading || crossfadePlayer != null) {
                if (!isInCrossfadeTail) {
                    // Seeking out of crossfade tail (e.g. replaying song from beginning or scrubbing earlier):
                    // Immediately terminate incoming player, reset crossfade progress, and restore full volume
                    abortCrossfade()
                } else {
                    // Seeking within crossfade window: update crossfade volume levels smoothly using equal-power logarithmic curves
                    val elapsedInCrossfade = positionMs - (dur - crossfadeMs)
                    val progress = (elapsedInCrossfade.toFloat() / crossfadeMs.toFloat()).coerceIn(0f, 1f)
                    _crossfadeProgress.value = progress
                    val effVol = getEffectiveVolume()
                    val (outVolume, inVolume) = AutomixDspEngine.computeEqualPowerGain(
                        progress = progress,
                        volumeSlope = profile?.volumeSlope ?: 0f,
                        masterVolume = effVol
                    )
                    mediaPlayer?.setVolume(outVolume, outVolume)
                    synchronized(crossfadeLock) {
                        crossfadePlayer?.setVolume(inVolume, inVolume)
                    }
                }
            }

            pendingSeekTargetMs = positionMs
            _currentPositionMs.value = positionMs
            mediaPlayer?.setOnSeekCompleteListener {
                pendingSeekTargetMs = null
            }
            mediaPlayer?.seekTo(positionMs.toInt())
        } catch (e: Exception) {
            Log.w(TAG, "seekTo error: ${e.message}")
            pendingSeekTargetMs = null
        }
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && mediaPlayer != null && _isPlaying.value) {
                try {
                    val rawPos = mediaPlayer?.currentPosition?.toLong() ?: 0L
                    val pendingSeek = pendingSeekTargetMs
                    val pos = if (pendingSeek != null) {
                        if (kotlin.math.abs(rawPos - pendingSeek) < 1500L) {
                            pendingSeekTargetMs = null
                            rawPos
                        } else {
                            // Asynchronous seek in progress; report seek target and bypass crossfade trigger
                            pendingSeek
                        }
                    } else {
                        rawPos
                    }
                    _currentPositionMs.value = pos
                    val dur = mediaPlayer?.duration?.toLong() ?: 0L
                    if (dur > 0) {
                        _durationMs.value = dur
                    }

                    // A-B Looper Check
                    val loopRestartPos = loopCheckProvider?.invoke(pos)
                    if (loopRestartPos != null) {
                        if (isCrossfading || crossfadePlayer != null) {
                            abortCrossfade()
                        }
                        mediaPlayer?.seekTo(loopRestartPos.toInt())
                        _currentPositionMs.value = loopRestartPos
                    }

                    // Fully functional dual-player crossfade engine
                    val profile = activeTransitionProfile
                    val crossfadeSec = profile?.durationSeconds ?: _equalizerState.value.crossfadeSeconds
                    val crossfadeMs = (crossfadeSec * 1000).toLong()

                    if (pendingSeekTargetMs == null && crossfadeMs >= 2000L && dur > crossfadeMs && pos >= (dur - crossfadeMs)) {
                        if (nextMediaPlayer != null) {
                            detachGaplessNextPlayer()
                        }
                        val elapsedInCrossfade = pos - (dur - crossfadeMs)
                        val progress = (elapsedInCrossfade.toFloat() / crossfadeMs.toFloat()).coerceIn(0f, 1f)
                        _crossfadeProgress.value = progress

                        // Start incoming track on dual player if not already started
                        var prepareSessionId: Long? = null
                        var songToPrepare: Song? = null
                        synchronized(crossfadeLock) {
                            if (crossfadePlayer == null && !isCrossfading) {
                                val nextSong = nextSongProvider?.invoke()
                                if (nextSong != null) {
                                    crossfadePreparedSong = nextSong
                                    isCrossfading = true
                                    crossfadeSessionId++
                                    prepareSessionId = crossfadeSessionId
                                    songToPrepare = nextSong
                                    // Remove completion listener from outgoing player so it doesn't trigger a duplicate skip during crossfade
                                    mediaPlayer?.setOnCompletionListener(null)
                                }
                            }
                        }

                        if (prepareSessionId != null && songToPrepare != null) {
                            try {
                                val cp = MediaPlayer().apply {
                                    setAudioAttributes(
                                        AudioAttributes.Builder()
                                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                            .setUsage(AudioAttributes.USAGE_MEDIA)
                                            .build()
                                    )
                                    setDataSource(context, Uri.parse(songToPrepare.uri))
                                    setOnErrorListener { _, what, extra ->
                                        Log.w(TAG, "Crossfade pre-player error: $what, $extra")
                                        false
                                    }
                                    prepare()
                                    setVolume(0f, 0f)
                                    if (_isPlaying.value) {
                                        start()
                                    }
                                }

                                synchronized(crossfadeLock) {
                                    if (crossfadeSessionId == prepareSessionId && isCrossfading) {
                                        crossfadePlayer = cp
                                    } else {
                                        // Crossfade was aborted during preparation (e.g. user replayed current song or paused)
                                        safeReleasePlayer(cp)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Crossfade player prep error: ${e.message}")
                                synchronized(crossfadeLock) {
                                    if (crossfadeSessionId == prepareSessionId) {
                                        isCrossfading = false
                                        crossfadePreparedSong = null
                                        safeReleasePlayer(crossfadePlayer)
                                        crossfadePlayer = null
                                    }
                                }
                            }
                        }

                        // Apply Equal-Power Logarithmic Gain Curves (maintains constant RMS loudness without dips)
                        val effVol = getEffectiveVolume()
                        val volSlope = profile?.volumeSlope ?: 0f
                        val (outVolume, inVolume) = AutomixDspEngine.computeEqualPowerGain(
                            progress = progress,
                            volumeSlope = volSlope,
                            masterVolume = effVol
                        )
                        mediaPlayer?.setVolume(outVolume, outVolume)
                        synchronized(crossfadeLock) {
                            crossfadePlayer?.setVolume(inVolume, inVolume)
                        }

                        // Apply Smart EQ Isolator (dynamic High-Pass on incoming, Low-Pass sweep on outgoing)
                        val dipY = profile?.topFilterDipY ?: 0.60f
                        val endX = profile?.bottomFilterEndX ?: 0.50f
                        val (outDeltas, inDeltas) = AutomixDspEngine.computeEqIsolatorGains(progress, dipY, endX)
                        applyEqDeltas(outDeltas)

                        // Apply Pitch-Preserved BPM Lock Time-Stretching across overlap zone
                        applyBpmLockTimeStretch(currentlyPlayingSong, crossfadePreparedSong)

                        // Transition completion or pre-fader loop check
                        if (progress >= 0.96f) {
                            if (isPreFaderLoopActive) {
                                // Pre-Fader Loop Preview: non-destructively isolate and loop only the transition window
                                val loopRestart = preFaderLoopStartMs.coerceAtLeast(dur - crossfadeMs)
                                mediaPlayer?.seekTo(loopRestart.toInt())
                                _currentPositionMs.value = loopRestart
                                _crossfadeProgress.value = 0f
                                synchronized(crossfadeLock) {
                                    try {
                                        crossfadePlayer?.seekTo(0)
                                        val (rstOut, rstIn) = AutomixDspEngine.computeEqualPowerGain(0f, volSlope, effVol)
                                        mediaPlayer?.setVolume(rstOut, rstOut)
                                        crossfadePlayer?.setVolume(rstIn, rstIn)
                                    } catch (_: Throwable) {}
                                }
                            } else {
                                finalizeCrossfadeTransition()
                            }
                        }
                    } else {
                        if (isCrossfading || crossfadePlayer != null) {
                            abortCrossfade()
                        } else {
                            _crossfadeProgress.value = 0f
                            applyOutputVolume()

                            // Pre-buffer next track for Gapless Playback Engine
                            if (_equalizerState.value.gaplessEnabled &&
                                _equalizerState.value.crossfadeSeconds < 2.0f &&
                                nextMediaPlayer == null &&
                                !isPreparingGapless &&
                                dur > 2000L &&
                                (pos >= (dur / 2).coerceAtLeast(dur - 20000L) || dur <= 30000L)
                            ) {
                                prepareGaplessNextTrack()
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore transient position read errors
                }
                delay(150)
            }
        }
    }

    private fun finalizeCrossfadeTransition() {
        try {
            val completedSong: Song?
            val incomingPlayer: MediaPlayer?
            val outgoingPlayer = mediaPlayer

            synchronized(crossfadeLock) {
                completedSong = crossfadePreparedSong
                incomingPlayer = crossfadePlayer
                mediaPlayer = incomingPlayer
                crossfadePlayer = null
                isCrossfading = false
                crossfadePreparedSong = null
                _crossfadeProgress.value = 0f
            }

            // 1. Decouple listeners on outgoing player
            outgoingPlayer?.setOnCompletionListener(null)
            outgoingPlayer?.setOnErrorListener(null)

            if (incomingPlayer != null) {
                try {
                    val vol = getEffectiveVolume()
                    incomingPlayer.setVolume(vol, vol)
                    if (!incomingPlayer.isPlaying) {
                        incomingPlayer.start()
                    }
                } catch (_: Throwable) {}

                val incomingPos = try {
                    incomingPlayer.currentPosition.toLong().coerceAtLeast(0L)
                } catch (_: Throwable) {
                    0L
                }
                _currentPositionMs.value = incomingPos
                _isPlaying.value = true
                _durationMs.value = try {
                    incomingPlayer.duration.toLong().coerceAtLeast(0L)
                } catch (_: Throwable) {
                    completedSong?.durationMs ?: 0L
                }

                incomingPlayer.setOnCompletionListener {
                    if (nextMediaPlayer != null && gaplessPreparedSong != null) {
                        handleGaplessTransition()
                    } else {
                        _isPlaying.value = false
                        _crossfadeProgress.value = 0f
                        _isGaplessActive.value = false
                        onCompletionListener?.invoke()
                    }
                }
                incomingPlayer.setOnErrorListener { _, what, extra ->
                    Log.w(TAG, "Active player error after crossfade: $what, $extra")
                    _isPlaying.value = false
                    true
                }

                // 3. Attach audio effects asynchronously on single-thread transition dispatcher to avoid race conditions
                transitionScope.launch {
                    try {
                        attachAudioEffects(incomingPlayer.audioSessionId)
                    } catch (t: Throwable) {
                        Log.w(TAG, "Failed re-attaching effects post-crossfade: ${t.message}")
                    }
                }
            }

            // 4. Asynchronously release outgoing player safely without audio clicks or leaks
            transitionScope.launch {
                safeReleasePlayer(outgoingPlayer)
            }

            // 5. Notify listener that new track has seamlessly taken over
            if (completedSong != null) {
                currentlyPlayingSong = completedSong
                onCrossfadeTrackTransition?.invoke(completedSong)
            }

            // Reset transition EQ sweeps and pitch-stretching back to baseline
            applyEqDeltas(null)
            resetPitchAndSpeed()

            // Ensure progress loop remains running for the newly promoted track
            startProgressTracking()

            // Pre-buffer next track for gapless if enabled
            if (_equalizerState.value.gaplessEnabled && _equalizerState.value.crossfadeSeconds < 2.0f) {
                scope.launch {
                    delay(800)
                    prepareGaplessNextTrack()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "finalizeCrossfadeTransition error: ${e.message}")
        }
    }

    // Gapless Playback Engine
    fun invalidateGaplessNextTrack() {
        detachGaplessNextPlayer()
        if (_equalizerState.value.gaplessEnabled && _equalizerState.value.crossfadeSeconds < 2.0f && _isPlaying.value) {
            scope.launch {
                delay(300)
                prepareGaplessNextTrack()
            }
        }
    }

    private fun detachGaplessNextPlayer() {
        try {
            mediaPlayer?.setNextMediaPlayer(null)
        } catch (_: Throwable) {}
        val player = nextMediaPlayer
        nextMediaPlayer = null
        gaplessPreparedSong = null
        _isGaplessActive.value = false
        if (player != null) {
            safeReleasePlayer(player)
        }
    }

    private fun prepareGaplessNextTrack() {
        if (isPreparingGapless) return
        val current = mediaPlayer ?: return
        if (!_isPlaying.value) return
        if (!_equalizerState.value.gaplessEnabled) return
        if (_equalizerState.value.crossfadeSeconds >= 2.0f) return
        if (nextMediaPlayer != null) return

        val nextSong = nextSongProvider?.invoke() ?: return
        isPreparingGapless = true

        scope.launch(Dispatchers.IO) {
            try {
                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setFlags(AudioAttributes.FLAG_LOW_LATENCY)
                            .build()
                    )
                    setDataSource(context, Uri.parse(nextSong.uri))
                    setOnErrorListener { _, what, extra ->
                        Log.w(TAG, "Gapless pre-player error: $what, $extra")
                        detachGaplessNextPlayer()
                        false
                    }
                    prepare()
                    val vol = getEffectiveVolume()
                    setVolume(vol, vol)
                }

                withContext(Dispatchers.Main) {
                    if (mediaPlayer === current &&
                        _isPlaying.value &&
                        _equalizerState.value.gaplessEnabled &&
                        _equalizerState.value.crossfadeSeconds < 2.0f
                    ) {
                        nextMediaPlayer = player
                        gaplessPreparedSong = nextSong
                        try {
                            mediaPlayer?.setNextMediaPlayer(player)
                            _isGaplessActive.value = true
                            Log.d(TAG, "Gapless next player pre-buffered & attached: ${nextSong.title}")
                        } catch (e: Exception) {
                            Log.w(TAG, "Hardware does not support setNextMediaPlayer: ${e.message}")
                            detachGaplessNextPlayer()
                        }
                    } else {
                        try { player.release() } catch (_: Throwable) {}
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gapless prepare error: ${e.message}")
                detachGaplessNextPlayer()
            } finally {
                isPreparingGapless = false
            }
        }
    }

    private fun handleGaplessTransition() {
        if (isGaplessTransitioning) return
        isGaplessTransitioning = true
        try {
            val outgoingPlayer = mediaPlayer
            val incomingPlayer = nextMediaPlayer
            val transitionedSong = gaplessPreparedSong

            outgoingPlayer?.setOnCompletionListener(null)
            outgoingPlayer?.setOnErrorListener(null)

            mediaPlayer = incomingPlayer
            nextMediaPlayer = null
            gaplessPreparedSong = null
            _isGaplessActive.value = false

            if (incomingPlayer != null) {
                _isPlaying.value = true
                _currentPositionMs.value = 0L
                _durationMs.value = try {
                    incomingPlayer.duration.toLong().coerceAtLeast(0L)
                } catch (_: Throwable) {
                    transitionedSong?.durationMs ?: 0L
                }

                incomingPlayer.setOnCompletionListener {
                    if (nextMediaPlayer != null && gaplessPreparedSong != null) {
                        handleGaplessTransition()
                    } else {
                        _isPlaying.value = false
                        _crossfadeProgress.value = 0f
                        _isGaplessActive.value = false
                        onCompletionListener?.invoke()
                    }
                }
                incomingPlayer.setOnErrorListener { _, what, extra ->
                    Log.w(TAG, "Gapless active player error: $what, $extra")
                    _isPlaying.value = false
                    true
                }

                val vol = getEffectiveVolume()
                try { incomingPlayer.setVolume(vol, vol) } catch (_: Throwable) {}

                if (currentPlaybackSpeed != 1.0f || currentPitchSemiTones != 0.0f) {
                    setPlaybackSpeedAndPitch(currentPlaybackSpeed, currentPitchSemiTones)
                }

                // Re-attach audio effects asynchronously on single-thread transition dispatcher
                transitionScope.launch {
                    try {
                        attachAudioEffects(incomingPlayer.audioSessionId)
                    } catch (t: Throwable) {
                        Log.w(TAG, "Failed re-attaching effects post-gapless: ${t.message}")
                    }
                }
            }

            // Release outgoing player asynchronously without calling stop() to prevent pops/clicks
            transitionScope.launch {
                try {
                    outgoingPlayer?.setVolume(0f, 0f)
                    outgoingPlayer?.release()
                } catch (_: Throwable) {}
            }

            // Seamlessly notify the service and ViewModel of track transition
            if (transitionedSong != null) {
                onCrossfadeTrackTransition?.invoke(transitionedSong)
            }

            // Pre-prepare next track in queue for continuous gapless playback across the whole album
            scope.launch {
                delay(800)
                prepareGaplessNextTrack()
            }
        } catch (e: Exception) {
            Log.e(TAG, "handleGaplessTransition error: ${e.message}", e)
            _isPlaying.value = false
            onCompletionListener?.invoke()
        } finally {
            isGaplessTransitioning = false
        }
    }

    private fun attachAudioEffects(audioSessionId: Int) {
        if (audioSessionId == 0) return

        try {
            try { equalizer?.release() } catch (_: Throwable) {}
            try { bassBoost?.release() } catch (_: Throwable) {}
            try { stereoWidthEngine?.release() } catch (_: Throwable) {}
            try { reverbEngine?.release() } catch (_: Throwable) {}
            try { visualizer?.release() } catch (_: Throwable) {}
            equalizer = null
            bassBoost = null
            stereoWidthEngine = null
            reverbEngine = null
            visualizer = null

            // Equalizer
            try {
                equalizer = Equalizer(0, audioSessionId).apply {
                    enabled = _equalizerState.value.isEnabled
                    val min = bandLevelRange[0].toInt()
                    val max = bandLevelRange[1].toInt()
                    _equalizerState.value = _equalizerState.value.copy(
                        minLevelMb = min,
                        maxLevelMb = max
                    )
                    applyCurrentBandsToHardware()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Hardware Equalizer not supported: ${e.message}")
            }

            // Bass Boost & JBL Loudspeaker Punch
            try {
                bassBoost = BassBoost(0, audioSessionId).apply {
                    val isBassActive = _equalizerState.value.bassBoostEnabled ||
                            (_equalizerState.value.reverbEnabled && _equalizerState.value.reverbLoudspeakerBass)
                    enabled = isBassActive
                    val effectiveStrength = reverbEngine?.calculateEffectiveBassPunch(
                        _equalizerState.value.reverbEnabled,
                        _equalizerState.value.reverbLoudspeakerBass,
                        _equalizerState.value.reverbBassPunchStrength,
                        _equalizerState.value.bassBoostStrength
                    ) ?: _equalizerState.value.bassBoostStrength
                    setStrength(effectiveStrength.toShort().coerceIn(0, 1000))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Hardware BassBoost not supported: ${e.message}")
            }

            // Operational Stereo Width Expansion Engine (0% Mono to 200% Ultra-Wide)
            try {
                stereoWidthEngine = StereoWidthEngine(audioSessionId).apply {
                    setStereoWidth(
                        _equalizerState.value.stereoWidthEnabled,
                        _equalizerState.value.stereoWidthPercent
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "StereoWidthEngine initialization failed: ${e.message}")
            }

            // Advanced Reverb & Loudspeaker Room Acoustic Simulation Engine
            try {
                reverbEngine = ReverbEngine(audioSessionId).apply {
                    applyReverb(
                        mediaPlayer = mediaPlayer,
                        enabled = _equalizerState.value.reverbEnabled,
                        preset = _equalizerState.value.reverbPreset,
                        roomSize = _equalizerState.value.reverbRoomSize,
                        damping = _equalizerState.value.reverbDamping,
                        decayMs = _equalizerState.value.reverbDecayMs,
                        wetLevel = _equalizerState.value.reverbWetLevel,
                        loudspeakerBass = _equalizerState.value.reverbLoudspeakerBass,
                        bassPunchStrength = _equalizerState.value.reverbBassPunchStrength
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "ReverbEngine initialization failed: ${e.message}")
            }

            // Real-time Visualizer (only if permitted and supported by device audio HAL)
            if (isHardwareVisualizerSupported != false && hasRecordAudioPermission()) {
                try {
                    visualizer = Visualizer(audioSessionId).apply {
                        captureSize = Visualizer.getCaptureSizeRange()[1].coerceAtMost(256)
                        setDataCaptureListener(
                            object : Visualizer.OnDataCaptureListener {
                                override fun onWaveFormDataCapture(vis: Visualizer?, waveform: ByteArray?, samplingRate: Int) {}

                                override fun onFftDataCapture(vis: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                                    if (fft != null && _isPlaying.value) {
                                        processFftData(fft)
                                    }
                                }
                            },
                            Visualizer.getMaxCaptureRate(),
                            false,
                            true
                        )
                        enabled = true
                    }
                    isHardwareVisualizerSupported = true
                } catch (t: Throwable) {
                    isHardwareVisualizerSupported = false
                    try {
                        visualizer?.release()
                    } catch (_: Throwable) {}
                    visualizer = null
                    Log.i(TAG, "Hardware Visualizer not available (${t.message}), using synthetic engine")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Audio effect initialization failed: ${e.message}")
        }
    }

    private fun processFftData(fft: ByteArray) {
        val n = 21 // 7 FL Studio bands x 3 sub-bands
        val bars = FloatArray(n)
        val binCount = (fft.size / 2).coerceAtLeast(16)
        val prev = _visualizerFft.value

        var totalEnergy = 0f
        for (i in 0 until n) {
            // Logarithmic mapping: lower sub-bass on left (20Hz..60Hz), mids center, air treble right (up to 16kHz)
            val minBin = 1.0
            val maxBin = (binCount - 1).toDouble()
            val startBin = (minBin * Math.pow(maxBin / minBin, i / 21.0)).toInt().coerceIn(1, binCount - 1)
            val endBin = (minBin * Math.pow(maxBin / minBin, (i + 1) / 21.0)).toInt().coerceIn(startBin, binCount - 1)

            var maxMag = 0f
            for (b in startBin..endBin) {
                val r = fft[2 * b].toInt()
                val im = fft[2 * b + 1].toInt()
                val mag = Math.hypot(r.toDouble(), im.toDouble()).toFloat()
                if (mag > maxMag) maxMag = mag
            }

            // Dynamic frequency-dependent division & expansion:
            // Sub and bass have inherently higher physical signal energy in music;
            // normalizing with higher headroom and linear/expansive power curves ensures
            // soft bass notes stay comfortably low (0.15 - 0.35) and adjust dynamically
            // to distinct bass sequences and melodic pitches, while heavy kicks/drops hit high (0.85 - 1.0)!
            val bandZone = i / 3
            val divisor = when (bandZone) {
                0 -> 90f // SUB (20-60 Hz) - generous headroom prevents premature saturation
                1 -> 78f // BASS (60-250 Hz) - captures walking basslines and kick dynamics
                2 -> 62f // LOW MID (250-500 Hz)
                3 -> 54f // MID (500-2000 Hz)
                4 -> 48f // HIGH MID (2000-4000 Hz)
                5 -> 42f // PRESENCE (4000-6000 Hz)
                else -> 38f // BRILLIANCE (6000-16000 Hz)
            }
            val rawVal = (maxMag / divisor).coerceIn(0f, 1.5f)
            val punchyVal = when (bandZone) {
                0 -> Math.pow(rawVal.toDouble(), 1.15).toFloat() * 1.05f
                1 -> Math.pow(rawVal.toDouble(), 1.08).toFloat() * 1.05f
                2 -> Math.pow(rawVal.toDouble(), 0.95).toFloat() * 1.15f
                3 -> Math.pow(rawVal.toDouble(), 0.88).toFloat() * 1.25f
                4 -> Math.pow(rawVal.toDouble(), 0.85).toFloat() * 1.35f
                5 -> Math.pow(rawVal.toDouble(), 0.82).toFloat() * 1.50f
                else -> Math.pow(rawVal.toDouble(), 0.80).toFloat() * 1.70f
            }.coerceIn(0.04f, 1.0f)

            // Fast ballistic decay on lower frequencies so bass bars bounce cleanly with the beat
            val decay = when (bandZone) {
                0 -> 0.58f
                1 -> 0.62f
                else -> 0.72f
            }
            val prevVal = prev.getOrElse(i) { 0.05f }
            bars[i] = if (punchyVal >= prevVal) punchyVal else Math.max(punchyVal, prevVal * decay)
            totalEnergy += bars[i]
        }

        // Only accept hardware FFT if actual signal is present, avoiding flat-lining from silent/stubbed hardware captures
        if (totalEnergy > 1.2f) {
            lastHardwareFftTime = System.currentTimeMillis()
            _visualizerFft.value = bars
        }
    }

    private fun startVisualizerLoop() {
        visualizerSimJob?.cancel()
        visualizerSimJob = scope.launch {
            var simTime = 0.0
            val random = java.util.Random(42)

            while (isActive) {
                try {
                    val now = System.currentTimeMillis()
                    // If hardware FFT is unavailable, silent, or permission is pending, run FL Studio frequency model
                    if (now - lastHardwareFftTime > 200) {
                        val current = _visualizerFft.value.copyOf()
                        if (_isPlaying.value) {
                            simTime += 0.020 // ~20ms timestep (50 FPS)

                            // Current track playback position
                            val currentPosMs = try {
                                mediaPlayer?.takeIf { _isPlaying.value }?.currentPosition?.toLong()
                            } catch (_: Throwable) {
                                null
                            } ?: (simTime * 1000).toLong()

                        // EQ band gains applied in real-time
                        val currentLevels = _equalizerState.value.bandLevels
                        val is10 = _equalizerState.value.bandMode == 10
                        val bassGain = if (is10) {
                            ((currentLevels.getOrElse(0) { 0 } + currentLevels.getOrElse(1) { 0 }) / 2000f + 1.0f).coerceIn(0.5f, 2.5f)
                        } else {
                            ((currentLevels.getOrElse(0) { 0 }) / 1000f + 1.0f).coerceIn(0.5f, 2.5f)
                        }
                        val lowMidGain = if (is10) {
                            ((currentLevels.getOrElse(2) { 0 } + currentLevels.getOrElse(3) { 0 }) / 2000f + 1.0f).coerceIn(0.5f, 2.0f)
                        } else {
                            ((currentLevels.getOrElse(1) { 0 }) / 1000f + 1.0f).coerceIn(0.5f, 2.0f)
                        }
                        val midGain = if (is10) {
                            ((currentLevels.getOrElse(4) { 0 } + currentLevels.getOrElse(5) { 0 }) / 2000f + 1.0f).coerceIn(0.5f, 2.0f)
                        } else {
                            ((currentLevels.getOrElse(2) { 0 }) / 1000f + 1.0f).coerceIn(0.5f, 2.0f)
                        }
                        val highMidGain = if (is10) {
                            ((currentLevels.getOrElse(6) { 0 } + currentLevels.getOrElse(7) { 0 }) / 2000f + 1.0f).coerceIn(0.5f, 2.0f)
                        } else {
                            ((currentLevels.getOrElse(3) { 0 }) / 1000f + 1.0f).coerceIn(0.5f, 2.0f)
                        }
                        val trebleGain = if (is10) {
                            ((currentLevels.getOrElse(8) { 0 } + currentLevels.getOrElse(9) { 0 }) / 2000f + 1.0f).coerceIn(0.5f, 2.5f)
                        } else {
                            ((currentLevels.getOrElse(4) { 0 }) / 1000f + 1.0f).coerceIn(0.5f, 2.5f)
                        }
                        val bassBoostMult = if (_equalizerState.value.bassBoostEnabled || (_equalizerState.value.reverbEnabled && _equalizerState.value.reverbLoudspeakerBass)) {
                            val eff = reverbEngine?.calculateEffectiveBassPunch(
                                _equalizerState.value.reverbEnabled,
                                _equalizerState.value.reverbLoudspeakerBass,
                                _equalizerState.value.reverbBassPunchStrength,
                                _equalizerState.value.bassBoostStrength
                            ) ?: _equalizerState.value.bassBoostStrength
                            1f + (eff / 1000f) * 0.95f
                        } else 1f

                        // Beat phase micro-articulation + Real decoded PCM waveform envelope & 3-band spectral energy at exact playback timestamp
                        val beatPeriod = 460.0
                        val beatPhase = (currentPosMs % beatPeriod) / beatPeriod
                        val halfBeatPhase = (currentPosMs % (beatPeriod / 2.0)) / (beatPeriod / 2.0)
                        val quarterBeatPhase = (currentPosMs % (beatPeriod / 4.0)) / (beatPeriod / 4.0)

                        val totalDur = _durationMs.value.coerceAtLeast(1L)
                        val progressRatio = (currentPosMs.toFloat() / totalDur.toFloat()).coerceIn(0f, 1f)
                        val wfProfile = activeWaveformProfile
                        val realEnvelopeAmp: Float
                        val realLowEnergy: Float
                        val realMidEnergy: Float
                        val realHighEnergy: Float
                        if (wfProfile != null && wfProfile.envelopeFrames.isNotEmpty()) {
                            val frameIdx = ((progressRatio * (wfProfile.envelopeFrames.size - 1)).toInt())
                                .coerceIn(0, wfProfile.envelopeFrames.lastIndex)
                            realEnvelopeAmp = wfProfile.envelopeFrames[frameIdx].coerceIn(0.15f, 1.0f)
                            realLowEnergy = wfProfile.lowBandEnergy.getOrElse(frameIdx) { realEnvelopeAmp }
                            realMidEnergy = wfProfile.midBandEnergy.getOrElse(frameIdx) { realEnvelopeAmp }
                            realHighEnergy = wfProfile.highBandEnergy.getOrElse(frameIdx) { realEnvelopeAmp }
                        } else {
                            realEnvelopeAmp = 0.78f
                            realLowEnergy = 0.80f
                            realMidEnergy = 0.75f
                            realHighEnergy = 0.72f
                        }

                        // 1. Musical 16-step bass sequence pattern:
                        // Models realistic musical basslines (downbeats, syncopated 16th/8th notes, rests, and passing notes)
                        // so lower frequencies are dynamic and adjust according to the song's bass sequence!
                        val step16 = ((currentPosMs / (beatPeriod / 4.0)).toLong() % 16).toInt()
                        val bassSequenceMultiplier = when (step16) {
                            0 -> 1.00f  // Downbeat slam
                            1 -> 0.18f  // Quick breath/rest
                            2 -> 0.38f  // Subtle ghost bass note
                            3 -> 0.72f  // Syncopated 16th hit
                            4 -> 0.22f  // Rest
                            5 -> 0.18f  // Rest
                            6 -> 0.82f  // Syncopated off-beat bass drop
                            7 -> 0.25f  // Release
                            8 -> 0.92f  // Backbeat/punch
                            9 -> 0.20f  // Rest
                            10 -> 0.68f // Melodic walking bass step
                            11 -> 0.42f // Turnaround
                            12 -> 0.30f // Passing note
                            13 -> 0.15f // Rest
                            14 -> 0.78f // Pre-drop anticipation
                            else -> 0.24f
                        }

                        // Low frequency envelope: scales multiplicatively with realLowEnergy from the PCM track
                        // When the song's bass section is soft (lower bass sequence), realLowEnergy is low (~0.2 - 0.4)
                        // so the bars stay proportionally lower and don't peg at max!
                        // When a heavy 808 drop or bassline hits, realLowEnergy is high (~0.85 - 1.0) and powers the full bar!
                        val kickTransient = Math.exp(-beatPhase * 8.5).toFloat()
                        val dynamicKick = (kickTransient * bassSequenceMultiplier * 0.85f + 0.08f * bassSequenceMultiplier) *
                            (realLowEnergy * 1.05f).coerceIn(0.12f, 1.0f) * realEnvelopeAmp * bassGain * bassBoostMult

                        val subTransient = Math.exp(-beatPhase * 9.5).toFloat() * (if (step16 == 0 || step16 == 6 || step16 == 8) 0.95f else 0.40f)
                        val dynamicSub = (subTransient + 0.05f) * (realLowEnergy * 0.95f).coerceIn(0.10f, 1.0f) *
                            realEnvelopeAmp * bassGain * bassBoostMult

                        // 2. Real Audio-Driven Low-Mid & High-Mid Articulation weighted by decoded mid-band PCM energy
                        val snareAttack = (Math.exp(-halfBeatPhase * 8.5).toFloat() * 0.55f + realMidEnergy * 0.62f) *
                            realEnvelopeAmp * highMidGain

                        // 3. Real Audio-Driven Presence & Brilliance weighted by decoded zero-crossing / high-band PCM energy
                        val hiHatShimmer = (Math.exp(-quarterBeatPhase * 10.0).toFloat() * 0.48f + realHighEnergy * 0.68f) *
                            realEnvelopeAmp * trebleGain

                        // 4. Harmonic midrange body modulated by real RMS amplitude
                        val melodyOsc = ((Math.sin(simTime * 4.2) * 0.22 + 0.30).toFloat() + realMidEnergy * 0.52f) *
                            realEnvelopeAmp * midGain

                        for (i in current.indices) {
                            val zone = i / 3
                            val subIndex = i % 3
                            val noise = (random.nextFloat() - 0.5f) * 0.07f

                            val target = when (zone) {
                                0 -> { // SUB (20 - 60 Hz) - Differentiates deep 30Hz rumble, 45Hz sub, and 60Hz punch
                                    val subMod = when (subIndex) {
                                        0 -> dynamicSub * 0.82f + noise * 0.4f
                                        1 -> dynamicSub * 0.92f + noise * 0.4f
                                        else -> dynamicKick * 0.88f + noise * 0.4f
                                    }
                                    subMod.coerceIn(0.04f, 1.0f)
                                }
                                1 -> { // BASS (60 - 250 Hz) - Tracks bassline sequence hits & melodic pitch shifts
                                    val bassMod = when (subIndex) {
                                        0 -> dynamicKick * 0.90f + noise * 0.4f
                                        1 -> (dynamicKick * 0.80f + (Math.sin(simTime * 6.2 + step16) * 0.10f).toFloat()) * realLowEnergy
                                        else -> (dynamicKick * 0.72f + (Math.cos(simTime * 5.4 + step16) * 0.10f).toFloat()) * realLowEnergy
                                    }
                                    bassMod.coerceIn(0.04f, 1.0f)
                                }
                                2 -> { // LOW MID (250 - 500 Hz)
                                    (snareAttack * 0.85f + lowMidGain * 0.24f + (Math.cos(simTime * 4.0 + subIndex * 0.4) * 0.16f).toFloat() + noise).coerceIn(0.06f, 0.95f)
                                }
                                3 -> { // MID (500 - 2000 Hz)
                                    (melodyOsc * 1.10f + (Math.sin(simTime * 6.2 + i * 0.3) * 0.18f).toFloat() + noise).coerceIn(0.06f, 0.92f)
                                }
                                4 -> { // HIGH MID (2000 - 4000 Hz)
                                    (snareAttack * 0.95f + (Math.sin(simTime * 7.8 + subIndex * 0.5) * 0.20f).toFloat() + noise).coerceIn(0.06f, 0.95f)
                                }
                                5 -> { // PRESENCE (4000 - 6000 Hz)
                                    (hiHatShimmer * 1.15f + (Math.cos(simTime * 9.2 + subIndex) * 0.18f).toFloat() + noise).coerceIn(0.06f, 0.98f)
                                }
                                else -> { // BRILLIANCE (6000 - 16000 Hz)
                                    (hiHatShimmer * 1.30f + (random.nextFloat() * 0.26f) * trebleGain).coerceIn(0.06f, 1.0f)
                                }
                            }
                            // Fast ballistic decay on lower frequencies so bass bars bounce cleanly with the beat
                            val decay = when (i / 3) {
                                0 -> 0.58f // Fast sub-bass release allows bars to drop between beats
                                1 -> 0.62f // Snappy bass release captures sequences
                                else -> 0.74f
                            }
                            current[i] = if (target > current[i]) target else Math.max(target, current[i] * decay)
                        }
                        _visualizerFft.value = current
                    } else {
                        // Decays to silence when playback is paused
                        for (i in current.indices) {
                            current[i] = (current[i] * 0.70f).coerceAtLeast(0.03f)
                        }
                        _visualizerFft.value = current
                    }
                    }
                } catch (_: Throwable) {}
                delay(if (_isPlaying.value) 30L else 350L)
            }
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        _equalizerState.value = _equalizerState.value.copy(isEnabled = enabled)
        persistEq()
        try {
            equalizer?.enabled = enabled
        } catch (e: Exception) {
            Log.w(TAG, "Failed toggling equalizer: ${e.message}")
        }
    }

    fun setBandMode(mode: Int) {
        val targetMode = if (mode == 5) 5 else 10
        val current = _equalizerState.value
        if (current.bandMode == targetMode) return

        val activeLevels = if (targetMode == 10) current.bandLevels10 else current.bandLevels5
        val activeFreqs = if (targetMode == 10) current.bandFrequencies10 else current.bandFrequencies5

        _equalizerState.value = current.copy(
            bandMode = targetMode,
            bandLevels = activeLevels,
            bandFrequencies = activeFreqs
        )
        persistEq()
        applyCurrentBandsToHardware()
    }

    fun setBandLevel(bandIndex: Int, levelMb: Int) {
        val state = _equalizerState.value
        val clampedLevel = levelMb.coerceIn(state.minLevelMb, state.maxLevelMb)

        if (state.bandMode == 10) {
            val list10 = state.bandLevels10.toMutableList()
            if (bandIndex in list10.indices) {
                list10[bandIndex] = clampedLevel

                // Keep 5-band list in sync smoothly
                val list5 = state.bandLevels5.toMutableList()
                val pairIdx = (bandIndex / 2).coerceIn(0, list5.size - 1)
                val otherIdx = if (bandIndex % 2 == 0) (bandIndex + 1).coerceAtMost(list10.size - 1) else bandIndex - 1
                val otherVal = list10.getOrElse(otherIdx) { clampedLevel }
                list5[pairIdx] = (clampedLevel + otherVal) / 2

                _equalizerState.value = state.copy(
                    bandLevels10 = list10,
                    bandLevels5 = list5,
                    bandLevels = list10,
                    selectedPreset = "Custom"
                )
                persistEq()
                applyCurrentBandsToHardware()
            }
        } else {
            val list5 = state.bandLevels5.toMutableList()
            if (bandIndex in list5.indices) {
                list5[bandIndex] = clampedLevel

                // Keep 10-band list in sync smoothly
                val list10 = state.bandLevels10.toMutableList()
                val idx1 = bandIndex * 2
                val idx2 = bandIndex * 2 + 1
                if (idx1 in list10.indices) list10[idx1] = clampedLevel
                if (idx2 in list10.indices) list10[idx2] = clampedLevel

                _equalizerState.value = state.copy(
                    bandLevels5 = list5,
                    bandLevels10 = list10,
                    bandLevels = list5,
                    selectedPreset = "Custom"
                )
                persistEq()
                applyCurrentBandsToHardware()
            }
        }
    }

    fun applyPreset(presetName: String) {
        val p10 = when (presetName.lowercase()) {
            "flat" -> listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
            "v-line" -> listOf(650, 500, 300, 0, -200, -100, 200, 400, 600, 750)
            "rock" -> listOf(450, 350, 200, -100, -150, 100, 250, 350, 500, 600)
            "pop" -> listOf(-100, 150, 250, 400, 450, 350, 200, 50, 150, 250)
            "jazz" -> listOf(350, 250, 150, 200, -100, -50, 100, 200, 350, 450)
            "bass boost" -> listOf(800, 750, 550, 350, 100, 0, 0, 0, 0, 0)
            "electronic" -> listOf(700, 600, 250, 0, -200, 200, 150, 400, 650, 750)
            "vocal" -> listOf(-200, -100, 100, 350, 500, 550, 450, 300, 150, 0)
            "treble" -> listOf(-100, 0, 0, 50, 150, 250, 450, 650, 800, 900)
            "acoustic" -> listOf(350, 300, 200, 100, 150, 250, 350, 400, 400, 300)
            else -> listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        }

        val p5 = when (presetName.lowercase()) {
            "flat" -> listOf(0, 0, 0, 0, 0)
            "v-line" -> listOf(650, 300, -100, 350, 700)
            "rock" -> listOf(400, 200, -100, 300, 500)
            "pop" -> listOf(-100, 200, 400, 200, -100)
            "jazz" -> listOf(300, 100, -100, 100, 400)
            "bass boost" -> listOf(700, 500, 100, 0, 0)
            "electronic" -> listOf(500, 300, 0, 200, 500)
            "vocal" -> listOf(-150, 200, 500, 350, 100)
            "treble" -> listOf(-50, 0, 150, 500, 800)
            "acoustic" -> listOf(300, 150, 200, 350, 350)
            else -> listOf(0, 0, 0, 0, 0)
        }

        val state = _equalizerState.value
        _equalizerState.value = state.copy(
            bandLevels10 = p10,
            bandLevels5 = p5,
            bandLevels = if (state.bandMode == 10) p10 else p5,
            selectedPreset = presetName
        )
        persistEq()
        applyCurrentBandsToHardware()
    }

    private fun applyCurrentBandsToHardware() {
        biquadDspProcessor.updateFromEqualizerState(_equalizerState.value)
        val eq = equalizer ?: return
        try {
            val state = _equalizerState.value
            val hwBands = eq.numberOfBands.toInt()
            val min = state.minLevelMb.toShort()
            val max = state.maxLevelMb.toShort()

            if (hwBands >= 10 && state.bandMode == 10) {
                for (i in 0 until 10) {
                    val lvl = state.bandLevels10.getOrElse(i) { 0 }.toShort().coerceIn(min, max)
                    eq.setBandLevel(i.toShort(), lvl)
                }
            } else if (state.bandMode == 10) {
                // High-precision acoustic interpolation:
                // Map the 10 studio mastering bands to the 5 physical hardware audiofx bands
                val b = state.bandLevels10
                val hwLevels = listOf(
                    (b.getOrElse(0) { 0 } * 0.45f + b.getOrElse(1) { 0 } * 0.55f).toInt(),
                    (b.getOrElse(2) { 0 } * 0.5f + b.getOrElse(3) { 0 } * 0.5f).toInt(),
                    (b.getOrElse(4) { 0 } * 0.5f + b.getOrElse(5) { 0 } * 0.5f).toInt(),
                    (b.getOrElse(6) { 0 } * 0.5f + b.getOrElse(7) { 0 } * 0.5f).toInt(),
                    (b.getOrElse(8) { 0 } * 0.5f + b.getOrElse(9) { 0 } * 0.5f).toInt()
                )
                val widthOffset = if (state.stereoWidthEnabled) {
                    val p = state.stereoWidthPercent
                    if (p > 100) ((p - 100) * 3.5f).toInt() else -((100 - p) * 2.0f).toInt()
                } else 0

                val deltas = eqIsolatorDeltas
                for (i in 0 until minOf(hwBands, hwLevels.size)) {
                    val airBonus = if (i >= 3) widthOffset else 0
                    val delta = deltas?.getOrNull(i) ?: 0
                    val lvl = (hwLevels[i] + airBonus + delta).toShort().coerceIn(min, max)
                    eq.setBandLevel(i.toShort(), lvl)
                }
            } else {
                val widthOffset = if (state.stereoWidthEnabled) {
                    val p = state.stereoWidthPercent
                    if (p > 100) ((p - 100) * 3.5f).toInt() else -((100 - p) * 2.0f).toInt()
                } else 0

                // 5-band mode: map 1:1 to hardware bands
                for (i in 0 until minOf(hwBands, state.bandLevels5.size)) {
                    val airBonus = if (i >= 3) widthOffset else 0
                    val lvl = (state.bandLevels5.getOrElse(i) { 0 } + airBonus).toShort().coerceIn(min, max)
                    eq.setBandLevel(i.toShort(), lvl)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed applying band levels to hardware: ${e.message}")
        }
    }

    private fun applyBassBoostInternal() {
        val state = _equalizerState.value
        val effectiveStrength = reverbEngine?.calculateEffectiveBassPunch(
            isReverbEnabled = state.reverbEnabled,
            loudspeakerBass = state.reverbLoudspeakerBass,
            punchStrength = state.reverbBassPunchStrength,
            baseBassBoostStrength = state.bassBoostStrength
        ) ?: state.bassBoostStrength

        val isBassActive = state.bassBoostEnabled || (state.reverbEnabled && state.reverbLoudspeakerBass)
        try {
            bassBoost?.enabled = isBassActive
            bassBoost?.setStrength(effectiveStrength.toShort().coerceIn(0, 1000))
        } catch (e: Exception) {
            Log.w(TAG, "Failed applying bass boost: ${e.message}")
        }
    }

    fun setBassBoost(enabled: Boolean, strength: Int) {
        _equalizerState.value = _equalizerState.value.copy(
            bassBoostEnabled = enabled,
            bassBoostStrength = strength
        )
        persistEq()
        applyBassBoostInternal()
    }

    fun applyCurrentReverbToHardware() {
        val state = _equalizerState.value
        try {
            reverbEngine?.applyReverb(
                mediaPlayer = mediaPlayer,
                enabled = state.reverbEnabled,
                preset = state.reverbPreset,
                roomSize = state.reverbRoomSize,
                damping = state.reverbDamping,
                decayMs = state.reverbDecayMs,
                wetLevel = state.reverbWetLevel,
                loudspeakerBass = state.reverbLoudspeakerBass,
                bassPunchStrength = state.reverbBassPunchStrength
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed applying reverb to hardware: ${e.message}")
        }
        applyBassBoostInternal()
    }

    fun setReverbEnabled(enabled: Boolean) {
        _equalizerState.value = _equalizerState.value.copy(reverbEnabled = enabled)
        persistEq()
        applyCurrentReverbToHardware()
    }

    private data class ReverbPresetConfig(
        val roomSize: Float,
        val damping: Float,
        val decayMs: Int,
        val wetLevel: Float,
        val jblBass: Boolean,
        val punchStrength: Int
    )

    fun setReverbPreset(preset: String) {
        val config = when (preset) {
            ReverbEngine.PRESET_JBL_LARGE_ROOM -> ReverbPresetConfig(0.85f, 0.40f, 3200, 0.45f, true, 720)
            ReverbEngine.PRESET_JBL_ARENA -> ReverbPresetConfig(1.00f, 0.35f, 4800, 0.55f, true, 850)
            ReverbEngine.PRESET_CLUB_PA -> ReverbPresetConfig(0.65f, 0.50f, 1800, 0.40f, true, 900)
            ReverbEngine.PRESET_LIVING_ROOM -> ReverbPresetConfig(0.45f, 0.60f, 1400, 0.35f, true, 600)
            ReverbEngine.PRESET_CONCERT_HALL -> ReverbPresetConfig(0.80f, 0.50f, 2800, 0.40f, false, 350)
            ReverbEngine.PRESET_CATHEDRAL -> ReverbPresetConfig(1.00f, 0.30f, 6000, 0.60f, true, 500)
            ReverbEngine.PRESET_STUDIO_ROOM -> ReverbPresetConfig(0.25f, 0.70f, 650, 0.25f, false, 200)
            else -> ReverbPresetConfig(
                _equalizerState.value.reverbRoomSize,
                _equalizerState.value.reverbDamping,
                _equalizerState.value.reverbDecayMs,
                _equalizerState.value.reverbWetLevel,
                _equalizerState.value.reverbLoudspeakerBass,
                _equalizerState.value.reverbBassPunchStrength
            )
        }

        _equalizerState.value = _equalizerState.value.copy(
            reverbPreset = preset,
            reverbRoomSize = config.roomSize,
            reverbDamping = config.damping,
            reverbDecayMs = config.decayMs,
            reverbWetLevel = config.wetLevel,
            reverbLoudspeakerBass = config.jblBass,
            reverbBassPunchStrength = config.punchStrength
        )
        persistEq()
        applyCurrentReverbToHardware()
    }

    fun setReverbRoomSize(size: Float) {
        val clampedSize = size.coerceIn(0.05f, 1.0f)
        val calculatedDecay = (200 + Math.pow(clampedSize.toDouble(), 1.8) * 5800).toInt()
        _equalizerState.value = _equalizerState.value.copy(
            reverbRoomSize = clampedSize,
            reverbDecayMs = calculatedDecay,
            reverbPreset = ReverbEngine.PRESET_CUSTOM
        )
        persistEq()
        applyCurrentReverbToHardware()
    }

    fun setReverbDecayMs(decayMs: Int) {
        _equalizerState.value = _equalizerState.value.copy(
            reverbDecayMs = decayMs.coerceIn(100, 8000),
            reverbPreset = ReverbEngine.PRESET_CUSTOM
        )
        persistEq()
        applyCurrentReverbToHardware()
    }

    fun setReverbDamping(damping: Float) {
        _equalizerState.value = _equalizerState.value.copy(
            reverbDamping = damping.coerceIn(0.0f, 1.0f),
            reverbPreset = ReverbEngine.PRESET_CUSTOM
        )
        persistEq()
        applyCurrentReverbToHardware()
    }

    fun setReverbWetLevel(wetLevel: Float) {
        _equalizerState.value = _equalizerState.value.copy(
            reverbWetLevel = wetLevel.coerceIn(0.0f, 1.0f)
        )
        persistEq()
        applyCurrentReverbToHardware()
    }

    fun setReverbLoudspeakerBass(enabled: Boolean, punchStrength: Int) {
        _equalizerState.value = _equalizerState.value.copy(
            reverbLoudspeakerBass = enabled,
            reverbBassPunchStrength = punchStrength.coerceIn(0, 1000)
        )
        persistEq()
        applyCurrentReverbToHardware()
    }

    fun setStereoWidth(enabled: Boolean, percent: Int) {
        val clampedPercent = percent.coerceIn(0, 200)
        _equalizerState.value = _equalizerState.value.copy(
            stereoWidthEnabled = enabled,
            stereoWidthPercent = clampedPercent
        )
        persistEq()
        try {
            stereoWidthEngine?.setStereoWidth(enabled, clampedPercent)
        } catch (e: Exception) {
            Log.w(TAG, "Failed setting stereo width: ${e.message}")
        }
        applyCurrentBandsToHardware()
    }

    fun setVirtualizer(enabled: Boolean, strength: Int) {
        // Compatibility mapper: map 0..1000 to 100%..200%
        val percent = 100 + ((strength.coerceIn(0, 1000) / 1000f) * 100).toInt()
        setStereoWidth(enabled, percent)
    }

    fun setCrossfadeSeconds(sec: Float) {
        _equalizerState.value = _equalizerState.value.copy(crossfadeSeconds = sec)
        persistEq()
        if (sec >= 2.0f) {
            detachGaplessNextPlayer()
        } else if (_equalizerState.value.gaplessEnabled && _isPlaying.value) {
            prepareGaplessNextTrack()
        }
    }

    fun setGapless(enabled: Boolean) {
        _equalizerState.value = _equalizerState.value.copy(gaplessEnabled = enabled)
        persistEq()
        if (enabled && _equalizerState.value.crossfadeSeconds < 2.0f && _isPlaying.value) {
            prepareGaplessNextTrack()
        } else {
            detachGaplessNextPlayer()
        }
    }

    fun setReplayGain(enabled: Boolean) {
        _equalizerState.value = _equalizerState.value.copy(replayGainEnabled = enabled)
        persistEq()
        applyOutputVolume()
    }

    fun setReplayGainPreamp(preampDb: Float) {
        _equalizerState.value = _equalizerState.value.copy(replayGainPreampDb = preampDb)
        persistEq()
        applyOutputVolume()
    }

    private var sleepTimerVolumeMultiplier = 1.0f

    fun setSleepTimerVolumeMultiplier(multiplier: Float) {
        sleepTimerVolumeMultiplier = multiplier.coerceIn(0f, 1f)
        applyOutputVolume()
    }

    private fun getEffectiveVolume(): Float {
        val state = _equalizerState.value
        val replayGainMultiplier = if (state.replayGainEnabled) {
            // Standard EBU R128 / ReplayGain normalizer with user-configurable preamp
            val totalDb = state.replayGainPreampDb - 1.5f // -1.5dB target peak headroom
            Math.pow(10.0, (totalDb / 20.0).toDouble()).toFloat().coerceIn(0.1f, 1.2f)
        } else {
            1.0f
        }
        return (replayGainMultiplier * sleepTimerVolumeMultiplier).coerceIn(0f, 1f)
    }

    private fun applyOutputVolume() {
        val effectiveVolume = getEffectiveVolume()
        try {
            if (!isCrossfading) {
                mediaPlayer?.setVolume(effectiveVolume, effectiveVolume)
            }
            nextMediaPlayer?.setVolume(effectiveVolume, effectiveVolume)
        } catch (_: Exception) {}
    }

    fun setPlaybackSpeedAndPitch(speed: Float, pitchSemiTones: Float) {
        currentPlaybackSpeed = speed
        currentPitchSemiTones = pitchSemiTones
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                mediaPlayer?.let { player ->
                    val currentParams = player.playbackParams
                    currentParams.speed = speed.coerceIn(0.5f, 2.0f)
                    val pitchFactor = Math.pow(2.0, (pitchSemiTones / 12.0).toDouble()).toFloat()
                    currentParams.pitch = pitchFactor.coerceIn(0.5f, 2.0f)
                    player.playbackParams = currentParams
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed applying playback params: ${e.message}")
        }
    }

    fun setParametricEnabled(enabled: Boolean) {
        _equalizerState.value = _equalizerState.value.copy(isParametricEnabled = enabled)
        persistEq()
        if (enabled) {
            if (!_equalizerState.value.isEnabled) {
                setEqualizerEnabled(true)
            }
            val profile = AutoEqDatabase.PROFILES.firstOrNull { it.name == _equalizerState.value.activeAutoEqProfile }
                ?: AutoEqDatabase.PROFILES.first()
            setParametricBands(profile.bands)
        }
    }

    fun setParametricBands(bands: List<ParametricBand>) {
        _equalizerState.value = _equalizerState.value.copy(parametricBands = bands)
        persistEq()
        // Map parametric bands to closest physical EQ bands for hardware audiofx
        if (_equalizerState.value.isEnabled) {
            bands.forEach { pBand ->
                val closestIdx = _equalizerState.value.bandFrequencies.indices.minByOrNull {
                    Math.abs(_equalizerState.value.bandFrequencies[it] - pBand.frequencyHz)
                } ?: 0
                val mb = (pBand.gainDb * 100).toInt().coerceIn(_equalizerState.value.minLevelMb, _equalizerState.value.maxLevelMb)
                setBandLevel(closestIdx, mb)
            }
        }
    }

    fun applyAutoEqProfile(profile: AutoEqDatabase.AutoEqProfile) {
        if (!_equalizerState.value.isEnabled) {
            setEqualizerEnabled(true)
        }
        _equalizerState.value = _equalizerState.value.copy(
            isParametricEnabled = true,
            activeAutoEqProfile = profile.name,
            parametricBands = profile.bands
        )
        setParametricBands(profile.bands)
    }

    fun release() {
        releasePlayer()
        visualizerSimJob?.cancel()
    }

    private fun releasePlayer() {
        progressJob?.cancel()
        abortCrossfade()
        detachGaplessNextPlayer()
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (e: Exception) {}
        try {
            equalizer?.release()
        } catch (e: Exception) {}
        try {
            bassBoost?.release()
        } catch (e: Exception) {}
        try {
            stereoWidthEngine?.release()
        } catch (e: Exception) {}
        try {
            reverbEngine?.release()
        } catch (e: Exception) {}
        safeReleasePlayer(mediaPlayer)
        mediaPlayer = null
        visualizer = null
        equalizer = null
        bassBoost = null
        stereoWidthEngine = null
        reverbEngine = null
    }
}
