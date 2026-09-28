package com.colorlines.engine

import org.junit.Assert.*
import org.junit.Test

class GameStatsTest {

    private fun rec(score: Int, completed: Boolean = true) =
        GameRecord(score = score, endedAt = 1_700_000_000_000L + score, moves = score, lines = 1, balls = 5, completed = completed)

    @Test
    fun addRecordPutsNewestFirstAndCaps() {
        var history = emptyList<GameRecord>()
        for (i in 1..(GameStats.HISTORY_LIMIT + 5)) history = GameStats.addRecord(history, rec(i))
        assertEquals(GameStats.HISTORY_LIMIT, history.size)
        assertEquals(GameStats.HISTORY_LIMIT + 5, history.first().score)
        assertEquals(6, history.last().score)
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
        val mixed = "$good;x,y,z;-1,5,5,1,5,1;10,5,5,1,5,maybe;10,5,5"
        assertEquals(listOf(rec(40)), GameStats.decodeHistory(mixed))
    }

    @Test
    fun historyDecodeCapsLength() {
        val many = (1..(GameStats.HISTORY_LIMIT + 20)).map { rec(it) }
        assertEquals(GameStats.HISTORY_LIMIT, GameStats.decodeHistory(GameStats.encodeHistory(many)).size)
    }
}
