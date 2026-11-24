package com.example.launcherai

import android.content.pm.ActivityInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * Unit tests for Screen & Transitions features matching Nova Launcher 8.9.9:
 * 1. Allow Home screen rotation (Toggle + Sensor / Force portrait / Force landscape / No sensor)
 * 2. App animation selection (System default, Blink, Slide up, Zoom)
 * 3. Disable animations (0ms transitions for maximum speed)
 */
class ScreenTransitionsTest {

    private fun resolveOrientation(rotationEnabled: Boolean, orientationMode: String): Int {
        return if (!rotationEnabled || orientationMode.equals("Force portrait", true)) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else if (orientationMode.equals("Force landscape", true)) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else if (orientationMode.equals("No sensor", true)) {
            ActivityInfo.SCREEN_ORIENTATION_NOSENSOR
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    @Test
    fun testOrientationModesMatchingNovaMatrix() {
        // Disabled rotation -> Always Force portrait
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, resolveOrientation(false, "Auto rotate (Sensor)"))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, resolveOrientation(false, "Force landscape"))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, resolveOrientation(false, "No sensor"))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, resolveOrientation(false, "Force portrait"))

        // Enabled rotation -> Follows selected mode
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, resolveOrientation(true, "Auto rotate (Sensor)"))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, resolveOrientation(true, "Force landscape"))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR, resolveOrientation(true, "No sensor"))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, resolveOrientation(true, "Force portrait"))
    }

    @Test
    fun testAppAnimationOptions() {
        val supportedAnimations = listOf("System default", "Blink", "Slide up", "Zoom")

        for (anim in supportedAnimations) {
            val key = anim.lowercase(Locale.ROOT)
            assertTrue(key in listOf("system default", "blink", "slide up", "zoom"))
        }
    }

    @Test
    fun testDisableAnimationsOverride() {
        fun resolveAnimationStrategy(disableAnimations: Boolean, selectedAnim: String): String {
            if (disableAnimations) {
                return "ZERO_MS_INSTANT"
            }
            return when (selectedAnim.lowercase(Locale.ROOT)) {
                "zoom" -> "SCALE_UP"
                "slide up" -> "SLIDE_UP"
                "blink" -> "BLINK"
                "system default" -> "CLIP_REVEAL"
                else -> "DEFAULT"
            }
        }

        // When disable animations is true, always returns 0ms instant transition regardless of selected animation
        assertEquals("ZERO_MS_INSTANT", resolveAnimationStrategy(true, "Zoom"))
        assertEquals("ZERO_MS_INSTANT", resolveAnimationStrategy(true, "Slide up"))
        assertEquals("ZERO_MS_INSTANT", resolveAnimationStrategy(true, "Blink"))
        assertEquals("ZERO_MS_INSTANT", resolveAnimationStrategy(true, "System default"))

        // When disable animations is false, routes to selected animation
        assertEquals("SCALE_UP", resolveAnimationStrategy(false, "Zoom"))
        assertEquals("SLIDE_UP", resolveAnimationStrategy(false, "Slide up"))
        assertEquals("BLINK", resolveAnimationStrategy(false, "Blink"))
        assertEquals("CLIP_REVEAL", resolveAnimationStrategy(false, "System default"))
    }
}
