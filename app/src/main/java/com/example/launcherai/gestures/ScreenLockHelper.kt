package com.example.launcherai.gestures

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

/**
 * Nova-style 3-strategy Screen Lock engine (Accessibility, Device Admin, Timeout)
 * with Accessibility battery-optimization recovery dialog and Screen Lock Method chooser.
 */
object ScreenLockHelper {

    fun getDeviceAdminComponent(context: Context): ComponentName =
        ComponentName(context, LauncherDeviceAdminReceiver::class.java)

    fun isDeviceAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            ?: return false
        return dpm.isAdminActive(getDeviceAdminComponent(context))
    }

    fun isAccessibilityServiceRunning(): Boolean = NovaAccessibilityService.instance != null

    fun isAccessibilityServiceEnabledInSettings(context: Context): Boolean {
        return try {
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""
            val expected = "${context.packageName}/${NovaAccessibilityService::class.java.name}"
            val shortName = "${context.packageName}/.gestures.NovaAccessibilityService"
            enabled.contains(expected) || enabled.contains(shortName)
        } catch (_: Exception) {
            false
        }
    }

    fun lockScreen(context: Context) {
        val prefs = context.getSharedPreferences(GesturePreferences.PREFS_NAME, Context.MODE_PRIVATE)
        val method = GesturePreferences.getScreenLockMethod(prefs)

        when (method) {
            ScreenLockMethod.ACCESSIBILITY -> tryAccessibilityLock(context)
            ScreenLockMethod.DEVICE_ADMIN -> tryDeviceAdminLock(context)
            ScreenLockMethod.TIMEOUT -> launchTimeoutLock(context)
            ScreenLockMethod.AUTO -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && isAccessibilityServiceRunning()) {
                    if (NovaAccessibilityService.lockScreen()) return
                }
                if (isDeviceAdminActive(context)) {
                    tryDeviceAdminLock(context)
                    return
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                    isAccessibilityServiceEnabledInSettings(context) &&
                    !isAccessibilityServiceRunning()
                ) {
                    showAccessibilityBatteryOptDialog(context)
                    return
                }
                showScreenLockMethodDialog(context)
            }
        }
    }

    private fun tryAccessibilityLock(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            Toast.makeText(context, "Accessibility Screen Lock requires Android 9+", Toast.LENGTH_SHORT).show()
            showScreenLockMethodDialog(context)
            return
        }
        if (NovaAccessibilityService.lockScreen()) return

        if (isAccessibilityServiceEnabledInSettings(context)) {
            showAccessibilityBatteryOptDialog(context)
        } else {
            showEnableAccessibilityDialog(context)
        }
    }

    private fun tryDeviceAdminLock(context: Context) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val comp = getDeviceAdminComponent(context)
        if (dpm != null && dpm.isAdminActive(comp)) {
            try {
                dpm.lockNow()
                return
            } catch (_: SecurityException) {
            }
        }
        showEnableDeviceAdminDialog(context)
    }

    private fun launchTimeoutLock(context: Context) {
        val intent = Intent(context, ScreenTimeoutActivity::class.java)
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun showScreenLockMethodDialog(
        context: Context,
        onMethodSelected: ((ScreenLockMethod) -> Unit)? = null
    ) {
        val prefs = context.getSharedPreferences(GesturePreferences.PREFS_NAME, Context.MODE_PRIVATE)
        val available = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                add(ScreenLockMethod.ACCESSIBILITY)
            }
            add(ScreenLockMethod.DEVICE_ADMIN)
            add(ScreenLockMethod.TIMEOUT)
        }
        val current = GesturePreferences.getScreenLockMethod(prefs)
        val checkedIdx = available.indexOf(current).let { if (it >= 0) it else -1 }
        val labels = available.map { method ->
            val activeTag = when (method) {
                ScreenLockMethod.ACCESSIBILITY -> if (isAccessibilityServiceRunning()) " (Active)" else ""
                ScreenLockMethod.DEVICE_ADMIN -> if (isDeviceAdminActive(context)) " (Active)" else ""
                ScreenLockMethod.TIMEOUT -> if (Settings.System.canWrite(context)) " (Ready)" else ""
                ScreenLockMethod.AUTO -> ""
            }
            "${method.title}$activeTag\n${method.summary}"
        }.toTypedArray()

        AlertDialog.Builder(context)
            .setTitle("Screen lock method")
            .setSingleChoiceItems(labels, checkedIdx) { dialog, which ->
                val chosen = available[which]
                GesturePreferences.setScreenLockMethod(prefs, chosen)
                dialog.dismiss()
                onMethodSelected?.invoke(chosen)
                when (chosen) {
                    ScreenLockMethod.ACCESSIBILITY -> {
                        if (!isAccessibilityServiceRunning()) showEnableAccessibilityDialog(context)
                    }
                    ScreenLockMethod.DEVICE_ADMIN -> {
                        if (!isDeviceAdminActive(context)) showEnableDeviceAdminDialog(context)
                    }
                    ScreenLockMethod.TIMEOUT -> {
                        if (!Settings.System.canWrite(context)) {
                            launchTimeoutLock(context)
                        }
                    }
                    ScreenLockMethod.AUTO -> {}
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun showEnableAccessibilityDialog(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("Enable Accessibility Service")
            .setMessage(
                "Screen lock uses Android's Accessibility performGlobalAction API so biometric unlock continues working. " +
                    "LauncherAI does not monitor or read any app content."
            )
            .setPositiveButton("Open Settings") { _, _ ->
                openAccessibilitySettings(context)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAccessibilityBatteryOptDialog(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("Accessibility")
            .setMessage(
                "LauncherAI's accessibility service is enabled but not running. This can be caused by system Battery optimization. " +
                    "Set LauncherAI to 'Don't optimize' and toggle the Accessibility service off and back on."
            )
            .setPositiveButton("Accessibility Settings") { _, _ ->
                openAccessibilitySettings(context)
            }
            .setNeutralButton("Battery Optimization") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } catch (_: Exception) {
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEnableDeviceAdminDialog(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("Screen lock")
            .setMessage("Device Admin screen lock requires Device Admin privileges in order to lock the screen.")
            .setPositiveButton("Enable") { _, _ ->
                try {
                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, getDeviceAdminComponent(context))
                        putExtra(
                            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                            "Allows LauncherAI to lock your screen when triggering the Screen lock gesture."
                        )
                    }
                    if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } catch (_: Exception) {
                    Toast.makeText(context, "Unable to open Device Admin prompt", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun openAccessibilitySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Toast.makeText(context, "Select LauncherAI", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(context, "Unable to open Accessibility settings", Toast.LENGTH_SHORT).show()
        }
    }
}
