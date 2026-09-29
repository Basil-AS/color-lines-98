package io.github.basil_as.basillines.engine

import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

/**
 * The rules of Color Lines / Lines 98 as documented by the originals (docs/ORIGINALS.md), one test per rule so a
 * regression names the rule that broke. Mirrors tests/rules.test.ts of the web engine.
 */
class RulesConformanceTest {
    private fun fresh(seed: Int = 1) = GameEngine(random = Random(seed))

    @Test
    fun boardIs9x9WithSevenColours() {
        assertEquals(9, fresh().size)
        assertEquals(7, BallColor.entries.size)
    }

    @Test
    fun startsWithFiveBallsAndShowsThreeNextColours() {
        val e = fresh()
        assertEquals(5, 81 - e.board.getEmptyCells().size)
        assertEquals(3, e.nextColors.size)
        assertEquals(0, e.score)
    }

    @Test
    fun movesOnlyThroughFreeCellsInFourDirections() {
        val board = Board(9)
        board[1, 0] = BallColor.RED
        board[0, 1] = BallColor.RED
        assertNull(PathFinder.findPath(Point(0, 0), Point(1, 1), board))
        val path = PathFinder.findPath(Point(0, 8), Point(8, 0), Board(9))!!
        for (i in 1 until path.size) {
            assertEquals(1, Math.abs(path[i].x - path[i - 1].x) + Math.abs(path[i].y - path[i - 1].y))
        }
    }

    @Test
    fun refusesAMoveWhenThePathIsBlocked() {
        val e = fresh()
        e.board.clear()
        e.board[0, 0] = BallColor.RED
        e.board[1, 0] = BallColor.GREEN
        e.board[0, 1] = BallColor.GREEN
        assertFalse(e.moveBall(Point(0, 0), Point(5, 5)).success)
        assertEquals(0, e.moves)
    }

    @Test
    fun clearsLinesOfFiveInARowColumnAndBothDiagonals() {
        val shapes = listOf<(Int) -> Point>({ Point(it, 2) }, { Point(2, it) }, { Point(it, it) }, { Point(it, 6 - it) })
        for (at in shapes) {
            val b = Board(9)
            for (i in 0 until 5) b[at(i).x, at(i).y] = BallColor.BLUE
            assertEquals(5, LineDetector.findLines(b).matchedPoints.size)
        }
        val four = Board(9)
        for (i in 0 until 4) four[i, 0] = BallColor.BLUE
        assertFalse(LineDetector.findLines(four).hasMatches)
    }

    @Test
    fun scores10_12_18_28_42ForLinesOf5To9() {
        assertEquals(listOf(10, 12, 18, 28, 42), (5..9).map { LineDetector.calculateScore(it, ScoringSystem.GAMOS_1992) })
        assertEquals(listOf(10, 12, 18, 28, 42), (5..9).map { LineDetector.calculateScore(it, ScoringSystem.LINES_98_CLASSIC) })
    }

    @Test
    fun aClearingMoveIsAFreeTurnAndKeepsTheAnnouncedColours() {
        val e = fresh(3)
        e.board.clear()
        for (x in 0..3) e.board[x, 0] = BallColor.RED
        e.board[5, 5] = BallColor.RED
        val next = e.nextColors
        val res = e.moveBall(Point(5, 5), Point(4, 0))
        assertEquals(5, res.clearedPoints.size)
        assertTrue(res.spawnedBalls.isEmpty())
        assertEquals(next, e.nextColors)
        assertEquals(81, e.board.getEmptyCells().size)
    }

    @Test
    fun otherwiseThreeBallsOfTheAnnouncedColoursAppear() {
        val e = fresh(4)
        e.board.clear()
        e.board[0, 0] = BallColor.RED
        e.board[1, 1] = BallColor.GREEN
        e.board[2, 2] = BallColor.BLUE
        val announced = e.nextColors
        val res = e.moveBall(Point(0, 0), Point(0, 5))
        assertTrue(res.success)
        if (res.clearedPoints.isEmpty()) {
            assertEquals(3, res.spawnedBalls.size)
            assertEquals(announced, res.spawnedBalls.map { it.color })
        }
        assertEquals(3, e.nextColors.size)
    }

    @Test
    fun endsTheGameWhenTheBoardIsFull() {
        val e = fresh(6)
        e.board.clear()
        for (y in 0 until 9) for (x in 0 until 9) e.board[x, y] = BallColor.entries[(x + 2 * y) % 7]
        e.board[0, 0] = null
        e.board[1, 0] = null
        e.board[8, 8] = null
        val res = e.moveBall(Point(2, 0), Point(1, 0))
        assertTrue(res.isGameOver)
        assertFalse(e.moveBall(Point(3, 0), Point(2, 0)).success)
    }

    @Test
    fun neverStartsWithAFinishedLine() {
        for (seed in 1..100) assertFalse(LineDetector.findLines(fresh(seed).board).hasMatches)
    }
}
