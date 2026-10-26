package io.github.basil_as.basillines.engine

import org.junit.Assert.*
import org.junit.Test

class CoachTest {
    @Test fun solutionsWorkOnRealEngineAndHaveBilingualTeachingText() {
        assertEquals(6, STRATEGY_LESSONS.size)
        assertEquals(6, STRATEGY_LESSONS.map { it.id }.toSet().size)
        for (lesson in STRATEGY_LESSONS) {
            val result = strategyEngine(lesson).moveBall(lesson.from, lesson.to)
            assertTrue(lesson.id, result.success)
            assertEquals(lesson.id, lesson.expectedCleared, result.clearedPoints.size)
            assertEquals(Verdict.DONE, judgeStrategy(lesson, lesson.from, lesson.to, result.success, result.clearedPoints.size))
            for (language in listOf("en", "ru")) {
                val text = lesson.text.getValue(language)
                for (value in listOf(text.title, text.explanation, text.hint, text.success, text.wrong)) assertTrue(value.length > 10)
            }
        }
    }

    @Test fun corridorAndPreviewTeachTheStatedConsequences() {
        val corridor = STRATEGY_LESSONS.first { it.id == "corridor" }
        val engine = strategyEngine(corridor)
        assertNull(PathFinder.findPath(Point(1, 4), Point(7, 4), engine.board))
        engine.moveBall(corridor.from, corridor.to)
        assertNotNull(PathFinder.findPath(Point(1, 4), Point(7, 4), engine.board))
        val preview = STRATEGY_LESSONS.first { it.id == "preview" }
        val planned = strategyEngine(preview)
        assertEquals(BallColor.RED, planned.nextColors.first())
        assertEquals(Point(5, 4), planned.nextSpawnPoints.first())
        val result = planned.moveBall(preview.from, preview.to)
        assertTrue(result.spawnedBalls.contains(SpawnedBall(Point(5, 4), BallColor.RED)))
        assertEquals(5, result.clearedPoints.size)
    }

    @Test fun eitherOpenEndOfThePairIsAccepted() {
        val lesson = STRATEGY_LESSONS.first()
        val target = Point(1, 4)
        val result = strategyEngine(lesson).moveBall(lesson.from, target)
        assertTrue(result.success)
        assertEquals(Verdict.DONE, judgeStrategy(lesson, lesson.from, target, result.success, result.clearedPoints.size))
    }

    @Test fun otherDiagonalEndAndSafeCorridorMovesAreAccepted() {
        val diagonal = STRATEGY_LESSONS.first { it.id == "diagonal" }
        val end = Point(0, 0)
        val clear = strategyEngine(diagonal).moveBall(diagonal.from, end)
        assertTrue(clear.success)
        assertEquals(Verdict.DONE, judgeStrategy(diagonal, diagonal.from, end, clear.success, clear.clearedPoints.size))
        val corridor = STRATEGY_LESSONS.first { it.id == "corridor" }
        for (y in 0..8) for (x in 0..8) {
            if (x == 4 || (y == 4 && (x == 3 || x == 5))) continue
            if (corridor.setup.balls.any { it.first == x && it.second == y }) continue
            val target = Point(x, y)
            val engine = strategyEngine(corridor)
            val result = engine.moveBall(corridor.from, target)
            assertTrue(result.success)
            assertEquals("$x,$y", Verdict.DONE, judgeStrategy(corridor, corridor.from, target, result.success, result.clearedPoints.size))
            assertNotNull(PathFinder.findPath(Point(3, 4), Point(5, 4), engine.board))
        }
        for (target in listOf(Point(3, 4), Point(5, 4))) {
            val result = strategyEngine(corridor).moveBall(corridor.from, target)
            assertTrue(result.success)
            assertEquals(Verdict.RETRY, judgeStrategy(corridor, corridor.from, target, result.success, result.clearedPoints.size))
        }
    }

    @Test fun unrelatedSafeMovesAndFailedSolutionsMustRetry() {
        for (lesson in STRATEGY_LESSONS) {
            val wrong = if (lesson.id == "corridor") Point(0, 0) else Point(8, 7)
            val source = if (lesson.id == "corridor") Point(1, 4) else lesson.from
            val result = strategyEngine(lesson).moveBall(source, wrong)
            assertTrue(lesson.id, result.success)
            assertEquals(Verdict.RETRY, judgeStrategy(lesson, source, wrong, result.success, result.clearedPoints.size))
            assertEquals(Verdict.RETRY, judgeStrategy(lesson, lesson.from, lesson.to, false, lesson.expectedCleared))
            assertEquals(Verdict.RETRY, judgeStrategy(lesson, lesson.from, lesson.to, true, lesson.expectedCleared + 1))
        }
    }
}
