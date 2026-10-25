package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.theme.AppThemeColors
import com.example.theme.DynamicPaletteEngine

val LocalAppTheme = staticCompositionLocalOf { DynamicPaletteEngine.defaultTheme }

object AppTheme {
    val colors: AppThemeColors
        @Composable
        get() = LocalAppTheme.current
}

@Composable
fun MyApplicationTheme(
    themeColors: AppThemeColors = DynamicPaletteEngine.defaultTheme,
    content: @Composable () -> Unit
) {
    val animSpec = tween<androidx.compose.ui.graphics.Color>(durationMillis = 420)
    val animatedPrimary by animateColorAsState(themeColors.primary, animSpec, label = "primary")
    val animatedPrimaryLight by animateColorAsState(themeColors.primaryLight, animSpec, label = "primaryLight")
    val animatedPrimaryDim by animateColorAsState(themeColors.primaryDim, animSpec, label = "primaryDim")
    val animatedBg by animateColorAsState(themeColors.background, animSpec, label = "bg")
    val animatedBgDark by animateColorAsState(themeColors.backgroundDark, animSpec, label = "bgDark")
    val animatedSurface by animateColorAsState(themeColors.surface, animSpec, label = "surface")
    val animatedSurfaceVar by animateColorAsState(themeColors.surfaceVariant, animSpec, label = "surfaceVar")
    val animatedCard by animateColorAsState(themeColors.card, animSpec, label = "card")
    val animatedTextPrimary by animateColorAsState(themeColors.textPrimary, animSpec, label = "textPrimary")
    val animatedTextSec by animateColorAsState(themeColors.textSecondary, animSpec, label = "textSec")
    val animatedTextTert by animateColorAsState(themeColors.textTertiary, animSpec, label = "textTert")
    val animatedDivider by animateColorAsState(themeColors.divider, animSpec, label = "divider")
    val animatedOnPrimary by animateColorAsState(themeColors.onPrimary, animSpec, label = "onPrimary")
    val animatedOnPrimaryContainer by animateColorAsState(themeColors.onPrimaryContainer, animSpec, label = "onPrimaryContainer")

    val animatedAppTheme = themeColors.copy(
        primary = animatedPrimary,
        primaryLight = animatedPrimaryLight,
        primaryDim = animatedPrimaryDim,
        background = animatedBg,
        backgroundDark = animatedBgDark,
        surface = animatedSurface,
        surfaceVariant = animatedSurfaceVar,
        card = animatedCard,
        textPrimary = animatedTextPrimary,
        textSecondary = animatedTextSec,
        textTertiary = animatedTextTert,
        divider = animatedDivider,
        onPrimary = animatedOnPrimary,
        onPrimaryContainer = animatedOnPrimaryContainer
    )

    val colorScheme = darkColorScheme(
        primary = animatedAppTheme.primary,
        onPrimary = animatedAppTheme.onPrimary,
        primaryContainer = animatedAppTheme.primaryDim,
        onPrimaryContainer = animatedAppTheme.onPrimaryContainer,
        secondary = animatedAppTheme.primaryLight,
        onSecondary = animatedAppTheme.onPrimary,
        secondaryContainer = animatedAppTheme.surfaceVariant,
        onSecondaryContainer = animatedAppTheme.textPrimary,
        background = animatedAppTheme.background,
        onBackground = animatedAppTheme.textPrimary,
        surface = animatedAppTheme.surface,
        onSurface = animatedAppTheme.textPrimary,
        surfaceVariant = animatedAppTheme.surfaceVariant,
        onSurfaceVariant = animatedAppTheme.textSecondary,
        outline = animatedAppTheme.divider,
        outlineVariant = animatedAppTheme.divider
    )

    CompositionLocalProvider(LocalAppTheme provides animatedAppTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
