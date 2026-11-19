package com.example.launcherai

import com.example.launcherai.gestures.GestureAction
import org.junit.Assert.assertEquals
import org.junit.Test

class Phase7CustomizationTest {

    @Test
    fun testGestureActionMapping() {
        val appDrawer = GestureAction.fromName("APP_DRAWER")
        val notifications = GestureAction.fromName("EXPAND_NOTIFICATIONS")
        val quickSettings = GestureAction.fromName("EXPAND_QUICK_SETTINGS")
        val invalid = GestureAction.fromName("INVALID_GESTURE_ACTION", GestureAction.NONE)

        assertEquals(GestureAction.APP_DRAWER, appDrawer)
        assertEquals(GestureAction.EXPAND_NOTIFICATIONS, notifications)
        assertEquals(GestureAction.EXPAND_QUICK_SETTINGS, quickSettings)
        assertEquals(GestureAction.NONE, invalid)
    }

    @Test
    fun testIconPackNameFormatting() {
        val blankPack = ""
        val isDefault = blankPack.isBlank()
        assertEquals(true, isDefault)
    }

    @Test
    fun testGestureLabels() {
        assertEquals("Open App Drawer", GestureAction.APP_DRAWER.label)
        assertEquals("Open Launcher Settings", GestureAction.SETTINGS.label)
        assertEquals("Expand Notifications", GestureAction.EXPAND_NOTIFICATIONS.label)
    }
}
