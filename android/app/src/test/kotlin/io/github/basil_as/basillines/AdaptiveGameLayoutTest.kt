package io.github.basil_as.basillines

import org.junit.Assert.*
import org.junit.Test

class AdaptiveGameLayoutTest {
    @Test fun landscapeUsesTheSafeHeightInsteadOfAWidthPercentage() {
        val layout = adaptiveGameLayout(899f, 423f)
        assertTrue(layout.sideBySide)
        assertEquals(407f, layout.boardSize, 0.01f)
        assertTrue(layout.panelWidth >= 176f)
        assertFits(layout, 899f, 423f)
    }

    @Test fun portraitBoardUsesAvailableWidth() {
        val layout = adaptiveGameLayout(320f, 640f)
        assertFalse(layout.sideBySide)
        assertEquals(296f, layout.boardSize, 0.01f)
    }

    @Test fun shortLandscapeStillFillsTheHeight() {
        val layout = adaptiveGameLayout(600f, 280f)
        assertTrue(layout.sideBySide)
        assertEquals(264f, layout.boardSize, 0.01f)
        assertFits(layout, 600f, 280f)
    }

    @Test fun tabletPortraitDoesNotWasteWidthOnAnUnnecessaryRail() {
        val layout = adaptiveGameLayout(840f, 1280f)
        assertFalse(layout.sideBySide)
        assertEquals(816f, layout.boardSize, 0.01f)
    }

    @Test fun tabletLandscapeUsesTheFullAvailableHeight() {
        val layout = adaptiveGameLayout(1280f, 840f)
        assertTrue(layout.sideBySide)
        assertEquals(824f, layout.boardSize, 0.01f)
    }

    @Test fun measuredChromeControlsTheShortPortraitBoard() {
        val layout = adaptiveGameLayout(360f, 480f, headerHeight = 180f, dockHeight = 64f)
        assertFalse(layout.sideBySide)
        assertEquals(188f, layout.boardSize, 0.01f)
    }

    @Test fun largeFontsReserveEnoughPanelWidthAndDoNotOverflow() {
        val normal = adaptiveGameLayout(899f, 423f)
        val large = adaptiveGameLayout(899f, 423f, fontScale = 2f)
        assertTrue(large.panelWidth > normal.panelWidth)
        assertFits(large, 899f, 423f)
    }

    @Test fun geometryFitsAcrossNarrowShortAndLargeWindows() {
        for (width in listOf(0f, 80f, 160f, 280f, 320f, 360f, 411f, 600f, 840f, 1280f)) {
            for (height in listOf(0f, 80f, 160f, 280f, 320f, 400f, 640f, 840f, 1280f)) {
                for (font in listOf(1f, 1.3f, 2f, 3f)) {
                    assertFits(adaptiveGameLayout(width, height, font), width, height)
                }
            }
        }
    }

    @Test fun invalidViewportNeverProducesInvalidConstraints() {
        for (width in listOf(Float.NaN, Float.POSITIVE_INFINITY, -10f)) {
            val layout = adaptiveGameLayout(width, 300f)
            assertEquals(0f, layout.boardSize, 0f)
            assertTrue(layout.padding.isFinite())
        }
    }

    private fun assertFits(layout: AdaptiveGameLayout, width: Float, height: Float) {
        assertTrue(layout.boardSize.isFinite() && layout.boardSize >= 0f)
        assertTrue(layout.panelWidth.isFinite() && layout.panelWidth >= 0f)
        assertTrue(layout.boardSize + 2 * layout.padding <= width + 0.01f)
        assertTrue(layout.boardSize + 2 * layout.padding <= height + 0.01f)
        if (layout.sideBySide) {
            assertTrue(layout.boardSize + layout.panelWidth + layout.gap + 2 * layout.padding <= width + 0.01f)
        }
    }
}
