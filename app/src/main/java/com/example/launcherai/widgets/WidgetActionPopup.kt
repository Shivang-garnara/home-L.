package com.example.launcherai.widgets

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.example.launcherai.R
import com.example.launcherai.ui.WorkspaceWidgetResolvedItem
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * Nova-style Widget context action popup shown on long-pressing an active AppWidget.
 * Provides immediate shortcuts to:
 *  - Resize Widget (opens AppWidgetResizeFrame)
 *  - Configure Widget (re-opens widget configuration intent)
 *  - Move Widget (initiates drag-and-drop)
 *  - App Info (opens system App Details)
 *  - Remove (removes from workspace and deallocates AppWidget ID)
 */
class WidgetActionPopup(
    context: Context,
    private val widgetItem: WorkspaceWidgetResolvedItem,
    private val onResize: () -> Unit,
    private val onConfigure: (() -> Unit)?,
    private val onMove: () -> Unit,
    private val onRemove: () -> Unit
) : BottomSheetDialog(context, R.style.Theme_LauncherAI) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_widget_actions)

        val ivIcon = findViewById<ImageView>(R.id.ivWidgetActionIcon)
        val tvTitle = findViewById<TextView>(R.id.tvWidgetActionTitle)
        val tvSubtitle = findViewById<TextView>(R.id.tvWidgetActionSubtitle)
        val tvSpanBadge = findViewById<TextView>(R.id.tvWidgetActionSpanBadge)

        val actionResize = findViewById<View>(R.id.actionResizeWidget)
        val actionConfigure = findViewById<View>(R.id.actionConfigureWidget)
        val actionMove = findViewById<View>(R.id.actionMoveWidget)
        val actionAppInfo = findViewById<View>(R.id.actionAppInfo)
        val actionRemove = findViewById<View>(R.id.actionRemoveWidget)

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
}
