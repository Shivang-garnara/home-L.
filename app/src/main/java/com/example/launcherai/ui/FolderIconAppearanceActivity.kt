package com.example.launcherai.ui

import android.app.WallpaperManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.launcherai.R
import com.example.launcherai.util.AppManager
import com.example.launcherai.databinding.ActivityFolderIconAppearanceBinding
import com.example.launcherai.icons.FolderIconRenderer
import com.example.launcherai.icons.IconShapeMasker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Nova-parity Folder Icon Appearance Sub-Screen (mirroring SettingsFolderIcon).
 * Features live wallpaper preview, visual grids for 7 Icon layouts (including Radial-5 & Radial-9)
 * and 6 Background shapes + More shapes dialog, Clip/Outline/Match folder window color toggles,
 * and conditional custom Colors + Transparency controls when unmatched.
 */
class FolderIconAppearanceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFolderIconAppearanceBinding
    private lateinit var prefs: SharedPreferences
    private var sampleDrawables: List<Drawable> = emptyList()

    private var previewLayout: String = FolderIconRenderer.PREVIEW_GRID
    private var shape: String = FolderIconRenderer.SHAPE_SYSTEM
    private var clipToBounds: Boolean = true
    private var outline: Boolean = true
    private var matchWindowColor: Boolean = true
    private var customBgColor: Int = 0xFFE0E0E0.toInt()
    private var customBgTransparency: Int = 20
    private var isCustomPaletteExpanded: Boolean = true

    private val mainShapes = listOf(
        FolderIconRenderer.SHAPE_SYSTEM to "Adaptive",
        FolderIconRenderer.SHAPE_ROUND to "Round",
        FolderIconRenderer.SHAPE_SQUIRCLE to "Squircle",
        FolderIconRenderer.SHAPE_ROUNDED_SQUARE to "Rounded square",
        FolderIconRenderer.SHAPE_FLOWER to "Flower",
        FolderIconRenderer.SHAPE_PLATFORM to "Platform"
    )

    private val layoutOptions = listOf(
        FolderIconRenderer.PREVIEW_GRID to "Grid",
        FolderIconRenderer.PREVIEW_GRID_9 to "Grid",
        FolderIconRenderer.PREVIEW_RADIAL_5 to "Radial",
        FolderIconRenderer.PREVIEW_RADIAL_9 to "Radial",
        FolderIconRenderer.PREVIEW_STACK to "Stack",
        FolderIconRenderer.PREVIEW_FAN to "Fan",
        FolderIconRenderer.PREVIEW_LINE to "Line"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFolderIconAppearanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemBarHelper.applySystemBars(this, isLightTheme = true)
        prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)

        loadState()
        setupWallpaperBackdrop()
        loadSampleIcons()
        setupViews()
        refreshAll()
    }

    private fun loadState() {
        previewLayout = prefs.getString("key_folder_icon_preview", FolderIconRenderer.PREVIEW_GRID)
            ?: FolderIconRenderer.PREVIEW_GRID
        shape = prefs.getString("key_folder_icon_shape", FolderIconRenderer.SHAPE_SYSTEM)
            ?: FolderIconRenderer.SHAPE_SYSTEM
        clipToBounds = prefs.getBoolean("key_folder_icon_clip", true)
        outline = prefs.getBoolean("key_folder_icon_outline", true)
        matchWindowColor = prefs.getBoolean("key_folder_icon_match_window_color", true)
        customBgColor = prefs.getInt("key_folder_icon_bg_color", 0xFFE0E0E0.toInt())
        customBgTransparency = prefs.getInt("key_folder_icon_bg_alpha", 20).coerceIn(0, 100)
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
            val apps = AppManager.getInstalledApps(this@FolderIconAppearanceActivity)
            val list = apps.take(9).mapNotNull { it.icon }
            val fallback = ContextCompat.getDrawable(this@FolderIconAppearanceActivity, R.mipmap.ic_launcher)
            val drawables = if (list.isNotEmpty()) list else List(9) { fallback!! }
            withContext(Dispatchers.Main) {
                sampleDrawables = drawables
                buildIconLayoutGrid()
                refreshLivePreview()
            }
        }
    }

    private fun setupViews() {
        binding.btnBack.setOnClickListener { finish() }

        binding.cbClipToBackground.setOnCheckedChangeListener { _, isChecked ->
            clipToBounds = isChecked
            prefs.edit().putBoolean("key_folder_icon_clip", isChecked).apply()
            refreshLivePreview()
        }
        binding.rowClipToBackground.setOnClickListener {
            if (binding.cbClipToBackground.isEnabled) {
                binding.cbClipToBackground.isChecked = !binding.cbClipToBackground.isChecked
            }
        }

        binding.cbOutline.setOnCheckedChangeListener { _, isChecked ->
            outline = isChecked
            prefs.edit().putBoolean("key_folder_icon_outline", isChecked).apply()
            refreshLivePreview()
        }
        binding.rowOutline.setOnClickListener {
            binding.cbOutline.isChecked = !binding.cbOutline.isChecked
        }

        binding.cbMatchWindowColor.setOnCheckedChangeListener { _, isChecked ->
            matchWindowColor = isChecked
            prefs.edit().putBoolean("key_folder_icon_match_window_color", isChecked).apply()
            refreshColorSectionVisibility()
            refreshLivePreview()
        }
        binding.rowMatchWindowColor.setOnClickListener {
            binding.cbMatchWindowColor.isChecked = !binding.cbMatchWindowColor.isChecked
        }

        binding.rowIconColorsHeader.setOnClickListener {
            isCustomPaletteExpanded = !isCustomPaletteExpanded
            binding.containerIconColorsBody.visibility = if (isCustomPaletteExpanded) View.VISIBLE else View.GONE
            binding.ivIconColorsChevron.setImageResource(
                if (isCustomPaletteExpanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down
            )
        }

        binding.rowIconCustomColor.setOnClickListener {
            showCustomHexDialog()
        }

        binding.sbIconTransparency.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                customBgTransparency = progress.coerceIn(0, 100)
                binding.tvIconTransparencyValue.text = "$customBgTransparency%"
                if (fromUser) {
                    prefs.edit().putInt("key_folder_icon_bg_alpha", customBgTransparency).apply()
                    refreshLivePreview()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun refreshAll() {
        binding.cbClipToBackground.isChecked = clipToBounds
        binding.cbOutline.isChecked = outline
        binding.cbMatchWindowColor.isChecked = matchWindowColor
        binding.sbIconTransparency.progress = customBgTransparency
        binding.tvIconTransparencyValue.text = "$customBgTransparency%"
        refreshClipEnabledState()
        refreshColorSectionVisibility()
        buildIconLayoutGrid()
        buildBackgroundShapeGrid()
        rebuildPaletteSwatches()
        refreshLivePreview()
    }

    private fun refreshClipEnabledState() {
        val isPlatform = shape.equals(FolderIconRenderer.SHAPE_PLATFORM, ignoreCase = true)
        binding.cbClipToBackground.isEnabled = !isPlatform
        binding.rowClipToBackground.alpha = if (isPlatform) 0.45f else 1.0f
    }

    private fun refreshColorSectionVisibility() {
        binding.containerCustomIconColors.visibility = if (matchWindowColor) View.GONE else View.VISIBLE
        if (!matchWindowColor) {
            rebuildPaletteSwatches()
        }
    }

    private fun buildIconLayoutGrid() {
        val grid = binding.gridIconLayouts
        grid.removeAllViews()
        val density = resources.displayMetrics.density
        val thumbPx = (44 * density).toInt()
        val activeColor = Color.parseColor("#6D3B68")
        val normalColor = ContextCompat.getColor(this, R.color.nova_text_brown_primary)

        val fallback = ContextCompat.getDrawable(this, R.mipmap.ic_launcher)!!
        val iconsForThumb = if (sampleDrawables.isNotEmpty()) sampleDrawables else List(9) { fallback }

        for ((layoutKey, label) in layoutOptions) {
            val isSelected = previewLayout.equals(layoutKey, ignoreCase = true)
            val cell = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding((4 * density).toInt(), (8 * density).toInt(), (4 * density).toInt(), (8 * density).toInt())
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins((3 * density).toInt(), (4 * density).toInt(), (3 * density).toInt(), (4 * density).toInt())
                }
                background = GradientDrawable().apply {
                    cornerRadius = 12f * density
                    setColor(if (isSelected) Color.parseColor("#EDE5F4") else Color.TRANSPARENT)
                    if (isSelected) {
                        setStroke((2 * density).toInt(), activeColor)
                    }
                }
                setOnClickListener {
                    previewLayout = layoutKey
                    prefs.edit().putString("key_folder_icon_preview", layoutKey).apply()
                    buildIconLayoutGrid()
                    refreshLivePreview()
                }
            }

            val iv = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(thumbPx, thumbPx)
                scaleType = ImageView.ScaleType.FIT_CENTER
                setImageDrawable(
                    FolderIconRenderer.renderFolderIcon(
                        context = this@FolderIconAppearanceActivity,
                        children = iconsForThumb,
                        targetSizePx = thumbPx,
                        previewLayout = layoutKey,
                        shape = shape,
                        bgColor = 0xFFE8E0DF.toInt(),
                        bgAlphaPercent = 15,
                        outline = true,
                        clipToBounds = clipToBounds
                    )
                )
            }
            val tv = TextView(this).apply {
                text = label
                textSize = 11.5f
                gravity = Gravity.CENTER
                maxLines = 1
                setTextColor(if (isSelected) activeColor else normalColor)
                setPadding(0, (4 * density).toInt(), 0, 0)
            }
            cell.addView(iv)
            cell.addView(tv)
            grid.addView(cell)
        }
    }

    private fun buildBackgroundShapeGrid() {
        val grid = binding.gridBackgroundShapes
        grid.removeAllViews()
        val density = resources.displayMetrics.density
        val thumbPx = (38 * density).toInt()
        val activeColor = Color.parseColor("#6D3B68")
        val normalTint = ContextCompat.getColor(this, R.color.nova_text_brown_secondary)
        val isCustomMoreSelected = mainShapes.none { it.first.equals(shape, ignoreCase = true) }

        fun createShapeCell(
            label: String,
            isSelected: Boolean,
            thumbDrawable: Drawable?,
            onClick: () -> Unit
        ): LinearLayout {
            return LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding((4 * density).toInt(), (8 * density).toInt(), (4 * density).toInt(), (8 * density).toInt())
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins((3 * density).toInt(), (4 * density).toInt(), (3 * density).toInt(), (4 * density).toInt())
                }
                background = GradientDrawable().apply {
                    cornerRadius = 12f * density
                    setColor(if (isSelected) Color.parseColor("#EDE5F4") else Color.TRANSPARENT)
                    if (isSelected) setStroke((2 * density).toInt(), activeColor)
                }
                setOnClickListener { onClick() }

                val iv = ImageView(this@FolderIconAppearanceActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(thumbPx, thumbPx)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    if (thumbDrawable != null) setImageDrawable(thumbDrawable)
                }
                val tv = TextView(this@FolderIconAppearanceActivity).apply {
                    text = label
                    textSize = 11f
                    gravity = Gravity.CENTER
                    maxLines = 2
                    setTextColor(if (isSelected) activeColor else ContextCompat.getColor(this@FolderIconAppearanceActivity, R.color.nova_text_brown_primary))
                    setPadding(0, (4 * density).toInt(), 0, 0)
                }
                addView(iv)
                addView(tv)
            }
        }

        for ((shapeKey, label) in mainShapes) {
            val isSelected = shape.equals(shapeKey, ignoreCase = true)
            val effectivePreviewShape = if (shapeKey == FolderIconRenderer.SHAPE_SYSTEM) {
                prefs.getString("key_icon_shape", FolderIconRenderer.SHAPE_ROUND) ?: FolderIconRenderer.SHAPE_ROUND
            } else {
                shapeKey
            }
            val thumbTint = if (isSelected) activeColor else normalTint
            val thumb = if (shapeKey == FolderIconRenderer.SHAPE_PLATFORM) {
                ContextCompat.getDrawable(this, R.drawable.ic_settings_folders)?.mutate()?.apply { setTint(thumbTint) }
            } else {
                IconShapeMasker.createShapeThumbnail(this, effectivePreviewShape, thumbPx, thumbTint)
            }
            grid.addView(
                createShapeCell(label, isSelected, thumb) {
                    shape = shapeKey
                    prefs.edit().putString("key_folder_icon_shape", shapeKey).apply()
                    refreshClipEnabledState()
                    buildBackgroundShapeGrid()
                    buildIconLayoutGrid()
                    refreshLivePreview()
                }
            )
        }

        // "More" 7th tile opening MoreShapesDialogFragment
        val moreThumb = if (isCustomMoreSelected) {
            IconShapeMasker.createShapeThumbnail(this, shape, thumbPx, activeColor)
        } else {
            ContextCompat.getDrawable(this, R.drawable.ic_plus)?.mutate()?.apply { setTint(normalTint) }
        }
        val moreLabel = if (isCustomMoreSelected) shape else "More"
        grid.addView(
            createShapeCell(moreLabel, isCustomMoreSelected, moreThumb) {
                val dlg = MoreShapesDialogFragment.newInstance(
                    prefKey = "key_folder_icon_shape",
                    defaultShape = FolderIconRenderer.SHAPE_SYSTEM
                )
                dlg.onShapeSelectedListener = { chosen ->
                    shape = chosen
                    refreshClipEnabledState()
                    buildBackgroundShapeGrid()
                    buildIconLayoutGrid()
                    refreshLivePreview()
                }
                dlg.show(supportFragmentManager, MoreShapesDialogFragment.TAG)
            }
        )
    }

    private fun rebuildPaletteSwatches() {
        populateSwatchGrid(binding.gridIconSystemPalette, SearchBarConfig.SYSTEM_PALETTE_COLORS)
        populateSwatchGrid(binding.gridIconFixedPalette, SearchBarConfig.FIXED_PALETTE_COLORS)
        binding.tvIconCustomHex.text = String.format("#%06X", 0xFFFFFF and customBgColor)
        (binding.viewIconColorBadge.background as? GradientDrawable)?.setColor(customBgColor or 0xFF000000.toInt())
    }

    private fun populateSwatchGrid(grid: GridLayout, colors: IntArray) {
        grid.removeAllViews()
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
                val isSelected = (color and 0xFFFFFF) == (customBgColor and 0xFFFFFF)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                    if (isSelected) {
                        setStroke((3 * density).toInt(), Color.parseColor("#6D3B68"))
                    } else {
                        setStroke((1 * density).toInt(), Color.parseColor("#33000000"))
                    }
                }
                setOnClickListener {
                    customBgColor = color
                    prefs.edit().putInt("key_folder_icon_bg_color", color).apply()
                    rebuildPaletteSwatches()
                    refreshLivePreview()
                }
            }
            grid.addView(circle)
        }
    }

    private fun showCustomHexDialog() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            setText(String.format("%06X", 0xFFFFFF and customBgColor))
            hint = "Hex e.g. E0E0E0"
            setPadding(48, 32, 48, 24)
        }
        AlertDialog.Builder(this)
            .setTitle("Custom color")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val raw = input.text.toString().trim().removePrefix("#")
                try {
                    val parsed = Integer.parseInt(raw, 16) or 0xFF000000.toInt()
                    customBgColor = parsed
                    prefs.edit().putInt("key_folder_icon_bg_color", parsed).apply()
                    rebuildPaletteSwatches()
                    refreshLivePreview()
                } catch (_: Exception) {
                    Toast.makeText(this, "Invalid hex color", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun refreshLivePreview() {
        val icons = if (sampleDrawables.isNotEmpty()) {
            sampleDrawables
        } else {
            val fallback = ContextCompat.getDrawable(this, R.mipmap.ic_launcher) ?: return
            List(9) { fallback }
        }
        val sizePx = (76 * resources.displayMetrics.density).toInt()
        val drawable = FolderIconRenderer.renderFolderIconFromPrefs(
            context = this,
            childrenDrawables = icons,
            targetSizePx = sizePx
        )
        binding.ivFolderIconPreview.setImageDrawable(drawable)
    }
}
