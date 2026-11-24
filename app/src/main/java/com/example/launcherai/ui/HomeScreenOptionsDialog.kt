package com.example.launcherai.ui

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.example.launcherai.R
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * Nova Launcher-style Home Screen Options bottom sheet shown upon long-pressing
 * an empty area of the desktop.
 * Provides direct access to:
 *  - Wallpapers (system wallpaper picker)
 *  - Widgets (Nova categorized widget picker)
 *  - Settings (LauncherSettingsActivity)
 */
class HomeScreenOptionsDialog(
    context: Context,
    private val onWidgetsClick: () -> Unit
) : BottomSheetDialog(context, R.style.Theme_LauncherAI) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_home_screen_options)

        behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true

        val btnWallpaper = findViewById<View>(R.id.btnHomeOptionWallpaper)
        val btnWidgets = findViewById<View>(R.id.btnHomeOptionWidgets)
        val btnSettings = findViewById<View>(R.id.btnHomeOptionSettings)

        btnWallpaper?.setOnClickListener {
            dismiss()
            try {
                val intent = Intent(Intent.ACTION_SET_WALLPAPER)
                context.startActivity(Intent.createChooser(intent, "Select Wallpaper"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        btnWidgets?.setOnClickListener {
            dismiss()
            onWidgetsClick()
        }

        btnSettings?.setOnClickListener {
            dismiss()
            try {
                val intent = Intent(context, LauncherSettingsActivity::class.java)
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
