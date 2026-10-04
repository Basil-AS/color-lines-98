package io.github.basil_as.basillines.engine

import kotlin.random.Random

data class SpawnedBall(val point: Point, val color: BallColor)

data class MoveResult(
    val success: Boolean,
    val path: List<Point>? = null,
    val clearedPoints: Set<Point> = emptySet(),
    val pointsEarned: Int = 0,
    val spawnedBalls: List<SpawnedBall> = emptyList(),
    val isGameOver: Boolean = false,
    val reason: String? = null
)

data class GameSnapshot(
    val board: Board,
    val score: Int,
    val nextColors: List<BallColor>,
    val nextPoints: List<Point>,
    val moves: Int,
    val linesCleared: Int,
    val ballsCleared: Int,
    val maxLine: Int
)

class GameEngine(
    val size: Int = 9,
    val ballsPerSpawn: Int = 3,
    val minLineLength: Int = 5,
    val scoringSystem: ScoringSystem = ScoringSystem.GAMOS_1992,
    random: Random = Random.Default,
    /** The colours in play (easy mode uses five). */
    val colors: List<BallColor> = BallColor.entries.toList(),
    val mode: ModeId = ModeId.CLASSIC,
    /** Seeded games (the daily challenge) draw every turn from a generator that depends only on this and the move number. */
    val seed: Long? = null
) {
    private var rng: Rng = { random.nextDouble() }

    val board: Board = Board(size)
    var score: Int = 0
        private set
    var bestScore: Int = 0
        private set
    var isGameOver: Boolean = false
        private set

    var moves: Int = 0
        private set
    var linesCleared: Int = 0
        private set
    var ballsCleared: Int = 0
        private set

    /** Length of the longest line cleared in this game. */
    var maxLine: Int = 0
        private set

    /** Active play time in milliseconds, reported by the UI through [addPlayTime]. */
    var playMs: Long = 0
        private set

    var selectedPoint: Point? = null
        private set

    var nextColors: List<BallColor> = emptyList()
        private set

    /** Cells where the next balls will appear, so the UI can preview them. */
    var nextSpawnPoints: List<Point> = emptyList()
        private set

    private val undoStack = mutableListOf<GameSnapshot>()

    /** How the game is being played: decision times, hesitation, danger. */
    var tel: TelemetryCounters = TelemetryCounters()
        private set

    init {
        startNewGame()
    }

    fun startNewGame() {
        board.clear()
        moves = 0
        reseedTurn()
        score = 0
        linesCleared = 0
        ballsCleared = 0
        maxLine = 0
        playMs = 0
        tel = TelemetryCounters()
        isGameOver = false
        selectedPoint = null
        undoStack.clear()

        // Generate first prediction
        generateNextColors()

        // Initial spawn: 5 balls; the original never starts with a finished line, so re-deal if one appears.
        for (attempt in 0 until 50) {
            board.clear()
            val initialEmpty = shuffle(board.getEmptyCells())
            val initialCount = minOf(5, initialEmpty.size)
            for (i in 0 until initialCount) {
                board[initialEmpty[i]] = randomColor()
            }
            if (!LineDetector.findLines(board, minLineLength, scoringSystem).hasMatches) break
        }

        // Generate next turn prediction
        generateNextColors()
        planSpawnPoints()
    }

    private fun planSpawnPoints() {
        val empty = shuffle(board.getEmptyCells())
        nextSpawnPoints = empty.take(minOf(ballsPerSpawn, empty.size))
    }

    /** Keeps the still-free planned cells and replaces those the player just occupied. */
    private fun repairSpawnPlan() {
        val kept = nextSpawnPoints.filter { board.isEmpty(it) }.toMutableList()
        val empty = board.getEmptyCells()
        val want = minOf(ballsPerSpawn, empty.size)
        if (kept.size < want) {
            for (c in shuffle(empty)) {
                if (kept.size >= want) break
                if (c !in kept) kept.add(c)
            }
        }
        nextSpawnPoints = kept
    }

    private fun randomColor(): BallColor = colors[(rng() * colors.size).toInt()]

    private fun generateNextColors() {
        nextColors = List(ballsPerSpawn) { randomColor() }
    }

    /** Fisher-Yates from the end, exactly like the web engine, so seeded games match. */
    private fun <T> shuffle(list: List<T>): List<T> {
        val arr = list.toMutableList()
        for (i in arr.size - 1 downTo 1) {
            val j = (rng() * (i + 1)).toInt()
            val tmp = arr[i]; arr[i] = arr[j]; arr[j] = tmp
        }
        return arr
    }

    private fun reseedTurn() {
        if (seed != null) rng = mulberry32(mixSeed(seed, moves))
    }

    /** Ends the game right now (a timed mode ran out of time). */
    fun endGame() {
        isGameOver = true
        selectedPoint = null
        undoStack.clear()
    }

    fun selectCell(point: Point): Boolean {
        if (isGameOver) return false
        if (!board.isInside(point)) return false

        val cellColor = board[point]
        if (cellColor != null) {
            // Select or switch selection to this ball
            selectedPoint = point
            return true
        }

        // Tap on empty cell: if a ball is currently selected, attempt to move
        val currentSelected = selectedPoint ?: return false
        val moveResult = moveBall(currentSelected, point)
        if (moveResult.success) {
            selectedPoint = null
        }
        return moveResult.success
    }

    fun unselect() {
        selectedPoint = null
    }

    fun getReachableCellsForSelected(): Set<Point> {
        val selected = selectedPoint ?: return emptySet()
        return PathFinder.getReachableCells(selected, board)
    }

    fun moveBall(from: Point, to: Point): MoveResult {
        if (isGameOver) {
            return MoveResult(success = false, reason = "Game is over")
        }
        val movingColor = board[from]
            ?: return MoveResult(success = false, reason = "Source cell is empty")

        if (!board.isEmpty(to)) {
            return MoveResult(success = false, reason = "Destination cell is not empty")
        }

        val path = PathFinder.findPath(from, to, board)
            ?: run { tel.miss(); return MoveResult(success = false, reason = "No unblocked path to destination") }

        // Save snapshot for Undo
        saveUndoSnapshot()

        // Execute move
        val linesBefore = linesCleared
        moves++
        reseedTurn()
        board[from] = null
        board[to] = movingColor
        repairSpawnPlan()

        // 1. Check if the moved ball completed a line
        val lineMatch = LineDetector.findLines(board, minLineLength, scoringSystem)

        if (lineMatch.hasMatches) {
            // Remove balls in completed lines
            for (p in lineMatch.matchedPoints) {
                board[p] = null
            }
            score += lineMatch.score
            linesCleared += lineMatch.lines.size
            ballsCleared += lineMatch.matchedPoints.size
            noteLongestLine(lineMatch.lines)
            if (score > bestScore) {
                bestScore = score
            }

            // In classic Color Lines, scoring a line gives a free turn: NO balls spawn!
            tel.moved(moves, linesCleared - linesBefore, board.getEmptyCells().size)
            return MoveResult(
                success = true,
                path = path,
                clearedPoints = lineMatch.matchedPoints,
                pointsEarned = lineMatch.score,
                spawnedBalls = emptyList(),
                isGameOver = false
            )
        }

        // 2. If no line was formed, spawn the next 3 balls
        val spawned = mutableListOf<SpawnedBall>()
        val spawnCount = minOf(nextColors.size, nextSpawnPoints.size)

        for (i in 0 until spawnCount) {
            val p = nextSpawnPoints[i]
            val color = nextColors[i]
            board[p] = color
            spawned.add(SpawnedBall(p, color))
        }

        // 3. Check if newly spawned balls formed any lines automatically
        val afterSpawnMatch = LineDetector.findLines(board, minLineLength, scoringSystem)
        var postSpawnScore = 0
        val postSpawnCleared = mutableSetOf<Point>()

        if (afterSpawnMatch.hasMatches) {
            for (p in afterSpawnMatch.matchedPoints) {
                board[p] = null
                postSpawnCleared.add(p)
            }
            postSpawnScore = afterSpawnMatch.score
            score += postSpawnScore
            linesCleared += afterSpawnMatch.lines.size
            ballsCleared += afterSpawnMatch.matchedPoints.size
            noteLongestLine(afterSpawnMatch.lines)
            if (score > bestScore) {
                bestScore = score
            }
        }

        // Generate next colors for the upcoming turn and where they will appear
        generateNextColors()
        planSpawnPoints()

        // 4. Check for game over
        if (board.getEmptyCells().isEmpty()) {
            isGameOver = true
        }

        tel.moved(moves, linesCleared - linesBefore, board.getEmptyCells().size)
        return MoveResult(
            success = true,
            path = path,
            clearedPoints = postSpawnCleared,
            pointsEarned = postSpawnScore,
            spawnedBalls = spawned,
            isGameOver = isGameOver
        )
    }

    private fun noteLongestLine(lines: List<List<Point>>) {
        for (line in lines) maxLine = maxOf(maxLine, line.size)
    }

    /** Adds active play time; non-positive values are ignored. */
    fun addPlayTime(ms: Long) {
        if (ms > 0) playMs += ms
    }

    private fun saveUndoSnapshot() {
        if (undoStack.size >= 20) {
            undoStack.removeAt(0)
        }
        undoStack.add(
            GameSnapshot(
                board = board.copy(),
                score = score,
                nextColors = nextColors.toList(),
                nextPoints = nextSpawnPoints.toList(),
                moves = moves,
                linesCleared = linesCleared,
                ballsCleared = ballsCleared,
                maxLine = maxLine
            )
        )
    }

    val canUndo: Boolean get() = undoStack.isNotEmpty() && !isGameOver

    fun undo(): Boolean {
        if (!canUndo) return false
        val snapshot = undoStack.removeAt(undoStack.lastIndex)

        for (y in 0 until size) {
            for (x in 0 until size) {
                board[x, y] = snapshot.board[x, y]
            }
        }
        score = snapshot.score
        moves = snapshot.moves
        reseedTurn()
        linesCleared = snapshot.linesCleared
        ballsCleared = snapshot.ballsCleared
        maxLine = snapshot.maxLine
        nextColors = snapshot.nextColors
        nextSpawnPoints = snapshot.nextPoints
        selectedPoint = null
        isGameOver = false
        tel.undo()
        return true
    }

    /**
     * The UI reports every touch on the board with the time since the previous one (null when unknown). A timed game
     * keeps its own clock, so it passes [countTime] = false.
     */
    fun noteAction(deltaMs: Long?, countTime: Boolean = true) {
        if (deltaMs != null && countTime) addPlayTime(deltaMs)
        tel.action(deltaMs)
    }

    fun noteHint() = tel.hint()

    /** Replaces the whole game with a previously saved one. The undo history is dropped. */
    internal fun restoreState(
        cells: List<BallColor?>,
        score: Int,
        moves: Int,
        linesCleared: Int,
        ballsCleared: Int,
        maxLine: Int,
        playMs: Long,
        nextColors: List<BallColor>,
        nextPoints: List<Point>?,
        isGameOver: Boolean,
        telemetry: List<Long>? = null
    ) {
        require(cells.size == size * size) { "Expected ${size * size} cells" }
        for (y in 0 until size) for (x in 0 until size) board[x, y] = cells[y * size + x]
        this.score = score
        this.moves = moves
        reseedTurn()
        this.linesCleared = linesCleared
        this.ballsCleared = ballsCleared
        this.maxLine = maxLine
        this.playMs = playMs
        tel = TelemetryCounters().also { it.load(telemetry) }
        this.nextColors = nextColors
        this.isGameOver = isGameOver
        selectedPoint = null
        undoStack.clear()
        if (nextPoints != null) nextSpawnPoints = nextPoints else planSpawnPoints()
    }
}
