package com.example.launcherai.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import com.example.launcherai.R
import com.example.launcherai.data.model.AppInfo
import com.example.launcherai.databinding.LayoutAppItemPopupBinding
import com.example.launcherai.util.AppManager

/**
 * Floating popup menu anchored to app icons on the Desktop and in the App Drawer.
 * 100% parity with Nova Launcher 8.9.9's PopupContainerWithArrow:
 * - Dynamically renders only the actions enabled in the Action Matrix (Home vs Drawer)
 * - Adheres to user-selected Corner Radius (0-28dp) and Card Background preferences
 * - Handles App Info, Edit, Widgets, Share, Remove (with Undo Bar), Uninstall, Play Store, Force Stop, and Hide
 */
class AppItemPopupMenu(
    private val context: Context,
    private val anchorView: View,
    private val appInfo: AppInfo,
    private val isDrawer: Boolean = false,
    private val onRemoveClick: (() -> Unit)? = null,
    private val onEditClick: (() -> Unit)? = null,
    private val onHideClick: (() -> Unit)? = null,
    private val onWidgetsClick: (() -> Unit)? = null
) {
    private val binding = LayoutAppItemPopupBinding.inflate(LayoutInflater.from(context))
    private val popupWindow: PopupWindow

    init {
        popupWindow = PopupWindow(
            binding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            isOutsideTouchable = true
            isFocusable = true
            elevation = 20f * context.resources.displayMetrics.density
        }

        applyStylingAndPreferences()
        setupActionListeners()
    }

    private fun applyStylingAndPreferences() {
        val prefs = context.getSharedPreferences(PopupMenuSettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)

        // 1. Corner radius & background styling
        val cornerRadiusDp = prefs.getInt(
            PopupMenuSettingsActivity.KEY_POPUP_CORNER_RADIUS,
            PopupMenuSettingsActivity.DEFAULT_CORNER_RADIUS_DP
        )
        val density = context.resources.displayMetrics.density
        val radiusPx = cornerRadiusDp * density
        val hasCardBg = prefs.getBoolean(PopupMenuSettingsActivity.KEY_POPUP_CARD_BG, true)

        val popupStyle = SurfaceStyleConfig.loadSurfaceStyle(context, SurfaceStyleConfig.SurfaceId.POPUP_MENU)
        val bgDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusPx
            if (popupStyle.hasCustomSurface) {
                setColor(popupStyle.surfaceColor)
                setStroke((1 * density).toInt(), androidx.core.graphics.ColorUtils.setAlphaComponent(popupStyle.effectiveAccentColor(), 80))
            } else if (hasCardBg) {
                setColor(ContextCompat.getColor(context, R.color.card_bg))
                setStroke((1 * density).toInt(), ContextCompat.getColor(context, R.color.card_stroke))
            } else {
                setColor(Color.parseColor("#E612151E"))
                setStroke(0, Color.TRANSPARENT)
            }
        }
        binding.popupContainer.background = bgDrawable
        if (popupStyle.hasCustomText || popupStyle.hasCustomAccent || popupStyle.hasCustomSurface) {
            applyPopupColorsRecursive(
                binding.popupContainer,
                popupStyle.effectiveTextColor(),
                popupStyle.effectiveAccentColor()
            )
        }

        // 2. Action matrix resolution (Home vs Drawer)
        val activeActions = if (isDrawer) {
            prefs.getStringSet(
                PopupMenuSettingsActivity.KEY_POPUP_ACTIONS_DRAWER,
                PopupMenuSettingsActivity.DEFAULT_ACTIONS_DRAWER
            ) ?: PopupMenuSettingsActivity.DEFAULT_ACTIONS_DRAWER
        } else {
            prefs.getStringSet(
                PopupMenuSettingsActivity.KEY_POPUP_ACTIONS_HOME,
                PopupMenuSettingsActivity.DEFAULT_ACTIONS_HOME
            ) ?: PopupMenuSettingsActivity.DEFAULT_ACTIONS_HOME
        }

        // Header Actions
        binding.btnActionInfo.visibility = if ("App info" in activeActions) View.VISIBLE else View.GONE
        binding.btnActionEdit.visibility = if ("Edit" in activeActions) View.VISIBLE else View.GONE
        binding.btnActionWidgets.visibility = if ("Widgets" in activeActions) View.VISIBLE else View.GONE
        binding.btnActionShare.visibility = if ("Share" in activeActions) View.VISIBLE else View.GONE

        val hasAnyHeader = binding.btnActionInfo.visibility == View.VISIBLE ||
                binding.btnActionEdit.visibility == View.VISIBLE ||
                binding.btnActionWidgets.visibility == View.VISIBLE ||
                binding.btnActionShare.visibility == View.VISIBLE

        binding.layoutHeaderActions.visibility = if (hasAnyHeader) View.VISIBLE else View.GONE
        binding.dividerHeader.visibility = if (hasAnyHeader) View.VISIBLE else View.GONE

        // Vertical Actions
        binding.rowActionRemove.visibility = if ("Remove" in activeActions && !isDrawer) View.VISIBLE else View.GONE
        binding.rowActionUninstall.visibility = if ("Uninstall" in activeActions) View.VISIBLE else View.GONE
        binding.rowActionPlayStore.visibility = if ("Play Store" in activeActions) View.VISIBLE else View.GONE
        binding.rowActionForceStop.visibility = if ("Force stop" in activeActions) View.VISIBLE else View.GONE
        binding.rowActionHide.visibility = if ("Hide" in activeActions && isDrawer) View.VISIBLE else View.GONE
    }

    private fun setupActionListeners() {
        // App Info
        binding.btnActionInfo.setOnClickListener {
            AppManager.openAppDetails(context, appInfo.packageName)
            dismiss()
        }

        // Edit
        binding.btnActionEdit.setOnClickListener {
            onEditClick?.invoke()
            dismiss()
        }

        // Widgets
        binding.btnActionWidgets.setOnClickListener {
            onWidgetsClick?.invoke()
            dismiss()
        }

        // Share
        binding.btnActionShare.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Check out ${appInfo.label}: https://play.google.com/store/apps/details?id=${appInfo.packageName}"
                )
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share ${appInfo.label}"))
            dismiss()
        }

        // Remove
        binding.rowActionRemove.setOnClickListener {
            onRemoveClick?.invoke()
            dismiss()
        }

        // Uninstall
        binding.rowActionUninstall.setOnClickListener {
            AppManager.uninstallApp(context, appInfo.packageName)
            dismiss()
        }

        // Play Store
        binding.rowActionPlayStore.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${appInfo.packageName}"))
                context.startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=${appInfo.packageName}")
                )
                context.startActivity(intent)
            }
            dismiss()
        }

        // Force Stop
        binding.rowActionForceStop.setOnClickListener {
            AppManager.openAppDetails(context, appInfo.packageName)
            dismiss()
        }

        // Hide App
        binding.rowActionHide.setOnClickListener {
            onHideClick?.invoke()
            dismiss()
        }
    }

    fun show() {
        anchorView.post {
            if (!anchorView.isAttachedToWindow) return@post
            val location = IntArray(2)
            anchorView.getLocationOnScreen(location)
            val anchorX = location[0]
            val anchorY = location[1]

            val displayMetrics = context.resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels
            val density = displayMetrics.density

            val defaultWidthPx = (240 * density).toInt()
            binding.root.measure(
                View.MeasureSpec.makeMeasureSpec(defaultWidthPx, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(screenHeight, View.MeasureSpec.AT_MOST)
            )
            val popupWidth = binding.root.measuredWidth.coerceAtLeast(defaultWidthPx)
            val popupHeight = binding.root.measuredHeight

            val marginPx = (16 * density).toInt()
            val maxPosX = (screenWidth - popupWidth - marginPx).coerceAtLeast(marginPx)
            val minPosX = marginPx.coerceAtMost(maxPosX)

            var posX = anchorX + (anchorView.width - popupWidth) / 2
            posX = posX.coerceIn(minPosX, maxPosX)

            var posY = anchorY - popupHeight - (12 * density).toInt()
            if (posY < (60 * density).toInt()) {
                posY = anchorY + anchorView.height + (12 * density).toInt()
            }

            try {
                popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, posX, posY)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun dismiss() {
        if (popupWindow.isShowing) {
            popupWindow.dismiss()
        }
    }

    private fun applyPopupColorsRecursive(group: ViewGroup, textColor: Int, accentColor: Int) {
        for (i in 0 until group.childCount) {
            when (val child = group.getChildAt(i)) {
                is android.widget.ImageView -> child.setColorFilter(accentColor)
                is android.widget.TextView -> child.setTextColor(textColor)
                is ViewGroup -> applyPopupColorsRecursive(child, textColor, accentColor)
            }
        }
    }
}
