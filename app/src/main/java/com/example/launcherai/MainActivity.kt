package com.example.launcherai

import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
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
import com.example.launcherai.ui.PageIndicatorView
import com.example.launcherai.ui.Workspace
import com.example.launcherai.ui.WorkspaceFolderResolvedItem
import com.example.launcherai.ui.WorkspacePageTransformer
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
    private var horizontalSnapHelper: PagerSnapHelper? = null
    private var selectedCategory: String = "All"
    private var fullDrawerAppsList: List<AppInfo> = emptyList()

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
        applyDrawerPlacementAndSettings()
        applyScrollAndTransitionSettings()
        applyAdvancedSettings()
        if (::dockHotseat.isInitialized) {
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            val dockEnabled = prefs.getBoolean("key_dock_enabled", true)
            dockHotseat.visibility = if (dockEnabled) View.VISIBLE else View.GONE
            dockHotseat.refreshDock()
        }
        workspace.adapter?.notifyDataSetChanged()
        if (::drawerAdapter.isInitialized) {
            drawerAdapter.notifyDataSetChanged()
        }
        viewModel.refreshInstalledApps()
    }

    private fun applySearchBarPlacement() {
        val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        val placement = prefs.getString("key_search_bar_placement_home", "DOCK_BELOW") ?: "DOCK_BELOW"
        val searchBar = findViewById<View>(R.id.barHomeSearch) ?: return
        val desktopLayout = findViewById<LinearLayout>(R.id.layoutDesktop) ?: return
        val dockHotseat = findViewById<View>(R.id.dockHotseat)

        when (placement) {
            "TOP" -> {
                searchBar.visibility = View.VISIBLE
                desktopLayout.removeView(searchBar)
                desktopLayout.addView(searchBar, 0)
            }
            "DOCK_ABOVE" -> {
                searchBar.visibility = View.VISIBLE
                desktopLayout.removeView(searchBar)
                if (dockHotseat != null) {
                    val dockIdx = desktopLayout.indexOfChild(dockHotseat)
                    desktopLayout.addView(searchBar, dockIdx.coerceAtLeast(0))
                } else {
                    desktopLayout.addView(searchBar)
                }
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
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            if (prefs.getBoolean("key_desktop_lock", false)) {
                Toast.makeText(this, "Home screen layout is locked", Toast.LENGTH_SHORT).show()
                return@setOnLongClickListener true
            }
            showWidgetPicker()
            true
        }

        // Save active page index when page changes
        workspace.setOnPageChangeListener { pageIndex ->
            viewModel.saveCurrentPage(pageIndex)
            updateWallpaperOffset(pageIndex)
        }
        applyScrollAndTransitionSettings()

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

    private fun applyScrollAndTransitionSettings() {
        if (!::workspace.isInitialized) return
        val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)

        // 1. Transition effect
        val effectName = prefs.getString("key_page_transition_effect", "SIMPLE") ?: "SIMPLE"
        val effect = WorkspacePageTransformer.Effect.fromString(effectName)
        workspace.setPageTransformer(WorkspacePageTransformer(effect))

        // 2. Infinite scroll
        val infiniteScroll = prefs.getBoolean("key_infinite_scroll", false)
        workspace.isInfiniteScrollEnabled = infiniteScroll

        // 3. Page indicator style & color
        val indicatorStyleName = prefs.getString("key_page_indicator_style", "DOTS") ?: "DOTS"
        val indicatorStyle = PageIndicatorView.Style.fromString(indicatorStyleName)
        val indicatorColor = prefs.getInt("key_page_indicator_color", Color.WHITE)
        workspace.setPageIndicatorStyle(indicatorStyle)
        workspace.setPageIndicatorColor(indicatorColor)

        // 4. Wallpaper scroll mode parallax binding & Wallpaper Zoom Effects
        val wpScrollMode = prefs.getString("key_wallpaper_scroll", "ON") ?: "ON"
        val wpZoomEffects = prefs.getBoolean("key_wallpaper_zoom_effects", false)
        workspace.setOnPageScrolledListener { position, positionOffset, _ ->
            val totalPages = workspace.getPageCount()
            if (totalPages > 1) {
                val fraction = ((position + positionOffset) / (totalPages - 1)).coerceIn(0f, 1f)
                val offset = when (wpScrollMode) {
                    "OFF" -> 0.5f
                    "REVERSE" -> 1.0f - fraction
                    else -> fraction
                }
                try {
                    val wm = WallpaperManager.getInstance(this)
                    val token = window.decorView.windowToken
                    if (token != null) {
                        wm.setWallpaperOffsets(token, offset, 0.5f)
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                            if (wpZoomEffects) {
                                val delta = kotlin.math.abs(positionOffset - 0.5f)
                                val zoom = (delta * 0.15f).coerceIn(0f, 0.15f)
                                applyWallpaperZoom(token, zoom)
                            } else {
                                applyWallpaperZoom(token, 0f)
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun applyWallpaperZoom(token: android.os.IBinder?, zoom: Float) {
        if (token == null) return
        try {
            val wm = WallpaperManager.getInstance(this)
            val method = wm.javaClass.getMethod("setWallpaperZoomOut", android.os.IBinder::class.java, Float::class.javaPrimitiveType)
            method.invoke(wm, token, zoom)
        } catch (_: Exception) {}
    }

    private fun applyAdvancedSettings() {
        val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        val showShadow = prefs.getBoolean("key_desktop_shadow", false)
        findViewById<View>(R.id.viewTopShadow)?.visibility = if (showShadow) View.VISIBLE else View.GONE
        findViewById<View>(R.id.viewBottomShadow)?.visibility = if (showShadow) View.VISIBLE else View.GONE
    }

    private fun updateWallpaperOffset(pageIndex: Int) {
        try {
            val totalPages = workspace.getPageCount()
            if (totalPages > 1) {
                val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                val wpScrollMode = prefs.getString("key_wallpaper_scroll", "ON") ?: "ON"
                val fraction = (pageIndex.toFloat() / (totalPages - 1)).coerceIn(0f, 1f)
                val offset = when (wpScrollMode) {
                    "OFF" -> 0.5f
                    "REVERSE" -> 1.0f - fraction
                    else -> fraction
                }
                val wm = WallpaperManager.getInstance(this)
                val token = window.decorView.windowToken
                if (token != null) {
                    wm.setWallpaperOffsets(token, offset, 0.5f)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        applyWallpaperZoom(token, 0f)
                    }
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
                val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                val searchMode = prefs.getString("key_search_mode", "Immersive") ?: "Immersive"
                if (searchMode.equals("Standard", ignoreCase = true)) {
                    try {
                        val intent = Intent(Intent.ACTION_WEB_SEARCH)
                        startActivity(intent)
                    } catch (e: Exception) {
                        openAppDrawer()
                    }
                } else {
                    openAppDrawer()
                }
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
                        fullDrawerAppsList = drawerApps
                        applyDrawerAppFilter()
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
            etDrawerSearch.post {
                etDrawerSearch.requestFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                imm?.showSoftInput(etDrawerSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            }
        } else {
            etDrawerSearch.clearFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            imm?.hideSoftInputFromWindow(etDrawerSearch.windowToken, 0)
        }
    }

    private fun applyDrawerPlacementAndSettings() {
        val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        val drawerPlacement = prefs.getString("key_search_bar_placement_drawer", "Top") ?: "Top"
        val showFrequent = prefs.getBoolean("key_drawer_frequent_apps", true)
        val drawerStyle = prefs.getString("key_app_drawer_style", "Vertical") ?: "Vertical"
        val smartFolders = prefs.getBoolean("key_drawer_smart_folders", true)

        findViewById<View>(R.id.layoutFrequentAppsHeader)?.visibility = if (showFrequent) View.VISIBLE else View.GONE

        val searchContainer = findViewById<View>(R.id.barDrawerSearchContainer) ?: return
        val drawerLayout = findViewById<LinearLayout>(R.id.layoutAppDrawerOverlay) ?: return

        when {
            drawerPlacement.equals("Hidden", ignoreCase = true) -> {
                searchContainer.visibility = View.GONE
            }
            drawerPlacement.equals("Bottom", ignoreCase = true) -> {
                searchContainer.visibility = View.VISIBLE
                drawerLayout.removeView(searchContainer)
                drawerLayout.addView(searchContainer)
            }
            else -> { // "Top"
                searchContainer.visibility = View.VISIBLE
                drawerLayout.removeView(searchContainer)
                drawerLayout.addView(searchContainer, 0)
            }
        }

        // Apply App Drawer Style (Vertical, List, Horizontal)
        when (drawerStyle) {
            "List" -> {
                horizontalSnapHelper?.attachToRecyclerView(null)
                horizontalSnapHelper = null
                rvDrawerApps.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
                drawerAdapter.setDisplayMode(AppsAdapter.DisplayMode.LIST)
                fastScroller.isEnabled = true
            }
            "Horizontal" -> {
                rvDrawerApps.layoutManager = GridLayoutManager(this, 5, RecyclerView.HORIZONTAL, false)
                drawerAdapter.setDisplayMode(AppsAdapter.DisplayMode.HORIZONTAL)
                fastScroller.isEnabled = false
                if (horizontalSnapHelper == null) {
                    val snap = PagerSnapHelper()
                    snap.attachToRecyclerView(rvDrawerApps)
                    horizontalSnapHelper = snap
                }
            }
            else -> { // "Vertical"
                horizontalSnapHelper?.attachToRecyclerView(null)
                horizontalSnapHelper = null
                rvDrawerApps.layoutManager = GridLayoutManager(this, 5, RecyclerView.VERTICAL, false)
                drawerAdapter.setDisplayMode(AppsAdapter.DisplayMode.GRID)
                fastScroller.isEnabled = true
            }
        }
        drawerAdapter.notifyDataSetChanged()

        // Smart Folders Category Chips
        val chipsContainer = findViewById<LinearLayout>(R.id.containerSuggestionChips)
        if (chipsContainer != null) {
            setupSmartFolderChips(chipsContainer, smartFolders)
        }
    }

    private fun setupSmartFolderChips(container: LinearLayout, isEnabled: Boolean) {
        val childCount = container.childCount
        if (childCount > 1) {
            container.removeViews(1, childCount - 1)
        }

        if (!isEnabled) {
            selectedCategory = "All"
            applyDrawerAppFilter()
            return
        }

        val categories = listOf("All", "Social", "Tools", "Media", "Games", "Work")
        val density = resources.displayMetrics.density

        categories.forEach { category ->
            val isSelected = (category == selectedCategory)
            val chipView = TextView(this).apply {
                text = category
                textSize = 13f
                setPadding((14 * density).toInt(), (6 * density).toInt(), (14 * density).toInt(), (6 * density).toInt())
                setBackgroundResource(if (isSelected) R.drawable.bg_nova_chip_active else R.drawable.bg_nova_chip_inactive)
                setTextColor(if (isSelected) android.graphics.Color.WHITE else ContextCompat.getColor(this@MainActivity, R.color.nova_text_brown_primary))
                isClickable = true
                isFocusable = true
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    (32 * density).toInt()
                ).apply {
                    marginStart = (8 * density).toInt()
                }
                layoutParams = lp

                setOnClickListener {
                    selectedCategory = category
                    setupSmartFolderChips(container, true)
                    applyDrawerAppFilter()
                }
            }
            container.addView(chipView)
        }
    }

    private fun filterAppsByCategory(apps: List<AppInfo>, category: String): List<AppInfo> {
        if (category == "All") return apps
        return apps.filter { app ->
            val pkg = app.packageName.lowercase()
            val label = app.label.lowercase()
            when (category) {
                "Social" -> {
                    pkg.contains("whatsapp") || pkg.contains("telegram") || pkg.contains("facebook") ||
                    pkg.contains("instagram") || pkg.contains("twitter") || pkg.contains("messenger") ||
                    pkg.contains("snapchat") || pkg.contains("discord") || pkg.contains("viber") ||
                    pkg.contains("message") || pkg.contains("dialer") || pkg.contains("contact") ||
                    label.contains("message") || label.contains("chat") || label.contains("phone")
                }
                "Tools" -> {
                    pkg.contains("calculator") || pkg.contains("calendar") || pkg.contains("clock") ||
                    pkg.contains("settings") || pkg.contains("file") || pkg.contains("camera") ||
                    pkg.contains("chrome") || pkg.contains("browser") || pkg.contains("note") ||
                    pkg.contains("keep") || pkg.contains("deskclock") || label.contains("tool") ||
                    label.contains("calc") || label.contains("clock") || label.contains("setting")
                }
                "Media" -> {
                    pkg.contains("youtube") || pkg.contains("music") || pkg.contains("video") ||
                    pkg.contains("gallery") || pkg.contains("photo") || pkg.contains("spotify") ||
                    pkg.contains("player") || pkg.contains("netflix") || label.contains("music") ||
                    label.contains("video") || label.contains("media") || label.contains("gallery")
                }
                "Games" -> {
                    pkg.contains("game") || pkg.contains("play.games") || label.contains("game")
                }
                "Work" -> {
                    pkg.contains("mail") || pkg.contains("gmail") || pkg.contains("outlook") ||
                    pkg.contains("docs") || pkg.contains("sheets") || pkg.contains("slides") ||
                    pkg.contains("office") || pkg.contains("pdf") || pkg.contains("drive") ||
                    pkg.contains("meet") || pkg.contains("zoom") || pkg.contains("teams") ||
                    label.contains("mail") || label.contains("drive") || label.contains("doc")
                }
                else -> true
            }
        }
    }

    private fun applyDrawerAppFilter() {
        val filtered = filterAppsByCategory(fullDrawerAppsList, selectedCategory)
        drawerAdapter.updateApps(filtered)
        fastScroller.updateApps(filtered)
        val query = viewModel.searchQuery.value
        tvDrawerEmpty.visibility = if (filtered.isEmpty() && query.isNotBlank()) View.VISIBLE else View.GONE

        val chipsContainer = findViewById<LinearLayout>(R.id.containerSuggestionChips) ?: return
        val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)

        if (query.isNotBlank()) {
            val shortcutSearch = prefs.getBoolean("key_shortcut_search", true)
            val hasContactPerm = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
            val contactSearch = prefs.getBoolean("key_contact_search", false) && hasContactPerm

            val childCount = chipsContainer.childCount
            if (childCount > 1) {
                chipsContainer.removeViews(1, childCount - 1)
            }

            val density = resources.displayMetrics.density

            // Add Contact results if contact search is enabled
            if (contactSearch) {
                val contacts = com.example.launcherai.search.ContactSearchHelper.searchContacts(this, query, 3)
                contacts.forEach { contact ->
                    val chipView = TextView(this).apply {
                        text = "📞 ${contact.name}"
                        textSize = 13f
                        setPadding((14 * density).toInt(), (6 * density).toInt(), (14 * density).toInt(), (6 * density).toInt())
                        setBackgroundResource(R.drawable.bg_nova_search_pill_drawer)
                        setTextColor(ContextCompat.getColor(this@MainActivity, R.color.nova_text_brown_primary))
                        val lp = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            (32 * density).toInt()
                        ).apply { marginStart = (8 * density).toInt() }
                        layoutParams = lp
                        setOnClickListener {
                            com.example.launcherai.search.ContactSearchHelper.dialContact(this@MainActivity, contact.number)
                        }
                    }
                    chipsContainer.addView(chipView)
                }
            }

            // Add Shortcut results if shortcut search is enabled
            if (shortcutSearch) {
                val shortcuts = com.example.launcherai.search.ShortcutSearchHelper.searchShortcuts(this, query)
                shortcuts.take(3).forEach { shortcut ->
                    val chipView = TextView(this).apply {
                        text = "🔍 ${shortcut.title}"
                        textSize = 13f
                        setPadding((14 * density).toInt(), (6 * density).toInt(), (14 * density).toInt(), (6 * density).toInt())
                        setBackgroundResource(R.drawable.bg_nova_search_pill_drawer)
                        setTextColor(ContextCompat.getColor(this@MainActivity, R.color.nova_text_brown_primary))
                        val lp = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            (32 * density).toInt()
                        ).apply { marginStart = (8 * density).toInt() }
                        layoutParams = lp
                        setOnClickListener {
                            try {
                                startActivity(shortcut.intent)
                            } catch (e: Exception) {
                                Toast.makeText(this@MainActivity, "Cannot start shortcut action", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    chipsContainer.addView(chipView)
                }
            }
        } else {
            val smartFolders = prefs.getBoolean("key_drawer_smart_folders", true)
            setupSmartFolderChips(chipsContainer, smartFolders)
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
