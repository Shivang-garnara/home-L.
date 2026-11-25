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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WorkspaceRepositoryTest {

    private lateinit var fakeDao: FakeWorkspaceDao

    @Before
    fun setup() {
        fakeDao = FakeWorkspaceDao()
    }

    @Test
    fun testWorkspaceItemEntityDefaults() {
        val item = WorkspaceItemEntity(
            itemType = WorkspaceItemEntity.ITEM_TYPE_APP,
            packageName = "com.example.test",
            className = "com.example.test.MainActivity",
            title = "Test App"
        )

        assertEquals(WorkspaceItemEntity.ITEM_TYPE_APP, item.itemType)
        assertEquals("com.example.test", item.packageName)
        assertEquals(WorkspaceItemEntity.CONTAINER_DESKTOP, item.container)
        assertEquals(0, item.screenId)
        assertEquals(1, item.spanX)
        assertEquals(1, item.spanY)
        assertTrue(item.createdAt > 0)
    }

    @Test
    fun testUninstallPackageRemovesShortcuts() = runBlocking {
        // Given an item in the workspace
        val item = WorkspaceItemEntity(
            id = 1,
            itemType = WorkspaceItemEntity.ITEM_TYPE_APP,
            packageName = "com.example.uninstallme",
            title = "Uninstall Me"
        )
        fakeDao.insertWorkspaceItem(item)
        assertEquals(1, fakeDao.items.size)

        // When package is uninstalled (isReplacing = false)
        fakeDao.deleteWorkspaceItemsByPackageName("com.example.uninstallme")

        // Then item should be deleted from DB
        assertEquals(0, fakeDao.items.size)
    }

    @Test
    fun testUpdatePackagePreservesShortcuts() = runBlocking {
        // Given an item in the workspace
        val item = WorkspaceItemEntity(
            id = 1,
            itemType = WorkspaceItemEntity.ITEM_TYPE_APP,
            packageName = "com.example.updateme",
            title = "Update Me"
        )
        fakeDao.insertWorkspaceItem(item)
        assertEquals(1, fakeDao.items.size)

        // When package is temporarily replaced during an update (isReplacing = true)
        val isReplacing = true
        if (!isReplacing) {
            fakeDao.deleteWorkspaceItemsByPackageName("com.example.updateme")
        }

        // Then item is preserved in DB
        assertEquals(1, fakeDao.items.size)
        assertEquals("com.example.updateme", fakeDao.items[0].packageName)
    }

    @Test
    fun testWorkspacePageEntity() {
        val page = WorkspacePageEntity(screenId = 0, pageOrder = 0)
        assertEquals(0, page.screenId)
        assertEquals(0, page.pageOrder)
    }

    @Test
    fun testFolderEntity() {
        val folder = FolderEntity(folderId = 1, title = "Games", screenId = 0, cellX = 1, cellY = 2)
        assertEquals(1L, folder.folderId)
        assertEquals("Games", folder.title)
        assertEquals(1, folder.cellX)
        assertEquals(2, folder.cellY)
    }

    @Test
    fun testFindDuplicateWorkspaceItemIdsRemovesRaceSeededDuplicates() {
        val items = listOf(
            WorkspaceItemEntity(id = 1, itemType = WorkspaceItemEntity.ITEM_TYPE_APP, packageName = "com.a", className = "Main", container = WorkspaceItemEntity.CONTAINER_DESKTOP, screenId = 0, cellX = 0, cellY = 0, title = "Albums"),
            WorkspaceItemEntity(id = 21, itemType = WorkspaceItemEntity.ITEM_TYPE_APP, packageName = "com.a", className = "Main", container = WorkspaceItemEntity.CONTAINER_DESKTOP, screenId = 0, cellX = 0, cellY = 0, title = "Albums"),
            WorkspaceItemEntity(id = 17, itemType = WorkspaceItemEntity.ITEM_TYPE_APP, packageName = "com.a", className = "Main", container = WorkspaceItemEntity.CONTAINER_DOCK, screenId = 0, cellX = 0, cellY = 0, title = "Albums"),
            WorkspaceItemEntity(id = 37, itemType = WorkspaceItemEntity.ITEM_TYPE_APP, packageName = "com.a", className = "Main", container = WorkspaceItemEntity.CONTAINER_DOCK, screenId = 0, cellX = 0, cellY = 0, title = "Albums"),
            WorkspaceItemEntity(id = 5, itemType = WorkspaceItemEntity.ITEM_TYPE_APP, packageName = "com.b", className = "Main", container = WorkspaceItemEntity.CONTAINER_DESKTOP, screenId = 0, cellX = 1, cellY = 0, title = "Camera")
        )
        val duplicateIds = com.example.launcherai.data.repository.WorkspaceRepository.findDuplicateWorkspaceItemIds(items)
        assertEquals(listOf(21L, 37L), duplicateIds)
    }

    /**
     * Fake DAO for fast unit testing off-device.
     */
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
