package com.example.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.pow

data class SleepTimerState(
    val isActive: Boolean = false,
    val remainingSeconds: Int = 0,
    val totalSeconds: Int = 0,
    val fadeOutDurationSeconds: Int = 30, // Smooth analog fade out
    val stopAtEndOfTrack: Boolean = false,
    val currentVolumeMultiplier: Float = 1.0f
)

/**
 * High-precision, silky-smooth Sleep Timer with true acoustic (decibel) fade out.
 * Updates volume every 100ms during fade out to eliminate abrupt stops.
 */
class ProSleepTimer(
    private val onVolumeFadeUpdate: (Float) -> Unit,
    private val onTimerFinished: () -> Unit
) {
    private val _timerState = MutableStateFlow(SleepTimerState())
    val timerState: StateFlow<SleepTimerState> = _timerState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null

    fun startTimer(minutes: Int, stopAtTrackEnd: Boolean = false, fadeSecs: Int = 30) {
        cancelTimer()
        val totalSec = (minutes * 60).coerceAtLeast(1)
        val fadeDuration = fadeSecs.coerceIn(5, totalSec)

        _timerState.value = SleepTimerState(
            isActive = true,
            remainingSeconds = totalSec,
            totalSeconds = totalSec,
            fadeOutDurationSeconds = fadeDuration,
            stopAtEndOfTrack = stopAtTrackEnd,
            currentVolumeMultiplier = 1.0f
        )

        val totalMs = totalSec * 1000L
        val fadeMs = fadeDuration * 1000L
        val startTime = System.currentTimeMillis()
        val targetEndTime = startTime + totalMs

        timerJob = scope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val remainingMs = (targetEndTime - now).coerceAtLeast(0L)
                val remainingSec = (remainingMs / 1000L).toInt()

                // Calculate perceptual volume multiplier with acoustic curve
                val volumeMultiplier = if (remainingMs <= fadeMs && fadeMs > 0L) {
                    val progress = (remainingMs.toFloat() / fadeMs.toFloat()).coerceIn(0f, 1f)
                    // Logarithmic perception curve (Weber-Fechner acoustic response)
                    // Ensures volume gently and naturally fades into whisper and silence
                    progress.pow(2.2f).coerceIn(0f, 1f)
                } else {
                    1.0f
                }

                _timerState.value = _timerState.value.copy(
                    remainingSeconds = remainingSec,
                    currentVolumeMultiplier = volumeMultiplier
                )
                onVolumeFadeUpdate(volumeMultiplier)

                if (remainingMs <= 0L) {
                    // Final gentle micro-ramp to complete silence before pausing
                    for (step in 5 downTo 0) {
                        val microVol = (step / 5f) * volumeMultiplier
                        onVolumeFadeUpdate(microVol)
                        delay(40)
                    }
                    _timerState.value = _timerState.value.copy(
                        isActive = false,
                        remainingSeconds = 0,
                        currentVolumeMultiplier = 0f
                    )
                    onVolumeFadeUpdate(0f)
                    onTimerFinished()
                    break
                }

                // If in the fade-out window, update at 100ms for buttery-smooth analog attenuation;
                // otherwise update every 500ms to save CPU
                val delayTime = if (remainingMs <= fadeMs) 100L else 500L
                delay(delayTime)
            }
        }
    }

    fun handleTrackCompleted(): Boolean {
        if (_timerState.value.isActive && _timerState.value.stopAtEndOfTrack) {
            cancelTimer()
            onTimerFinished()
            return true
        }
        return false
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        _timerState.value = SleepTimerState()
        onVolumeFadeUpdate(1.0f)
    }
}
