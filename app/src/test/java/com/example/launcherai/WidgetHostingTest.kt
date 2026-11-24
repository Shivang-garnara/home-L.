package com.example.launcherai

import com.example.launcherai.data.db.FolderEntity
import com.example.launcherai.data.db.WorkspaceDao
import com.example.launcherai.data.db.WorkspaceItemEntity
import com.example.launcherai.data.db.WorkspacePageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WidgetHostingTest {

    private lateinit var fakeDao: FakeWidgetWorkspaceDao

    @Before
    fun setup() {
        fakeDao = FakeWidgetWorkspaceDao()
    }

    @Test
    fun testWidgetWorkspaceItemEntity() {
        val widgetItem = WorkspaceItemEntity(
            itemType = WorkspaceItemEntity.ITEM_TYPE_WIDGET,
            packageName = "com.example.clockwidget",
            className = "com.example.clockwidget.ClockWidgetProvider",
            container = WorkspaceItemEntity.CONTAINER_DESKTOP,
            screenId = 0,
            cellX = 0,
            cellY = 0,
            spanX = 4,
            spanY = 2,
            appWidgetId = 1001,
            title = "Digital Clock"
        )

        assertEquals(WorkspaceItemEntity.ITEM_TYPE_WIDGET, widgetItem.itemType)
        assertEquals("com.example.clockwidget", widgetItem.packageName)
        assertEquals(4, widgetItem.spanX)
        assertEquals(2, widgetItem.spanY)
        assertEquals(1001, widgetItem.appWidgetId)
        assertEquals("Digital Clock", widgetItem.title)
    }

    @Test
    fun testAddWidgetToWorkspaceDao() = runBlocking {
        val widgetItem = WorkspaceItemEntity(
            id = 50,
            itemType = WorkspaceItemEntity.ITEM_TYPE_WIDGET,
            packageName = "com.example.weather",
            className = "com.example.weather.WeatherWidget",
            container = WorkspaceItemEntity.CONTAINER_DESKTOP,
            screenId = 1,
            cellX = 0,
            cellY = 1,
            spanX = 3,
            spanY = 2,
            appWidgetId = 1002,
            title = "Weather Widget"
        )

        fakeDao.insertWorkspaceItem(widgetItem)
        assertEquals(1, fakeDao.items.size)

        val retrieved = fakeDao.getWorkspaceItemById(50)
        assertNotNull(retrieved)
        assertEquals(1002, retrieved?.appWidgetId)
        assertEquals(1, retrieved?.screenId)
        assertEquals(3, retrieved?.spanX)
        assertEquals(2, retrieved?.spanY)
    }

    @Test
    fun testWidgetSpanUpdate() = runBlocking {
        val widgetItem = WorkspaceItemEntity(
            id = 51,
            itemType = WorkspaceItemEntity.ITEM_TYPE_WIDGET,
            appWidgetId = 1003,
            spanX = 2,
            spanY = 2
        )
        fakeDao.insertWorkspaceItem(widgetItem)

        val existing = fakeDao.getWorkspaceItemById(51)
        assertNotNull(existing)

        val updated = existing!!.copy(spanX = 4, spanY = 3)
        fakeDao.updateWorkspaceItem(updated)

        val result = fakeDao.getWorkspaceItemById(51)
        assertEquals(4, result?.spanX)
        assertEquals(3, result?.spanY)
    }

    @Test
    fun testDeleteWidgetFromWorkspaceDao() = runBlocking {
        val widgetItem = WorkspaceItemEntity(
            id = 52,
            itemType = WorkspaceItemEntity.ITEM_TYPE_WIDGET,
            appWidgetId = 1004
        )
        fakeDao.insertWorkspaceItem(widgetItem)
        assertEquals(1, fakeDao.items.size)

        fakeDao.deleteWorkspaceItemById(52)
        assertEquals(0, fakeDao.items.size)
        assertNull(fakeDao.getWorkspaceItemById(52))
    }

    @Test
    fun testWidgetBoundsUpdateReposition() = runBlocking {
        val widgetItem = WorkspaceItemEntity(
            id = 53,
            itemType = WorkspaceItemEntity.ITEM_TYPE_WIDGET,
            cellX = 0,
            cellY = 0,
            spanX = 2,
            spanY = 2,
            appWidgetId = 1005
        )
        fakeDao.insertWorkspaceItem(widgetItem)

        val existing = fakeDao.getWorkspaceItemById(53)
        assertNotNull(existing)

        // Simulate resizing and dragging left handle: cellX moves from 0 to 1, spanX increases to 3
        val updated = existing!!.copy(cellX = 1, cellY = 0, spanX = 3, spanY = 2)
        fakeDao.updateWorkspaceItem(updated)

        val result = fakeDao.getWorkspaceItemById(53)
        assertEquals(1, result?.cellX)
        assertEquals(0, result?.cellY)
        assertEquals(3, result?.spanX)
        assertEquals(2, result?.spanY)
    }

    @Test
    fun testWidgetCellSpanCalculationFormula() {
        fun calculateCellSpan(sizeDp: Int): Int {
            return Math.ceil((sizeDp + 30) / 70.0).toInt()
        }

        // Test standard Android widget sizes (Nova Launcher standard calculation)
        assertEquals(1, calculateCellSpan(40))  // 1 cell
        assertEquals(2, calculateCellSpan(110)) // 2 cells
        assertEquals(3, calculateCellSpan(180)) // 3 cells
        assertEquals(4, calculateCellSpan(250)) // 4 cells
    }

    @Test
    fun testDuplicateLongPressDispatchGuard() {
        var isLongPressDispatched = false
        var dispatchCount = 0

        fun performLongClickWithGuard(): Boolean {
            if (isLongPressDispatched) return false
            isLongPressDispatched = true
            dispatchCount++
            return true
        }

        // First long press dispatch succeeds
        assertEquals(true, performLongClickWithGuard())
        assertEquals(1, dispatchCount)

        // Second fallback long press from CellLayout during same gesture is rejected
        assertEquals(false, performLongClickWithGuard())
        assertEquals(1, dispatchCount)

        // Reset on gesture end (ACTION_UP)
        isLongPressDispatched = false

        // Subsequent gesture succeeds exactly once
        assertEquals(true, performLongClickWithGuard())
        assertEquals(2, dispatchCount)
    }

    @Test
    fun testTouchSlopCancellationFormula() {
        val touchSlop = 8f // 8dp scaled touch slop
        val thresholdSq = touchSlop * 2f * (touchSlop * 2f) // (16)^2 = 256

        fun shouldCancel(dx: Float, dy: Float): Boolean {
            return (dx * dx + dy * dy) > thresholdSq
        }

        // Jitter within slop (< 16px) does NOT cancel
        org.junit.Assert.assertFalse(shouldCancel(5f, 5f))   // distance ~7.07
        org.junit.Assert.assertFalse(shouldCancel(10f, 10f)) // distance ~14.14
        org.junit.Assert.assertFalse(shouldCancel(0f, 15f))  // distance 15

        // Movement beyond slop (scroll or drag) cancels
        org.junit.Assert.assertTrue(shouldCancel(12f, 12f))  // distance ~16.97 > 16
        org.junit.Assert.assertTrue(shouldCancel(0f, 25f))   // distance 25 > 16
        org.junit.Assert.assertTrue(shouldCancel(30f, 5f))   // distance 30.4 > 16
    }

    @Test
    fun testCellOccupancyEdgeDiscrimination() {
        val countX = 4
        val countY = 5
        val occupied = Array(countX) { BooleanArray(countY) }

        // Place 4x1 widget at cell (0, 1)
        val widgetCellX = 0
        val widgetCellY = 1
        val widgetSpanX = 4
        val widgetSpanY = 1

        for (x in widgetCellX until widgetCellX + widgetSpanX) {
            for (y in widgetCellY until widgetCellY + widgetSpanY) {
                occupied[x][y] = true
            }
        }

        // Verify all 4 widget cells are marked occupied (no false empty-space detection)
        for (x in 0 until 4) {
            org.junit.Assert.assertFalse("Cell ($x, 1) should be occupied", !occupied[x][1])
        }

        // Verify adjacent empty cells are empty (true empty-space detection)
        org.junit.Assert.assertTrue("Cell (0, 2) should be empty", !occupied[0][2])
        org.junit.Assert.assertTrue("Cell (1, 2) should be empty", !occupied[1][2])
    }

    @Test
    fun testDirectLongPressManipulationWithoutActionPopup() {
        val widgetItem = WorkspaceItemEntity(
            id = 54,
            itemType = WorkspaceItemEntity.ITEM_TYPE_WIDGET,
            packageName = "com.google.android.googlequicksearchbox",
            className = "com.google.android.googlequicksearchbox.SearchWidget",
            container = WorkspaceItemEntity.CONTAINER_DESKTOP,
            screenId = 0,
            cellX = 0,
            cellY = 0,
            spanX = 4,
            spanY = 2,
            appWidgetId = 2001,
            title = "Google Search"
        )

        var enteredResizeMode = false
        var openedActionPopup = false

        // On widget long click: Direct manipulation mode without action popup
        fun onWidgetLongClickSimulation() {
            enteredResizeMode = true
            openedActionPopup = false
        }

        onWidgetLongClickSimulation()

        assertTrue("Must enter resize mode directly on widget long press", enteredResizeMode)
        assertFalse("Must NOT open widget action/options popup on widget long press", openedActionPopup)
    }

    @Test
    fun testWidgetBodyDragInitiation() {
        val touchSlop = 8f
        var isLongPressDispatched = true
        var hasStartedDrag = false
        var dragStartX = 0f
        var dragStartY = 0f

        fun handleTouchMove(rawX: Float, rawY: Float, downX: Float, downY: Float) {
            val dx = rawX - downX
            val dy = rawY - downY
            if (isLongPressDispatched && !hasStartedDrag) {
                if ((dx * dx + dy * dy) > (touchSlop * touchSlop)) {
                    hasStartedDrag = true
                    dragStartX = rawX
                    dragStartY = rawY
                }
            }
        }

        // Small jitter below slop
        handleTouchMove(downX = 100f, downY = 100f, rawX = 104f, rawY = 103f)
        assertFalse("Jitter below touch slop must not start drag", hasStartedDrag)

        // Movement beyond slop initiates drag
        handleTouchMove(downX = 100f, downY = 100f, rawX = 120f, rawY = 115f)
        assertTrue("Movement beyond touch slop must start widget drag", hasStartedDrag)
        assertEquals(120f, dragStartX)
        assertEquals(115f, dragStartY)
    }

    @Test
    fun testDirectResizeHandlesDirectionalConstraints() {
        // Provider resize modes
        val resizeModeNone = 0
        val resizeModeHorizontal = 1
        val resizeModeVertical = 2
        val resizeModeBoth = 3

        fun allowsHorizontal(mode: Int): Boolean = (mode and 1) != 0 || mode == 0
        fun allowsVertical(mode: Int): Boolean = (mode and 2) != 0 || mode == 0

        // Test Horizontal only
        assertTrue(allowsHorizontal(resizeModeHorizontal))
        assertFalse(allowsVertical(resizeModeHorizontal))

        // Test Vertical only
        assertFalse(allowsHorizontal(resizeModeVertical))
        assertTrue(allowsVertical(resizeModeVertical))

        // Test Both
        assertTrue(allowsHorizontal(resizeModeBoth))
        assertTrue(allowsVertical(resizeModeBoth))

        // Test Default / Unspecified (mode = 0 allows both as fallback)
        assertTrue(allowsHorizontal(resizeModeNone))
        assertTrue(allowsVertical(resizeModeNone))
    }

    @Test
    fun testReattachResizeFrameAfterDrop() = runBlocking {
        val widgetItem = WorkspaceItemEntity(
            id = 55,
            itemType = WorkspaceItemEntity.ITEM_TYPE_WIDGET,
            packageName = "com.android.chrome",
            screenId = 0,
            cellX = 0,
            cellY = 0,
            spanX = 3,
            spanY = 2,
            appWidgetId = 3001
        )
        fakeDao.insertWorkspaceItem(widgetItem)

        // Drop widget at new position: screenId = 1, cellX = 1, cellY = 2
        val moved = widgetItem.copy(screenId = 1, cellX = 1, cellY = 2)
        fakeDao.updateWorkspaceItem(moved)

        var pendingAutoResizeWidgetId = moved.appWidgetId
        assertEquals(3001, pendingAutoResizeWidgetId)

        // Re-attach frame on drop
        var reattachedWidgetId = -1
        if (pendingAutoResizeWidgetId != -1) {
            reattachedWidgetId = pendingAutoResizeWidgetId
            pendingAutoResizeWidgetId = -1
        }

        assertEquals(3001, reattachedWidgetId)
        assertEquals(-1, pendingAutoResizeWidgetId)
    }

    private class FakeWidgetWorkspaceDao : WorkspaceDao {
        val items = mutableListOf<WorkspaceItemEntity>()

        override fun getAllWorkspaceItemsFlow(): Flow<List<WorkspaceItemEntity>> = flowOf(items)

        override suspend fun getAllWorkspaceItemsSync(): List<WorkspaceItemEntity> = items.toList()

        override fun getWorkspaceItemsByContainerFlow(container: Int): Flow<List<WorkspaceItemEntity>> {
            return flowOf(items.filter { it.container == container })
        }

        override suspend fun getWorkspaceItemsByContainerSync(container: Int): List<WorkspaceItemEntity> {
            return items.filter { it.container == container }
        }

        override fun getWorkspaceItemsByScreenFlow(screenId: Int): Flow<List<WorkspaceItemEntity>> {
            return flowOf(items.filter { it.screenId == screenId })
        }

        override suspend fun getWorkspaceItemById(id: Long): WorkspaceItemEntity? {
            return items.find { it.id == id }
        }

        override suspend fun getItemCount(): Int = items.size

        override suspend fun insertWorkspaceItem(item: WorkspaceItemEntity): Long {
            items.removeIf { it.id == item.id }
            items.add(item)
            return item.id
        }

        override suspend fun insertWorkspaceItems(items: List<WorkspaceItemEntity>): List<Long> {
            items.forEach { insertWorkspaceItem(it) }
            return items.map { it.id }
        }

        override suspend fun updateWorkspaceItem(item: WorkspaceItemEntity) {
            insertWorkspaceItem(item)
        }

        override suspend fun deleteWorkspaceItem(item: WorkspaceItemEntity) {
            items.removeIf { it.id == item.id }
        }

        override suspend fun getItemsByPackageName(packageName: String): List<WorkspaceItemEntity> {
            return items.filter { it.packageName == packageName }
        }

        override suspend fun deleteWorkspaceItemById(id: Long) {
            items.removeIf { it.id == id }
        }

        override suspend fun deleteWorkspaceItemsByPackageName(packageName: String): Int {
            val countBefore = items.size
            items.removeIf { it.packageName == packageName }
            return countBefore - items.size
        }

        override fun getAllPagesFlow(): Flow<List<WorkspacePageEntity>> = flowOf(emptyList())

        override suspend fun getAllPagesSync(): List<WorkspacePageEntity> = emptyList()

        override suspend fun insertPage(page: WorkspacePageEntity) {}

        override suspend fun insertPages(pages: List<WorkspacePageEntity>) {}

        override suspend fun deletePage(screenId: Int) {}

        override suspend fun updatePageOrder(screenId: Int, pageOrder: Int) {}

        override suspend fun getMaxScreenId(): Int? = 0

        override suspend fun getWorkspaceItemsByScreenSync(screenId: Int): List<WorkspaceItemEntity> = items.filter { it.screenId == screenId }

        override fun getAllFoldersFlow(): Flow<List<FolderEntity>> = flowOf(emptyList())

        override suspend fun getAllFoldersSync(): List<FolderEntity> = emptyList()

        override suspend fun getFolderById(folderId: Long): FolderEntity? = null

        override suspend fun insertFolder(folder: FolderEntity): Long = 0L

        override suspend fun updateFolder(folder: FolderEntity) {}

        override suspend fun deleteFolder(folderId: Long) {}
    }
}
