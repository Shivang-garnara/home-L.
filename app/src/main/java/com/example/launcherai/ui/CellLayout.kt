package com.example.launcherai.ui

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
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

    var onEmptySpaceLongClickListener: ((cellX: Int, cellY: Int) -> Boolean)? = null

    fun findChildUnder(x: Float, y: Float): View? {
        for (i in (childCount - 1) downTo 0) {
            val child = getChildAt(i)
            if (child.visibility == VISIBLE) {
                if (x >= child.left && x <= child.right && y >= child.top && y <= child.bottom) {
                    return child
                }
            }
        }
        return null
    }

    private var lastTouchX = 0f
    private var lastTouchY = 0f

    private val gestureDetector = android.view.GestureDetector(context, object : android.view.GestureDetector.SimpleOnGestureListener() {
        override fun onLongPress(e: MotionEvent) {
            val child = findChildUnder(e.x, e.y)
            val cell = pointToCellExact(e.x, e.y)
            if (child == null) {
                parent?.requestDisallowInterceptTouchEvent(true)
                onEmptySpaceLongClickListener?.invoke(cell[0], cell[1])
            } else if (child is com.example.launcherai.widgets.LauncherAppWidgetHostView) {
                parent?.requestDisallowInterceptTouchEvent(true)
                child.performLongClick()
            }
        }
    })

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            lastTouchX = ev.x
            lastTouchY = ev.y
        } else if (ev.actionMasked == MotionEvent.ACTION_MOVE) {
            val touchSlop = android.view.ViewConfiguration.get(context).scaledTouchSlop
            if (Math.abs(ev.x - lastTouchX) > touchSlop || Math.abs(ev.y - lastTouchY) > touchSlop) {
                cancelLongPress()
            }
        }
        gestureDetector.onTouchEvent(ev)
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            lastTouchX = ev.x
            lastTouchY = ev.y
        } else if (ev.actionMasked == MotionEvent.ACTION_MOVE) {
            val touchSlop = android.view.ViewConfiguration.get(context).scaledTouchSlop
            if (Math.abs(ev.x - lastTouchX) > touchSlop || Math.abs(ev.y - lastTouchY) > touchSlop) {
                cancelLongPress()
            }
        }
        gestureDetector.onTouchEvent(ev)
        return true
    }

    private var mOccupied = Array(countX) { BooleanArray(countY) }

    private var dropTargetCellX: Int = -1
    private var dropTargetCellY: Int = -1
    private var dropTargetSpanX: Int = 1
    private var dropTargetSpanY: Int = 1
    private var dropTargetValid: Boolean = true
    private var isDropTargetActive: Boolean = false

    private val dropTargetFillPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.FILL
    }
    private val dropTargetStrokePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 2.5f * resources.displayMetrics.density
    }

    init {
        setWillNotDraw(false)
        resetOccupancyMatrix()
        isLongClickable = true
        setOnLongClickListener {
            val child = findChildUnder(lastTouchX, lastTouchY)
            if (child == null) {
                val cell = pointToCellExact(lastTouchX, lastTouchY)
                onEmptySpaceLongClickListener?.invoke(cell[0], cell[1]) ?: false
            } else {
                false
            }
        }
    }

    fun setDropTarget(cellX: Int, cellY: Int, spanX: Int, spanY: Int, isValid: Boolean) {
        if (dropTargetCellX != cellX || dropTargetCellY != cellY ||
            dropTargetSpanX != spanX || dropTargetSpanY != spanY ||
            dropTargetValid != isValid || !isDropTargetActive
        ) {
            dropTargetCellX = cellX
            dropTargetCellY = cellY
            dropTargetSpanX = spanX
            dropTargetSpanY = spanY
            dropTargetValid = isValid
            isDropTargetActive = true
            invalidate()
        }
    }

    fun clearDropTarget() {
        if (dropTargetCellX != -1 || dropTargetCellY != -1 || isDropTargetActive) {
            dropTargetCellX = -1
            dropTargetCellY = -1
            isDropTargetActive = false
            invalidate()
        }
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        super.onDraw(canvas)
        if (isDropTargetActive && dropTargetCellX >= 0 && dropTargetCellY >= 0) {
            val cellW = (width - paddingLeft - paddingRight) / maxOf(1, countX).toFloat()
            val cellH = (height - paddingTop - paddingBottom) / maxOf(1, countY).toFloat()

            val left = paddingLeft + dropTargetCellX * cellW + 4f
            val top = paddingTop + dropTargetCellY * cellH + 4f
            val right = left + dropTargetSpanX * cellW - 8f
            val bottom = top + dropTargetSpanY * cellH - 8f

            val cornerRadius = 14f * resources.displayMetrics.density

            if (dropTargetValid) {
                dropTargetFillPaint.color = 0x2A4285F4 // Translucent Google Blue
                dropTargetStrokePaint.color = 0x884285F4.toInt()
            } else {
                dropTargetFillPaint.color = 0x2AEA4335 // Translucent Red
                dropTargetStrokePaint.color = 0x88EA4335.toInt()
            }

            val rect = android.graphics.RectF(left, top, right, bottom)
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, dropTargetFillPaint)
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, dropTargetStrokePaint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        clearDropTarget()
    }

    private fun resetOccupancyMatrix() {
        mOccupied = Array(countX) { BooleanArray(countY) }
    }

    override fun removeAllViews() {
        super.removeAllViews()
        resetOccupancyMatrix()
        clearDropTarget()
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
        val prefs = context.getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        val overlapPlacing = prefs.getBoolean("key_overlap_when_placing", false)
        val widgetOverlap = prefs.getBoolean("key_widget_overlap", false)
        if (overlapPlacing || (widgetOverlap && (spanX > 1 || spanY > 1))) {
            return true
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
        val startX = cellX.coerceIn(0, countX)
        val endX = (cellX + spanX).coerceIn(0, countX)
        val startY = cellY.coerceIn(0, countY)
        val endY = (cellY + spanY).coerceIn(0, countY)
        for (x in startX until endX) {
            for (y in startY until endY) {
                mOccupied[x][y] = true
            }
        }
    }

    fun markCellsAsUnoccupied(cellX: Int, cellY: Int, spanX: Int = 1, spanY: Int = 1) {
        val startX = cellX.coerceIn(0, countX)
        val endX = (cellX + spanX).coerceIn(0, countX)
        val startY = cellY.coerceIn(0, countY)
        val endY = (cellY + spanY).coerceIn(0, countY)
        for (x in startX until endX) {
            for (y in startY until endY) {
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

    fun getCellWidth(): Int {
        val usableWidth = maxOf(0, width - paddingLeft - paddingRight)
        return if (usableWidth > 0) usableWidth / maxOf(1, countX) else (resources.displayMetrics.widthPixels - paddingLeft - paddingRight) / maxOf(1, countX)
    }

    fun getCellHeight(): Int {
        val usableHeight = maxOf(0, height - paddingTop - paddingBottom)
        return if (usableHeight > 0) usableHeight / maxOf(1, countY) else (resources.displayMetrics.heightPixels - paddingTop - paddingBottom) / maxOf(1, countY)
    }

    fun getSpanWidthPx(cellX: Int, spanX: Int): Int {
        val usableWidth = maxOf(0, width - paddingLeft - paddingRight)
        if (usableWidth <= 0) return getCellWidth() * spanX
        val leftPx = (usableWidth * cellX) / maxOf(1, countX)
        val rightPx = (usableWidth * (cellX + spanX)) / maxOf(1, countX)
        return maxOf(0, rightPx - leftPx)
    }

    fun getSpanHeightPx(cellY: Int, spanY: Int): Int {
        val usableHeight = maxOf(0, height - paddingTop - paddingBottom)
        if (usableHeight <= 0) return getCellHeight() * spanY
        val topPx = (usableHeight * cellY) / maxOf(1, countY)
        val bottomPx = (usableHeight * (cellY + spanY)) / maxOf(1, countY)
        return maxOf(0, bottomPx - topPx)
    }

    fun isRegionAvailableForChild(
        targetCellX: Int,
        targetCellY: Int,
        targetSpanX: Int,
        targetSpanY: Int,
        child: View
    ): Boolean {
        if (targetCellX < 0 || targetCellY < 0 ||
            targetCellX + targetSpanX > countX ||
            targetCellY + targetSpanY > countY
        ) {
            return false
        }

        val childLp = child.layoutParams as? LayoutParams

        for (x in targetCellX until targetCellX + targetSpanX) {
            for (y in targetCellY until targetCellY + targetSpanY) {
                if (mOccupied[x][y]) {
                    if (childLp != null) {
                        val isWithinChild = x in childLp.cellX until (childLp.cellX + childLp.cellHSpan) &&
                                y in childLp.cellY until (childLp.cellY + childLp.cellVSpan)
                        if (!isWithinChild) {
                            return false
                        }
                    } else {
                        return false
                    }
                }
            }
        }
        return true
    }

    fun updateChildSpanAndPosition(
        child: View,
        newCellX: Int,
        newCellY: Int,
        newSpanX: Int,
        newSpanY: Int
    ) {
        val lp = child.layoutParams as? LayoutParams ?: return
        markCellsAsUnoccupied(lp.cellX, lp.cellY, lp.cellHSpan, lp.cellVSpan)
        lp.cellX = newCellX
        lp.cellY = newCellY
        lp.cellHSpan = newSpanX
        lp.cellVSpan = newSpanY
        markCellsAsOccupied(newCellX, newCellY, newSpanX, newSpanY)
        clearDropTarget()
        requestLayout()
    }

    /**
     * Converts a local pixel coordinate (x, y) into grid column and row indexes (cellX, cellY).
     */
    fun pointToCellExact(x: Float, y: Float): IntArray {
        val usableW = maxOf(1, width - paddingLeft - paddingRight)
        val usableH = maxOf(1, height - paddingTop - paddingBottom)

        val relX = (x - paddingLeft).coerceIn(0f, usableW.toFloat())
        val relY = (y - paddingTop).coerceIn(0f, usableH.toFloat())

        val cellX = ((relX / usableW) * countX).toInt().coerceIn(0, countX - 1)
        val cellY = ((relY / usableH) * countY).toInt().coerceIn(0, countY - 1)

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
        var targetX = cellX.coerceIn(0, maxOf(0, countX - spanX))
        var targetY = cellY.coerceIn(0, maxOf(0, countY - spanY))

        if (preventOverlap && !isCellEmpty(targetX, targetY, spanX, spanY)) {
            val vacant = findVacantCell(spanX, spanY) ?: return false
            targetX = vacant[0]
            targetY = vacant[1]
        }

        markCellsAsOccupied(targetX, targetY, spanX, spanY)
        val lp = LayoutParams(targetX, targetY, spanX, spanY)
        addView(child, lp)
        clearDropTarget()
        return true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        val usableWidth = maxOf(0, width - paddingLeft - paddingRight)
        val usableHeight = maxOf(0, height - paddingTop - paddingBottom)

        val count = childCount
        for (i in 0 until count) {
            val child = getChildAt(i)
            if (child.visibility != GONE) {
                val lp = child.layoutParams as? LayoutParams
                val cellX = lp?.cellX ?: 0
                val cellY = lp?.cellY ?: 0
                val hSpan = lp?.cellHSpan ?: 1
                val vSpan = lp?.cellVSpan ?: 1
                val leftMargin = lp?.leftMargin ?: 0
                val rightMargin = lp?.rightMargin ?: 0
                val topMargin = lp?.topMargin ?: 0
                val bottomMargin = lp?.bottomMargin ?: 0

                val leftPx = (usableWidth * cellX) / maxOf(1, countX)
                val rightPx = (usableWidth * (cellX + hSpan)) / maxOf(1, countX)
                val topPx = (usableHeight * cellY) / maxOf(1, countY)
                val bottomPx = (usableHeight * (cellY + vSpan)) / maxOf(1, countY)

                val childWidth = maxOf(0, (rightPx - leftPx) - leftMargin - rightMargin)
                val childHeight = maxOf(0, (bottomPx - topPx) - topMargin - bottomMargin)

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

        val usableWidth = maxOf(0, width - paddingLeft - paddingRight)
        val usableHeight = maxOf(0, height - paddingTop - paddingBottom)

        val count = childCount
        for (i in 0 until count) {
            val child = getChildAt(i)
            if (child.visibility != GONE) {
                val lp = child.layoutParams as? LayoutParams ?: continue

                val leftPx = (usableWidth * lp.cellX) / maxOf(1, countX)
                val rightPx = (usableWidth * (lp.cellX + lp.cellHSpan)) / maxOf(1, countX)
                val topPx = (usableHeight * lp.cellY) / maxOf(1, countY)
                val bottomPx = (usableHeight * (lp.cellY + lp.cellVSpan)) / maxOf(1, countY)

                val childLeft = paddingLeft + leftPx + lp.leftMargin
                val childTop = paddingTop + topPx + lp.topMargin
                val childWidth = maxOf(0, (rightPx - leftPx) - lp.leftMargin - lp.rightMargin)
                val childHeight = maxOf(0, (bottomPx - topPx) - lp.topMargin - lp.bottomMargin)

                child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight)
                if (child is com.example.launcherai.widgets.LauncherAppWidgetHostView && childWidth > 0 && childHeight > 0) {
                    child.updateSizeOptionsForPixels(childWidth, childHeight)
                }
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
