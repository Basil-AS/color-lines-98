package com.colorlines.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.colorlines.engine.GameRecord
import com.colorlines.engine.GameStats
import com.colorlines.engine.LineDetector
import com.colorlines.engine.ScoringSystem
import java.text.DateFormat
import java.util.Date

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun GameOverDialog(score: Int, best: Int, newRecord: Boolean, onPlayAgain: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text(stringResource(R.string.gameover_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (newRecord) {
                    Text(
                        "★ " + stringResource(R.string.gameover_new_record),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(stringResource(R.string.gameover_final), score.toString(), Modifier.weight(1f))
                    StatCard(stringResource(R.string.gameover_best), best.toString(), Modifier.weight(1f))
                }
            }
        },
        confirmButton = { Button(onClick = onPlayAgain) { Text(stringResource(R.string.gameover_play_again)) } }
    )
}

@Composable
fun HelpDialog(onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.help_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.help_objective))
                Text(stringResource(R.string.help_scoring))
                Column {
                    for (n in 5..9) {
                        Text(
                            "• " + stringResource(
                                R.string.help_scoring_line,
                                pluralStringResource(R.plurals.balls, n, n),
                                LineDetector.calculateScore(n, ScoringSystem.GAMOS_1992)
                            )
                        )
                    }
                }
                Text(stringResource(R.string.help_movement))
                Text(stringResource(R.string.help_free_turn))
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.btn_close)) } }
    )
}

@Composable
fun StatsDialog(history: List<GameRecord>, onClear: () -> Unit, onClose: () -> Unit) {
    val summary = GameStats.summarize(history)
    var confirming by remember { mutableStateOf(false) }
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.stats_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(stringResource(R.string.stats_games_played), summary.gamesPlayed.toString(), Modifier.weight(1f))
                    StatCard(stringResource(R.string.stats_best), summary.bestScore.toString(), Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(stringResource(R.string.stats_average), summary.averageScore.toString(), Modifier.weight(1f))
                    StatCard(stringResource(R.string.stats_lines), summary.totalLines.toString(), Modifier.weight(1f))
                }
                Text(
                    stringResource(R.string.stats_recent),
                    modifier = Modifier.padding(top = 8.dp),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (history.isEmpty()) {
                    Text(stringResource(R.string.stats_empty))
                }
                for (game in history.take(10)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    ) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(game.score.toString(), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                                if (game == summary.bestRecord) Badge(stringResource(R.string.stats_best_mark), filled = true)
                                if (!game.completed) Badge(stringResource(R.string.stats_unfinished), filled = false)
                            }
                            Text(
                                dateFormat.format(Date(game.endedAt)) + " · " +
                                    pluralStringResource(R.plurals.moves, game.moves, game.moves),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (history.isNotEmpty()) {
                    if (confirming) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(stringResource(R.string.stats_confirm_clear))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onClear(); confirming = false }) {
                                    Text(stringResource(R.string.stats_confirm_yes))
                                }
                                OutlinedButton(onClick = { confirming = false }) {
                                    Text(stringResource(R.string.stats_confirm_no))
                                }
                            }
                        }
                    } else {
                        OutlinedButton(onClick = { confirming = true }) { Text(stringResource(R.string.stats_clear)) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.btn_close)) } }
    )
}

@Composable
private fun Badge(text: String, filled: Boolean) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        border = if (filled) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(Modifier.padding(horizontal = 6.dp, vertical = 1.dp)) {
            Text(text.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}
