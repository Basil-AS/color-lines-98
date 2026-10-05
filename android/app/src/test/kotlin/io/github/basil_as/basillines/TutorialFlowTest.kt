package io.github.basil_as.basillines

import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "en-rUS-w411dp-h891dp-port")
class TutorialFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun descriptionMatches(regex: Regex) = SemanticsMatcher("description matches $regex") { node ->
        node.config.getOrNull(SemanticsProperties.ContentDescription)?.any { regex.matches(it) } == true
    }

    private val ballCell = descriptionMatches(Regex("Row \\d+, column \\d+, [a-z]+ ball.*"))
    private val emptyCell = descriptionMatches(Regex("Row \\d+, column \\d+, empty.*"))
    private val reachable = descriptionMatches(Regex("Row \\d+, column \\d+, empty, reachable.*"))

    private fun ballCells() = rule.onAllNodes(ballCell)
    private fun emptyCells() = rule.onAllNodes(emptyCell)
    private fun ballCount() = ballCells().fetchSemanticsNodes().size

    private fun tapCell(x: Int, y: Int) {
        rule.onNodeWithContentDescription("Row ${y + 1}, column ${x + 1},", substring = true).performClick()
        rule.waitForIdle()
    }

    private fun advanceClock(ms: Long = 1500L) {
        rule.mainClock.advanceTimeBy(ms)
        ShadowLooper.idleMainLooper(ms, TimeUnit.MILLISECONDS)
        rule.waitForIdle()
    }

    private fun openHelp() {
        rule.onNodeWithContentDescription("Rules and info").performClick()
        rule.waitForIdle()
    }

    private fun startTutorialFromHelp() {
        openHelp()
        rule.onNode(hasText("Take the tutorial")).performClick()
        rule.waitForIdle()
    }

    @Test
    fun openingHelpAndTappingTakeTheTutorialShowsBannerTextAndStep1() {
        startTutorialFromHelp()
        rule.onNode(hasText("Tap a ball to pick it up", substring = true)).assertIsDisplayed()
        rule.onNode(hasText("Step 1 of 7", substring = true)).assertIsDisplayed()
    }

    @Test
    fun skipTheTutorialRestoresOriginalGameStateAndLeavesStorageUntouched() {
        // Play one move first so score/moves > 0
        ballCells().onFirst().performClick()
        rule.onAllNodes(reachable).onFirst().performClick()
        rule.waitForIdle()

        val ballsBefore = ballCount()
        val prefs = context.getSharedPreferences("colorlines_prefs", Context.MODE_PRIVATE)
        val rawSavedBefore = prefs.getString("game", null)
        val storage = GameStorage(context)
        val savedBefore = storage.loadGame()
        val historyBefore = storage.history
        val bestBefore = storage.progress.bestScore

        startTutorialFromHelp()
        rule.onNode(hasText("Tap a ball to pick it up", substring = true)).assertIsDisplayed()

        // Skipping removes the banner and restores previous game
        rule.onNode(hasText("Skip the tutorial")).performClick()
        rule.waitForIdle()

        assertEquals(0, rule.onAllNodes(hasText("Tap a ball to pick it up", substring = true)).fetchSemanticsNodes().size)
        assertEquals(0, rule.onAllNodes(hasText("Skip the tutorial")).fetchSemanticsNodes().size)

        assertEquals(ballsBefore, ballCount())
        val rawSavedAfter = prefs.getString("game", null)
        assertEquals(rawSavedBefore, rawSavedAfter)
        val savedAfter = storage.loadGame()
        assertEquals(savedBefore?.score, savedAfter?.score)
        assertEquals(savedBefore?.moves, savedAfter?.moves)
        assertEquals(historyBefore, storage.history)
        assertEquals(bestBefore, storage.progress.bestScore)
    }

    @Test
    fun drivingScriptedSolutionAdvancesAllStepsAndReturnsToOriginalGame() {
        val ballsBefore = ballCount()

        startTutorialFromHelp()
        rule.onNode(hasText("Step 1 of 7", substring = true)).assertIsDisplayed()

        // Step 1 -> 2: pick red ball at (2, 4)
        tapCell(2, 4)
        rule.onNode(hasText("Now tap an empty cell", substring = true)).assertIsDisplayed()
        rule.onNode(hasText("Step 2 of 7", substring = true)).assertIsDisplayed()

        // Step 2 -> 3: move it to (2, 2)
        tapCell(2, 2)
        advanceClock(1500L)
        rule.onNode(hasText("The three colours in the Next panel", substring = true)).assertIsDisplayed()
        rule.onNode(hasText("Step 3 of 7", substring = true)).assertIsDisplayed()
        rule.onNode(hasText("Continue")).assertIsDisplayed()

        // Step 3 -> 4: Continue
        rule.onNode(hasText("Continue")).performClick()
        rule.waitForIdle()
        rule.onNode(hasText("Line up five balls", substring = true)).assertIsDisplayed()
        rule.onNode(hasText("Step 4 of 7", substring = true)).assertIsDisplayed()

        // Step 4 -> 5: pick (7, 2), move to (5, 5) -> line cleared, free turn
        tapCell(7, 2)
        tapCell(5, 5)
        advanceClock(1500L)
        rule.onNode(hasText("free turn", substring = true)).assertIsDisplayed()
        rule.onNode(hasText("Step 5 of 7", substring = true)).assertIsDisplayed()

        // Step 5 -> 6: any ball move (move green ball from 0,0 to 0,1)
        tapCell(0, 0)
        tapCell(0, 1)
        advanceClock(1500L)
        rule.onNode(hasText("cannot pass", substring = true)).assertIsDisplayed()
        rule.onNode(hasText("Step 6 of 7", substring = true)).assertIsDisplayed()

        // Step 6 -> 7: pick boxed red ball (4, 4), tap (0, 0)
        tapCell(4, 4)
        tapCell(0, 0)
        advanceClock(1500L)
        rule.onNode(hasText("The game ends when the board is full", substring = true)).assertIsDisplayed()
        rule.onNode(hasText("Step 7 of 7", substring = true)).assertIsDisplayed()
        rule.onNode(hasText("Start playing")).assertIsDisplayed()

        // Step 7 -> end: Start playing returns to original game
        rule.onNode(hasText("Start playing")).performClick()
        rule.waitForIdle()
        assertEquals(0, rule.onAllNodes(hasText("Start playing")).fetchSemanticsNodes().size)
        assertEquals(0, rule.onAllNodes(hasText("Training")).fetchSemanticsNodes().size)
        assertEquals(ballsBefore, ballCount())
    }

    @Test
    fun wrongMoveInLineStepShowsRetryTextAndRestoresLineBoard() {
        startTutorialFromHelp()

        // Advance to Line step (step 4)
        tapCell(2, 4)
        tapCell(2, 2)
        advanceClock(1500L)
        rule.onNode(hasText("Continue")).performClick()
        rule.waitForIdle()
        rule.onNode(hasText("Line up five balls", substring = true)).assertIsDisplayed()

        // Wrong move: move green ball at (0, 0) to (0, 1) instead of making line
        tapCell(0, 0)
        tapCell(0, 1)
        advanceClock(1500L)

        // Retry text is displayed
        rule.onNode(hasText("Not quite", substring = true)).assertIsDisplayed()

        // Line board is restored: red ball still at (7, 2)
        rule.onNodeWithContentDescription("Row 3, column 8, red ball", substring = true).assertIsDisplayed()
        // and green ball is still at (0, 0)
        rule.onNodeWithContentDescription("Row 1, column 1, green ball", substring = true).assertIsDisplayed()
    }

    @Test
    fun hintButtonDoesNothingDuringTutorial() {
        // Before tutorial: hint button shows 3 hints left
        rule.onNode(hasContentDescription("Hint (3 left)")).assertIsDisplayed()

        startTutorialFromHelp()
        rule.onNode(hasText("Tap a ball to pick it up", substring = true)).assertIsDisplayed()

        // During tutorial, the hint button shows 0 left and tapping it does nothing
        val hintNode = rule.onNode(hasContentDescription("Hint (0 left)"))
        hintNode.assertIsDisplayed().performClick()
        rule.waitForIdle()

        // Tutorial stays in place
        rule.onNode(hasText("Tap a ball to pick it up", substring = true)).assertIsDisplayed()

        // Exiting tutorial restores original hints
        rule.onNode(hasText("Skip the tutorial")).performClick()
        rule.waitForIdle()
        rule.onNode(hasContentDescription("Hint (3 left)")).assertIsDisplayed()
    }

    @Test
    fun dosThemeAfterFinishedGameShowsResultActionThatOpensGameOverDialog() {
        val cells = buildString {
            for (y in 0 until 9) for (x in 0 until 9) {
                val empty = (x == 0 && y == 0) || (x == 1 && y == 0) || (x == 8 && y == 8)
                append(if (empty) 0 else ((x + 2 * y) % 7) + 1)
            }
        }
        val game = listOf("v3", 60, 5, 1, 5, 0, 5, 1000, "1,1,2", "0:0,8:8,1:0", cells).joinToString("|")
        val prefs = context.getSharedPreferences("colorlines_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().putString("theme", AppTheme.COLORLINES_92.name).putString("player_name", "Ann").putString("game", game).commit()
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()

        rule.onNodeWithContentDescription("Row 1, column 3, blue ball", substring = true).performClick()
        rule.onNodeWithContentDescription("Row 1, column 2, empty", substring = true).performClick()
        rule.waitForIdle()

        // The DOS theme after finished game offers "Result" action (content description starts with "Result")
        val resultAction = rule.onNode(
            SemanticsMatcher("description starts with Result") { node ->
                node.config.getOrNull(SemanticsProperties.ContentDescription)?.any { it.startsWith("Result") } == true
            }
        )
        resultAction.assertIsDisplayed().performClick()
        rule.waitForIdle()

        // Opening Result displays the game over dialog
        rule.onAllNodes(hasText("Game over")).onFirst().assertIsDisplayed()
    }
}
