package com.example.launcherai.ui

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

/**
 * Custom ViewPager2 PageTransformer implementing Nova Launcher style scroll transitions:
 * - SIMPLE: Linear translation without 3D perspective distortion.
 * - CUBE: 3D rotating cube animation with camera perspective and dynamic edge pivots.
 * - CARD_STACK: Depth-based card layering with translation compensation and scale down.
 * - ACCORDION: Horizontal accordion expansion and compression from the edge pivot.
 * - FLIP: 3D card flip rotation on the Y-axis with 90° face culling.
 */
class WorkspacePageTransformer(
    var effect: Effect = Effect.SIMPLE
) : ViewPager2.PageTransformer {

    enum class Effect {
        SIMPLE,
        CUBE,
        CARD_STACK,
        ACCORDION,
        FLIP;

        companion object {
            fun fromString(name: String?): Effect {
                return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: SIMPLE
            }
        }
    }

    override fun transformPage(view: View, position: Float) {
        val density = view.resources.displayMetrics.density

        when (effect) {
            Effect.SIMPLE -> {
                view.alpha = 1f
                view.translationX = 0f
                view.translationY = 0f
                view.translationZ = 0f
                view.rotation = 0f
                view.rotationX = 0f
                view.rotationY = 0f
                view.scaleX = 1f
                view.scaleY = 1f
                view.pivotX = view.width * 0.5f
                view.pivotY = view.height * 0.5f
            }

            Effect.CUBE -> {
                view.cameraDistance = 800f * density * 16f
                if (position < -1f || position > 1f) {
                    view.alpha = 0f
                } else {
                    view.alpha = 1f
                    view.translationX = 0f
                    view.translationY = 0f
                    view.translationZ = 0f
                    view.scaleX = 1f
                    view.scaleY = 1f
                    view.pivotY = view.height * 0.5f

                    if (position <= 0f) {
                        // Left page: pivot on right edge, rotate inward
                        view.pivotX = view.width.toFloat()
                        view.rotationY = 90f * position
                    } else {
                        // Right page: pivot on left edge, rotate inward
                        view.pivotX = 0f
                        view.rotationY = 90f * position
                    }
                }
            }

            Effect.CARD_STACK -> {
                view.cameraDistance = 1200f * density * 16f
                view.rotationX = 0f
                view.rotationY = 0f
                view.pivotX = view.width * 0.5f
                view.pivotY = view.height * 0.5f

                if (position < -1f) {
                    view.alpha = 0f
                } else if (position <= 0f) {
                    // Current page sliding away normally
                    view.alpha = 1f
                    view.translationX = 0f
                    view.scaleX = 1f
                    view.scaleY = 1f
                    view.translationZ = 0f
                } else if (position <= 1f) {
                    // Incoming page from right: remains underneath and scales up smoothly
                    view.alpha = 1f - (position * 0.4f)
                    view.translationX = -view.width * position
                    val scale = 0.82f + (1f - position) * 0.18f
                    view.scaleX = scale
                    view.scaleY = scale
                    view.translationZ = -position * 10f
                } else {
                    view.alpha = 0f
                }
            }

            Effect.ACCORDION -> {
                view.cameraDistance = 1000f * density * 16f
                view.rotationX = 0f
                view.rotationY = 0f
                view.pivotY = view.height * 0.5f

                if (position < -1f || position > 1f) {
                    view.alpha = 0f
                } else {
                    view.alpha = 1f
                    val scale = 1f - abs(position)
                    view.scaleX = scale.coerceIn(0f, 1f)
                    view.scaleY = 1f
                    view.translationZ = 0f

                    if (position <= 0f) {
                        view.pivotX = view.width.toFloat()
                        view.translationX = 0f
                    } else {
                        view.pivotX = 0f
                        view.translationX = 0f
                    }
                }
            }

            Effect.FLIP -> {
                view.cameraDistance = 1000f * density * 16f
                view.pivotX = view.width * 0.5f
                view.pivotY = view.height * 0.5f
                view.rotationX = 0f

                if (position < -1f || position > 1f) {
                    view.alpha = 0f
                } else {
                    val rotation = -180f * position
                    view.rotationY = rotation
                    // Counteract default horizontal translation so it flips in-place
                    view.translationX = -position * view.width
                    view.scaleX = 1f
                    view.scaleY = 1f
                    view.translationZ = 0f
                    // Culling back face when rotated beyond 90 degrees
                    view.alpha = if (abs(position) <= 0.5f) 1f else 0f
                }
            }
        }
    }
}
