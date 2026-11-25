package com.example.launcherai.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Miniature and interactive dialog block-grid preview matching Nova Launcher's FancyPrefGridView
 * for Folder Immersive Grid selection (rows x cols of rounded app icon slots inside a card frame).
 */
class FolderGridBlockPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var rows: Int = 3
        set(value) {
            field = value.coerceIn(2, 7)
            invalidate()
        }

    var cols: Int = 4
        set(value) {
            field = value.coerceIn(3, 7)
            invalidate()
        }

    var isMiniThumbnail: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    private val density = resources.displayMetrics.density

    private val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E8DFE8")
        style = Paint.Style.FILL
    }

    private val cardStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6D3B68")
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
    }

    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6D3B68")
        style = Paint.Style.FILL
    }

    private val frameRect = RectF()

    fun setGrid(newRows: Int, newCols: Int) {
        rows = newRows
        cols = newCols
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 4f || h <= 4f) return

        val pad = if (isMiniThumbnail) 2f * density else 6f * density
        frameRect.set(pad, pad, w - pad, h - pad)
        val corner = if (isMiniThumbnail) 6f * density else 16f * density
        canvas.drawRoundRect(frameRect, corner, corner, cardBgPaint)
        if (!isMiniThumbnail) {
            canvas.drawRoundRect(frameRect, corner, corner, cardStrokePaint)
        }

        val innerPad = if (isMiniThumbnail) 5f * density else 14f * density
        val availW = (frameRect.width() - innerPad * 2).coerceAtLeast(1f)
        val availH = (frameRect.height() - innerPad * 2).coerceAtLeast(1f)

        val cellW = availW / cols
        val cellH = availH / rows
        val dotRadius = (minOf(cellW, cellH) * if (isMiniThumbnail) 0.28f else 0.30f).coerceAtLeast(1.5f)

        cellPaint.alpha = if (isMiniThumbnail) 200 else 185
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val cx = frameRect.left + innerPad + c * cellW + cellW / 2f
                val cy = frameRect.top + innerPad + r * cellH + cellH / 2f
                canvas.drawCircle(cx, cy, dotRadius, cellPaint)
            }
        }
    }
}
