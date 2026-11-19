package com.example.launcherai.gestures

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.Toast

enum class GestureAction(val label: String) {
    NONE("No Action"),
    APP_DRAWER("Open App Drawer"),
    SETTINGS("Open Launcher Settings"),
    EXPAND_NOTIFICATIONS("Expand Notifications"),
    EXPAND_QUICK_SETTINGS("Expand Quick Settings"),
    SEARCH("Open Search"),
    LOCK_SCREEN("Lock Screen");

    companion object {
        fun fromName(name: String, default: GestureAction = NONE): GestureAction {
            return entries.find { it.name == name } ?: default
        }
    }
}

/**
 * Centralized gesture detector for launcher touch gestures (Swipe Up/Down, Double Tap, Pinch, Two-finger swipe).
 */
class GestureHandler(
    private val context: Context,
    private val onExecuteAction: (GestureAction) -> Unit
) {

    var swipeUpAction: GestureAction = GestureAction.APP_DRAWER
    var swipeDownAction: GestureAction = GestureAction.EXPAND_NOTIFICATIONS
    var doubleTapAction: GestureAction = GestureAction.SEARCH
    var pinchInAction: GestureAction = GestureAction.SETTINGS
    var twoFingerSwipeUpAction: GestureAction = GestureAction.SETTINGS
    var twoFingerSwipeDownAction: GestureAction = GestureAction.EXPAND_QUICK_SETTINGS

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            onExecuteAction(doubleTapAction)
            return true
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (e1 == null) return false
            val dy = e2.y - e1.y
            val dx = e2.x - e1.x
            val pointerCount = e2.pointerCount

            if (Math.abs(dy) > Math.abs(dx) && Math.abs(dy) > 120) {
                if (dy < 0) {
                    // Swipe Up
                    if (pointerCount >= 2) {
                        onExecuteAction(twoFingerSwipeUpAction)
                    } else {
                        onExecuteAction(swipeUpAction)
                    }
                    return true
                } else {
                    // Swipe Down
                    if (pointerCount >= 2) {
                        onExecuteAction(twoFingerSwipeDownAction)
                    } else {
                        onExecuteAction(swipeDownAction)
                    }
                    return true
                }
            }
            return false
        }
    })

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if (detector.scaleFactor < 0.85f) {
                onExecuteAction(pinchInAction)
                return true
            }
            return false
        }
    })

    fun onTouchEvent(event: MotionEvent): Boolean {
        var handled = scaleDetector.onTouchEvent(event)
        if (!handled) {
            handled = gestureDetector.onTouchEvent(event)
        }
        return handled
    }

    companion object {
        fun executeSystemAction(context: Context, action: GestureAction) {
            when (action) {
                GestureAction.EXPAND_NOTIFICATIONS -> {
                    try {
                        val sbservice = context.getSystemService("statusbar")
                        val statusbarManager = Class.forName("android.app.StatusBarManager")
                        val showsb = statusbarManager.getMethod("expandNotificationsPanel")
                        showsb.invoke(sbservice)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Notifications panel unsupported on this device", Toast.LENGTH_SHORT).show()
                    }
                }
                GestureAction.EXPAND_QUICK_SETTINGS -> {
                    try {
                        val sbservice = context.getSystemService("statusbar")
                        val statusbarManager = Class.forName("android.app.StatusBarManager")
                        val showsb = statusbarManager.getMethod("expandSettingsPanel")
                        showsb.invoke(sbservice)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Quick settings unsupported on this device", Toast.LENGTH_SHORT).show()
                    }
                }
                GestureAction.LOCK_SCREEN -> {
                    Toast.makeText(context, "Lock screen gesture requires Device Admin capability", Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }
}
