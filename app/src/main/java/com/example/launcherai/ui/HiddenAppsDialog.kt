package com.example.launcherai.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.launcherai.AppInfo
import com.example.launcherai.R
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * BottomSheetDialog allowing users to hide or unhide specific applications from the App Drawer.
 */
class HiddenAppsDialog(
    context: Context,
    private val installedApps: List<AppInfo>,
    private val hiddenPackages: Set<String>,
    private val onToggleHide: (String) -> Unit
) : BottomSheetDialog(context, R.style.Theme_LauncherAI) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_hidden_apps)

        val tvTitle = findViewById<TextView>(R.id.tvDialogTitle)
        tvTitle?.text = "Hide Applications"

        val rv = findViewById<RecyclerView>(R.id.rvWidgetProviders)
        val btnClose = findViewById<View>(R.id.btnCloseWidgetPicker)

        btnClose?.setOnClickListener { dismiss() }

        rv?.layoutManager = LinearLayoutManager(context)
        rv?.adapter = HiddenAppsAdapter(installedApps, hiddenPackages, onToggleHide)
    }

    private class HiddenAppsAdapter(
        private val apps: List<AppInfo>,
        private val hiddenPackages: Set<String>,
        private val onToggle: (String) -> Unit
    ) : RecyclerView.Adapter<HiddenAppsAdapter.ViewHolder>() {

        override fun getItemCount(): Int = apps.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_widget_provider, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = apps[position]
            holder.tvLabel.text = app.label
            holder.tvApp.text = if (hiddenPackages.contains(app.packageName)) "Hidden from drawer" else "Visible in drawer"
            holder.tvSpan.text = app.packageName
            holder.ivIcon.setImageDrawable(app.icon)

            holder.itemView.setOnClickListener {
                onToggle(app.packageName)
                notifyItemChanged(position)
            }
        }

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivIcon: ImageView = view.findViewById(R.id.ivWidgetPreview)
            val tvLabel: TextView = view.findViewById(R.id.tvWidgetLabel)
            val tvApp: TextView = view.findViewById(R.id.tvWidgetApp)
            val tvSpan: TextView = view.findViewById(R.id.tvWidgetSpan)
        }
    }
}
