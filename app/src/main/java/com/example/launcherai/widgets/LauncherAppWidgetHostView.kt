package com.example.launcherai.widgets

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.graphics.Outline
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.SizeF
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
import kotlin.math.roundToInt

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
    private val defaultWidgetPadding = Rect()
    private var isSettingAppWidget = false

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
        // Clip to outline with rounded corners
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, customCornerRadius)
            }
        }
        clipToOutline = true
    }

    override fun setAppWidget(appWidgetId: Int, info: AppWidgetProviderInfo?) {
        isSettingAppWidget = true
        super.setAppWidget(appWidgetId, info)
        isSettingAppWidget = false
        applyStoredPaddingPreference()
    }

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        if (isSettingAppWidget) {
            defaultWidgetPadding.set(left, top, right, bottom)
        }
        super.setPadding(left, top, right, bottom)
    }

    fun hasDefaultPadding(): Boolean {
        return defaultWidgetPadding.left != 0 ||
            defaultWidgetPadding.top != 0 ||
            defaultWidgetPadding.right != 0 ||
            defaultWidgetPadding.bottom != 0
    }

    fun isWidgetPaddingEnabled(): Boolean {
        if (appWidgetId <= 0) return false
        val prefs = context.getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("widget_padding_$appWidgetId", false)
    }

    fun applyStoredPaddingPreference() {
        val enabled = isWidgetPaddingEnabled()
        if (enabled) {
            super.setPadding(
                defaultWidgetPadding.left,
                defaultWidgetPadding.top,
                defaultWidgetPadding.right,
                defaultWidgetPadding.bottom
            )
        } else {
            super.setPadding(0, 0, 0, 0)
        }
    }

    fun toggleWidgetPadding(): Boolean {
        val newState = !isWidgetPaddingEnabled()
        if (appWidgetId > 0) {
            context.getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("widget_padding_$appWidgetId", newState)
                .apply()
        }
        applyStoredPaddingPreference()
        if (width > 0 && height > 0) {
            updateSizeOptionsForPixels(width, height)
        }
        requestLayout()
        return newState
    }

    /**
     * Updates widget size options for responsive layouts (clocks, weather, lists)
     * from a cell span and cell dimensions in pixels.
     */
    fun updateSizeOptions(spanX: Int, spanY: Int, cellWidthPx: Int, cellHeightPx: Int) {
        updateSizeOptionsForPixels(cellWidthPx * spanX, cellHeightPx * spanY)
    }

    /**
     * Updates widget size options using the exact laid-out pixel width and height,
     * subtracting hostView padding and deduplicating against existing AppWidgetOptions
     * (matching Nova Launcher's hb.b.f / bb.k.updateAppWidgetSize).
     */
    fun updateSizeOptionsForPixels(widthPx: Int, heightPx: Int) {
        val density = resources.displayMetrics.density
        if (density <= 0f || widthPx <= 0 || heightPx <= 0) return

        val contentWidthPx = maxOf(0, widthPx - paddingLeft - paddingRight)
        val contentHeightPx = maxOf(0, heightPx - paddingTop - paddingBottom)

        val widthDpF = contentWidthPx / density
        val heightDpF = contentHeightPx / density
        val widthDp = widthDpF.roundToInt().coerceAtLeast(1)
        val heightDp = heightDpF.roundToInt().coerceAtLeast(1)

        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val existingOptions = if (appWidgetId > 0) {
                appWidgetManager.getAppWidgetOptions(appWidgetId)
            } else {
                null
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val sizes = arrayListOf(SizeF(widthDpF, heightDpF))
                @Suppress("DEPRECATION")
                val existingSizes = existingOptions?.getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)
                if (existingOptions != null &&
                    existingSizes == sizes &&
                    existingOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) == widthDp &&
                    existingOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) == heightDp &&
                    existingOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH) == widthDp &&
                    existingOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT) == heightDp
                ) {
                    return
                }
                val options = Bundle().apply {
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp)
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, heightDp)
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthDp)
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, heightDp)
                }
                updateAppWidgetSize(options, sizes)
            } else {
                if (existingOptions != null &&
                    existingOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) == widthDp &&
                    existingOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) == heightDp &&
                    existingOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH) == widthDp &&
                    existingOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT) == heightDp
                ) {
                    return
                }
                val options = Bundle().apply {
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp)
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, heightDp)
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthDp)
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, heightDp)
                }
                @Suppress("DEPRECATION")
                updateAppWidgetSize(options, widthDp, heightDp, widthDp, heightDp)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setWidgetCornerRadiusDp(radiusDp: Float) {
        customCornerRadius = radiusDp * resources.displayMetrics.density
        clipToOutline = customCornerRadius > 0f
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
