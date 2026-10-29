package com.example.data.model

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val songCount: Int,
    val albumArtUri: String? = null,
    val firstSongPath: String = "",
    val firstSongUri: String = ""
) {
    val parsedArtUri: android.net.Uri? by lazy(LazyThreadSafetyMode.NONE) {
        if (!albumArtUri.isNullOrEmpty()) android.net.Uri.parse(albumArtUri) else null
    }
}
