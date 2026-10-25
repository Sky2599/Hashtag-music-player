package com.example.player

import android.media.AudioFormat
import android.media.audiofx.Virtualizer
import android.util.Log

/**
 * Operational Stereo Width Expansion & Soundstage Processing Engine.
 *
 * Provides real-time audio soundstage manipulation calibrated from:
 * - 0% (Mono): Collapses the stereo difference signal into mono center.
 * - 100% (Normal Stereo): Natural, uncompressed stereo field (unity width).
 * - 200% (Ultra-Wide Stereo): Spatial phase manipulation and binaural HRTF expansion,
 *   pushing the soundstage far outside the listener's headphones.
 *
 * Mid/Side (M/S) Matrix Formulation:
 *   Mid  = (Left + Right) * 0.5f  [Sum / Center Information]
 *   Side = (Left - Right) * 0.5f  [Difference / Spatial Width Information]
 *   Left_out  = Mid + (widthFactor * Side)
 *   Right_out = Mid - (widthFactor * Side)
 *
 * Real-time Hardware HAL Audio Session Binding:
 * - For widthFactor > 1.0 (101%..200%):
 *   Engages the hardware DSP spatial phase manipulator in forced BINAURAL mode,
 *   applying interaural time delays, pinna filters, and cross-channel phase manipulation.
 * - For widthFactor == 1.0 (100%):
 *   Bypasses spatial phase manipulation for bit-accurate stereo fidelity.
 * - For widthFactor < 1.0 (0%..99%):
 *   Progressively collapses stereo difference content down to pure mono (0%).
 */
class StereoWidthEngine(private val audioSessionId: Int) {
    private val TAG = "StereoWidthEngine"

    private var virtualizer: Virtualizer? = null
    var isEnabled: Boolean = true
        private set

    var widthPercent: Int = 100
        private set

    val widthFactor: Float
        get() = (widthPercent / 100f).coerceIn(0f, 2f)

    init {
        initAudioEffect()
    }

    private fun initAudioEffect() {
        if (audioSessionId == 0) return
        try {
            virtualizer?.release()
        } catch (_: Throwable) {}
        virtualizer = null

        try {
            // Use high priority (1000) to ensure control over audio session
            val virt = Virtualizer(1000, audioSessionId)
            try {
                virt.enabled = true
                try {
                    // Try binaural mode first for headphone spatialization
                    virt.forceVirtualizationMode(Virtualizer.VIRTUALIZATION_MODE_BINAURAL)
                } catch (_: Throwable) {
                    try {
                        virt.forceVirtualizationMode(Virtualizer.VIRTUALIZATION_MODE_TRANSAURAL)
                    } catch (_: Throwable) {
                        virt.forceVirtualizationMode(Virtualizer.VIRTUALIZATION_MODE_AUTO)
                    }
                }
            } catch (e: Throwable) {
                Log.d(TAG, "forceVirtualizationMode note: ${e.message}")
            }
            virtualizer = virt
            applyWidthToEffect()
            Log.d(TAG, "StereoWidthEngine initialized successfully on session $audioSessionId")
        } catch (e: Exception) {
            Log.w(TAG, "Hardware spatial effect initialization failed: ${e.message}")
        }
    }

    /**
     * Updates the operational stereo width dial setting.
     * @param enabled Whether stereo width processing is engaged.
     * @param percent Calibrated from 0% (Mono) to 200% (Ultra-Wide Stereo).
     */
    fun setStereoWidth(enabled: Boolean, percent: Int) {
        this.isEnabled = enabled
        this.widthPercent = percent.coerceIn(0, 200)
        applyWidthToEffect()
    }

    private fun applyWidthToEffect() {
        val effect = virtualizer ?: return
        try {
            if (!isEnabled || widthPercent <= 100) {
                // At 100% (normal stereo) or below (mono/narrow), bypass hardware virtualizer
                // to maintain bit-perfect uncolored stereo or allow mono matrixing
                try {
                    if (effect.strengthSupported) {
                        effect.setStrength(0)
                    }
                } catch (_: Throwable) {}
                effect.enabled = false
                return
            }

            // For 101%..200%, scale spatial expansion across full 0..1000 strength range
            // 101% -> 10 strength (subtle widening)
            // 150% -> 500 strength (noticeable wide soundstage)
            // 200% -> 1000 strength (maximum spatial DSP expansion)
            val dspStrength = (((widthPercent - 100) / 100f) * 1000f).toInt().coerceIn(10, 1000).toShort()
            
            effect.enabled = true
            if (effect.strengthSupported) {
                effect.setStrength(dspStrength)
            }
            Log.d(TAG, "Stereo Width engaged: $widthPercent% -> Hardware DSP Strength $dspStrength/1000")
        } catch (e: Exception) {
            Log.w(TAG, "Failed applying stereo width to audio effect: ${e.message}")
        }
    }

    /**
     * Mathematical Mid/Side (M/S) stereo matrix transformation on raw sample pairs.
     * Can be applied directly to stereo PCM float samples.
     */
    fun processMsPair(left: Float, right: Float, factor: Float = widthFactor): Pair<Float, Float> {
        val mid = (left + right) * 0.5f
        val side = (left - right) * 0.5f

        val outLeft = (mid + (factor * side)).coerceIn(-1.0f, 1.0f)
        val outRight = (mid - (factor * side)).coerceIn(-1.0f, 1.0f)
        return Pair(outLeft, outRight)
    }

    /**
     * Mid/Side stereo matrix processing on an interleaved float buffer: [L, R, L, R, ...]
     */
    fun processInterleavedPcm(buffer: FloatArray, offset: Int = 0, length: Int = buffer.size) {
        val factor = if (isEnabled) widthFactor else 1.0f
        var i = offset
        val end = offset + (length and 1.inv())
        while (i < end) {
            val left = buffer[i]
            val right = buffer[i + 1]
            val mid = (left + right) * 0.5f
            val side = (left - right) * 0.5f

            buffer[i] = (mid + (factor * side)).coerceIn(-1.0f, 1.0f)
            buffer[i + 1] = (mid - (factor * side)).coerceIn(-1.0f, 1.0f)
            i += 2
        }
    }

    /**
     * User-facing description of current soundstage state.
     */
    fun getSoundstageMode(): String {
        return when {
            !isEnabled -> "Bypassed"
            widthPercent == 0 -> "Mono"
            widthPercent < 100 -> "Narrow"
            widthPercent == 100 -> "Normal"
            widthPercent <= 150 -> "Wide"
            else -> "Ultra-Wide"
        }
    }

    fun release() {
        try {
            virtualizer?.enabled = false
            virtualizer?.release()
        } catch (_: Throwable) {}
        virtualizer = null
    }
}
