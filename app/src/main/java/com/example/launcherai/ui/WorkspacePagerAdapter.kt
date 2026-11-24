package com.example.launcherai.ui

import android.appwidget.AppWidgetHost
import android.content.Context
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
    private val onWidgetLongClick: ((View, WorkspaceWidgetResolvedItem, Float, Float) -> Boolean)? = null,
    private val onWidgetDragRequested: ((View, WorkspaceWidgetResolvedItem, Float, Float) -> Unit)? = null,
    private val onEmptySpaceLongClick: ((cellX: Int, cellY: Int) -> Boolean)? = null
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

    fun getPageAt(position: Int): WorkspacePageEntity? {
        return pages.getOrNull(position)
    }

    fun getPositionForScreenId(screenId: Int): Int {
        val index = pages.indexOfFirst { it.screenId == screenId }
        return if (index >= 0) index else 0
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

    private val attachedCellLayouts = mutableSetOf<CellLayout>()

    fun clearAllDropTargets() {
        for (layout in attachedCellLayouts) {
            layout.clearDropTarget()
        }
    }

    override fun onViewAttachedToWindow(holder: WorkspacePageViewHolder) {
        super.onViewAttachedToWindow(holder)
        attachedCellLayouts.add(holder.cellLayout)
    }

    override fun onViewDetachedFromWindow(holder: WorkspacePageViewHolder) {
        super.onViewDetachedFromWindow(holder)
        attachedCellLayouts.remove(holder.cellLayout)
        holder.cellLayout.clearDropTarget()
    }

    override fun onBindViewHolder(holder: WorkspacePageViewHolder, position: Int) {
        val cellLayout = holder.cellLayout
        cellLayout.clearDropTarget()
        cellLayout.countX = gridConfig.cols
        cellLayout.countY = gridConfig.rows
        cellLayout.onEmptySpaceLongClickListener = onEmptySpaceLongClick
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

                    val prefs = cellLayout.context.getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                    val shape = prefs.getString("key_icon_shape", "Round") ?: "Round"
                    val sizePercent = prefs.getInt("key_icon_size_percent", 125)

                    val cellWidthPx = if (cellLayout.width > 0) {
                        (cellLayout.width - cellLayout.paddingLeft - cellLayout.paddingRight) / maxOf(1, gridConfig.cols)
                    } else {
                        cellLayout.context.resources.displayMetrics.widthPixels / maxOf(1, gridConfig.cols)
                    }
                    val cellHeightPx = if (cellLayout.height > 0) {
                        (cellLayout.height - cellLayout.paddingTop - cellLayout.paddingBottom) / maxOf(1, gridConfig.rows)
                    } else {
                        cellLayout.context.resources.displayMetrics.heightPixels / maxOf(1, gridConfig.rows)
                    }
                    val sizePx = com.example.launcherai.icons.DeviceIconMetrics.getWorkspaceIconSizePx(
                        cellLayout.context,
                        cellWidthPx,
                        cellHeightPx,
                        sizePercent
                    )

                    val maskedIcon = com.example.launcherai.icons.IconShapeMasker.createMaskedIcon(
                        cellLayout.context,
                        appInfo.icon ?: androidx.core.content.ContextCompat.getDrawable(cellLayout.context, R.mipmap.ic_launcher),
                        shape,
                        sizePercent,
                        targetSizePx = sizePx
                    )

                    ivAppIcon.setImageDrawable(maskedIcon ?: appInfo.icon)

                    ivAppIcon.layoutParams = ivAppIcon.layoutParams.apply {
                        width = sizePx
                        height = sizePx
                    }
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
                    val folderPrefs = cellLayout.context.getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                    val folderShape = folderPrefs.getString("key_icon_shape", "Round") ?: "Round"

                    previewViews.forEachIndexed { index, iv ->
                        if (index < children.size) {
                            val childIcon = children[index].appInfo.icon
                            val maskedMini = com.example.launcherai.icons.IconShapeMasker.createMaskedIcon(
                                cellLayout.context,
                                childIcon,
                                folderShape,
                                100
                            )
                            iv.setImageDrawable(maskedMini ?: childIcon)
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
                        val hostView = (host.createView(cellLayout.context, widgetResolved.appWidgetId, providerInfo) as? com.example.launcherai.widgets.LauncherAppWidgetHostView)
                            ?: com.example.launcherai.widgets.LauncherAppWidgetHostView(cellLayout.context).apply {
                                setAppWidget(widgetResolved.appWidgetId, providerInfo)
                            }
                        hostView.tag = entity

                        val cellW = cellLayout.getCellWidth()
                        val cellH = cellLayout.getCellHeight()
                        if (cellW > 0 && cellH > 0) {
                            hostView.updateSizeOptions(entity.spanX, entity.spanY, cellW, cellH)
                        }

                        hostView.setOnLongClickListener { v ->
                            val touchX = if (hostView.lastTouchRawX > 0f) hostView.lastTouchRawX else {
                                val loc = IntArray(2)
                                hostView.getLocationOnScreen(loc)
                                loc[0] + hostView.width / 2f
                            }
                            val touchY = if (hostView.lastTouchRawY > 0f) hostView.lastTouchRawY else {
                                val loc = IntArray(2)
                                hostView.getLocationOnScreen(loc)
                                loc[1] + hostView.height / 2f
                            }
                            onWidgetLongClick?.invoke(v, widgetResolved, touchX, touchY) ?: false
                        }

                        hostView.onLongPressDragListener = { rawX, rawY ->
                            onWidgetDragRequested?.invoke(hostView, widgetResolved, rawX, rawY)
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
