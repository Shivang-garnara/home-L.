package com.example.launcherai.ui

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import com.example.launcherai.MainActivity
import com.example.launcherai.R
import com.example.launcherai.ai.ui.NovaAiChatActivity

/**
 * Applies SurfaceId.LAUNCHER_SETTINGS (Surface, Text, Accent) across all sub-settings activities
 * (HomeScreenSettingsActivity, AppDrawerSettingsActivity, FolderSettingsActivity, SearchSettingsActivity,
 * DockSettingsActivity, LookFeelSettingsActivity, GesturesInputsSettingsActivity, SurfaceStyleActivity, etc.).
 */
object LauncherSettingsThemeApplier {

    private val SKIP_CLASSES = setOf(
        MainActivity::class.java.name,
        SetupActivity::class.java.name,
        LauncherSettingsActivity::class.java.name, // has its own fine-grained applier
        NovaAiChatActivity::class.java.name        // styled by SurfaceId.LAUNCHER_AI
    )

    private val SKIP_VIEW_IDS = setOf(
        R.id.containerSearchPill,
        R.id.viewCustomColorSwatch,
        R.id.gridActivePalette,
        R.id.gridFixedColors,
        R.id.rowAccentPalette,
        R.id.layoutContrastSwatches,
        R.id.btnContrastMinus,
        R.id.btnContrastPlus
    )

    fun applyIfSettingsActivity(activity: Activity) {
        if (activity.javaClass.name in SKIP_CLASSES) return
        val style = SurfaceStyleConfig.loadSurfaceStyle(activity, SurfaceStyleConfig.SurfaceId.LAUNCHER_SETTINGS)
        // Only override default XML colors when user has customized Launcher Settings or Palette/Mode
        val paletteType = SurfaceStyleConfig.getPaletteType(activity)
        val nightMode = SurfaceStyleConfig.getNightMode(activity)
        val shouldApply = style.hasCustomSurface ||
            style.hasCustomText ||
            style.hasCustomAccent ||
            paletteType != SurfaceStyleConfig.PaletteType.SYSTEM ||
            nightMode != SurfaceStyleConfig.NightMode.FOLLOW_SYSTEM
        if (!shouldApply) return

        apply(activity, style)
    }

    fun apply(activity: Activity, style: SurfaceStyleConfig.SurfaceStyle) {
        val surface = style.surfaceColor
        val textPrimary = style.effectiveTextColor()
        val textSecondary = ColorUtils.setAlphaComponent(textPrimary, 178)
        val accent = style.effectiveAccentColor()
        val isLight = ColorUtils.calculateLuminance(surface) > 0.5
        val cardSurface = ColorUtils.blendARGB(surface, accent, 0.08f)

        SystemBarHelper.applySystemBars(activity, isLightTheme = isLight, transparentBars = false)
        @Suppress("DEPRECATION")
        activity.window.statusBarColor = surface
        @Suppress("DEPRECATION")
        activity.window.navigationBarColor = surface

        val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        val root = content.getChildAt(0) ?: return
        root.setBackgroundColor(surface)

        val density = activity.resources.displayMetrics.density
        val scaledDensity = activity.resources.displayMetrics.scaledDensity
        applyToViewTree(root, surface, cardSurface, textPrimary, textSecondary, accent, density, scaledDensity)
    }

    private fun applyToViewTree(
        view: View,
        surface: Int,
        cardSurface: Int,
        textPrimary: Int,
        textSecondary: Int,
        accent: Int,
        density: Float,
        scaledDensity: Float
    ) {
        if (view.id in SKIP_VIEW_IDS) return
        val idName = if (view.id != View.NO_ID) {
            try { view.resources.getResourceEntryName(view.id) } catch (_: Exception) { "" }
        } else ""
        if (idName.contains("Swatch", ignoreCase = true) ||
            idName.contains("Preview", ignoreCase = true) ||
            idName.contains("ColorBox", ignoreCase = true) ||
            idName.contains("SearchBarPreview", ignoreCase = true)
        ) {
            return
        }

        // Tint card containers that have a solid background drawable
        if (view is ViewGroup && view.background is ColorDrawable) {
            view.setBackgroundColor(surface)
        } else if (view is ViewGroup && view.background is GradientDrawable && view.childCount > 0) {
            (view.background.mutate() as? GradientDrawable)?.setColor(cardSurface)
        }

        when (view) {
            is CompoundButton -> {
                view.setTextColor(textPrimary)
                view.buttonTintList = ColorStateList.valueOf(accent)
                if (view is androidx.appcompat.widget.SwitchCompat) {
                    view.thumbTintList = ColorStateList.valueOf(accent)
                    view.trackTintList = ColorStateList.valueOf(ColorUtils.setAlphaComponent(accent, 90))
                }
            }
            is SeekBar -> {
                view.progressTintList = ColorStateList.valueOf(accent)
                view.thumbTintList = ColorStateList.valueOf(accent)
            }
            is EditText -> {
                view.setTextColor(textPrimary)
                view.setHintTextColor(textSecondary)
            }
            is TextView -> {
                val sp = view.textSize / scaledDensity
                val isBold = view.typeface?.isBold == true || view.typeface?.style == Typeface.BOLD
                when {
                    sp <= 13.5f && isBold -> view.setTextColor(accent) // section header
                    sp >= 15f -> view.setTextColor(textPrimary)        // title / toolbar title
                    else -> view.setTextColor(textSecondary)           // summary / caption
                }
            }
            is ImageButton -> {
                view.setColorFilter(textPrimary)
            }
            is ImageView -> {
                // Skip palette quadrant icon or launcher app icons
                if (idName != "ivPaletteIcon" && !idName.contains("Logo", ignoreCase = true)) {
                    view.setColorFilter(accent)
                }
            }
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applyToViewTree(
                    view.getChildAt(i),
                    surface,
                    cardSurface,
                    textPrimary,
                    textSecondary,
                    accent,
                    density,
                    scaledDensity
                )
            }
        }
    }
}
