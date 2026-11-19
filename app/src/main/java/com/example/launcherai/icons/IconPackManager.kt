package com.example.launcherai.icons

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import org.xmlpull.v1.XmlPullParser

data class IconPackInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable?
)

/**
 * Manages discovery, parsing, and drawable lookup for third-party Android icon packs
 * (compatible with Nova, ADW, Go, and Atom icon pack standards).
 */
object IconPackManager {

    private val ICON_PACK_INTENTS = arrayOf(
        "com.novalauncher.THEME",
        "org.adw.launcher.THEME",
        "com.dlto.atom.launcher.THEME",
        "com.gau.go.launcherex.theme"
    )

    fun getInstalledIconPacks(context: Context): List<IconPackInfo> {
        val pm = context.packageManager
        val iconPacks = mutableListOf<IconPackInfo>()
        val addedPackages = mutableSetOf<String>()

        // Always add Default System Icons as option
        iconPacks.add(
            IconPackInfo(
                label = "Default System Icons",
                packageName = "",
                icon = pm.defaultActivityIcon
            )
        )

        for (action in ICON_PACK_INTENTS) {
            val intent = Intent(action)
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            for (ri in resolveInfos) {
                val pkg = ri.activityInfo.packageName
                if (addedPackages.add(pkg)) {
                    val label = ri.loadLabel(pm).toString()
                    val icon = ri.loadIcon(pm)
                    iconPacks.add(IconPackInfo(label, pkg, icon))
                }
            }
        }

        return iconPacks
    }

    fun getIconDrawable(
        context: Context,
        iconPackPackage: String,
        appPackageName: String,
        fallbackIcon: Drawable?
    ): Drawable? {
        if (iconPackPackage.isBlank()) return fallbackIcon
        val pm = context.packageManager
        try {
            val res = pm.getResourcesForApplication(iconPackPackage)
            val resourceName = appPackageName.lowercase().replace('.', '_')
            val resId = res.getIdentifier(resourceName, "drawable", iconPackPackage)
            if (resId != 0) {
                return res.getDrawable(resId, null)
            }
        } catch (e: Exception) {
            // Fallback gracefully to original app icon
        }
        return fallbackIcon
    }
}
