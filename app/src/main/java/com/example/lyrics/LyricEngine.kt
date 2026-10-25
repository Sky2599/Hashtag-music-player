package com.example.lyrics

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.example.data.model.Song
import java.io.File
import java.io.FileInputStream

data class LyricLine(
    val timeMs: Long,
    val text: String
)

object LyricEngine {
    private const val TAG = "LyricEngine"

    // Regex for LRC format: [01:23.45] or [01:23.456]
    private val LRC_REGEX = Regex("""^\[(\d{1,2}):(\d{2})(?:\.(\d{2,3}))?\](.*)$""")

    /**
     * Attempts to find and parse actual lyrics for a given song:
     * 1. Checks for local companion .lrc or .txt file (song.lrc, song.mp3.lrc, song.txt)
     * 2. Checks embedded ID3 USLT / SYLT frames directly from audio file
     * 3. Checks Vorbis / FLAC comment tags (LYRICS or UNSYNCEDLYRICS)
     * 4. If unavailable, returns emptyList()
     */
    fun getLyricsForSong(context: Context, song: Song): List<LyricLine> {
        // 1. Try local companion .lrc or .txt file
        val companionLyrics = tryLoadCompanionLrc(song.path)
        if (companionLyrics.isNotEmpty()) {
            return companionLyrics
        }

        // 2. Try embedded ID3 USLT / SYLT lyrics directly from the file bytes
        val id3Lyrics = tryExtractId3LyricsFromFile(song.path)
        if (id3Lyrics.isNotEmpty()) {
            return id3Lyrics
        }

        // 3. Try Vorbis / FLAC tag lyrics if FLAC / OGG
        val vorbisLyrics = tryExtractVorbisLyricsFromFile(song.path)
        if (vorbisLyrics.isNotEmpty()) {
            return vorbisLyrics
        }

        // If no actual lyrics exist for this song, return emptyList so "(no lyrics found)" is shown
        return emptyList()
    }

    private fun tryLoadCompanionLrc(filePath: String): List<LyricLine> {
        if (filePath.isBlank()) return emptyList()
        try {
            val f = File(filePath)
            val parent = f.parentFile ?: return emptyList()
            val nameWithoutExt = f.nameWithoutExtension

            val candidateLrc1 = File(parent, "$nameWithoutExt.lrc")
            val candidateLrc2 = File(parent, "${f.name}.lrc")
            val candidateTxt = File(parent, "$nameWithoutExt.txt")

            val target = when {
                candidateLrc1.exists() -> candidateLrc1
                candidateLrc2.exists() -> candidateLrc2
                candidateTxt.exists() -> candidateTxt
                else -> null
            }

            if (target != null && target.canRead()) {
                val lines = target.readLines()
                return parseTextOrLrcContent(lines)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed reading companion lyric file: ${e.message}")
        }
        return emptyList()
    }

    private fun tryExtractId3LyricsFromFile(filePath: String): List<LyricLine> {
        if (filePath.isBlank()) return emptyList()
        try {
            val file = File(filePath)
            if (!file.exists() || !file.canRead()) return emptyList()
            val maxBytesToRead = minOf(file.length(), 256 * 1024L).toInt()
            if (maxBytesToRead < 20) return emptyList()

            val bytes = ByteArray(maxBytesToRead)
            FileInputStream(file).use { fis ->
                fis.read(bytes)
            }

            val usltBytes = "USLT".toByteArray(Charsets.ISO_8859_1)
            for (i in 0 until bytes.size - 14) {
                if (bytes[i] == usltBytes[0] &&
                    bytes[i + 1] == usltBytes[1] &&
                    bytes[i + 2] == usltBytes[2] &&
                    bytes[i + 3] == usltBytes[3]
                ) {
                    val size = ((bytes[i + 4].toInt() and 0xFF) shl 24) or
                        ((bytes[i + 5].toInt() and 0xFF) shl 16) or
                        ((bytes[i + 6].toInt() and 0xFF) shl 8) or
                        (bytes[i + 7].toInt() and 0xFF)
                    val frameLength = size.coerceIn(1, bytes.size - i - 10)
                    val contentOffset = i + 10 + 4
                    if (contentOffset < bytes.size) {
                        val textBytes = bytes.copyOfRange(
                            contentOffset,
                            (contentOffset + frameLength).coerceAtMost(bytes.size)
                        )
                        val text = String(textBytes, Charsets.UTF_8).replace("\u0000", "").trim()
                        if (text.isNotBlank()) {
                            return parseTextOrLrcContent(text.lines())
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return emptyList()
    }

    private fun tryExtractVorbisLyricsFromFile(filePath: String): List<LyricLine> {
        if (filePath.isBlank()) return emptyList()
        try {
            val file = File(filePath)
            if (!file.exists() || !file.canRead()) return emptyList()
            val maxBytesToRead = minOf(file.length(), 256 * 1024L).toInt()
            if (maxBytesToRead < 20) return emptyList()

            val bytes = ByteArray(maxBytesToRead)
            FileInputStream(file).use { fis -> fis.read(bytes) }
            val textContent = String(bytes, Charsets.ISO_8859_1)

            val tagMarkers = listOf("LYRICS=", "UNSYNCEDLYRICS=")
            for (marker in tagMarkers) {
                val idx = textContent.indexOf(marker, ignoreCase = true)
                if (idx != -1) {
                    val start = idx + marker.length
                    val end = textContent.indexOf('\u0000', start).let { if (it == -1) (start + 2000).coerceAtMost(textContent.length) else it }
                    val raw = textContent.substring(start, end).trim()
                    if (raw.isNotBlank()) {
                        return parseTextOrLrcContent(raw.lines())
                    }
                }
            }
        } catch (_: Exception) {}
        return emptyList()
    }

    fun parseTextOrLrcContent(lines: List<String>): List<LyricLine> {
        val lrc = parseLrcContent(lines)
        if (lrc.isNotEmpty()) return lrc
        val nonBlank = lines.map { it.trim() }.filter { it.isNotEmpty() }
        return nonBlank.mapIndexed { idx, text ->
            LyricLine(timeMs = idx * 3000L, text = text)
        }
    }

    fun parseLrcContent(lines: List<String>): List<LyricLine> {
        val result = mutableListOf<LyricLine>()
        for (rawLine in lines) {
            val line = rawLine.trim()
            val match = LRC_REGEX.find(line)
            if (match != null) {
                val min = match.groupValues[1].toLongOrNull() ?: 0L
                val sec = match.groupValues[2].toLongOrNull() ?: 0L
                val msStr = match.groupValues[3]
                val ms = when {
                    msStr.length == 2 -> (msStr.toLongOrNull() ?: 0L) * 10
                    msStr.length == 3 -> msStr.toLongOrNull() ?: 0L
                    else -> 0L
                }
                val timeMs = min * 60000L + sec * 1000L + ms
                val text = match.groupValues[4].trim()
                if (text.isNotEmpty()) {
                    result.add(LyricLine(timeMs, text))
                }
            }
        }
        return result.sortedBy { it.timeMs }
    }
}
