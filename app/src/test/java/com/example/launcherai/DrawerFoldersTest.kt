package com.example.launcherai

import com.example.launcherai.data.db.WorkspaceItemEntity
import com.example.launcherai.data.model.AppInfo
import com.example.launcherai.data.model.DrawerFolderHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawerFoldersTest {

    private fun sampleInstalledApps(): List<AppInfo> = listOf(
        AppInfo(label = "Calculator", packageName = "com.android.calc", activityName = ".Calc", category = AppInfo.CATEGORY_TOOLS),
        AppInfo(label = "Calendar", packageName = "com.android.cal", activityName = ".Cal", category = AppInfo.CATEGORY_PRODUCTIVITY),
        AppInfo(label = "Chess", packageName = "com.games.chess", activityName = ".Chess", category = AppInfo.CATEGORY_GAMES),
        AppInfo(label = "Chrome", packageName = "com.android.chrome", activityName = ".Main", category = AppInfo.CATEGORY_TOOLS),
        AppInfo(label = "Messages", packageName = "com.android.msg", activityName = ".Msg", category = AppInfo.CATEGORY_COMMUNICATION)
    )

    @Test
    fun testDrawerContainerConstantDistinctFromDesktopAndDock() {
        assertNotEquals(WorkspaceItemEntity.CONTAINER_DESKTOP, WorkspaceItemEntity.CONTAINER_DRAWER)
        assertNotEquals(WorkspaceItemEntity.CONTAINER_DOCK, WorkspaceItemEntity.CONTAINER_DRAWER)
        assertEquals(-102, WorkspaceItemEntity.CONTAINER_DRAWER)
    }

    @Test
    fun testDrawerFolderHidesMemberAppsFromMainDrawerWhenKeepInMainFalse() {
        val drawerFolder = WorkspaceItemEntity(
            id = 50L,
            itemType = WorkspaceItemEntity.ITEM_TYPE_FOLDER,
            title = "Utilities",
            container = WorkspaceItemEntity.CONTAINER_DRAWER,
            cellX = 0 // hide apps from main drawer (Nova default)
        )
        val desktopItem = WorkspaceItemEntity(
            id = 1L,
            itemType = WorkspaceItemEntity.ITEM_TYPE_APP,
            packageName = "com.android.chrome",
            className = ".Main",
            container = WorkspaceItemEntity.CONTAINER_DESKTOP
        )
        val folderChild1 = WorkspaceItemEntity(
            id = 51L,
            itemType = WorkspaceItemEntity.ITEM_TYPE_APP,
            packageName = "com.android.calc",
            className = ".Calc",
            container = 50,
            rank = 0,
            title = "Calculator"
        )
        val folderChild2 = WorkspaceItemEntity(
            id = 52L,
            itemType = WorkspaceItemEntity.ITEM_TYPE_APP,
            packageName = "com.android.cal",
            className = ".Cal",
            container = 50,
            rank = 1,
            title = "Calendar"
        )

        val items = DrawerFolderHelper.buildDrawerItems(
            installedApps = sampleInstalledApps(),
            allWorkspaceItems = listOf(desktopItem, drawerFolder, folderChild1, folderChild2),
            foldersFirst = true
        )

        // Folders first -> "Utilities" is first, followed by ungrouped apps (Chess, Chrome, Messages)
        assertEquals(4, items.size)
        assertTrue(items[0].isFolder)
        assertEquals(50L, items[0].folderId)
        assertEquals("Utilities", items[0].label)
        assertEquals(listOf("Calculator", "Calendar"), items[0].folderChildren.map { it.label })
        assertFalse(items[0].keepAppsInMainDrawer)

        val ungroupedLabels = items.drop(1).map { it.label }
        assertEquals(listOf("Chess", "Chrome", "Messages"), ungroupedLabels)
    }

    @Test
    fun testDrawerFolderKeepsMemberAppsInMainDrawerWhenKeepFlagEnabled() {
        val drawerFolder = WorkspaceItemEntity(
            id = 60L,
            itemType = WorkspaceItemEntity.ITEM_TYPE_FOLDER,
            title = "Games",
            container = WorkspaceItemEntity.CONTAINER_DRAWER,
            cellX = 1 // keep apps in main drawer
        )
        val child = WorkspaceItemEntity(
            id = 61L,
            itemType = WorkspaceItemEntity.ITEM_TYPE_APP,
            packageName = "com.games.chess",
            className = ".Chess",
            container = 60,
            rank = 0,
            title = "Chess"
        )

        val items = DrawerFolderHelper.buildDrawerItems(
            installedApps = sampleInstalledApps(),
            allWorkspaceItems = listOf(drawerFolder, child),
            foldersFirst = true
        )

        assertTrue(items[0].isFolder)
        assertTrue(items[0].keepAppsInMainDrawer)
        assertEquals(listOf("Chess"), items[0].folderChildren.map { it.label })
        assertTrue(items.any { !it.isFolder && it.packageName == "com.games.chess" })
    }

    @Test
    fun testEmptyDrawerFolderRemainsVisibleInDrawer() {
        val emptyFolder = WorkspaceItemEntity(
            id = 70L,
            itemType = WorkspaceItemEntity.ITEM_TYPE_FOLDER,
            title = "EmptyBox",
            container = WorkspaceItemEntity.CONTAINER_DRAWER,
            cellX = 0
        )

        val items = DrawerFolderHelper.buildDrawerItems(
            installedApps = sampleInstalledApps(),
            allWorkspaceItems = listOf(emptyFolder),
            foldersFirst = false
        )

        val folder = items.find { it.isFolder }
        assertEquals("EmptyBox", folder?.label)
        assertEquals(0, folder?.folderChildren?.size)
        // Inline alphabetical sorting places EmptyBox between Chrome and Messages
        assertEquals(listOf("Calculator", "Calendar", "Chess", "Chrome", "EmptyBox", "Messages"), items.map { it.label })
    }

    @Test
    fun testSearchQueryFindsHiddenFolderAppsAndFolderTitle() {
        val drawerFolder = WorkspaceItemEntity(
            id = 80L,
            itemType = WorkspaceItemEntity.ITEM_TYPE_FOLDER,
            title = "Math Tools",
            container = WorkspaceItemEntity.CONTAINER_DRAWER,
            cellX = 0
        )
        val calcChild = WorkspaceItemEntity(
            id = 81L,
            itemType = WorkspaceItemEntity.ITEM_TYPE_APP,
            packageName = "com.android.calc",
            className = ".Calc",
            container = 80,
            rank = 0,
            title = "Calculator"
        )

        // Searching "calc" still finds Calculator even though it is hidden inside folder from browse
        val appSearch = DrawerFolderHelper.buildDrawerItems(
            installedApps = sampleInstalledApps(),
            allWorkspaceItems = listOf(drawerFolder, calcChild),
            searchQuery = "calc",
            foldersFirst = true
        )
        assertEquals(listOf("Calculator"), appSearch.map { it.label })

        // Searching "math" finds the folder itself
        val folderSearch = DrawerFolderHelper.buildDrawerItems(
            installedApps = sampleInstalledApps(),
            allWorkspaceItems = listOf(drawerFolder, calcChild),
            searchQuery = "math",
            foldersFirst = true
        )
        assertEquals(1, folderSearch.size)
        assertTrue(folderSearch[0].isFolder)
        assertEquals("Math Tools", folderSearch[0].label)

        // Globally hidden package stays excluded even in search & inside folder
        val hiddenSearch = DrawerFolderHelper.buildDrawerItems(
            installedApps = sampleInstalledApps(),
            allWorkspaceItems = listOf(drawerFolder, calcChild),
            searchQuery = "calc",
            hiddenPackages = setOf("com.android.calc"),
            foldersFirst = true
        )
        assertTrue(hiddenSearch.isEmpty())
    }

    @Test
    fun testFoldersFirstVsInlineAlphabeticalSorting() {
        val items = listOf(
            AppInfo("Zoom", "com.z", ".Z"),
            AppInfo("Arcade", "drawer_folder:1", "", isFolder = true, folderId = 1L),
            AppInfo("Alpha", "com.a", ".A"),
            AppInfo("Social", "drawer_folder:2", "", isFolder = true, folderId = 2L)
        )

        val foldersFirst = DrawerFolderHelper.sortDrawerItems(items, foldersFirst = true)
        assertEquals(listOf("Arcade", "Social", "Alpha", "Zoom"), foldersFirst.map { it.label })

        val inlineAlpha = DrawerFolderHelper.sortDrawerItems(items, foldersFirst = false)
        assertEquals(listOf("Alpha", "Arcade", "Social", "Zoom"), inlineAlpha.map { it.label })
    }
}
