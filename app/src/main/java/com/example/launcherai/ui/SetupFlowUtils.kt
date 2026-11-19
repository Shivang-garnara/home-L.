package com.example.launcherai.ui

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

/**
 * Pure production logic for First-Run Setup routing, step persistence, resume functionality,
 * and default launcher role checks.
 * Called directly by SetupActivity, MainActivity & LauncherSettingsActivity and tested by unit tests.
 */
object SetupFlowUtils {

    const val KEY_FIRST_RUN_COMPLETED = "key_first_run_completed"
    const val KEY_SETUP_CURRENT_STEP = "key_setup_current_step"

    /**
     * Determines whether MainActivity should launch SetupActivity on launch.
     */
    fun shouldLaunchSetup(context: Context, skipWizardExtra: Boolean): Boolean {
        if (skipWizardExtra) return false
        val prefs = context.getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        val isCompleted = prefs.getBoolean(KEY_FIRST_RUN_COMPLETED, false)
        return !isCompleted
    }

    /**
     * Checks if LauncherAI is currently set as the default home app.
     */
    fun isDefaultLauncher(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                return true
            }
        }
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addCategory(Intent.CATEGORY_DEFAULT)
        }
        val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val defaultPkg = resolveInfo?.activityInfo?.packageName
        return defaultPkg == context.packageName
    }

    /**
     * Saves the active setup step index so setup can resume at the same step after process death.
     */
    fun saveSetupCurrentStep(context: Context, step: Int) {
        val prefs = context.getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_SETUP_CURRENT_STEP, step.coerceIn(1, 5)).apply()
    }

    /**
     * Retrieves the saved setup step index (defaulting to step 1).
     */
    fun getSetupCurrentStep(context: Context): Int {
        val prefs = context.getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        return prefs.getInt(KEY_SETUP_CURRENT_STEP, 1).coerceIn(1, 5)
    }

    /**
     * Marks setup as fully completed.
     */
    fun markSetupCompleted(context: Context) {
        val prefs = context.getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_FIRST_RUN_COMPLETED, true)
            .putInt(KEY_SETUP_CURRENT_STEP, 1)
            .apply()
    }

    /**
     * Resets setup state to uncompleted step 1 (for testing & re-running setup).
     */
    fun resetSetup(context: Context) {
        val prefs = context.getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_FIRST_RUN_COMPLETED, false)
            .putInt(KEY_SETUP_CURRENT_STEP, 1)
            .apply()
    }
}
