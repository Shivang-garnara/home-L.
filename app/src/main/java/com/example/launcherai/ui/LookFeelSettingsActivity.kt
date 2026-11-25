package com.example.launcherai.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.launcherai.R
import com.example.launcherai.databinding.ActivityLookFeelSettingsBinding
import com.example.launcherai.icons.IconPackManager
import com.example.launcherai.icons.IconShapeMasker

/**
 * Dedicated Look & feel Settings Activity matching Nova Launcher 8.9.9 Look & Feel.
 * Manages icon preferences (themes, shapes, shadow, size), screen orientation, app launch animation,
 * popup menu items, drop targets, undo bar, and system status/navigation bar appearance.
 */
class LookFeelSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLookFeelSettingsBinding
    private lateinit var prefs: SharedPreferences

    companion object {
        const val PREFS_NAME = "workspace_prefs"

        const val KEY_SELECTED_ICON_PACK = "key_selected_icon_pack"
        const val KEY_ICON_SHAPE = "key_icon_shape"
        const val KEY_ICON_SIZE_PERCENT = "key_icon_size_percent"
        const val KEY_RESHAPE_LEGACY_ICONS = "key_reshape_legacy_icons"
        const val KEY_ICON_SHADOW = "key_icon_shadow"
        const val KEY_ADAPTIVE_ICON_ANIMATIONS = "key_adaptive_icon_animations"
        const val KEY_ANDROID_71_ICONS = "key_android_71_icons"

        const val KEY_SCREEN_ROTATION_ENABLED = "key_screen_rotation_enabled"
        const val KEY_SCREEN_ORIENTATION_MODE = "key_screen_orientation_mode"
        const val KEY_APP_ANIMATION = "key_app_animation"
        const val KEY_DISABLE_ANIMATIONS = "key_disable_animations"

        const val KEY_POPUP_ITEMS = "key_popup_items"
        const val KEY_DROP_TARGETS = "key_drop_targets"
        const val KEY_SHOW_UNDO_BAR = "key_show_undo_bar"

        const val KEY_SHOW_NOTIFICATION_BAR = "key_show_notification_bar"
        const val KEY_TRANSPARENT_NOTIFICATION_BAR = "key_transparent_notification_bar"
        const val KEY_TRANSPARENT_NAVIGATION_BAR = "key_transparent_navigation_bar"
        const val KEY_DARK_STATUS_ICONS = "key_dark_status_icons"

        const val DEFAULT_SHAPE = "Round"
        const val DEFAULT_ICON_SIZE_PERCENT = 125
        const val MIN_ICON_SIZE = 80
        const val MAX_ICON_SIZE = 150

        val DEFAULT_POPUP_ITEMS = setOf("App info", "Edit", "Remove", "Uninstall")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLookFeelSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        setupHeader()
        setupIconPreferences()
        setupScreenAndTransitions()
        setupPopupMenuAndDesktop()
        setupNotificationBar()

        updateSubtitles()
        updateLivePreview()
    }

    override fun onResume() {
        super.onResume()
        updateSubtitles()
        updateLivePreview()
    }

    private fun setupHeader() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    // =========================================================================
    // SECTION 1: ICON PREFERENCES
    // =========================================================================
    private fun setupIconPreferences() {
        // Icon Theme Picker
        binding.rowIconTheme.setOnClickListener {
            showIconThemeDialog()
        }

        // Icon Shape Picker
        binding.rowIconShape.setOnClickListener {
            showIconShapeDialog()
        }

        // Horizontal Shape Strip
        setupShapeSelectorStrip()

        // Reshape Legacy Icons Toggle
        binding.swReshapeLegacy.isChecked = prefs.getBoolean(KEY_RESHAPE_LEGACY_ICONS, true)
        binding.swReshapeLegacy.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_RESHAPE_LEGACY_ICONS, isChecked).apply()
            updateLivePreview()
        }

        // Icon Shadow Toggle
        binding.swIconShadow.isChecked = prefs.getBoolean(KEY_ICON_SHADOW, true)
        binding.swIconShadow.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_ICON_SHADOW, isChecked).apply()
            updateLivePreview()
        }

        // Adaptive Icon Animations (Wiggle during drag and drop)
        binding.swAdaptiveAnimations.isChecked = prefs.getBoolean(KEY_ADAPTIVE_ICON_ANIMATIONS, true)
        binding.swAdaptiveAnimations.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_ADAPTIVE_ICON_ANIMATIONS, isChecked).apply()
        }

        // Android 7.1 Legacy Icons Toggle
        binding.swAndroid71Icons.isChecked = prefs.getBoolean(KEY_ANDROID_71_ICONS, false)
        binding.swAndroid71Icons.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_ANDROID_71_ICONS, isChecked).apply()
            Toast.makeText(this, "Icon style updated", Toast.LENGTH_SHORT).show()
        }

        // Icon Size SeekBar (80% to 150%)
        val currentSize = prefs.getInt(KEY_ICON_SIZE_PERCENT, DEFAULT_ICON_SIZE_PERCENT)
        binding.sbIconSize.max = MAX_ICON_SIZE - MIN_ICON_SIZE
        binding.sbIconSize.progress = (currentSize - MIN_ICON_SIZE).coerceIn(0, MAX_ICON_SIZE - MIN_ICON_SIZE)
        binding.tvIconSizeValue.text = "$currentSize%"

        binding.sbIconSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val size = MIN_ICON_SIZE + progress
                binding.tvIconSizeValue.text = "$size%"
                if (fromUser) {
                    prefs.edit().putInt(KEY_ICON_SIZE_PERCENT, size).apply()
                    updateLivePreview()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.btnIconSizeDec.setOnClickListener {
            val cur = prefs.getInt(KEY_ICON_SIZE_PERCENT, DEFAULT_ICON_SIZE_PERCENT)
            if (cur > MIN_ICON_SIZE) {
                val next = (cur - 5).coerceAtLeast(MIN_ICON_SIZE)
                prefs.edit().putInt(KEY_ICON_SIZE_PERCENT, next).apply()
                binding.sbIconSize.progress = next - MIN_ICON_SIZE
                binding.tvIconSizeValue.text = "$next%"
                updateLivePreview()
            }
        }

        binding.btnIconSizeInc.setOnClickListener {
            val cur = prefs.getInt(KEY_ICON_SIZE_PERCENT, DEFAULT_ICON_SIZE_PERCENT)
            if (cur < MAX_ICON_SIZE) {
                val next = (cur + 5).coerceAtMost(MAX_ICON_SIZE)
                prefs.edit().putInt(KEY_ICON_SIZE_PERCENT, next).apply()
                binding.sbIconSize.progress = next - MIN_ICON_SIZE
                binding.tvIconSizeValue.text = "$next%"
                updateLivePreview()
            }
        }
    }

    private fun showIconThemeDialog() {
        val iconPacks = IconPackManager.getInstalledIconPacks(this)
        val selectedPkg = prefs.getString(KEY_SELECTED_ICON_PACK, "") ?: ""

        val labels = iconPacks.map { it.label }.toTypedArray()
        var checkedIndex = iconPacks.indexOfFirst { it.packageName == selectedPkg }
        if (checkedIndex < 0) checkedIndex = 0

        AlertDialog.Builder(this)
            .setTitle("Icon theme")
            .setSingleChoiceItems(labels, checkedIndex) { dialog, which ->
                val chosen = iconPacks[which]
                prefs.edit().putString(KEY_SELECTED_ICON_PACK, chosen.packageName).apply()
                updateSubtitles()
                updateLivePreview()
                Toast.makeText(this, "Icon theme: ${chosen.label}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupShapeSelectorStrip() {
        val density = resources.displayMetrics.density
        val thumbSizePx = (38 * density).toInt()
        val thumbColor = ContextCompat.getColor(this, R.color.nova_orange)

        // Generate thumbnails for shape items
        binding.ivShapeThumbRound.setImageDrawable(IconShapeMasker.createShapeThumbnail(this, "Round", thumbSizePx, thumbColor))
        binding.ivShapeThumbSquircle.setImageDrawable(IconShapeMasker.createShapeThumbnail(this, "Squircle", thumbSizePx, thumbColor))
        binding.ivShapeThumbRoundedSquare.setImageDrawable(IconShapeMasker.createShapeThumbnail(this, "Rounded square", thumbSizePx, thumbColor))
        binding.ivShapeThumbFlower.setImageDrawable(IconShapeMasker.createShapeThumbnail(this, "Flower", thumbSizePx, thumbColor))
        binding.ivShapeThumbTeardrop.setImageDrawable(IconShapeMasker.createShapeThumbnail(this, "Teardrop", thumbSizePx, thumbColor))

        fun selectShape(shapeName: String) {
            prefs.edit().putString(KEY_ICON_SHAPE, shapeName).apply()
            updateSubtitles()
            updateShapeStripSelection(shapeName)
            updateLivePreview()
        }

        binding.btnShapeRound.setOnClickListener { selectShape("Round") }
        binding.btnShapeSquircle.setOnClickListener { selectShape("Squircle") }
        binding.btnShapeRoundedSquare.setOnClickListener { selectShape("Rounded square") }
        binding.btnShapeFlower.setOnClickListener { selectShape("Flower") }
        binding.btnShapeTeardrop.setOnClickListener { selectShape("Teardrop") }
        binding.btnShapeMore.setOnClickListener { showMoreShapesDialog() }

        val activeShape = prefs.getString(KEY_ICON_SHAPE, DEFAULT_SHAPE) ?: DEFAULT_SHAPE
        updateShapeStripSelection(activeShape)
    }

    private fun updateShapeStripSelection(selectedShape: String) {
        val s = selectedShape.lowercase()
        binding.btnShapeRound.isSelected = s == "round"
        binding.btnShapeSquircle.isSelected = s == "squircle"
        binding.btnShapeRoundedSquare.isSelected = s == "rounded square"
        binding.btnShapeFlower.isSelected = s == "flower"
        binding.btnShapeTeardrop.isSelected = s == "teardrop"
        binding.btnShapeMore.isSelected = s !in listOf("round", "squircle", "rounded square", "flower", "teardrop")
    }

    private fun showMoreShapesDialog() {
        val moreDialog = MoreShapesDialogFragment.newInstance()
        moreDialog.onShapeSelectedListener = { chosen ->
            prefs.edit().putString(KEY_ICON_SHAPE, chosen).apply()
            updateSubtitles()
            updateShapeStripSelection(chosen)
            updateLivePreview()
        }
        moreDialog.show(supportFragmentManager, MoreShapesDialogFragment.TAG)
    }

    private fun showIconShapeDialog() {
        val shapes = arrayOf("Round", "Squircle", "Rounded square", "Flower", "Teardrop", "More shapes...")
        val currentShape = prefs.getString(KEY_ICON_SHAPE, DEFAULT_SHAPE) ?: DEFAULT_SHAPE
        var checkedIdx = shapes.indexOfFirst { it.equals(currentShape, ignoreCase = true) }
        if (checkedIdx < 0) checkedIdx = 0

        AlertDialog.Builder(this)
            .setTitle("Icon shape")
            .setSingleChoiceItems(shapes, checkedIdx) { dialog, which ->
                if (which == shapes.size - 1) {
                    dialog.dismiss()
                    showMoreShapesDialog()
                } else {
                    val chosen = shapes[which]
                    prefs.edit().putString(KEY_ICON_SHAPE, chosen).apply()
                    updateSubtitles()
                    updateShapeStripSelection(chosen)
                    updateLivePreview()
                    dialog.dismiss()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // =========================================================================
    // SECTION 2: SCREEN & TRANSITIONS
    // =========================================================================
    private fun setupScreenAndTransitions() {
        // Screen Rotation Switch
        val isRotationEnabled = prefs.getBoolean(KEY_SCREEN_ROTATION_ENABLED, false)
        binding.swScreenRotation.isChecked = isRotationEnabled
        binding.swScreenRotation.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_SCREEN_ROTATION_ENABLED, isChecked).apply()
            if (isChecked) {
                val currentMode = prefs.getString(KEY_SCREEN_ORIENTATION_MODE, "Auto rotate (Sensor)")
                if (currentMode.equals("Force portrait", ignoreCase = true)) {
                    prefs.edit().putString(KEY_SCREEN_ORIENTATION_MODE, "Auto rotate (Sensor)").apply()
                }
            }
            updateSubtitles()
        }

        binding.rowScreenRotation.setOnClickListener {
            val modes = arrayOf("Auto rotate (Sensor)", "Force portrait", "Force landscape", "No sensor")
            val currentMode = prefs.getString(KEY_SCREEN_ORIENTATION_MODE, "Auto rotate (Sensor)") ?: "Auto rotate (Sensor)"
            val checkedIdx = modes.indexOfFirst { it.equals(currentMode, ignoreCase = true) }.coerceAtLeast(0)

            AlertDialog.Builder(this)
                .setTitle("Screen orientation")
                .setSingleChoiceItems(modes, checkedIdx) { dialog, which ->
                    val chosen = modes[which]
                    prefs.edit().putString(KEY_SCREEN_ORIENTATION_MODE, chosen).apply()
                    if (chosen != "Force portrait") {
                        prefs.edit().putBoolean(KEY_SCREEN_ROTATION_ENABLED, true).apply()
                        binding.swScreenRotation.isChecked = true
                    } else {
                        prefs.edit().putBoolean(KEY_SCREEN_ROTATION_ENABLED, false).apply()
                        binding.swScreenRotation.isChecked = false
                    }
                    updateSubtitles()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // App Animation Selector
        binding.rowAppAnimation.setOnClickListener {
            val animations = arrayOf("System default", "Blink", "Slide up", "Zoom")
            val currentAnim = prefs.getString(KEY_APP_ANIMATION, "System default") ?: "System default"
            val checkedIdx = animations.indexOfFirst { it.equals(currentAnim, ignoreCase = true) }.coerceAtLeast(0)

            AlertDialog.Builder(this)
                .setTitle("App animation")
                .setSingleChoiceItems(animations, checkedIdx) { dialog, which ->
                    prefs.edit().putString(KEY_APP_ANIMATION, animations[which]).apply()
                    updateSubtitles()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Disable Animations Toggle
        binding.swDisableAnimations.isChecked = prefs.getBoolean(KEY_DISABLE_ANIMATIONS, false)
        binding.swDisableAnimations.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_DISABLE_ANIMATIONS, isChecked).apply()
            updateSubtitles()
        }
    }

    // =========================================================================
    // SECTION 3: POPUP MENU & DESKTOP BEHAVIOR
    // =========================================================================
    private fun setupPopupMenuAndDesktop() {
        // Dedicated Popup Menu Settings Sub-Screen
        binding.rowPopupMenu.setOnClickListener {
            val intent = Intent(this, PopupMenuSettingsActivity::class.java)
            startActivity(intent)
        }

        // Drop Targets Toggle
        binding.swDropTargets.isChecked = prefs.getBoolean(KEY_DROP_TARGETS, true)
        binding.swDropTargets.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_DROP_TARGETS, isChecked).apply()
        }

        // Show Undo Bar Toggle
        binding.swUndoBar.isChecked = prefs.getBoolean(KEY_SHOW_UNDO_BAR, true)
        binding.swUndoBar.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_SHOW_UNDO_BAR, isChecked).apply()
        }
    }

    // =========================================================================
    // SECTION 4: NOTIFICATION BAR & SYSTEM BARS
    // =========================================================================
    private fun setupNotificationBar() {
        // Show Notification Bar
        binding.swShowNotificationBar.isChecked = prefs.getBoolean(KEY_SHOW_NOTIFICATION_BAR, true)
        binding.swShowNotificationBar.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_SHOW_NOTIFICATION_BAR, isChecked).apply()
        }

        // Transparent Notification Bar
        binding.swTransparentNotificationBar.isChecked = prefs.getBoolean(KEY_TRANSPARENT_NOTIFICATION_BAR, true)
        binding.swTransparentNotificationBar.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_TRANSPARENT_NOTIFICATION_BAR, isChecked).apply()
        }

        // Transparent Navigation Bar
        binding.swTransparentNavigationBar.isChecked = prefs.getBoolean(KEY_TRANSPARENT_NAVIGATION_BAR, true)
        binding.swTransparentNavigationBar.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_TRANSPARENT_NAVIGATION_BAR, isChecked).apply()
        }

        // Dark Icons Selector
        binding.rowDarkIcons.setOnClickListener {
            val options = arrayOf("Auto", "Light icons", "Dark icons")
            val current = prefs.getString(KEY_DARK_STATUS_ICONS, "Auto") ?: "Auto"
            val checkedIdx = options.indexOfFirst { it.equals(current, ignoreCase = true) }.coerceAtLeast(0)

            AlertDialog.Builder(this)
                .setTitle("Dark icons")
                .setSingleChoiceItems(options, checkedIdx) { dialog, which ->
                    prefs.edit().putString(KEY_DARK_STATUS_ICONS, options[which]).apply()
                    updateSubtitles()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    // =========================================================================
    // UI REFRESH & LIVE PREVIEW
    // =========================================================================
    private fun updateSubtitles() {
        // Icon Theme Subtitle
        val selectedPkg = prefs.getString(KEY_SELECTED_ICON_PACK, "") ?: ""
        val iconPacks = IconPackManager.getInstalledIconPacks(this)
        val themeName = iconPacks.find { it.packageName == selectedPkg }?.label ?: "Default System Icons"
        binding.tvIconThemeSubtitle.text = themeName

        // Icon Shape Subtitle
        val currentShape = prefs.getString(KEY_ICON_SHAPE, DEFAULT_SHAPE) ?: DEFAULT_SHAPE
        binding.tvIconShapeSubtitle.text = currentShape

        // Screen Rotation Subtitle
        val isRotation = prefs.getBoolean(KEY_SCREEN_ROTATION_ENABLED, false)
        val orientationMode = prefs.getString(KEY_SCREEN_ORIENTATION_MODE, "Auto rotate (Sensor)") ?: "Auto rotate (Sensor)"
        binding.tvScreenRotationSubtitle.text = if (isRotation) "Enabled ($orientationMode)" else "Locked in portrait"

        // App Animation Subtitle
        val anim = prefs.getString(KEY_APP_ANIMATION, "System default") ?: "System default"
        val isAnimDisabled = prefs.getBoolean(KEY_DISABLE_ANIMATIONS, false)
        binding.rowAppAnimation.alpha = if (isAnimDisabled) 0.5f else 1.0f
        binding.rowAppAnimation.isEnabled = !isAnimDisabled
        binding.tvAppAnimationSubtitle.text = if (isAnimDisabled) "$anim (Animations disabled)" else anim

        // Popup Menu Subtitle
        val cornerRadius = prefs.getInt(PopupMenuSettingsActivity.KEY_POPUP_CORNER_RADIUS, PopupMenuSettingsActivity.DEFAULT_CORNER_RADIUS_DP)
        val hasCardBg = prefs.getBoolean(PopupMenuSettingsActivity.KEY_POPUP_CARD_BG, true)
        binding.tvPopupMenuSubtitle.text = "${cornerRadius}dp corner radius" + (if (hasCardBg) ", Card background" else "")

        // Dark Icons Subtitle
        val darkIcons = prefs.getString(KEY_DARK_STATUS_ICONS, "Auto") ?: "Auto"
        binding.tvDarkIconsSubtitle.text = darkIcons
    }

    private fun updateLivePreview() {
        val currentShape = prefs.getString(KEY_ICON_SHAPE, DEFAULT_SHAPE) ?: DEFAULT_SHAPE
        val currentSize = prefs.getInt(KEY_ICON_SIZE_PERCENT, DEFAULT_ICON_SIZE_PERCENT)
        val selectedPack = prefs.getString(KEY_SELECTED_ICON_PACK, "") ?: ""
        val hasShadow = prefs.getBoolean(KEY_ICON_SHADOW, true)
        val reshapeLegacy = prefs.getBoolean(KEY_RESHAPE_LEGACY_ICONS, true)

        updateShapeStripSelection(currentShape)

        val density = resources.displayMetrics.density
        val targetSizePx = (48 * density * (currentSize / 100f)).toInt().coerceIn((36 * density).toInt(), (64 * density).toInt())

        // Sample drawables for preview
        val baseDrawables = arrayOf(
            ContextCompat.getDrawable(this, R.mipmap.ic_launcher) ?: packageManager.defaultActivityIcon,
            ContextCompat.getDrawable(this, android.R.drawable.ic_menu_agenda) ?: packageManager.defaultActivityIcon,
            ContextCompat.getDrawable(this, android.R.drawable.ic_menu_recent_history) ?: packageManager.defaultActivityIcon,
            ContextCompat.getDrawable(this, android.R.drawable.ic_menu_manage) ?: packageManager.defaultActivityIcon
        )

        val imageViews = arrayOf(
            binding.ivPreview1,
            binding.ivPreview2,
            binding.ivPreview3,
            binding.ivPreview4
        )

        val samplePkgs = arrayOf(
            packageName,
            "com.android.calculator2",
            "com.android.deskclock",
            "com.android.settings"
        )

        for (i in 0 until 4) {
            val iv = imageViews[i]
            var rawDrawable = baseDrawables[i]

            // If an icon pack is selected, lookup icon
            if (selectedPack.isNotBlank()) {
                val packDrawable = IconPackManager.getIconDrawable(this, selectedPack, samplePkgs[i], rawDrawable)
                if (packDrawable != null) {
                    rawDrawable = packDrawable
                }
            }

            // Apply shape mask, scale & legacy reshape
            val masked = IconShapeMasker.createMaskedIcon(
                context = this,
                originalDrawable = rawDrawable,
                shapeName = currentShape,
                scalePercent = currentSize,
                targetSizePx = targetSizePx,
                reshapeLegacy = reshapeLegacy
            )

            iv.layoutParams.width = targetSizePx
            iv.layoutParams.height = targetSizePx
            iv.setImageDrawable(masked ?: rawDrawable)

            // Elevation shadow
            if (hasShadow) {
                iv.elevation = 6f * density
            } else {
                iv.elevation = 0f
            }
        }
    }
}
