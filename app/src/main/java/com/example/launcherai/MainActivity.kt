package com.example.launcherai

import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.launcherai.data.db.WorkspaceItemEntity
import com.example.launcherai.dragndrop.DragController
import com.example.launcherai.gestures.GestureAction
import com.example.launcherai.gestures.GestureHandler
import com.example.launcherai.ui.DragLayer
import com.example.launcherai.ui.DockLayout
import com.example.launcherai.ui.FastScroller
import com.example.launcherai.ui.HiddenAppsDialog
import com.example.launcherai.ui.LauncherSettingsActivity
import com.example.launcherai.ui.Workspace
import com.example.launcherai.ui.WorkspaceFolderResolvedItem
import com.example.launcherai.ui.WorkspaceResolvedItem
import com.example.launcherai.ui.WorkspaceViewModel
import com.example.launcherai.widgets.LauncherAppWidgetHost
import com.example.launcherai.widgets.WidgetPickerDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val viewModel: WorkspaceViewModel by viewModels {
        WorkspaceViewModel.Factory(application)
    }

    private lateinit var appWidgetManager: AppWidgetManager
    private lateinit var appWidgetHost: LauncherAppWidgetHost
    private lateinit var gestureHandler: GestureHandler

    private var pendingWidgetIdToBind: Int = -1
    private var pendingWidgetInfo: AppWidgetProviderInfo? = null
    private var pendingWidgetSpanX: Int = 1
    private var pendingWidgetSpanY: Int = 1

    private lateinit var dragLayer: DragLayer
    private lateinit var dragController: DragController
    private lateinit var workspace: Workspace
    private lateinit var dockHotseat: DockLayout
    private lateinit var rvDrawerApps: RecyclerView
    private lateinit var layoutAppDrawerOverlay: LinearLayout
    private lateinit var etDrawerSearch: EditText
    private lateinit var tvDrawerEmpty: TextView
    private lateinit var tvFastScrollBubble: TextView

    private lateinit var folderOverlay: FrameLayout
    private lateinit var etFolderTitle: EditText
    private lateinit var btnCloseFolder: ImageView
    private lateinit var rvFolderApps: RecyclerView
    private lateinit var folderAppsAdapter: AppsAdapter
    private var activeFolderId: Long? = null

    private lateinit var drawerAdapter: AppsAdapter
    private lateinit var fastScroller: FastScroller

    private val bindWidgetLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            onWidgetBoundSuccess()
        } else {
            if (pendingWidgetIdToBind != -1) {
                appWidgetHost.deleteAppWidgetId(pendingWidgetIdToBind)
            }
            resetPendingWidgetState()
        }
    }

    private val configureWidgetLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val info = pendingWidgetInfo
            if (info != null && pendingWidgetIdToBind != -1) {
                val curPage = workspace.getCurrentPage()
                val cellLayout = workspace.getActiveCellLayout()
                val vacant = cellLayout?.findVacantCell(pendingWidgetSpanX, pendingWidgetSpanY) ?: intArrayOf(0, 0)
                viewModel.addAppWidgetToWorkspace(
                    appWidgetId = pendingWidgetIdToBind,
                    providerInfo = info,
                    screenId = curPage,
                    cellX = vacant[0],
                    cellY = vacant[1],
                    spanX = pendingWidgetSpanX,
                    spanY = pendingWidgetSpanY
                )
            }
        } else {
            if (pendingWidgetIdToBind != -1) {
                appWidgetHost.deleteAppWidgetId(pendingWidgetIdToBind)
            }
        }
        resetPendingWidgetState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (checkFirstRunWizard()) return

        com.example.launcherai.ui.SystemBarHelper.applySystemBars(this, isLightTheme = false, transparentBars = true)
        setContentView(R.layout.activity_main)

        appWidgetManager = AppWidgetManager.getInstance(this)
        appWidgetHost = LauncherAppWidgetHost(this)

        initViews()
        setupListeners()
        setupGestureHandler()
        setupBackHandler()
        observeViewModel()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        checkFirstRunWizard()
    }

    override fun onResume() {
        super.onResume()
        refreshGridAndSettings()
        if (intent.getBooleanExtra("open_drawer", false)) {
            openAppDrawer()
        }
    }

    private fun checkFirstRunWizard(): Boolean {
        if (intent.getBooleanExtra("reset_setup", false)) {
            com.example.launcherai.ui.SetupFlowUtils.resetSetup(this)
        }
        val skipWizard = intent.getBooleanExtra("skip_wizard", false)
        val forceSetup = intent.getBooleanExtra("force_setup", false)
        if (forceSetup || com.example.launcherai.ui.SetupFlowUtils.shouldLaunchSetup(this, skipWizard)) {
            startActivity(Intent(this, com.example.launcherai.ui.SetupActivity::class.java))
            finish()
            return true
        }
        return false
    }

    override fun onStop() {
        super.onStop()
        appWidgetHost.stopListening()
    }

    private fun refreshGridAndSettings() {
        val gridConfig = viewModel.gridConfig.value
        val activeCell = workspace.getActiveCellLayout()
        if (activeCell != null) {
            activeCell.countX = gridConfig.cols
            activeCell.countY = gridConfig.rows
        }

        applySearchBarPlacement()
        viewModel.refreshInstalledApps()
    }

    private fun applySearchBarPlacement() {
        val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        val placement = prefs.getString("key_search_bar_placement_home", "DOCK_BELOW") ?: "DOCK_BELOW"
        val searchBar = findViewById<View>(R.id.barHomeSearch) ?: return
        val desktopLayout = findViewById<LinearLayout>(R.id.layoutDesktop) ?: return

        when (placement) {
            "TOP" -> {
                searchBar.visibility = View.VISIBLE
                desktopLayout.removeView(searchBar)
                desktopLayout.addView(searchBar, 0)
            }
            "HIDDEN" -> {
                searchBar.visibility = View.GONE
            }
            else -> { // "DOCK_BELOW"
                searchBar.visibility = View.VISIBLE
                desktopLayout.removeView(searchBar)
                desktopLayout.addView(searchBar)
            }
        }
    }

    private fun setupGestureHandler() {
        gestureHandler = GestureHandler(this) { action ->
            executeGestureAction(action)
        }

        val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        gestureHandler.swipeUpAction = GestureAction.fromName(
            prefs.getString("key_gesture_swipe_up", "") ?: "",
            GestureAction.APP_DRAWER
        )
        gestureHandler.swipeDownAction = GestureAction.fromName(
            prefs.getString("key_gesture_swipe_down", "") ?: "",
            GestureAction.EXPAND_NOTIFICATIONS
        )
    }

    private fun executeGestureAction(action: GestureAction) {
        when (action) {
            GestureAction.APP_DRAWER -> openAppDrawer()
            GestureAction.SETTINGS -> startActivity(Intent(this, LauncherSettingsActivity::class.java))
            GestureAction.SEARCH -> {
                openAppDrawer()
                etDrawerSearch.requestFocus()
            }
            else -> GestureHandler.executeSystemAction(this, action)
        }
    }

    private fun initViews() {
        dragLayer = findViewById(R.id.rootLauncher)
        workspace = findViewById(R.id.workspace)
        rvDrawerApps = findViewById(R.id.rvDrawerApps)
        layoutAppDrawerOverlay = findViewById(R.id.layoutAppDrawerOverlay)
        etDrawerSearch = findViewById(R.id.etDrawerSearch)
        tvDrawerEmpty = findViewById(R.id.tvDrawerEmpty)
        tvFastScrollBubble = findViewById(R.id.tvFastScrollBubble)

        folderOverlay = findViewById(R.id.folderOverlay)
        etFolderTitle = findViewById(R.id.etFolderTitle)
        btnCloseFolder = findViewById(R.id.btnCloseFolder)
        rvFolderApps = findViewById(R.id.rvFolderApps)

        dockHotseat = findViewById(R.id.dockHotseat)
        dockHotseat.setListeners(
            onAppClick = { appInfo ->
                AppManager.launchApp(this@MainActivity, appInfo)
            },
            onFolderClick = { folderResolved ->
                openFolderOverlay(folderResolved)
            },
            onAppDrawerClick = {
                openAppDrawer()
            },
            onAppLongClick = { view, item, touchX, touchY ->
                dragController.startDrag(view, item, touchX, touchY)
                true
            },
            onFolderLongClick = { view, folderResolved, touchX, touchY ->
                val item = WorkspaceResolvedItem(
                    entity = folderResolved.entity,
                    appInfo = AppInfo(
                        label = folderResolved.title,
                        packageName = "",
                        activityName = ""
                    )
                )
                dragController.startDrag(view, item, touchX, touchY)
                true
            }
        )

        val dropTargetBarView = findViewById<View>(R.id.dropTargetBar)
        dragController = DragController(
            context = this,
            dragLayer = dragLayer,
            workspace = workspace,
            dockLayout = dockHotseat,
            dropTargetBar = dropTargetBarView,
            viewModel = viewModel,
            appWidgetHost = appWidgetHost
        )
        dropTargetBarView?.let { dragController.initDropTargetBar(it) }
        dragLayer.dragController = dragController

        workspace.setOnLongClickListener {
            showWidgetPicker()
            true
        }

        // Save active page index when page changes
        workspace.setOnPageChangeListener { pageIndex ->
            viewModel.saveCurrentPage(pageIndex)
            updateWallpaperOffset(pageIndex)
        }

        // Folder popup overlay controls
        folderOverlay.setOnClickListener { closeFolderOverlay() }
        btnCloseFolder.setOnClickListener { closeFolderOverlay() }

        etFolderTitle.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val newTitle = etFolderTitle.text.toString().trim()
                activeFolderId?.let { id ->
                    if (newTitle.isNotBlank()) {
                        viewModel.updateFolderTitle(id, newTitle)
                    }
                }
            }
            false
        }

        // App Drawer Grid (5 columns - Ref 11)
        rvDrawerApps.layoutManager = GridLayoutManager(this, 5)
        drawerAdapter = AppsAdapter(
            apps = emptyList(),
            onAppClick = { app ->
                AppManager.launchApp(this, app)
                closeAppDrawer()
            },
            onAppLongClick = { view, app, touchX, touchY ->
                closeAppDrawer()
                val curPage = workspace.getCurrentPage()
                val cellLayout = workspace.getActiveCellLayout()
                val vacant = cellLayout?.findVacantCell(1, 1) ?: intArrayOf(0, 0)

                val item = WorkspaceResolvedItem(
                    entity = WorkspaceItemEntity(
                        itemType = WorkspaceItemEntity.ITEM_TYPE_APP,
                        packageName = app.packageName,
                        className = app.activityName,
                        container = WorkspaceItemEntity.CONTAINER_DESKTOP,
                        screenId = curPage,
                        cellX = vacant[0],
                        cellY = vacant[1],
                        title = app.label
                    ),
                    appInfo = app
                )
                dragController.startDrag(view, item, touchX, touchY)
                true
            }
        )
        rvDrawerApps.adapter = drawerAdapter

        // FastScroller
        fastScroller = FastScroller(rvDrawerApps, tvFastScrollBubble)
    }

    private fun updateWallpaperOffset(pageIndex: Int) {
        try {
            val totalPages = workspace.getPageCount()
            if (totalPages > 1) {
                val offset = pageIndex.toFloat() / (totalPages - 1)
                val wm = WallpaperManager.getInstance(this)
                val token = window.decorView.windowToken
                if (token != null) {
                    wm.setWallpaperOffsets(token, offset, 0f)
                }
            }
        } catch (e: Exception) {
            // Wallpaper offsets unsupported on some custom ROMs
        }
    }

    private fun setupListeners() {
        // Bottom pale peach search bar listeners (Ref 10)
        findViewById<View>(R.id.btnHomeSearchGrid)?.setOnClickListener {
            openAppDrawer()
        }

        findViewById<View>(R.id.btnHomeSearchMic)?.setOnClickListener {
            try {
                val voiceIntent = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                }
                startActivity(voiceIntent)
            } catch (e: Exception) {
                try {
                    val webSearchIntent = Intent(Intent.ACTION_WEB_SEARCH)
                    startActivity(webSearchIntent)
                } catch (e2: Exception) {
                    Toast.makeText(this, "Voice search not available on this device", Toast.LENGTH_SHORT).show()
                }
            }
        }

        findViewById<View>(R.id.barHomeSearch)?.apply {
            setOnClickListener {
                openAppDrawer()
            }
            setOnLongClickListener {
                startActivity(Intent(this@MainActivity, NovaAiChatActivity::class.java))
                true
            }
        }

        // Suggestion Chips (Ref 11)
        findViewById<View>(R.id.chipLauncherSettings)?.setOnClickListener {
            startActivity(Intent(this, LauncherSettingsActivity::class.java))
        }

        // Drawer Controls
        etDrawerSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (folderOverlay.visibility == View.VISIBLE) {
                    closeFolderOverlay()
                } else if (layoutAppDrawerOverlay.visibility == View.VISIBLE) {
                    closeAppDrawer()
                } else if (dragController.isDragging) {
                    dragController.cancelDrag()
                }
            }
        })
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Multi-page Workspace collector (Apps, Folders & Widgets)
                launch {
                    combine(
                        viewModel.pages,
                        viewModel.resolvedDisplayPageItemsMap,
                        viewModel.gridConfig
                    ) { pages, displayItemsMap, gridConfig ->
                        Triple(pages, displayItemsMap, gridConfig)
                    }.collect { (pages, displayItemsMap, gridConfig) ->
                        workspace.updateDisplayPages(
                            pages = pages,
                            pageDisplayItemsMap = displayItemsMap,
                            gridConfig = gridConfig,
                            appWidgetHost = appWidgetHost,
                            onAppClick = { appInfo ->
                                AppManager.launchApp(this@MainActivity, appInfo)
                            },
                            onFolderClick = { folderResolved ->
                                openFolderOverlay(folderResolved)
                            },
                            onAppLongClick = { view, item, touchX, touchY ->
                                dragController.startDrag(view, item, touchX, touchY)
                                true
                            },
                            onFolderLongClick = { view, folderResolved, touchX, touchY ->
                                val item = WorkspaceResolvedItem(
                                    entity = folderResolved.entity,
                                    appInfo = AppInfo(
                                        label = folderResolved.title,
                                        packageName = "",
                                        activityName = ""
                                    )
                                )
                                dragController.startDrag(view, item, touchX, touchY)
                                true
                            },
                            onWidgetLongClick = { view, widgetResolved, touchX, touchY ->
                                val item = WorkspaceResolvedItem(
                                    entity = widgetResolved.entity,
                                    appInfo = AppInfo(
                                        label = widgetResolved.entity.title ?: "Widget",
                                        packageName = widgetResolved.entity.packageName ?: "",
                                        activityName = ""
                                    )
                                )
                                dragController.startDrag(view, item, touchX, touchY)
                                true
                            }
                        )
                        val activeCell = workspace.getActiveCellLayout()
                        if (activeCell != null) {
                            activeCell.countX = gridConfig.cols
                            activeCell.countY = gridConfig.rows
                        }
                        val savedPage = viewModel.getSavedCurrentPage()
                        workspace.setCurrentPage(savedPage, smoothScroll = false)
                    }
                }

                // Resolved Dock items collector (Apps & Folders + Capacity)
                launch {
                    combine(viewModel.resolvedDockItems, viewModel.dockCapacity) { dockItems, capacity ->
                        Pair(dockItems, capacity)
                    }.collect { (dockItems, capacity) ->
                        dockHotseat.updateDockItems(dockItems, capacity)
                    }
                }

                // App Drawer filtered collector
                launch {
                    viewModel.drawerApps.collect { drawerApps ->
                        drawerAdapter.updateApps(drawerApps)
                        fastScroller.updateApps(drawerApps)
                        val query = viewModel.searchQuery.value
                        tvDrawerEmpty.visibility = if (drawerApps.isEmpty() && query.isNotBlank()) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    private fun showWidgetPicker() {
        val providers = appWidgetManager.installedProviders
        if (providers.isEmpty()) {
            Toast.makeText(this, "No installed widget providers found", Toast.LENGTH_SHORT).show()
            return
        }
        val dialog = WidgetPickerDialog(this, providers) { providerInfo, spanX, spanY ->
            requestAddWidget(providerInfo, spanX, spanY)
        }
        dialog.show()
    }

    private fun showHiddenAppsDialog() {
        val dialog = HiddenAppsDialog(
            context = this,
            installedApps = viewModel.installedApps.value,
            hiddenPackages = viewModel.hiddenPackages.value,
            onToggleHide = { packageName ->
                viewModel.toggleHidePackage(packageName)
            }
        )
        dialog.show()
    }

    private fun requestAddWidget(providerInfo: AppWidgetProviderInfo, spanX: Int, spanY: Int) {
        val appWidgetId = appWidgetHost.allocateAppWidgetId()
        pendingWidgetIdToBind = appWidgetId
        pendingWidgetInfo = providerInfo
        pendingWidgetSpanX = spanX
        pendingWidgetSpanY = spanY

        val bound = appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, providerInfo.provider)
        if (bound) {
            onWidgetBoundSuccess()
        } else {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, providerInfo.provider)
            }
            bindWidgetLauncher.launch(intent)
        }
    }

    private fun onWidgetBoundSuccess() {
        val info = pendingWidgetInfo ?: return
        val appWidgetId = pendingWidgetIdToBind

        if (info.configure != null) {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                component = info.configure
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            configureWidgetLauncher.launch(intent)
        } else {
            val curPage = workspace.getCurrentPage()
            val cellLayout = workspace.getActiveCellLayout()
            val vacant = cellLayout?.findVacantCell(pendingWidgetSpanX, pendingWidgetSpanY) ?: intArrayOf(0, 0)
            viewModel.addAppWidgetToWorkspace(
                appWidgetId = appWidgetId,
                providerInfo = info,
                screenId = curPage,
                cellX = vacant[0],
                cellY = vacant[1],
                spanX = pendingWidgetSpanX,
                spanY = pendingWidgetSpanY
            )
            resetPendingWidgetState()
        }
    }

    private fun resetPendingWidgetState() {
        pendingWidgetIdToBind = -1
        pendingWidgetInfo = null
        pendingWidgetSpanX = 1
        pendingWidgetSpanY = 1
    }

    private fun openFolderOverlay(folderResolved: WorkspaceFolderResolvedItem) {
        activeFolderId = folderResolved.entity.id
        etFolderTitle.setText(folderResolved.title)

        rvFolderApps.layoutManager = GridLayoutManager(this, 3)
        folderAppsAdapter = AppsAdapter(
            apps = folderResolved.children.map { it.appInfo },
            onAppClick = { appInfo ->
                AppManager.launchApp(this, appInfo)
                closeFolderOverlay()
            }
        )
        rvFolderApps.adapter = folderAppsAdapter

        folderOverlay.visibility = View.VISIBLE
        folderOverlay.alpha = 0f
        folderOverlay.animate().alpha(1f).setDuration(200).start()
    }

    private fun closeFolderOverlay() {
        val newTitle = etFolderTitle.text.toString().trim()
        activeFolderId?.let { id ->
            if (newTitle.isNotBlank()) {
                viewModel.updateFolderTitle(id, newTitle)
            }
        }

        folderOverlay.animate().alpha(0f).setDuration(150).withEndAction {
            folderOverlay.visibility = View.GONE
            activeFolderId = null
        }.start()
    }

    private fun openAppDrawer() {
        layoutAppDrawerOverlay.visibility = View.VISIBLE
        layoutAppDrawerOverlay.alpha = 0f
        layoutAppDrawerOverlay.animate().alpha(1f).setDuration(200).start()

        applyDrawerPlacementAndSettings()

        val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        val showKeyboard = prefs.getBoolean("key_drawer_show_keyboard", true)
        if (showKeyboard) {
            etDrawerSearch.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            imm?.showSoftInput(etDrawerSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun applyDrawerPlacementAndSettings() {
        val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        val drawerPlacement = prefs.getString("key_search_bar_placement_drawer", "Top") ?: "Top"
        val showFrequent = prefs.getBoolean("key_drawer_frequent_apps", true)

        findViewById<View>(R.id.layoutFrequentAppsHeader)?.visibility = if (showFrequent) View.VISIBLE else View.GONE

        val searchContainer = findViewById<View>(R.id.barDrawerSearchContainer) ?: return
        val drawerLayout = findViewById<LinearLayout>(R.id.layoutAppDrawerOverlay) ?: return

        if (drawerPlacement.contains("Bottom", ignoreCase = true)) {
            drawerLayout.removeView(searchContainer)
            drawerLayout.addView(searchContainer)
        } else {
            drawerLayout.removeView(searchContainer)
            drawerLayout.addView(searchContainer, 0)
        }
    }

    private fun closeAppDrawer() {
        layoutAppDrawerOverlay.animate().alpha(0f).setDuration(150).withEndAction {
            layoutAppDrawerOverlay.visibility = View.GONE
            etDrawerSearch.setText("")
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
