package com.colorlines.engine

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
    val nextColors: List<BallColor>
)

class GameEngine(
    val size: Int = 9,
    val ballsPerSpawn: Int = 3,
    val minLineLength: Int = 5,
    val scoringSystem: ScoringSystem = ScoringSystem.GAMOS_1992,
    private val random: Random = Random.Default
) {
    val board: Board = Board(size)
    var score: Int = 0
        private set
    var bestScore: Int = 0
        private set
    var isGameOver: Boolean = false
        private set

    var selectedPoint: Point? = null
        private set

    var nextColors: List<BallColor> = emptyList()
        private set

    private val undoStack = mutableListOf<GameSnapshot>()

    init {
        startNewGame()
    }

    fun startNewGame() {
        board.clear()
        score = 0
        isGameOver = false
        selectedPoint = null
        undoStack.clear()

        // Generate first prediction
        generateNextColors()

        // Initial spawn: 3 or 5 balls on clean board (classic is 3 initial balls or 5 balls)
        val initialEmpty = board.getEmptyCells().shuffled(random)
        val initialCount = minOf(5, initialEmpty.size)
        for (i in 0 until initialCount) {
            val randomColor = BallColor.entries.random(random)
            board[initialEmpty[i]] = randomColor
        }

        // Generate next turn prediction
        generateNextColors()
    }

    private fun generateNextColors() {
        nextColors = List(ballsPerSpawn) { BallColor.entries.random(random) }
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
            ?: return MoveResult(success = false, reason = "No unblocked path to destination")

        // Save snapshot for Undo
        saveUndoSnapshot()

        // Execute move
        board[from] = null
        board[to] = movingColor

        // 1. Check if the moved ball completed a line
        val lineMatch = LineDetector.findLines(board, minLineLength, scoringSystem)

        if (lineMatch.hasMatches) {
            // Remove balls in completed lines
            for (p in lineMatch.matchedPoints) {
                board[p] = null
            }
            score += lineMatch.score
            if (score > bestScore) {
                bestScore = score
            }

            // In classic Color Lines, scoring a line gives a free turn: NO balls spawn!
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
        val emptyCells = board.getEmptyCells().shuffled(random)
        val spawned = mutableListOf<SpawnedBall>()
        val spawnCount = minOf(nextColors.size, emptyCells.size)

        for (i in 0 until spawnCount) {
            val p = emptyCells[i]
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
            if (score > bestScore) {
                bestScore = score
            }
        }

        // Generate next colors for the upcoming turn
        generateNextColors()

        // 4. Check for game over
        if (board.getEmptyCells().isEmpty()) {
            isGameOver = true
        }

        return MoveResult(
            success = true,
            path = path,
            clearedPoints = postSpawnCleared,
            pointsEarned = postSpawnScore,
            spawnedBalls = spawned,
            isGameOver = isGameOver
        )
    }

    private fun saveUndoSnapshot() {
        if (undoStack.size >= 20) {
            undoStack.removeAt(0)
        }
        undoStack.add(
            GameSnapshot(
                board = board.copy(),
                score = score,
                nextColors = nextColors.toList()
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
        nextColors = snapshot.nextColors
        selectedPoint = null
        isGameOver = false
        return true
    }
}
