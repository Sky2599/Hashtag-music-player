package com.example.data.util

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AppSettings
import com.example.player.EqualizerState

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = run {
        val newPrefs = context.getSharedPreferences("hashtag_music_player_prefs", Context.MODE_PRIVATE)
        val oldPrefs = context.getSharedPreferences("musicolet_app_prefs", Context.MODE_PRIVATE)
        if (newPrefs.all.isEmpty() && oldPrefs.all.isNotEmpty()) {
            val editor = newPrefs.edit()
            oldPrefs.all.forEach { (key, value) ->
                when (value) {
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Float -> editor.putFloat(key, value)
                    is String -> editor.putString(key, value)
                    is Set<*> -> @Suppress("UNCHECKED_CAST") editor.putStringSet(key, value as Set<String>)
                }
            }
            editor.apply()
        }
        newPrefs
    }

    fun loadSettings(): AppSettings {
        val themeEngineSaved = prefs.getString("theme_engine", null)
        val dynamicAlbumArtSaved = prefs.getBoolean("dynamic_theme", true)
        val resolvedThemeEngine = themeEngineSaved ?: if (dynamicAlbumArtSaved) {
            AppSettings.THEME_ALBUM_ART
        } else {
            AppSettings.THEME_DARK_CONTRAST
        }

        return AppSettings(
            metadataReaderPriority = prefs.getString("meta_reader", "MediaStore API") ?: "MediaStore API",
            excludedFolders = prefs.getStringSet("excluded_folders", emptySet()) ?: emptySet(),
            autoUpdateLibrary = prefs.getBoolean("auto_update_lib", true),
            crossfadeDurationSec = prefs.getFloat("crossfade_sec", 3.0f),
            gaplessPlayback = prefs.getBoolean("gapless", true),
            replayGain = prefs.getBoolean("replay_gain", false),
            audioOutputDestination = prefs.getString("audio_out", "AAudio (Low Latency)") ?: "AAudio (Low Latency)",
            themeEngine = resolvedThemeEngine,
            darkThemeMode = prefs.getString("dark_theme", "Onyx Dark") ?: "Onyx Dark",
            albumGridColumns = prefs.getInt("grid_cols", 2),
            showTechnicalChips = prefs.getBoolean("show_chips", true),
            dynamicThemeFromAlbumArt = resolvedThemeEngine == AppSettings.THEME_ALBUM_ART,
            highResolutionVisualizer = prefs.getBoolean("hi_res_vis", true),
            showAudioPipelineInNowPlaying = prefs.getBoolean("show_pipeline_now_playing", true),
            bitPerfectOutput = prefs.getBoolean("bit_perfect", true),
            usbDacPassthrough = prefs.getBoolean("usb_dac", false),
            audioBufferSize = prefs.getString("audio_buf", "Standard (Optimal)") ?: "Standard (Optimal)",
            autoDeviceProfilesEnabled = prefs.getBoolean("auto_device_profiles", true),
            preferredOutputCodec = prefs.getString("preferred_codec", "LDAC / Bit-Perfect PCM Direct") ?: "LDAC / Bit-Perfect PCM Direct",
            pauseOnHeadphoneDisconnect = prefs.getBoolean("pause_on_detach", true),
            visualizerStyle = prefs.getString("vis_style", AppSettings.VIS_STYLE_FL_BARS) ?: AppSettings.VIS_STYLE_FL_BARS,
            visualizerColorScheme = prefs.getString("vis_color", AppSettings.VIS_COLOR_ALBUM_ADAPTIVE) ?: AppSettings.VIS_COLOR_ALBUM_ADAPTIVE,
            visualizerSensitivity = prefs.getFloat("vis_sens", 1.0f),
            visualizerBarCount = prefs.getInt("vis_bars", 21),
            visualizerPeakCaps = prefs.getBoolean("vis_peak_caps", true),
            visualizerShowLabels = prefs.getBoolean("vis_labels", true)
        )
    }

    fun saveSettings(settings: AppSettings) {
        prefs.edit()
            .putString("meta_reader", settings.metadataReaderPriority)
            .putStringSet("excluded_folders", settings.excludedFolders)
            .putBoolean("auto_update_lib", settings.autoUpdateLibrary)
            .putFloat("crossfade_sec", settings.crossfadeDurationSec)
            .putBoolean("gapless", settings.gaplessPlayback)
            .putBoolean("replay_gain", settings.replayGain)
            .putString("audio_out", settings.audioOutputDestination)
            .putString("theme_engine", settings.themeEngine)
            .putString("dark_theme", settings.darkThemeMode)
            .putInt("grid_cols", settings.albumGridColumns)
            .putBoolean("show_chips", settings.showTechnicalChips)
            .putBoolean("dynamic_theme", settings.themeEngine == AppSettings.THEME_ALBUM_ART)
            .putBoolean("hi_res_vis", settings.highResolutionVisualizer)
            .putBoolean("show_pipeline_now_playing", settings.showAudioPipelineInNowPlaying)
            .putBoolean("bit_perfect", settings.bitPerfectOutput)
            .putBoolean("usb_dac", settings.usbDacPassthrough)
            .putString("audio_buf", settings.audioBufferSize)
            .putBoolean("auto_device_profiles", settings.autoDeviceProfilesEnabled)
            .putString("preferred_codec", settings.preferredOutputCodec)
            .putBoolean("pause_on_detach", settings.pauseOnHeadphoneDisconnect)
            .putString("vis_style", settings.visualizerStyle)
            .putString("vis_color", settings.visualizerColorScheme)
            .putFloat("vis_sens", settings.visualizerSensitivity)
            .putInt("vis_bars", settings.visualizerBarCount)
            .putBoolean("vis_peak_caps", settings.visualizerPeakCaps)
            .putBoolean("vis_labels", settings.visualizerShowLabels)
            .apply()
    }

    fun saveLastPlaybackState(
        songId: Long,
        positionMs: Long,
        queueIds: List<Long>,
        queueIndex: Int = 0,
        isShuffle: Boolean = false,
        isRepeat: Boolean = false
    ) {
        prefs.edit()
            .putLong("last_song_id", songId)
            .putLong("last_position_ms", positionMs)
            .putString("last_queue_ids", queueIds.joinToString(","))
            .putInt("last_queue_index", queueIndex)
            .putBoolean("is_shuffle", isShuffle)
            .putBoolean("is_repeat", isRepeat)
            .apply()
    }

    fun getLastSongId(): Long = prefs.getLong("last_song_id", -1L)
    fun getLastPositionMs(): Long = prefs.getLong("last_position_ms", 0L)
    fun getLastQueueIds(): List<Long> {
        val raw = prefs.getString("last_queue_ids", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(",").mapNotNull { it.trim().toLongOrNull() }
    }
    fun getLastQueueIndex(): Int = prefs.getInt("last_queue_index", 0)
    fun getLastShuffle(): Boolean = prefs.getBoolean("is_shuffle", false)
    fun getLastRepeat(): Boolean = prefs.getBoolean("is_repeat", false)

    fun saveLastTab(tabIndex: Int) {
        prefs.edit().putInt("last_active_tab", tabIndex).apply()
    }

    fun getLastTab(): Int = prefs.getInt("last_active_tab", 0)

    fun saveMultiQueueState(
        activeSlotIndex: Int,
        slotNames: List<String>,
        slotSongIds: List<List<Long>>,
        slotActiveIndices: List<Int>
    ) {
        val editor = prefs.edit()
        editor.putInt("mq_active_slot", activeSlotIndex)
        for (i in 0 until 4) {
            editor.putString("mq_name_$i", slotNames.getOrElse(i) { "Queue ${i + 1}" })
            editor.putString("mq_ids_$i", slotSongIds.getOrNull(i)?.joinToString(",") ?: "")
            editor.putInt("mq_idx_$i", slotActiveIndices.getOrElse(i) { 0 })
        }
        editor.apply()
    }

    fun getSavedActiveQueueSlot(): Int = prefs.getInt("mq_active_slot", 0).coerceIn(0, 3)

    fun getSavedQueueSlotName(slotIndex: Int, defaultName: String): String {
        return prefs.getString("mq_name_$slotIndex", defaultName) ?: defaultName
    }

    fun getSavedQueueSlotIds(slotIndex: Int): List<Long> {
        val raw = prefs.getString("mq_ids_$slotIndex", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(",").mapNotNull { it.trim().toLongOrNull() }
    }

    fun getSavedQueueSlotActiveIndex(slotIndex: Int): Int {
        return prefs.getInt("mq_idx_$slotIndex", 0).coerceAtLeast(0)
    }

    fun saveEqualizerState(state: EqualizerState) {
        prefs.edit()
            .putBoolean("eq_enabled", state.isEnabled)
            .putInt("eq_band_mode", state.bandMode)
            .putString("eq_bands_5", state.bandLevels5.joinToString(","))
            .putString("eq_bands_10", state.bandLevels10.joinToString(","))
            .putString("eq_preset", state.selectedPreset)
            .putBoolean("eq_bass_enabled", state.bassBoostEnabled)
            .putInt("eq_bass_strength", state.bassBoostStrength)
            .putBoolean("eq_stereo_enabled", state.stereoWidthEnabled)
            .putInt("eq_stereo_percent", state.stereoWidthPercent)
            .putFloat("eq_crossfade", state.crossfadeSeconds)
            .putBoolean("eq_gapless", state.gaplessEnabled)
            .putBoolean("eq_replay_gain", state.replayGainEnabled)
            .putBoolean("eq_reverb_enabled", state.reverbEnabled)
            .putString("eq_reverb_preset", state.reverbPreset)
            .putFloat("eq_reverb_room_size", state.reverbRoomSize)
            .putFloat("eq_reverb_damping", state.reverbDamping)
            .putInt("eq_reverb_decay", state.reverbDecayMs)
            .putFloat("eq_reverb_wet", state.reverbWetLevel)
            .putBoolean("eq_reverb_jbl_bass", state.reverbLoudspeakerBass)
            .putInt("eq_reverb_bass_punch", state.reverbBassPunchStrength)
            .apply()
    }

    fun loadEqualizerState(): EqualizerState {
        val defaultState = EqualizerState()
        val isEnabled = prefs.getBoolean("eq_enabled", defaultState.isEnabled)
        val bandMode = prefs.getInt("eq_band_mode", defaultState.bandMode)

        val rawBands5 = prefs.getString("eq_bands_5", null)
        val bands5 = if (!rawBands5.isNullOrBlank()) {
            val parsed = rawBands5.split(",").mapNotNull { it.trim().toIntOrNull() }
            if (parsed.size == 5) parsed else defaultState.bandLevels5
        } else {
            defaultState.bandLevels5
        }

        val rawBands10 = prefs.getString("eq_bands_10", null)
        val bands10 = if (!rawBands10.isNullOrBlank()) {
            val parsed = rawBands10.split(",").mapNotNull { it.trim().toIntOrNull() }
            if (parsed.size == 10) parsed else defaultState.bandLevels10
        } else {
            defaultState.bandLevels10
        }

        val activeBands = if (bandMode == 10) bands10 else bands5
        val activeFreqs = if (bandMode == 10) defaultState.bandFrequencies10 else defaultState.bandFrequencies5

        return EqualizerState(
            isEnabled = isEnabled,
            bandMode = bandMode,
            bandLevels5 = bands5,
            bandLevels10 = bands10,
            bandFrequencies5 = defaultState.bandFrequencies5,
            bandFrequencies10 = defaultState.bandFrequencies10,
            bandLevels = activeBands,
            bandFrequencies = activeFreqs,
            minLevelMb = defaultState.minLevelMb,
            maxLevelMb = defaultState.maxLevelMb,
            bassBoostEnabled = prefs.getBoolean("eq_bass_enabled", defaultState.bassBoostEnabled),
            bassBoostStrength = prefs.getInt("eq_bass_strength", defaultState.bassBoostStrength),
            stereoWidthEnabled = prefs.getBoolean("eq_stereo_enabled", defaultState.stereoWidthEnabled),
            stereoWidthPercent = prefs.getInt("eq_stereo_percent", defaultState.stereoWidthPercent),
            crossfadeSeconds = prefs.getFloat("eq_crossfade", defaultState.crossfadeSeconds),
            gaplessEnabled = prefs.getBoolean("eq_gapless", defaultState.gaplessEnabled),
            replayGainEnabled = prefs.getBoolean("eq_replay_gain", defaultState.replayGainEnabled),
            selectedPreset = prefs.getString("eq_preset", defaultState.selectedPreset) ?: defaultState.selectedPreset,
            reverbEnabled = prefs.getBoolean("eq_reverb_enabled", defaultState.reverbEnabled),
            reverbPreset = prefs.getString("eq_reverb_preset", defaultState.reverbPreset) ?: defaultState.reverbPreset,
            reverbRoomSize = prefs.getFloat("eq_reverb_room_size", defaultState.reverbRoomSize),
            reverbDamping = prefs.getFloat("eq_reverb_damping", defaultState.reverbDamping),
            reverbDecayMs = prefs.getInt("eq_reverb_decay", defaultState.reverbDecayMs),
            reverbWetLevel = prefs.getFloat("eq_reverb_wet", defaultState.reverbWetLevel),
            reverbLoudspeakerBass = prefs.getBoolean("eq_reverb_jbl_bass", defaultState.reverbLoudspeakerBass),
            reverbBassPunchStrength = prefs.getInt("eq_reverb_bass_punch", defaultState.reverbBassPunchStrength)
        )
    }
}
