package com.example.launcherai.ui

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.lifecycleScope
import com.example.launcherai.R
import com.example.launcherai.util.AppManager
import com.example.launcherai.databinding.ActivityFolderSettingsBinding
import com.example.launcherai.icons.FolderGeometry
import com.example.launcherai.icons.FolderIconRenderer
import com.example.launcherai.icons.IconShapeMasker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Nova Launcher Folders Settings Screen (SettingsFolder + FolderWindowSettings).
 * Includes live wallpaper opened-folder preview, Folder Icon Appearance navigation row,
 * Icon size + Match desktop size, expandable Label accordion, expandable 3-tab Colors accordion,
 * Mode-conditional Window/Immersive controls (Shadow, Card background, Align Bottom, Grid AUTO vs Picker),
 * Corner radius, Show folder name, Scroll direction, and Gestures & Behavior.
 */
class FolderSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFolderSettingsBinding
    private lateinit var prefs: SharedPreferences
    private var sampleDrawables: List<Drawable> = emptyList()

    private var activeColorTab = TAB_SURFACE
    private var isColorsExpanded = false
    private var isLabelExpanded = false

    // Folder Window state
    private var windowStyle: String = WINDOW_STYLE_WINDOW
    private var windowBgColor: Int = 0xFF202534.toInt()
    private var windowBgAlpha: Int = 80 // opacity % 0..100
    private var windowTextColor: Int = 0xFFFFFFFF.toInt()
    private var windowAccentColor: Int = 0xFFD54526.toInt()
    private var windowCornerRadius: Int = 16
    private var windowShadow: Boolean = true
    private var cardBackground: Boolean = false
    private var alignBottom: Boolean = false
    private var showFolderName: Boolean = true
    private var windowGridRows: Int = 3
    private var windowGridCols: Int = 4
    private var windowScrollDir: String = SCROLL_DIR_VERTICAL

    // Layout & Labels state
    private var matchDesktopSize: Boolean = true
    private var folderIconSizePercent: Int = 100
    private var folderLabelVisible: Boolean = true
    private var folderLabelSizePercent: Int = 100
    private var folderLabelFont: String = "Normal"
    private var folderLabelColor: Int = 0xFFFFFFFF.toInt()
    private var folderLabelShadow: Boolean = true
    private var folderLabelSingleLine: Boolean = true

    // Behavior state
    private var folderTapAction: String = TAP_ACTION_OPEN_FOLDER
    private var folderAutoClose: Boolean = true

    companion object {
        const val WINDOW_STYLE_WINDOW = "WINDOW"
        const val WINDOW_STYLE_IMMERSIVE = "IMMERSIVE"
        const val SCROLL_DIR_VERTICAL = "VERTICAL"
        const val SCROLL_DIR_HORIZONTAL = "HORIZONTAL"

        const val KEY_FOLDER_WINDOW_STYLE = "key_folder_window_style"
        const val KEY_FOLDER_WINDOW_BG_COLOR = "key_folder_window_bg_color"
        const val KEY_FOLDER_WINDOW_BG_ALPHA = "key_folder_window_bg_alpha"
        const val KEY_FOLDER_WINDOW_TEXT_COLOR = "key_folder_window_text_color"
        const val KEY_FOLDER_WINDOW_ACCENT_COLOR = "key_folder_window_accent_color"
        const val KEY_FOLDER_WINDOW_CORNER_RADIUS = "key_folder_window_corner_radius"
        const val KEY_FOLDER_WINDOW_SHADOW = "key_folder_window_shadow"
        const val KEY_FOLDER_IMMERSIVE_CARD_BG = "key_folder_immersive_card_bg"
        const val KEY_FOLDER_IMMERSIVE_ALIGN_BOTTOM = "key_folder_immersive_align_bottom"
        const val KEY_FOLDER_SHOW_NAME = "key_folder_show_name"
        const val KEY_FOLDER_GRID_ROWS = "key_folder_grid_rows"
        const val KEY_FOLDER_GRID_COLS = "key_folder_grid_cols"
        const val KEY_FOLDER_SCROLL_DIRECTION = "key_folder_scroll_direction"

        const val KEY_FOLDER_MATCH_DESKTOP_SIZE = "key_folder_match_desktop_size"
        const val KEY_FOLDER_ICON_SIZE_PERCENT = "key_folder_icon_size_percent"
        const val KEY_FOLDER_LABEL_VISIBLE = "key_folder_label_visible"
        const val KEY_FOLDER_LABEL_SIZE_PERCENT = "key_folder_label_size_percent"
        const val KEY_FOLDER_LABEL_FONT = "key_folder_label_font"
        const val KEY_FOLDER_LABEL_COLOR = "key_folder_label_color"
        const val KEY_FOLDER_LABEL_SHADOW = "key_folder_label_shadow"
        const val KEY_FOLDER_LABEL_SINGLE_LINE = "key_folder_label_single_line"

        const val KEY_FOLDER_TAP_ACTION = "key_folder_tap_action"
        const val KEY_FOLDER_AUTO_CLOSE = "key_folder_auto_close"
        const val TAP_ACTION_OPEN_FOLDER = "OPEN_FOLDER"
        const val TAP_ACTION_FIRST_APP = "FIRST_APP"

        private const val TAB_SURFACE = 0
        private const val TAB_TEXT = 1
        private const val TAB_ACCENT = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFolderSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemBarHelper.applySystemBars(this, isLightTheme = true)
        prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)

        loadState()
        setupWallpaperBackdrop()
        loadSampleIcons()
        setupListeners()
        refreshAllUi()
    }

    override fun onResume() {
        super.onResume()
        refreshFolderIconAppearanceRow()
    }

    private fun loadState() {
        windowStyle = prefs.getString(KEY_FOLDER_WINDOW_STYLE, WINDOW_STYLE_WINDOW) ?: WINDOW_STYLE_WINDOW
        windowBgColor = prefs.getInt(KEY_FOLDER_WINDOW_BG_COLOR, 0xFF202534.toInt())
        windowBgAlpha = prefs.getInt(KEY_FOLDER_WINDOW_BG_ALPHA, 80).coerceIn(0, 100)
        windowTextColor = prefs.getInt(KEY_FOLDER_WINDOW_TEXT_COLOR, 0xFFFFFFFF.toInt())
        windowAccentColor = prefs.getInt(KEY_FOLDER_WINDOW_ACCENT_COLOR, 0xFFD54526.toInt())
        windowCornerRadius = prefs.getInt(KEY_FOLDER_WINDOW_CORNER_RADIUS, 16).coerceIn(0, 32)
        windowShadow = prefs.getBoolean(KEY_FOLDER_WINDOW_SHADOW, true)
        cardBackground = prefs.getBoolean(KEY_FOLDER_IMMERSIVE_CARD_BG, false)
        alignBottom = prefs.getBoolean(KEY_FOLDER_IMMERSIVE_ALIGN_BOTTOM, false)
        showFolderName = prefs.getBoolean(KEY_FOLDER_SHOW_NAME, true)
        windowGridRows = prefs.getInt(KEY_FOLDER_GRID_ROWS, 3).coerceIn(2, 7)
        windowGridCols = prefs.getInt(KEY_FOLDER_GRID_COLS, 4).coerceIn(3, 6)
        windowScrollDir = prefs.getString(KEY_FOLDER_SCROLL_DIRECTION, SCROLL_DIR_VERTICAL) ?: SCROLL_DIR_VERTICAL

        matchDesktopSize = prefs.getBoolean(KEY_FOLDER_MATCH_DESKTOP_SIZE, true)
        folderIconSizePercent = prefs.getInt(KEY_FOLDER_ICON_SIZE_PERCENT, 100).coerceIn(70, 150)
        folderLabelVisible = prefs.getBoolean(KEY_FOLDER_LABEL_VISIBLE, true)
        folderLabelSizePercent = prefs.getInt(KEY_FOLDER_LABEL_SIZE_PERCENT, 100).coerceIn(80, 130)
        folderLabelFont = prefs.getString(KEY_FOLDER_LABEL_FONT, "Normal") ?: "Normal"
        folderLabelColor = prefs.getInt(KEY_FOLDER_LABEL_COLOR, windowTextColor)
        folderLabelShadow = prefs.getBoolean(KEY_FOLDER_LABEL_SHADOW, true)
        folderLabelSingleLine = prefs.getBoolean(KEY_FOLDER_LABEL_SINGLE_LINE, true)

        folderTapAction = prefs.getString(KEY_FOLDER_TAP_ACTION, TAP_ACTION_OPEN_FOLDER) ?: TAP_ACTION_OPEN_FOLDER
        folderAutoClose = prefs.getBoolean(KEY_FOLDER_AUTO_CLOSE, true)
    }

    private fun setupWallpaperBackdrop() {
        try {
            val wp = WallpaperManager.getInstance(this).drawable
            if (wp != null) {
                binding.previewBackdrop.background = wp
            }
        } catch (_: Exception) {
        }
    }

    private fun loadSampleIcons() {
        lifecycleScope.launch(Dispatchers.IO) {
            val apps = AppManager.getInstalledApps(this@FolderSettingsActivity)
            val icons = apps.take(9).mapNotNull { it.icon }
            val fallback = ContextCompat.getDrawable(this@FolderSettingsActivity, R.mipmap.ic_launcher)!!
            val resolved = if (icons.isNotEmpty()) icons else List(9) { fallback }
            withContext(Dispatchers.Main) {
                sampleDrawables = resolved
                refreshFolderIconAppearanceRow()
                refreshOpenedFolderPreview()
            }
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        // Folder icon appearance sub-screen entry
        binding.rowFolderIconAppearance.setOnClickListener {
            startActivity(Intent(this, FolderIconAppearanceActivity::class.java))
        }

        // Layout: Icon size + Match desktop size
        binding.sbFolderIconSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val pct = 70 + progress
                if (fromUser && !matchDesktopSize) {
                    folderIconSizePercent = pct
                    prefs.edit().putInt(KEY_FOLDER_ICON_SIZE_PERCENT, pct).apply()
                    refreshIconSizeUi()
                    refreshOpenedFolderPreview()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        binding.btnIconSizeMinus.setOnClickListener {
            if (!matchDesktopSize && folderIconSizePercent > 70) {
                folderIconSizePercent = (folderIconSizePercent - 5).coerceAtLeast(70)
                prefs.edit().putInt(KEY_FOLDER_ICON_SIZE_PERCENT, folderIconSizePercent).apply()
                refreshIconSizeUi()
                refreshOpenedFolderPreview()
            }
        }
        binding.btnIconSizePlus.setOnClickListener {
            if (!matchDesktopSize && folderIconSizePercent < 150) {
                folderIconSizePercent = (folderIconSizePercent + 5).coerceAtMost(150)
                prefs.edit().putInt(KEY_FOLDER_ICON_SIZE_PERCENT, folderIconSizePercent).apply()
                refreshIconSizeUi()
                refreshOpenedFolderPreview()
            }
        }
        binding.cbMatchDesktopSize.setOnCheckedChangeListener { _, isChecked ->
            matchDesktopSize = isChecked
            prefs.edit().putBoolean(KEY_FOLDER_MATCH_DESKTOP_SIZE, isChecked).apply()
            refreshIconSizeUi()
            refreshOpenedFolderPreview()
        }
        binding.rowMatchDesktopSize.setOnClickListener {
            binding.cbMatchDesktopSize.isChecked = !binding.cbMatchDesktopSize.isChecked
        }

        // Expandable Label accordion
        binding.rowLabelHeader.setOnClickListener {
            isLabelExpanded = !isLabelExpanded
            binding.containerLabelExpanded.visibility = if (isLabelExpanded) View.VISIBLE else View.GONE
            binding.ivLabelChevron.setImageResource(
                if (isLabelExpanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down
            )
        }
        binding.swFolderLabel.setOnCheckedChangeListener { _, isChecked ->
            folderLabelVisible = isChecked
            prefs.edit().putBoolean(KEY_FOLDER_LABEL_VISIBLE, isChecked).apply()
            refreshOpenedFolderPreview()
        }
        binding.sbFolderLabelSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val pct = (80 + progress).coerceIn(80, 130)
                binding.tvFolderLabelSizeValue.text = "$pct%"
                if (fromUser) {
                    folderLabelSizePercent = pct
                    prefs.edit().putInt(KEY_FOLDER_LABEL_SIZE_PERCENT, pct).apply()
                    refreshOpenedFolderPreview()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        binding.btnLabelFontDropdown.setOnClickListener {
            val fonts = arrayOf("Normal", "Medium", "Condensed", "Light")
            val cur = fonts.indexOfFirst { it.equals(folderLabelFont, ignoreCase = true) }.coerceAtLeast(0)
            AlertDialog.Builder(this)
                .setTitle("Font")
                .setSingleChoiceItems(fonts, cur) { dialog, idx ->
                    folderLabelFont = fonts[idx]
                    prefs.edit().putString(KEY_FOLDER_LABEL_FONT, folderLabelFont).apply()
                    binding.tvFolderLabelFontValue.text = folderLabelFont
                    refreshOpenedFolderPreview()
                    dialog.dismiss()
                }
                .show()
        }
        binding.cbLabelShadow.setOnCheckedChangeListener { _, isChecked ->
            folderLabelShadow = isChecked
            prefs.edit().putBoolean(KEY_FOLDER_LABEL_SHADOW, isChecked).apply()
            refreshOpenedFolderPreview()
        }
        binding.cbLabelSingleLine.setOnCheckedChangeListener { _, isChecked ->
            folderLabelSingleLine = isChecked
            prefs.edit().putBoolean(KEY_FOLDER_LABEL_SINGLE_LINE, isChecked).apply()
            refreshOpenedFolderPreview()
        }

        // Expandable 3-Tab Colors Accordion
        binding.rowColorsHeader.setOnClickListener {
            isColorsExpanded = !isColorsExpanded
            binding.containerColorsExpanded.visibility = if (isColorsExpanded) View.VISIBLE else View.GONE
            binding.ivColorsChevron.setImageResource(
                if (isColorsExpanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down
            )
            if (isColorsExpanded) rebuildPaletteGrids()
        }
        binding.tabColorSurface.setOnClickListener { switchColorTab(TAB_SURFACE) }
        binding.tabColorText.setOnClickListener { switchColorTab(TAB_TEXT) }
        binding.tabColorAccent.setOnClickListener { switchColorTab(TAB_ACCENT) }
        binding.rowCustomColor.setOnClickListener { showCustomHexDialog() }

        binding.sbWindowTransparency.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val transparency = progress.coerceIn(0, 100)
                binding.tvWindowTransparencyValue.text = "$transparency%"
                if (fromUser) {
                    windowBgAlpha = (100 - transparency).coerceIn(0, 100)
                    prefs.edit().putInt(KEY_FOLDER_WINDOW_BG_ALPHA, windowBgAlpha).apply()
                    refreshFolderIconAppearanceRow()
                    refreshOpenedFolderPreview()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Mode dropdown (Window | Immersive)
        binding.btnModeDropdown.setOnClickListener {
            val labels = arrayOf("Window", "Immersive")
            val cur = if (windowStyle.equals(WINDOW_STYLE_IMMERSIVE, ignoreCase = true)) 1 else 0
            AlertDialog.Builder(this)
                .setTitle("Mode")
                .setSingleChoiceItems(labels, cur) { dialog, idx ->
                    windowStyle = if (idx == 1) WINDOW_STYLE_IMMERSIVE else WINDOW_STYLE_WINDOW
                    prefs.edit().putString(KEY_FOLDER_WINDOW_STYLE, windowStyle).apply()
                    refreshModeDependentControls()
                    refreshOpenedFolderPreview()
                    dialog.dismiss()
                }
                .show()
        }

        // Window Shadow (WINDOW only)
        binding.swWindowShadow.setOnCheckedChangeListener { _, isChecked ->
            windowShadow = isChecked
            prefs.edit().putBoolean(KEY_FOLDER_WINDOW_SHADOW, isChecked).apply()
            refreshOpenedFolderPreview()
        }
        binding.rowWindowShadow.setOnClickListener {
            binding.swWindowShadow.isChecked = !binding.swWindowShadow.isChecked
        }

        // Card background (IMMERSIVE only)
        binding.swCardBackground.setOnCheckedChangeListener { _, isChecked ->
            cardBackground = isChecked
            prefs.edit().putBoolean(KEY_FOLDER_IMMERSIVE_CARD_BG, isChecked).apply()
            refreshModeDependentControls()
            refreshOpenedFolderPreview()
        }
        binding.rowCardBackground.setOnClickListener {
            binding.swCardBackground.isChecked = !binding.swCardBackground.isChecked
        }

        // Align Bottom (IMMERSIVE only)
        binding.swAlignBottom.setOnCheckedChangeListener { _, isChecked ->
            alignBottom = isChecked
            prefs.edit().putBoolean(KEY_FOLDER_IMMERSIVE_ALIGN_BOTTOM, isChecked).apply()
            refreshOpenedFolderPreview()
        }
        binding.rowAlignBottom.setOnClickListener {
            binding.swAlignBottom.isChecked = !binding.swAlignBottom.isChecked
        }

        // Grid row (AUTO in Window; visual picker in Immersive)
        binding.rowFolderGrid.setOnClickListener {
            val resolved = FolderGeometry.resolveFolderWindowAppearance(
                windowStyle, windowShadow, cardBackground, alignBottom, windowCornerRadius, windowBgAlpha
            )
            if (resolved.isGridAutoLocked) return@setOnClickListener
            val dlg = FolderGridDialogFragment.newInstance()
            dlg.setOnGridSavedListener { cols, rows ->
                windowGridCols = cols
                windowGridRows = rows
                refreshModeDependentControls()
                refreshOpenedFolderPreview()
            }
            dlg.show(supportFragmentManager, FolderGridDialogFragment.TAG)
        }

        // Corner radius slider + clickable number
        binding.sbCornerRadius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                windowCornerRadius = progress.coerceIn(0, 32)
                binding.tvCornerRadiusValue.text = "${windowCornerRadius}dp"
                if (fromUser) {
                    prefs.edit().putInt(KEY_FOLDER_WINDOW_CORNER_RADIUS, windowCornerRadius).apply()
                    refreshOpenedFolderPreview()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        binding.tvCornerRadiusValue.setOnClickListener {
            if (!binding.sbCornerRadius.isEnabled) return@setOnClickListener
            val input = EditText(this).apply {
                inputType = InputType.TYPE_CLASS_NUMBER
                setText(windowCornerRadius.toString())
                setPadding(48, 32, 48, 24)
            }
            AlertDialog.Builder(this)
                .setTitle("Corner radius (0–32dp)")
                .setView(input)
                .setPositiveButton("OK") { _, _ ->
                    val parsed = input.text.toString().toIntOrNull()?.coerceIn(0, 32) ?: return@setPositiveButton
                    windowCornerRadius = parsed
                    prefs.edit().putInt(KEY_FOLDER_WINDOW_CORNER_RADIUS, parsed).apply()
                    binding.sbCornerRadius.progress = parsed
                    binding.tvCornerRadiusValue.text = "${parsed}dp"
                    refreshOpenedFolderPreview()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Show folder name switch
        binding.swShowFolderName.setOnCheckedChangeListener { _, isChecked ->
            showFolderName = isChecked
            prefs.edit().putBoolean(KEY_FOLDER_SHOW_NAME, isChecked).apply()
            refreshOpenedFolderPreview()
        }
        binding.rowShowFolderName.setOnClickListener {
            binding.swShowFolderName.isChecked = !binding.swShowFolderName.isChecked
        }

        // Scroll direction dropdown
        binding.btnScrollDirectionDropdown.setOnClickListener {
            val labels = arrayOf("Vertical", "Horizontal")
            val cur = if (windowScrollDir == SCROLL_DIR_HORIZONTAL) 1 else 0
            AlertDialog.Builder(this)
                .setTitle("Scroll direction")
                .setSingleChoiceItems(labels, cur) { dialog, idx ->
                    windowScrollDir = if (idx == 1) SCROLL_DIR_HORIZONTAL else SCROLL_DIR_VERTICAL
                    prefs.edit().putString(KEY_FOLDER_SCROLL_DIRECTION, windowScrollDir).apply()
                    binding.tvScrollDirectionValue.text = labels[idx]
                    refreshOpenedFolderPreview()
                    dialog.dismiss()
                }
                .show()
        }

        // Gestures & Behavior
        binding.rowTapAction.setOnClickListener {
            val items = arrayOf("Open folder (swipe opens 1st app)", "Launch first app (swipe opens folder)")
            val cur = if (folderTapAction == TAP_ACTION_FIRST_APP) 1 else 0
            AlertDialog.Builder(this)
                .setTitle("Tap action")
                .setSingleChoiceItems(items, cur) { dialog, idx ->
                    folderTapAction = if (idx == 1) TAP_ACTION_FIRST_APP else TAP_ACTION_OPEN_FOLDER
                    prefs.edit().putString(KEY_FOLDER_TAP_ACTION, folderTapAction).apply()
                    refreshTapActionSummary()
                    dialog.dismiss()
                }
                .show()
        }
        binding.swAutoClose.setOnCheckedChangeListener { _, isChecked ->
            folderAutoClose = isChecked
            prefs.edit().putBoolean(KEY_FOLDER_AUTO_CLOSE, isChecked).apply()
        }
        binding.rowAutoClose.setOnClickListener {
            binding.swAutoClose.isChecked = !binding.swAutoClose.isChecked
        }
    }

    private fun refreshAllUi() {
        binding.cbMatchDesktopSize.isChecked = matchDesktopSize
        binding.swFolderLabel.isChecked = folderLabelVisible
        binding.sbFolderLabelSize.progress = (folderLabelSizePercent - 80).coerceIn(0, 50)
        binding.tvFolderLabelSizeValue.text = "${folderLabelSizePercent}%"
        binding.tvFolderLabelFontValue.text = folderLabelFont
        binding.cbLabelShadow.isChecked = folderLabelShadow
        binding.cbLabelSingleLine.isChecked = folderLabelSingleLine

        binding.sbWindowTransparency.progress = (100 - windowBgAlpha).coerceIn(0, 100)
        binding.tvWindowTransparencyValue.text = "${100 - windowBgAlpha}%"

        binding.swWindowShadow.isChecked = windowShadow
        binding.swCardBackground.isChecked = cardBackground
        binding.swAlignBottom.isChecked = alignBottom
        binding.sbCornerRadius.progress = windowCornerRadius
        binding.tvCornerRadiusValue.text = "${windowCornerRadius}dp"
        binding.swShowFolderName.isChecked = showFolderName
        binding.tvScrollDirectionValue.text =
            if (windowScrollDir == SCROLL_DIR_HORIZONTAL) "Horizontal" else "Vertical"
        binding.swAutoClose.isChecked = folderAutoClose

        switchColorTab(activeColorTab)
        refreshColorBadge()
        refreshIconSizeUi()
        refreshModeDependentControls()
        refreshTapActionSummary()
        refreshFolderIconAppearanceRow()
        refreshOpenedFolderPreview()
    }

    private fun refreshIconSizeUi() {
        val desktopPct = prefs.getInt("key_desktop_icon_size_percent", 100).coerceIn(70, 150)
        val effective = if (matchDesktopSize) desktopPct else folderIconSizePercent
        binding.sbFolderIconSize.progress = (effective - 70).coerceIn(0, 80)
        binding.sbFolderIconSize.isEnabled = !matchDesktopSize
        binding.btnIconSizeMinus.isEnabled = !matchDesktopSize
        binding.btnIconSizePlus.isEnabled = !matchDesktopSize
        binding.sbFolderIconSize.alpha = if (matchDesktopSize) 0.45f else 1.0f
        binding.tvFolderIconSizeValue.text = if (matchDesktopSize) "$effective% (Desktop)" else "$effective%"
    }

    private fun refreshModeDependentControls() {
        val resolved = FolderGeometry.resolveFolderWindowAppearance(
            style = windowStyle,
            shadowEnabled = windowShadow,
            cardBackgroundEnabled = cardBackground,
            alignBottomEnabled = alignBottom,
            cornerRadiusDp = windowCornerRadius,
            bgAlphaPercent = windowBgAlpha
        )
        binding.tvModeValue.text = if (resolved.isImmersive) "Immersive" else "Window"
        binding.rowWindowShadow.visibility = if (!resolved.isImmersive) View.VISIBLE else View.GONE
        binding.rowCardBackground.visibility = if (resolved.isImmersive) View.VISIBLE else View.GONE
        binding.rowAlignBottom.visibility = if (resolved.isImmersive) View.VISIBLE else View.GONE

        // Grid locked to AUTO in Window mode, interactive in Immersive mode
        binding.rowFolderGrid.isEnabled = !resolved.isGridAutoLocked
        binding.rowFolderGrid.alpha = if (resolved.isGridAutoLocked) 0.55f else 1.0f
        if (resolved.isGridAutoLocked) {
            binding.tvGridAutoBadge.visibility = View.VISIBLE
            binding.miniGridPreview.visibility = View.GONE
            binding.tvFolderGridSubtitle.text = "Automatic"
        } else {
            binding.tvGridAutoBadge.visibility = View.GONE
            binding.miniGridPreview.visibility = View.VISIBLE
            binding.miniGridPreview.isMiniThumbnail = true
            binding.miniGridPreview.setGrid(windowGridRows, windowGridCols)
            binding.tvFolderGridSubtitle.text = "$windowGridRows × $windowGridCols"
        }

        // Corner radius active when Window OR Immersive Card background is ON
        binding.sbCornerRadius.isEnabled = resolved.isCornerRadiusEnabled
        binding.rowCornerRadius.alpha = if (resolved.isCornerRadiusEnabled) 1.0f else 0.45f
    }

    private fun refreshTapActionSummary() {
        binding.tvTapActionSummary.text = if (folderTapAction == TAP_ACTION_FIRST_APP) {
            "Launch first app (swipe opens folder)"
        } else {
            "Open folder (swipe opens 1st app)"
        }
    }

    private fun refreshFolderIconAppearanceRow() {
        val preview = prefs.getString("key_folder_icon_preview", FolderIconRenderer.PREVIEW_GRID)
            ?: FolderIconRenderer.PREVIEW_GRID
        val previewName = when (preview.uppercase()) {
            FolderIconRenderer.PREVIEW_GRID_9 -> "Grid (3×3)"
            FolderIconRenderer.PREVIEW_RADIAL_5 -> "Radial"
            FolderIconRenderer.PREVIEW_RADIAL_9 -> "Radial (Clustered)"
            FolderIconRenderer.PREVIEW_STACK -> "Stack"
            FolderIconRenderer.PREVIEW_FAN -> "Fan"
            FolderIconRenderer.PREVIEW_LINE -> "Line"
            FolderIconRenderer.PREVIEW_FIRST_APP -> "First app"
            else -> "Grid"
        }
        val rawShape = prefs.getString("key_folder_icon_shape", FolderIconRenderer.SHAPE_SYSTEM)
            ?: FolderIconRenderer.SHAPE_SYSTEM
        val shapeLabel = if (rawShape == FolderIconRenderer.SHAPE_SYSTEM) "Adaptive" else rawShape
        val matched = prefs.getBoolean("key_folder_icon_match_window_color", true)
        val colorTag = if (matched) "Match window" else "Custom color"
        binding.tvFolderIconAppearanceSummary.text = "$previewName • $shapeLabel • $colorTag"

        if (sampleDrawables.isNotEmpty()) {
            val sizePx = (44 * resources.displayMetrics.density).toInt()
            binding.ivFolderIconThumb.setImageDrawable(
                FolderIconRenderer.renderFolderIconFromPrefs(this, sampleDrawables, sizePx)
            )
        }
    }

    private fun switchColorTab(tab: Int) {
        activeColorTab = tab
        val activeColor = Color.parseColor("#6D3B68")
        val inactiveColor = ContextCompat.getColor(this, R.color.nova_text_brown_secondary)
        val tabs = arrayOf(binding.tabColorSurface, binding.tabColorText, binding.tabColorAccent)
        tabs.forEachIndexed { idx, tv ->
            val selected = idx == tab
            tv.setTextColor(if (selected) activeColor else inactiveColor)
            tv.setBackgroundColor(if (selected) Color.parseColor("#D9CEEC") else Color.TRANSPARENT)
        }
        if (isColorsExpanded) rebuildPaletteGrids()
    }

    private fun currentTabColor(): Int = when (activeColorTab) {
        TAB_TEXT -> windowTextColor
        TAB_ACCENT -> windowAccentColor
        else -> windowBgColor
    }

    private fun applyColorForActiveTab(color: Int) {
        when (activeColorTab) {
            TAB_TEXT -> {
                windowTextColor = color
                folderLabelColor = color
                prefs.edit()
                    .putInt(KEY_FOLDER_WINDOW_TEXT_COLOR, color)
                    .putInt(KEY_FOLDER_LABEL_COLOR, color)
                    .apply()
            }
            TAB_ACCENT -> {
                windowAccentColor = color
                prefs.edit().putInt(KEY_FOLDER_WINDOW_ACCENT_COLOR, color).apply()
            }
            else -> {
                windowBgColor = color
                prefs.edit().putInt(KEY_FOLDER_WINDOW_BG_COLOR, color).apply()
                refreshFolderIconAppearanceRow()
            }
        }
        rebuildPaletteGrids()
        refreshColorBadge()
        refreshOpenedFolderPreview()
    }

    private fun refreshColorBadge() {
        (binding.badgeColorPreview.background as? GradientDrawable)?.setColor(windowBgColor or 0xFF000000.toInt())
        binding.tvBadgeTt.setTextColor(windowTextColor)
        (binding.viewBadgeAccentDot.background as? GradientDrawable)?.setColor(windowAccentColor or 0xFF000000.toInt())
    }

    private fun rebuildPaletteGrids() {
        populateSwatchGrid(binding.gridSystemPalette, SearchBarConfig.SYSTEM_PALETTE_COLORS)
        populateSwatchGrid(binding.gridFixedPalette, SearchBarConfig.FIXED_PALETTE_COLORS)
        binding.tvCustomColorHex.text = String.format("#%06X", 0xFFFFFF and currentTabColor())
    }

    private fun populateSwatchGrid(grid: GridLayout, colors: IntArray) {
        grid.removeAllViews()
        val selected = currentTabColor()
        val density = resources.displayMetrics.density
        val sizePx = (38 * density).toInt()
        val marginPx = (5 * density).toInt()

        for (color in colors) {
            val circle = View(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = sizePx
                    height = sizePx
                    setMargins(marginPx, marginPx, marginPx, marginPx)
                }
                val isSelected = (color and 0xFFFFFF) == (selected and 0xFFFFFF)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                    if (isSelected) {
                        setStroke((3 * density).toInt(), Color.parseColor("#6D3B68"))
                    } else {
                        setStroke((1 * density).toInt(), Color.parseColor("#33000000"))
                    }
                }
                setOnClickListener { applyColorForActiveTab(color) }
            }
            grid.addView(circle)
        }
    }

    private fun showCustomHexDialog() {
        val tabTitle = when (activeColorTab) {
            TAB_TEXT -> "Text color"
            TAB_ACCENT -> "Accent color"
            else -> "Folder background color"
        }
        val defaultCol = when (activeColorTab) {
            TAB_TEXT -> 0xFFFFFFFF.toInt()
            TAB_ACCENT -> 0xFFD54526.toInt()
            else -> 0xFF202534.toInt()
        }
        NovaColorPickerBottomSheet.newInstance(
            title = tabTitle,
            initialColor = currentTabColor(),
            defaultColor = defaultCol
        ) { chosen ->
            applyColorForActiveTab(chosen)
        }.show(supportFragmentManager, NovaColorPickerBottomSheet.TAG)
    }

    private fun refreshOpenedFolderPreview() {
        val density = resources.displayMetrics.density
        val resolved = FolderGeometry.resolveFolderWindowAppearance(
            style = windowStyle,
            shadowEnabled = windowShadow,
            cardBackgroundEnabled = cardBackground,
            alignBottomEnabled = alignBottom,
            cornerRadiusDp = windowCornerRadius,
            bgAlphaPercent = windowBgAlpha
        )

        val lp = binding.previewFolderCard.layoutParams as FrameLayout.LayoutParams
        if (resolved.isImmersive) {
            lp.width = FrameLayout.LayoutParams.MATCH_PARENT
            lp.gravity = if (resolved.alignBottom) Gravity.BOTTOM else Gravity.CENTER
        } else {
            lp.width = (256 * density).toInt()
            lp.gravity = Gravity.CENTER
        }
        binding.previewFolderCard.layoutParams = lp

        binding.previewFolderCard.elevation = resolved.effectiveElevationDp * density
        val alphaInt = ((resolved.effectiveBackgroundAlpha / 100f) * 255).toInt().coerceIn(0, 255)
        val fillColor = if (resolved.showCardBackground) {
            ColorUtils.setAlphaComponent(windowBgColor, alphaInt)
        } else {
            Color.TRANSPARENT
        }
        binding.previewFolderCard.background = GradientDrawable().apply {
            setColor(fillColor)
            cornerRadius = resolved.effectiveCornerRadiusDp * density
        }

        // Folder header name + accent dots
        binding.previewFolderHeader.visibility = if (showFolderName) View.VISIBLE else View.GONE
        binding.tvPreviewFolderTitle.setTextColor(windowTextColor)
        binding.tvPreviewOverflowDots.setTextColor(windowTextColor)
        (binding.dotPreviewActive.background as? GradientDrawable)?.setColor(windowAccentColor or 0xFF000000.toInt())
        (binding.dotPreviewInactive.background as? GradientDrawable)?.setColor(windowTextColor or 0xFF000000.toInt())

        // Sample icons & labels
        val effectiveIconPct = if (matchDesktopSize) {
            prefs.getInt("key_desktop_icon_size_percent", 100)
        } else {
            folderIconSizePercent
        }
        val iconPx = (36f * density * (effectiveIconPct / 100f)).toInt().coerceIn((24 * density).toInt(), (48 * density).toInt())
        val iconShape = prefs.getString("key_icon_shape", "Round") ?: "Round"
        val iconViews = arrayOf(binding.ivSampleApp1, binding.ivSampleApp2, binding.ivSampleApp3, binding.ivSampleApp4)
        val labelViews = arrayOf(binding.tvSampleApp1, binding.tvSampleApp2, binding.tvSampleApp3, binding.tvSampleApp4)

        val tf = when (folderLabelFont) {
            "Medium" -> Typeface.create("sans-serif-medium", Typeface.NORMAL)
            "Condensed" -> Typeface.create("sans-serif-condensed", Typeface.NORMAL)
            "Light" -> Typeface.create("sans-serif-light", Typeface.NORMAL)
            else -> Typeface.DEFAULT
        }

        for (i in 0 until 4) {
            val iv = iconViews[i]
            iv.layoutParams = iv.layoutParams.apply {
                width = iconPx
                height = iconPx
            }
            if (i < sampleDrawables.size) {
                val masked = IconShapeMasker.createMaskedIcon(this, sampleDrawables[i], iconShape, effectiveIconPct, iconPx)
                iv.setImageDrawable(masked ?: sampleDrawables[i])
            }

            val tv = labelViews[i]
            tv.visibility = if (folderLabelVisible) View.VISIBLE else View.GONE
            tv.textSize = 10.5f * (folderLabelSizePercent / 100f)
            tv.typeface = tf
            tv.setTextColor(folderLabelColor)
            if (folderLabelShadow) {
                tv.setShadowLayer(3f, 0f, 1f, 0x88000000.toInt())
            } else {
                tv.setShadowLayer(0f, 0f, 0f, 0)
            }
            tv.maxLines = if (folderLabelSingleLine) 1 else 2
            tv.isSingleLine = folderLabelSingleLine
        }
    }
}
