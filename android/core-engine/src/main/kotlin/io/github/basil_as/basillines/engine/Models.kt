package io.github.basil_as.basillines.engine

enum class BallColor(val id: Int, val displayName: String, val hexCode: String) {
    RED(1, "Red", "#E53935"),
    GREEN(2, "Green", "#43A047"),
    BLUE(3, "Blue", "#1E88E5"),
    CYAN(4, "Cyan", "#00ACC1"),
    MAGENTA(5, "Magenta", "#8E24AA"),
    YELLOW(6, "Yellow", "#FDD835"),
    BROWN(7, "Brown", "#6D4C41");

    companion object {
        fun fromId(id: Int): BallColor? = entries.find { it.id == id }
    }
}

data class Point(val x: Int, val y: Int) {
    fun isNeighbor(other: Point): Boolean {
        val dx = kotlin.math.abs(x - other.x)
        val dy = kotlin.math.abs(y - other.y)
        return (dx == 1 && dy == 0) || (dx == 0 && dy == 1)
    }
}
