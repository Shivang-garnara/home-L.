package com.example.launcherai

import com.example.launcherai.data.db.WorkspaceDao
import com.example.launcherai.data.db.WorkspaceItemEntity
import com.example.launcherai.dragndrop.DragController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DragControllerTest {

    private lateinit var fakeDao: FakeDragWorkspaceDao

    @Before
    fun setup() {
        fakeDao = FakeDragWorkspaceDao()
    }

    @Test
    fun testUpdateWorkspaceItemPositionOnSamePage() = runBlocking {
        val item = WorkspaceItemEntity(
            id = 1,
            itemType = WorkspaceItemEntity.ITEM_TYPE_APP,
            packageName = "com.example.app1",
            screenId = 0,
            cellX = 0,
            cellY = 0
        )
        fakeDao.insertWorkspaceItem(item)

        // Move to (cellX = 2, cellY = 3) on screenId 0
        val existing = fakeDao.getWorkspaceItemById(1)
        assertNotNull(existing)
        val updated = existing!!.copy(screenId = 0, cellX = 2, cellY = 3)
        fakeDao.updateWorkspaceItem(updated)

        val result = fakeDao.getWorkspaceItemById(1)
        assertEquals(0, result?.screenId)
        assertEquals(2, result?.cellX)
        assertEquals(3, result?.cellY)
    }

    @Test
    fun testUpdateWorkspaceItemPositionToDifferentPage() = runBlocking {
        val item = WorkspaceItemEntity(
            id = 2,
            itemType = WorkspaceItemEntity.ITEM_TYPE_APP,
            packageName = "com.example.app2",
            screenId = 0,
            cellX = 1,
            cellY = 1
        )
        fakeDao.insertWorkspaceItem(item)

        // Move to screenId 1 (cellX = 0, cellY = 2)
        val existing = fakeDao.getWorkspaceItemById(2)
        assertNotNull(existing)
        val updated = existing!!.copy(screenId = 1, cellX = 0, cellY = 2)
        fakeDao.updateWorkspaceItem(updated)

        val result = fakeDao.getWorkspaceItemById(2)
        assertEquals(1, result?.screenId)
        assertEquals(0, result?.cellX)
        assertEquals(2, result?.cellY)
    }

    @Test
    fun testRemoveShortcutFromWorkspace() = runBlocking {
        val item = WorkspaceItemEntity(
            id = 3,
            itemType = WorkspaceItemEntity.ITEM_TYPE_APP,
            packageName = "com.example.app3",
            screenId = 0,
            cellX = 0,
            cellY = 0
        )
        fakeDao.insertWorkspaceItem(item)
        assertEquals(1, fakeDao.items.size)

        // Drop on Remove Target
        fakeDao.deleteWorkspaceItemById(3)

        assertEquals(0, fakeDao.items.size)
        assertNull(fakeDao.getWorkspaceItemById(3))
    }

    @Test
    fun testDropTargetTypeEnum() {
        val removeTarget = DragController.DropTargetType.REMOVE
        val appInfoTarget = DragController.DropTargetType.APP_INFO
        val uninstallTarget = DragController.DropTargetType.UNINSTALL

        assertEquals("REMOVE", removeTarget.name)
        assertEquals("APP_INFO", appInfoTarget.name)
        assertEquals("UNINSTALL", uninstallTarget.name)
    }

    @Test
    fun testCalculateDropCell_MultiSpanNoOverflow() {
        val cellW = 250f
        val cellH = 200f
        val countX = 4
        val countY = 5

        // 4x1 full-width widget
        // Holding finger in column 2 (localX = 500f), regX = 500f (center)
        val cellCenter = DragController.calculateDropCell(
            localX = 500f,
            localY = 100f,
            spanX = 4,
            spanY = 1,
            cellW = cellW,
            cellH = cellH,
            pLeft = 0f,
            pTop = 0f,
            regX = 500f,
            regY = 100f,
            countX = countX,
            countY = countY
        )
        assertEquals(0, cellCenter[0])
        assertEquals(0, cellCenter[1])

        // Even if finger is near the right edge (localX = 900f), cellX must strictly be 0 for a 4-span widget
        val cellRight = DragController.calculateDropCell(
            localX = 900f,
            localY = 100f,
            spanX = 4,
            spanY = 1,
            cellW = cellW,
            cellH = cellH,
            pLeft = 0f,
            pTop = 0f,
            regX = 250f,
            regY = 100f,
            countX = countX,
            countY = countY
        )
        assertEquals(0, cellRight[0])
        assertEquals(0, cellRight[1])

        // 2x2 widget: maxCellX is 2, maxCellY is 3
        val cell2x2Right = DragController.calculateDropCell(
            localX = 950f,
            localY = 850f,
            spanX = 2,
            spanY = 2,
            cellW = cellW,
            cellH = cellH,
            pLeft = 0f,
            pTop = 0f,
            regX = 250f,
            regY = 200f,
            countX = countX,
            countY = countY
        )
        assertEquals(2, cell2x2Right[0])
        assertEquals(3, cell2x2Right[1])
        assertTrue(cell2x2Right[0] + 2 <= countX)
        assertTrue(cell2x2Right[1] + 2 <= countY)

        // Negative coordinates clamp to 0
        val cellNegative = DragController.calculateDropCell(
            localX = -100f,
            localY = -50f,
            spanX = 1,
            spanY = 1,
            cellW = cellW,
            cellH = cellH,
            pLeft = 0f,
            pTop = 0f,
            regX = 50f,
            regY = 50f,
            countX = countX,
            countY = countY
        )
        assertEquals(0, cellNegative[0])
        assertEquals(0, cellNegative[1])
    }

    private class FakeDragWorkspaceDao : WorkspaceDao {
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

        override fun getAllPagesFlow(): Flow<List<com.example.launcherai.data.db.WorkspacePageEntity>> = flowOf(emptyList())

        override suspend fun getAllPagesSync(): List<com.example.launcherai.data.db.WorkspacePageEntity> = emptyList()

        override suspend fun insertPage(page: com.example.launcherai.data.db.WorkspacePageEntity) {}

        override suspend fun insertPages(pages: List<com.example.launcherai.data.db.WorkspacePageEntity>) {}

        override suspend fun deletePage(screenId: Int) {}

        override suspend fun updatePageOrder(screenId: Int, pageOrder: Int) {}

        override suspend fun getMaxScreenId(): Int? = 0

        override suspend fun getWorkspaceItemsByScreenSync(screenId: Int): List<WorkspaceItemEntity> = items.filter { it.screenId == screenId }

        override fun getAllFoldersFlow(): Flow<List<com.example.launcherai.data.db.FolderEntity>> = flowOf(emptyList())

        override suspend fun getAllFoldersSync(): List<com.example.launcherai.data.db.FolderEntity> = emptyList()

        override suspend fun getFolderById(folderId: Long): com.example.launcherai.data.db.FolderEntity? = null

        override suspend fun insertFolder(folder: com.example.launcherai.data.db.FolderEntity): Long = 0L

        override suspend fun updateFolder(folder: com.example.launcherai.data.db.FolderEntity) {}

        override suspend fun deleteFolder(folderId: Long) {}
    }
}
