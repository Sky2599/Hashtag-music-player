package com.example.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.model.Song
import com.example.data.util.SettingsManager
import com.example.service.MusicPlaybackService
import kotlinx.coroutines.runBlocking

class QueueWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return QueueRemoteViewsFactory(applicationContext)
    }
}

class QueueRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private val queueSongs = mutableListOf<Song>()
    private var currentSongId: Long = -1L

    override fun onCreate() {
        loadQueueData()
    }

    override fun onDataSetChanged() {
        loadQueueData()
    }

    private fun loadQueueData() {
        try {
            val settingsManager = SettingsManager(context)
            val queueIds = settingsManager.getLastQueueIds()
            currentSongId = settingsManager.getLastSongId()

            if (queueIds.isNotEmpty()) {
                val db = AppDatabase.getDatabase(context)
                runBlocking {
                    val all = db.songDao().getAllSongsOnce().map { it.toSong() }
                    val map = all.associateBy { it.id }
                    queueSongs.clear()
                    queueIds.forEach { id ->
                        map[id]?.let { queueSongs.add(it) }
                    }
                }
            } else {
                queueSongs.clear()
            }
        } catch (e: Exception) {
            android.util.Log.w("QueueRemoteViewsFactory", "Failed to load queue data: ${e.message}")
        }
    }

    override fun onDestroy() {
        queueSongs.clear()
    }

    override fun getCount(): Int = queueSongs.size

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_queue_item)
        if (position in queueSongs.indices) {
            val song = queueSongs[position]
            val isCurrent = song.id == currentSongId

            views.setTextViewText(R.id.widget_queue_item_index, if (isCurrent) "▶" else "${position + 1}")
            views.setTextViewText(R.id.widget_queue_item_title, song.title)
            views.setTextViewText(R.id.widget_queue_item_artist, "${song.artist} • ${song.album}")
            views.setTextViewText(R.id.widget_queue_item_duration, song.durationFormatted)

            // Fill-in Intent to play this queue index on click
            val fillInIntent = Intent().apply {
                putExtra(MusicPlaybackService.EXTRA_QUEUE_INDEX, position)
                putExtra(MusicPlaybackService.EXTRA_SONG_ID, song.id)
            }
            views.setOnClickFillInIntent(R.id.widget_queue_item_root, fillInIntent)
        }
        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
