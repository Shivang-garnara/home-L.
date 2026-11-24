package com.example.launcherai.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.example.launcherai.R

/**
 * Fast Scroll A–Z Accent Bar matching Nova Launcher's AllAppsFastScroll rail.
 * Displays vertical section letters (#, A–Z) with touch tracking, letter highlighting,
 * and accent color indicator.
 */
class FastScrollRailView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val alphabet = arrayOf(
        "#", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L",
        "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"
    )

    private val normalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x8853433F.toInt()
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT
    }

    private val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.nova_accent_orange)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val thumbTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x22D54526.toInt()
        style = Paint.Style.FILL
    }

    private var activeIndex = -1
    private var scrollRatio = 0f
    private var isDragging = false

    var accentColor: Int = ContextCompat.getColor(context, R.color.nova_accent_orange)
        set(value) {
            field = value
            activePaint.color = value
            thumbTrackPaint.color = (value and 0x00FFFFFF) or 0x26000000.toInt()
            invalidate()
        }

    var onSectionChangeListener: ((letter: String, touchY: Float) -> Unit)? = null
    var onScrollEndListener: (() -> Unit)? = null

    init {
        val density = resources.displayMetrics.density
        normalPaint.textSize = 9.5f * density
        activePaint.textSize = 11f * density
    }

    fun setScrollProgress(ratio: Float) {
        if (!isDragging) {
            this.scrollRatio = ratio.coerceIn(0f, 1f)
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val availableHeight = height - paddingTop - paddingBottom
        if (availableHeight <= 0) return

        val itemHeight = availableHeight / alphabet.size.toFloat()
        val centerX = width / 2f

        // Draw track indicator when scrolling
        if (scrollRatio > 0f && !isDragging) {
            val thumbY = paddingTop + (scrollRatio * (availableHeight - itemHeight))
            canvas.drawCircle(centerX, thumbY + (itemHeight / 2f), width / 3f, thumbTrackPaint)
        }

        for (i in alphabet.indices) {
            val letterY = paddingTop + (i * itemHeight) + (itemHeight / 2f) + (normalPaint.textSize / 3f)
            if (i == activeIndex) {
                // Draw accent circle around touched letter
                canvas.drawCircle(centerX, paddingTop + (i * itemHeight) + (itemHeight / 2f), (width / 2.2f), thumbTrackPaint)
                canvas.drawText(alphabet[i], centerX, letterY, activePaint)
            } else {
                canvas.drawText(alphabet[i], centerX, letterY, normalPaint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                isDragging = true
                val availableHeight = height - paddingTop - paddingBottom
                if (availableHeight > 0) {
                    val itemHeight = availableHeight / alphabet.size.toFloat()
                    val touchY = (event.y - paddingTop).coerceIn(0f, availableHeight.toFloat())
                    val newIndex = (touchY / itemHeight).toInt().coerceIn(0, alphabet.size - 1)

                    if (newIndex != activeIndex) {
                        activeIndex = newIndex
                        invalidate()
                        onSectionChangeListener?.invoke(alphabet[newIndex], event.y)
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                activeIndex = -1
                invalidate()
                onScrollEndListener?.invoke()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
