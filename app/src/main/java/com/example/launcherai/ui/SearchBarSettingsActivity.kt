package com.example.launcherai.ui

import android.app.WallpaperManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.launcherai.R
import com.example.launcherai.databinding.ActivitySearchBarSettingsBinding
import com.google.android.material.materialswitch.MaterialSwitch
import java.util.Collections

/**
 * Dedicated Nova-parity Search Bar Customization Screen.
 * Supports MODE_DESKTOP, MODE_DRAWER, and MODE_NOVA_NOW with live preview,
 * Placement dropdown, expandable 3-tab Color picker, Transparency & Corner radius sliders +
 * direct number dialogs, Wireframe/Shadow, 4 Logo tabs + Monochrome, drag-to-reorder
 * Additional Actions with Weather/Assistant/Date gear dialogs, and Search Interface swap.
 */
class SearchBarSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBarSettingsBinding
    private lateinit var prefs: SharedPreferences
    private lateinit var config: SearchBarConfig
    private var mode: String = SearchBarConfig.MODE_DESKTOP

    private var activeColorTab = TAB_SURFACE
    private var isColorsExpanded = false
    private lateinit var actionAdapter: AdditionalActionsAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper

    companion object {
        const val EXTRA_MODE = "extra_bar_mode"
        private const val TAB_SURFACE = 0
        private const val TAB_TEXT = 1
        private const val TAB_ACCENT = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBarSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemBarHelper.applySystemBars(this, isLightTheme = true)
        prefs = getSharedPreferences(SearchSettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
        mode = intent.getStringExtra(EXTRA_MODE) ?: SearchBarConfig.MODE_DESKTOP
        config = SearchBarConfig.load(prefs, mode)

        binding.btnBack.setOnClickListener { finish() }

        binding.tvBarTitle.text = when (mode) {
            SearchBarConfig.MODE_DRAWER -> "Drawer search bar"
            SearchBarConfig.MODE_NOVA_NOW -> "Settings search bar"
            else -> "Desktop search bar"
        }

        setupWallpaperBackdrop()
        setupPlacementAndModeVisibility()
        setupColorsAccordion()
        setupSlidersAndCheckboxes()
        setupLogoSelector()
        setupAdditionalActions()
        setupSearchInterface()
        refreshAllUi()
    }

    private fun saveConfig() {
        SearchBarConfig.save(prefs, config)
        refreshLivePreview()
    }

    private fun setupWallpaperBackdrop() {
        try {
            val wm = WallpaperManager.getInstance(this)
            val wp = wm.drawable
            if (wp != null) {
                binding.previewBackdrop.background = wp
            }
        } catch (_: Exception) {
            // Keep dark fallback background
        }
    }

    private fun setupPlacementAndModeVisibility() {
        if (mode == SearchBarConfig.MODE_NOVA_NOW) {
            binding.rowPlacement.visibility = View.GONE
            binding.rowNovaNowEnable.visibility = View.VISIBLE
            binding.rowTransparency.visibility = View.GONE
            binding.rowWireframeShadow.visibility = View.VISIBLE
            binding.boxWireframe.visibility = View.INVISIBLE
            binding.sectionLogo.visibility = View.GONE
            binding.sectionAdditionalActions.visibility = View.GONE
            binding.sectionSearchInterface.visibility = View.GONE

            binding.sbCornerRadius.max = 100
            binding.swNovaNowEnable.isChecked = config.enabled
            binding.swNovaNowEnable.setOnCheckedChangeListener { _, isChecked ->
                config = config.copy(enabled = isChecked)
                saveConfig()
            }
        } else {
            binding.rowPlacement.visibility = View.VISIBLE
            binding.rowNovaNowEnable.visibility = View.GONE
            binding.sbCornerRadius.max = 22
            if (mode == SearchBarConfig.MODE_DRAWER) {
                binding.rowDirectlyUseGoogleApp.visibility = View.GONE
            }

            binding.btnPlacementDropdown.setOnClickListener {
                if (mode == SearchBarConfig.MODE_DESKTOP) {
                    val labels = arrayOf("None", "Widget", "Dock (Above icons)", "Dock (Below icons)", "Persistent")
                    val values = arrayOf("NONE", "WIDGET", "DOCK_ABOVE", "DOCK_BELOW", "PERSISTENT")
                    val curIdx = values.indexOf(config.placement).coerceAtLeast(3)
                    AlertDialog.Builder(this)
                        .setTitle("Placement")
                        .setSingleChoiceItems(labels, curIdx) { dialog, idx ->
                            config = config.copy(placement = values[idx])
                            saveConfig()
                            refreshPlacementLabel()
                            dialog.dismiss()
                        }
                        .show()
                } else {
                    val labels = arrayOf("None", "Top", "Bottom")
                    val curIdx = labels.indexOfFirst { it.equals(config.placement, ignoreCase = true) }.coerceAtLeast(1)
                    AlertDialog.Builder(this)
                        .setTitle("Placement")
                        .setSingleChoiceItems(labels, curIdx) { dialog, idx ->
                            config = config.copy(placement = labels[idx])
                            saveConfig()
                            refreshPlacementLabel()
                            dialog.dismiss()
                        }
                        .show()
                }
            }
        }
    }

    // =========================================================================
    // COLORS ACCORDION (SYSTEM PALETTE + FIXED COLORS + CUSTOM COLOR + 3 TABS)
    // =========================================================================
    private fun setupColorsAccordion() {
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

        binding.sbMinimumContrast.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val delta = (20 + progress).coerceIn(20, 50)
                binding.tvMinimumContrastValue.text = "${delta}Δ"
                if (fromUser) {
                    config = when (activeColorTab) {
                        TAB_TEXT -> config.copy(textMinContrast = delta)
                        TAB_ACCENT -> config.copy(accentMinContrast = delta)
                        else -> config
                    }
                    saveConfig()
                    refreshColorBadges()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.rowCustomColor.setOnClickListener {
            showCustomColorDialog()
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
        val showContrast = (tab == TAB_TEXT || tab == TAB_ACCENT)
        binding.rowMinimumContrast.visibility = if (showContrast) View.VISIBLE else View.GONE
        if (showContrast) {
            refreshMinimumContrastSlider()
        }
        rebuildPaletteGrids()
    }

    private fun refreshMinimumContrastSlider() {
        val delta = if (activeColorTab == TAB_TEXT) config.textMinContrast else config.accentMinContrast
        binding.sbMinimumContrast.progress = (delta - 20).coerceIn(0, 30)
        binding.tvMinimumContrastValue.text = "${delta}Δ"

        val bgHsl = FloatArray(3)
        androidx.core.graphics.ColorUtils.colorToHSL(config.surfaceColor or 0xFF000000.toInt(), bgHsl)
        val l = bgHsl[2]
        val startL = (if (l <= 0.5f) l + 0.20f else l - 0.20f).coerceIn(0f, 1f)
        val endL = (if (l <= 0.5f) l + 0.50f else l - 0.50f).coerceIn(0f, 1f)
        val startColor = androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(0f, 0f, startL))
        val endColor = androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(0f, 0f, endL))
        val density = resources.displayMetrics.density
        binding.trackMinimumContrast.background = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(startColor, endColor)
        ).apply {
            cornerRadius = 6f * density
        }
    }

    private fun currentTabColor(): Int = when (activeColorTab) {
        TAB_TEXT -> config.textColor
        TAB_ACCENT -> config.accentColor
        else -> config.surfaceColor
    }

    private fun applyColorForActiveTab(color: Int) {
        config = when (activeColorTab) {
            TAB_TEXT -> config.copy(textColor = color)
            TAB_ACCENT -> config.copy(accentColor = color)
            else -> config.copy(surfaceColor = color)
        }
        saveConfig()
        rebuildPaletteGrids()
        refreshColorBadges()
    }

    private fun rebuildPaletteGrids() {
        populateSwatchGrid(binding.gridSystemPalette, SearchBarConfig.SYSTEM_PALETTE_COLORS)
        populateSwatchGrid(binding.gridFixedPalette, SearchBarConfig.FIXED_PALETTE_COLORS)
        binding.tvCustomColorHex.text = String.format("#%06X", 0xFFFFFF and currentTabColor())
    }

    private fun populateSwatchGrid(grid: android.widget.GridLayout, colors: IntArray) {
        grid.removeAllViews()
        val selected = currentTabColor()
        val density = resources.displayMetrics.density
        val sizePx = (40 * density).toInt()
        val marginPx = (5 * density).toInt()

        for (color in colors) {
            val circle = View(this).apply {
                layoutParams = android.widget.GridLayout.LayoutParams().apply {
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

    private fun showCustomColorDialog() {
        val tabTitle = when (activeColorTab) {
            TAB_TEXT -> "Text color"
            TAB_ACCENT -> "Accent color"
            else -> "Surface color"
        }
        NovaColorPickerBottomSheet.newInstance(
            title = tabTitle,
            initialColor = currentTabColor()
        ) { chosen ->
            applyColorForActiveTab(chosen)
        }.show(supportFragmentManager, NovaColorPickerBottomSheet.TAG)
    }

    // =========================================================================
    // TRANSPARENCY, WIREFRAME, SHADOW, CORNER RADIUS
    // =========================================================================
    private fun setupSlidersAndCheckboxes() {
        // Transparency
        binding.sbTransparency.progress = config.transparencyPercent
        binding.tvTransparencyValue.text = "${config.transparencyPercent}%"
        binding.sbTransparency.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    config = config.copy(transparencyPercent = progress)
                    binding.tvTransparencyValue.text = "$progress%"
                    saveConfig()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        binding.btnTransparencyDec.setOnClickListener {
            val next = (config.transparencyPercent - 5).coerceAtLeast(0)
            config = config.copy(transparencyPercent = next)
            binding.sbTransparency.progress = next
            binding.tvTransparencyValue.text = "$next%"
            saveConfig()
        }
        binding.btnTransparencyInc.setOnClickListener {
            val next = (config.transparencyPercent + 5).coerceAtMost(100)
            config = config.copy(transparencyPercent = next)
            binding.sbTransparency.progress = next
            binding.tvTransparencyValue.text = "$next%"
            saveConfig()
        }
        binding.tvTransparencyValue.setOnClickListener {
            showPrecisionNumberDialog("Transparency", config.transparencyPercent, 0, 100) { v ->
                config = config.copy(transparencyPercent = v)
                binding.sbTransparency.progress = v
                binding.tvTransparencyValue.text = "$v%"
                saveConfig()
            }
        }

        // Wireframe & Shadow
        binding.cbWireframe.isChecked = config.isWireframe
        binding.cbWireframe.setOnCheckedChangeListener { _, isChecked ->
            config = config.copy(isWireframe = isChecked)
            saveConfig()
        }
        binding.cbShadow.isChecked = config.hasShadow
        binding.cbShadow.setOnCheckedChangeListener { _, isChecked ->
            config = config.copy(hasShadow = isChecked)
            saveConfig()
        }

        // Corner Radius
        val maxRad = if (mode == SearchBarConfig.MODE_NOVA_NOW) 100 else 22
        val suffix = if (mode == SearchBarConfig.MODE_NOVA_NOW) "%" else "dp"
        binding.sbCornerRadius.progress = config.cornerRadius.coerceIn(0, maxRad)
        binding.tvCornerRadiusValue.text = "${config.cornerRadius}$suffix"
        binding.sbCornerRadius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    config = config.copy(cornerRadius = progress)
                    binding.tvCornerRadiusValue.text = "$progress$suffix"
                    saveConfig()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        binding.btnRadiusDec.setOnClickListener {
            val step = if (mode == SearchBarConfig.MODE_NOVA_NOW) 5 else 1
            val next = (config.cornerRadius - step).coerceAtLeast(0)
            config = config.copy(cornerRadius = next)
            binding.sbCornerRadius.progress = next
            binding.tvCornerRadiusValue.text = "$next$suffix"
            saveConfig()
        }
        binding.btnRadiusInc.setOnClickListener {
            val step = if (mode == SearchBarConfig.MODE_NOVA_NOW) 5 else 1
            val next = (config.cornerRadius + step).coerceAtMost(maxRad)
            config = config.copy(cornerRadius = next)
            binding.sbCornerRadius.progress = next
            binding.tvCornerRadiusValue.text = "$next$suffix"
            saveConfig()
        }
        binding.tvCornerRadiusValue.setOnClickListener {
            showPrecisionNumberDialog("Corner radius", config.cornerRadius, 0, maxRad) { v ->
                config = config.copy(cornerRadius = v)
                binding.sbCornerRadius.progress = v
                binding.tvCornerRadiusValue.text = "$v$suffix"
                saveConfig()
            }
        }
    }

    private fun showPrecisionNumberDialog(title: String, current: Int, min: Int, max: Int, onApply: (Int) -> Unit) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(current.toString())
            setSelection(text.length)
        }
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val v = input.text.toString().toIntOrNull()?.coerceIn(min, max) ?: current
                onApply(v)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // =========================================================================
    // LOGO SELECTOR + MONOCHROME
    // =========================================================================
    private fun setupLogoSelector() {
        val buttons = arrayOf(binding.btnLogo0, binding.btnLogo1, binding.btnLogo2, binding.btnLogo3)
        buttons.forEachIndexed { idx, view ->
            view.setOnClickListener {
                config = config.copy(logoStyle = idx)
                saveConfig()
                refreshLogoSelection()
            }
        }

        binding.cbMonochrome.isChecked = config.isMonochrome
        binding.cbMonochrome.setOnCheckedChangeListener { _, isChecked ->
            config = config.copy(isMonochrome = isChecked)
            saveConfig()
        }
    }

    // =========================================================================
    // ADDITIONAL ACTIONS (REORDER + CHECKBOX + GEAR DIALOGS)
    // =========================================================================
    private fun setupAdditionalActions() {
        val items = config.orderedActions.toMutableList()
        actionAdapter = AdditionalActionsAdapter(items)
        binding.rvAdditionalActions.layoutManager = LinearLayoutManager(this)
        binding.rvAdditionalActions.adapter = actionAdapter

        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition
                if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false
                Collections.swap(actionAdapter.items, from, to)
                actionAdapter.notifyItemMoved(from, to)
                config = config.copy(orderedActions = actionAdapter.items.toList())
                saveConfig()
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        }
        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(binding.rvAdditionalActions)
    }

    // =========================================================================
    // SEARCH INTERFACE (NOVA SEARCH + DIRECTLY USE GOOGLE APP SWAP)
    // =========================================================================
    private fun setupSearchInterface() {
        binding.rowNovaSearchInterface.setOnClickListener {
            finish()
        }

        binding.cbDirectlyUseGoogle.isChecked = config.useGoogleAppDirectly
        binding.cbDirectlyUseGoogle.setOnCheckedChangeListener { _, isChecked ->
            // Smart Nova swap: replace Google App row with Nova Search (and vice-versa)
            val newOrder = config.orderedActions.map { action ->
                when {
                    isChecked && action == SearchBarConfig.ACTION_GOOGLE_APP -> SearchBarConfig.ACTION_NOVA_SEARCH
                    !isChecked && action == SearchBarConfig.ACTION_NOVA_SEARCH -> SearchBarConfig.ACTION_GOOGLE_APP
                    else -> action
                }
            }
            val newEnabled = config.enabledActions.map { action ->
                when {
                    isChecked && action == SearchBarConfig.ACTION_GOOGLE_APP -> SearchBarConfig.ACTION_NOVA_SEARCH
                    !isChecked && action == SearchBarConfig.ACTION_NOVA_SEARCH -> SearchBarConfig.ACTION_GOOGLE_APP
                    else -> action
                }
            }.toSet()
            val newLogo = if (isChecked) SearchBarConfig.LOGO_GOOGLE_G else SearchBarConfig.LOGO_NOVA_DOTS
            config = config.copy(
                useGoogleAppDirectly = isChecked,
                orderedActions = newOrder,
                enabledActions = newEnabled,
                logoStyle = newLogo
            )
            saveConfig()
            actionAdapter.replaceAll(newOrder)
            refreshLogoSelection()
        }
    }

    // =========================================================================
    // SUB-DIALOGS FOR GEAR ICONS (WEATHER, ASSISTANT, DATE)
    // =========================================================================
    private fun showWeatherGearDialog() {
        val density = resources.displayMetrics.density
        val pad = (20 * density).toInt()
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        val tvUnits = TextView(this).apply {
            text = "Units: ${config.weatherUnits}"
            textSize = 16f
            setTextColor(ContextCompat.getColor(this@SearchBarSettingsActivity, R.color.nova_text_brown_primary))
            setPadding(0, 12, 0, 12)
            setOnClickListener {
                val next = if (config.weatherUnits == "Fahrenheit") "Celsius" else "Fahrenheit"
                config = config.copy(weatherUnits = next)
                text = "Units: $next"
                saveConfig()
            }
        }

        val tvNotice = TextView(this).apply {
            text = "AccuWeather\nLauncherAI and AccuWeather's partnership provides weather information that clicks through to https://accuweather.com."
            textSize = 13f
            setTextColor(ContextCompat.getColor(this@SearchBarSettingsActivity, R.color.nova_text_brown_secondary))
            setPadding(0, 16, 0, 16)
        }

        val swCustomTab = MaterialSwitch(this).apply {
            text = "Open in Custom Tab"
            isChecked = config.weatherCustomTab
            setOnCheckedChangeListener { _, isChecked ->
                config = config.copy(weatherCustomTab = isChecked)
                saveConfig()
            }
        }

        val tvLocation = TextView(this).apply {
            text = "Location: ${config.weatherLocation} (Tap to toggle Auto/Manual)"
            textSize = 15f
            setTextColor(ContextCompat.getColor(this@SearchBarSettingsActivity, R.color.nova_text_brown_primary))
            setPadding(0, 18, 0, 8)
            setOnClickListener {
                val next = if (config.weatherLocation == "Auto") "Manual" else "Auto"
                config = config.copy(weatherLocation = next)
                text = "Location: $next (Tap to toggle Auto/Manual)"
                saveConfig()
            }
        }

        container.addView(tvUnits)
        container.addView(tvNotice)
        container.addView(swCustomTab)
        container.addView(tvLocation)

        AlertDialog.Builder(this)
            .setTitle("Weather")
            .setView(container)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showAssistantGearDialog() {
        val options = arrayOf("Assistant", "Gemini")
        val curIdx = options.indexOf(config.assistantIcon).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle("Assistant icon")
            .setSingleChoiceItems(options, curIdx) { dialog, idx ->
                config = config.copy(assistantIcon = options[idx])
                saveConfig()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDateGearDialog() {
        val options = arrayOf("January 31", "Jan 31", "1/31")
        val curIdx = options.indexOf(config.dateFormat).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle("Date format")
            .setSingleChoiceItems(options, curIdx) { dialog, idx ->
                config = config.copy(dateFormat = options[idx])
                saveConfig()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // =========================================================================
    // REFRESH UI & LIVE SEARCH BAR PREVIEW
    // =========================================================================
    private fun refreshAllUi() {
        refreshPlacementLabel()
        refreshColorBadges()
        refreshLogoSelection()
        refreshLivePreview()
    }

    private fun refreshPlacementLabel() {
        val map = mapOf(
            "NONE" to "None",
            "WIDGET" to "Widget",
            "DOCK_ABOVE" to "Dock (Above icons)",
            "DOCK_BELOW" to "Dock (Below icons)",
            "PERSISTENT" to "Persistent"
        )
        binding.tvPlacementValue.text = if (mode == SearchBarConfig.MODE_DESKTOP) {
            map[config.placement] ?: "Dock (Below icons)"
        } else {
            config.placement
        }
    }

    private fun refreshColorBadges() {
        binding.badgeColorPreview.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(config.surfaceColor)
            setStroke((1.5f * resources.displayMetrics.density).toInt(), Color.parseColor("#6D3B68"))
        }
        binding.tvBadgeTt.setTextColor(config.effectiveTextColor())
        binding.viewBadgeAccentDot.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(config.effectiveAccentColor())
        }
        binding.tvCustomColorHex.text = String.format("#%06X", 0xFFFFFF and currentTabColor())
    }

    private fun refreshLogoSelection() {
        val inds = arrayOf(binding.indLogo0, binding.indLogo1, binding.indLogo2, binding.indLogo3)
        inds.forEachIndexed { idx, view ->
            view.visibility = if (idx == config.logoStyle) View.VISIBLE else View.INVISIBLE
        }
    }

    private fun refreshLivePreview() {
        val density = resources.displayMetrics.density
        binding.liveSearchBarPreview.background = SearchBarConfig.buildBarBackground(config, density)
        binding.liveSearchBarPreview.elevation = if (config.hasShadow) 6f * density else 0f

        val logoRes = when (config.logoStyle) {
            SearchBarConfig.LOGO_CALENDAR_SEARCH -> R.drawable.ic_bar_logo_calendar_search
            SearchBarConfig.LOGO_MAGNIFIER -> R.drawable.ic_search
            SearchBarConfig.LOGO_GOOGLE_G -> R.drawable.ic_bar_logo_google_g
            else -> R.drawable.ic_bar_logo_nova_dots
        }
        binding.ivPreviewLeftLogo.setImageResource(logoRes)
        if (config.isMonochrome || config.logoStyle == SearchBarConfig.LOGO_MAGNIFIER) {
            binding.ivPreviewLeftLogo.setColorFilter(config.effectiveAccentColor())
        } else {
            binding.ivPreviewLeftLogo.clearColorFilter()
        }

        binding.tvPreviewHint.text = if (mode == SearchBarConfig.MODE_NOVA_NOW) "Search apps" else ""
        binding.tvPreviewHint.setTextColor(config.effectiveTextColor())

        binding.containerPreviewActions.removeAllViews()
        if (mode != SearchBarConfig.MODE_NOVA_NOW) {
            val iconSize = (22 * density).toInt()
            val padEnd = (10 * density).toInt()

            for (action in config.orderedActions) {
                if (!config.enabledActions.contains(action)) continue
                if (action == SearchBarConfig.ACTION_DATE) {
                    val tv = TextView(this).apply {
                        text = SearchBarConfig.formatDate(config.dateFormat)
                        textSize = 12f
                        setTextColor(config.textColor)
                        setPadding(padEnd, 0, 4, 0)
                    }
                    binding.containerPreviewActions.addView(tv)
                } else {
                    val res = when (action) {
                        SearchBarConfig.ACTION_WEATHER -> R.drawable.ic_bar_weather
                        SearchBarConfig.ACTION_VOICE_SEARCH -> R.drawable.ic_bar_voice_search
                        SearchBarConfig.ACTION_ASSISTANT -> if (config.assistantIcon == "Gemini") R.drawable.ic_sparkle_ai else R.drawable.ic_bar_assistant
                        SearchBarConfig.ACTION_PLAY_STORE -> R.drawable.ic_bar_play_store
                        SearchBarConfig.ACTION_GOOGLE_APP -> R.drawable.ic_bar_logo_google_g
                        SearchBarConfig.ACTION_NOVA_SEARCH -> R.drawable.ic_bar_logo_nova_dots
                        SearchBarConfig.ACTION_APP_DRAWER -> R.drawable.ic_bar_app_drawer
                        SearchBarConfig.ACTION_GOOGLE_LENS -> R.drawable.ic_bar_lens
                        else -> R.drawable.ic_search
                    }
                    val iv = ImageView(this).apply {
                        layoutParams = LinearLayout.LayoutParams(iconSize + padEnd, iconSize).apply {
                            marginStart = (4 * density).toInt()
                        }
                        setImageResource(res)
                        if (config.isMonochrome) {
                            setColorFilter(config.accentColor)
                        } else {
                            clearColorFilter()
                        }
                    }
                    binding.containerPreviewActions.addView(iv)
                }
            }
        }
    }

    // =========================================================================
    // RECYCLERVIEW ADAPTER FOR ADDITIONAL ACTIONS
    // =========================================================================
    inner class AdditionalActionsAdapter(
        val items: MutableList<String>
    ) : RecyclerView.Adapter<AdditionalActionsAdapter.ActionVH>() {

        fun replaceAll(newItems: List<String>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionVH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_search_bar_action, parent, false)
            return ActionVH(view)
        }

        override fun onBindViewHolder(holder: ActionVH, position: Int) {
            val key = items[position]
            val (title, subtitle, iconRes, hasGear) = when (key) {
                SearchBarConfig.ACTION_WEATHER -> Quad("Weather", "Powered by AccuWeather", R.drawable.ic_bar_weather, true)
                SearchBarConfig.ACTION_VOICE_SEARCH -> Quad("Voice search", null, R.drawable.ic_bar_voice_search, false)
                SearchBarConfig.ACTION_ASSISTANT -> Quad(
                    "Assistant",
                    null,
                    if (config.assistantIcon == "Gemini") R.drawable.ic_sparkle_ai else R.drawable.ic_bar_assistant,
                    true
                )
                SearchBarConfig.ACTION_PLAY_STORE -> Quad("Play Store", null, R.drawable.ic_bar_play_store, false)
                SearchBarConfig.ACTION_GOOGLE_APP -> Quad("Google App", null, R.drawable.ic_bar_logo_google_g, false)
                SearchBarConfig.ACTION_NOVA_SEARCH -> Quad("Launcher Search", null, R.drawable.ic_bar_logo_nova_dots, false)
                SearchBarConfig.ACTION_APP_DRAWER -> Quad("App drawer", null, R.drawable.ic_bar_app_drawer, false)
                SearchBarConfig.ACTION_DATE -> Quad("Date", null, R.drawable.ic_bar_date, true)
                SearchBarConfig.ACTION_GOOGLE_LENS -> Quad("Google Lens", null, R.drawable.ic_bar_lens, false)
                else -> Quad(key, null, R.drawable.ic_search, false)
            }

            holder.tvTitle.text = title
            if (subtitle != null) {
                holder.tvSubtitle.visibility = View.VISIBLE
                holder.tvSubtitle.text = subtitle
            } else {
                holder.tvSubtitle.visibility = View.GONE
            }
            holder.ivIcon.setImageResource(iconRes)

            holder.btnGear.visibility = if (hasGear) View.VISIBLE else View.GONE
            holder.btnGear.setOnClickListener {
                when (key) {
                    SearchBarConfig.ACTION_WEATHER -> showWeatherGearDialog()
                    SearchBarConfig.ACTION_ASSISTANT -> showAssistantGearDialog()
                    SearchBarConfig.ACTION_DATE -> showDateGearDialog()
                }
            }

            holder.cbEnabled.setOnCheckedChangeListener(null)
            holder.cbEnabled.isChecked = config.enabledActions.contains(key)
            holder.cbEnabled.setOnCheckedChangeListener { _, isChecked ->
                val newSet = config.enabledActions.toMutableSet()
                if (isChecked) newSet.add(key) else newSet.remove(key)
                config = config.copy(enabledActions = newSet)
                saveConfig()
            }

            holder.ivHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    itemTouchHelper.startDrag(holder)
                }
                false
            }
        }

        override fun getItemCount(): Int = items.size

        inner class ActionVH(view: View) : RecyclerView.ViewHolder(view) {
            val ivIcon: ImageView = view.findViewById(R.id.ivActionIcon)
            val tvTitle: TextView = view.findViewById(R.id.tvActionTitle)
            val tvSubtitle: TextView = view.findViewById(R.id.tvActionSubtitle)
            val btnGear: ImageButton = view.findViewById(R.id.btnActionSettings)
            val cbEnabled: CheckBox = view.findViewById(R.id.cbActionEnabled)
            val ivHandle: ImageView = view.findViewById(R.id.ivDragHandle)
        }
    }

    private data class Quad(val first: String, val second: String?, val third: Int, val fourth: Boolean)
}
