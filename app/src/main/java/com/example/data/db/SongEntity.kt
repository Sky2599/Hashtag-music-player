package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Song

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val uri: String,
    val path: String,
    val mimeType: String,
    val sizeBytes: Long,
    val bitrateKbps: Int,
    val sampleRateHz: Int,
    val bitDepth: Int,
    val format: String,
    val playCount: Int = 0,
    val isFavorite: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis(),
    val customArtworkUri: String? = null,
    val trackNumber: Int = 0
) {
    fun toSong(): Song = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        albumId = albumId,
        durationMs = durationMs,
        uri = uri,
        path = path,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        bitrateKbps = bitrateKbps,
        sampleRateHz = sampleRateHz,
        bitDepth = bitDepth,
        format = format,
        playCount = playCount,
        isFavorite = isFavorite,
        dateAdded = dateAdded,
        customArtworkUri = customArtworkUri,
        trackNumber = trackNumber
    )

    companion object {
        fun fromSong(song: Song): SongEntity = SongEntity(
            id = song.id,
            title = song.title,
            artist = song.artist,
            album = song.album,
            albumId = song.albumId,
            durationMs = song.durationMs,
            uri = song.uri,
            path = song.path,
            mimeType = song.mimeType,
            sizeBytes = song.sizeBytes,
            bitrateKbps = song.bitrateKbps,
            sampleRateHz = song.sampleRateHz,
            bitDepth = song.bitDepth,
            format = song.format,
            playCount = song.playCount,
            isFavorite = song.isFavorite,
            dateAdded = song.dateAdded,
            customArtworkUri = song.customArtworkUri,
            trackNumber = song.trackNumber
        )
    }
}
