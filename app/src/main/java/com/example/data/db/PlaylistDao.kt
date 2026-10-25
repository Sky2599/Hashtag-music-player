package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM custom_playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<CustomPlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(playlist: CustomPlaylistEntity)

    @Query("DELETE FROM custom_playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: String)
}
