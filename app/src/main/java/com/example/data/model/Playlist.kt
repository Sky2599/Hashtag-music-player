package com.example.data.model

enum class SmartPlaylistType {
    RECENTLY_ADDED,
    MOST_PLAYED,
    FAVORITES,
    HI_RES,
    ALL_SONGS
}

data class SmartPlaylist(
    val type: SmartPlaylistType,
    val title: String,
    val subtitle: String,
    val songCount: Int = 0
)
