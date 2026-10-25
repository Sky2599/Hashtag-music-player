package com.example.player

import java.io.File

data class CueTrack(
    val trackNumber: Int,
    val title: String,
    val performer: String,
    val startOffsetMs: Long,
    val durationMs: Long = 0L
)

data class CueSheet(
    val title: String,
    val performer: String,
    val audioFileName: String,
    val tracks: List<CueTrack>
)

object CueSheetParser {
    /**
     * Parses an external .cue sheet file or string content into discrete tracks.
     */
    fun parseCueFile(cueFile: File): CueSheet? {
        if (!cueFile.exists()) return null
        return try {
            val lines = cueFile.readLines()
            parseLines(lines)
        } catch (e: Exception) {
            null
        }
    }

    fun parseLines(lines: List<String>): CueSheet {
        var albumTitle = "Unknown Album"
        var albumPerformer = "Unknown Artist"
        var audioFile = ""

        val tracks = mutableListOf<CueTrack>()
        var currentTrackNum = 0
        var currentTrackTitle = ""
        var currentTrackPerformer = ""
        var currentStartMs = 0L

        for (rawLine in lines) {
            val line = rawLine.trim()
            when {
                line.startsWith("PERFORMER", ignoreCase = true) && currentTrackNum == 0 -> {
                    albumPerformer = cleanString(line.substring(9).trim())
                }
                line.startsWith("TITLE", ignoreCase = true) && currentTrackNum == 0 -> {
                    albumTitle = cleanString(line.substring(5).trim())
                }
                line.startsWith("FILE", ignoreCase = true) -> {
                    val afterFile = line.substring(4).trim()
                    audioFile = cleanString(afterFile.substringBeforeLast("WAVE").substringBeforeLast("MP3").trim())
                }
                line.startsWith("TRACK", ignoreCase = true) -> {
                    // Commit previous track if exists
                    if (currentTrackNum > 0) {
                        tracks.add(
                            CueTrack(
                                trackNumber = currentTrackNum,
                                title = if (currentTrackTitle.isNotBlank()) currentTrackTitle else "Track $currentTrackNum",
                                performer = if (currentTrackPerformer.isNotBlank()) currentTrackPerformer else albumPerformer,
                                startOffsetMs = currentStartMs
                            )
                        )
                    }
                    val parts = line.split("\\s+".toRegex())
                    currentTrackNum = parts.getOrNull(1)?.toIntOrNull() ?: (tracks.size + 1)
                    currentTrackTitle = ""
                    currentTrackPerformer = albumPerformer
                }
                line.startsWith("TITLE", ignoreCase = true) && currentTrackNum > 0 -> {
                    currentTrackTitle = cleanString(line.substring(5).trim())
                }
                line.startsWith("PERFORMER", ignoreCase = true) && currentTrackNum > 0 -> {
                    currentTrackPerformer = cleanString(line.substring(9).trim())
                }
                line.startsWith("INDEX 01", ignoreCase = true) -> {
                    val timeStr = line.substring(8).trim()
                    currentStartMs = parseMmSsFf(timeStr)
                }
            }
        }

        if (currentTrackNum > 0) {
            tracks.add(
                CueTrack(
                    trackNumber = currentTrackNum,
                    title = if (currentTrackTitle.isNotBlank()) currentTrackTitle else "Track $currentTrackNum",
                    performer = if (currentTrackPerformer.isNotBlank()) currentTrackPerformer else albumPerformer,
                    startOffsetMs = currentStartMs
                )
            )
        }

        // Calculate track durations from next track start offsets
        val computedTracks = tracks.mapIndexed { idx, track ->
            val nextStart = tracks.getOrNull(idx + 1)?.startOffsetMs
            val duration = if (nextStart != null && nextStart > track.startOffsetMs) {
                nextStart - track.startOffsetMs
            } else 0L
            track.copy(durationMs = duration)
        }

        return CueSheet(
            title = albumTitle,
            performer = albumPerformer,
            audioFileName = audioFile,
            tracks = computedTracks
        )
    }

    private fun cleanString(str: String): String {
        return str.removeSurrounding("\"").trim()
    }

    private fun parseMmSsFf(time: String): Long {
        // MM:SS:FF (where FF is 75 frames per second)
        val parts = time.split(":")
        if (parts.size < 2) return 0L
        val mm = parts[0].toLongOrNull() ?: 0L
        val ss = parts[1].toLongOrNull() ?: 0L
        val ff = parts.getOrNull(2)?.toLongOrNull() ?: 0L

        val ms = (mm * 60 * 1000) + (ss * 1000) + (ff * 1000 / 75)
        return ms
    }
}
