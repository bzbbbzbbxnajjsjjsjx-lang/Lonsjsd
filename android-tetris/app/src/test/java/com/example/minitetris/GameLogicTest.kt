package com.example.minitetris

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameLogicTest {

    private fun emptyBoard() = List(ROWS) { List(COLS) { 0 } }

    private fun stateWith(shape: Shape, row: Int = 0, col: Int = 3, board: List<List<Int>> = emptyBoard()) =
        GameState(board = board, piece = Falling(shape, shape.cells, row, col), next = Shape.O)

    @Test
    fun rotatingFourTimesReturnsOriginalShape() {
        var cells = Shape.T.cells
        repeat(4) { cells = cells.rotated() }
        assertEquals(Shape.T.cells.toSet(), cells.toSet())
    }

    @Test
    fun pieceStopsAtWalls() {
        // The O piece's cells start one column in, so its leftmost legal origin is -1 and its rightmost is COLS - 3.
        assertEquals(-1, stateWith(Shape.O, col = -1).move(-1).piece!!.col)
        assertEquals(COLS - 3, stateWith(Shape.O, col = COLS - 3).move(1).piece!!.col)
    }

    @Test
    fun hardDropLocksPieceOnBottomAndSpawnsNext() {
        val s = stateWith(Shape.O).hardDrop()
        // The O piece covers columns 4 and 5 on the last two rows once it lands.
        assertEquals(Shape.O.ordinal + 1, s.board[ROWS - 1][4])
        assertEquals(Shape.O.ordinal + 1, s.board[ROWS - 2][5])
        assertNotNull(s.piece)
        assertEquals(0, s.piece!!.row)
    }

    @Test
    fun fullRowIsClearedAndScored() {
        // Bottom row is filled except columns 0 and 1, which an O piece completes.
        val bottom = List(COLS) { c -> if (c < 2) 0 else 1 }
        val board = List(ROWS) { r -> if (r == ROWS - 1) bottom else List(COLS) { 0 } }
        val s = stateWith(Shape.O, col = -1, board = board).hardDrop()
        assertEquals(1, s.lines)
        assertEquals(100, s.score)
        // The row above, which held the top of the O piece, drops into the cleared row.
        assertTrue(s.board[ROWS - 1][0] != 0)
        assertEquals(0, s.board[ROWS - 1][2])
    }

    @Test
    fun spawnIsBlockedOnFullBoard() {
        val full = List(ROWS) { List(COLS) { 1 } }
        assertFalse(fits(full, Shape.O.cells, 0, 3))
        assertTrue(fits(emptyBoard(), Shape.O.cells, 0, 3))
    }

    @Test
    fun gravityGetsFasterWithLevel() {
        assertTrue(GameState(lines = 0).gravityMs() > GameState(lines = 30).gravityMs())
        assertEquals(120L, GameState(lines = 1000).gravityMs())
    }

    @Test
    fun newGameStartsWithFallingPiece() {
        assertNotNull(newGame().piece)
        assertFalse(newGame().over)
        assertNull(GameState().piece)
    }
}
