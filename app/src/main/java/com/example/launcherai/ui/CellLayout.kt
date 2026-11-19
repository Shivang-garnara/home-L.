package com.example.launcherai.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup

/**
 * Reusable two-dimensional CellLayout component for the home-screen workspace.
 * Manages item positioning, grid cell occupancy matrix, collision detection,
 * and cell dimensions calculation.
 */
class CellLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    var countX: Int = 4
        set(value) {
            field = value
            resetOccupancyMatrix()
            requestLayout()
        }

    var countY: Int = 5
        set(value) {
            field = value
            resetOccupancyMatrix()
            requestLayout()
        }

    private var mOccupied = Array(countX) { BooleanArray(countY) }

    init {
        resetOccupancyMatrix()
    }

    private fun resetOccupancyMatrix() {
        mOccupied = Array(countX) { BooleanArray(countY) }
    }

    override fun removeAllViews() {
        super.removeAllViews()
        resetOccupancyMatrix()
    }

    override fun removeView(view: View?) {
        if (view != null && view.layoutParams is LayoutParams) {
            val lp = view.layoutParams as LayoutParams
            markCellsAsUnoccupied(lp.cellX, lp.cellY, lp.cellHSpan, lp.cellVSpan)
        }
        super.removeView(view)
    }

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

    /**
     * Converts a local pixel coordinate (x, y) into grid column and row indexes (cellX, cellY).
     */
    fun pointToCellExact(x: Float, y: Float): IntArray {
        val cellW = (width - paddingLeft - paddingRight) / maxOf(1, countX)
        val cellH = (height - paddingTop - paddingBottom) / maxOf(1, countY)

        val relX = x - paddingLeft
        val relY = y - paddingTop

        val cellX = (relX / maxOf(1, cellW)).toInt().coerceIn(0, countX - 1)
        val cellY = (relY / maxOf(1, cellH)).toInt().coerceIn(0, countY - 1)

        return intArrayOf(cellX, cellY)
    }

    /**
     * Finds the child View occupying grid coordinate (cellX, cellY), if any.
     */
    fun getChildAtCell(cellX: Int, cellY: Int): View? {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val lp = child.layoutParams as? LayoutParams ?: continue
            if (cellX in lp.cellX until lp.cellX + lp.cellHSpan &&
                cellY in lp.cellY until lp.cellY + lp.cellVSpan) {
                return child
            }
        }
        return null
    }

    /**
     * Adds a child view to a specific grid cell position with collision check.
     */
    fun addViewToCell(
        child: View,
        cellX: Int,
        cellY: Int,
        spanX: Int = 1,
        spanY: Int = 1,
        preventOverlap: Boolean = true
    ): Boolean {
        var targetX = cellX
        var targetY = cellY

        if (preventOverlap && !isCellEmpty(targetX, targetY, spanX, spanY)) {
            val vacant = findVacantCell(spanX, spanY) ?: return false
            targetX = vacant[0]
            targetY = vacant[1]
        }

        markCellsAsOccupied(targetX, targetY, spanX, spanY)
        val lp = LayoutParams(targetX, targetY, spanX, spanY)
        addView(child, lp)
        return true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        val cellWidth = (width - paddingLeft - paddingRight) / maxOf(1, countX)
        val cellHeight = (height - paddingTop - paddingBottom) / maxOf(1, countY)

        val count = childCount
        for (i in 0 until count) {
            val child = getChildAt(i)
            if (child.visibility != GONE) {
                val lp = child.layoutParams as? LayoutParams
                val hSpan = lp?.cellHSpan ?: 1
                val vSpan = lp?.cellVSpan ?: 1
                val leftMargin = lp?.leftMargin ?: 0
                val rightMargin = lp?.rightMargin ?: 0
                val topMargin = lp?.topMargin ?: 0
                val bottomMargin = lp?.bottomMargin ?: 0

                val childWidth = maxOf(0, cellWidth * hSpan - leftMargin - rightMargin)
                val childHeight = maxOf(0, cellHeight * vSpan - topMargin - bottomMargin)

                child.measure(
                    MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY)
                )
            }
        }

        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val width = r - l
        val height = b - t

        val cellWidth = (width - paddingLeft - paddingRight) / maxOf(1, countX)
        val cellHeight = (height - paddingTop - paddingBottom) / maxOf(1, countY)

        val count = childCount
        for (i in 0 until count) {
            val child = getChildAt(i)
            if (child.visibility != GONE) {
                val lp = child.layoutParams as? LayoutParams ?: continue

                val childLeft = paddingLeft + lp.cellX * cellWidth + lp.leftMargin
                val childTop = paddingTop + lp.cellY * cellHeight + lp.topMargin
                val childWidth = maxOf(0, cellWidth * lp.cellHSpan - lp.leftMargin - lp.rightMargin)
                val childHeight = maxOf(0, cellHeight * lp.cellVSpan - lp.topMargin - lp.bottomMargin)

                child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight)
            }
        }
    }

    override fun generateLayoutParams(attrs: AttributeSet?): ViewGroup.LayoutParams {
        return LayoutParams(context, attrs)
    }

    override fun generateDefaultLayoutParams(): ViewGroup.LayoutParams {
        return LayoutParams(0, 0, 1, 1)
    }

    override fun generateLayoutParams(p: ViewGroup.LayoutParams?): ViewGroup.LayoutParams {
        return LayoutParams(p ?: generateDefaultLayoutParams())
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams?): Boolean {
        return p is LayoutParams
    }

    class LayoutParams : MarginLayoutParams {
        var cellX: Int = 0
        var cellY: Int = 0
        var cellHSpan: Int = 1
        var cellVSpan: Int = 1

        constructor(cellX: Int, cellY: Int, cellHSpan: Int = 1, cellVSpan: Int = 1) : super(
            MATCH_PARENT,
            MATCH_PARENT
        ) {
            this.cellX = cellX
            this.cellY = cellY
            this.cellHSpan = cellHSpan
            this.cellVSpan = cellVSpan
        }

        constructor(c: Context, attrs: AttributeSet?) : super(c, attrs)
        constructor(source: ViewGroup.LayoutParams) : super(source)
    }
}
