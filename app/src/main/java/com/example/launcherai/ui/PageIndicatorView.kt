package com.example.launcherai.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.ColorUtils

/**
 * Animated page indicator component for the paged workspace.
 * Supports:
 * - DOTS: Circles for inactive pages and an animated gliding active dot.
 * - LINE: Modern track line with an animated sliding active indicator pill.
 * - NONE: Hidden indicator.
 * - Custom accent color tinting.
 */
class PageIndicatorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class Style {
        DOTS,
        LINE,
        NONE;

        companion object {
            fun fromString(name: String?): Style {
                return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: DOTS
            }
        }
    }

    private var pageCount: Int = 0
    private var activePosition: Int = 0
    private var positionOffset: Float = 0f
    private var currentStyle: Style = Style.DOTS
    private var indicatorColor: Int = Color.WHITE

    private val inactivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x66FFFFFF.toInt()
        style = Paint.Style.FILL
    }

    private val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.FILL
    }

    private val lineTrackRect = RectF()
    private val linePillRect = RectF()

    private val dotRadius = dpToPx(3.5f)
    private val activeDotRadius = dpToPx(4.5f)
    private val dotSpacing = dpToPx(16f)

    private val lineTrackHeight = dpToPx(3f)
    private val lineTrackWidth = dpToPx(80f)
    private val linePillWidth = dpToPx(28f)

    fun setStyle(style: Style) {
        if (this.currentStyle != style) {
            this.currentStyle = style
            visibility = if (style == Style.NONE) GONE else VISIBLE
            requestLayout()
            invalidate()
        }
    }

    fun setIndicatorColor(color: Int) {
        this.indicatorColor = color
        activePaint.color = color
        inactivePaint.color = ColorUtils.setAlphaComponent(color, 90)
        invalidate()
    }

    fun setPageCount(count: Int) {
        if (this.pageCount != count) {
            this.pageCount = count
            requestLayout()
            invalidate()
        }
    }

    fun onPageScrolled(position: Int, offset: Float) {
        this.activePosition = position
        this.positionOffset = offset
        invalidate()
    }

    fun onPageSelected(position: Int) {
        this.activePosition = position
        this.positionOffset = 0f
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (currentStyle == Style.NONE || pageCount <= 1) {
            setMeasuredDimension(0, 0)
            return
        }

        val desiredWidth = when (currentStyle) {
            Style.DOTS -> ((pageCount - 1) * dotSpacing + activeDotRadius * 4).toInt() + paddingLeft + paddingRight
            Style.LINE -> (lineTrackWidth + dpToPx(16f)).toInt() + paddingLeft + paddingRight
            Style.NONE -> 0
        }
        val desiredHeight = (activeDotRadius * 2.5f).toInt() + paddingTop + paddingBottom

        setMeasuredDimension(
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (currentStyle == Style.NONE || pageCount <= 1) return

        when (currentStyle) {
            Style.DOTS -> drawDots(canvas)
            Style.LINE -> drawLine(canvas)
            Style.NONE -> { /* no-op */ }
        }
    }

    private fun drawDots(canvas: Canvas) {
        val totalWidth = (pageCount - 1) * dotSpacing
        val startX = (width - totalWidth) / 2f
        val centerY = height / 2f

        // Draw inactive dots
        for (i in 0 until pageCount) {
            val cx = startX + i * dotSpacing
            canvas.drawCircle(cx, centerY, dotRadius, inactivePaint)
        }

        // Draw sliding active indicator dot
        val activeCx = startX + (activePosition + positionOffset) * dotSpacing
        canvas.drawCircle(activeCx, centerY, activeDotRadius, activePaint)
    }

    private fun drawLine(canvas: Canvas) {
        val startX = (width - lineTrackWidth) / 2f
        val centerY = height / 2f
        val top = centerY - lineTrackHeight / 2f
        val bottom = centerY + lineTrackHeight / 2f

        // Background track line
        lineTrackRect.set(startX, top, startX + lineTrackWidth, bottom)
        canvas.drawRoundRect(lineTrackRect, lineTrackHeight / 2f, lineTrackHeight / 2f, inactivePaint)

        // Sliding active pill
        val availableTravel = lineTrackWidth - linePillWidth
        val progress = if (pageCount > 1) {
            ((activePosition + positionOffset) / (pageCount - 1)).coerceIn(0f, 1f)
        } else 0f

        val pillStart = startX + progress * availableTravel
        linePillRect.set(pillStart, top, pillStart + linePillWidth, bottom)
        canvas.drawRoundRect(linePillRect, lineTrackHeight / 2f, lineTrackHeight / 2f, activePaint)
    }

    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }
}
