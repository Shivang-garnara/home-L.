package com.example.launcherai.ui

import android.app.WallpaperManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import java.util.Calendar

/**
 * Nova Launcher-parity Style configuration:
 * - Global Palette (System, Local Wallpaper, Custom)
 * - Global Mode (Follow system, Light, Dark, Auto, Schedule)
 * - Per-Surface Style (Surface, Text + min contrast, Accent + min contrast)
 *   across all 10 launcher surfaces.
 */
object SurfaceStyleConfig {

    enum class PaletteType(val key: String, val label: String) {
        SYSTEM("SYSTEM", "System"),
        WALLPAPER("WALLPAPER", "Local Wallpaper"),
        CUSTOM("CUSTOM", "Custom");

        companion object {
            fun fromKey(key: String?): PaletteType =
                entries.firstOrNull { it.key == key } ?: SYSTEM
        }
    }

    enum class NightMode(val key: String, val label: String) {
        FOLLOW_SYSTEM("SYSTEM", "Follow system"),
        LIGHT("LIGHT", "Light"),
        DARK("DARK", "Dark"),
        AUTO("AUTO", "Auto"),
        SCHEDULE("SCHEDULE", "Schedule");

        companion object {
            fun fromKey(key: String?): NightMode =
                entries.firstOrNull { it.key == key } ?: FOLLOW_SYSTEM
        }
    }

    enum class SurfaceId(
        val key: String,
        val title: String,
        val showTextBadge: Boolean = true,
        val searchBarMode: String? = null
    ) {
        LAUNCHER_SETTINGS("launcher_settings", "Launcher Settings"),
        HOME_SCREEN("home_screen", "Home screen", showTextBadge = false),
        APP_DRAWER("app_drawer", "App drawer"),
        FOLDERS("folders", "Folders"),
        POPUP_MENU("popup_menu", "Popup menu"),
        LAUNCHER_AI("launcher_ai", "Launcher AI"),
        DESKTOP_SEARCH_BAR("desktop_search_bar", "Desktop search bar", searchBarMode = SearchBarConfig.MODE_DESKTOP),
        DRAWER_SEARCH_BAR("drawer_search_bar", "Drawer search bar", searchBarMode = SearchBarConfig.MODE_DRAWER),
        SETTINGS_SEARCH_BAR("settings_search_bar", "Settings search bar", searchBarMode = SearchBarConfig.MODE_NOVA_NOW);

        companion object {
            fun fromKey(key: String?): SurfaceId =
                entries.firstOrNull { it.key == key } ?: LAUNCHER_SETTINGS
        }
    }

    data class SurfaceStyle(
        val surfaceId: SurfaceId,
        val surfaceColor: Int,
        val textColor: Int,
        val accentColor: Int,
        val textMinContrast: Int,
        val accentMinContrast: Int,
        val hasCustomSurface: Boolean,
        val hasCustomText: Boolean,
        val hasCustomAccent: Boolean
    ) {
        fun effectiveTextColor(): Int =
            SearchBarConfig.resolveContrastColor(textColor, surfaceColor, textMinContrast)

        fun effectiveAccentColor(): Int =
            SearchBarConfig.resolveContrastColor(accentColor, surfaceColor, accentMinContrast)
    }

    private const val PREFS_NAME = "workspace_prefs"
    private const val KEY_PALETTE_TYPE = "style_palette_type"
    private const val KEY_CUSTOM_SEED = "style_custom_palette_seed"
    private const val KEY_SCHEDULE_START_HOUR = "style_schedule_start_hour"
    private const val KEY_SCHEDULE_START_MIN = "style_schedule_start_min"
    private const val KEY_SCHEDULE_END_HOUR = "style_schedule_end_hour"
    private const val KEY_SCHEDULE_END_MIN = "style_schedule_end_min"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getPaletteType(context: Context): PaletteType =
        PaletteType.fromKey(prefs(context).getString(KEY_PALETTE_TYPE, PaletteType.SYSTEM.key))

    fun setPaletteType(context: Context, type: PaletteType) {
        prefs(context).edit().putString(KEY_PALETTE_TYPE, type.key).apply()
    }

    fun getCustomSeedColor(context: Context): Int =
        prefs(context).getInt(KEY_CUSTOM_SEED, Color.parseColor("#3F51B5"))

    fun setCustomSeedColor(context: Context, color: Int) {
        prefs(context).edit().putInt(KEY_CUSTOM_SEED, color or 0xFF000000.toInt()).apply()
    }

    fun getNightMode(context: Context): NightMode =
        NightMode.fromKey(LauncherThemeManager.getThemeMode(context))

    fun setNightMode(context: Context, mode: NightMode) {
        LauncherThemeManager.setThemeMode(context, mode.key)
    }

    fun getScheduleTimes(context: Context): IntArray {
        val p = prefs(context)
        return intArrayOf(
            p.getInt(KEY_SCHEDULE_START_HOUR, 20),
            p.getInt(KEY_SCHEDULE_START_MIN, 0),
            p.getInt(KEY_SCHEDULE_END_HOUR, 7),
            p.getInt(KEY_SCHEDULE_END_MIN, 0)
        )
    }

    fun setScheduleTimes(context: Context, startHour: Int, startMin: Int, endHour: Int, endMin: Int) {
        prefs(context).edit()
            .putInt(KEY_SCHEDULE_START_HOUR, startHour.coerceIn(0, 23))
            .putInt(KEY_SCHEDULE_START_MIN, startMin.coerceIn(0, 59))
            .putInt(KEY_SCHEDULE_END_HOUR, endHour.coerceIn(0, 23))
            .putInt(KEY_SCHEDULE_END_MIN, endMin.coerceIn(0, 59))
            .apply()
    }

    fun isDarkEffective(context: Context): Boolean {
        return when (getNightMode(context)) {
            NightMode.LIGHT -> false
            NightMode.DARK -> true
            NightMode.AUTO -> {
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                hour < 6 || hour >= 18
            }
            NightMode.SCHEDULE -> {
                val times = getScheduleTimes(context)
                val now = Calendar.getInstance()
                val nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
                val startMin = times[0] * 60 + times[1]
                val endMin = times[2] * 60 + times[3]
                if (startMin <= endMin) {
                    nowMin in startMin until endMin
                } else {
                    nowMin >= startMin || nowMin < endMin
                }
            }
            NightMode.FOLLOW_SYSTEM -> {
                val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                uiMode == Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    /**
     * Returns a 36-color (6x6) tonal palette matching the active PaletteType:
     * rows 0-1: Primary accent tones (light -> dark)
     * rows 2-3: Neutral tones (light -> dark)
     * rows 4-5: Secondary/Tertiary accent tones (light -> dark)
     */
    fun getActivePaletteColors(context: Context): IntArray {
        return when (getPaletteType(context)) {
            PaletteType.SYSTEM -> buildSystemPalette(context)
            PaletteType.WALLPAPER -> buildWallpaperPalette(context)
            PaletteType.CUSTOM -> buildTonalPaletteFromSeeds(
                primary = getCustomSeedColor(context),
                secondary = shiftHue(getCustomSeedColor(context), 35f),
                neutral = desaturate(getCustomSeedColor(context), 0.12f)
            )
        }
    }

    private fun buildSystemPalette(context: Context): IntArray {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return try {
                val ids = intArrayOf(
                    android.R.color.system_accent1_10, android.R.color.system_accent1_50,
                    android.R.color.system_accent1_100, android.R.color.system_accent1_200,
                    android.R.color.system_accent1_300, android.R.color.system_accent1_400,
                    android.R.color.system_accent1_500, android.R.color.system_accent1_600,
                    android.R.color.system_accent1_700, android.R.color.system_accent1_800,
                    android.R.color.system_accent1_900, android.R.color.system_accent1_1000,

                    android.R.color.system_neutral1_10, android.R.color.system_neutral1_50,
                    android.R.color.system_neutral1_100, android.R.color.system_neutral1_200,
                    android.R.color.system_neutral1_300, android.R.color.system_neutral1_400,
                    android.R.color.system_neutral1_500, android.R.color.system_neutral1_600,
                    android.R.color.system_neutral1_700, android.R.color.system_neutral1_800,
                    android.R.color.system_neutral1_900, android.R.color.system_neutral1_1000,

                    android.R.color.system_accent3_10, android.R.color.system_accent3_50,
                    android.R.color.system_accent3_100, android.R.color.system_accent3_200,
                    android.R.color.system_accent3_300, android.R.color.system_accent3_400,
                    android.R.color.system_accent3_500, android.R.color.system_accent3_600,
                    android.R.color.system_accent3_700, android.R.color.system_accent3_800,
                    android.R.color.system_accent3_900, android.R.color.system_accent3_1000
                )
                IntArray(ids.size) { i -> ContextCompat.getColor(context, ids[i]) }
            } catch (_: Exception) {
                buildTonalPaletteFromSeeds(
                    Color.parseColor("#3F51B5"),
                    Color.parseColor("#8E24AA"),
                    Color.parseColor("#606470")
                )
            }
        }
        return buildTonalPaletteFromSeeds(
            Color.parseColor("#3F51B5"),
            Color.parseColor("#8E24AA"),
            Color.parseColor("#606470")
        )
    }

    private fun buildWallpaperPalette(context: Context): IntArray {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            try {
                val wm = WallpaperManager.getInstance(context)
                val colors = wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                if (colors != null) {
                    val primary = colors.primaryColor.toArgb()
                    val secondary = colors.secondaryColor?.toArgb() ?: shiftHue(primary, 35f)
                    val neutral = desaturate(primary, 0.14f)
                    return buildTonalPaletteFromSeeds(primary, secondary, neutral)
                }
            } catch (_: SecurityException) {
                // Storage/wallpaper permission not granted; fall back gracefully
            } catch (_: Exception) {
            }
        }
        return buildTonalPaletteFromSeeds(
            Color.parseColor("#4A6572"),
            Color.parseColor("#F9AA33"),
            Color.parseColor("#5C646B")
        )
    }

    private fun buildTonalPaletteFromSeeds(primary: Int, secondary: Int, neutral: Int): IntArray {
        val result = IntArray(36)
        val seeds = intArrayOf(primary, neutral, secondary)
        val lightnessSteps = floatArrayOf(
            0.98f, 0.94f, 0.87f, 0.77f, 0.67f, 0.56f,
            0.47f, 0.39f, 0.31f, 0.23f, 0.16f, 0.08f
        )
        for (group in 0 until 3) {
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(seeds[group], hsl)
            for (step in 0 until 12) {
                val outHsl = floatArrayOf(hsl[0], hsl[1], lightnessSteps[step])
                result[group * 12 + step] = ColorUtils.HSLToColor(outHsl)
            }
        }
        return result
    }

    private fun shiftHue(color: Int, degrees: Float): Int {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)
        hsl[0] = (hsl[0] + degrees) % 360f
        if (hsl[0] < 0f) hsl[0] += 360f
        return ColorUtils.HSLToColor(hsl)
    }

    private fun desaturate(color: Int, maxSat: Float): Int {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)
        hsl[1] = hsl[1].coerceAtMost(maxSat)
        return ColorUtils.HSLToColor(hsl)
    }

    private fun surfaceKey(id: SurfaceId, field: String): String = "style_surface_${id.key}_$field"

    fun loadSurfaceStyle(context: Context, id: SurfaceId): SurfaceStyle {
        val p = prefs(context)
        val palette = getActivePaletteColors(context)
        val dark = isDarkEffective(context)

        // Default surface, text, accent derived from active palette + mode
        val defaultSurface = when (id) {
            SurfaceId.HOME_SCREEN -> Color.TRANSPARENT
            SurfaceId.LAUNCHER_AI -> if (dark) palette[21] else palette[18]
            SurfaceId.SETTINGS_SEARCH_BAR -> if (dark) palette[22] else palette[21]
            SurfaceId.DESKTOP_SEARCH_BAR, SurfaceId.DRAWER_SEARCH_BAR ->
                if (dark) palette[21] else palette[2]
            else -> if (dark) palette[22] else palette[12]
        }
        val defaultText = when (id) {
            SurfaceId.HOME_SCREEN -> Color.WHITE
            SurfaceId.LAUNCHER_AI, SurfaceId.SETTINGS_SEARCH_BAR -> Color.WHITE
            else -> if (dark) palette[13] else palette[22]
        }
        val defaultAccent = if (dark) palette[4] else palette[8]

        // If linked to a SearchBarConfig mode, read from SearchBarConfig when no surface override exists
        val searchMode = id.searchBarMode
        if (searchMode != null) {
            val sb = SearchBarConfig.load(p, searchMode)
            val hasCustomSurf = p.contains(surfaceKey(id, "surface"))
            val hasCustomTxt = p.contains(surfaceKey(id, "text"))
            val hasCustomAcc = p.contains(surfaceKey(id, "accent"))
            return SurfaceStyle(
                surfaceId = id,
                surfaceColor = if (hasCustomSurf) p.getInt(surfaceKey(id, "surface"), sb.surfaceColor) else sb.surfaceColor,
                textColor = if (hasCustomTxt) p.getInt(surfaceKey(id, "text"), sb.textColor) else sb.textColor,
                accentColor = if (hasCustomAcc) p.getInt(surfaceKey(id, "accent"), sb.accentColor) else sb.accentColor,
                textMinContrast = sb.textMinContrast,
                accentMinContrast = sb.accentMinContrast,
                hasCustomSurface = hasCustomSurf,
                hasCustomText = hasCustomTxt,
                hasCustomAccent = hasCustomAcc
            )
        }

        val hasCustomSurf = p.contains(surfaceKey(id, "surface"))
        val hasCustomTxt = p.contains(surfaceKey(id, "text"))
        val hasCustomAcc = p.contains(surfaceKey(id, "accent"))

        return SurfaceStyle(
            surfaceId = id,
            surfaceColor = if (hasCustomSurf) p.getInt(surfaceKey(id, "surface"), defaultSurface) else defaultSurface,
            textColor = if (hasCustomTxt) p.getInt(surfaceKey(id, "text"), defaultText) else defaultText,
            accentColor = if (hasCustomAcc) p.getInt(surfaceKey(id, "accent"), defaultAccent) else defaultAccent,
            textMinContrast = p.getInt(surfaceKey(id, "text_contrast"), 40).coerceIn(20, 99),
            accentMinContrast = p.getInt(surfaceKey(id, "accent_contrast"), 40).coerceIn(20, 99),
            hasCustomSurface = hasCustomSurf,
            hasCustomText = hasCustomTxt,
            hasCustomAccent = hasCustomAcc
        )
    }

    fun saveSurfaceColor(context: Context, id: SurfaceId, color: Int) {
        val p = prefs(context)
        p.edit().putInt(surfaceKey(id, "surface"), color).apply()
        when (id) {
            SurfaceId.APP_DRAWER -> p.edit().putInt("key_drawer_background_color", color).apply()
            SurfaceId.FOLDERS -> p.edit().putInt(FolderSettingsActivity.KEY_FOLDER_WINDOW_BG_COLOR, color).apply()
            else -> Unit
        }
        id.searchBarMode?.let { mode ->
            val current = SearchBarConfig.load(p, mode)
            SearchBarConfig.save(p, current.copy(surfaceColor = color))
        }
    }

    fun saveTextColor(context: Context, id: SurfaceId, color: Int) {
        val p = prefs(context)
        p.edit().putInt(surfaceKey(id, "text"), color).apply()
        when (id) {
            SurfaceId.HOME_SCREEN -> p.edit().putInt("key_label_color", color).apply()
            SurfaceId.APP_DRAWER -> p.edit().putInt("key_drawer_label_color", color).apply()
            SurfaceId.FOLDERS -> p.edit()
                .putInt(FolderSettingsActivity.KEY_FOLDER_WINDOW_TEXT_COLOR, color)
                .putInt("key_folder_label_color", color)
                .apply()
            else -> Unit
        }
        id.searchBarMode?.let { mode ->
            val current = SearchBarConfig.load(p, mode)
            SearchBarConfig.save(p, current.copy(textColor = color))
        }
    }

    fun saveAccentColor(context: Context, id: SurfaceId, color: Int) {
        val p = prefs(context)
        p.edit().putInt(surfaceKey(id, "accent"), color).apply()
        when (id) {
            SurfaceId.HOME_SCREEN -> p.edit().putInt("key_page_indicator_color", color).apply()
            SurfaceId.APP_DRAWER -> p.edit().putInt("key_drawer_fastscroll_color", color).apply()
            SurfaceId.FOLDERS -> p.edit().putInt(FolderSettingsActivity.KEY_FOLDER_WINDOW_ACCENT_COLOR, color).apply()
            else -> Unit
        }
        id.searchBarMode?.let { mode ->
            val current = SearchBarConfig.load(p, mode)
            SearchBarConfig.save(p, current.copy(accentColor = color))
        }
    }

    fun saveTextMinContrast(context: Context, id: SurfaceId, contrast: Int) {
        val clamped = contrast.coerceIn(20, 99)
        val p = prefs(context)
        p.edit().putInt(surfaceKey(id, "text_contrast"), clamped).apply()
        val resolved = loadSurfaceStyle(context, id).effectiveTextColor()
        when (id) {
            SurfaceId.HOME_SCREEN -> p.edit().putInt("key_label_color", resolved).apply()
            SurfaceId.APP_DRAWER -> p.edit().putInt("key_drawer_label_color", resolved).apply()
            SurfaceId.FOLDERS -> p.edit()
                .putInt(FolderSettingsActivity.KEY_FOLDER_WINDOW_TEXT_COLOR, resolved)
                .putInt("key_folder_label_color", resolved)
                .apply()
            else -> Unit
        }
        id.searchBarMode?.let { mode ->
            val current = SearchBarConfig.load(p, mode)
            SearchBarConfig.save(p, current.copy(textMinContrast = clamped))
        }
    }

    fun saveAccentMinContrast(context: Context, id: SurfaceId, contrast: Int) {
        val clamped = contrast.coerceIn(20, 99)
        val p = prefs(context)
        p.edit().putInt(surfaceKey(id, "accent_contrast"), clamped).apply()
        val resolved = loadSurfaceStyle(context, id).effectiveAccentColor()
        when (id) {
            SurfaceId.HOME_SCREEN -> p.edit().putInt("key_page_indicator_color", resolved).apply()
            SurfaceId.APP_DRAWER -> p.edit().putInt("key_drawer_fastscroll_color", resolved).apply()
            SurfaceId.FOLDERS -> p.edit().putInt(FolderSettingsActivity.KEY_FOLDER_WINDOW_ACCENT_COLOR, resolved).apply()
            else -> Unit
        }
        id.searchBarMode?.let { mode ->
            val current = SearchBarConfig.load(p, mode)
            SearchBarConfig.save(p, current.copy(accentMinContrast = clamped))
        }
    }

    fun resetSurfaceStyle(context: Context, id: SurfaceId) {
        val editor = prefs(context).edit()
            .remove(surfaceKey(id, "surface"))
            .remove(surfaceKey(id, "text"))
            .remove(surfaceKey(id, "accent"))
            .remove(surfaceKey(id, "text_contrast"))
            .remove(surfaceKey(id, "accent_contrast"))
        when (id) {
            SurfaceId.HOME_SCREEN -> {
                editor.remove("key_label_color")
                editor.remove("key_page_indicator_color")
            }
            SurfaceId.APP_DRAWER -> {
                editor.remove("key_drawer_background_color")
                editor.remove("key_drawer_label_color")
                editor.remove("key_drawer_fastscroll_color")
            }
            SurfaceId.FOLDERS -> {
                editor.remove(FolderSettingsActivity.KEY_FOLDER_WINDOW_BG_COLOR)
                editor.remove(FolderSettingsActivity.KEY_FOLDER_WINDOW_TEXT_COLOR)
                editor.remove(FolderSettingsActivity.KEY_FOLDER_WINDOW_ACCENT_COLOR)
                editor.remove("key_folder_label_color")
            }
            else -> Unit
        }
        editor.apply()
    }
}
