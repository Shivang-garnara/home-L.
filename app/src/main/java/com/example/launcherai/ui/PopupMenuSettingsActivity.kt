package com.example.launcherai.ui

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.launcherai.R
import com.example.launcherai.databinding.ActivityPopupMenuSettingsBinding

/**
 * Dedicated sub-screen for configuring the App Icon Popup Menu.
 * 100% parity with Nova Launcher 8.9.9's SettingsPopupMenu:
 * - Live preview bubble with dynamic corner radius and background styling
 * - Styling controls: Card background, Visually group shortcuts, Corner radius slider (0-28dp)
 * - Complete Action Matrix with independent Home (Desktop) vs Drawer checkboxes
 */
class PopupMenuSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPopupMenuSettingsBinding
    private lateinit var prefs: SharedPreferences

    companion object {
        const val PREFS_NAME = "look_feel_prefs"
        const val KEY_POPUP_CARD_BG = "key_popup_card_bg"
        const val KEY_POPUP_GROUP_SHORTCUTS = "key_popup_group_shortcuts"
        const val KEY_POPUP_CORNER_RADIUS = "key_popup_corner_radius"
        const val KEY_POPUP_ACTIONS_HOME = "key_popup_actions_home"
        const val KEY_POPUP_ACTIONS_DRAWER = "key_popup_actions_drawer"

        const val DEFAULT_CORNER_RADIUS_DP = 8

        val DEFAULT_ACTIONS_HOME = setOf(
            "Edit",
            "Remove",
            "App info",
            "Share",
            "Widgets"
        )

        val DEFAULT_ACTIONS_DRAWER = setOf(
            "Edit",
            "App info",
            "Share",
            "Widgets",
            "Uninstall",
            "Hide"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPopupMenuSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        setupHeader()
        setupStylingControls()
        setupActionMatrix()
        updateLivePreview()
    }

    private fun setupHeader() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    // =========================================================================
    // SECTION 1: STYLING CONTROLS
    // =========================================================================
    private fun setupStylingControls() {
        // Card Background Toggle
        binding.swCardBg.isChecked = prefs.getBoolean(KEY_POPUP_CARD_BG, true)
        binding.swCardBg.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_POPUP_CARD_BG, isChecked).apply()
            updateLivePreview()
        }
        binding.rowCardBg.setOnClickListener {
            binding.swCardBg.toggle()
        }

        // Visually Group Shortcuts Toggle
        binding.swGroupShortcuts.isChecked = prefs.getBoolean(KEY_POPUP_GROUP_SHORTCUTS, false)
        binding.swGroupShortcuts.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_POPUP_GROUP_SHORTCUTS, isChecked).apply()
            updateLivePreview()
        }
        binding.rowGroupShortcuts.setOnClickListener {
            binding.swGroupShortcuts.toggle()
        }

        // Corner Radius Slider (0 to 28dp, default 8dp)
        val curRadius = prefs.getInt(KEY_POPUP_CORNER_RADIUS, DEFAULT_CORNER_RADIUS_DP)
        binding.sbCornerRadius.progress = curRadius
        binding.tvCornerRadiusValue.text = "${curRadius}dp"

        binding.sbCornerRadius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.tvCornerRadiusValue.text = "${progress}dp"
                if (fromUser) {
                    prefs.edit().putInt(KEY_POPUP_CORNER_RADIUS, progress).apply()
                    updateLivePreview()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.btnRadiusDec.setOnClickListener {
            val cur = prefs.getInt(KEY_POPUP_CORNER_RADIUS, DEFAULT_CORNER_RADIUS_DP)
            if (cur > 0) {
                val next = cur - 1
                prefs.edit().putInt(KEY_POPUP_CORNER_RADIUS, next).apply()
                binding.sbCornerRadius.progress = next
                binding.tvCornerRadiusValue.text = "${next}dp"
                updateLivePreview()
            }
        }

        binding.btnRadiusInc.setOnClickListener {
            val cur = prefs.getInt(KEY_POPUP_CORNER_RADIUS, DEFAULT_CORNER_RADIUS_DP)
            if (cur < 28) {
                val next = cur + 1
                prefs.edit().putInt(KEY_POPUP_CORNER_RADIUS, next).apply()
                binding.sbCornerRadius.progress = next
                binding.tvCornerRadiusValue.text = "${next}dp"
                updateLivePreview()
            }
        }
    }

    // =========================================================================
    // SECTION 2: ACTION MATRIX
    // =========================================================================
    private fun setupActionMatrix() {
        val homeActions = prefs.getStringSet(KEY_POPUP_ACTIONS_HOME, DEFAULT_ACTIONS_HOME)?.toMutableSet()
            ?: DEFAULT_ACTIONS_HOME.toMutableSet()
        val drawerActions = prefs.getStringSet(KEY_POPUP_ACTIONS_DRAWER, DEFAULT_ACTIONS_DRAWER)?.toMutableSet()
            ?: DEFAULT_ACTIONS_DRAWER.toMutableSet()

        fun bindAction(
            actionName: String,
            cbHome: CheckBox,
            cbDrawer: CheckBox
        ) {
            cbHome.isChecked = actionName in homeActions
            cbDrawer.isChecked = actionName in drawerActions

            cbHome.setOnCheckedChangeListener { _, isChecked ->
                val current = prefs.getStringSet(KEY_POPUP_ACTIONS_HOME, DEFAULT_ACTIONS_HOME)?.toMutableSet()
                    ?: DEFAULT_ACTIONS_HOME.toMutableSet()
                if (isChecked) current.add(actionName) else current.remove(actionName)
                prefs.edit().putStringSet(KEY_POPUP_ACTIONS_HOME, current).apply()
                updateLivePreview()
            }

            cbDrawer.setOnCheckedChangeListener { _, isChecked ->
                val current = prefs.getStringSet(KEY_POPUP_ACTIONS_DRAWER, DEFAULT_ACTIONS_DRAWER)?.toMutableSet()
                    ?: DEFAULT_ACTIONS_DRAWER.toMutableSet()
                if (isChecked) current.add(actionName) else current.remove(actionName)
                prefs.edit().putStringSet(KEY_POPUP_ACTIONS_DRAWER, current).apply()
            }
        }

        bindAction("Edit", binding.cbEditHome, binding.cbEditDrawer)
        bindAction("Remove", binding.cbRemoveHome, binding.cbRemoveDrawer)
        bindAction("App info", binding.cbAppInfoHome, binding.cbAppInfoDrawer)
        bindAction("Share", binding.cbShareHome, binding.cbShareDrawer)
        bindAction("Widgets", binding.cbWidgetsHome, binding.cbWidgetsDrawer)
        bindAction("Uninstall", binding.cbUninstallHome, binding.cbUninstallDrawer)
        bindAction("Play Store", binding.cbPlayStoreHome, binding.cbPlayStoreDrawer)
        bindAction("Force stop", binding.cbForceStopHome, binding.cbForceStopDrawer)
        bindAction("Hide", binding.cbHideHome, binding.cbHideDrawer)
    }

    // =========================================================================
    // SECTION 3: LIVE PREVIEW UPDATE
    // =========================================================================
    private fun updateLivePreview() {
        val radiusDp = prefs.getInt(KEY_POPUP_CORNER_RADIUS, DEFAULT_CORNER_RADIUS_DP)
        val density = resources.displayMetrics.density
        val radiusPx = radiusDp * density

        val hasCardBg = prefs.getBoolean(KEY_POPUP_CARD_BG, true)
        val groupShortcuts = prefs.getBoolean(KEY_POPUP_GROUP_SHORTCUTS, false)

        val homeActions = prefs.getStringSet(KEY_POPUP_ACTIONS_HOME, DEFAULT_ACTIONS_HOME) ?: DEFAULT_ACTIONS_HOME

        // Dynamic Popup Bubble Background Drawable
        val bgDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusPx
            if (hasCardBg) {
                setColor(ContextCompat.getColor(this@PopupMenuSettingsActivity, R.color.card_bg))
                setStroke((1 * density).toInt(), ContextCompat.getColor(this@PopupMenuSettingsActivity, R.color.card_stroke))
            } else {
                setColor(Color.parseColor("#E612151E"))
                setStroke(0, Color.TRANSPARENT)
            }
        }
        binding.layoutPreviewPopupBubble.background = bgDrawable

        // Header Action Icons Visibility based on Home matrix
        binding.ivPreviewActionInfo.visibility = if ("App info" in homeActions) View.VISIBLE else View.GONE
        binding.ivPreviewActionEdit.visibility = if ("Edit" in homeActions) View.VISIBLE else View.GONE
        binding.ivPreviewActionWidgets.visibility = if ("Widgets" in homeActions) View.VISIBLE else View.GONE
        binding.ivPreviewActionShare.visibility = if ("Share" in homeActions) View.VISIBLE else View.GONE

        val hasAnyHeader = binding.ivPreviewActionInfo.visibility == View.VISIBLE ||
                binding.ivPreviewActionEdit.visibility == View.VISIBLE ||
                binding.ivPreviewActionWidgets.visibility == View.VISIBLE ||
                binding.ivPreviewActionShare.visibility == View.VISIBLE

        binding.layoutPreviewHeaderActions.visibility = if (hasAnyHeader) View.VISIBLE else View.GONE
        binding.previewDivider1.visibility = if (hasAnyHeader) View.VISIBLE else View.GONE

        // Group Shortcuts Visual Styling
        if (groupShortcuts) {
            val shortcutBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 6 * density
                setColor(Color.parseColor("#1AFFFFFF"))
            }
            binding.layoutPreviewShortcuts.background = shortcutBg
            binding.layoutPreviewShortcuts.setPadding((6 * density).toInt(), (4 * density).toInt(), (6 * density).toInt(), (4 * density).toInt())
        } else {
            binding.layoutPreviewShortcuts.background = null
            binding.layoutPreviewShortcuts.setPadding(0, 0, 0, 0)
        }
    }
}
