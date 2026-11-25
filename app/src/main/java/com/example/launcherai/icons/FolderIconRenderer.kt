package com.example.launcherai.icons

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.ColorUtils
import kotlin.math.min

/**
 * High-performance folder icon rendering engine matching Nova Launcher 8.9.9 (FolderIcon / SettingsFolderIcon).
 *
 * Implements Nova's 4-pass rendering pipeline:
 *  1. Background Plate fill (custom shape, color, and alpha transparency).
 *  2. Canvas Clipping to shape path (when clipToBounds is enabled).
 *  3. Preview Sub-Icons layout (Grid 2x2, Grid 3x3, Stack, Fan, Line).
 *  4. Outline Stroke around shape perimeter (when outline is enabled).
 */
object FolderIconRenderer {

    const val PREVIEW_GRID = "GRID"
    const val PREVIEW_GRID_9 = "GRID_9"
    const val PREVIEW_RADIAL_5 = "RADIAL_5"
    const val PREVIEW_RADIAL_9 = "RADIAL_9"
    const val PREVIEW_STACK = "STACK"
    const val PREVIEW_FAN = "FAN"
    const val PREVIEW_LINE = "LINE"
    const val PREVIEW_FIRST_APP = "FIRST_APP"

    const val SHAPE_SYSTEM = "SYSTEM"
    const val SHAPE_ROUND = "Round"
    const val SHAPE_ROUNDED_SQUARE = "Rounded square"
    const val SHAPE_SQUIRCLE = "Squircle"
    const val SHAPE_SQUARE = "Square"
    const val SHAPE_TEARDROP = "Teardrop"
    const val SHAPE_FLOWER = "Flower"
    const val SHAPE_PLATFORM = "Platform"

    /**
     * Renders a folder icon based on the user's stored preferences.
     */
    fun renderFolderIconFromPrefs(
        context: Context,
        childrenDrawables: List<Drawable>,
        targetSizePx: Int? = null
    ): Drawable {
        val prefs = context.getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        val preview = prefs.getString("key_folder_icon_preview", PREVIEW_GRID) ?: PREVIEW_GRID
        val shape = prefs.getString("key_folder_icon_shape", SHAPE_SYSTEM) ?: SHAPE_SYSTEM
        val matchWindowColor = prefs.getBoolean("key_folder_icon_match_window_color", true)
        val resolvedPlate = FolderGeometry.resolveFolderIconColor(
            matchWindowColor = matchWindowColor,
            windowBgColor = prefs.getInt("key_folder_window_bg_color", 0xFF202534.toInt()),
            windowBgOpacityPercent = prefs.getInt("key_folder_window_bg_alpha", 80),
            customIconBgColor = prefs.getInt("key_folder_icon_bg_color", 0xFFE0E0E0.toInt()),
            customIconBgTransparencyPercent = prefs.getInt("key_folder_icon_bg_alpha", 20)
        )
        val bgColor = resolvedPlate.color
        val bgAlpha = resolvedPlate.transparencyPercent
        val outline = prefs.getBoolean("key_folder_icon_outline", true)
        val clip = prefs.getBoolean("key_folder_icon_clip", true)

        val size = targetSizePx ?: (48f * context.resources.displayMetrics.density).toInt()

        return renderFolderIcon(
            context = context,
            children = childrenDrawables,
            previewLayout = preview,
            shape = shape,
            bgColor = bgColor,
            bgAlphaPercent = bgAlpha,
            outline = outline,
            clipToBounds = clip,
            targetSizePx = size
        )
    }

    /**
     * Direct canvas rendering of a folder icon with explicit styling arguments.
     */
    fun renderFolderIcon(
        context: Context,
        children: List<Drawable>,
        previewLayout: String,
        shape: String,
        bgColor: Int,
        bgAlphaPercent: Int,
        outline: Boolean,
        clipToBounds: Boolean,
        targetSizePx: Int
    ): Drawable {
        val size = targetSizePx.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val density = context.resources.displayMetrics.density

        // Resolve active shape
        val effectiveShape = if (shape.equals(SHAPE_SYSTEM, ignoreCase = true)) {
            val prefs = context.getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.getString("key_icon_shape", SHAPE_ROUND) ?: SHAPE_ROUND
        } else {
            shape
        }

        val shapePath: Path? = if (effectiveShape.equals(SHAPE_PLATFORM, ignoreCase = true) ||
            effectiveShape.equals("None", ignoreCase = true)
        ) {
            null
        } else {
            IconShapeMasker.createShapePath(effectiveShape, size.toFloat())
        }

        // Pass 1: Draw Background Plate (Fill with Color & Alpha)
        if (bgAlphaPercent < 100) {
            val alpha255 = (((100 - bgAlphaPercent) * 255) / 100).coerceIn(0, 255)
            val fillColor = ColorUtils.setAlphaComponent(bgColor, alpha255)
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = fillColor
                style = Paint.Style.FILL
            }
            if (shapePath != null) {
                canvas.drawPath(shapePath, fillPaint)
            } else if (effectiveShape.equals(SHAPE_PLATFORM, ignoreCase = true)) {
                val platRect = android.graphics.RectF(
                    size * 0.08f,
                    size * 0.64f,
                    size * 0.92f,
                    size * 0.94f
                )
                canvas.drawOval(platRect, fillPaint)
            } else {
                canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), fillPaint)
            }
        }

        // Pass 2: Clip to Bounds (if enabled)
        val saveCount = canvas.save()
        if (clipToBounds && shapePath != null) {
            canvas.clipPath(shapePath)
        }

        // Pass 3: Draw Sub-Icons according to Preview Layout
        when (previewLayout.uppercase()) {
            PREVIEW_GRID_9 -> drawGrid9Preview(canvas, children, size)
            PREVIEW_RADIAL_5 -> drawRadialPreview(canvas, children, size, maxRingSlots = 5)
            PREVIEW_RADIAL_9 -> drawRadialPreview(canvas, children, size, maxRingSlots = 9)
            PREVIEW_STACK -> drawStackPreview(canvas, children, size)
            PREVIEW_FAN -> drawFanPreview(canvas, children, size)
            PREVIEW_LINE -> drawLinePreview(canvas, children, size)
            PREVIEW_FIRST_APP -> drawFirstAppPreview(canvas, children, size)
            else -> drawGrid4Preview(canvas, children, size) // Default GRID 2x2
        }

        canvas.restoreToCount(saveCount)

        // Pass 4: Draw Outline Stroke (if enabled)
        if (outline && shapePath != null) {
            val isDark = ColorUtils.calculateLuminance(bgColor) < 0.5
            val strokeColor = if (isDark) 0x33FFFFFF else 0x33000000
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = strokeColor
                style = Paint.Style.STROKE
                strokeWidth = (1.5f * density).coerceAtLeast(1f)
            }
            canvas.drawPath(shapePath, strokePaint)
        }

        return BitmapDrawable(context.resources, bitmap)
    }

    /**
     * GRID 2x2 (4 Icons): 2 columns by 2 rows with proportional spacing.
     */
    private fun drawGrid4Preview(canvas: Canvas, children: List<Drawable>, size: Int) {
        val count = min(4, children.size)
        if (count == 0) return

        val iconScale = 0.40f
        val itemSize = (size * iconScale).toInt()
        val padding = ((size - 2 * itemSize) / 3f)

        for (i in 0 until count) {
            val col = i % 2
            val row = i / 2
            val left = (padding + col * (itemSize + padding)).toInt()
            val top = (padding + row * (itemSize + padding)).toInt()
            val drawable = children[i]
            drawable.bounds = Rect(left, top, left + itemSize, top + itemSize)
            drawable.draw(canvas)
        }
    }

    /**
     * GRID 3x3 (9 Icons): 3 columns by 3 rows miniature grid.
     */
    private fun drawGrid9Preview(canvas: Canvas, children: List<Drawable>, size: Int) {
        val count = min(9, children.size)
        if (count == 0) return

        val iconScale = 0.25f
        val itemSize = (size * iconScale).toInt()
        val padding = ((size - 3 * itemSize) / 4f)

        for (i in 0 until count) {
            val col = i % 3
            val row = i / 3
            val left = (padding + col * (itemSize + padding)).toInt()
            val top = (padding + row * (itemSize + padding)).toInt()
            val drawable = children[i]
            drawable.bounds = Rect(left, top, left + itemSize, top + itemSize)
            drawable.draw(canvas)
        }
    }

    /**
     * RADIAL (5-icon outer ring) & RADIAL_9 (clustered center + outer ring) using Nova's Laa/a math.
     */
    private fun drawRadialPreview(canvas: Canvas, children: List<Drawable>, size: Int, maxRingSlots: Int) {
        val slots = FolderGeometry.computeRadialSlots(children.size, size.toFloat(), maxRingSlots)
        for (i in slots.indices) {
            val slot = slots[i]
            val drawable = children[i]
            drawable.bounds = Rect(
                slot.left.toInt(),
                slot.top.toInt(),
                slot.right.toInt(),
                slot.bottom.toInt()
            )
            drawable.draw(canvas)
        }
    }

    /**
     * STACK (3 Icons): Diagonally layered cards (Nova rz.e math).
     */
    private fun drawStackPreview(canvas: Canvas, children: List<Drawable>, size: Int) {
        val count = min(3, children.size)
        if (count == 0) return

        val scales = floatArrayOf(0.70f, 0.60f, 0.50f)
        val stepOffset = size * 0.12f

        // Draw in reverse order so item 0 renders on top
        for (i in (count - 1) downTo 0) {
            val itemScale = scales[i.coerceIn(0, 2)]
            val itemSize = (size * itemScale).toInt()
            val offset = i * stepOffset
            val left = ((size - itemSize) / 2f + offset * 0.7f - stepOffset * 0.6f).toInt()
            val top = ((size - itemSize) / 2f - offset * 0.7f + stepOffset * 0.6f).toInt()

            val drawable = children[i]
            drawable.bounds = Rect(left, top, left + itemSize, top + itemSize)
            drawable.draw(canvas)
        }
    }

    /**
     * FAN (3 Icons): Angled spread fan deck (Nova rz.b math).
     */
    private fun drawFanPreview(canvas: Canvas, children: List<Drawable>, size: Int) {
        val count = min(3, children.size)
        if (count == 0) return

        if (count == 1) {
            val itemSize = (size * 0.65f).toInt()
            val left = (size - itemSize) / 2
            val top = (size - itemSize) / 2
            children[0].bounds = Rect(left, top, left + itemSize, top + itemSize)
            children[0].draw(canvas)
            return
        }

        // Draw left and right wings first, then center card
        val sideScale = 0.50f
        val centerScale = 0.62f
        val sideSize = (size * sideScale).toInt()
        val centerSize = (size * centerScale).toInt()

        // Wing 1 (Left card)
        if (count >= 2) {
            val save = canvas.save()
            val cx = size * 0.32f
            val cy = size * 0.50f
            canvas.rotate(-16f, cx, cy)
            val left = (cx - sideSize / 2f).toInt()
            val top = (cy - sideSize / 2f).toInt()
            children[1].bounds = Rect(left, top, left + sideSize, top + sideSize)
            children[1].draw(canvas)
            canvas.restoreToCount(save)
        }

        // Wing 2 (Right card)
        if (count >= 3) {
            val save = canvas.save()
            val cx = size * 0.68f
            val cy = size * 0.50f
            canvas.rotate(16f, cx, cy)
            val left = (cx - sideSize / 2f).toInt()
            val top = (cy - sideSize / 2f).toInt()
            children[2].bounds = Rect(left, top, left + sideSize, top + sideSize)
            children[2].draw(canvas)
            canvas.restoreToCount(save)
        }

        // Center card (on top)
        val centerLeft = ((size - centerSize) / 2f).toInt()
        val centerTop = ((size - centerSize) / 2f + size * 0.04f).toInt()
        children[0].bounds = Rect(centerLeft, centerTop, centerLeft + centerSize, centerTop + centerSize)
        children[0].draw(canvas)
    }

    /**
     * LINE (3 Icons): Horizontal centered row of miniature icons.
     */
    private fun drawLinePreview(canvas: Canvas, children: List<Drawable>, size: Int) {
        val count = min(3, children.size)
        if (count == 0) return

        val iconScale = 0.28f
        val itemSize = (size * iconScale).toInt()
        val padding = ((size - count * itemSize) / (count + 1).toFloat())
        val top = ((size - itemSize) / 2f).toInt()

        for (i in 0 until count) {
            val left = (padding + i * (itemSize + padding)).toInt()
            val drawable = children[i]
            drawable.bounds = Rect(left, top, left + itemSize, top + itemSize)
            drawable.draw(canvas)
        }
    }

    /**
     * FIRST_APP (1 Icon): Single first app icon preview (Nova Launcher signature style).
     */
    private fun drawFirstAppPreview(canvas: Canvas, children: List<Drawable>, size: Int) {
        if (children.isEmpty()) return
        val itemSize = (size * 0.80f).toInt()
        val left = (size - itemSize) / 2
        val top = (size - itemSize) / 2
        val first = children[0]
        first.bounds = Rect(left, top, left + itemSize, top + itemSize)
        first.draw(canvas)
    }
}
