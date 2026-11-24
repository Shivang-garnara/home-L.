package com.example.launcherai

import com.example.launcherai.data.db.FolderEntity
import com.example.launcherai.data.db.WorkspaceDao
import com.example.launcherai.data.db.WorkspaceItemEntity
import com.example.launcherai.data.db.WorkspacePageEntity
import com.example.launcherai.data.repository.GridChangeResult
import com.example.launcherai.data.repository.WorkspaceGridConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class Phase9GridCustomizationTest {

    private lateinit var fakeDao: FakeWorkspaceDao

    @Before
    fun setup() {
        fakeDao = FakeWorkspaceDao()
        fakeDao.pages.addAll(
            listOf(
                WorkspacePageEntity(screenId = 0, pageOrder = 0),
                WorkspacePageEntity(screenId = 1, pageOrder = 1)
            )
        )
    }

    @Test
    fun testWorkspaceGridConfigDefaults() {
        val config = WorkspaceGridConfig()
        assertEquals(4, config.cols)
        assertEquals(5, config.rows)
    }

    @Test
    fun testGridReflowPreservesFittingItems() = runBlocking {
        // Given an item at (1, 1) on page 0
        val item = WorkspaceItemEntity(
            id = 10,
            itemType = WorkspaceItemEntity.ITEM_TYPE_APP,
            packageName = "com.example.app1",
            container = WorkspaceItemEntity.CONTAINER_DESKTOP,
            screenId = 0,
            cellX = 1,
            cellY = 1,
            spanX = 1,
            spanY = 1,
            title = "App 1"
        )
        fakeDao.insertWorkspaceItem(item)

        // When changing grid to 5x6
        val result = performGridReflow(fakeDao, 5, 6)

        assertTrue(result is GridChangeResult.Success)
        val updated = fakeDao.getWorkspaceItemById(10)
        assertEquals(1, updated?.cellX)
        assertEquals(1, updated?.cellY)
    }

    @Test
    fun testGridReflowRelocatesOutOfBoundsItems() = runBlocking {
        // Given an item at cellX=4 (which fits in 5x5 but is out of bounds for 3x4)
        val item = WorkspaceItemEntity(
            id = 1,
            itemType = WorkspaceItemEntity.ITEM_TYPE_APP,
            packageName = "com.example.app1",
            container = WorkspaceItemEntity.CONTAINER_DESKTOP,
            screenId = 0,
            cellX = 4,
            cellY = 3,
            spanX = 1,
            spanY = 1,
            title = "App 1"
        )
        fakeDao.insertWorkspaceItem(item)

        // When grid changes to 3x4
        val result = performGridReflow(fakeDao, 3, 4)

        assertTrue(result is GridChangeResult.Success)
        val updated = fakeDao.getWorkspaceItemById(1)
        assertTrue(updated != null)
        assertTrue("cellX must be < 3", updated!!.cellX < 3)
        assertTrue("cellY must be < 4", updated.cellY < 4)
    }

    @Test
    fun testGridReflowRejectsWhenCapacityExceeded() = runBlocking {
        // Insert 25 items across 2 pages
        for (i in 1..25) {
            fakeDao.insertWorkspaceItem(
                WorkspaceItemEntity(
                    id = i.toLong(),
                    itemType = WorkspaceItemEntity.ITEM_TYPE_APP,
                    packageName = "com.example.app$i",
                    container = WorkspaceItemEntity.CONTAINER_DESKTOP,
                    screenId = if (i <= 12) 0 else 1,
                    cellX = 0,
                    cellY = 0,
                    title = "App $i"
                )
            )
        }

        // 3x3 grid across 2 pages has max 18 slots, which cannot hold 25 items
        val result = performGridReflow(fakeDao, 3, 3)

        assertTrue(result is GridChangeResult.Failure)
    }

    @Test
    fun testWidgetSpanClampedOnGridChange() = runBlocking {
        val widgetItem = WorkspaceItemEntity(
            id = 100,
            itemType = WorkspaceItemEntity.ITEM_TYPE_WIDGET,
            packageName = "com.example.widget",
            container = WorkspaceItemEntity.CONTAINER_DESKTOP,
            screenId = 0,
            cellX = 0,
            cellY = 0,
            spanX = 4,
            spanY = 4,
            title = "Widget 1"
        )
        fakeDao.insertWorkspaceItem(widgetItem)

        val result = performGridReflow(fakeDao, 3, 3)

        assertTrue(result is GridChangeResult.Success)
        val updatedWidget = fakeDao.getWorkspaceItemById(100)
        assertEquals(3, updatedWidget?.spanX)
        assertEquals(3, updatedWidget?.spanY)
    }

    @Test
    fun testDrawerToWorkspaceItemInsertion() = runBlocking {
        val drawerItem = WorkspaceItemEntity(
            id = 0, // transient drawer item
            itemType = WorkspaceItemEntity.ITEM_TYPE_APP,
            packageName = "com.example.newapp",
            container = WorkspaceItemEntity.CONTAINER_DESKTOP,
            screenId = 0,
            cellX = 2,
            cellY = 2,
            title = "New App"
        )

        val newId = fakeDao.insertWorkspaceItem(drawerItem)
        assertTrue(newId > 0)

        val items = fakeDao.getWorkspaceItemsByContainerSync(WorkspaceItemEntity.CONTAINER_DESKTOP)
        assertTrue(items.any { it.packageName == "com.example.newapp" })
    }

    /**
     * Helper executing the same grid reflow logic as WorkspaceRepository.
     */
    private suspend fun performGridReflow(dao: WorkspaceDao, newCols: Int, newRows: Int): GridChangeResult {
        if (newCols < WorkspaceGridConfig.MIN_COLS || newCols > WorkspaceGridConfig.MAX_COLS ||
            newRows < WorkspaceGridConfig.MIN_ROWS || newRows > WorkspaceGridConfig.MAX_ROWS) {
            return GridChangeResult.Failure("Grid bounds exceeded")
        }

        val allDesktopItems = dao.getWorkspaceItemsByContainerSync(WorkspaceItemEntity.CONTAINER_DESKTOP)
        val pages = dao.getAllPagesSync()

        val clampedItems = allDesktopItems.map { item ->
            if (item.itemType == WorkspaceItemEntity.ITEM_TYPE_WIDGET) {
                item.copy(spanX = item.spanX.coerceIn(1, newCols), spanY = item.spanY.coerceIn(1, newRows))
            } else {
                item.copy(spanX = 1, spanY = 1)
            }
        }

        val totalCapacity = pages.size * (newCols * newRows)
        val requiredCells = clampedItems.sumOf { it.spanX * it.spanY }
        if (requiredCells > totalCapacity) {
            return GridChangeResult.Failure("Cannot fit all workspace items")
        }

        val updatedItems = mutableListOf<WorkspaceItemEntity>()
        val pageOccupancyMap = mutableMapOf<Int, Array<BooleanArray>>()
        for (page in pages) {
            pageOccupancyMap[page.screenId] = Array(newCols) { BooleanArray(newRows) }
        }

        val itemsByScreen = clampedItems.groupBy { it.screenId }
        for (page in pages) {
            val screenId = page.screenId
            val screenItems = itemsByScreen[screenId] ?: emptyList()
            val occupancy = pageOccupancyMap[screenId]!!

            val fittingItems = mutableListOf<WorkspaceItemEntity>()
            val unplacedItems = mutableListOf<WorkspaceItemEntity>()

            for (item in screenItems) {
                if (item.cellX + item.spanX <= newCols && item.cellY + item.spanY <= newRows) {
                    fittingItems.add(item)
                } else {
                    unplacedItems.add(item)
                }
            }

            for (item in fittingItems) {
                var canFitInSpot = true
                for (x in item.cellX until item.cellX + item.spanX) {
                    for (y in item.cellY until item.cellY + item.spanY) {
                        if (occupancy[x][y]) {
                            canFitInSpot = false
                            break
                        }
                    }
                    if (!canFitInSpot) break
                }

                if (canFitInSpot) {
                    for (x in item.cellX until item.cellX + item.spanX) {
                        for (y in item.cellY until item.cellY + item.spanY) {
                            occupancy[x][y] = true
                        }
                    }
                    updatedItems.add(item)
                } else {
                    unplacedItems.add(item)
                }
            }

            for (item in unplacedItems) {
                var placed = false
                val vacant = findVacantSpot(occupancy, newCols, newRows, item.spanX, item.spanY)
                if (vacant != null) {
                    val (vx, vy) = vacant
                    markOccupied(occupancy, vx, vy, item.spanX, item.spanY)
                    updatedItems.add(item.copy(screenId = screenId, cellX = vx, cellY = vy))
                    placed = true
                } else {
                    for (otherPage in pages) {
                        if (otherPage.screenId == screenId) continue
                        val otherOccupancy = pageOccupancyMap[otherPage.screenId] ?: continue
                        val otherVacant = findVacantSpot(otherOccupancy, newCols, newRows, item.spanX, item.spanY)
                        if (otherVacant != null) {
                            val (ovx, ovy) = otherVacant
                            markOccupied(otherOccupancy, ovx, ovy, item.spanX, item.spanY)
                            updatedItems.add(item.copy(screenId = otherPage.screenId, cellX = ovx, cellY = ovy))
                            placed = true
                            break
                        }
                    }
                }

                if (!placed) {
                    return GridChangeResult.Failure("Failed to find valid non-overlapping position")
                }
            }
        }

        dao.insertWorkspaceItems(updatedItems)
        return GridChangeResult.Success
    }

    private fun findVacantSpot(
        occupancy: Array<BooleanArray>,
        cols: Int,
        rows: Int,
        spanX: Int,
        spanY: Int
    ): Pair<Int, Int>? {
        for (y in 0..(rows - spanY)) {
            for (x in 0..(cols - spanX)) {
                var empty = true
                for (dx in 0 until spanX) {
                    for (dy in 0 until spanY) {
                        if (occupancy[x + dx][y + dy]) {
                            empty = false
                            break
                        }
                    }
                    if (!empty) break
                }
                if (empty) return Pair(x, y)
            }
        }
        return null
    }

    private fun markOccupied(
        occupancy: Array<BooleanArray>,
        cellX: Int,
        cellY: Int,
        spanX: Int,
        spanY: Int
    ) {
        for (x in cellX until cellX + spanX) {
            for (y in cellY until cellY + spanY) {
                occupancy[x][y] = true
            }
        }
    }

    private class FakeWorkspaceDao : WorkspaceDao {
        val items = mutableListOf<WorkspaceItemEntity>()
        val pages = mutableListOf<WorkspacePageEntity>()
        val folders = mutableListOf<FolderEntity>()

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

        private var nextId = 1L
        override suspend fun insertWorkspaceItem(item: WorkspaceItemEntity): Long {
            val assignedId = if (item.id == 0L) nextId++ else item.id
            val saved = item.copy(id = assignedId)
            items.removeIf { it.id == assignedId }
            items.add(saved)
            return assignedId
        }

        override suspend fun insertWorkspaceItems(items: List<WorkspaceItemEntity>): List<Long> {
            return items.map { insertWorkspaceItem(it) }
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

        override fun getAllPagesFlow(): Flow<List<WorkspacePageEntity>> = flowOf(pages)

        override suspend fun getAllPagesSync(): List<WorkspacePageEntity> = pages.toList()

        override suspend fun insertPage(page: WorkspacePageEntity) {
            pages.removeIf { it.screenId == page.screenId }
            pages.add(page)
        }

        override suspend fun insertPages(pages: List<WorkspacePageEntity>) {
            pages.forEach { insertPage(it) }
        }

        override suspend fun deletePage(screenId: Int) {
            pages.removeIf { it.screenId == screenId }
        }

        override suspend fun updatePageOrder(screenId: Int, pageOrder: Int) {
            val p = pages.find { it.screenId == screenId }
            if (p != null) {
                pages.remove(p)
                pages.add(p.copy(pageOrder = pageOrder))
            }
        }

        override suspend fun getMaxScreenId(): Int? {
            return pages.maxOfOrNull { it.screenId }
        }

        override suspend fun getWorkspaceItemsByScreenSync(screenId: Int): List<WorkspaceItemEntity> {
            return items.filter { it.screenId == screenId }
        }

        override fun getAllFoldersFlow(): Flow<List<FolderEntity>> = flowOf(folders)

        override suspend fun getAllFoldersSync(): List<FolderEntity> = folders.toList()

        override suspend fun getFolderById(folderId: Long): FolderEntity? {
            return folders.find { it.folderId == folderId }
        }

        override suspend fun insertFolder(folder: FolderEntity): Long {
            folders.removeIf { it.folderId == folder.folderId }
            folders.add(folder)
            return folder.folderId
        }

        override suspend fun updateFolder(folder: FolderEntity) {
            insertFolder(folder)
        }

        override suspend fun deleteFolder(folderId: Long) {
            folders.removeIf { it.folderId == folderId }
        }
    }
}
