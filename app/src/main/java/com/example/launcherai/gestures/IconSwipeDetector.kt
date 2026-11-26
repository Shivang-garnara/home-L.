package com.example.launcherai.gestures

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.example.launcherai.ui.DragLayer
import kotlin.math.abs

/**
 * Shared touch helper for desktop & dock icons/folders (mirrors Nova fz.e + NovaLauncher.p1):
 * - Captures raw touch coordinates for long-press drag positioning
 * - Announces DragLayer suppression bitmask (0x1 swipe-up / 0x2 swipe-down) so global gestures yield
 * - Detects vertical swipe up / swipe down cleanly across ViewPager2 / DockLayout
 * - Plays Nova's 125ms + 100ms bounce animation before executing swipe action
 */
class IconSwipeDetector(
    context: Context,
    private val canSwipeUp: () -> Boolean,
    private val canSwipeDown: () -> Boolean,
    private val findDragLayer: (View) -> DragLayer?,
    private val onSwipeUp: (View) -> Unit,
    private val onSwipeDown: (View) -> Unit
) : View.OnTouchListener {

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var startX = 0f
    private var startY = 0f
    private var pendingDirection = 0 // 0 = none, 1 = up, -1 = down

    var lastRawTouchX: Float = 0f
        private set
    var lastRawTouchY: Float = 0f
        private set

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val loc = IntArray(2)
                v.getLocationOnScreen(loc)
                lastRawTouchX = loc[0] + event.x
                lastRawTouchY = loc[1] + event.y
                startX = event.x
                startY = event.y
                pendingDirection = 0

                val mask = (if (canSwipeUp()) NovaGestureRecognizer.FLAG_SUPPRESS_SWIPE_UP else 0) or
                    (if (canSwipeDown()) NovaGestureRecognizer.FLAG_SUPPRESS_SWIPE_DOWN else 0)
                findDragLayer(v)?.setItemSwipeSuppression(mask)
                return false
            }

            MotionEvent.ACTION_MOVE -> {
                val diffUp = startY - event.y
                val diffDown = event.y - startY
                val diffX = abs(startX - event.x)
                val threshold = touchSlop * 1.6f

                if (canSwipeUp() && diffUp > threshold && diffUp > diffX * 1.3f) {
                    pendingDirection = 1
                    v.cancelLongPress()
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                } else if (canSwipeDown() && diffDown > threshold && diffDown > diffX * 1.3f) {
                    pendingDirection = -1
                    v.cancelLongPress()
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                }
                return false
            }

            MotionEvent.ACTION_UP -> {
                findDragLayer(v)?.setItemSwipeSuppression(0)
                val dir = pendingDirection
                pendingDirection = 0
                if (dir != 0) {
                    v.cancelLongPress()
                    val offsetPx = (-dir) * 8f * v.resources.displayMetrics.density
                    v.animate()
                        .translationY(offsetPx)
                        .setDuration(125L)
                        .withEndAction {
                            v.animate().translationY(0f).setDuration(100L).start()
                        }
                        .start()
                    if (dir > 0) {
                        onSwipeUp(v)
                    } else {
                        onSwipeDown(v)
                    }
                    return true
                }
                return false
            }

            MotionEvent.ACTION_CANCEL -> {
                pendingDirection = 0
                findDragLayer(v)?.setItemSwipeSuppression(0)
                return false
            }
        }
        return false
    }

    companion object {
        fun findDragLayerUpwards(view: View): DragLayer? {
            var current: Any? = view.parent
            while (current is View) {
                if (current is DragLayer) return current
                current = current.parent
            }
            return null
        }
    }
}
