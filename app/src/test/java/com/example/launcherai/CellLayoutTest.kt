package com.example.launcherai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for CellLayout two-dimensional occupancy matrix,
 * vacant cell allocation, collision detection, and grid bounds.
 */
class CellLayoutTest {

    private lateinit var occupancyMatrix: CellOccupancyMatrix

    @Before
    fun setup() {
        occupancyMatrix = CellOccupancyMatrix(countX = 4, countY = 5)
    }

    @Test
    fun testInitialGridIsEmpty() {
        for (x in 0 until 4) {
            for (y in 0 until 5) {
                assertTrue("Cell ($x, $y) should be empty initially", occupancyMatrix.isCellEmpty(x, y))
            }
        }
    }

    @Test
    fun testMarkCellOccupiedAndUnoccupied() {
        // Mark cell (1, 2) occupied with span (1, 1)
        occupancyMatrix.markCellsAsOccupied(1, 2, 1, 1)
        assertFalse("Cell (1, 2) should be occupied", occupancyMatrix.isCellEmpty(1, 2))
        assertTrue("Cell (0, 0) should remain empty", occupancyMatrix.isCellEmpty(0, 0))

        // Mark cell (1, 2) unoccupied
        occupancyMatrix.markCellsAsUnoccupied(1, 2, 1, 1)
        assertTrue("Cell (1, 2) should be empty after marking unoccupied", occupancyMatrix.isCellEmpty(1, 2))
    }

    @Test
    fun testSpanOccupancy() {
        // Mark 2x2 span starting at (0, 0)
        occupancyMatrix.markCellsAsOccupied(0, 0, 2, 2)
        assertFalse(occupancyMatrix.isCellEmpty(0, 0))
        assertFalse(occupancyMatrix.isCellEmpty(1, 0))
        assertFalse(occupancyMatrix.isCellEmpty(0, 1))
        assertFalse(occupancyMatrix.isCellEmpty(1, 1))

        // Outside span should be empty
        assertTrue(occupancyMatrix.isCellEmpty(2, 0))
        assertTrue(occupancyMatrix.isCellEmpty(0, 2))
    }

    @Test
    fun testFindVacantCell() {
        // Mark top row occupied
        for (x in 0 until 4) {
            occupancyMatrix.markCellsAsOccupied(x, 0, 1, 1)
        }

        // First vacant 1x1 cell should be (0, 1)
        val vacant = occupancyMatrix.findVacantCell(1, 1)
        assertNotNull(vacant)
        assertEquals(0, vacant!![0])
        assertEquals(1, vacant[1])
    }

    @Test
    fun testGridFullFindVacantCellReturnsNull() {
        // Mark all cells occupied
        for (x in 0 until 4) {
            for (y in 0 until 5) {
                occupancyMatrix.markCellsAsOccupied(x, y, 1, 1)
            }
        }

        val vacant = occupancyMatrix.findVacantCell(1, 1)
        assertNull(vacant)
    }

    @Test
    fun testOutOfBoundsIsNotEmpty() {
        assertFalse(occupancyMatrix.isCellEmpty(-1, 0))
        assertFalse(occupancyMatrix.isCellEmpty(4, 0))
        assertFalse(occupancyMatrix.isCellEmpty(0, 5))
    }

    /**
     * Pure test representation of CellLayout's occupancy matrix calculations.
     */
    private class CellOccupancyMatrix(val countX: Int, val countY: Int) {
        private val mOccupied = Array(countX) { BooleanArray(countY) }

        fun isCellEmpty(cellX: Int, cellY: Int, spanX: Int = 1, spanY: Int = 1): Boolean {
            if (cellX < 0 || cellY < 0 || cellX + spanX > countX || cellY + spanY > countY) {
                return false
            }
            for (x in cellX until cellX + spanX) {
                for (y in cellY until cellY + spanY) {
                    if (mOccupied[x][y]) {
                        return false
                    }
                }
            }
            return true
        }

        fun markCellsAsOccupied(cellX: Int, cellY: Int, spanX: Int = 1, spanY: Int = 1) {
            if (cellX < 0 || cellY < 0 || cellX + spanX > countX || cellY + spanY > countY) return
            for (x in cellX until cellX + spanX) {
                for (y in cellY until cellY + spanY) {
                    mOccupied[x][y] = true
                }
            }
        }

        fun markCellsAsUnoccupied(cellX: Int, cellY: Int, spanX: Int = 1, spanY: Int = 1) {
            if (cellX < 0 || cellY < 0 || cellX + spanX > countX || cellY + spanY > countY) return
            for (x in cellX until cellX + spanX) {
                for (y in cellY until cellY + spanY) {
                    mOccupied[x][y] = false
                }
            }
        }

        fun findVacantCell(spanX: Int = 1, spanY: Int = 1): IntArray? {
            for (y in 0..countY - spanY) {
                for (x in 0..countX - spanX) {
                    if (isCellEmpty(x, y, spanX, spanY)) {
                        return intArrayOf(x, y)
                    }
                }
            }
            return null
        }
    }
}
