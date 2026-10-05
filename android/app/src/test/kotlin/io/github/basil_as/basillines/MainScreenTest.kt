package io.github.basil_as.basillines

import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    private val reachable = descriptionMatches(Regex("Row \\d+, column \\d+, empty, reachable.*"))

    private val incoming = descriptionMatches(Regex("Row \\d+, column \\d+, empty.*will appear here"))

    private fun ballCells() = rule.onAllNodes(ballCell)
    private fun emptyCells() = rule.onAllNodes(emptyCell)
    private fun ballCount() = ballCells().fetchSemanticsNodes().size

    /** "New game" now opens a dialog; this presses its Start button. */
    private fun confirmNewGame() {
        rule.onNode(hasText("Start", substring = true)).performClick()
        rule.waitForIdle()
    }

    private fun playOneMove() {
        ballCells().onFirst().performClick()
        rule.onAllNodes(reachable).onFirst().performClick()
        rule.waitForIdle()
    }

    @Test
    fun showsScoreAndAFullBoard() {
        rule.onNodeWithContentDescription("Game board").assertIsDisplayed()
        // Portrait keeps the HUD compact: no title, the score is what must be visible.
        rule.onAllNodes(hasText("Score")).onFirst().assertIsDisplayed()
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
        confirmNewGame()
        rule.waitForIdle()
        assertEquals(5, ballCount())
        rule.onNodeWithContentDescription("Undo move").assertIsDisplayed()
    }

    @Test
    fun theNewGameDialogCanAlwaysBeCancelled() {
        // Without a game in progress the dialog used to offer only "Start": there must be a way back.
        rule.onNodeWithContentDescription("New game").performClick()
        rule.onNode(hasText("Cancel")).assertIsDisplayed().performClick()
        rule.waitForIdle()
        assertEquals(0, rule.onAllNodes(hasText("Start", substring = true)).fetchSemanticsNodes().size)
        assertEquals(5, ballCount())

        // With a move made the safe button keeps the game and the other one says what it does.
        playOneMove()
        val before = ballCount()
        rule.onNodeWithContentDescription("New game").performClick()
        rule.onNode(hasText("Keep playing")).performClick()
        rule.waitForIdle()
        assertEquals(before, ballCount())
    }

    @Test
    fun theMindTabExplainsWhatItNeedsAndTheEffectsCanBeSwitched() {
        rule.onNodeWithContentDescription("Statistics").performClick()
        rule.onNode(hasText("Mind")).performClick()
        rule.onAllNodes(hasText("more games are needed", substring = true)).onFirst().assertExists()
        rule.onAllNodes(hasText("Close")).onFirst().performClick()

        rule.onNodeWithContentDescription("Settings").performClick()
        rule.onNode(hasText("Calm")).performScrollTo().performClick()
        rule.waitForIdle()
        val prefs = ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("colorlines_prefs", Context.MODE_PRIVATE)
        assertEquals("calm", prefs.getString("effects", null))
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
        confirmNewGame()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Statistics").performClick()
        rule.onAllNodes(hasText("unfinished", ignoreCase = true)).onFirst().assertExists()
    }

    @Test
    fun announcesThreeCellsWhereTheNextBallsWillAppear() {
        assertEquals(3, rule.onAllNodes(incoming).fetchSemanticsNodes().size)
        playOneMove()
        assertEquals(3, rule.onAllNodes(incoming).fetchSemanticsNodes().size)
    }

    @Test
    fun thePreviewCanBeSwitchedOffAndIsRemembered() {
        rule.onNodeWithContentDescription("Settings").performClick()
        rule.onNode(hasText("Show where new balls will appear")).performClick()
        rule.waitForIdle()
        rule.onAllNodes(hasText("Close")).onFirst().performClick()
        rule.waitForIdle()
        assertEquals(0, rule.onAllNodes(incoming).fetchSemanticsNodes().size)
        assertEquals(false, GameStorage(ApplicationProvider.getApplicationContext()).spawnPreview)
    }

    @Test
    fun theWindowsLookShowsTheMarkersAndTheDosLookDoesNot() {
        val prefs = ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("colorlines_prefs", Context.MODE_PRIVATE)
        for ((theme, expected) in listOf(AppTheme.LINES_98 to 3, AppTheme.COLORLINES_92 to 0, AppTheme.MODERN to 3)) {
            prefs.edit().clear().putString("theme", theme.name).commit()
            rule.activityRule.scenario.recreate()
            rule.waitForIdle()
            assertEquals("$theme", expected, rule.onAllNodes(incoming).fetchSemanticsNodes().size)
        }
    }

    @Test
    fun theLanguageCanBeSwitchedInsideTheGame() {
        rule.onNodeWithContentDescription("Settings").performClick()
        rule.onNode(hasText("Русский")).performScrollTo().performClick()
        rule.waitForIdle()
        assertEquals(AppLanguage.RU, GameStorage(ApplicationProvider.getApplicationContext()).language)
        // The activity is recreated with the chosen language, whatever the system uses.
        assertTrue(rule.onAllNodes(hasText("Счёт")).fetchSemanticsNodes().isNotEmpty())
        GameStorage(ApplicationProvider.getApplicationContext()).language = AppLanguage.AUTO
    }

    @Test
    fun newGameWarnsAboutAGameInProgressAndKeepPlayingChangesNothing() {
        playOneMove()
        val before = ballCount()
        rule.onNodeWithContentDescription("New game").performClick()
        rule.waitForIdle()
        rule.onAllNodes(hasText("counted as unfinished", substring = true)).onFirst().assertIsDisplayed()
        rule.onNode(hasText("Keep playing")).performClick()
        rule.waitForIdle()
        assertEquals(before, ballCount())
        assertTrue(rule.onAllNodes(hasText("counted as unfinished", substring = true)).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun modesShowTheirColoursClockAndWhatASwitchDoes() {
        playOneMove()
        rule.onNodeWithContentDescription("New game").performClick()
        rule.waitForIdle()
        rule.onNode(hasText("Change mode")).performClick()
        rule.waitForIdle()
        rule.onAllNodes(hasText("7 colours, no time limit", substring = true)).onFirst().assertExists()
        rule.onNode(hasText("5 colours", substring = true)).assertExists()
        rule.onNode(hasText("3 min", substring = true)).assertExists()
        rule.onNode(hasText("Blitz")).performClick()
        rule.waitForIdle()
        rule.onNode(hasText("switching from Classic to Blitz", substring = true)).assertIsDisplayed()
        rule.onNode(hasText("Start: Blitz")).assertExists()
        rule.onAllNodes(hasText("counted as unfinished", substring = true)).onFirst().assertExists()
    }

    @Test
    fun newGameDialogWithAGameInProgressFoldsTheModeListUntilChangeModeIsTapped() {
        playOneMove()
        rule.onNodeWithContentDescription("New game").performClick()
        rule.waitForIdle()
        rule.onNode(hasText("Change mode")).assertIsDisplayed()
        rule.onNode(hasText("Blitz")).assertDoesNotExist()
        rule.onNode(hasText("5 colours", substring = true)).assertDoesNotExist()
        rule.onNode(hasText("3 min", substring = true)).assertDoesNotExist()
        assertTrue(rule.onAllNodes(hasText("7 colours, no time limit", substring = true)).fetchSemanticsNodes().isEmpty())

        rule.onNode(hasText("Change mode")).performClick()
        rule.waitForIdle()
        rule.onNode(hasText("Blitz")).assertIsDisplayed()
        rule.onNode(hasText("5 colours", substring = true)).assertExists()
        rule.onNode(hasText("3 min", substring = true)).assertExists()
        rule.onAllNodes(hasText("7 colours, no time limit", substring = true)).onFirst().assertExists()
    }

    @Test
    fun theHelpExplainsTheModes() {
        rule.onNodeWithContentDescription("Rules and info").performClick()
        rule.waitForIdle()
        rule.onNode(hasText("Game modes")).performScrollTo().assertExists()
        rule.onNode(hasText("Easy: only 5 colours", substring = true)).performScrollTo().assertExists()
        rule.onNode(hasText("Daily challenge: everyone gets the same balls", substring = true)).performScrollTo().assertExists()
    }

    @Test
    fun theStatisticsHaveFiveTabsWithLongTermContent() {
        rule.onNodeWithContentDescription("Statistics").performClick()
        rule.waitForIdle()
        for (tab in listOf("Overview", "Career", "Seasons", "Records", "Data")) rule.onNode(hasText(tab)).assertExists()
        rule.onNode(hasText("Career")).performClick()
        rule.onNode(hasText("Next milestones")).performScrollTo().assertExists()
        rule.onNode(hasText("Seasons")).performClick()
        rule.onNode(hasText("Your typical month", substring = true)).performScrollTo().assertExists()
        rule.onNode(hasText("Records")).performClick()
        rule.onNode(hasText("Play a few games", substring = true)).assertExists()
        rule.onNode(hasText("Data")).performClick()
        rule.onNode(hasText("Save backup (JSON)")).assertExists()
        rule.onNode(hasText("Load a backup from a file")).assertExists()
    }

    @Test
    fun todaysGoalsAreShownWithProgress() {
        rule.onNode(hasText("Goals 0/3")).assertExists().performClick()
        rule.waitForIdle()
        rule.onNode(hasText("Today\'s goals".replace("\\", ""))).assertExists()
        rule.onNode(hasText("Goals are built from your own recent results", substring = true)).assertExists()
        rule.onNode(hasText("+25 XP for each goal")).assertExists()
    }

    @Test
    fun theDrawnF1ToF4KeysOfTheDosLookAreTappable() {
        GameStorage(ApplicationProvider.getApplicationContext()).theme = AppTheme.COLORLINES_92
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()
        // F1 opens the original Help window, a second tap closes it again.
        rule.onNodeWithContentDescription("F1: Help").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Close").assertExists()
        rule.onNodeWithContentDescription("Close").performClick()
        rule.waitForIdle()
        // F2 switches the sound, F3 hides the next balls.
        val sound = GameStorage(ApplicationProvider.getApplicationContext())
        rule.onNodeWithContentDescription("F2: Sound").performClick()
        rule.onNodeWithContentDescription("F3: Show next balls").performClick()
        // F4 asks before restarting.
        rule.onNodeWithContentDescription("F4: Restart").performClick()
        rule.waitForIdle()
        rule.onNode(hasText("Start", substring = true)).assertExists()
        assertTrue(sound.showNext.not())
    }

    @Test
    fun settingsShowTheVersionAndTheUpdateControls() {
        rule.onNodeWithContentDescription("Settings").performClick()
        rule.waitForIdle()
        rule.onNode(hasText("Version", substring = true)).performScrollTo().assertExists()
        rule.onNode(hasText("Check for updates automatically")).performScrollTo().assertExists()
        rule.onNode(hasText("Check for updates")).performScrollTo().performClick()
        rule.waitForIdle()
        // Offline in tests: the answer is the friendly failure, not a crash.
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("Could not check for updates", substring = true)).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun theAutomaticCheckIsOnByDefaultAndCanBeSwitchedOff() {
        assertTrue(GameStorage(ApplicationProvider.getApplicationContext()).autoUpdateCheck)
        rule.onNodeWithContentDescription("Settings").performClick()
        rule.onNode(hasText("Check for updates automatically")).performScrollTo().performClick()
        rule.waitForIdle()
        assertTrue(!GameStorage(ApplicationProvider.getApplicationContext()).autoUpdateCheck)
        GameStorage(ApplicationProvider.getApplicationContext()).autoUpdateCheck = true
    }

    @Test
    fun aFreshGameHasNoWarning() {
        rule.onNodeWithContentDescription("New game").performClick()
        rule.waitForIdle()
        assertTrue(rule.onAllNodes(hasText("counted as unfinished", substring = true)).fetchSemanticsNodes().isEmpty())
        rule.onNode(hasText("Start", substring = true)).assertIsDisplayed()
    }

    @Test
    fun theModeChosenIsUsedAndRemembered() {
        rule.onNodeWithContentDescription("New game").performClick()
        rule.waitForIdle()
        rule.onNode(hasText("Change mode")).performClick()
        rule.waitForIdle()
        rule.onNode(hasText("Blitz")).performClick()
        confirmNewGame()
        assertEquals(io.github.basil_as.basillines.engine.ModeId.BLITZ, GameStorage(ApplicationProvider.getApplicationContext()).mode)
        // A timed mode shows its clock.
        rule.onAllNodes(hasText("3:00")).onFirst().assertIsDisplayed()
    }

    @Test
    fun aHintSelectsABallAndCostsOne() {
        rule.onNodeWithContentDescription("Hint (3 left)").performClick()
        rule.waitForIdle()
        assertTrue(rule.onAllNodes(reachable).fetchSemanticsNodes().isNotEmpty())
        rule.onNodeWithContentDescription("Hint (2 left)").assertIsDisplayed()
    }

    @Test
    fun switchingTheThemeIsRemembered() {
        rule.onNodeWithContentDescription("Settings").performClick()
        // The themes are folded behind one row until it is tapped.
        assertEquals(0, rule.onAllNodes(hasText("Modern light")).fetchSemanticsNodes().size)
        rule.onNode(hasText("Change")).performScrollTo().performClick()
        rule.onNode(hasText("Modern light")).performClick()
        rule.waitForIdle()
        assertEquals(AppTheme.LIGHT, GameStorage(ApplicationProvider.getApplicationContext()).theme)
        rule.onNode(hasText("Change")).performScrollTo().performClick()
        rule.onNode(hasText("Lines 98 (Windows)")).performClick()
        rule.waitForIdle()
        assertEquals(AppTheme.LINES_98, GameStorage(ApplicationProvider.getApplicationContext()).theme)
        // The Windows theme has a title bar and a text menu, like the original.
        rule.onAllNodes(hasText("Close")).onFirst().performClick()
        rule.waitForIdle()
        assertTrue(rule.onAllNodes(hasText("Color Lines")).fetchSemanticsNodes().isNotEmpty())
        rule.onNode(hasText("New game")).assertExists()
        rule.onNode(hasText("Statistics")).assertExists()
        // And every theme still plays.
        rule.onNodeWithContentDescription("Game board").assertIsDisplayed()
    }

    @Test
    fun everyThemeShowsTheBoardAndCanBePlayed() {
        for (theme in AppTheme.entries) {
            GameStorage(ApplicationProvider.getApplicationContext()).theme = theme
            rule.activityRule.scenario.recreate()
            rule.waitForIdle()
            if (theme != AppTheme.COLORLINES_92) rule.onNodeWithContentDescription("Game board").assertIsDisplayed()
            when (theme) {
                AppTheme.COLORLINES_92 -> rule.onNodeWithContentDescription("F4: Restart").performClick()
                // Lines 98 has a text menu like the Windows original instead of icon buttons.
                AppTheme.LINES_98 -> rule.onNode(hasText("New game")).performClick()
                else -> rule.onNodeWithContentDescription("New game").performClick()
            }
            rule.waitForIdle()
            confirmNewGame()
            assertEquals(5, ballCount())
            playOneMove()
            assertTrue("$theme keeps a valid board", ballCount() in 3..8)
        }
    }

    @Test
    fun statisticsStartAtLevelOneWithAllAchievementsLocked() {
        rule.onNodeWithContentDescription("Statistics").performClick()
        rule.onAllNodes(hasText("Level 1")).onFirst().assertIsDisplayed()
        assertTrue(rule.onAllNodes(hasText("locked")).fetchSemanticsNodes().size == 23)
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
        prefs().edit().putString("theme", "GLITTER").putString("game", "garbage").putString("history", "x;y").commit()
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
        s.theme = AppTheme.LINES_98
        s.bestScore = 120
        val engine = io.github.basil_as.basillines.engine.GameEngine()
        s.saveGame(engine)
        val again = GameStorage(context)
        assertEquals(AppTheme.LINES_98, again.theme)
        assertEquals(120, again.bestScore)
        assertEquals(engine.score, again.loadGame()!!.score)
    }
}


@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "en-rUS-w411dp-h891dp-port")
class GameOverFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    /** A board with exactly three empty cells, so one move fills it and ends the game. */
    private fun almostFullGame(): String {
        val cells = buildString {
            for (y in 0 until 9) for (x in 0 until 9) {
                val empty = (x == 0 && y == 0) || (x == 1 && y == 0) || (x == 8 && y == 8)
                append(if (empty) 0 else ((x + 2 * y) % 7) + 1)
            }
        }
        return listOf("v3", 0, 0, 0, 0, 0, 0, 0, "1,1,2", "0:0,8:8,1:0", cells).joinToString("|")
    }

    @Test
    fun finishingAGameShowsRewardsAndRecordsTheResult() {
        val prefs = context.getSharedPreferences("colorlines_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().putString("game", almostFullGame()).commit()
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()

        rule.onNodeWithContentDescription("Row 1, column 3, blue ball", substring = true).performClick()
        rule.onNodeWithContentDescription("Row 1, column 2, empty", substring = true).performClick()
        rule.waitForIdle()

        rule.onAllNodes(hasText("Result", substring = true)).onFirst().performClick()
        rule.waitForIdle()

        rule.onAllNodes(hasText("Game over")).onFirst().assertIsDisplayed()
        rule.onAllNodes(hasText("First steps")).onFirst().assertIsDisplayed()
        val storage = GameStorage(context)
        assertEquals(1, storage.history.size)
        assertEquals(1, storage.progress.totalGames)
        assertTrue("first_game" in storage.progress.achievements)

        rule.onAllNodes(hasText("Play again")).onFirst().performClick()
        rule.waitForIdle()
        // A finished game is recorded once; the next game does not add an "abandoned" entry.
        assertEquals(1, GameStorage(context).history.size)
    }

    @Test
    fun gameOverShowsNoDialogByItself() {
        val prefs = context.getSharedPreferences("colorlines_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().putString("game", almostFullGame()).commit()
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()

        rule.onNodeWithContentDescription("Row 1, column 3, blue ball", substring = true).performClick()
        rule.onNodeWithContentDescription("Row 1, column 2, empty", substring = true).performClick()
        rule.waitForIdle()

        assertTrue(rule.onAllNodes(hasText("Game over")).fetchSemanticsNodes().isEmpty())
        rule.onAllNodes(hasText("New game")).onFirst().assertIsDisplayed()
        rule.onAllNodes(hasText("Result", substring = true)).onFirst().assertIsDisplayed()
    }

    @Test
    fun newGameAfterGameOverStartsFreshGameOfSameModeWithNoDialog() {
        val prefs = context.getSharedPreferences("colorlines_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().putString("game", almostFullGame()).commit()
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()

        rule.onNodeWithContentDescription("Row 1, column 3, blue ball", substring = true).performClick()
        rule.onNodeWithContentDescription("Row 1, column 2, empty", substring = true).performClick()
        rule.waitForIdle()

        rule.onAllNodes(hasText("New game")).onFirst().performClick()
        rule.waitForIdle()

        assertTrue(rule.onAllNodes(hasText("Game over")).fetchSemanticsNodes().isEmpty())
        assertTrue(rule.onAllNodes(hasText("Change mode")).fetchSemanticsNodes().isEmpty())
        assertTrue(rule.onAllNodes(hasText("Result", substring = true)).fetchSemanticsNodes().isEmpty())
        val storage = GameStorage(context)
        assertEquals(io.github.basil_as.basillines.engine.ModeId.CLASSIC, storage.mode)
        assertEquals(1, storage.history.size)
    }

    @Test
    fun resultDialogCanBeDismissedWithCloseAndDoesNotReappearUntilResultIsTappedAgain() {
        val prefs = context.getSharedPreferences("colorlines_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().putString("game", almostFullGame()).commit()
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()

        rule.onNodeWithContentDescription("Row 1, column 3, blue ball", substring = true).performClick()
        rule.onNodeWithContentDescription("Row 1, column 2, empty", substring = true).performClick()
        rule.waitForIdle()

        rule.onAllNodes(hasText("Result", substring = true)).onFirst().performClick()
        rule.waitForIdle()
        rule.onAllNodes(hasText("Game over")).onFirst().assertIsDisplayed()

        rule.onNode(hasText("Close")).performClick()
        rule.waitForIdle()
        assertTrue(rule.onAllNodes(hasText("Game over")).fetchSemanticsNodes().isEmpty())

        rule.waitForIdle()
        assertTrue(rule.onAllNodes(hasText("Game over")).fetchSemanticsNodes().isEmpty())

        rule.onAllNodes(hasText("Result", substring = true)).onFirst().performClick()
        rule.waitForIdle()
        rule.onAllNodes(hasText("Game over")).onFirst().assertIsDisplayed()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppIdentityTest {
    @Test
    fun usesTheBasilAsPackageName() {
        assertEquals("io.github.basil_as.basillines", ApplicationProvider.getApplicationContext<Context>().packageName)
    }

    @Test
    fun appNameIsColorLines() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Color Lines", context.getString(R.string.app_name))
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GameStorageMigrationTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before
    fun clear() {
        context.getSharedPreferences("colorlines_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun oldThemeNamesAreMigrated() {
        assertEquals(AppTheme.LINES_98, GameStorage.parseTheme("CLASSIC_98"))
        assertEquals(AppTheme.MODERN, GameStorage.parseTheme("nope"))
        assertEquals(AppTheme.MODERN, GameStorage.parseTheme(null))
        AppTheme.entries.forEach { assertEquals(it, GameStorage.parseTheme(it.name)) }
    }

    @Test
    fun spawnPreviewFollowsTheThemeUntilTheUserChoosesAndIsRemembered() {
        val s = GameStorage(context)
        assertNull(s.spawnPreview)
        s.spawnPreview = false
        assertEquals(false, GameStorage(context).spawnPreview)
        s.spawnPreview = true
        assertEquals(true, GameStorage(context).spawnPreview)
    }

    @Test
    fun theHallPlayerNameAndNextToggleAreRemembered() {
        val s = GameStorage(context)
        assertTrue(s.hall.isEmpty())
        assertEquals("", s.playerName)
        assertTrue(s.showNext)
        s.hall = listOf(io.github.basil_as.basillines.engine.HallEntry("Ann", 300, 5))
        s.playerName = "Alexander the Great"
        s.showNext = false
        val again = GameStorage(context)
        assertEquals(listOf(io.github.basil_as.basillines.engine.HallEntry("Ann", 300, 5)), again.hall)
        assertEquals("Alexander th", again.playerName)
        assertEquals(false, again.showNext)
    }

    @Test
    fun aProfileIsRebuiltFromAnOlderHistory() {
        val prefs = context.getSharedPreferences("colorlines_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("history", "50,1790000000000,20,2,10,1;30,1789990000000,10,1,5,0").commit()
        val p = GameStorage(context).progress
        assertEquals(2, p.totalGames)
        assertEquals(50, p.bestScore)
        assertTrue("first_game" in p.achievements)
    }
}


@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "en-rUS-w411dp-h891dp-port")
class DosThemeTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun startDos() {
        val prefs = context.getSharedPreferences("colorlines_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().putString("theme", AppTheme.COLORLINES_92.name).commit()
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()
    }

    private fun balls() = rule.onAllNodes(
        SemanticsMatcher("ball cell") { node ->
            node.config.getOrNull(SemanticsProperties.ContentDescription)?.any { Regex("Row \\d+, column \\d+, [a-z]+ ball.*").matches(it) } == true
        }
    ).fetchSemanticsNodes().size

    @Test
    fun showsTheOriginalControlsAndABoardOfCells() {
        startDos()
        for (label in listOf("F1: Help", "F2: Sound", "F3: Show next balls", "F4: Restart")) {
            rule.onNodeWithContentDescription(label).assertIsDisplayed()
        }
        assertEquals(5, balls())
        rule.onNodeWithContentDescription("Top Ten").assertIsDisplayed()
    }

    @Test
    fun f1OpensTheOriginalHelpWindowAndAnyTapClosesIt() {
        startDos()
        rule.onNodeWithContentDescription("F1: Help").performClick()
        rule.onNodeWithContentDescription("Close").assertIsDisplayed()
        rule.onNodeWithContentDescription("Close").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("F1: Help").assertIsDisplayed()
    }

    @Test
    fun f3TogglesTheNextBallsAndIsRemembered() {
        startDos()
        rule.onNodeWithContentDescription("F3: Show next balls").performClick()
        rule.waitForIdle()
        assertEquals(false, GameStorage(context).showNext)
    }

    @Test
    fun aFinishedGameEntersTheTopTenAndBecomesTheKing() {
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

        assertTrue(rule.onAllNodes(hasText("Game over")).fetchSemanticsNodes().isEmpty())
        rule.onNode(hasText("Ann")).assertIsDisplayed()
        val hall = GameStorage(context).hall
        assertEquals(1, hall.size)
        assertEquals("Ann", hall[0].name)
        assertEquals(60, hall[0].score)
    }
}


@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "en-rUS-w411dp-h891dp-port")
class AnalysisTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun seedHistory(count: Int) {
        val now = System.currentTimeMillis()
        val history = (0 until count).joinToString(";") { i -> "${200 - i * 4},${now - i * 3_600_000L},40,4,20,1,5,120000" }
        context.getSharedPreferences("colorlines_prefs", Context.MODE_PRIVATE).edit().clear().putString("history", history).commit()
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()
    }

    @Test
    fun showsTheTrendEfficiencyAndCharts() {
        seedHistory(25)
        rule.onNodeWithContentDescription("Statistics").performClick()
        rule.waitForIdle()
        rule.onAllNodes(hasText("Analysis")).onFirst().assertExists()
        rule.onAllNodes(hasText("better than the 10 before", substring = true)).onFirst().assertExists()
        rule.onAllNodes(hasText("points per move", substring = true)).onFirst().assertExists()
        rule.onAllNodes(hasContentDescription("Games per day, last 14 days", substring = true)).onFirst().assertExists()
    }

    @Test
    fun asksForMoreGamesWhileTheHistoryIsShort() {
        seedHistory(3)
        rule.onNodeWithContentDescription("Statistics").performClick()
        rule.waitForIdle()
        rule.onAllNodes(hasText("Finish 17 more games", substring = true)).onFirst().assertExists()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SoundManagerTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun everyEventPlaysInEveryLookWithoutFailing() {
        val sounds = SoundManager(context)
        sounds.isEnabled = true
        for (profile in SoundManager.Profile.entries) {
            sounds.profile = profile
            for (kind in io.github.basil_as.basillines.engine.SoundKind.entries) {
                for (points in listOf(0, 10, 20, 60)) sounds.play(kind, points)
            }
        }
        sounds.release()
    }

    @Test
    fun mutedSoundDoesNothing() {
        val sounds = SoundManager(context)
        sounds.isEnabled = false
        for (profile in SoundManager.Profile.entries) {
            sounds.profile = profile
            for (kind in io.github.basil_as.basillines.engine.SoundKind.entries) sounds.play(kind)
        }
        assertEquals(false, SoundManager(context).isEnabled)
        sounds.release()
    }
}
