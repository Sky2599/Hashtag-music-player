package com.example.data.model

data class RadioMix(
    val id: String,
    val title: String,
    val subtitle: String,
    val seedSong: Song? = null,
    val tracks: List<Song>,
    val coverArtUri: String? = null,
    val moodCategory: String = "All",
    val badgeLabel: String? = null
)
