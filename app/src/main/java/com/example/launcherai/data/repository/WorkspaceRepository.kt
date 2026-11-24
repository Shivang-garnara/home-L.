package com.example.launcherai.data.repository

import android.content.Context
import com.example.launcherai.AppInfo
import com.example.launcherai.AppManager
import com.example.launcherai.data.db.LauncherDatabase
import com.example.launcherai.data.db.WorkspaceDao
import com.example.launcherai.data.db.WorkspaceItemEntity
import com.example.launcherai.data.db.WorkspacePageEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WorkspaceRepository(
    private val context: Context,
    private val workspaceDao: WorkspaceDao = LauncherDatabase.getInstance(context).workspaceDao()
) {

    private val prefs = context.getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedAppsFlow: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _gridConfigFlow = MutableStateFlow(readStoredGridConfig())
    val gridConfigFlow: StateFlow<WorkspaceGridConfig> = _gridConfigFlow.asStateFlow()

    private val _dockCapacityFlow = MutableStateFlow(readStoredDockCapacity())
    val dockCapacityFlow: StateFlow<Int> = _dockCapacityFlow.asStateFlow()

    val allWorkspaceItemsFlow: Flow<List<WorkspaceItemEntity>> = workspaceDao.getAllWorkspaceItemsFlow()
    val desktopItemsFlow: Flow<List<WorkspaceItemEntity>> = workspaceDao.getWorkspaceItemsByContainerFlow(WorkspaceItemEntity.CONTAINER_DESKTOP)
    val dockItemsFlow: Flow<List<WorkspaceItemEntity>> = workspaceDao.getWorkspaceItemsByContainerFlow(WorkspaceItemEntity.CONTAINER_DOCK)
    val allPagesFlow: Flow<List<WorkspacePageEntity>> = workspaceDao.getAllPagesFlow()

    init {
        repositoryScope.launch {
            refreshInstalledApps()
            initializeDefaultWorkspaceIfEmpty()
            ensurePagesExist()
        }
    }

    private fun readStoredGridConfig(): WorkspaceGridConfig {
        val cols = prefs.getInt("key_grid_cols", WorkspaceGridConfig.DEFAULT_COLS)
        val rows = prefs.getInt("key_grid_rows", WorkspaceGridConfig.DEFAULT_ROWS)
        return WorkspaceGridConfig(cols, rows)
    }

    private fun readStoredDockCapacity(): Int {
        return prefs.getInt("key_dock_capacity", 5)
    }

    fun getGridConfig(): WorkspaceGridConfig = _gridConfigFlow.value

    fun getDockCapacity(): Int = _dockCapacityFlow.value

    /**
     * Authoritative dock capacity update.
     * Evaluates dock items against new capacity (3, 5, 7 slots).
     * Accounts for reserved App Drawer slot (newCapacity / 2).
     * Reflows overflow items safely to vacant desktop cells, persists updated layout to Room DB,
     * and updates SharedPreferences & dockCapacityFlow.
     */
    suspend fun setDockCapacity(newCapacity: Int): GridChangeResult = withContext(Dispatchers.IO) {
        val validCapacity = when (newCapacity) {
            3, 7 -> newCapacity
            else -> 5
        }

        if (validCapacity == _dockCapacityFlow.value) {
            return@withContext GridChangeResult.Success
        }

        val dockItems = workspaceDao.getWorkspaceItemsByContainerSync(WorkspaceItemEntity.CONTAINER_DOCK)
        val newDrawerSlot = validCapacity / 2

        val validDockSlots = (0 until validCapacity).filter { it != newDrawerSlot }
        val dockOccupancy = BooleanArray(validCapacity)
        dockOccupancy[newDrawerSlot] = true // Drawer slot is reserved

        val updatedDockItems = mutableListOf<WorkspaceItemEntity>()
        val unplacedDockItems = mutableListOf<WorkspaceItemEntity>()

        // First pass: keep dock items that fit in valid dock slots without colliding with new drawer slot or each other
        for (item in dockItems) {
            if (item.cellX in 0 until validCapacity && item.cellX != newDrawerSlot && !dockOccupancy[item.cellX]) {
                dockOccupancy[item.cellX] = true
                updatedDockItems.add(item)
            } else {
                unplacedDockItems.add(item)
            }
        }

        // Second pass: try placing unplaced dock items into remaining vacant valid dock slots
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

        // Third pass: if items cannot fit in dock capacity, reflow to vacant desktop cells
        if (reflowedToDesktop.isNotEmpty()) {
            val grid = _gridConfigFlow.value
            var pages = workspaceDao.getAllPagesSync()
            if (pages.isEmpty()) {
                ensurePagesExist()
                pages = workspaceDao.getAllPagesSync()
            }

            val desktopItems = workspaceDao.getWorkspaceItemsByContainerSync(WorkspaceItemEntity.CONTAINER_DESKTOP)
            val pageOccupancyMap = mutableMapOf<Int, Array<BooleanArray>>()
            for (page in pages) {
                val occ = Array(grid.cols) { BooleanArray(grid.rows) }
                val screenItems = desktopItems.filter { it.screenId == page.screenId }
                for (item in screenItems) {
                    markOccupied(occ, item.cellX, item.cellY, item.spanX, item.spanY)
                }
                pageOccupancyMap[page.screenId] = occ
            }

            val desktopReflowedItems = mutableListOf<WorkspaceItemEntity>()
            for (item in reflowedToDesktop) {
                var placedOnDesktop = false
                for (page in pages) {
                    val occ = pageOccupancyMap[page.screenId] ?: continue
                    val vacant = findVacantSpot(occ, grid.cols, grid.rows, 1, 1)
                    if (vacant != null) {
                        val (vx, vy) = vacant
                        markOccupied(occ, vx, vy, 1, 1)
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
                        placedOnDesktop = true
                        break
                    }
                }
                if (!placedOnDesktop) {
                    return@withContext GridChangeResult.Failure("Cannot reduce dock capacity: workspace pages are full")
                }
            }

            if (desktopReflowedItems.isNotEmpty()) {
                workspaceDao.insertWorkspaceItems(desktopReflowedItems)
            }
        }

        if (updatedDockItems.isNotEmpty()) {
            workspaceDao.insertWorkspaceItems(updatedDockItems)
        }

        prefs.edit().putInt("key_dock_capacity", validCapacity).apply()
        _dockCapacityFlow.value = validCapacity
        GridChangeResult.Success
    }

    /**
     * Authoritative grid configuration update.
     * Evaluates placements against new dimensions, reflows out-of-bounds/colliding items,
     * persists updated item positions to Room DB, and updates SharedPreferences & gridConfigFlow.
     */
    suspend fun setGridConfig(newCols: Int, newRows: Int): GridChangeResult = withContext(Dispatchers.IO) {
        if (newCols < WorkspaceGridConfig.MIN_COLS || newCols > WorkspaceGridConfig.MAX_COLS ||
            newRows < WorkspaceGridConfig.MIN_ROWS || newRows > WorkspaceGridConfig.MAX_ROWS) {
            return@withContext GridChangeResult.Failure("Grid dimensions $newCols x $newRows exceed valid bounds (${WorkspaceGridConfig.MIN_COLS}..${WorkspaceGridConfig.MAX_COLS} x ${WorkspaceGridConfig.MIN_ROWS}..${WorkspaceGridConfig.MAX_ROWS})")
        }

        val current = _gridConfigFlow.value
        val allDesktopItems = workspaceDao.getWorkspaceItemsByContainerSync(WorkspaceItemEntity.CONTAINER_DESKTOP)
        var pages = workspaceDao.getAllPagesSync()
        if (pages.isEmpty()) {
            ensurePagesExist()
            pages = workspaceDao.getAllPagesSync()
        }

        // Clamp widget spans if necessary and validate widget fits
        val clampedItems = allDesktopItems.map { item ->
            if (item.itemType == WorkspaceItemEntity.ITEM_TYPE_WIDGET) {
                val clampedSpanX = item.spanX.coerceIn(1, newCols)
                val clampedSpanY = item.spanY.coerceIn(1, newRows)
                item.copy(spanX = clampedSpanX, spanY = clampedSpanY)
            } else {
                item.copy(spanX = 1, spanY = 1)
            }
        }

        // Check total workspace capacity
        val totalCapacity = pages.size * (newCols * newRows)
        val requiredCells = clampedItems.sumOf { it.spanX * it.spanY }
        if (requiredCells > totalCapacity) {
            return@withContext GridChangeResult.Failure("Cannot fit all workspace items ($requiredCells cells required, total capacity is $totalCapacity cells)")
        }

        val updatedItems = mutableListOf<WorkspaceItemEntity>()
        val pageOccupancyMap = mutableMapOf<Int, Array<BooleanArray>>()

        for (page in pages) {
            pageOccupancyMap[page.screenId] = Array(newCols) { BooleanArray(newRows) }
        }

        // Group items by screen
        val itemsByScreen = clampedItems.groupBy { it.screenId }

        for (page in pages) {
            val screenId = page.screenId
            val screenItems = itemsByScreen[screenId] ?: emptyList()
            val occupancy = pageOccupancyMap[screenId]!!

            val fittingItems = mutableListOf<WorkspaceItemEntity>()
            val unplacedItems = mutableListOf<WorkspaceItemEntity>()

            for (item in screenItems) {
                if (item.cellX + item.spanX <= newCols && item.cellY + item.spanY <= newRows) {
                    fittingItems.add(item)
                } else {
                    unplacedItems.add(item)
                }
            }

            // Place fitting items first
            for (item in fittingItems) {
                var canFitInSpot = true
                for (x in item.cellX until item.cellX + item.spanX) {
                    for (y in item.cellY until item.cellY + item.spanY) {
                        if (occupancy[x][y]) {
                            canFitInSpot = false
                            break
                        }
                    }
                    if (!canFitInSpot) break
                }

                if (canFitInSpot) {
                    for (x in item.cellX until item.cellX + item.spanX) {
                        for (y in item.cellY until item.cellY + item.spanY) {
                            occupancy[x][y] = true
                        }
                    }
                    updatedItems.add(item)
                } else {
                    unplacedItems.add(item)
                }
            }

            // Place unplaced items on current page or find another page
            for (item in unplacedItems) {
                var placed = false

                // Try current page first
                val vacant = findVacantSpot(occupancy, newCols, newRows, item.spanX, item.spanY)
                if (vacant != null) {
                    val (vx, vy) = vacant
                    markOccupied(occupancy, vx, vy, item.spanX, item.spanY)
                    updatedItems.add(item.copy(screenId = screenId, cellX = vx, cellY = vy))
                    placed = true
                } else {
                    // Try other existing pages
                    for (otherPage in pages) {
                        if (otherPage.screenId == screenId) continue
                        val otherOccupancy = pageOccupancyMap[otherPage.screenId] ?: continue
                        val otherVacant = findVacantSpot(otherOccupancy, newCols, newRows, item.spanX, item.spanY)
                        if (otherVacant != null) {
                            val (ovx, ovy) = otherVacant
                            markOccupied(otherOccupancy, ovx, ovy, item.spanX, item.spanY)
                            updatedItems.add(item.copy(screenId = otherPage.screenId, cellX = ovx, cellY = ovy))
                            placed = true
                            break
                        }
                    }
                }

                if (!placed) {
                    return@withContext GridChangeResult.Failure("Cannot find valid non-overlapping position for item '${item.title ?: item.packageName}'")
                }
            }
        }

        // Persist reflowed layout to Room DB
        if (updatedItems.isNotEmpty()) {
            workspaceDao.insertWorkspaceItems(updatedItems)
        }

        // Persist preference and publish new grid config
        prefs.edit()
            .putInt("key_grid_cols", newCols)
            .putInt("key_grid_rows", newRows)
            .apply()

        _gridConfigFlow.value = WorkspaceGridConfig(newCols, newRows)
        GridChangeResult.Success
    }

    private fun findVacantSpot(
        occupancy: Array<BooleanArray>,
        cols: Int,
        rows: Int,
        spanX: Int,
        spanY: Int
    ): Pair<Int, Int>? {
        for (y in 0..(rows - spanY)) {
            for (x in 0..(cols - spanX)) {
                var empty = true
                for (dx in 0 until spanX) {
                    for (dy in 0 until spanY) {
                        if (occupancy[x + dx][y + dy]) {
                            empty = false
                            break
                        }
                    }
                    if (!empty) break
                }
                if (empty) return Pair(x, y)
            }
        }
        return null
    }

    private fun markOccupied(
        occupancy: Array<BooleanArray>,
        cellX: Int,
        cellY: Int,
        spanX: Int,
        spanY: Int
    ) {
        for (x in cellX until cellX + spanX) {
            for (y in cellY until cellY + spanY) {
                occupancy[x][y] = true
            }
        }
    }

    /**
     * Refreshes the list of installed applications from PackageManager off the main thread.
     */
    suspend fun refreshInstalledApps() = withContext(Dispatchers.IO) {
        val apps = AppManager.getInstalledApps(context)
        _installedApps.value = apps
    }

    /**
     * Ensures at least screenId 0 and 1 exist in workspace_pages if DB has pages missing.
     */
    private suspend fun ensurePagesExist() = withContext(Dispatchers.IO) {
        val pages = workspaceDao.getAllPagesSync()
        if (pages.isEmpty()) {
            workspaceDao.insertPages(
                listOf(
                    WorkspacePageEntity(screenId = 0, pageOrder = 0),
                    WorkspacePageEntity(screenId = 1, pageOrder = 1)
                )
            )
        }
    }

    /**
     * Appends a new blank workspace page and returns the created WorkspacePageEntity.
     */
    suspend fun addNewPage(): WorkspacePageEntity = withContext(Dispatchers.IO) {
        val pages = workspaceDao.getAllPagesSync()
        val maxScreenId = workspaceDao.getMaxScreenId() ?: (pages.maxOfOrNull { it.screenId } ?: -1)
        val newScreenId = maxScreenId + 1
        val newPageOrder = pages.size
        val newPage = WorkspacePageEntity(screenId = newScreenId, pageOrder = newPageOrder)
        workspaceDao.insertPage(newPage)
        newPage
    }

    /**
     * Safely deletes a workspace page, migrating items to the nearest valid page.
     * Prevents deletion if only 1 page remains.
     */
    suspend fun deletePage(screenId: Int): Boolean = withContext(Dispatchers.IO) {
        val pages = workspaceDao.getAllPagesSync()
        if (pages.size <= 1) return@withContext false

        val pageToDelete = pages.find { it.screenId == screenId } ?: return@withContext false

        // Determine destination page for item migration
        val remainingPages = pages.filter { it.screenId != screenId }
        val targetPage = remainingPages.find { it.pageOrder < pageToDelete.pageOrder }
            ?: remainingPages.first()

        // Migrate items on deleted page
        val itemsOnPage = workspaceDao.getWorkspaceItemsByScreenSync(screenId)

        for (item in itemsOnPage) {
            val vacant = findVacantCellOnPage(targetPage.screenId, item.spanX, item.spanY)
            if (vacant != null) {
                workspaceDao.updateWorkspaceItem(
                    item.copy(
                        screenId = targetPage.screenId,
                        cellX = vacant[0],
                        cellY = vacant[1]
                    )
                )
            } else {
                // If target page has no space, move to any page with space
                var moved = false
                for (otherPage in remainingPages) {
                    val otherVacant = findVacantCellOnPage(otherPage.screenId, item.spanX, item.spanY)
                    if (otherVacant != null) {
                        workspaceDao.updateWorkspaceItem(
                            item.copy(
                                screenId = otherPage.screenId,
                                cellX = otherVacant[0],
                                cellY = otherVacant[1]
                            )
                        )
                        moved = true
                        break
                    }
                }
                if (!moved) {
                    // Fallback: place on target page
                    workspaceDao.updateWorkspaceItem(
                        item.copy(
                            screenId = targetPage.screenId,
                            cellX = 0,
                            cellY = 0
                        )
                    )
                }
            }
        }

        // Delete page entity
        workspaceDao.deletePage(screenId)

        // Re-index remaining page orders
        val updatedPages = workspaceDao.getAllPagesSync()
        updatedPages.forEachIndexed { index, page ->
            if (page.pageOrder != index) {
                workspaceDao.updatePageOrder(page.screenId, index)
            }
        }

        // If deleted page was default, reset default page to first remaining page
        val currentDefault = getDefaultPage()
        if (currentDefault == screenId) {
            setDefaultPage(remainingPages.first().screenId)
        }

        true
    }

    /**
     * Reorders pages according to the provided list of screenIds, updating pageOrder column only.
     */
    suspend fun reorderPages(screenIdOrder: List<Int>) = withContext(Dispatchers.IO) {
        screenIdOrder.forEachIndexed { index, screenId ->
            workspaceDao.updatePageOrder(screenId, index)
        }
    }

    /**
     * Persists the default home screenId in SharedPreferences.
     */
    fun setDefaultPage(screenId: Int) {
        val prefs = context.getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("key_default_screen_id", screenId).apply()
    }

    /**
     * Returns the persisted default home screenId.
     */
    fun getDefaultPage(): Int {
        val prefs = context.getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("key_default_screen_id", 0)
    }

    private suspend fun findVacantCellOnPage(screenId: Int, spanX: Int, spanY: Int): IntArray? = withContext(Dispatchers.IO) {
        val grid = getGridConfig()
        val itemsOnScreen = workspaceDao.getWorkspaceItemsByScreenSync(screenId)
        val occ = Array(grid.cols) { BooleanArray(grid.rows) }
        for (item in itemsOnScreen) {
            val sx = item.cellX.coerceIn(0, grid.cols - 1)
            val sy = item.cellY.coerceIn(0, grid.rows - 1)
            val ex = (sx + item.spanX).coerceAtMost(grid.cols)
            val ey = (sy + item.spanY).coerceAtMost(grid.rows)
            for (x in sx until ex) {
                for (y in sy until ey) {
                    occ[x][y] = true
                }
            }
        }
        for (y in 0..grid.rows - spanY) {
            for (x in 0..grid.cols - spanX) {
                var fits = true
                for (cx in x until x + spanX) {
                    for (cy in y until y + spanY) {
                        if (occ[cx][cy]) {
                            fits = false
                            break
                        }
                    }
                    if (!fits) break
                }
                if (fits) return@withContext intArrayOf(x, y)
            }
        }
        null
    }

    /**
     * Populates initial workspace default entries only on first installation.
     * Never resets or overwrites an existing user's workspace.
     */
    suspend fun initializeDefaultWorkspaceIfEmpty() = withContext(Dispatchers.IO) {
        val existingCount = workspaceDao.getItemCount()
        if (existingCount > 0) {
            // User workspace already exists; preserve it.
            return@withContext
        }

        // Initialize default page 0 and page 1
        workspaceDao.insertPages(
            listOf(
                WorkspacePageEntity(screenId = 0, pageOrder = 0),
                WorkspacePageEntity(screenId = 1, pageOrder = 1)
            )
        )

        val installedApps = AppManager.getInstalledApps(context)
        if (installedApps.isEmpty()) return@withContext

        val itemsToInsert = mutableListOf<WorkspaceItemEntity>()

        // Page 0 Desktop Apps (up to 8 items in 4x2 grid)
        val page0Apps = installedApps.take(8)
        page0Apps.forEachIndexed { index, app ->
            val cellX = index % 4
            val cellY = index / 4
            itemsToInsert.add(
                WorkspaceItemEntity(
                    itemType = WorkspaceItemEntity.ITEM_TYPE_APP,
                    packageName = app.packageName,
                    className = app.activityName,
                    container = WorkspaceItemEntity.CONTAINER_DESKTOP,
                    screenId = 0,
                    cellX = cellX,
                    cellY = cellY,
                    spanX = 1,
                    spanY = 1,
                    rank = index,
                    title = app.label
                )
            )
        }

        // Page 1 Desktop Apps (up to 8 additional items in 4x2 grid if available)
        if (installedApps.size > 8) {
            val page1Apps = installedApps.drop(8).take(8)
            page1Apps.forEachIndexed { index, app ->
                val cellX = index % 4
                val cellY = index / 4
                itemsToInsert.add(
                    WorkspaceItemEntity(
                        itemType = WorkspaceItemEntity.ITEM_TYPE_APP,
                        packageName = app.packageName,
                        className = app.activityName,
                        container = WorkspaceItemEntity.CONTAINER_DESKTOP,
                        screenId = 1,
                        cellX = cellX,
                        cellY = cellY,
                        spanX = 1,
                        spanY = 1,
                        rank = index,
                        title = app.label
                    )
                )
            }
        }

        // Default Dock Apps (up to 4 items in container = CONTAINER_DOCK)
        val dockApps = installedApps.take(4)
        dockApps.forEachIndexed { index, app ->
            itemsToInsert.add(
                WorkspaceItemEntity(
                    itemType = WorkspaceItemEntity.ITEM_TYPE_APP,
                    packageName = app.packageName,
                    className = app.activityName,
                    container = WorkspaceItemEntity.CONTAINER_DOCK,
                    screenId = 0,
                    cellX = index,
                    cellY = 0,
                    spanX = 1,
                    spanY = 1,
                    rank = index,
                    title = app.label
                )
            )
        }

        workspaceDao.insertWorkspaceItems(itemsToInsert)
    }

    // --- Package Monitoring Hooks ---

    suspend fun onPackageAdded(packageName: String) = withContext(Dispatchers.IO) {
        refreshInstalledApps()
    }

    suspend fun onPackageRemoved(packageName: String, isReplacing: Boolean) = withContext(Dispatchers.IO) {
        refreshInstalledApps()
        if (!isReplacing) {
            // App was completely uninstalled. Remove invalid shortcuts from Room database.
            workspaceDao.deleteWorkspaceItemsByPackageName(packageName)
            // Cleanup folders if apps were removed from folders
            val folders = workspaceDao.getAllFoldersSync()
            folders.forEach { folder ->
                evaluateAndCleanupFolder(folder.folderId)
            }
        }
    }

    suspend fun onPackageChanged(packageName: String) = withContext(Dispatchers.IO) {
        refreshInstalledApps()
    }

    suspend fun onPackageReplaced(packageName: String) = withContext(Dispatchers.IO) {
        refreshInstalledApps()
    }

    // --- Folder Operations ---

    suspend fun createFolder(
        existingItem: WorkspaceItemEntity,
        draggedItem: WorkspaceItemEntity,
        folderTitle: String = "Folder"
    ): Long = withContext(Dispatchers.IO) {
        val existingId = if (existingItem.id == 0L) {
            workspaceDao.insertWorkspaceItem(existingItem)
        } else existingItem.id

        val draggedId = if (draggedItem.id == 0L) {
            workspaceDao.insertWorkspaceItem(draggedItem)
        } else draggedItem.id

        val folderId = workspaceDao.insertFolder(
            com.example.launcherai.data.db.FolderEntity(
                title = folderTitle,
                screenId = existingItem.screenId,
                cellX = existingItem.cellX,
                cellY = existingItem.cellY
            )
        )

        val folderWorkspaceItem = WorkspaceItemEntity(
            id = folderId,
            itemType = WorkspaceItemEntity.ITEM_TYPE_FOLDER,
            title = folderTitle,
            container = existingItem.container,
            screenId = existingItem.screenId,
            cellX = existingItem.cellX,
            cellY = existingItem.cellY
        )
        workspaceDao.insertWorkspaceItem(folderWorkspaceItem)

        val targetExisting = workspaceDao.getWorkspaceItemById(existingId) ?: existingItem
        workspaceDao.updateWorkspaceItem(
            targetExisting.copy(
                container = folderId.toInt(),
                screenId = 0,
                cellX = 0,
                cellY = 0,
                rank = 0
            )
        )

        val targetDragged = workspaceDao.getWorkspaceItemById(draggedId) ?: draggedItem
        workspaceDao.updateWorkspaceItem(
            targetDragged.copy(
                container = folderId.toInt(),
                screenId = 0,
                cellX = 0,
                cellY = 0,
                rank = 1
            )
        )

        folderId
    }

    suspend fun addItemToFolder(folderId: Long, item: WorkspaceItemEntity) = withContext(Dispatchers.IO) {
        val itemId = if (item.id == 0L) {
            workspaceDao.insertWorkspaceItem(item)
        } else item.id

        val targetItem = workspaceDao.getWorkspaceItemById(itemId) ?: item
        val children = workspaceDao.getWorkspaceItemsByContainerSync(folderId.toInt())
        workspaceDao.updateWorkspaceItem(
            targetItem.copy(
                container = folderId.toInt(),
                screenId = 0,
                cellX = 0,
                cellY = 0,
                rank = children.size
            )
        )
    }

    suspend fun moveItemOutOfFolder(
        item: WorkspaceItemEntity,
        targetScreenId: Int,
        targetCellX: Int,
        targetCellY: Int
    ) = withContext(Dispatchers.IO) {
        val sourceFolderId = item.container
        workspaceDao.updateWorkspaceItem(
            item.copy(
                container = WorkspaceItemEntity.CONTAINER_DESKTOP,
                screenId = targetScreenId,
                cellX = targetCellX,
                cellY = targetCellY,
                rank = 0
            )
        )

        if (sourceFolderId > 0) {
            evaluateAndCleanupFolder(sourceFolderId.toLong())
        }
    }

    suspend fun evaluateAndCleanupFolder(folderId: Long) = withContext(Dispatchers.IO) {
        val remainingChildren = workspaceDao.getWorkspaceItemsByContainerSync(folderId.toInt())
        val folderWorkspaceItem = workspaceDao.getWorkspaceItemById(folderId) ?: return@withContext

        if (remainingChildren.size == 1) {
            val singleChild = remainingChildren.first()
            workspaceDao.updateWorkspaceItem(
                singleChild.copy(
                    container = folderWorkspaceItem.container,
                    screenId = folderWorkspaceItem.screenId,
                    cellX = folderWorkspaceItem.cellX,
                    cellY = folderWorkspaceItem.cellY,
                    rank = 0
                )
            )
            workspaceDao.deleteWorkspaceItemById(folderId)
            workspaceDao.deleteFolder(folderId)
        } else if (remainingChildren.isEmpty()) {
            workspaceDao.deleteWorkspaceItemById(folderId)
            workspaceDao.deleteFolder(folderId)
        }
    }

    suspend fun updateFolderTitle(folderId: Long, newTitle: String) = withContext(Dispatchers.IO) {
        val folderRecord = workspaceDao.getFolderById(folderId)
        if (folderRecord != null) {
            workspaceDao.updateFolder(folderRecord.copy(title = newTitle))
        }
        val folderItem = workspaceDao.getWorkspaceItemById(folderId)
        if (folderItem != null) {
            workspaceDao.updateWorkspaceItem(folderItem.copy(title = newTitle))
        }
    }

    suspend fun getWorkspaceItemById(id: Long): WorkspaceItemEntity? = withContext(Dispatchers.IO) {
        workspaceDao.getWorkspaceItemById(id)
    }

    suspend fun getWorkspaceItemsByContainerSync(container: Int): List<WorkspaceItemEntity> = withContext(Dispatchers.IO) {
        workspaceDao.getWorkspaceItemsByContainerSync(container)
    }

    // --- DB Direct Access Helpers ---

    suspend fun addWorkspaceItem(item: WorkspaceItemEntity): Long = withContext(Dispatchers.IO) {
        workspaceDao.insertWorkspaceItem(item)
    }

    suspend fun updateWorkspaceItem(item: WorkspaceItemEntity) = withContext(Dispatchers.IO) {
        workspaceDao.updateWorkspaceItem(item)
    }

    suspend fun updateWorkspaceItemPosition(id: Long, screenId: Int, cellX: Int, cellY: Int) = withContext(Dispatchers.IO) {
        val existing = workspaceDao.getWorkspaceItemById(id) ?: return@withContext
        val updated = existing.copy(
            screenId = screenId,
            cellX = cellX,
            cellY = cellY
        )
        workspaceDao.updateWorkspaceItem(updated)
    }

    suspend fun deleteWorkspaceItem(item: WorkspaceItemEntity) = withContext(Dispatchers.IO) {
        workspaceDao.deleteWorkspaceItem(item)
    }

    suspend fun deleteWorkspaceItemById(id: Long) = withContext(Dispatchers.IO) {
        workspaceDao.deleteWorkspaceItemById(id)
    }
}
