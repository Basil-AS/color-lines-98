package io.github.basil_as.basillines.engine

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
            valid.replaceFirst("v3", "v9"),
            valid.dropLast(1),                                    // board too short
            valid + "0",                                          // board too long
            valid.substring(0, valid.length - 1) + "9",           // unknown colour id
            parts.toMutableList().also { it[1] = "-3" }.joinToString("|"),
            parts.toMutableList().also { it[1] = "abc" }.joinToString("|"),
            parts.toMutableList().also { it[5] = "2" }.joinToString("|"),
            parts.toMutableList().also { it[8] = "1,2" }.joinToString("|"),
            parts.toMutableList().also { it[8] = "1,2,9" }.joinToString("|"),
            parts.toMutableList().also { it[6] = "-1" }.joinToString("|"),
            parts.toMutableList().also { it[7] = "x" }.joinToString("|")
        )
        for (input in bad) assertNull("should reject: $input", GameStateCodec.decode(input))
    }

    @Test
    fun readsTheLegacyV1FormatAndReplansThePreview() {
        val v3 = GameStateCodec.encode(playedEngine(9)).split("|")
        val v1 = (listOf("v1") + v3.subList(1, 6) + v3[8] + v3.last()).joinToString("|")
        val restored = GameStateCodec.decode(v1)
        assertNotNull(restored)
        assertEquals(minOf(3, restored!!.board.getEmptyCells().size), restored.nextSpawnPoints.size)
    }

    @Test
    fun previewSurvivesARoundTripAndBadPointsAreRejected() {
        val engine = playedEngine(4)
        assertEquals(engine.nextSpawnPoints, GameStateCodec.decode(GameStateCodec.encode(engine))!!.nextSpawnPoints)

        val parts = GameStateCodec.encode(engine).split("|").toMutableList()
        fun withPoints(value: String) = parts.toMutableList().also { it[9] = value }.joinToString("|")
        val occupied = (0 until 81).first { engine.board[it % 9, it / 9] != null }
        for (bad in listOf("99:0,1:1,2:2", "1:1,1:1,2:2", "${occupied % 9}:${occupied / 9},1:1,2:2", "1:1", "a:b,1:1,2:2")) {
            assertNull("should reject points: $bad", GameStateCodec.decode(withPoints(bad)))
        }
    }

    @Test
    fun readsTheV2FormatWithoutLongestLineAndPlayTime() {
        val v3 = GameStateCodec.encode(playedEngine(2)).split("|")
        val v2 = (listOf("v2") + v3.subList(1, 6) + v3.subList(8, 11)).joinToString("|")
        val restored = GameStateCodec.decode(v2)
        assertNotNull(restored)
        assertEquals(0, restored!!.maxLine)
        assertEquals(0L, restored.playMs)
    }

    @Test
    fun longestLineAndPlayTimeSurviveARoundTrip() {
        val engine = playedEngine(6)
        engine.addPlayTime(4321)
        val restored = GameStateCodec.decode(GameStateCodec.encode(engine))!!
        assertEquals(engine.maxLine, restored.maxLine)
        assertEquals(4321L, restored.playMs)
    }
}
