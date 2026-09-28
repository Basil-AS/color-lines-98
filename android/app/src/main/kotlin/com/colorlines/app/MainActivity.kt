package com.colorlines.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colorlines.engine.BallColor
import com.colorlines.engine.GameEngine
import com.colorlines.engine.Point

enum class AppTheme {
    MODERN, CLASSIC_98
}

class MainActivity : ComponentActivity() {
    private lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        soundManager = SoundManager(this)

        setContent {
            ColorLinesApp(soundManager = soundManager)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
    }
}

@Composable
fun ColorLinesApp(soundManager: SoundManager) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val prefs = remember { context.getSharedPreferences("colorlines_prefs", Context.MODE_PRIVATE) }

    var theme by remember {
        val savedTheme = prefs.getString("theme", AppTheme.MODERN.name)
        mutableStateOf(AppTheme.valueOf(savedTheme ?: AppTheme.MODERN.name))
    }
    var bestScore by remember {
        mutableIntStateOf(prefs.getInt("best_score", 0))
    }
    var soundEnabled by remember {
        mutableStateOf(soundManager.isEnabled)
    }

    val engine = remember { GameEngine() }
    var tick by remember { mutableIntStateOf(0) }
    var showGameOverDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    fun refreshState() {
        tick++
        if (engine.score > bestScore) {
            bestScore = engine.score
            prefs.edit().putInt("best_score", bestScore).apply()
        }
        if (engine.isGameOver) {
            showGameOverDialog = true
            soundManager.playLose()
        }
    }

    val reachableCells = remember(engine.selectedPoint, tick) {
        engine.getReachableCellsForSelected()
    }

    val bgColor = if (theme == AppTheme.MODERN) Color(0xFF121217) else Color(0xFF008080)
    val panelBg = if (theme == AppTheme.MODERN) Color(0xFF1E1E24) else Color(0xFFC0C0C0)
    val textColor = if (theme == AppTheme.MODERN) Color.White else Color.Black

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = bgColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header / HUD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = if (theme == AppTheme.MODERN) RoundedCornerShape(16.dp) else RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(containerColor = panelBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Top row: Title and Stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Color Lines 98",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            fontFamily = if (theme == AppTheme.CLASSIC_98) FontFamily.Monospace else FontFamily.Default
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatBox(label = "Score", value = engine.score, theme = theme)
                            StatBox(label = "Best", value = maxOf(engine.score, bestScore), theme = theme)
                        }
                    }

                    // Bottom row: Next Preview and Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Next Balls Preview
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .background(
                                    if (theme == AppTheme.MODERN) Color(0xFF141418) else Color(0xFF808080),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Next:",
                                fontSize = 12.sp,
                                color = if (theme == AppTheme.MODERN) Color.LightGray else Color.White
                            )
                            engine.nextColors.forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(getColorForBall(color))
                                )
                            }
                        }

                        // Action Controls
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Undo
                            FilledTonalButton(
                                onClick = {
                                    if (engine.undo()) {
                                        soundManager.playClick()
                                        refreshState()
                                    }
                                },
                                enabled = engine.canUndo,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("↶ Undo", fontSize = 12.sp)
                            }

                            // New Game
                            FilledTonalButton(
                                onClick = {
                                    engine.startNewGame()
                                    soundManager.playClick()
                                    refreshState()
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("New", fontSize = 12.sp)
                            }

                            // Sound Toggle
                            FilledTonalButton(
                                onClick = {
                                    soundEnabled = !soundEnabled
                                    soundManager.isEnabled = soundEnabled
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(if (soundEnabled) "🔊" else "🔇", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 9x9 Game Board
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(if (theme == AppTheme.MODERN) RoundedCornerShape(16.dp) else RoundedCornerShape(0.dp))
                    .background(if (theme == AppTheme.MODERN) Color(0xFF1E1E24) else Color(0xFF808080))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                GameBoardCanvas(
                    engine = engine,
                    theme = theme,
                    reachableCells = reachableCells,
                    onCellTap = { point ->
                        val currentBall = engine.board[point]
                        if (currentBall != null) {
                            engine.selectCell(point)
                            soundManager.playSelect()
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            refreshState()
                        } else if (engine.selectedPoint != null) {
                            val from = engine.selectedPoint!!
                            val moveResult = engine.moveBall(from, point)
                            if (moveResult.success) {
                                engine.unselect()
                                soundManager.playJump()
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                                if (moveResult.clearedPoints.isNotEmpty()) {
                                    soundManager.playEat(moveResult.pointsEarned)
                                }
                            } else {
                                soundManager.playClick()
                            }
                            refreshState()
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom bar: Theme Switcher & Rules
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = theme == AppTheme.MODERN,
                        onClick = {
                            theme = AppTheme.MODERN
                            prefs.edit().putString("theme", theme.name).apply()
                        },
                        label = { Text("Modern") }
                    )
                    FilterChip(
                        selected = theme == AppTheme.CLASSIC_98,
                        onClick = {
                            theme = AppTheme.CLASSIC_98
                            prefs.edit().putString("theme", theme.name).apply()
                        },
                        label = { Text("Classic 98") }
                    )
                }

                TextButton(onClick = { showHelpDialog = true }) {
                    Text("Rules & Info", color = textColor)
                }
            }
        }
    }

    // Game Over Dialog
    if (showGameOverDialog) {
        AlertDialog(
            onDismissRequest = { showGameOverDialog = false },
            title = { Text("Game Over!") },
            text = {
                Text("Final Score: ${engine.score}\nBest Score: $bestScore")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showGameOverDialog = false
                        engine.startNewGame()
                        refreshState()
                    }
                ) {
                    Text("Play Again")
                }
            }
        )
    }

    // Help Dialog
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("Color Lines 98 Rules") },
            text = {
                Text(
                    "• Align 5 or more balls of the same color horizontally, vertically, or diagonally.\n\n" +
                    "• Classic Gamos 1992 scoring:\n" +
                    "  5 balls = 10 pts\n" +
                    "  6 balls = 12 pts\n" +
                    "  7 balls = 18 pts\n" +
                    "  8 balls = 28 pts\n" +
                    "  9 balls = 42 pts\n\n" +
                    "• A ball can only move if an unobstructed path exists."
                )
            },
            confirmButton = {
                Button(onClick = { showHelpDialog = false }) {
                    Text("Got it")
                }
            }
        )
    }
}

@Composable
fun StatBox(label: String, value: Int, theme: AppTheme) {
    Column(
        modifier = Modifier
            .background(
                if (theme == AppTheme.MODERN) Color(0xFF141418) else Color.Black,
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, fontSize = 10.sp, color = Color.Gray)
        Text(
            text = value.toString(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (theme == AppTheme.MODERN) Color(0xFF00E676) else Color.Red,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun GameBoardCanvas(
    engine: GameEngine,
    theme: AppTheme,
    reachableCells: Set<Point>,
    onCellTap: (Point) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "selectionPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val cellSize = size.width / 9f
                    val col = (offset.x / cellSize).toInt().coerceIn(0, 8)
                    val row = (offset.y / cellSize).toInt().coerceIn(0, 8)
                    onCellTap(Point(col, row))
                }
            }
    ) {
        val boardSize = 9
        val cellSize = size.width / boardSize
        val cellPadding = 2f

        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                val point = Point(col, row)
                val left = col * cellSize + cellPadding
                val top = row * cellSize + cellPadding
                val cellWidth = cellSize - cellPadding * 2

                // Draw cell background
                val cellColor = if (theme == AppTheme.MODERN) {
                    Color(0xFF2B2B36)
                } else {
                    Color(0xFFB0B0B0)
                }
                drawRect(
                    color = cellColor,
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(cellWidth, cellWidth)
                )

                // Draw reachable dot for empty cell
                if (point in reachableCells && engine.board.isEmpty(point)) {
                    drawCircle(
                        color = if (theme == AppTheme.MODERN) Color(0xFF4FC3F7).copy(alpha = 0.6f) else Color(0xFF000080).copy(alpha = 0.5f),
                        radius = cellWidth * 0.12f,
                        center = Offset(left + cellWidth / 2f, top + cellWidth / 2f)
                    )
                }

                // Draw ball if present
                val ball = engine.board[point]
                if (ball != null) {
                    val isSelected = engine.selectedPoint == point
                    val currentRadius = if (isSelected) {
                        (cellWidth * 0.40f) * pulseScale
                    } else {
                        cellWidth * 0.40f
                    }
                    val centerX = left + cellWidth / 2f
                    val centerY = top + cellWidth / 2f

                    val baseColor = getColorForBall(ball)
                    val highlightColor = Color.White.copy(alpha = 0.75f)

                    // 3D sphere gradient
                    val brush = Brush.radialGradient(
                        colors = listOf(highlightColor, baseColor, baseColor.copy(alpha = 0.85f), Color.Black.copy(alpha = 0.4f)),
                        center = Offset(centerX - currentRadius * 0.35f, centerY - currentRadius * 0.35f),
                        radius = currentRadius * 1.3f
                    )

                    // Shadow underneath
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.3f),
                        radius = currentRadius * 0.95f,
                        center = Offset(centerX, centerY + currentRadius * 0.15f)
                    )

                    // Ball sphere
                    drawCircle(
                        brush = brush,
                        radius = currentRadius,
                        center = Offset(centerX, centerY)
                    )
                }
            }
        }
    }
}

fun getColorForBall(ball: BallColor): Color {
    return when (ball) {
        BallColor.RED -> Color(0xFFE53935)
        BallColor.GREEN -> Color(0xFF43A047)
        BallColor.BLUE -> Color(0xFF1E88E5)
        BallColor.CYAN -> Color(0xFF00ACC1)
        BallColor.MAGENTA -> Color(0xFF8E24AA)
        BallColor.YELLOW -> Color(0xFFFDD835)
        BallColor.BROWN -> Color(0xFF6D4C41)
    }
}
