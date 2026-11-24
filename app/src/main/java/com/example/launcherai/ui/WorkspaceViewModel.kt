package com.example.launcherai.ui

import android.app.Application
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.launcherai.AppInfo
import com.example.launcherai.LauncherApplication
import com.example.launcherai.data.db.WorkspaceItemEntity
import com.example.launcherai.data.db.WorkspacePageEntity
import com.example.launcherai.data.repository.WorkspaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

data class WorkspaceResolvedItem(
    val entity: WorkspaceItemEntity,
    val appInfo: AppInfo
)

data class WorkspaceFolderResolvedItem(
    val entity: WorkspaceItemEntity,
    val title: String,
    val children: List<WorkspaceResolvedItem>
)

data class WorkspaceWidgetResolvedItem(
    val entity: WorkspaceItemEntity,
    val appWidgetId: Int,
    val providerInfo: AppWidgetProviderInfo?
)

sealed class WorkspaceItemDisplay {
    data class App(val item: WorkspaceResolvedItem) : WorkspaceItemDisplay()
    data class Folder(val folder: WorkspaceFolderResolvedItem) : WorkspaceItemDisplay()
    data class Widget(val widget: WorkspaceWidgetResolvedItem) : WorkspaceItemDisplay()

    val entity: WorkspaceItemEntity
        get() = when (this) {
            is App -> item.entity
            is Folder -> folder.entity
            is Widget -> widget.entity
        }
}

class WorkspaceViewModel(
    application: Application,
    private val repository: WorkspaceRepository = (application as? LauncherApplication)?.repository
        ?: WorkspaceRepository(application)
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)

    val installedApps: StateFlow<List<AppInfo>> = repository.installedAppsFlow
    val gridConfig: StateFlow<com.example.launcherai.data.repository.WorkspaceGridConfig> = repository.gridConfigFlow
    val dockCapacity: StateFlow<Int> = repository.dockCapacityFlow

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow(AppInfo.CATEGORY_ALL)
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _hiddenPackages = MutableStateFlow<Set<String>>(emptySet())
    val hiddenPackages: StateFlow<Set<String>> = _hiddenPackages.asStateFlow()

    // Workspace pages from DB
    val pages: StateFlow<List<WorkspacePageEntity>> = repository.allPagesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf(WorkspacePageEntity(0, 0), WorkspacePageEntity(1, 1))
        )

    private val _defaultPageId = MutableStateFlow(repository.getDefaultPage())
    val defaultPageId: StateFlow<Int> = _defaultPageId.asStateFlow()

    fun addNewPage(onCreated: ((WorkspacePageEntity) -> Unit)? = null) {
        viewModelScope.launch {
            val newPage = repository.addNewPage()
            onCreated?.invoke(newPage)
        }
    }

    fun deletePage(screenId: Int, onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val success = repository.deletePage(screenId)
            _defaultPageId.value = repository.getDefaultPage()
            onResult?.invoke(success)
        }
    }

    fun reorderPages(screenIdOrder: List<Int>) {
        viewModelScope.launch {
            repository.reorderPages(screenIdOrder)
        }
    }

    fun setDefaultPage(screenId: Int) {
        repository.setDefaultPage(screenId)
        _defaultPageId.value = screenId
    }

    fun getValidDefaultPage(): Int {
        val currentDefault = repository.getDefaultPage()
        val currentPages = pages.value
        return if (currentPages.any { it.screenId == currentDefault }) {
            currentDefault
        } else {
            val fallback = currentPages.firstOrNull()?.screenId ?: 0
            repository.setDefaultPage(fallback)
            _defaultPageId.value = fallback
            fallback
        }
    }

    init {
        loadHiddenPackages()
    }

    private fun loadHiddenPackages() {
        val savedSet = prefs.getStringSet("key_hidden_packages", emptySet()) ?: emptySet()
        _hiddenPackages.value = savedSet
    }

    fun toggleHidePackage(packageName: String) {
        val current = _hiddenPackages.value.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        _hiddenPackages.value = current
        prefs.edit().putStringSet("key_hidden_packages", current).apply()
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    // Filtered apps for drawer based on search query, category, and hidden apps
    val drawerApps: StateFlow<List<AppInfo>> = combine(
        installedApps,
        _searchQuery,
        _selectedCategory,
        _hiddenPackages
    ) { apps, query, category, hidden ->
        var result = apps.filter { it.packageName !in hidden }

        if (category != AppInfo.CATEGORY_ALL) {
            result = result.filter { it.category == category }
        }

        if (query.isNotBlank()) {
            val lower = query.lowercase(Locale.ROOT)
            result = result.filter { it.label.lowercase(Locale.ROOT).contains(lower) }
        }

        result
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Top 5 suggested applications for top drawer row
    val suggestedApps: StateFlow<List<AppInfo>> = installedApps
        .map { apps ->
            val hidden = _hiddenPackages.value
            apps.filter { it.packageName !in hidden }.take(5)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Desktop items (Apps, Folders, Widgets) grouped by screenId
    val resolvedDisplayPageItemsMap: StateFlow<Map<Int, List<WorkspaceItemDisplay>>> = repository.allWorkspaceItemsFlow
        .map { items -> resolveAndGroupDisplayItems(getApplication(), items) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    // Resolved Dock items (Apps & Folders) for dynamic dock
    val resolvedDockItems: StateFlow<List<WorkspaceItemDisplay>> = repository.dockItemsFlow
        .map { items -> resolveDockDisplayItems(getApplication(), items) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Backward compatibility for single list collectors
    val desktopApps: StateFlow<List<AppInfo>> = repository.desktopItemsFlow
        .map { items -> resolveWorkspaceItemsToAppInfo(getApplication(), items) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val dockItems: StateFlow<List<WorkspaceItemEntity>> = repository.dockItemsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun refreshInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.refreshInstalledApps()
        }
    }

    fun saveCurrentPage(screenId: Int) {
        prefs.edit().putInt("key_current_screen_id", screenId).apply()
    }

    fun getSavedCurrentPage(): Int {
        return prefs.getInt("key_current_screen_id", 0)
    }

    fun updateItemPosition(id: Long, screenId: Int, cellX: Int, cellY: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateWorkspaceItemPosition(id, screenId, cellX, cellY)
        }
    }

    fun updateDockItemPosition(id: Long, cellX: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.getWorkspaceItemById(id) ?: return@launch
            repository.updateWorkspaceItem(
                existing.copy(
                    container = WorkspaceItemEntity.CONTAINER_DOCK,
                    screenId = 0,
                    cellX = cellX,
                    cellY = 0,
                    rank = cellX
                )
            )
        }
    }

    fun deleteWorkspaceItem(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteWorkspaceItemById(id)
            repository.evaluateAndCleanupFolder(id)
        }
    }

    fun evaluateAndCleanupFolder(folderId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.evaluateAndCleanupFolder(folderId)
        }
    }

    fun createFolder(existingItem: WorkspaceItemEntity, draggedItem: WorkspaceItemEntity, folderTitle: String = "Folder") {
        viewModelScope.launch(Dispatchers.IO) {
            repository.createFolder(existingItem, draggedItem, folderTitle)
        }
    }

    fun addItemToFolder(folderId: Long, item: WorkspaceItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addItemToFolder(folderId, item)
        }
    }

    fun moveItemOutOfFolder(item: WorkspaceItemEntity, targetScreenId: Int, targetCellX: Int, targetCellY: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.moveItemOutOfFolder(item, targetScreenId, targetCellX, targetCellY)
        }
    }

    fun updateFolderTitle(folderId: Long, newTitle: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateFolderTitle(folderId, newTitle)
        }
    }

    fun setGridConfig(cols: Int, rows: Int, onResult: (com.example.launcherai.data.repository.GridChangeResult) -> Unit) {
        viewModelScope.launch {
            val result = repository.setGridConfig(cols, rows)
            onResult(result)
        }
    }

    fun setDockCapacity(capacity: Int, onResult: (com.example.launcherai.data.repository.GridChangeResult) -> Unit) {
        viewModelScope.launch {
            val result = repository.setDockCapacity(capacity)
            onResult(result)
        }
    }

    fun addWorkspaceItem(item: WorkspaceItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addWorkspaceItem(item)
        }
    }

    fun addAppWidgetToWorkspace(
        appWidgetId: Int,
        providerInfo: AppWidgetProviderInfo,
        screenId: Int,
        cellX: Int,
        cellY: Int,
        spanX: Int,
        spanY: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val curGrid = repository.getGridConfig()
            val validSpanX = spanX.coerceIn(1, curGrid.cols)
            val validSpanY = spanY.coerceIn(1, curGrid.rows)

            val widgetItem = WorkspaceItemEntity(
                itemType = WorkspaceItemEntity.ITEM_TYPE_WIDGET,
                packageName = providerInfo.provider.packageName,
                className = providerInfo.provider.className,
                container = WorkspaceItemEntity.CONTAINER_DESKTOP,
                screenId = screenId,
                cellX = cellX,
                cellY = cellY,
                spanX = validSpanX,
                spanY = validSpanY,
                appWidgetId = appWidgetId,
                title = providerInfo.loadLabel(getApplication<Application>().packageManager)
            )
            repository.addWorkspaceItem(widgetItem)
        }
    }

    fun updateWidgetSpan(id: Long, newSpanX: Int, newSpanY: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val curGrid = repository.getGridConfig()
            val validSpanX = newSpanX.coerceIn(1, curGrid.cols)
            val validSpanY = newSpanY.coerceIn(1, curGrid.rows)

            val existing = repository.getWorkspaceItemById(id) ?: return@launch
            repository.updateWorkspaceItem(existing.copy(spanX = validSpanX, spanY = validSpanY))
        }
    }

    fun updateWidgetBounds(id: Long, newCellX: Int, newCellY: Int, newSpanX: Int, newSpanY: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val curGrid = repository.getGridConfig()
            val validSpanX = newSpanX.coerceIn(1, curGrid.cols)
            val validSpanY = newSpanY.coerceIn(1, curGrid.rows)
            val validCellX = newCellX.coerceIn(0, curGrid.cols - validSpanX)
            val validCellY = newCellY.coerceIn(0, curGrid.rows - validSpanY)

            val existing = repository.getWorkspaceItemById(id) ?: return@launch
            repository.updateWorkspaceItem(
                existing.copy(
                    cellX = validCellX,
                    cellY = validCellY,
                    spanX = validSpanX,
                    spanY = validSpanY
                )
            )
        }
    }

    fun deleteAppWidget(widgetResolved: WorkspaceWidgetResolvedItem, host: android.appwidget.AppWidgetHost?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (widgetResolved.appWidgetId != -1) {
                    host?.deleteAppWidgetId(widgetResolved.appWidgetId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            repository.deleteWorkspaceItemById(widgetResolved.entity.id)
        }
    }

    private suspend fun resolveDockDisplayItems(
        context: Context,
        dockItems: List<WorkspaceItemEntity>
    ): List<WorkspaceItemDisplay> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        dockItems.mapNotNull { item ->
            if (item.itemType == WorkspaceItemEntity.ITEM_TYPE_APP) {
                val resolvedApp = resolveSingleAppItem(pm, item) ?: return@mapNotNull null
                WorkspaceItemDisplay.App(resolvedApp)
            } else if (item.itemType == WorkspaceItemEntity.ITEM_TYPE_FOLDER) {
                val resolvedChildren = repository.getWorkspaceItemsByContainerSync(item.id.toInt())
                    .mapNotNull { resolveSingleAppItem(pm, it) }
                WorkspaceItemDisplay.Folder(
                    WorkspaceFolderResolvedItem(
                        entity = item,
                        title = item.title ?: "Folder",
                        children = resolvedChildren
                    )
                )
            } else null
        }
    }

    private suspend fun resolveAndGroupDisplayItems(
        context: Context,
        allItems: List<WorkspaceItemEntity>
    ): Map<Int, List<WorkspaceItemDisplay>> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val widgetManager = AppWidgetManager.getInstance(context)

        // Desktop items: container == CONTAINER_DESKTOP (-100)
        val desktopItems = allItems.filter { it.container == WorkspaceItemEntity.CONTAINER_DESKTOP }

        // Folder children grouped by container (folderId)
        val folderChildrenMap = allItems.filter { it.container > 0 }.groupBy { it.container }

        val displayItems = desktopItems.mapNotNull { desktopItem ->
            when (desktopItem.itemType) {
                WorkspaceItemEntity.ITEM_TYPE_APP -> {
                    val resolvedApp = resolveSingleAppItem(pm, desktopItem) ?: return@mapNotNull null
                    WorkspaceItemDisplay.App(resolvedApp)
                }
                WorkspaceItemEntity.ITEM_TYPE_FOLDER -> {
                    val childrenEntities = folderChildrenMap[desktopItem.id.toInt()] ?: emptyList()
                    val resolvedChildren = childrenEntities.mapNotNull { resolveSingleAppItem(pm, it) }
                    val folderResolved = WorkspaceFolderResolvedItem(
                        entity = desktopItem,
                        title = desktopItem.title ?: "Folder",
                        children = resolvedChildren
                    )
                    WorkspaceItemDisplay.Folder(folderResolved)
                }
                WorkspaceItemEntity.ITEM_TYPE_WIDGET -> {
                    val providerInfo = widgetManager.getAppWidgetInfo(desktopItem.appWidgetId)
                    val widgetResolved = WorkspaceWidgetResolvedItem(
                        entity = desktopItem,
                        appWidgetId = desktopItem.appWidgetId,
                        providerInfo = providerInfo
                    )
                    WorkspaceItemDisplay.Widget(widgetResolved)
                }
                else -> null
            }
        }

        displayItems.groupBy { it.entity.screenId }
    }

    private fun resolveSingleAppItem(
        pm: PackageManager,
        item: WorkspaceItemEntity
    ): WorkspaceResolvedItem? {
        val pkg = item.packageName ?: return null
        val cls = item.className ?: ""
        return try {
            val appInfo = pm.getApplicationInfo(pkg, 0)
            val label = item.title ?: pm.getApplicationLabel(appInfo).toString()
            val rawIcon = pm.getApplicationIcon(appInfo)

            val selectedIconPack = prefs.getString("key_selected_icon_pack", "") ?: ""
            val icon = if (selectedIconPack.isNotBlank()) {
                com.example.launcherai.icons.IconPackManager.getIconDrawable(
                    getApplication(),
                    selectedIconPack,
                    pkg,
                    rawIcon
                )
            } else rawIcon

            val app = AppInfo(
                label = label,
                packageName = pkg,
                activityName = cls,
                icon = icon,
                isSystem = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            )
            WorkspaceResolvedItem(item, app)
        } catch (e: PackageManager.NameNotFoundException) {
            if (item.title != null) {
                val fallbackApp = AppInfo(
                    label = item.title,
                    packageName = pkg,
                    activityName = cls,
                    icon = pm.defaultActivityIcon,
                    isSystem = false
                )
                WorkspaceResolvedItem(item, fallbackApp)
            } else null
        }
    }

    private suspend fun resolveWorkspaceItemsToAppInfo(
        context: Context,
        items: List<WorkspaceItemEntity>
    ): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        items.mapNotNull { item ->
            val pkg = item.packageName ?: return@mapNotNull null
            val cls = item.className ?: ""
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val label = item.title ?: pm.getApplicationLabel(appInfo).toString()
                val icon = pm.getApplicationIcon(appInfo)
                AppInfo(
                    label = label,
                    packageName = pkg,
                    activityName = cls,
                    icon = icon,
                    isSystem = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                )
            } catch (e: PackageManager.NameNotFoundException) {
                if (item.title != null) {
                    AppInfo(
                        label = item.title,
                        packageName = pkg,
                        activityName = cls,
                        icon = pm.defaultActivityIcon,
                        isSystem = false
                    )
                } else null
            }
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WorkspaceViewModel(application) as T
        }
    }
}
