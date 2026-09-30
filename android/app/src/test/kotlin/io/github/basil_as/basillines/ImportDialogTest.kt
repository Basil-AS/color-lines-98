package io.github.basil_as.basillines

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import io.github.basil_as.basillines.engine.Careers
import io.github.basil_as.basillines.engine.DayEntry
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
class ImportDialogTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun replacingNeedsTheCheckboxAndMergingLosesNothing() {
        var merged = 0
        var replaced = 0
        rule.setContent {
            ImportDialog(games = 12, currentGames = 34, exportedAt = 1_700_000_000_000, first = 1_600_000_000_000, last = 1_699_000_000_000,
                onMerge = { merged++ }, onReplace = { replaced++ }, onCancel = {})
        }
        rule.onNodeWithText("Backup from", substring = true).assertExists()
        rule.onNodeWithText("Nothing you have here is lost", substring = true).assertExists()
        rule.onNodeWithText("Replace my data").performClick()
        rule.onNodeWithText("replaces your 34 games", substring = true).assertExists()
        rule.onNodeWithText("Yes, replace").assertIsNotEnabled()
        rule.onNodeWithText("Yes, replace").performClick()
        assertEquals(0, replaced)
        rule.onNodeWithText("I understand, replace my data").performClick()
        rule.onNodeWithText("Yes, replace").assertIsEnabled().performClick()
        assertEquals(1, replaced)
        assertEquals(0, merged)
    }

    @Test
    fun theLedgerSurvivesARestart() {
        val storage = GameStorage(ApplicationProvider.getApplicationContext())
        storage.ledger = mapOf("2026-09-01" to DayEntry(3, 2, 900, 500, 40, 6, 120_000))
        assertEquals(mapOf("2026-09-01" to DayEntry(3, 2, 900, 500, 40, 6, 120_000)), GameStorage(ApplicationProvider.getApplicationContext()).ledger)
        storage.ledger = emptyMap()
        assertEquals(emptyMap<String, DayEntry>(), Careers.sanitize(null))
    }
}
