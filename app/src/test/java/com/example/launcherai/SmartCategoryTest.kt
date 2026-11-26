package com.example.launcherai

import com.example.launcherai.data.db.WorkspaceItemEntity
import com.example.launcherai.data.model.AppInfo
import com.example.launcherai.data.model.DrawerFolderHelper
import com.example.launcherai.data.model.SmartCategoryHelper
import com.example.launcherai.data.model.SmartFolderCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartCategoryTest {

    @Test
    fun testNovaSevenSmartCategoriesPresent() {
        val autoIds = SmartFolderCategory.NOVA_AUTO_CATEGORIES.map { it.id }.toSet()
        assertEquals(
            setOf("ENTERTAINMENT", "FINANCE", "FOOD", "GAMES", "SHOPPING", "SOCIAL", "TRAVEL"),
            autoIds
        )
    }

    @Test
    fun testOfflineClassifierCoversNovaCategoriesAndGameEngines() {
        assertEquals(
            SmartFolderCategory.GAMES,
            SmartCategoryHelper.classifySmartCategory(-1, "com.studio.pixel", "com.unity3d.player.UnityPlayerActivity", "Pixel")
        )
        assertEquals(
            SmartFolderCategory.GAMES,
            SmartCategoryHelper.classifySmartCategory(-1, "com.gameloft.android.asphalt", ".MainActivity", "Asphalt")
        )
        assertEquals(
            SmartFolderCategory.FINANCE,
            SmartCategoryHelper.classifySmartCategory(-1, "com.google.android.apps.nbu.paisa.user", ".Main", "GPay")
        )
        assertEquals(
            SmartFolderCategory.FOOD,
            SmartCategoryHelper.classifySmartCategory(-1, "in.swiggy.android", ".Main", "Swiggy")
        )
        assertEquals(
            SmartFolderCategory.SHOPPING,
            SmartCategoryHelper.classifySmartCategory(-1, "com.flipkart.android", ".Main", "Flipkart")
        )
        assertEquals(
            SmartFolderCategory.TRAVEL,
            SmartCategoryHelper.classifySmartCategory(-1, "com.ubercab", ".Main", "Uber")
        )
        assertEquals(
            SmartFolderCategory.ENTERTAINMENT,
            SmartCategoryHelper.classifySmartCategory(-1, "com.google.android.youtube", ".Main", "YouTube")
        )
        assertEquals(
            SmartFolderCategory.SOCIAL,
            SmartCategoryHelper.classifySmartCategory(-1, "com.whatsapp", ".Main", "WhatsApp")
        )
        // Android Play Store should stay Tools rather than generic Shopping
        assertEquals(
            SmartFolderCategory.TOOLS,
            SmartCategoryHelper.classifySmartCategory(-1, "com.android.vending", ".AssetBrowserActivity", "Play Store")
        )
        // Browsers and email should NOT fall into Social even when Android OS tags them CATEGORY_SOCIAL (4)
        assertEquals(
            SmartFolderCategory.TOOLS,
            SmartCategoryHelper.classifySmartCategory(4, "com.android.chrome", ".Main", "Chrome")
        )
        assertEquals(
            SmartFolderCategory.TOOLS,
            SmartCategoryHelper.classifySmartCategory(4, "com.brave.browser", ".Main", "Brave")
        )
        assertEquals(
            SmartFolderCategory.PRODUCTIVITY,
            SmartCategoryHelper.classifySmartCategory(4, "com.google.android.gm", ".Main", "Gmail")
        )
    }

    @Test
    fun testInitialSmartFolderSyncAppliesNovaThreeAppThresholdAndRespectsCustomFolders() {
        val installed = listOf(
            // 4 Entertainment apps -> 1 already in custom folder, 3 left -> creates Entertainment
            AppInfo("YouTube", "com.google.android.youtube", ".Main", category = AppInfo.CATEGORY_ENTERTAINMENT),
            AppInfo("YT Music", "com.google.android.apps.youtube.music", ".Main", category = AppInfo.CATEGORY_ENTERTAINMENT),
            AppInfo("Music Player", "com.android.mediacenter", ".Main", category = AppInfo.CATEGORY_ENTERTAINMENT),
            AppInfo("Netflix", "com.netflix.mediaclient", ".Main", category = AppInfo.CATEGORY_ENTERTAINMENT),
            // 3 Social apps -> creates Social
            AppInfo("WhatsApp", "com.whatsapp", ".Main", category = AppInfo.CATEGORY_SOCIAL),
            AppInfo("Telegram", "org.telegram.messenger", ".Main", category = AppInfo.CATEGORY_SOCIAL),
            AppInfo("Instagram", "com.instagram.android", ".Main", category = AppInfo.CATEGORY_SOCIAL),
            // Only 2 Games apps -> below >= 3 threshold -> should NOT auto-create
            AppInfo("Chess", "com.games.chess", ".Main", category = AppInfo.CATEGORY_GAMES),
            AppInfo("Sudoku", "com.games.sudoku", ".Main", category = AppInfo.CATEGORY_GAMES)
        )

        val customFolder = WorkspaceItemEntity(
            id = 10L,
            itemType = WorkspaceItemEntity.ITEM_TYPE_FOLDER,
            title = "Watch Later",
            container = WorkspaceItemEntity.CONTAINER_DRAWER
        )
        val netflixInCustom = WorkspaceItemEntity(
            id = 11L,
            itemType = WorkspaceItemEntity.ITEM_TYPE_APP,
            packageName = "com.netflix.mediaclient",
            className = ".Main",
            container = 10
        )

        val plan = SmartCategoryHelper.planSmartFolderSync(
            installedApps = installed,
            allWorkspaceItems = listOf(customFolder, netflixInCustom),
            smartFoldersEnabled = true,
            didInitialLoad = false,
            dismissedCategoryIds = emptySet()
        )

        val createdTitles = plan.foldersToCreate.map { it.first.defaultTitle }
        assertEquals(listOf("Entertainment", "Social", "Games"), createdTitles)
        // Netflix stayed in custom folder ("Watch Later"), not duplicated into Entertainment
        val entertainmentApps = plan.foldersToCreate[0].second.map { it.packageName }
        assertFalse(entertainmentApps.contains("com.netflix.mediaclient"))
        assertEquals(3, entertainmentApps.size)
        assertTrue(plan.markInitialSyncDone)
    }

    @Test
    fun testIncrementalInstallSyncAndExcludedKeyProtection() {
        val installed = listOf(
            AppInfo("YouTube", "com.google.android.youtube", ".Main", category = AppInfo.CATEGORY_ENTERTAINMENT),
            AppInfo("Spotify", "com.spotify.music", ".Main", category = AppInfo.CATEGORY_ENTERTAINMENT),
            AppInfo("VLC", "org.videolan.vlc", ".Main", category = AppInfo.CATEGORY_ENTERTAINMENT)
        )

        val entFolder = WorkspaceItemEntity(
            id = 20L,
            itemType = WorkspaceItemEntity.ITEM_TYPE_FOLDER,
            packageName = SmartCategoryHelper.encodeCategoryPackageName("ENTERTAINMENT"),
            title = "Entertainment",
            container = WorkspaceItemEntity.CONTAINER_DRAWER,
            cellY = SmartCategoryHelper.FLAG_SMART_AUTO_CREATED or SmartCategoryHelper.FLAG_AUTO_ADD_NEW_APPS
        )
        val existingYt = WorkspaceItemEntity(
            id = 21L,
            itemType = WorkspaceItemEntity.ITEM_TYPE_APP,
            packageName = "com.google.android.youtube",
            className = ".Main",
            container = 20
        )

        val excludedKey = SmartCategoryHelper.excludedAppKey("ENTERTAINMENT", "org.videolan.vlc", ".Main")
        val plan = SmartCategoryHelper.planSmartFolderSync(
            installedApps = installed,
            allWorkspaceItems = listOf(entFolder, existingYt),
            smartFoldersEnabled = true,
            didInitialLoad = true,
            dismissedCategoryIds = emptySet(),
            excludedKeys = setOf(excludedKey)
        )

        assertTrue(plan.foldersToCreate.isEmpty())
        val added = plan.appsToAddByFolderId[20L].orEmpty().map { it.packageName }
        // Newly installed Spotify gets auto-added, while user-removed VLC remains excluded
        assertEquals(listOf("com.spotify.music"), added)
    }

    @Test
    fun testDisableSmartFoldersHidesAutoCreatedButKeepsCustomPresetFolder() {
        val autoSmart = WorkspaceItemEntity(
            id = 30L,
            itemType = WorkspaceItemEntity.ITEM_TYPE_FOLDER,
            packageName = SmartCategoryHelper.encodeCategoryPackageName("SOCIAL"),
            title = "Social",
            container = WorkspaceItemEntity.CONTAINER_DRAWER,
            cellY = SmartCategoryHelper.FLAG_SMART_AUTO_CREATED or SmartCategoryHelper.FLAG_AUTO_ADD_NEW_APPS
        )
        val customPreset = WorkspaceItemEntity(
            id = 31L,
            itemType = WorkspaceItemEntity.ITEM_TYPE_FOLDER,
            packageName = SmartCategoryHelper.encodeCategoryPackageName("GAMES"),
            title = "My Games",
            container = WorkspaceItemEntity.CONTAINER_DRAWER,
            cellY = SmartCategoryHelper.FLAG_AUTO_ADD_NEW_APPS
        )

        val plan = SmartCategoryHelper.planSmartFolderSync(
            installedApps = emptyList(),
            allWorkspaceItems = listOf(autoSmart, customPreset),
            smartFoldersEnabled = false,
            didInitialLoad = true,
            dismissedCategoryIds = emptySet()
        )

        assertEquals(listOf(30L), plan.folderIdsToDisable)

        // Disabled autoSmart folder is excluded by DrawerFolderHelper & ungroups its children back into drawer
        val disabledAutoSmart = autoSmart.copy(cellY = autoSmart.cellY or SmartCategoryHelper.FLAG_SMART_DISABLED)
        val childSocial = WorkspaceItemEntity(
            id = 32L,
            itemType = WorkspaceItemEntity.ITEM_TYPE_APP,
            packageName = "com.whatsapp",
            className = ".Main",
            container = 30,
            title = "WhatsApp"
        )
        val drawerItems = DrawerFolderHelper.buildDrawerItems(
            installedApps = listOf(AppInfo("WhatsApp", "com.whatsapp", ".Main", category = AppInfo.CATEGORY_SOCIAL)),
            allWorkspaceItems = listOf(disabledAutoSmart, customPreset, childSocial),
            foldersFirst = true
        )

        assertEquals(listOf("My Games", "WhatsApp"), drawerItems.map { it.label })
        assertNull(drawerItems.find { it.label == "Social" })
    }

    @Test
    fun testSuggestionCandidatesAndHomeFolderInference() {
        val apps = listOf(
            AppInfo("Swiggy", "in.swiggy.android", ".Main", category = AppInfo.CATEGORY_FOOD),
            AppInfo("Zomato", "com.application.zomato", ".Main", category = AppInfo.CATEGORY_FOOD),
            AppInfo("Dominos", "com.dominos.app", ".Main", category = AppInfo.CATEGORY_FOOD)
        )

        val suggestions = SmartCategoryHelper.getSuggestionCandidates(
            installedApps = apps,
            allWorkspaceItems = emptyList(),
            smartFoldersEnabled = true,
            dismissedCategoryIds = emptySet()
        )
        assertEquals(1, suggestions.size)
        assertEquals(SmartFolderCategory.FOOD, suggestions[0].category)
        assertEquals(3, suggestions[0].matchingApps.size)

        // Dismissed category does not re-suggest
        val afterDismiss = SmartCategoryHelper.getSuggestionCandidates(
            installedApps = apps,
            allWorkspaceItems = emptyList(),
            smartFoldersEnabled = true,
            dismissedCategoryIds = setOf("FOOD")
        )
        assertTrue(afterDismiss.isEmpty())

        // Home screen merge inference
        assertEquals("Food", SmartCategoryHelper.inferFolderTitleForApps(apps[0], apps[1]))
        val otherApp = AppInfo("Maps", "com.google.android.apps.maps", ".Main", category = AppInfo.CATEGORY_TRAVEL)
        assertEquals("Folder", SmartCategoryHelper.inferFolderTitleForApps(apps[0], otherApp))

        // Quick rename suggestions prioritize categories inside folder
        val quick = SmartCategoryHelper.getQuickRenameSuggestions(listOf(apps[0], apps[1], otherApp))
        assertEquals("Food", quick[0])
        assertEquals("Travel", quick[1])
    }
}
