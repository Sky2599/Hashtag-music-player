package com.example.player

enum class FilterType(val displayName: String) {
    PEAK("Peaking EQ"),
    LOW_SHELF("Low Shelf"),
    HIGH_SHELF("High Shelf"),
    LOW_PASS("Low Pass"),
    HIGH_PASS("High Pass"),
    NOTCH("Notch")
}

data class ParametricBand(
    val id: Int,
    val frequencyHz: Float, // 20Hz .. 20000Hz
    val gainDb: Float,       // -15.0dB .. +15.0dB
    val qFactor: Float,      // 0.3 .. 10.0
    val filterType: FilterType = FilterType.PEAK,
    val isEnabled: Boolean = true
)

data class EqualizerState(
    val isEnabled: Boolean = true,
    val bandMode: Int = 10, // 5 or 10 bands. Default is 10 bands.
    val bandLevels5: List<Int> = listOf(0, 0, 0, 0, 0),
    val bandLevels10: List<Int> = listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
    val bandFrequencies5: List<Int> = listOf(60, 230, 910, 3600, 14000), // in Hz
    val bandFrequencies10: List<Int> = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000), // in Hz
    val bandLevels: List<Int> = listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0), // active list matching bandMode
    val bandFrequencies: List<Int> = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000), // in Hz
    val minLevelMb: Int = -1200,
    val maxLevelMb: Int = 1200,
    val bassBoostEnabled: Boolean = true,
    val bassBoostStrength: Int = 450, // 0 to 1000
    val stereoWidthEnabled: Boolean = true,
    val stereoWidthPercent: Int = 100, // 0% (Mono) to 200% (Ultra-Wide Stereo), default 100% (Normal)
    val virtualizerEnabled: Boolean = false,
    val virtualizerStrength: Int = 300, // 0 to 1000
    val crossfadeSeconds: Float = 3.0f, // 0 to 12s
    val gaplessEnabled: Boolean = true,
    val replayGainEnabled: Boolean = false,
    val replayGainPreampDb: Float = 0f, // -6.0dB to +6.0dB
    val selectedPreset: String = "Flat",

    // Advanced Parametric EQ Engine (PEQ)
    val isParametricEnabled: Boolean = false,
    val parametricBands: List<ParametricBand> = listOf(
        ParametricBand(0, 32f, 0f, 1.0f, FilterType.LOW_SHELF),
        ParametricBand(1, 64f, 0f, 1.41f, FilterType.PEAK),
        ParametricBand(2, 125f, 0f, 1.41f, FilterType.PEAK),
        ParametricBand(3, 250f, 0f, 1.41f, FilterType.PEAK),
        ParametricBand(4, 500f, 0f, 1.41f, FilterType.PEAK),
        ParametricBand(5, 1000f, 0f, 1.41f, FilterType.PEAK),
        ParametricBand(6, 2000f, 0f, 1.41f, FilterType.PEAK),
        ParametricBand(7, 4000f, 0f, 1.41f, FilterType.PEAK),
        ParametricBand(8, 8000f, 0f, 1.41f, FilterType.PEAK),
        ParametricBand(9, 16000f, 0f, 1.0f, FilterType.HIGH_SHELF)
    ),
    val activeAutoEqProfile: String = "Harman Target (In-Ear)",

    // Advanced Reverb & Loudspeaker Room Acoustic Simulation Engine
    val reverbEnabled: Boolean = false,
    val reverbPreset: String = ReverbEngine.PRESET_JBL_LARGE_ROOM,
    val reverbRoomSize: Float = 0.85f,       // 0.05f .. 1.0f (chamber to huge stadium)
    val reverbDamping: Float = 0.40f,        // 0.0f .. 1.0f (air absorption / high-frequency roll-off)
    val reverbDecayMs: Int = 3200,           // 100 .. 8000 ms decay time
    val reverbWetLevel: Float = 0.45f,       // 0.0f .. 1.0f (reverb wet mix / send level)
    val reverbLoudspeakerBass: Boolean = true, // JBL Boombox dual-radiator sub-bass emulation
    val reverbBassPunchStrength: Int = 720   // 0 .. 1000 sub-bass punch strength
) {
    fun bandLevelDb(bandIndex: Int): Float {
        val mb = bandLevels.getOrElse(bandIndex) { 0 }
        return mb / 100f
    }
}
