package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AppSettings
import com.example.player.EqualizerState
import com.example.ui.screens.EqualizerScreen
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.LocalAppTheme
import com.example.ui.theme.DeepPurple
import com.example.ui.theme.DeepPurpleSurface
import com.example.ui.theme.ElectricVioletLight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun EqualizerDialog(
    equalizerState: EqualizerState,
    visualizerFft: FloatArray = FloatArray(21) { 0.05f },
    visualizerFftProvider: (() -> FloatArray)? = null,
    appSettings: AppSettings = AppSettings(),
    onSettingsChanged: (AppSettings) -> Unit = {},
    onToggleEqualizer: (Boolean) -> Unit,
    onBandLevelChange: (Int, Int) -> Unit,
    onBandModeChange: (Int) -> Unit = {},
    onPresetSelect: (String) -> Unit,
    onToggleBassBoost: (Boolean) -> Unit,
    onBassBoostStrengthChange: (Int) -> Unit,
    onToggleStereoWidth: (Boolean) -> Unit,
    onStereoWidthChange: (Int) -> Unit,
    onCrossfadeChange: (Float) -> Unit,
    onToggleGapless: (Boolean) -> Unit,
    onToggleReplayGain: (Boolean) -> Unit,
    onReplayGainPreampChange: (Float) -> Unit = {},
    onToggleParametricEq: (Boolean) -> Unit = {},
    onApplyAutoEqProfile: (com.example.player.AutoEqDatabase.AutoEqProfile) -> Unit = {},
    onToggleReverb: (Boolean) -> Unit = {},
    onReverbPresetSelect: (String) -> Unit = {},
    onReverbRoomSizeChange: (Float) -> Unit = {},
    onReverbDecayChange: (Int) -> Unit = {},
    onReverbDampingChange: (Float) -> Unit = {},
    onReverbWetLevelChange: (Float) -> Unit = {},
    onToggleReverbLoudspeakerBass: (Boolean) -> Unit = {},
    onReverbBassPunchChange: (Int) -> Unit = {},
    onDismiss: () -> Unit
) {
    val theme = LocalAppTheme.current
    val isLiquidGlass = theme.toneProfileLabel.contains("Liquid Glass", ignoreCase = true)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = if (isLiquidGlass) Color(0xDD081122) else DeepPurple
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Dialog Top Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isLiquidGlass) Color(0x280D1B2D) else DeepPurpleSurface)
                        .then(
                            if (isLiquidGlass) {
                                Modifier.border(
                                    1.dp,
                                    Brush.verticalGradient(listOf(Color(0x35FFFFFF), Color.Transparent)),
                                    androidx.compose.ui.graphics.RectangleShape
                                )
                            } else Modifier
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = if (isLiquidGlass) Color(0xFF00F0FF) else ElectricVioletLight,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Equaliser & DSP",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                EqualizerScreen(
                    equalizerState = equalizerState,
                    visualizerFft = visualizerFft,
                    visualizerFftProvider = visualizerFftProvider ?: { visualizerFft },
                    appSettings = appSettings,
                    onSettingsChanged = onSettingsChanged,
                    onToggleEqualizer = onToggleEqualizer,
                    onBandLevelChange = onBandLevelChange,
                    onBandModeChange = onBandModeChange,
                    onPresetSelect = onPresetSelect,
                    onToggleBassBoost = onToggleBassBoost,
                    onBassBoostStrengthChange = onBassBoostStrengthChange,
                    onToggleStereoWidth = onToggleStereoWidth,
                    onStereoWidthChange = onStereoWidthChange,
                    onCrossfadeChange = onCrossfadeChange,
                    onToggleGapless = onToggleGapless,
                    onToggleReplayGain = onToggleReplayGain,
                    onReplayGainPreampChange = onReplayGainPreampChange,
                    onToggleParametricEq = onToggleParametricEq,
                    onApplyAutoEqProfile = onApplyAutoEqProfile,
                    onToggleReverb = onToggleReverb,
                    onReverbPresetSelect = onReverbPresetSelect,
                    onReverbRoomSizeChange = onReverbRoomSizeChange,
                    onReverbDecayChange = onReverbDecayChange,
                    onReverbDampingChange = onReverbDampingChange,
                    onReverbWetLevelChange = onReverbWetLevelChange,
                    onToggleReverbLoudspeakerBass = onToggleReverbLoudspeakerBass,
                    onReverbBassPunchChange = onReverbBassPunchChange,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
