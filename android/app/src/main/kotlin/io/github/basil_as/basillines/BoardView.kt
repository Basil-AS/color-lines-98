package io.github.basil_as.basillines

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import io.github.basil_as.basillines.engine.BallSize
import io.github.basil_as.basillines.engine.BallTone
import io.github.basil_as.basillines.engine.CellShape
import io.github.basil_as.basillines.engine.GridStyle
import io.github.basil_as.basillines.engine.Look
import io.github.basil_as.basillines.engine.LookPalettes
import io.github.basil_as.basillines.engine.RingLevel
import io.github.basil_as.basillines.engine.ShapeSet
import io.github.basil_as.basillines.engine.Volume
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.basil_as.basillines.engine.BallColor
import io.github.basil_as.basillines.engine.Point

const val BOARD_SIZE = 9

/**
 * An immutable picture of the board. The UI draws from this instead of the live engine, so Compose
 * always sees a change (a new game, an undo, a move) and never shows a stale board.
 */
data class BoardSnapshot(
    val cells: List<BallColor?>,
    val selected: Point?,
    val reachable: Set<Point>,
    /** Cells where the next balls will appear (empty when the preview is switched off). */
    val incoming: Map<Point, BallColor>,
    /** Where the hint says to put the selected ball. */
    val hintTarget: Point? = null,
    /** The tutorial also marks the ball to pick up. */
    val hintSource: Point? = null
) {
    operator fun get(p: Point): BallColor? = cells[p.y * BOARD_SIZE + p.x]
}

@Composable
fun colorLabel(color: BallColor): String = stringResource(
    when (color) {
        BallColor.RED -> R.string.color_red
        BallColor.GREEN -> R.string.color_green
        BallColor.BLUE -> R.string.color_blue
        BallColor.CYAN -> R.string.color_cyan
        BallColor.MAGENTA -> R.string.color_magenta
        BallColor.YELLOW -> R.string.color_yellow
        BallColor.BROWN -> R.string.color_brown
    }
)

@Composable
internal fun rememberSprites(palette: Palette): List<ImageBitmap>? {
    if (palette.ballStyle != BallStyle.SPRITE) return null
    val resources = LocalContext.current.resources
    return remember {
        listOf(
            R.drawable.ball_sprite_0, R.drawable.ball_sprite_1, R.drawable.ball_sprite_2, R.drawable.ball_sprite_3,
            R.drawable.ball_sprite_4, R.drawable.ball_sprite_5, R.drawable.ball_sprite_6
        ).map { ImageBitmap.imageResource(resources, it) }
    }
}

@Composable
fun BoardView(
    snapshot: BoardSnapshot,
    palette: Palette,
    onCellTap: (Point) -> Unit,
    modifier: Modifier = Modifier,
    /** The move to play out over the board (travelling ball, sparks, points), and how lively to be. */
    fx: MoveFx? = null,
    level: EffectsLevel = EffectsLevel.OFF,
    /** The board is nearly full: it glows and breathes. */
    danger: Boolean = false
) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    // One clock per effect, in milliseconds; read only while drawing, so a frame never recomposes the cells.
    val fxClock = remember(fx?.id) { androidx.compose.animation.core.Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(fx?.id, level) {
        if (fx != null && level != EffectsLevel.OFF) fxClock.animateTo(fx.totalMs, tween(fx.totalMs.toInt(), easing = androidx.compose.animation.core.LinearEasing))
    }
    val rippleClock = remember { androidx.compose.animation.core.Animatable(1f) }
    var ripplePoint by remember { mutableStateOf<Point?>(null) }
    val wiggleClock = remember { androidx.compose.animation.core.Animatable(1f) }
    var wigglePoint by remember { mutableStateOf<Point?>(null) }
    val latest = androidx.compose.runtime.rememberUpdatedState(snapshot)
    androidx.compose.runtime.LaunchedEffect(level) {
        // Now and then a ball wiggles on its own.
        while (level == EffectsLevel.FULL) {
            kotlinx.coroutines.delay(3500L + (Math.random() * 4500).toLong())
            val balls = latest.value.cells.indices.filter { latest.value.cells[it] != null && latest.value.selected != Point(it % BOARD_SIZE, it / BOARD_SIZE) }
            if (balls.isEmpty()) continue
            val pick = balls.random()
            wigglePoint = Point(pick % BOARD_SIZE, pick / BOARD_SIZE)
            wiggleClock.snapTo(0f)
            wiggleClock.animateTo(1f, tween(800))
        }
    }
    val tap: (Point) -> Unit = { p ->
        onCellTap(p)
        if (level != EffectsLevel.OFF) {
            ripplePoint = p
            scope.launch { rippleClock.snapTo(0f); rippleClock.animateTo(1f, tween(400)) }
        }
    }
    val dangerAlpha by transition0(danger && level != EffectsLevel.OFF)
    val animationsEnabled = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }
    val transition = rememberInfiniteTransition(label = "selectionPulse")
    val animatedScale by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(450, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseScale"
    )
    val pulse = if (animationsEnabled) animatedScale else 1f
    val sprites = rememberSprites(palette)
    val boardLabel = stringResource(R.string.board_label)
    val shape = if (palette.square) RoundedCornerShape(0.dp) else RoundedCornerShape(16.dp)
    val framePadding = if (palette.square) 3.dp else 4.dp

    Box(
        modifier = modifier
            .graphicsLayer {
                val shake = FxFrame(fx, fxClock.value, level).shake()
                translationX = shake.x * size.width / BOARD_SIZE
                translationY = shake.y * size.width / BOARD_SIZE
            }
            .clip(shape)
            .background(palette.boardBackground)
            .padding(framePadding)
            .semantics { contentDescription = boardLabel }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cellSize = size.width / BOARD_SIZE
            val frame = FxFrame(fx, fxClock.value, level)
            for (row in 0 until BOARD_SIZE) {
                for (col in 0 until BOARD_SIZE) {
                    val point = Point(col, row)
                    val left = col * cellSize
                    val top = row * cellSize
                    drawCell(palette, left, top, cellSize, col, row)

                    val center = Offset(left + cellSize / 2f, top + cellSize / 2f)
                    val ball = snapshot[point]
                    if (snapshot.hintTarget == point || snapshot.hintSource == point) {
                        drawRect(
                            palette.accent, Offset(left + 3f, top + 3f), Size(cellSize - 6f, cellSize - 6f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 3f,
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                            )
                        )
                    }
                    if (ball == null) {
                        if (point in snapshot.reachable) {
                            drawCircle(palette.reachableDot, radius = cellSize * 0.09f, center = center)
                        }
                        snapshot.incoming[point]?.let { drawBall(palette, sprites, it, center, cellSize * 0.17f, marker = false) }
                    } else if (!frame.hidden(point)) {
                        val radius = cellSize * 0.38f * if (snapshot.selected == point) pulse else 1f
                        val (sx, sy) = frame.scaleOf(point)
                        val wiggle = if (wigglePoint == point && wiggleClock.value < 1f) sin(wiggleClock.value * 5f * PI.toFloat()) * 10f * (1f - wiggleClock.value) else 0f
                        if (sx <= 0f) Unit
                        else if (sx == 1f && sy == 1f && wiggle == 0f) drawBall(palette, sprites, ball, center, radius, shadow = true, measurer = textMeasurer)
                        else withTransform({ scale(sx, sy, center); rotate(wiggle, center) }) { drawBall(palette, sprites, ball, center, radius, shadow = true, measurer = textMeasurer) }
                    }
                }
            }
            drawFx(frame, cellSize, textMeasurer) { color, at, r -> drawBall(palette, sprites, color, at, r, shadow = true, measurer = textMeasurer) }
            val ring = ripplePoint
            if (ring != null && rippleClock.value < 1f) {
                val k = rippleClock.value
                drawCircle(Color.White.copy(alpha = 0.8f * (1f - k)), cellSize * (0.25f + 0.4f * k), Offset((ring.x + 0.5f) * cellSize, (ring.y + 0.5f) * cellSize), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f))
            }
            if (dangerAlpha > 0f) {
                drawRect(Color(0xFFFF5252).copy(alpha = dangerAlpha), Offset.Zero, size, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f))
            }
        }

        // One accessible, focusable target per cell on top of the drawing.
        Column(Modifier.fillMaxSize()) {
            for (row in 0 until BOARD_SIZE) {
                Row(Modifier.weight(1f)) {
                    for (col in 0 until BOARD_SIZE) {
                        CellTarget(snapshot, Point(col, row), tap, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** A cell under the look studio's grid and cell-shape choices: the gap between cells is the board showing through. */
private fun DrawScope.drawStudioCell(palette: Palette, look: Look, left: Float, top: Float, cell: Float, col: Int, row: Int) {
    val pad = when (look.grid) {
        GridStyle.PLAIN, GridStyle.CHECKER_LINES -> 2f
        GridStyle.THIN -> 1f
        GridStyle.BOLD -> maxOf(3f, cell * 0.08f)
        GridStyle.NONE, GridStyle.CHECKER, GridStyle.DOTS -> 0f
    }
    val chosen = palette.overrides.cells
    val bare = look.grid == GridStyle.NONE || look.grid == GridStyle.CHECKER || look.grid == GridStyle.DOTS
    val corner = when {
        chosen == CellShape.ROUND -> cell / 2f - pad
        chosen == CellShape.SQUARE -> 0f
        chosen == CellShape.ROUNDED -> cell * 0.16f
        bare -> 0f
        palette.cellStyle == CellStyle.ROUNDED -> cell * 0.16f
        else -> 0f
    }
    val checkered = look.grid == GridStyle.CHECKER || look.grid == GridStyle.CHECKER_LINES
    val fill = if (checkered && (col + row) % 2 == 1) lerp(palette.cell, palette.text, 0.14f) else palette.cell
    drawRoundRect(fill, Offset(left + pad, top + pad), Size(cell - pad * 2, cell - pad * 2), CornerRadius(corner))
    if (look.grid == GridStyle.DOTS) {
        drawCircle(palette.text.copy(alpha = 0.3f), radius = maxOf(1.5f, cell * 0.03f), center = Offset(left + cell / 2f, top + cell / 2f))
    }
}

internal fun DrawScope.drawCell(palette: Palette, left: Float, top: Float, cell: Float, col: Int = 0, row: Int = 0) {
    val look = palette.look
    if (look != null && (palette.overrides.grid != null || palette.overrides.cells != null)) {
        drawStudioCell(palette, look, left, top, cell, col, row)
        return
    }
    when (palette.cellStyle) {
        CellStyle.ROUNDED -> {
            val pad = 2f
            drawRoundRect(
                palette.cell, Offset(left + pad, top + pad), Size(cell - pad * 2, cell - pad * 2),
                CornerRadius(cell * 0.16f)
            )
        }
        CellStyle.GRID -> {
            drawRect(palette.cell, Offset(left, top), Size(cell, cell))
            drawRect(
                palette.boardBackground,
                Offset(left, top),
                Size(cell, cell),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = maxOf(1.5f, cell * 0.03f))
            )
        }
        CellStyle.WIN98, CellStyle.DOS -> {
            val light = Color.White
            val dark = if (palette.cellStyle == CellStyle.WIN98) Color(0xFF808080) else Color(0xFF555555)
            val b = maxOf(2f, cell * 0.06f)
            drawRect(palette.cell, Offset(left, top), Size(cell, cell))
            // Raised bevel: light top and left edges, dark bottom and right edges.
            drawRect(light, Offset(left, top), Size(cell, b))
            drawRect(light, Offset(left, top), Size(b, cell))
            drawRect(dark, Offset(left, top + cell - b), Size(cell, b))
            drawRect(dark, Offset(left + cell - b, top), Size(b, cell))
        }
    }
}

/**
 * A ball under the look studio: the size, depth and colours the player chose, then a ring and a marker on top.
 * Without a choice this is exactly the theme's own ball.
 */
internal fun DrawScope.drawBall(
    palette: Palette,
    sprites: List<ImageBitmap>?,
    ball: BallColor,
    center: Offset,
    radius: Float,
    shadow: Boolean = false,
    measurer: TextMeasurer? = null,
    marker: Boolean = true
) {
    val look = palette.look
    if (look == null) {
        drawBaseBall(palette, sprites, ball, center, radius, shadow)
        return
    }
    val sizeChoice = palette.overrides.ballSize
    val ring = palette.overrides.ring
    var r = radius
    if (sizeChoice != null) r *= sizeChoice.scale / BallSize.NORMAL.scale
    r *= when (ring) {
        RingLevel.HIGH -> 0.95f
        RingLevel.MAX -> 0.91f
        else -> 1f
    }
    val tones = palette.tones
    if (tones != null) drawStudioBall(palette, look, tones.getValue(ball), center, r, shadow)
    else drawBaseBall(palette, sprites, ball, center, r, shadow)

    if (ring == RingLevel.HIGH || ring == RingLevel.MAX) {
        val width = if (ring == RingLevel.MAX) maxOf(3f, r * 0.11f) else maxOf(2f, r * 0.07f)
        drawCircle(palette.text.copy(alpha = 0.9f), r + width / 2f, center, style = Stroke(width = width))
    }
    val shapes = palette.overrides.shapes
    if (marker && shapes != null && shapes != ShapeSet.OFF && r >= 8f) {
        val ink = tones?.get(ball)?.let { Color(it.ink) } ?: if (baseColorOf(palette, ball).luminance() > 0.4f) Color.Black else Color.White
        if (shapes == ShapeSet.DIGITS && measurer != null) {
            val layout = measurer.measure(LookPalettes.digit(ball), TextStyle(fontSize = (r * 1.05f).toSp(), fontWeight = FontWeight.ExtraBold))
            drawText(layout, ink, Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f))
        } else {
            drawContrastSymbol(ball, center, r * 0.5f, ink)
        }
    }
}

/** The colour a theme paints a ball in, for working out which ink reads on it. */
private fun baseColorOf(palette: Palette, ball: BallColor): Color = when (palette.ballStyle) {
    BallStyle.NEON -> neonColor(ball)
    BallStyle.CONTRAST, BallStyle.SOLID -> contrastColor(ball)
    BallStyle.SOLID_CLEAR -> clearMeadowColor(ball)
    else -> ballColor(ball)
}

/** A soft dark ellipse under the ball, where it touches the board. */
private fun DrawScope.drawContactShadow(center: Offset, r: Float, strength: Float) {
    val at = Offset(center.x, center.y + r * 0.82f)
    drawOval(
        brush = Brush.radialGradient(colors = listOf(Color.Black.copy(alpha = strength), Color.Transparent), center = at, radius = r * 0.95f),
        topLeft = Offset(center.x - r, center.y + r * 0.52f),
        size = Size(r * 2f, r * 0.62f)
    )
}

/** A lit sphere: highlight, body and shade are the colour itself, lighter and darker, so the hue is kept. */
private fun DrawScope.drawLitSphere(tone: BallTone, center: Offset, r: Float, bounce: Boolean) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(tone.hi), Color(tone.base), Color(tone.lo)),
            center = Offset(center.x - r * 0.32f, center.y - r * 0.38f), radius = r * 1.45f
        ),
        radius = r, center = center
    )
    if (bounce) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.2f), Color.Transparent),
                center = Offset(center.x, center.y + r * 1.2f), radius = r
            ),
            radius = r, center = center
        )
    }
}

/** A small crisp highlight at the upper left. */
private fun DrawScope.drawSpecular(center: Offset, r: Float) {
    val at = Offset(center.x - r * 0.42f, center.y - r * 0.52f)
    withTransform({ rotate(-30f, at) }) {
        drawOval(
            brush = Brush.radialGradient(colors = listOf(Color.White.copy(alpha = 0.62f), Color.Transparent), center = at, radius = r * 0.36f),
            topLeft = Offset(at.x - r * 0.38f, at.y - r * 0.24f),
            size = Size(r * 0.76f, r * 0.48f)
        )
    }
}

/** Soft is a satin finish; glossy adds the highlight and the light bouncing back from below. */
private fun DrawScope.drawStudioBall(palette: Palette, look: Look, tone: BallTone, center: Offset, r: Float, shadow: Boolean) {
    when (look.volume) {
        Volume.FLAT -> drawCircle(Color(tone.base), r, center)
        Volume.SOFT -> {
            if (shadow) drawContactShadow(center, r, 0.32f)
            drawLitSphere(tone, center, r, bounce = false)
        }
        Volume.GLOSSY -> {
            if (shadow) drawContactShadow(center, r, 0.38f)
            drawLitSphere(tone, center, r, bounce = true)
            drawCircle(Color.White.copy(alpha = 0.14f), r - 0.5f, center, style = Stroke(width = 1f))
            if (shadow) drawSpecular(center, r)
        }
    }
}

private fun DrawScope.drawBaseBall(
    palette: Palette,
    sprites: List<ImageBitmap>?,
    ball: BallColor,
    center: Offset,
    radius: Float,
    shadow: Boolean
) {
    when (palette.ballStyle) {
        BallStyle.SPRITE -> {
            val bitmap = sprites?.get(spriteIndex(ball))
            if (bitmap != null) {
                val d = (radius * 2.4f).toInt().coerceAtLeast(2)
                drawImage(
                    bitmap,
                    dstOffset = IntOffset((center.x - d / 2f).toInt(), (center.y - d / 2f).toInt()),
                    dstSize = IntSize(d, d),
                    // The modern look scales the sprites smoothly; the original keeps its hard pixels.
                    filterQuality = if (palette.cellStyle == CellStyle.ROUNDED) androidx.compose.ui.graphics.FilterQuality.Medium else androidx.compose.ui.graphics.FilterQuality.None
                )
            }
        }
        BallStyle.DOS -> {
            val (hi, mid, lo) = dosBands(ball)
            if (shadow) drawCircle(Color.Black.copy(alpha = 0.55f), radius, center + Offset(radius * 0.12f, radius * 0.12f))
            drawCircle(Color.Black, radius + 1.5f, center)
            drawCircle(lo, radius, center)
            drawCircle(mid, radius * 0.86f, center + Offset(-radius * 0.06f, -radius * 0.06f))
            drawCircle(hi, radius * 0.55f, center + Offset(-radius * 0.22f, -radius * 0.24f))
            drawCircle(Color.White, radius * 0.2f, center + Offset(-radius * 0.32f, -radius * 0.36f))
        }
        BallStyle.NEON -> {
            val base = neonColor(ball)
            drawCircle(base.copy(alpha = 0.22f), radius * 1.3f, center)
            drawCircle(base.copy(alpha = 0.35f), radius * 1.12f, center)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.85f), base, base.copy(alpha = 0.55f)),
                    center = Offset(center.x - radius * 0.3f, center.y - radius * 0.3f),
                    radius = radius * 1.25f
                ),
                radius = radius,
                center = center
            )
        }
        BallStyle.CONTRAST -> {
            val tint = palette.symbolTint?.invoke(ball)
            drawCircle(palette.ballBorder, radius + 2f, center)
            drawCircle(tint?.first ?: contrastColor(ball), radius - 1f, center)
            if (palette.overrides.shapes == null) drawContrastSymbol(ball, center, radius * 0.5f, tint?.second ?: Color.Black)
        }
        BallStyle.SOLID -> {
            drawCircle(palette.ballBorder, radius + 2f, center)
            drawCircle(contrastColor(ball), radius - 1f, center)
        }
        BallStyle.SOLID_CLEAR -> {
            drawCircle(palette.ballBorder, radius + 2f, center)
            drawCircle(clearMeadowColor(ball), radius - 1f, center)
        }
        BallStyle.GLOSSY -> {
            val tone = LookPalettes.tone(ballColor(ball).toArgb())
            if (shadow) drawContactShadow(center, radius, 0.38f)
            drawLitSphere(tone, center, radius, bounce = true)
            drawCircle(Color.White.copy(alpha = 0.14f), radius - 0.5f, center, style = Stroke(width = 1f))
            if (shadow) drawSpecular(center, radius)
        }
    }
}

/** A shape per colour (circle, triangle, square, diamond, star, plus, heart), drawn in black on the ball. */
private fun DrawScope.drawContrastSymbol(ball: BallColor, c: Offset, r: Float, ink: Color) {
    fun polygon(pts: List<Offset>) {
        val path = androidx.compose.ui.graphics.Path()
        path.moveTo(pts[0].x, pts[0].y)
        for (i in 1 until pts.size) path.lineTo(pts[i].x, pts[i].y)
        path.close()
        drawPath(path, ink)
    }
    when (ball) {
        BallColor.RED -> drawCircle(ink, r * 0.85f, c)
        BallColor.GREEN -> polygon(listOf(Offset(c.x, c.y - r), Offset(c.x + r, c.y + r * 0.8f), Offset(c.x - r, c.y + r * 0.8f)))
        BallColor.BLUE -> drawRect(ink, Offset(c.x - r * 0.8f, c.y - r * 0.8f), Size(r * 1.6f, r * 1.6f))
        BallColor.CYAN -> polygon(listOf(Offset(c.x, c.y - r), Offset(c.x + r, c.y), Offset(c.x, c.y + r), Offset(c.x - r, c.y)))
        BallColor.MAGENTA -> {
            val pts = (0 until 10).map { i ->
                val a = Math.PI / 5 * i - Math.PI / 2
                val rad = if (i % 2 == 0) r else r * 0.45f
                Offset(c.x + (Math.cos(a) * rad).toFloat(), c.y + (Math.sin(a) * rad).toFloat())
            }
            polygon(pts)
        }
        BallColor.YELLOW -> {
            drawRect(ink, Offset(c.x - r * 0.3f, c.y - r), Size(r * 0.6f, r * 2f))
            drawRect(ink, Offset(c.x - r, c.y - r * 0.3f), Size(r * 2f, r * 0.6f))
        }
        BallColor.BROWN -> {
            drawCircle(ink, r * 0.5f, Offset(c.x - r * 0.45f, c.y - r * 0.3f))
            drawCircle(ink, r * 0.5f, Offset(c.x + r * 0.45f, c.y - r * 0.3f))
            polygon(listOf(Offset(c.x - r * 0.93f, c.y - r * 0.05f), Offset(c.x + r * 0.93f, c.y - r * 0.05f), Offset(c.x, c.y + r)))
        }
    }
}

@Composable
internal fun CellTarget(
    snapshot: BoardSnapshot,
    point: Point,
    onTap: (Point) -> Unit,
    modifier: Modifier
) {
    val ball = snapshot[point]
    val coming = if (ball == null) snapshot.incoming[point] else null
    val isSelected = snapshot.selected == point
    val content = if (ball == null) stringResource(R.string.cell_empty)
    else stringResource(R.string.cell_ball, colorLabel(ball))
    val state = (if (isSelected) stringResource(R.string.cell_selected) else "") +
        (if (ball == null && point in snapshot.reachable) stringResource(R.string.cell_reachable) else "") +
        (if (coming != null) stringResource(R.string.cell_incoming, colorLabel(coming)) else "")
    val description = stringResource(R.string.cell_description, point.y + 1, point.x + 1, content, state)

    Box(
        modifier
            .fillMaxSize()
            .semantics {
                contentDescription = description
                role = Role.Button
                selected = isSelected
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onTap(point) }
    )
}


/** A slow breathing between 0.25 and 0.8 while [on], else 0. */
@Composable
private fun transition0(on: Boolean): androidx.compose.runtime.State<Float> {
    if (!on) return remember { mutableStateOf(0f) }
    val t = rememberInfiniteTransition(label = "danger")
    return t.animateFloat(0.25f, 0.8f, infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "dangerAlpha")
}
