package io.github.basil_as.basillines.engine

import kotlin.random.Random

/**
 * Compact text format for saving a game:
 * `v3|score|moves|lines|balls|over|maxLine|playMs|next|points|cells` where `next` is three comma-separated colour ids,
 * `points` the cells where they will appear as `x:y` pairs and `cells` 81 digits (0 = empty,
 * otherwise the colour id). The older `v1` (no `points`) and `v2` (no `maxLine`/`playMs`) formats
 * can still be read.
 * Undo history is not saved.
 */
object GameStateCodec {
    private const val VERSION = "v3"
    private const val V2 = "v2"
    private const val V1 = "v1"
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
            engine.maxLine,
            engine.playMs,
            engine.nextColors.joinToString(",") { it.id.toString() },
            engine.nextSpawnPoints.joinToString(",") { "${it.x}:${it.y}" },
            cells
        ).joinToString("|")
    }

    /** Returns null for anything that is not a valid saved game. */
    fun decode(text: String?, random: Random = Random.Default): GameEngine? {
        if (text.isNullOrEmpty()) return null
        val parts = text.split("|")
        val version = parts.firstOrNull()
        val expected = when (version) {
            V1 -> 8
            V2 -> 9
            VERSION -> 11
            else -> return null
        }
        if (parts.size != expected) return null
        val hasPoints = version != V1
        val hasExtras = version == VERSION

        val counters = (1..4).map { parts[it].toIntOrNull()?.takeIf { n -> n >= 0 } ?: return null }
        val over = when (parts[5]) {
            "0" -> false
            "1" -> true
            else -> return null
        }
        var maxLine = 0
        var playMs = 0L
        if (hasExtras) {
            maxLine = parts[6].toIntOrNull()?.takeIf { it >= 0 } ?: return null
            playMs = parts[7].toLongOrNull()?.takeIf { it >= 0 } ?: return null
        }
        val nextIndex = if (hasExtras) 8 else 6
        val next = parts[nextIndex].split(",").map { BallColor.fromId(it.toIntOrNull() ?: return null) ?: return null }
        if (next.size != 3) return null

        val cellText = parts.last()
        if (cellText.length != SIZE * SIZE) return null
        val cells = cellText.map { ch ->
            when (ch) {
                '0' -> null
                in '1'..'7' -> BallColor.fromId(ch - '0') ?: return null
                else -> return null
            }
        }

        var points: List<Point>? = null
        if (hasPoints) {
            val text2 = parts[nextIndex + 1]
            val parsed = if (text2.isEmpty()) emptyList() else text2.split(",").map { pair ->
                val xy = pair.split(":")
                if (xy.size != 2) return null
                val x = xy[0].toIntOrNull() ?: return null
                val y = xy[1].toIntOrNull() ?: return null
                if (x !in 0 until SIZE || y !in 0 until SIZE) return null
                Point(x, y)
            }
            val emptyCount = cells.count { it == null }
            if (parsed.size != minOf(3, emptyCount)) return null
            if (parsed.toSet().size != parsed.size) return null
            if (parsed.any { cells[it.y * SIZE + it.x] != null }) return null
            points = parsed
        }

        val engine = GameEngine(size = SIZE, random = random)
        engine.restoreState(cells, counters[0], counters[1], counters[2], counters[3], maxLine, playMs, next, points, over)
        return engine
    }
}
