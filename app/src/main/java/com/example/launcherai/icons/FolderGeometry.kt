package com.example.launcherai.icons

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Pure-Kotlin geometry and state helpers mirroring Nova Launcher's folder runtime behavior:
 * - `Laa/a;.k` & `Laa/a;.l` radial multi-ring slot coordinates for 5-icon and 9-icon previews
 * - `Li10/n0;` & `Li10/p0;` folder window mode locks (Grid AUTO, Shadow, Card background, Align Bottom)
 * - `Li10/i0;` & `Laa/b1;` folder icon background linking (`Match folder window color`)
 */
object FolderGeometry {

    const val MODE_WINDOW = "WINDOW"
    const val MODE_IMMERSIVE = "IMMERSIVE"

    data class RadialSlot(
        val left: Float,
        val top: Float,
        val size: Float
    ) {
        val right: Float get() = left + size
        val bottom: Float get() = top + size
        val centerX: Float get() = left + size / 2f
        val centerY: Float get() = top + size / 2f
    }

    data class ResolvedFolderWindowAppearance(
        val isImmersive: Boolean,
        val showShadow: Boolean,
        val showCardBackground: Boolean,
        val alignBottom: Boolean,
        val isGridAutoLocked: Boolean,
        val isCornerRadiusEnabled: Boolean,
        val effectiveElevationDp: Float,
        val effectiveCornerRadiusDp: Float,
        val effectiveBackgroundAlpha: Int
    )

    data class ResolvedFolderIconColors(
        val color: Int,
        /** Transparency percentage 0 (opaque) .. 100 (fully transparent) for FolderIconRenderer. */
        val transparencyPercent: Int
    )

    /**
     * Nova Launcher `Laa/a;.l:(I)F` sub-icon scale factor relative to available folder plate size.
     */
    fun radialScaleFactor(count: Int): Float = when {
        count <= 3 -> 0.51f
        count == 4 -> 0.44f
        count == 5 -> 0.40f
        count == 6 -> 0.36f
        count == 7 -> 0.33f
        else -> 0.30f
    }

    /**
     * Computes Nova-accurate slot rectangles for `RADIAL_5` (maxRingSlots = 5) and
     * `RADIAL_9` (maxRingSlots = 9, clustered center + outer ring for >= 7 items).
     */
    fun computeRadialSlots(
        itemCount: Int,
        plateSizePx: Float,
        maxRingSlots: Int = 5
    ): List<RadialSlot> {
        val cappedMax = if (maxRingSlots >= 9) 9 else 5
        val count = min(itemCount, cappedMax).coerceAtLeast(0)
        if (count == 0 || plateSizePx <= 0f) return emptyList()

        val avail = plateSizePx * if (cappedMax >= 9 && count >= 7) 0.84f else 0.80f
        val centerXY = plateSizePx / 2f

        if (count == 1) {
            val iconSize = avail * 0.56f
            return listOf(RadialSlot(centerXY - iconSize / 2f, centerXY - iconSize / 2f, iconSize))
        }

        // For RADIAL_9 with 7..9 items: 1 center focal icon + (count - 1) outer ring icons (mirroring Laa/a;.k)
        val hasCenterHub = cappedMax >= 9 && count >= 7
        val ringCount = if (hasCenterHub) count - 1 else count
        val scaleFactor = if (hasCenterHub) {
            when (count) {
                7 -> 0.32f
                8 -> 0.305f
                else -> 0.29f
            }
        } else {
            radialScaleFactor(ringCount)
        }
        val iconSize = avail * scaleFactor
        val halfIcon = iconSize / 2f

        // Mirror Laa/a;.c and Laa/a;.k radius formula:
        // c = 1.15 * avail / 2; orbitRadius = (c / 2) * (1 + ((ringCount - 2) * 0.25) / (g - 2))
        val g = max(cappedMax, 3)
        val baseC = (1.15f * avail) / 2f
        val ringFactor = 1f + ((max(ringCount, 2) - 2) * 0.25f) / (g - 2).toFloat()
        val orbitRadius = (baseC / 2f) * ringFactor

        val startOffset = when (ringCount) {
            2 -> 0.0
            3 -> PI / 2.0
            4 -> PI / 4.0
            5 -> PI / 10.0
            6 -> PI / 6.0
            7 -> 0.673198 // ~38.57 deg (2*PI/7 * 0.75) from Nova Laa/a;.k
            8 -> PI / 4.0
            else -> 0.0
        }
        val baseAngle = PI
        val dir = -1.0

        val slots = ArrayList<RadialSlot>(count)
        if (hasCenterHub) {
            // Put first item as the central hub or slot index matching visual cluster
            val centerIdx = 0
            for (i in 0 until count) {
                if (i == centerIdx) {
                    slots.add(RadialSlot(centerXY - halfIcon, centerXY - halfIcon, iconSize))
                } else {
                    val ringPos = i - 1
                    val angle = baseAngle + dir * (startOffset + (2.0 * PI / ringCount) * ringPos)
                    val cx = centerXY + (orbitRadius * cos(angle)).toFloat()
                    val cy = centerXY + (-orbitRadius * sin(angle)).toFloat()
                    slots.add(RadialSlot(cx - halfIcon, cy - halfIcon, iconSize))
                }
            }
        } else {
            for (ringPos in 0 until ringCount) {
                val angle = baseAngle + dir * (startOffset + (2.0 * PI / ringCount) * ringPos)
                val cx = centerXY + (orbitRadius * cos(angle)).toFloat()
                val cy = centerXY + (-orbitRadius * sin(angle)).toFloat()
                slots.add(RadialSlot(cx - halfIcon, cy - halfIcon, iconSize))
            }
        }
        return slots
    }

    /**
     * Resolves folder icon plate color + transparency according to Nova `Match folder window color` (`Li10/i0;.d == 262914`).
     * Note `windowBgAlphaPercent` stores opacity (0..100, default 80), while icon bg alpha stores transparency (0..100, default 20).
     */
    fun resolveFolderIconColor(
        matchWindowColor: Boolean,
        windowBgColor: Int,
        windowBgOpacityPercent: Int,
        customIconBgColor: Int,
        customIconBgTransparencyPercent: Int
    ): ResolvedFolderIconColors {
        return if (matchWindowColor) {
            val matchedTransparency = (100 - windowBgOpacityPercent.coerceIn(0, 100)).coerceIn(0, 100)
            ResolvedFolderIconColors(windowBgColor, matchedTransparency)
        } else {
            ResolvedFolderIconColors(customIconBgColor, customIconBgTransparencyPercent.coerceIn(0, 100))
        }
    }

    /**
     * Resolves conditional rules between `Window` and `Immersive` presentation modes (`FolderWindowSettings` + `Folder.m0`).
     */
    fun resolveFolderWindowAppearance(
        style: String,
        shadowEnabled: Boolean,
        cardBackgroundEnabled: Boolean,
        alignBottomEnabled: Boolean,
        cornerRadiusDp: Int,
        bgAlphaPercent: Int
    ): ResolvedFolderWindowAppearance {
        val isImmersive = style.equals(MODE_IMMERSIVE, ignoreCase = true)
        val showShadow = !isImmersive && shadowEnabled
        val showCardBg = !isImmersive || cardBackgroundEnabled
        val alignBottom = isImmersive && alignBottomEnabled
        val isGridAuto = !isImmersive
        val isCornerRadiusEnabled = showCardBg
        val effectiveElevation = if (showShadow) 5f else 0f
        val effectiveCornerRadius = if (showCardBg) cornerRadiusDp.coerceAtLeast(0).toFloat() else 0f
        val effectiveAlpha = if (showCardBg) bgAlphaPercent.coerceIn(0, 100) else 0
        return ResolvedFolderWindowAppearance(
            isImmersive = isImmersive,
            showShadow = showShadow,
            showCardBackground = showCardBg,
            alignBottom = alignBottom,
            isGridAutoLocked = isGridAuto,
            isCornerRadiusEnabled = isCornerRadiusEnabled,
            effectiveElevationDp = effectiveElevation,
            effectiveCornerRadiusDp = effectiveCornerRadius,
            effectiveBackgroundAlpha = effectiveAlpha
        )
    }
}
