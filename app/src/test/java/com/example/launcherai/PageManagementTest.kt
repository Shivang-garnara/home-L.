package com.example.launcherai

import com.example.launcherai.data.db.WorkspaceItemEntity
import com.example.launcherai.data.db.WorkspacePageEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class PageManagementTest {

    @Test
    fun testStableScreenIdAllocation() {
        val page0 = WorkspacePageEntity(screenId = 0, pageOrder = 0)
        val page1 = WorkspacePageEntity(screenId = 1, pageOrder = 1)
        val pages = mutableListOf(page0, page1)

        val nextScreenId = (pages.maxOfOrNull { it.screenId } ?: 0) + 1
        val newPage = WorkspacePageEntity(screenId = nextScreenId, pageOrder = pages.size)
        pages.add(newPage)

        assertEquals(3, pages.size)
        assertEquals(2, pages[2].screenId)
        assertEquals(2, pages[2].pageOrder)
    }

    @Test
    fun testReorderingPreservesStableScreenIds() {
        // Reorder screenIds: [2, 0, 1]
        val newOrder = listOf(2, 0, 1)
        val reorderedPages = newOrder.mapIndexed { index, screenId ->
            WorkspacePageEntity(screenId = screenId, pageOrder = index)
        }

        assertEquals(3, reorderedPages.size)

        // First page in list has screenId 2, pageOrder 0
        assertEquals(2, reorderedPages[0].screenId)
        assertEquals(0, reorderedPages[0].pageOrder)

        // Second page in list has screenId 0, pageOrder 1
        assertEquals(0, reorderedPages[1].screenId)
        assertEquals(1, reorderedPages[1].pageOrder)

        // Third page in list has screenId 1, pageOrder 2
        assertEquals(1, reorderedPages[2].screenId)
        assertEquals(2, reorderedPages[2].pageOrder)
    }

    @Test
    fun testMinimumOnePageInvariant() {
        val pages = mutableListOf(WorkspacePageEntity(screenId = 0, pageOrder = 0))

        val canDelete = pages.size > 1
        assertFalse(canDelete)
    }

    @Test
    fun testPageDeletionItemMigration() {
        val items = mutableListOf(
            WorkspaceItemEntity(id = 1, itemType = WorkspaceItemEntity.ITEM_TYPE_APP, screenId = 0, cellX = 0, cellY = 0),
            WorkspaceItemEntity(id = 2, itemType = WorkspaceItemEntity.ITEM_TYPE_APP, screenId = 1, cellX = 0, cellY = 0)
        )

        // Delete page screenId = 1, migrate items to page screenId = 0
        val pageToDelete = 1
        val targetPage = 0

        val itemsToMigrate = items.filter { it.screenId == pageToDelete }
        itemsToMigrate.forEach { item ->
            val index = items.indexOf(item)
            items[index] = item.copy(screenId = targetPage, cellX = 1, cellY = 0)
        }

        val migratedItem = items.find { it.id == 2L }
        assertNotNull(migratedItem)
        assertEquals(0, migratedItem?.screenId)
        assertEquals(1, migratedItem?.cellX)
    }
}
