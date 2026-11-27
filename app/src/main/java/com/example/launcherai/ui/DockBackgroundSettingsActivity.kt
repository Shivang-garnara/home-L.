package com.example.launcherai.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.launcherai.R
import com.example.launcherai.databinding.ActivityDockBackgroundSettingsBinding

/**
 * Nova Launcher-parity Dock Background Settings screen (SettingsDockBackground).
 * Supports enable/disable, color, transparency, horizontal content padding,
 * draw behind navigation bar, and per-corner Crop/Round/Squircle shape & radius.
 */
class DockBackgroundSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDockBackgroundSettingsBinding
    private lateinit var config: DockBackgroundConfig
    private lateinit var previewDrawable: DockBackgroundDrawable

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_NovaLight)
        super.onCreate(savedInstanceState)
        SystemBarHelper.applySystemBars(this, isLightTheme = true, transparentBars = false)
        binding = ActivityDockBackgroundSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemBarHelper.applyEdgeToEdgeInsets(
            insetsRootView = binding.root,
            topInsetView = binding.headerContainer
        )

        config = DockBackgroundConfig.load(this)
        previewDrawable = DockBackgroundDrawable(config, resources.displayMetrics.density)
        binding.dockBgPreviewBar.background = previewDrawable

        setupViews()
        setupListeners()
        updatePreview()
    }

    private fun setupViews() {
        binding.swDockBgEnabled.isChecked = config.enabled
        binding.layoutDockBgOptions.visibility = if (config.enabled) View.VISIBLE else View.GONE

        binding.tvDockBgColorHex.text = String.format("#%06X", 0xFFFFFF and config.color)
        binding.viewDockBgColorSwatch.backgroundTintList = ColorStateList.valueOf(config.color)

        binding.sbTransparency.progress = config.transparencyPercent
        binding.tvTransparencyValue.text = "${config.transparencyPercent}%"

        binding.swContentPadding.isChecked = config.horizontalPaddingDp > 0
        binding.swDrawBehindNavbarBg.isChecked = config.drawBehindNavbar
        binding.layoutBottomCorners.visibility = if (config.drawBehindNavbar) View.GONE else View.VISIBLE

        binding.sbTopLeftRadius.progress = config.topLeft.radiusPercent
        binding.sbTopRightRadius.progress = config.topRight.radiusPercent
        binding.sbBottomLeftRadius.progress = config.bottomLeft.radiusPercent
        binding.sbBottomRightRadius.progress = config.bottomRight.radiusPercent
        updateCornerLabels()
    }

    private fun updateCornerLabels() {
        binding.tvTopLeftShape.text = "${config.topLeft.shape.label} · ${config.topLeft.radiusPercent}%"
        binding.tvTopRightShape.text = "${config.topRight.shape.label} · ${config.topRight.radiusPercent}%"
        binding.tvBottomLeftShape.text = "${config.bottomLeft.shape.label} · ${config.bottomLeft.radiusPercent}%"
        binding.tvBottomRightShape.text = "${config.bottomRight.shape.label} · ${config.bottomRight.radiusPercent}%"
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.swDockBgEnabled.setOnCheckedChangeListener { _, isChecked ->
            config = config.copy(enabled = isChecked)
            binding.layoutDockBgOptions.visibility = if (isChecked) View.VISIBLE else View.GONE
            saveAndRefreshPreview()
        }
        binding.rowEnableDockBg.setOnClickListener { binding.swDockBgEnabled.toggle() }

        binding.rowDockBgColor.setOnClickListener { showColorPickerDialog() }

        binding.sbTransparency.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                config = config.copy(transparencyPercent = progress)
                binding.tvTransparencyValue.text = "$progress%"
                updatePreview()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                config.save(this@DockBackgroundSettingsActivity)
            }
        })

        binding.swContentPadding.setOnCheckedChangeListener { _, isChecked ->
            config = config.copy(horizontalPaddingDp = if (isChecked) 8 else 0)
            saveAndRefreshPreview()
        }
        binding.rowContentPadding.setOnClickListener { binding.swContentPadding.toggle() }

        binding.swDrawBehindNavbarBg.setOnCheckedChangeListener { _, isChecked ->
            config = config.copy(drawBehindNavbar = isChecked)
            binding.layoutBottomCorners.visibility = if (isChecked) View.GONE else View.VISIBLE
            saveAndRefreshPreview()
        }
        binding.rowDrawBehindNavbarBg.setOnClickListener { binding.swDrawBehindNavbarBg.toggle() }

        bindCornerControls(
            row = binding.rowTopLeftShape,
            seekBar = binding.sbTopLeftRadius,
            title = "Top Left Shape",
            getter = { config.topLeft },
            setter = { spec -> config = config.copy(topLeft = spec) }
        )
        bindCornerControls(
            row = binding.rowTopRightShape,
            seekBar = binding.sbTopRightRadius,
            title = "Top Right Shape",
            getter = { config.topRight },
            setter = { spec -> config = config.copy(topRight = spec) }
        )
        bindCornerControls(
            row = binding.rowBottomLeftShape,
            seekBar = binding.sbBottomLeftRadius,
            title = "Bottom Left Shape",
            getter = { config.bottomLeft },
            setter = { spec -> config = config.copy(bottomLeft = spec) }
        )
        bindCornerControls(
            row = binding.rowBottomRightShape,
            seekBar = binding.sbBottomRightRadius,
            title = "Bottom Right Shape",
            getter = { config.bottomRight },
            setter = { spec -> config = config.copy(bottomRight = spec) }
        )
    }

    private fun bindCornerControls(
        row: View,
        seekBar: SeekBar,
        title: String,
        getter: () -> DockCornerSpec,
        setter: (DockCornerSpec) -> Unit
    ) {
        row.setOnClickListener {
            val shapes = DockCornerShape.entries.toTypedArray()
            val labels = shapes.map { it.label }.toTypedArray()
            val currentIndex = shapes.indexOf(getter().shape).coerceAtLeast(0)
            AlertDialog.Builder(this)
                .setTitle(title)
                .setSingleChoiceItems(labels, currentIndex) { dialog, which ->
                    setter(getter().copy(shape = shapes[which]))
                    updateCornerLabels()
                    saveAndRefreshPreview()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                setter(getter().copy(radiusPercent = progress))
                updateCornerLabels()
                updatePreview()
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {
                config.save(this@DockBackgroundSettingsActivity)
            }
        })
    }

    private fun showColorPickerDialog() {
        NovaColorPickerBottomSheet.newInstance(
            title = "Dock background color",
            initialColor = config.color,
            defaultColor = Color.parseColor("#1F1F2B")
        ) { chosen ->
            config = config.copy(color = chosen)
            binding.tvDockBgColorHex.text = String.format("#%06X", 0xFFFFFF and chosen)
            binding.viewDockBgColorSwatch.backgroundTintList = ColorStateList.valueOf(chosen)
            saveAndRefreshPreview()
        }.show(supportFragmentManager, NovaColorPickerBottomSheet.TAG)
    }

    private fun saveAndRefreshPreview() {
        config.save(this)
        updatePreview()
    }

    private fun updatePreview() {
        previewDrawable.config = config
        previewDrawable.invalidateSelf()
        binding.dockBgPreviewBar.invalidate()
    }
}
