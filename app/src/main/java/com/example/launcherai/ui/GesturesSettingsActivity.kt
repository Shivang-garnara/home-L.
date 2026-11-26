package com.example.launcherai.ui

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewConfiguration
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.launcherai.R
import com.example.launcherai.databinding.ActivityGesturesSettingsBinding
import com.example.launcherai.gestures.GesturePreferences
import com.example.launcherai.gestures.GestureSlot
import com.example.launcherai.gestures.NamedAction
import com.example.launcherai.gestures.NovaAction
import com.example.launcherai.gestures.ScreenLockHelper

/**
 * Full Nova 8.9.9 Gestures & inputs settings screen:
 * - Button inputs (Home button + Only on main home page sub-toggle, conditional Long-press Menu)
 * - 11 desktop gestures (Swipe up/down/left/right, Double tap, Double tap + swipe up/down,
 *   Pinch in/out, Two finger swipe up/down, Two finger rotation CW/CCW)
 * - Conflict notice when Infinite Scroll disables Swipe left/right
 * - Contextual gear icons for Expand notifications (haptic + incremental mode) and Screen lock method
 */
class GesturesSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGesturesSettingsBinding
    private lateinit var prefs: SharedPreferences

    private val gestureSlots = listOf(
        GestureSlot.SWIPE_UP,
        GestureSlot.SWIPE_DOWN,
        GestureSlot.SWIPE_LEFT,
        GestureSlot.SWIPE_RIGHT,
        GestureSlot.DOUBLE_TAP,
        GestureSlot.DOUBLE_TAP_SWIPE_UP,
        GestureSlot.DOUBLE_TAP_SWIPE_DOWN,
        GestureSlot.PINCH_IN,
        GestureSlot.PINCH_OUT,
        GestureSlot.TWO_FINGER_SWIPE_UP,
        GestureSlot.TWO_FINGER_SWIPE_DOWN,
        GestureSlot.ROTATE_CW,
        GestureSlot.ROTATE_CCW
    )

    private val rowViews = mutableMapOf<GestureSlot, GestureRowHolder>()

    private val actionPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val slotKey = result.data!!.getStringExtra(ChooseActionActivity.EXTRA_SLOT_KEY) ?: return@registerForActivityResult
            val rawAction = result.data!!.getStringExtra(ChooseActionActivity.EXTRA_SELECTED_ACTION)
            val slot = GestureSlot.entries.firstOrNull { it.prefKey == slotKey } ?: return@registerForActivityResult
            val action = NamedAction.deserialize(rawAction)
            GesturePreferences.setAction(prefs, slot, action)
            refreshAll()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_NovaLight)
        super.onCreate(savedInstanceState)
        SystemBarHelper.applySystemBars(this, isLightTheme = true, transparentBars = false)
        binding = ActivityGesturesSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemBarHelper.applyEdgeToEdgeInsets(
            insetsRootView = binding.root,
            topInsetView = binding.headerContainer
        )

        prefs = getSharedPreferences(GesturePreferences.PREFS_NAME, Context.MODE_PRIVATE)
        binding.btnBack.setOnClickListener { finish() }

        buildGestureRows()
        setupStaticListeners()
        refreshAll()
    }

    override fun onResume() {
        super.onResume()
        refreshAll()
    }

    private fun buildGestureRows() {
        binding.containerGestureRows.removeAllViews()
        rowViews.clear()
        val inflater = LayoutInflater.from(this)
        val brownTint = ContextCompat.getColor(this, R.color.nova_icon_tint_brown)

        for (slot in gestureSlots) {
            val rowView = inflater.inflate(R.layout.item_action_picker_row, binding.containerGestureRows, false)
            // Normalize horizontal padding to match container
            rowView.setPadding(0, rowView.paddingTop, 0, rowView.paddingBottom)
            val ivIcon = rowView.findViewById<ImageView>(R.id.ivRowIcon)
            val tvTitle = rowView.findViewById<TextView>(R.id.tvRowTitle)
            val tvSubtitle = rowView.findViewById<TextView>(R.id.tvRowSubtitle)
            val btnGear = rowView.findViewById<ImageButton>(R.id.btnRowGear)

            ivIcon.layoutParams.width = (24 * resources.displayMetrics.density).toInt()
            ivIcon.layoutParams.height = (24 * resources.displayMetrics.density).toInt()
            ivIcon.setImageResource(iconForSlot(slot))
            ivIcon.setColorFilter(brownTint)

            tvTitle.text = slot.title
            tvSubtitle.visibility = View.VISIBLE

            rowView.setOnClickListener { openActionPickerFor(slot) }

            rowViews[slot] = GestureRowHolder(rowView, tvSubtitle, btnGear)
            binding.containerGestureRows.addView(rowView)
        }
    }

    private fun setupStaticListeners() {
        binding.rowHomeButton.setOnClickListener { openActionPickerFor(GestureSlot.HOME_BUTTON) }
        binding.rowLongPressMenu.setOnClickListener { openActionPickerFor(GestureSlot.LONG_PRESS_MENU) }

        binding.swHomeOnlyDefault.setOnCheckedChangeListener { _, isChecked ->
            GesturePreferences.setHomeButtonOnlyOnDefault(prefs, isChecked)
        }
        binding.rowHomeOnlyDefault.setOnClickListener {
            if (binding.swHomeOnlyDefault.isEnabled) {
                binding.swHomeOnlyDefault.isChecked = !binding.swHomeOnlyDefault.isChecked
            }
        }

        binding.rowScreenLockMethod.setOnClickListener {
            ScreenLockHelper.showScreenLockMethodDialog(this) {
                refreshAll()
            }
        }

        binding.swForceShowLongMenu.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(GesturePreferences.KEY_SHOW_LONG_PRESS_MENU_FORCE, isChecked).apply()
            refreshAll()
        }
    }

    private fun openActionPickerFor(slot: GestureSlot) {
        val intent = ChooseActionActivity.createIntent(
            context = this,
            title = slot.title,
            slotKey = slot.prefKey,
            folderMode = false
        )
        actionPickerLauncher.launch(intent)
    }

    private fun refreshAll() {
        // 1. Button Inputs
        val homeAction = GesturePreferences.getAction(prefs, GestureSlot.HOME_BUTTON)
        binding.tvHomeButtonSummary.text = homeAction.displaySummary()
        configureGearButton(binding.btnHomeButtonGear, GestureSlot.HOME_BUTTON, homeAction)

        val homeHasAction = !homeAction.isNone
        binding.swHomeOnlyDefault.isEnabled = homeHasAction
        binding.rowHomeOnlyDefault.alpha = if (homeHasAction) 1.0f else 0.45f
        binding.swHomeOnlyDefault.setOnCheckedChangeListener(null)
        binding.swHomeOnlyDefault.isChecked = GesturePreferences.isHomeButtonOnlyOnDefault(prefs)
        binding.swHomeOnlyDefault.setOnCheckedChangeListener { _, isChecked ->
            GesturePreferences.setHomeButtonOnlyOnDefault(prefs, isChecked)
        }

        // Long-press Menu conditional visibility matching Nova SettingsGestures.A()
        val hasPermMenuKey = ViewConfiguration.get(this).hasPermanentMenuKey()
        val hasSysProp = GesturePreferences.hasSystemLongPressMenuSetting()
        val forceLongMenu = prefs.getBoolean(GesturePreferences.KEY_SHOW_LONG_PRESS_MENU_FORCE, false)
        binding.swForceShowLongMenu.setOnCheckedChangeListener(null)
        binding.swForceShowLongMenu.isChecked = forceLongMenu
        binding.swForceShowLongMenu.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(GesturePreferences.KEY_SHOW_LONG_PRESS_MENU_FORCE, isChecked).apply()
            refreshAll()
        }

        val showLongMenu = GesturePreferences.shouldShowLongPressMenu(
            hasPermanentMenuKey = hasPermMenuKey,
            hasSystemLongPressProp = hasSysProp,
            forceShow = forceLongMenu
        )
        binding.rowLongPressMenu.visibility = if (showLongMenu) View.VISIBLE else View.GONE
        val longMenuAction = GesturePreferences.getAction(prefs, GestureSlot.LONG_PRESS_MENU)
        binding.tvLongMenuSummary.text = longMenuAction.displaySummary()
        configureGearButton(binding.btnLongMenuGear, GestureSlot.LONG_PRESS_MENU, longMenuAction)

        // 2. Infinite scroll conflict with Swipe left/right (Nova w00.b)
        val infiniteScroll = GesturePreferences.isInfiniteScrollEnabled(prefs)
        binding.rowSwipeLeftRightConflict.visibility = if (infiniteScroll) View.VISIBLE else View.GONE

        for (slot in gestureSlots) {
            val holder = rowViews[slot] ?: continue
            if (infiniteScroll && (slot == GestureSlot.SWIPE_LEFT || slot == GestureSlot.SWIPE_RIGHT)) {
                holder.root.visibility = View.GONE
                continue
            }
            holder.root.visibility = View.VISIBLE
            val action = GesturePreferences.getAction(prefs, slot)
            holder.summary.text = action.displaySummary()
            configureGearButton(holder.gear, slot, action)
        }

        // 3. Screen lock method summary
        val method = GesturePreferences.getScreenLockMethod(prefs)
        val statusSuffix = when {
            ScreenLockHelper.isAccessibilityServiceRunning() -> " • Accessibility Active"
            ScreenLockHelper.isDeviceAdminActive(this) -> " • Device Admin Active"
            else -> ""
        }
        binding.tvScreenLockMethodSummary.text = "${method.title}$statusSuffix"
    }

    private fun configureGearButton(gear: ImageButton, slot: GestureSlot, action: NamedAction) {
        val isScreenLock = action.kind == NamedAction.Kind.NOVA_ACTION && action.novaAction == NovaAction.SCREEN_OFF
        val isSwipeDownExpand = slot == GestureSlot.SWIPE_DOWN &&
            action.kind == NamedAction.Kind.NOVA_ACTION &&
            action.novaAction == NovaAction.EXPAND_STATUS_BAR

        when {
            isScreenLock -> {
                gear.visibility = View.VISIBLE
                gear.setOnClickListener {
                    ScreenLockHelper.showScreenLockMethodDialog(this) { refreshAll() }
                }
            }
            isSwipeDownExpand -> {
                gear.visibility = View.VISIBLE
                gear.setOnClickListener { showExpandNotificationsOptionsDialog() }
            }
            else -> {
                gear.visibility = View.GONE
                gear.setOnClickListener(null)
            }
        }
    }

    private fun showExpandNotificationsOptionsDialog() {
        val labels = arrayOf(
            "Haptic feedback",
            "Legacy fling mode (wait for swipe release)"
        )
        val checked = booleanArrayOf(
            GesturePreferences.isSwipeDownHaptic(prefs),
            GesturePreferences.isSwipeDownLegacy(prefs)
        )
        AlertDialog.Builder(this)
            .setTitle("Expand notifications")
            .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                when (which) {
                    0 -> GesturePreferences.setSwipeDownHaptic(prefs, isChecked)
                    1 -> GesturePreferences.setSwipeDownLegacy(prefs, isChecked)
                }
            }
            .setPositiveButton("Done", null)
            .show()
    }

    private fun iconForSlot(slot: GestureSlot): Int {
        return when (slot) {
            GestureSlot.HOME_BUTTON -> R.drawable.ic_settings_home
            GestureSlot.LONG_PRESS_MENU -> R.drawable.ic_settings
            GestureSlot.SWIPE_UP, GestureSlot.TWO_FINGER_SWIPE_UP -> R.drawable.ic_chevron_up
            GestureSlot.SWIPE_DOWN, GestureSlot.TWO_FINGER_SWIPE_DOWN -> R.drawable.ic_chevron_down
            GestureSlot.SWIPE_LEFT -> R.drawable.ic_arrow_back
            GestureSlot.SWIPE_RIGHT -> R.drawable.ic_settings_gestures
            GestureSlot.DOUBLE_TAP,
            GestureSlot.DOUBLE_TAP_SWIPE_UP,
            GestureSlot.DOUBLE_TAP_SWIPE_DOWN -> R.drawable.ic_settings_gestures
            GestureSlot.PINCH_IN,
            GestureSlot.PINCH_OUT,
            GestureSlot.ROTATE_CW,
            GestureSlot.ROTATE_CCW -> R.drawable.ic_settings_gestures
        }
    }

    private data class GestureRowHolder(
        val root: View,
        val summary: TextView,
        val gear: ImageButton
    )
}
