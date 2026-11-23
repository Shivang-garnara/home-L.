package com.example.launcherai.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.lifecycleScope
import com.example.launcherai.LauncherApplication
import com.example.launcherai.R
import com.example.launcherai.data.repository.GridChangeResult
import com.example.launcherai.databinding.ActivityHomeScreenSettingsBinding
import kotlinx.coroutines.launch

/**
 * Dedicated Home Screen Settings activity matching Nova Launcher 8.9.9.
 * Provides a live 5-icon preview card, icon size steppers, label visibility & typography controls,
 * interactive desktop grid dialog, and padding dialog.
 */
class HomeScreenSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeScreenSettingsBinding

    // Current State
    private var iconSizePercent: Int = 100
    private var labelVisible: Boolean = true
    private var labelSizePercent: Int = 100
    private var labelFont: String = "Normal"
    private var labelColor: Int = Color.WHITE
    private var labelShadow: Boolean = true
    private var labelSingleLine: Boolean = true

    private var gridCols: Int = 5
    private var gridRows: Int = 6
    private var paddingHorizontalStep: Int = 0
    private var paddingVerticalStep: Int = 0

    // Search Placement State
    private var searchBarPlacement: String = "DOCK_BELOW"

    // Scroll & Page Transitions State
    private var wallpaperScroll: String = "ON"
    private var widgetCornerRadius: Int = 16
    private var transitionEffect: String = "SIMPLE"
    private var infiniteScroll: Boolean = false
    private var pageIndicatorStyle: String = "DOTS"
    private var pageIndicatorColor: Int = Color.WHITE

    // New Apps State
    private var addIconToHome: Boolean = false

    // Advanced State
    private var desktopLock: Boolean = false
    private var desktopShadow: Boolean = false
    private var widgetOverlap: Boolean = false
    private var overlapWhenPlacing: Boolean = false
    private var wallpaperZoomEffects: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_NovaLight)
        super.onCreate(savedInstanceState)
        SystemBarHelper.applySystemBars(this, isLightTheme = true, transparentBars = false)
        binding = ActivityHomeScreenSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemBarHelper.applyEdgeToEdgeInsets(
            insetsRootView = binding.root,
            topInsetView = binding.headerContainer
        )

        loadPreferences()
        setupViews()
        setupListeners()
        updatePreview()
    }

    private fun loadPreferences() {
        val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        iconSizePercent = prefs.getInt("key_icon_size_percent", 100)
        labelVisible = prefs.getBoolean("key_label_visible", true)
        labelSizePercent = prefs.getInt("key_label_size_percent", 100)
        labelFont = prefs.getString("key_label_font", "Normal") ?: "Normal"
        labelColor = prefs.getInt("key_label_color", Color.WHITE)
        labelShadow = prefs.getBoolean("key_label_shadow", true)
        labelSingleLine = prefs.getBoolean("key_label_single_line", true)

        val repository = (application as LauncherApplication).repository
        val currentGrid = repository.getGridConfig()
        gridCols = currentGrid.cols
        gridRows = currentGrid.rows

        paddingHorizontalStep = prefs.getInt("key_desktop_padding_horizontal", 0)
        paddingVerticalStep = prefs.getInt("key_desktop_padding_vertical", 0)

        searchBarPlacement = prefs.getString("key_search_bar_placement_home", "DOCK_BELOW") ?: "DOCK_BELOW"

        wallpaperScroll = prefs.getString("key_wallpaper_scroll", "ON") ?: "ON"
        widgetCornerRadius = prefs.getInt("key_widget_corner_radius", 16)
        transitionEffect = prefs.getString("key_page_transition_effect", "SIMPLE") ?: "SIMPLE"
        infiniteScroll = prefs.getBoolean("key_infinite_scroll", false)
        pageIndicatorStyle = prefs.getString("key_page_indicator_style", "DOTS") ?: "DOTS"
        pageIndicatorColor = prefs.getInt("key_page_indicator_color", Color.WHITE)

        addIconToHome = prefs.getBoolean("key_add_icon_to_home", false)

        desktopLock = prefs.getBoolean("key_desktop_lock", false)
        desktopShadow = prefs.getBoolean("key_desktop_shadow", false)
        widgetOverlap = prefs.getBoolean("key_widget_overlap", false)
        overlapWhenPlacing = prefs.getBoolean("key_overlap_when_placing", false)
        wallpaperZoomEffects = prefs.getBoolean("key_wallpaper_zoom_effects", false)
    }

    private fun setupViews() {
        binding.tvIconSizeValue.text = "$iconSizePercent%"
        binding.sbIconSize.progress = iconSizePercent

        binding.swLabelEnabled.isChecked = labelVisible
        binding.layoutLabelSubOptions.visibility = if (labelVisible) View.VISIBLE else View.GONE

        binding.tvLabelSizeValue.text = "$labelSizePercent%"
        binding.sbLabelSize.progress = labelSizePercent

        binding.tvFontValue.text = labelFont
        updateLabelColorUI()
        binding.swShadowEnabled.isChecked = labelShadow
        binding.swSingleLineEnabled.isChecked = labelSingleLine

        updateGridSubtitle()
        updatePaddingSubtitle()
        updateDockSubtitle()
        updateSearchBarPlacementUI()
        updateScrollSectionUI()
        binding.swAddIconToHome.isChecked = addIconToHome
        updateAdvancedSectionUI()
    }

    override fun onResume() {
        super.onResume()
        loadPreferences()
        updateGridSubtitle()
        updatePaddingSubtitle()
        updateDockSubtitle()
        updateSearchBarPlacementUI()
        updateScrollSectionUI()
        binding.swAddIconToHome.isChecked = addIconToHome
        updateAdvancedSectionUI()
    }

    private fun updateSearchBarPlacementUI() {
        binding.tvSearchBarPlacementSubtitle.text = when (searchBarPlacement) {
            "NONE" -> "None"
            "TOP" -> "Persistent (Top of screen)"
            "DOCK_ABOVE" -> "Dock (Above icons)"
            else -> "Dock (Below icons)"
        }
    }

    private fun updateAdvancedSectionUI() {
        binding.swLockLayout.isChecked = desktopLock
        binding.swShowShadow.isChecked = desktopShadow
        binding.swWidgetOverlap.isChecked = widgetOverlap
        binding.swOverlapWhenPlacing.isChecked = overlapWhenPlacing
        binding.swWallpaperZoomEffects.isChecked = wallpaperZoomEffects

        binding.swOverlapWhenPlacing.isEnabled = widgetOverlap
        binding.rowOverlapWhenPlacing.alpha = if (widgetOverlap) 1.0f else 0.45f
    }

    private fun updateScrollSectionUI() {
        binding.tvWallpaperScrollingValue.text = when (wallpaperScroll) {
            "OFF" -> "Off"
            "REVERSE" -> "Reverse"
            else -> "On"
        }
        binding.tvWidgetCornerRadiusValue.text = "${widgetCornerRadius}dp"
        binding.tvTransitionEffectValue.text = when (transitionEffect) {
            "CUBE" -> "Cube"
            "CARD_STACK" -> "Card stack"
            "ACCORDION" -> "Accordion"
            "FLIP" -> "Flip"
            else -> "Simple"
        }
        binding.swInfiniteScroll.isChecked = infiniteScroll

        binding.tvPageIndicatorValue.text = when (pageIndicatorStyle) {
            "LINE" -> "Line"
            "NONE" -> "None"
            else -> "Dots"
        }
        binding.viewPageIndicatorColorSwatch.backgroundTintList = android.content.res.ColorStateList.valueOf(pageIndicatorColor)
        binding.viewPageIndicatorColorSwatch.visibility = if (pageIndicatorStyle == "NONE") View.GONE else View.VISIBLE
    }

    private fun updateDockSubtitle() {
        val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        val dockEnabled = prefs.getBoolean("key_dock_enabled", true)
        val repository = (application as LauncherApplication).repository
        val dockIcons = repository.getDockCapacity()
        val dockBg = prefs.getString("key_dock_bg_style", "Transparent") ?: "Transparent"
        binding.tvDockSubtitle.text = if (dockEnabled) {
            "$dockIcons icons • $dockBg"
        } else {
            "Disabled"
        }
    }

    private fun updateGridSubtitle() {
        binding.tvDesktopGridSubtitle.text = "$gridRows ✕ $gridCols"
    }

    private fun stepToLabel(step: Int): String = when (step) {
        1 -> "Small"
        2 -> "Medium"
        3 -> "Large"
        else -> "None"
    }

    private fun updatePaddingSubtitle() {
        binding.tvPaddingSubtitle.text = "${stepToLabel(paddingHorizontalStep)} ✕ ${stepToLabel(paddingVerticalStep)}"
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Icon Size controls
        binding.btnIconSizeMinus.setOnClickListener {
            if (iconSizePercent > 70) {
                iconSizePercent -= 5
                binding.sbIconSize.progress = iconSizePercent
                saveIconSize()
            }
        }

        binding.btnIconSizePlus.setOnClickListener {
            if (iconSizePercent < 150) {
                iconSizePercent += 5
                binding.sbIconSize.progress = iconSizePercent
                saveIconSize()
            }
        }

        binding.sbIconSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                iconSizePercent = progress
                binding.tvIconSizeValue.text = "$iconSizePercent%"
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                saveIconSize()
            }
        })

        // Label Switch
        binding.swLabelEnabled.setOnCheckedChangeListener { _, isChecked ->
            labelVisible = isChecked
            binding.layoutLabelSubOptions.visibility = if (isChecked) View.VISIBLE else View.GONE
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("key_label_visible", isChecked).apply()
            updatePreview()
        }

        binding.rowLabelToggle.setOnClickListener {
            binding.swLabelEnabled.toggle()
        }

        // Label Size Slider
        binding.sbLabelSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                labelSizePercent = progress
                binding.tvLabelSizeValue.text = "$labelSizePercent%"
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                prefs.edit().putInt("key_label_size_percent", labelSizePercent).apply()
            }
        })

        // Font family selection dialog
        binding.rowFont.setOnClickListener {
            showFontDialog()
        }

        // Color selection dialog
        binding.rowLabelColor.setOnClickListener {
            showLabelColorDialog()
        }

        // Shadow Switch
        binding.swShadowEnabled.setOnCheckedChangeListener { _, isChecked ->
            labelShadow = isChecked
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("key_label_shadow", isChecked).apply()
            updatePreview()
        }
        binding.rowShadow.setOnClickListener {
            binding.swShadowEnabled.toggle()
        }

        // Single line Switch
        binding.swSingleLineEnabled.setOnCheckedChangeListener { _, isChecked ->
            labelSingleLine = isChecked
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("key_label_single_line", isChecked).apply()
            updatePreview()
        }
        binding.rowSingleLine.setOnClickListener {
            binding.swSingleLineEnabled.toggle()
        }

        // Desktop Grid row
        binding.rowDesktopGrid.setOnClickListener {
            showDesktopGridDialog()
        }

        // Padding row
        binding.rowPadding.setOnClickListener {
            showPaddingDialog()
        }

        // Dock row
        binding.rowDock.setOnClickListener {
            startActivity(android.content.Intent(this, DockSettingsActivity::class.java))
        }

        // Search Placement Listener
        binding.rowSearchBarPlacement.setOnClickListener {
            showSearchBarPlacementDialog()
        }

        // Scroll & Transitions Listeners
        binding.rowWallpaperScrolling.setOnClickListener {
            showWallpaperScrollingDialog()
        }

        binding.rowWidgetCornerRadius.setOnClickListener {
            showWidgetCornerRadiusDialog()
        }

        binding.rowTransitionEffect.setOnClickListener {
            showTransitionEffectDialog()
        }

        binding.rowInfiniteScroll.setOnClickListener {
            binding.swInfiniteScroll.toggle()
        }
        binding.swInfiniteScroll.setOnCheckedChangeListener { _, isChecked ->
            infiniteScroll = isChecked
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("key_infinite_scroll", isChecked).apply()
        }

        binding.rowPageIndicator.setOnClickListener {
            showPageIndicatorDialog()
        }

        // New Apps Listeners
        binding.rowAddIconToHome.setOnClickListener {
            binding.swAddIconToHome.toggle()
        }
        binding.swAddIconToHome.setOnCheckedChangeListener { _, isChecked ->
            addIconToHome = isChecked
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("key_add_icon_to_home", isChecked).apply()
        }

        // Advanced Listeners
        binding.rowLockLayout.setOnClickListener {
            binding.swLockLayout.toggle()
        }
        binding.swLockLayout.setOnCheckedChangeListener { _, isChecked ->
            desktopLock = isChecked
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("key_desktop_lock", isChecked).apply()
        }

        binding.rowShowShadow.setOnClickListener {
            binding.swShowShadow.toggle()
        }
        binding.swShowShadow.setOnCheckedChangeListener { _, isChecked ->
            desktopShadow = isChecked
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("key_desktop_shadow", isChecked).apply()
        }

        binding.rowWidgetOverlap.setOnClickListener {
            binding.swWidgetOverlap.toggle()
        }
        binding.swWidgetOverlap.setOnCheckedChangeListener { _, isChecked ->
            widgetOverlap = isChecked
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("key_widget_overlap", isChecked).apply()
            if (!isChecked && overlapWhenPlacing) {
                overlapWhenPlacing = false
                prefs.edit().putBoolean("key_overlap_when_placing", false).apply()
            }
            updateAdvancedSectionUI()
        }

        binding.rowOverlapWhenPlacing.setOnClickListener {
            if (widgetOverlap) {
                binding.swOverlapWhenPlacing.toggle()
            }
        }
        binding.swOverlapWhenPlacing.setOnCheckedChangeListener { _, isChecked ->
            if (widgetOverlap) {
                overlapWhenPlacing = isChecked
                val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("key_overlap_when_placing", isChecked).apply()
            }
        }

        binding.rowWallpaperZoomEffects.setOnClickListener {
            binding.swWallpaperZoomEffects.toggle()
        }
        binding.swWallpaperZoomEffects.setOnCheckedChangeListener { _, isChecked ->
            wallpaperZoomEffects = isChecked
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("key_wallpaper_zoom_effects", isChecked).apply()
        }
    }

    private fun saveIconSize() {
        val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("key_icon_size_percent", iconSizePercent).apply()
        updatePreview()
    }

    private fun getColorName(color: Int): String = when (color and 0x00FFFFFF) {
        0xFFFFFF -> "White"
        0x212121 -> "Charcoal"
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

    private fun updateLabelColorUI() {
        binding.tvLabelColorName.text = getColorName(labelColor)
        val density = resources.displayMetrics.density
        val strokePx = (1.5f * density).toInt()
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(labelColor)
            setStroke(strokePx, 0x44000000.toInt())
        }
        binding.viewLabelColorSwatch.background = drawable
    }

    private fun showLabelColorDialog() {
        val dialog = LabelColorDialogFragment.newInstance()
        dialog.setOnColorSavedListener { color ->
            labelColor = color
            updateLabelColorUI()
            updatePreview()
        }
        dialog.show(supportFragmentManager, LabelColorDialogFragment.TAG)
    }

    private fun showFontDialog() {
        val fonts = arrayOf("Normal", "Medium", "Condensed", "Light")
        val currentIndex = fonts.indexOf(labelFont).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Font")
            .setSingleChoiceItems(fonts, currentIndex) { dialog, which ->
                labelFont = fonts[which]
                binding.tvFontValue.text = labelFont
                val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString("key_label_font", labelFont).apply()
                updatePreview()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDesktopGridDialog() {
        val dialog = DesktopGridDialogFragment.newInstance()
        dialog.setOnGridSavedListener { cols, rows, subgrid ->
            lifecycleScope.launch {
                val repository = (application as LauncherApplication).repository
                val result = repository.setGridConfig(cols, rows)
                if (result is GridChangeResult.Success) {
                    gridCols = cols
                    gridRows = rows
                    updateGridSubtitle()
                } else if (result is GridChangeResult.Failure) {
                    Toast.makeText(this@HomeScreenSettingsActivity, result.reason, Toast.LENGTH_LONG).show()
                }
            }
        }
        dialog.show(supportFragmentManager, DesktopGridDialogFragment.TAG)
    }

    private fun showPaddingDialog() {
        val dialog = PaddingDialogFragment.newInstance()
        dialog.setOnPaddingSavedListener { horizStep, vertStep ->
            paddingHorizontalStep = horizStep
            paddingVerticalStep = vertStep
            updatePaddingSubtitle()
        }
        dialog.show(supportFragmentManager, PaddingDialogFragment.TAG)
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

        // Scale icons: base 48dp * (iconSizePercent / 100)
        val density = resources.displayMetrics.density
        val iconPx = (48f * density * (iconSizePercent / 100f)).toInt()
        previewIcons.forEach { iv ->
            iv.layoutParams = iv.layoutParams.apply {
                width = iconPx
                height = iconPx
            }
        }

        // Apply label visibility & typography
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

    private fun showWallpaperScrollingDialog() {
        val options = arrayOf("On", "Off", "Reverse")
        val currentIndex = when (wallpaperScroll) {
            "OFF" -> 1
            "REVERSE" -> 2
            else -> 0
        }
        AlertDialog.Builder(this)
            .setTitle("Wallpaper scrolling")
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                wallpaperScroll = when (which) {
                    1 -> "OFF"
                    2 -> "REVERSE"
                    else -> "ON"
                }
                val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString("key_wallpaper_scroll", wallpaperScroll).apply()
                updateScrollSectionUI()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showWidgetCornerRadiusDialog() {
        val dialog = WidgetCornerRadiusDialogFragment.newInstance()
        dialog.setOnRadiusSavedListener { radius ->
            widgetCornerRadius = radius
            updateScrollSectionUI()
        }
        dialog.show(supportFragmentManager, WidgetCornerRadiusDialogFragment.TAG)
    }

    private fun showTransitionEffectDialog() {
        val options = arrayOf("Simple", "Cube", "Card stack", "Accordion", "Flip")
        val currentIndex = when (transitionEffect) {
            "CUBE" -> 1
            "CARD_STACK" -> 2
            "ACCORDION" -> 3
            "FLIP" -> 4
            else -> 0
        }
        AlertDialog.Builder(this)
            .setTitle("Transition effect")
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                transitionEffect = when (which) {
                    1 -> "CUBE"
                    2 -> "CARD_STACK"
                    3 -> "ACCORDION"
                    4 -> "FLIP"
                    else -> "SIMPLE"
                }
                val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString("key_page_transition_effect", transitionEffect).apply()
                updateScrollSectionUI()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPageIndicatorDialog() {
        val options = arrayOf("Dots", "Line", "None", "Change Accent Color...")
        AlertDialog.Builder(this)
            .setTitle("Page indicator")
            .setItems(options) { dialog, which ->
                val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                when (which) {
                    0 -> {
                        pageIndicatorStyle = "DOTS"
                        prefs.edit().putString("key_page_indicator_style", "DOTS").apply()
                        updateScrollSectionUI()
                    }
                    1 -> {
                        pageIndicatorStyle = "LINE"
                        prefs.edit().putString("key_page_indicator_style", "LINE").apply()
                        updateScrollSectionUI()
                    }
                    2 -> {
                        pageIndicatorStyle = "NONE"
                        prefs.edit().putString("key_page_indicator_style", "NONE").apply()
                        updateScrollSectionUI()
                    }
                    3 -> {
                        showPageIndicatorColorDialog()
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPageIndicatorColorDialog() {
        val dialog = LabelColorDialogFragment.newInstance()
        dialog.setOnColorSavedListener { color ->
            pageIndicatorColor = color
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putInt("key_page_indicator_color", color).apply()
            updateScrollSectionUI()
        }
        dialog.show(supportFragmentManager, LabelColorDialogFragment.TAG)
    }

    private fun showSearchBarPlacementDialog() {
        val options = arrayOf(
            "None",
            "Persistent (Top of screen)",
            "Dock (Above icons)",
            "Dock (Below icons)"
        )
        val values = arrayOf("NONE", "TOP", "DOCK_ABOVE", "DOCK_BELOW")
        val currentIndex = values.indexOf(searchBarPlacement).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Search bar placement")
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                searchBarPlacement = values[which]
                val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString("key_search_bar_placement_home", searchBarPlacement).apply()
                updateSearchBarPlacementUI()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
