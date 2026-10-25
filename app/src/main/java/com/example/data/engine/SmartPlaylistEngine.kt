package com.example.data.engine

import com.example.data.model.Song
import org.json.JSONArray
import org.json.JSONObject

/**
 * Rule-based Dynamic Smart Playlist Engine.
 *
 * Supports multi-condition rules combined with AND (`matchAll = true`) or OR (`matchAll = false`) logic,
 * plus sorting and optional track count limits.
 *
 * Examples supported out of the box:
 * - Format = FLAC AND Sample Rate >= 96kHz
 * - Play Count > 10 AND Added in Last 30 Days
 * - Artist contains "21 Savage" AND Duration < 4:00
 */
enum class SmartRuleField(val label: String, val unitHint: String = "", val isNumeric: Boolean = false) {
    FORMAT("Format", "e.g. FLAC, WAV, MP3", isNumeric = false),
    SAMPLE_RATE("Sample Rate (Hz)", "e.g. 96000, 48000, 44100", isNumeric = true),
    BITRATE("Bitrate (kbps)", "e.g. 320, 900, 1411", isNumeric = true),
    BIT_DEPTH("Bit Depth (bit)", "e.g. 16, 24, 32", isNumeric = true),
    PLAY_COUNT("Play Count", "e.g. 10, 25, 50", isNumeric = true),
    ADDED_WITHIN_DAYS("Added in Last (Days)", "e.g. 7, 30, 90", isNumeric = true),
    ARTIST("Artist", "e.g. 21 Savage, Kendrick", isNumeric = false),
    TITLE("Title", "e.g. Remix, Intro, Live", isNumeric = false),
    ALBUM("Album", "e.g. Deluxe, Unplugged", isNumeric = false),
    DURATION_SECONDS("Duration (Seconds or M:SS)", "e.g. 240 or 4:00", isNumeric = true),
    IS_HI_RES("Hi-Res Lossless", "true or false", isNumeric = false),
    IS_FAVORITE("Favorite Track", "true or false", isNumeric = false)
}

enum class SmartRuleOperator(val symbol: String, val label: String) {
    EQUALS("=", "Equals"),
    NOT_EQUALS("≠", "Not Equals"),
    CONTAINS("∋", "Contains"),
    NOT_CONTAINS("∌", "Does Not Contain"),
    GREATER_THAN(">", "Greater Than (>)"),
    GREATER_OR_EQUAL(">=", "At Least (>=)"),
    LESS_THAN("<", "Less Than (<)"),
    LESS_OR_EQUAL("<=", "At Most (<=)")
}

data class SmartPlaylistRule(
    val field: SmartRuleField = SmartRuleField.FORMAT,
    val operator: SmartRuleOperator = SmartRuleOperator.EQUALS,
    val value: String = "FLAC"
) {
    fun toSummaryText(): String {
        return when (field) {
            SmartRuleField.ADDED_WITHIN_DAYS -> "Added in last $value days"
            SmartRuleField.SAMPLE_RATE -> {
                val hz = value.toIntOrNull()
                val formatted = if (hz != null && hz >= 1000) "${hz / 1000.0}kHz".replace(".0kHz", "kHz") else "${value}Hz"
                "Sample Rate ${operator.symbol} $formatted"
            }
            SmartRuleField.DURATION_SECONDS -> {
                val secs = parseDurationSeconds(value)
                val mins = secs / 60
                val rem = secs % 60
                "Duration ${operator.symbol} ${mins}:${rem.toString().padStart(2, '0')}"
            }
            SmartRuleField.IS_HI_RES -> "Hi-Res = $value"
            SmartRuleField.IS_FAVORITE -> "Favorite = $value"
            else -> "${field.label} ${operator.symbol} $value"
        }
    }
}

data class SmartPlaylistConfig(
    val matchAll: Boolean = true, // true = AND, false = OR
    val rules: List<SmartPlaylistRule> = emptyList(),
    val sortBy: String = "MOST_PLAYED", // MOST_PLAYED, RECENTLY_ADDED, TITLE, SAMPLE_RATE_DESC, DURATION_ASC
    val maxTracks: Int = 500
)

object SmartPlaylistEngine {

    fun availableOperatorsForField(field: SmartRuleField): List<SmartRuleOperator> {
        return when (field) {
            SmartRuleField.FORMAT -> listOf(
                SmartRuleOperator.EQUALS,
                SmartRuleOperator.NOT_EQUALS,
                SmartRuleOperator.CONTAINS
            )
            SmartRuleField.ARTIST, SmartRuleField.TITLE, SmartRuleField.ALBUM -> listOf(
                SmartRuleOperator.CONTAINS,
                SmartRuleOperator.EQUALS,
                SmartRuleOperator.NOT_CONTAINS
            )
            SmartRuleField.ADDED_WITHIN_DAYS -> listOf(
                SmartRuleOperator.LESS_OR_EQUAL,
                SmartRuleOperator.GREATER_THAN
            )
            SmartRuleField.IS_HI_RES, SmartRuleField.IS_FAVORITE -> listOf(
                SmartRuleOperator.EQUALS
            )
            else -> listOf(
                SmartRuleOperator.GREATER_OR_EQUAL,
                SmartRuleOperator.GREATER_THAN,
                SmartRuleOperator.LESS_THAN,
                SmartRuleOperator.LESS_OR_EQUAL,
                SmartRuleOperator.EQUALS
            )
        }
    }

    fun evaluate(songs: List<Song>, config: SmartPlaylistConfig): List<Song> {
        if (config.rules.isEmpty()) return emptyList()
        val nowSeconds = System.currentTimeMillis() / 1000L

        val filtered = songs.filter { song ->
            if (config.matchAll) {
                config.rules.all { rule -> matchesRule(song, rule, nowSeconds) }
            } else {
                config.rules.any { rule -> matchesRule(song, rule, nowSeconds) }
            }
        }

        val sorted = when (config.sortBy) {
            "RECENTLY_ADDED" -> filtered.sortedByDescending { it.dateAdded }
            "SAMPLE_RATE_DESC" -> filtered.sortedWith(compareByDescending<Song> { it.sampleRateHz }.thenByDescending { it.bitrateKbps })
            "DURATION_ASC" -> filtered.sortedBy { it.durationMs }
            "TITLE" -> filtered.sortedBy { it.title.lowercase() }
            else -> filtered.sortedWith(compareByDescending<Song> { it.playCount }.thenByDescending { it.dateAdded })
        }

        return if (config.maxTracks > 0) sorted.take(config.maxTracks) else sorted
    }

    private fun matchesRule(song: Song, rule: SmartPlaylistRule, nowSeconds: Long): Boolean {
        val rawTarget = rule.value.trim()
        if (rawTarget.isEmpty()) return true

        return when (rule.field) {
            SmartRuleField.FORMAT -> compareString(song.format, rawTarget, rule.operator)
            SmartRuleField.ARTIST -> compareString(song.artist, rawTarget, rule.operator)
            SmartRuleField.TITLE -> compareString(song.title, rawTarget, rule.operator)
            SmartRuleField.ALBUM -> compareString(song.album, rawTarget, rule.operator)
            SmartRuleField.SAMPLE_RATE -> {
                val targetHz = parseSampleRateHz(rawTarget)
                compareNumber(song.sampleRateHz.toLong(), targetHz.toLong(), rule.operator)
            }
            SmartRuleField.BITRATE -> {
                val targetKbps = rawTarget.filter { it.isDigit() }.toLongOrNull() ?: 0L
                compareNumber(song.bitrateKbps.toLong(), targetKbps, rule.operator)
            }
            SmartRuleField.BIT_DEPTH -> {
                val targetBits = rawTarget.filter { it.isDigit() }.toLongOrNull() ?: 16L
                compareNumber(song.bitDepth.toLong(), targetBits, rule.operator)
            }
            SmartRuleField.PLAY_COUNT -> {
                val targetCount = rawTarget.filter { it.isDigit() }.toLongOrNull() ?: 0L
                compareNumber(song.playCount.toLong(), targetCount, rule.operator)
            }
            SmartRuleField.ADDED_WITHIN_DAYS -> {
                val days = rawTarget.filter { it.isDigit() }.toLongOrNull() ?: 30L
                val songTimestampSec = if (song.dateAdded > 100_000_000_000L) song.dateAdded / 1000L else song.dateAdded
                val ageDays = ((nowSeconds - songTimestampSec).coerceAtLeast(0L)) / 86400L
                when (rule.operator) {
                    SmartRuleOperator.GREATER_THAN -> ageDays > days
                    SmartRuleOperator.GREATER_OR_EQUAL -> ageDays >= days
                    else -> ageDays <= days
                }
            }
            SmartRuleField.DURATION_SECONDS -> {
                val targetSec = parseDurationSeconds(rawTarget)
                val songSec = song.durationMs / 1000L
                compareNumber(songSec, targetSec, rule.operator)
            }
            SmartRuleField.IS_HI_RES -> {
                val targetBool = rawTarget.equals("true", ignoreCase = true) || rawTarget.equals("yes", ignoreCase = true)
                val isSongHiRes = song.sampleRateHz > 48000 || song.bitDepth > 16 || song.bitrateKbps >= 900
                isSongHiRes == targetBool
            }
            SmartRuleField.IS_FAVORITE -> {
                val targetBool = rawTarget.equals("true", ignoreCase = true) || rawTarget.equals("yes", ignoreCase = true)
                song.isFavorite == targetBool
            }
        }
    }

    private fun compareString(actual: String, target: String, op: SmartRuleOperator): Boolean {
        val a = actual.trim().lowercase()
        val t = target.trim().lowercase()
        return when (op) {
            SmartRuleOperator.EQUALS -> a == t
            SmartRuleOperator.NOT_EQUALS -> a != t
            SmartRuleOperator.CONTAINS -> a.contains(t)
            SmartRuleOperator.NOT_CONTAINS -> !a.contains(t)
            else -> a.contains(t)
        }
    }

    private fun compareNumber(actual: Long, target: Long, op: SmartRuleOperator): Boolean {
        return when (op) {
            SmartRuleOperator.EQUALS -> actual == target
            SmartRuleOperator.NOT_EQUALS -> actual != target
            SmartRuleOperator.GREATER_THAN -> actual > target
            SmartRuleOperator.GREATER_OR_EQUAL -> actual >= target
            SmartRuleOperator.LESS_THAN -> actual < target
            SmartRuleOperator.LESS_OR_EQUAL -> actual <= target
            else -> actual >= target
        }
    }

    private fun parseSampleRateHz(input: String): Int {
        val cleaned = input.trim().lowercase().replace("hz", "").trim()
        if (cleaned.endsWith("k")) {
            val kVal = cleaned.removeSuffix("k").toDoubleOrNull() ?: 48.0
            return (kVal * 1000).toInt()
        }
        val num = cleaned.toDoubleOrNull() ?: 48000.0
        return if (num < 1000.0) (num * 1000).toInt() else num.toInt()
    }

    fun serializeConfig(config: SmartPlaylistConfig): String {
        val root = JSONObject()
        root.put("matchAll", config.matchAll)
        root.put("sortBy", config.sortBy)
        root.put("maxTracks", config.maxTracks)
        val arr = JSONArray()
        for (rule in config.rules) {
            val obj = JSONObject()
            obj.put("field", rule.field.name)
            obj.put("operator", rule.operator.name)
            obj.put("value", rule.value)
            arr.put(obj)
        }
        root.put("rules", arr)
        return root.toString()
    }

    fun deserializeConfig(json: String): SmartPlaylistConfig? {
        if (json.isBlank()) return null
        return try {
            val root = JSONObject(json)
            val matchAll = root.optBoolean("matchAll", true)
            val sortBy = root.optString("sortBy", "MOST_PLAYED")
            val maxTracks = root.optInt("maxTracks", 500)
            val arr = root.optJSONArray("rules") ?: JSONArray()
            val rules = mutableListOf<SmartPlaylistRule>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val field = runCatching { SmartRuleField.valueOf(obj.getString("field")) }.getOrDefault(SmartRuleField.FORMAT)
                val op = runCatching { SmartRuleOperator.valueOf(obj.getString("operator")) }.getOrDefault(SmartRuleOperator.EQUALS)
                val value = obj.optString("value", "")
                rules.add(SmartPlaylistRule(field, op, value))
            }
            SmartPlaylistConfig(matchAll = matchAll, rules = rules, sortBy = sortBy, maxTracks = maxTracks)
        } catch (_: Exception) {
            null
        }
    }

    fun formatConfigSummary(json: String): String {
        val config = deserializeConfig(json) ?: return "Dynamic Smart Rules"
        if (config.rules.isEmpty()) return "Dynamic Smart Rules"
        val joiner = if (config.matchAll) " AND " else " OR "
        return config.rules.joinToString(joiner) { it.toSummaryText() }
    }
}

private fun parseDurationSeconds(raw: String): Long {
    val trimmed = raw.trim()
    if (trimmed.contains(":")) {
        val parts = trimmed.split(":")
        val mins = parts.getOrNull(0)?.trim()?.toLongOrNull() ?: 0L
        val secs = parts.getOrNull(1)?.trim()?.toLongOrNull() ?: 0L
        return mins * 60L + secs
    }
    return trimmed.filter { it.isDigit() }.toLongOrNull() ?: 240L
}
