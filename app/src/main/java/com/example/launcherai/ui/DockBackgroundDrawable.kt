package com.example.launcherai.ui

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable

/**
 * Custom Drawable rendering Nova Launcher's Dock Background (m10.d / kz.p)
 * with per-corner Crop / Round / Squircle shapes, corner radius percentages,
 * horizontal content padding, color + transparency, and draw-behind-navbar support.
 */
class DockBackgroundDrawable(
    var config: DockBackgroundConfig,
    private val density: Float
) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val path = Path()
    private val arcRect = RectF()

    override fun draw(canvas: Canvas) {
        if (!config.enabled) return

        val bounds = bounds
        if (bounds.isEmpty) return

        val padX = config.horizontalPaddingDp * density
        val padTop = if (config.horizontalPaddingDp > 0) (4f * density) else 0f
        val padBottom = if (!config.drawBehindNavbar && config.horizontalPaddingDp > 0) (4f * density) else 0f

        val left = bounds.left + padX
        val top = bounds.top + padTop
        val right = bounds.right - padX
        val bottom = bounds.bottom - padBottom
        if (right <= left || bottom <= top) return

        val width = right - left
        val height = bottom - top
        val maxCornerPx = minOf(width, height) / 2f

        val tlSpec = config.topLeft
        val trSpec = config.topRight
        val blSpec = if (config.drawBehindNavbar) DockCornerSpec(0, DockCornerShape.ROUND) else config.bottomLeft
        val brSpec = if (config.drawBehindNavbar) DockCornerSpec(0, DockCornerShape.ROUND) else config.bottomRight

        val tlR = (tlSpec.radiusPercent / 100f) * maxCornerPx
        val trR = (trSpec.radiusPercent / 100f) * maxCornerPx
        val brR = (brSpec.radiusPercent / 100f) * maxCornerPx
        val blR = (blSpec.radiusPercent / 100f) * maxCornerPx

        path.rewind()
        // Start after top-left corner on top edge
        path.moveTo(left + tlR, top)

        // Top edge -> Top-Right corner
        path.lineTo(right - trR, top)
        appendTopRightCorner(right, top, trR, trSpec.shape)

        // Right edge -> Bottom-Right corner
        path.lineTo(right, bottom - brR)
        appendBottomRightCorner(right, bottom, brR, brSpec.shape)

        // Bottom edge -> Bottom-Left corner
        path.lineTo(left + blR, bottom)
        appendBottomLeftCorner(left, bottom, blR, blSpec.shape)

        // Left edge -> Top-Left corner
        path.lineTo(left, top + tlR)
        appendTopLeftCorner(left, top, tlR, tlSpec.shape)

        path.close()

        paint.color = config.getFillColorWithAlpha()
        canvas.drawPath(path, paint)
    }

    private fun appendTopRightCorner(right: Float, top: Float, r: Float, shape: DockCornerShape) {
        if (r <= 0.5f) {
            path.lineTo(right, top)
            return
        }
        when (shape) {
            DockCornerShape.CROP -> path.lineTo(right, top + r)
            DockCornerShape.ROUND -> {
                arcRect.set(right - 2f * r, top, right, top + 2f * r)
                path.arcTo(arcRect, -90f, 90f, false)
            }
            DockCornerShape.SQUIRCLE -> {
                val c = r * SQUIRCLE_HANDLE
                path.cubicTo(right - c, top, right, top + c, right, top + r)
            }
        }
    }

    private fun appendBottomRightCorner(right: Float, bottom: Float, r: Float, shape: DockCornerShape) {
        if (r <= 0.5f) {
            path.lineTo(right, bottom)
            return
        }
        when (shape) {
            DockCornerShape.CROP -> path.lineTo(right - r, bottom)
            DockCornerShape.ROUND -> {
                arcRect.set(right - 2f * r, bottom - 2f * r, right, bottom)
                path.arcTo(arcRect, 0f, 90f, false)
            }
            DockCornerShape.SQUIRCLE -> {
                val c = r * SQUIRCLE_HANDLE
                path.cubicTo(right, bottom - c, right - c, bottom, right - r, bottom)
            }
        }
    }

    private fun appendBottomLeftCorner(left: Float, bottom: Float, r: Float, shape: DockCornerShape) {
        if (r <= 0.5f) {
            path.lineTo(left, bottom)
            return
        }
        when (shape) {
            DockCornerShape.CROP -> path.lineTo(left, bottom - r)
            DockCornerShape.ROUND -> {
                arcRect.set(left, bottom - 2f * r, left + 2f * r, bottom)
                path.arcTo(arcRect, 90f, 90f, false)
            }
            DockCornerShape.SQUIRCLE -> {
                val c = r * SQUIRCLE_HANDLE
                path.cubicTo(left + c, bottom, left, bottom - c, left, bottom - r)
            }
        }
    }

    private fun appendTopLeftCorner(left: Float, top: Float, r: Float, shape: DockCornerShape) {
        if (r <= 0.5f) {
            path.lineTo(left, top)
            return
        }
        when (shape) {
            DockCornerShape.CROP -> path.lineTo(left + r, top)
            DockCornerShape.ROUND -> {
                arcRect.set(left, top, left + 2f * r, top + 2f * r)
                path.arcTo(arcRect, 180f, 90f, false)
            }
            DockCornerShape.SQUIRCLE -> {
                val c = r * SQUIRCLE_HANDLE
                path.cubicTo(left, top + c, left + c, top, left + r, top)
            }
        }
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    companion object {
        // Approximate squircle control-point factor (softer than circular 0.552)
        private const val SQUIRCLE_HANDLE = 0.2f
    }
}
