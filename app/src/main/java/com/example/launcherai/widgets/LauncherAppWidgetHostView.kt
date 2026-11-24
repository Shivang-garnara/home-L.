package com.example.launcherai.widgets

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Outline
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.TextView
import com.example.launcherai.R
import kotlin.math.abs

/**
 * Enhanced Nova-style AppWidgetHostView for LauncherAI.
 * Features:
 *  - Responsive RemoteViews size options calculations (Android 12+ AppWidget size options).
 *  - Rounded corner outline clipping matching Nova Launcher aesthetic.
 *  - Nova CheckLongPressHelper pattern: reliably intercepts and dispatches long press
 *    even when inner RemoteViews (buttons, search inputs, list views) consume touch events.
 *  - High-fidelity crash and error view fallback.
 */
class LauncherAppWidgetHostView(context: Context) : AppWidgetHostView(context) {

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val longPressTimeout = (ViewConfiguration.getLongPressTimeout() * 0.75f).toLong()
    private var downX = 0f
    private var downY = 0f
    var lastTouchRawX = 0f
        private set
    var lastTouchRawY = 0f
        private set
    private var isLongPressDispatched = false
    private var hasStartedDragAfterLongPress = false
    var onLongPressDragListener: ((rawX: Float, rawY: Float) -> Unit)? = null
    private var customCornerRadius: Float = 16f * context.resources.displayMetrics.density

    private val checkLongPressRunnable = Runnable {
        if (!isLongPressDispatched && isAttachedToWindow) {
            cancelChildLongPress(this)
            val handled = performLongClick()
            if (handled) {
                parent?.requestDisallowInterceptTouchEvent(true)
            }
        }
    }

    private fun cancelChildLongPress(root: View) {
        root.cancelLongPress()
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                cancelChildLongPress(root.getChildAt(i))
            }
        }
    }

    init {
        // Zero out system-enforced margins for full-bleed modern widget look
        setPadding(0, 0, 0, 0)

        // Clip to outline with rounded corners
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, customCornerRadius)
            }
        }
        clipToOutline = true
    }

    /**
     * Updates widget size options for responsive layouts (clocks, weather, lists).
     */
    fun updateSizeOptions(spanX: Int, spanY: Int, cellWidthPx: Int, cellHeightPx: Int) {
        val density = resources.displayMetrics.density
        if (density <= 0f) return

        val widthDp = ((cellWidthPx * spanX) / density).toInt()
        val heightDp = ((cellHeightPx * spanY) / density).toInt()

        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, heightDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, heightDp)
        }

        try {
            updateAppWidgetSize(options, widthDp, heightDp, widthDp, heightDp)
            if (appWidgetId > 0) {
                AppWidgetManager.getInstance(context).updateAppWidgetOptions(appWidgetId, options)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setWidgetCornerRadiusDp(radiusDp: Float) {
        customCornerRadius = radiusDp * resources.displayMetrics.density
        invalidateOutline()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
                lastTouchRawX = ev.rawX
                lastTouchRawY = ev.rawY
                isLongPressDispatched = false
                hasStartedDragAfterLongPress = false
                removeCallbacks(checkLongPressRunnable)
                postDelayed(checkLongPressRunnable, longPressTimeout)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = ev.x - downX
                val dy = ev.y - downY
                val slop = touchSlop.toFloat()
                // Check if touch moved outside view bounds + slop, or exceeded slop * 2 distance (Nova CheckLongPressHelper parity)
                val outOfBounds = ev.x < -slop || ev.y < -slop || ev.x >= width + slop || ev.y >= height + slop
                val movedBeyondSlop = (dx * dx + dy * dy) > (slop * 2f * (slop * 2f))
                if (!isLongPressDispatched) {
                    if (outOfBounds || movedBeyondSlop) {
                        removeCallbacks(checkLongPressRunnable)
                    }
                } else if (!hasStartedDragAfterLongPress) {
                    if (movedBeyondSlop || (dx * dx + dy * dy) > (slop * slop)) {
                        hasStartedDragAfterLongPress = true
                        parent?.requestDisallowInterceptTouchEvent(false)
                        onLongPressDragListener?.invoke(ev.rawX, ev.rawY)
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                removeCallbacks(checkLongPressRunnable)
                hasStartedDragAfterLongPress = false
                if (isLongPressDispatched) {
                    isLongPressDispatched = false
                    return true
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun performLongClick(): Boolean {
        if (isLongPressDispatched) {
            // Already dispatched for this gesture, prevent duplicate action or drag invocation
            return false
        }
        isLongPressDispatched = true
        return super.performLongClick()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(checkLongPressRunnable)
        isLongPressDispatched = false
        hasStartedDragAfterLongPress = false
    }

    override fun cancelLongPress() {
        super.cancelLongPress()
        // Do not prematurely remove checkLongPressRunnable here;
        // checkLongPressRunnable is strictly managed by touch state in dispatchTouchEvent.
    }

    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        super.requestDisallowInterceptTouchEvent(disallowIntercept)
        // Disallow intercept from children (e.g. nested lists) does not cancel the host's long-press
        // unless actual scrolling movement beyond touch slop occurs in dispatchTouchEvent.
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return if (isLongPressDispatched) {
            true
        } else {
            super.onInterceptTouchEvent(ev)
        }
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        return if (isLongPressDispatched) {
            true
        } else {
            super.onTouchEvent(ev)
        }
    }

    override fun getDescendantFocusability(): Int {
        return FOCUS_BEFORE_DESCENDANTS
    }

    override fun getErrorView(): View {
        return FrameLayout(context).apply {
            setBackgroundResource(R.drawable.bg_card_rounded)
            val tv = TextView(context).apply {
                text = "Widget Unavailable"
                setTextColor(context.getColor(R.color.text_secondary))
                textSize = 12f
                gravity = Gravity.CENTER
            }
            addView(
                tv,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
            )
        }
    }
}
