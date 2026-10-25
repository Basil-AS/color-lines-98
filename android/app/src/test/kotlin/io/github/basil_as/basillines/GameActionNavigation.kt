package io.github.basil_as.basillines

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick

/** Follow the compact game menu when the current theme does not expose the action inline. */
internal fun ComposeTestRule.gameAction(label: String, menu: String = "Game"): SemanticsNodeInteraction {
    val action = hasContentDescription(label) or hasText(label)
    if (onAllNodes(action).fetchSemanticsNodes().isEmpty()) {
        onNode(hasContentDescription(menu)).performClick()
    }
    return onNode(action)
}

internal fun ComposeTestRule.clickGameAction(label: String, menu: String = "Game") {
    gameAction(label, menu).performClick()
}
