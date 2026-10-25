package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlist_transitions")
data class PlaylistTransitionEntity(
    @PrimaryKey val id: String, // "${playlistId}_${outgoingSongId}_${incomingSongId}"
    val playlistId: String,
    val outgoingSongId: Long,
    val incomingSongId: Long,
    val barCount: Int = 16,
    val durationSeconds: Float = 8f,
    val presetName: String = "Dynamic Automix",
    val topFilterDipX: Float = 0.20f,
    val topFilterDipY: Float = 0.56f,
    val bottomFilterEndX: Float = 0.54f,
    val volumeSlope: Float = 0.16f,
    val targetBpm: Int = 120,
    val bpmLockEnabled: Boolean = true,
    val hpfCutoffHz: Float = 250f,
    val lpfCutoffHz: Float = 5000f,
    val updatedAt: Long = System.currentTimeMillis()
)
