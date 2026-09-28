package com.colorlines.engine

class Board(val size: Int = 9) {
    private val grid: Array<Array<BallColor?>> = Array(size) { arrayOfNulls(size) }

    fun isInside(x: Int, y: Int): Boolean = x in 0 until size && y in 0 until size

    fun isInside(p: Point): Boolean = isInside(p.x, p.y)

    operator fun get(x: Int, y: Int): BallColor? {
        if (!isInside(x, y)) return null
        return grid[y][x]
    }

    operator fun get(p: Point): BallColor? = get(p.x, p.y)

    operator fun set(x: Int, y: Int, color: BallColor?) {
        require(isInside(x, y)) { "Coordinates ($x, $y) out of bounds for board size $size" }
        grid[y][x] = color
    }

    operator fun set(p: Point, color: BallColor?) {
        set(p.x, p.y, color)
    }

    fun isEmpty(x: Int, y: Int): Boolean = isInside(x, y) && grid[y][x] == null

    fun isEmpty(p: Point): Boolean = isEmpty(p.x, p.y)

    fun getEmptyCells(): List<Point> {
        val emptyList = mutableListOf<Point>()
        for (y in 0 until size) {
            for (x in 0 until size) {
                if (grid[y][x] == null) {
                    emptyList.add(Point(x, y))
                }
            }
        }
        return emptyList
    }

    fun getOccupiedCells(): List<Pair<Point, BallColor>> {
        val occupied = mutableListOf<Pair<Point, BallColor>>()
        for (y in 0 until size) {
            for (x in 0 until size) {
                val color = grid[y][x]
                if (color != null) {
                    occupied.add(Point(x, y) to color)
                }
            }
        }
        return occupied
    }

    fun clear() {
        for (y in 0 until size) {
            for (x in 0 until size) {
                grid[y][x] = null
            }
        }
    }

    fun copy(): Board {
        val newBoard = Board(size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                newBoard[x, y] = grid[y][x]
            }
        }
        return newBoard
    }
}
