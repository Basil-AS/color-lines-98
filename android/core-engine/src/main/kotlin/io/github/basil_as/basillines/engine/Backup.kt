package io.github.basil_as.basillines.engine

/** The backup file: the same JSON as the web app writes (see src/backup.ts), so results move between the two. */
data class BackupSettings(
    val theme: String? = null,
    val language: String? = null,
    val playerName: String? = null,
    val soundEnabled: Boolean? = null,
    val spawnPreview: Boolean? = null,
    val showNext: Boolean? = null,
    val mode: ModeId? = null,
    val effects: String? = null,
    val vibration: Boolean? = null
)

data class Backup(
    val exportedAt: Long,
    val platform: String,
    val appVersion: String,
    val history: List<GameRecord>,
    val ledger: Ledger,
    val progress: Progress,
    val hall: List<HallEntry>,
    val settings: BackupSettings
)

sealed interface ParsedBackup {
    data class Ok(val backup: Backup) : ParsedBackup
    enum class Error : ParsedBackup { EMPTY, TOO_BIG, NOT_JSON, NOT_BACKUP, NEWER }
}

object Backups {
    const val FORMAT = "color-lines-backup"
    const val VERSION = 2
    const val MAX_CHARS = 64 * 1024 * 1024

    private fun num(v: Any?): Long? = (v as? Double)?.takeIf { it >= 0 && it == Math.floor(it) && it < 9e15 }?.toLong()

    fun encode(b: Backup): String = Json.write(
        linkedMapOf(
            "format" to FORMAT,
            "version" to VERSION,
            "exportedAt" to b.exportedAt,
            "app" to mapOf("platform" to b.platform, "version" to b.appVersion),
            "history" to b.history.map {
                mapOf(
                    "score" to it.score, "endedAt" to it.endedAt, "moves" to it.moves, "lines" to it.lines, "balls" to it.balls,
                    "completed" to it.completed, "maxLine" to it.maxLine, "durationMs" to it.durationMs, "mode" to it.mode.id
                ) + (it.cog?.let { c -> mapOf("cog" to CogKey.entries.associate { k -> k.json to c[k] }) } ?: emptyMap())
            },
            "ledger" to Careers.encode(b.ledger),
            "progress" to mapOf(
                "totalGames" to b.progress.totalGames, "completedGames" to b.progress.completedGames,
                "totalScore" to b.progress.totalScore, "totalLines" to b.progress.totalLines, "totalBalls" to b.progress.totalBalls,
                "totalMoves" to b.progress.totalMoves, "totalPlayMs" to b.progress.totalPlayMs, "bestScore" to b.progress.bestScore,
                "bestLine" to b.progress.bestLine, "mostLinesInGame" to b.progress.mostLinesInGame,
                "longestGameMoves" to b.progress.longestGameMoves, "days" to b.progress.days, "achievements" to b.progress.achievements,
                "bonusXp" to b.progress.bonusXp, "goalDays" to b.progress.goalDays,
                "gamesByMode" to ModeId.entries.associate { it.id to (b.progress.gamesByMode[it] ?: 0) },
                "bestByMode" to ModeId.entries.associate { it.id to (b.progress.bestByMode[it] ?: 0) }
            ),
            "hall" to b.hall.map { mapOf("name" to it.name, "score" to it.score, "at" to it.at) },
            "settings" to buildMap<String, Any?> {
                b.settings.theme?.let { put("theme", it) }
                b.settings.language?.let { put("language", it) }
                b.settings.playerName?.let { put("playerName", it) }
                b.settings.soundEnabled?.let { put("soundEnabled", it) }
                put("spawnPreview", b.settings.spawnPreview)
                b.settings.showNext?.let { put("showNext", it) }
                b.settings.mode?.let { put("mode", it.id) }
                b.settings.effects?.let { put("effects", it) }
                b.settings.vibration?.let { put("vibration", it) }
            }
        )
    )

    private fun cogOf(raw: Any?): Cognition? {
        val o = raw as? Map<*, *> ?: return null
        val values = CogKey.entries.map { k ->
            val v = o[k.json]
            if (v == null && k == CogKey.MIN_EMPTY) 81L else num(v) ?: return null
        }
        return Cognition.of(values)
    }

    fun sanitizeHistory(raw: Any?): List<GameRecord> {
        val list = raw as? List<*> ?: return emptyList()
        return list.mapNotNull { item ->
            val o = item as? Map<*, *> ?: return@mapNotNull null
            val mode = if (o["mode"] == null) ModeId.CLASSIC else ModeId.fromId(o["mode"] as? String) ?: return@mapNotNull null
            fun i(name: String, optional: Boolean = false): Long? = if (o[name] == null && optional) 0L else num(o[name])
            val score = i("score") ?: return@mapNotNull null
            val endedAt = i("endedAt") ?: return@mapNotNull null
            val moves = i("moves") ?: return@mapNotNull null
            val lines = i("lines") ?: return@mapNotNull null
            val balls = i("balls") ?: return@mapNotNull null
            val completed = o["completed"] as? Boolean ?: return@mapNotNull null
            val maxLine = i("maxLine", true) ?: return@mapNotNull null
            val duration = i("durationMs", true) ?: return@mapNotNull null
            if (maxOf(score, moves, lines, balls, maxLine) > Int.MAX_VALUE) return@mapNotNull null
            GameRecord(score.toInt(), endedAt, moves.toInt(), lines.toInt(), balls.toInt(), completed, maxLine.toInt(), duration, mode, cogOf(o["cog"]))
        }.sortedByDescending { it.endedAt }
    }

    private fun sanitizeHall(raw: Any?): List<HallEntry> {
        val list = raw as? List<*> ?: return emptyList()
        return list.mapNotNull { item ->
            val o = item as? Map<*, *> ?: return@mapNotNull null
            val score = num(o["score"])?.takeIf { it <= Int.MAX_VALUE } ?: return@mapNotNull null
            val at = num(o["at"]) ?: return@mapNotNull null
            val name = (o["name"] as? String) ?: return@mapNotNull null
            HallEntry(name.take(Hall.NAME_LIMIT), score.toInt(), at)
        }.sortedWith(compareByDescending<HallEntry> { it.score }.thenBy { it.at }).take(Hall.SIZE)
    }

    private fun sanitizeProgress(raw: Any?, history: List<GameRecord>): Progress {
        val o = raw as? Map<*, *> ?: return ProgressTracker.rebuild(history)
        fun n(name: String): Long = num(o[name]) ?: 0L
        // Achievement ids this version does not know are kept, so a round trip between the apps loses nothing.
        val ach = (o["achievements"] as? Map<*, *>)?.mapNotNull { (k, v) ->
            val id = k as? String ?: return@mapNotNull null
            val at = num(v) ?: return@mapNotNull null
            if (Regex("[a-z0-9_]{1,40}").matches(id)) id to at else null
        }?.toMap() ?: emptyMap()
        fun mode(name: String): Map<ModeId, Int> {
            val m = o[name] as? Map<*, *> ?: return ModeId.entries.associateWith { id -> history.count { it.mode == id } }.let { counts ->
                if (name == "gamesByMode") counts else ModeId.entries.associateWith { id -> history.filter { it.mode == id }.maxOfOrNull { it.score } ?: 0 }
            }
            return ModeId.entries.associateWith { id -> num(m[id.id])?.coerceAtMost(Int.MAX_VALUE.toLong())?.toInt() ?: 0 }
        }
        val days = (o["days"] as? List<*>)?.filterIsInstance<String>()?.filter { ProgressTracker.isDay(it) }?.sorted()?.takeLast(ProgressTracker.DAYS_LIMIT) ?: emptyList()
        fun i(name: String) = n(name).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        return Progress(
            i("totalGames"), i("completedGames"), i("totalScore"), i("totalLines"), i("totalBalls"), i("totalMoves"), n("totalPlayMs"),
            i("bestScore"), i("bestLine"), i("mostLinesInGame"), i("longestGameMoves"), days, ach,
            i("bonusXp"), (o["goalDays"] as? List<*>)?.filterIsInstance<String>()?.filter { ProgressTracker.isDay(it) }?.sorted()?.takeLast(ProgressTracker.DAYS_LIMIT) ?: emptyList(),
            mode("gamesByMode"), mode("bestByMode")
        )
    }

    private fun sanitizeSettings(raw: Any?): BackupSettings {
        val o = raw as? Map<*, *> ?: return BackupSettings()
        return BackupSettings(
            theme = (o["theme"] as? String)?.takeIf { it.length <= 20 },
            language = (o["language"] as? String)?.takeIf { it == "auto" || it == "en" || it == "ru" },
            playerName = (o["playerName"] as? String)?.take(Hall.NAME_LIMIT),
            soundEnabled = o["soundEnabled"] as? Boolean,
            spawnPreview = o["spawnPreview"] as? Boolean,
            showNext = o["showNext"] as? Boolean,
            mode = ModeId.fromId(o["mode"] as? String),
            effects = (o["effects"] as? String)?.takeIf { it == "off" || it == "calm" || it == "full" },
            vibration = o["vibration"] as? Boolean
        )
    }

    /** Nothing in the file is trusted: every part goes through the same validation as stored data. */
    fun parse(text: String): ParsedBackup {
        if (text.isBlank()) return ParsedBackup.Error.EMPTY
        if (text.length > MAX_CHARS) return ParsedBackup.Error.TOO_BIG
        val root = try { Json.parse(text) } catch (_: Json.ParseError) { return ParsedBackup.Error.NOT_JSON }
        val o = root as? Map<*, *> ?: return ParsedBackup.Error.NOT_BACKUP
        if (o["format"] != FORMAT) return ParsedBackup.Error.NOT_BACKUP
        val version = (o["version"] as? Double) ?: return ParsedBackup.Error.NOT_BACKUP
        if (version > VERSION) return ParsedBackup.Error.NEWER
        val history = sanitizeHistory(o["history"])
        val app = o["app"] as? Map<*, *>
        return ParsedBackup.Ok(
            Backup(
                exportedAt = num(o["exportedAt"]) ?: 0L,
                platform = (app?.get("platform") as? String)?.take(20) ?: "unknown",
                appVersion = (app?.get("version") as? String)?.take(20) ?: "",
                history = history,
                // The file's own ledger is trusted over one rebuilt in this device's time zone (games near midnight would double up).
                ledger = if (o["ledger"] != null) Careers.sanitize(o["ledger"]) else Careers.fromHistory(history),
                progress = sanitizeProgress(o["progress"], history),
                hall = sanitizeHall(o["hall"]),
                settings = sanitizeSettings(o["settings"])
            )
        )
    }

    private fun key(g: GameRecord) = "${g.endedAt}:${g.score}:${g.moves}:${g.mode.id}"

    /** Adds the games of a backup without duplicates; nothing already here is lost. */
    fun mergeHistories(current: List<GameRecord>, incoming: List<GameRecord>): List<GameRecord> {
        val byKey = LinkedHashMap<String, GameRecord>()
        for (g in current + incoming) {
            val k = key(g)
            val old = byKey[k]
            // The same game on both sides: keep the copy that carries the play data.
            if (old == null || (old.cog == null && g.cog != null)) byKey[k] = g
        }
        return byKey.values.sortedByDescending { it.endedAt }
    }

    fun mergeHalls(a: List<HallEntry>, b: List<HallEntry>): List<HallEntry> {
        val seen = HashSet<String>()
        return (a + b).filter { seen.add("${it.name}:${it.score}:${it.at}") }
            .sortedWith(compareByDescending<HallEntry> { it.score }.thenBy { it.at }).take(Hall.SIZE)
    }

    /** Rebuilt from the merged games, never below what either side had. */
    fun mergeProgress(current: Progress, incoming: Progress, history: List<GameRecord>): Progress {
        val base = ProgressTracker.rebuild(history)
        val ach = (incoming.achievements + current.achievements).toMutableMap()
        for ((id, at) in base.achievements) ach[id] = minOf(at, ach[id] ?: at)
        return Progress(
            maxOf(base.totalGames, current.totalGames, incoming.totalGames),
            maxOf(base.completedGames, current.completedGames, incoming.completedGames),
            maxOf(base.totalScore, current.totalScore, incoming.totalScore),
            maxOf(base.totalLines, current.totalLines, incoming.totalLines),
            maxOf(base.totalBalls, current.totalBalls, incoming.totalBalls),
            maxOf(base.totalMoves, current.totalMoves, incoming.totalMoves),
            maxOf(base.totalPlayMs, current.totalPlayMs, incoming.totalPlayMs),
            maxOf(base.bestScore, current.bestScore, incoming.bestScore),
            maxOf(base.bestLine, current.bestLine, incoming.bestLine),
            maxOf(base.mostLinesInGame, current.mostLinesInGame, incoming.mostLinesInGame),
            maxOf(base.longestGameMoves, current.longestGameMoves, incoming.longestGameMoves),
            (current.days + incoming.days + base.days).toSortedSet().toList().takeLast(ProgressTracker.DAYS_LIMIT),
            ach,
            maxOf(current.bonusXp, incoming.bonusXp),
            (current.goalDays + incoming.goalDays).toSortedSet().toList().takeLast(ProgressTracker.DAYS_LIMIT),
            ModeId.entries.associateWith { maxOf(base.gamesByMode[it] ?: 0, current.gamesByMode[it] ?: 0, incoming.gamesByMode[it] ?: 0) },
            ModeId.entries.associateWith { maxOf(base.bestByMode[it] ?: 0, current.bestByMode[it] ?: 0, incoming.bestByMode[it] ?: 0) }
        )
    }

    private fun csv(v: Any): String {
        val s = v.toString()
        val safe = if (s.isNotEmpty() && s[0] in "=+-@") "'$s" else s
        return if (safe.any { it == ',' || it == '"' || it == '\n' }) "\"" + safe.replace("\"", "\"\"") + "\"" else safe
    }

    /** One row per game, oldest first, with the play data (empty for games without it); cells that start like a formula are defused. */
    fun toCsv(history: List<GameRecord>): String = buildString {
        append("date,mode,score,moves,lines,balls,longest_line,play_seconds,completed,")
        append("local_hour,weekday,avg_decision_s,decision_spread_s,fast_moves,slow_moves,undos,hints,misses,clearing_moves,danger_moves,fewest_free_cells\n")
        val fallback = java.util.TimeZone.getDefault()
        for (g in history.asReversed()) {
            val c = g.cog
            val play: List<Any> = if (c == null) List(12) { "" } else {
                val tm = c[CogKey.TM]
                val mean = if (tm > 0) c[CogKey.THINK].toDouble() / tm else 0.0
                val spread = if (tm > 1) Math.sqrt(maxOf(0.0, c[CogKey.THINK_SQ] * 10_000.0 / tm - mean * mean)) else 0.0
                val (hour, weekday) = Telemetry.localTime(g.endedAt, c[CogKey.TZ], fallback.getOffset(g.endedAt) / 60_000)
                listOf(
                    hour, weekday + 1, String.format(java.util.Locale.ROOT, "%.2f", mean / 1000), String.format(java.util.Locale.ROOT, "%.2f", spread / 1000),
                    c[CogKey.FAST], c[CogKey.SLOW], c[CogKey.UNDO], c[CogKey.HINT], c[CogKey.MISS], c[CogKey.CLEARS], c[CogKey.DANGER], c[CogKey.MIN_EMPTY]
                )
            }
            append((listOf(java.time.Instant.ofEpochMilli(g.endedAt).toString(), g.mode.id, g.score, g.moves, g.lines, g.balls, g.maxLine, Math.round(g.durationMs / 1000.0), g.completed) + play).joinToString(",") { csv(it) })
            append('\n')
        }
    }
}
