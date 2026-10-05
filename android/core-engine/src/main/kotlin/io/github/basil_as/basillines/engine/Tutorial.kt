package io.github.basil_as.basillines.engine

import kotlin.random.Random

/**
 * The tutorial: a short scripted game on the real board and the real rules. It is a model only (steps, boards and
 * what counts as done); the screens decide how to show it. It never touches saved games, records or statistics.
 */
enum class StepId {
    SELECT,
    MOVE,
    NEXT,
    LINE,
    FREE,
    BLOCKED,
    END
}

data class StepSetup(
    val balls: List<Triple<Int, Int, BallColor>>,
    val next: List<BallColor>,
    val nextPoints: List<Point>
)

data class TutorialStep(
    val id: StepId,
    /** Read-only step: a text and a "Continue" button, nothing to do on the board. */
    val info: Boolean = false,
    /** The board this step starts on; when absent the board of the previous step simply goes on. */
    val setup: StepSetup? = null,
    /** The ball to pick up and the cell to drop it on, shown as a hint. */
    val from: Point? = null,
    val to: Point? = null
)

val TUTORIAL_STEPS: List<TutorialStep> = listOf(
    TutorialStep(
        id = StepId.SELECT,
        info = false,
        setup = StepSetup(
            balls = listOf(
                Triple(2, 4, BallColor.RED),
                Triple(6, 2, BallColor.BLUE),
                Triple(5, 6, BallColor.GREEN),
                Triple(1, 1, BallColor.YELLOW)
            ),
            next = listOf(BallColor.BLUE, BallColor.GREEN, BallColor.YELLOW),
            nextPoints = listOf(Point(7, 7), Point(0, 8), Point(8, 0))
        ),
        from = Point(2, 4)
    ),
    TutorialStep(
        id = StepId.MOVE,
        info = false,
        from = Point(2, 4),
        to = Point(2, 2)
    ),
    TutorialStep(
        id = StepId.NEXT,
        info = true
    ),
    TutorialStep(
        id = StepId.LINE,
        info = false,
        setup = StepSetup(
            balls = listOf(
                Triple(1, 5, BallColor.RED),
                Triple(2, 5, BallColor.RED),
                Triple(3, 5, BallColor.RED),
                Triple(4, 5, BallColor.RED),
                Triple(7, 2, BallColor.RED),
                Triple(6, 6, BallColor.BLUE),
                Triple(0, 0, BallColor.GREEN)
            ),
            next = listOf(BallColor.BLUE, BallColor.GREEN, BallColor.YELLOW),
            nextPoints = listOf(Point(8, 8), Point(0, 8), Point(8, 0))
        ),
        from = Point(7, 2),
        to = Point(5, 5)
    ),
    TutorialStep(
        id = StepId.FREE,
        info = false
    ),
    TutorialStep(
        id = StepId.BLOCKED,
        info = false,
        setup = StepSetup(
            balls = listOf(
                Triple(4, 4, BallColor.RED),
                Triple(3, 4, BallColor.BLUE),
                Triple(5, 4, BallColor.GREEN),
                Triple(4, 3, BallColor.YELLOW),
                Triple(4, 5, BallColor.MAGENTA)
            ),
            next = listOf(BallColor.CYAN, BallColor.BLUE, BallColor.GREEN),
            nextPoints = listOf(Point(8, 8), Point(0, 8), Point(8, 0))
        ),
        from = Point(4, 4),
        to = Point(0, 0)
    ),
    TutorialStep(
        id = StepId.END,
        info = true
    )
)

/** What the player did on the board. */
sealed class TutorialEvent {
    data class Select(val point: Point) : TutorialEvent()
    data class Move(val success: Boolean, val cleared: Int = 0) : TutorialEvent()
}

/** [DONE]: go to the next step. [RETRY]: the step starts again with a hint at what was missed. [IGNORE]: nothing happens. */
enum class Verdict {
    DONE,
    RETRY,
    IGNORE
}

fun judge(step: TutorialStep, event: TutorialEvent): Verdict {
    if (step.info) return Verdict.IGNORE
    return when (step.id) {
        StepId.SELECT -> if (event is TutorialEvent.Select) Verdict.DONE else Verdict.IGNORE
        StepId.MOVE, StepId.FREE -> {
            if (event is TutorialEvent.Move && event.success) Verdict.DONE else Verdict.IGNORE
        }
        StepId.LINE -> {
            if (event !is TutorialEvent.Move || !event.success) Verdict.IGNORE
            else if (event.cleared > 0) Verdict.DONE else Verdict.RETRY
        }
        StepId.BLOCKED -> {
            if (event !is TutorialEvent.Move) Verdict.IGNORE
            else if (event.success) Verdict.RETRY else Verdict.DONE
        }
        StepId.NEXT, StepId.END -> Verdict.IGNORE
    }
}

/** Builds an engine on a step's board, score 0, the step's next colours and next spawn points. */
fun stepEngine(step: TutorialStep, rng: Random = Random.Default): GameEngine? {
    val setup = step.setup ?: return null
    val size = 9
    val cells = MutableList<BallColor?>(size * size) { null }
    for ((x, y, color) in setup.balls) {
        if (x !in 0 until size || y !in 0 until size) return null
        if (cells[y * size + x] != null) return null
        cells[y * size + x] = color
    }

    val seen = mutableSetOf<Point>()
    for (p in setup.nextPoints) {
        if (p.x !in 0 until size || p.y !in 0 until size) return null
        if (cells[p.y * size + p.x] != null) return null
        if (!seen.add(p)) return null
    }

    val engine = GameEngine(size = size, random = rng)
    engine.restoreState(
        cells = cells,
        score = 0,
        moves = 0,
        linesCleared = 0,
        ballsCleared = 0,
        maxLine = 0,
        playMs = 0L,
        nextColors = setup.next.toList(),
        nextPoints = setup.nextPoints.toList(),
        isGameOver = false,
        telemetry = null
    )
    return engine
}

/** Overload accepting an [Rng] function, adapted to [Random]. */
fun stepEngine(step: TutorialStep, rng: Rng): GameEngine? =
    stepEngine(
        step,
        object : Random() {
            override fun nextBits(bitCount: Int): Int {
                val d = rng().coerceIn(0.0, 0.9999999999999999)
                return (d * (1L shl bitCount)).toInt()
            }
            override fun nextDouble(): Double = rng()
        }
    )
