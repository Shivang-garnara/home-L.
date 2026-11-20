package com.example.launcherai.icons

import android.content.Context
import kotlin.math.min

/**
 * Device-Adaptive Icon Sizing Engine (Nova Launcher 8.9.9 DeviceProfile Architecture).
 *
 * Dynamically computes safe, device-proportioned icon dimensions based on screen smallest width (sw),
 * display density, active grid columns/rows, and dock capacity.
 * Clamps maximum allowable icon scale so that icons never overflow, clip, or collide with
 * neighboring grid cells or text labels, even when the user selects maximum size (150%).
 */
object DeviceIconMetrics {

    /**
     * Base icon size in dp depending on device form factor (matching Nova Launcher dimen/app_icon_size):
     * - Phones (< 600dp): 48dp
     * - Foldables / small tablets (600dp - 720dp): 56dp
     * - Large tablets (>= 720dp): 64dp
     */
    fun getBaseIconSizeDp(context: Context): Float {
        val swDp = context.resources.configuration.smallestScreenWidthDp
        return when {
            swDp >= 720 -> 64f
            swDp >= 600 -> 56f
            else -> 48f
        }
    }

    /**
     * Calculates the device-adaptive workspace icon size in pixels.
     * Takes into account cell width, cell height, label height, and user's scale percentage.
     */
    fun getWorkspaceIconSizePx(
        context: Context,
        cellWidthPx: Int,
        cellHeightPx: Int,
        sizePercent: Int
    ): Int {
        val density = context.resources.displayMetrics.density
        val basePx = (getBaseIconSizeDp(context) * density).toInt()
        val desiredPx = (basePx * (sizePercent.coerceIn(50, 200) / 100f)).toInt()

        val safeCellWidth = if (cellWidthPx > 0) cellWidthPx else (context.resources.displayMetrics.widthPixels / 4)
        val safeCellHeight = if (cellHeightPx > 0) cellHeightPx else (context.resources.displayMetrics.heightPixels / 5)

        // Reserve space for label text (~18dp) + label margin (~4dp) + cell vertical margins (~6dp)
        val labelSpacePx = (28 * density).toInt()
        val maxSafeWidth = (safeCellWidth * 0.78f).toInt()
        val maxSafeHeight = ((safeCellHeight - labelSpacePx) * 0.85f).toInt()
        val maxSafePx = maxOf((32 * density).toInt(), min(maxSafeWidth, maxSafeHeight))

        return min(desiredPx, maxSafePx)
    }

    /**
     * Calculates the device-adaptive app drawer icon size in pixels.
     * With numColumns (default 5), calculates column width and ensures icon fits cleanly without clipping labels.
     */
    fun getDrawerIconSizePx(
        context: Context,
        numColumns: Int = 5,
        sizePercent: Int
    ): Int {
        val density = context.resources.displayMetrics.density
        val screenWidthPx = context.resources.displayMetrics.widthPixels
        val columnWidthPx = screenWidthPx / maxOf(1, numColumns)

        val basePx = (getBaseIconSizeDp(context) * density).toInt()
        val desiredPx = (basePx * (sizePercent.coerceIn(50, 200) / 100f)).toInt()

        // Max safe size within drawer column: leave 28% margin for cell padding & text
        val maxSafePx = (columnWidthPx * 0.72f).toInt()
        return min(desiredPx, maxSafePx)
    }

    /**
     * Calculates the device-adaptive dock icon size in pixels.
     */
    fun getDockIconSizePx(
        context: Context,
        capacity: Int,
        dockHeightPx: Int,
        sizePercent: Int
    ): Int {
        val density = context.resources.displayMetrics.density
        val screenWidthPx = context.resources.displayMetrics.widthPixels
        val slotWidthPx = screenWidthPx / maxOf(1, capacity)

        val basePx = ((getBaseIconSizeDp(context) - 2f) * density).toInt()
        val desiredPx = (basePx * (sizePercent.coerceIn(50, 200) / 100f)).toInt()

        val safeDockHeight = if (dockHeightPx > 0) dockHeightPx else (72 * density).toInt()
        val maxSafePx = min(
            (slotWidthPx * 0.78f).toInt(),
            (safeDockHeight * 0.78f).toInt()
        )
        return min(desiredPx, maxSafePx)
    }

    /**
     * Calculates the preview icon size in pixels for SetupActivity.
     */
    fun getPreviewIconSizePx(
        context: Context,
        sizePercent: Int
    ): Int {
        val density = context.resources.displayMetrics.density
        val basePx = (getBaseIconSizeDp(context) * density).toInt()
        val desiredPx = (basePx * (sizePercent.coerceIn(50, 200) / 100f)).toInt()
        val maxSafePx = (76 * density).toInt()
        return min(desiredPx, maxSafePx)
    }

    /**
     * Calculates the device-adaptive list-row icon size in pixels for List drawer style.
     */
    fun getListIconSizePx(
        context: Context,
        sizePercent: Int
    ): Int {
        val density = context.resources.displayMetrics.density
        val basePx = (44f * density).toInt()
        val desiredPx = (basePx * (sizePercent.coerceIn(50, 200) / 100f)).toInt()
        val maxSafePx = (54 * density).toInt()
        return min(desiredPx, maxSafePx)
    }
}
