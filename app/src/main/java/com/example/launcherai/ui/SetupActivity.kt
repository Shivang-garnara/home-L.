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
import android.widget.Toast
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
        binding.ddSearchMode.setOnClickListener {
            showSingleChoiceDropdown("Mode", arrayOf("Immersive", "Standard")) { chosen ->
                binding.ddSearchMode.text = "$chosen ▾"
                getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                    .edit().putString("key_search_mode", chosen).apply()
            }
        }

        binding.tglShortcutSearch.setOnCheckedChangeListener { _, isChecked ->
            getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("key_shortcut_search", isChecked).apply()
        }

        binding.tglContactSearch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                    requestContactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                } else {
                    getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                        .edit().putBoolean("key_contact_search", true).apply()
                }
            } else {
                getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                    .edit().putBoolean("key_contact_search", false).apply()
            }
        }

        binding.ddSearchPlacementHome.setOnClickListener {
            showSingleChoiceDropdown("Home Search Bar Placement", arrayOf("Dock (Below icons)", "Top", "Hidden")) { chosen ->
                binding.ddSearchPlacementHome.text = "$chosen ▾"
                val value = when {
                    chosen.contains("Top") -> "TOP"
                    chosen.contains("Hidden") -> "HIDDEN"
                    else -> "DOCK_BELOW"
                }
                getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                    .edit().putString("key_search_bar_placement_home", value).apply()
            }
        }

        binding.ddSearchPlacementDrawer.setOnClickListener {
            showSingleChoiceDropdown("Drawer Search Bar Placement", arrayOf("Top", "Bottom")) { chosen ->
                binding.ddSearchPlacementDrawer.text = "$chosen ▾"
                getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                    .edit().putString("key_search_bar_placement_drawer", chosen).apply()
            }
        }

        // STEP 3: APP DRAWER
        binding.ddAppDrawerStyle.setOnClickListener {
            showSingleChoiceDropdown("App Drawer Style", arrayOf("Vertical", "Horizontal")) { chosen ->
                binding.ddAppDrawerStyle.text = "$chosen ▾"
                getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                    .edit().putString("key_app_drawer_style", chosen).apply()
            }
        }

        binding.tglShowKeyboard.setOnCheckedChangeListener { _, isChecked ->
            getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("key_drawer_show_keyboard", isChecked).apply()
        }

        binding.tglFrequentApps.setOnCheckedChangeListener { _, isChecked ->
            getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("key_drawer_frequent_apps", isChecked).apply()
        }

        binding.tglFrequentShortcuts.setOnCheckedChangeListener { _, isChecked ->
            getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("key_drawer_frequent_shortcuts", isChecked).apply()
        }

        binding.tglSmartFolders.setOnCheckedChangeListener { _, isChecked ->
            getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("key_drawer_smart_folders", isChecked).apply()
        }

        // STEP 4: ICON STYLE
        binding.shapeRoundedSquare.setOnClickListener { selectIconShape("Rounded square") }
        binding.shapeRound.setOnClickListener { selectIconShape("Round") }
        binding.shapeSquircle.setOnClickListener { selectIconShape("Squircle") }
        binding.shapeFlower.setOnClickListener { selectIconShape("Flower") }

        binding.btnMoreShapes.setOnClickListener {
            val dialog = MoreShapesDialogFragment.newInstance()
            dialog.onShapeSelectedListener = { selectedShape ->
                selectIconShape(selectedShape)
            }
            dialog.show(supportFragmentManager, MoreShapesDialogFragment.TAG)
        }

        // STEP 5: ALL DONE
        binding.btnGoHome.setOnClickListener {
            if (!SetupFlowUtils.isDefaultLauncher(this)) {
                requestSystemDefaultLauncher(PendingActionAfterChooser.GO_HOME_FINAL)
            } else {
                completeSetupAndGoHome()
            }
        }
    }

    private fun selectIconShape(shape: String) {
        getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            .edit().putString("key_icon_shape", shape).apply()
        Toast.makeText(this, "Icon shape set to $shape", Toast.LENGTH_SHORT).show()
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
