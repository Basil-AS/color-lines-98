package io.github.basil_as.basillines.engine

enum class DosImage { LAYOUT, SHEET }

sealed interface DosDraw {
    data class Image(val img: DosImage, val src: SpriteRect, val dx: Int, val dy: Int) : DosDraw
    data class Fill(val x: Int, val y: Int, val w: Int, val h: Int, val argb: Long) : DosDraw
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
    val pressed: Set<DosButton> = emptySet()
)

/** Everything to draw for one frame of the 1992 screen, back to front. */
object DosScene {
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
        draws += DosDraw.Image(DosImage.SHEET, DosSprites.pretenderRect(pretenderFrame), DosSprites.PRETENDER_POS.first, DosSprites.PRETENDER_POS.second)

        val lit = mapOf(
            DosButton.HELP to (DosButton.HELP in state.pressed),
            DosButton.SOUND to state.soundOn,
            DosButton.NEXT to state.showNext,
            DosButton.RESTART to (DosButton.RESTART in state.pressed)
        )
        for (button in DosButton.entries) {
            draws += DosDraw.Image(DosImage.SHEET, DosSprites.labelRect(button, lit.getValue(button)), DosSprites.buttonX(button), DosSprites.LABEL_Y)
        }
        return draws
    }
}
