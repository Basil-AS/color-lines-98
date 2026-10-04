package io.github.basil_as.basillines.engine

import org.junit.Assert.*
import org.junit.Test

class GameStatsTest {

    private fun rec(score: Int, completed: Boolean = true) =
        GameRecord(score = score, endedAt = 1_700_000_000_000L + score, moves = score, lines = 1, balls = 5, completed = completed, maxLine = 5, durationMs = 60_000L)

    @Test
    fun addRecordPutsNewestFirstAndKeepsEverything() {
        var history = emptyList<GameRecord>()
        for (i in 1..2500) history = GameStats.addRecord(history, rec(i))
        assertEquals(2500, history.size)
        assertEquals(2500, history.first().score)
        assertEquals(1, history.last().score)
    }

    @Test
    fun summarizeEmpty() {
        val s = GameStats.summarize(emptyList())
        assertEquals(0, s.gamesPlayed)
        assertEquals(0, s.bestScore)
        assertEquals(0, s.averageScore)
        assertNull(s.lastScore)
        assertNull(s.bestRecord)
    }

    @Test
    fun summarizeAggregates() {
        val s = GameStats.summarize(listOf(rec(30), rec(10, completed = false), rec(20)))
        assertEquals(3, s.gamesPlayed)
        assertEquals(2, s.completedGames)
        assertEquals(30, s.bestScore)
        assertEquals(20, s.averageScore)
        assertEquals(60, s.totalScore)
        assertEquals(3, s.totalLines)
        assertEquals(30, s.lastScore)
        assertEquals(30, s.bestRecord!!.score)
    }

    @Test
    fun averageIsRounded() {
        assertEquals(11, GameStats.summarize(listOf(rec(10), rec(11))).averageScore)
    }

    @Test
    fun newRecordNeedsPositiveScoreAboveBest() {
        assertTrue(GameStats.isNewRecord(100, 50))
        assertFalse(GameStats.isNewRecord(50, 50))
        assertFalse(GameStats.isNewRecord(0, 0))
        assertTrue(GameStats.isNewRecord(10, 0))
    }

    @Test
    fun historyCodecRoundTrips() {
        val history = listOf(rec(40), rec(15, completed = false))
        assertEquals(history, GameStats.decodeHistory(GameStats.encodeHistory(history)))
        assertEquals(emptyList<GameRecord>(), GameStats.decodeHistory(GameStats.encodeHistory(emptyList())))
    }

    @Test
    fun historyDecodeDropsMalformedEntriesAndNeverThrows() {
        val good = GameStats.encodeHistory(listOf(rec(40)))
        assertEquals(emptyList<GameRecord>(), GameStats.decodeHistory(null))
        assertEquals(emptyList<GameRecord>(), GameStats.decodeHistory(""))
        assertEquals(emptyList<GameRecord>(), GameStats.decodeHistory("garbage"))
        val mixed = "$good;x,y,z;-1,5,5,1,5,1,5,0;10,5,5,1,5,maybe,5,0;10,5,5"
        assertEquals(listOf(rec(40)), GameStats.decodeHistory(mixed))
    }

    @Test
    fun historyDecodeDoesNotCapLength() {
        val many = (1..1500).map { rec(it) }
        assertEquals(1500, GameStats.decodeHistory(GameStats.encodeHistory(many)).size)
    }

    @Test
    fun playDataSurvivesTheDeviceFormat() {
        val cog = Cognition.of(CogKey.entries.map { if (it == CogKey.MIN_EMPTY) 20L else (it.ordinal + 1).toLong() }.let { l -> l.mapIndexed { i, v -> if (i == 0) 112L else v } })!!
        val withPlay = rec(5).copy(cog = cog)
        assertEquals(listOf(withPlay, rec(4)), GameStats.decodeHistory(GameStats.encodeHistory(listOf(withPlay, rec(4)))))
    }

    @Test
    fun oldSixFieldRecordsStillDecode() {
        assertEquals(
            listOf(GameRecord(7, 1_700_000_000_007L, 7, 1, 5, true, 0, 0L)),
            GameStats.decodeHistory("7,1700000000007,7,1,5,1")
        )
    }
}
