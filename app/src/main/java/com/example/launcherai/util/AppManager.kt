package com.example.launcherai.util

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.example.launcherai.R
import com.example.launcherai.data.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

object AppManager {

    suspend fun getInstalledApps(context: Context): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }

        val apps = mutableListOf<AppInfo>()
        for (info in resolveInfos) {
            val label = info.loadLabel(pm).toString()
            val packageName = info.activityInfo.packageName
            val activityName = info.activityInfo.name
            val icon = try {
                val prefs = context.getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                val preferLegacy71 = prefs.getBoolean("key_android_71_icons", false)
                var resolvedIcon: android.graphics.drawable.Drawable? = null
                if (preferLegacy71) {
                    try {
                        val roundIconField = info.activityInfo.applicationInfo.javaClass.getField("roundIcon")
                        val roundIconResId = roundIconField.getInt(info.activityInfo.applicationInfo)
                        if (roundIconResId != 0) {
                            resolvedIcon = pm.getResourcesForApplication(info.activityInfo.applicationInfo)
                                .getDrawable(roundIconResId, null)
                        }
                    } catch (e: Exception) {
                        // roundIcon not available or failed
                    }
                }
                resolvedIcon ?: info.loadIcon(pm)
            } catch (e: Exception) {
                pm.defaultActivityIcon
            }

            val category = resolveCategory(info.activityInfo.applicationInfo, packageName)

            // Exclude self from launcher list if desired, or keep
            apps.add(AppInfo(
                label = label,
                packageName = packageName,
                activityName = activityName,
                icon = icon,
                isSystem = (info.activityInfo.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0,
                category = category
            ))
        }

        apps.sortBy { it.label.lowercase() }
        apps
    }

    private fun resolveCategory(appInfo: android.content.pm.ApplicationInfo, packageName: String): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when (appInfo.category) {
                android.content.pm.ApplicationInfo.CATEGORY_GAME -> return AppInfo.CATEGORY_GAMES
                android.content.pm.ApplicationInfo.CATEGORY_AUDIO,
                android.content.pm.ApplicationInfo.CATEGORY_VIDEO -> return AppInfo.CATEGORY_ENTERTAINMENT
                android.content.pm.ApplicationInfo.CATEGORY_IMAGE -> return AppInfo.CATEGORY_PHOTOGRAPHY
                android.content.pm.ApplicationInfo.CATEGORY_SOCIAL,
                android.content.pm.ApplicationInfo.CATEGORY_PRODUCTIVITY -> return AppInfo.CATEGORY_PRODUCTIVITY
                android.content.pm.ApplicationInfo.CATEGORY_MAPS,
                android.content.pm.ApplicationInfo.CATEGORY_NEWS -> return AppInfo.CATEGORY_COMMUNICATION
            }
        }
        val lowerPkg = packageName.lowercase()
        return when {
            lowerPkg.contains("game") || lowerPkg.contains("play") -> AppInfo.CATEGORY_GAMES
            lowerPkg.contains("chat") || lowerPkg.contains("messag") || lowerPkg.contains("mail") || lowerPkg.contains("dial") || lowerPkg.contains("phone") -> AppInfo.CATEGORY_COMMUNICATION
            lowerPkg.contains("media") || lowerPkg.contains("video") || lowerPkg.contains("music") || lowerPkg.contains("youtube") -> AppInfo.CATEGORY_ENTERTAINMENT
            lowerPkg.contains("camera") || lowerPkg.contains("photo") || lowerPkg.contains("gallery") -> AppInfo.CATEGORY_PHOTOGRAPHY
            lowerPkg.contains("doc") || lowerPkg.contains("note") || lowerPkg.contains("office") || lowerPkg.contains("drive") -> AppInfo.CATEGORY_PRODUCTIVITY
            lowerPkg.contains("setting") || lowerPkg.contains("tool") || lowerPkg.contains("calc") || lowerPkg.contains("clock") -> AppInfo.CATEGORY_TOOLS
            else -> AppInfo.CATEGORY_OTHER
        }
    }

    fun launchApp(context: Context, app: AppInfo, sourceView: View? = null) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            } ?: Intent().apply {
                setClassName(app.packageName, app.activityName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val optsBundle = buildLaunchOptions(context, sourceView)
            if (optsBundle != null) {
                context.startActivity(intent, optsBundle)
            } else {
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot launch ${app.label}: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun buildLaunchOptions(context: Context, sourceView: View?): Bundle? {
        val prefs = context.getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("key_disable_animations", false)) {
            return ActivityOptions.makeCustomAnimation(context, 0, 0).toBundle()
        }
        val anim = prefs.getString("key_app_animation", "System default") ?: "System default"
        return when (anim.lowercase(Locale.ROOT)) {
            "zoom" -> {
                if (sourceView != null && sourceView.width > 0 && sourceView.height > 0) {
                    ActivityOptions.makeScaleUpAnimation(sourceView, 0, 0, sourceView.width, sourceView.height).toBundle()
                } else {
                    ActivityOptions.makeCustomAnimation(context, R.anim.blink_enter, R.anim.blink_exit).toBundle()
                }
            }
            "slide up" -> ActivityOptions.makeCustomAnimation(context, R.anim.slide_up_in, R.anim.slide_up_out).toBundle()
            "blink" -> ActivityOptions.makeCustomAnimation(context, R.anim.blink_enter, R.anim.blink_exit).toBundle()
            "system default" -> {
                if (sourceView != null && sourceView.width > 0 && sourceView.height > 0) {
                    ActivityOptions.makeClipRevealAnimation(sourceView, 0, 0, sourceView.width, sourceView.height).toBundle()
                } else null
            }
            else -> null
        }
    }

    fun launchSystemAction(context: Context, action: String) {
        try {
            val intent = when (action) {
                "phone" -> Intent(Intent.ACTION_DIAL)
                "messages" -> Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_MESSAGING) }
                "browser" -> Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_BROWSER) }
                "camera" -> Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                "settings" -> Intent(android.provider.Settings.ACTION_SETTINGS)
                else -> null
            }
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Action unavailable on this device", Toast.LENGTH_SHORT).show()
        }
    }

    fun openAppDetails(context: Context, packageName: String) {
        try {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "App Info unavailable: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun uninstallApp(context: Context, packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = android.net.Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Uninstall unavailable: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
