package com.example.launcherai.ui

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.lifecycleScope
import com.example.launcherai.LauncherApplication
import com.example.launcherai.R
import com.example.launcherai.databinding.ActivityAppDrawerSettingsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Dedicated App Drawer Settings activity matching Nova Launcher 8.9.9 (SettingsDrawer / drawer_* preferences).
 * Provides live 5-icon preview, drawer layout styles, grid stepper, independent icon/typography styling,
 * gestures (animations, swipe indicator, pull to search), search placement, and smart folders.
 */
class AppDrawerSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppDrawerSettingsBinding

    // Layout State
    private var drawerStyle: String = "Vertical"
    private var gridCols: Int = 5
    private var gridRows: Int = 6
    private var iconSizePercent: Int = 100
    private var labelVisible: Boolean = true
    private var labelSizePercent: Int = 100
    private var labelFont: String = "Normal"
    private var labelColor: Int = 0xFF2C2523.toInt()
    private var labelShadow: Boolean = false
    private var labelSingleLine: Boolean = true

    // Appearance State
    private var drawerBgColor: Int = 0xFFF7F2EE.toInt()
    private var drawerBgAlpha: Int = 0
    private var fastScrollColor: Int = 0xFFD54526.toInt()

    // Gestures State
    private var drawerAnimation: String = "SLIDE"
    private var swipeToOpen: Boolean = true
    private var swipeIndicator: Boolean = true
    private var pullToSearch: Boolean = true

    // Search State
    private var searchBarPlacement: String = "Top"
    private var showKeyboard: Boolean = true
    private var searchIncognito: Boolean = false

    // Apps State
    private var frequentApps: Boolean = true
    private var smartFolders: Boolean = true
    private var foldersFirst: Boolean = false

    // Advanced State
    private var rememberPosition: Boolean = false
    private var autoClose: Boolean = true
    private var fastScroll: Boolean = true
    private var swipeHaptic: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_NovaLight)
        super.onCreate(savedInstanceState)
        SystemBarHelper.applySystemBars(this, isLightTheme = true, transparentBars = false)
        binding = ActivityAppDrawerSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemBarHelper.applyEdgeToEdgeInsets(
            insetsRootView = binding.root,
            topInsetView = binding.headerContainer,
            bottomInsetView = null
        )

        loadPreferences()
        setupUI()
        setupListeners()
        updatePreview()
    }

    override fun onResume() {
        super.onResume()
        updateDrawerGroupsSubtitle()
    }

    private fun loadPreferences() {
        val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)

        // Layout
        drawerStyle = prefs.getString("key_app_drawer_style", "Vertical") ?: "Vertical"
        gridCols = prefs.getInt("key_drawer_grid_cols", 5)
        gridRows = prefs.getInt("key_drawer_grid_rows", 6)
        iconSizePercent = prefs.getInt("key_drawer_icon_size_percent", 100)
        labelVisible = prefs.getBoolean("key_drawer_label_visible", true)
        labelSizePercent = prefs.getInt("key_drawer_label_size_percent", 100)
        labelFont = prefs.getString("key_drawer_label_font", "Normal") ?: "Normal"
        labelColor = prefs.getInt("key_drawer_label_color", 0xFF2C2523.toInt())
        labelShadow = prefs.getBoolean("key_drawer_label_shadow", false)
        labelSingleLine = prefs.getBoolean("key_drawer_label_single_line", true)

        // Appearance
        drawerBgColor = prefs.getInt("key_drawer_background_color", 0xFFF7F2EE.toInt())
        drawerBgAlpha = prefs.getInt("key_drawer_background_alpha", 0)
        fastScrollColor = prefs.getInt("key_drawer_fastscroll_color", 0xFFD54526.toInt())

        // Gestures
        drawerAnimation = prefs.getString("key_drawer_animation", "SLIDE") ?: "SLIDE"
        swipeToOpen = prefs.getBoolean("key_drawer_swipe_to_open", true)
        swipeIndicator = prefs.getBoolean("key_drawer_swipe_indicator", true)
        pullToSearch = prefs.getBoolean("key_drawer_pull_to_search", true)

        // Search
        searchBarPlacement = prefs.getString("key_search_bar_placement_drawer", "Top") ?: "Top"
        if (searchBarPlacement.equals("Hidden", ignoreCase = true)) searchBarPlacement = "None"
        showKeyboard = prefs.getBoolean("key_drawer_show_keyboard", true)
        searchIncognito = prefs.getBoolean("key_drawer_search_incognito", false)

        // Apps
        frequentApps = prefs.getBoolean("key_drawer_frequent_apps", true)
        smartFolders = prefs.getBoolean("key_drawer_smart_folders", true)
        foldersFirst = prefs.getBoolean("key_drawer_folders_first", true)

        // Advanced
        rememberPosition = prefs.getBoolean("key_drawer_remember_position", false)
        autoClose = prefs.getBoolean("key_drawer_autoclose", true)
        fastScroll = prefs.getBoolean("key_drawer_fastscroll", true)
        swipeHaptic = prefs.getBoolean("key_drawer_swipe_haptic", true)
    }

    private fun setupUI() {
        // Layout UI
        binding.tvDrawerStyleSubtitle.text = drawerStyle
        binding.tvDrawerGridSubtitle.text = "$gridRows ✕ $gridCols"

        binding.sbDrawerIconSize.progress = iconSizePercent
        binding.tvDrawerIconSizeValue.text = "$iconSizePercent%"

        binding.swDrawerLabelEnabled.isChecked = labelVisible
        binding.layoutDrawerLabelSubOptions.visibility = if (labelVisible) View.VISIBLE else View.GONE
        binding.sbDrawerLabelSize.progress = labelSizePercent
        binding.tvDrawerLabelSizeValue.text = "$labelSizePercent%"
        binding.tvDrawerFontValue.text = labelFont
        updateLabelColorUI()
        binding.swDrawerShadowEnabled.isChecked = labelShadow
        binding.swDrawerSingleLineEnabled.isChecked = labelSingleLine

        // Appearance UI
        updateDrawerBgColorUI()
        binding.sbDrawerBgAlpha.progress = drawerBgAlpha
        binding.tvDrawerBgAlphaValue.text = "$drawerBgAlpha%"
        updateFastScrollColorUI()

        // Gestures UI
        binding.tvDrawerAnimationValue.text = when (drawerAnimation.uppercase()) {
            "ZOOM" -> "Zoom"
            "FADE" -> "Fade"
            else -> "Slide"
        }
        binding.swDrawerSwipeToOpen.isChecked = swipeToOpen
        binding.swDrawerSwipeIndicator.isChecked = swipeIndicator
        binding.swDrawerPullToSearch.isChecked = pullToSearch

        // Search UI
        binding.tvDrawerSearchBarPlacementSubtitle.text = searchBarPlacement
        binding.swDrawerShowKeyboard.isChecked = showKeyboard
        binding.swDrawerSearchIncognito.isChecked = searchIncognito

        // Apps UI
        binding.swDrawerFrequentApps.isChecked = frequentApps
        binding.rowDrawerSmartFolders.visibility = android.view.View.GONE
        binding.swDrawerFoldersFirst.isChecked = foldersFirst
        updateHideAppsSubtitle()

        // Advanced UI
        binding.swDrawerRememberPosition.isChecked = rememberPosition
        binding.swDrawerAutoClose.isChecked = autoClose
        binding.swDrawerFastScroll.isChecked = fastScroll
        binding.swDrawerSwipeHaptic.isChecked = swipeHaptic
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Layout Listeners
        binding.rowDrawerStyle.setOnClickListener {
            showDrawerStyleDialog()
        }

        binding.rowDrawerGrid.setOnClickListener {
            showDrawerGridDialog()
        }

        binding.btnDrawerIconSizeMinus.setOnClickListener {
            if (iconSizePercent > 70) {
                iconSizePercent -= 5
                binding.sbDrawerIconSize.progress = iconSizePercent
                saveIconSize()
            }
        }

        binding.btnDrawerIconSizePlus.setOnClickListener {
            if (iconSizePercent < 150) {
                iconSizePercent += 5
                binding.sbDrawerIconSize.progress = iconSizePercent
                saveIconSize()
            }
        }

        binding.sbDrawerIconSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                iconSizePercent = progress
                binding.tvDrawerIconSizeValue.text = "$iconSizePercent%"
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                saveIconSize()
            }
        })

        binding.swDrawerLabelEnabled.setOnCheckedChangeListener { _, isChecked ->
            labelVisible = isChecked
            binding.layoutDrawerLabelSubOptions.visibility = if (isChecked) View.VISIBLE else View.GONE
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("key_drawer_label_visible", isChecked).apply()
            updatePreview()
        }

        binding.rowDrawerLabelToggle.setOnClickListener {
            binding.swDrawerLabelEnabled.toggle()
        }

        binding.sbDrawerLabelSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                labelSizePercent = progress
                binding.tvDrawerLabelSizeValue.text = "$labelSizePercent%"
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                prefs.edit().putInt("key_drawer_label_size_percent", labelSizePercent).apply()
            }
        })

        binding.rowDrawerFont.setOnClickListener {
            showFontDialog()
        }

        binding.rowDrawerLabelColor.setOnClickListener {
            showLabelColorDialog()
        }

        binding.swDrawerShadowEnabled.setOnCheckedChangeListener { _, isChecked ->
            labelShadow = isChecked
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("key_drawer_label_shadow", isChecked).apply()
            updatePreview()
        }
        binding.rowDrawerShadow.setOnClickListener {
            binding.swDrawerShadowEnabled.toggle()
        }

        binding.swDrawerSingleLineEnabled.setOnCheckedChangeListener { _, isChecked ->
            labelSingleLine = isChecked
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("key_drawer_label_single_line", isChecked).apply()
            updatePreview()
        }
        binding.rowDrawerSingleLine.setOnClickListener {
            binding.swDrawerSingleLineEnabled.toggle()
        }

        // Appearance Listeners
        binding.rowDrawerBackgroundColor.setOnClickListener {
            showDrawerBgColorDialog()
        }

        binding.sbDrawerBgAlpha.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                drawerBgAlpha = progress
                binding.tvDrawerBgAlphaValue.text = "$drawerBgAlpha%"
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                prefs.edit().putInt("key_drawer_background_alpha", drawerBgAlpha).apply()
            }
        })

        binding.rowDrawerFastScrollAccentColor.setOnClickListener {
            showFastScrollColorDialog()
        }

        // Gestures Listeners
        binding.rowDrawerAnimation.setOnClickListener {
            showDrawerAnimationDialog()
        }

        binding.rowDrawerSwipeToOpen.setOnClickListener {
            binding.swDrawerSwipeToOpen.toggle()
        }
        binding.swDrawerSwipeToOpen.setOnCheckedChangeListener { _, isChecked ->
            swipeToOpen = isChecked
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("key_drawer_swipe_to_open", isChecked).apply()
        }

        binding.rowDrawerSwipeIndicator.setOnClickListener {
            binding.swDrawerSwipeIndicator.toggle()
        }
        binding.swDrawerSwipeIndicator.setOnCheckedChangeListener { _, isChecked ->
            swipeIndicator = isChecked
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("key_drawer_swipe_indicator", isChecked).apply()
        }

        binding.rowDrawerPullToSearch.setOnClickListener {
            binding.swDrawerPullToSearch.toggle()
        }
        binding.swDrawerPullToSearch.setOnCheckedChangeListener { _, isChecked ->
            pullToSearch = isChecked
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("key_drawer_pull_to_search", isChecked).apply()
        }

        // Search Listeners
        binding.rowDrawerSearchBarPlacement.setOnClickListener {
            showSearchBarPlacementDialog()
        }

        binding.rowDrawerShowKeyboard.setOnClickListener {
            binding.swDrawerShowKeyboard.toggle()
        }
        binding.swDrawerShowKeyboard.setOnCheckedChangeListener { _, isChecked ->
            showKeyboard = isChecked
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("key_drawer_show_keyboard", isChecked).apply()
        }

        binding.rowDrawerSearchIncognito.setOnClickListener {
            binding.swDrawerSearchIncognito.toggle()
        }
        binding.swDrawerSearchIncognito.setOnCheckedChangeListener { _, isChecked ->
            searchIncognito = isChecked
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("key_drawer_search_incognito", isChecked).apply()
        }

        // Apps Listeners
        binding.rowDrawerFrequentApps.setOnClickListener {
            binding.swDrawerFrequentApps.toggle()
        }
        binding.swDrawerFrequentApps.setOnCheckedChangeListener { _, isChecked ->
            frequentApps = isChecked
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("key_drawer_frequent_apps", isChecked).apply()
        }

        binding.rowDrawerSmartFolders.setOnClickListener {
            binding.swDrawerSmartFolders.toggle()
        }
        binding.swDrawerSmartFolders.setOnCheckedChangeListener { _, isChecked ->
            smartFolders = isChecked
            lifecycleScope.launch(Dispatchers.IO) {
                val repo = (application as? LauncherApplication)?.repository
                if (repo != null) {
                    repo.setSmartFoldersEnabled(isChecked)
                } else {
                    getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                        .edit().putBoolean("key_drawer_smart_folders", isChecked).apply()
                }
                withContext(Dispatchers.Main) {
                    updateDrawerGroupsSubtitle()
                }
            }
        }

        binding.rowDrawerFoldersFirst.setOnClickListener {
            binding.swDrawerFoldersFirst.toggle()
        }
        binding.swDrawerFoldersFirst.setOnCheckedChangeListener { _, isChecked ->
            foldersFirst = isChecked
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("key_drawer_folders_first", isChecked).apply()
        }

        binding.rowDrawerGroups.setOnClickListener {
            startActivity(Intent(this, DrawerGroupsActivity::class.java))
        }

        binding.rowDrawerHideApps.setOnClickListener {
            showHideAppsDialog()
        }

        // Advanced Listeners
        binding.rowDrawerRememberPosition.setOnClickListener {
            binding.swDrawerRememberPosition.toggle()
        }
        binding.swDrawerRememberPosition.setOnCheckedChangeListener { _, isChecked ->
            rememberPosition = isChecked
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("key_drawer_remember_position", isChecked).apply()
        }

        binding.rowDrawerAutoClose.setOnClickListener {
            binding.swDrawerAutoClose.toggle()
        }
        binding.swDrawerAutoClose.setOnCheckedChangeListener { _, isChecked ->
            autoClose = isChecked
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("key_drawer_autoclose", isChecked).apply()
        }

        binding.rowDrawerFastScroll.setOnClickListener {
            binding.swDrawerFastScroll.toggle()
        }
        binding.swDrawerFastScroll.setOnCheckedChangeListener { _, isChecked ->
            fastScroll = isChecked
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("key_drawer_fastscroll", isChecked).apply()
        }

        binding.rowDrawerSwipeHaptic.setOnClickListener {
            binding.swDrawerSwipeHaptic.toggle()
        }
        binding.swDrawerSwipeHaptic.setOnCheckedChangeListener { _, isChecked ->
            swipeHaptic = isChecked
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("key_drawer_swipe_haptic", isChecked).apply()
        }
    }

    private fun saveIconSize() {
        val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("key_drawer_icon_size_percent", iconSizePercent).apply()
        updatePreview()
    }

    private fun showDrawerStyleDialog() {
        val styles = arrayOf("Vertical", "List", "Horizontal")
        val currentIndex = styles.indexOf(drawerStyle).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("App drawer style")
            .setSingleChoiceItems(styles, currentIndex) { dialog, which ->
                drawerStyle = styles[which]
                binding.tvDrawerStyleSubtitle.text = drawerStyle
                val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString("key_app_drawer_style", drawerStyle).apply()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDrawerGridDialog() {
        val dialog = DrawerGridDialogFragment.newInstance()
        dialog.setOnGridSavedListener { cols, rows ->
            gridCols = cols
            gridRows = rows
            binding.tvDrawerGridSubtitle.text = "$gridRows ✕ $gridCols"
        }
        dialog.show(supportFragmentManager, DrawerGridDialogFragment.TAG)
    }

    private fun showDrawerAnimationDialog() {
        val animations = arrayOf("Slide", "Zoom", "Fade")
        val values = arrayOf("SLIDE", "ZOOM", "FADE")
        val currentIndex = values.indexOf(drawerAnimation.uppercase()).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Transition animation")
            .setSingleChoiceItems(animations, currentIndex) { dialog, which ->
                drawerAnimation = values[which]
                binding.tvDrawerAnimationValue.text = animations[which]
                val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString("key_drawer_animation", drawerAnimation).apply()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSearchBarPlacementDialog() {
        val placements = arrayOf("Top", "Bottom", "None")
        val currentIndex = placements.indexOf(searchBarPlacement).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Search bar placement")
            .setSingleChoiceItems(placements, currentIndex) { dialog, which ->
                searchBarPlacement = placements[which]
                binding.tvDrawerSearchBarPlacementSubtitle.text = searchBarPlacement
                val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString("key_search_bar_placement_drawer", searchBarPlacement).apply()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showFontDialog() {
        val fonts = arrayOf("Normal", "Medium", "Condensed", "Light")
        val currentIndex = fonts.indexOf(labelFont).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Font")
            .setSingleChoiceItems(fonts, currentIndex) { dialog, which ->
                labelFont = fonts[which]
                binding.tvDrawerFontValue.text = labelFont
                val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString("key_drawer_label_font", labelFont).apply()
                updatePreview()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLabelColorDialog() {
        val dialog = LabelColorDialogFragment.newInstance()
        dialog.setOnColorSavedListener { color ->
            labelColor = color
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putInt("key_drawer_label_color", color).apply()
            updateLabelColorUI()
            updatePreview()
        }
        dialog.show(supportFragmentManager, LabelColorDialogFragment.TAG)
    }

    private fun showDrawerBgColorDialog() {
        val dialog = DrawerColorPickerDialogFragment.newInstance(
            title = "Drawer background color",
            initialColor = drawerBgColor
        )
        dialog.setOnColorSavedListener { color ->
            drawerBgColor = color
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putInt("key_drawer_background_color", color).apply()
            updateDrawerBgColorUI()
            updatePreview()
        }
        dialog.show(supportFragmentManager, "drawer_bg_color_dialog")
    }

    private fun showFastScrollColorDialog() {
        val dialog = DrawerColorPickerDialogFragment.newInstance(
            title = "Fast scroll accent color",
            initialColor = fastScrollColor
        )
        dialog.setOnColorSavedListener { color ->
            fastScrollColor = color
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putInt("key_drawer_fastscroll_color", color).apply()
            updateFastScrollColorUI()
        }
        dialog.show(supportFragmentManager, "drawer_fastscroll_color_dialog")
    }

    private fun updateDrawerBgColorUI() {
        binding.tvDrawerBackgroundColorSubtitle.text = "${getColorName(drawerBgColor)} (${String.format("#%06X", drawerBgColor and 0x00FFFFFF)})"
        val density = resources.displayMetrics.density
        val strokePx = (1.5f * density).toInt()
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(drawerBgColor or 0xFF000000.toInt())
            setStroke(strokePx, 0x44000000.toInt())
        }
        binding.viewDrawerBgColorSwatch.background = drawable
    }

    private fun updateFastScrollColorUI() {
        binding.tvDrawerFastScrollColorSubtitle.text = "${getColorName(fastScrollColor)} (${String.format("#%06X", fastScrollColor and 0x00FFFFFF)})"
        val density = resources.displayMetrics.density
        val strokePx = (1.5f * density).toInt()
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(fastScrollColor or 0xFF000000.toInt())
            setStroke(strokePx, 0x44000000.toInt())
        }
        binding.viewDrawerFastScrollColorSwatch.background = drawable
    }

    private fun updateDrawerGroupsSubtitle() {
        lifecycleScope.launch(Dispatchers.IO) {
            val folders = (application as? LauncherApplication)?.repository?.getDrawerFoldersWithChildrenSync()
                ?: emptyList()
            withContext(Dispatchers.Main) {
                binding.tvDrawerGroupsSubtitle.text = when (folders.size) {
                    0 -> "Customize folders in the drawer"
                    1 -> "1 drawer folder configured"
                    else -> "${folders.size} drawer folders configured"
                }
            }
        }
    }

    private fun updateHideAppsSubtitle() {
        val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        val hidden = prefs.getStringSet("key_hidden_packages", emptySet()) ?: emptySet()
        binding.tvDrawerHideAppsSubtitle.text = if (hidden.isEmpty()) {
            "None hidden"
        } else if (hidden.size == 1) {
            "1 application hidden"
        } else {
            "${hidden.size} applications hidden"
        }
    }

    private fun showHideAppsDialog() {
        val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        val hidden = prefs.getStringSet("key_hidden_packages", emptySet()) ?: emptySet()
        val dialog = HiddenAppsDialogFragment.newInstance(
            hiddenPackages = hidden,
            apps = null
        ) { _ ->
            updateHideAppsSubtitle()
        }
        dialog.show(supportFragmentManager, HiddenAppsDialogFragment.TAG)
    }

    private fun updateLabelColorUI() {
        binding.tvDrawerLabelColorName.text = getColorName(labelColor)
        val density = resources.displayMetrics.density
        val strokePx = (1.5f * density).toInt()
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(labelColor)
            setStroke(strokePx, 0x44000000.toInt())
        }
        binding.viewDrawerLabelColorSwatch.background = drawable
    }

    private fun getColorName(color: Int): String = when (color and 0x00FFFFFF) {
        0xFFFFFF -> "White"
        0x212121, 0x2C2523 -> "Charcoal"
        0xB0B0B0 -> "Gray"
        0xF44336 -> "Red"
        0xFF9800 -> "Orange"
        0xFFEB3B -> "Yellow"
        0x4CAF50 -> "Green"
        0x2196F3 -> "Blue"
        0x9C27B0 -> "Purple"
        0xE91E63 -> "Pink"
        else -> String.format("#%06X", color and 0x00FFFFFF)
    }

    private fun updatePreview() {
        val previewIcons = listOf(
            binding.ivPreview1,
            binding.ivPreview2,
            binding.ivPreview3,
            binding.ivPreview4,
            binding.ivPreview5
        )
        val previewLabels = listOf(
            binding.tvPreview1,
            binding.tvPreview2,
            binding.tvPreview3,
            binding.tvPreview4,
            binding.tvPreview5
        )

        val density = resources.displayMetrics.density

        // Apply drawer background color and transparency (alpha)
        val alpha255 = (((100 - drawerBgAlpha) * 255) / 100).coerceIn(0, 255)
        val compositeColor = ColorUtils.setAlphaComponent(drawerBgColor, alpha255)
        val cornerPx = 16f * density
        val cardBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = cornerPx
            setColor(compositeColor)
            setStroke((1.5f * density).toInt(), 0x1A000000)
        }
        binding.layoutPreviewCard.background = cardBg

        // Scale icons: base 48dp * (iconSizePercent / 100)
        val iconPx = (48f * density * (iconSizePercent / 100f)).toInt()
        previewIcons.forEach { iv ->
            iv.layoutParams = iv.layoutParams.apply {
                width = iconPx
                height = iconPx
            }
        }

        // Apply label typography
        val typeface = when (labelFont) {
            "Medium" -> Typeface.create("sans-serif-medium", Typeface.NORMAL)
            "Condensed" -> Typeface.create("sans-serif-condensed", Typeface.NORMAL)
            "Light" -> Typeface.create("sans-serif-light", Typeface.NORMAL)
            else -> Typeface.DEFAULT
        }

        val baseTextSizeSp = 11f
        val scaledTextSize = baseTextSizeSp * (labelSizePercent / 100f)

        previewLabels.forEach { tv ->
            if (!labelVisible) {
                tv.visibility = View.GONE
            } else {
                tv.visibility = View.VISIBLE
                tv.textSize = scaledTextSize
                tv.typeface = typeface
                tv.setTextColor(labelColor)
                val isDark = ColorUtils.calculateLuminance(labelColor) < 0.5
                if (labelShadow) {
                    if (isDark) {
                        tv.setShadowLayer(3f, 0f, 1f, 0x44FFFFFF.toInt())
                    } else {
                        tv.setShadowLayer(4f, 0f, 2f, 0x88000000.toInt())
                    }
                } else {
                    tv.setShadowLayer(0f, 0f, 0f, 0)
                }
                if (labelSingleLine) {
                    tv.maxLines = 1
                    tv.isSingleLine = true
                } else {
                    tv.maxLines = 2
                    tv.isSingleLine = false
                }
            }
        }
    }
}
