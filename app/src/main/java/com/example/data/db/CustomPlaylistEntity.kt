package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_playlists")
data class CustomPlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val songIdsCsv: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val isSmartPlaylist: Boolean
        get() = songIdsCsv.startsWith("SMART_RULES:")

    val smartRulesJson: String
        get() = if (isSmartPlaylist) songIdsCsv.removePrefix("SMART_RULES:") else ""

    fun getSongIds(): List<Long> {
        if (songIdsCsv.isBlank() || isSmartPlaylist) return emptyList()
        return songIdsCsv.split(",").mapNotNull { it.trim().toLongOrNull() }
    }

    fun resolveSongs(allSongs: List<com.example.data.model.Song>): List<com.example.data.model.Song> {
        return if (isSmartPlaylist) {
            val config = com.example.data.engine.SmartPlaylistEngine.deserializeConfig(smartRulesJson)
            if (config != null) {
                com.example.data.engine.SmartPlaylistEngine.evaluate(allSongs, config)
            } else {
                emptyList()
            }
        } else {
            val ids = getSongIds()
            if (ids.isEmpty()) return emptyList()
            val map = allSongs.associateBy { it.id }
            ids.mapNotNull { map[it] }
        }
    }
}
