package com.colorlines.engine

import java.util.ArrayDeque

object PathFinder {

    private val DIRECTIONS = arrayOf(
        Point(1, 0),
        Point(-1, 0),
        Point(0, 1),
        Point(0, -1)
    )

    /**
     * Finds shortest orthogonal path between [start] and [target] using BFS (Lee Algorithm).
     * Returns list of points from start to target (inclusive), or null if no unblocked path exists.
     */
    fun findPath(start: Point, target: Point, board: Board): List<Point>? {
        if (!board.isInside(start) || !board.isInside(target)) return null
        if (start == target) return listOf(start)
        if (!board.isEmpty(target)) return null

        val queue = ArrayDeque<Point>()
        val cameFrom = mutableMapOf<Point, Point>()
        val visited = mutableSetOf<Point>()

        queue.add(start)
        visited.add(start)

        while (queue.isNotEmpty()) {
            val current = queue.poll()
            if (current == target) {
                // Reconstruct path from target back to start
                val path = mutableListOf<Point>()
                var curr: Point? = target
                while (curr != null) {
                    path.add(curr)
                    curr = cameFrom[curr]
                }
                return path.reversed()
            }

            for (dir in DIRECTIONS) {
                val next = Point(current.x + dir.x, current.y + dir.y)
                if (board.isInside(next) && next !in visited) {
                    // Cell is passable if it's empty OR it is the target
                    if (board.isEmpty(next) || next == target) {
                        visited.add(next)
                        cameFrom[next] = current
                        queue.add(next)
                    }
                }
            }
        }

        return null
    }

    /**
     * Returns all empty cells reachable from [start] on the current [board].
     * Used for highlighting valid tap destinations on Android and Web UI.
     */
    fun getReachableCells(start: Point, board: Board): Set<Point> {
        val reachable = mutableSetOf<Point>()
        if (!board.isInside(start)) return reachable

        val queue = ArrayDeque<Point>()
        val visited = mutableSetOf<Point>()

        queue.add(start)
        visited.add(start)

        while (queue.isNotEmpty()) {
            val current = queue.poll()
            for (dir in DIRECTIONS) {
                val next = Point(current.x + dir.x, current.y + dir.y)
                if (board.isInside(next) && next !in visited && board.isEmpty(next)) {
                    visited.add(next)
                    reachable.add(next)
                    queue.add(next)
                }
            }
        }

        return reachable
    }
}
