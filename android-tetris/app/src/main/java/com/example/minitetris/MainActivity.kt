package com.example.minitetris

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.toShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.min
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TetrisTheme {
                TetrisScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TetrisTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val colors = when {
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialExpressiveTheme(
        colorScheme = colors,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TetrisScreen() {
    var game by remember { mutableStateOf(newGame()) }
    var paused by remember { mutableStateOf(false) }
    val score by animateIntAsState(game.score, animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(), label = "score")
    val density = LocalDensity.current
    val dragStep = with(density) { 24.dp.toPx() }

    // Each action is applied only while the game is running.
    fun update(action: (GameState) -> GameState) {
        if (!paused && !game.over) game = action(game)
    }

    // Gravity: one step per interval, stopped while paused or after game over.
    LaunchedEffect(paused, game.over) {
        while (!paused && !game.over) {
            delay(game.gravityMs())
            game = game.step()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Header(score = score, lines = game.lines, next = game.next)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Board(
                game = game,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { update { it.rotate() } })
                    }
                    .pointerInput(Unit) {
                        var dx = 0f
                        var dy = 0f
                        detectDragGestures(
                            onDragStart = {
                                dx = 0f
                                dy = 0f
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                dx += amount.x
                                dy += amount.y
                                if (dx > dragStep) {
                                    dx = 0f
                                    update { it.move(1) }
                                } else if (dx < -dragStep) {
                                    dx = 0f
                                    update { it.move(-1) }
                                }
                                if (dy > dragStep * 2) {
                                    dy = 0f
                                    update { it.hardDrop() }
                                }
                            },
                        )
                    },
            )
            if (game.over) {
                GameOver(score = game.score, onNewGame = {
                    game = newGame()
                    paused = false
                })
            }
        }

        Controls(
            paused = paused,
            enabled = !game.over,
            onLeft = { update { it.move(-1) } },
            onRight = { update { it.move(1) } },
            onRotate = { update { it.rotate() } },
            onDrop = { update { it.hardDrop() } },
            onTogglePause = { paused = !paused },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Header(score: Int, lines: Int, next: Shape) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = stringResource(R.string.score),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = score.toString(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${stringResource(R.string.lines)}: $lines",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // The next piece sits on a cookie-shaped badge.
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(MaterialShapes.Cookie9Sided.toShape())
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            PiecePreview(next, cell = 12.dp, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun PiecePreview(shape: Shape, cell: androidx.compose.ui.unit.Dp, color: Color) {
    val cells = shape.cells
    val rows = cells.maxOf { it.first } + 1
    val cols = cells.maxOf { it.second } + 1
    Canvas(modifier = Modifier.size(cell * cols.toFloat(), cell * rows.toFloat())) {
        val c = cell.toPx()
        for ((r, col) in cells) {
            drawRoundRect(
                color = color,
                topLeft = Offset(col * c, r * c),
                size = Size(c * 0.9f, c * 0.9f),
                cornerRadius = CornerRadius(c * 0.25f),
            )
        }
    }
}

@Composable
private fun Board(game: GameState, modifier: Modifier) {
    val palette = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.inversePrimary,
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
    )
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    Canvas(modifier = modifier.aspectRatio(COLS / ROWS.toFloat())) {
        val cell = min(size.width / COLS, size.height / ROWS)
        val originX = (size.width - cell * COLS) / 2
        val originY = (size.height - cell * ROWS) / 2
        val gap = cell * 0.08f
        val radius = CornerRadius(cell * 0.22f)
        val block = Size(cell - gap * 2, cell - gap * 2)

        drawRoundRect(
            color = track,
            topLeft = Offset(originX, originY),
            size = Size(cell * COLS, cell * ROWS),
            cornerRadius = CornerRadius(cell * 0.4f),
        )
        for (r in 0 until ROWS) {
            for (c in 0 until COLS) {
                val v = game.board[r][c]
                if (v != 0) {
                    drawRoundRect(
                        color = palette[v - 1],
                        topLeft = Offset(originX + c * cell + gap, originY + r * cell + gap),
                        size = block,
                        cornerRadius = radius,
                    )
                }
            }
        }
        game.piece?.let { p ->
            val color = palette[p.shape.ordinal]
            for ((r, c) in p.cells) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(originX + (p.col + c) * cell + gap, originY + (p.row + r) * cell + gap),
                    size = block,
                    cornerRadius = radius,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Controls(
    paused: Boolean,
    enabled: Boolean,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onRotate: () -> Unit,
    onDrop: () -> Unit,
    onTogglePause: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalButton(onClick = onLeft, enabled = enabled, shape = shape, modifier = Modifier.weight(1f)) {
                Text("←", fontSize = 22.sp)
            }
            FilledTonalButton(onClick = onRight, enabled = enabled, shape = shape, modifier = Modifier.weight(1f)) {
                Text("→", fontSize = 22.sp)
            }
            Button(onClick = onRotate, enabled = enabled, shape = shape, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.rotate))
            }
            Button(onClick = onDrop, enabled = enabled, shape = shape, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.drop))
            }
        }
        OutlinedButton(
            onClick = onTogglePause,
            enabled = enabled,
            shape = shape,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(if (paused) R.string.resume else R.string.pause))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GameOver(score: Int, onNewGame: () -> Unit) {
    Surface(
        modifier = Modifier.padding(24.dp),
        shape = MaterialShapes.Cookie9Sided.toShape(),
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.game_over),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = score.toString(),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onNewGame, shape = RoundedCornerShape(24.dp)) {
                Text(stringResource(R.string.new_game))
            }
        }
    }
}
