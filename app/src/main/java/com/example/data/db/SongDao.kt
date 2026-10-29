package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY dateAdded DESC")
    fun getRecentlyAdded(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE playCount > 0 ORDER BY playCount DESC")
    fun getMostPlayed(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavorites(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE bitrateKbps >= 320 OR format = 'FLAC' OR format = 'WAV' ORDER BY bitrateKbps DESC")
    fun getHiResSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE album = :albumName ORDER BY CASE WHEN trackNumber > 0 THEN trackNumber ELSE 999999 END ASC, title ASC")
    fun getSongsByAlbum(albumName: String): Flow<List<SongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<SongEntity>)

    @Query("UPDATE songs SET playCount = playCount + 1 WHERE id = :songId")
    suspend fun incrementPlayCount(songId: Long)

    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE id = :songId")
    suspend fun setFavorite(songId: Long, isFavorite: Boolean)

    @Query("UPDATE songs SET title = :title, artist = :artist, album = :album, trackNumber = :trackNumber WHERE id = :songId")
    suspend fun updateSongMetadata(songId: Long, title: String, artist: String, album: String, trackNumber: Int)

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getSongCount(): Int

    @Query("DELETE FROM songs WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("SELECT * FROM songs")
    suspend fun getAllSongsOnce(): List<SongEntity>

    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY title ASC")
    suspend fun getFavoritesOnce(): List<SongEntity>

    @Query("SELECT * FROM songs ORDER BY dateAdded DESC LIMIT 50")
    suspend fun getRecentlyAddedOnce(): List<SongEntity>

    @Query("SELECT * FROM songs WHERE id = :songId LIMIT 1")
    suspend fun getSongById(songId: Long): SongEntity?

    @Query("DELETE FROM songs WHERE id < 0")
    suspend fun deleteDemoSongs()

    @Query("DELETE FROM songs")
    suspend fun deleteAll()
}
