package com.example.launcherai

import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
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
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
import com.example.launcherai.ui.DockLayout
import com.example.launcherai.ui.DragLayer
import com.example.launcherai.ui.FastScrollRailView
import com.example.launcherai.ui.FastScroller
import com.example.launcherai.ui.FolderSettingsActivity
import com.example.launcherai.ui.HiddenAppsDialogFragment
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
    private var ivDrawerSwipeIndicator: ImageView? = null

    private lateinit var folderOverlay: FrameLayout
    private lateinit var folderCard: LinearLayout
    private lateinit var etFolderTitle: EditText
    private lateinit var btnCloseFolder: ImageView
    private lateinit var rvFolderApps: RecyclerView
    private lateinit var folderPageIndicator: PageIndicatorView
    private lateinit var folderAppsAdapter: AppsAdapter
    private var folderSnapHelper: PagerSnapHelper? = null
    private var activeFolderId: Long? = null

    private lateinit var drawerAdapter: AppsAdapter
    private lateinit var fastScroller: FastScroller
    private lateinit var rvFrequentApps: RecyclerView
    private lateinit var frequentAppsAdapter: AppsAdapter
    private lateinit var tvFrequentAppsEmpty: TextView
    private var horizontalSnapHelper: PagerSnapHelper? = null
    private var selectedCategory: String = "All"
    private var fullDrawerAppsList: List<AppInfo> = emptyList()
    private var savedDrawerPosition: Int = 0
    private var savedDrawerOffset: Int = 0

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

        val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        val autoClose = prefs.getBoolean("key_drawer_autoclose", true)
        val rememberPos = prefs.getBoolean("key_drawer_remember_position", false)

        if (::layoutAppDrawerOverlay.isInitialized && layoutAppDrawerOverlay.visibility == View.VISIBLE) {
            if (rememberPos) {
                val lm = rvDrawerApps.layoutManager as? LinearLayoutManager
                if (lm != null) {
                    val pos = lm.findFirstVisibleItemPosition()
                    if (pos >= 0) {
                        savedDrawerPosition = pos
                        val firstView = lm.findViewByPosition(pos)
                        savedDrawerOffset = firstView?.top ?: 0
                        prefs.edit()
                            .putInt("key_drawer_saved_pos", savedDrawerPosition)
                            .putInt("key_drawer_saved_offset", savedDrawerOffset)
                            .apply()
                    }
                }
            }

            if (autoClose) {
                // Instantly reset drawer state so user returns to desktop workspace
                layoutAppDrawerOverlay.visibility = View.GONE
                layoutAppDrawerOverlay.translationY = 0f
                layoutAppDrawerOverlay.scaleX = 1f
                layoutAppDrawerOverlay.scaleY = 1f
                layoutAppDrawerOverlay.alpha = 1f
                workspace.scaleX = 1f
                workspace.scaleY = 1f
                workspace.alpha = 1f
                dockHotseat.alpha = 1f
                etDrawerSearch.setText("")
            }
        }
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
        viewModel.loadHiddenPackages()
        viewModel.refreshInstalledApps()
    }

    private fun applySearchBarPlacement() {
        val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        val placement = prefs.getString("key_search_bar_placement_home", "DOCK_BELOW") ?: "DOCK_BELOW"
        val searchBar = findViewById<View>(R.id.barHomeSearch) ?: return
        val desktopLayout = findViewById<LinearLayout>(R.id.layoutDesktop) ?: return
        val dockHotseat = findViewById<View>(R.id.dockHotseat)

        val density = resources.displayMetrics.density
        val lp = searchBar.layoutParams as? LinearLayout.LayoutParams ?: LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (55 * density).toInt()
        )
        val marginH = (16 * density).toInt()
        lp.marginStart = marginH
        lp.marginEnd = marginH

        when (placement) {
            "TOP" -> {
                searchBar.visibility = View.VISIBLE
                lp.topMargin = (36 * density).toInt()
                lp.bottomMargin = (8 * density).toInt()
                searchBar.layoutParams = lp
                desktopLayout.removeView(searchBar)
                desktopLayout.addView(searchBar, 0)
            }
            "DOCK_ABOVE" -> {
                searchBar.visibility = View.VISIBLE
                lp.topMargin = (4 * density).toInt()
                lp.bottomMargin = (6 * density).toInt()
                searchBar.layoutParams = lp
                desktopLayout.removeView(searchBar)
                if (dockHotseat != null) {
                    val dockIdx = desktopLayout.indexOfChild(dockHotseat)
                    desktopLayout.addView(searchBar, dockIdx.coerceAtLeast(0))
                } else {
                    desktopLayout.addView(searchBar)
                }
            }
            "HIDDEN", "NONE" -> {
                searchBar.visibility = View.GONE
            }
            else -> { // "DOCK_BELOW"
                searchBar.visibility = View.VISIBLE
                lp.topMargin = (4 * density).toInt()
                lp.bottomMargin = (12 * density).toInt()
                searchBar.layoutParams = lp
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
        val swipeToOpen = prefs.getBoolean("key_drawer_swipe_to_open", true)
        gestureHandler.swipeUpAction = if (swipeToOpen) {
            GestureAction.fromName(
                prefs.getString("key_gesture_swipe_up", "") ?: "",
                GestureAction.APP_DRAWER
            )
        } else {
            GestureAction.NONE
        }
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
        ivDrawerSwipeIndicator = findViewById(R.id.ivDrawerSwipeIndicator)
        ivDrawerSwipeIndicator?.setOnClickListener {
            openAppDrawer()
        }

        folderOverlay = findViewById(R.id.folderOverlay)
        folderCard = findViewById(R.id.folderCard)
        etFolderTitle = findViewById(R.id.etFolderTitle)
        btnCloseFolder = findViewById(R.id.btnCloseFolder)
        rvFolderApps = findViewById(R.id.rvFolderApps)
        folderPageIndicator = findViewById(R.id.folderPageIndicator)

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
                val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                val autoClose = prefs.getBoolean("key_drawer_autoclose", true)
                if (autoClose) {
                    closeAppDrawer()
                }
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

        // Frequently Used Apps Row (Nova drawer_predictive_apps)
        rvFrequentApps = findViewById(R.id.rvFrequentApps)
        tvFrequentAppsEmpty = findViewById(R.id.tvFrequentAppsEmpty)
        val initialDrawerPrefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        val initialDrawerCols = initialDrawerPrefs.getInt("key_drawer_grid_cols", 5).coerceAtLeast(1)
        rvFrequentApps.layoutManager = GridLayoutManager(this, initialDrawerCols)
        frequentAppsAdapter = AppsAdapter(
            apps = emptyList(),
            onAppClick = { app ->
                AppManager.launchApp(this, app)
                val p = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                val autoClose = p.getBoolean("key_drawer_autoclose", true)
                if (autoClose) {
                    closeAppDrawer()
                }
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
        rvFrequentApps.adapter = frequentAppsAdapter

        // FastScroller with A–Z rail accent bar (matching Nova Launcher AllAppsFastScroll)
        val fastScrollRail = findViewById<FastScrollRailView>(R.id.fastScrollRail)
        fastScroller = FastScroller(rvDrawerApps, tvFastScrollBubble, fastScrollRail)

        // Pull down to search listener (Nova Launcher drawer_pull_to_search)
        var pullStartY = 0f
        rvDrawerApps.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                if (!prefs.getBoolean("key_drawer_pull_to_search", true)) return false
                if (etDrawerSearch.hasFocus()) return false

                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        pullStartY = e.y
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dy = e.y - pullStartY
                        if (!rv.canScrollVertically(-1) && dy > 120f) {
                            if (prefs.getBoolean("key_drawer_swipe_haptic", true)) {
                                rv.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                            }
                            etDrawerSearch.requestFocus()
                            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                            imm?.showSoftInput(etDrawerSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                            return true
                        }
                    }
                }
                return false
            }
        })
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

                // Frequently Used Apps collector (Nova drawer_predictive_apps)
                launch {
                    viewModel.suggestedApps.collect { suggested ->
                        val p = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                        val cols = p.getInt("key_drawer_grid_cols", 5).coerceAtLeast(1)
                        val topApps = suggested.take(cols)
                        frequentAppsAdapter.updateApps(topApps)
                        if (topApps.isEmpty()) {
                            tvFrequentAppsEmpty.visibility = View.VISIBLE
                            rvFrequentApps.visibility = View.GONE
                        } else {
                            tvFrequentAppsEmpty.visibility = View.GONE
                            rvFrequentApps.visibility = View.VISIBLE
                        }
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
        val dialog = HiddenAppsDialogFragment.newInstance(
            hiddenPackages = viewModel.hiddenPackages.value,
            apps = viewModel.installedApps.value
        ) { _ ->
            viewModel.loadHiddenPackages()
        }
        dialog.show(supportFragmentManager, HiddenAppsDialogFragment.TAG)
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

        val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        val style = prefs.getString(FolderSettingsActivity.KEY_FOLDER_WINDOW_STYLE, FolderSettingsActivity.WINDOW_STYLE_WINDOW) ?: FolderSettingsActivity.WINDOW_STYLE_WINDOW
        val bgColor = prefs.getInt(FolderSettingsActivity.KEY_FOLDER_WINDOW_BG_COLOR, 0xFF202534.toInt())
        val bgAlpha = prefs.getInt(FolderSettingsActivity.KEY_FOLDER_WINDOW_BG_ALPHA, 80)
        val cornerRadiusDp = prefs.getInt(FolderSettingsActivity.KEY_FOLDER_WINDOW_CORNER_RADIUS, 16)
        val shadow = prefs.getBoolean(FolderSettingsActivity.KEY_FOLDER_WINDOW_SHADOW, true)
        val rows = prefs.getInt(FolderSettingsActivity.KEY_FOLDER_GRID_ROWS, 3).coerceIn(2, 6)
        val cols = prefs.getInt(FolderSettingsActivity.KEY_FOLDER_GRID_COLS, 4).coerceIn(3, 6)
        val scrollDir = prefs.getString(FolderSettingsActivity.KEY_FOLDER_SCROLL_DIRECTION, FolderSettingsActivity.SCROLL_DIR_VERTICAL) ?: FolderSettingsActivity.SCROLL_DIR_VERTICAL

        val density = resources.displayMetrics.density
        val alphaInt = ((bgAlpha / 100f) * 255).toInt().coerceIn(0, 255)
        val cardBgTint = ColorUtils.setAlphaComponent(bgColor, alphaInt)

        if (style == FolderSettingsActivity.WINDOW_STYLE_IMMERSIVE) {
            // Full-screen blurred/tinted sheet presentation (Nova Immersive mode)
            val scrimColor = ColorUtils.setAlphaComponent(bgColor, alphaInt)
            folderOverlay.setBackgroundColor(scrimColor)

            val params = folderCard.layoutParams as FrameLayout.LayoutParams
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            params.height = ViewGroup.LayoutParams.MATCH_PARENT
            params.gravity = Gravity.NO_GRAVITY
            params.setMargins(0, 0, 0, 0)
            folderCard.layoutParams = params

            // In Immersive mode, elevation is 0 and corner radius is 0
            folderCard.elevation = 0f
            val cardBg = GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                cornerRadius = 0f
            }
            folderCard.background = cardBg

            // Apply system insets for status bar and navigation bar
            val insets = ViewCompat.getRootWindowInsets(folderOverlay)
            val statusBarTop = insets?.getInsets(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout())?.top ?: (24 * density).toInt()
            val navBarBottom = insets?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: (24 * density).toInt()
            folderCard.setPadding(
                (24 * density).toInt(),
                statusBarTop + (16 * density).toInt(),
                (24 * density).toInt(),
                navBarBottom + (16 * density).toInt()
            )
        } else {
            // Window mode: Centered floating card with elevation and rounded corners
            val scrimAlpha = (0.6f * 255).toInt()
            folderOverlay.setBackgroundColor(Color.argb(scrimAlpha, 0, 0, 0))

            val displayWidth = resources.displayMetrics.widthPixels
            val maxCardWidth = (displayWidth - (48 * density).toInt()).coerceAtLeast((280 * density).toInt())
            val targetCardWidth = ((cols * 76 + 40) * density).toInt().coerceIn((280 * density).toInt(), maxCardWidth)

            val params = folderCard.layoutParams as FrameLayout.LayoutParams
            params.width = targetCardWidth
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT
            params.gravity = Gravity.CENTER
            params.setMargins((24 * density).toInt(), (48 * density).toInt(), (24 * density).toInt(), (48 * density).toInt())
            folderCard.layoutParams = params

            folderCard.elevation = if (shadow) (5f * density) else 0f
            val cardBg = GradientDrawable().apply {
                setColor(cardBgTint)
                cornerRadius = cornerRadiusDp * density
            }
            folderCard.background = cardBg
            folderCard.setPadding((20 * density).toInt(), (20 * density).toInt(), (20 * density).toInt(), (20 * density).toInt())
        }

        // Configure Grid Dimensions & Scroll Direction
        val apps = folderResolved.children.map { it.appInfo }
        folderAppsAdapter = AppsAdapter(
            apps = apps,
            onAppClick = { appInfo ->
                AppManager.launchApp(this, appInfo)
                val autoClose = prefs.getBoolean(FolderSettingsActivity.KEY_FOLDER_AUTO_CLOSE, true)
                if (autoClose) {
                    closeFolderOverlay()
                }
            },
            isFolder = true
        )

        folderSnapHelper?.attachToRecyclerView(null)
        folderSnapHelper = null

        if (scrollDir == FolderSettingsActivity.SCROLL_DIR_HORIZONTAL) {
            val layoutManager = GridLayoutManager(this, rows, RecyclerView.HORIZONTAL, false)
            rvFolderApps.layoutManager = layoutManager
            rvFolderApps.adapter = folderAppsAdapter

            val itemsPerPage = rows * cols
            val pageCount = if (itemsPerPage > 0) Math.ceil(apps.size.toDouble() / itemsPerPage).toInt().coerceAtLeast(1) else 1

            if (pageCount > 1) {
                folderPageIndicator.visibility = View.VISIBLE
                folderPageIndicator.setPageCount(pageCount)
                folderPageIndicator.onPageSelected(0)

                val snapHelper = PagerSnapHelper()
                snapHelper.attachToRecyclerView(rvFolderApps)
                folderSnapHelper = snapHelper

                rvFolderApps.clearOnScrollListeners()
                rvFolderApps.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        super.onScrollStateChanged(recyclerView, newState)
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                            val snapView = snapHelper.findSnapView(layoutManager)
                            if (snapView != null) {
                                val pos = layoutManager.getPosition(snapView)
                                val pageIndex = if (itemsPerPage > 0) (pos / itemsPerPage).coerceIn(0, pageCount - 1) else 0
                                folderPageIndicator.onPageSelected(pageIndex)
                            }
                        }
                    }
                })
            } else {
                folderPageIndicator.visibility = View.GONE
                rvFolderApps.clearOnScrollListeners()
            }
        } else {
            rvFolderApps.layoutManager = GridLayoutManager(this, cols, RecyclerView.VERTICAL, false)
            rvFolderApps.adapter = folderAppsAdapter
            folderPageIndicator.visibility = View.GONE
            rvFolderApps.clearOnScrollListeners()
        }

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
            folderSnapHelper?.attachToRecyclerView(null)
            folderSnapHelper = null
        }.start()
    }

    private fun openAppDrawer() {
        val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        val animMode = prefs.getString("key_drawer_animation", "SLIDE") ?: "SLIDE"
        ivDrawerSwipeIndicator?.visibility = View.GONE

        when (animMode.uppercase()) {
            "ZOOM" -> {
                layoutAppDrawerOverlay.visibility = View.VISIBLE
                layoutAppDrawerOverlay.translationY = 0f
                layoutAppDrawerOverlay.scaleX = 1.25f
                layoutAppDrawerOverlay.scaleY = 1.25f
                layoutAppDrawerOverlay.alpha = 0f
                layoutAppDrawerOverlay.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(260).setInterpolator(android.view.animation.DecelerateInterpolator()).start()
                workspace.animate().scaleX(0.7f).scaleY(0.7f).alpha(0f).setDuration(240).start()
                dockHotseat.animate().alpha(0f).setDuration(200).start()
            }
            "FADE" -> {
                layoutAppDrawerOverlay.visibility = View.VISIBLE
                layoutAppDrawerOverlay.translationY = 0f
                layoutAppDrawerOverlay.scaleX = 1f
                layoutAppDrawerOverlay.scaleY = 1f
                layoutAppDrawerOverlay.alpha = 0f
                layoutAppDrawerOverlay.animate().alpha(1f).setDuration(220).start()
                workspace.animate().alpha(0f).setDuration(200).start()
                dockHotseat.animate().alpha(0f).setDuration(200).start()
            }
            else -> { // "SLIDE"
                layoutAppDrawerOverlay.visibility = View.VISIBLE
                layoutAppDrawerOverlay.scaleX = 1f
                layoutAppDrawerOverlay.scaleY = 1f
                layoutAppDrawerOverlay.alpha = 1f
                val height = resources.displayMetrics.heightPixels.toFloat()
                layoutAppDrawerOverlay.translationY = height
                layoutAppDrawerOverlay.animate().translationY(0f).setDuration(280).setInterpolator(android.view.animation.DecelerateInterpolator(1.5f)).start()
                workspace.animate().scaleX(0.85f).scaleY(0.85f).alpha(0f).setDuration(240).start()
                dockHotseat.animate().alpha(0f).setDuration(200).start()
            }
        }

        if (prefs.getBoolean("key_drawer_swipe_haptic", true)) {
            rvDrawerApps.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
        }

        val rememberPos = prefs.getBoolean("key_drawer_remember_position", false)
        if (rememberPos) {
            savedDrawerPosition = prefs.getInt("key_drawer_saved_pos", savedDrawerPosition)
            savedDrawerOffset = prefs.getInt("key_drawer_saved_offset", savedDrawerOffset)
            if (savedDrawerPosition > 0 || savedDrawerOffset != 0) {
                rvDrawerApps.post {
                    (rvDrawerApps.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(
                        savedDrawerPosition,
                        savedDrawerOffset
                    )
                }
            } else {
                rvDrawerApps.scrollToPosition(0)
            }
        } else {
            savedDrawerPosition = 0
            savedDrawerOffset = 0
            rvDrawerApps.scrollToPosition(0)
        }

        val incognito = prefs.getBoolean("key_drawer_search_incognito", false)
        if (incognito) {
            etDrawerSearch.imeOptions = etDrawerSearch.imeOptions or android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
        } else {
            etDrawerSearch.imeOptions = etDrawerSearch.imeOptions and android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING.inv()
        }

        applyDrawerPlacementAndSettings()

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
        val cols = prefs.getInt("key_drawer_grid_cols", 5).coerceAtLeast(1)
        val rows = prefs.getInt("key_drawer_grid_rows", 6).coerceAtLeast(1)
        val swipeIndicator = prefs.getBoolean("key_drawer_swipe_indicator", true)
        val swipeToOpen = prefs.getBoolean("key_drawer_swipe_to_open", true)

        ivDrawerSwipeIndicator?.visibility = if (swipeIndicator && swipeToOpen && layoutAppDrawerOverlay.visibility != View.VISIBLE) View.VISIBLE else View.GONE

        findViewById<View>(R.id.layoutFrequentAppsHeader)?.visibility = if (showFrequent) View.VISIBLE else View.GONE
        if (::rvFrequentApps.isInitialized) {
            rvFrequentApps.layoutManager = GridLayoutManager(this, cols)
        }

        val searchContainer = findViewById<View>(R.id.barDrawerSearchContainer) ?: return
        val drawerLayout = findViewById<LinearLayout>(R.id.layoutAppDrawerOverlay) ?: return

        // Apply drawer background color and transparency (Nova drawer_background_color / drawer_background_alpha)
        val drawerBgColor = prefs.getInt("key_drawer_background_color", 0xFFF7F2EE.toInt())
        val drawerBgAlpha = prefs.getInt("key_drawer_background_alpha", 0)
        val alpha255 = (((100 - drawerBgAlpha) * 255) / 100).coerceIn(0, 255)
        val compositeDrawerBg = ColorUtils.setAlphaComponent(drawerBgColor, alpha255)
        drawerLayout.setBackgroundColor(compositeDrawerBg)

        // Apply fast scroll accent color
        val fastScrollColor = prefs.getInt("key_drawer_fastscroll_color", 0xFFD54526.toInt())
        fastScroller.setAccentColor(fastScrollColor)

        val isHidden = drawerPlacement.equals("None", ignoreCase = true) || drawerPlacement.equals("Hidden", ignoreCase = true)

        when {
            isHidden -> {
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

        // Apply App Drawer Style (Vertical, List, Horizontal) matching Nova Launcher (d0)
        val fastScrollEnabled = prefs.getBoolean("key_drawer_fastscroll", true)
        when (drawerStyle) {
            "List" -> {
                horizontalSnapHelper?.attachToRecyclerView(null)
                horizontalSnapHelper = null
                rvDrawerApps.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
                drawerAdapter.setDisplayMode(AppsAdapter.DisplayMode.LIST)
                fastScroller.isEnabled = fastScrollEnabled
            }
            "Horizontal" -> {
                rvDrawerApps.layoutManager = GridLayoutManager(this, rows, RecyclerView.HORIZONTAL, false)
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
                rvDrawerApps.layoutManager = GridLayoutManager(this, cols, RecyclerView.VERTICAL, false)
                drawerAdapter.setDisplayMode(AppsAdapter.DisplayMode.GRID)
                fastScroller.isEnabled = fastScrollEnabled
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
        val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        val filtered = filterAppsByCategory(fullDrawerAppsList, selectedCategory)
        val foldersFirst = prefs.getBoolean("key_drawer_folders_first", false)
        val sorted = if (foldersFirst) {
            filtered.sortedWith(compareBy({ !it.isFolder }, { it.label.lowercase(Locale.ROOT) }))
        } else {
            filtered.sortedBy { it.label.lowercase(Locale.ROOT) }
        }
        drawerAdapter.updateApps(sorted)
        fastScroller.updateApps(sorted)
        val query = viewModel.searchQuery.value
        tvDrawerEmpty.visibility = if (sorted.isEmpty() && query.isNotBlank()) View.VISIBLE else View.GONE

        val rememberPos = prefs.getBoolean("key_drawer_remember_position", false)
        if (query.isBlank() && selectedCategory == "All" && rememberPos && (savedDrawerPosition > 0 || savedDrawerOffset != 0)) {
            rvDrawerApps.post {
                (rvDrawerApps.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(
                    savedDrawerPosition,
                    savedDrawerOffset
                )
            }
        }

        val chipsContainer = findViewById<LinearLayout>(R.id.containerSuggestionChips) ?: return

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
                            val p = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                            if (p.getBoolean("key_drawer_autoclose", true)) {
                                closeAppDrawer()
                            }
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
                                val p = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                                if (p.getBoolean("key_drawer_autoclose", true)) {
                                    closeAppDrawer()
                                }
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
        val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        val animMode = prefs.getString("key_drawer_animation", "SLIDE") ?: "SLIDE"
        val rememberPos = prefs.getBoolean("key_drawer_remember_position", false)

        if (rememberPos) {
            val lm = rvDrawerApps.layoutManager as? LinearLayoutManager
            if (lm != null) {
                val pos = lm.findFirstVisibleItemPosition()
                if (pos >= 0) {
                    savedDrawerPosition = pos
                    val firstView = lm.findViewByPosition(pos)
                    savedDrawerOffset = firstView?.top ?: 0
                    prefs.edit()
                        .putInt("key_drawer_saved_pos", savedDrawerPosition)
                        .putInt("key_drawer_saved_offset", savedDrawerOffset)
                        .apply()
                }
            }
        } else {
            savedDrawerPosition = 0
            savedDrawerOffset = 0
            prefs.edit()
                .putInt("key_drawer_saved_pos", 0)
                .putInt("key_drawer_saved_offset", 0)
                .apply()
            rvDrawerApps.scrollToPosition(0)
        }

        when (animMode.uppercase()) {
            "ZOOM" -> {
                layoutAppDrawerOverlay.animate().scaleX(1.25f).scaleY(1.25f).alpha(0f).setDuration(220).withEndAction {
                    layoutAppDrawerOverlay.visibility = View.GONE
                    layoutAppDrawerOverlay.scaleX = 1f
                    layoutAppDrawerOverlay.scaleY = 1f
                    etDrawerSearch.setText("")
                    applyDrawerPlacementAndSettings()
                }.start()
                workspace.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(240).start()
                dockHotseat.animate().alpha(1f).setDuration(200).start()
            }
            "FADE" -> {
                layoutAppDrawerOverlay.animate().alpha(0f).setDuration(200).withEndAction {
                    layoutAppDrawerOverlay.visibility = View.GONE
                    etDrawerSearch.setText("")
                    applyDrawerPlacementAndSettings()
                }.start()
                workspace.animate().alpha(1f).setDuration(200).start()
                dockHotseat.animate().alpha(1f).setDuration(200).start()
            }
            else -> { // "SLIDE"
                val height = resources.displayMetrics.heightPixels.toFloat()
                layoutAppDrawerOverlay.animate().translationY(height).setDuration(240).setInterpolator(android.view.animation.AccelerateInterpolator(1.5f)).withEndAction {
                    layoutAppDrawerOverlay.visibility = View.GONE
                    layoutAppDrawerOverlay.translationY = 0f
                    etDrawerSearch.setText("")
                    applyDrawerPlacementAndSettings()
                }.start()
                workspace.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(240).start()
                dockHotseat.animate().alpha(1f).setDuration(200).start()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
