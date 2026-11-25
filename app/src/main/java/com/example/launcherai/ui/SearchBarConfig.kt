package com.example.launcherai.ui

import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Configuration store & renderer helper for Nova-style Desktop Search Bar, Drawer Search Bar,
 * and Nova Now Search Bar.
 */
data class SearchBarConfig(
    val mode: String, // MODE_DESKTOP, MODE_DRAWER, MODE_NOVA_NOW
    val enabled: Boolean,
    val placement: String,
    val surfaceColor: Int,
    val textColor: Int,
    val accentColor: Int,
    val transparencyPercent: Int, // 0..100
    val isWireframe: Boolean,
    val hasShadow: Boolean,
    val cornerRadius: Int, // 0..22 dp (or 0..100% for Nova Now)
    val logoStyle: Int, // 0=Nova Dots, 1=Widget/Calendar Search, 2=Magnifier, 3=Google G
    val isMonochrome: Boolean,
    val orderedActions: List<String>,
    val enabledActions: Set<String>,
    val useGoogleAppDirectly: Boolean,
    val weatherUnits: String,
    val weatherCustomTab: Boolean,
    val weatherLocation: String,
    val assistantIcon: String,
    val dateFormat: String
) {
    companion object {
        const val MODE_DESKTOP = "desktop"
        const val MODE_DRAWER = "drawer"
        const val MODE_NOVA_NOW = "novanow"

        const val LOGO_NOVA_DOTS = 0
        const val LOGO_CALENDAR_SEARCH = 1
        const val LOGO_MAGNIFIER = 2
        const val LOGO_GOOGLE_G = 3

        const val ACTION_WEATHER = "weather"
        const val ACTION_VOICE_SEARCH = "voice_search"
        const val ACTION_ASSISTANT = "assistant"
        const val ACTION_PLAY_STORE = "play_store"
        const val ACTION_GOOGLE_APP = "google_app"
        const val ACTION_NOVA_SEARCH = "nova_search"
        const val ACTION_APP_DRAWER = "app_drawer"
        const val ACTION_DATE = "date"
        const val ACTION_GOOGLE_LENS = "google_lens"

        val DEFAULT_DESKTOP_ACTIONS = listOf(
            ACTION_WEATHER,
            ACTION_VOICE_SEARCH,
            ACTION_ASSISTANT,
            ACTION_PLAY_STORE,
            ACTION_GOOGLE_APP,
            ACTION_APP_DRAWER,
            ACTION_DATE,
            ACTION_GOOGLE_LENS
        )

        val DEFAULT_DRAWER_ACTIONS = listOf(
            ACTION_WEATHER,
            ACTION_VOICE_SEARCH,
            ACTION_ASSISTANT,
            ACTION_PLAY_STORE,
            ACTION_GOOGLE_APP,
            ACTION_DATE,
            ACTION_GOOGLE_LENS
        )

        val SYSTEM_PALETTE_COLORS = intArrayOf(
            Color.parseColor("#FFFFFF"),
            Color.parseColor("#FDF1F7"),
            Color.parseColor("#FAD7EC"),
            Color.parseColor("#F4A4DF"),
            Color.parseColor("#D893CE"),
            Color.parseColor("#B574AA"),
            Color.parseColor("#905482"),
            Color.parseColor("#6D3B68"),
            Color.parseColor("#4D2349"),
            Color.parseColor("#3E2A38"),
            Color.parseColor("#2E142D"),
            Color.parseColor("#1D0A1C")
        )

        val FIXED_PALETTE_COLORS = intArrayOf(
            Color.parseColor("#FFFFFF"), Color.parseColor("#EAEAEA"), Color.parseColor("#CCCCCC"),
            Color.parseColor("#999999"), Color.parseColor("#666666"), Color.parseColor("#000000"),
            Color.parseColor("#FFEBEE"), Color.parseColor("#FFCDD2"), Color.parseColor("#EF9A9A"),
            Color.parseColor("#E53935"), Color.parseColor("#B71C1C"), Color.parseColor("#5C0E0E"),
            Color.parseColor("#FFF3E0"), Color.parseColor("#FFE0B2"), Color.parseColor("#FFCC80"),
            Color.parseColor("#FB8C00"), Color.parseColor("#E65100"), Color.parseColor("#5D2800"),
            Color.parseColor("#E8F5E9"), Color.parseColor("#C8E6C9"), Color.parseColor("#A5D6A7"),
            Color.parseColor("#43A047"), Color.parseColor("#1B5E20"), Color.parseColor("#0D3310"),
            Color.parseColor("#E3F2FD"), Color.parseColor("#BBDEFB"), Color.parseColor("#90CAF9"),
            Color.parseColor("#1E88E5"), Color.parseColor("#0D47A1"), Color.parseColor("#07224E"),
            Color.parseColor("#F3E5F5"), Color.parseColor("#E1BEE7"), Color.parseColor("#CE93D8"),
            Color.parseColor("#8E24AA"), Color.parseColor("#4A148C"), Color.parseColor("#2A084E")
        )

        private fun key(mode: String, field: String): String = "searchbar_${mode}_$field"

        fun load(prefs: SharedPreferences, mode: String): SearchBarConfig {
            val defaultActions = if (mode == MODE_DESKTOP) DEFAULT_DESKTOP_ACTIONS else DEFAULT_DRAWER_ACTIONS

            // Maintain backwards-compatibility with legacy SearchSettingsActivity / MainActivity keys
            val placement = when (mode) {
                MODE_DESKTOP -> prefs.getString("key_search_bar_placement_home", "DOCK_BELOW") ?: "DOCK_BELOW"
                MODE_DRAWER -> prefs.getString("key_search_bar_placement_drawer", "Top") ?: "Top"
                else -> "Enabled"
            }

            val enabled = when (mode) {
                MODE_NOVA_NOW -> prefs.getBoolean("key_nova_now_bar_enabled", true)
                else -> true
            }

            val defaultSurface = if (mode == MODE_NOVA_NOW) Color.parseColor("#3E2A38") else Color.parseColor("#F9E7F1")
            val defaultText = if (mode == MODE_NOVA_NOW) Color.parseColor("#D6C6CF") else Color.parseColor("#3E2A38")
            val defaultAccent = if (mode == MODE_NOVA_NOW) Color.parseColor("#D893CE") else Color.parseColor("#6D3B68")

            val surfaceColor = prefs.getInt(key(mode, "surface"), defaultSurface)
            val textColor = prefs.getInt(key(mode, "text"), defaultText)
            val accentColor = prefs.getInt(key(mode, "accent"), defaultAccent)
            val transparency = prefs.getInt(key(mode, "transparency"), 0).coerceIn(0, 100)
            val isWireframe = if (mode == MODE_DESKTOP) {
                prefs.getBoolean(SearchSettingsActivity.KEY_HOME_BAR_WIREFRAME, prefs.getBoolean(key(mode, "wireframe"), false))
            } else {
                prefs.getBoolean(key(mode, "wireframe"), false)
            }
            val hasShadow = if (mode == MODE_DESKTOP) {
                prefs.getBoolean(SearchSettingsActivity.KEY_HOME_BAR_SHADOW, prefs.getBoolean(key(mode, "shadow"), true))
            } else {
                prefs.getBoolean(key(mode, "shadow"), true)
            }
            val cornerRadius = when (mode) {
                MODE_DESKTOP -> prefs.getInt(SearchSettingsActivity.KEY_HOME_BAR_RADIUS, 20).coerceIn(0, 22)
                MODE_DRAWER -> prefs.getInt(SearchSettingsActivity.KEY_DRAWER_BAR_RADIUS, 22).coerceIn(0, 22)
                else -> prefs.getInt(SearchSettingsActivity.KEY_NOVA_NOW_BAR_RADIUS, 25).coerceIn(0, 100)
            }

            val useGoogleDirect = if (mode == MODE_DESKTOP) {
                val legacyInterface = prefs.getString(SearchSettingsActivity.KEY_HOME_BAR_INTERFACE, "Nova Search") ?: "Nova Search"
                prefs.getBoolean(key(mode, "use_google_direct"), legacyInterface.contains("Google App", ignoreCase = true))
            } else {
                false
            }

            val defaultLogo = if (useGoogleDirect) LOGO_GOOGLE_G else if (mode == MODE_NOVA_NOW) LOGO_MAGNIFIER else LOGO_NOVA_DOTS
            val logoStyle = prefs.getInt(key(mode, "logo"), defaultLogo)
            val isMonochrome = prefs.getBoolean(key(mode, "monochrome"), true)

            val savedOrderRaw = prefs.getString(key(mode, "action_order"), null)
            val orderedActions = if (savedOrderRaw.isNullOrBlank()) {
                defaultActions
            } else {
                val parsed = savedOrderRaw.split(",").filter { it.isNotBlank() }.toMutableList()
                for (def in defaultActions) {
                    if (!parsed.contains(def)) parsed.add(def)
                }
                parsed
            }

            val defaultEnabled = if (mode == MODE_DESKTOP) setOf(ACTION_VOICE_SEARCH, ACTION_APP_DRAWER) else setOf(ACTION_VOICE_SEARCH)
            val enabledActions = prefs.getStringSet(key(mode, "enabled_actions"), defaultEnabled) ?: defaultEnabled

            val weatherUnits = prefs.getString(key(mode, "weather_units"), "Fahrenheit") ?: "Fahrenheit"
            val weatherCustomTab = prefs.getBoolean(key(mode, "weather_custom_tab"), true)
            val weatherLocation = prefs.getString(key(mode, "weather_location"), "Auto") ?: "Auto"
            val assistantIcon = prefs.getString(key(mode, "assistant_icon"), "Assistant") ?: "Assistant"
            val dateFormat = prefs.getString(key(mode, "date_format"), "January 31") ?: "January 31"

            return SearchBarConfig(
                mode = mode,
                enabled = enabled,
                placement = placement,
                surfaceColor = surfaceColor,
                textColor = textColor,
                accentColor = accentColor,
                transparencyPercent = transparency,
                isWireframe = isWireframe,
                hasShadow = hasShadow,
                cornerRadius = cornerRadius,
                logoStyle = logoStyle,
                isMonochrome = isMonochrome,
                orderedActions = orderedActions,
                enabledActions = enabledActions,
                useGoogleAppDirectly = useGoogleDirect,
                weatherUnits = weatherUnits,
                weatherCustomTab = weatherCustomTab,
                weatherLocation = weatherLocation,
                assistantIcon = assistantIcon,
                dateFormat = dateFormat
            )
        }

        fun save(prefs: SharedPreferences, config: SearchBarConfig) {
            val m = config.mode
            val ed = prefs.edit()

            when (m) {
                MODE_DESKTOP -> {
                    ed.putString("key_search_bar_placement_home", config.placement)
                    ed.putInt(SearchSettingsActivity.KEY_HOME_BAR_RADIUS, config.cornerRadius)
                    ed.putBoolean(SearchSettingsActivity.KEY_HOME_BAR_SHADOW, config.hasShadow)
                    ed.putBoolean(SearchSettingsActivity.KEY_HOME_BAR_WIREFRAME, config.isWireframe)
                    ed.putString(
                        SearchSettingsActivity.KEY_HOME_BAR_INTERFACE,
                        if (config.useGoogleAppDirectly) "Directly use Google App" else "Nova Search"
                    )
                }
                MODE_DRAWER -> {
                    ed.putString("key_search_bar_placement_drawer", config.placement)
                    ed.putInt(SearchSettingsActivity.KEY_DRAWER_BAR_RADIUS, config.cornerRadius)
                }
                MODE_NOVA_NOW -> {
                    ed.putBoolean("key_nova_now_bar_enabled", config.enabled)
                    ed.putInt(SearchSettingsActivity.KEY_NOVA_NOW_BAR_RADIUS, config.cornerRadius)
                }
            }

            ed.putInt(key(m, "surface"), config.surfaceColor)
            ed.putInt(key(m, "text"), config.textColor)
            ed.putInt(key(m, "accent"), config.accentColor)
            ed.putInt(key(m, "transparency"), config.transparencyPercent)
            ed.putBoolean(key(m, "wireframe"), config.isWireframe)
            ed.putBoolean(key(m, "shadow"), config.hasShadow)
            ed.putInt(key(m, "logo"), config.logoStyle)
            ed.putBoolean(key(m, "monochrome"), config.isMonochrome)
            ed.putString(key(m, "action_order"), config.orderedActions.joinToString(","))
            ed.putStringSet(key(m, "enabled_actions"), config.enabledActions)
            ed.putBoolean(key(m, "use_google_direct"), config.useGoogleAppDirectly)

            ed.putString(key(m, "weather_units"), config.weatherUnits)
            ed.putBoolean(key(m, "weather_custom_tab"), config.weatherCustomTab)
            ed.putString(key(m, "weather_location"), config.weatherLocation)
            ed.putString(key(m, "assistant_icon"), config.assistantIcon)
            ed.putString(key(m, "date_format"), config.dateFormat)
            ed.apply()
        }

        fun formatDate(formatChoice: String): String {
            val pattern = when (formatChoice) {
                "Jan 31" -> "MMM d"
                "1/31" -> "M/d"
                else -> "MMMM d"
            }
            return SimpleDateFormat(pattern, Locale.getDefault()).format(Date())
        }

        fun buildBarBackground(config: SearchBarConfig, density: Float, barHeightPx: Int = (52 * density).toInt()): GradientDrawable {
            val alpha = ((100 - config.transparencyPercent) * 255 / 100).coerceIn(0, 255)
            val effectiveSurface = Color.argb(
                alpha,
                Color.red(config.surfaceColor),
                Color.green(config.surfaceColor),
                Color.blue(config.surfaceColor)
            )
            val radiusPx = if (config.mode == MODE_NOVA_NOW) {
                (barHeightPx / 2f) * (config.cornerRadius / 100f)
            } else {
                config.cornerRadius * density
            }

            return GradientDrawable().apply {
                cornerRadius = radiusPx
                if (config.isWireframe) {
                    setColor(Color.TRANSPARENT)
                    val borderAlpha = alpha.coerceAtLeast(180)
                    val strokeColor = Color.argb(
                        borderAlpha,
                        Color.red(config.accentColor),
                        Color.green(config.accentColor),
                        Color.blue(config.accentColor)
                    )
                    setStroke((1.5f * density).toInt().coerceAtLeast(2), strokeColor)
                } else {
                    setColor(effectiveSurface)
                }
            }
        }
    }
}
