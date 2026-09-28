package com.colorlines.engine

import org.junit.Assert.*
import org.junit.Test

class GameEngineTest {

    @Test
    fun testBoardInitializationAndCopy() {
        val board = Board(9)
        assertEquals(81, board.getEmptyCells().size)

        board[0, 0] = BallColor.RED
        board[8, 8] = BallColor.BLUE

        assertEquals(BallColor.RED, board[0, 0])
        assertEquals(BallColor.BLUE, board[8, 8])
        assertNull(board[1, 1])

        val copy = board.copy()
        assertEquals(BallColor.RED, copy[0, 0])
        assertEquals(BallColor.BLUE, copy[8, 8])

        copy[0, 0] = BallColor.GREEN
        assertEquals(BallColor.RED, board[0, 0]) // Original unchanged
        assertEquals(BallColor.GREEN, copy[0, 0])
    }

    @Test
    fun testPathFinderWithObstacles() {
        val board = Board(9)
        val start = Point(0, 0)
        val target = Point(2, 0)

        // Block direct path with a wall: (1, 0)
        board[1, 0] = BallColor.BROWN

        // Path must go around via (0,1) -> (1,1) -> (2,1) -> (2,0)
        val path = PathFinder.findPath(start, target, board)
        assertNotNull(path)
        assertTrue(path!!.size >= 5)
        assertEquals(start, path.first())
        assertEquals(target, path.last())
        assertFalse(path.contains(Point(1, 0)))
    }

    @Test
    fun testPathFinderCompletelyBlocked() {
        val board = Board(9)
        val start = Point(0, 0)
        val target = Point(5, 5)

        // Surround start completely
        board[1, 0] = BallColor.RED
        board[0, 1] = BallColor.RED

        val path = PathFinder.findPath(start, target, board)
        assertNull("Path should be blocked", path)
    }

    @Test
    fun testLineDetectorHorizontal() {
        val board = Board(9)
        // Place 5 horizontal red balls
        for (x in 2..6) {
            board[x, 3] = BallColor.RED
        }

        val result = LineDetector.findLines(board, minLength = 5, scoringSystem = ScoringSystem.GAMOS_1992)
        assertTrue(result.hasMatches)
        assertEquals(1, result.lines.size)
        assertEquals(5, result.matchedPoints.size)
        assertEquals(10, result.score) // 2*(25) - 100 + 60 = 10
    }

    @Test
    fun testLineDetectorVertical6Balls() {
        val board = Board(9)
        for (y in 1..6) {
            board[4, y] = BallColor.BLUE
        }

        val result = LineDetector.findLines(board, minLength = 5, scoringSystem = ScoringSystem.GAMOS_1992)
        assertTrue(result.hasMatches)
        assertEquals(6, result.matchedPoints.size)
        assertEquals(12, result.score) // 2*(36) - 120 + 60 = 12
    }

    @Test
    fun testLineDetectorDiagonals() {
        val board = Board(9)
        // Diagonal Down-Right (\)
        for (i in 0..4) {
            board[i, i] = BallColor.YELLOW
        }

        val result = LineDetector.findLines(board, minLength = 5)
        assertTrue(result.hasMatches)
        assertEquals(5, result.matchedPoints.size)
        assertEquals(10, result.score)
    }

    @Test
    fun testLineDetectorCrossIntersection() {
        val board = Board(9)
        val center = Point(4, 4)

        // Horizontal line of 5
        for (x in 2..6) {
            board[x, 4] = BallColor.GREEN
        }
        // Vertical line of 5 intersecting at (4,4)
        for (y in 2..6) {
            board[4, y] = BallColor.GREEN
        }

        val result = LineDetector.findLines(board, minLength = 5)
        assertTrue(result.hasMatches)
        assertEquals(2, result.lines.size)
        // 5 + 5 - 1 intersection = 9 unique points
        assertEquals(9, result.matchedPoints.size)
        // Score: 10 + 10 = 20
        assertEquals(20, result.score)
    }

    @Test
    fun testLineDetectorLessThan5DoesNotTrigger() {
        val board = Board(9)
        for (x in 0..3) {
            board[x, 0] = BallColor.CYAN
        }
        val result = LineDetector.findLines(board, minLength = 5)
        assertFalse(result.hasMatches)
        assertEquals(0, result.score)
    }

    @Test
    fun testGameEngineScoringMoveDoesNotSpawnBalls() {
        val engine = GameEngine(size = 9)
        // Clear board for deterministic testing
        engine.board.clear()

        // Place 4 red balls in a row
        for (x in 0..3) {
            engine.board[x, 0] = BallColor.RED
        }
        // Place the 5th red ball somewhere else
        engine.board[5, 5] = BallColor.RED

        val emptyBefore = engine.board.getEmptyCells().size

        // Move 5th red ball to (4, 0) completing line of 5
        val moveResult = engine.moveBall(Point(5, 5), Point(4, 0))

        assertTrue(moveResult.success)
        assertEquals(5, moveResult.clearedPoints.size)
        assertEquals(10, moveResult.pointsEarned)
        assertEquals(10, engine.score)
        assertTrue("No balls should spawn when line is cleared", moveResult.spawnedBalls.isEmpty())

        // Line should be cleared from board
        for (x in 0..4) {
            assertNull(engine.board[x, 0])
        }

        // Empty cells should increase by 5 (all 5 balls on board were cleared)
        assertEquals(emptyBefore + 5, engine.board.getEmptyCells().size)
    }

    @Test
    fun testGameEngineUndo() {
        val engine = GameEngine(size = 9)
        engine.board.clear()

        engine.board[0, 0] = BallColor.RED
        val initialEmpty = engine.board.getEmptyCells().size

        // Non-scoring move
        val result = engine.moveBall(Point(0, 0), Point(1, 0))
        assertTrue(result.success)
        assertEquals(3, result.spawnedBalls.size)

        // Undo
        assertTrue(engine.canUndo)
        val undone = engine.undo()
        assertTrue(undone)

        assertEquals(BallColor.RED, engine.board[0, 0])
        assertNull(engine.board[1, 0])
        assertEquals(initialEmpty, engine.board.getEmptyCells().size)
    }

    @Test
    fun testAntiDiagonalLineOf7IsSingleLine() {
        val board = Board(9)
        for (i in 0..6) {
            board[i, 8 - i] = BallColor.MAGENTA
        }

        val result = LineDetector.findLines(board, minLength = 5)
        assertEquals(1, result.lines.size)
        assertEquals(7, result.matchedPoints.size)
        assertEquals(18, result.score)
    }

    @Test
    fun testFullLengthLineScoresOnceOnEveryAxis() {
        val shapes: List<(Int) -> Point> = listOf(
            { i -> Point(i, 0) },
            { i -> Point(0, i) },
            { i -> Point(i, i) },
            { i -> Point(i, 8 - i) }
        )
        for (at in shapes) {
            val board = Board(9)
            for (i in 0..8) {
                val p = at(i)
                board[p.x, p.y] = BallColor.CYAN
            }
            val result = LineDetector.findLines(board, minLength = 5)
            assertEquals(1, result.lines.size)
            assertEquals(42, result.score)
        }
    }
}
