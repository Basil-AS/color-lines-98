package io.github.basil_as.basillines

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

/** Day or night for every look: AUTO follows the system, the others are the player's choice. */
enum class DayNight(val id: String) {
    AUTO("auto"), DAY("day"), NIGHT("night");

    companion object {
        fun parse(raw: String?): DayNight? = entries.firstOrNull { it.id == raw }
    }
}

/** Whether the screen is in its night form: the choice, or the system when it is AUTO. */
fun resolveNight(scheme: DayNight, systemDark: Boolean): Boolean = when (scheme) {
    DayNight.AUTO -> systemDark
    DayNight.DAY -> false
    DayNight.NIGHT -> true
}

/**
 * True when the look is made for the night, false when for the day, null when it has one form only: the original
 * games, the monochrome looks and the player's own colours keep their colours whatever the scheme is.
 */
val AppTheme.nativeNight: Boolean?
    get() = when (this) {
        AppTheme.MODERN, AppTheme.NEON, AppTheme.SYNTHWAVE, AppTheme.OCEAN, AppTheme.CONTRAST, AppTheme.RETRO98 -> true
        AppTheme.MATERIAL, AppTheme.PAPER, AppTheme.CLEAR, AppTheme.SPECTRUM, AppTheme.MEADOW, AppTheme.ORCHID, AppTheme.LINES_98_PLUS -> false
        AppTheme.CUSTOM, AppTheme.GAMEBOY, AppTheme.TERMINAL, AppTheme.LINES_98, AppTheme.COLORLINES_92 -> null
    }

private fun tint(neutral: Long, accent: Color): Color = lerp(Color(neutral), accent, 0.08f)

private fun readableOn(fill: Color): Color = if (fill.luminance() > 0.45f) Color(0xFF111318) else Color.White

/**
 * The palette of a look in the other scheme, mirroring the CSS of the web app: neutral surfaces tinted by the accent,
 * the accent lightened (night) or darkened (day) to stay readable, the board left as the look made it.
 */
fun flipped(palette: Palette, night: Boolean): Palette {
    if (palette.dark == night) return palette
    val a = palette.accent
    return if (night) {
        val accent = lerp(a, Color.White, 0.35f)
        val text = Color(0xFFEEF1F6)
        val muted = Color(0xFF9BA3B1)
        palette.copy(
            background = tint(0xFF0D1016, a), panel = tint(0xFF171A21, a), chip = tint(0xFF21252E, a), cell = tint(0xFF1F232B, a),
            text = text, textMuted = muted, statBackground = tint(0xFF171A21, a), statLabel = muted, statValue = text,
            accent = accent, onAccent = readableOn(accent), danger = lerp(palette.danger, Color.White, 0.25f),
            reachableDot = accent.copy(alpha = 0.7f), dark = true
        )
    } else {
        val accent = lerp(a, Color.Black, 0.45f)
        val text = Color(0xFF1A1D24)
        val muted = Color(0xFF565C6B)
        palette.copy(
            background = tint(0xFFEEF0F4, a), panel = tint(0xFFFAFBFC, a), chip = tint(0xFFE6E9EF, a), cell = tint(0xFFF4F5F8, a),
            text = text, textMuted = muted, statBackground = tint(0xFFE9ECF1, a), statLabel = muted, statValue = text,
            accent = accent, onAccent = readableOn(accent), danger = lerp(palette.danger, Color.Black, 0.25f),
            reachableDot = accent.copy(alpha = 0.7f), dark = false
        )
    }
}
