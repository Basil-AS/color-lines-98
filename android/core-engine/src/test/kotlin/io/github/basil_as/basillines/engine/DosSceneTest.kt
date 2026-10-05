package io.github.basil_as.basillines.engine

import org.junit.Assert.*
import org.junit.Test

class DosSceneTest {
    private fun state(
        cells: List<BallColor?> = List(81) { null },
        selected: Point? = null,
        showNext: Boolean = true,
        score: Int = 0,
        kingScore: Int = 100,
        soundOn: Boolean = true,
        now: Long = 0,
        effects: List<DosEffect> = emptyList(),
        coronation: Long? = null
    ) = DosState(cells, selected, listOf(BallColor.RED, BallColor.GREEN, BallColor.BLUE), showNext, score, kingScore, soundOn, now, effects, coronation)

    private fun DosDraw.sheet() = this as? DosDraw.Image

    @Test
    fun ballFramesStayInsideTheSheet() {
        for (c in BallColor.entries) for (f in BallFrame.entries) {
            val r = DosSprites.ballRect(c, f)
            assertTrue(r.x >= 0 && r.y >= 0 && r.x + r.w <= DosSprites.SHEET_W && r.y + r.h <= DosSprites.SHEET_H)
        }
        for (i in 0..6) {
            val k = DosSprites.kingRect(i)
            assertTrue(k.x + k.w <= DosSprites.SHEET_W && k.y + k.h <= DosSprites.SHEET_H)
        }
        for (i in 0..5) {
            val p = DosSprites.pretenderRect(i)
            assertTrue(p.x + p.w <= DosSprites.SHEET_W && p.y + p.h <= DosSprites.SHEET_H)
        }
    }

    @Test
    fun boardCellsSitOnTheOriginal34x24Grid() {
        assertEquals(Pair(171, 61), DosSprites.cellOrigin(0, 0))
        assertEquals(Pair(171 + 8 * 34, 61 + 8 * 24), DosSprites.cellOrigin(8, 8))
    }

    @Test
    fun glyphsStartWithABlankAndFindDigitsAndLetters() {
        assertEquals(318 + 9, DosSprites.glyphRect('!')!!.x)
        assertEquals(318 + 9 * 16, DosSprites.glyphRect('0')!!.x)
        assertNotNull(DosSprites.glyphRect('A'))
        assertNotNull(DosSprites.glyphRect('z'))
        assertNull(DosSprites.glyphRect('☃'))
    }

    @Test
    fun theLayoutIsTheBackground() {
        val first = DosScene.build(state()).first() as DosDraw.Image
        assertEquals(DosImage.LAYOUT, first.img)
        assertEquals(0, first.dx)
    }

    @Test
    fun drawsOneTileForEveryBallAtItsCell() {
        val cells = MutableList<BallColor?>(81) { null }
        cells[0] = BallColor.RED
        cells[80] = BallColor.RED
        val tiles = DosScene.build(state(cells = cells)).mapNotNull { it.sheet() }
            .filter { it.img == DosImage.SHEET && it.src.w == 34 && it.src.h == 24 && it.dy >= 61 }
        assertEquals(listOf(DosSprites.cellOrigin(0, 0), DosSprites.cellOrigin(8, 8)), tiles.map { it.dx to it.dy })
    }

    @Test
    fun nextBallsShowOnlyWhileNextIsOn() {
        val on = DosScene.build(state(showNext = true)).count { it.sheet()?.img == DosImage.SHEET }
        val off = DosScene.build(state(showNext = false)).count { it.sheet()?.img == DosImage.SHEET }
        assertTrue(on > off)
    }

    @Test
    fun theSelectedBallSquashesToBounce() {
        val cells = MutableList<BallColor?>(81) { null }
        cells[10] = BallColor.BLUE
        val at = { now: Long ->
            DosScene.build(state(cells = cells, selected = Point(1, 1), now = now)).mapNotNull { it.sheet() }
                .first { it.img == DosImage.SHEET && it.dy == DosSprites.cellOrigin(1, 1).second }.src.x
        }
        assertNotEquals(at(0), at(150))
    }

    @Test
    fun spawnGrowsAndBurstDisappears() {
        val cells = MutableList<BallColor?>(81) { null }
        cells[0] = BallColor.GREEN
        val spawn = { now: Long ->
            DosScene.build(state(cells = cells, now = now, effects = listOf(DosEffect(EffectKind.SPAWN, 0, 0, BallColor.GREEN, 0))))
                .mapNotNull { it.sheet() }.first { it.img == DosImage.SHEET && it.dx == 171 && it.dy == 61 }.src.x
        }
        assertNotEquals(spawn(10), spawn(1000))

        val burst = { now: Long ->
            DosScene.build(state(now = now, effects = listOf(DosEffect(EffectKind.BURST, 2, 3, BallColor.RED, 0))))
                .mapNotNull { it.sheet() }.count { it.img == DosImage.SHEET && it.dx == DosSprites.cellOrigin(2, 3).first && it.dy == DosSprites.cellOrigin(2, 3).second }
        }
        assertEquals(1, burst(20))
        assertEquals(0, burst(5000))
    }

    @Test
    fun scoreDisplaysUseTheOriginalDigitsRightAligned() {
        val digits = DosScene.build(state(score = 42, kingScore = 1234)).mapNotNull { it.sheet() }
            .filter { it.img == DosImage.SHEET && it.src.w == 9 && it.dy < 20 }
        assertEquals(2, digits.count { it.dx >= 507 })
        assertEquals(4, digits.count { it.dx < 200 })
        assertEquals(507 + 7 * 9, digits.filter { it.dx >= 507 }.maxOf { it.dx })
    }

    @Test
    fun theCrownMovesToThePretenderAfterTheCoronation() {
        val frame = { now: Long, start: Long? ->
            DosScene.build(state(now = now, coronation = start, score = 500)).mapNotNull { it.sheet() }
                .first { it.img == DosImage.SHEET && it.dx == 516 }.src
        }
        assertNotEquals(frame(0, null), frame(5000, 0))
        assertNotEquals(frame(100, 0), frame(5000, 0))
    }

    @Test
    fun theSoundLabelGoesGreyWhenMuted() {
        val label = { on: Boolean ->
            DosScene.build(state(soundOn = on)).mapNotNull { it.sheet() }
                .first { it.img == DosImage.SHEET && it.dy == DosSprites.LABEL_Y && it.dx == 272 }.src.x
        }
        assertNotEquals(label(true), label(false))
    }

    @Test
    fun thePretenderAnimatesToo() {
        fun frame(now: Long): SpriteRect {
            val st = DosState(List(81) { null }, null, emptyList(), false, 0, 100, false, now)
            return DosScene.build(st).filterIsInstance<DosDraw.Image>().first { it.dx == DosSprites.PRETENDER_POS.first && it.dy == DosSprites.PRETENDER_POS.second }.src
        }
        assertEquals(DosSprites.pretenderRect(0), frame(0))
        assertEquals(DosSprites.pretenderRect(5), frame(3700))
    }

    @Test
    fun russianLabelsReplaceTheEnglishOnesAndOnlyThose() {
        val st = DosState(List(81) { null }, null, emptyList(), true, 0, 100, true, 0, russian = true)
        val texts = DosScene.build(st).filterIsInstance<DosDraw.Text>().map { it.text }
        assertEquals(listOf("ПОМОЩЬ", "ЗВУК", "ДАЛЕЕ", "ЗАНОВО", "Далее", "цвета"), texts)
        // English keeps the original label pictures and draws no text at all.
        assertEquals(0, DosScene.build(st.copy(russian = false)).filterIsInstance<DosDraw.Text>().size)
        assertEquals(emptyList<DosDraw>(), DosScene.windowText(false, false, emptyList()))
        val help = DosScene.windowText(false, true, emptyList()).filterIsInstance<DosDraw.Text>().map { it.text }
        assertEquals("Справка", help.first())
        assertEquals(true, help.drop(1).all { it.length <= 26 })
        val top = DosScene.windowText(true, true, listOf(HallEntry("Ann", 300, 1))).filterIsInstance<DosDraw.Text>().map { it.text }
        assertEquals(listOf("Десятка лучших", "Ваше имя", "Ann", "300"), top)
    }

    @Test
    fun wrapTextBreaksAtWordsAndNeverExceedsTheWidth() {
        val lines = DosScene.wrapText("Цель игры набрать больше очков чем король", 14)
        assertEquals(true, lines.all { it.length <= 14 })
        assertEquals("Цель игры набрать больше очков чем король", lines.joinToString(" "))
    }

    @Test
    fun towerGrowsWithTheScoreAndTopsOutAtTheKingPillar() {
        assertEquals(0, DosSprites.towerRise(0, 100))
        assertEquals(DosSprites.Tower.MAX_RISE / 2, DosSprites.towerRise(50, 100))
        assertEquals(DosSprites.Tower.MAX_RISE, DosSprites.towerRise(100, 100))
        assertEquals(DosSprites.Tower.MAX_RISE, DosSprites.towerRise(500, 100))
        assertEquals(DosSprites.Tower.MAX_RISE, DosSprites.towerRise(10, 0))
    }

    @Test
    fun drawsTheLiftedFigureHigherAndKeepsTheFootRingInPlace() {
        val low = DosScene.build(state(score = 0, kingScore = 100))
        val high = DosScene.build(state(score = 60, kingScore = 100))
        val pretender = { draws: List<DosDraw> ->
            draws.filterIsInstance<DosDraw.Image>()
                .filter { it.img == DosImage.SHEET && it.dx == DosSprites.PRETENDER_POS.first }
                .last()
        }
        assertTrue(pretender(high).dy < pretender(low).dy)
        val foot = high.filterIsInstance<DosDraw.Image>()
            .filter { it.img == DosImage.LAYOUT && it.src.y == DosSprites.Tower.FOOT_TOP }
        assertEquals(1, foot.size)
        assertEquals(DosSprites.Tower.FOOT_TOP, foot.first().dy)
    }

    @Test
    fun stretchedDrawsHaveDestinationHeightAtLeastSourceHeight() {
        val scene = DosScene.build(state(score = 60, kingScore = 100))
        val stretched = scene.filterIsInstance<DosDraw.Image>().filter { it.dh != null }
        assertTrue(stretched.isNotEmpty())
        for (draw in stretched) {
            val dh = draw.dh!!
            assertTrue("dh ($dh) should be >= sh (${draw.src.h})", dh >= draw.src.h)
        }
    }
}

