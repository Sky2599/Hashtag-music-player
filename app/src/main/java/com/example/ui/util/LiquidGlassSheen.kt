package com.example.ui.util

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * High-performance Liquid Glass Specular Reflection & Press-Reactive Sheen.
 * Replaces distracting continuous loop animations with realistic physical glass:
 * 1. Resting: Static specular top reflection & subtle refraction border.
 * 2. Pressed: Luminous specular flash & chromatic edge gleam when touched.
 */
object LiquidGlassSheen {
    val GlassBorderGradient = Brush.linearGradient(
        listOf(
            Color(0x95FFFFFF),
            Color(0x4000F0FF),
            Color(0x15FFFFFF),
            Color(0x5500F0FF),
            Color(0x80FFFFFF)
        )
    )

    val GlassPressedBorderGradient = Brush.linearGradient(
        listOf(
            Color(0xE0FFFFFF),
            Color(0xAA00F0FF),
            Color(0x60FFFFFF),
            Color(0xBB00F0FF),
            Color(0xF0FFFFFF)
        )
    )

    val SubtleGlassBorder = Brush.linearGradient(
        listOf(
            Color(0x60FFFFFF),
            Color(0x2500F0FF),
            Color(0x10FFFFFF),
            Color(0x40FFFFFF)
        )
    )
}

/**
 * Draws a realistic glass reflection that flashes/shines when pressed.
 */
fun Modifier.glassPressSheen(
    isLiquidGlass: Boolean,
    isPressed: Boolean,
    baseAlpha: Float = 0.08f
): Modifier = composed {
    val pressSheen by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = tween(durationMillis = if (isPressed) 80 else 320),
        label = "glass_press_sheen"
    )

    this.drawWithContent {
        drawContent()
        if (isLiquidGlass) {
            // 1. Resting top specular glass reflection line (physical light hitting top curve of glass)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = baseAlpha + (0.28f * pressSheen)),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = size.height * (0.35f + 0.15f * pressSheen)
                )
            )

            // 2. Dynamic pressed specular gleam: bursts across the surface on touch
            if (pressSheen > 0.01f) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.38f * pressSheen),
                            Color(0x9000F0FF).copy(alpha = 0.28f * pressSheen),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.4f),
                        radius = size.maxDimension * 0.75f
                    )
                )
            }
        }
    }
}
