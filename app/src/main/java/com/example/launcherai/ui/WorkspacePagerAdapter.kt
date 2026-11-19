package com.example.launcherai.ui

import android.appwidget.AppWidgetHost
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.launcherai.AppInfo
import com.example.launcherai.R
import com.example.launcherai.data.db.WorkspacePageEntity
import com.example.launcherai.data.repository.WorkspaceGridConfig

class WorkspacePagerAdapter(
    private var pages: List<WorkspacePageEntity> = emptyList(),
    private var pageDisplayItemsMap: Map<Int, List<WorkspaceItemDisplay>> = emptyMap(),
    private var gridConfig: WorkspaceGridConfig = WorkspaceGridConfig(),
    var appWidgetHost: AppWidgetHost? = null,
    private val onAppClick: (AppInfo) -> Unit,
    private val onFolderClick: ((WorkspaceFolderResolvedItem) -> Unit)? = null,
    private val onAppLongClick: ((View, WorkspaceResolvedItem, Float, Float) -> Boolean)? = null,
    private val onFolderLongClick: ((View, WorkspaceFolderResolvedItem, Float, Float) -> Boolean)? = null,
    private val onWidgetLongClick: ((View, WorkspaceWidgetResolvedItem, Float, Float) -> Boolean)? = null
) : RecyclerView.Adapter<WorkspacePagerAdapter.WorkspacePageViewHolder>() {

    fun updateData(
        newPages: List<WorkspacePageEntity>,
        newItemsMap: Map<Int, List<WorkspaceItemDisplay>>,
        newGridConfig: WorkspaceGridConfig = gridConfig
    ) {
        this.pages = newPages
        this.pageDisplayItemsMap = newItemsMap
        this.gridConfig = newGridConfig
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = pages.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkspacePageViewHolder {
        val cellLayout = CellLayout(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            countX = gridConfig.cols
            countY = gridConfig.rows
        }
        return WorkspacePageViewHolder(cellLayout)
    }

    override fun onBindViewHolder(holder: WorkspacePageViewHolder, position: Int) {
        val cellLayout = holder.cellLayout
        cellLayout.countX = gridConfig.cols
        cellLayout.countY = gridConfig.rows
        cellLayout.removeAllViews()

        val page = pages[position]
        val displayItems = pageDisplayItemsMap[page.screenId] ?: emptyList()

        val inflater = LayoutInflater.from(cellLayout.context)
        for (itemDisplay in displayItems) {
            when (itemDisplay) {
                is WorkspaceItemDisplay.App -> {
                    val resolvedItem = itemDisplay.item
                    val entity = resolvedItem.entity
                    val appInfo = resolvedItem.appInfo

                    val iconView = inflater.inflate(R.layout.item_app_grid, cellLayout, false)
                    iconView.tag = entity
                    val ivAppIcon = iconView.findViewById<ImageView>(R.id.ivAppIcon)
                    val tvAppLabel = iconView.findViewById<TextView>(R.id.tvAppLabel)

                    ivAppIcon.setImageDrawable(appInfo.icon)
                    tvAppLabel.text = appInfo.label

                    var lastTouchX = 0f
                    var lastTouchY = 0f

                    iconView.setOnTouchListener { _, event ->
                        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                            val loc = IntArray(2)
                            iconView.getLocationOnScreen(loc)
                            lastTouchX = loc[0] + event.x
                            lastTouchY = loc[1] + event.y
                        }
                        false
                    }

                    iconView.setOnClickListener {
                        onAppClick(appInfo)
                    }

                    iconView.setOnLongClickListener { v ->
                        onAppLongClick?.invoke(v, resolvedItem, lastTouchX, lastTouchY) ?: false
                    }

                    cellLayout.addViewToCell(
                        child = iconView,
                        cellX = entity.cellX,
                        cellY = entity.cellY,
                        spanX = entity.spanX,
                        spanY = entity.spanY,
                        preventOverlap = true
                    )
                }

                is WorkspaceItemDisplay.Folder -> {
                    val folderResolved = itemDisplay.folder
                    val entity = folderResolved.entity

                    val folderView = inflater.inflate(R.layout.item_folder_grid, cellLayout, false)
                    folderView.tag = entity
                    val tvFolderTitle = folderView.findViewById<TextView>(R.id.tvFolderTitle)
                    val ivPreview1 = folderView.findViewById<ImageView>(R.id.ivPreview1)
                    val ivPreview2 = folderView.findViewById<ImageView>(R.id.ivPreview2)
                    val ivPreview3 = folderView.findViewById<ImageView>(R.id.ivPreview3)
                    val ivPreview4 = folderView.findViewById<ImageView>(R.id.ivPreview4)

                    tvFolderTitle.text = folderResolved.title

                    val previewViews = listOf(ivPreview1, ivPreview2, ivPreview3, ivPreview4)
                    val children = folderResolved.children
                    previewViews.forEachIndexed { index, iv ->
                        if (index < children.size) {
                            iv.setImageDrawable(children[index].appInfo.icon)
                            iv.visibility = View.VISIBLE
                        } else {
                            iv.setImageDrawable(null)
                            iv.visibility = View.INVISIBLE
                        }
                    }

                    var lastTouchX = 0f
                    var lastTouchY = 0f

                    folderView.setOnTouchListener { _, event ->
                        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                            val loc = IntArray(2)
                            folderView.getLocationOnScreen(loc)
                            lastTouchX = loc[0] + event.x
                            lastTouchY = loc[1] + event.y
                        }
                        false
                    }

                    folderView.setOnClickListener {
                        onFolderClick?.invoke(folderResolved)
                    }

                    folderView.setOnLongClickListener { v ->
                        onFolderLongClick?.invoke(v, folderResolved, lastTouchX, lastTouchY) ?: false
                    }

                    cellLayout.addViewToCell(
                        child = folderView,
                        cellX = entity.cellX,
                        cellY = entity.cellY,
                        spanX = entity.spanX,
                        spanY = entity.spanY,
                        preventOverlap = true
                    )
                }

                is WorkspaceItemDisplay.Widget -> {
                    val widgetResolved = itemDisplay.widget
                    val entity = widgetResolved.entity
                    val providerInfo = widgetResolved.providerInfo

                    val host = appWidgetHost
                    if (host != null && providerInfo != null) {
                        val hostView = host.createView(cellLayout.context, widgetResolved.appWidgetId, providerInfo)
                        hostView.setAppWidget(widgetResolved.appWidgetId, providerInfo)
                        hostView.tag = entity

                        var lastTouchX = 0f
                        var lastTouchY = 0f

                        hostView.setOnTouchListener { _, event ->
                            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                                val loc = IntArray(2)
                                hostView.getLocationOnScreen(loc)
                                lastTouchX = loc[0] + event.x
                                lastTouchY = loc[1] + event.y
                            }
                            false
                        }

                        hostView.setOnLongClickListener { v ->
                            onWidgetLongClick?.invoke(v, widgetResolved, lastTouchX, lastTouchY) ?: false
                        }

                        cellLayout.addViewToCell(
                            child = hostView,
                            cellX = entity.cellX,
                            cellY = entity.cellY,
                            spanX = entity.spanX,
                            spanY = entity.spanY,
                            preventOverlap = true
                        )
                    }
                }
            }
        }
    }

    class WorkspacePageViewHolder(val cellLayout: CellLayout) : RecyclerView.ViewHolder(cellLayout)
}
