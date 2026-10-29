package com.example.data.model

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long = 0,
    val durationMs: Long,
    val uri: String,
    val path: String = "",
    val mimeType: String = "audio/mpeg",
    val sizeBytes: Long = 0,
    val bitrateKbps: Int = 320,
    val sampleRateHz: Int = 44100,
    val bitDepth: Int = 16,
    val format: String = "MP3",
    val playCount: Int = 0,
    val isFavorite: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis(),
    val customArtworkUri: String? = null,
    val trackNumber: Int = 0
) {
    val albumArtUri: Uri? by lazy(LazyThreadSafetyMode.NONE) {
        if (!customArtworkUri.isNullOrEmpty()) {
            Uri.parse(customArtworkUri)
        } else if (albumId > 0 && !album.equals("Unknown Album", ignoreCase = true) && album.isNotBlank()) {
            Uri.parse("content://media/external/audio/albumart/$albumId")
        } else null
    }

    val durationFormatted: String by lazy(LazyThreadSafetyMode.NONE) {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        String.format("%02d:%02d", minutes, seconds)
    }

    val techSpecsTag: String by lazy(LazyThreadSafetyMode.NONE) {
        "$format • ${bitrateKbps} kbps • ${sampleRateHz / 1000.0} kHz • ${bitDepth}-bit"
    }
}
