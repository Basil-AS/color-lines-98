package com.colorlines.app

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.colorlines.engine.BallColor
import com.colorlines.engine.Point

const val BOARD_SIZE = 9

/**
 * An immutable picture of the board. The UI draws from this instead of the live engine, so Compose
 * always sees a change (a new game, an undo, a move) and never shows a stale board.
 */
data class BoardSnapshot(
    val cells: List<BallColor?>,
    val selected: Point?,
    val reachable: Set<Point>
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
fun BoardView(
    snapshot: BoardSnapshot,
    palette: Palette,
    onCellTap: (Point) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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
    val boardLabel = stringResource(R.string.board_label)
    val shape = if (palette.square) RoundedCornerShape(0.dp) else RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(palette.boardBackground)
            .padding(4.dp)
            .semantics { contentDescription = boardLabel }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cellSize = size.width / BOARD_SIZE
            val pad = 2f
            val inner = cellSize - pad * 2
            for (row in 0 until BOARD_SIZE) {
                for (col in 0 until BOARD_SIZE) {
                    val point = Point(col, row)
                    val left = col * cellSize + pad
                    val top = row * cellSize + pad
                    drawRect(palette.cell, Offset(left, top), Size(inner, inner))

                    val ball = snapshot[point]
                    val center = Offset(left + inner / 2f, top + inner / 2f)
                    if (ball == null) {
                        if (point in snapshot.reachable) {
                            drawCircle(palette.reachableDot, radius = inner * 0.12f, center = center)
                        }
                        continue
                    }
                    val radius = inner * 0.40f * if (snapshot.selected == point) pulse else 1f
                    val base = ballColor(ball)
                    drawCircle(
                        Color.Black.copy(alpha = 0.3f),
                        radius = radius * 0.95f,
                        center = Offset(center.x, center.y + radius * 0.15f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.75f),
                                base,
                                base.copy(alpha = 0.85f),
                                Color.Black.copy(alpha = 0.4f)
                            ),
                            center = Offset(center.x - radius * 0.35f, center.y - radius * 0.35f),
                            radius = radius * 1.3f
                        ),
                        radius = radius,
                        center = center
                    )
                }
            }
        }

        // One accessible, focusable target per cell on top of the drawing.
        Column(Modifier.fillMaxSize()) {
            for (row in 0 until BOARD_SIZE) {
                Row(Modifier.weight(1f)) {
                    for (col in 0 until BOARD_SIZE) {
                        val point = Point(col, row)
                        CellTarget(snapshot, point, onCellTap, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CellTarget(
    snapshot: BoardSnapshot,
    point: Point,
    onTap: (Point) -> Unit,
    modifier: Modifier
) {
    val ball = snapshot[point]
    val isSelected = snapshot.selected == point
    val content = if (ball == null) stringResource(R.string.cell_empty)
    else stringResource(R.string.cell_ball, colorLabel(ball))
    val state = (if (isSelected) stringResource(R.string.cell_selected) else "") +
        (if (ball == null && point in snapshot.reachable) stringResource(R.string.cell_reachable) else "")
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

