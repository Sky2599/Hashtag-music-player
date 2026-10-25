package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dynamic Theme delegated colors based on currently playing song (Symfonium style)
val DeepPurple: Color
    @Composable
    get() = AppTheme.colors.background

val DeepPurpleDark: Color
    @Composable
    get() = AppTheme.colors.backgroundDark

val DeepPurpleSurface: Color
    @Composable
    get() = AppTheme.colors.surface

val DeepPurpleSurfaceVariant: Color
    @Composable
    get() = AppTheme.colors.surfaceVariant

val DeepPurpleCard: Color
    @Composable
    get() = AppTheme.colors.card

// Vibrant Accent Colors
val ElectricViolet: Color
    @Composable
    get() = AppTheme.colors.primary

val ElectricVioletLight: Color
    @Composable
    get() = AppTheme.colors.primaryLight

val ElectricVioletDim: Color
    @Composable
    get() = AppTheme.colors.primaryDim

val OnPrimaryAccent: Color
    @Composable
    get() = AppTheme.colors.onPrimary

// Yellow for artwork placeholders
val VibrantYellow = Color(0xFFFFB700)
val VibrantYellowDark = Color(0xFFE5A400)

// Text and neutral accents
val TextPrimary: Color
    @Composable
    get() = AppTheme.colors.textPrimary

val TextSecondary: Color
    @Composable
    get() = AppTheme.colors.textSecondary

val TextTertiary: Color
    @Composable
    get() = AppTheme.colors.textTertiary

val DividerPurple: Color
    @Composable
    get() = AppTheme.colors.divider
