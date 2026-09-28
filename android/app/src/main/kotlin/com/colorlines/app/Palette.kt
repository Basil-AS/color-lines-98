package com.colorlines.app

import androidx.compose.ui.graphics.Color
import com.colorlines.engine.BallColor

data class Palette(
    val background: Color,
    val panel: Color,
    val chip: Color,
    val boardBackground: Color,
    val cell: Color,
    val text: Color,
    val textMuted: Color,
    val statValue: Color,
    val accent: Color,
    val onAccent: Color,
    val reachableDot: Color,
    val square: Boolean
)

val ModernPalette = Palette(
    background = Color(0xFF121217),
    panel = Color(0xFF1E1E24),
    chip = Color(0xFF141418),
    boardBackground = Color(0xFF1E1E24),
    cell = Color(0xFF2B2B36),
    text = Color(0xFFF0F0F5),
    textMuted = Color(0xFFB0B0BA),
    statValue = Color(0xFF00E676),
    accent = Color(0xFF00E676),
    onAccent = Color.Black,
    reachableDot = Color(0xFF4FC3F7).copy(alpha = 0.7f),
    square = false
)

val ClassicPalette = Palette(
    background = Color(0xFF008080),
    panel = Color(0xFFC0C0C0),
    chip = Color(0xFF808080),
    boardBackground = Color(0xFF808080),
    cell = Color(0xFFB0B0B0),
    text = Color.Black,
    textMuted = Color(0xFF303030),
    statValue = Color(0xFFFF0000),
    accent = Color(0xFF000080),
    onAccent = Color.White,
    reachableDot = Color(0xFF000080).copy(alpha = 0.6f),
    square = true
)

fun paletteFor(theme: AppTheme): Palette = if (theme == AppTheme.MODERN) ModernPalette else ClassicPalette

fun ballColor(ball: BallColor): Color = when (ball) {
    BallColor.RED -> Color(0xFFE53935)
    BallColor.GREEN -> Color(0xFF43A047)
    BallColor.BLUE -> Color(0xFF1E88E5)
    BallColor.CYAN -> Color(0xFF00ACC1)
    BallColor.MAGENTA -> Color(0xFF8E24AA)
    BallColor.YELLOW -> Color(0xFFFDD835)
    BallColor.BROWN -> Color(0xFF6D4C41)
}
