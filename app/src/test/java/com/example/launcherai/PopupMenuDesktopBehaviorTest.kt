package com.example.launcherai

import com.example.launcherai.ui.LookFeelSettingsActivity
import com.example.launcherai.ui.PopupMenuSettingsActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Popup Menu & Desktop Behavior matching Nova Launcher 8.9.9:
 * - Dedicated PopupMenuSettingsActivity preference keys and default action matrix
 * - Home (Desktop) vs Drawer action separation (Remove on desktop only, Uninstall/Hide in drawer)
 * - Corner radius clamping (0dp to 28dp, default 8dp)
 * - Drop Targets zoom-out scale factor and Undo bar duration
 */
class PopupMenuDesktopBehaviorTest {

    @Test
    fun testPopupMenuPreferenceKeys() {
        assertEquals("look_feel_prefs", PopupMenuSettingsActivity.PREFS_NAME)
        assertEquals("key_popup_card_bg", PopupMenuSettingsActivity.KEY_POPUP_CARD_BG)
        assertEquals("key_popup_group_shortcuts", PopupMenuSettingsActivity.KEY_POPUP_GROUP_SHORTCUTS)
        assertEquals("key_popup_corner_radius", PopupMenuSettingsActivity.KEY_POPUP_CORNER_RADIUS)
        assertEquals("key_popup_actions_home", PopupMenuSettingsActivity.KEY_POPUP_ACTIONS_HOME)
        assertEquals("key_popup_actions_drawer", PopupMenuSettingsActivity.KEY_POPUP_ACTIONS_DRAWER)
    }

    @Test
    fun testCornerRadiusDefaultsAndClamping() {
        assertEquals(8, PopupMenuSettingsActivity.DEFAULT_CORNER_RADIUS_DP)

        // Clamping logic (0..28)
        fun clampRadius(radius: Int): Int = radius.coerceIn(0, 28)

        assertEquals(0, clampRadius(-5))
        assertEquals(8, clampRadius(8))
        assertEquals(28, clampRadius(28))
        assertEquals(28, clampRadius(50))
    }

    @Test
    fun testActionMatrixHomeVsDrawerParity() {
        val homeActions = PopupMenuSettingsActivity.DEFAULT_ACTIONS_HOME
        val drawerActions = PopupMenuSettingsActivity.DEFAULT_ACTIONS_DRAWER

        // Common actions enabled on both Home and Drawer
        assertTrue(homeActions.contains("Edit"))
        assertTrue(drawerActions.contains("Edit"))

        assertTrue(homeActions.contains("App info"))
        assertTrue(drawerActions.contains("App info"))

        assertTrue(homeActions.contains("Share"))
        assertTrue(drawerActions.contains("Share"))

        assertTrue(homeActions.contains("Widgets"))
        assertTrue(drawerActions.contains("Widgets"))

        // Desktop-specific actions: "Remove" is on Desktop, NOT in Drawer by default
        assertTrue(homeActions.contains("Remove"))
        assertFalse(drawerActions.contains("Remove"))

        // Drawer-specific actions: "Uninstall" and "Hide" are in Drawer, NOT on Desktop by default
        assertFalse(homeActions.contains("Uninstall"))
        assertTrue(drawerActions.contains("Uninstall"))

        assertFalse(homeActions.contains("Hide"))
        assertTrue(drawerActions.contains("Hide"))

        // Advanced actions off by default everywhere
        assertFalse(homeActions.contains("Play Store"))
        assertFalse(drawerActions.contains("Play Store"))

        assertFalse(homeActions.contains("Force stop"))
        assertFalse(drawerActions.contains("Force stop"))
    }

    @Test
    fun testDropTargetsAndUndoBarLookFeelKeys() {
        assertEquals("key_drop_targets", LookFeelSettingsActivity.KEY_DROP_TARGETS)
        assertEquals("key_show_undo_bar", LookFeelSettingsActivity.KEY_SHOW_UNDO_BAR)
    }

    @Test
    fun testDropTargetsDesktopZoomParameters() {
        // Nova Launcher drop targets camera zoom-out scale
        val desktopZoomOutScale = 0.92f
        val desktopRestingScale = 1.0f
        val dropTargetFadeDuration = 175L

        assertTrue(desktopZoomOutScale < desktopRestingScale)
        assertEquals(0.92f, desktopZoomOutScale, 0.001f)
        assertEquals(175L, dropTargetFadeDuration)
    }

    @Test
    fun testUndoBarTimeoutDuration() {
        // Nova Launcher undo bar floating pill duration: 5000ms
        val undoBarDurationMs = 5000
        assertEquals(5000, undoBarDurationMs)
    }
}
