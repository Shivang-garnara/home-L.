package com.example.launcherai.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.example.launcherai.R
import com.example.launcherai.databinding.ActivitySurfaceStyleBinding
import kotlin.math.roundToInt

/**
 * Nova Launcher-parity unified Surface Style editor with 3 bottom tabs:
 * - Surface (System/Wallpaper/Custom palette 6x6 circles + Fixed colors 6x6 circles + Custom color)
 * - Text (Visibility notice + Minimum contrast (-/+) slider + 5 contrast swatches 99Δ..59Δ + Custom color)
 * - Accent (Minimum contrast (-/+) slider + Palette teardrop row with Δ + Fixed colors teardrop grid with Δ + Custom color)
 */
class SurfaceStyleActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySurfaceStyleBinding
    private lateinit var surfaceId: SurfaceStyleConfig.SurfaceId
    private var activeTab: Tab = Tab.SURFACE
    private var isColorPickerExpanded = true

    enum class Tab { SURFACE, TEXT, ACCENT }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySurfaceStyleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        surfaceId = SurfaceStyleConfig.SurfaceId.fromKey(intent.getStringExtra(EXTRA_SURFACE_KEY))
        binding.tvTitle.text = surfaceId.title

        binding.btnBack.setOnClickListener { finish() }

        binding.btnResetSurface.setOnClickListener {
            SurfaceStyleConfig.resetSurfaceStyle(this, surfaceId)
            refreshEditor()
            Toast.makeText(this, "${surfaceId.title} style reset", Toast.LENGTH_SHORT).show()
        }

        binding.rowColorPickerHeader.setOnClickListener {
            isColorPickerExpanded = !isColorPickerExpanded
            binding.layoutColorPickerBody.visibility = if (isColorPickerExpanded) View.VISIBLE else View.GONE
            binding.ivColorPickerChevron.rotation = if (isColorPickerExpanded) 180f else 0f
        }

        binding.tabSurface.setOnClickListener { selectTab(Tab.SURFACE) }
        binding.tabText.setOnClickListener { selectTab(Tab.TEXT) }
        binding.tabAccent.setOnClickListener { selectTab(Tab.ACCENT) }

        binding.btnContrastMinus.setOnClickListener { stepContrast(-5) }
        binding.btnContrastPlus.setOnClickListener { stepContrast(5) }

        binding.seekMinContrast.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                val delta = (progress + 20).coerceIn(20, 99)
                saveContrastForActiveTab(delta)
                binding.tvContrastValue.text = "${delta}Δ"
                refreshAfterContrastChange()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.rowCustomColor.setOnClickListener {
            val style = SurfaceStyleConfig.loadSurfaceStyle(this, surfaceId)
            val currentColor = when (activeTab) {
                Tab.SURFACE -> style.surfaceColor
                Tab.TEXT -> style.textColor
                Tab.ACCENT -> style.accentColor
            }
            val title = when (activeTab) {
                Tab.SURFACE -> "${surfaceId.title} • Surface"
                Tab.TEXT -> "${surfaceId.title} • Text"
                Tab.ACCENT -> "${surfaceId.title} • Accent"
            }
            NovaColorPickerBottomSheet.newInstance(
                title = title,
                initialColor = currentColor,
                defaultColor = currentColor
            ) { chosen ->
                applyChosenColor(chosen)
            }.show(supportFragmentManager, NovaColorPickerBottomSheet.TAG)
        }

        selectTab(Tab.SURFACE)
    }

    private fun stepContrast(step: Int) {
        val style = SurfaceStyleConfig.loadSurfaceStyle(this, surfaceId)
        val cur = if (activeTab == Tab.TEXT) style.textMinContrast else style.accentMinContrast
        val next = (cur + step).coerceIn(20, 99)
        saveContrastForActiveTab(next)
        binding.tvContrastValue.text = "${next}Δ"
        binding.seekMinContrast.progress = next - 20
        refreshAfterContrastChange()
    }

    private fun saveContrastForActiveTab(delta: Int) {
        when (activeTab) {
            Tab.TEXT -> SurfaceStyleConfig.saveTextMinContrast(this, surfaceId, delta)
            Tab.ACCENT -> SurfaceStyleConfig.saveAccentMinContrast(this, surfaceId, delta)
            Tab.SURFACE -> Unit
        }
    }

    private fun refreshAfterContrastChange() {
        when (activeTab) {
            Tab.TEXT -> refreshTextContrastSwatches()
            Tab.ACCENT -> refreshAccentGrids()
            Tab.SURFACE -> Unit
        }
    }

    private fun selectTab(tab: Tab) {
        activeTab = tab
        val activeColor = ContextCompat.getColor(this, R.color.nova_accent_orange)
        val inactiveColor = ContextCompat.getColor(this, R.color.nova_text_brown_secondary)

        val tabs = listOf(
            Triple(Tab.SURFACE, binding.indicatorSurface, Pair(binding.iconSurface, binding.labelSurface)),
            Triple(Tab.TEXT, binding.indicatorText, Pair(binding.iconText, binding.labelText)),
            Triple(Tab.ACCENT, binding.indicatorAccent, Pair(binding.iconAccent, binding.labelAccent))
        )
        for ((t, indicator, iconLabel) in tabs) {
            val selected = t == tab
            if (selected) {
                indicator.setBackgroundResource(R.drawable.bg_nova_bottom_nav_pill)
                iconLabel.first.setColorFilter(activeColor)
                iconLabel.second.setTextColor(activeColor)
            } else {
                indicator.background = null
                iconLabel.first.setColorFilter(inactiveColor)
                iconLabel.second.setTextColor(inactiveColor)
            }
        }
        refreshEditor()
    }

    private fun refreshEditor() {
        val style = SurfaceStyleConfig.loadSurfaceStyle(this, surfaceId)
        val paletteType = SurfaceStyleConfig.getPaletteType(this)

        val currentColor = when (activeTab) {
            Tab.SURFACE -> style.surfaceColor
            Tab.TEXT -> style.textColor
            Tab.ACCENT -> style.accentColor
        }

        when (activeTab) {
            Tab.SURFACE -> {
                binding.layoutTextNotice.visibility = View.GONE
                binding.layoutContrastSection.visibility = View.GONE
                binding.layoutPaletteAndFixed.visibility = View.VISIBLE
                binding.ivPaletteIcon.visibility = View.VISIBLE
                binding.gridActivePalette.visibility = View.VISIBLE
                binding.scrollAccentPalette.visibility = View.GONE
                binding.tvActivePaletteLabel.text = when (paletteType) {
                    SurfaceStyleConfig.PaletteType.SYSTEM -> "System palette"
                    SurfaceStyleConfig.PaletteType.WALLPAPER -> "Local Wallpaper"
                    SurfaceStyleConfig.PaletteType.CUSTOM -> "Custom palette"
                }
                populateCircleGrid(
                    grid = binding.gridActivePalette,
                    colors = SurfaceStyleConfig.getActivePaletteColors(this),
                    selectedColor = currentColor
                )
                populateCircleGrid(
                    grid = binding.gridFixedColors,
                    colors = SearchBarConfig.FIXED_PALETTE_COLORS,
                    selectedColor = currentColor
                )
            }
            Tab.TEXT -> {
                binding.layoutTextNotice.visibility = View.VISIBLE
                binding.layoutContrastSection.visibility = View.VISIBLE
                binding.layoutContrastSwatches.visibility = View.VISIBLE
                binding.layoutPaletteAndFixed.visibility = View.GONE
                binding.tvContrastValue.text = "${style.textMinContrast}Δ"
                binding.seekMinContrast.progress = (style.textMinContrast - 20).coerceIn(0, 79)
                refreshTextContrastSwatches()
            }
            Tab.ACCENT -> {
                binding.layoutTextNotice.visibility = View.GONE
                binding.layoutContrastSection.visibility = View.VISIBLE
                binding.layoutContrastSwatches.visibility = View.GONE
                binding.layoutPaletteAndFixed.visibility = View.VISIBLE
                binding.ivPaletteIcon.visibility = View.GONE
                binding.gridActivePalette.visibility = View.GONE
                binding.scrollAccentPalette.visibility = View.VISIBLE
                binding.tvActivePaletteLabel.text = "Palette"
                binding.tvContrastValue.text = "${style.accentMinContrast}Δ"
                binding.seekMinContrast.progress = (style.accentMinContrast - 20).coerceIn(0, 79)
                refreshAccentGrids()
            }
        }

        // Custom color row rounded-square swatch
        val density = resources.displayMetrics.density
        binding.viewCustomColorSwatch.background = GradientDrawable().apply {
            cornerRadius = 10f * density
            setColor(currentColor)
            setStroke((1.5f * density).toInt(), 0x33000000)
        }
    }

    /**
     * Nova's Text tab: 5 rounded-square swatches (99Δ, 89Δ, 79Δ, 69Δ, 59Δ) with Δ label below each.
     */
    private fun refreshTextContrastSwatches() {
        val style = SurfaceStyleConfig.loadSurfaceStyle(this, surfaceId)
        val container = binding.layoutContrastSwatches
        container.removeAllViews()

        val density = resources.displayMetrics.density
        val boxSize = (46 * density).toInt()
        val deltas = intArrayOf(99, 89, 79, 69, 59)
        val selectedNoAlpha = style.textColor or 0xFF000000.toInt()
        val bgOpaque = if (Color.alpha(style.surfaceColor) == 0) Color.BLACK else (style.surfaceColor or 0xFF000000.toInt())
        val isLightSurface = ColorUtils.calculateLuminance(bgOpaque) > 0.5
        val extremeColor = if (isLightSurface) Color.BLACK else Color.WHITE

        for (d in deltas) {
            val factor = ((100 - d) / 100f).coerceIn(0f, 0.55f)
            val resolved = ColorUtils.blendARGB(extremeColor, bgOpaque, factor)
            val isSelected = (resolved or 0xFF000000.toInt()) == selectedNoAlpha

            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener {
                    SurfaceStyleConfig.saveTextColor(this@SurfaceStyleActivity, surfaceId, resolved)
                    SurfaceStyleConfig.saveTextMinContrast(this@SurfaceStyleActivity, surfaceId, d)
                    refreshEditor()
                }
            }

            val box = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(boxSize, boxSize)
                background = GradientDrawable().apply {
                    cornerRadius = 12f * density
                    setColor(resolved)
                    if (isSelected) {
                        setStroke((2.5f * density).toInt(), ContextCompat.getColor(this@SurfaceStyleActivity, R.color.nova_accent_orange))
                    } else {
                        setStroke((1.5f * density).toInt(), 0x33000000)
                    }
                }
            }

            val label = TextView(this).apply {
                text = "${d}Δ"
                textSize = 12f
                setTextColor(ContextCompat.getColor(this@SurfaceStyleActivity, R.color.nova_text_brown_secondary))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = (6 * density).toInt() }
            }

            col.addView(box)
            col.addView(label)
            container.addView(col)
        }
    }

    /**
     * Nova's Accent tab: horizontal Palette row of teardrops + 6-column Fixed colors grid of teardrops,
     * each displaying its contrast Δ against the surface below the swatch.
     */
    private fun refreshAccentGrids() {
        val style = SurfaceStyleConfig.loadSurfaceStyle(this, surfaceId)
        val selectedNoAlpha = style.accentColor or 0xFF000000.toInt()
        val density = resources.displayMetrics.density
        val dropSize = (42 * density).toInt()

        // Palette row: pick 8 representative tonal shades from active palette
        val palette = SurfaceStyleConfig.getActivePaletteColors(this)
        val paletteRowColors = IntArray(8) { i -> palette[(i * 1).coerceAtMost(palette.lastIndex)] }
        val row = binding.rowAccentPalette
        row.removeAllViews()
        for (rawColor in paletteRowColors) {
            val resolved = SearchBarConfig.resolveContrastColor(rawColor, style.surfaceColor, style.accentMinContrast)
            val delta = contrastDelta(resolved, style.surfaceColor)
            val isSelected = (resolved or 0xFF000000.toInt()) == selectedNoAlpha ||
                (rawColor or 0xFF000000.toInt()) == selectedNoAlpha
            val item = buildTeardropSwatchItem(resolved, "${delta}Δ", isSelected, dropSize, density) {
                applyChosenColor(resolved)
            }.apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = (10 * density).toInt() }
            }
            row.addView(item)
        }

        // Fixed colors 6x6 teardrop grid
        val fixedGrid = binding.gridFixedColors
        fixedGrid.removeAllViews()
        for (rawColor in SearchBarConfig.FIXED_PALETTE_COLORS) {
            val resolved = SearchBarConfig.resolveContrastColor(rawColor, style.surfaceColor, style.accentMinContrast)
            val delta = contrastDelta(resolved, style.surfaceColor)
            val isSelected = (resolved or 0xFF000000.toInt()) == selectedNoAlpha
            val cell = buildTeardropSwatchItem(resolved, "${delta}Δ", isSelected, dropSize, density) {
                applyChosenColor(resolved)
            }.apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = LinearLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    bottomMargin = (10 * density).toInt()
                }
            }
            fixedGrid.addView(cell)
        }
    }

    private fun buildTeardropSwatchItem(
        color: Int,
        deltaLabel: String,
        isSelected: Boolean,
        dropSize: Int,
        density: Float,
        onClick: () -> Unit
    ): LinearLayout {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setOnClickListener { onClick() }
        }

        val frame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(dropSize, dropSize)
        }
        val r = 16f * density
        val sharp = 4f * density
        val dropView = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(dropSize, dropSize)
            background = GradientDrawable().apply {
                cornerRadii = floatArrayOf(sharp, sharp, r, r, sharp, sharp, r, r)
                setColor(color)
                if (isSelected) {
                    setStroke((2.5f * density).toInt(), ContextCompat.getColor(this@SurfaceStyleActivity, R.color.nova_accent_orange))
                } else {
                    setStroke((1f * density).toInt(), 0x26000000)
                }
            }
        }
        frame.addView(dropView)

        if (isSelected) {
            val check = ImageView(this).apply {
                layoutParams = FrameLayout.LayoutParams((18 * density).toInt(), (18 * density).toInt(), Gravity.CENTER)
                setImageResource(R.drawable.ic_check)
                setColorFilter(if (ColorUtils.calculateLuminance(color) > 0.5) Color.BLACK else Color.WHITE)
            }
            frame.addView(check)
        }

        val tv = TextView(this).apply {
            text = deltaLabel
            textSize = 11f
            setTextColor(ContextCompat.getColor(this@SurfaceStyleActivity, R.color.nova_text_brown_secondary))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (4 * density).toInt() }
        }

        col.addView(frame)
        col.addView(tv)
        return col
    }

    private fun contrastDelta(fg: Int, bg: Int): Int {
        val lumFg = ColorUtils.calculateLuminance(fg)
        val lumBg = ColorUtils.calculateLuminance(if (Color.alpha(bg) == 0) Color.BLACK else bg)
        return (kotlin.math.abs(lumFg - lumBg) * 99.0).roundToInt().coerceIn(0, 99)
    }

    private fun populateCircleGrid(grid: GridLayout, colors: IntArray, selectedColor: Int) {
        grid.removeAllViews()
        val density = resources.displayMetrics.density
        val swatchSize = (40 * density).toInt()
        val margin = (6 * density).toInt()
        val selectedNoAlpha = selectedColor or 0xFF000000.toInt()

        for (color in colors) {
            val isSelected = (color or 0xFF000000.toInt()) == selectedNoAlpha
            val cell = FrameLayout(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = swatchSize + margin * 2
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                }
            }
            val swatch = View(this).apply {
                layoutParams = FrameLayout.LayoutParams(swatchSize, swatchSize, Gravity.CENTER)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                    if (isSelected) {
                        setStroke((3 * density).toInt(), ContextCompat.getColor(this@SurfaceStyleActivity, R.color.nova_accent_orange))
                    } else {
                        setStroke((1 * density).toInt(), 0x26000000)
                    }
                }
            }
            cell.addView(swatch)

            if (isSelected) {
                val check = ImageView(this).apply {
                    layoutParams = FrameLayout.LayoutParams((18 * density).toInt(), (18 * density).toInt(), Gravity.CENTER)
                    setImageResource(R.drawable.ic_check)
                    setColorFilter(if (ColorUtils.calculateLuminance(color) > 0.5) Color.BLACK else Color.WHITE)
                }
                cell.addView(check)
            }

            cell.setOnClickListener { applyChosenColor(color) }
            grid.addView(cell)
        }
    }

    private fun applyChosenColor(color: Int) {
        when (activeTab) {
            Tab.SURFACE -> SurfaceStyleConfig.saveSurfaceColor(this, surfaceId, color)
            Tab.TEXT -> SurfaceStyleConfig.saveTextColor(this, surfaceId, color)
            Tab.ACCENT -> SurfaceStyleConfig.saveAccentColor(this, surfaceId, color)
        }
        refreshEditor()
    }

    companion object {
        private const val EXTRA_SURFACE_KEY = "extra_surface_key"

        fun createIntent(context: Context, surfaceId: SurfaceStyleConfig.SurfaceId): Intent {
            return Intent(context, SurfaceStyleActivity::class.java).apply {
                putExtra(EXTRA_SURFACE_KEY, surfaceId.key)
            }
        }
    }
}
