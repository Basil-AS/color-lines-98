package com.colorlines.app

import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.hasText
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "en-rUS-w411dp-h891dp-port")
class MainScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun descriptionMatches(regex: Regex) = SemanticsMatcher("description matches $regex") { node ->
        node.config.getOrNull(SemanticsProperties.ContentDescription)?.any { regex.matches(it) } == true
    }

    // "Row 1, column 5, green ball" / "Row 2, column 3, empty, reachable"; the "Next balls" label is not a cell.
    private val ballCell = descriptionMatches(Regex("Row \\d+, column \\d+, [a-z]+ ball.*"))
    private val emptyCell = descriptionMatches(Regex("Row \\d+, column \\d+, empty.*"))
    private val reachable = descriptionMatches(Regex("Row \\d+, column \\d+, empty, reachable"))

    private fun ballCells() = rule.onAllNodes(ballCell)
    private fun emptyCells() = rule.onAllNodes(emptyCell)
    private fun ballCount() = ballCells().fetchSemanticsNodes().size

    private fun playOneMove() {
        ballCells().onFirst().performClick()
        rule.onAllNodes(reachable).onFirst().performClick()
        rule.waitForIdle()
    }

    @Test
    fun showsTitleAndAFullBoard() {
        rule.onNodeWithContentDescription("Game board").assertIsDisplayed()
        rule.onAllNodes(hasText("Basil Lines")).onFirst().assertIsDisplayed()
        assertEquals(81, ballCount() + emptyCells().fetchSemanticsNodes().size)
        assertEquals(5, ballCount())
    }

    @Test
    fun selectingABallMarksItSelectedAndShowsReachableCells() {
        ballCells().onFirst().performClick()
        rule.waitForIdle()
        rule.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true)).onFirst().assertIsDisplayed()
        assertTrue(rule.onAllNodes(reachable).fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun aMoveSpawnsBallsAndNewGameReplacesTheWholeBoard() {
        assertEquals(5, ballCount())
        playOneMove()
        // Regression: after "New game" the previous board used to stay on screen.
        val afterMove = ballCount()
        assertTrue("a move adds three balls unless it clears a line", afterMove in 3..8)

        rule.onNodeWithContentDescription("New game").performClick()
        rule.waitForIdle()
        assertEquals(5, ballCount())
        rule.onNodeWithContentDescription("Undo move").assertIsDisplayed()
    }

    @Test
    fun undoRestoresTheBoardBeforeTheMove() {
        playOneMove()
        rule.onNodeWithContentDescription("Undo move").performClick()
        rule.waitForIdle()
        assertEquals(5, ballCount())
    }

    @Test
    fun gameSurvivesActivityRecreation() {
        playOneMove()
        val before = ballCount()
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()
        assertEquals(before, ballCount())
    }

    @Test
    fun statsAndHelpDialogsOpenAndClose() {
        rule.onNodeWithContentDescription("Statistics").performClick()
        rule.onAllNodes(hasText("No games yet", substring = true)).onFirst().assertIsDisplayed()
        rule.onAllNodes(hasText("Close")).onFirst().performClick()

        rule.onNodeWithContentDescription("Rules and info").performClick()
        rule.onAllNodes(hasText("Rules & scoring")).onFirst().assertIsDisplayed()
    }

    @Test
    fun abandoningAGameWithMovesLandsInTheHistory() {
        playOneMove()
        rule.onNodeWithContentDescription("New game").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Statistics").performClick()
        rule.onAllNodes(hasText("unfinished", ignoreCase = true)).onFirst().assertIsDisplayed()
    }

    @Test
    fun soundToggleChangesItsLabel() {
        rule.onNodeWithContentDescription("Mute sound").performClick()
        rule.onNodeWithContentDescription("Unmute sound").assertIsDisplayed()
    }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ru-rRU-w411dp-h891dp-port")
class RussianScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun usesTheSystemLanguage() {
        rule.onNodeWithContentDescription("Игровое поле").assertIsDisplayed()
        rule.onNodeWithContentDescription("Новая игра").assertIsDisplayed()
        rule.onNodeWithContentDescription("Статистика").performClick()
        rule.onAllNodes(hasText("Пока нет партий", substring = true)).onFirst().assertIsDisplayed()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LandscapeAndSmallScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    @Config(qualifiers = "en-rUS-w891dp-h411dp-land")
    fun landscapeShowsTheBoardAndControls() {
        rule.onNodeWithContentDescription("Game board").assertIsDisplayed()
        rule.onNodeWithContentDescription("New game").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "en-rUS-w320dp-h568dp-port")
    fun smallPhoneShowsTheBoard() {
        rule.onNodeWithContentDescription("Game board").assertIsDisplayed()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GameStorageTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before
    fun clear() {
        context.getSharedPreferences("colorlines_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun prefs() = context.getSharedPreferences("colorlines_prefs", Context.MODE_PRIVATE)

    @Test
    fun defaultsWhenNothingIsStored() {
        val s = GameStorage(context)
        assertEquals(AppTheme.MODERN, s.theme)
        assertEquals(0, s.bestScore)
        assertTrue(s.history.isEmpty())
        assertEquals(null, s.loadGame())
    }

    @Test
    fun corruptValuesFallBackToDefaults() {
        prefs().edit().putString("theme", "NEON").putString("game", "garbage").putString("history", "x;y").commit()
        val s = GameStorage(context)
        assertEquals(AppTheme.MODERN, s.theme)
        assertEquals(null, s.loadGame())
        assertTrue(s.history.isEmpty())
    }

    @Test
    fun negativeBestScoreIsIgnored() {
        prefs().edit().putInt("best_score", -50).commit()
        assertEquals(0, GameStorage(context).bestScore)
    }

    @Test
    fun valuesRoundTrip() {
        val s = GameStorage(context)
        s.theme = AppTheme.CLASSIC_98
        s.bestScore = 120
        val engine = com.colorlines.engine.GameEngine()
        s.saveGame(engine)
        val again = GameStorage(context)
        assertEquals(AppTheme.CLASSIC_98, again.theme)
        assertEquals(120, again.bestScore)
        assertEquals(engine.score, again.loadGame()!!.score)
    }
}
