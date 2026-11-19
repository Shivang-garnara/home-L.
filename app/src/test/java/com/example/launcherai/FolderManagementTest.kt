package com.example.launcherai

import com.example.launcherai.data.db.FolderEntity
import com.example.launcherai.data.db.WorkspaceDao
import com.example.launcherai.data.db.WorkspaceItemEntity
import com.example.launcherai.data.db.WorkspacePageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class FolderManagementTest {

    private lateinit var fakeDao: FakeFolderWorkspaceDao

    @Before
    fun setup() {
        fakeDao = FakeFolderWorkspaceDao()
    }

    @Test
    fun testCreateFolderWithTwoApps() = runBlocking {
        val appA = WorkspaceItemEntity(id = 1, itemType = "APP", packageName = "com.app.a", container = -100, screenId = 0, cellX = 0, cellY = 0)
        val appB = WorkspaceItemEntity(id = 2, itemType = "APP", packageName = "com.app.b", container = -100, screenId = 0, cellX = 1, cellY = 0)
        fakeDao.insertWorkspaceItem(appA)
        fakeDao.insertWorkspaceItem(appB)

        // Create folder at appA's position
        val folderId = fakeDao.insertFolder(FolderEntity(title = "Social", screenId = 0, cellX = 0, cellY = 0))
        fakeDao.insertWorkspaceItem(
            WorkspaceItemEntity(
                id = folderId,
                itemType = WorkspaceItemEntity.ITEM_TYPE_FOLDER,
                title = "Social",
                container = WorkspaceItemEntity.CONTAINER_DESKTOP,
                screenId = 0,
                cellX = 0,
                cellY = 0
            )
        )

        // Add both apps to folder
        fakeDao.updateWorkspaceItem(appA.copy(container = folderId.toInt(), rank = 0))
        fakeDao.updateWorkspaceItem(appB.copy(container = folderId.toInt(), rank = 1))

        // Verify folder item on desktop
        val folderWorkspaceItem = fakeDao.getWorkspaceItemById(folderId)
        assertNotNull(folderWorkspaceItem)
        assertEquals(WorkspaceItemEntity.ITEM_TYPE_FOLDER, folderWorkspaceItem?.itemType)
        assertEquals("Social", folderWorkspaceItem?.title)

        // Verify folder contents
        val folderChildren = fakeDao.getWorkspaceItemsByContainerSync(folderId.toInt())
        assertEquals(2, folderChildren.size)
        assertEquals("com.app.a", folderChildren[0].packageName)
        assertEquals("com.app.b", folderChildren[1].packageName)
    }

    @Test
    fun testAddItemToFolder() = runBlocking {
        val folderId = 10L
        val app1 = WorkspaceItemEntity(id = 1, itemType = "APP", packageName = "com.app.1", container = folderId.toInt(), rank = 0)
        val app2 = WorkspaceItemEntity(id = 2, itemType = "APP", packageName = "com.app.2", container = folderId.toInt(), rank = 1)
        val app3 = WorkspaceItemEntity(id = 3, itemType = "APP", packageName = "com.app.3", container = -100, screenId = 0, cellX = 2, cellY = 0)

        fakeDao.insertWorkspaceItem(app1)
        fakeDao.insertWorkspaceItem(app2)
        fakeDao.insertWorkspaceItem(app3)

        // Add app3 to folder 10
        val childrenBefore = fakeDao.getWorkspaceItemsByContainerSync(folderId.toInt())
        fakeDao.updateWorkspaceItem(app3.copy(container = folderId.toInt(), rank = childrenBefore.size))

        val childrenAfter = fakeDao.getWorkspaceItemsByContainerSync(folderId.toInt())
        assertEquals(3, childrenAfter.size)
        assertEquals(2, childrenAfter.last().rank)
    }

    @Test
    fun testMoveItemOutOfFolderPromotesRemainingApp() = runBlocking {
        val folderId = 20L
        val folderItem = WorkspaceItemEntity(id = folderId, itemType = "FOLDER", title = "Tools", container = -100, screenId = 0, cellX = 1, cellY = 1)
        val app1 = WorkspaceItemEntity(id = 1, itemType = "APP", packageName = "com.tool.1", container = folderId.toInt(), rank = 0)
        val app2 = WorkspaceItemEntity(id = 2, itemType = "APP", packageName = "com.tool.2", container = folderId.toInt(), rank = 1)

        fakeDao.insertFolder(FolderEntity(folderId = folderId, title = "Tools", screenId = 0, cellX = 1, cellY = 1))
        fakeDao.insertWorkspaceItem(folderItem)
        fakeDao.insertWorkspaceItem(app1)
        fakeDao.insertWorkspaceItem(app2)

        // Move app1 out of folder to desktop (screenId = 0, cellX = 2, cellY = 2)
        fakeDao.updateWorkspaceItem(app1.copy(container = WorkspaceItemEntity.CONTAINER_DESKTOP, screenId = 0, cellX = 2, cellY = 2))

        // Evaluate folder cleanup (1 item remaining: app2)
        val remaining = fakeDao.getWorkspaceItemsByContainerSync(folderId.toInt())
        assertEquals(1, remaining.size)

        // Promote single remaining child (app2) to folder's desktop position (1, 1) and delete folder
        val singleChild = remaining.first()
        fakeDao.updateWorkspaceItem(
            singleChild.copy(
                container = folderItem.container,
                screenId = folderItem.screenId,
                cellX = folderItem.cellX,
                cellY = folderItem.cellY,
                rank = 0
            )
        )
        fakeDao.deleteWorkspaceItemById(folderId)
        fakeDao.deleteFolder(folderId)

        // Verify folder is deleted and app2 is promoted to desktop (1, 1)
        assertNull(fakeDao.getFolderById(folderId))
        assertNull(fakeDao.getWorkspaceItemById(folderId))

        val promotedApp = fakeDao.getWorkspaceItemById(2)
        assertNotNull(promotedApp)
        assertEquals(WorkspaceItemEntity.CONTAINER_DESKTOP, promotedApp?.container)
        assertEquals(1, promotedApp?.cellX)
        assertEquals(1, promotedApp?.cellY)
    }

    @Test
    fun testUpdateFolderTitle() = runBlocking {
        val folderId = 30L
        fakeDao.insertFolder(FolderEntity(folderId = folderId, title = "Old Title"))
        fakeDao.insertWorkspaceItem(WorkspaceItemEntity(id = folderId, itemType = "FOLDER", title = "Old Title"))

        // Update folder title to "Productivity"
        val folderRecord = fakeDao.getFolderById(folderId)
        fakeDao.updateFolder(folderRecord!!.copy(title = "Productivity"))

        val folderItem = fakeDao.getWorkspaceItemById(folderId)
        fakeDao.updateWorkspaceItem(folderItem!!.copy(title = "Productivity"))

        assertEquals("Productivity", fakeDao.getFolderById(folderId)?.title)
        assertEquals("Productivity", fakeDao.getWorkspaceItemById(folderId)?.title)
    }

    private class FakeFolderWorkspaceDao : WorkspaceDao {
        val items = mutableListOf<WorkspaceItemEntity>()
        val folders = mutableListOf<FolderEntity>()

        override fun getAllWorkspaceItemsFlow(): Flow<List<WorkspaceItemEntity>> = flowOf(items)

        override suspend fun getAllWorkspaceItemsSync(): List<WorkspaceItemEntity> = items.toList()

        override fun getWorkspaceItemsByContainerFlow(container: Int): Flow<List<WorkspaceItemEntity>> {
            return flowOf(items.filter { it.container == container }.sortedBy { it.rank })
        }

        override suspend fun getWorkspaceItemsByContainerSync(container: Int): List<WorkspaceItemEntity> {
            return items.filter { it.container == container }.sortedBy { it.rank }
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

        override fun getAllFoldersFlow(): Flow<List<FolderEntity>> = flowOf(folders)

        override suspend fun getAllFoldersSync(): List<FolderEntity> = folders.toList()

        override suspend fun getFolderById(folderId: Long): FolderEntity? {
            return folders.find { it.folderId == folderId }
        }

        override suspend fun insertFolder(folder: FolderEntity): Long {
            val id = if (folder.folderId == 0L) (folders.size + 100).toLong() else folder.folderId
            val newFolder = folder.copy(folderId = id)
            folders.removeIf { it.folderId == id }
            folders.add(newFolder)
            return id
        }

        override suspend fun updateFolder(folder: FolderEntity) {
            insertFolder(folder)
        }

        override suspend fun deleteFolder(folderId: Long) {
            folders.removeIf { it.folderId == folderId }
        }
    }
}
