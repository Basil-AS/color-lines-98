package io.github.basil_as.basillines.engine

import kotlin.math.hypot
import kotlin.math.pow

/**
 * The look studio, shared by the Android app and the tests: every theme is a starting point and the player can switch
 * depth, ring, markers, colour set, ball size and grid style on top of it, per theme. A choice that is not set follows
 * the theme. The same ids and the same share code are used by the web app. Colours are 0xAARRGGBB ints.
 */
enum class Volume(val id: String) { FLAT("flat"), SOFT("soft"), GLOSSY("glossy") }
enum class RingLevel(val id: String) { NORMAL("normal"), HIGH("high"), MAX("max") }
enum class ShapeSet(val id: String) { OFF("off"), SHAPES("shapes"), DIGITS("digits") }
enum class PaletteId(val id: String) { CLASSIC("classic"), VIVID("vivid"), DISTINCT("distinct"), SOFT("soft"), CUSTOM("custom") }
enum class BallSize(val id: String, val scale: Float) { COMPACT("compact", 0.68f), NORMAL("normal", 0.82f), LARGE("large", 0.94f) }
enum class GridStyle(val id: String) { PLAIN("plain"), NONE("none"), THIN("thin"), BOLD("bold"), CHECKER("checker"), CHECKER_LINES("checkerLines"), DOTS("dots") }
enum class CellShape(val id: String) { SQUARE("square"), ROUNDED("rounded"), ROUND("round") }

data class Look(
    val volume: Volume,
    val ring: RingLevel,
    val shapes: ShapeSet,
    val palette: PaletteId,
    val ballSize: BallSize,
    val grid: GridStyle,
    val cells: CellShape
)

/** What the player has changed; null follows the theme. */
data class LookOverrides(
    val volume: Volume? = null,
    val ring: RingLevel? = null,
    val shapes: ShapeSet? = null,
    val palette: PaletteId? = null,
    val ballSize: BallSize? = null,
    val grid: GridStyle? = null,
    val cells: CellShape? = null
) {
    val isEmpty: Boolean get() = this == LookOverrides()
}

object LookSpec {
    /** The theme ids of the web app, in the order of its picker. */
    val THEME_IDS = listOf(
        "modern", "light", "material", "neon", "synthwave", "ocean", "paper", "gameboy", "terminal", "contrast", "clear",
        "clearNight", "retro98", "spectrum", "meadow", "orchid", "custom", "lines98",
        "lines98plus", "colorlines92"
    )

    /** Themes by collection, in the order of the picker; every theme is in exactly one. */
    val FAMILIES: List<Pair<String, List<String>>> = listOf(
        "modern" to listOf("modern", "light", "material", "ocean", "paper"),
        "vivid" to listOf("clear", "clearNight", "spectrum", "meadow", "orchid"),
        "neon" to listOf("neon", "synthwave", "retro98"),
        "mono" to listOf("gameboy", "terminal", "contrast"),
        "retro" to listOf("lines98", "lines98plus", "colorlines92"),
        "own" to listOf("custom")
    )

    /** The balls of the original games and of the DOS look are artwork, so the studio leaves them alone. */
    fun isStudioTheme(themeId: String): Boolean = themeId != "lines98" && themeId != "lines98plus" && themeId != "colorlines92"

    fun defaults(themeId: String): Look {
        val base = Look(Volume.SOFT, RingLevel.NORMAL, ShapeSet.OFF, PaletteId.VIVID, BallSize.NORMAL, GridStyle.PLAIN, CellShape.ROUNDED)
        return when (themeId) {
            "modern", "light", "material", "ocean", "paper" -> base.copy(volume = Volume.GLOSSY, palette = PaletteId.CLASSIC)
            "neon", "synthwave" -> base.copy(volume = Volume.GLOSSY)
            "contrast", "clear", "clearNight", "retro98", "spectrum", "meadow", "orchid" ->
                base.copy(volume = Volume.FLAT, ring = RingLevel.HIGH, cells = CellShape.SQUARE)
            "gameboy", "terminal" -> base.copy(volume = Volume.FLAT, ring = RingLevel.HIGH, shapes = ShapeSet.SHAPES, cells = CellShape.SQUARE)
            else -> base
        }
    }

    fun effective(themeId: String, o: LookOverrides?): Look {
        val d = defaults(themeId)
        if (o == null) return d
        return Look(
            o.volume ?: d.volume, o.ring ?: d.ring, o.shapes ?: d.shapes, o.palette ?: d.palette,
            o.ballSize ?: d.ballSize, o.grid ?: d.grid, o.cells ?: d.cells
        )
    }

    /** The studio draws the balls (depth and colours) for the custom theme and once depth or colours are chosen. */
    fun studioActive(themeId: String, o: LookOverrides?): Boolean =
        isStudioTheme(themeId) && (themeId == "custom" || o?.volume != null || o?.palette != null)

    /** The colour set the studio draws with; the custom theme uses its own balls unless a set is chosen. */
    fun paletteInUse(themeId: String, o: LookOverrides?): PaletteId =
        if (themeId == "custom" && o?.palette == null) PaletteId.CUSTOM else effective(themeId, o).palette
}

object Colors {
    fun argb(hex: String): Int? {
        val h = hex.trim().removePrefix("#")
        val full = when (h.length) {
            3 -> h.map { "$it$it" }.joinToString("")
            6 -> h
            else -> return null
        }
        val v = full.toLongOrNull(16) ?: return null
        return (0xFF000000L or v).toInt()
    }

    fun hex(color: Int): String = "#%06x".format(color and 0xFFFFFF)

    fun red(c: Int) = (c shr 16) and 0xFF
    fun green(c: Int) = (c shr 8) and 0xFF
    fun blue(c: Int) = c and 0xFF
    fun rgb(r: Int, g: Int, b: Int): Int = (0xFF shl 24) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)

    /** [amount] of [b] mixed into [a]; 0 is a, 1 is b. */
    fun mix(a: Int, b: Int, amount: Float): Int {
        val t = amount.coerceIn(0f, 1f)
        fun ch(x: Int, y: Int) = Math.round(x + (y - x) * t)
        return rgb(ch(red(a), red(b)), ch(green(a), green(b)), ch(blue(a), blue(b)))
    }

    fun luminance(c: Int): Double {
        fun lin(v: Int): Double {
            val s = v / 255.0
            return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * lin(red(c)) + 0.7152 * lin(green(c)) + 0.0722 * lin(blue(c))
    }

    fun contrast(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    fun isDark(c: Int): Boolean = luminance(c) < 0.36

    const val BLACK: Int = 0xFF000000.toInt()
    const val WHITE: Int = 0xFFFFFFFF.toInt()

    /** Black or white, whichever reads better on the colour. */
    fun inkOn(c: Int): Int = if (contrast(c, BLACK) >= contrast(c, WHITE)) BLACK else WHITE

    /** Moves [fg] towards black or white until it reaches [ratio] against [bg]. */
    fun ensureContrast(fg: Int, bg: Int, ratio: Double): Int {
        if (contrast(fg, bg) >= ratio) return fg
        val target = if (isDark(bg)) WHITE else BLACK
        for (step in 1..20) {
            val candidate = mix(fg, target, step / 20f)
            if (contrast(candidate, bg) >= ratio) return candidate
        }
        return target
    }

    /** Straight-line distance in RGB: crude, but enough to tell "alike" from "different". */
    fun distance(a: Int, b: Int): Double =
        hypot(hypot((red(a) - red(b)).toDouble(), (green(a) - green(b)).toDouble()), (blue(a) - blue(b)).toDouble())
}

/** One ball colour as the studio draws it: the body, a highlight, a shade and the ink for a marker on it. */
data class BallTone(val base: Int, val hi: Int, val lo: Int, val ink: Int)

object LookPalettes {
    private fun c(hex: String): Int = Colors.argb(hex)!!

    private fun colors(red: String, green: String, blue: String, cyan: String, magenta: String, yellow: String, brown: String): Map<BallColor, Int> =
        mapOf(
            BallColor.RED to c(red), BallColor.GREEN to c(green), BallColor.BLUE to c(blue), BallColor.CYAN to c(cyan),
            BallColor.MAGENTA to c(magenta), BallColor.YELLOW to c(yellow), BallColor.BROWN to c(brown)
        )

    /** Vivid keeps red and violet far apart; Distinct is the Okabe-Ito colour-blind safe set spread over lightness. */
    val SETS: Map<PaletteId, Map<BallColor, Int>> = mapOf(
        PaletteId.CLASSIC to colors("#e53935", "#43a047", "#1e88e5", "#00acc1", "#8e24aa", "#fdd835", "#6d4c41"),
        PaletteId.VIVID to colors("#f0261b", "#2fbf4a", "#2f7bff", "#00d8f0", "#8b3dff", "#ffd60a", "#a9703a"),
        PaletteId.DISTINCT to colors("#d55e00", "#009e73", "#0072b2", "#56b4e9", "#cc79a7", "#f0e442", "#4a2f1b"),
        PaletteId.SOFT to colors("#e0675f", "#6fb97a", "#6f9de0", "#6cc7d6", "#a98bd6", "#eed26a", "#a98467")
    )

    /** Highlight and shade stay close to the colour itself, so a ball looks lit rather than whitened or dirtied. */
    fun tone(base: Int): BallTone = BallTone(base, Colors.mix(base, Colors.WHITE, 0.34f), Colors.mix(base, Colors.BLACK, 0.32f), Colors.inkOn(base))

    fun tones(palette: PaletteId, custom: Map<BallColor, Int>): Map<BallColor, BallTone> {
        val set = if (palette == PaletteId.CUSTOM) custom else SETS.getValue(palette)
        return BallColor.entries.associateWith { tone(set[it] ?: SETS.getValue(PaletteId.VIVID).getValue(it)) }
    }

    /** The marker on a ball: a digit for each colour, or one of seven shapes (drawn by the app). */
    fun digit(color: BallColor): String = color.id.toString()
}

/** The colours the builder asks for; everything else is derived. */
data class CustomTheme(
    val dark: Boolean,
    val page: Int,
    val board: Int,
    val cell: Int,
    val accent: Int,
    val balls: Map<BallColor, Int>
) {
    companion object {
        /** A fresh custom look starts with Vivid balls, the yellow dark enough to stand out on a light cell. */
        val DEFAULT = CustomTheme(
            false, Colors.argb("#e8eef2")!!, Colors.argb("#3f5566")!!, Colors.argb("#f2f6f8")!!, Colors.argb("#2a7ab0")!!,
            LookPalettes.SETS.getValue(PaletteId.VIVID) + (BallColor.YELLOW to Colors.argb("#f2b705")!!)
        )

        fun preset(dark: Boolean): CustomTheme = if (dark) {
            CustomTheme(
                true, Colors.argb("#14181d")!!, Colors.argb("#0b0e11")!!, Colors.argb("#262d35")!!, Colors.argb("#4fd1a5")!!,
                LookPalettes.SETS.getValue(PaletteId.VIVID)
            )
        } else DEFAULT
    }
}

/** Every colour the interface needs, worked out from a [CustomTheme]. */
data class CustomColors(
    val page: Int, val panel: Int, val border: Int, val text: Int, val muted: Int, val accent: Int, val onAccent: Int,
    val board: Int, val cell: Int, val stat: Int, val ctrl: Int, val danger: Int, val dark: Boolean
)

fun CustomTheme.colors(): CustomColors {
    val toward = if (dark) Colors.WHITE else Colors.BLACK
    val away = if (dark) Colors.BLACK else Colors.WHITE
    val panel = Colors.mix(page, away, if (dark) 0.04f else 0.55f)
    val text = Colors.argb(if (dark) "#eef1f4" else "#1d242b")!!
    val acc = Colors.ensureContrast(accent, panel, 3.0)
    val ctrl = Colors.mix(panel, toward, if (dark) 0.1f else 0.06f)
    val stat = Colors.mix(page, toward, if (dark) 0.12f else 0.06f)
    val card = Colors.mix(panel, toward, 0.04f)
    // Muted text sits on the panel, the stat boxes, the controls and the cards: it must read on every one of them.
    val muted = listOf(panel, stat, ctrl, card).fold(Colors.mix(text, page, 0.35f)) { c, bg -> Colors.ensureContrast(c, bg, 4.5) }
    return CustomColors(
        page = page, panel = panel, border = Colors.mix(panel, toward, 0.16f), text = text, muted = muted, accent = acc,
        onAccent = Colors.inkOn(acc), board = board, cell = cell, stat = stat,
        ctrl = ctrl, danger = Colors.argb(if (dark) "#ff6b6b" else "#b4232c")!!, dark = dark
    )
}

/** What is wrong with a set of ball colours: pairs that look alike, and colours that get lost against the cells. */
data class BallWarnings(val similar: List<Pair<BallColor, BallColor>>, val faint: List<BallColor>) {
    val isClean: Boolean get() = similar.isEmpty() && faint.isEmpty()
}

fun ballWarnings(balls: Map<BallColor, Int>, cell: Int): BallWarnings {
    val all = BallColor.entries
    val similar = buildList {
        for (i in all.indices) for (j in i + 1 until all.size) {
            val a = balls[all[i]] ?: continue
            val b = balls[all[j]] ?: continue
            if (Colors.distance(a, b) < 70.0) add(all[i] to all[j])
        }
    }
    val faint = all.filter { (balls[it]?.let { c -> Colors.contrast(c, cell) } ?: 21.0) < 1.5 }
    return BallWarnings(similar, faint)
}

/** Parsing and writing of the studio's data. Anything that is not a known option is dropped. */
object LookStore {
    private fun <T> pick(raw: Any?, options: Array<T>, id: (T) -> String): T? = options.firstOrNull { id(it) == raw }

    fun overridesFrom(raw: Any?): LookOverrides {
        val o = raw as? Map<*, *> ?: return LookOverrides()
        return LookOverrides(
            volume = pick(o["volume"], Volume.entries.toTypedArray()) { it.id },
            ring = pick(o["contrast"], RingLevel.entries.toTypedArray()) { it.id },
            shapes = pick(o["shapes"], ShapeSet.entries.toTypedArray()) { it.id },
            palette = pick(o["palette"], PaletteId.entries.toTypedArray()) { it.id },
            ballSize = pick(o["ballSize"], BallSize.entries.toTypedArray()) { it.id },
            grid = pick(o["grid"], GridStyle.entries.toTypedArray()) { it.id },
            cells = pick(o["cells"], CellShape.entries.toTypedArray()) { it.id }
        )
    }

    /** The web app calls the ring "contrast"; the file formats keep its names. */
    fun overridesTo(o: LookOverrides): Map<String, Any?> = buildMap {
        o.volume?.let { put("volume", it.id) }
        o.ring?.let { put("contrast", it.id) }
        o.shapes?.let { put("shapes", it.id) }
        o.palette?.let { put("palette", it.id) }
        o.ballSize?.let { put("ballSize", it.id) }
        o.grid?.let { put("grid", it.id) }
        o.cells?.let { put("cells", it.id) }
    }

    fun storeFrom(raw: Any?): Map<String, LookOverrides> {
        val o = raw as? Map<*, *> ?: return emptyMap()
        val out = LinkedHashMap<String, LookOverrides>()
        for (id in LookSpec.THEME_IDS) {
            val overrides = overridesFrom(o[id])
            if (!overrides.isEmpty) out[id] = overrides
        }
        return out
    }

    fun storeTo(store: Map<String, LookOverrides>): Map<String, Any?> =
        store.filterValues { !it.isEmpty }.mapValues { overridesTo(it.value) }

    private val BALL_KEYS = mapOf(
        BallColor.RED to "red", BallColor.GREEN to "green", BallColor.BLUE to "blue", BallColor.CYAN to "cyan",
        BallColor.MAGENTA to "magenta", BallColor.YELLOW to "yellow", BallColor.BROWN to "brown"
    )

    fun customFrom(raw: Any?): CustomTheme {
        val d = CustomTheme.DEFAULT
        val o = raw as? Map<*, *> ?: return d
        fun color(key: String, fallback: Int): Int = (o[key] as? String)?.let { Colors.argb(it) } ?: fallback
        val balls = o["balls"] as? Map<*, *>
        return CustomTheme(
            dark = o["dark"] as? Boolean ?: d.dark,
            page = color("page", d.page), board = color("board", d.board), cell = color("cell", d.cell), accent = color("accent", d.accent),
            balls = BallColor.entries.associateWith { ball -> (balls?.get(BALL_KEYS.getValue(ball)) as? String)?.let { Colors.argb(it) } ?: d.balls.getValue(ball) }
        )
    }

    fun customTo(c: CustomTheme): Map<String, Any?> = mapOf(
        "dark" to c.dark, "page" to Colors.hex(c.page), "board" to Colors.hex(c.board), "cell" to Colors.hex(c.cell), "accent" to Colors.hex(c.accent),
        "balls" to BallColor.entries.associate { BALL_KEYS.getValue(it) to Colors.hex(c.balls.getValue(it)) }
    )
}

/** A short text that carries a look from one device to another; the web app writes and reads the same format. */
data class LookShare(val themeId: String, val overrides: LookOverrides, val custom: CustomTheme?)

object LookCode {
    private const val PREFIX = "CL3:"
    private const val MAX_LENGTH = 4000

    fun encode(share: LookShare): String {
        val body = buildMap<String, Any?> {
            put("v", 1)
            put("theme", share.themeId)
            put("look", LookStore.overridesTo(share.overrides))
            if (share.themeId == "custom" && share.custom != null) put("custom", LookStore.customTo(share.custom))
        }
        return PREFIX + java.util.Base64.getEncoder().encodeToString(Json.write(body).toByteArray(Charsets.UTF_8))
    }

    fun decode(code: String): LookShare? {
        val text = code.trim()
        if (!text.startsWith(PREFIX) || text.length > MAX_LENGTH) return null
        return try {
            val json = String(java.util.Base64.getDecoder().decode(text.removePrefix(PREFIX)), Charsets.UTF_8)
            val raw = Json.parseOrNull(json) as? Map<*, *> ?: return null
            val theme = raw["theme"] as? String ?: return null
            if (theme !in LookSpec.THEME_IDS) return null
            LookShare(theme, LookStore.overridesFrom(raw["look"]), if (theme == "custom") LookStore.customFrom(raw["custom"]) else null)
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
