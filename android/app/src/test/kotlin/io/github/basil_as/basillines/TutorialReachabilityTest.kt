package io.github.basil_as.basillines

import android.content.Context
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [29, 37], qualifiers = "ru-rRU-w914dp-h411dp-land")
class TutorialReachabilityTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @After fun restoreFont() { RuntimeEnvironment.setFontScale(1f) }

    private fun tap(row: Int, column: Int) {
        rule.onNodeWithContentDescription("Ряд $row, столбец $column,", substring = true)
            .performTouchInput { click() }
        rule.mainClock.advanceTimeBy(1500)
        ShadowLooper.idleMainLooper(1500, TimeUnit.MILLISECONDS)
        rule.waitForIdle()
    }

    @Test fun enlargedRussianLandscapeTutorialAcceptsTheRequiredLineMoveByTouch() = verifyLineMove(1.3f)

    @Test
    @Config(qualifiers = "ru-rRU-w740dp-h280dp-land")
    fun shortLandscapeAtDoubleFontKeepsTheLineMoveReachable() = themes.forEach { verifyLineMove(2f, it) }

    @Test
    @Config(qualifiers = "ru-rRU-w411dp-h891dp-port")
    fun portraitAtDoubleFontKeepsTheLineMoveReachable() = themes.forEach { verifyLineMove(2f, it) }

    private val themes = listOf(AppTheme.MODERN, AppTheme.COLORLINES_92, AppTheme.LINES_98, AppTheme.LINES_98_PLUS)

    private fun text(label: String) = rule.onNodeWithText(label).also {
        if (!it.isDisplayed()) it.performScrollTo()
    }

    private fun verifyLineMove(fontScale: Float, theme: AppTheme = AppTheme.MODERN) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("colorlines_prefs", Context.MODE_PRIVATE).edit().clear()
            .putString("theme", theme.name).commit()
        RuntimeEnvironment.setFontScale(fontScale)
        rule.activityRule.scenario.recreate()
        val prefs = context.getSharedPreferences("colorlines_prefs", Context.MODE_PRIVATE)
        val savedGame = prefs.getString("game", null)
        val settings = rule.gameAction("Настройки", "Игра")
        if (!settings.isDisplayed()) settings.performScrollTo()
        settings.performClick()
        text("Пройти обучение").performClick()
        tap(5, 3)
        tap(3, 3)
        text("Продолжить").performTouchInput { click() }
        text("Шаг 4 из 7").assertIsDisplayed()
        val board = rule.onNodeWithTag("adaptive-game-board").fetchSemanticsNode().boundsInRoot
        val instructions = rule.onNodeWithText("Обучение").fetchSemanticsNode().boundsInRoot
        val cells = rule.onAllNodes(hasContentDescription("Ряд ", substring = true)).fetchSemanticsNodes()
        assertEquals(81, cells.size)
        cells.forEach { cell ->
            val bounds = cell.boundsInRoot
            assertTrue("$theme cell must fit the unobscured board: $bounds / $board", bounds.width > 0 && bounds.height > 0 &&
                bounds.left >= board.left - 1 && bounds.right <= board.right + 1 && bounds.top >= board.top - 1 && bounds.bottom <= board.bottom + 1 && bounds.bottom <= instructions.top)
        }
        tap(3, 8)
        tap(6, 6)
        text("Шаг 5 из 7").assertIsDisplayed()
        text("Пропустить обучение").performTouchInput { click() }
        rule.waitForIdle()
        assertEquals(theme, GameStorage(context).theme)
        assertEquals(savedGame, prefs.getString("game", null))
    }
}
