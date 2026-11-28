package com.example.launcherai.widgets

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.launcherai.R
import com.example.launcherai.ui.PopupMenuSettingsActivity
import com.example.launcherai.ui.WorkspaceWidgetResolvedItem

/**
 * Nova-style floating Widget context popup anchored to the long-pressed AppWidget
 * (matching PopupContainerWithArrow & i10.d/c widget shortcuts).
 * Provides:
 *  - Resize (opens AppWidgetResizeFrame)
 *  - Padding (toggles system default padding vs full-bleed 0 padding)
 *  - Widget settings (re-opens widget configuration intent when configurable)
 *  - Move (initiates drag-and-drop)
 *  - App info (opens system App Details)
 *  - Remove (removes from workspace and deallocates AppWidget ID)
 */
class WidgetActionPopup(
    private val context: Context,
    private val anchorView: LauncherAppWidgetHostView,
    private val widgetItem: WorkspaceWidgetResolvedItem,
    private val onResize: () -> Unit,
    private val onTogglePadding: (() -> Unit)? = null,
    private val onConfigure: (() -> Unit)?,
    private val onMove: () -> Unit,
    private val onRemove: () -> Unit
) {
    private val rootView: View = LayoutInflater.from(context).inflate(R.layout.dialog_widget_actions, null)
    private val popupWindow: PopupWindow
    var onDismissListener: (() -> Unit)? = null

    val isShowing: Boolean
        get() = popupWindow.isShowing

    init {
        popupWindow = PopupWindow(
            rootView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            isOutsideTouchable = true
            isFocusable = true
            elevation = 20f * context.resources.displayMetrics.density
            setOnDismissListener { onDismissListener?.invoke() }
        }

        applyPopupStyling()
        bindViews()
    }

    private fun applyPopupStyling() {
        val prefs = context.getSharedPreferences(PopupMenuSettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val cornerRadiusDp = prefs.getInt(
            PopupMenuSettingsActivity.KEY_POPUP_CORNER_RADIUS,
            PopupMenuSettingsActivity.DEFAULT_CORNER_RADIUS_DP
        )
        val density = context.resources.displayMetrics.density
        val radiusPx = cornerRadiusDp * density
        val hasCardBg = prefs.getBoolean(PopupMenuSettingsActivity.KEY_POPUP_CARD_BG, true)

        val popupStyle = com.example.launcherai.ui.SurfaceStyleConfig.loadSurfaceStyle(
            context,
            com.example.launcherai.ui.SurfaceStyleConfig.SurfaceId.POPUP_MENU
        )
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
        val container = rootView.findViewById<ViewGroup>(R.id.widgetPopupContainer)
        container?.background = bgDrawable
        if (container != null && (popupStyle.hasCustomText || popupStyle.hasCustomAccent || popupStyle.hasCustomSurface)) {
            applyPopupColorsRecursive(
                container,
                popupStyle.effectiveTextColor(),
                popupStyle.effectiveAccentColor()
            )
        }
    }

    private fun applyPopupColorsRecursive(group: ViewGroup, textColor: Int, accentColor: Int) {
        for (i in 0 until group.childCount) {
            when (val child = group.getChildAt(i)) {
                is ImageView -> child.setColorFilter(accentColor)
                is TextView -> child.setTextColor(textColor)
                is ViewGroup -> applyPopupColorsRecursive(child, textColor, accentColor)
            }
        }
    }

    private fun bindViews() {
        val ivIcon = rootView.findViewById<ImageView>(R.id.ivWidgetActionIcon)
        val tvTitle = rootView.findViewById<TextView>(R.id.tvWidgetActionTitle)
        val tvSubtitle = rootView.findViewById<TextView>(R.id.tvWidgetActionSubtitle)
        val tvSpanBadge = rootView.findViewById<TextView>(R.id.tvWidgetActionSpanBadge)

        val actionResize = rootView.findViewById<View>(R.id.actionResizeWidget)
        val actionPadding = rootView.findViewById<View>(R.id.actionTogglePadding)
        val tvPaddingStatus = rootView.findViewById<TextView>(R.id.tvPaddingStatus)
        val actionConfigure = rootView.findViewById<View>(R.id.actionConfigureWidget)
        val actionMove = rootView.findViewById<View>(R.id.actionMoveWidget)
        val actionAppInfo = rootView.findViewById<View>(R.id.actionAppInfo)
        val actionRemove = rootView.findViewById<View>(R.id.actionRemoveWidget)

        val pm = context.packageManager
        val providerInfo = widgetItem.providerInfo
        val entity = widgetItem.entity

        val widgetLabel = entity.title ?: providerInfo?.loadLabel(pm)?.toString() ?: "Widget"
        val packageName = entity.packageName ?: providerInfo?.provider?.packageName ?: ""

        val appLabel = try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }

        tvTitle?.text = widgetLabel
        tvSubtitle?.text = appLabel
        tvSpanBadge?.text = "${entity.spanX} × ${entity.spanY}"

        val icon = try {
            providerInfo?.loadIcon(context, 0) ?: pm.getApplicationIcon(packageName)
        } catch (e: Exception) {
            null
        }
        if (icon != null) {
            ivIcon?.setImageDrawable(icon)
        } else {
            ivIcon?.setImageResource(R.drawable.ic_apps_drawer)
        }

        actionResize?.setOnClickListener {
            dismiss()
            onResize()
        }

        if (anchorView.hasDefaultPadding() || anchorView.isWidgetPaddingEnabled()) {
            actionPadding?.visibility = View.VISIBLE
            tvPaddingStatus?.text = if (anchorView.isWidgetPaddingEnabled()) "On" else "Off"
            actionPadding?.setOnClickListener {
                anchorView.toggleWidgetPadding()
                onTogglePadding?.invoke()
                dismiss()
            }
        } else {
            actionPadding?.visibility = View.GONE
        }

        if (providerInfo?.configure != null && onConfigure != null) {
            actionConfigure?.visibility = View.VISIBLE
            actionConfigure?.setOnClickListener {
                dismiss()
                onConfigure()
            }
        } else {
            actionConfigure?.visibility = View.GONE
        }

        actionMove?.setOnClickListener {
            dismiss()
            onMove()
        }

        actionAppInfo?.setOnClickListener {
            dismiss()
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        actionRemove?.setOnClickListener {
            dismiss()
            onRemove()
        }
    }

    fun show() {
        rootView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val popupWidth = rootView.measuredWidth
        val popupHeight = rootView.measuredHeight

        val anchorLoc = IntArray(2)
        anchorView.getLocationInWindow(anchorLoc)
        val anchorX = anchorLoc[0]
        val anchorY = anchorLoc[1]

        val dm = context.resources.displayMetrics
        val marginPx = (12 * dm.density).toInt()
        val gapPx = (8 * dm.density).toInt()

        val desiredX = anchorX + (anchorView.width - popupWidth) / 2
        val xPos = desiredX.coerceIn(marginPx, maxOf(marginPx, dm.widthPixels - popupWidth - marginPx))

        val showAbove = anchorY - popupHeight - gapPx >= marginPx
        val yPos = if (showAbove) {
            anchorY - popupHeight - gapPx
        } else {
            (anchorY + anchorView.height + gapPx).coerceAtMost(maxOf(marginPx, dm.heightPixels - popupHeight - marginPx))
        }

        popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, xPos, yPos)
    }

    fun dismiss() {
        if (popupWindow.isShowing) {
            popupWindow.dismiss()
        }
    }
}
