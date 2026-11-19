package com.example.launcherai

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppsAdapter(
    private var apps: List<AppInfo>,
    private val onAppClick: (AppInfo) -> Unit,
    private val onAppLongClick: ((View, AppInfo, Float, Float) -> Boolean)? = null
) : RecyclerView.Adapter<AppsAdapter.AppViewHolder>() {

    fun updateApps(newApps: List<AppInfo>) {
        apps = newApps
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app_grid, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        holder.bind(app)
    }

    override fun getItemCount(): Int = apps.size

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivAppIcon)
        private val tvLabel: TextView = itemView.findViewById(R.id.tvAppLabel)

        fun bind(app: AppInfo) {
            tvLabel.text = app.label
            val prefs = itemView.context.getSharedPreferences("workspace_prefs", android.content.Context.MODE_PRIVATE)
            val shape = prefs.getString("key_icon_shape", "Rounded square") ?: "Rounded square"
            val sizePercent = prefs.getInt("key_icon_size_percent", 100)

            val maskedIcon = com.example.launcherai.icons.IconShapeMasker.createMaskedIcon(
                itemView.context,
                app.icon ?: androidx.core.content.ContextCompat.getDrawable(itemView.context, R.mipmap.ic_launcher),
                shape,
                sizePercent
            )

            if (maskedIcon != null) {
                ivIcon.setImageDrawable(maskedIcon)
            } else if (app.icon != null) {
                ivIcon.setImageDrawable(app.icon)
            } else {
                ivIcon.setImageResource(R.mipmap.ic_launcher)
            }

            var lastTouchX = 0f
            var lastTouchY = 0f

            itemView.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    val loc = IntArray(2)
                    itemView.getLocationOnScreen(loc)
                    lastTouchX = loc[0] + event.x
                    lastTouchY = loc[1] + event.y
                }
                false
            }

            itemView.setOnClickListener {
                onAppClick(app)
            }

            itemView.setOnLongClickListener { v ->
                onAppLongClick?.invoke(v, app, lastTouchX, lastTouchY) ?: false
            }
        }
    }
}
