package com.example.launcherai.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Interactive preview of the desktop grid dimensions (rows x cols) matching Nova Launcher's GridPreviewView.
 * Renders a phone mockup frame with status bar, dock, and dynamic grid cells with app icon placeholders.
 */
class GridPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var rows: Int = 6
        set(value) {
            field = value.coerceIn(3, 12)
            invalidate()
        }

    var cols: Int = 5
        set(value) {
            field = value.coerceIn(3, 12)
            invalidate()
        }

    private val density = resources.displayMetrics.density

    // Paints
    private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#44332A")
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }

    private val screenBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFF4EE")
        style = Paint.Style.FILL
    }

    private val gridLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#55D54526")
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
        pathEffect = DashPathEffect(floatArrayOf(4f * density, 4f * density), 0f)
    }

    private val dockLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8853433F")
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
    }

    private val iconDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D54526")
        style = Paint.Style.FILL
    }

    private val dockDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#7D5245")
        style = Paint.Style.FILL
    }

    private val phoneRect = RectF()

    fun setGrid(newRows: Int, newCols: Int) {
        rows = newRows
        cols = newCols
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val padding = 12f * density
        val viewW = width.toFloat()
        val viewH = height.toFloat()

        if (viewW <= padding * 2 || viewH <= padding * 2) return

        // Compute phone mockup bounds maintaining ~9:18 aspect ratio centered
        val maxAvailableW = viewW - padding * 2
        val maxAvailableH = viewH - padding * 2

        var phoneW = maxAvailableW
        var phoneH = phoneW * 2.05f

        if (phoneH > maxAvailableH) {
            phoneH = maxAvailableH
            phoneW = phoneH / 2.05f
        }

        val left = (viewW - phoneW) / 2f
        val top = (viewH - phoneH) / 2f
        phoneRect.set(left, top, left + phoneW, top + phoneH)

        val cornerRadius = 18f * density

        // Draw phone screen background and outer frame
        canvas.drawRoundRect(phoneRect, cornerRadius, cornerRadius, screenBgPaint)
        canvas.drawRoundRect(phoneRect, cornerRadius, cornerRadius, framePaint)

        // Draw camera cutout / speaker notch
        val speakerW = 28f * density
        val speakerH = 4f * density
        val speakerLeft = phoneRect.centerX() - speakerW / 2f
        val speakerTop = phoneRect.top + 6f * density
        canvas.drawRoundRect(
            speakerLeft, speakerTop, speakerLeft + speakerW, speakerTop + speakerH,
            2f * density, 2f * density, framePaint
        )

        // Partition screen: Status bar (top 20dp), Workspace, Dock (bottom 36dp)
        val statusBarH = 16f * density
        val dockH = 34f * density

        val workspaceTop = phoneRect.top + statusBarH + 6f * density
        val workspaceBottom = phoneRect.bottom - dockH
        val workspaceLeft = phoneRect.left + 8f * density
        val workspaceRight = phoneRect.right - 8f * density

        // Draw dock separator line
        canvas.drawLine(
            phoneRect.left + 12f * density,
            workspaceBottom,
            phoneRect.right - 12f * density,
            workspaceBottom,
            dockLinePaint
        )

        // Draw dock icon dots (e.g. 5 dots in dock)
        val dockIconCount = 5
        val dockStepX = (phoneRect.width() - 24f * density) / dockIconCount
        val dockCenterY = workspaceBottom + dockH / 2f
        for (i in 0 until dockIconCount) {
            val cx = phoneRect.left + 12f * density + dockStepX * (i + 0.5f)
            canvas.drawCircle(cx, dockCenterY, 3.5f * density, dockDotPaint)
        }

        // Draw workspace grid
        val wsW = workspaceRight - workspaceLeft
        val wsH = workspaceBottom - workspaceTop

        val cellW = wsW / maxOf(1, cols)
        val cellH = wsH / maxOf(1, rows)

        // Vertical grid lines
        for (c in 1 until cols) {
            val gx = workspaceLeft + c * cellW
            canvas.drawLine(gx, workspaceTop, gx, workspaceBottom, gridLinePaint)
        }

        // Horizontal grid lines
        for (r in 1 until rows) {
            val gy = workspaceTop + r * cellH
            canvas.drawLine(workspaceLeft, gy, workspaceRight, gy, gridLinePaint)
        }

        // Draw icon dots in each grid cell
        val dotRadius = minOf(cellW, cellH) * 0.22f
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val cx = workspaceLeft + c * cellW + cellW / 2f
                val cy = workspaceTop + r * cellH + cellH / 2f
                canvas.drawCircle(cx, cy, dotRadius, iconDotPaint)
            }
        }
    }
}
