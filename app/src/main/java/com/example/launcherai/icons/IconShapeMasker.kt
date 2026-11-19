package com.example.launcherai.icons

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Custom Icon Masking & Scaling engine (Phase 9.3R).
 * Applies icon size scale % and adaptive path masks (Rounded square, Round, Squircle, Flower, Teardrop, Hexagon, etc.)
 * across workspace icons, dock icons, and app drawer items.
 */
object IconShapeMasker {

    fun createMaskedIcon(context: Context, originalDrawable: Drawable?, shapeName: String, scalePercent: Int): Drawable? {
        if (originalDrawable == null) return null

        val density = context.resources.displayMetrics.density
        val baseSize = (56 * density).toInt()
        val scaleFactor = (scalePercent.coerceIn(50, 200) / 100f)
        val finalSize = maxOf(1, (baseSize * scaleFactor).toInt())

        val bitmap = Bitmap.createBitmap(finalSize, finalSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val path = createShapePath(shapeName, finalSize.toFloat())

        canvas.save()
        canvas.clipPath(path)

        originalDrawable.setBounds(0, 0, finalSize, finalSize)
        originalDrawable.draw(canvas)
        canvas.restore()

        return BitmapDrawable(context.resources, bitmap)
    }

    private fun createShapePath(shapeName: String, size: Float): Path {
        val path = Path()
        val rect = RectF(0f, 0f, size, size)
        val cx = size / 2f
        val cy = size / 2f
        val radius = size / 2f

        when (shapeName.trim().lowercase()) {
            "round" -> {
                path.addCircle(cx, cy, radius, Path.Direction.CW)
            }
            "square" -> {
                path.addRect(rect, Path.Direction.CW)
            }
            "squircle" -> {
                val r = size * 0.38f
                path.addRoundRect(rect, r, r, Path.Direction.CW)
            }
            "teardrop" -> {
                val radii = floatArrayOf(
                    radius, radius,
                    radius, radius,
                    radius, radius,
                    0f, 0f
                )
                path.addRoundRect(rect, radii, Path.Direction.CW)
            }
            "pebble" -> {
                val radii = floatArrayOf(
                    radius, radius,
                    size * 0.2f, size * 0.2f,
                    radius, radius,
                    size * 0.2f, size * 0.2f
                )
                path.addRoundRect(rect, radii, Path.Direction.CW)
            }
            "flower" -> {
                val numPetals = 8
                val innerR = radius * 0.82f
                val outerR = radius
                for (i in 0 until 360) {
                    val angle = Math.toRadians(i.toDouble())
                    val r = innerR + (outerR - innerR) * (0.5f + 0.5f * sin(angle * numPetals)).toFloat()
                    val x = cx + r * cos(angle).toFloat()
                    val y = cy + r * sin(angle).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
            }
            "pentagon" -> {
                createPolygonPath(path, cx, cy, radius, 5)
            }
            "hexagon", "hexagon2" -> {
                createPolygonPath(path, cx, cy, radius, 6)
            }
            "heptagon" -> {
                createPolygonPath(path, cx, cy, radius, 7)
            }
            "octagon" -> {
                createPolygonPath(path, cx, cy, radius, 8)
            }
            "vessel" -> {
                val radii = floatArrayOf(
                    size * 0.4f, size * 0.4f,
                    size * 0.1f, size * 0.1f,
                    size * 0.4f, size * 0.4f,
                    size * 0.1f, size * 0.1f
                )
                path.addRoundRect(rect, radii, Path.Direction.CW)
            }
            "crop" -> {
                val r = size * 0.15f
                path.addRoundRect(rect, r, r, Path.Direction.CW)
            }
            else -> { // "Rounded square (Default)"
                val r = size * 0.22f
                path.addRoundRect(rect, r, r, Path.Direction.CW)
            }
        }
        return path
    }

    private fun createPolygonPath(path: Path, cx: Float, cy: Float, radius: Float, sides: Int) {
        val angleStep = 2.0 * Math.PI / sides
        val startAngle = -Math.PI / 2.0
        for (i in 0 until sides) {
            val angle = startAngle + i * angleStep
            val x = cx + radius * cos(angle).toFloat()
            val y = cy + radius * sin(angle).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
    }
}
