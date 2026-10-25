package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistTransitionDao {
    @Query("SELECT * FROM playlist_transitions WHERE playlistId = :playlistId")
    fun getTransitionsForPlaylist(playlistId: String): Flow<List<PlaylistTransitionEntity>>

    @Query("SELECT * FROM playlist_transitions WHERE playlistId = :playlistId AND outgoingSongId = :outId AND incomingSongId = :inId LIMIT 1")
    suspend fun getTransition(playlistId: String, outId: Long, inId: Long): PlaylistTransitionEntity?

    @Query("SELECT * FROM playlist_transitions WHERE outgoingSongId = :outId AND incomingSongId = :inId LIMIT 1")
    suspend fun findTransitionForPair(outId: Long, inId: Long): PlaylistTransitionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTransition(transition: PlaylistTransitionEntity)

    @Query("DELETE FROM playlist_transitions WHERE playlistId = :playlistId")
    suspend fun clearTransitionsForPlaylist(playlistId: String)

    @Query("DELETE FROM playlist_transitions WHERE id = :id")
    suspend fun deleteTransition(id: String)
}
