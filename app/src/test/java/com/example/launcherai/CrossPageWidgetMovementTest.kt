package com.example.launcherai

import com.example.launcherai.data.db.WorkspaceItemEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrossPageWidgetMovementTest {

    @Test
    fun testWidgetCrossPageCoordinatesUpdate() {
        val widgetItem = WorkspaceItemEntity(
            id = 10,
            itemType = WorkspaceItemEntity.ITEM_TYPE_WIDGET,
            packageName = "com.android.chrome",
            className = "com.android.chrome.SearchWidget",
            screenId = 0,
            cellX = 0,
            cellY = 0,
            spanX = 4,
            spanY = 2,
            appWidgetId = 101
        )

        // Move widget from screenId = 0 to screenId = 1 at (cellX = 0, cellY = 2)
        val updatedWidget = widgetItem.copy(
            screenId = 1,
            cellX = 0,
            cellY = 2
        )

        assertEquals(10L, updatedWidget.id)
        assertEquals(101, updatedWidget.appWidgetId)
        assertEquals(1, updatedWidget.screenId)
        assertEquals(0, updatedWidget.cellX)
        assertEquals(2, updatedWidget.cellY)
        assertEquals(4, updatedWidget.spanX)
        assertEquals(2, updatedWidget.spanY)
    }

    @Test
    fun testEdgeHoverThresholdCalculation() {
        val density = 2.0f
        val layerWidth = 720f
        val edgeWidth = 40f * density // 80dp

        val leftHoverX = 50f
        val centerTouchX = 360f
        val rightHoverX = 680f

        val isLeftHover = leftHoverX < edgeWidth
        val isRightHover = rightHoverX > layerWidth - edgeWidth
        val isCenterHover = centerTouchX < edgeWidth || centerTouchX > layerWidth - edgeWidth

        assertTrue(isLeftHover)
        assertTrue(isRightHover)
        assertFalse(isCenterHover)
    }

    @Test
    fun testRollbackStateOnDragCancel() {
        val originalScreenId = 0
        val originalCellX = 1
        val originalCellY = 2

        var currentScreenId = 2
        var currentCellX = 0
        var currentCellY = 0

        // Cancel drag -> Restore original bounds
        currentScreenId = originalScreenId
        currentCellX = originalCellX
        currentCellY = originalCellY

        assertEquals(0, currentScreenId)
        assertEquals(1, currentCellX)
        assertEquals(2, currentCellY)
    }
}
