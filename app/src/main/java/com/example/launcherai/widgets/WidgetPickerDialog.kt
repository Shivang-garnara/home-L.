package com.example.launcherai.widgets

import android.app.Dialog
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.launcherai.R
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * Dialog displaying installed Android AppWidget providers, widget preview images,
 * app labels, and calculated cell grid dimensions.
 */
class WidgetPickerDialog(
    context: Context,
    private val providers: List<AppWidgetProviderInfo>,
    private val onProviderSelected: (AppWidgetProviderInfo, spanX: Int, spanY: Int) -> Unit
) : BottomSheetDialog(context, R.style.Theme_LauncherAI) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_widget_picker)

        val rv = findViewById<RecyclerView>(R.id.rvWidgetProviders)
        val btnClose = findViewById<View>(R.id.btnCloseWidgetPicker)

        btnClose?.setOnClickListener { dismiss() }

        rv?.layoutManager = LinearLayoutManager(context)
        rv?.adapter = WidgetProviderAdapter(providers) { provider, spanX, spanY ->
            dismiss()
            onProviderSelected(provider, spanX, spanY)
        }
    }

    private class WidgetProviderAdapter(
        private val providers: List<AppWidgetProviderInfo>,
        private val onSelect: (AppWidgetProviderInfo, Int, Int) -> Unit
    ) : RecyclerView.Adapter<WidgetProviderAdapter.ViewHolder>() {

        override fun getItemCount(): Int = providers.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_widget_provider, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val provider = providers[position]
            val context = holder.itemView.context
            val pm = context.packageManager

            val label = provider.loadLabel(pm).toString()
            val appName = try {
                val appInfo = pm.getApplicationInfo(provider.provider.packageName, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                provider.provider.packageName
            }

            // Calculate grid span from minWidth and minHeight based on active grid config
            val gridConfig = (context.applicationContext as? com.example.launcherai.LauncherApplication)?.repository?.getGridConfig()
                ?: com.example.launcherai.data.repository.WorkspaceGridConfig()
            val density = context.resources.displayMetrics.density
            val minWidthDp = (provider.minWidth / density).toInt()
            val minHeightDp = (provider.minHeight / density).toInt()

            val spanX = calculateCellSpan(minWidthDp).coerceIn(1, gridConfig.cols)
            val spanY = calculateCellSpan(minHeightDp).coerceIn(1, gridConfig.rows)

            holder.tvWidgetLabel.text = label
            holder.tvWidgetApp.text = appName
            holder.tvWidgetSpan.text = "$spanX x $spanY cells (${minWidthDp}dp x ${minHeightDp}dp)"

            val previewDrawable: Drawable? = try {
                provider.loadPreviewImage(context, 0) ?: provider.loadIcon(context, 0)
            } catch (e: Exception) {
                null
            }

            if (previewDrawable != null) {
                holder.ivPreview.setImageDrawable(previewDrawable)
            } else {
                holder.ivPreview.setImageResource(R.drawable.ic_apps_drawer)
            }

            holder.itemView.setOnClickListener {
                onSelect(provider, spanX, spanY)
            }
        }

        private fun calculateCellSpan(sizeDp: Int): Int {
            return Math.ceil((sizeDp + 30) / 70.0).toInt()
        }

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivPreview: ImageView = view.findViewById(R.id.ivWidgetPreview)
            val tvWidgetLabel: TextView = view.findViewById(R.id.tvWidgetLabel)
            val tvWidgetApp: TextView = view.findViewById(R.id.tvWidgetApp)
            val tvWidgetSpan: TextView = view.findViewById(R.id.tvWidgetSpan)
        }
    }
}
