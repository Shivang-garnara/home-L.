package com.example.launcherai.data.model

import com.example.launcherai.data.db.WorkspaceItemEntity
import java.util.Locale

/**
 * Pure helper utilities for Nova Launcher-style App Drawer folders:
 * - Resolves drawer folder entries (CONTAINER_DRAWER = -102) from Room entities
 * - Filters out apps placed into folders when keepAppsInMainDrawer == false (Nova hideApps)
 * - Keeps folder contents searchable by folder name or app name
 * - Supports "Folders first" vs inline alphabetical sorting
 */
object DrawerFolderHelper {

    const val FOLDER_PACKAGE_PREFIX = "drawer_folder:"

    fun componentKey(packageName: String, activityName: String?): String {
        val cls = activityName?.trim().orEmpty()
        return if (cls.isEmpty()) packageName else "$packageName/$cls"
    }

    /**
     * Resolves drawer folder AppInfo entries and ungrouped/visible apps for the App Drawer.
     */
    fun buildDrawerItems(
        installedApps: List<AppInfo>,
        allWorkspaceItems: List<WorkspaceItemEntity>,
        searchQuery: String = "",
        selectedCategory: String = AppInfo.CATEGORY_ALL,
        hiddenPackages: Set<String> = emptySet(),
        foldersFirst: Boolean = true
    ): List<AppInfo> {
        val visibleInstalled = installedApps.filter { !it.isFolder && it.packageName !in hiddenPackages }
        val appByKey = HashMap<String, AppInfo>(visibleInstalled.size)
        val appByPackage = HashMap<String, AppInfo>(visibleInstalled.size)
        for (app in visibleInstalled) {
            appByKey[componentKey(app.packageName, app.activityName)] = app
            if (!appByPackage.containsKey(app.packageName)) {
                appByPackage[app.packageName] = app
            }
        }

        val drawerFolderEntities = allWorkspaceItems.filter {
            it.container == WorkspaceItemEntity.CONTAINER_DRAWER &&
                it.itemType == WorkspaceItemEntity.ITEM_TYPE_FOLDER &&
                !SmartCategoryHelper.isFolderDisabled(it)
        }
        val activeFolderIds = drawerFolderEntities.map { it.id.toInt() }.toHashSet()
        val childrenByContainer = allWorkspaceItems
            .filter { it.container in activeFolderIds && it.itemType == WorkspaceItemEntity.ITEM_TYPE_APP }
            .groupBy { it.container }

        val hiddenKeysByFolders = HashSet<String>()
        val hiddenPackagesFallback = HashSet<String>()
        val drawerFolders = mutableListOf<AppInfo>()

        for (folderEntity in drawerFolderEntities) {
            val keepInMain = folderEntity.cellX == 1
            val rawChildren = childrenByContainer[folderEntity.id.toInt()]
                ?.sortedWith(compareBy({ it.rank }, { it.id }))
                .orEmpty()

            val resolvedChildren = rawChildren.mapNotNull { child ->
                val pkg = child.packageName ?: return@mapNotNull null
                if (pkg in hiddenPackages) return@mapNotNull null
                val exactKey = componentKey(pkg, child.className)
                val resolved = appByKey[exactKey] ?: appByPackage[pkg]
                if (!keepInMain) {
                    if (child.className.isNullOrBlank()) {
                        hiddenPackagesFallback.add(pkg)
                    } else {
                        hiddenKeysByFolders.add(exactKey)
                        if (resolved != null) {
                            hiddenKeysByFolders.add(componentKey(resolved.packageName, resolved.activityName))
                        }
                    }
                }
                resolved ?: AppInfo(
                    label = child.title ?: pkg,
                    packageName = pkg,
                    activityName = child.className.orEmpty(),
                    isSystem = false
                )
            }

            drawerFolders.add(
                AppInfo(
                    label = folderEntity.title?.takeIf { it.isNotBlank() } ?: "Folder",
                    packageName = "$FOLDER_PACKAGE_PREFIX${folderEntity.id}",
                    activityName = "",
                    icon = null,
                    isSystem = false,
                    category = AppInfo.CATEGORY_ALL,
                    isFolder = true,
                    folderId = folderEntity.id,
                    folderChildren = resolvedChildren,
                    keepAppsInMainDrawer = keepInMain,
                    smartCategoryId = SmartCategoryHelper.decodeCategoryId(folderEntity.packageName),
                    autoAddNewApps = SmartCategoryHelper.isAutoAddEnabled(folderEntity)
                )
            )
        }

        val trimmedQuery = searchQuery.trim()
        val combined = if (trimmedQuery.isNotEmpty()) {
            val lower = trimmedQuery.lowercase(Locale.ROOT)
            val matchingFolders = drawerFolders.filter { it.label.lowercase(Locale.ROOT).contains(lower) }
            val matchingApps = visibleInstalled.filter { it.label.lowercase(Locale.ROOT).contains(lower) }
            matchingFolders + matchingApps
        } else {
            val ungroupedApps = visibleInstalled.filter { app ->
                app.packageName !in hiddenPackagesFallback &&
                    componentKey(app.packageName, app.activityName) !in hiddenKeysByFolders
            }
            drawerFolders + ungroupedApps
        }

        val categoryFiltered = if (selectedCategory == AppInfo.CATEGORY_ALL) {
            combined
        } else {
            combined.filter { item ->
                if (item.isFolder) {
                    item.smartCategoryId?.let {
                        SmartCategoryHelper.matchesCategory(
                            AppInfo(item.label, "", "", category = it),
                            selectedCategory
                        )
                    } == true || item.folderChildren.any { SmartCategoryHelper.matchesCategory(it, selectedCategory) }
                } else {
                    SmartCategoryHelper.matchesCategory(item, selectedCategory)
                }
            }
        }

        return sortDrawerItems(categoryFiltered, foldersFirst)
    }

    fun sortDrawerItems(items: List<AppInfo>, foldersFirst: Boolean = true): List<AppInfo> {
        return if (foldersFirst) {
            items.sortedWith(compareBy({ !it.isFolder }, { it.label.lowercase(Locale.ROOT) }))
        } else {
            items.sortedBy { it.label.lowercase(Locale.ROOT) }
        }
    }
}
