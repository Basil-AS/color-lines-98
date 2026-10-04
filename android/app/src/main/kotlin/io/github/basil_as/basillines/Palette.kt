package io.github.basil_as.basillines

import androidx.compose.ui.graphics.Color
import io.github.basil_as.basillines.engine.BallColor

enum class CellStyle { ROUNDED, WIN98, DOS }
enum class BallStyle { GLOSSY, SPRITE, DOS, NEON, CONTRAST }

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
    val windowTitleBar: Boolean,
    /** Fill and ink per ball for the shape-coded looks (Game Boy, terminal); null uses the high-contrast colours. */
    val symbolTint: ((BallColor) -> Pair<Color, Color>)? = null,
    val ballBorder: Color = Color.White
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

/** 98 Modern: the glossy Lines 98 sprites on a clean, current interface. */
private val Lines98Plus = Palette(
    background = Color(0xFFDDE5EF), panel = Color(0xFFFBFCFE), chip = Color(0xFFEAEFF6),
    boardBackground = Color(0xFFAEB6C4), cell = Color(0xFFD3D8E1),
    text = Color(0xFF1B2333), textMuted = Color(0xFF566177),
    statBackground = Color(0xFF151A24), statLabel = Color(0xFF9FB0CC), statValue = Color(0xFFFF4D4D),
    accent = Color(0xFF1F5FD1), onAccent = Color.White, danger = Color(0xFFC62828),
    reachableDot = Color(0xFF1F5FD1).copy(alpha = 0.7f),
    dark = false, cellStyle = CellStyle.ROUNDED, ballStyle = BallStyle.SPRITE, windowTitleBar = false
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

/** Material: Material 3 baseline purple, large radii, tonal surfaces (same tokens as the web theme). */
private val Material = Palette(
    background = Color(0xFFFEF7FF), panel = Color(0xFFF7F2FA), chip = Color(0xFFE8DEF8),
    boardBackground = Color(0xFFE7E0EC), cell = Color(0xFFFEF7FF),
    text = Color(0xFF1D1B20), textMuted = Color(0xFF49454F),
    statBackground = Color(0xFFE8DEF8), statLabel = Color(0xFF49454F), statValue = Color(0xFF1D1B20),
    accent = Color(0xFF6750A4), onAccent = Color.White, danger = Color(0xFFB3261E),
    reachableDot = Color(0xFF6750A4).copy(alpha = 0.7f),
    dark = false, cellStyle = CellStyle.ROUNDED, ballStyle = BallStyle.GLOSSY, windowTitleBar = false
)

/** Neon: dark violet with cyan and magenta glow. */
private val Neon = Palette(
    background = Color(0xFF07060F), panel = Color(0xFF0E0B1F), chip = Color(0xFF211A45),
    boardBackground = Color(0xFF0A0818), cell = Color(0xFF15102E),
    text = Color(0xFFF5F0FF), textMuted = Color(0xFFB7A8E6),
    statBackground = Color(0xFF0A0818), statLabel = Color(0xFFB7A8E6), statValue = Color(0xFF00F0FF),
    accent = Color(0xFF00F0FF), onAccent = Color(0xFF001A1D), danger = Color(0xFFFF3D71),
    reachableDot = Color(0xFF00F0FF).copy(alpha = 0.8f),
    dark = true, cellStyle = CellStyle.ROUNDED, ballStyle = BallStyle.NEON, windowTitleBar = false
)

/** High contrast: black and white with yellow accents; every colour also has its own shape. */
private val Contrast = Palette(
    background = Color.Black, panel = Color.Black, chip = Color(0xFF333333),
    boardBackground = Color.Black, cell = Color(0xFF0D0D0D),
    text = Color.White, textMuted = Color(0xFFE6E6E6),
    statBackground = Color.Black, statLabel = Color(0xFFE6E6E6), statValue = Color(0xFFFFE600),
    accent = Color(0xFFFFE600), onAccent = Color.Black, danger = Color(0xFFFF6B6B),
    reachableDot = Color(0xFF00E5FF),
    dark = true, cellStyle = CellStyle.ROUNDED, ballStyle = BallStyle.CONTRAST, windowTitleBar = false
)


/** Synthwave: violet dusk with hot pink and cyan glow. */
private val Synthwave = Palette(
    background = Color(0xFF140A2E), panel = Color(0xFF1C0A3A), chip = Color(0xFF3A1A78),
    boardBackground = Color(0xFF1A0A3A), cell = Color(0xFF2A1258),
    text = Color(0xFFFDEAFF), textMuted = Color(0xFFC9A6E8),
    statBackground = Color(0xFF1A0A3A), statLabel = Color(0xFFC9A6E8), statValue = Color(0xFFFFD166),
    accent = Color(0xFFFF4FBF), onAccent = Color(0xFF1A0630), danger = Color(0xFFFF5D8F),
    reachableDot = Color(0xFF4FD5FF).copy(alpha = 0.85f),
    dark = true, cellStyle = CellStyle.ROUNDED, ballStyle = BallStyle.NEON, windowTitleBar = false
)

/** Ocean: deep teal water with aqua accents. */
private val Ocean = Palette(
    background = Color(0xFF04222F), panel = Color(0xFF073447), chip = Color(0xFF0A465C),
    boardBackground = Color(0xFF052C3B), cell = Color(0xFF0A465C),
    text = Color(0xFFE6FBFF), textMuted = Color(0xFF9FD3DF),
    statBackground = Color(0xFF052C3B), statLabel = Color(0xFF9FD3DF), statValue = Color(0xFF2EE6C5),
    accent = Color(0xFF2EE6C5), onAccent = Color(0xFF02201C), danger = Color(0xFFFF7A7A),
    reachableDot = Color(0xFFFFD166).copy(alpha = 0.85f),
    dark = true, cellStyle = CellStyle.ROUNDED, ballStyle = BallStyle.GLOSSY, windowTitleBar = false
)

/** Paper: a wooden board on warm sepia paper. */
private val Paper = Palette(
    background = Color(0xFFEFE3C8), panel = Color(0xFFFBF3DF), chip = Color(0xFFF1E2BD),
    boardBackground = Color(0xFFC59A62), cell = Color(0xFFE9D3A7),
    text = Color(0xFF3A2A14), textMuted = Color(0xFF6D5A3A),
    statBackground = Color(0xFFF1E2BD), statLabel = Color(0xFF6D5A3A), statValue = Color(0xFF3A2A14),
    accent = Color(0xFF9A4A1F), onAccent = Color.White, danger = Color(0xFFA82B1C),
    reachableDot = Color(0xFF9A4A1F).copy(alpha = 0.8f),
    dark = false, cellStyle = CellStyle.ROUNDED, ballStyle = BallStyle.GLOSSY, windowTitleBar = false
)

private val GB_TINT: (BallColor) -> Pair<Color, Color> = { ball ->
        when (ball) {
            BallColor.RED -> Color(0xFF0F380F) to Color(0xFF9BBC0F)
            BallColor.GREEN -> Color(0xFF306230) to Color(0xFF9BBC0F)
            BallColor.BLUE -> Color(0xFF0F380F) to Color(0xFF8BAC0F)
            BallColor.CYAN -> Color(0xFF8BAC0F) to Color(0xFF0F380F)
            BallColor.MAGENTA -> Color(0xFF306230) to Color(0xFF8BAC0F)
            BallColor.YELLOW -> Color(0xFF9BBC0F) to Color(0xFF0F380F)
            BallColor.BROWN -> Color(0xFF0F380F) to Color(0xFF9BBC0F)
        }
    }

private val AMBER_TINT: (BallColor) -> Pair<Color, Color> = { ball ->
        when (ball) {
            BallColor.RED -> Color(0xFF7A5200) to Color(0xFFFFCC33)
            BallColor.GREEN -> Color(0xFFB37A00) to Color(0xFF120900)
            BallColor.BLUE -> Color(0xFF4D3300) to Color(0xFFFFB000)
            BallColor.CYAN -> Color(0xFFFFCC33) to Color(0xFF120900)
            BallColor.MAGENTA -> Color(0xFF7A5200) to Color(0xFFFFE28A)
            BallColor.YELLOW -> Color(0xFFFFB000) to Color(0xFF120900)
            BallColor.BROWN -> Color(0xFF3D2800) to Color(0xFFFFB000)
        }
    }

/** Game Boy (1989): four greens; the shape on each ball tells the colours apart. */
private val GameBoy = Palette(
    background = Color(0xFF8BAC0F), panel = Color(0xFF9BBC0F), chip = Color(0xFF8BAC0F),
    boardBackground = Color(0xFF306230), cell = Color(0xFF9BBC0F),
    text = Color(0xFF0F380F), textMuted = Color(0xFF133713),
    statBackground = Color(0xFF8BAC0F), statLabel = Color(0xFF133713), statValue = Color(0xFF0F380F),
    accent = Color(0xFF0F380F), onAccent = Color(0xFF9BBC0F), danger = Color(0xFF0F380F),
    reachableDot = Color(0xFF0F380F),
    dark = false, cellStyle = CellStyle.DOS, ballStyle = BallStyle.CONTRAST, windowTitleBar = false,
    symbolTint = GB_TINT, ballBorder = Color(0xFF0F380F)
)

/** Amber terminal: phosphor orange on black. */
private val Terminal = Palette(
    background = Color(0xFF120900), panel = Color(0xFF1A0D00), chip = Color(0xFF2E1A00),
    boardBackground = Color(0xFF120900), cell = Color(0xFF1F1100),
    text = Color(0xFFFFB000), textMuted = Color(0xFFC48500),
    statBackground = Color(0xFF120900), statLabel = Color(0xFFC48500), statValue = Color(0xFFFFCC33),
    accent = Color(0xFFFFCC33), onAccent = Color(0xFF120900), danger = Color(0xFFFF6A3D),
    reachableDot = Color(0xFFFFCC33),
    dark = true, cellStyle = CellStyle.DOS, ballStyle = BallStyle.CONTRAST, windowTitleBar = false,
    symbolTint = AMBER_TINT, ballBorder = Color(0xFFFFB000)
)

fun paletteFor(theme: AppTheme): Palette = when (theme) {
    AppTheme.MODERN -> Modern
    AppTheme.LIGHT -> Light
    AppTheme.MATERIAL -> Material
    AppTheme.NEON -> Neon
    AppTheme.SYNTHWAVE -> Synthwave
    AppTheme.OCEAN -> Ocean
    AppTheme.PAPER -> Paper
    AppTheme.GAMEBOY -> GameBoy
    AppTheme.TERMINAL -> Terminal
    AppTheme.CONTRAST -> Contrast
    AppTheme.LINES_98 -> Lines98
    AppTheme.LINES_98_PLUS -> Lines98Plus
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

/** Neon glow colour of a ball (brighter than the glossy one). */
fun neonColor(ball: BallColor): Color = when (ball) {
    BallColor.RED -> Color(0xFFFF2A55)
    BallColor.GREEN -> Color(0xFF2BFF5E)
    BallColor.BLUE -> Color(0xFF3A5BFF)
    BallColor.CYAN -> Color(0xFF00F0FF)
    BallColor.MAGENTA -> Color(0xFFFF3CDC)
    BallColor.YELLOW -> Color(0xFFFFE600)
    BallColor.BROWN -> Color(0xFFFF9A3C)
}

/** High-contrast fill of a ball; the shape on it (see [contrastSymbol]) carries the meaning too. */
fun contrastColor(ball: BallColor): Color = when (ball) {
    BallColor.RED -> Color(0xFFFF3B30)
    BallColor.GREEN -> Color(0xFF34C759)
    BallColor.BLUE -> Color(0xFF5AA9FF)
    BallColor.CYAN -> Color(0xFF00E5FF)
    BallColor.MAGENTA -> Color(0xFFFF5CF0)
    BallColor.YELLOW -> Color(0xFFFFE600)
    BallColor.BROWN -> Color(0xFFD9A066)
}
