package io.github.basil_as.basillines

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import io.github.basil_as.basillines.engine.BallColor
import io.github.basil_as.basillines.engine.Point
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The life of the board (mirrors src/effects.ts): balls that travel along their path and land with a squash, sparks when a
 * line goes, floating points, shaking on a big one, a ring where you touch, a nervous glow when the board is nearly full.
 * Everything is drawn over the board from one clock and never changes the game.
 */
enum class EffectsLevel(val id: String) {
    OFF("off"), CALM("calm"), FULL("full");

    companion object {
        fun parse(raw: String?) = entries.firstOrNull { it.id == raw }
    }
}

data class MoveFx(
    /** Distinguishes one move from the next, so the animation restarts. */
    val id: Long,
    val path: List<Point>,
    val color: BallColor,
    val spawned: List<Pair<Point, BallColor>>,
    val cleared: List<Pair<Point, BallColor>>,
    val points: Int,
    /** Consecutive clearing moves, this one included. */
    val combo: Int,
    /** A shower over the whole board (a record), no move. */
    val celebrate: Boolean = false
) {
    val travelMs: Float get() = travelMs(path.size - 1)
    /** The whole animation lasts this long. */
    val totalMs: Float get() = if (celebrate) 2400f else travelMs + 1100f
}

fun travelMs(steps: Int): Float = (steps * 45f).coerceIn(150f, 460f)

fun sparkCount(points: Int, level: EffectsLevel): Int = minOf(if (level == EffectsLevel.FULL) 26 else 12, 8 + Math.round(points / 4f))

private fun sparkColor(c: BallColor): Color = when (c) {
    BallColor.RED -> Color(0xFFFF5252)
    BallColor.GREEN -> Color(0xFF4CAF50)
    BallColor.BLUE -> Color(0xFF448AFF)
    BallColor.CYAN -> Color(0xFF18FFFF)
    BallColor.MAGENTA -> Color(0xFFE040FB)
    BallColor.YELLOW -> Color(0xFFFFEB3B)
    BallColor.BROWN -> Color(0xFFA1663A)
}

/** A repeatable pseudo-random number in 0..1 for the spark [i] of the effect [seed] (the animation is a function of time). */
private fun rnd(seed: Int, i: Int, salt: Int): Float {
    var h = seed * 73856093 xor i * 19349663 xor salt * 83492791
    h = h xor (h ushr 13); h *= 1274126177; h = h xor (h ushr 16)
    return (h and 0xFFFF) / 65535f
}

private fun easeOutBack(x: Float): Float {
    val c1 = 1.70158f; val c3 = c1 + 1
    val t = x - 1
    return 1 + c3 * t * t * t + c1 * t * t
}

/** What the board draws differently while an effect plays. */
class FxFrame(val fx: MoveFx?, val t: Float, val level: EffectsLevel) {
    private val active get() = fx != null && level != EffectsLevel.OFF && t < fx.totalMs && !fx.celebrate
    private val travel get() = fx?.travelMs ?: 0f

    /** The ball seat at [p] stays empty until the travelling ball arrives. */
    fun hidden(p: Point): Boolean = active && fx!!.path.lastOrNull() == p && t < travel

    /** How big the ball at [p] is drawn: popping in for new balls, squashing on landing. Returns (scaleX, scaleY). */
    fun scaleOf(p: Point): Pair<Float, Float> {
        val f = fx ?: return 1f to 1f
        if (!active) return 1f to 1f
        if (f.spawned.any { it.first == p }) {
            val x = ((t - travel - 60f) / 500f)
            val s = if (x <= 0f) 0f else if (x >= 1f) 1f else easeOutBack(x)
            return s to s
        }
        if (f.path.lastOrNull() == p && t >= travel && t < travel + 360f) {
            val x = (t - travel) / 360f
            return if (x < 0.45f) (1.18f - 0.26f * (x / 0.45f)) to (0.78f + 0.32f * (x / 0.45f)) else (0.92f + 0.08f * ((x - 0.45f) / 0.55f)) to (1.1f - 0.1f * ((x - 0.45f) / 0.55f))
        }
        return 1f to 1f
    }

    /** The shake of the whole board, in cell widths. */
    fun shake(): Offset {
        val f = fx ?: return Offset.Zero
        if (!active || level != EffectsLevel.FULL || f.cleared.isEmpty() || !(f.points >= 18 || f.combo >= 2)) return Offset.Zero
        val x = (t - travel) / (if (f.points >= 42) 480f else 340f)
        if (x < 0f || x > 1f) return Offset.Zero
        val amp = (if (f.points >= 42) 0.12f else 0.05f) * (1f - x)
        return Offset(sin(x * 6 * PI).toFloat() * amp, cos(x * 5 * PI).toFloat() * amp * 0.4f)
    }
}

/** Draws the moving parts of an effect over the cells. [drawBall] paints a ball of a colour at a centre with a radius. */
fun DrawScope.drawFx(
    frame: FxFrame,
    cell: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer?,
    drawBall: (BallColor, Offset, Float) -> Unit
) {
    val fx = frame.fx ?: return
    val t = frame.t
    val level = frame.level
    if (level == EffectsLevel.OFF || t >= fx.totalMs) return
    fun centre(p: Point) = Offset((p.x + 0.5f) * cell, (p.y + 0.5f) * cell)

    if (fx.celebrate) {
        if (level != EffectsLevel.FULL) return
        val palette = BallColor.entries.map { sparkColor(it) }
        for (i in 0 until 70) {
            val delay = rnd(7, i, 1) * 300f
            val tau = (t - delay) / 1000f
            if (tau < 0f) continue
            val life = 1.4f + rnd(7, i, 2) * 0.8f
            if (tau > life) continue
            val x = rnd(7, i, 3) * size.width + (rnd(7, i, 4) - 0.5f) * 60f * tau
            val y = -10f + (40f + rnd(7, i, 5) * 120f) * tau * cell / 40f + 120f * tau * tau * cell / 40f / 2
            drawCircle(palette[i % palette.size].copy(alpha = 1f - tau / life), (2f + rnd(7, i, 6) * 3f) * cell / 40f, Offset(x, y))
        }
        return
    }

    val travel = fx.travelMs
    // The ball on its way (and a trail of fading dots in the full mode).
    if (t < travel && fx.path.size > 1) {
        val steps = fx.path.size - 1
        val pos = (t / travel).coerceIn(0f, 1f).let { it * it * (3 - 2 * it) } * steps
        val i = pos.toInt().coerceAtMost(steps - 1)
        val a = centre(fx.path[i]); val b = centre(fx.path[i + 1])
        val at = Offset(a.x + (b.x - a.x) * (pos - i), a.y + (b.y - a.y) * (pos - i))
        if (level == EffectsLevel.FULL) {
            for (k in 0..i) {
                val age = (pos - k).coerceAtLeast(0f)
                drawCircle(sparkColor(fx.color).copy(alpha = (0.55f - age * 0.12f).coerceAtLeast(0f)), cell * 0.1f, centre(fx.path[k]))
            }
        }
        drawBall(fx.color, at, cell * 0.38f * 0.94f)
    }
    // Balls about to burn wait where they were until the mover arrives.
    if (t < travel) for ((p, color) in fx.cleared) if (p != fx.path.lastOrNull()) drawBall(color, centre(p), cell * 0.38f)

    if (t >= travel && fx.cleared.isNotEmpty()) {
        val tau = (t - travel) / 1000f
        val per = sparkCount(fx.points, level)
        for ((n, entry) in fx.cleared.withIndex()) {
            val (p, color) = entry
            val c = centre(p)
            for (i in 0 until per / 2 + 3) {
                val life = 0.45f + rnd(n, i, 1) * 0.35f
                if (tau > life) continue
                val ang = rnd(n, i, 2) * 2 * PI.toFloat()
                val v = (40f + rnd(n, i, 3) * (if (level == EffectsLevel.FULL) 150f else 90f)) * cell / 40f
                val x = c.x + cos(ang) * v * tau
                val y = c.y + (sin(ang) * v - 30f * cell / 40f) * tau + 130f * cell / 40f * tau * tau
                val k = 1f - tau / life
                drawCircle(sparkColor(color).copy(alpha = k), (2f + rnd(n, i, 4) * 3f) * cell / 40f * (0.4f + k * 0.6f), Offset(x, y))
            }
            if (tau < 0.35f) {
                val k = 1f - tau / 0.35f
                drawCircle(Color.White.copy(alpha = k), cell * 0.3f * (1f + (1f - k) * 1.4f), c, style = Stroke(width = 2f))
            }
        }
        if (level == EffectsLevel.FULL && textMeasurer != null) {
            val mid = fx.cleared.map { centre(it.first) }.let { l -> Offset(l.sumOf { it.x.toDouble() }.toFloat() / l.size, l.sumOf { it.y.toDouble() }.toFloat() / l.size) }
            fun pop(text: String, at: Offset, color: Color, delay: Float, sizeSp: Float) {
                val x = (t - travel - delay) / 1050f
                if (x < 0f || x > 1f) return
                val alpha = if (x < 0.15f) x / 0.15f else 1f - (x - 0.15f) / 0.85f
                val scale = if (x < 0.15f) 0.5f + x / 0.15f * 0.75f else 1.25f - (x - 0.15f) * 0.3f
                val layout = textMeasurer.measure(text, androidx.compose.ui.text.TextStyle(fontSize = sizeSp.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold))
                val origin = Offset(at.x - layout.size.width / 2f, at.y - layout.size.height / 2f - x * cell * 2f)
                withTransform({ scale(scale, scale, Offset(at.x, at.y - x * cell * 2f)) }) {
                    drawText(layout, Color.Black.copy(alpha = alpha * 0.7f), origin + Offset(2f, 3f))
                    drawText(layout, color.copy(alpha = alpha), origin)
                }
            }
            pop("+${fx.points}", mid, if (fx.points >= 28) Color(0xFFFFEB3B) else Color.White, 0f, if (fx.points >= 28) 30f else 24f)
            if (fx.combo >= 2) pop("×${fx.combo}", Offset(mid.x, mid.y - cell * 0.9f), Color(0xFFFF80AB), 140f, 26f)
        }
    }
}

// ---- touch feedback ---------------------------------------------------------------------------

data class HapticStep(
    val durationMs: Long,
    val amplitude: Int,
    val gapMs: Long = 0L
) {
    fun scaled(scale: Float): HapticStep =
        copy(amplitude = scaleAmplitude(amplitude, scale))
}

fun scaleAmplitude(amplitude: Int, scale: Float): Int {
    return (amplitude * scale).roundToInt().coerceIn(1, 255)
}

val HAPTIC_PATTERNS: Map<HapticKind, List<HapticStep>> = mapOf(
    HapticKind.SELECT to listOf(
        HapticStep(14L, 110)
    ),
    HapticKind.MOVE to listOf(
        HapticStep(18L, 140)
    ),
    HapticKind.BLOCKED to listOf(
        HapticStep(40L, 220, 50L),
        HapticStep(40L, 220)
    ),
    HapticKind.CLEAR to listOf(
        HapticStep(22L, 120, 12L),
        HapticStep(22L, 180, 12L),
        HapticStep(26L, 240)
    ),
    HapticKind.BIG_CLEAR to listOf(
        HapticStep(20L, 130, 12L),
        HapticStep(24L, 180, 12L),
        HapticStep(28L, 220, 16L),
        HapticStep(50L, 255)
    ),
    HapticKind.COMBO to listOf(
        HapticStep(16L, 120, 40L),
        HapticStep(16L, 150, 30L),
        HapticStep(18L, 185, 20L),
        HapticStep(20L, 220, 14L),
        HapticStep(24L, 255)
    ),
    HapticKind.DANGER to listOf(
        HapticStep(60L, 200, 100L),
        HapticStep(60L, 200)
    ),
    HapticKind.GAME_OVER to listOf(
        HapticStep(80L, 240, 15L),
        HapticStep(80L, 180, 15L),
        HapticStep(80L, 120, 15L),
        HapticStep(70L, 60)
    ),
    HapticKind.RECORD to listOf(
        HapticStep(25L, 220, 30L),
        HapticStep(25L, 220, 30L),
        HapticStep(25L, 240, 45L),
        HapticStep(40L, 160, 10L),
        HapticStep(60L, 210, 10L),
        HapticStep(100L, 255)
    ),
    HapticKind.HINT to listOf(
        HapticStep(12L, 90, 45L),
        HapticStep(12L, 90)
    ),
    HapticKind.UNDO to listOf(
        HapticStep(10L, 70, 10L),
        HapticStep(16L, 130)
    )
)

fun stepsToTimings(steps: List<HapticStep>): LongArray {
    val timings = ArrayList<Long>(steps.size * 2)
    for (i in steps.indices) {
        val s = steps[i]
        timings.add(s.durationMs)
        if (s.gapMs > 0L && i < steps.size - 1) {
            timings.add(s.gapMs)
        }
    }
    return timings.toLongArray()
}

fun stepsToWaveform(steps: List<HapticStep>, intensity: Float = 1.0f): Pair<LongArray, IntArray> {
    val timings = ArrayList<Long>(steps.size * 2)
    val amplitudes = ArrayList<Int>(steps.size * 2)
    for (i in steps.indices) {
        val s = steps[i]
        timings.add(s.durationMs)
        amplitudes.add(scaleAmplitude(s.amplitude, intensity))
        if (s.gapMs > 0L && i < steps.size - 1) {
            timings.add(s.gapMs)
            amplitudes.add(0)
        }
    }
    return timings.toLongArray() to amplitudes.toIntArray()
}

enum class HapticKind {
    SELECT, MOVE, BLOCKED, CLEAR, BIG_CLEAR, COMBO, DANGER, GAME_OVER, RECORD, HINT, UNDO;

    val steps: List<HapticStep> get() = HAPTIC_PATTERNS.getValue(this)
    val pattern: LongArray get() = stepsToTimings(steps)
    val totalDurationMs: Long get() = steps.sumOf { it.durationMs + it.gapMs }
}

/** Vibration patterns for game events (the same table as src/haptics.ts). */
class Haptics(context: Context) {
    var enabled: Boolean = true
    var intensity: Float = 1.0f

    private val vibrator: Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= 31) (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        else @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }.getOrNull()

    val supported: Boolean get() = vibrator?.hasVibrator() == true

    fun play(kind: HapticKind, intensity: Float = this.intensity) {
        val v = vibrator ?: return
        if (!enabled || !v.hasVibrator()) return
        // A game must never fail because the vibrator does (a battery saver, a missing permission).
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (tryPlayComposition(v, kind, intensity)) return
            }

            if (v.hasAmplitudeControl()) {
                val (timings, amplitudes) = stepsToWaveform(kind.steps, intensity)
                v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                // Waveform timings are "wait, buzz, wait, buzz..." so the pattern starts with no wait.
                val timings = longArrayOf(0) + kind.pattern
                v.vibrate(VibrationEffect.createWaveform(timings, -1))
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun tryPlayComposition(v: Vibrator, kind: HapticKind, scale: Float): Boolean {
        val isCompositionKind = when (kind) {
            HapticKind.CLEAR, HapticKind.BIG_CLEAR, HapticKind.COMBO, HapticKind.RECORD, HapticKind.GAME_OVER -> true
            else -> false
        }
        if (!isCompositionKind) return false

        val primitivesSupported = runCatching {
            v.areAllPrimitivesSupported(
                VibrationEffect.Composition.PRIMITIVE_CLICK,
                VibrationEffect.Composition.PRIMITIVE_TICK,
                VibrationEffect.Composition.PRIMITIVE_THUD,
                VibrationEffect.Composition.PRIMITIVE_QUICK_RISE,
                VibrationEffect.Composition.PRIMITIVE_SLOW_RISE,
                VibrationEffect.Composition.PRIMITIVE_QUICK_FALL
            )
        }.getOrDefault(false)
        if (!primitivesSupported) return false

        return runCatching {
            val comp = VibrationEffect.startComposition()
            val s = scale.coerceIn(0.01f, 1.0f)
            when (kind) {
                HapticKind.CLEAR -> {
                    comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, (0.8f * s).coerceIn(0.01f, 1.0f))
                    comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, (1.0f * s).coerceIn(0.01f, 1.0f), 15)
                }
                HapticKind.BIG_CLEAR -> {
                    comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_SLOW_RISE, (0.9f * s).coerceIn(0.01f, 1.0f))
                    comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, (1.0f * s).coerceIn(0.01f, 1.0f), 20)
                }
                HapticKind.COMBO -> {
                    comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, (0.5f * s).coerceIn(0.01f, 1.0f))
                    comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, (0.7f * s).coerceIn(0.01f, 1.0f), 35)
                    comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, (0.85f * s).coerceIn(0.01f, 1.0f), 25)
                    comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, (1.0f * s).coerceIn(0.01f, 1.0f), 15)
                }
                HapticKind.RECORD -> {
                    comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, (0.9f * s).coerceIn(0.01f, 1.0f))
                    comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, (0.9f * s).coerceIn(0.01f, 1.0f), 35)
                    comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, (1.0f * s).coerceIn(0.01f, 1.0f), 35)
                    comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_SLOW_RISE, (1.0f * s).coerceIn(0.01f, 1.0f), 45)
                }
                HapticKind.GAME_OVER -> {
                    comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, (1.0f * s).coerceIn(0.01f, 1.0f))
                    comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, (0.6f * s).coerceIn(0.01f, 1.0f), 40)
                    comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, (0.3f * s).coerceIn(0.01f, 1.0f), 40)
                }
                else -> return false
            }
            v.vibrate(comp.compose())
            true
        }.getOrDefault(false)
    }
}
