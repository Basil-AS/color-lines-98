package io.github.basil_as.basillines.engine

/**
 * Coordinates of the original Color Lines (Gamos, 1992) artwork, taken from its `lines.lib` archive:
 * the layout is the empty game screen, the sheet holds every sprite. The DOS screen is 640x350 with
 * pixels that are 1.37 times taller than wide.
 */
data class SpriteRect(val x: Int, val y: Int, val w: Int, val h: Int)

enum class BallFrame { FULL, SMALL, MEDIUM, WIDE, BURST1, BURST2 }

enum class DosButton { HELP, SOUND, NEXT, RESTART }

object DosSprites {
    const val SCREEN_W = 640
    const val SCREEN_H = 350
    const val SHEET_W = 640
    const val SHEET_H = 350

    const val BOARD_X = 171
    const val BOARD_Y = 61
    const val CELL_W = 34
    const val CELL_H = 24

    fun cellOrigin(cx: Int, cy: Int) = Pair(BOARD_X + cx * CELL_W, BOARD_Y + cy * CELL_H)

    /** Row of each colour in the ball table of the sheet. */
    private fun row(color: BallColor) = when (color) {
        BallColor.GREEN -> 0
        BallColor.RED -> 1
        BallColor.MAGENTA -> 2
        BallColor.CYAN -> 3
        BallColor.BROWN -> 4
        BallColor.YELLOW -> 5
        BallColor.BLUE -> 6
    }

    /** A ball on its grey cell tile (34x24), in one of its animation frames. */
    fun ballRect(color: BallColor, frame: BallFrame) =
        SpriteRect(44 + 34 * frame.ordinal, 171 + 24 * row(color), CELL_W, CELL_H)

    /** A cell without a ball (taken from the empty board of the layout). */
    val EMPTY_CELL = SpriteRect(BOARD_X, BOARD_Y, CELL_W, CELL_H)

    /** King frames, 2 per row: 0/1 crowned (glinting), 2/3 crown fading, 4 crownless. */
    fun kingRect(index: Int) = SpriteRect(478 + 73 * (index % 2), 1 + 74 * (index / 2), 72, 73)
    val KING_POS = Pair(51, 72)

    /** Pretender frames: 0 plain, 1-3 the crown appearing, 4 big crown, 5 sword raised. */
    fun pretenderRect(index: Int) = SpriteRect(249 + 51 * (index % 4), 170 + 48 * (index / 4), 50, 47)
    val PRETENDER_POS = Pair(516, 156)

    val LCD_KING = SpriteRect(55, 11, 73, 11)
    val LCD_PLAYER = SpriteRect(507, 11, 73, 11)
    const val LCD_CHARS = 8

    const val NEXT_X = 272
    const val NEXT_Y = 5
    const val NEXT_PITCH = 34

    fun buttonX(button: DosButton) = when (button) {
        DosButton.HELP -> 139
        DosButton.SOUND -> 272
        DosButton.NEXT -> 406
        DosButton.RESTART -> 539
    }
    const val BUTTON_Y = 321
    const val LABEL_Y = BUTTON_Y + 2

    /** Label sprite of a button: grey (idle/off) or green (pressed/on). */
    fun labelRect(button: DosButton, lit: Boolean) =
        SpriteRect(74 * (button.ordinal * 2 + if (lit) 1 else 0), 340, 73, 10)

    /** The 9x9 green bitmap font; the first cell of the first row is a blank. */
    fun glyphRect(ch: Char): SpriteRect? {
        val code = ch.code
        val (row, first) = when (code) {
            in 32..64 -> 304 to 32
            in 65..96 -> 316 to 65
            in 97..122 -> 329 to 97
            else -> return null
        }
        return SpriteRect(318 + 9 * (code - first), row, 9, 9)
    }

    val TOP_TEN_WINDOW = SpriteRect(0, 0, 238, 166)
    val HELP_WINDOW = SpriteRect(240, 0, 236, 166)
    val WINDOW_POS = Pair(206, 86)
}
