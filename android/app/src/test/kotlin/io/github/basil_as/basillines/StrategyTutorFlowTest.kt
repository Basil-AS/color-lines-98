package io.github.basil_as.basillines

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import io.github.basil_as.basillines.engine.STRATEGY_LESSONS
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "en-rUS-w411dp-h891dp-port")
class StrategyTutorFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun strategyIsSeparateAndLeavingRestoresSavedGame() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = GameStorage(context)
        val prefs = context.getSharedPreferences("colorlines_prefs", Context.MODE_PRIVATE)
        val before = prefs.getString("game", null)
        val history = storage.history
        rule.clickGameAction("Rules and info")
        rule.onNode(hasText("Strategy coach")).performClick()
        rule.onNode(hasText("Lesson 1 of 6")).assertIsDisplayed()
        rule.onNode(hasText("Show a hint")).performClick()
        rule.onNode(hasText("Leave training")).performClick()
        assertEquals(before, prefs.getString("game", null))
        assertEquals(history, storage.history)
    }

    @Test fun successfulStrategyMoveWaitsForContinueAndExplainsWhy() {
        rule.clickGameAction("Rules and info")
        rule.onNode(hasText("Strategy coach")).performClick()
        // The first puzzle keeps a growing line open at both ends.
        rule.onNodeWithContentDescription("Row 3, column 8,", substring = true).performClick()
        rule.onNodeWithContentDescription("Row 5, column 5,", substring = true).performClick()
        rule.onNode(hasText("Continue")).assertIsDisplayed()
        rule.onNode(hasText("Lesson 1 of 6")).assertIsDisplayed()
        rule.onNode(hasText("Continue")).performClick()
        rule.onNode(hasText("Lesson 2 of 6")).assertIsDisplayed()
    }
    @Test fun solvingEveryLessonExplainsEachMoveAndRestoresTheGame() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("colorlines_prefs", Context.MODE_PRIVATE)
        val before = prefs.getString("game", null)
        val history = GameStorage(context).history
        rule.clickGameAction("Rules and info")
        rule.onNode(hasText("Strategy coach")).performClick()
        STRATEGY_LESSONS.forEachIndexed { index, lesson ->
            rule.onNode(hasText("Lesson ${index + 1} of 6")).assertIsDisplayed()
            rule.onNodeWithContentDescription("Row ${lesson.from.y + 1}, column ${lesson.from.x + 1},", substring = true).performClick()
            rule.onNodeWithContentDescription("Row ${lesson.to.y + 1}, column ${lesson.to.x + 1},", substring = true).performClick()
            rule.onNode(hasText(lesson.text.getValue("en").success)).assertIsDisplayed()
            rule.onNode(hasText(if (index == STRATEGY_LESSONS.lastIndex) "Return to game" else "Continue")).performClick()
        }
        assertEquals(before, prefs.getString("game", null))
        assertEquals(history, GameStorage(context).history)
    }

    @Test fun wrongMoveExplainsTheMistakeAndRestoresThePuzzle() {
        rule.clickGameAction("Rules and info")
        rule.onNode(hasText("Strategy coach")).performClick()
        val lesson = STRATEGY_LESSONS.first()
        rule.onNodeWithContentDescription("Row ${lesson.from.y + 1}, column ${lesson.from.x + 1},", substring = true).performClick()
        rule.onNodeWithContentDescription("Row 1, column 1,", substring = true).performClick()
        rule.onNode(hasText(lesson.text.getValue("en").wrong)).assertIsDisplayed()
        rule.onNodeWithContentDescription("Row ${lesson.from.y + 1}, column ${lesson.from.x + 1}, red ball", substring = true).assertIsDisplayed()
        rule.onNode(hasText("Show a hint")).assertIsDisplayed()
    }

    @Test fun trainingTimeIsNotChargedToTheRestoredGame() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = GameStorage(context)
        val original = storage.loadGame()!!.board.getOccupiedCells().first().first
        fun tapOriginal() = rule.onNodeWithContentDescription(
            "Row ${original.y + 1}, column ${original.x + 1},", substring = true
        ).performClick()
        tapOriginal()
        val before = storage.loadGame()!!.playMs
        rule.clickGameAction("Rules and info")
        rule.onNode(hasText("Strategy coach")).performClick()
        val source = STRATEGY_LESSONS.first().from
        rule.onNodeWithContentDescription("Row ${source.y + 1}, column ${source.x + 1},", substring = true).performClick()
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofSeconds(20))
        rule.onNode(hasText("Leave training")).performClick()
        tapOriginal()
        assertEquals(before, storage.loadGame()!!.playMs)
    }

}
