package io.github.basil_as.basillines.engine

/** Same seeded generator as the web app (src/engine/rng.ts), so a daily challenge is identical everywhere. */
typealias Rng = () -> Double

fun mulberry32(seed: Long): Rng {
    var s = seed.toInt()
    return {
        s += 0x6d2b79f5
        var t = s
        t = (t xor (t ushr 15)) * (t or 1)
        t = t xor (t + (t xor (t ushr 7)) * (t or 61))
        ((t xor (t ushr 14)).toLong() and 0xFFFFFFFFL).toDouble() / 4294967296.0
    }
}

/** FNV-1a hash of a string to an unsigned 32-bit number (kept in a Long). */
fun hashString(text: String): Long {
    var h = 0x811c9dc5L.toInt()
    for (ch in text) {
        h = h xor ch.code
        h *= 0x01000193
    }
    return h.toLong() and 0xFFFFFFFFL
}

/** Combines a seed and a counter into a new seed (every turn of a seeded game is reseeded). */
fun mixSeed(seed: Long, n: Int): Long {
    var h = seed.toInt() xor ((n + 1) * 0x9e3779b1L.toInt())
    h = (h xor (h ushr 16)) * 0x85ebca6bL.toInt()
    h = (h xor (h ushr 13)) * 0xc2b2ae35L.toInt()
    return (h xor (h ushr 16)).toLong() and 0xFFFFFFFFL
}
