package com.example.launcherai.ui

import android.content.Context
import android.graphics.Color
import androidx.appcompat.app.AppCompatDelegate

/**
 * Nova Launcher-parity Theme & Style presets for the Style tab
 * (Bloom, Glass, Linen, Midnight, Mono, Paper).
 */
data class LauncherThemePreset(
    val id: String,
    val name: String,
    val description: String,
    val iconShape: String,
    val surfaceColor: Int,
    val textColor: Int,
    val accentColor: Int,
    val transparencyPercent: Int,
    val cornerRadiusDp: Int,
    val dockCornerPercent: Int,
    val nightMode: String // "SYSTEM", "LIGHT", "DARK"
)

object LauncherThemeManager {

    private const val PREFS_NAME = "workspace_prefs"
    const val KEY_ACTIVE_THEME_ID = "key_active_launcher_theme_id"
    const val KEY_THEME_MODE = "key_launcher_theme_mode"

    val PRESETS = listOf(
        LauncherThemePreset(
            id = "bloom",
            name = "Bloom",
            description = "Flower-shaped icons, rose colors",
            iconShape = "Flower",
            surfaceColor = Color.parseColor("#FCE4EC"),
            textColor = Color.parseColor("#4A1432"),
            accentColor = Color.parseColor("#C2185B"),
            transparencyPercent = 10,
            cornerRadiusDp = 20,
            dockCornerPercent = 24,
            nightMode = "LIGHT"
        ),
        LauncherThemePreset(
            id = "glass",
            name = "Glass",
            description = "Clear glass icons and surfaces",
            iconShape = "Squircle",
            surfaceColor = Color.parseColor("#242834"),
            textColor = Color.parseColor("#FFFFFF"),
            accentColor = Color.parseColor("#80D8FF"),
            transparencyPercent = 60,
            cornerRadiusDp = 20,
            dockCornerPercent = 28,
            nightMode = "SYSTEM"
        ),
        LauncherThemePreset(
            id = "linen",
            name = "Linen",
            description = "Warm woven linen, always light",
            iconShape = "Rounded square (Default)",
            surfaceColor = Color.parseColor("#EFE9E1"),
            textColor = Color.parseColor("#3E2723"),
            accentColor = Color.parseColor("#795548"),
            transparencyPercent = 5,
            cornerRadiusDp = 16,
            dockCornerPercent = 20,
            nightMode = "LIGHT"
        ),
        LauncherThemePreset(
            id = "midnight",
            name = "Midnight",
            description = "Navy night sky, always dark",
            iconShape = "Squircle",
            surfaceColor = Color.parseColor("#0F172A"),
            textColor = Color.parseColor("#E2E8F0"),
            accentColor = Color.parseColor("#60A5FA"),
            transparencyPercent = 10,
            cornerRadiusDp = 18,
            dockCornerPercent = 24,
            nightMode = "DARK"
        ),
        LauncherThemePreset(
            id = "mono",
            name = "Mono",
            description = "True black. Saves battery on OLED",
            iconShape = "Round",
            surfaceColor = Color.parseColor("#000000"),
            textColor = Color.parseColor("#FFFFFF"),
            accentColor = Color.parseColor("#B0B0B0"),
            transparencyPercent = 0,
            cornerRadiusDp = 16,
            dockCornerPercent = 20,
            nightMode = "DARK"
        ),
        LauncherThemePreset(
            id = "paper",
            name = "Paper",
            description = "Ink on paper, always light",
            iconShape = "Rounded square (Default)",
            surfaceColor = Color.parseColor("#FAFAFA"),
            textColor = Color.parseColor("#1A1A1A"),
            accentColor = Color.parseColor("#424242"),
            transparencyPercent = 0,
            cornerRadiusDp = 12,
            dockCornerPercent = 16,
            nightMode = "LIGHT"
        )
    )

    fun getActiveTheme(context: Context): LauncherThemePreset? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val id = prefs.getString(KEY_ACTIVE_THEME_ID, null) ?: return null
        return PRESETS.firstOrNull { it.id == id }
    }

    fun getThemeMode(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM"
    }

    fun setThemeMode(context: Context, mode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_MODE, mode)
            .apply()
    }

    fun applyTheme(context: Context, preset: LauncherThemePreset) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_ACTIVE_THEME_ID, preset.id)
            .putString(KEY_THEME_MODE, preset.nightMode)
            .putString("key_icon_shape", preset.iconShape)
            // Folder window colors
            .putInt(FolderSettingsActivity.KEY_FOLDER_WINDOW_BG_COLOR, preset.surfaceColor)
            .putInt(FolderSettingsActivity.KEY_FOLDER_WINDOW_BG_ALPHA, 100 - preset.transparencyPercent)
            .putInt(FolderSettingsActivity.KEY_FOLDER_WINDOW_TEXT_COLOR, preset.textColor)
            .putInt(FolderSettingsActivity.KEY_FOLDER_WINDOW_ACCENT_COLOR, preset.accentColor)
            .putInt(FolderSettingsActivity.KEY_FOLDER_WINDOW_CORNER_RADIUS, preset.cornerRadiusDp)
            .apply()

        // Search bars (Desktop, Drawer, Settings)
        listOf(
            SearchBarConfig.MODE_DESKTOP,
            SearchBarConfig.MODE_DRAWER,
            SearchBarConfig.MODE_NOVA_NOW
        ).forEach { mode ->
            val current = SearchBarConfig.load(prefs, mode)
            val updated = current.copy(
                surfaceColor = preset.surfaceColor,
                textColor = preset.textColor,
                accentColor = preset.accentColor,
                transparencyPercent = preset.transparencyPercent,
                cornerRadius = if (mode == SearchBarConfig.MODE_NOVA_NOW) preset.dockCornerPercent else preset.cornerRadiusDp.coerceAtMost(22)
            )
            SearchBarConfig.save(prefs, updated)
        }

        // Dock background
        val dockBg = DockBackgroundConfig.load(context).copy(
            enabled = true,
            color = preset.surfaceColor,
            transparencyPercent = preset.transparencyPercent,
            topLeft = DockCornerSpec(preset.dockCornerPercent, DockCornerShape.ROUND),
            topRight = DockCornerSpec(preset.dockCornerPercent, DockCornerShape.ROUND),
            bottomLeft = DockCornerSpec(preset.dockCornerPercent, DockCornerShape.ROUND),
            bottomRight = DockCornerSpec(preset.dockCornerPercent, DockCornerShape.ROUND)
        )
        dockBg.save(context)
    }

    fun clearTheme(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_ACTIVE_THEME_ID)
            .putString(KEY_THEME_MODE, "SYSTEM")
            .putString("key_icon_shape", "Rounded square (Default)")
            .apply()
    }

    fun applyThemeMode(mode: String) {
        when (mode) {
            "LIGHT" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "DARK" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "AUTO" -> {
                val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                val night = hour < 6 || hour >= 18
                AppCompatDelegate.setDefaultNightMode(
                    if (night) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                )
            }
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}
