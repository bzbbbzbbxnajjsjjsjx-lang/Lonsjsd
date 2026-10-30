package com.example.minitetris

const val COLS = 10
const val ROWS = 20

/** The seven tetrominoes, as (row, col) offsets in their spawn orientation. */
enum class Shape(val cells: List<Pair<Int, Int>>) {
    I(listOf(0 to 0, 0 to 1, 0 to 2, 0 to 3)),
    O(listOf(0 to 1, 0 to 2, 1 to 1, 1 to 2)),
    T(listOf(0 to 0, 0 to 1, 0 to 2, 1 to 1)),
    S(listOf(0 to 1, 0 to 2, 1 to 0, 1 to 1)),
    Z(listOf(0 to 0, 0 to 1, 1 to 1, 1 to 2)),
    J(listOf(0 to 0, 1 to 0, 1 to 1, 1 to 2)),
    L(listOf(0 to 2, 1 to 0, 1 to 1, 1 to 2)),
}

/** Turns the offsets 90 degrees clockwise and moves them back to the top-left corner. */
fun List<Pair<Int, Int>>.rotated(): List<Pair<Int, Int>> {
    val turned = map { (r, c) -> c to -r }
    val minR = turned.minOf { it.first }
    val minC = turned.minOf { it.second }
    return turned.map { (r, c) -> (r - minR) to (c - minC) }
}

/** The piece in play: its shape, current offsets, and its top-left position on the board. */
data class Falling(val shape: Shape, val cells: List<Pair<Int, Int>>, val row: Int, val col: Int)

/**
 * Whole game state. Every action returns a new state, so the UI only has to hold one value.
 * Board cells are 0 when empty, otherwise shape ordinal + 1.
 */
data class GameState(
    val board: List<List<Int>> = List(ROWS) { List(COLS) { 0 } },
    val piece: Falling? = null,
    val next: Shape = Shape.entries.random(),
    val score: Int = 0,
    val lines: Int = 0,
    val over: Boolean = false,
) {
    val level: Int get() = lines / 10 + 1

    /** Milliseconds between automatic drops; faster as the level rises. */
    fun gravityMs(): Long = maxOf(120L, 800L - (level - 1) * 70L)

    fun move(dx: Int): GameState {
        val p = piece ?: return this
        return if (fits(board, p.cells, p.row, p.col + dx)) copy(piece = p.copy(col = p.col + dx)) else this
    }

    /** Rotates, trying a few sideways offsets so a piece can turn next to a wall. */
    fun rotate(): GameState {
        val p = piece ?: return this
        val turned = p.cells.rotated()
        for (kick in listOf(0, -1, 1, -2, 2)) {
            if (fits(board, turned, p.row, p.col + kick)) {
                return copy(piece = p.copy(cells = turned, col = p.col + kick))
            }
        }
        return this
    }

    /** One gravity tick: move down, or lock the piece when it cannot move down. */
    fun step(): GameState {
        val p = piece ?: return this
        return if (fits(board, p.cells, p.row + 1, p.col)) copy(piece = p.copy(row = p.row + 1)) else lock()
    }

    fun hardDrop(): GameState {
        val p = piece ?: return this
        var row = p.row
        while (fits(board, p.cells, row + 1, p.col)) row++
        return copy(piece = p.copy(row = row)).lock()
    }

    /** Writes the falling piece into the board, clears full rows, scores, and spawns the next piece. */
    private fun lock(): GameState {
        val p = piece ?: return this
        val filled = board.map { it.toMutableList() }
        for ((r, c) in p.cells) {
            filled[p.row + r][p.col + c] = p.shape.ordinal + 1
        }
        val kept = filled.filter { row -> row.any { it == 0 } }.map { it.toList() }
        val cleared = ROWS - kept.size
        val newBoard = List(cleared) { List(COLS) { 0 } } + kept
        val spawned = next
        val spawnCells = spawned.cells
        val spawnFalling = Falling(spawned, spawnCells, row = 0, col = 3)
        val blocked = !fits(newBoard, spawnCells, 0, 3)
        return copy(
            board = newBoard,
            piece = if (blocked) null else spawnFalling,
            next = Shape.entries.random(),
            score = score + linePoints(cleared) * level,
            lines = lines + cleared,
            over = blocked,
        )
    }

    private fun linePoints(n: Int): Int = when (n) {
        1 -> 100
        2 -> 300
        3 -> 500
        4 -> 800
        else -> 0
    }
}

/** True when every cell of the piece, placed at (row, col), is inside the board and on an empty cell. */
fun fits(board: List<List<Int>>, cells: List<Pair<Int, Int>>, row: Int, col: Int): Boolean =
    cells.all { (r, c) ->
        val rr = row + r
        val cc = col + c
        cc in 0 until COLS && rr < ROWS && (rr < 0 || board[rr][cc] == 0)
    }

/** A fresh game with the first piece already falling and one piece queued as "next". */
fun newGame(): GameState {
    val first = Shape.entries.random()
    return GameState(piece = Falling(first, first.cells, row = 0, col = 3), next = Shape.entries.random())
}
