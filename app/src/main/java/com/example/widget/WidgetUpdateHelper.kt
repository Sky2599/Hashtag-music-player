package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.model.Song
import com.example.data.util.SettingsManager
import com.example.service.MusicPlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object WidgetUpdateHelper {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastArtworkCache: Pair<Long, Bitmap>? = null

    /**
     * Refreshes all widgets from live service state if running, or falls back to persisted state
     * with isPlaying = false so widgets never falsely claim music is playing when stopped.
     */
    fun refreshWidgetsFromState(context: Context) {
        val activeService = MusicPlaybackService.activeInstance
        if (activeService != null) {
            val song = activeService.currentSong
            val playing = activeService.isActuallyPlaying()
            val pos = activeService.getCurrentPositionMs()
            val dur = activeService.getDurationMs().takeIf { it > 0L } ?: (song?.durationMs ?: 0L)
            updateAllWidgets(
                context = context,
                currentSong = song,
                isPlaying = playing,
                positionMs = pos,
                durationMs = dur,
                updateQueueList = true
            )
            return
        }

        scope.launch {
            try {
                val settingsManager = SettingsManager(context)
                val lastSongId = settingsManager.getLastSongId()
                val lastPos = settingsManager.getLastPositionMs()
                var restoredSong: Song? = null
                if (lastSongId != -1L) {
                    val db = AppDatabase.getDatabase(context)
                    val songs = db.songDao().getAllSongsOnce()
                    restoredSong = songs.find { it.id == lastSongId }?.toSong()
                }
                updateAllWidgetsInternal(
                    context = context,
                    currentSong = restoredSong,
                    isPlaying = false, // Service is not running -> definitely NOT playing
                    positionMs = lastPos,
                    durationMs = restoredSong?.durationMs ?: 0L,
                    updateQueueList = true
                )
            } catch (e: Exception) {
                android.util.Log.w("WidgetUpdateHelper", "refreshWidgetsFromState error: ${e.message}")
            }
        }
    }

    fun updateAllWidgets(
        context: Context,
        currentSong: Song?,
        isPlaying: Boolean,
        positionMs: Long = 0L,
        durationMs: Long = currentSong?.durationMs ?: 0L,
        queue: List<Song> = emptyList(),
        updateQueueList: Boolean = false
    ) {
        scope.launch {
            updateAllWidgetsInternal(
                context = context,
                currentSong = currentSong,
                isPlaying = isPlaying,
                positionMs = positionMs,
                durationMs = durationMs,
                updateQueueList = updateQueueList
            )
        }
    }

    /**
     * Lightweight progress-only update for the Audiophile Widget seek bar and time labels.
     * Avoids re-decoding Bitmaps or resetting adapters every second.
     */
    fun updateWidgetProgressOnly(
        context: Context,
        currentSong: Song?,
        isPlaying: Boolean,
        positionMs: Long,
        durationMs: Long
    ) {
        if (currentSong == null) return
        scope.launch {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val audiophileIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, AudiophilePlayerWidgetProvider::class.java)
                )
                if (audiophileIds.isNotEmpty()) {
                    val views = RemoteViews(context.packageName, R.layout.widget_audiophile_player)
                    val total = durationMs.coerceAtLeast(1L)
                    val current = positionMs.coerceIn(0L, total)
                    val progressPercent = ((current * 1000L) / total).toInt().coerceIn(0, 1000)
                    views.setProgressBar(R.id.widget_progress_bar, 1000, progressPercent, false)
                    views.setTextViewText(R.id.widget_time_elapsed, formatDuration(current))
                    views.setTextViewText(R.id.widget_time_duration, formatDuration(total))
                    views.setImageViewResource(
                        R.id.widget_audiophile_play_pause,
                        if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
                    )
                    appWidgetManager.partiallyUpdateAppWidget(audiophileIds, views)
                }
            } catch (e: Exception) {
                android.util.Log.w("WidgetUpdateHelper", "Progress update failed: ${e.message}")
            }
        }
    }

    private fun updateAllWidgetsInternal(
        context: Context,
        currentSong: Song?,
        isPlaying: Boolean,
        positionMs: Long,
        durationMs: Long,
        updateQueueList: Boolean
    ) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)

            // 1. Compact Player Widget
            val compactIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, CompactPlayerWidgetProvider::class.java)
            )
            if (compactIds.isNotEmpty()) {
                val views = buildCompactPlayerRemoteViews(context, currentSong, isPlaying)
                appWidgetManager.updateAppWidget(compactIds, views)
            }

            // 2. Detailed Audiophile Widget
            val audiophileIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, AudiophilePlayerWidgetProvider::class.java)
            )
            if (audiophileIds.isNotEmpty()) {
                val views = buildAudiophilePlayerRemoteViews(
                    context, currentSong, isPlaying, positionMs, durationMs
                )
                appWidgetManager.updateAppWidget(audiophileIds, views)
            }

            // 3. Quick Queue Widget
            val queueIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, QuickQueueWidgetProvider::class.java)
            )
            if (queueIds.isNotEmpty()) {
                val views = buildQuickQueueRemoteViews(context, currentSong, isPlaying)
                appWidgetManager.updateAppWidget(queueIds, views)
                if (updateQueueList) {
                    appWidgetManager.notifyAppWidgetViewDataChanged(queueIds, R.id.widget_queue_list)
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("WidgetUpdateHelper", "Failed to update widgets: ${e.message}")
        }
    }

    private fun buildCompactPlayerRemoteViews(
        context: Context,
        song: Song?,
        isPlaying: Boolean
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_compact_player)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context, 100, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)

        if (song != null) {
            views.setTextViewText(R.id.widget_title, song.title)
            views.setTextViewText(R.id.widget_artist, "${song.artist} • ${song.album}")
            val artwork = getArtworkBitmap(context, song, 120)
            views.setImageViewBitmap(R.id.widget_artwork, artwork)
        } else {
            views.setTextViewText(R.id.widget_title, "Hashtag Music Player")
            views.setTextViewText(R.id.widget_artist, "Tap to open library")
            views.setImageViewResource(R.id.widget_artwork, R.drawable.ic_widget_music)
        }

        // Use ACTION_TOGGLE_PLAY_PAUSE so clicking is always instantaneous and never out-of-sync
        val playPauseIntent = Intent(context, MusicPlaybackService::class.java).apply {
            action = MusicPlaybackService.ACTION_TOGGLE_PLAY_PAUSE
        }
        val playPausePendingIntent = PendingIntent.getService(
            context, 101, playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_btn_play_pause, playPausePendingIntent)
        views.setImageViewResource(
            R.id.widget_btn_play_pause,
            if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
        )

        val prevIntent = Intent(context, MusicPlaybackService::class.java).apply {
            action = MusicPlaybackService.ACTION_PREV
        }
        val prevPendingIntent = PendingIntent.getService(
            context, 102, prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_btn_prev, prevPendingIntent)

        val nextIntent = Intent(context, MusicPlaybackService::class.java).apply {
            action = MusicPlaybackService.ACTION_NEXT
        }
        val nextPendingIntent = PendingIntent.getService(
            context, 103, nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_btn_next, nextPendingIntent)

        return views
    }

    private fun buildAudiophilePlayerRemoteViews(
        context: Context,
        song: Song?,
        isPlaying: Boolean,
        positionMs: Long,
        durationMs: Long
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_audiophile_player)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context, 200, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_audiophile_root, openAppPendingIntent)

        if (song != null) {
            views.setTextViewText(R.id.widget_audiophile_title, song.title)
            views.setTextViewText(R.id.widget_audiophile_artist, "${song.artist} — ${song.album}")
            views.setTextViewText(R.id.widget_format_tag, "${song.format} • ${song.bitrateKbps} kbps")
            views.setTextViewText(R.id.widget_dsp_tag, "${song.sampleRateHz / 1000}kHz • 32-bit DSP")

            val artwork = getArtworkBitmap(context, song, 160)
            views.setImageViewBitmap(R.id.widget_audiophile_artwork, artwork)

            val total = durationMs.coerceAtLeast(1L)
            val current = positionMs.coerceIn(0L, total)
            val progressPercent = ((current * 1000L) / total).toInt().coerceIn(0, 1000)
            views.setProgressBar(R.id.widget_progress_bar, 1000, progressPercent, false)

            views.setTextViewText(R.id.widget_time_elapsed, formatDuration(current))
            views.setTextViewText(R.id.widget_time_duration, formatDuration(total))
        } else {
            views.setTextViewText(R.id.widget_audiophile_title, "Hashtag Music Player")
            views.setTextViewText(R.id.widget_audiophile_artist, "32-bit Bit-Perfect Local Audio")
            views.setTextViewText(R.id.widget_format_tag, "Hi-Res Ready")
            views.setTextViewText(R.id.widget_dsp_tag, "AAudio Low Latency")
            views.setImageViewResource(R.id.widget_audiophile_artwork, R.drawable.ic_widget_music)
            views.setProgressBar(R.id.widget_progress_bar, 1000, 0, false)
            views.setTextViewText(R.id.widget_time_elapsed, "00:00")
            views.setTextViewText(R.id.widget_time_duration, "00:00")
        }

        val playPauseIntent = Intent(context, MusicPlaybackService::class.java).apply {
            action = MusicPlaybackService.ACTION_TOGGLE_PLAY_PAUSE
        }
        val playPausePendingIntent = PendingIntent.getService(
            context, 201, playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_audiophile_play_pause, playPausePendingIntent)
        views.setImageViewResource(
            R.id.widget_audiophile_play_pause,
            if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
        )

        val prevIntent = Intent(context, MusicPlaybackService::class.java).apply {
            action = MusicPlaybackService.ACTION_PREV
        }
        val prevPendingIntent = PendingIntent.getService(
            context, 202, prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_audiophile_prev, prevPendingIntent)

        val nextIntent = Intent(context, MusicPlaybackService::class.java).apply {
            action = MusicPlaybackService.ACTION_NEXT
        }
        val nextPendingIntent = PendingIntent.getService(
            context, 203, nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_audiophile_next, nextPendingIntent)

        return views
    }

    private fun buildQuickQueueRemoteViews(
        context: Context,
        song: Song?,
        isPlaying: Boolean
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_quick_queue)

        if (song != null) {
            views.setTextViewText(R.id.widget_queue_current_title, song.title)
            views.setTextViewText(
                R.id.widget_queue_current_artist,
                "${song.artist} • ${if (isPlaying) "Now Playing" else "Paused"}"
            )
            val artwork = getArtworkBitmap(context, song, 100)
            views.setImageViewBitmap(R.id.widget_queue_artwork, artwork)
        } else {
            views.setTextViewText(R.id.widget_queue_current_title, "Now Playing Queue")
            views.setTextViewText(R.id.widget_queue_current_artist, "Tap track to play")
            views.setImageViewResource(R.id.widget_queue_artwork, R.drawable.ic_widget_music)
        }

        val playPauseIntent = Intent(context, MusicPlaybackService::class.java).apply {
            action = MusicPlaybackService.ACTION_TOGGLE_PLAY_PAUSE
        }
        val playPausePendingIntent = PendingIntent.getService(
            context, 301, playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_queue_btn_play_pause, playPausePendingIntent)
        views.setImageViewResource(
            R.id.widget_queue_btn_play_pause,
            if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
        )

        val nextIntent = Intent(context, MusicPlaybackService::class.java).apply {
            action = MusicPlaybackService.ACTION_NEXT
        }
        val nextPendingIntent = PendingIntent.getService(
            context, 302, nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_queue_btn_next, nextPendingIntent)

        val serviceIntent = Intent(context, QueueWidgetService::class.java)
        views.setRemoteAdapter(R.id.widget_queue_list, serviceIntent)
        views.setEmptyView(R.id.widget_queue_list, R.id.widget_queue_empty)

        val itemClickIntent = Intent(context, MusicPlaybackService::class.java).apply {
            action = MusicPlaybackService.ACTION_PLAY_QUEUE_INDEX
        }
        val itemClickPendingIntent = PendingIntent.getService(
            context, 303, itemClickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        views.setPendingIntentTemplate(R.id.widget_queue_list, itemClickPendingIntent)

        return views
    }

    private fun getArtworkBitmap(context: Context, song: Song, targetSize: Int): Bitmap {
        val cached = lastArtworkCache
        if (cached != null && cached.first == song.id && cached.second.width == targetSize) {
            return cached.second
        }

        var loaded: Bitmap? = null
        song.albumArtUri?.let { uri ->
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                    loaded = BitmapFactory.decodeStream(stream, null, opts)
                }
            } catch (_: Exception) {}
        }

        if (loaded == null && song.uri.isNotEmpty()) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, Uri.parse(song.uri))
                val pictureBytes = retriever.embeddedPicture
                retriever.release()
                if (pictureBytes != null) {
                    val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                    loaded = BitmapFactory.decodeByteArray(pictureBytes, 0, pictureBytes.size, opts)
                }
            } catch (_: Exception) {}
        }

        val bitmap = loaded ?: run {
            val bmp = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val gradient = LinearGradient(
                0f, 0f, targetSize.toFloat(), targetSize.toFloat(),
                intArrayOf(0xFFFFB700.toInt(), 0xFF7C4DFF.toInt(), 0xFF1A002C.toInt()),
                null,
                Shader.TileMode.CLAMP
            )
            val paint = Paint().apply {
                shader = gradient
                isAntiAlias = true
            }
            canvas.drawRoundRect(0f, 0f, targetSize.toFloat(), targetSize.toFloat(), 16f, 16f, paint)
            bmp
        }

        val scaled = if (bitmap.width != targetSize || bitmap.height != targetSize) {
            Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true)
        } else {
            bitmap
        }

        lastArtworkCache = Pair(song.id, scaled)
        return scaled
    }

    private fun formatDuration(ms: Long): String {
        val totalSec = (ms / 1000L).coerceAtLeast(0L)
        val min = totalSec / 60L
        val sec = totalSec % 60L
        return String.format("%02d:%02d", min, sec)
    }
}
