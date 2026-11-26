package com.example.launcherai.data.repository

import android.content.Context
import com.example.launcherai.data.db.LauncherDatabase
import com.example.launcherai.data.db.WorkspaceDao
import com.example.launcherai.data.db.WorkspaceItemEntity
import com.example.launcherai.data.db.WorkspacePageEntity
import com.example.launcherai.data.model.AppInfo
import com.example.launcherai.data.model.DrawerFolderHelper
import com.example.launcherai.data.model.SmartCategoryHelper
import com.example.launcherai.data.model.SmartFolderCategory
import com.example.launcherai.util.AppManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class WorkspaceRepository(
    private val context: Context,
    private val workspaceDao: WorkspaceDao = LauncherDatabase.getInstance(context).workspaceDao()
) {

    companion object {
        private val seedMutex = Mutex()

        const val KEY_DRAWER_SMART_FOLDERS = "key_drawer_smart_folders"
        const val KEY_SMART_FOLDERS_INITIAL_DONE = "key_drawer_smart_folders_did_initial_load"
        const val KEY_DISMISSED_SMART_CATEGORIES = "key_dismissed_smart_categories"
        const val KEY_EXCLUDED_SMART_FOLDER_KEYS = "key_excluded_smart_folder_keys"
        const val KEY_HIDDEN_PACKAGES = "key_hidden_packages"

        /**
         * Identifies redundant duplicate APP items sharing the exact same slot and component,
         * keeping the earliest inserted row (lowest id).
         */
        fun findDuplicateWorkspaceItemIds(items: List<WorkspaceItemEntity>): List<Long> {
            val seen = HashSet<String>()
            val dupIds = mutableListOf<Long>()
            for (item in items.sortedBy { it.id }) {
                if (item.itemType != WorkspaceItemEntity.ITEM_TYPE_APP) continue
                val key = "${item.container}|${item.screenId}|${item.cellX}|${item.cellY}|${item.packageName ?: ""}|${item.className ?: ""}"
                if (!seen.add(key)) {
                    dupIds.add(item.id)
                }
            }
            return dupIds
        }
    }

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
            deduplicateOverlappingItems()
            ensurePagesExist()
            syncSmartFoldersIfNeeded()
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
        seedMutex.withLock {
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
    }

    suspend fun deduplicateOverlappingItems() = withContext(Dispatchers.IO) {
        seedMutex.withLock {
            val allItems = workspaceDao.getAllWorkspaceItemsSync()
            val duplicateIds = findDuplicateWorkspaceItemIds(allItems)
            for (dupId in duplicateIds) {
                workspaceDao.deleteWorkspaceItemById(dupId)
            }
        }
    }

    // --- Package Monitoring Hooks ---

    suspend fun onPackageAdded(packageName: String) = withContext(Dispatchers.IO) {
        refreshInstalledApps()
        syncSmartFoldersIfNeeded()
        val autoAdd = prefs.getBoolean("key_add_icon_to_home", false)
        if (autoAdd) {
            val pm = context.packageManager
            val launchIntent = pm.getLaunchIntentForPackage(packageName) ?: return@withContext
            val appInfo = try {
                pm.getApplicationInfo(packageName, 0)
            } catch (e: Exception) {
                null
            } ?: return@withContext

            val appLabel = pm.getApplicationLabel(appInfo).toString()

            // Check if already placed on desktop to avoid duplicate shortcuts
            val existing = workspaceDao.getItemsByPackageName(packageName)
            if (existing.any { it.container == WorkspaceItemEntity.CONTAINER_DESKTOP }) {
                return@withContext
            }

            ensurePagesExist()
            var pages = workspaceDao.getAllPagesSync()
            if (pages.isEmpty()) {
                workspaceDao.insertPage(WorkspacePageEntity(screenId = 0, pageOrder = 0))
                pages = workspaceDao.getAllPagesSync()
            }

            val grid = _gridConfigFlow.value
            val desktopItems = workspaceDao.getWorkspaceItemsByContainerSync(WorkspaceItemEntity.CONTAINER_DESKTOP)

            var targetScreenId = -1
            var targetCellX = 0
            var targetCellY = 0
            var foundSpot = false

            for (page in pages) {
                val occ = Array(grid.cols) { BooleanArray(grid.rows) }
                val screenItems = desktopItems.filter { it.screenId == page.screenId }
                for (item in screenItems) {
                    markOccupied(occ, item.cellX, item.cellY, item.spanX, item.spanY)
                }
                val spot = findVacantSpot(occ, grid.cols, grid.rows, 1, 1)
                if (spot != null) {
                    targetScreenId = page.screenId
                    targetCellX = spot.first
                    targetCellY = spot.second
                    foundSpot = true
                    break
                }
            }

            if (!foundSpot) {
                // All existing pages full: create new page
                val newScreenId = (pages.map { it.screenId }.maxOrNull() ?: 0) + 1
                val newPageOrder = (pages.map { it.pageOrder }.maxOrNull() ?: 0) + 1
                workspaceDao.insertPage(WorkspacePageEntity(screenId = newScreenId, pageOrder = newPageOrder))
                targetScreenId = newScreenId
                targetCellX = 0
                targetCellY = 0
            }

            val newItem = WorkspaceItemEntity(
                container = WorkspaceItemEntity.CONTAINER_DESKTOP,
                screenId = targetScreenId,
                cellX = targetCellX,
                cellY = targetCellY,
                spanX = 1,
                spanY = 1,
                itemType = WorkspaceItemEntity.ITEM_TYPE_APP,
                packageName = packageName,
                className = launchIntent.component?.className,
                title = appLabel
            )
            workspaceDao.insertWorkspaceItem(newItem)

            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(
                    context,
                    "Shortcut \"$appLabel\" added to home screen",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
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
        syncSmartFoldersIfNeeded()
    }

    suspend fun onPackageReplaced(packageName: String) = withContext(Dispatchers.IO) {
        refreshInstalledApps()
        syncSmartFoldersIfNeeded()
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

        val folderId = workspaceDao.insertWorkspaceItem(
            WorkspaceItemEntity(
                itemType = WorkspaceItemEntity.ITEM_TYPE_FOLDER,
                title = folderTitle,
                container = existingItem.container,
                screenId = existingItem.screenId,
                cellX = existingItem.cellX,
                cellY = existingItem.cellY
            )
        )

        workspaceDao.insertFolder(
            com.example.launcherai.data.db.FolderEntity(
                folderId = folderId,
                title = folderTitle,
                screenId = existingItem.screenId,
                cellX = existingItem.cellX,
                cellY = existingItem.cellY
            )
        )

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
        val folderWorkspaceItem = workspaceDao.getWorkspaceItemById(folderId) ?: return@withContext
        // Nova keeps App Drawer folders alive even when empty or down to 1 app until explicitly deleted.
        if (folderWorkspaceItem.container == WorkspaceItemEntity.CONTAINER_DRAWER) {
            return@withContext
        }
        val remainingChildren = workspaceDao.getWorkspaceItemsByContainerSync(folderId.toInt())

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

    suspend fun sortFolderAppsAlphabetically(folderId: Long) = withContext(Dispatchers.IO) {
        val children = workspaceDao.getWorkspaceItemsByContainerSync(folderId.toInt())
        val sorted = children.sortedBy { (it.title ?: "").lowercase() }
        sorted.forEachIndexed { idx, child ->
            if (child.rank != idx) {
                workspaceDao.updateWorkspaceItem(child.copy(rank = idx))
            }
        }
    }

    suspend fun addAppsToFolder(
        folderId: Long,
        apps: List<com.example.launcherai.data.model.AppInfo>
    ) = withContext(Dispatchers.IO) {
        val existing = workspaceDao.getWorkspaceItemsByContainerSync(folderId.toInt())
        val existingKeys = existing.map {
            com.example.launcherai.data.model.DrawerFolderHelper.componentKey(it.packageName.orEmpty(), it.className)
        }.toHashSet()
        var nextRank = (existing.maxOfOrNull { it.rank } ?: -1) + 1
        for (app in apps) {
            val key = com.example.launcherai.data.model.DrawerFolderHelper.componentKey(app.packageName, app.activityName)
            if (key !in existingKeys) {
                existingKeys.add(key)
                workspaceDao.insertWorkspaceItem(
                    WorkspaceItemEntity(
                        itemType = WorkspaceItemEntity.ITEM_TYPE_APP,
                        packageName = app.packageName,
                        className = app.activityName,
                        container = folderId.toInt(),
                        screenId = 0,
                        cellX = 0,
                        cellY = 0,
                        spanX = 1,
                        spanY = 1,
                        rank = nextRank++,
                        title = app.label
                    )
                )
            }
        }
    }

    // --- App Drawer Folder Operations (Nova Drawer Groups / TAB_FOLDER & Smart Folders parity) ---

    suspend fun syncSmartFoldersIfNeeded(forceInitialEnable: Boolean = false) = withContext(Dispatchers.IO) {
        seedMutex.withLock {
            val apps = _installedApps.value.ifEmpty {
                AppManager.getInstalledApps(context).also { _installedApps.value = it }
            }
            if (apps.isEmpty()) return@withLock

            val allItems = workspaceDao.getAllWorkspaceItemsSync()
            val smartEnabled = prefs.getBoolean(KEY_DRAWER_SMART_FOLDERS, true)
            val didInitialLoad = if (forceInitialEnable) false else prefs.getBoolean(KEY_SMART_FOLDERS_INITIAL_DONE, false)
            val dismissed = prefs.getStringSet(KEY_DISMISSED_SMART_CATEGORIES, emptySet())?.toSet() ?: emptySet()
            val excluded = prefs.getStringSet(KEY_EXCLUDED_SMART_FOLDER_KEYS, emptySet())?.toSet() ?: emptySet()
            val hidden = prefs.getStringSet(KEY_HIDDEN_PACKAGES, emptySet())?.toSet() ?: emptySet()

            val plan = SmartCategoryHelper.planSmartFolderSync(
                installedApps = apps,
                allWorkspaceItems = allItems,
                smartFoldersEnabled = smartEnabled,
                didInitialLoad = didInitialLoad,
                dismissedCategoryIds = dismissed,
                excludedKeys = excluded,
                hiddenPackages = hidden
            )

            for (folderId in plan.folderIdsToDisable) {
                val existing = workspaceDao.getWorkspaceItemById(folderId) ?: continue
                workspaceDao.updateWorkspaceItem(
                    existing.copy(cellY = existing.cellY or SmartCategoryHelper.FLAG_SMART_DISABLED)
                )
            }

            for (folderId in plan.folderIdsToEnable) {
                val existing = workspaceDao.getWorkspaceItemById(folderId) ?: continue
                val clearedFlags = (existing.cellY and SmartCategoryHelper.FLAG_SMART_DISABLED.inv()
                    and SmartCategoryHelper.FLAG_SMART_USER_DISMISSED.inv()) or SmartCategoryHelper.FLAG_AUTO_ADD_NEW_APPS
                workspaceDao.updateWorkspaceItem(existing.copy(cellY = clearedFlags))
            }

            for ((folderId, appsToAdd) in plan.appsToAddByFolderId) {
                if (appsToAdd.isNotEmpty()) {
                    addAppsToFolder(folderId, appsToAdd)
                }
            }

            for ((category, matchingApps) in plan.foldersToCreate) {
                createDrawerFolderInternal(
                    title = category.defaultTitle,
                    keepAppsInMainDrawer = false,
                    initialApps = matchingApps,
                    categoryId = category.id,
                    flags = SmartCategoryHelper.FLAG_SMART_AUTO_CREATED or SmartCategoryHelper.FLAG_AUTO_ADD_NEW_APPS
                )
            }

            if (plan.markInitialSyncDone) {
                prefs.edit().putBoolean(KEY_SMART_FOLDERS_INITIAL_DONE, true).apply()
            }
        }
    }

    suspend fun setSmartFoldersEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        if (enabled) {
            prefs.edit()
                .putBoolean(KEY_DRAWER_SMART_FOLDERS, true)
                .putBoolean(KEY_SMART_FOLDERS_INITIAL_DONE, false)
                .remove(KEY_DISMISSED_SMART_CATEGORIES)
                .remove(KEY_EXCLUDED_SMART_FOLDER_KEYS)
                .apply()
            syncSmartFoldersIfNeeded(forceInitialEnable = true)
        } else {
            prefs.edit()
                .putBoolean(KEY_DRAWER_SMART_FOLDERS, false)
                .apply()
            syncSmartFoldersIfNeeded(forceInitialEnable = false)
        }
    }

    suspend fun enableSmartCategoryFolder(
        category: SmartFolderCategory,
        initialApps: List<AppInfo>
    ): Long = withContext(Dispatchers.IO) {
        seedMutex.withLock {
            removeDismissedSmartCategoryLocked(category.id)
            val allItems = workspaceDao.getAllWorkspaceItemsSync()
            val existingFolder = allItems.find {
                it.container == WorkspaceItemEntity.CONTAINER_DRAWER &&
                    it.itemType == WorkspaceItemEntity.ITEM_TYPE_FOLDER &&
                    SmartCategoryHelper.decodeCategory(it.packageName) == category
            }
            if (existingFolder != null) {
                val updatedFlags = (existingFolder.cellY and SmartCategoryHelper.FLAG_SMART_DISABLED.inv()
                    and SmartCategoryHelper.FLAG_SMART_USER_DISMISSED.inv()) or SmartCategoryHelper.FLAG_AUTO_ADD_NEW_APPS
                workspaceDao.updateWorkspaceItem(existingFolder.copy(cellY = updatedFlags))
                if (initialApps.isNotEmpty()) {
                    addAppsToFolder(existingFolder.id, initialApps)
                }
                existingFolder.id
            } else {
                createDrawerFolderInternal(
                    title = category.defaultTitle,
                    keepAppsInMainDrawer = false,
                    initialApps = initialApps,
                    categoryId = category.id,
                    flags = SmartCategoryHelper.FLAG_SMART_AUTO_CREATED or SmartCategoryHelper.FLAG_AUTO_ADD_NEW_APPS
                )
            }
        }
    }

    suspend fun dismissSmartCategorySuggestion(category: SmartFolderCategory) = withContext(Dispatchers.IO) {
        seedMutex.withLock {
            addDismissedSmartCategoryLocked(category.id)
            val allItems = workspaceDao.getAllWorkspaceItemsSync()
            allItems.filter {
                it.container == WorkspaceItemEntity.CONTAINER_DRAWER &&
                    it.itemType == WorkspaceItemEntity.ITEM_TYPE_FOLDER &&
                    SmartCategoryHelper.decodeCategory(it.packageName) == category &&
                    SmartCategoryHelper.isFolderDisabled(it)
            }.forEach { entity ->
                workspaceDao.updateWorkspaceItem(
                    entity.copy(cellY = entity.cellY or SmartCategoryHelper.FLAG_SMART_USER_DISMISSED)
                )
            }
        }
    }

    private fun addDismissedSmartCategoryLocked(categoryId: String) {
        val current = HashSet(prefs.getStringSet(KEY_DISMISSED_SMART_CATEGORIES, emptySet()) ?: emptySet())
        current.add(categoryId.uppercase(java.util.Locale.ROOT))
        prefs.edit().putStringSet(KEY_DISMISSED_SMART_CATEGORIES, current).apply()
    }

    private fun removeDismissedSmartCategoryLocked(categoryId: String) {
        val current = HashSet(prefs.getStringSet(KEY_DISMISSED_SMART_CATEGORIES, emptySet()) ?: emptySet())
        if (current.remove(categoryId.uppercase(java.util.Locale.ROOT))) {
            prefs.edit().putStringSet(KEY_DISMISSED_SMART_CATEGORIES, current).apply()
        }
    }

    private fun updateExcludedSmartKeysLocked(categoryId: String, addKeys: Set<String>, removeKeys: Set<String>) {
        if (addKeys.isEmpty() && removeKeys.isEmpty()) return
        val current = HashSet(prefs.getStringSet(KEY_EXCLUDED_SMART_FOLDER_KEYS, emptySet()) ?: emptySet())
        current.addAll(addKeys)
        current.removeAll(removeKeys)
        prefs.edit().putStringSet(KEY_EXCLUDED_SMART_FOLDER_KEYS, current).apply()
    }

    private suspend fun createDrawerFolderInternal(
        title: String,
        keepAppsInMainDrawer: Boolean,
        initialApps: List<AppInfo>,
        categoryId: String?,
        flags: Int
    ): Long {
        val safeTitle = title.trim().ifEmpty { "Folder" }
        val keepFlag = if (keepAppsInMainDrawer) 1 else 0
        val encodedPkg = categoryId?.let { SmartCategoryHelper.encodeCategoryPackageName(it) }
        val folderId = workspaceDao.insertWorkspaceItem(
            WorkspaceItemEntity(
                itemType = WorkspaceItemEntity.ITEM_TYPE_FOLDER,
                packageName = encodedPkg,
                title = safeTitle,
                container = WorkspaceItemEntity.CONTAINER_DRAWER,
                screenId = 0,
                cellX = keepFlag,
                cellY = flags
            )
        )
        workspaceDao.insertFolder(
            com.example.launcherai.data.db.FolderEntity(
                folderId = folderId,
                title = safeTitle,
                screenId = 0,
                cellX = keepFlag,
                cellY = flags
            )
        )
        if (initialApps.isNotEmpty()) {
            addAppsToFolder(folderId, initialApps)
        }
        return folderId
    }

    suspend fun createDrawerFolder(
        title: String,
        keepAppsInMainDrawer: Boolean = false,
        initialApps: List<AppInfo> = emptyList(),
        categoryId: String? = null,
        autoAddNewApps: Boolean = false
    ): Long = withContext(Dispatchers.IO) {
        if (!categoryId.isNullOrBlank()) {
            seedMutex.withLock {
                removeDismissedSmartCategoryLocked(categoryId)
            }
        }
        val flags = if (!categoryId.isNullOrBlank() && autoAddNewApps) {
            SmartCategoryHelper.FLAG_AUTO_ADD_NEW_APPS
        } else 0
        createDrawerFolderInternal(title, keepAppsInMainDrawer, initialApps, categoryId, flags)
    }

    suspend fun updateDrawerFolder(
        folderId: Long,
        title: String,
        keepAppsInMainDrawer: Boolean,
        categoryId: String? = null,
        autoAddNewApps: Boolean = false,
        updateCategoryConfig: Boolean = false
    ) = withContext(Dispatchers.IO) {
        val safeTitle = title.trim().ifEmpty { "Folder" }
        val keepFlag = if (keepAppsInMainDrawer) 1 else 0
        val folderItem = workspaceDao.getWorkspaceItemById(folderId)
        val newPkg = if (updateCategoryConfig) {
            categoryId?.let { SmartCategoryHelper.encodeCategoryPackageName(it) }
        } else {
            folderItem?.packageName
        }
        val newFlags = if (updateCategoryConfig && folderItem != null) {
            val baseFlags = folderItem.cellY and SmartCategoryHelper.FLAG_AUTO_ADD_NEW_APPS.inv()
            if (!categoryId.isNullOrBlank() && autoAddNewApps) {
                baseFlags or SmartCategoryHelper.FLAG_AUTO_ADD_NEW_APPS
            } else {
                baseFlags and SmartCategoryHelper.FLAG_SMART_AUTO_CREATED.inv()
            }
        } else {
            folderItem?.cellY ?: 0
        }

        val folderRecord = workspaceDao.getFolderById(folderId)
        if (folderRecord != null) {
            workspaceDao.updateFolder(folderRecord.copy(title = safeTitle, cellX = keepFlag, cellY = newFlags))
        }
        if (folderItem != null) {
            workspaceDao.updateWorkspaceItem(
                folderItem.copy(
                    title = safeTitle,
                    cellX = keepFlag,
                    cellY = newFlags,
                    packageName = newPkg
                )
            )
        }
    }

    suspend fun setDrawerFolderApps(
        folderId: Long,
        selectedApps: List<AppInfo>
    ) = withContext(Dispatchers.IO) {
        val folderItem = workspaceDao.getWorkspaceItemById(folderId)
        val catId = SmartCategoryHelper.decodeCategoryId(folderItem?.packageName)
        val existing = workspaceDao.getWorkspaceItemsByContainerSync(folderId.toInt())
        val selectedByKey = selectedApps.associateBy {
            DrawerFolderHelper.componentKey(it.packageName, it.activityName)
        }

        val kept = mutableListOf<WorkspaceItemEntity>()
        val keptKeys = HashSet<String>()
        val removedExcludedKeys = HashSet<String>()
        for (item in existing.sortedWith(compareBy({ it.rank }, { it.id }))) {
            val pkg = item.packageName.orEmpty()
            val key = DrawerFolderHelper.componentKey(pkg, item.className)
            if (key in selectedByKey) {
                kept.add(item)
                keptKeys.add(key)
            } else {
                workspaceDao.deleteWorkspaceItemById(item.id)
                if (catId != null) {
                    removedExcludedKeys.add(SmartCategoryHelper.excludedAppKey(catId, pkg, item.className))
                }
            }
        }

        kept.forEachIndexed { idx, item ->
            if (item.rank != idx) {
                workspaceDao.updateWorkspaceItem(item.copy(rank = idx))
            }
        }

        val newlyAddedExcludedClears = HashSet<String>()
        var nextRank = kept.size
        for (app in selectedApps) {
            val key = DrawerFolderHelper.componentKey(app.packageName, app.activityName)
            if (catId != null) {
                newlyAddedExcludedClears.add(SmartCategoryHelper.excludedAppKey(catId, app.packageName, app.activityName))
            }
            if (key !in keptKeys) {
                keptKeys.add(key)
                workspaceDao.insertWorkspaceItem(
                    WorkspaceItemEntity(
                        itemType = WorkspaceItemEntity.ITEM_TYPE_APP,
                        packageName = app.packageName,
                        className = app.activityName,
                        container = folderId.toInt(),
                        screenId = 0,
                        cellX = 0,
                        cellY = 0,
                        spanX = 1,
                        spanY = 1,
                        rank = nextRank++,
                        title = app.label
                    )
                )
            }
        }

        if (catId != null) {
            seedMutex.withLock {
                updateExcludedSmartKeysLocked(catId, removedExcludedKeys, newlyAddedExcludedClears)
            }
        }
    }

    suspend fun removeAppFromDrawerFolder(folderId: Long, itemId: Long) = withContext(Dispatchers.IO) {
        val folderItem = workspaceDao.getWorkspaceItemById(folderId)
        val catId = SmartCategoryHelper.decodeCategoryId(folderItem?.packageName)
        val removingItem = if (catId != null) workspaceDao.getWorkspaceItemById(itemId) else null

        workspaceDao.deleteWorkspaceItemById(itemId)
        val remaining = workspaceDao.getWorkspaceItemsByContainerSync(folderId.toInt())
            .sortedWith(compareBy({ it.rank }, { it.id }))
        remaining.forEachIndexed { idx, item ->
            if (item.rank != idx) {
                workspaceDao.updateWorkspaceItem(item.copy(rank = idx))
            }
        }

        if (catId != null && removingItem?.packageName != null) {
            val exKey = SmartCategoryHelper.excludedAppKey(catId, removingItem.packageName, removingItem.className)
            seedMutex.withLock {
                updateExcludedSmartKeysLocked(catId, setOf(exKey), emptySet())
            }
        }
    }

    suspend fun deleteDrawerFolder(folderId: Long) = withContext(Dispatchers.IO) {
        val folderItem = workspaceDao.getWorkspaceItemById(folderId)
        if (folderItem != null && SmartCategoryHelper.isSmartAutoCreated(folderItem)) {
            val catId = SmartCategoryHelper.decodeCategoryId(folderItem.packageName)
            if (catId != null) {
                seedMutex.withLock {
                    addDismissedSmartCategoryLocked(catId)
                }
            }
        }
        val children = workspaceDao.getWorkspaceItemsByContainerSync(folderId.toInt())
        for (child in children) {
            workspaceDao.deleteWorkspaceItemById(child.id)
        }
        workspaceDao.deleteWorkspaceItemById(folderId)
        workspaceDao.deleteFolder(folderId)
    }

    suspend fun copyDrawerFolderToDesktop(
        drawerFolderId: Long,
        preferredScreenId: Int = 0
    ): Long? = withContext(Dispatchers.IO) {
        val folderItem = workspaceDao.getWorkspaceItemById(drawerFolderId) ?: return@withContext null
        val children = workspaceDao.getWorkspaceItemsByContainerSync(drawerFolderId.toInt())
            .sortedWith(compareBy({ it.rank }, { it.id }))
        ensurePagesExist()
        val pages = workspaceDao.getAllPagesSync()
        val orderedPages = buildList {
            pages.find { it.screenId == preferredScreenId }?.let { add(it) }
            addAll(pages.filter { it.screenId != preferredScreenId })
        }
        var targetScreenId = preferredScreenId
        var targetCell: IntArray? = null
        for (page in orderedPages) {
            val spot = findVacantCellOnPage(page.screenId, 1, 1)
            if (spot != null) {
                targetScreenId = page.screenId
                targetCell = spot
                break
            }
        }
        if (targetCell == null) {
            val newScreenId = (pages.maxOfOrNull { it.screenId } ?: 0) + 1
            val newOrder = (pages.maxOfOrNull { it.pageOrder } ?: 0) + 1
            workspaceDao.insertPage(WorkspacePageEntity(screenId = newScreenId, pageOrder = newOrder))
            targetScreenId = newScreenId
            targetCell = intArrayOf(0, 0)
        }

        val safeTitle = folderItem.title ?: "Folder"
        val desktopFolderId = workspaceDao.insertWorkspaceItem(
            WorkspaceItemEntity(
                itemType = WorkspaceItemEntity.ITEM_TYPE_FOLDER,
                title = safeTitle,
                container = WorkspaceItemEntity.CONTAINER_DESKTOP,
                screenId = targetScreenId,
                cellX = targetCell[0],
                cellY = targetCell[1]
            )
        )
        workspaceDao.insertFolder(
            com.example.launcherai.data.db.FolderEntity(
                folderId = desktopFolderId,
                title = safeTitle,
                screenId = targetScreenId,
                cellX = targetCell[0],
                cellY = targetCell[1]
            )
        )
        children.forEachIndexed { idx, child ->
            workspaceDao.insertWorkspaceItem(
                WorkspaceItemEntity(
                    itemType = WorkspaceItemEntity.ITEM_TYPE_APP,
                    packageName = child.packageName,
                    className = child.className,
                    container = desktopFolderId.toInt(),
                    screenId = 0,
                    cellX = 0,
                    cellY = 0,
                    spanX = 1,
                    spanY = 1,
                    rank = idx,
                    title = child.title
                )
            )
        }
        desktopFolderId
    }

    suspend fun getDrawerFoldersWithChildrenSync(): List<Pair<WorkspaceItemEntity, List<WorkspaceItemEntity>>> =
        withContext(Dispatchers.IO) {
            val drawerFolders = workspaceDao.getWorkspaceItemsByContainerSync(WorkspaceItemEntity.CONTAINER_DRAWER)
                .filter {
                    it.itemType == WorkspaceItemEntity.ITEM_TYPE_FOLDER &&
                        !SmartCategoryHelper.isFolderDisabled(it)
                }
                .sortedBy { (it.title ?: "").lowercase() }
            drawerFolders.map { folder ->
                val children = workspaceDao.getWorkspaceItemsByContainerSync(folder.id.toInt())
                    .sortedWith(compareBy({ it.rank }, { it.id }))
                folder to children
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
