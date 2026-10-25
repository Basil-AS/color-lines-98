package io.github.basil_as.basillines

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.basil_as.basillines.engine.BallColor
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [26, 29, 35, 36, 37], qualifiers = "en-rUS-w1200dp-h900dp-land")
class AdaptiveGameScreenTest {
    @get:Rule val rule = createComposeRule()

    private fun show(width: Int, height: Int, font: Float = 1f, onSettings: () -> Unit = {}, score: Int = 123) {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, font)) {
                MaterialTheme {
                    Box(Modifier.size(width.dp, height.dp).testTag("viewport")) {
                        ModernGameScreen(
                            paletteFor(AppTheme.MODERN), score, 987,
                            listOf(BallColor.RED, BallColor.GREEN, BallColor.BLUE), true, true,
                            BoardSnapshot(List(81) { null }, null, emptySet(), emptyMap()),
                            {}, {}, {}, {}, onSettings, {}, {},
                            GameExtras(modeLabel = "Classic", hintsLeft = 3, goalsLabel = "Goals 0/3")
                        )
                    }
                }
            }
        }
    }

    @Test fun landscapeKeepsTheBoardSquareAndNearlyFullHeight() {
        show(899, 423)
        val board = rule.onNodeWithTag("adaptive-game-board").getUnclippedBoundsInRoot()
        val viewport = rule.onNodeWithTag("viewport").getUnclippedBoundsInRoot()
        assertEquals((board.right - board.left).value, (board.bottom - board.top).value, 0.1f)
        assertTrue("Landscape board must occupy the short dimension", (board.bottom - board.top) > (viewport.bottom - viewport.top) * .8f)
        assertTrue(board.left >= viewport.left && board.right <= viewport.right)
        assertTrue(board.top >= viewport.top && board.bottom <= viewport.bottom)
        rule.onNodeWithContentDescription("Goals 0/3").assertIsDisplayed()
    }

    @Test fun narrowPortraitUsesFullBoardWidthAndKeepsPrimaryActionsReachable() {
        show(320, 640)
        val board = rule.onNodeWithTag("adaptive-game-board").getUnclippedBoundsInRoot()
        assertEquals(296f, (board.right - board.left).value, .5f)
        val undo = rule.onNodeWithContentDescription("Undo move").assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue((undo.bottom - undo.top) >= 48.dp)
        rule.onNodeWithContentDescription("Hint (3 left)").assertIsDisplayed()
    }

    @Test fun settingsRemainsAvailableThroughTheCompactGameMenu() {
        var opened = false
        show(320, 640, onSettings = { opened = true })
        rule.onNodeWithContentDescription("Game").performClick()
        rule.onNodeWithText("Settings").performClick()
        rule.runOnIdle { assertTrue(opened) }
    }

    @Test fun largeTextInAShortWindowDoesNotPushTheBoardOutsideTheViewport() {
        show(360, 480, 2f)
        val board = rule.onNodeWithTag("adaptive-game-board").getUnclippedBoundsInRoot()
        val viewport = rule.onNodeWithTag("viewport").getUnclippedBoundsInRoot()
        assertTrue((board.right - board.left) > 0.dp && (board.bottom - board.top) > 0.dp)
        assertTrue(board.left >= viewport.left && board.right <= viewport.right)
        assertTrue(board.top >= viewport.top && board.bottom <= viewport.bottom)
        rule.onNodeWithContentDescription("Undo move").assertIsDisplayed()
    }
    @Test fun shortLandscapeKeepsPrimaryActionsVisibleWhileHeaderScrolls() {
        show(899, 280, 3f)
        rule.onNodeWithText("Next").performScrollTo()
        rule.onNodeWithContentDescription("Undo move").assertIsDisplayed()
        rule.onNodeWithContentDescription("Hint (3 left)").assertIsDisplayed()
    }

    @Test fun landscapeShowsTheEntireScoreEvenForLongGames() {
        show(899, 423, score = 12345678)
        rule.onNode(hasText("12345678") and hasAnyAncestor(hasTestTag("adaptive-game-panel")), useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { get ->
                val layouts = mutableListOf<TextLayoutResult>()
                assertTrue(get(layouts))
                assertFalse("Score must not overflow: " + layouts.joinToString { "size=${it.size}, width=${it.didOverflowWidth}, height=${it.didOverflowHeight}, lines=${it.lineCount}, input=${it.layoutInput}" }, layouts.any { it.hasVisualOverflow })
            }
    }

    @Test fun landscapeMenuKeepsEverySecondaryActionVisible() {
        show(899, 280, 3f)
        rule.onNodeWithContentDescription("Game").assertIsDisplayed().performClick()
        rule.onNodeWithText("Settings").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Rules and info").performScrollTo().assertIsDisplayed()
    }

}
