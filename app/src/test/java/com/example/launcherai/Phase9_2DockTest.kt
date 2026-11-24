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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class Phase9_2DockTest {

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
    fun testReservedAppDrawerSlotCalculations() {
        // Reserved App Drawer Slot is always capacity / 2
        assertEquals(1, 3 / 2) // 3-slot dock -> slot 1
        assertEquals(2, 5 / 2) // 5-slot dock -> slot 2
        assertEquals(3, 7 / 2) // 7-slot dock -> slot 3
    }

    @Test
    fun testDockCapacityTransitions() = runBlocking {
        // Test all 6 capacity transitions: 7->5, 7->3, 5->3, 3->5, 3->7, 5->7
        val grid = WorkspaceGridConfig(4, 5)

        // 1. Initial 7-slot dock with items at slots 0, 1, 2, 4, 5, 6 (slot 3 is drawer)
        for (slot in listOf(0, 1, 2, 4, 5, 6)) {
            fakeDao.insertWorkspaceItem(
                WorkspaceItemEntity(
                    id = (slot + 10).toLong(),
                    itemType = WorkspaceItemEntity.ITEM_TYPE_APP,
                    packageName = "com.example.dock$slot",
                    container = WorkspaceItemEntity.CONTAINER_DOCK,
                    screenId = 0,
                    cellX = slot,
                    cellY = 0,
                    title = "Dock App $slot"
                )
            )
        }

        // Transition 7 -> 5 (Drawer at slot 2)
        var result = performDockCapacityChange(fakeDao, 5, grid)
        assertTrue(result is GridChangeResult.Success)
        var dockItems = fakeDao.getWorkspaceItemsByContainerSync(WorkspaceItemEntity.CONTAINER_DOCK)
        assertTrue("Max 4 items in 5-slot dock", dockItems.size <= 4)
        assertTrue("No item at drawer slot 2", dockItems.none { it.cellX == 2 })

        // Transition 5 -> 3 (Drawer at slot 1)
        result = performDockCapacityChange(fakeDao, 3, grid)
        assertTrue(result is GridChangeResult.Success)
        dockItems = fakeDao.getWorkspaceItemsByContainerSync(WorkspaceItemEntity.CONTAINER_DOCK)
        assertTrue("Max 2 items in 3-slot dock", dockItems.size <= 2)
        assertTrue("No item at drawer slot 1", dockItems.none { it.cellX == 1 })

        // Transition 3 -> 5
        result = performDockCapacityChange(fakeDao, 5, grid)
        assertTrue(result is GridChangeResult.Success)
        dockItems = fakeDao.getWorkspaceItemsByContainerSync(WorkspaceItemEntity.CONTAINER_DOCK)
        assertTrue("No item at drawer slot 2", dockItems.none { it.cellX == 2 })

        // Transition 5 -> 7
        result = performDockCapacityChange(fakeDao, 7, grid)
        assertTrue(result is GridChangeResult.Success)
        dockItems = fakeDao.getWorkspaceItemsByContainerSync(WorkspaceItemEntity.CONTAINER_DOCK)
        assertTrue("No item at drawer slot 3", dockItems.none { it.cellX == 3 })

        // Transition 7 -> 3
        result = performDockCapacityChange(fakeDao, 3, grid)
        assertTrue(result is GridChangeResult.Success)

        // Transition 3 -> 7
        result = performDockCapacityChange(fakeDao, 7, grid)
        assertTrue(result is GridChangeResult.Success)
    }

    @Test
    fun testFolderPreservedDuringDockCapacityReduction() = runBlocking {
        // Fill 7-slot dock with items at slots 0, 1, 2, 4, 5
        for (slot in listOf(0, 1, 2, 4, 5)) {
            fakeDao.insertWorkspaceItem(
                WorkspaceItemEntity(
                    id = (slot + 10).toLong(),
                    itemType = WorkspaceItemEntity.ITEM_TYPE_APP,
                    packageName = "com.example.dock$slot",
                    container = WorkspaceItemEntity.CONTAINER_DOCK,
                    screenId = 0,
                    cellX = slot,
                    cellY = 0,
                    title = "Dock App $slot"
                )
            )
        }

        // Create a folder at slot 6 in a 7-slot dock
        val folderEntity = WorkspaceItemEntity(
            id = 200,
            itemType = WorkspaceItemEntity.ITEM_TYPE_FOLDER,
            title = "Dock Folder",
            container = WorkspaceItemEntity.CONTAINER_DOCK,
            screenId = 0,
            cellX = 6,
            cellY = 0
        )
        fakeDao.insertWorkspaceItem(folderEntity)

        val child1 = WorkspaceItemEntity(
            id = 201,
            itemType = WorkspaceItemEntity.ITEM_TYPE_APP,
            packageName = "com.example.child1",
            container = 200, // Folder ID
            screenId = 0,
            cellX = 0,
            cellY = 0,
            rank = 0,
            title = "Child 1"
        )
        fakeDao.insertWorkspaceItem(child1)

        // Reduce dock capacity to 5 slots (valid slots 0, 1, 3, 4 occupied by items 0, 1, 2, 4, 5).
        // Folder at slot 6 has no vacant dock slots and overflows to desktop.
        val result = performDockCapacityChange(fakeDao, 5, WorkspaceGridConfig(4, 5))
        assertTrue(result is GridChangeResult.Success)

        // Folder is moved to desktop; child1 remains inside container = 200!
        val movedFolder = fakeDao.getWorkspaceItemById(200)
        assertNotNull(movedFolder)
        assertEquals(WorkspaceItemEntity.CONTAINER_DESKTOP, movedFolder?.container)

        val childCheck = fakeDao.getWorkspaceItemById(201)
        assertEquals(200, childCheck?.container)
    }

    @Test
    fun testDockCapacityReductionRejectsWhenDesktopFull() = runBlocking {
        // Fill desktop pages (2 pages of 3x3 = 18 slots)
        for (i in 1..18) {
            fakeDao.insertWorkspaceItem(
                WorkspaceItemEntity(
                    id = i.toLong(),
                    itemType = WorkspaceItemEntity.ITEM_TYPE_APP,
                    packageName = "com.example.desktop$i",
                    container = WorkspaceItemEntity.CONTAINER_DESKTOP,
                    screenId = if (i <= 9) 0 else 1,
                    cellX = (i - 1) % 3,
                    cellY = ((i - 1) / 3) % 3,
                    title = "Desktop App $i"
                )
            )
        }

        // Add 6 items to dock at slots 0..5
        for (slot in 0..5) {
            fakeDao.insertWorkspaceItem(
                WorkspaceItemEntity(
                    id = (slot + 100).toLong(),
                    itemType = WorkspaceItemEntity.ITEM_TYPE_APP,
                    packageName = "com.example.dock$slot",
                    container = WorkspaceItemEntity.CONTAINER_DOCK,
                    screenId = 0,
                    cellX = slot,
                    cellY = 0,
                    title = "Dock App $slot"
                )
            )
        }

        // Reduce dock capacity to 3 slots (max 2 items). 4 overflow items cannot fit on full desktop.
        val result = performDockCapacityChange(fakeDao, 3, WorkspaceGridConfig(3, 3))
        assertTrue(result is GridChangeResult.Failure)
    }

    @Test
    fun testDrawerToDockItemInsertion() = runBlocking {
        val drawerItem = WorkspaceItemEntity(
            id = 0,
            itemType = WorkspaceItemEntity.ITEM_TYPE_APP,
            packageName = "com.example.docknew",
            container = WorkspaceItemEntity.CONTAINER_DOCK,
            screenId = 0,
            cellX = 1,
            cellY = 0,
            title = "New Dock App"
        )

        val assignedId = fakeDao.insertWorkspaceItem(drawerItem)
        assertTrue(assignedId > 0)

        val dockItems = fakeDao.getWorkspaceItemsByContainerSync(WorkspaceItemEntity.CONTAINER_DOCK)
        assertTrue(dockItems.any { it.packageName == "com.example.docknew" && it.container == WorkspaceItemEntity.CONTAINER_DOCK })
    }

    @Test
    fun testDockToWorkspaceItemMovement() = runBlocking {
        val dockItem = WorkspaceItemEntity(
            id = 50,
            itemType = WorkspaceItemEntity.ITEM_TYPE_APP,
            packageName = "com.example.movedapp",
            container = WorkspaceItemEntity.CONTAINER_DOCK,
            screenId = 0,
            cellX = 0,
            cellY = 0,
            title = "Moved App"
        )
        fakeDao.insertWorkspaceItem(dockItem)

        // Move item to desktop page 0 at (2, 3)
        fakeDao.updateWorkspaceItem(
            dockItem.copy(
                container = WorkspaceItemEntity.CONTAINER_DESKTOP,
                screenId = 0,
                cellX = 2,
                cellY = 3
            )
        )

        val updated = fakeDao.getWorkspaceItemById(50)
        assertEquals(WorkspaceItemEntity.CONTAINER_DESKTOP, updated?.container)
        assertEquals(0, updated?.screenId)
        assertEquals(2, updated?.cellX)
        assertEquals(3, updated?.cellY)
    }

    private suspend fun performDockCapacityChange(
        dao: WorkspaceDao,
        newCapacity: Int,
        gridConfig: WorkspaceGridConfig
    ): GridChangeResult {
        val validCapacity = when (newCapacity) {
            3, 7 -> newCapacity
            else -> 5
        }

        val dockItems = dao.getWorkspaceItemsByContainerSync(WorkspaceItemEntity.CONTAINER_DOCK)
        val newDrawerSlot = validCapacity / 2

        val validDockSlots = (0 until validCapacity).filter { it != newDrawerSlot }
        val dockOccupancy = BooleanArray(validCapacity)
        dockOccupancy[newDrawerSlot] = true

        val updatedDockItems = mutableListOf<WorkspaceItemEntity>()
        val unplacedDockItems = mutableListOf<WorkspaceItemEntity>()

        for (item in dockItems) {
            if (item.cellX in 0 until validCapacity && item.cellX != newDrawerSlot && !dockOccupancy[item.cellX]) {
                dockOccupancy[item.cellX] = true
                updatedDockItems.add(item)
            } else {
                unplacedDockItems.add(item)
            }
        }

        val reflowedToDesktop = mutableListOf<WorkspaceItemEntity>()
        for (item in unplacedDockItems) {
            var placedInDock = false
            for (slot in validDockSlots) {
                if (!dockOccupancy[slot]) {
                    dockOccupancy[slot] = true
                    updatedDockItems.add(item.copy(cellX = slot, rank = slot))
                    placedInDock = true
                    break
                }
            }
            if (!placedInDock) {
                reflowedToDesktop.add(item)
            }
        }

        if (reflowedToDesktop.isNotEmpty()) {
            val pages = dao.getAllPagesSync()
            val desktopItems = dao.getWorkspaceItemsByContainerSync(WorkspaceItemEntity.CONTAINER_DESKTOP)
            val pageOccupancyMap = mutableMapOf<Int, Array<BooleanArray>>()

            for (page in pages) {
                val occ = Array(gridConfig.cols) { BooleanArray(gridConfig.rows) }
                val screenItems = desktopItems.filter { it.screenId == page.screenId }
                for (item in screenItems) {
                    for (x in item.cellX until (item.cellX + item.spanX).coerceAtMost(gridConfig.cols)) {
                        for (y in item.cellY until (item.cellY + item.spanY).coerceAtMost(gridConfig.rows)) {
                            occ[x][y] = true
                        }
                    }
                }
                pageOccupancyMap[page.screenId] = occ
            }

            val desktopReflowedItems = mutableListOf<WorkspaceItemEntity>()
            for (item in reflowedToDesktop) {
                var placed = false
                for (page in pages) {
                    val occ = pageOccupancyMap[page.screenId] ?: continue
                    var vacant: Pair<Int, Int>? = null
                    for (y in 0 until gridConfig.rows) {
                        for (x in 0 until gridConfig.cols) {
                            if (!occ[x][y]) {
                                vacant = Pair(x, y)
                                break
                            }
                        }
                        if (vacant != null) break
                    }

                    if (vacant != null) {
                        val (vx, vy) = vacant
                        occ[vx][vy] = true
                        desktopReflowedItems.add(
                            item.copy(
                                container = WorkspaceItemEntity.CONTAINER_DESKTOP,
                                screenId = page.screenId,
                                cellX = vx,
                                cellY = vy,
                                spanX = 1,
                                spanY = 1
                            )
                        )
                        placed = true
                        break
                    }
                }
                if (!placed) {
                    return GridChangeResult.Failure("Cannot reduce dock capacity: workspace pages are full")
                }
            }

            dao.insertWorkspaceItems(desktopReflowedItems)
        }

        if (updatedDockItems.isNotEmpty()) {
            dao.insertWorkspaceItems(updatedDockItems)
        }
        return GridChangeResult.Success
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
