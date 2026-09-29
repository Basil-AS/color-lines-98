package io.github.basil_as.basillines.engine

import org.junit.Assert.*
import org.junit.Test

class HallTest {
    private fun e(name: String, score: Int, at: Long = 1) = HallEntry(name, score, at)

    @Test
    fun theDefaultKingIsHandicapWith100Points() {
        assertEquals(Hall.DEFAULT_KING, Hall.kingOf(emptyList()))
        assertEquals("Handicap", Hall.DEFAULT_KING.name)
        assertEquals(100, Hall.DEFAULT_KING.score)
        assertEquals("Ann", Hall.kingOf(listOf(e("Ann", 500))).name)
    }

    @Test
    fun insertKeepsTheTableSortedAndCapped() {
        var hall = emptyList<HallEntry>()
        for ((n, s) in listOf("a" to 30, "b" to 90, "c" to 60)) hall = Hall.insert(hall, e(n, s))
        assertEquals(listOf(90, 60, 30), hall.map { it.score })
        for (i in 1..13) hall = Hall.insert(hall, e("p$i", i * 10))
        assertEquals(Hall.SIZE, hall.size)
        assertEquals(130, hall.first().score)
    }

    @Test
    fun aTieGoesBelowTheOlderResultAndZeroIsIgnored() {
        val hall = Hall.insert(listOf(e("old", 50, 1)), e("new", 50, 2))
        assertEquals(listOf("old", "new"), hall.map { it.name })
        assertTrue(Hall.insert(emptyList(), e("zero", 0)).isEmpty())
    }

    @Test
    fun rankOfReturnsThePlaceOrMinusOne() {
        val hall = (0 until Hall.SIZE).map { e("p", (Hall.SIZE - it) * 10) }
        assertEquals(0, Hall.rankOf(hall, 1000))
        assertEquals(Hall.SIZE - 5, Hall.rankOf(hall, 55))
        assertEquals(-1, Hall.rankOf(hall, 5))
        assertEquals(0, Hall.rankOf(emptyList(), 10))
        assertEquals(-1, Hall.rankOf(emptyList(), 0))
    }

    @Test
    fun theCodecRoundTripsAndDropsJunk() {
        val hall = listOf(e("Ann", 300, 5), e("Bob", 200, 6))
        assertEquals(hall, Hall.decode(Hall.encode(hall)))
        assertEquals(emptyList<HallEntry>(), Hall.decode(null))
        assertEquals(emptyList<HallEntry>(), Hall.decode("garbage"))
        assertEquals(listOf(e("ok", 10, 1)), Hall.decode("10,1,ok;-5,1,neg;x,y,z;7"))
        assertEquals("Alexander th", Hall.decode("9,1,Alexander the Great").single().name)
    }
}
