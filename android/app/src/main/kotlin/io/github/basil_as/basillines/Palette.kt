package io.github.basil_as.basillines

import androidx.compose.ui.graphics.Color
import io.github.basil_as.basillines.engine.BallColor

enum class CellStyle { ROUNDED, WIN98, DOS }
enum class BallStyle { GLOSSY, SPRITE, DOS }

data class Palette(
    val background: Color,
    val panel: Color,
    val chip: Color,
    val boardBackground: Color,
    val cell: Color,
    val text: Color,
    val textMuted: Color,
    val statBackground: Color,
    val statLabel: Color,
    val statValue: Color,
    val accent: Color,
    val onAccent: Color,
    val danger: Color,
    val reachableDot: Color,
    val dark: Boolean,
    val cellStyle: CellStyle,
    val ballStyle: BallStyle,
    val windowTitleBar: Boolean
) {
    val square: Boolean get() = cellStyle != CellStyle.ROUNDED
}

private val Modern = Palette(
    background = Color(0xFF121217), panel = Color(0xFF1E1E24), chip = Color(0xFF141418),
    boardBackground = Color(0xFF17171C), cell = Color(0xFF2B2B36),
    text = Color(0xFFF0F0F5), textMuted = Color(0xFFB0B0BA),
    statBackground = Color(0xFF141418), statLabel = Color(0xFFB0B0BA), statValue = Color(0xFF00E676),
    accent = Color(0xFF00E676), onAccent = Color(0xFF06210F), danger = Color(0xFFFF5252),
    reachableDot = Color(0xFF4FC3F7).copy(alpha = 0.7f),
    dark = true, cellStyle = CellStyle.ROUNDED, ballStyle = BallStyle.GLOSSY, windowTitleBar = false
)

private val Light = Palette(
    background = Color(0xFFE9ECF5), panel = Color(0xFFFFFFFF), chip = Color(0xFFEEF0F8),
    boardBackground = Color(0xFFDFE3F0), cell = Color(0xFFF7F8FC),
    text = Color(0xFF191B26), textMuted = Color(0xFF55596B),
    statBackground = Color(0xFFEEF0F8), statLabel = Color(0xFF55596B), statValue = Color(0xFF191B26),
    accent = Color(0xFF2563EB), onAccent = Color.White, danger = Color(0xFFC62828),
    reachableDot = Color(0xFF2563EB).copy(alpha = 0.7f),
    dark = false, cellStyle = CellStyle.ROUNDED, ballStyle = BallStyle.GLOSSY, windowTitleBar = false
)

/** Lines 98 for Windows: grey window, bevelled cells, black displays with red digits, original sprites. */
private val Lines98 = Palette(
    background = Color(0xFF008080), panel = Color(0xFFC0C0C0), chip = Color(0xFFC0C0C0),
    boardBackground = Color(0xFF808080), cell = Color(0xFFC0C0C0),
    text = Color.Black, textMuted = Color(0xFF303030),
    statBackground = Color.Black, statLabel = Color(0xFFC0C0C0), statValue = Color(0xFFFF0000),
    accent = Color(0xFF000080), onAccent = Color.White, danger = Color(0xFF800000),
    reachableDot = Color(0xFF000080).copy(alpha = 0.6f),
    dark = false, cellStyle = CellStyle.WIN98, ballStyle = BallStyle.SPRITE, windowTitleBar = true
)

/** Color Lines 1992 for DOS: the 16 EGA/VGA colours, hard bevels, flat-shaded balls. */
private val ColorLines92 = Palette(
    background = Color.Black, panel = Color(0xFFAAAAAA), chip = Color(0xFFAAAAAA),
    boardBackground = Color(0xFF555555), cell = Color(0xFFAAAAAA),
    text = Color.Black, textMuted = Color(0xFF303030),
    statBackground = Color.Black, statLabel = Color(0xFF55FFFF), statValue = Color(0xFFFFFF55),
    accent = Color(0xFF0000AA), onAccent = Color.White, danger = Color(0xFFAA0000),
    reachableDot = Color(0xFF0000AA).copy(alpha = 0.8f),
    dark = false, cellStyle = CellStyle.DOS, ballStyle = BallStyle.DOS, windowTitleBar = false
)

fun paletteFor(theme: AppTheme): Palette = when (theme) {
    AppTheme.MODERN -> Modern
    AppTheme.LIGHT -> Light
    AppTheme.LINES_98 -> Lines98
    AppTheme.COLORLINES_92 -> ColorLines92
}

/** Glossy (modern) colour of a ball. */
fun ballColor(ball: BallColor): Color = when (ball) {
    BallColor.RED -> Color(0xFFE53935)
    BallColor.GREEN -> Color(0xFF43A047)
    BallColor.BLUE -> Color(0xFF1E88E5)
    BallColor.CYAN -> Color(0xFF00ACC1)
    BallColor.MAGENTA -> Color(0xFF8E24AA)
    BallColor.YELLOW -> Color(0xFFFDD835)
    BallColor.BROWN -> Color(0xFF6D4C41)
}

/** Highlight, body and shadow band of a ball in the 16-colour DOS palette. */
fun dosBands(ball: BallColor): Triple<Color, Color, Color> = when (ball) {
    BallColor.RED -> Triple(Color(0xFFFF5555), Color(0xFFAA0000), Color(0xFF550000))
    BallColor.GREEN -> Triple(Color(0xFF55FF55), Color(0xFF00AA00), Color(0xFF005500))
    BallColor.BLUE -> Triple(Color(0xFF5555FF), Color(0xFF0000AA), Color(0xFF000055))
    BallColor.CYAN -> Triple(Color(0xFF55FFFF), Color(0xFF00AAAA), Color(0xFF005555))
    BallColor.MAGENTA -> Triple(Color(0xFFFF55FF), Color(0xFFAA00AA), Color(0xFF550055))
    BallColor.YELLOW -> Triple(Color(0xFFFFFF55), Color(0xFFFFAA00), Color(0xFFAA5500))
    BallColor.BROWN -> Triple(Color(0xFFFFAA55), Color(0xFFAA5500), Color(0xFF552A00))
}

/** Index of the Lines 98 sprite for a colour (same order as the web assets). */
fun spriteIndex(ball: BallColor): Int = when (ball) {
    BallColor.RED -> 0
    BallColor.GREEN -> 1
    BallColor.BLUE -> 2
    BallColor.CYAN -> 3
    BallColor.MAGENTA -> 4
    BallColor.YELLOW -> 5
    BallColor.BROWN -> 6
}
