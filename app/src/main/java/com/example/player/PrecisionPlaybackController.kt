package com.example.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AbLoopState(
    val isEnabled: Boolean = false,
    val pointA: Long? = null,
    val pointB: Long? = null,
    val playbackSpeed: Float = 1.0f, // 0.5x to 2.0x
    val pitchSemiTones: Float = 0.0f // -6st to +6st
)

class PrecisionPlaybackController {
    private val _loopState = MutableStateFlow(AbLoopState())
    val loopState: StateFlow<AbLoopState> = _loopState.asStateFlow()

    fun setPointA(posMs: Long) {
        val cur = _loopState.value
        val validB = if (cur.pointB != null && cur.pointB > posMs) cur.pointB else null
        _loopState.value = cur.copy(pointA = posMs, pointB = validB, isEnabled = validB != null)
    }

    fun setPointB(posMs: Long) {
        val cur = _loopState.value
        val a = cur.pointA ?: 0L
        if (posMs > a) {
            _loopState.value = cur.copy(pointB = posMs, isEnabled = true)
        }
    }

    fun clearLoop() {
        _loopState.value = _loopState.value.copy(isEnabled = false, pointA = null, pointB = null)
    }

    fun toggleLoop(enabled: Boolean) {
        _loopState.value = _loopState.value.copy(isEnabled = enabled)
    }

    fun setPlaybackSpeed(speed: Float) {
        _loopState.value = _loopState.value.copy(playbackSpeed = speed.coerceIn(0.5f, 2.0f))
    }

    fun setPitch(semiTones: Float) {
        _loopState.value = _loopState.value.copy(pitchSemiTones = semiTones.coerceIn(-6f, 6f))
    }

    /**
     * Checks if position has surpassed Point B and returns target seek position (Point A) if loop triggers.
     */
    fun checkLoopTrigger(currentPosMs: Long): Long? {
        val state = _loopState.value
        if (!state.isEnabled) return null
        val b = state.pointB ?: return null
        val a = state.pointA ?: 0L
        if (currentPosMs >= b) {
            return a
        }
        return null
    }
}
