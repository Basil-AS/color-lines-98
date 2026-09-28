package com.colorlines.engine

data class LineMatchResult(
    val lines: List<List<Point>>,
    val matchedPoints: Set<Point>,
    val score: Int
) {
    val hasMatches: Boolean get() = matchedPoints.isNotEmpty()
}

enum class ScoringSystem {
    GAMOS_1992,
    LINES_98_CLASSIC
}

object LineDetector {

    const val DEFAULT_MIN_LENGTH = 5

    private val AXES = arrayOf(
        Point(1, 0),  // Horizontal
        Point(0, 1),  // Vertical
        Point(1, 1),  // Diagonal Down-Right (\)
        Point(1, -1)  // Diagonal Up-Right (/)
    )

    fun calculateScore(lineLength: Int, system: ScoringSystem = ScoringSystem.GAMOS_1992): Int {
        if (lineLength < DEFAULT_MIN_LENGTH) return 0
        return when (system) {
            ScoringSystem.GAMOS_1992 -> {
                // Formula from Color Lines 1992 (Gamos): 2*L^2 - 20*L + 60
                2 * lineLength * lineLength - 20 * lineLength + 60
            }
            ScoringSystem.LINES_98_CLASSIC -> {
                // Standard Lines 98 table
                when (lineLength) {
                    5 -> 10
                    6 -> 12
                    7 -> 18
                    8 -> 28
                    else -> 42 + (lineLength - 9) * 16
                }
            }
        }
    }

    /**
     * Scans the board for all lines of identical ball colors >= [minLength].
     */
    fun findLines(
        board: Board,
        minLength: Int = DEFAULT_MIN_LENGTH,
        scoringSystem: ScoringSystem = ScoringSystem.GAMOS_1992
    ): LineMatchResult {
        val foundLines = mutableListOf<List<Point>>()
        val allMatchedPoints = mutableSetOf<Point>()
        val size = board.size

        // We check lines along the 4 primary axes
        for (axis in AXES) {
            val visitedInDirection = Array(size) { BooleanArray(size) }

            for (y in 0 until size) {
                for (x in 0 until size) {
                    if (visitedInDirection[y][x]) continue

                    val startColor = board[x, y] ?: continue
                    val currentLine = mutableListOf<Point>()

                    var cx = x
                    var cy = y

                    while (board.isInside(cx, cy) && board[cx, cy] == startColor) {
                        currentLine.add(Point(cx, cy))
                        visitedInDirection[cy][cx] = true
                        cx += axis.x
                        cy += axis.y
                    }

                    if (currentLine.size >= minLength) {
                        foundLines.add(currentLine)
                        allMatchedPoints.addAll(currentLine)
                    }
                }
            }
        }

        var totalScore = 0
        for (line in foundLines) {
            totalScore += calculateScore(line.size, scoringSystem)
        }

        return LineMatchResult(foundLines, allMatchedPoints, totalScore)
    }
}
