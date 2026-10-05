package io.github.basil_as.basillines.engine

enum class DosImage { LAYOUT, SHEET }

enum class DosFont { MONO, SERIF }

enum class DosAlign { LEFT, CENTER, RIGHT }

sealed interface DosDraw {
    data class Image(val img: DosImage, val src: SpriteRect, val dx: Int, val dy: Int, val dh: Int? = null) : DosDraw
    data class Fill(val x: Int, val y: Int, val w: Int, val h: Int, val argb: Long) : DosDraw

    /** Text in scene pixels (the Russian overlay of the original labels); [y] is the baseline. */
    data class Text(
        val text: String, val x: Int, val y: Int, val argb: Long, val size: Int,
        val align: DosAlign, val font: DosFont, val shadowArgb: Long? = null
    ) : DosDraw
}

enum class EffectKind { SPAWN, BURST }

data class DosEffect(val kind: EffectKind, val x: Int, val y: Int, val color: BallColor, val startMs: Long)

data class DosState(
    val cells: List<BallColor?>,
    val selected: Point?,
    val next: List<BallColor>,
    val showNext: Boolean,
    val score: Int,
    val kingScore: Int,
    val soundOn: Boolean,
    val nowMs: Long,
    val effects: List<DosEffect> = emptyList(),
    /** When the pretender started taking the throne; null while the king still reigns. */
    val coronationStart: Long? = null,
    val pressed: Set<DosButton> = emptySet(),
    /** Russian labels over the original English ones (the hero captions always stay English). */
    val russian: Boolean = false
)

/** Everything to draw for one frame of the 1992 screen, back to front. */
object DosScene {
    private val RU_BUTTONS = mapOf(DosButton.HELP to "ПОМОЩЬ", DosButton.SOUND to "ЗВУК", DosButton.NEXT to "ДАЛЕЕ", DosButton.RESTART to "ЗАНОВО")
    private const val RU_HELP =
        "Цель игры набрать больше очков, чем «король». Очки растут, когда вы выстраиваете по горизонтали, вертикали или диагонали линию из пяти и более шаров одного цвета. Линии строятся перемещением шаров по свободным клеткам. Желаем успеха!"

    /** Splits [text] at word boundaries into lines of at most [max] characters. */
    fun wrapText(text: String, max: Int): List<String> {
        val lines = mutableListOf<String>()
        var line = ""
        for (word in text.split(" ")) {
            val next = if (line.isEmpty()) word else "$line $word"
            if (next.length > max && line.isNotEmpty()) { lines += line; line = word } else line = next
        }
        if (line.isNotEmpty()) lines += line
        return lines
    }

    /** The Russian text over the Help or Top Ten window (empty for English, which the window picture already holds). */
    fun windowText(top10: Boolean, russian: Boolean, hall: List<HallEntry>): List<DosDraw> {
        if (!russian) return emptyList()
        val (x, y) = DosSprites.WINDOW_POS
        val out = mutableListOf<DosDraw>()
        if (!top10) {
            out += DosDraw.Fill(x + 88, y + 6, 60, 18, 0xFFAAAAAA)
            out += DosDraw.Text("Справка", x + 118, y + 20, 0xFFFF5555, 14, DosAlign.CENTER, DosFont.SERIF, 0xFFFFFFFF)
            out += DosDraw.Fill(x + 18, y + 28, 200, 122, 0xFF000000)
            wrapText(RU_HELP, 26).forEachIndexed { i, l -> out += DosDraw.Text(l, x + 118, y + 40 + i * 11, 0xFF00AA00, 9, DosAlign.CENTER, DosFont.MONO) }
        } else {
            out += DosDraw.Fill(x + 70, y + 5, 100, 18, 0xFFAAAAAA)
            out += DosDraw.Text("Десятка лучших", x + 120, y + 20, 0xFFFF5555, 13, DosAlign.CENTER, DosFont.SERIF, 0xFFFFFFFF)
            out += DosDraw.Fill(x + 12, y + 143, 92, 15, 0xFFAAAAAA)
            out += DosDraw.Text("Ваше имя", x + 16, y + 155, 0xFFFF5555, 12, DosAlign.LEFT, DosFont.SERIF)
            hall.take(10).forEachIndexed { i, h ->
                val line = (y + 39 + i * 11.6).toInt()
                out += DosDraw.Text(h.name, x + 52, line, 0xFF00AA00, 9, DosAlign.LEFT, DosFont.MONO)
                out += DosDraw.Text(h.score.toString(), x + 210, line, 0xFF00AA00, 9, DosAlign.RIGHT, DosFont.MONO)
            }
        }
        return out
    }

    val SPAWN_FRAMES = listOf(BallFrame.SMALL, BallFrame.MEDIUM, BallFrame.WIDE)
    val BURST_FRAMES = listOf(BallFrame.BURST1, BallFrame.BURST2)
    const val SPAWN_STEP = 70L
    const val BURST_STEP = 90L
    const val CORONATION_STEP = 260L

    private fun lcd(value: Int, box: SpriteRect): List<DosDraw> {
        val text = maxOf(0, value).toString().takeLast(DosSprites.LCD_CHARS)
        val out = mutableListOf<DosDraw>(DosDraw.Fill(box.x, box.y, box.w, box.h, 0xFF000000))
        val start = DosSprites.LCD_CHARS - text.length
        text.forEachIndexed { i, ch ->
            DosSprites.glyphRect(ch)?.let { out += DosDraw.Image(DosImage.SHEET, it, box.x + (start + i) * 9, box.y + 1) }
        }
        return out
    }

    private fun ballFrame(state: DosState, index: Int, effect: DosEffect?): BallFrame {
        val x = index % 9
        val y = index / 9
        if (state.selected?.x == x && state.selected.y == y) {
            return if ((state.nowMs / 140) % 2 == 0L) BallFrame.FULL else BallFrame.WIDE
        }
        if (effect?.kind == EffectKind.SPAWN) {
            val step = ((state.nowMs - effect.startMs) / SPAWN_STEP).toInt()
            return if (step < SPAWN_FRAMES.size) SPAWN_FRAMES[maxOf(0, step)] else BallFrame.FULL
        }
        return BallFrame.FULL
    }

    fun build(state: DosState): List<DosDraw> {
        val draws = mutableListOf<DosDraw>(
            DosDraw.Image(DosImage.LAYOUT, SpriteRect(0, 0, DosSprites.SCREEN_W, DosSprites.SCREEN_H), 0, 0)
        )
        draws += lcd(state.kingScore, DosSprites.LCD_KING)
        draws += lcd(state.score, DosSprites.LCD_PLAYER)

        for (i in 0 until 3) {
            val dx = DosSprites.NEXT_X + DosSprites.NEXT_PITCH * i
            val color = state.next.getOrNull(i)
            draws += if (state.showNext && color != null) {
                DosDraw.Image(DosImage.SHEET, DosSprites.ballRect(color, BallFrame.SMALL), dx, DosSprites.NEXT_Y)
            } else {
                DosDraw.Image(DosImage.LAYOUT, DosSprites.EMPTY_CELL, dx, DosSprites.NEXT_Y)
            }
        }

        val effectAt = state.effects.associateBy { it.y * 9 + it.x }
        state.cells.forEachIndexed { index, color ->
            if (color == null) return@forEachIndexed
            val (dx, dy) = DosSprites.cellOrigin(index % 9, index / 9)
            draws += DosDraw.Image(DosImage.SHEET, DosSprites.ballRect(color, ballFrame(state, index, effectAt[index])), dx, dy)
        }
        for (e in state.effects) {
            if (e.kind != EffectKind.BURST || state.cells[e.y * 9 + e.x] != null) continue
            val step = ((state.nowMs - e.startMs) / BURST_STEP).toInt()
            if (step < 0 || step >= BURST_FRAMES.size) continue
            val (dx, dy) = DosSprites.cellOrigin(e.x, e.y)
            draws += DosDraw.Image(DosImage.SHEET, DosSprites.ballRect(e.color, BURST_FRAMES[step]), dx, dy)
        }

        val dt = state.coronationStart?.let { state.nowMs - it } ?: -1L
        var kingFrame = ((state.nowMs / 700) % 2).toInt() // the gold on the crown glints
        // The pretender lifts his sword now and then, more eagerly the closer he gets to the record (as on the web).
        val closing = state.kingScore > 0 && state.score >= state.kingScore * 0.75
        val swordPeriod = if (closing) 1800L else 4200L
        var pretenderFrame = if (state.nowMs % swordPeriod >= swordPeriod - 650) 5 else 0
        if (dt >= 0) {
            val step = (dt / CORONATION_STEP).toInt()
            kingFrame = when (step) { 0 -> 2; 1 -> 3; else -> 4 }
            pretenderFrame = minOf(4, step + 1)
        }
        draws += DosDraw.Image(DosImage.SHEET, DosSprites.kingRect(kingFrame), DosSprites.KING_POS.first, DosSprites.KING_POS.second)

        // The pretender's pillar grows with the score: lift the figure and the top of the pedestal, fill the gap with more
        // pedestal body and keep the foot ring where it was.
        val rise = if (dt >= 0) DosSprites.Tower.MAX_RISE else DosSprites.towerRise(state.score, state.kingScore)
        if (rise > 0) {
            val chunkH = DosSprites.Tower.STRETCH.sy - DosSprites.Tower.TOP // the figure, the rim and the first rows of the pedestal body
            draws += DosDraw.Fill(DosSprites.Tower.X, DosSprites.Tower.TOP - rise, DosSprites.Tower.W, DosSprites.Tower.FOOT_TOP - DosSprites.Tower.TOP + rise, 0xFF000000)
            draws += DosDraw.Image(
                DosImage.LAYOUT,
                SpriteRect(DosSprites.Tower.X, DosSprites.Tower.STRETCH.sy, DosSprites.Tower.W, DosSprites.Tower.STRETCH.sh),
                DosSprites.Tower.X,
                DosSprites.Tower.TOP + chunkH - rise,
                dh = DosSprites.Tower.FOOT_TOP - (DosSprites.Tower.TOP + chunkH - rise)
            )
            draws += DosDraw.Image(
                DosImage.LAYOUT,
                SpriteRect(DosSprites.Tower.X, DosSprites.Tower.TOP, DosSprites.Tower.W, chunkH),
                DosSprites.Tower.X,
                DosSprites.Tower.TOP - rise
            )
            draws += DosDraw.Image(
                DosImage.LAYOUT,
                SpriteRect(DosSprites.Tower.X, DosSprites.Tower.FOOT_TOP, DosSprites.Tower.W, 7),
                DosSprites.Tower.X,
                DosSprites.Tower.FOOT_TOP
            )
        }
        draws += DosDraw.Image(DosImage.SHEET, DosSprites.pretenderRect(pretenderFrame), DosSprites.PRETENDER_POS.first, DosSprites.PRETENDER_POS.second - rise)

        val lit = mapOf(
            DosButton.HELP to (DosButton.HELP in state.pressed),
            DosButton.SOUND to state.soundOn,
            DosButton.NEXT to state.showNext,
            DosButton.RESTART to (DosButton.RESTART in state.pressed)
        )
        for (button in DosButton.entries) {
            if (!state.russian) {
                draws += DosDraw.Image(DosImage.SHEET, DosSprites.labelRect(button, lit.getValue(button)), DosSprites.buttonX(button), DosSprites.LABEL_Y)
                continue
            }
            // Cover the English label with the black display and write the Russian word in the same colours.
            val bx = DosSprites.buttonX(button)
            draws += DosDraw.Fill(bx, DosSprites.BUTTON_Y, 73, 13, 0xFF000000)
            draws += DosDraw.Text(RU_BUTTONS.getValue(button), bx + 36, DosSprites.BUTTON_Y + 10, if (lit.getValue(button)) 0xFF00AA00 else 0xFF555555, 11, DosAlign.CENTER, DosFont.MONO)
        }
        if (state.russian) {
            // "Next" and "Colors" are part of the layout picture: cover them and write "Далее" and "цвета".
            draws += DosDraw.Fill(208, 8, 46, 20, 0xFFAAAAAA)
            draws += DosDraw.Fill(388, 8, 60, 20, 0xFFAAAAAA)
            draws += DosDraw.Text("Далее", 231, 24, 0xFFFFFFFF, 15, DosAlign.CENTER, DosFont.SERIF, 0xFF555555)
            draws += DosDraw.Text("цвета", 418, 24, 0xFFFFFFFF, 15, DosAlign.CENTER, DosFont.SERIF, 0xFF555555)
        }
        return draws
    }
}
