package io.github.basil_as.basillines.engine

/** The "Top Ten" of the original game: the best result is the reigning king. */
data class HallEntry(val name: String, val score: Int, val at: Long)

object Hall {
    const val SIZE = 10
    const val NAME_LIMIT = 12
    val DEFAULT_KING = HallEntry("Handicap", 100, 0)

    fun kingOf(hall: List<HallEntry>): HallEntry = hall.firstOrNull() ?: DEFAULT_KING

    /** Position (0 = king) a score would take in the table, or -1 when it does not make the top ten. */
    fun rankOf(hall: List<HallEntry>, score: Int): Int {
        if (score <= 0) return -1
        val place = hall.count { it.score >= score }
        return if (place < SIZE) place else -1
    }

    /** Adds a result; on equal scores the older one stays above. */
    fun insert(hall: List<HallEntry>, entry: HallEntry): List<HallEntry> {
        if (entry.score <= 0) return hall
        val place = rankOf(hall, entry.score)
        if (place == -1) return hall
        val clean = entry.copy(name = entry.name.take(NAME_LIMIT))
        return (hall.take(place) + clean + hall.drop(place)).take(SIZE)
    }

    fun encode(hall: List<HallEntry>): String =
        hall.joinToString(";") { "${it.score},${it.at},${it.name.replace(";", " ").replace(",", " ")}" }

    /** Malformed entries are dropped; never throws. */
    fun decode(text: String?): List<HallEntry> {
        if (text.isNullOrBlank()) return emptyList()
        return text.split(";").mapNotNull { entry ->
            val parts = entry.split(",", limit = 3)
            if (parts.size != 3) return@mapNotNull null
            val score = parts[0].toIntOrNull()?.takeIf { it >= 0 } ?: return@mapNotNull null
            val at = parts[1].toLongOrNull()?.takeIf { it >= 0 } ?: return@mapNotNull null
            HallEntry(parts[2].take(NAME_LIMIT), score, at)
        }.sortedWith(compareByDescending<HallEntry> { it.score }.thenBy { it.at }).take(SIZE)
    }
}
