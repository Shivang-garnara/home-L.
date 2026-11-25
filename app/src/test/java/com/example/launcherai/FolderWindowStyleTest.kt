package com.example.launcherai

import com.example.launcherai.icons.FolderGeometry
import com.example.launcherai.icons.FolderIconRenderer
import com.example.launcherai.ui.FolderSettingsActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

/**
 * Unit tests for Folder Window Style configurations matching Nova Launcher 8.9.9
 * (Presentation style WINDOW vs IMMERSIVE, Background Color & Alpha, Corner Radius,
 * Drop Shadow / Elevation, Grid Dimensions Rows x Cols, and Scroll Direction).
 */
class FolderWindowStyleTest {

    @Test
    fun testDefaultWindowStylePreferences() {
        assertEquals("WINDOW", FolderSettingsActivity.WINDOW_STYLE_WINDOW)
        assertEquals("IMMERSIVE", FolderSettingsActivity.WINDOW_STYLE_IMMERSIVE)
        assertEquals("VERTICAL", FolderSettingsActivity.SCROLL_DIR_VERTICAL)
        assertEquals("HORIZONTAL", FolderSettingsActivity.SCROLL_DIR_HORIZONTAL)

        assertEquals("key_folder_window_style", FolderSettingsActivity.KEY_FOLDER_WINDOW_STYLE)
        assertEquals("key_folder_window_bg_color", FolderSettingsActivity.KEY_FOLDER_WINDOW_BG_COLOR)
        assertEquals("key_folder_window_bg_alpha", FolderSettingsActivity.KEY_FOLDER_WINDOW_BG_ALPHA)
        assertEquals("key_folder_window_corner_radius", FolderSettingsActivity.KEY_FOLDER_WINDOW_CORNER_RADIUS)
        assertEquals("key_folder_window_shadow", FolderSettingsActivity.KEY_FOLDER_WINDOW_SHADOW)
        assertEquals("key_folder_grid_rows", FolderSettingsActivity.KEY_FOLDER_GRID_ROWS)
        assertEquals("key_folder_grid_cols", FolderSettingsActivity.KEY_FOLDER_GRID_COLS)
        assertEquals("key_folder_scroll_direction", FolderSettingsActivity.KEY_FOLDER_SCROLL_DIRECTION)
    }

    @Test
    fun testWindowPresentationElevationAndCornerRadius() {
        val windowStyle = FolderSettingsActivity.WINDOW_STYLE_WINDOW
        val cornerRadiusDp = 16
        val shadowEnabled = true

        val effectiveCornerRadius = if (windowStyle == FolderSettingsActivity.WINDOW_STYLE_WINDOW) cornerRadiusDp else 0
        val effectiveElevationDp = if (windowStyle == FolderSettingsActivity.WINDOW_STYLE_WINDOW && shadowEnabled) 5f else 0f

        assertEquals(16, effectiveCornerRadius)
        assertEquals(5f, effectiveElevationDp, 0.01f)
    }

    @Test
    fun testImmersivePresentationBypassesCornerRadiusAndElevation() {
        val immersiveStyle = FolderSettingsActivity.WINDOW_STYLE_IMMERSIVE
        val cornerRadiusDp = 24
        val shadowEnabled = true

        // In Nova Launcher IMMERSIVE mode, corners are flush (0dp) and drop shadow elevation is 0dp
        val effectiveCornerRadius = if (immersiveStyle == FolderSettingsActivity.WINDOW_STYLE_WINDOW) cornerRadiusDp else 0
        val effectiveElevationDp = if (immersiveStyle == FolderSettingsActivity.WINDOW_STYLE_WINDOW && shadowEnabled) 5f else 0f

        assertEquals(0, effectiveCornerRadius)
        assertEquals(0f, effectiveElevationDp, 0.01f)
    }

    @Test
    fun testHorizontalPagingCalculation() {
        val rows = 3
        val cols = 4
        val itemsPerPage = rows * cols // 12 items per page

        // 8 items -> 1 page
        val items8 = 8
        val pages8 = Math.ceil(items8.toDouble() / itemsPerPage).toInt().coerceAtLeast(1)
        assertEquals(1, pages8)

        // 12 items -> 1 page
        val items12 = 12
        val pages12 = Math.ceil(items12.toDouble() / itemsPerPage).toInt().coerceAtLeast(1)
        assertEquals(1, pages12)

        // 13 items -> 2 pages
        val items13 = 13
        val pages13 = Math.ceil(items13.toDouble() / itemsPerPage).toInt().coerceAtLeast(1)
        assertEquals(2, pages13)

        // 25 items -> 3 pages
        val items25 = 25
        val pages25 = Math.ceil(items25.toDouble() / itemsPerPage).toInt().coerceAtLeast(1)
        assertEquals(3, pages25)
    }

    @Test
    fun testAlphaComponentCalculation() {
        val bgAlphaPercent = 80
        val alphaInt = ((bgAlphaPercent / 100f) * 255).toInt().coerceIn(0, 255)
        assertEquals(204, alphaInt)

        val fullAlphaPercent = 100
        val fullAlphaInt = ((fullAlphaPercent / 100f) * 255).toInt().coerceIn(0, 255)
        assertEquals(255, fullAlphaInt)

        val zeroAlphaPercent = 0
        val zeroAlphaInt = ((zeroAlphaPercent / 100f) * 255).toInt().coerceIn(0, 255)
        assertEquals(0, zeroAlphaInt)
    }

    @Test
    fun testFolderContentIconLayoutPreferences() {
        assertEquals("key_folder_match_desktop_size", FolderSettingsActivity.KEY_FOLDER_MATCH_DESKTOP_SIZE)
        assertEquals("key_folder_icon_size_percent", FolderSettingsActivity.KEY_FOLDER_ICON_SIZE_PERCENT)
        assertEquals("key_folder_label_visible", FolderSettingsActivity.KEY_FOLDER_LABEL_VISIBLE)
        assertEquals("key_folder_label_size_percent", FolderSettingsActivity.KEY_FOLDER_LABEL_SIZE_PERCENT)
        assertEquals("key_folder_label_font", FolderSettingsActivity.KEY_FOLDER_LABEL_FONT)
        assertEquals("key_folder_label_color", FolderSettingsActivity.KEY_FOLDER_LABEL_COLOR)
        assertEquals("key_folder_label_shadow", FolderSettingsActivity.KEY_FOLDER_LABEL_SHADOW)
        assertEquals("key_folder_label_single_line", FolderSettingsActivity.KEY_FOLDER_LABEL_SINGLE_LINE)
    }

    @Test
    fun testFolderMatchDesktopSizeDelegation() {
        val desktopIconSizePercent = 120
        val customFolderIconSizePercent = 85

        // When matchDesktopSize is true -> delegates to desktop icon size
        val matchDesktopSize = true
        val resolvedSizeA = if (matchDesktopSize) desktopIconSizePercent else customFolderIconSizePercent
        assertEquals(120, resolvedSizeA)

        // When matchDesktopSize is false -> uses custom folder icon size
        val matchDesktopSizeFalse = false
        val resolvedSizeB = if (matchDesktopSizeFalse) desktopIconSizePercent else customFolderIconSizePercent
        assertEquals(85, resolvedSizeB)
    }

    @Test
    fun testFolderLabelStylingDefaults() {
        val defaultColor = 0xFFFFFFFF.toInt()
        val defaultShadow = true
        val defaultSingleLine = true
        val defaultFont = "Normal"

        assertEquals(-1, defaultColor) // 0xFFFFFFFF
        assertTrue(defaultShadow)
        assertTrue(defaultSingleLine)
        assertEquals("Normal", defaultFont)
    }

    @Test
    fun testFolderGesturesAndBehaviorPreferences() {
        assertEquals("key_folder_tap_action", FolderSettingsActivity.KEY_FOLDER_TAP_ACTION)
        assertEquals("key_folder_auto_close", FolderSettingsActivity.KEY_FOLDER_AUTO_CLOSE)
        assertEquals("OPEN_FOLDER", FolderSettingsActivity.TAP_ACTION_OPEN_FOLDER)
        assertEquals("FIRST_APP", FolderSettingsActivity.TAP_ACTION_FIRST_APP)
        assertEquals("FIRST_APP", com.example.launcherai.icons.FolderIconRenderer.PREVIEW_FIRST_APP)
    }

    @Test
    fun testFolderTapActionDispatchResolution() {
        var actionExecuted = ""
        val openFolder: () -> Unit = { actionExecuted = "OPEN_FOLDER" }
        val launchFirstApp: () -> Unit = { actionExecuted = "LAUNCH_FIRST_APP" }

        // Test Mode 1: OPEN_FOLDER (Default)
        val modeOpen = FolderSettingsActivity.TAP_ACTION_OPEN_FOLDER
        // Tap -> opens folder
        if (modeOpen == FolderSettingsActivity.TAP_ACTION_FIRST_APP) launchFirstApp() else openFolder()
        assertEquals("OPEN_FOLDER", actionExecuted)

        // Swipe up -> launches first app
        if (modeOpen == FolderSettingsActivity.TAP_ACTION_FIRST_APP) openFolder() else launchFirstApp()
        assertEquals("LAUNCH_FIRST_APP", actionExecuted)

        // Test Mode 2: FIRST_APP (Signature Nova Mode)
        val modeFirstApp = FolderSettingsActivity.TAP_ACTION_FIRST_APP
        // Tap -> launches first app
        if (modeFirstApp == FolderSettingsActivity.TAP_ACTION_FIRST_APP) launchFirstApp() else openFolder()
        assertEquals("LAUNCH_FIRST_APP", actionExecuted)

        // Swipe up -> opens folder
        if (modeFirstApp == FolderSettingsActivity.TAP_ACTION_FIRST_APP) openFolder() else launchFirstApp()
        assertEquals("OPEN_FOLDER", actionExecuted)
    }

    @Test
    fun testFolderAutoCloseCondition() {
        var overlayClosed = false
        val closeOverlay = { overlayClosed = true }

        // Auto-close enabled (default)
        val autoCloseEnabled = true
        if (autoCloseEnabled) closeOverlay()
        assertTrue(overlayClosed)

        // Auto-close disabled
        overlayClosed = false
        val autoCloseDisabled = false
        if (autoCloseDisabled) closeOverlay()
        assertFalse(overlayClosed)
    }

    @Test
    fun testNovaParityKeysAndConstants() {
        assertEquals("key_folder_show_name", FolderSettingsActivity.KEY_FOLDER_SHOW_NAME)
        assertEquals("key_folder_immersive_card_bg", FolderSettingsActivity.KEY_FOLDER_IMMERSIVE_CARD_BG)
        assertEquals("key_folder_immersive_align_bottom", FolderSettingsActivity.KEY_FOLDER_IMMERSIVE_ALIGN_BOTTOM)
        assertEquals("key_folder_window_text_color", FolderSettingsActivity.KEY_FOLDER_WINDOW_TEXT_COLOR)
        assertEquals("key_folder_window_accent_color", FolderSettingsActivity.KEY_FOLDER_WINDOW_ACCENT_COLOR)
        assertEquals("RADIAL_5", FolderIconRenderer.PREVIEW_RADIAL_5)
        assertEquals("RADIAL_9", FolderIconRenderer.PREVIEW_RADIAL_9)
        assertEquals("Flower", FolderIconRenderer.SHAPE_FLOWER)
    }

    @Test
    fun testRadial5AndRadial9SlotGeometryInsidePlateWithoutOverlap() {
        val plateSize = 100f
        val radial5 = FolderGeometry.computeRadialSlots(itemCount = 5, plateSizePx = plateSize, maxRingSlots = 5)
        assertEquals(5, radial5.size)
        radial5.forEach { slot ->
            assertTrue(slot.left >= 0f && slot.top >= 0f)
            assertTrue(slot.right <= plateSize && slot.bottom <= plateSize)
        }

        val radial9 = FolderGeometry.computeRadialSlots(itemCount = 9, plateSizePx = plateSize, maxRingSlots = 9)
        assertEquals(9, radial9.size)
        // First slot acts as centered hub, remaining 8 sit evenly around orbit radius
        assertEquals(plateSize / 2f, radial9[0].centerX, 0.5f)
        assertEquals(plateSize / 2f, radial9[0].centerY, 0.5f)
        for (i in 1 until radial9.size) {
            val slot = radial9[i]
            assertTrue(slot.left >= 0f && slot.top >= 0f)
            assertTrue(slot.right <= plateSize && slot.bottom <= plateSize)
            val distFromCenter = hypot(slot.centerX - plateSize / 2f, slot.centerY - plateSize / 2f)
            assertTrue("Outer icon must sit outside center icon radius", distFromCenter >= slot.size * 0.85f)
        }
    }

    @Test
    fun testModeConditionalGridAutoAndImmersiveControls() {
        val windowState = FolderGeometry.resolveFolderWindowAppearance(
            style = FolderSettingsActivity.WINDOW_STYLE_WINDOW,
            shadowEnabled = true,
            cardBackgroundEnabled = false,
            alignBottomEnabled = true,
            cornerRadiusDp = 16,
            bgAlphaPercent = 80
        )
        assertFalse(windowState.isImmersive)
        assertTrue(windowState.showShadow)
        assertTrue(windowState.isGridAutoLocked)
        assertFalse(windowState.alignBottom)
        assertTrue(windowState.isCornerRadiusEnabled)
        assertEquals(5f, windowState.effectiveElevationDp, 0.01f)
        assertEquals(16f, windowState.effectiveCornerRadiusDp, 0.01f)

        val immersiveNoCard = FolderGeometry.resolveFolderWindowAppearance(
            style = FolderSettingsActivity.WINDOW_STYLE_IMMERSIVE,
            shadowEnabled = true,
            cardBackgroundEnabled = false,
            alignBottomEnabled = true,
            cornerRadiusDp = 20,
            bgAlphaPercent = 80
        )
        assertTrue(immersiveNoCard.isImmersive)
        assertFalse(immersiveNoCard.showShadow)
        assertFalse(immersiveNoCard.isGridAutoLocked)
        assertTrue(immersiveNoCard.alignBottom)
        assertFalse(immersiveNoCard.isCornerRadiusEnabled)
        assertEquals(0f, immersiveNoCard.effectiveCornerRadiusDp, 0.01f)
        assertEquals(0, immersiveNoCard.effectiveBackgroundAlpha)

        val immersiveWithCard = FolderGeometry.resolveFolderWindowAppearance(
            style = FolderSettingsActivity.WINDOW_STYLE_IMMERSIVE,
            shadowEnabled = true,
            cardBackgroundEnabled = true,
            alignBottomEnabled = false,
            cornerRadiusDp = 20,
            bgAlphaPercent = 80
        )
        assertTrue(immersiveWithCard.showCardBackground)
        assertTrue(immersiveWithCard.isCornerRadiusEnabled)
        assertEquals(20f, immersiveWithCard.effectiveCornerRadiusDp, 0.01f)
        assertEquals(80, immersiveWithCard.effectiveBackgroundAlpha)
    }

    @Test
    fun testMatchFolderWindowColorResolution() {
        val matched = FolderGeometry.resolveFolderIconColor(
            matchWindowColor = true,
            windowBgColor = 0xFF1E88E5.toInt(),
            windowBgOpacityPercent = 80,
            customIconBgColor = 0xFFE0E0E0.toInt(),
            customIconBgTransparencyPercent = 40
        )
        assertEquals(0xFF1E88E5.toInt(), matched.color)
        assertEquals(20, matched.transparencyPercent) // 80% opaque == 20% transparency

        val unmatched = FolderGeometry.resolveFolderIconColor(
            matchWindowColor = false,
            windowBgColor = 0xFF1E88E5.toInt(),
            windowBgOpacityPercent = 80,
            customIconBgColor = 0xFF4CAF50.toInt(),
            customIconBgTransparencyPercent = 40
        )
        assertEquals(0xFF4CAF50.toInt(), unmatched.color)
        assertEquals(40, unmatched.transparencyPercent)
    }
}
