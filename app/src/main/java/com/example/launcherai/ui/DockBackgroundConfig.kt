package com.example.launcherai.ui

import android.content.Context
import android.graphics.Color

enum class DockCornerShape(val label: String) {
    CROP("Crop"),
    ROUND("Round"),
    SQUIRCLE("Squircle");

    companion object {
        fun fromName(name: String?): DockCornerShape {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: ROUND
        }
    }
}

data class DockCornerSpec(
    val radiusPercent: Int = 20,
    val shape: DockCornerShape = DockCornerShape.ROUND
)

/**
 * Dock Background configuration matching Nova Launcher's wy.c / SettingsDockBackground.
 */
data class DockBackgroundConfig(
    val enabled: Boolean = false,
    val color: Int = Color.parseColor("#1F1F2B"),
    val transparencyPercent: Int = 20,
    val horizontalPaddingDp: Int = 8,
    val drawBehindNavbar: Boolean = false,
    val topLeft: DockCornerSpec = DockCornerSpec(20, DockCornerShape.ROUND),
    val topRight: DockCornerSpec = DockCornerSpec(20, DockCornerShape.ROUND),
    val bottomLeft: DockCornerSpec = DockCornerSpec(20, DockCornerShape.ROUND),
    val bottomRight: DockCornerSpec = DockCornerSpec(20, DockCornerShape.ROUND)
) {
    fun getFillColorWithAlpha(): Int {
        val clampedTrans = transparencyPercent.coerceIn(0, 100)
        val alpha = ((100 - clampedTrans) * 255) / 100
        return (color and 0x00FFFFFF) or (alpha shl 24)
    }

    fun summaryText(): String {
        if (!enabled) return "Disabled"
        val opacity = 100 - transparencyPercent.coerceIn(0, 100)
        return "${topLeft.shape.label} · $opacity% opacity"
    }

    fun save(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putInt(KEY_COLOR, color)
            .putInt(KEY_TRANSPARENCY, transparencyPercent.coerceIn(0, 100))
            .putInt(KEY_PADDING_DP, horizontalPaddingDp)
            .putBoolean(KEY_DRAW_BEHIND_NAVBAR, drawBehindNavbar)
            .putInt(KEY_TL_RADIUS, topLeft.radiusPercent.coerceIn(0, 100))
            .putString(KEY_TL_SHAPE, topLeft.shape.name)
            .putInt(KEY_TR_RADIUS, topRight.radiusPercent.coerceIn(0, 100))
            .putString(KEY_TR_SHAPE, topRight.shape.name)
            .putInt(KEY_BL_RADIUS, bottomLeft.radiusPercent.coerceIn(0, 100))
            .putString(KEY_BL_SHAPE, bottomLeft.shape.name)
            .putInt(KEY_BR_RADIUS, bottomRight.radiusPercent.coerceIn(0, 100))
            .putString(KEY_BR_SHAPE, bottomRight.shape.name)
            .putString("key_dock_bg_style", if (enabled) "Custom" else "Transparent")
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "workspace_prefs"
        private const val KEY_ENABLED = "key_dock_bg_enabled"
        private const val KEY_COLOR = "key_dock_bg_color"
        private const val KEY_TRANSPARENCY = "key_dock_bg_transparency"
        private const val KEY_PADDING_DP = "key_dock_bg_padding_dp"
        private const val KEY_DRAW_BEHIND_NAVBAR = "key_dock_draw_behind_navbar"
        private const val KEY_TL_RADIUS = "key_dock_bg_tl_radius"
        private const val KEY_TL_SHAPE = "key_dock_bg_tl_shape"
        private const val KEY_TR_RADIUS = "key_dock_bg_tr_radius"
        private const val KEY_TR_SHAPE = "key_dock_bg_tr_shape"
        private const val KEY_BL_RADIUS = "key_dock_bg_bl_radius"
        private const val KEY_BL_SHAPE = "key_dock_bg_bl_shape"
        private const val KEY_BR_RADIUS = "key_dock_bg_br_radius"
        private const val KEY_BR_SHAPE = "key_dock_bg_br_shape"

        fun load(context: Context): DockBackgroundConfig {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val legacyStyle = prefs.getString("key_dock_bg_style", "Transparent") ?: "Transparent"
            val hasExplicitEnabled = prefs.contains(KEY_ENABLED)
            val enabled = if (hasExplicitEnabled) {
                prefs.getBoolean(KEY_ENABLED, false)
            } else {
                legacyStyle == "Glass" || legacyStyle == "Platform"
            }

            val defaultColor = if (legacyStyle == "Glass") Color.parseColor("#2A2A38") else Color.parseColor("#1F1F2B")
            val defaultTrans = if (legacyStyle == "Glass") 35 else 20

            return DockBackgroundConfig(
                enabled = enabled,
                color = prefs.getInt(KEY_COLOR, defaultColor),
                transparencyPercent = prefs.getInt(KEY_TRANSPARENCY, defaultTrans).coerceIn(0, 100),
                horizontalPaddingDp = prefs.getInt(KEY_PADDING_DP, 8),
                drawBehindNavbar = prefs.getBoolean(KEY_DRAW_BEHIND_NAVBAR, false),
                topLeft = DockCornerSpec(
                    radiusPercent = prefs.getInt(KEY_TL_RADIUS, 20).coerceIn(0, 100),
                    shape = DockCornerShape.fromName(prefs.getString(KEY_TL_SHAPE, DockCornerShape.ROUND.name))
                ),
                topRight = DockCornerSpec(
                    radiusPercent = prefs.getInt(KEY_TR_RADIUS, 20).coerceIn(0, 100),
                    shape = DockCornerShape.fromName(prefs.getString(KEY_TR_SHAPE, DockCornerShape.ROUND.name))
                ),
                bottomLeft = DockCornerSpec(
                    radiusPercent = prefs.getInt(KEY_BL_RADIUS, 20).coerceIn(0, 100),
                    shape = DockCornerShape.fromName(prefs.getString(KEY_BL_SHAPE, DockCornerShape.ROUND.name))
                ),
                bottomRight = DockCornerSpec(
                    radiusPercent = prefs.getInt(KEY_BR_RADIUS, 20).coerceIn(0, 100),
                    shape = DockCornerShape.fromName(prefs.getString(KEY_BR_SHAPE, DockCornerShape.ROUND.name))
                )
            )
        }
    }
}
