package io.github.basil_as.basillines.engine

import org.junit.Assert.*
import org.junit.Test

class TutorialTest {

    private fun step(id: StepId): TutorialStep = TUTORIAL_STEPS.first { it.id == id }
    private val rng: Rng = { 0.5 }

    @Test
    fun startsOnABoardAndEndsWithReadOnlySteps() {
        assertNotNull(TUTORIAL_STEPS[0].setup)
        assertTrue(TUTORIAL_STEPS.last().info)
        assertEquals(TUTORIAL_STEPS.size, TUTORIAL_STEPS.map { it.id }.toSet().size)
    }

    @Test
    fun onlyBuildsValidBoardsWithTheNextBallsOnFreeCells() {
        for (s in TUTORIAL_STEPS) {
            if (s.setup == null) continue
            val engine = stepEngine(s, rng)
            assertNotNull("Step ${s.id} engine should not be null", engine)
            assertEquals(3, engine!!.nextColors.size)
            for (q in engine.nextSpawnPoints) {
                assertNull(engine.board[q.x, q.y])
            }
            assertFalse(engine.isGameOver)
        }
    }

    @Test
    fun showsHintsOnABallAndAnEmptyCell() {
        for (s in TUTORIAL_STEPS) {
            if (s.setup == null || s.from == null) continue
            val engine = stepEngine(s, rng)
            assertNotNull(engine)
            assertNotNull("Step ${s.id} from should point to a ball", engine!!.board[s.from.x, s.from.y])
            if (s.to != null) {
                assertNull("Step ${s.id} to should point to an empty cell", engine.board[s.to.x, s.to.y])
            }
        }
    }

    @Test
    fun theLineStepClearsALineWithTheHintedMoveAndGivesAFreeTurn() {
        val s = step(StepId.LINE)
        val engine = stepEngine(s, rng)
        assertNotNull(engine)
        val res = engine!!.moveBall(s.from!!, s.to!!)
        assertTrue(res.success)
        assertTrue("Cleared points should be >= 5", res.clearedPoints.size >= 5)
        assertEquals(0, res.spawnedBalls.size)
    }

    @Test
    fun theMoveStepSpawnsTheAnnouncedBalls() {
        val s = step(StepId.SELECT)
        val engine = stepEngine(s, rng)
        assertNotNull(engine)
        val next = engine!!.nextColors.toList()
        val moveStep = step(StepId.MOVE)
        val res = engine.moveBall(moveStep.from!!, moveStep.to!!)
        assertTrue(res.success)
        assertEquals(next, res.spawnedBalls.map { it.color })
    }

    @Test
    fun theBoxedInBallOfTheBlockedStepCannotMove() {
        val s = step(StepId.BLOCKED)
        val engine = stepEngine(s, rng)
        assertNotNull(engine)
        val res = engine!!.moveBall(s.from!!, s.to!!)
        assertFalse(res.success)
    }

    @Test
    fun selectAnySelectionFinishesIt() {
        assertEquals(Verdict.DONE, judge(step(StepId.SELECT), TutorialEvent.Select(Point(6, 2))))
        assertEquals(Verdict.IGNORE, judge(step(StepId.SELECT), TutorialEvent.Move(success = false)))
    }

    @Test
    fun moveAndFreeASuccessfulMoveFinishesThemAProblematicOneDoesNot() {
        for (id in listOf(StepId.MOVE, StepId.FREE)) {
            assertEquals(Verdict.DONE, judge(step(id), TutorialEvent.Move(success = true)))
            assertEquals(Verdict.IGNORE, judge(step(id), TutorialEvent.Move(success = false)))
        }
    }

    @Test
    fun lineOnlyAMoveThatClearsCountsAnyOtherMoveRestartsTheStep() {
        assertEquals(Verdict.DONE, judge(step(StepId.LINE), TutorialEvent.Move(success = true, cleared = 5)))
        assertEquals(Verdict.RETRY, judge(step(StepId.LINE), TutorialEvent.Move(success = true, cleared = 0)))
        assertEquals(Verdict.IGNORE, judge(step(StepId.LINE), TutorialEvent.Move(success = false)))
    }

    @Test
    fun blockedTryingAndFailingIsTheLessonAMoveThatWorksRestartsIt() {
        assertEquals(Verdict.DONE, judge(step(StepId.BLOCKED), TutorialEvent.Move(success = false)))
        assertEquals(Verdict.RETRY, judge(step(StepId.BLOCKED), TutorialEvent.Move(success = true)))
    }

    @Test
    fun readOnlyStepsIgnoreTheBoard() {
        assertEquals(Verdict.IGNORE, judge(step(StepId.NEXT), TutorialEvent.Move(success = true, cleared = 5)))
        assertEquals(Verdict.IGNORE, judge(step(StepId.END), TutorialEvent.Select(Point(0, 0))))
    }
}
