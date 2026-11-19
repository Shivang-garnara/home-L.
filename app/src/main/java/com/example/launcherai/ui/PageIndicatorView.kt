package com.example.launcherai.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * Animated dot page indicator component for the paged workspace.
 * Synchronizes dot positions and smooth sliding animation with ViewPager2 scroll offsets.
 */
class PageIndicatorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var pageCount: Int = 0
    private var activePosition: Int = 0
    private var positionOffset: Float = 0f

    private val inactivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x66FFFFFF.toInt()
        style = Paint.Style.FILL
    }

    private val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.FILL
    }

    private val dotRadius = dpToPx(4f)
    private val activeDotRadius = dpToPx(5f)
    private val dotSpacing = dpToPx(16f)

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
        val desiredWidth = if (pageCount > 0) {
            ((pageCount - 1) * dotSpacing + activeDotRadius * 4).toInt() + paddingLeft + paddingRight
        } else 0
        val desiredHeight = (activeDotRadius * 2.5f).toInt() + paddingTop + paddingBottom

        setMeasuredDimension(
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (pageCount <= 1) return

        val totalWidth = (pageCount - 1) * dotSpacing
        val startX = (width - totalWidth) / 2f
        val centerY = height / 2f

        // Draw inactive background dots
        for (i in 0 until pageCount) {
            val cx = startX + i * dotSpacing
            canvas.drawCircle(cx, centerY, dotRadius, inactivePaint)
        }

        // Draw sliding active indicator dot
        val activeCx = startX + (activePosition + positionOffset) * dotSpacing
        canvas.drawCircle(activeCx, centerY, activeDotRadius, activePaint)
    }

    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }
}
