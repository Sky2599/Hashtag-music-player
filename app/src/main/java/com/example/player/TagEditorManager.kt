package com.example.player

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.example.data.model.Song
import java.io.File

data class EditableAudioTags(
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String,
    val genre: String,
    val year: String,
    val trackNumber: String,
    val discNumber: String,
    val composer: String,
    val lyrics: String
)

object TagEditorManager {
    fun readTags(context: Context, song: Song): EditableAudioTags {
        val retriever = MediaMetadataRetriever()
        return try {
            if (song.path.isNotBlank() && File(song.path).exists()) {
                retriever.setDataSource(song.path)
            } else {
                retriever.setDataSource(context, Uri.parse(song.uri))
            }
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: song.title
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: song.artist
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: song.album
            val albumArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST) ?: song.artist
            val genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE) ?: "Studio / Lossless"
            val year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR) ?: "2024"
            val trackNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER) ?: if (song.trackNumber > 0) "${song.trackNumber}" else "1"
            val discNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER) ?: "1"
            val composer = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER) ?: song.artist

            EditableAudioTags(
                title = title,
                artist = artist,
                album = album,
                albumArtist = albumArtist,
                genre = genre,
                year = year,
                trackNumber = trackNumber,
                discNumber = discNumber,
                composer = composer,
                lyrics = ""
            )
        } catch (e: Exception) {
            EditableAudioTags(
                title = song.title,
                artist = song.artist,
                album = song.album,
                albumArtist = song.artist,
                genre = "Hi-Res",
                year = "2024",
                trackNumber = if (song.trackNumber > 0) "${song.trackNumber}" else "1",
                discNumber = "1",
                composer = song.artist,
                lyrics = ""
            )
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    suspend fun updateSongTags(
        context: Context,
        songDao: com.example.data.db.SongDao,
        songId: Long,
        newTitle: String,
        newArtist: String,
        newAlbum: String,
        newTrackNumber: Int?
    ) {
        try {
            songDao.updateSongMetadata(
                songId = songId,
                title = newTitle.trim(),
                artist = newArtist.trim(),
                album = newAlbum.trim(),
                trackNumber = newTrackNumber ?: 1
            )
        } catch (e: Exception) {
            android.util.Log.e("TagEditorManager", "Error updating song tags: ${e.message}")
        }
    }
}
