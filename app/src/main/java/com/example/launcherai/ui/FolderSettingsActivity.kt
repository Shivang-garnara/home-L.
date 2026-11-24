package com.example.launcherai.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.launcherai.AppManager
import com.example.launcherai.R
import com.example.launcherai.databinding.ActivityFolderSettingsBinding
import com.example.launcherai.icons.FolderIconRenderer
import kotlinx.coroutines.launch

/**
 * Dedicated Folders Settings activity matching Nova Launcher 8.9.9 (SettingsFolder / SettingsFolderIcon).
 * Provides live folder icon preview, preview layout selector, shape picker, background color & alpha slider,
 * outline stroke toggle, and clip to bounds toggle.
 */
class FolderSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFolderSettingsBinding

    // Folder Icon State
    private var previewLayout: String = FolderIconRenderer.PREVIEW_GRID
    private var shape: String = FolderIconRenderer.SHAPE_SYSTEM
    private var bgColor: Int = 0xFFE0E0E0.toInt()
    private var bgAlpha: Int = 20
    private var outline: Boolean = true
    private var clipToBounds: Boolean = true

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
        previewLayout = prefs.getString("key_folder_icon_preview", FolderIconRenderer.PREVIEW_GRID) ?: FolderIconRenderer.PREVIEW_GRID
        shape = prefs.getString("key_folder_icon_shape", FolderIconRenderer.SHAPE_SYSTEM) ?: FolderIconRenderer.SHAPE_SYSTEM
        bgColor = prefs.getInt("key_folder_icon_bg_color", 0xFFE0E0E0.toInt())
        bgAlpha = prefs.getInt("key_folder_icon_bg_alpha", 20)
        outline = prefs.getBoolean("key_folder_icon_outline", true)
        clipToBounds = prefs.getBoolean("key_folder_icon_clip", true)
    }

    private fun loadSampleDrawables() {
        // Provide immediate fallback icons
        sampleDrawables = listOfNotNull(
            ContextCompat.getDrawable(this, R.drawable.ic_launcher_foreground),
            ContextCompat.getDrawable(this, R.drawable.ic_settings_folders),
            ContextCompat.getDrawable(this, R.drawable.ic_settings_home),
            ContextCompat.getDrawable(this, R.drawable.ic_settings_drawer)
        )

        // Asynchronously load real installed app icons for accurate live preview
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
        binding.tvFolderPreviewLayoutSubtitle.text = getPreviewLayoutDisplayName(previewLayout)
        binding.tvFolderShapeSubtitle.text = getShapeDisplayName(shape)
        updateColorSwatchUI()

        binding.sbFolderBgAlpha.progress = bgAlpha
        binding.tvFolderBgAlphaValue.text = "$bgAlpha%"

        binding.swFolderOutline.isChecked = outline
        binding.swFolderClip.isChecked = clipToBounds
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.rowFolderPreviewLayout.setOnClickListener {
            showPreviewLayoutDialog()
        }

        binding.rowFolderShape.setOnClickListener {
            showShapeDialog()
        }

        binding.rowFolderBgColor.setOnClickListener {
            showColorPickerDialog()
        }

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

        binding.rowFolderOutline.setOnClickListener {
            binding.swFolderOutline.toggle()
        }

        binding.swFolderClip.setOnCheckedChangeListener { _, isChecked ->
            clipToBounds = isChecked
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("key_folder_icon_clip", isChecked).apply()
            updatePreview()
        }

        binding.rowFolderClip.setOnClickListener {
            binding.swFolderClip.toggle()
        }
    }

    private fun showPreviewLayoutDialog() {
        val options = arrayOf(
            "Grid (2x2)",
            "Grid (3x3)",
            "Stack",
            "Fan",
            "Line"
        )
        val values = arrayOf(
            FolderIconRenderer.PREVIEW_GRID,
            FolderIconRenderer.PREVIEW_GRID_9,
            FolderIconRenderer.PREVIEW_STACK,
            FolderIconRenderer.PREVIEW_FAN,
            FolderIconRenderer.PREVIEW_LINE
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
    }

    private fun getPreviewLayoutDisplayName(layout: String): String = when (layout.uppercase()) {
        FolderIconRenderer.PREVIEW_GRID_9 -> "Grid (3x3)"
        FolderIconRenderer.PREVIEW_STACK -> "Stack"
        FolderIconRenderer.PREVIEW_FAN -> "Fan"
        FolderIconRenderer.PREVIEW_LINE -> "Line"
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
