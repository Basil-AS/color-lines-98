package com.colorlines.engine

import kotlin.random.Random

/**
 * Compact text format for saving a game:
 * `v1|score|moves|lines|balls|over|next|cells` where `next` is three comma-separated colour ids
 * and `cells` is 81 digits (0 = empty, otherwise the colour id). Undo history is not saved.
 */
object GameStateCodec {
    private const val VERSION = "v1"
    private const val SIZE = 9

    fun encode(engine: GameEngine): String {
        require(engine.size == SIZE) { "Only 9x9 games can be saved" }
        val cells = buildString {
            for (y in 0 until SIZE) for (x in 0 until SIZE) append(engine.board[x, y]?.id ?: 0)
        }
        return listOf(
            VERSION,
            engine.score,
            engine.moves,
            engine.linesCleared,
            engine.ballsCleared,
            if (engine.isGameOver) 1 else 0,
            engine.nextColors.joinToString(",") { it.id.toString() },
            cells
        ).joinToString("|")
    }

    /** Returns null for anything that is not a valid saved game. */
    fun decode(text: String?, random: Random = Random.Default): GameEngine? {
        if (text.isNullOrEmpty()) return null
        val parts = text.split("|")
        if (parts.size != 8 || parts[0] != VERSION) return null

        val counters = (1..4).map { parts[it].toIntOrNull()?.takeIf { n -> n >= 0 } ?: return null }
        val over = when (parts[5]) {
            "0" -> false
            "1" -> true
            else -> return null
        }
        val next = parts[6].split(",").map { BallColor.fromId(it.toIntOrNull() ?: return null) ?: return null }
        if (next.size != 3) return null

        val cellText = parts[7]
        if (cellText.length != SIZE * SIZE) return null
        val cells = cellText.map { ch ->
            when (ch) {
                '0' -> null
                in '1'..'7' -> BallColor.fromId(ch - '0') ?: return null
                else -> return null
            }
        }

        val engine = GameEngine(size = SIZE, random = random)
        engine.restoreState(cells, counters[0], counters[1], counters[2], counters[3], next, over)
        return engine
    }
}
