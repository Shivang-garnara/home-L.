package com.example.launcherai.ui

import android.graphics.Rect
import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

/**
 * Custom ViewPager2 PageTransformer implementing all 13 Nova Launcher scroll transitions:
 * Simple, Cube, Card stack, Accordion, Flip, Flip vertical, Glass, In and out,
 * Revolving door, Tablet, Throw, Wipe, and Zoom fade.
 */
class WorkspacePageTransformer(
    var effect: Effect = Effect.SIMPLE
) : ViewPager2.PageTransformer {

    enum class Effect(val displayName: String) {
        SIMPLE("Simple"),
        CUBE("Cube"),
        CARD_STACK("Card stack"),
        ACCORDION("Accordion"),
        FLIP("Flip"),
        FLIP_VERTICAL("Flip vertical"),
        GLASS("Glass"),
        IN_AND_OUT("In and out"),
        REVOLVING_DOOR("Revolving door"),
        TABLET("Tablet"),
        THROW("Throw"),
        WIPE("Wipe"),
        ZOOM_FADE("Zoom fade");

        companion object {
            fun fromString(name: String?): Effect {
                return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: SIMPLE
            }
        }
    }

    private val wipeRect = Rect()

    override fun transformPage(view: View, position: Float) {
        val density = view.resources.displayMetrics.density
        // Reset clipBounds unless WIPE is active
        if (effect != Effect.WIPE && view.clipBounds != null) {
            view.clipBounds = null
        }

        when (effect) {
            Effect.SIMPLE -> {
                resetTransforms(view)
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
                    view.rotation = 0f
                    view.rotationX = 0f
                    view.scaleX = 1f
                    view.scaleY = 1f
                    view.pivotY = view.height * 0.5f

                    if (position <= 0f) {
                        view.pivotX = view.width.toFloat()
                        view.rotationY = 90f * position
                    } else {
                        view.pivotX = 0f
                        view.rotationY = 90f * position
                    }
                }
            }

            Effect.CARD_STACK -> {
                view.cameraDistance = 1200f * density * 16f
                view.rotation = 0f
                view.rotationX = 0f
                view.rotationY = 0f
                view.pivotX = view.width * 0.5f
                view.pivotY = view.height * 0.5f

                if (position < -1f || position > 1f) {
                    view.alpha = 0f
                } else if (position <= 0f) {
                    view.alpha = 1f
                    view.translationX = 0f
                    view.translationY = 0f
                    view.scaleX = 1f
                    view.scaleY = 1f
                    view.translationZ = 0f
                } else {
                    view.alpha = 1f - (position * 0.4f)
                    view.translationX = -view.width * position
                    view.translationY = 0f
                    val scale = 0.82f + (1f - position) * 0.18f
                    view.scaleX = scale
                    view.scaleY = scale
                    view.translationZ = -position * 10f
                }
            }

            Effect.ACCORDION -> {
                view.cameraDistance = 1000f * density * 16f
                view.rotation = 0f
                view.rotationX = 0f
                view.rotationY = 0f
                view.translationY = 0f
                view.pivotY = view.height * 0.5f

                if (position < -1f || position > 1f) {
                    view.alpha = 0f
                } else {
                    view.alpha = 1f
                    val scale = (1f - abs(position)).coerceIn(0f, 1f)
                    view.scaleX = scale
                    view.scaleY = 1f
                    view.translationZ = 0f
                    view.translationX = 0f
                    view.pivotX = if (position <= 0f) view.width.toFloat() else 0f
                }
            }

            Effect.FLIP -> {
                view.cameraDistance = 1000f * density * 16f
                view.pivotX = view.width * 0.5f
                view.pivotY = view.height * 0.5f
                view.rotation = 0f
                view.rotationX = 0f
                view.translationY = 0f

                if (position < -1f || position > 1f) {
                    view.alpha = 0f
                } else {
                    view.rotationY = -180f * position
                    view.translationX = -position * view.width
                    val scale = 1f - (0.5f - abs(0.5f - abs(position))) * 0.5f
                    view.scaleX = scale
                    view.scaleY = scale
                    view.translationZ = 0f
                    view.alpha = if (abs(position) <= 0.5f) 1f else 0f
                }
            }

            Effect.FLIP_VERTICAL -> {
                view.cameraDistance = 1000f * density * 16f
                view.pivotX = view.width * 0.5f
                view.pivotY = view.height * 0.5f
                view.rotation = 0f
                view.rotationY = 0f
                view.translationY = 0f

                if (position < -1f || position > 1f) {
                    view.alpha = 0f
                } else {
                    view.rotationX = -180f * position
                    view.translationX = -position * view.width
                    val scale = 1f - (0.5f - abs(0.5f - abs(position))) * 0.5f
                    view.scaleX = scale
                    view.scaleY = scale
                    view.translationZ = 0f
                    view.alpha = if (abs(position) <= 0.5f) 1f else 0f
                }
            }

            Effect.GLASS -> {
                resetTransforms(view)
                val absPos = abs(position)
                if (absPos > 1f) {
                    view.alpha = 0f
                } else {
                    view.translationX = -position * view.width
                    view.alpha = (1f - absPos).coerceIn(0f, 1f)
                    val scale = if (absPos < 0.5f) 1f else (1f - absPos) + 0.5f
                    view.scaleX = scale
                    view.scaleY = scale
                }
            }

            Effect.IN_AND_OUT -> {
                view.cameraDistance = 800f * density * 16f
                view.rotation = 0f
                view.rotationX = 0f
                view.translationX = 0f
                view.translationY = 0f
                view.translationZ = 0f

                val absPos = abs(position)
                if (absPos > 1f) {
                    view.alpha = 0f
                } else {
                    val rotY = -90f * position
                    val scale = (0.5f - abs(0.5f - absPos)) + 1.0f
                    view.scaleX = scale
                    view.scaleY = scale
                    view.pivotX = when {
                        rotY == 0f -> view.width * 0.5f
                        rotY < 0f -> view.width.toFloat()
                        else -> 0f
                    }
                    view.pivotY = view.height * 0.5f
                    view.rotationY = rotY
                    view.alpha = if (absPos > 0.75f) (1f - ((absPos - 0.75f) / 0.25f)).coerceIn(0f, 1f) else 1f
                }
            }

            Effect.REVOLVING_DOOR -> {
                view.cameraDistance = 800f * density * 16f
                view.rotation = 0f
                view.rotationX = 0f
                view.translationX = 0f
                view.translationY = 0f
                view.translationZ = 0f
                view.scaleX = 1f
                view.scaleY = 1f
                view.pivotX = view.width * 0.5f
                view.pivotY = view.height * 0.5f

                if (position < -1f || position > 1f) {
                    view.alpha = 0f
                } else {
                    view.rotationY = 25f * position
                    view.alpha = 1f - abs(position) * 0.25f
                }
            }

            Effect.TABLET -> {
                view.cameraDistance = 1200f * density * 16f
                view.rotation = 0f
                view.rotationX = 0f
                view.translationY = 0f
                view.translationZ = 0f
                view.scaleX = 1f
                view.scaleY = 1f
                view.pivotX = view.width * 0.5f
                view.pivotY = view.height * 0.5f

                val absPos = abs(position)
                if (absPos > 1f) {
                    view.alpha = 0f
                } else {
                    view.translationX = -position * view.width * 0.15f
                    view.rotationY = -45f * position
                    view.alpha = (1f - absPos).coerceIn(0f, 1f)
                }
            }

            Effect.THROW -> {
                resetTransforms(view)
                val absPos = abs(position)
                if (absPos > 1f) {
                    view.alpha = 0f
                } else {
                    view.pivotX = view.width * 0.5f
                    view.pivotY = view.height * 1.5f
                    view.rotation = -20f * position
                    view.translationY = absPos * view.height * 0.15f
                    view.alpha = (1f - absPos).coerceIn(0f, 1f)
                }
            }

            Effect.WIPE -> {
                resetTransforms(view)
                val absPos = abs(position)
                if (absPos > 1f) {
                    view.alpha = 0f
                } else {
                    view.alpha = 1f
                    view.translationX = -position * view.width
                    val w = view.width
                    val h = view.height
                    if (w > 0 && h > 0) {
                        if (position <= 0f) {
                            val left = (w * absPos).toInt().coerceIn(0, w)
                            wipeRect.set(left, 0, w, h)
                        } else {
                            val right = (w * (1f - position)).toInt().coerceIn(0, w)
                            wipeRect.set(0, 0, right, h)
                        }
                        view.clipBounds = wipeRect
                    }
                }
            }

            Effect.ZOOM_FADE -> {
                resetTransforms(view)
                val absPos = abs(position)
                if (absPos > 1f) {
                    view.alpha = 0f
                } else {
                    val scale = 1f - absPos * 0.3f
                    view.scaleX = scale
                    view.scaleY = scale
                    view.alpha = (1f - absPos).coerceIn(0f, 1f)
                }
            }
        }
    }

    private fun resetTransforms(view: View) {
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
}
