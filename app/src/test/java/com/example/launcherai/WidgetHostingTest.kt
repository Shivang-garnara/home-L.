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

        override fun getAllFoldersFlow(): Flow<List<FolderEntity>> = flowOf(emptyList())

        override suspend fun getAllFoldersSync(): List<FolderEntity> = emptyList()

        override suspend fun getFolderById(folderId: Long): FolderEntity? = null

        override suspend fun insertFolder(folder: FolderEntity): Long = 0L

        override suspend fun updateFolder(folder: FolderEntity) {}

        override suspend fun deleteFolder(folderId: Long) {}
    }
}
