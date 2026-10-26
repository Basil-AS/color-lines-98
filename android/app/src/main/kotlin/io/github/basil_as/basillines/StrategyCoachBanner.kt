package io.github.basil_as.basillines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Explanations follow the attempted move; the player decides when to advance. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StrategyCoachBanner(
    title: String,
    explanation: String,
    hintText: String?,
    step: Int,
    total: Int,
    solved: Boolean,
    last: Boolean,
    onHint: () -> Unit,
    onContinue: () -> Unit,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier.fillMaxWidth().padding(8.dp), shape = MaterialTheme.shapes.medium, tonalElevation = 6.dp) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.coach_progress, step, total), style = MaterialTheme.typography.labelMedium)
            Text(title, fontWeight = FontWeight.Bold)
            Text(explanation, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            if (hintText != null) Text(hintText)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (solved) Button(onClick = onContinue) {
                    Text(stringResource(if (last) R.string.tutorial_finish else R.string.tutorial_continue))
                } else if (hintText == null) OutlinedButton(onClick = onHint) { Text(stringResource(R.string.coach_hint)) }
                OutlinedButton(onClick = onLeave) { Text(stringResource(R.string.coach_leave)) }
            }
        }
    }
}
