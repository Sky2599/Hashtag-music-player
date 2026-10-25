package com.example.data.model

data class AppSettings(
    // Library & Storage
    val metadataReaderPriority: String = "MediaStore API", // "MediaStore API", "TagLib High-Speed"
    val excludedFolders: Set<String> = emptySet(),
    val autoUpdateLibrary: Boolean = true,

    // Playback & Engine
    val crossfadeDurationSec: Float = 3.0f,
    val gaplessPlayback: Boolean = true,
    val replayGain: Boolean = false,
    val replayGainPreampDb: Float = 0.0f, // -6.0dB .. +6.0dB
    val audioOutputDestination: String = "AAudio (Low Latency)", // "AAudio (Low Latency)", "OpenSL ES", "Standard AudioTrack"
    val pauseOnHeadphoneDisconnect: Boolean = true,

    // UI & Theme Customization
    // Theme switching options:
    // 1. "material_you" -> Material You (Dynamic System Accent / Monet palette)
    // 2. "dark_contrast" -> Hashtag theme (Deep Purple #1A002C + Warm Orange #FFB700)
    // 3. "album_art" -> Dynamic Album Art (Palette API color matching)
    // 4. "citiview" -> Citiview theme
    val themeEngine: String = THEME_ALBUM_ART,
    val darkThemeMode: String = "Onyx Dark", // "Onyx Dark", "Pure AMOLED"
    val albumGridColumns: Int = 2, // 2 or 3 columns
    val showTechnicalChips: Boolean = true,
    val dynamicThemeFromAlbumArt: Boolean = true,
    val highResolutionVisualizer: Boolean = true,
    val showAudioPipelineInNowPlaying: Boolean = true, // Shows Live Visualizer / Pipeline HUD in Now Playing

    // Visualizer Customization Engine Settings
    val visualizerStyle: String = VIS_STYLE_FL_BARS, // "fl_bars", "neon_bars", "wave_line", "symmetric", "dot_matrix"
    val visualizerColorScheme: String = VIS_COLOR_ALBUM_ADAPTIVE, // "album_adaptive", "citiview_neon", "sunset_blaze", "emerald_glow", "ice_pearl"
    val visualizerSensitivity: Float = 1.0f, // 0.5f .. 2.0f
    val visualizerBarCount: Int = 21, // 7, 14, 21
    val visualizerPeakCaps: Boolean = true,
    val visualizerShowLabels: Boolean = true,

    // Audio Hardware & Output
    val bitPerfectOutput: Boolean = true,
    val usbDacPassthrough: Boolean = false,
    val audioBufferSize: String = "Standard (Optimal)", // "Small (Low Latency)", "Standard (Optimal)", "Large (High Stability)"

    // High-Resolution & Per-Device Output Profiles
    val autoDeviceProfilesEnabled: Boolean = true,
    val preferredOutputCodec: String = "LDAC / Bit-Perfect PCM Direct"
) {
    companion object {
        const val THEME_MATERIAL_YOU = "material_you"
        const val THEME_DARK_CONTRAST = "dark_contrast" // "Hashtag theme"
        const val THEME_ALBUM_ART = "album_art"
        const val THEME_CITIVIEW = "citiview"

        // Visualizer Styles
        const val VIS_STYLE_FL_BARS = "fl_bars"
        const val VIS_STYLE_NEON_BARS = "neon_bars"
        const val VIS_STYLE_WAVE_LINE = "wave_line"
        const val VIS_STYLE_SYMMETRIC = "symmetric"
        const val VIS_STYLE_DOT_MATRIX = "dot_matrix"

        // Visualizer Color Schemes
        const val VIS_COLOR_ALBUM_ADAPTIVE = "album_adaptive"
        const val VIS_COLOR_CITIVIEW = "citiview_neon"
        const val VIS_COLOR_SUNSET = "sunset_blaze"
        const val VIS_COLOR_EMERALD = "emerald_glow"
        const val VIS_COLOR_ICE = "ice_pearl"
    }
}
