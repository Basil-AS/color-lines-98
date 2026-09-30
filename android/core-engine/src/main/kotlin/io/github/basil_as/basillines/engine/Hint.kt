package io.github.basil_as.basillines.engine

data class Hint(val from: Point, val to: Point, val clears: Boolean, val value: Int)

object Hinter {
    private val AXES = listOf(1 to 0, 0 to 1, 1 to 1, 1 to -1)

    private fun longestRun(board: Board, x: Int, y: Int): Int {
        val color = board[x, y] ?: return 0
        var best = 1
        for ((ax, ay) in AXES) {
            var n = 1
            for (s in intArrayOf(1, -1)) {
                var cx = x + ax * s
                var cy = y + ay * s
                while (board.isInside(Point(cx, cy)) && board[cx, cy] == color) {
                    n++
                    cx += ax * s
                    cy += ay * s
                }
            }
            best = maxOf(best, n)
        }
        return best
    }

    /** The move that clears the most balls, or else builds the longest run; null if nothing can move. */
    fun find(board: Board, minLength: Int = 5): Hint? {
        var best: Hint? = null
        var bestGain = -1
        for (y in 0 until board.size) for (x in 0 until board.size) {
            val color = board[x, y] ?: continue
            val from = Point(x, y)
            for (to in PathFinder.getReachableCells(from, board)) {
                val trial = board.copy()
                trial[from] = null
                trial[to] = color
                val cleared = LineDetector.findLines(trial, minLength).matchedPoints.size
                val run = if (cleared > 0) cleared else longestRun(trial, to.x, to.y)
                val gain = if (cleared > 0) 100 + cleared else run - longestRun(board, x, y)
                if (gain <= 0 && best != null) continue
                if (gain > bestGain) {
                    bestGain = gain
                    best = Hint(from, to, cleared > 0, run)
                }
            }
        }
        return best
    }
}
