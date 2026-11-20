package com.example.launcherai.ui

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.content.res.ColorStateList
import android.graphics.Color
import android.widget.SeekBar
import android.widget.Toast
import com.example.launcherai.icons.IconShapeMasker
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.launcherai.MainActivity
import com.example.launcherai.R
import com.example.launcherai.databinding.ActivitySetupBinding

/**
 * First-Run Setup Wizard matching Nova Launcher 8.9.6 reference screens 01, 04, 05, 06, 07, 08.
 * Features controlled step progression, lifecycle-safe step persistence, and native RoleManager default chooser integration.
 */
class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding
    private var currentStep = 1

    private val requestContactsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("key_contact_search", isGranted).apply()
        binding.tglContactSearch.isChecked = isGranted
        if (!isGranted) {
            binding.tvContactSearchSub.text = "Permission denied. Contact search disabled until granted in System Settings."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_NovaDark)
        super.onCreate(savedInstanceState)
        SystemBarHelper.applySystemBars(this, isLightTheme = false, transparentBars = false)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initStepListeners()
        setupBackHandler()

        val intentStep = intent.getIntExtra("setup_step", -1)
        val initialStep = if (intentStep != -1) intentStep else SetupFlowUtils.getSetupCurrentStep(this)
        updateStepVisibility(initialStep)

        if (intent.getBooleanExtra("open_more_shapes", false)) {
            val dialog = MoreShapesDialogFragment.newInstance()
            dialog.show(supportFragmentManager, MoreShapesDialogFragment.TAG)
        }
    }

    private var pendingActionAfterChooser = PendingActionAfterChooser.NONE

    private enum class PendingActionAfterChooser {
        NONE,
        ADVANCE_TO_STEP_2,
        GO_HOME_FINAL
    }

    private val defaultLauncherRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        val action = pendingActionAfterChooser
        pendingActionAfterChooser = PendingActionAfterChooser.NONE
        when (action) {
            PendingActionAfterChooser.ADVANCE_TO_STEP_2 -> goNextStep()
            PendingActionAfterChooser.GO_HOME_FINAL -> completeSetupAndGoHome()
            else -> {}
        }
    }

    private fun initStepListeners() {
        // Header navigation
        binding.btnBackSetup.setOnClickListener {
            goPreviousStep()
        }

        binding.fabNextSetup.setOnClickListener {
            goNextStep()
        }

        // STEP 1: WELCOME
        binding.btnGetStarted.setOnClickListener {
            if (!SetupFlowUtils.isDefaultLauncher(this)) {
                requestSystemDefaultLauncher(PendingActionAfterChooser.ADVANCE_TO_STEP_2)
            } else {
                goNextStep()
            }
        }

        binding.btnRestoreBackup.setOnClickListener {
            Toast.makeText(this, "Restore backup feature not available yet", Toast.LENGTH_SHORT).show()
        }

        binding.btnSkipSetup.setOnClickListener {
            completeSetupAndGoHome()
        }

        // STEP 2: SEARCH
        initSearchStep()

        // STEP 3: APP DRAWER
        initAppDrawerStep()

        // STEP 4: ICON STYLE
        initIconStyleStep()

        // STEP 5: ALL DONE
        binding.btnGoHome.setOnClickListener {
            if (!SetupFlowUtils.isDefaultLauncher(this)) {
                requestSystemDefaultLauncher(PendingActionAfterChooser.GO_HOME_FINAL)
            } else {
                completeSetupAndGoHome()
            }
        }
    }

    private fun initSearchStep() {
        val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)

        // Mode Dropdown
        val currentMode = prefs.getString("key_search_mode", "Immersive") ?: "Immersive"
        binding.ddSearchMode.text = "$currentMode ▾"
        binding.ddSearchMode.setOnClickListener {
            showSingleChoiceDropdown("Mode", arrayOf("Immersive", "Standard")) { chosen ->
                binding.ddSearchMode.text = "$chosen ▾"
                prefs.edit().putString("key_search_mode", chosen).apply()
            }
        }

        // Shortcut Search Toggle
        val shortcutSearch = prefs.getBoolean("key_shortcut_search", true)
        binding.tglShortcutSearch.isChecked = shortcutSearch
        binding.tglShortcutSearch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("key_shortcut_search", isChecked).apply()
        }

        // Contact Search Toggle
        val hasContactPerm = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        val contactSearch = prefs.getBoolean("key_contact_search", false) && hasContactPerm
        binding.tglContactSearch.isChecked = contactSearch
        binding.tglContactSearch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                    requestContactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                } else {
                    prefs.edit().putBoolean("key_contact_search", true).apply()
                }
            } else {
                prefs.edit().putBoolean("key_contact_search", false).apply()
            }
        }

        // Home Placement Dropdown
        val homePlacement = prefs.getString("key_search_bar_placement_home", "DOCK_BELOW") ?: "DOCK_BELOW"
        binding.ddSearchPlacementHome.text = when (homePlacement) {
            "DOCK_ABOVE" -> "Dock (Above icons) ▾"
            "TOP" -> "Top ▾"
            "HIDDEN" -> "Hidden ▾"
            else -> "Dock (Below icons) ▾"
        }
        binding.ddSearchPlacementHome.setOnClickListener {
            showSingleChoiceDropdown(
                "Home Search Bar Placement",
                arrayOf("Dock (Below icons)", "Dock (Above icons)", "Top", "Hidden")
            ) { chosen ->
                binding.ddSearchPlacementHome.text = "$chosen ▾"
                val value = when {
                    chosen.contains("Above") -> "DOCK_ABOVE"
                    chosen.contains("Top") -> "TOP"
                    chosen.contains("Hidden") -> "HIDDEN"
                    else -> "DOCK_BELOW"
                }
                prefs.edit().putString("key_search_bar_placement_home", value).apply()
            }
        }

        // Drawer Placement Dropdown
        val drawerPlacement = prefs.getString("key_search_bar_placement_drawer", "Top") ?: "Top"
        binding.ddSearchPlacementDrawer.text = "$drawerPlacement ▾"
        binding.ddSearchPlacementDrawer.setOnClickListener {
            showSingleChoiceDropdown("Drawer Search Bar Placement", arrayOf("Top", "Bottom", "Hidden")) { chosen ->
                binding.ddSearchPlacementDrawer.text = "$chosen ▾"
                prefs.edit().putString("key_search_bar_placement_drawer", chosen).apply()
            }
        }
    }

    private fun initAppDrawerStep() {
        val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)

        // Dropdown: App Drawer Style (Vertical, List, Horizontal)
        val currentStyle = prefs.getString("key_app_drawer_style", "Vertical") ?: "Vertical"
        binding.ddAppDrawerStyle.text = "$currentStyle ▾"

        binding.ddAppDrawerStyle.setOnClickListener {
            showSingleChoiceDropdown("App Drawer Style", arrayOf("Vertical", "List", "Horizontal")) { chosen ->
                binding.ddAppDrawerStyle.text = "$chosen ▾"
                prefs.edit().putString("key_app_drawer_style", chosen).apply()
            }
        }

        // Toggles
        val showKb = prefs.getBoolean("key_drawer_show_keyboard", true)
        val freqApps = prefs.getBoolean("key_drawer_frequent_apps", true)
        val freqShortcuts = prefs.getBoolean("key_drawer_frequent_shortcuts", true)
        val smartFolders = prefs.getBoolean("key_drawer_smart_folders", true)

        binding.tglShowKeyboard.isChecked = showKb
        binding.tglFrequentApps.isChecked = freqApps
        binding.tglFrequentShortcuts.isChecked = freqShortcuts
        binding.tglSmartFolders.isChecked = smartFolders

        binding.tglShowKeyboard.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("key_drawer_show_keyboard", isChecked).apply()
        }

        binding.tglFrequentApps.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("key_drawer_frequent_apps", isChecked).apply()
        }

        binding.tglFrequentShortcuts.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("key_drawer_frequent_shortcuts", isChecked).apply()
        }

        binding.tglSmartFolders.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("key_drawer_smart_folders", isChecked).apply()
        }
    }

    private fun initIconStyleStep() {
        val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)

        // Color Swatches
        val currentAccent = prefs.getString("key_accent_color", "#00BCD4") ?: "#00BCD4"
        updateColorSwatchSelection(currentAccent)

        binding.containerSwatchCyan.setOnClickListener { selectAccentColor("#00BCD4") }
        binding.containerSwatchWhite.setOnClickListener { selectAccentColor("#FFFFFF") }
        binding.containerSwatchGreen.setOnClickListener { selectAccentColor("#00E676") }
        binding.containerSwatchMagenta.setOnClickListener { selectAccentColor("#E040FB") }

        // Icon Size Slider (70% - 150%, default 125%)
        val currentSize = prefs.getInt("key_icon_size_percent", 125)
        binding.seekIconSize.progress = (currentSize - 70).coerceIn(0, 80)
        updateIconSizeDisplay(currentSize)

        binding.seekIconSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val size = 70 + progress
                updateIconSizeDisplay(size)
                prefs.edit().putInt("key_icon_size_percent", size).apply()
                updateIconPreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.btnIconSizeMinus.setOnClickListener {
            val nextProg = (binding.seekIconSize.progress - 5).coerceAtLeast(0)
            binding.seekIconSize.progress = nextProg
        }

        binding.btnIconSizePlus.setOnClickListener {
            val nextProg = (binding.seekIconSize.progress + 5).coerceAtMost(80)
            binding.seekIconSize.progress = nextProg
        }

        // Icon Shapes (Round is default in Nova 8.9.9)
        val currentShape = prefs.getString("key_icon_shape", "Round") ?: "Round"
        updateShapeSelectionUI(currentShape)

        binding.shapeRound.setOnClickListener { selectIconShape("Round") }
        binding.shapeSquircle.setOnClickListener { selectIconShape("Squircle") }
        binding.shapeRoundedSquare.setOnClickListener { selectIconShape("Rounded square") }
        binding.shapeFlower.setOnClickListener { selectIconShape("Flower") }

        binding.btnMoreShapes.setOnClickListener {
            val dialog = MoreShapesDialogFragment.newInstance()
            dialog.onShapeSelectedListener = { selectedShape ->
                selectIconShape(selectedShape)
            }
            dialog.show(supportFragmentManager, MoreShapesDialogFragment.TAG)
        }

        updateIconPreview()
    }

    private fun selectAccentColor(colorHex: String) {
        getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            .edit().putString("key_accent_color", colorHex).apply()
        updateColorSwatchSelection(colorHex)
    }

    private fun updateColorSwatchSelection(selectedHex: String) {
        binding.ringSwatchCyan.visibility = if (selectedHex.equals("#00BCD4", ignoreCase = true)) View.VISIBLE else View.GONE
        binding.ringSwatchWhite.visibility = if (selectedHex.equals("#FFFFFF", ignoreCase = true)) View.VISIBLE else View.GONE
        binding.ringSwatchGreen.visibility = if (selectedHex.equals("#00E676", ignoreCase = true)) View.VISIBLE else View.GONE
        binding.ringSwatchMagenta.visibility = if (selectedHex.equals("#E040FB", ignoreCase = true)) View.VISIBLE else View.GONE
    }

    private fun updateIconSizeDisplay(size: Int) {
        binding.tvIconSizeValue.text = "$size%"
    }

    private fun selectIconShape(shape: String) {
        getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            .edit().putString("key_icon_shape", shape).apply()
        updateShapeSelectionUI(shape)
        updateIconPreview()
    }

    private fun updateShapeSelectionUI(shape: String) {
        val activeColor = ContextCompat.getColor(this, R.color.nova_accent_orange)
        val inactiveColor = Color.parseColor("#555555")
        val textWhite = ContextCompat.getColor(this, R.color.white)

        // Reset all shapes to inactive
        binding.ivShapeRound.backgroundTintList = ColorStateList.valueOf(inactiveColor)
        binding.tvShapeRound.setTextColor(textWhite)
        binding.ivShapeSquircle.backgroundTintList = ColorStateList.valueOf(inactiveColor)
        binding.tvShapeSquircle.setTextColor(textWhite)
        binding.ivShapeRoundedSquare.backgroundTintList = ColorStateList.valueOf(inactiveColor)
        binding.tvShapeRoundedSquare.setTextColor(textWhite)
        binding.ivShapeFlower.backgroundTintList = ColorStateList.valueOf(inactiveColor)
        binding.tvShapeFlower.setTextColor(textWhite)
        binding.ivShapeMore.backgroundTintList = ColorStateList.valueOf(inactiveColor)
        binding.tvShapeMore.setTextColor(textWhite)
        binding.tvShapeMore.text = "More"

        when (shape.trim().lowercase()) {
            "round" -> {
                binding.ivShapeRound.backgroundTintList = ColorStateList.valueOf(activeColor)
                binding.tvShapeRound.setTextColor(activeColor)
            }
            "squircle" -> {
                binding.ivShapeSquircle.backgroundTintList = ColorStateList.valueOf(activeColor)
                binding.tvShapeSquircle.setTextColor(activeColor)
            }
            "rounded square" -> {
                binding.ivShapeRoundedSquare.backgroundTintList = ColorStateList.valueOf(activeColor)
                binding.tvShapeRoundedSquare.setTextColor(activeColor)
            }
            "flower" -> {
                binding.ivShapeFlower.backgroundTintList = ColorStateList.valueOf(activeColor)
                binding.tvShapeFlower.setTextColor(activeColor)
            }
            else -> {
                // Extended shape chosen from MoreShapesDialogFragment
                binding.ivShapeMore.backgroundTintList = ColorStateList.valueOf(activeColor)
                binding.tvShapeMore.setTextColor(activeColor)
                binding.tvShapeMore.text = shape
            }
        }
    }

    private fun updateIconPreview() {
        val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        val shape = prefs.getString("key_icon_shape", "Round") ?: "Round"
        val sizePercent = prefs.getInt("key_icon_size_percent", 125)

        val baseDrawable = ContextCompat.getDrawable(this, R.mipmap.ic_launcher)
        val previewSizePx = com.example.launcherai.icons.DeviceIconMetrics.getPreviewIconSizePx(this, sizePercent)
        val masked = IconShapeMasker.createMaskedIcon(this, baseDrawable, shape, sizePercent, targetSizePx = previewSizePx)
        binding.ivIconStylePreview.setImageDrawable(masked ?: baseDrawable)

        binding.ivIconStylePreview.layoutParams = binding.ivIconStylePreview.layoutParams.apply {
            width = previewSizePx
            height = previewSizePx
        }
    }

    private fun showSingleChoiceDropdown(title: String, options: Array<String>, onSelected: (String) -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(options) { _, which ->
                onSelected(options[which])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun goNextStep() {
        if (currentStep < 5) {
            updateStepVisibility(currentStep + 1)
        }
    }

    private fun goPreviousStep() {
        if (currentStep > 1) {
            updateStepVisibility(currentStep - 1)
        } else {
            finish()
        }
    }

    private fun updateStepVisibility(step: Int) {
        currentStep = step
        SetupFlowUtils.saveSetupCurrentStep(this, step)

        binding.step1Welcome.visibility = if (step == 1) View.VISIBLE else View.GONE
        binding.step2Search.visibility = if (step == 2) View.VISIBLE else View.GONE
        binding.step3AppDrawer.visibility = if (step == 3) View.VISIBLE else View.GONE
        binding.step4IconStyle.visibility = if (step == 4) View.VISIBLE else View.GONE
        binding.step5AllDone.visibility = if (step == 5) View.VISIBLE else View.GONE

        if (step == 2) {
            initSearchStep()
        }
        if (step == 3) {
            initAppDrawerStep()
        }
        if (step == 4) {
            initIconStyleStep()
        }

        binding.headerSetupBar.visibility = if (step > 1) View.VISIBLE else View.GONE
        binding.fabNextSetup.visibility = if (step in 2..4) View.VISIBLE else View.GONE

        if (step in 2..4) {
            binding.progressSetup.visibility = View.VISIBLE
            binding.ivProgressChecked.visibility = View.GONE
            binding.progressSetup.progress = when (step) {
                2 -> 25
                3 -> 50
                4 -> 75
                else -> 100
            }
        } else if (step == 5) {
            binding.progressSetup.visibility = View.GONE
            binding.ivProgressChecked.visibility = View.VISIBLE
        }
    }

    private fun requestSystemDefaultLauncher(action: PendingActionAfterChooser) {
        pendingActionAfterChooser = action
        var launched = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val roleManager = getSystemService(RoleManager::class.java)
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                    defaultLauncherRoleLauncher.launch(intent)
                    launched = true
                }
            } catch (e: Exception) {
                launched = false
            }
        }
        if (!launched) {
            try {
                val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                defaultLauncherRoleLauncher.launch(intent)
            } catch (e: Exception) {
                try {
                    val intent = Intent(Settings.ACTION_SETTINGS)
                    defaultLauncherRoleLauncher.launch(intent)
                } catch (e2: Exception) {
                    Toast.makeText(this, "Please select LauncherAI as Home App in Android Settings > Apps > Default Apps", Toast.LENGTH_LONG).show()
                    if (action == PendingActionAfterChooser.ADVANCE_TO_STEP_2) {
                        goNextStep()
                    } else if (action == PendingActionAfterChooser.GO_HOME_FINAL) {
                        completeSetupAndGoHome()
                    }
                }
            }
        }
    }

    private fun completeSetupAndGoHome() {
        SetupFlowUtils.markSetupCompleted(this)

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("skip_wizard", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goPreviousStep()
            }
        })
    }
}
