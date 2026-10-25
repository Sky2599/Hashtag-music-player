package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Hashtag music player", appName)
  }

  @Test
  fun `equalizer state dB conversion works as expected`() {
    val state = com.example.player.EqualizerState(
      bandLevels = listOf(300, -200, 0, 450, -600)
    )
    assertEquals(3.0f, state.bandLevelDb(0), 0.01f)
    assertEquals(-2.0f, state.bandLevelDb(1), 0.01f)
    assertEquals(0.0f, state.bandLevelDb(2), 0.01f)
  }

  @Test
  fun `stereo width engine M-S matrix collapses to mono at 0 percent`() {
    val engine = com.example.player.StereoWidthEngine(0)
    engine.setStereoWidth(enabled = true, percent = 0)
    assertEquals(0.0f, engine.widthFactor, 0.001f)
    assertEquals("Mono", engine.getSoundstageMode())

    // Left = 0.8, Right = 0.2 -> Mid = 0.5, Side = 0.3
    // At 0% widthFactor, both Left and Right should collapse to pure Mid (0.5)
    val (outLeft, outRight) = engine.processMsPair(0.8f, 0.2f)
    assertEquals(0.5f, outLeft, 0.001f)
    assertEquals(0.5f, outRight, 0.001f)
  }

  @Test
  fun `stereo width engine M-S matrix preserves natural stereo at 100 percent`() {
    val engine = com.example.player.StereoWidthEngine(0)
    engine.setStereoWidth(enabled = true, percent = 100)
    assertEquals(1.0f, engine.widthFactor, 0.001f)
    assertEquals("Normal", engine.getSoundstageMode())

    val (outLeft, outRight) = engine.processMsPair(0.8f, 0.2f)
    assertEquals(0.8f, outLeft, 0.001f)
    assertEquals(0.2f, outRight, 0.001f)
  }

  @Test
  fun `stereo width engine M-S matrix expands soundstage at 200 percent`() {
    val engine = com.example.player.StereoWidthEngine(0)
    engine.setStereoWidth(enabled = true, percent = 200)
    assertEquals(2.0f, engine.widthFactor, 0.001f)
    assertEquals("Ultra-Wide", engine.getSoundstageMode())

    // Left = 0.6, Right = 0.4 -> Mid = 0.5, Side = 0.1
    // At 200% widthFactor (2.0), outLeft = 0.5 + 2*0.1 = 0.7, outRight = 0.5 - 2*0.1 = 0.3
    val (outLeft, outRight) = engine.processMsPair(0.6f, 0.4f)
    assertEquals(0.7f, outLeft, 0.001f)
    assertEquals(0.3f, outRight, 0.001f)
  }

  @Test
  fun `pure black album cover produces pure obsidian noir theme with high contrast diamond platinum primary`() {
    val bitmap = android.graphics.Bitmap.createBitmap(64, 64, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.BLACK)

    val theme = com.example.theme.DynamicPaletteEngine.extractHighContrastPaletteFromBitmap(bitmap)
    org.junit.Assert.assertTrue(
      "Theme label should indicate Obsidian/Noir: ${theme.toneProfileLabel}",
      theme.toneProfileLabel.contains("Obsidian", ignoreCase = true) || theme.toneProfileLabel.contains("Noir", ignoreCase = true)
    )
    // Background must be pure black
    assertEquals(androidx.compose.ui.graphics.Color(0xFF000000.toInt()), theme.background)
    // Primary must have high contrast (> 12:1) against surface and background
    org.junit.Assert.assertTrue(
      "Contrast ratio should be >= 12:1 but was ${theme.contrastRatio}",
      theme.contrastRatio >= 12.0f
    )

    // Test spectrum visualizer generation for obsidian theme
    val spectrum = com.example.theme.DynamicPaletteEngine.generateSpectrumColors(theme)
    assertEquals(7, spectrum.bandColors.size)
    assertEquals(7, spectrum.bandBottomColors.size)
  }

  @Test
  fun `black cover with small red emblem triggers cyber noir neon boost`() {
    val bitmap = android.graphics.Bitmap.createBitmap(64, 64, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.BLACK)

    // Draw small 8x8 bright red emblem in the center (representing ~1.5% focal art like Pink Floyd or DAMN)
    val paint = android.graphics.Paint().apply {
      color = android.graphics.Color.RED
    }
    canvas.drawRect(28f, 28f, 36f, 36f, paint)

    val theme = com.example.theme.DynamicPaletteEngine.extractHighContrastPaletteFromBitmap(bitmap)
    org.junit.Assert.assertTrue(
      "Theme label should indicate Cyber-Noir or Obsidian: ${theme.toneProfileLabel}",
      theme.toneProfileLabel.contains("Cyber-Noir", ignoreCase = true) || theme.toneProfileLabel.contains("Obsidian", ignoreCase = true)
    )
    org.junit.Assert.assertTrue(
      "Contrast ratio should be >= 7.0 but was ${theme.contrastRatio}",
      theme.contrastRatio >= 7.0f
    )

    val spectrum = com.example.theme.DynamicPaletteEngine.generateSpectrumColors(theme)
    assertEquals(7, spectrum.bandColors.size)
  }

  @Test
  fun `settings manager persists pauseOnHeadphoneDisconnect correctly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val settingsManager = com.example.data.util.SettingsManager(context)
    
    // Default should be true
    val defaultSettings = settingsManager.loadSettings()
    org.junit.Assert.assertTrue("Default pauseOnHeadphoneDisconnect should be true", defaultSettings.pauseOnHeadphoneDisconnect)

    // Save disabled
    settingsManager.saveSettings(defaultSettings.copy(pauseOnHeadphoneDisconnect = false))
    val disabledSettings = settingsManager.loadSettings()
    org.junit.Assert.assertFalse("pauseOnHeadphoneDisconnect should persist false", disabledSettings.pauseOnHeadphoneDisconnect)

    // Save re-enabled
    settingsManager.saveSettings(disabledSettings.copy(pauseOnHeadphoneDisconnect = true))
    val enabledSettings = settingsManager.loadSettings()
    org.junit.Assert.assertTrue("pauseOnHeadphoneDisconnect should persist true", enabledSettings.pauseOnHeadphoneDisconnect)
  }

  @Test
  fun `playback state and active tab persist across app restarts`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val settingsManager = com.example.data.util.SettingsManager(context)

    settingsManager.saveLastPlaybackState(
      songId = 12345L,
      positionMs = 45200L,
      queueIds = listOf(101L, 102L, 12345L, 104L),
      queueIndex = 2,
      isShuffle = true,
      isRepeat = true
    )
    settingsManager.saveLastTab(4) // Songs tab

    assertEquals(12345L, settingsManager.getLastSongId())
    assertEquals(45200L, settingsManager.getLastPositionMs())
    assertEquals(listOf(101L, 102L, 12345L, 104L), settingsManager.getLastQueueIds())
    assertEquals(2, settingsManager.getLastQueueIndex())
    org.junit.Assert.assertTrue(settingsManager.getLastShuffle())
    org.junit.Assert.assertTrue(settingsManager.getLastRepeat())
    assertEquals(4, settingsManager.getLastTab())
  }
}
