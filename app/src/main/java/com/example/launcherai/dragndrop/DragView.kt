package com.example.launcherai.dragndrop

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import android.widget.FrameLayout

/**
 * Floating elevated view rendering a drag preview bitmap of the item being dragged across DragLayer.
 */
class DragView(
    context: Context,
    private val bitmap: Bitmap,
    private val registrationX: Int,
    private val registrationY: Int
) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    init {
        elevation = 16f * resources.displayMetrics.density
        scaleX = 1.08f
        scaleY = 1.08f
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
