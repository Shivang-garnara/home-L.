package com.example.launcherai.ui

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.launcherai.LauncherApplication
import com.example.launcherai.R
import com.example.launcherai.ai.ui.NovaAiSettingsActivity
import com.example.launcherai.data.repository.GridChangeResult
import com.example.launcherai.databinding.ActivityLauncherSettingsBinding
import com.example.launcherai.gestures.GestureAction
import com.example.launcherai.icons.IconPackManager
import kotlinx.coroutines.launch

/**
 * LauncherAI Settings main hub matching Nova Launcher 8.9.6 reference screens 12 & 13.
 * Implemented using Android ViewBinding for layout components.
 */
class LauncherSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLauncherSettingsBinding
    private val settingsRows = mutableListOf<Pair<View, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_NovaLight)
        super.onCreate(savedInstanceState)
        SystemBarHelper.applySystemBars(this, isLightTheme = true, transparentBars = false)
        binding = ActivityLauncherSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemBarHelper.applyEdgeToEdgeInsets(
            insetsRootView = binding.root,
            topInsetView = binding.headerContainer,
            bottomInsetView = binding.bottomNavBar
        )

        initViews()
        setupBottomNavigation()
        setupSearchFiltering()
        setupRowListeners()
        setupBackHandler()
        updateSummaryTexts()
    }

    override fun onResume() {
        super.onResume()
        checkDefaultLauncherStatus()
        updateSummaryTexts()
        if (intent.getBooleanExtra("scroll_down", false)) {
            binding.scrollViewSettingsMain.post {
                binding.scrollViewSettingsMain.scrollTo(0, 750)
            }
        }
    }

    private fun initViews() {
        // Register all 12 rows for search index
        val rows = listOf(
            Pair(binding.rowAiAssistant, "AI assistant Chat and privacy"),
            Pair(binding.rowHomeScreen, "Home screen Set grid, icon layout, dock settings, and more"),
            Pair(binding.rowAppDrawer, "App drawer Set layout, style, opening gesture, and more"),
            Pair(binding.rowFolders, "Folders Set window styles, background colors, and icon layout"),
            Pair(binding.rowSearch, "Search LauncherAI search window and bar configuration"),
            Pair(binding.rowCards, "Cards Build cards to show in LauncherAI Search or set conditions for them to appear in your app drawer or feed"),
            Pair(binding.rowLookFeel, "Look & feel Icon preferences, popup menu, and notification bar options"),
            Pair(binding.rowGesturesInputs, "Gestures & inputs Define secondary home button action, along with swipe, tap, and pinch gestures on home screen"),
            Pair(binding.rowFeedPage, "Feed page Control and customize the feed pages at the edge of your home screen"),
            Pair(binding.rowIntegrations, "Integrations Connect other Apps to LauncherAI, for example to enhance shortcut search content"),
            Pair(binding.rowNotificationBadges, "Notification badges Choose from dynamic badges, notification dots, or numeric dots"),
            Pair(binding.rowBackupRestore, "Backup & restore Backup or restore your launcher settings and desktop layout")
        )

        rows.forEach { (view, keywords) ->
            settingsRows.add(Pair(view, keywords.lowercase()))
        }

        binding.bannerNotDefault.setOnClickListener {
            requestDefaultLauncher()
        }

        binding.btnBackFromUnavailable.setOnClickListener {
            binding.layoutFeatureUnavailable.visibility = View.GONE
        }
    }

    private fun checkDefaultLauncherStatus() {
        val isDefault = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            roleManager?.isRoleHeld(RoleManager.ROLE_HOME) == true
        } else {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            resolveInfo?.activityInfo?.packageName == packageName
        }

        binding.bannerNotDefault.visibility = if (SettingsFilterUtils.shouldShowDefaultBanner(isDefault)) View.VISIBLE else View.GONE
    }

    private fun requestDefaultLauncher() {
        var launched = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val roleManager = getSystemService(RoleManager::class.java)
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                    @Suppress("DEPRECATION")
                    startActivityForResult(intent, REQUEST_CODE_SET_DEFAULT)
                    launched = true
                }
            } catch (e: Exception) {
                launched = false
            }
        }
        if (!launched) {
            try {
                val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                startActivity(intent)
            } catch (e: Exception) {
                try {
                    val intent = Intent(Settings.ACTION_SETTINGS)
                    startActivity(intent)
                } catch (e2: Exception) {
                    Toast.makeText(this, "Open Android Settings > Apps > Default Apps to select LauncherAI", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.tabSettings.setOnClickListener {
            switchToSettingsTab()
        }

        binding.tabStyle.setOnClickListener {
            switchToStyleTab()
        }
    }

    private fun switchToSettingsTab() {
        binding.layoutStyleTabContent.visibility = View.GONE
        binding.layoutFeatureUnavailable.visibility = View.GONE
        binding.scrollViewSettingsMain.visibility = View.VISIBLE

        binding.indicatorSettingsTab.setBackgroundResource(R.drawable.bg_nova_bottom_nav_pill)
        binding.indicatorStyleTab.background = null

        binding.ivTabSettingsIcon.setColorFilter(ContextCompat.getColor(this, R.color.nova_text_brown_primary))
        binding.tvTabSettingsLabel.setTextColor(ContextCompat.getColor(this, R.color.nova_text_brown_primary))

        binding.ivTabStyleIcon.setColorFilter(ContextCompat.getColor(this, R.color.nova_text_brown_secondary))
        binding.tvTabStyleLabel.setTextColor(ContextCompat.getColor(this, R.color.nova_text_brown_secondary))
    }

    private fun switchToStyleTab() {
        binding.scrollViewSettingsMain.visibility = View.GONE
        binding.layoutFeatureUnavailable.visibility = View.GONE
        binding.layoutStyleTabContent.visibility = View.VISIBLE

        binding.indicatorStyleTab.setBackgroundResource(R.drawable.bg_nova_bottom_nav_pill)
        binding.indicatorSettingsTab.background = null

        binding.ivTabStyleIcon.setColorFilter(ContextCompat.getColor(this, R.color.nova_text_brown_primary))
        binding.tvTabStyleLabel.setTextColor(ContextCompat.getColor(this, R.color.nova_text_brown_primary))

        binding.ivTabSettingsIcon.setColorFilter(ContextCompat.getColor(this, R.color.nova_text_brown_secondary))
        binding.tvTabSettingsLabel.setTextColor(ContextCompat.getColor(this, R.color.nova_text_brown_secondary))
    }

    private fun setupSearchFiltering() {
        binding.etSettingsSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim()?.lowercase() ?: ""
                binding.btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                filterSettingsRows(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnClearSearch.setOnClickListener {
            binding.etSettingsSearch.setText("")
        }
    }

    private fun filterSettingsRows(query: String) {
        var visibleCount = 0
        settingsRows.forEach { (view, keywords) ->
            if (SettingsFilterUtils.matchesQuery(query, keywords)) {
                view.visibility = View.VISIBLE
                visibleCount++
            } else {
                view.visibility = View.GONE
            }
        }

        binding.tvSettingsEmptySearch.visibility = if (visibleCount == 0 && query.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun setupRowListeners() {
        // Row 1: AI assistant
        binding.rowAiAssistant.setOnClickListener {
            startActivity(Intent(this, NovaAiSettingsActivity::class.java))
        }

        // Row 2: Home screen
        binding.rowHomeScreen.setOnClickListener {
            startActivity(Intent(this, HomeScreenSettingsActivity::class.java))
        }

        // Row 3: App drawer
        binding.rowAppDrawer.setOnClickListener {
            startActivity(Intent(this, AppDrawerSettingsActivity::class.java))
        }

        // Row 4: Folders
        binding.rowFolders.setOnClickListener {
            startActivity(Intent(this, FolderSettingsActivity::class.java))
        }

        // Row 5: Search
        binding.rowSearch.setOnClickListener {
            startActivity(Intent(this, SearchSettingsActivity::class.java))
        }

        // Row 6: Cards
        binding.rowCards.setOnClickListener {
            showUnavailableScreen("Cards", R.drawable.ic_settings_cards)
        }

        // Row 7: Look & feel
        binding.rowLookFeel.setOnClickListener {
            startActivity(Intent(this, LookFeelSettingsActivity::class.java))
        }

        // Row 8: Gestures & inputs
        binding.rowGesturesInputs.setOnClickListener {
            showGesturesDialog()
        }

        // Row 9: Feed page
        binding.rowFeedPage.setOnClickListener {
            showUnavailableScreen("Feed page", R.drawable.ic_settings_feed)
        }

        // Row 10: Integrations
        binding.rowIntegrations.setOnClickListener {
            showUnavailableScreen("Integrations", R.drawable.ic_settings_integrations)
        }

        // Row 11: Notification badges
        binding.rowNotificationBadges.setOnClickListener {
            showUnavailableScreen("Notification badges", R.drawable.ic_settings_badges)
        }

        // Row 12: Backup & restore
        binding.rowBackupRestore.setOnClickListener {
            showBackupRestoreDialog()
        }
    }

    private fun showUnavailableScreen(title: String, iconResId: Int) {
        binding.tvUnavailableTitle.text = title
        binding.ivUnavailableIcon.setImageResource(iconResId)
        binding.layoutFeatureUnavailable.visibility = View.VISIBLE
    }

    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.layoutFeatureUnavailable.visibility == View.VISIBLE) {
                    binding.layoutFeatureUnavailable.visibility = View.GONE
                } else if (binding.layoutStyleTabContent.visibility == View.VISIBLE) {
                    switchToSettingsTab()
                } else {
                    finish()
                }
            }
        })
    }

    private fun updateSummaryTexts() {
        val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        val repository = (application as LauncherApplication).repository
        val currentGrid = repository.getGridConfig()
        val paddingHoriz = prefs.getInt("key_desktop_padding_horizontal", 0)
        val paddingVert = prefs.getInt("key_desktop_padding_vertical", 0)
        fun stepToLabel(step: Int): String = when (step) {
            1 -> "Small"
            2 -> "Medium"
            3 -> "Large"
            else -> "None"
        }
        binding.tvHomeScreenSubtitle.text = "${currentGrid.rows} ✕ ${currentGrid.cols} • ${stepToLabel(paddingHoriz)} ✕ ${stepToLabel(paddingVert)}"

        val shape = prefs.getString("key_icon_shape", "Round") ?: "Round"
        val sizePercent = prefs.getInt("key_icon_size_percent", 125)
        val packPkg = prefs.getString("key_selected_icon_pack", "") ?: ""
        val packLabel = if (packPkg.isEmpty()) "System" else {
            IconPackManager.getInstalledIconPacks(this).firstOrNull { it.packageName == packPkg }?.label ?: "Custom"
        }
        binding.tvLookFeelSubtitle.text = "$packLabel • $shape • $sizePercent%"

        val drawerStyle = prefs.getString("key_app_drawer_style", "Vertical") ?: "Vertical"
        val drawerRows = prefs.getInt("key_drawer_grid_rows", 6)
        val drawerCols = prefs.getInt("key_drawer_grid_cols", 5)
        binding.tvAppDrawerSubtitle.text = "$drawerStyle • $drawerRows ✕ $drawerCols"

        val searchMode = prefs.getString("key_search_mode", "Immersive") ?: "Immersive"
        val homePlacement = prefs.getString("key_search_bar_placement_home", "DOCK_BELOW") ?: "DOCK_BELOW"
        val homeLabel = when (homePlacement) {
            "DOCK_ABOVE" -> "Dock (Above)"
            "TOP" -> "Top"
            "HIDDEN" -> "Hidden"
            else -> "Dock (Below)"
        }
        val rawDrawerPlacement = prefs.getString("key_search_bar_placement_drawer", "Top") ?: "Top"
        val drawerPlacement = if (rawDrawerPlacement.equals("Hidden", ignoreCase = true)) "None" else rawDrawerPlacement
        binding.tvSearchSubtitle.text = "Mode: $searchMode • Home: $homeLabel • Drawer: $drawerPlacement"

        val folderPreview = prefs.getString("key_folder_icon_preview", "GRID") ?: "GRID"
        val folderPreviewName = when (folderPreview.uppercase()) {
            "GRID_9" -> "Grid (3x3)"
            "RADIAL_5" -> "Radial"
            "RADIAL_9" -> "Radial (Clustered)"
            "STACK" -> "Stack"
            "FAN" -> "Fan"
            "LINE" -> "Line"
            "FIRST_APP" -> "First app (1 icon)"
            else -> "Grid (2x2)"
        }
        val folderShape = prefs.getString("key_folder_icon_shape", "SYSTEM") ?: "SYSTEM"
        val folderShapeName = if (folderShape.equals("SYSTEM", ignoreCase = true)) "System default" else folderShape
        val folderWindowStyle = prefs.getString("key_folder_window_style", "WINDOW") ?: "WINDOW"
        val folderStyleName = if (folderWindowStyle.equals("IMMERSIVE", ignoreCase = true)) "Immersive" else "Window"
        binding.tvFoldersSubtitle.text = "$folderPreviewName • $folderShapeName • $folderStyleName"
    }

    private fun showHomeScreenGridDialog() {
        val repository = (application as LauncherApplication).repository
        val currentGrid = repository.getGridConfig()
        val options = arrayOf("3 columns x 4 rows", "4 columns x 5 rows (Default)", "5 columns x 6 rows", "6 columns x 7 rows")
        val activeIndex = when (currentGrid.cols) {
            3 -> 0
            5 -> 2
            6 -> 3
            else -> 1
        }

        AlertDialog.Builder(this)
            .setTitle("Home Screen Grid Dimension")
            .setSingleChoiceItems(options, activeIndex) { dialog, which ->
                val (cols, rows) = when (which) {
                    0 -> Pair(3, 4)
                    2 -> Pair(5, 6)
                    3 -> Pair(6, 7)
                    else -> Pair(4, 5)
                }

                lifecycleScope.launch {
                    val result = repository.setGridConfig(cols, rows)
                    when (result) {
                        is GridChangeResult.Success -> {
                            updateSummaryTexts()
                            dialog.dismiss()
                        }
                        is GridChangeResult.Failure -> {
                            Toast.makeText(this@LauncherSettingsActivity, "Cannot set grid: ${result.reason}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAppDrawerSettingsDialog() {
        val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        val currentStyle = prefs.getString("key_app_drawer_style", "Vertical") ?: "Vertical"
        val showKb = prefs.getBoolean("key_drawer_show_keyboard", true)
        val freqApps = prefs.getBoolean("key_drawer_frequent_apps", true)

        val options = arrayOf(
            "App Drawer Style: $currentStyle",
            "Show Keyboard: ${if (showKb) "ON" else "OFF"}",
            "Frequently Used Apps: ${if (freqApps) "ON" else "OFF"}",
            "Open Drawer Setup Wizard"
        )

        AlertDialog.Builder(this)
            .setTitle("App Drawer Configuration")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val styles = arrayOf("Vertical", "List", "Horizontal")
                        val activeIdx = styles.indexOf(currentStyle).coerceAtLeast(0)
                        AlertDialog.Builder(this)
                            .setTitle("App Drawer Style")
                            .setSingleChoiceItems(styles, activeIdx) { d, idx ->
                                prefs.edit().putString("key_app_drawer_style", styles[idx]).apply()
                                d.dismiss()
                                updateSummaryTexts()
                                Toast.makeText(this, "Drawer style set to ${styles[idx]}", Toast.LENGTH_SHORT).show()
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                    1 -> {
                        prefs.edit().putBoolean("key_drawer_show_keyboard", !showKb).apply()
                        updateSummaryTexts()
                        Toast.makeText(this, "Show keyboard: ${if (!showKb) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        prefs.edit().putBoolean("key_drawer_frequent_apps", !freqApps).apply()
                        updateSummaryTexts()
                        Toast.makeText(this, "Frequently used apps: ${if (!freqApps) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
                    }
                    3 -> {
                        startActivity(Intent(this, SetupActivity::class.java).apply {
                            putExtra("setup_step", 3)
                        })
                    }
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showSearchDialog() {
        val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        val searchMode = prefs.getString("key_search_mode", "Immersive") ?: "Immersive"
        val shortcutSearch = prefs.getBoolean("key_shortcut_search", true)
        val hasContactPerm = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        val contactSearch = prefs.getBoolean("key_contact_search", false) && hasContactPerm

        val homePlacement = prefs.getString("key_search_bar_placement_home", "DOCK_BELOW") ?: "DOCK_BELOW"
        val homeLabel = when (homePlacement) {
            "DOCK_ABOVE" -> "Dock (Above icons)"
            "TOP" -> "Top"
            "HIDDEN" -> "Hidden"
            else -> "Dock (Below icons)"
        }
        val drawerPlacement = prefs.getString("key_search_bar_placement_drawer", "Top") ?: "Top"

        val options = arrayOf(
            "Search Mode: $searchMode",
            "Shortcut Search: ${if (shortcutSearch) "ON" else "OFF"}",
            "Contact Search: ${if (contactSearch) "ON" else "OFF"}",
            "Home Search Bar: $homeLabel",
            "Drawer Search Bar: $drawerPlacement",
            "Open Search Setup Wizard"
        )

        AlertDialog.Builder(this)
            .setTitle("Search Configuration")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val modes = arrayOf("Immersive", "Standard")
                        val activeIdx = modes.indexOf(searchMode).coerceAtLeast(0)
                        AlertDialog.Builder(this)
                            .setTitle("Search Mode")
                            .setSingleChoiceItems(modes, activeIdx) { d, idx ->
                                prefs.edit().putString("key_search_mode", modes[idx]).apply()
                                d.dismiss()
                                updateSummaryTexts()
                                Toast.makeText(this, "Search mode: ${modes[idx]}", Toast.LENGTH_SHORT).show()
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                    1 -> {
                        prefs.edit().putBoolean("key_shortcut_search", !shortcutSearch).apply()
                        updateSummaryTexts()
                        Toast.makeText(this, "Shortcut Search: ${if (!shortcutSearch) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        val newState = !contactSearch
                        if (newState && !hasContactPerm) {
                            Toast.makeText(this, "Grant Contacts permission to enable Contact Search", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, SetupActivity::class.java).apply {
                                putExtra("setup_step", 2)
                            })
                        } else {
                            prefs.edit().putBoolean("key_contact_search", newState).apply()
                            updateSummaryTexts()
                            Toast.makeText(this, "Contact Search: ${if (newState) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    3 -> {
                        val homeOptions = arrayOf("Dock (Below icons)", "Dock (Above icons)", "Top", "Hidden")
                        val homeVals = arrayOf("DOCK_BELOW", "DOCK_ABOVE", "TOP", "HIDDEN")
                        val activeIdx = homeVals.indexOf(homePlacement).coerceAtLeast(0)
                        AlertDialog.Builder(this)
                            .setTitle("Home Search Bar Placement")
                            .setSingleChoiceItems(homeOptions, activeIdx) { d, idx ->
                                prefs.edit().putString("key_search_bar_placement_home", homeVals[idx]).apply()
                                d.dismiss()
                                updateSummaryTexts()
                                Toast.makeText(this, "Home search bar set to ${homeOptions[idx]}", Toast.LENGTH_SHORT).show()
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                    4 -> {
                        val drawerOptions = arrayOf("Top", "Bottom", "Hidden")
                        val activeIdx = drawerOptions.indexOf(drawerPlacement).coerceAtLeast(0)
                        AlertDialog.Builder(this)
                            .setTitle("Drawer Search Bar Placement")
                            .setSingleChoiceItems(drawerOptions, activeIdx) { d, idx ->
                                prefs.edit().putString("key_search_bar_placement_drawer", drawerOptions[idx]).apply()
                                d.dismiss()
                                updateSummaryTexts()
                                Toast.makeText(this, "Drawer search bar set to ${drawerOptions[idx]}", Toast.LENGTH_SHORT).show()
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                    5 -> {
                        startActivity(Intent(this, SetupActivity::class.java).apply {
                            putExtra("setup_step", 2)
                        })
                    }
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showGesturesDialog() {
        val gestureActions = GestureAction.values().map { it.label }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Configure Swipe Up Gesture")
            .setSingleChoiceItems(gestureActions, 1) { dialog, which ->
                val selectedAction = GestureAction.values()[which]
                getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                    .edit().putString("key_gesture_swipe_up", selectedAction.name).apply()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showBackupRestoreDialog() {
        val options = arrayOf("Backup Workspace Layout & Settings", "Restore Layout & Settings")

        AlertDialog.Builder(this)
            .setTitle("Backup & Restore")
            .setItems(options) { _, _ ->
                Toast.makeText(this, "Backup action completed", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    companion object {
        private const val REQUEST_CODE_SET_DEFAULT = 1001
    }
}
