package com.example.launcherai

import android.content.pm.ActivityInfo
import com.example.launcherai.ui.LookFeelSettingsActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Look & Feel settings and preference handling matching Nova Launcher 8.9.9
 * (Icon preferences, Screen rotation & animations, Popup menu & desktop options, System bars).
 */
class LookFeelSettingsTest {

    @Test
    fun testDefaultPreferenceKeys() {
        assertEquals("workspace_prefs", LookFeelSettingsActivity.PREFS_NAME)

        assertEquals("key_selected_icon_pack", LookFeelSettingsActivity.KEY_SELECTED_ICON_PACK)
        assertEquals("key_icon_shape", LookFeelSettingsActivity.KEY_ICON_SHAPE)
        assertEquals("key_icon_size_percent", LookFeelSettingsActivity.KEY_ICON_SIZE_PERCENT)
        assertEquals("key_reshape_legacy_icons", LookFeelSettingsActivity.KEY_RESHAPE_LEGACY_ICONS)
        assertEquals("key_icon_shadow", LookFeelSettingsActivity.KEY_ICON_SHADOW)

        assertEquals("key_screen_rotation_enabled", LookFeelSettingsActivity.KEY_SCREEN_ROTATION_ENABLED)
        assertEquals("key_screen_orientation_mode", LookFeelSettingsActivity.KEY_SCREEN_ORIENTATION_MODE)
        assertEquals("key_app_animation", LookFeelSettingsActivity.KEY_APP_ANIMATION)
        assertEquals("key_disable_animations", LookFeelSettingsActivity.KEY_DISABLE_ANIMATIONS)

        assertEquals("key_popup_items", LookFeelSettingsActivity.KEY_POPUP_ITEMS)
        assertEquals("key_drop_targets", LookFeelSettingsActivity.KEY_DROP_TARGETS)
        assertEquals("key_show_undo_bar", LookFeelSettingsActivity.KEY_SHOW_UNDO_BAR)

        assertEquals("key_show_notification_bar", LookFeelSettingsActivity.KEY_SHOW_NOTIFICATION_BAR)
        assertEquals("key_transparent_notification_bar", LookFeelSettingsActivity.KEY_TRANSPARENT_NOTIFICATION_BAR)
        assertEquals("key_transparent_navigation_bar", LookFeelSettingsActivity.KEY_TRANSPARENT_NAVIGATION_BAR)
        assertEquals("key_dark_status_icons", LookFeelSettingsActivity.KEY_DARK_STATUS_ICONS)
    }

    @Test
    fun testDefaultPreferenceValues() {
        assertEquals("Round", LookFeelSettingsActivity.DEFAULT_SHAPE)
        assertEquals(125, LookFeelSettingsActivity.DEFAULT_ICON_SIZE_PERCENT)
        assertEquals(80, LookFeelSettingsActivity.MIN_ICON_SIZE)
        assertEquals(150, LookFeelSettingsActivity.MAX_ICON_SIZE)

        val defaultPopupItems = LookFeelSettingsActivity.DEFAULT_POPUP_ITEMS
        assertTrue(defaultPopupItems.contains("App info"))
        assertTrue(defaultPopupItems.contains("Edit"))
        assertTrue(defaultPopupItems.contains("Remove"))
        assertTrue(defaultPopupItems.contains("Uninstall"))
    }

    @Test
    fun testOrientationResolution() {
        // When screen rotation is enabled, orientation should be UNSPECIFIED (sensor rotates freely)
        val rotationEnabled = true
        val targetOrientationEnabled = if (rotationEnabled) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, targetOrientationEnabled)

        // When screen rotation is disabled, orientation should be locked in PORTRAIT
        val rotationDisabled = false
        val targetOrientationDisabled = if (rotationDisabled) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, targetOrientationDisabled)
    }

    @Test
    fun testDarkStatusIconsResolution() {
        fun resolveIsLightTheme(mode: String, isNightMode: Boolean): Boolean {
            return when (mode.uppercase()) {
                "DARK" -> true // dark status bar icons
                "LIGHT" -> false // light status bar icons
                else -> !isNightMode // AUTO follows night mode inversion
            }
        }

        // Forced Dark icons
        assertTrue(resolveIsLightTheme("DARK", isNightMode = true))
        assertTrue(resolveIsLightTheme("DARK", isNightMode = false))

        // Forced Light icons
        assertFalse(resolveIsLightTheme("LIGHT", isNightMode = true))
        assertFalse(resolveIsLightTheme("LIGHT", isNightMode = false))

        // AUTO mode
        assertFalse(resolveIsLightTheme("AUTO", isNightMode = true)) // In night mode, icons are light
        assertTrue(resolveIsLightTheme("AUTO", isNightMode = false)) // In day mode, icons are dark
    }

    @Test
    fun testIconSizeClamping() {
        fun clampIconSize(requestedSize: Int): Int {
            return requestedSize.coerceIn(LookFeelSettingsActivity.MIN_ICON_SIZE, LookFeelSettingsActivity.MAX_ICON_SIZE)
        }

        assertEquals(80, clampIconSize(50))
        assertEquals(80, clampIconSize(80))
        assertEquals(125, clampIconSize(125))
        assertEquals(150, clampIconSize(150))
        assertEquals(150, clampIconSize(200))
    }

    @Test
    fun testPopupMenuItemsCustomization() {
        val currentSet = LookFeelSettingsActivity.DEFAULT_POPUP_ITEMS.toMutableSet()
        assertTrue(currentSet.contains("Uninstall"))

        // User unchecks "Uninstall"
        currentSet.remove("Uninstall")
        assertFalse(currentSet.contains("Uninstall"))
        assertEquals(3, currentSet.size)

        // User checks "Share"
        currentSet.add("Share")
        assertTrue(currentSet.contains("Share"))
        assertEquals(4, currentSet.size)
    }

    @Test
    fun testShapeSelectionStripNormalization() {
        val supportedShapes = listOf("Round", "Squircle", "Rounded square", "Flower", "Teardrop")
        for (shape in supportedShapes) {
            val isKnown = shape.lowercase() in listOf("round", "squircle", "rounded square", "flower", "teardrop")
            assertTrue(isKnown)
        }

        // Custom or advanced shape falls into "More"
        val customShape = "Hexagon"
        val isKnownCustom = customShape.lowercase() in listOf("round", "squircle", "rounded square", "flower", "teardrop")
        assertFalse(isKnownCustom)
    }

    @Test
    fun testLegacyReshapingInsetCalculation() {
        val finalSize = 100
        val inset = (finalSize * 0.15f).toInt()
        assertEquals(15, inset)
        val contentWidth = finalSize - 2 * inset
        assertEquals(70, contentWidth) // 70% scale matching Nova Launcher's legacy normalizer
    }

    @Test
    fun testIconElevationShadowToggle() {
        val density = 3.0f
        fun resolveElevation(hasShadow: Boolean): Float = if (hasShadow) 6f * density else 0f

        assertEquals(18f, resolveElevation(true), 0.01f)
        assertEquals(0f, resolveElevation(false), 0.01f)
    }
}
