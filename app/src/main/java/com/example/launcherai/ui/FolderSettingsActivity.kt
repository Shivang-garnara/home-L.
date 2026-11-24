package com.example.launcherai.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.launcherai.R
import com.example.launcherai.databinding.ActivityFolderSettingsBinding
import com.example.launcherai.icons.FolderIconRenderer
import com.example.launcherai.util.AppManager
import kotlinx.coroutines.launch

/**
 * Dedicated Folders Settings activity matching Nova Launcher 8.9.9 (SettingsFolder / SettingsFolderIcon).
 * Provides live folder icon preview, preview layout selector, shape picker, background color & alpha slider,
 * outline stroke toggle, clip to bounds toggle, complete Window Style configuration (Presentation style,
 * Window Background color & alpha, Corner radius, Drop shadow, Grid dimensions, Scroll direction), and
 * independent Folder Content Icon Layout controls (match desktop size, icon size, label visibility, font,
 * text size, text color, shadow, single line).
 */
class FolderSettingsActivity : AppCompatActivity() {

    companion object {
        const val WINDOW_STYLE_WINDOW = "WINDOW"
        const val WINDOW_STYLE_IMMERSIVE = "IMMERSIVE"
        const val SCROLL_DIR_VERTICAL = "VERTICAL"
        const val SCROLL_DIR_HORIZONTAL = "HORIZONTAL"

        const val KEY_FOLDER_WINDOW_STYLE = "key_folder_window_style"
        const val KEY_FOLDER_WINDOW_BG_COLOR = "key_folder_window_bg_color"
        const val KEY_FOLDER_WINDOW_BG_ALPHA = "key_folder_window_bg_alpha"
        const val KEY_FOLDER_WINDOW_CORNER_RADIUS = "key_folder_window_corner_radius"
        const val KEY_FOLDER_WINDOW_SHADOW = "key_folder_window_shadow"
        const val KEY_FOLDER_GRID_ROWS = "key_folder_grid_rows"
        const val KEY_FOLDER_GRID_COLS = "key_folder_grid_cols"
        const val KEY_FOLDER_SCROLL_DIRECTION = "key_folder_scroll_direction"

        // Folder Content Icon Layout Keys (matching Nova's folder_cellspecs)
        const val KEY_FOLDER_MATCH_DESKTOP_SIZE = "key_folder_match_desktop_size"
        const val KEY_FOLDER_ICON_SIZE_PERCENT = "key_folder_icon_size_percent"
        const val KEY_FOLDER_LABEL_VISIBLE = "key_folder_label_visible"
        const val KEY_FOLDER_LABEL_SIZE_PERCENT = "key_folder_label_size_percent"
        const val KEY_FOLDER_LABEL_FONT = "key_folder_label_font"
        const val KEY_FOLDER_LABEL_COLOR = "key_folder_label_color"
        const val KEY_FOLDER_LABEL_SHADOW = "key_folder_label_shadow"
        const val KEY_FOLDER_LABEL_SINGLE_LINE = "key_folder_label_single_line"

        // Gestures & Behavior Keys (matching Nova Launcher ia.f / kx.g0)
        const val TAP_ACTION_OPEN_FOLDER = "OPEN_FOLDER"
        const val TAP_ACTION_FIRST_APP = "FIRST_APP"
        const val KEY_FOLDER_TAP_ACTION = "key_folder_tap_action"
        const val KEY_FOLDER_AUTO_CLOSE = "key_folder_auto_close"
    }

    private lateinit var binding: ActivityFolderSettingsBinding

    // Folder Icon State
    private var previewLayout: String = FolderIconRenderer.PREVIEW_GRID
    private var shape: String = FolderIconRenderer.SHAPE_SYSTEM
    private var bgColor: Int = 0xFFE0E0E0.toInt()
    private var bgAlpha: Int = 20
    private var outline: Boolean = true
    private var clipToBounds: Boolean = true

    // Folder Window Style State
    private var windowStyle: String = WINDOW_STYLE_WINDOW
    private var windowBgColor: Int = 0xFF202534.toInt()
    private var windowBgAlpha: Int = 80
    private var windowCornerRadius: Int = 16
    private var windowShadow: Boolean = true
    private var windowGridRows: Int = 3
    private var windowGridCols: Int = 4
    private var windowScrollDir: String = SCROLL_DIR_VERTICAL

    // Folder Content Icon Layout State (folder_cellspecs)
    private var matchDesktopSize: Boolean = true
    private var folderIconSizePercent: Int = 100
    private var folderLabelVisible: Boolean = true
    private var folderLabelSizePercent: Int = 100
    private var folderLabelFont: String = "Normal"
    private var folderLabelColor: Int = 0xFFFFFFFF.toInt()
    private var folderLabelShadow: Boolean = true
    private var folderLabelSingleLine: Boolean = true

    // Gestures & Behavior State
    private var folderTapAction: String = TAP_ACTION_OPEN_FOLDER
    private var folderAutoClose: Boolean = true

    // Sample drawables for live preview
    private var sampleDrawables: List<Drawable> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_NovaLight)
        super.onCreate(savedInstanceState)
        SystemBarHelper.applySystemBars(this, isLightTheme = true, transparentBars = false)
        binding = ActivityFolderSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemBarHelper.applyEdgeToEdgeInsets(
            insetsRootView = binding.root,
            topInsetView = binding.headerContainer,
            bottomInsetView = null
        )

        loadPreferences()
        loadSampleDrawables()
        setupUI()
        setupListeners()
        updatePreview()
    }

    private fun loadPreferences() {
        val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        // Icon prefs
        previewLayout = prefs.getString("key_folder_icon_preview", FolderIconRenderer.PREVIEW_GRID) ?: FolderIconRenderer.PREVIEW_GRID
        shape = prefs.getString("key_folder_icon_shape", FolderIconRenderer.SHAPE_SYSTEM) ?: FolderIconRenderer.SHAPE_SYSTEM
        bgColor = prefs.getInt("key_folder_icon_bg_color", 0xFFE0E0E0.toInt())
        bgAlpha = prefs.getInt("key_folder_icon_bg_alpha", 20)
        outline = prefs.getBoolean("key_folder_icon_outline", true)
        clipToBounds = prefs.getBoolean("key_folder_icon_clip", true)

        // Window style prefs
        windowStyle = prefs.getString(KEY_FOLDER_WINDOW_STYLE, WINDOW_STYLE_WINDOW) ?: WINDOW_STYLE_WINDOW
        windowBgColor = prefs.getInt(KEY_FOLDER_WINDOW_BG_COLOR, 0xFF202534.toInt())
        windowBgAlpha = prefs.getInt(KEY_FOLDER_WINDOW_BG_ALPHA, 80)
        windowCornerRadius = prefs.getInt(KEY_FOLDER_WINDOW_CORNER_RADIUS, 16)
        windowShadow = prefs.getBoolean(KEY_FOLDER_WINDOW_SHADOW, true)
        windowGridRows = prefs.getInt(KEY_FOLDER_GRID_ROWS, 3)
        windowGridCols = prefs.getInt(KEY_FOLDER_GRID_COLS, 4)
        windowScrollDir = prefs.getString(KEY_FOLDER_SCROLL_DIRECTION, SCROLL_DIR_VERTICAL) ?: SCROLL_DIR_VERTICAL

        // Folder content icon layout prefs
        matchDesktopSize = prefs.getBoolean(KEY_FOLDER_MATCH_DESKTOP_SIZE, true)
        folderIconSizePercent = prefs.getInt(KEY_FOLDER_ICON_SIZE_PERCENT, 100)
        folderLabelVisible = prefs.getBoolean(KEY_FOLDER_LABEL_VISIBLE, true)
        folderLabelSizePercent = prefs.getInt(KEY_FOLDER_LABEL_SIZE_PERCENT, 100)
        folderLabelFont = prefs.getString(KEY_FOLDER_LABEL_FONT, "Normal") ?: "Normal"
        folderLabelColor = prefs.getInt(KEY_FOLDER_LABEL_COLOR, 0xFFFFFFFF.toInt())
        folderLabelShadow = prefs.getBoolean(KEY_FOLDER_LABEL_SHADOW, true)
        folderLabelSingleLine = prefs.getBoolean(KEY_FOLDER_LABEL_SINGLE_LINE, true)

        // Gestures & Behavior prefs
        folderTapAction = prefs.getString(KEY_FOLDER_TAP_ACTION, TAP_ACTION_OPEN_FOLDER) ?: TAP_ACTION_OPEN_FOLDER
        folderAutoClose = prefs.getBoolean(KEY_FOLDER_AUTO_CLOSE, true)
    }

    private fun loadSampleDrawables() {
        sampleDrawables = listOfNotNull(
            ContextCompat.getDrawable(this, R.drawable.ic_launcher_foreground),
            ContextCompat.getDrawable(this, R.drawable.ic_settings_folders),
            ContextCompat.getDrawable(this, R.drawable.ic_settings_home),
            ContextCompat.getDrawable(this, R.drawable.ic_settings_drawer)
        )

        lifecycleScope.launch {
            try {
                val apps = AppManager.getInstalledApps(this@FolderSettingsActivity)
                val drawables = apps.mapNotNull { it.icon }.take(9)
                if (drawables.isNotEmpty()) {
                    sampleDrawables = drawables
                    updatePreview()
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun setupUI() {
        // Folder Icon UI
        binding.tvFolderPreviewLayoutSubtitle.text = getPreviewLayoutDisplayName(previewLayout)
        binding.tvFolderShapeSubtitle.text = getShapeDisplayName(shape)
        updateColorSwatchUI()

        binding.sbFolderBgAlpha.progress = bgAlpha
        binding.tvFolderBgAlphaValue.text = "$bgAlpha%"

        binding.swFolderOutline.isChecked = outline
        binding.swFolderClip.isChecked = clipToBounds

        // Window Style UI
        updateWindowStyleUI()

        // Icon Layout UI
        updateIconLayoutUI()

        // Gestures & Behavior UI
        updateGesturesUI()
    }

    private fun updateWindowStyleUI() {
        binding.tvFolderWindowStyleSubtitle.text = if (windowStyle == WINDOW_STYLE_IMMERSIVE) "Immersive" else "Window"
        updateWindowColorSwatchUI()

        binding.sbFolderWindowBgAlpha.progress = windowBgAlpha
        binding.tvFolderWindowBgAlphaValue.text = "$windowBgAlpha%"

        binding.tvFolderWindowCornerRadiusSubtitle.text = "$windowCornerRadius dp"
        binding.swFolderWindowShadow.isChecked = windowShadow

        binding.layoutWindowSpecificSettings.visibility =
            if (windowStyle == WINDOW_STYLE_WINDOW) View.VISIBLE else View.GONE

        binding.tvFolderWindowGridSubtitle.text = "$windowGridRows rows ✕ $windowGridCols columns"
        binding.tvFolderWindowScrollDirSubtitle.text =
            if (windowScrollDir == SCROLL_DIR_HORIZONTAL) "Horizontal paged" else "Vertical scroll"
    }

    private fun updateIconLayoutUI() {
        binding.swFolderMatchDesktopSize.isChecked = matchDesktopSize
        binding.layoutFolderCustomIconSize.visibility = if (matchDesktopSize) View.GONE else View.VISIBLE
        binding.sbFolderIconSize.progress = (folderIconSizePercent - 70).coerceIn(0, 80)
        binding.tvFolderIconSizeValue.text = "$folderIconSizePercent%"

        binding.swFolderLabelVisible.isChecked = folderLabelVisible
        binding.layoutFolderLabelSettings.visibility = if (folderLabelVisible) View.VISIBLE else View.GONE
        binding.tvFolderLabelFontSubtitle.text = folderLabelFont
        binding.sbFolderLabelSize.progress = (folderLabelSizePercent - 70).coerceIn(0, 70)
        binding.tvFolderLabelSizeValue.text = "$folderLabelSizePercent%"
        updateFolderLabelColorSwatchUI()
        binding.swFolderLabelShadow.isChecked = folderLabelShadow
        binding.swFolderLabelSingleLine.isChecked = folderLabelSingleLine
    }

    private fun updateWindowColorSwatchUI() {
        val hexString = String.format("#%06X", windowBgColor and 0x00FFFFFF)
        val colorName = getColorName(windowBgColor)
        binding.tvFolderWindowBgColorSubtitle.text = "$colorName ($hexString)"

        val density = resources.displayMetrics.density
        val strokePx = (1.5f * density).toInt()
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(windowBgColor or 0xFF000000.toInt())
            setStroke(strokePx, 0x44000000.toInt())
        }
        binding.viewFolderWindowBgColorSwatch.background = drawable
    }

    private fun updateFolderLabelColorSwatchUI() {
        val hexString = String.format("#%06X", folderLabelColor and 0x00FFFFFF)
        val colorName = getColorName(folderLabelColor)
        binding.tvFolderLabelColorSubtitle.text = "$colorName ($hexString)"

        val density = resources.displayMetrics.density
        val strokePx = (1.5f * density).toInt()
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(folderLabelColor or 0xFF000000.toInt())
            setStroke(strokePx, 0x44000000.toInt())
        }
        binding.viewFolderLabelColorSwatch.background = drawable
    }

    private fun updateGesturesUI() {
        binding.tvFolderTapActionSubtitle.text = if (folderTapAction == TAP_ACTION_FIRST_APP) {
            "Launch first app (swipe to open)"
        } else {
            "Open folder"
        }
        binding.swFolderAutoClose.isChecked = folderAutoClose
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        // Icon Listeners
        binding.rowFolderPreviewLayout.setOnClickListener { showPreviewLayoutDialog() }
        binding.rowFolderShape.setOnClickListener { showShapeDialog() }
        binding.rowFolderBgColor.setOnClickListener { showColorPickerDialog() }

        binding.sbFolderBgAlpha.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                bgAlpha = progress
                binding.tvFolderBgAlphaValue.text = "$bgAlpha%"
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                prefs.edit().putInt("key_folder_icon_bg_alpha", bgAlpha).apply()
            }
        })

        binding.swFolderOutline.setOnCheckedChangeListener { _, isChecked ->
            outline = isChecked
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("key_folder_icon_outline", isChecked).apply()
            updatePreview()
        }
        binding.rowFolderOutline.setOnClickListener { binding.swFolderOutline.toggle() }

        binding.swFolderClip.setOnCheckedChangeListener { _, isChecked ->
            clipToBounds = isChecked
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("key_folder_icon_clip", isChecked).apply()
            updatePreview()
        }
        binding.rowFolderClip.setOnClickListener { binding.swFolderClip.toggle() }

        // Window Style Listeners
        binding.rowFolderWindowStyle.setOnClickListener { showWindowStyleDialog() }
        binding.rowFolderWindowBgColor.setOnClickListener { showWindowColorPickerDialog() }

        binding.sbFolderWindowBgAlpha.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                windowBgAlpha = progress
                binding.tvFolderWindowBgAlphaValue.text = "$windowBgAlpha%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                prefs.edit().putInt(KEY_FOLDER_WINDOW_BG_ALPHA, windowBgAlpha).apply()
            }
        })

        binding.rowFolderWindowCornerRadius.setOnClickListener { showCornerRadiusDialog() }

        binding.swFolderWindowShadow.setOnCheckedChangeListener { _, isChecked ->
            windowShadow = isChecked
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_FOLDER_WINDOW_SHADOW, isChecked).apply()
        }
        binding.rowFolderWindowShadow.setOnClickListener { binding.swFolderWindowShadow.toggle() }

        binding.rowFolderWindowGrid.setOnClickListener { showGridDialog() }
        binding.rowFolderWindowScrollDir.setOnClickListener { showScrollDirDialog() }

        // Icon Layout Listeners (folder_cellspecs)
        binding.swFolderMatchDesktopSize.setOnCheckedChangeListener { _, isChecked ->
            matchDesktopSize = isChecked
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_FOLDER_MATCH_DESKTOP_SIZE, isChecked).apply()
            binding.layoutFolderCustomIconSize.visibility = if (isChecked) View.GONE else View.VISIBLE
        }
        binding.rowFolderMatchDesktopSize.setOnClickListener { binding.swFolderMatchDesktopSize.toggle() }

        binding.sbFolderIconSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                folderIconSizePercent = progress + 70
                binding.tvFolderIconSizeValue.text = "$folderIconSizePercent%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                prefs.edit().putInt(KEY_FOLDER_ICON_SIZE_PERCENT, folderIconSizePercent).apply()
            }
        })

        binding.swFolderLabelVisible.setOnCheckedChangeListener { _, isChecked ->
            folderLabelVisible = isChecked
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_FOLDER_LABEL_VISIBLE, isChecked).apply()
            binding.layoutFolderLabelSettings.visibility = if (isChecked) View.VISIBLE else View.GONE
            updatePreview()
        }
        binding.rowFolderLabelVisible.setOnClickListener { binding.swFolderLabelVisible.toggle() }

        binding.rowFolderLabelFont.setOnClickListener { showLabelFontDialog() }

        binding.sbFolderLabelSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                folderLabelSizePercent = progress + 70
                binding.tvFolderLabelSizeValue.text = "$folderLabelSizePercent%"
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                prefs.edit().putInt(KEY_FOLDER_LABEL_SIZE_PERCENT, folderLabelSizePercent).apply()
            }
        })

        binding.rowFolderLabelColor.setOnClickListener { showLabelColorPickerDialog() }

        binding.swFolderLabelShadow.setOnCheckedChangeListener { _, isChecked ->
            folderLabelShadow = isChecked
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_FOLDER_LABEL_SHADOW, isChecked).apply()
            updatePreview()
        }
        binding.rowFolderLabelShadow.setOnClickListener { binding.swFolderLabelShadow.toggle() }

        binding.swFolderLabelSingleLine.setOnCheckedChangeListener { _, isChecked ->
            folderLabelSingleLine = isChecked
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_FOLDER_LABEL_SINGLE_LINE, isChecked).apply()
        }
        binding.rowFolderLabelSingleLine.setOnClickListener { binding.swFolderLabelSingleLine.toggle() }

        // Gestures & Behavior Listeners
        binding.rowFolderTapAction.setOnClickListener { showTapActionDialog() }

        binding.swFolderAutoClose.setOnCheckedChangeListener { _, isChecked ->
            folderAutoClose = isChecked
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_FOLDER_AUTO_CLOSE, isChecked).apply()
        }
        binding.rowFolderAutoClose.setOnClickListener { binding.swFolderAutoClose.toggle() }
    }

    private fun showTapActionDialog() {
        val options = arrayOf(
            "Open folder",
            "Launch first app (swipe to open)"
        )
        val currentIndex = if (folderTapAction == TAP_ACTION_FIRST_APP) 1 else 0

        AlertDialog.Builder(this)
            .setTitle("Tap action")
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                folderTapAction = if (which == 1) TAP_ACTION_FIRST_APP else TAP_ACTION_OPEN_FOLDER
                val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString(KEY_FOLDER_TAP_ACTION, folderTapAction).apply()
                updateGesturesUI()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLabelFontDialog() {
        val fonts = arrayOf("Normal", "Medium", "Light", "Condensed")
        val currentIndex = fonts.indexOf(folderLabelFont).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Folder label font")
            .setSingleChoiceItems(fonts, currentIndex) { dialog, which ->
                folderLabelFont = fonts[which]
                binding.tvFolderLabelFontSubtitle.text = folderLabelFont
                val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString(KEY_FOLDER_LABEL_FONT, folderLabelFont).apply()
                updatePreview()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLabelColorPickerDialog() {
        val dialog = DrawerColorPickerDialogFragment.newInstance(
            title = "Folder label text color",
            initialColor = folderLabelColor
        )
        dialog.setOnColorSavedListener { color ->
            folderLabelColor = color
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putInt(KEY_FOLDER_LABEL_COLOR, color).apply()
            updateFolderLabelColorSwatchUI()
            updatePreview()
        }
        dialog.show(supportFragmentManager, "folder_label_color_dialog")
    }

    private fun showWindowStyleDialog() {
        val options = arrayOf(
            "Window (Centered dialog card)",
            "Immersive (Full-screen blurred sheet)"
        )
        val values = arrayOf(WINDOW_STYLE_WINDOW, WINDOW_STYLE_IMMERSIVE)
        val currentIndex = values.indexOf(windowStyle).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Presentation style")
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                windowStyle = values[which]
                val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString(KEY_FOLDER_WINDOW_STYLE, windowStyle).apply()
                updateWindowStyleUI()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showWindowColorPickerDialog() {
        val dialog = DrawerColorPickerDialogFragment.newInstance(
            title = "Window background color",
            initialColor = windowBgColor
        )
        dialog.setOnColorSavedListener { color ->
            windowBgColor = color
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putInt(KEY_FOLDER_WINDOW_BG_COLOR, color).apply()
            updateWindowColorSwatchUI()
        }
        dialog.show(supportFragmentManager, "folder_window_bg_color_dialog")
    }

    private fun showCornerRadiusDialog() {
        val density = resources.displayMetrics.density
        val paddingPx = (24 * density).toInt()

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(paddingPx, (16 * density).toInt(), paddingPx, (8 * density).toInt())
        }

        val tvValue = TextView(this).apply {
            text = "$windowCornerRadius dp"
            textSize = 18f
            setTextColor(ContextCompat.getColor(this@FolderSettingsActivity, R.color.nova_orange))
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(0, 0, 0, (12 * density).toInt())
        }

        val seekBar = SeekBar(this).apply {
            max = 32
            progress = windowCornerRadius
            progressTintList = ContextCompat.getColorStateList(this@FolderSettingsActivity, R.color.nova_orange)
            thumbTintList = ContextCompat.getColorStateList(this@FolderSettingsActivity, R.color.nova_orange)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    tvValue.text = "$progress dp"
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }

        layout.addView(tvValue)
        layout.addView(seekBar)

        AlertDialog.Builder(this)
            .setTitle("Corner radius (0 – 32 dp)")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                windowCornerRadius = seekBar.progress
                binding.tvFolderWindowCornerRadiusSubtitle.text = "$windowCornerRadius dp"
                val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                prefs.edit().putInt(KEY_FOLDER_WINDOW_CORNER_RADIUS, windowCornerRadius).apply()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showGridDialog() {
        val options = arrayOf(
            "3 rows ✕ 3 columns",
            "3 rows ✕ 4 columns (Default)",
            "4 rows ✕ 4 columns",
            "4 rows ✕ 5 columns",
            "5 rows ✕ 5 columns"
        )
        val gridPairs = arrayOf(
            Pair(3, 3),
            Pair(3, 4),
            Pair(4, 4),
            Pair(4, 5),
            Pair(5, 5)
        )

        var selectedIndex = gridPairs.indexOfFirst { it.first == windowGridRows && it.second == windowGridCols }
        if (selectedIndex < 0) selectedIndex = 1

        AlertDialog.Builder(this)
            .setTitle("Folder grid dimensions")
            .setSingleChoiceItems(options, selectedIndex) { dialog, which ->
                val pair = gridPairs[which]
                windowGridRows = pair.first
                windowGridCols = pair.second
                binding.tvFolderWindowGridSubtitle.text = "$windowGridRows rows ✕ $windowGridCols columns"
                val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                prefs.edit()
                    .putInt(KEY_FOLDER_GRID_ROWS, windowGridRows)
                    .putInt(KEY_FOLDER_GRID_COLS, windowGridCols)
                    .apply()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showScrollDirDialog() {
        val options = arrayOf(
            "Vertical scroll (Default)",
            "Horizontal paged"
        )
        val values = arrayOf(SCROLL_DIR_VERTICAL, SCROLL_DIR_HORIZONTAL)
        val currentIndex = values.indexOf(windowScrollDir).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Scroll direction")
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                windowScrollDir = values[which]
                binding.tvFolderWindowScrollDirSubtitle.text = options[which]
                val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString(KEY_FOLDER_SCROLL_DIRECTION, windowScrollDir).apply()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPreviewLayoutDialog() {
        val options = arrayOf(
            "Grid (2x2)",
            "Grid (3x3)",
            "Stack",
            "Fan",
            "Line",
            "First app (1 icon)"
        )
        val values = arrayOf(
            FolderIconRenderer.PREVIEW_GRID,
            FolderIconRenderer.PREVIEW_GRID_9,
            FolderIconRenderer.PREVIEW_STACK,
            FolderIconRenderer.PREVIEW_FAN,
            FolderIconRenderer.PREVIEW_LINE,
            FolderIconRenderer.PREVIEW_FIRST_APP
        )
        val currentIndex = values.indexOf(previewLayout).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Folder preview")
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                previewLayout = values[which]
                binding.tvFolderPreviewLayoutSubtitle.text = options[which]
                val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString("key_folder_icon_preview", previewLayout).apply()
                updatePreview()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showShapeDialog() {
        val options = arrayOf(
            "System default",
            "Round",
            "Rounded square",
            "Squircle",
            "Square",
            "Teardrop",
            "Platform / None"
        )
        val values = arrayOf(
            FolderIconRenderer.SHAPE_SYSTEM,
            FolderIconRenderer.SHAPE_ROUND,
            FolderIconRenderer.SHAPE_ROUNDED_SQUARE,
            FolderIconRenderer.SHAPE_SQUIRCLE,
            FolderIconRenderer.SHAPE_SQUARE,
            FolderIconRenderer.SHAPE_TEARDROP,
            FolderIconRenderer.SHAPE_PLATFORM
        )
        val currentIndex = values.indexOf(shape).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Folder icon shape")
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                shape = values[which]
                binding.tvFolderShapeSubtitle.text = options[which]
                val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString("key_folder_icon_shape", shape).apply()
                updatePreview()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showColorPickerDialog() {
        val dialog = DrawerColorPickerDialogFragment.newInstance(
            title = "Folder background color",
            initialColor = bgColor
        )
        dialog.setOnColorSavedListener { color ->
            bgColor = color
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putInt("key_folder_icon_bg_color", color).apply()
            updateColorSwatchUI()
            updatePreview()
        }
        dialog.show(supportFragmentManager, "folder_bg_color_dialog")
    }

    private fun updateColorSwatchUI() {
        val hexString = String.format("#%06X", bgColor and 0x00FFFFFF)
        val colorName = getColorName(bgColor)
        binding.tvFolderBgColorSubtitle.text = "$colorName ($hexString)"

        val density = resources.displayMetrics.density
        val strokePx = (1.5f * density).toInt()
        val drawable = GradientDrawable().apply {
            this.shape = GradientDrawable.OVAL
            setColor(bgColor or 0xFF000000.toInt())
            setStroke(strokePx, 0x44000000.toInt())
        }
        binding.viewFolderBgColorSwatch.background = drawable
    }

    private fun updatePreview() {
        val density = resources.displayMetrics.density
        val previewSizePx = (64f * density).toInt()

        val renderedDrawable = FolderIconRenderer.renderFolderIcon(
            context = this,
            children = sampleDrawables,
            previewLayout = previewLayout,
            shape = shape,
            bgColor = bgColor,
            bgAlphaPercent = bgAlpha,
            outline = outline,
            clipToBounds = clipToBounds,
            targetSizePx = previewSizePx
        )

        binding.ivFolderPreview.setImageDrawable(renderedDrawable)

        // Live preview of label styling matching Nova Launcher
        if (folderLabelVisible) {
            binding.tvFolderPreviewLabel.visibility = View.VISIBLE
            binding.tvFolderPreviewLabel.setTextColor(folderLabelColor)
            val baseSizeSp = 12f
            binding.tvFolderPreviewLabel.textSize = baseSizeSp * (folderLabelSizePercent / 100f)
            val tf = when (folderLabelFont.lowercase()) {
                "medium" -> android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                "light" -> android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL)
                "condensed" -> android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.NORMAL)
                else -> android.graphics.Typeface.DEFAULT
            }
            binding.tvFolderPreviewLabel.typeface = tf
            if (folderLabelShadow) {
                binding.tvFolderPreviewLabel.setShadowLayer(4f, 2f, 2f, 0x88000000.toInt())
            } else {
                binding.tvFolderPreviewLabel.setShadowLayer(0f, 0f, 0f, 0)
            }
        } else {
            binding.tvFolderPreviewLabel.visibility = View.GONE
        }
    }

    private fun getPreviewLayoutDisplayName(layout: String): String = when (layout.uppercase()) {
        FolderIconRenderer.PREVIEW_GRID_9 -> "Grid (3x3)"
        FolderIconRenderer.PREVIEW_STACK -> "Stack"
        FolderIconRenderer.PREVIEW_FAN -> "Fan"
        FolderIconRenderer.PREVIEW_LINE -> "Line"
        FolderIconRenderer.PREVIEW_FIRST_APP -> "First app (1 icon)"
        else -> "Grid (2x2)"
    }

    private fun getShapeDisplayName(shapeName: String): String = when (shapeName.trim().lowercase()) {
        "round" -> "Round"
        "rounded square", "roundedsquare" -> "Rounded square"
        "squircle" -> "Squircle"
        "square" -> "Square"
        "teardrop" -> "Teardrop"
        "platform", "none" -> "Platform / None"
        else -> "System default"
    }

    private fun getColorName(color: Int): String = when (color and 0x00FFFFFF) {
        0xFFFFFF -> "White"
        0xE0E0E0 -> "Light Gray"
        0x202534 -> "Dark Slate"
        0x212121, 0x2C2523, 0x2A2A2A -> "Charcoal"
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
}
