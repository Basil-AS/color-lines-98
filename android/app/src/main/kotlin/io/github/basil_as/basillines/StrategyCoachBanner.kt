package io.github.basil_as.basillines

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A compact instruction deck replaces the controls; full explanations open independently. */
@Composable
internal fun TrainingInstructions(
    title: String, progress: String, explanation: String, hint: String?,
    primaryLabel: String?, onPrimary: () -> Unit,
    leaveLabel: String?, onLeave: () -> Unit,
    modifier: Modifier = Modifier
) {
    var details by remember(title, progress, explanation) { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val expandedCopy = configuration.screenWidthDp > configuration.screenHeightDp &&
        configuration.screenHeightDp >= 360 && LocalDensity.current.fontScale <= 1.4f
    if (details) SupportingDialog(
        onDismissRequest = { details = false },
        title = { Text(title) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(explanation)
            if (hint != null) Text(hint)
        } },
        confirmButton = { OutlinedButton(onClick = { details = false }) { Text(stringResource(R.string.btn_close)) } }
    )
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .25f))) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(progress, fontSize = 11.sp, lineHeight = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(title, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(hint ?: explanation, fontSize = 13.sp, lineHeight = 17.sp,
                maxLines = if (expandedCopy) Int.MAX_VALUE else 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (primaryLabel != null) Button(onClick = onPrimary, modifier = Modifier.weight(1f)) {
                    TrainingActionLabel(primaryLabel)
                }
                OutlinedButton(onClick = { details = true }, modifier = Modifier.weight(1f)) {
                    TrainingActionLabel(stringResource(R.string.training_details))
                }
                if (leaveLabel != null) OutlinedButton(onClick = onLeave, modifier = Modifier.weight(1f)) {
                    TrainingActionLabel(leaveLabel)
                }
            }
        }
    }
}

@Composable
private fun TrainingActionLabel(text: String) {
    Text(text, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
}

@Composable
internal fun StrategyCoachBanner(
    title: String, explanation: String, hintText: String?, step: Int, total: Int,
    solved: Boolean, last: Boolean, onHint: () -> Unit, onContinue: () -> Unit,
    onLeave: () -> Unit, modifier: Modifier = Modifier
) {
    TrainingInstructions(
        title, stringResource(R.string.coach_progress, step, total), explanation,
        if (solved) null else hintText,
        when { solved -> stringResource(if (last) R.string.coach_finish else R.string.coach_continue)
            hintText == null -> stringResource(R.string.coach_hint_show); else -> null },
        if (solved) onContinue else onHint,
        stringResource(R.string.coach_skip), onLeave, modifier
    )
}
