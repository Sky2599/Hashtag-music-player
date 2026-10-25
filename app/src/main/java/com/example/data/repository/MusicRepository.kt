package com.example.data.repository

import android.content.Context
import com.example.data.db.SongDao
import com.example.data.db.SongEntity
import com.example.data.model.Album
import com.example.data.model.Song
import com.example.data.scanner.MediaScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MusicRepository(
    private val songDao: SongDao,
    private val playlistDao: com.example.data.db.PlaylistDao
) {

    val customPlaylists: Flow<List<com.example.data.db.CustomPlaylistEntity>> = playlistDao.getAllPlaylists()

    suspend fun savePlaylist(playlist: com.example.data.db.CustomPlaylistEntity) = withContext(Dispatchers.IO) {
        playlistDao.insertOrUpdate(playlist)
    }

    suspend fun deletePlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylist(playlistId)
    }

    val allSongs: Flow<List<Song>> = songDao.getAllSongs().map { list ->
        list.map { it.toSong() }
    }.flowOn(Dispatchers.Default)

    val recentlyAdded: Flow<List<Song>> = songDao.getRecentlyAdded().map { list ->
        list.map { it.toSong() }
    }.flowOn(Dispatchers.Default)

    val mostPlayed: Flow<List<Song>> = songDao.getMostPlayed().map { list ->
        list.map { it.toSong() }
    }.flowOn(Dispatchers.Default)

    val favorites: Flow<List<Song>> = songDao.getFavorites().map { list ->
        list.map { it.toSong() }
    }.flowOn(Dispatchers.Default)

    val hiResSongs: Flow<List<Song>> = songDao.getHiResSongs().map { list ->
        list.map { it.toSong() }
    }.flowOn(Dispatchers.Default)

    val albums: Flow<List<Album>> = allSongs.map { songs ->
        songs.groupBy { it.album }
            .map { (albumTitle, songList) ->
                val first = songList.first()
                Album(
                    id = first.albumId,
                    title = albumTitle,
                    artist = first.artist,
                    songCount = songList.size,
                    albumArtUri = first.albumArtUri?.toString()
                )
            }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title.trim() })
    }.flowOn(Dispatchers.Default)

    suspend fun refreshLibrary(
        context: Context,
        excludedFolders: Set<String> = emptySet(),
        forceDeepScan: Boolean = false
    ) = withContext(Dispatchers.IO) {
        try {
            val scanned = MediaScanner.scanDeviceAudio(context, excludedFolders, forceDeepScan)
            if (scanned.isNotEmpty()) {
                val existing = songDao.getAllSongsOnce().associateBy { it.id }
                val entitiesToInsert = mutableListOf<SongEntity>()
                val scannedIdSet = HashSet<Long>(scanned.size)

                for (song in scanned) {
                    scannedIdSet.add(song.id)
                    val prev = existing[song.id]
                    if (prev == null) {
                        // Brand new track discovered on device
                        entitiesToInsert.add(SongEntity.fromSong(song))
                    } else if (prev.dateAdded != song.dateAdded || prev.durationMs != song.durationMs || prev.title != song.title) {
                        // Modified file metadata
                        entitiesToInsert.add(
                            SongEntity.fromSong(
                                song.copy(
                                    playCount = prev.playCount,
                                    isFavorite = prev.isFavorite
                                )
                            )
                        )
                    }
                }

                // Batch insert only actual new or updated songs
                if (entitiesToInsert.isNotEmpty()) {
                    entitiesToInsert.chunked(250).forEach { chunk ->
                        songDao.insertAll(chunk)
                    }
                }

                // Remove deleted songs safely without exceeding SQLite 999 variable limit
                val idsToDelete = existing.keys.filter { it !in scannedIdSet }
                if (idsToDelete.isNotEmpty()) {
                    idsToDelete.chunked(250).forEach { chunk ->
                        songDao.deleteByIds(chunk)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MusicRepository", "refreshLibrary error: ${e.message}", e)
        }
    }

    suspend fun toggleFavorite(songId: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        songDao.setFavorite(songId, isFavorite)
    }

    suspend fun incrementPlayCount(songId: Long) = withContext(Dispatchers.IO) {
        songDao.incrementPlayCount(songId)
    }

    fun getSongsByAlbum(album: String): Flow<List<Song>> =
        songDao.getSongsByAlbum(album).map { list -> list.map { it.toSong() } }
}
