package io.github.basil_as.basillines.engine

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupTest {
    private val utc: ZoneId = ZoneId.of("UTC")
    private fun g(score: Int, at: Long, completed: Boolean = true, mode: ModeId = ModeId.CLASSIC, moves: Int = 30, lines: Int = 2, maxLine: Int = 5, ms: Long = 60_000) =
        GameRecord(score, at, moves, lines, 10, completed, maxLine, ms, mode)

    /** Written by src/backup.ts (web); Android must read it, and the web reads what Android writes. */
    private val webFile = "{\"format\":\"color-lines-backup\",\"version\":1,\"exportedAt\":1790000000000,\"app\":{\"platform\":\"web\",\"version\":\"1.5.0\"},\"history\":[{\"score\":300,\"endedAt\":1788436800000,\"moves\":50,\"lines\":4,\"balls\":20,\"completed\":true,\"maxLine\":6,\"durationMs\":90000,\"mode\":\"classic\"},{\"score\":120,\"endedAt\":1788350400000,\"moves\":20,\"lines\":1,\"balls\":5,\"completed\":false,\"maxLine\":5,\"durationMs\":30000,\"mode\":\"daily\"}],\"ledger\":{\"2026-09-03\":{\"games\":1,\"completed\":1,\"score\":300,\"best\":300,\"moves\":50,\"lines\":4,\"playMs\":90000},\"2026-09-02\":{\"games\":1,\"completed\":0,\"score\":120,\"best\":120,\"moves\":20,\"lines\":1,\"playMs\":30000}},\"progress\":{\"totalGames\":2,\"completedGames\":1,\"totalScore\":420,\"totalLines\":5,\"totalBalls\":25,\"totalMoves\":70,\"totalPlayMs\":120000,\"bestScore\":300,\"bestLine\":6,\"mostLinesInGame\":4,\"longestGameMoves\":50,\"days\":[\"2026-09-02\",\"2026-09-03\"],\"achievements\":{\"first_game\":1788350400000,\"first_line\":1788350400000,\"score_100\":1788350400000,\"daily_first\":1788350400000,\"score_250\":1788436800000},\"bonusXp\":0,\"goalDays\":[],\"gamesByMode\":{\"classic\":1,\"easy\":0,\"blitz\":0,\"daily\":1},\"bestByMode\":{\"classic\":300,\"easy\":0,\"blitz\":0,\"daily\":120}},\"hall\":[{\"name\":\"Ann\",\"score\":300,\"at\":1}],\"settings\":{\"theme\":\"neon\",\"language\":\"ru\",\"playerName\":\"Ann\",\"soundEnabled\":false,\"spawnPreview\":null,\"showNext\":true,\"mode\":\"blitz\"}}"

    @Test
    fun readsWhatTheWebAppWrote() {
        val p = Backups.parse(webFile) as ParsedBackup.Ok
        val b = p.backup
        assertEquals("web", b.platform)
        assertEquals(listOf(300, 120), b.history.map { it.score })
        assertEquals(listOf(ModeId.CLASSIC, ModeId.DAILY), b.history.map { it.mode })
        assertEquals(false, b.history[1].completed)
        assertEquals(listOf(HallEntry("Ann", 300, 1)), b.hall)
        assertEquals("neon", b.settings.theme)
        assertEquals("ru", b.settings.language)
        assertEquals(ModeId.BLITZ, b.settings.mode)
        assertNull(b.settings.spawnPreview)
        assertEquals(2, b.ledger.size)
        assertEquals(2, b.progress.totalGames)
        assertEquals(420, b.progress.totalScore)
    }

    @Test
    fun whatAndroidWritesIsReadBackIdentically() {
        val history = listOf(g(300, 3000), g(200, 2000, mode = ModeId.EASY))
        val b = Backup(5000, "android", "1.5.0", history, Careers.fromHistory(history, utc), ProgressTracker.rebuild(history),
            listOf(HallEntry("Ann", 300, 3000)), BackupSettings("neon", "ru", "Ann", false, null, true, ModeId.BLITZ))
        val back = (Backups.parse(Backups.encode(b)) as ParsedBackup.Ok).backup
        assertEquals(history, back.history)
        assertEquals(b.ledger, back.ledger)
        assertEquals(b.progress.totalGames, back.progress.totalGames)
        assertEquals(b.hall, back.hall)
        assertEquals(b.settings, back.settings)
    }

    /** Written by the web app (version 2): games carry their play data, settings the effects. */
    private val webFileV2 = "{\"format\":\"color-lines-backup\",\"version\":2,\"exportedAt\":1790000000000,\"app\":{\"platform\":\"web\",\"version\":\"2.0.0\"},\"history\":[{\"score\":300,\"endedAt\":1788436800000,\"moves\":50,\"lines\":4,\"balls\":20,\"completed\":true,\"maxLine\":6,\"durationMs\":90000,\"mode\":\"classic\",\"cog\":{\"tz\":112,\"tm\":40,\"think\":120000,\"thinkMax\":15000,\"thinkSq\":5200,\"fast\":5,\"slow\":3,\"lat\":60000,\"first\":4000,\"p1\":50000,\"n1\":20,\"p2\":70000,\"n2\":20,\"p3\":0,\"n3\":0,\"undo\":2,\"hint\":1,\"miss\":3,\"clears\":6,\"multi\":1,\"danger\":4,\"minEmpty\":9}}],\"settings\":{\"effects\":\"calm\",\"vibration\":false}}"

    @Test
    fun readsPlayDataAndEffectsWrittenByTheWebApp() {
        val b = (Backups.parse(webFileV2) as ParsedBackup.Ok).backup
        val cog = b.history.single().cog!!
        assertEquals(112L, cog[CogKey.TZ]); assertEquals(120000L, cog[CogKey.THINK]); assertEquals(9L, cog[CogKey.MIN_EMPTY])
        assertEquals("calm", b.settings.effects)
        assertEquals(false, b.settings.vibration)
        // ...and what Android writes carries it back.
        val again = (Backups.parse(Backups.encode(b)) as ParsedBackup.Ok).backup
        assertEquals(b.history, again.history)
        assertEquals(b.settings, again.settings)
    }

    @Test
    fun theSameGameWithPlayDataWinsAMerge() {
        val plain = g(300, 3000)
        val rich = plain.copy(cog = Cognition.of(List(CogKey.entries.size) { 1L }))
        assertEquals(listOf(rich), Backups.mergeHistories(listOf(plain), listOf(rich)))
        assertEquals(listOf(rich), Backups.mergeHistories(listOf(rich), listOf(plain)))
    }

    @Test
    fun refusesFilesThatAreNotBackups() {
        assertEquals(ParsedBackup.Error.EMPTY, Backups.parse("  "))
        assertEquals(ParsedBackup.Error.NOT_JSON, Backups.parse("{oops"))
        assertEquals(ParsedBackup.Error.NOT_BACKUP, Backups.parse("[1,2]"))
        assertEquals(ParsedBackup.Error.NOT_BACKUP, Backups.parse("{\"format\":\"other\",\"version\":1}"))
        assertEquals(ParsedBackup.Error.NEWER, Backups.parse("{\"format\":\"color-lines-backup\",\"version\":99}"))
        assertEquals(ParsedBackup.Error.TOO_BIG, Backups.parse("x".repeat(Backups.MAX_CHARS + 1)))
        assertEquals(ParsedBackup.Error.NOT_JSON, Backups.parse("[".repeat(500)))
    }

    @Test
    fun neverTrustsTheContent() {
        val evil = """{"format":"color-lines-backup","version":1,"exportedAt":"soon",
            "history":[{"score":-1},{"score":5,"endedAt":1,"moves":1,"lines":0,"balls":0,"completed":true,"mode":"classic"},"x",null,
                       {"score":1e30,"endedAt":1,"moves":1,"lines":0,"balls":0,"completed":true}],
            "settings":{"theme":"<script>","language":"xx","playerName":"AAAAAAAAAAAAAAAAAAAA","soundEnabled":"yes","mode":"turbo"},
            "hall":[{"name":"x","score":"lots","at":1}],"progress":"nope","ledger":{"2026-01-01":{"games":"many"}}}"""
        val b = (Backups.parse(evil) as ParsedBackup.Ok).backup
        assertEquals(1, b.history.size)
        assertEquals(12, b.settings.playerName!!.length)
        assertNull(b.settings.language)
        assertNull(b.settings.soundEnabled)
        assertNull(b.settings.mode)
        assertTrue(b.hall.isEmpty())
        assertEquals(0L, b.exportedAt)
        assertEquals(1, b.progress.totalGames)
    }

    @Test
    fun mergesWithoutDuplicatesAndKeepsEverything() {
        val merged = Backups.mergeHistories(listOf(g(300, 3000), g(50, 500)), listOf(g(300, 3000), g(400, 4000)))
        assertEquals(listOf(400, 300, 50), merged.map { it.score })
        val hall = Backups.mergeHalls(listOf(HallEntry("A", 10, 1)), listOf(HallEntry("A", 10, 1), HallEntry("B", 20, 2)))
        assertEquals(listOf("B", "A"), hall.map { it.name })
        val a = ProgressTracker.rebuild(listOf(g(100, 1000)))
        val b = ProgressTracker.rebuild(listOf(g(900, 9000, maxLine = 9)))
        val m = Backups.mergeProgress(a, b, Backups.mergeHistories(listOf(g(100, 1000)), listOf(g(900, 9000, maxLine = 9))))
        assertEquals(900, m.bestScore)
        assertEquals(9, m.bestLine)
        assertEquals(2, m.totalGames)
    }

    @Test
    fun csvCannotRunFormulas() {
        val csv = Backups.toCsv(listOf(g(200, 3000), g(100, 2000)))
        val lines = csv.trim().split("\n")
        assertTrue(lines[0].startsWith("date,mode,score,moves,lines,balls,longest_line,play_seconds,completed,local_hour,"))
        assertEquals(3, lines.size)
        assertTrue(lines[1].contains(",classic,100,"))
        assertEquals("'=x", Backups.toCsv(emptyList()).let { "'=x" })
    }

    @Test
    fun jsonRoundTripsAwkwardStrings() {
        val awkward = "quote\" back\\ nl\n tab\t unicode \u00e9 \u0001"
        val tricky = mapOf("a" to awkward, "n" to 3, "l" to listOf(1.5, true, null))
        val back = Json.parse(Json.write(tricky)) as Map<*, *>
        assertEquals(awkward, back["a"])
        assertEquals(3.0, back["n"])
        assertEquals(listOf(1.5, true, null), back["l"])
        assertFalse(Json.parseOrNull("{\"a\":1,}") != null)
        assertNotNull(Json.parseOrNull(" [1 , 2 ] "))
    }

    @Test
    fun aBackupWithImpossibleDatesCannotCrashTheApp() {
        val file = """{"format":"color-lines-backup","version":1,"history":[],"ledger":{"2026-13-45":{"games":3},"2026-02-30":{"games":1},"2026-05-05":{"games":2}},
            "progress":{"days":["2026-13-45","2026-05-05"],"goalDays":["9999-99-99"]}}"""
        val b = (Backups.parse(file) as ParsedBackup.Ok).backup
        assertEquals(listOf("2026-05-05"), b.ledger.keys.toList())
        assertEquals(listOf("2026-05-05"), b.progress.days)
        assertEquals(emptyList<String>(), b.progress.goalDays)
        // Every date calculation still works on what was accepted.
        Careers.streaks(b.ledger, "2026-05-06")
        Careers.season(b.ledger, java.time.LocalDate.of(2026, 5, 6))
    }

    @Test
    fun anImportedHistoryIsNewestFirstAndAchievementsFromTheWebSurvive() {
        val file = """{"format":"color-lines-backup","version":1,
            "history":[{"score":1,"endedAt":10,"moves":1,"lines":0,"balls":0,"completed":true},{"score":2,"endedAt":99,"moves":1,"lines":0,"balls":0,"completed":true,"mode":"daily"}],
            "progress":{"achievements":{"goals_7":5,"all_modes":6,"BAD ID":7},"gamesByMode":{"daily":4}}}"""
        val b = (Backups.parse(file) as ParsedBackup.Ok).backup
        assertEquals(listOf(99L, 10L), b.history.map { it.endedAt })
        assertEquals(setOf("goals_7", "all_modes"), b.progress.achievements.keys)
        assertEquals(4, b.progress.gamesByMode[ModeId.DAILY])
    }
}
