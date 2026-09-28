package com.colorlines.engine

import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class GameStateCodecTest {

    private fun playedEngine(seed: Int): GameEngine {
        val engine = GameEngine(random = Random(seed))
        repeat(6) {
            val from = firstMovable(engine) ?: return engine
            engine.moveBall(from.first, from.second)
        }
        return engine
    }

    private fun firstMovable(engine: GameEngine): Pair<Point, Point>? {
        for (y in 0 until 9) for (x in 0 until 9) {
            val p = Point(x, y)
            if (engine.board[p] == null) continue
            engine.selectCell(p)
            val target = engine.getReachableCellsForSelected().firstOrNull()
            engine.unselect()
            if (target != null) return p to target
        }
        return null
    }

    private fun snapshot(e: GameEngine) = Triple(
        (0 until 9).flatMap { y -> (0 until 9).map { x -> e.board[x, y] } },
        listOf(e.score, e.moves, e.linesCleared, e.ballsCleared),
        e.nextColors to e.isGameOver
    )

    @Test
    fun roundTripPreservesEverything() {
        val engine = playedEngine(7)
        val restored = GameStateCodec.decode(GameStateCodec.encode(engine))
        assertNotNull(restored)
        assertEquals(snapshot(engine), snapshot(restored!!))
    }

    @Test
    fun restoredEngineKeepsPlayingByTheRules() {
        val restored = GameStateCodec.decode(GameStateCodec.encode(playedEngine(3)))!!
        val move = firstMovable(restored)!!
        assertTrue(restored.moveBall(move.first, move.second).success)
    }

    @Test
    fun undoHistoryIsNotPersisted() {
        val engine = playedEngine(5)
        assertTrue(engine.canUndo)
        assertFalse(GameStateCodec.decode(GameStateCodec.encode(engine))!!.canUndo)
    }

    @Test
    fun rejectsMalformedInputWithoutThrowing() {
        val valid = GameStateCodec.encode(playedEngine(1))
        val parts = valid.split("|")
        val bad = listOf(
            null,
            "",
            "garbage",
            valid.replaceFirst("v1", "v2"),
            valid.dropLast(1),                                    // board too short
            valid + "0",                                          // board too long
            valid.substring(0, valid.length - 1) + "9",           // unknown colour id
            parts.toMutableList().also { it[1] = "-3" }.joinToString("|"),
            parts.toMutableList().also { it[1] = "abc" }.joinToString("|"),
            parts.toMutableList().also { it[5] = "2" }.joinToString("|"),
            parts.toMutableList().also { it[6] = "1,2" }.joinToString("|"),
            parts.toMutableList().also { it[6] = "1,2,9" }.joinToString("|")
        )
        for (input in bad) assertNull("should reject: $input", GameStateCodec.decode(input))
    }
}
