package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.MediaMetadataRetriever
import android.media.Rating
import android.media.browse.MediaBrowser
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.service.media.MediaBrowserService
import android.util.Log
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.model.Song
import com.example.data.util.SettingsManager
import com.example.player.AudioPlayerEngine
import com.example.widget.WidgetUpdateHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicPlaybackService : MediaBrowserService() {
    companion object {
        const val CHANNEL_ID = "musicolet_playback_channel"
        const val NOTIFICATION_ID = 101

        const val ACTION_PLAY = "com.example.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.ACTION_PAUSE"
        const val ACTION_TOGGLE_PLAY_PAUSE = "com.example.ACTION_TOGGLE_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.ACTION_NEXT"
        const val ACTION_PREV = "com.example.ACTION_PREV"
        const val ACTION_STOP = "com.example.ACTION_STOP"
        const val ACTION_PLAY_QUEUE_INDEX = "com.example.ACTION_PLAY_QUEUE_INDEX"
        const val ACTION_TOGGLE_FAVORITE = "com.example.ACTION_TOGGLE_FAVORITE"
        const val ACTION_TOGGLE_SHUFFLE = "com.example.ACTION_TOGGLE_SHUFFLE"
        const val ACTION_TOGGLE_REPEAT = "com.example.ACTION_TOGGLE_REPEAT"
        const val ACTION_FAST_FORWARD_15 = "com.example.ACTION_FAST_FORWARD_15"
        const val ACTION_REWIND_10 = "com.example.ACTION_REWIND_10"

        const val MEDIA_ROOT_ID = "hashtag_music_root"
        const val MEDIA_FAVORITES_ID = "hashtag_favorites"
        const val MEDIA_ALL_SONGS_ID = "hashtag_all_songs"
        const val MEDIA_RECENT_ID = "hashtag_recent"

        const val EXTRA_QUEUE_INDEX = "extra_queue_index"
        const val EXTRA_SONG_ID = "extra_song_id"

        @Volatile
        var activeInstance: MusicPlaybackService? = null
            private set
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    lateinit var playerEngine: AudioPlayerEngine
        private set

    private var mediaSession: MediaSession? = null
    var currentSong: Song? = null
        private set
    var currentQueue: List<Song> = emptyList()
        private set
    private var cachedArtworkBitmap: Bitmap? = null
    private var cachedSongId: Long? = null

    var nextSongCallback: (() -> Song?)? = null
    var onTrackTransitioned: ((Song) -> Unit)? = null
    var onSkipNextCallback: (() -> Unit)? = null
    var onSkipPrevCallback: (() -> Unit)? = null
    var onPlayQueueIndexCallback: ((Int) -> Unit)? = null
    var onToggleFavoriteCallback: ((Song) -> Unit)? = null
    var onToggleShuffleCallback: (() -> Unit)? = null
    var onToggleRepeatCallback: (() -> Unit)? = null
    var onPlaySongIdCallback: ((Long) -> Unit)? = null

    private var lastWidgetProgressUpdateMs = 0L

    inner class LocalBinder : Binder() {
        fun getService(): MusicPlaybackService = this@MusicPlaybackService
    }

    fun isActuallyPlaying(): Boolean {
        return ::playerEngine.isInitialized && playerEngine.isPlaying.value
    }

    fun getCurrentPositionMs(): Long {
        return if (::playerEngine.isInitialized) playerEngine.currentPositionMs.value else 0L
    }

    fun getDurationMs(): Long {
        return if (::playerEngine.isInitialized) playerEngine.durationMs.value else 0L
    }

    override fun onCreate() {
        super.onCreate()
        activeInstance = this
        playerEngine = AudioPlayerEngine(this)
        playerEngine.nextSongProvider = { nextSongCallback?.invoke() }
        playerEngine.onCrossfadeTrackTransition = { transitionedSong ->
            currentSong = transitionedSong
            updatePlaybackState(PlaybackState.STATE_PLAYING)
            onTrackTransitioned?.invoke(transitionedSong)
            WidgetUpdateHelper.updateAllWidgets(
                context = this,
                currentSong = transitionedSong,
                isPlaying = true,
                positionMs = 0L,
                durationMs = transitionedSong.durationMs,
                updateQueueList = true
            )
            serviceScope.launch(Dispatchers.IO) {
                updateMetadata(transitionedSong)
                showNotification(transitionedSong, true)
            }
        }
        createNotificationChannel()
        initMediaSession()
        observeEngineForWidgetsAndNotification()
        registerDisconnectReceivers()
    }

    private var disconnectReceiver: BroadcastReceiver? = null
    private var audioDeviceCallback: AudioDeviceCallback? = null
    private var lastDisconnectPauseTimestamp = 0L

    fun onHeadphonesOrBluetoothDisconnected(reason: String) {
        val now = System.currentTimeMillis()
        if (now - lastDisconnectPauseTimestamp < 800L) {
            return
        }

        val shouldPause = try {
            SettingsManager(this).loadSettings().pauseOnHeadphoneDisconnect
        } catch (_: Exception) {
            true
        }

        if (!shouldPause) {
            return
        }

        if (isActuallyPlaying()) {
            lastDisconnectPauseTimestamp = now
            Log.i("MusicPlaybackService", "Auto-pausing playback due to disconnect: $reason")
            playerEngine.pause()
        }
    }

    private fun registerDisconnectReceivers() {
        val filter = IntentFilter().apply {
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }

        disconnectReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val action = intent?.action ?: return
                when (action) {
                    AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                        onHeadphonesOrBluetoothDisconnected("AudioManager ACTION_AUDIO_BECOMING_NOISY")
                    }
                    Intent.ACTION_HEADSET_PLUG -> {
                        val state = intent.getIntExtra("state", -1)
                        if (state == 0) {
                            onHeadphonesOrBluetoothDisconnected("Headset unplugged (ACTION_HEADSET_PLUG)")
                        }
                    }
                    BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                        val state = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, -1)
                        if (state == BluetoothA2dp.STATE_DISCONNECTED || state == BluetoothA2dp.STATE_DISCONNECTING) {
                            onHeadphonesOrBluetoothDisconnected("Bluetooth A2DP disconnected")
                        }
                    }
                    BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                        val state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, -1)
                        if (state == BluetoothHeadset.STATE_DISCONNECTED || state == BluetoothHeadset.STATE_DISCONNECTING) {
                            onHeadphonesOrBluetoothDisconnected("Bluetooth Headset disconnected")
                        }
                    }
                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                        onHeadphonesOrBluetoothDisconnected("Bluetooth Device ACL disconnected")
                    }
                }
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(disconnectReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                registerReceiver(disconnectReceiver, filter)
            }
        } catch (e: Exception) {
            Log.w("MusicPlaybackService", "Failed to register disconnectReceiver: ${e.message}")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioDeviceCallback = object : AudioDeviceCallback() {
                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                    if (removedDevices.isNullOrEmpty()) return
                    val isPeripheral = removedDevices.any { device ->
                        when (device.type) {
                            AudioDeviceInfo.TYPE_WIRED_HEADSET,
                            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                            AudioDeviceInfo.TYPE_USB_HEADSET,
                            AudioDeviceInfo.TYPE_USB_DEVICE,
                            AudioDeviceInfo.TYPE_USB_ACCESSORY,
                            AudioDeviceInfo.TYPE_LINE_ANALOG,
                            AudioDeviceInfo.TYPE_LINE_DIGITAL,
                            AudioDeviceInfo.TYPE_HEARING_AID -> true
                            else -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    device.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                                    device.type == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
                                    device.type == AudioDeviceInfo.TYPE_BLE_BROADCAST
                                } else false
                            }
                        }
                    }
                    if (isPeripheral) {
                        onHeadphonesOrBluetoothDisconnected("AudioDeviceCallback removed peripheral")
                    }
                }
            }
            try {
                val am = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                am?.registerAudioDeviceCallback(audioDeviceCallback!!, null)
            } catch (e: Exception) {
                Log.w("MusicPlaybackService", "Failed to register audioDeviceCallback: ${e.message}")
            }
        }
    }

    private fun unregisterDisconnectReceivers() {
        disconnectReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (_: Exception) {}
            disconnectReceiver = null
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioDeviceCallback?.let {
                try {
                    val am = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                    am?.unregisterAudioDeviceCallback(it)
                } catch (_: Exception) {}
                audioDeviceCallback = null
            }
        }
    }

    private fun observeEngineForWidgetsAndNotification() {
        // 1. Automatically sync Play/Pause changes to Notification, MediaSession, and Home Screen Widgets
        serviceScope.launch {
            playerEngine.isPlaying.collect { playing ->
                val song = currentSong
                updatePlaybackState(if (playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED)
                if (song != null) {
                    val dur = playerEngine.durationMs.value.takeIf { it > 0L } ?: song.durationMs
                    WidgetUpdateHelper.updateAllWidgets(
                        context = this@MusicPlaybackService,
                        currentSong = song,
                        isPlaying = playing,
                        positionMs = playerEngine.currentPositionMs.value,
                        durationMs = dur,
                        updateQueueList = false
                    )
                    withContext(Dispatchers.IO) {
                        showNotification(song, playing)
                    }
                } else {
                    WidgetUpdateHelper.updateAllWidgets(
                        context = this@MusicPlaybackService,
                        currentSong = null,
                        isPlaying = false
                    )
                }
            }
        }

        // 2. Continuously update widget progress bar every ~900ms while playing
        serviceScope.launch {
            combine(
                playerEngine.currentPositionMs,
                playerEngine.durationMs,
                playerEngine.isPlaying
            ) { pos, dur, playing -> Triple(pos, dur, playing) }
                .collect { (pos, dur, playing) ->
                    val song = currentSong ?: return@collect
                    val now = System.currentTimeMillis()
                    if (playing && now - lastWidgetProgressUpdateMs >= 850L) {
                        lastWidgetProgressUpdateMs = now
                        val totalDur = if (dur > 0L) dur else song.durationMs
                        WidgetUpdateHelper.updateWidgetProgressOnly(
                            context = this@MusicPlaybackService,
                            currentSong = song,
                            isPlaying = true,
                            positionMs = pos,
                            durationMs = totalDur
                        )
                    }
                }
        }
    }

    fun setQueue(songs: List<Song>) {
        currentQueue = songs
        try {
            val queueItems = songs.take(100).mapIndexed { index, song ->
                val desc = android.media.MediaDescription.Builder()
                    .setMediaId(song.id.toString())
                    .setTitle(song.title)
                    .setSubtitle(song.artist)
                    .setDescription(song.album)
                    .setMediaUri(Uri.parse(song.uri))
                    .apply {
                        song.albumArtUri?.let { setIconUri(it) }
                    }
                    .build()
                MediaSession.QueueItem(desc, index.toLong())
            }
            mediaSession?.setQueue(queueItems)
            mediaSession?.setQueueTitle("Hashtag Playback Queue")
        } catch (e: Exception) {
            Log.w("MusicPlaybackService", "Error setting media session queue: ${e.message}")
        }
        currentSong?.let { updateMetadata(it) }
    }

    fun handleFastForward(seconds: Long = 15L) {
        if (!::playerEngine.isInitialized) return
        val current = playerEngine.currentPositionMs.value
        val total = playerEngine.durationMs.value.takeIf { it > 0L } ?: (currentSong?.durationMs ?: 0L)
        val newPos = (current + seconds * 1000L).coerceAtMost(total)
        playerEngine.seekTo(newPos)
        updatePlaybackState(if (isActuallyPlaying()) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED)
    }

    fun handleRewind(seconds: Long = 10L) {
        if (!::playerEngine.isInitialized) return
        val current = playerEngine.currentPositionMs.value
        val newPos = (current - seconds * 1000L).coerceAtLeast(0L)
        playerEngine.seekTo(newPos)
        updatePlaybackState(if (isActuallyPlaying()) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED)
    }

    fun handleSkipToQueueIndex(index: Int) {
        val cb = onPlayQueueIndexCallback
        if (cb != null && index in currentQueue.indices) {
            cb.invoke(index)
        } else if (index in currentQueue.indices) {
            val song = currentQueue[index]
            playSong(song)
        }
    }

    fun handleToggleFavorite() {
        val song = currentSong ?: return
        val cb = onToggleFavoriteCallback
        if (cb != null) {
            cb.invoke(song)
        } else {
            serviceScope.launch(Dispatchers.IO) {
                try {
                    val newFav = !song.isFavorite
                    val db = AppDatabase.getDatabase(this@MusicPlaybackService)
                    db.songDao().setFavorite(song.id, newFav)
                    val updated = song.copy(isFavorite = newFav)
                    withContext(Dispatchers.Main) {
                        currentSong = updated
                        updateMetadata(updated)
                        updatePlaybackState(if (isActuallyPlaying()) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED)
                        showNotification(updated, isActuallyPlaying())
                    }
                } catch (e: Exception) {
                    Log.w("MusicPlaybackService", "Failed to toggle favorite: ${e.message}")
                }
            }
        }
    }

    fun handleToggleShuffle() {
        val cb = onToggleShuffleCallback
        if (cb != null) {
            cb.invoke()
        } else {
            serviceScope.launch(Dispatchers.IO) {
                try {
                    val sm = SettingsManager(this@MusicPlaybackService)
                    val newShuffle = !sm.getLastShuffle()
                    sm.saveLastShuffle(newShuffle)
                    withContext(Dispatchers.Main) {
                        updatePlaybackState(if (isActuallyPlaying()) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun handleToggleRepeat() {
        val cb = onToggleRepeatCallback
        if (cb != null) {
            cb.invoke()
        } else {
            serviceScope.launch(Dispatchers.IO) {
                try {
                    val sm = SettingsManager(this@MusicPlaybackService)
                    val newRepeat = !sm.getLastRepeat()
                    sm.saveLastRepeat(newRepeat)
                    withContext(Dispatchers.Main) {
                        updatePlaybackState(if (isActuallyPlaying()) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun handlePlaySongId(songId: Long) {
        val cb = onPlaySongIdCallback
        if (cb != null) {
            cb.invoke(songId)
        } else {
            serviceScope.launch(Dispatchers.IO) {
                try {
                    val db = AppDatabase.getDatabase(this@MusicPlaybackService)
                    val entity = db.songDao().getSongById(songId)
                    if (entity != null) {
                        val song = entity.toSong()
                        withContext(Dispatchers.Main) {
                            playSong(song)
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun initMediaSession() {
        mediaSession = MediaSession(this, "HashtagMusicPlayerMediaSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    handleResumeOrRestore()
                }

                override fun onPause() {
                    playerEngine.pause()
                }

                override fun onSkipToNext() {
                    handleSkipNext()
                }

                override fun onSkipToPrevious() {
                    handleSkipPrev()
                }

                override fun onFastForward() {
                    handleFastForward()
                }

                override fun onRewind() {
                    handleRewind()
                }

                override fun onSkipToQueueItem(queueId: Long) {
                    handleSkipToQueueIndex(queueId.toInt())
                }

                override fun onSetRating(rating: Rating) {
                    handleToggleFavorite()
                }

                override fun onCustomAction(action: String, extras: Bundle?) {
                    when (action) {
                        ACTION_TOGGLE_FAVORITE -> handleToggleFavorite()
                        ACTION_TOGGLE_SHUFFLE -> handleToggleShuffle()
                        ACTION_TOGGLE_REPEAT -> handleToggleRepeat()
                        ACTION_FAST_FORWARD_15 -> handleFastForward()
                        ACTION_REWIND_10 -> handleRewind()
                    }
                }

                override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
                    mediaId?.toLongOrNull()?.let { songId ->
                        handlePlaySongId(songId)
                    }
                }

                override fun onSeekTo(pos: Long) {
                    playerEngine.seekTo(pos)
                    val song = currentSong
                    if (song != null) {
                        WidgetUpdateHelper.updateWidgetProgressOnly(
                            context = this@MusicPlaybackService,
                            currentSong = song,
                            isPlaying = playerEngine.isPlaying.value,
                            positionMs = pos,
                            durationMs = playerEngine.durationMs.value.takeIf { it > 0L } ?: song.durationMs
                        )
                        updatePlaybackState(if (isActuallyPlaying()) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED)
                    }
                }

                override fun onStop() {
                    playerEngine.pause()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            })
            setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
            isActive = true
        }
        sessionToken = mediaSession?.sessionToken
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground playback notification for Hashtag music player"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun playSong(song: Song, startPositionMs: Long = 0L) {
        currentSong = song
        playerEngine.playSong(song, startPositionMs)
        updatePlaybackState(PlaybackState.STATE_PLAYING)
        WidgetUpdateHelper.updateAllWidgets(
            context = this,
            currentSong = song,
            isPlaying = true,
            positionMs = startPositionMs,
            durationMs = song.durationMs,
            updateQueueList = true
        )
        serviceScope.launch(Dispatchers.IO) {
            updateMetadata(song)
            showNotification(song, true)
        }
    }

    fun prepareSong(song: Song, startPositionMs: Long = 0L) {
        currentSong = song
        playerEngine.prepareSong(song, startPositionMs)
        updatePlaybackState(PlaybackState.STATE_PAUSED)
        WidgetUpdateHelper.updateAllWidgets(
            context = this,
            currentSong = song,
            isPlaying = false,
            positionMs = startPositionMs,
            durationMs = song.durationMs,
            updateQueueList = true
        )
        serviceScope.launch(Dispatchers.IO) {
            updateMetadata(song)
        }
    }

    private fun updateMetadata(song: Song) {
        val artwork = getSongArtworkBitmap(song)
        val trackIndex = currentQueue.indexOfFirst { it.id == song.id }
        val trackNum = if (trackIndex >= 0) (trackIndex + 1).toLong() else song.trackNumber.toLong().takeIf { it > 0 } ?: 1L
        val totalTracks = currentQueue.size.toLong().takeIf { it > 0 } ?: 1L

        val metadata = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, song.title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, song.artist)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, song.album)
            .putString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST, song.artist)
            .putLong(MediaMetadata.METADATA_KEY_DURATION, song.durationMs)
            .putLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER, trackNum)
            .putLong(MediaMetadata.METADATA_KEY_NUM_TRACKS, totalTracks)
            .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, artwork)
            .putBitmap(MediaMetadata.METADATA_KEY_ART, artwork)
            .putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, song.title)
            .putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, song.artist)
            .putString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION, song.album)
            .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, song.id.toString())
            .putString(MediaMetadata.METADATA_KEY_MEDIA_URI, song.uri)
            .putRating(MediaMetadata.METADATA_KEY_RATING, Rating.newHeartRating(song.isFavorite))
            .build()
        mediaSession?.setMetadata(metadata)
    }

    private fun getSongArtworkBitmap(song: Song): Bitmap {
        if (cachedSongId == song.id && cachedArtworkBitmap != null) {
            return cachedArtworkBitmap!!
        }

        var loadedBitmap: Bitmap? = null

        song.albumArtUri?.let { uri ->
            try {
                contentResolver.openInputStream(uri)?.use { stream ->
                    val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                    loadedBitmap = BitmapFactory.decodeStream(stream, null, opts)
                }
            } catch (_: Exception) {}
        }

        if (loadedBitmap == null) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(this, Uri.parse(song.uri))
                val pictureBytes = retriever.embeddedPicture
                retriever.release()
                if (pictureBytes != null) {
                    val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                    loadedBitmap = BitmapFactory.decodeByteArray(pictureBytes, 0, pictureBytes.size, opts)
                }
            } catch (_: Exception) {}
        }

        val finalBitmap = loadedBitmap ?: run {
            val size = 256
            Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
                val canvas = Canvas(this)
                val gradient = LinearGradient(
                    0f, 0f, size.toFloat(), size.toFloat(),
                    intArrayOf(0xFF7C4DFF.toInt(), 0xFF311B92.toInt(), 0xFF120E24.toInt()),
                    null,
                    Shader.TileMode.CLAMP
                )
                val bgPaint = Paint().apply {
                    shader = gradient
                    isAntiAlias = true
                }
                canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

                val ringPaint = Paint().apply {
                    color = 0x22FFFFFF
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                    isAntiAlias = true
                }
                canvas.drawCircle(size / 2f, size / 2f, size * 0.38f, ringPaint)
                canvas.drawCircle(size / 2f, size / 2f, size * 0.26f, ringPaint)

                val centerPaint = Paint().apply {
                    color = 0xFFB388FF.toInt()
                    isAntiAlias = true
                }
                canvas.drawCircle(size / 2f, size / 2f, size * 0.12f, centerPaint)
            }
        }

        cachedSongId = song.id
        cachedArtworkBitmap = finalBitmap
        return finalBitmap
    }

    private fun updatePlaybackState(state: Int) {
        val position = playerEngine.currentPositionMs.value
        val isPlaying = state == PlaybackState.STATE_PLAYING
        val speed = if (isPlaying) 1.0f else 0.0f

        val actions = PlaybackState.ACTION_PLAY or
            PlaybackState.ACTION_PAUSE or
            PlaybackState.ACTION_PLAY_PAUSE or
            PlaybackState.ACTION_STOP or
            PlaybackState.ACTION_SKIP_TO_NEXT or
            PlaybackState.ACTION_SKIP_TO_PREVIOUS or
            PlaybackState.ACTION_SEEK_TO or
            PlaybackState.ACTION_FAST_FORWARD or
            PlaybackState.ACTION_REWIND or
            PlaybackState.ACTION_SKIP_TO_QUEUE_ITEM or
            PlaybackState.ACTION_SET_RATING

        val playbackState = PlaybackState.Builder()
            .setActions(actions)
            .setState(state, position, speed, SystemClock.elapsedRealtime())
            .addCustomAction(
                PlaybackState.CustomAction.Builder(
                    ACTION_TOGGLE_FAVORITE,
                    if (currentSong?.isFavorite == true) "Unfavorite" else "Favorite",
                    if (currentSong?.isFavorite == true) R.drawable.ic_notification_favorite_filled else R.drawable.ic_notification_favorite_border
                ).build()
            )
            .addCustomAction(
                PlaybackState.CustomAction.Builder(
                    ACTION_FAST_FORWARD_15,
                    "Fast Forward 15s",
                    android.R.drawable.ic_media_ff
                ).build()
            )
            .addCustomAction(
                PlaybackState.CustomAction.Builder(
                    ACTION_REWIND_10,
                    "Rewind 10s",
                    android.R.drawable.ic_media_rew
                ).build()
            )
            .build()
        mediaSession?.setPlaybackState(playbackState)
    }

    fun showNotification(song: Song, isPlaying: Boolean) {
        val openIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val favoriteIcon = if (song.isFavorite) {
            R.drawable.ic_notification_favorite_filled
        } else {
            R.drawable.ic_notification_favorite_border
        }
        val favoriteAction = Notification.Action.Builder(
            favoriteIcon,
            if (song.isFavorite) "Favorited" else "Favorite",
            getServicePendingIntent(ACTION_TOGGLE_FAVORITE)
        ).build()

        val playPauseAction = if (isPlaying) {
            Notification.Action.Builder(
                android.R.drawable.ic_media_pause,
                "Pause",
                getServicePendingIntent(ACTION_PAUSE)
            ).build()
        } else {
            Notification.Action.Builder(
                android.R.drawable.ic_media_play,
                "Play",
                getServicePendingIntent(ACTION_PLAY)
            ).build()
        }

        val prevAction = Notification.Action.Builder(
            android.R.drawable.ic_media_previous,
            "Previous",
            getServicePendingIntent(ACTION_PREV)
        ).build()

        val nextAction = Notification.Action.Builder(
            android.R.drawable.ic_media_next,
            "Next",
            getServicePendingIntent(ACTION_NEXT)
        ).build()

        val artworkBitmap = getSongArtworkBitmap(song)

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }

        val notification = builder
            .setContentTitle(song.title)
            .setContentText("${song.artist} • ${song.album}")
            .setSubText(song.techSpecsTag)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(artworkBitmap)
            .setContentIntent(openPendingIntent)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(1, 2, 3)
            )
            .addAction(favoriteAction)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .setOngoing(isPlaying)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            android.util.Log.w("MusicPlaybackService", "Could not start foreground: ${e.message}")
        }
    }

    private fun getServicePendingIntent(action: String): PendingIntent {
        val intent = Intent(this, MusicPlaybackService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this, action.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun handleResumeOrRestore() {
        if (currentSong != null) {
            playerEngine.resume()
        } else {
            // Restore last song from database if widget was tapped when service had no active song
            serviceScope.launch(Dispatchers.IO) {
                try {
                    val settingsManager = SettingsManager(this@MusicPlaybackService)
                    val lastId = settingsManager.getLastSongId()
                    val lastPos = settingsManager.getLastPositionMs()
                    val db = AppDatabase.getDatabase(this@MusicPlaybackService)
                    val all = db.songDao().getAllSongsOnce().map { it.toSong() }
                    val target = all.find { it.id == lastId } ?: all.firstOrNull()
                    if (target != null) {
                        withContext(Dispatchers.Main) {
                            currentSong = target
                            playerEngine.playSong(target, lastPos)
                            updatePlaybackState(PlaybackState.STATE_PLAYING)
                        }
                        updateMetadata(target)
                        showNotification(target, true)
                    }
                } catch (e: Exception) {
                    android.util.Log.w("MusicPlaybackService", "Failed to restore song from widget: ${e.message}")
                }
            }
        }
    }

    private fun handleSkipNext() {
        val cb = onSkipNextCallback
        if (cb != null) {
            cb.invoke()
        } else {
            // Standalone fallback if Activity is unbound
            serviceScope.launch(Dispatchers.IO) {
                try {
                    val settingsManager = SettingsManager(this@MusicPlaybackService)
                    val qIds = settingsManager.getLastQueueIds()
                    val curIdx = settingsManager.getLastQueueIndex()
                    val db = AppDatabase.getDatabase(this@MusicPlaybackService)
                    val all = db.songDao().getAllSongsOnce().map { it.toSong() }
                    if (all.isNotEmpty()) {
                        val map = all.associateBy { it.id }
                        val q = qIds.mapNotNull { map[it] }.ifEmpty { all }
                        val nextIdx = (curIdx + 1) % q.size
                        val nextSong = q[nextIdx]
                        settingsManager.saveLastPlaybackState(
                            songId = nextSong.id,
                            positionMs = 0L,
                            queueIds = q.map { it.id },
                            queueIndex = nextIdx,
                            isShuffle = settingsManager.getLastShuffle(),
                            isRepeat = settingsManager.getLastRepeat()
                        )
                        withContext(Dispatchers.Main) {
                            playSong(nextSong)
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun handleSkipPrev() {
        val cb = onSkipPrevCallback
        if (cb != null) {
            cb.invoke()
        } else {
            serviceScope.launch(Dispatchers.IO) {
                try {
                    val settingsManager = SettingsManager(this@MusicPlaybackService)
                    val qIds = settingsManager.getLastQueueIds()
                    val curIdx = settingsManager.getLastQueueIndex()
                    val db = AppDatabase.getDatabase(this@MusicPlaybackService)
                    val all = db.songDao().getAllSongsOnce().map { it.toSong() }
                    if (all.isNotEmpty()) {
                        val map = all.associateBy { it.id }
                        val q = qIds.mapNotNull { map[it] }.ifEmpty { all }
                        val prevIdx = if (curIdx - 1 < 0) q.size - 1 else curIdx - 1
                        val prevSong = q[prevIdx]
                        settingsManager.saveLastPlaybackState(
                            songId = prevSong.id,
                            positionMs = 0L,
                            queueIds = q.map { it.id },
                            queueIndex = prevIdx,
                            isShuffle = settingsManager.getLastShuffle(),
                            isRepeat = settingsManager.getLastRepeat()
                        )
                        withContext(Dispatchers.Main) {
                            playSong(prevSong)
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE_PLAY_PAUSE -> {
                if (playerEngine.isPlaying.value) {
                    playerEngine.pause()
                } else {
                    handleResumeOrRestore()
                }
            }
            ACTION_PLAY -> {
                handleResumeOrRestore()
            }
            ACTION_PAUSE -> {
                playerEngine.pause()
            }
            ACTION_NEXT -> {
                handleSkipNext()
            }
            ACTION_PREV -> {
                handleSkipPrev()
            }
            ACTION_TOGGLE_FAVORITE -> {
                handleToggleFavorite()
            }
            ACTION_TOGGLE_SHUFFLE -> {
                handleToggleShuffle()
            }
            ACTION_TOGGLE_REPEAT -> {
                handleToggleRepeat()
            }
            ACTION_FAST_FORWARD_15 -> {
                handleFastForward()
            }
            ACTION_REWIND_10 -> {
                handleRewind()
            }
            ACTION_PLAY_QUEUE_INDEX -> {
                val queueIdx = intent.getIntExtra(EXTRA_QUEUE_INDEX, -1)
                if (queueIdx >= 0) {
                    val cb = onPlayQueueIndexCallback
                    if (cb != null) {
                        cb.invoke(queueIdx)
                    } else {
                        serviceScope.launch(Dispatchers.IO) {
                            try {
                                val settingsManager = SettingsManager(this@MusicPlaybackService)
                                val qIds = settingsManager.getLastQueueIds()
                                val db = AppDatabase.getDatabase(this@MusicPlaybackService)
                                val all = db.songDao().getAllSongsOnce().map { it.toSong() }
                                val map = all.associateBy { it.id }
                                val q = qIds.mapNotNull { map[it] }
                                if (queueIdx in q.indices) {
                                    val target = q[queueIdx]
                                    settingsManager.saveLastPlaybackState(
                                        songId = target.id,
                                        positionMs = 0L,
                                        queueIds = qIds,
                                        queueIndex = queueIdx,
                                        isShuffle = settingsManager.getLastShuffle(),
                                        isRepeat = settingsManager.getLastRepeat()
                                    )
                                    withContext(Dispatchers.Main) {
                                        playSong(target)
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
            ACTION_STOP -> {
                playerEngine.pause()
                WidgetUpdateHelper.updateAllWidgets(
                    context = this,
                    currentSong = currentSong,
                    isPlaying = false,
                    positionMs = playerEngine.currentPositionMs.value,
                    durationMs = currentSong?.durationMs ?: 0L
                )
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        if (intent?.action == SERVICE_INTERFACE || intent?.action == "android.media.browse.MediaBrowserService") {
            return super.onBind(intent)
        }
        return binder
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot {
        return BrowserRoot(MEDIA_ROOT_ID, null)
    }

    override fun onLoadChildren(parentId: String, result: Result<List<MediaBrowser.MediaItem>>) {
        result.detach()
        serviceScope.launch(Dispatchers.IO) {
            val mediaItems = ArrayList<MediaBrowser.MediaItem>()
            try {
                val db = AppDatabase.getDatabase(this@MusicPlaybackService)
                when (parentId) {
                    MEDIA_ROOT_ID -> {
                        mediaItems.add(createBrowsableItem(MEDIA_FAVORITES_ID, "Favorites", "Starred tracks"))
                        mediaItems.add(createBrowsableItem(MEDIA_ALL_SONGS_ID, "All Songs", "All library tracks"))
                        mediaItems.add(createBrowsableItem(MEDIA_RECENT_ID, "Recently Added", "Recently added tracks"))
                    }
                    MEDIA_FAVORITES_ID -> {
                        val songs = db.songDao().getFavoritesOnce().map { it.toSong() }
                        for (song in songs) {
                            mediaItems.add(createPlayableItem(song))
                        }
                    }
                    MEDIA_ALL_SONGS_ID -> {
                        val songs = db.songDao().getAllSongsOnce().map { it.toSong() }
                        for (song in songs) {
                            mediaItems.add(createPlayableItem(song))
                        }
                    }
                    MEDIA_RECENT_ID -> {
                        val songs = db.songDao().getRecentlyAddedOnce().map { it.toSong() }
                        for (song in songs) {
                            mediaItems.add(createPlayableItem(song))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("MusicPlaybackService", "Error loading media children: ${e.message}")
            }
            result.sendResult(mediaItems)
        }
    }

    private fun createBrowsableItem(id: String, title: String, subtitle: String): MediaBrowser.MediaItem {
        val desc = android.media.MediaDescription.Builder()
            .setMediaId(id)
            .setTitle(title)
            .setSubtitle(subtitle)
            .build()
        return MediaBrowser.MediaItem(desc, MediaBrowser.MediaItem.FLAG_BROWSABLE)
    }

    private fun createPlayableItem(song: Song): MediaBrowser.MediaItem {
        val desc = android.media.MediaDescription.Builder()
            .setMediaId(song.id.toString())
            .setTitle(song.title)
            .setSubtitle(song.artist)
            .setDescription(song.album)
            .setMediaUri(Uri.parse(song.uri))
            .apply {
                song.albumArtUri?.let { setIconUri(it) }
            }
            .build()
        return MediaBrowser.MediaItem(desc, MediaBrowser.MediaItem.FLAG_PLAYABLE)
    }

    override fun onDestroy() {
        activeInstance = null
        WidgetUpdateHelper.updateAllWidgets(
            context = this,
            currentSong = currentSong,
            isPlaying = false,
            positionMs = if (::playerEngine.isInitialized) playerEngine.currentPositionMs.value else 0L,
            durationMs = currentSong?.durationMs ?: 0L
        )
        super.onDestroy()
        unregisterDisconnectReceivers()
        serviceScope.cancel()
        playerEngine.release()
        mediaSession?.release()
    }
}
