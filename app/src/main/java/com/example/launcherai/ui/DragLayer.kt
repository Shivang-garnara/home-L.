package com.example.launcherai.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import com.example.launcherai.dragndrop.DragController

/**
 * Root workspace layout container. Handles touch intercept and event dispatching
 * during active drag operations managed by DragController.
 */
class DragLayer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var dragController: DragController? = null
    var activeResizeFrame: com.example.launcherai.widgets.AppWidgetResizeFrame? = null

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val dc = dragController
        if (dc != null && dc.isDragging) {
            when (ev.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    dc.onDragMove(ev.x, ev.y)
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    dc.endDrag(ev.x, ev.y)
                    performClick()
                    return true
                }
                MotionEvent.ACTION_CANCEL -> {
                    dc.cancelDrag()
                    return true
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val resizeFrame = activeResizeFrame
        if (resizeFrame != null && ev.actionMasked == MotionEvent.ACTION_DOWN) {
            val rect = android.graphics.Rect()
            resizeFrame.getHitRect(rect)
            // Expand hit bounds to include top badge and handles
            rect.inset(-60, -60)
            if (!rect.contains(ev.x.toInt(), ev.y.toInt())) {
                resizeFrame.dismiss()
                activeResizeFrame = null
                return true
            }
        }

        val dc = dragController ?: return super.onInterceptTouchEvent(ev)
        if (dc.isDragging) {
            return true
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val dc = dragController ?: return super.onTouchEvent(ev)
        if (dc.isDragging) {
            when (ev.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    dc.onDragMove(ev.x, ev.y)
                }
                MotionEvent.ACTION_UP -> {
                    dc.endDrag(ev.x, ev.y)
                    performClick()
                }
                MotionEvent.ACTION_CANCEL -> {
                    dc.cancelDrag()
                }
            }
            return true
        }
        return super.onTouchEvent(ev)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
