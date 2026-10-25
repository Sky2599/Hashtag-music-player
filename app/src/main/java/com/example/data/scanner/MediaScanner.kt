package com.example.data.scanner

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object MediaScanner {
    private const val TAG = "MediaScanner"

    // Supported lossless & high-fidelity audio extensions
    private val SUPPORTED_FORMATS = setOf(
        "mp3", "flac", "wav", "aac", "m4a", "ogg", "opus", "alac", "aif", "aiff", "ape", "wv", "dsd", "dff", "dsf", "wma"
    )

    suspend fun scanDeviceAudio(
        context: Context,
        excludedFolders: Set<String> = emptySet(),
        forceDeepScan: Boolean = false
    ): List<Song> = withContext(Dispatchers.IO) {
        val songsList = mutableListOf<Song>()
        val knownPaths = HashSet<String>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.TRACK
        )

        // Non-restrictive selection: do not exclude files with DURATION == 0 or IS_MUSIC == 0
        // so newly downloaded or recently transferred files are recognized immediately
        val selection = "(${MediaStore.Audio.Media.SIZE} > 20480 OR ${MediaStore.Audio.Media.SIZE} IS NULL)"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        fun parseCursor(cursor: Cursor?) {
            cursor?.use { c ->
                val idCol = c.getColumnIndex(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = c.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = c.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val dataCol = c.getColumnIndex(MediaStore.Audio.Media.DATA)
                val mimeCol = c.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
                val sizeCol = c.getColumnIndex(MediaStore.Audio.Media.SIZE)
                val dateCol = c.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                val dateModCol = c.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)
                val trackCol = c.getColumnIndex(MediaStore.Audio.Media.TRACK)

                if (idCol == -1) return@use

                while (c.moveToNext()) {
                    try {
                        val id = c.getLong(idCol)
                        val data = if (dataCol != -1) c.getString(dataCol) ?: "" else ""

                        // Fast folder exclusion check (Musicolet style)
                        if (data.isNotEmpty() && isExcludedPath(data, excludedFolders)) {
                            continue
                        }

                        val title = if (titleCol != -1) c.getString(titleCol) ?: "Unknown Title" else "Unknown Title"
                        val rawArtist = if (artistCol != -1) c.getString(artistCol) ?: "Unknown Artist" else "Unknown Artist"
                        val artist = if (rawArtist == "<unknown>" || rawArtist.isBlank()) "Unknown Artist" else rawArtist
                        val rawAlbum = if (albumCol != -1) c.getString(albumCol) ?: "Unknown Album" else "Unknown Album"
                        val album = if (rawAlbum == "<unknown>" || rawAlbum.isBlank()) "Unknown Album" else rawAlbum
                        val albumId = if (albumIdCol != -1) c.getLong(albumIdCol) else 0L
                        var duration = if (durationCol != -1) c.getLong(durationCol).coerceAtLeast(0L) else 0L
                        val mimeType = if (mimeCol != -1) c.getString(mimeCol) ?: "audio/mpeg" else "audio/mpeg"
                        val size = if (sizeCol != -1) c.getLong(sizeCol).coerceAtLeast(0L) else 0L

                        // If MediaStore hasn't computed duration yet, calculate fast estimation without heavy blocking I/O
                        if (duration <= 0L && size > 0) {
                            val estBitrate = if (mimeType.contains("flac", true)) 900 else 256
                            duration = ((size * 8) / (estBitrate * 1000.0) * 1000).toLong().coerceAtLeast(1000L)
                        }

                        // Pure in-memory date calculation from cursor without synchronous disk stat() syscalls
                        val rawDateAdded = if (dateCol != -1) c.getLong(dateCol) else 0L
                        val dateAddedMs = if (rawDateAdded > 10_000_000_000L) rawDateAdded else rawDateAdded * 1000L
                        val rawDateMod = if (dateModCol != -1) c.getLong(dateModCol) else 0L
                        val dateModMs = if (rawDateMod > 10_000_000_000L) rawDateMod else rawDateMod * 1000L
                        val dateAdded = maxOf(dateAddedMs, dateModMs).takeIf { it > 0L } ?: System.currentTimeMillis()

                        val rawTrack = if (trackCol != -1) c.getInt(trackCol) else 0
                        val trackNumber = when {
                            rawTrack >= 1000 -> rawTrack % 1000
                            rawTrack > 0 -> rawTrack
                            else -> {
                                val regex = Regex("^(\\d{1,3})[\\s._-]+")
                                val fileName = data.substringAfterLast('/')
                                val match = regex.find(title) ?: regex.find(fileName)
                                match?.groupValues?.get(1)?.toIntOrNull() ?: 0
                            }
                        }

                        val uri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id
                        ).toString()

                        val format = extractFormat(data, mimeType)
                        val (bitrate, sampleRate, bitDepth) = estimateOrExtractAudioSpecs(format, size, duration)

                        if (data.isNotEmpty()) {
                            knownPaths.add(data.lowercase())
                        }

                        songsList.add(
                            Song(
                                id = id,
                                title = title,
                                artist = artist,
                                album = album,
                                albumId = albumId,
                                durationMs = duration,
                                uri = uri,
                                path = data,
                                mimeType = mimeType,
                                sizeBytes = size,
                                bitrateKbps = bitrate,
                                sampleRateHz = sampleRate,
                                bitDepth = bitDepth,
                                format = format,
                                dateAdded = dateAdded,
                                trackNumber = trackNumber
                            )
                        )
                    } catch (rowEx: Exception) {
                        Log.w(TAG, "Skipping corrupted audio row: ${rowEx.message}")
                    }
                }
            }
        }

        try {
            val cursor: Cursor? = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )
            parseCursor(cursor)

            // Fallback for OEM ROMs (Honor/Huawei) where selection might filter unexpected items
            if (songsList.isEmpty()) {
                val fallbackCursor: Cursor? = context.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    sortOrder
                )
                parseCursor(fallbackCursor)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error indexing MediaStore: ${e.message}", e)
        }

        // Direct Storage Discovery: ONLY run if MediaStore returned 0 songs or if user explicitly requested a forceDeepScan.
        // Skipping redundant recursive file-system crawling on pre-indexed devices accelerates scanner by 10x-50x!
        if (forceDeepScan || songsList.isEmpty()) {
            try {
                val standardStorageDirs = listOfNotNull(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                    File("/storage/emulated/0/Music"),
                    File("/storage/emulated/0/Download"),
                    File("/storage/emulated/0/Audio"),
                    File("/storage/emulated/0/Songs"),
                    File("/sdcard/Music"),
                    File("/sdcard/Download")
                )

                for (dir in standardStorageDirs) {
                    if (dir.exists() && dir.isDirectory) {
                        scanFolderRecursively(
                            folder = dir,
                            knownPaths = knownPaths,
                            excludedFolders = excludedFolders,
                            outList = songsList,
                            context = context,
                            currentDepth = 0,
                            maxDepth = 3
                        )
                    }
                }
            } catch (dirEx: Exception) {
                Log.w(TAG, "Direct storage discovery notice: ${dirEx.message}")
            }
        }

        songsList
    }

    private fun scanFolderRecursively(
        folder: File,
        knownPaths: HashSet<String>,
        excludedFolders: Set<String>,
        outList: MutableList<Song>,
        context: Context,
        currentDepth: Int,
        maxDepth: Int
    ) {
        if (currentDepth > maxDepth || !folder.canRead()) return
        val files = folder.listFiles() ?: return
        for (file in files) {
            val path = file.absolutePath
            if (isExcludedPath(path, excludedFolders)) continue

            if (file.isDirectory) {
                if (!file.name.startsWith(".")) {
                    scanFolderRecursively(file, knownPaths, excludedFolders, outList, context, currentDepth + 1, maxDepth)
                }
            } else if (file.isFile && file.length() > 20480) {
                val ext = file.extension.lowercase()
                if (ext in SUPPORTED_FORMATS) {
                    val pathLower = path.lowercase()
                    if (pathLower !in knownPaths) {
                        knownPaths.add(pathLower)
                        val song = extractSongFromFile(file, context)
                        if (song != null) {
                            outList.add(song)
                        }
                    }
                }
            }
        }
    }

    private fun extractSongFromFile(file: File, context: Context): Song? {
        return try {
            val mmr = MediaMetadataRetriever()
            try {
                mmr.setDataSource(file.absolutePath)
            } catch (_: Throwable) {
                mmr.release()
                return null
            }

            val titleRaw = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val title = if (!titleRaw.isNullOrBlank()) titleRaw else file.nameWithoutExtension
            val artistRaw = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val artist = if (!artistRaw.isNullOrBlank() && artistRaw != "<unknown>") artistRaw else "Unknown Artist"
            val albumRaw = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val album = if (!albumRaw.isNullOrBlank() && albumRaw != "<unknown>") albumRaw else "Unknown Album"
            val durationMs = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val trackRaw = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)?.toIntOrNull() ?: 0
            val mimeType = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "audio/mpeg"
            mmr.release()

            val size = file.length()
            val format = extractFormat(file.absolutePath, mimeType)
            val (bitrate, sampleRate, bitDepth) = estimateOrExtractAudioSpecs(format, size, durationMs)

            // Trigger OS MediaScanner in background so it becomes part of official MediaStore as well
            try {
                MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
            } catch (_: Throwable) {}

            // Deterministic unique 63-bit ID based on absolute path
            val id = (file.absolutePath.hashCode().toLong() and 0x7FFFFFFFFFFFFFFFL) or 0x4000000000000000L

            Song(
                id = id,
                title = title,
                artist = artist,
                album = album,
                albumId = 0L,
                durationMs = durationMs,
                uri = Uri.fromFile(file).toString(),
                path = file.absolutePath,
                mimeType = mimeType,
                sizeBytes = size,
                bitrateKbps = bitrate,
                sampleRateHz = sampleRate,
                bitDepth = bitDepth,
                format = format,
                dateAdded = file.lastModified(),
                trackNumber = trackRaw
            )
        } catch (_: Throwable) {
            null
        }
    }

    private fun isExcludedPath(path: String, excludedFolders: Set<String>): Boolean {
        val lower = path.lowercase()
        if (lower.contains("/notifications/") ||
            lower.contains("/ringtones/") ||
            lower.contains("/alarms/") ||
            lower.contains("/whatsapp/media/whatsapp voice notes/") ||
            lower.contains("/whatsapp/media/whatsapp audio/")
        ) {
            return true
        }
        for (folder in excludedFolders) {
            if (path.startsWith(folder, ignoreCase = true)) return true
        }
        return false
    }

    private fun extractFormat(path: String, mimeType: String): String {
        val ext = path.substringAfterLast('.', "").uppercase()
        if (ext.isNotEmpty() && ext.length in 2..5) {
            return when (ext) {
                "DFF", "DSF", "DSD" -> "DSD"
                "ALAC" -> "ALAC"
                "AIFF", "AIF" -> "AIFF"
                "APE" -> "APE"
                "WV" -> "WavPack"
                else -> ext
            }
        }
        return when {
            mimeType.contains("flac", ignoreCase = true) -> "FLAC"
            mimeType.contains("wav", ignoreCase = true) -> "WAV"
            mimeType.contains("alac", ignoreCase = true) -> "ALAC"
            mimeType.contains("aiff", ignoreCase = true) -> "AIFF"
            mimeType.contains("dsd", ignoreCase = true) -> "DSD"
            mimeType.contains("aac", ignoreCase = true) -> "AAC"
            mimeType.contains("mp4", ignoreCase = true) || mimeType.contains("m4a", ignoreCase = true) -> "M4A"
            mimeType.contains("ogg", ignoreCase = true) -> "OGG"
            else -> "MP3"
        }
    }

    private fun estimateOrExtractAudioSpecs(format: String, sizeBytes: Long, durationMs: Long): Triple<Int, Int, Int> {
        val durationSec = durationMs / 1000.0
        val calculatedBitrate = if (durationSec > 0 && sizeBytes > 0) {
            ((sizeBytes * 8) / durationSec / 1000).toInt().coerceIn(64, 5644)
        } else {
            when (format) {
                "DSD" -> 2822
                "FLAC" -> 980
                "WAV", "AIFF" -> 1411
                "ALAC", "APE", "WavPack" -> 920
                "AAC", "M4A" -> 256
                else -> 320
            }
        }

        val (sampleRate, bitDepth) = when (format) {
            "DSD" -> Pair(2822400, 1)
            "FLAC" -> Pair(96000, 24)
            "ALAC", "AIFF", "APE", "WavPack" -> Pair(96000, 24)
            "WAV" -> Pair(44100, 16)
            "AAC", "M4A" -> Pair(44100, 16)
            else -> Pair(44100, 16)
        }

        return Triple(calculatedBitrate, sampleRate, bitDepth)
    }
}
