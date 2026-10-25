package io.github.basil_as.basillines.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LookTest {
    @Test
    fun everyThemeIsInExactlyOneFamily() {
        val all = LookSpec.FAMILIES.flatMap { it.second }
        assertEquals(LookSpec.THEME_IDS.sorted(), all.sorted())
        assertEquals(all.size, all.toSet().size)
    }

    @Test
    fun aThemeFollowsItsDefaultsUntilSomethingIsChosen() {
        assertEquals(LookSpec.defaults("modern"), LookSpec.effective("modern", null))
        assertEquals(LookSpec.defaults("modern"), LookSpec.effective("modern", LookOverrides()))
        assertEquals(Volume.FLAT, LookSpec.effective("modern", LookOverrides(volume = Volume.FLAT)).volume)
        assertEquals(ShapeSet.SHAPES, LookSpec.effective("gameboy", null).shapes)
        assertFalse(LookSpec.studioActive("modern", null))
        assertTrue(LookSpec.studioActive("modern", LookOverrides(volume = Volume.FLAT)))
        assertTrue(LookSpec.studioActive("modern", LookOverrides(palette = PaletteId.DISTINCT)))
        assertFalse(LookSpec.studioActive("modern", LookOverrides(ring = RingLevel.MAX, grid = GridStyle.DOTS)))
        assertTrue(LookSpec.studioActive("custom", null))
    }

    @Test
    fun theOriginalGamesAreLeftAlone() {
        for (id in listOf("lines98", "lines98plus", "colorlines92")) {
            assertFalse(LookSpec.isStudioTheme(id))
            assertFalse(LookSpec.studioActive(id, LookOverrides(volume = Volume.FLAT)))
        }
    }

    @Test
    fun theCustomThemeUsesItsOwnBallsUnlessASetIsChosen() {
        assertEquals(PaletteId.CUSTOM, LookSpec.paletteInUse("custom", null))
        assertEquals(PaletteId.DISTINCT, LookSpec.paletteInUse("custom", LookOverrides(palette = PaletteId.DISTINCT)))
        assertEquals(PaletteId.CLASSIC, LookSpec.paletteInUse("modern", null))
    }

    @Test
    fun redAndVioletAreFarApartInTheVividAndDistinctSets() {
        val vivid = LookPalettes.SETS.getValue(PaletteId.VIVID)
        assertEquals("#f0261b", Colors.hex(vivid.getValue(BallColor.RED)))
        assertEquals("#8b3dff", Colors.hex(vivid.getValue(BallColor.MAGENTA)))
        for (set in listOf(PaletteId.VIVID, PaletteId.DISTINCT)) {
            val s = LookPalettes.SETS.getValue(set)
            assertTrue(Colors.distance(s.getValue(BallColor.RED), s.getValue(BallColor.MAGENTA)) > 100.0)
        }
    }

    @Test
    fun theDistinctSetDiffersInLightnessToo() {
        val lum = LookPalettes.SETS.getValue(PaletteId.DISTINCT).values.map { Colors.luminance(it) }.sorted()
        for (i in 1 until lum.size) assertTrue(lum[i] - lum[i - 1] > 0.01)
    }

    @Test
    fun tonesHaveHighlightShadeAndReadableInk() {
        val tones = LookPalettes.tones(PaletteId.DISTINCT, CustomTheme.DEFAULT.balls)
        assertEquals(Colors.BLACK, tones.getValue(BallColor.YELLOW).ink)
        assertEquals(Colors.WHITE, tones.getValue(BallColor.BLUE).ink)
        for (t in tones.values) assertTrue(Colors.luminance(t.hi) > Colors.luminance(t.lo))
    }

    @Test
    fun colourMathMatchesTheWebImplementation() {
        assertEquals(0xFFFF8000.toInt(), Colors.argb("#ff8000"))
        assertEquals(0xFFAABBCC.toInt(), Colors.argb("#abc"))
        assertNull(Colors.argb("nope"))
        assertEquals("#808080", Colors.hex(Colors.mix(Colors.BLACK, Colors.WHITE, 0.5f)))
        assertEquals(21.0, Colors.contrast(Colors.BLACK, Colors.WHITE), 0.01)
        assertTrue(Colors.isDark(Colors.argb("#101418")!!))
        val fixed = Colors.ensureContrast(Colors.argb("#8a8a8a")!!, Colors.WHITE, 4.5)
        assertTrue(Colors.contrast(fixed, Colors.WHITE) >= 4.5)
    }

    @Test
    fun customColoursAreReadableOnLightAndDarkSurfaces() {
        for (dark in listOf(false, true)) {
            val c = CustomTheme.preset(dark).colors()
            assertTrue(Colors.contrast(c.text, c.panel) > 7.0)
            for (surface in listOf(c.panel, c.stat, c.ctrl)) assertTrue(Colors.contrast(c.muted, surface) >= 4.5)
            assertTrue(Colors.contrast(c.accent, c.panel) >= 3.0)
            assertTrue(Colors.contrast(c.onAccent, c.accent) > 4.0)
        }
    }

    @Test
    fun warnsAboutSimilarAndFaintColours() {
        val alike = CustomTheme.DEFAULT.balls + (BallColor.MAGENTA to Colors.argb("#f0281c")!!)
        assertTrue(ballWarnings(alike, Colors.WHITE).similar.contains(BallColor.RED to BallColor.MAGENTA))
        assertTrue(ballWarnings(CustomTheme.DEFAULT.balls, CustomTheme.DEFAULT.cell).isClean)
        val dark = CustomTheme.preset(true)
        assertTrue(ballWarnings(dark.balls, dark.cell).isClean)
        val faint = ballWarnings(CustomTheme.DEFAULT.balls + (BallColor.YELLOW to Colors.argb("#f4f4f4")!!), Colors.argb("#f2f6f8")!!)
        assertTrue(faint.faint.contains(BallColor.YELLOW))
    }

    @Test
    fun unknownOptionsAreDropped() {
        val raw = mapOf(
            "modern" to mapOf("volume" to "glossy", "contrast" to "insane", "nope" to 1),
            "bogus" to mapOf("volume" to "flat"),
            "neon" to "x",
            "light" to emptyMap<String, Any?>()
        )
        assertEquals(mapOf("modern" to LookOverrides(volume = Volume.GLOSSY)), LookStore.storeFrom(raw))
        assertEquals(emptyMap<String, LookOverrides>(), LookStore.storeFrom(null))
    }

    @Test
    fun theCheckerGridWithLinesHasItsOwnId() {
        assertEquals("checkerLines", GridStyle.CHECKER_LINES.id)
        val o = LookOverrides(grid = GridStyle.CHECKER_LINES)
        assertEquals(o, LookStore.overridesFrom(Json.parse(Json.write(LookStore.overridesTo(o)))))
        assertEquals(GridStyle.entries.size, GridStyle.entries.map { it.id }.toSet().size)
    }

    @Test
    fun theHighlightAndShadeKeepTheHueOfTheBall() {
        for ((_, base) in LookPalettes.SETS.getValue(PaletteId.VIVID)) {
            val t = LookPalettes.tone(base)
            // Lighter and darker, but not washed out to white or sunk to black.
            assertTrue(Colors.luminance(t.hi) > Colors.luminance(base))
            assertTrue(Colors.luminance(t.lo) < Colors.luminance(base))
            assertTrue(Colors.luminance(t.hi) < 0.95)
            assertTrue(Colors.luminance(t.lo) > 0.002)
        }
    }

    @Test
    fun theStoreSurvivesAJsonRoundTrip() {
        val store = mapOf("modern" to LookOverrides(Volume.FLAT, RingLevel.MAX, ShapeSet.DIGITS, PaletteId.DISTINCT, BallSize.LARGE, GridStyle.CHECKER, CellShape.ROUND))
        val text = Json.write(LookStore.storeTo(store))
        assertEquals(store, LookStore.storeFrom(Json.parse(text)))
        assertTrue(text.contains("\"contrast\":\"max\""))
    }

    @Test
    fun customThemeSanitisesAndRoundTrips() {
        assertEquals(CustomTheme.DEFAULT, LookStore.customFrom(null))
        val c = LookStore.customFrom(mapOf("dark" to true, "page" to "zzz", "accent" to "#ABC", "balls" to mapOf("red" to "#123456", "green" to 7)))
        assertTrue(c.dark)
        assertEquals(CustomTheme.DEFAULT.page, c.page)
        assertEquals(0xFFAABBCC.toInt(), c.accent)
        assertEquals(0xFF123456.toInt(), c.balls.getValue(BallColor.RED))
        assertEquals(CustomTheme.DEFAULT.balls.getValue(BallColor.GREEN), c.balls.getValue(BallColor.GREEN))
        val dark = CustomTheme.preset(true)
        assertEquals(dark, LookStore.customFrom(Json.parse(Json.write(LookStore.customTo(dark)))))
    }

    @Test
    fun codesSurviveARoundTripAndCarryCustomColoursOnlyForTheCustomTheme() {
        val share = LookShare("custom", LookOverrides(volume = Volume.GLOSSY, grid = GridStyle.BOLD), CustomTheme.preset(true))
        val code = LookCode.encode(share)
        assertTrue(code.startsWith("CL3:"))
        assertEquals(share, LookCode.decode(code))
        val modern = LookCode.decode(LookCode.encode(LookShare("modern", LookOverrides(ring = RingLevel.HIGH), CustomTheme.preset(true))))
        assertNotNull(modern)
        assertNull(modern!!.custom)
    }

    @Test
    fun readsACodeWrittenByTheWebApp() {
        val code = "CL3:eyJ2IjoxLCJ0aGVtZSI6Im1vZGVybiIsImxvb2siOnsidm9sdW1lIjoiZmxhdCIsImdyaWQiOiJ0aGluIiwiY29udHJhc3QiOiJtYXgifX0="
        assertEquals(
            LookShare("modern", LookOverrides(volume = Volume.FLAT, ring = RingLevel.MAX, grid = GridStyle.THIN), null),
            LookCode.decode(code)
        )
    }

    @Test
    fun refusesTextThatIsNotALookCode() {
        assertNull(LookCode.decode(""))
        assertNull(LookCode.decode("hello"))
        assertNull(LookCode.decode("CL3:!!!"))
        val bad = java.util.Base64.getEncoder().encodeToString("{\"theme\":\"nope\"}".toByteArray())
        assertNull(LookCode.decode("CL3:$bad"))
        assertNull(LookCode.decode("CL3:" + "A".repeat(5000)))
    }

    @Test
    fun aHostileCodeIsCleanedBeforeItReachesTheStore() {
        val body = "{\"theme\":\"modern\",\"look\":{\"volume\":\"flat\",\"grid\":\"<script>\"},\"custom\":5}"
        val code = "CL3:" + java.util.Base64.getEncoder().encodeToString(body.toByteArray())
        assertEquals(LookShare("modern", LookOverrides(volume = Volume.FLAT), null), LookCode.decode(code))
    }

    @Test
    fun theLightModernAndClearNightAreSchemesOfModernAndClear() {
        assertEquals("modern", LookSpec.canonicalTheme("light"))
        assertEquals("clear", LookSpec.canonicalTheme("clearNight"))
        assertEquals("day", LookSpec.schemeOfRenamed("light"))
        assertEquals("night", LookSpec.schemeOfRenamed("clearNight"))
        assertNull(LookSpec.schemeOfRenamed("modern"))
        assertFalse("light" in LookSpec.THEME_IDS)
        assertFalse("clearNight" in LookSpec.THEME_IDS)
        // What was chosen on the old looks moves to the new ones, and choices made on those win.
        val store = LookStore.storeFrom(
            mapOf(
                "light" to mapOf("volume" to "flat", "grid" to "dots"),
                "modern" to mapOf("volume" to "glossy"),
                "clearNight" to mapOf("contrast" to "high")
            )
        )
        assertEquals(LookOverrides(volume = Volume.GLOSSY, grid = GridStyle.DOTS), store["modern"])
        assertEquals(LookOverrides(ring = RingLevel.HIGH), store["clear"])
        val body = "{\"theme\":\"clearNight\",\"look\":{\"volume\":\"flat\"}}"
        val code = "CL3:" + java.util.Base64.getEncoder().encodeToString(body.toByteArray())
        assertEquals(LookShare("clear", LookOverrides(volume = Volume.FLAT), null), LookCode.decode(code))
    }
}
