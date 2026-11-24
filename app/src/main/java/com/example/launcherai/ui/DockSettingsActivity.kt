package com.example.launcherai.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.launcherai.LauncherApplication
import com.example.launcherai.R
import com.example.launcherai.data.repository.GridChangeResult
import com.example.launcherai.databinding.ActivityDockSettingsBinding
import kotlinx.coroutines.launch

/**
 * Dedicated Dock Settings Hub matching Nova Launcher 8.9.9.
 * Provides controls for Dock master toggle, icon scaling, label visibility,
 * dock icon capacity (3, 5, 7 slots), dock background shapes (Glass, Platform, Transparent),
 * and search bar placement.
 */
class DockSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDockSettingsBinding

    // Current State
    private var dockEnabled: Boolean = true
    private var matchDesktopSize: Boolean = true
    private var desktopIconSizePercent: Int = 100
    private var dockIconSizePercent: Int = 100
    private var dockShowLabels: Boolean = false
    private var dockIconsCount: Int = 5
    private var dockBgStyle: String = "Transparent"
    private var drawBehindNavbar: Boolean = true
    private var searchBarPlacement: String = "DOCK_BELOW"

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_NovaLight)
        super.onCreate(savedInstanceState)
        SystemBarHelper.applySystemBars(this, isLightTheme = true, transparentBars = false)
        binding = ActivityDockSettingsBinding.inflate(layoutInflater)
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
        dockEnabled = prefs.getBoolean("key_dock_enabled", true)
        matchDesktopSize = prefs.getBoolean("key_dock_match_desktop_size", true)
        desktopIconSizePercent = prefs.getInt("key_icon_size_percent", 100)
        dockIconSizePercent = prefs.getInt("key_dock_icon_size_percent", 100)
        dockShowLabels = prefs.getBoolean("key_dock_show_labels", false)
        dockBgStyle = prefs.getString("key_dock_bg_style", "Transparent") ?: "Transparent"
        drawBehindNavbar = prefs.getBoolean("key_dock_draw_behind_navbar", true)
        searchBarPlacement = prefs.getString("key_search_bar_placement_home", "DOCK_BELOW") ?: "DOCK_BELOW"

        val repository = (application as LauncherApplication).repository
        dockIconsCount = repository.getDockCapacity()
    }

    private fun setupViews() {
        binding.swDockEnabled.isChecked = dockEnabled
        binding.layoutDockOptions.visibility = if (dockEnabled) View.VISIBLE else View.GONE

        binding.swMatchDesktopSize.isChecked = matchDesktopSize
        binding.layoutDockIconSize.visibility = if (matchDesktopSize) View.GONE else View.VISIBLE

        binding.tvDockIconSizeValue.text = "$dockIconSizePercent%"
        binding.sbDockIconSize.progress = dockIconSizePercent

        binding.swDockLabels.isChecked = dockShowLabels
        binding.tvDockIconsCountValue.text = "$dockIconsCount"
        binding.tvDockBackgroundValue.text = dockBgStyle
        binding.swDrawBehindNavbar.isChecked = drawBehindNavbar

        binding.tvSearchBarPlacementValue.text = when (searchBarPlacement) {
            "DOCK_ABOVE" -> "Above icons"
            "NONE" -> "None"
            else -> "Below icons"
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Enable Dock Toggle
        binding.swDockEnabled.setOnCheckedChangeListener { _, isChecked ->
            dockEnabled = isChecked
            binding.layoutDockOptions.visibility = if (isChecked) View.VISIBLE else View.GONE
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("key_dock_enabled", isChecked).apply()
            updatePreview()
        }
        binding.rowEnableDock.setOnClickListener {
            binding.swDockEnabled.toggle()
        }

        // Match Desktop Size Toggle
        binding.swMatchDesktopSize.setOnCheckedChangeListener { _, isChecked ->
            matchDesktopSize = isChecked
            binding.layoutDockIconSize.visibility = if (isChecked) View.GONE else View.VISIBLE
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("key_dock_match_desktop_size", isChecked).apply()
            updatePreview()
        }
        binding.rowMatchDesktop.setOnClickListener {
            binding.swMatchDesktopSize.toggle()
        }

        // Dock Icon Size Stepper & Slider
        binding.btnDockIconMinus.setOnClickListener {
            if (dockIconSizePercent > 70) {
                dockIconSizePercent -= 5
                binding.sbDockIconSize.progress = dockIconSizePercent
                saveDockIconSize()
            }
        }
        binding.btnDockIconPlus.setOnClickListener {
            if (dockIconSizePercent < 150) {
                dockIconSizePercent += 5
                binding.sbDockIconSize.progress = dockIconSizePercent
                saveDockIconSize()
            }
        }
        binding.sbDockIconSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                dockIconSizePercent = progress
                binding.tvDockIconSizeValue.text = "$dockIconSizePercent%"
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                saveDockIconSize()
            }
        })

        // Dock Labels Toggle
        binding.swDockLabels.setOnCheckedChangeListener { _, isChecked ->
            dockShowLabels = isChecked
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("key_dock_show_labels", isChecked).apply()
            updatePreview()
        }
        binding.rowDockLabels.setOnClickListener {
            binding.swDockLabels.toggle()
        }

        // Dock Icons Count Dialog (3, 5, 7)
        binding.rowDockIconsCount.setOnClickListener {
            showDockIconsDialog()
        }

        // Dock Background Dialog
        binding.rowDockBackground.setOnClickListener {
            showDockBackgroundDialog()
        }

        // Draw Behind Nav Bar Toggle
        binding.swDrawBehindNavbar.setOnCheckedChangeListener { _, isChecked ->
            drawBehindNavbar = isChecked
            val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("key_dock_draw_behind_navbar", isChecked).apply()
        }
        binding.rowDrawBehindNavbar.setOnClickListener {
            binding.swDrawBehindNavbar.toggle()
        }

        // Search Bar Placement Dialog
        binding.rowSearchBarPlacement.setOnClickListener {
            showSearchBarPlacementDialog()
        }
    }

    private fun saveDockIconSize() {
        val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("key_dock_icon_size_percent", dockIconSizePercent).apply()
        updatePreview()
    }

    private fun showDockIconsDialog() {
        val options = arrayOf("3 icons", "5 icons (Default)", "7 icons")
        val values = intArrayOf(3, 5, 7)
        val currentIndex = values.indexOf(dockIconsCount).coerceAtLeast(1)

        AlertDialog.Builder(this)
            .setTitle("Dock Icons")
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                val selectedCount = values[which]
                lifecycleScope.launch {
                    val repository = (application as LauncherApplication).repository
                    val result = repository.setDockCapacity(selectedCount)
                    if (result is GridChangeResult.Success) {
                        dockIconsCount = selectedCount
                        binding.tvDockIconsCountValue.text = "$dockIconsCount"
                        val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                        prefs.edit().putInt("key_dock_capacity", selectedCount).apply()
                        updatePreview()
                    } else if (result is GridChangeResult.Failure) {
                        Toast.makeText(this@DockSettingsActivity, result.reason, Toast.LENGTH_LONG).show()
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDockBackgroundDialog() {
        val options = arrayOf("Transparent", "Glass", "Platform")
        val currentIndex = options.indexOf(dockBgStyle).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Dock Background")
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                dockBgStyle = options[which]
                binding.tvDockBackgroundValue.text = dockBgStyle
                val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString("key_dock_bg_style", dockBgStyle).apply()
                updatePreview()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSearchBarPlacementDialog() {
        val options = arrayOf("Below icons (Default)", "Above icons", "None")
        val keys = arrayOf("DOCK_BELOW", "DOCK_ABOVE", "NONE")
        val currentIndex = keys.indexOf(searchBarPlacement).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Search Bar Placement")
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                searchBarPlacement = keys[which]
                binding.tvSearchBarPlacementValue.text = when (searchBarPlacement) {
                    "DOCK_ABOVE" -> "Above icons"
                    "NONE" -> "None"
                    else -> "Below icons"
                }
                val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString("key_search_bar_placement_home", searchBarPlacement).apply()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updatePreview() {
        // Opacity when dock is disabled
        binding.cardDockPreviewContainer.alpha = if (dockEnabled) 1.0f else 0.4f

        // Dock background shape & style
        when (dockBgStyle) {
            "Glass" -> binding.dockPreviewBar.setBackgroundResource(R.drawable.bg_dock_glass)
            "Platform" -> binding.dockPreviewBar.setBackgroundResource(R.drawable.bg_dock_platform)
            else -> binding.dockPreviewBar.background = null
        }

        // Icon scaling
        val effectivePercent = if (matchDesktopSize) desktopIconSizePercent else dockIconSizePercent
        val density = resources.displayMetrics.density
        val iconPx = (44f * density * (effectivePercent / 100f)).toInt()

        val previewIcons = listOf(
            binding.ivDockPrev1,
            binding.ivDockPrev2,
            binding.ivDockPrev3,
            binding.ivDockPrev4,
            binding.ivDockPrev5
        )
        previewIcons.forEach { iv ->
            iv.layoutParams = iv.layoutParams.apply {
                width = iconPx
                height = iconPx
            }
        }

        // Label visibility
        val previewLabels = listOf(
            binding.tvDockPrev1,
            binding.tvDockPrev2,
            binding.tvDockPrev3,
            binding.tvDockPrev4,
            binding.tvDockPrev5
        )
        previewLabels.forEach { tv ->
            tv.visibility = if (dockShowLabels) View.VISIBLE else View.GONE
        }
    }
}

