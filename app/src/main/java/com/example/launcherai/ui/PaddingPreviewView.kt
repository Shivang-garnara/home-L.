package com.example.launcherai.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Interactive preview of workspace edge padding matching Nova Launcher's PaddingPreviewView.
 * Renders a phone screen with an inner workspace card whose margins dynamically contract/expand
 * as horizontal and vertical padding sliders are adjusted.
 */
class PaddingPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Steps 0..3: None (0dp), Small (8dp), Medium (16dp), Large (24dp)
    var horizontalStep: Int = 0
        set(value) {
            field = value.coerceIn(0, 3)
            invalidate()
        }

    var verticalStep: Int = 0
        set(value) {
            field = value.coerceIn(0, 3)
            invalidate()
        }

    private val density = resources.displayMetrics.density

    private val outerFramePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#44332A")
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }

    private val outerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EFE6DF")
        style = Paint.Style.FILL
    }

    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        setShadowLayer(8f * density, 0f, 4f * density, Color.parseColor("#33000000"))
    }

    private val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D54526")
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D54526")
        style = Paint.Style.FILL
    }

    private val outerRect = RectF()
    private val cardRect = RectF()

    init {
        // Need software layer for shadow drawing
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun setPaddingSteps(horiz: Int, vert: Int) {
        horizontalStep = horiz
        verticalStep = vert
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val padding = 16f * density
        val viewW = width.toFloat()
        val viewH = height.toFloat()

        if (viewW <= padding * 2 || viewH <= padding * 2) return

        val maxAvailableW = viewW - padding * 2
        val maxAvailableH = viewH - padding * 2

        var phoneW = maxAvailableW
        var phoneH = phoneW * 1.6f

        if (phoneH > maxAvailableH) {
            phoneH = maxAvailableH
            phoneW = phoneH / 1.6f
        }

        val left = (viewW - phoneW) / 2f
        val top = (viewH - phoneH) / 2f
        outerRect.set(left, top, left + phoneW, top + phoneH)

        val cornerRadius = 14f * density

        // Draw phone screen
        canvas.drawRoundRect(outerRect, cornerRadius, cornerRadius, outerBgPaint)
        canvas.drawRoundRect(outerRect, cornerRadius, cornerRadius, outerFramePaint)

        // Margin offsets based on step (None=4dp, Small=10dp, Medium=18dp, Large=26dp)
        val marginX = (4f + horizontalStep * 7f) * density
        val marginY = (4f + verticalStep * 7f) * density

        cardRect.set(
            outerRect.left + marginX,
            outerRect.top + marginY,
            outerRect.right - marginX,
            outerRect.bottom - marginY
        )

        val cardCorner = 10f * density
        canvas.drawRoundRect(cardRect, cardCorner, cardCorner, cardPaint)
        canvas.drawRoundRect(cardRect, cardCorner, cardCorner, cardBorderPaint)

        // Draw 3x4 small sample dots inside card to simulate icons
        val rows = 4
        val cols = 3
        val cellW = cardRect.width() / cols
        val cellH = cardRect.height() / rows
        val dotR = 3.5f * density

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val cx = cardRect.left + c * cellW + cellW / 2f
                val cy = cardRect.top + r * cellH + cellH / 2f
                canvas.drawCircle(cx, cy, dotR, dotPaint)
            }
        }
    }
}
