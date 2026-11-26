package com.example.launcherai.ui

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.widget.FrameLayout
import androidx.core.graphics.Insets
import com.example.launcherai.dragndrop.DragController

/**
 * Root workspace layout container. Handles touch intercept, event dispatching,
 * active drag operations, mandatory system gesture inset guards (mirroring Nova bb.k),
 * multi-touch unblocking, and 1:1 real-time finger tracking for App Drawer opening & closing.
 */
class DragLayer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var dragController: DragController? = null
    var activeResizeFrame: com.example.launcherai.widgets.AppWidgetResizeFrame? = null
    var gestureHandler: com.example.launcherai.gestures.GestureHandler? = null
    var isAppDrawerOpen: () -> Boolean = { false }
    var isDrawerAtTop: () -> Boolean = { true }
    var isFolderOverlayOpen: () -> Boolean = { false }
    var onDrawerTranslationUpdate: ((transY: Float) -> Unit)? = null
    var onDrawerDragFinish: ((open: Boolean, velocityY: Float) -> Unit)? = null

    private val mandatoryGestureInsets = Rect()
    private var touchStartedOutsideGestureInsets = true
    private var itemSwipeSuppressionMask = 0

    private var touchStartX = 0f
    private var touchStartY = 0f
    private var isDraggingDrawer = false
    private var initialDrawerTransY = 0f
    private var velocityTracker: VelocityTracker? = null

    fun updateMandatorySystemGestureInsets(insets: Insets) {
        mandatoryGestureInsets.set(insets.left, insets.top, insets.right, insets.bottom)
    }

    fun setItemSwipeSuppression(mask: Int) {
        itemSwipeSuppressionMask = mask
        gestureHandler?.setItemSuppressionMask(mask)
    }

    private fun isInsideSafeGestureArea(ev: MotionEvent): Boolean {
        val w = width
        val h = height
        if (w <= 0 || h <= 0) return true
        val x = ev.x
        val y = ev.y
        return x >= mandatoryGestureInsets.left &&
            x < (w - mandatoryGestureInsets.right) &&
            y >= mandatoryGestureInsets.top &&
            y < (h - mandatoryGestureInsets.bottom)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val dc = dragController
        if (dc != null && dc.isDragging) {
            return super.dispatchTouchEvent(ev)
        }

        // Nova bb.k: multi-touch unblocks child interception so 2-finger pinch/rotate/swipe work over widgets & pager
        if (ev.pointerCount >= 2) {
            requestDisallowInterceptTouchEvent(false)
            if (isDraggingDrawer) {
                isDraggingDrawer = false
                onDrawerDragFinish?.invoke(isAppDrawerOpen(), 0f)
            }
        }

        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker?.addMovement(ev)

        val height = resources.displayMetrics.heightPixels.toFloat()

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = ev.rawX
                touchStartY = ev.rawY
                isDraggingDrawer = false
                initialDrawerTransY = if (isAppDrawerOpen()) 0f else height
                touchStartedOutsideGestureInsets = isInsideSafeGestureArea(ev)
                setItemSwipeSuppression(0)
            }
            MotionEvent.ACTION_MOVE -> {
                val dy = ev.rawY - touchStartY
                val dx = ev.rawX - touchStartX
                val density = resources.displayMetrics.density

                if (!isDraggingDrawer &&
                    dc?.isDragging != true &&
                    ev.pointerCount == 1 &&
                    touchStartedOutsideGestureInsets &&
                    !isFolderOverlayOpen()
                ) {
                    val isVertical = Math.abs(dy) > Math.abs(dx) * 1.2f
                    val drawerOpen = isAppDrawerOpen()
                    val suppressUp = (itemSwipeSuppressionMask and com.example.launcherai.gestures.NovaGestureRecognizer.FLAG_SUPPRESS_SWIPE_UP) != 0
                    val allowDrawerDragUp = !suppressUp && (gestureHandler?.shouldDragDrawerOnSwipeUp() != false)

                    if (isVertical) {
                        if (!drawerOpen && allowDrawerDragUp && dy < -18 * density) {
                            // Start dragging UP from home screen to open drawer
                            isDraggingDrawer = true
                            initialDrawerTransY = height
                            parent?.requestDisallowInterceptTouchEvent(true)

                            val cancelEvent = MotionEvent.obtain(ev)
                            cancelEvent.action = MotionEvent.ACTION_CANCEL
                            super.dispatchTouchEvent(cancelEvent)
                            cancelEvent.recycle()
                        } else if (drawerOpen && dy > 18 * density && isDrawerAtTop()) {
                            // Start dragging DOWN from open drawer to close drawer
                            isDraggingDrawer = true
                            initialDrawerTransY = 0f
                            parent?.requestDisallowInterceptTouchEvent(true)

                            val cancelEvent = MotionEvent.obtain(ev)
                            cancelEvent.action = MotionEvent.ACTION_CANCEL
                            super.dispatchTouchEvent(cancelEvent)
                            cancelEvent.recycle()
                        }
                    }
                }

                if (isDraggingDrawer) {
                    val currentTransY = (initialDrawerTransY + dy).coerceIn(0f, height)
                    onDrawerTranslationUpdate?.invoke(currentTransY)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDraggingDrawer) {
                    velocityTracker?.computeCurrentVelocity(1000)
                    val velocityY = velocityTracker?.yVelocity ?: 0f
                    val dy = ev.rawY - touchStartY
                    val currentTransY = (initialDrawerTransY + dy).coerceIn(0f, height)

                    val shouldOpen = if (Math.abs(velocityY) > 600f) {
                        velocityY < 0f
                    } else {
                        currentTransY < height * 0.6f
                    }

                    onDrawerDragFinish?.invoke(shouldOpen, velocityY)
                    isDraggingDrawer = false
                    velocityTracker?.recycle()
                    velocityTracker = null
                    setItemSwipeSuppression(0)
                    return true
                }

                velocityTracker?.recycle()
                velocityTracker = null
            }
        }

        if (!isAppDrawerOpen() && !isFolderOverlayOpen() && touchStartedOutsideGestureInsets) {
            val handledGesture = gestureHandler?.onTouchEvent(ev) == true
            if (handledGesture && ev.actionMasked != MotionEvent.ACTION_DOWN) {
                val cancelEvent = MotionEvent.obtain(ev)
                cancelEvent.action = MotionEvent.ACTION_CANCEL
                super.dispatchTouchEvent(cancelEvent)
                cancelEvent.recycle()
                return true
            }
        }
        val dispatched = super.dispatchTouchEvent(ev)
        if (ev.actionMasked == MotionEvent.ACTION_UP || ev.actionMasked == MotionEvent.ACTION_CANCEL) {
            setItemSwipeSuppression(0)
        }
        return dispatched
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
