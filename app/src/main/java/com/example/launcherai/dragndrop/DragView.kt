package com.example.launcherai.dragndrop

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View

/**
 * Floating elevated view rendering a drag preview bitmap of the item being dragged across DragLayer.
 * Implements Nova Launcher 8.9.9 physics-based adaptive icon wiggle and dynamic drag tilt.
 */
class DragView(
    context: Context,
    private val bitmap: Bitmap,
    private val registrationX: Int,
    private val registrationY: Int
) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val isAdaptiveWiggleEnabled: Boolean

    private var lastTouchX: Float = 0f
    private var lastTouchY: Float = 0f
    private var hasMoved: Boolean = false
    private var currentTiltRotation: Float = 0f

    init {
        elevation = 16f * resources.displayMetrics.density
        scaleX = 1.08f
        scaleY = 1.08f

        val prefs = context.getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        isAdaptiveWiggleEnabled = prefs.getBoolean("key_adaptive_icon_animations", true)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(bitmap.width, bitmap.height)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
    }

    fun move(touchX: Float, touchY: Float) {
        translationX = touchX - registrationX
        translationY = touchY - registrationY

        if (isAdaptiveWiggleEnabled) {
            if (hasMoved) {
                val deltaX = touchX - lastTouchX
                // Tilt proportional to horizontal drag velocity, clamped to +/-12 degrees
                val targetTilt = (deltaX * 0.35f).coerceIn(-12f, 12f)
                // Smooth interpolation (spring dampening)
                currentTiltRotation += (targetTilt - currentTiltRotation) * 0.35f
                rotation = currentTiltRotation

                // Subtle dynamic breathing scale during drag motion
                val speed = kotlin.math.abs(deltaX)
                scaleX = 1.08f + (speed * 0.0015f).coerceAtMost(0.06f)
                scaleY = 1.08f + (speed * 0.0015f).coerceAtMost(0.06f)
            } else {
                hasMoved = true
            }
            lastTouchX = touchX
            lastTouchY = touchY
        }
    }

    /**
     * Resets the tilt when hovering stationary or ending drag.
     */
    fun settle() {
        if (rotation != 0f) {
            animate().rotation(0f).scaleX(1.08f).scaleY(1.08f).setDuration(150).start()
        }
    }

    companion object {
        fun createBitmapFromView(view: View): Bitmap {
            val bitmap = Bitmap.createBitmap(
                maxOf(1, view.width),
                maxOf(1, view.height),
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            return bitmap
        }
    }
}
