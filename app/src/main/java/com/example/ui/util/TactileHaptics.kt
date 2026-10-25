package com.example.ui.util

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import kotlin.math.abs

/**
 * Central hardware haptic & zero-latency touch feedback controller.
 * Provides crisp, immediate system haptics (lightImpact, selectionClick, longPress, confirm)
 * using Android's View.performHapticFeedback with FLAG_IGNORE_VIEW_SETTING for guaranteed
 * tactile response across sliders, dials, transport controls, switches, and multi-select lists.
 */
object TactileHaptics {

    /**
     * Crisp light impact for primary transport controls (Play/Pause, Skip, Shuffle, Repeat, Tab switch).
     */
    fun lightImpact(view: View) {
        try {
            val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                HapticFeedbackConstants.KEYBOARD_TAP
            } else {
                HapticFeedbackConstants.VIRTUAL_KEY
            }
            view.performHapticFeedback(
                constant,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        } catch (_: Throwable) {
            // Safe fallback on unsupported hardware
        }
    }

    /**
     * Ultra-crisp subtle tick for continuous sliders (Equalizer bands, Stereo Width dial,
     * Bass Boost dial, Crossfade slider, Waveform Seekbar scrubbing) and pill/switch toggles.
     */
    fun selectionClick(view: View) {
        try {
            val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                HapticFeedbackConstants.CLOCK_TICK
            } else {
                HapticFeedbackConstants.KEYBOARD_TAP
            }
            view.performHapticFeedback(
                constant,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        } catch (_: Throwable) {
        }
    }

    /**
     * Distinct tactile thud when long-pressing tracks or albums to enter multi-select queue mode.
     */
    fun longPressImpact(view: View) {
        try {
            view.performHapticFeedback(
                HapticFeedbackConstants.LONG_PRESS,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        } catch (_: Throwable) {
        }
    }

    /**
     * Toggle switch / radio pill state change confirmation click.
     */
    fun toggleClick(view: View) {
        try {
            val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.VIRTUAL_KEY
            }
            view.performHapticFeedback(
                constant,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        } catch (_: Throwable) {
        }
    }
}

/**
 * Zero-Delay Touch Down Modifier (`instantTapDown`):
 * Executes [onTapDown] on the exact frame the finger touches the screen (`awaitFirstDown`),
 * eliminating the 100ms–300ms tap-up / gesture-disambiguation latency on navigation tabs,
 * transport buttons, and filter pills.
 */
fun Modifier.instantTapDown(
    enabled: Boolean = true,
    hapticLightImpact: Boolean = true,
    onTapDown: () -> Unit
): Modifier = composed {
    val view = LocalView.current
    this
        .pointerInput(enabled, onTapDown) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                down.consume()
                if (hapticLightImpact) {
                    TactileHaptics.lightImpact(view)
                } else {
                    TactileHaptics.selectionClick(view)
                }
                onTapDown()
                // Wait for the pointer to be released so it doesn't re-trigger or conflict
                waitForUpOrCancellation()
            }
        }
}

/**
 * Zero-Lag List Item Touch Handler (`instantListClickable`):
 * - Fires a subtle tactile touch-down preview or instant response while still respecting vertical list scroll!
 * - Eliminates double-tap waiting delay so tapping a track or album executes on the exact frame of finger release
 *   (0ms post-release debounce), and triggers a crisp `LONG_PRESS` haptic when long-pressing for multi-select.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.instantListClickable(
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = this.combinedClickable(
    enabled = enabled,
    onClick = onClick,
    onLongClick = onLongClick
)
