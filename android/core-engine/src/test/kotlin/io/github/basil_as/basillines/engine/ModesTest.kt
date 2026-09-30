package io.github.basil_as.basillines.engine

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModesTest {
    @Test
    fun theGeneratorMatchesTheWebApp() {
        // Values computed by src/engine/rng.ts; the daily challenge must be identical on both platforms.
        val r = mulberry32(12345)
        assertEquals("0.979728267761", "%.12f".format(java.util.Locale.ROOT, r()))
        assertEquals("0.306752264500", "%.12f".format(java.util.Locale.ROOT, r()))
        assertEquals("0.484205421526", "%.12f".format(java.util.Locale.ROOT, r()))
        assertEquals(1712149114L, hashString("color-lines-daily:2026-09-30"))
        assertEquals(1712149114L, Modes.dailySeed("2026-09-30"))
        assertEquals(1235146675L, mixSeed(987654321, 7))
    }

    @Test
    fun theDailyGameIsTheSameAsOnTheWeb() {
        val e = Modes.createEngine(ModeId.DAILY, "2026-09-30")
        val rows = (0 until 9).joinToString("/") { y ->
            (0 until 9).joinToString("") { x -> e.board[x, y]?.displayName?.first()?.lowercase() ?: "." }
        }
        assertEquals("c......../....c..../........./........./...rg..../......b../........./........./.........", rows)
        assertEquals(listOf("Brown", "Green", "Cyan"), e.nextColors.map { it.displayName })
        assertEquals(listOf(Point(4, 7), Point(6, 1), Point(7, 3)), e.nextSpawnPoints)
    }

    @Test
    fun easyModeUsesFiveColours() {
        val e = Modes.createEngine(ModeId.EASY, "2026-09-30", Random(3))
        assertEquals(5, e.colors.size)
        repeat(30) {
            val from = e.board.getOccupiedCells().first().first
            val to = PathFinder.getReachableCells(from, e.board).firstOrNull() ?: return@repeat
            e.moveBall(from, to)
        }
        assertTrue(e.board.getOccupiedCells().all { it.second in Modes.EASY_COLORS })
    }

    @Test
    fun aSeededGameReplaysIdentically() {
        fun play(): String {
            val e = Modes.createEngine(ModeId.DAILY, "2026-01-01")
            repeat(15) {
                val from = e.board.getOccupiedCells().first().first
                val to = PathFinder.getReachableCells(from, e.board).lastOrNull() ?: return@repeat
                e.moveBall(from, to)
            }
            return GameStateCodec.encode(e)
        }
        assertEquals(play(), play())
    }

    @Test
    fun undoKeepsASeededGameOnTheSameTrack() {
        val e = Modes.createEngine(ModeId.DAILY, "2026-02-02")
        val from = e.board.getOccupiedCells().first().first
        val to = PathFinder.getReachableCells(from, e.board).first()
        e.moveBall(from, to)
        val after = e.nextColors to e.nextSpawnPoints
        e.undo()
        e.moveBall(from, to)
        assertEquals(after, e.nextColors to e.nextSpawnPoints)
    }

    @Test
    fun blitzHasAClockAndEndGameStopsIt() {
        val e = Modes.createEngine(ModeId.BLITZ, "x", Random(1))
        assertEquals(180_000L, Modes.remainingMs(e))
        e.addPlayTime(200_000)
        assertEquals(0L, Modes.remainingMs(e))
        e.endGame()
        assertTrue(e.isGameOver)
        assertNull(Modes.remainingMs(Modes.createEngine(ModeId.CLASSIC, "x")))
    }

    @Test
    fun theSaveRemembersModeColoursAndSeed() {
        val e = Modes.createEngine(ModeId.DAILY, "2026-03-03")
        val back = GameStateCodec.decode(GameStateCodec.encode(e))
        assertNotNull(back)
        assertEquals(ModeId.DAILY, back!!.mode)
        assertEquals(e.seed, back.seed)
        assertEquals(GameStateCodec.encode(e), GameStateCodec.encode(back))
        val easy = GameStateCodec.decode(GameStateCodec.encode(Modes.createEngine(ModeId.EASY, "d")))!!
        assertEquals(5, easy.colors.size)
    }

    @Test
    fun oldSavesAndBrokenModesAreHandled() {
        val old = GameStateCodec.encode(Modes.createEngine(ModeId.CLASSIC, "d")).split("|").take(11).toMutableList()
        old[0] = "v3"
        assertEquals(ModeId.CLASSIC, GameStateCodec.decode(old.joinToString("|"))!!.mode)
        val bad = GameStateCodec.encode(Modes.createEngine(ModeId.CLASSIC, "d")).replace("|classic|", "|turbo|")
        assertNull(GameStateCodec.decode(bad))
    }

    @Test
    fun historyRecordsTheModeAndReadsOldRecords() {
        val e = Modes.createEngine(ModeId.EASY, "d", Random(2))
        val rec = GameStats.recordFrom(e, true, 1000)
        assertEquals(ModeId.EASY, GameStats.decodeHistory(GameStats.encodeHistory(listOf(rec))).single().mode)
        assertEquals(ModeId.CLASSIC, GameStats.decodeHistory("10,1,1,1,1,1,5,100").single().mode)
        assertTrue(GameStats.decodeHistory("10,1,1,1,1,1,5,100,nope").isEmpty())
    }

    @Test
    fun hintFindsTheLineCompletingMove() {
        val b = Board()
        for (x in 0 until 4) b[x, 0] = BallColor.RED
        b[8, 8] = BallColor.RED
        val h = Hinter.find(b)!!
        assertTrue(h.clears)
        assertEquals(Point(8, 8), h.from)
        assertEquals(Point(4, 0), h.to)
        assertNull(Hinter.find(Board()))
        assertFalse(Hinter.find(Board().also { it[0, 0] = BallColor.RED; it[1, 0] = BallColor.RED; it[8, 8] = BallColor.RED })!!.clears)
    }
}
