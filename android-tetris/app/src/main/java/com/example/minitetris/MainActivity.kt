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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
    val dragStep = with(LocalDensity.current) { 24.dp.toPx() }
    val context = LocalContext.current

    // Actions only apply while the game runs: not paused and not over. Each change gets its haptic.
    fun update(action: (GameState) -> GameState) {
        if (!paused && !game.over) {
            val before = game
            game = action(before)
            Haptics.forChange(context, before, game)
        }
    }

    // Gravity: one step per interval, stopped while paused or after game over.
    LaunchedEffect(paused, game.over) {
        while (!paused && !game.over) {
            delay(game.gravityMs())
            val before = game
            game = game.step()
            Haptics.forChange(context, before, game)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Header(score = score, lines = game.lines, next = game.next)

        // The board is a fixed-proportion card that fills the height left over, centred.
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(COLS / ROWS.toFloat()),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Board(
                    game = game,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
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
            }

            if (game.over) {
                Overlay(title = stringResource(R.string.game_over)) {
                    Text(
                        text = game.score.toString(),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        game = newGame()
                        paused = false
                    }) {
                        Text(stringResource(R.string.new_game))
                    }
                }
            } else if (paused) {
                Overlay(title = stringResource(R.string.paused_title)) {
                    FilledTonalButton(onClick = { paused = false }) {
                        Text(stringResource(R.string.resume))
                    }
                }
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
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                Text(
                    text = stringResource(R.string.score),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = score.toString(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${stringResource(R.string.lines)}: $lines",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // The next piece sits on a cookie-shaped badge.
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(MaterialShapes.Cookie9Sided.toShape())
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            PiecePreview(next, cell = 11.dp, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun PiecePreview(shape: Shape, cell: Dp, color: Color) {
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
    val grid = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(modifier = modifier) {
        val cell = min(size.width / COLS, size.height / ROWS)
        val originX = (size.width - cell * COLS) / 2
        val originY = (size.height - cell * ROWS) / 2
        val gap = cell * 0.08f
        val radius = CornerRadius(cell * 0.24f)
        val block = Size(cell - gap * 2, cell - gap * 2)

        // Faint grid so the empty cells read as a board.
        for (r in 0 until ROWS) {
            for (c in 0 until COLS) {
                drawRoundRect(
                    color = grid.copy(alpha = 0.35f),
                    topLeft = Offset(originX + c * cell + gap, originY + r * cell + gap),
                    size = block,
                    cornerRadius = radius,
                )
            }
        }
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
    val shape = RoundedCornerShape(20.dp)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FilledTonalButton(onClick = onLeft, enabled = enabled, shape = shape, modifier = Modifier.weight(1f).height(56.dp)) {
                Text("←", style = MaterialTheme.typography.titleLarge)
            }
            FilledTonalButton(onClick = onRight, enabled = enabled, shape = shape, modifier = Modifier.weight(1f).height(56.dp)) {
                Text("→", style = MaterialTheme.typography.titleLarge)
            }
            Button(onClick = onRotate, enabled = enabled, shape = shape, modifier = Modifier.weight(1f).height(56.dp)) {
                Text(stringResource(R.string.rotate))
            }
            Button(onClick = onDrop, enabled = enabled, shape = shape, modifier = Modifier.weight(1f).height(56.dp)) {
                Text(stringResource(R.string.drop))
            }
        }
        OutlinedButton(
            onClick = onTogglePause,
            enabled = enabled,
            shape = shape,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text(stringResource(if (paused) R.string.resume else R.string.pause))
        }
    }
}

/** A centred, expressive card laid over the board for pause and game over. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Overlay(title: String, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = MaterialShapes.Cookie9Sided.toShape(),
            color = MaterialTheme.colorScheme.tertiaryContainer,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 36.dp, vertical = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                content()
            }
        }
    }
}
