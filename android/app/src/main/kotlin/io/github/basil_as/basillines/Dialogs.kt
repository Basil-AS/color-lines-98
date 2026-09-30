package io.github.basil_as.basillines

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import io.github.basil_as.basillines.engine.GameRecord
import io.github.basil_as.basillines.engine.GameStats
import io.github.basil_as.basillines.engine.Insights
import io.github.basil_as.basillines.engine.LineDetector
import io.github.basil_as.basillines.engine.Levels
import io.github.basil_as.basillines.engine.Progress
import io.github.basil_as.basillines.engine.ProgressTracker
import io.github.basil_as.basillines.engine.ScoringSystem
import java.text.DateFormat
import java.util.Date

private val LEVEL_TITLES = listOf(
    R.string.level_title_novice, R.string.level_title_apprentice, R.string.level_title_skilled,
    R.string.level_title_expert, R.string.level_title_master, R.string.level_title_grandmaster,
    R.string.level_title_legend
)

/** Name and description of every achievement, keyed by the id used in the engine. */
val ACHIEVEMENT_TEXT: Map<String, Pair<Int, Int>> = mapOf(
    "first_game" to (R.string.ach_first_game_name to R.string.ach_first_game_desc),
    "first_line" to (R.string.ach_first_line_name to R.string.ach_first_line_desc),
    "long_line_7" to (R.string.ach_long_line_7_name to R.string.ach_long_line_7_desc),
    "long_line_9" to (R.string.ach_long_line_9_name to R.string.ach_long_line_9_desc),
    "score_100" to (R.string.ach_score_100_name to R.string.ach_score_100_desc),
    "score_250" to (R.string.ach_score_250_name to R.string.ach_score_250_desc),
    "score_500" to (R.string.ach_score_500_name to R.string.ach_score_500_desc),
    "score_1000" to (R.string.ach_score_1000_name to R.string.ach_score_1000_desc),
    "games_10" to (R.string.ach_games_10_name to R.string.ach_games_10_desc),
    "games_50" to (R.string.ach_games_50_name to R.string.ach_games_50_desc),
    "games_100" to (R.string.ach_games_100_name to R.string.ach_games_100_desc),
    "lines_50" to (R.string.ach_lines_50_name to R.string.ach_lines_50_desc),
    "lines_250" to (R.string.ach_lines_250_name to R.string.ach_lines_250_desc),
    "streak_3" to (R.string.ach_streak_3_name to R.string.ach_streak_3_desc),
    "streak_7" to (R.string.ach_streak_7_name to R.string.ach_streak_7_desc),
    "marathon" to (R.string.ach_marathon_name to R.string.ach_marathon_desc),
    "level_5" to (R.string.ach_level_5_name to R.string.ach_level_5_desc),
    "level_10" to (R.string.ach_level_10_name to R.string.ach_level_10_desc)
)

private val THEME_NAMES = listOf(
    AppTheme.MODERN to R.string.theme_modern,
    AppTheme.LIGHT to R.string.theme_light,
    AppTheme.LINES_98 to R.string.theme_lines98,
    AppTheme.COLORLINES_92 to R.string.theme_colorlines92
)

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    if (totalSeconds < 60) return stringResource(R.string.time_s, totalSeconds)
    val totalMinutes = totalSeconds / 60
    if (totalMinutes < 60) return stringResource(R.string.time_m, totalMinutes)
    return stringResource(R.string.time_hm, totalMinutes / 60, "%02d".format(totalMinutes % 60))
}

@Composable
fun GameOverDialog(
    score: Int,
    best: Int,
    newRecord: Boolean,
    xpGained: Int,
    levelUp: Int?,
    unlocked: List<String>,
    onPlayAgain: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text(stringResource(R.string.gameover_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (newRecord) {
                    Text(
                        "★ " + stringResource(R.string.gameover_newRecord),
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(stringResource(R.string.gameover_final), score.toString(), Modifier.weight(1f))
                    StatCard(stringResource(R.string.gameover_best), best.toString(), Modifier.weight(1f))
                }
                if (xpGained > 0) Text(stringResource(R.string.gameover_xp, xpGained), fontWeight = FontWeight.Bold)
                if (levelUp != null) {
                    Text(
                        stringResource(R.string.gameover_levelUp, levelUp),
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold
                    )
                }
                if (unlocked.isNotEmpty()) {
                    Text(stringResource(R.string.gameover_unlocked), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    unlocked.forEach { id -> AchievementRow(id, unlockedAt = 0L, showDate = false) }
                }
            }
        },
        confirmButton = { Button(onClick = onPlayAgain) { Text(stringResource(R.string.gameover_playAgain)) } }
    )
}

@Composable
private fun AchievementRow(id: String, unlockedAt: Long?, showDate: Boolean = true) {
    val text = ACHIEVEMENT_TEXT[id] ?: return
    val done = unlockedAt != null
    val dateFormat = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (done) 0.7f else 0.3f))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(if (done) "★" else "🔒", color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
            Column(Modifier.weight(1f)) {
                Text(stringResource(text.first), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(stringResource(text.second), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (showDate) {
                Text(
                    if (done && unlockedAt != null) dateFormat.format(Date(unlockedAt)) else stringResource(R.string.stats_locked),
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
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
                                R.string.help_scoringLine,
                                pluralStringResource(R.plurals.balls, n, n),
                                LineDetector.calculateScore(n, ScoringSystem.GAMOS_1992)
                            )
                        )
                    }
                }
                Text(stringResource(R.string.help_movement))
                Text(stringResource(R.string.help_freeTurn))
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.btn_close)) } }
    )
}

@Composable
fun SettingsDialog(
    theme: AppTheme,
    onTheme: (AppTheme) -> Unit,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    spawnPreview: Boolean,
    onTogglePreview: () -> Unit,
    language: AppLanguage,
    onLanguage: (AppLanguage) -> Unit,
    playerName: String,
    onPlayerName: (String) -> Unit,
    onClose: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.settings_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.settings_theme), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                THEME_NAMES.forEach { (value, name) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = theme == value, role = Role.RadioButton, onClick = { onTheme(value) })
                            .height(48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = theme == value, onClick = null)
                        Text(stringResource(name), Modifier.padding(start = 12.dp))
                    }
                }
                SwitchRow(stringResource(R.string.settings_sound), soundEnabled, onToggleSound)
                SwitchRow(stringResource(R.string.settings_preview), spawnPreview, onTogglePreview)
                var editingName by remember { mutableStateOf(false) }
                TextButton(onClick = { editingName = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.settings_playerName) + ": " + playerName.ifBlank { "—" },
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                }
                if (editingName) {
                    PlayerNameDialog(playerName, onPlayerName, onDone = { editingName = false })
                }
                Text(stringResource(R.string.lang_label), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                listOf(
                    AppLanguage.AUTO to R.string.lang_auto,
                    AppLanguage.EN to R.string.lang_en,
                    AppLanguage.RU to R.string.lang_ru
                ).forEach { (value, name) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = language == value, role = Role.RadioButton, onClick = { onLanguage(value) })
                            .height(48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = language == value, onClick = null)
                        Text(stringResource(name), Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.btn_close)) } }
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = { onToggle() })
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, Modifier.weight(1f).padding(end = 12.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun TrendChart(values: List<Int>) {
    if (values.size < 2) return
    val color = MaterialTheme.colorScheme.primary
    val max = maxOf(values.max(), 1)
    Canvas(Modifier.fillMaxWidth().height(56.dp)) {
        val step = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = i * step
            val y = size.height - 4f - (v.toFloat() / max) * (size.height - 8f)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            drawCircle(color, radius = 4f, center = Offset(x, y))
        }
        drawPath(path, color, style = Stroke(width = 4f, cap = StrokeCap.Round))
    }
}

@Composable
fun StatsDialog(
    history: List<GameRecord>,
    progress: Progress,
    now: Long,
    onClear: () -> Unit,
    onClose: () -> Unit
) {
    val summary = GameStats.summarize(history)
    var confirming by remember { mutableStateOf(false) }
    var showAll by remember { mutableStateOf(false) }
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
    val level = Levels.info(ProgressTracker.xpOf(progress))
    val title = LEVEL_TITLES[Levels.titleIndex(level.level)]
    val streak = ProgressTracker.currentStreak(progress.days, ProgressTracker.dayKey(now))
    val unlockedCount = ProgressTracker.ACHIEVEMENTS.count { it.id in progress.achievements }
    val shown = if (showAll) history else history.take(10)

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.stats_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.level_label, level.level), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            Text(stringResource(title), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(progress = { level.fraction.toFloat() }, modifier = Modifier.fillMaxWidth())
                        Text(
                            stringResource(R.string.level_xp, level.into, level.needed),
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val cards = listOf(
                    stringResource(R.string.stats_gamesPlayed) to progress.totalGames.toString(),
                    stringResource(R.string.stats_best) to progress.bestScore.toString(),
                    stringResource(R.string.stats_average) to summary.averageScore.toString(),
                    stringResource(R.string.stats_totalScore) to progress.totalScore.toString(),
                    stringResource(R.string.stats_lines) to progress.totalLines.toString(),
                    stringResource(R.string.stats_bestLine) to progress.bestLine.toString(),
                    stringResource(R.string.stats_streak) to pluralStringResource(R.plurals.days, streak, streak),
                    stringResource(R.string.stats_bestStreak) to ProgressTracker.bestStreak(progress.days).let { pluralStringResource(R.plurals.days, it, it) },
                    stringResource(R.string.stats_playTime) to formatDuration(progress.totalPlayMs),
                    stringResource(R.string.stats_longestGame) to pluralStringResource(R.plurals.moves, progress.longestGameMoves, progress.longestGameMoves)
                )
                cards.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { (label, value) -> StatCard(label, value, Modifier.weight(1f)) }
                    }
                }

                if (history.isNotEmpty()) InsightsSection(history, progress, level.level, level.xp, now)

                Text(stringResource(R.string.stats_trend), Modifier.padding(top = 8.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (history.size < 2) Text(stringResource(R.string.stats_empty)) else TrendChart(ProgressTracker.scoreTrend(history, 20))

                Text(
                    stringResource(R.string.stats_achievements) + " · " +
                        stringResource(R.string.stats_achievementsCount, unlockedCount, ProgressTracker.ACHIEVEMENTS.size),
                    Modifier.padding(top = 8.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ProgressTracker.ACHIEVEMENTS.forEach { AchievementRow(it.id, progress.achievements[it.id]) }

                Text(stringResource(R.string.stats_recent), Modifier.padding(top = 8.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (history.isEmpty()) Text(stringResource(R.string.stats_empty))
                shown.forEach { game ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    ) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(game.score.toString(), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                                if (game == summary.bestRecord) Badge(stringResource(R.string.stats_bestMark), filled = true)
                                if (!game.completed) Badge(stringResource(R.string.stats_unfinished), filled = false)
                            }
                            Text(
                                dateFormat.format(Date(game.endedAt)) + " · " +
                                    pluralStringResource(R.plurals.moves, game.moves, game.moves) + " · " +
                                    formatDuration(game.durationMs),
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (history.size > 10) {
                    TextButton(onClick = { showAll = !showAll }) {
                        Text(if (showAll) stringResource(R.string.stats_showLess) else stringResource(R.string.stats_showAll, history.size))
                    }
                }
                if (history.isNotEmpty() || progress.totalGames > 0) {
                    if (confirming) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(stringResource(R.string.stats_confirmClear))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onClear(); confirming = false }) { Text(stringResource(R.string.stats_confirmYes)) }
                                OutlinedButton(onClick = { confirming = false }) { Text(stringResource(R.string.stats_confirmNo)) }
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

@Composable
private fun PlayerNameDialog(name: String, onName: (String) -> Unit, onDone: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text(stringResource(R.string.settings_playerName)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = onName,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { TextButton(onClick = onDone) { Text(stringResource(R.string.btn_close)) } }
    )
}

@Composable
private fun BarChart(values: List<Int>, labels: List<String>, description: String) {
    val max = maxOf(values.maxOrNull() ?: 1, 1)
    val color = MaterialTheme.colorScheme.primary
    val a11y = description + ": " + labels.zip(values).joinToString(", ") { "${it.first} ${it.second}" }
    Row(
        Modifier.fillMaxWidth().height(84.dp).semantics { contentDescription = a11y },
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        values.forEachIndexed { i, v ->
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(if (v == 0) 0.03f else maxOf(0.1f, v.toFloat() / max) * 0.8f)
                        .background(color, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                )
                Text(labels[i], fontSize = 9.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun InsightsSection(history: List<GameRecord>, progress: Progress, level: Int, xp: Int, now: Long) {
    val window = 10
    val trend = Insights.trendOf(history, window)
    val eff = Insights.efficiency(history)
    val eta = Insights.levelEta(history, Levels.threshold(level + 1) - xp)
    val days = Insights.dailyActivity(history, 14, now)
    val hist = Insights.scoreHistogram(history, 50)
    val weekdays = Insights.weekdayActivity(history)
    val steps = Insights.recordProgression(history).takeLast(5)
    val dateFormat = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }
    val weekdayNames = remember {
        // 2024-01-01 is a Monday.
        (0 until 7).map { java.time.DayOfWeek.of(it + 1).getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault()) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            stringResource(R.string.insights_title), Modifier.padding(top = 8.dp), fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            if (trend == null) {
                stringResource(R.string.insights_trend_needMore, maxOf(1, window * 2 - history.size))
            } else {
                val id = when (trend.direction) {
                    Insights.Direction.UP -> R.string.insights_trend_up
                    Insights.Direction.DOWN -> R.string.insights_trend_down
                    Insights.Direction.FLAT -> R.string.insights_trend_flat
                }
                stringResource(id, window, trend.recentAverage, kotlin.math.abs(trend.changePercent))
            },
            fontSize = 14.sp
        )
        Text(stringResource(R.string.insights_efficiency, "%.1f".format(eff.average), "%.1f".format(eff.best)), fontSize = 14.sp)
        if (eta != null) {
            Text(
                stringResource(R.string.insights_eta, pluralStringResource(R.plurals.games, eta, eta), level + 1),
                fontSize = 14.sp
            )
        }
        Text(stringResource(R.string.insights_daily), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        BarChart(days.map { it.games }, days.map { it.day.takeLast(2) }, stringResource(R.string.insights_daily))
        Text(stringResource(R.string.insights_histogram), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        BarChart(hist.map { it.count }, hist.map { stringResource(R.string.insights_bucketLabel, it.from) }, stringResource(R.string.insights_histogram))
        Text(stringResource(R.string.insights_weekdays), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        BarChart(weekdays.map { it.average }, weekdayNames, stringResource(R.string.insights_weekdays))
        if (steps.isNotEmpty()) {
            Text(stringResource(R.string.insights_records), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            steps.forEach { Text("${it.score} · " + dateFormat.format(Date(it.at)), fontSize = 13.sp) }
        }
    }
}
