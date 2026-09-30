package io.github.basil_as.basillines

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.basil_as.basillines.engine.Careers
import io.github.basil_as.basillines.engine.GameRecord
import io.github.basil_as.basillines.engine.Ledger
import io.github.basil_as.basillines.engine.ProgressTracker
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private fun num(v: Number): String = NumberFormat.getInstance().format(v)

@Composable
private fun Subtitle(text: String) = Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp))

@Composable
private fun Muted(text: String) = Text(text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
private fun KeyValue(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, Modifier.weight(1f).padding(end = 8.dp))
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

/** "September 2026" for a "YYYY-MM" key, in the language of the app. */
private fun monthName(month: String): String {
    val (y, m) = month.split("-").map { it.toInt() }
    return LocalDate.of(y, m, 1).month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault()).replaceFirstChar { it.uppercase() } + " " + y
}

private fun tierName(t: Careers.Tier): Int = when (t) {
    Careers.Tier.BRONZE -> R.string.season_tier_bronze
    Careers.Tier.SILVER -> R.string.season_tier_silver
    Careers.Tier.GOLD -> R.string.season_tier_gold
    Careers.Tier.PLATINUM -> R.string.season_tier_platinum
    Careers.Tier.LEGEND -> R.string.season_tier_legend
}

@Composable
fun CareerTab(ledger: Ledger, today: LocalDate) {
    val t = Careers.totals(ledger)
    val (current, longest) = Careers.streaks(ledger, today.toString())
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Muted(stringResource(R.string.career_intro))
        KeyValue(stringResource(R.string.stats_gamesPlayed), num(t.games))
        KeyValue(stringResource(R.string.stats_totalScore), num(t.score))
        KeyValue(stringResource(R.string.stats_lines), num(t.lines))
        KeyValue(stringResource(R.string.career_activeDays), num(t.activeDays))
        KeyValue(stringResource(R.string.stats_streak), pluralStringResource(R.plurals.days, current, current))
        KeyValue(stringResource(R.string.stats_bestStreak), pluralStringResource(R.plurals.days, longest, longest))
        KeyValue(stringResource(R.string.stats_playTime), formatDuration(t.playMs))

        Subtitle(stringResource(R.string.career_heatmap))
        Heatmap(ledger, today)

        Subtitle(stringResource(R.string.career_milestones))
        for ((track, value, ladder) in Careers.milestones(t)) {
            val name = stringResource(
                when (track) {
                    Careers.Track.GAMES -> R.string.career_ms_games
                    Careers.Track.SCORE -> R.string.career_ms_score
                    Careers.Track.LINES -> R.string.career_ms_lines
                    Careers.Track.HOURS -> R.string.career_ms_hours
                    Careers.Track.DAYS -> R.string.career_ms_days
                }
            )
            Column(Modifier.semantics(mergeDescendants = true) { contentDescription = name }) {
                Text(name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                LinearProgressIndicator(progress = { ladder.fraction.toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp))
                Muted(stringResource(R.string.career_ms_progress, num(value), num(ladder.next), ladder.reached.toString()))
            }
        }

        Subtitle(stringResource(R.string.career_months))
        val months = Careers.monthly(ledger).takeLast(12).reversed()
        if (months.isEmpty()) Muted(stringResource(R.string.stats_empty))
        for (m in months) {
            KeyValue(monthName(m.month), stringResource(R.string.career_monthRow, m.games.toString(), num(m.best), num(m.average)))
        }
    }
}

@Composable
private fun Heatmap(ledger: Ledger, today: LocalDate) {
    val grid = Careers.heatmap(ledger, today, 26)
    val accent = MaterialTheme.colorScheme.primary
    val empty = MaterialTheme.colorScheme.surfaceVariant
    val label = stringResource(R.string.career_heatmap)
    Canvas(Modifier.fillMaxWidth().height(96.dp).semantics { contentDescription = label }) {
        val cell = minOf(size.width / grid.size, size.height / 7f)
        for ((w, col) in grid.withIndex()) for ((r, c) in col.withIndex()) {
            if (c == null) continue
            val color = if (c.second == 0) empty else accent.copy(alpha = 0.25f + 0.25f * (c.second - 1))
            drawRoundRect(color, Offset(w * cell + 1f, r * cell + 1f), Size(cell - 2f, cell - 2f), CornerRadius(cell * 0.2f))
        }
    }
}

@Composable
fun SeasonsTab(ledger: Ledger, today: LocalDate) {
    val s = Careers.season(ledger, today)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.season_title, monthName(s.month)), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        Text(stringResource(tierName(s.tier)), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Muted(stringResource(R.string.season_points, num(s.points), s.games.toString()))
        val next = s.nextTier
        val toNext = s.toNext
        Muted(
            if (next != null && toNext != null) stringResource(R.string.season_toNext, num(toNext), stringResource(tierName(next)))
            else stringResource(R.string.season_top)
        )
        Muted(stringResource(R.string.season_daysLeft, s.daysLeft.toString()))
        Muted(stringResource(R.string.season_baseline, num(s.baseline)))
        Subtitle(stringResource(R.string.season_past))
        if (s.past.isEmpty()) Muted(stringResource(R.string.season_none))
        for ((month, points, tier) in s.past) KeyValue(monthName(month), stringResource(tierName(tier)) + " · " + num(points))
        s.bestMonth?.let { Muted(stringResource(R.string.season_best, monthName(it.month), num(it.score))) }
    }
}

@Composable
fun RecordsTab(history: List<GameRecord>, ledger: Ledger) {
    val rows = mutableListOf<Pair<String, String>>()
    history.maxByOrNull { it.score }?.takeIf { it.score > 0 }?.let { rows += stringResource(R.string.rec_bestScore) to num(it.score) }
    history.maxOfOrNull { it.maxLine }?.takeIf { it > 0 }?.let { rows += stringResource(R.string.rec_bestLine) to it.toString() }
    history.maxByOrNull { it.lines }?.takeIf { it.lines > 0 }?.let { rows += stringResource(R.string.rec_mostLines) to num(it.lines) }
    history.filter { it.moves >= 15 }.maxByOrNull { it.score.toDouble() / it.moves }?.let {
        rows += stringResource(R.string.rec_bestEfficiency) to "%.1f".format(Locale.ROOT, it.score.toDouble() / it.moves)
    }
    history.maxByOrNull { it.durationMs }?.takeIf { it.durationMs > 0 }?.let { rows += stringResource(R.string.rec_longestGame) to formatDuration(it.durationMs) }
    ledger.maxByOrNull { it.value.score }?.takeIf { it.value.score > 0 }?.let { rows += stringResource(R.string.rec_bestDay) to "${num(it.value.score)} · ${it.key}" }
    ledger.maxByOrNull { it.value.games }?.let { rows += stringResource(R.string.rec_busiestDay) to "${it.value.games} · ${it.key}" }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Subtitle(stringResource(R.string.rec_title))
        if (rows.isEmpty()) Muted(stringResource(R.string.rec_none))
        for ((label, value) in rows) KeyValue(label, value)
    }
}

@Composable
fun DataTab(message: String?, onExportJson: () -> Unit, onExportCsv: () -> Unit, onImport: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Subtitle(stringResource(R.string.data_title))
        Muted(stringResource(R.string.data_intro))
        OutlinedButton(onClick = onExportJson, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.data_exportJson)) }
        OutlinedButton(onClick = onExportCsv, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.data_exportCsv)) }
        OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.data_import)) }
        if (message != null) Text(message, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
    }
}
