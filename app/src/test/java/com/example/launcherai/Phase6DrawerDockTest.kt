package com.example.launcherai

import com.example.launcherai.data.db.FolderEntity
import com.example.launcherai.data.db.WorkspaceDao
import com.example.launcherai.data.db.WorkspaceItemEntity
import com.example.launcherai.data.db.WorkspacePageEntity
import com.example.launcherai.data.model.AppInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class Phase6DrawerDockTest {

    private lateinit var fakeDao: FakePhase6WorkspaceDao

    @Before
    fun setup() {
        fakeDao = FakePhase6WorkspaceDao()
    }

    @Test
    fun testDockDatabasePersistence() = runBlocking {
        // Dock items container = CONTAINER_DOCK (-101)
        val phoneDockItem = WorkspaceItemEntity(
            id = 101,
            itemType = WorkspaceItemEntity.ITEM_TYPE_APP,
            packageName = "com.android.dialer",
            container = WorkspaceItemEntity.CONTAINER_DOCK,
            screenId = 0,
            cellX = 0,
            rank = 0,
            title = "Phone"
        )
        fakeDao.insertWorkspaceItem(phoneDockItem)

        val dockItems = fakeDao.getWorkspaceItemsByContainerSync(WorkspaceItemEntity.CONTAINER_DOCK)
        assertEquals(1, dockItems.size)
        assertEquals("Phone", dockItems[0].title)

        // Rearrange dock item to cellX = 2
        fakeDao.updateWorkspaceItem(phoneDockItem.copy(cellX = 2, rank = 2))
        val updatedDock = fakeDao.getWorkspaceItemsByContainerSync(WorkspaceItemEntity.CONTAINER_DOCK)
        assertEquals(2, updatedDock[0].cellX)
    }

    @Test
    fun testMoveAppBetweenWorkspaceAndDock() = runBlocking {
        val app = WorkspaceItemEntity(
            id = 102,
            itemType = WorkspaceItemEntity.ITEM_TYPE_APP,
            packageName = "com.example.browser",
            container = WorkspaceItemEntity.CONTAINER_DESKTOP,
            screenId = 0,
            cellX = 1,
            cellY = 1
        )
        fakeDao.insertWorkspaceItem(app)

        // Move to Dock
        fakeDao.updateWorkspaceItem(app.copy(container = WorkspaceItemEntity.CONTAINER_DOCK, cellX = 3, cellY = 0, rank = 3))
        val dockItems = fakeDao.getWorkspaceItemsByContainerSync(WorkspaceItemEntity.CONTAINER_DOCK)
        assertEquals(1, dockItems.size)
        assertEquals("com.example.browser", dockItems[0].packageName)

        // Move back to Workspace Desktop
        fakeDao.updateWorkspaceItem(dockItems[0].copy(container = WorkspaceItemEntity.CONTAINER_DESKTOP, screenId = 1, cellX = 0, cellY = 0))
        val desktopItems = fakeDao.getWorkspaceItemsByContainerSync(WorkspaceItemEntity.CONTAINER_DESKTOP)
        assertEquals(1, desktopItems.size)
        assertEquals(1, desktopItems[0].screenId)
    }

    @Test
    fun testCategoryFiltering() {
        val apps = listOf(
            AppInfo(label = "Game 1", packageName = "com.game1", activityName = "Main", category = AppInfo.CATEGORY_GAMES),
            AppInfo(label = "Calculator", packageName = "com.calc", activityName = "Main", category = AppInfo.CATEGORY_TOOLS),
            AppInfo(label = "Camera", packageName = "com.camera", activityName = "Main", category = AppInfo.CATEGORY_PHOTOGRAPHY)
        )

        val gameApps = apps.filter { it.category == AppInfo.CATEGORY_GAMES }
        val toolApps = apps.filter { it.category == AppInfo.CATEGORY_TOOLS }

        assertEquals(1, gameApps.size)
        assertEquals("Game 1", gameApps[0].label)
        assertEquals(1, toolApps.size)
        assertEquals("Calculator", toolApps[0].label)
    }

    @Test
    fun testHiddenPackagesFiltering() {
        val apps = listOf(
            AppInfo(label = "App A", packageName = "com.a", activityName = "Main"),
            AppInfo(label = "App B", packageName = "com.b", activityName = "Main"),
            AppInfo(label = "App C", packageName = "com.c", activityName = "Main")
        )

        val hiddenSet = mutableSetOf("com.b")
        val visibleApps = apps.filter { it.packageName !in hiddenSet }

        assertEquals(2, visibleApps.size)
        assertFalse(visibleApps.any { it.packageName == "com.b" })

        // Unhide "com.b"
        hiddenSet.remove("com.b")
        val restoredApps = apps.filter { it.packageName !in hiddenSet }
        assertEquals(3, restoredApps.size)
    }

    private class FakePhase6WorkspaceDao : WorkspaceDao {
        val items = mutableListOf<WorkspaceItemEntity>()

        override fun getAllWorkspaceItemsFlow(): Flow<List<WorkspaceItemEntity>> = flowOf(items)

        override suspend fun getAllWorkspaceItemsSync(): List<WorkspaceItemEntity> = items.toList()

        override fun getWorkspaceItemsByContainerFlow(container: Int): Flow<List<WorkspaceItemEntity>> {
            return flowOf(items.filter { it.container == container }.sortedBy { it.cellX })
        }

        override suspend fun getWorkspaceItemsByContainerSync(container: Int): List<WorkspaceItemEntity> {
            return items.filter { it.container == container }.sortedBy { it.cellX }
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

        override fun getAllFoldersFlow(): Flow<List<FolderEntity>> = flowOf(emptyList())

        override suspend fun getAllFoldersSync(): List<FolderEntity> = emptyList()

        override suspend fun getFolderById(folderId: Long): FolderEntity? = null

        override suspend fun insertFolder(folder: FolderEntity): Long = 0L

        override suspend fun updateFolder(folder: FolderEntity) {}

        override suspend fun deleteFolder(folderId: Long) {}
    }
}
