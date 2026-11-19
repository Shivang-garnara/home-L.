package com.example.launcherai.widgets

import android.appwidget.AppWidgetHostView
import android.content.Context
import android.view.MotionEvent

/**
 * Custom AppWidgetHostView extending system AppWidgetHostView for hosting
 * native RemoteViews widgets inside CellLayout pages.
 */
class LauncherAppWidgetHostView(context: Context) : AppWidgetHostView(context) {

    private var downX: Float = 0f
    private var downY: Float = 0f

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun getDescendantFocusability(): Int {
        return FOCUS_BEFORE_DESCENDANTS
    }
}
