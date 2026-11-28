package com.example.launcherai.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable

/**
 * Draws Nova Launcher's circular Surface Style preview badge:
 * - Main circle filled with the surface color (with a subtle border)
 * - Optional "TT" glyph in the center using effective text color
 * - Bottom-right teardrop / rounded accent badge
 */
class SurfacePreviewBadgeDrawable(
    private val surfaceColor: Int,
    private val textColor: Int,
    private val accentColor: Int,
    private val showText: Boolean = true
) : Drawable() {

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0x33000000
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val dropPath = Path()

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.isEmpty) return

        val size = minOf(b.width(), b.height()).toFloat()
        val cx = b.exactCenterX() - size * 0.06f
        val cy = b.exactCenterY() - size * 0.06f
        val radius = size * 0.38f

        // Surface circle
        fillPaint.color = if (Color.alpha(surfaceColor) == 0) 0x1AFFFFFF else surfaceColor
        canvas.drawCircle(cx, cy, radius, fillPaint)
        strokePaint.strokeWidth = (size * 0.03f).coerceAtLeast(1.5f)
        canvas.drawCircle(cx, cy, radius, strokePaint)

        // "TT" text inside circle
        if (showText) {
            textPaint.color = textColor
            textPaint.textSize = radius * 0.85f
            val yOffset = (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText("T", cx - radius * 0.22f, cy - yOffset, textPaint)
            textPaint.textSize = radius * 0.62f
            val yOffsetSmall = (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText("T", cx + radius * 0.26f, cy - yOffsetSmall + radius * 0.08f, textPaint)
        }

        // Bottom-right accent drop (rounded-corner square with top-left sharp corner, matching Nova)
        val dropSize = radius * 0.88f
        val dropLeft = cx + radius * 0.25f
        val dropTop = cy + radius * 0.25f
        val rect = RectF(dropLeft, dropTop, dropLeft + dropSize, dropTop + dropSize)
        val r = dropSize * 0.45f
        dropPath.reset()
        dropPath.addRoundRect(
            rect,
            floatArrayOf(r * 0.25f, r * 0.25f, r, r, r, r, r, r),
            Path.Direction.CW
        )
        fillPaint.color = accentColor
        canvas.drawPath(dropPath, fillPaint)
    }

    override fun setAlpha(alpha: Int) {
        fillPaint.alpha = alpha
        strokePaint.alpha = alpha
        textPaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        fillPaint.colorFilter = colorFilter
        strokePaint.colorFilter = colorFilter
        textPaint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
