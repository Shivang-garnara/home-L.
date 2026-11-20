package com.example.launcherai

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.launcherai.icons.DeviceIconMetrics
import com.example.launcherai.icons.IconShapeMasker

class AppsAdapter(
    private var apps: List<AppInfo>,
    private val onAppClick: (AppInfo) -> Unit,
    private val onAppLongClick: ((View, AppInfo, Float, Float) -> Boolean)? = null,
    private var displayMode: DisplayMode = DisplayMode.GRID
) : RecyclerView.Adapter<AppsAdapter.AppViewHolder>() {

    enum class DisplayMode {
        GRID,
        LIST,
        HORIZONTAL
    }

    fun setDisplayMode(mode: DisplayMode) {
        if (this.displayMode != mode) {
            this.displayMode = mode
            notifyDataSetChanged()
        }
    }

    fun updateApps(newApps: List<AppInfo>) {
        apps = newApps
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (displayMode == DisplayMode.LIST) 1 else 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val layoutRes = if (viewType == 1) R.layout.item_app_list else R.layout.item_app_grid
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val lp = holder.itemView.layoutParams
        if (displayMode == DisplayMode.HORIZONTAL) {
            val dm = holder.itemView.resources.displayMetrics
            val itemWidth = (dm.widthPixels - (16 * dm.density).toInt()) / 5
            if (lp != null && lp.width != itemWidth) {
                lp.width = itemWidth
                holder.itemView.layoutParams = lp
            }
        } else {
            if (lp != null && lp.width != ViewGroup.LayoutParams.MATCH_PARENT) {
                lp.width = ViewGroup.LayoutParams.MATCH_PARENT
                holder.itemView.layoutParams = lp
            }
        }
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
            val shape = prefs.getString("key_icon_shape", "Round") ?: "Round"
            val sizePercent = prefs.getInt("key_icon_size_percent", 125)

            val sizePx = if (displayMode == DisplayMode.LIST) {
                DeviceIconMetrics.getListIconSizePx(itemView.context, sizePercent)
            } else {
                DeviceIconMetrics.getDrawerIconSizePx(itemView.context, 5, sizePercent)
            }

            val maskedIcon = IconShapeMasker.createMaskedIcon(
                itemView.context,
                app.icon ?: androidx.core.content.ContextCompat.getDrawable(itemView.context, R.mipmap.ic_launcher),
                shape,
                sizePercent,
                targetSizePx = sizePx
            )

            if (maskedIcon != null) {
                ivIcon.setImageDrawable(maskedIcon)
            } else if (app.icon != null) {
                ivIcon.setImageDrawable(app.icon)
            } else {
                ivIcon.setImageResource(R.mipmap.ic_launcher)
            }

            ivIcon.layoutParams = ivIcon.layoutParams.apply {
                width = sizePx
                height = sizePx
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
