package com.example.launcherai.ui

import android.appwidget.AppWidgetHost
import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.launcherai.R
import com.example.launcherai.data.db.WorkspacePageEntity
import com.example.launcherai.data.model.AppInfo
import com.example.launcherai.data.repository.WorkspaceGridConfig
import kotlin.math.abs

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
            applyWorkspacePadding(this)
        }
        return WorkspacePageViewHolder(cellLayout)
    }

    private fun applyWorkspacePadding(cellLayout: CellLayout) {
        val prefs = cellLayout.context.getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        val paddingHorizStep = prefs.getInt("key_desktop_padding_horizontal", 0)
        val paddingVertStep = prefs.getInt("key_desktop_padding_vertical", 0)
        val dpHoriz = when (paddingHorizStep) {
            1 -> 8
            2 -> 16
            3 -> 24
            else -> 0
        }
        val dpVert = when (paddingVertStep) {
            1 -> 8
            2 -> 16
            3 -> 24
            else -> 0
        }
        val density = cellLayout.context.resources.displayMetrics.density
        val pxHoriz = (dpHoriz * density).toInt()
        val pxVert = (dpVert * density).toInt()
        cellLayout.setPadding(pxHoriz, pxVert, pxHoriz, pxVert)
    }

    override fun onBindViewHolder(holder: WorkspacePageViewHolder, position: Int) {
        val cellLayout = holder.cellLayout
        cellLayout.countX = gridConfig.cols
        cellLayout.countY = gridConfig.rows
        applyWorkspacePadding(cellLayout)
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

                    val iconShadow = prefs.getBoolean("key_icon_shadow", true)
                    ivAppIcon.elevation = if (iconShadow) 4f * cellLayout.context.resources.displayMetrics.density else 0f

                    ivAppIcon.layoutParams = ivAppIcon.layoutParams.apply {
                        width = sizePx
                        height = sizePx
                    }
                    tvAppLabel.text = appInfo.label

                    val labelVisible = prefs.getBoolean("key_label_visible", true)
                    if (!labelVisible) {
                        tvAppLabel.visibility = View.GONE
                    } else {
                        tvAppLabel.visibility = View.VISIBLE
                        val labelSizePercent = prefs.getInt("key_label_size_percent", 100)
                        tvAppLabel.textSize = 11f * (labelSizePercent / 100f)
                        val fontChoice = prefs.getString("key_label_font", "Normal") ?: "Normal"
                        tvAppLabel.typeface = when (fontChoice) {
                            "Medium" -> android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
                            "Condensed" -> android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.NORMAL)
                            "Light" -> android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL)
                            else -> android.graphics.Typeface.DEFAULT
                        }
                        val labelColor = prefs.getInt("key_label_color", android.graphics.Color.WHITE)
                        tvAppLabel.setTextColor(labelColor)
                        val shadowEnabled = prefs.getBoolean("key_label_shadow", true)
                        if (shadowEnabled) {
                            val isDark = androidx.core.graphics.ColorUtils.calculateLuminance(labelColor) < 0.5
                            if (isDark) {
                                tvAppLabel.setShadowLayer(3f, 0f, 1f, 0x44FFFFFF.toInt())
                            } else {
                                tvAppLabel.setShadowLayer(4f, 0f, 2f, 0x88000000.toInt())
                            }
                        } else {
                            tvAppLabel.setShadowLayer(0f, 0f, 0f, 0)
                        }
                        val singleLine = prefs.getBoolean("key_label_single_line", true)
                        if (singleLine) {
                            tvAppLabel.maxLines = 1
                            tvAppLabel.isSingleLine = true
                        } else {
                            tvAppLabel.maxLines = 2
                            tvAppLabel.isSingleLine = false
                        }
                    }

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

                    val folderPrefs = cellLayout.context.getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                    tvFolderTitle.text = folderResolved.title
                    val folderLabelVisible = folderPrefs.getBoolean("key_label_visible", true)
                    if (!folderLabelVisible) {
                        tvFolderTitle.visibility = View.GONE
                    } else {
                        tvFolderTitle.visibility = View.VISIBLE
                        val labelSizePercent = folderPrefs.getInt("key_label_size_percent", 100)
                        tvFolderTitle.textSize = 11f * (labelSizePercent / 100f)
                        val fontChoice = folderPrefs.getString("key_label_font", "Normal") ?: "Normal"
                        tvFolderTitle.typeface = when (fontChoice) {
                            "Medium" -> android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
                            "Condensed" -> android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.NORMAL)
                            "Light" -> android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL)
                            else -> android.graphics.Typeface.DEFAULT
                        }
                        val labelColor = folderPrefs.getInt("key_label_color", android.graphics.Color.WHITE)
                        tvFolderTitle.setTextColor(labelColor)
                        val shadowEnabled = folderPrefs.getBoolean("key_label_shadow", true)
                        if (shadowEnabled) {
                            val isDark = androidx.core.graphics.ColorUtils.calculateLuminance(labelColor) < 0.5
                            if (isDark) {
                                tvFolderTitle.setShadowLayer(3f, 0f, 1f, 0x44FFFFFF.toInt())
                            } else {
                                tvFolderTitle.setShadowLayer(4f, 0f, 2f, 0x88000000.toInt())
                            }
                        } else {
                            tvFolderTitle.setShadowLayer(0f, 0f, 0f, 0)
                        }
                    }

                    val ivFolderIcon = folderView.findViewById<ImageView>(R.id.ivFolderIcon)
                    val gridFolderFallback = folderView.findViewById<View>(R.id.gridFolderFallback)
                    val flFolderPreview = folderView.findViewById<FrameLayout>(R.id.flFolderPreview)

                    val iconSizePercent = folderPrefs.getInt("key_desktop_icon_size_percent", 100)
                    val density = cellLayout.context.resources.displayMetrics.density
                    val folderTargetPx = (48f * density * (iconSizePercent / 100f)).toInt()
                    flFolderPreview?.layoutParams?.let { lp ->
                        lp.width = folderTargetPx
                        lp.height = folderTargetPx
                    }

                    val children = folderResolved.children
                    val childrenDrawables = children.mapNotNull { it.appInfo.icon }.take(9)

                    if (ivFolderIcon != null) {
                        val rendered = com.example.launcherai.icons.FolderIconRenderer.renderFolderIconFromPrefs(
                            context = cellLayout.context,
                            childrenDrawables = childrenDrawables,
                            targetSizePx = folderTargetPx
                        )
                        ivFolderIcon.setImageDrawable(rendered)
                        ivFolderIcon.visibility = View.VISIBLE
                        gridFolderFallback?.visibility = View.GONE
                        flFolderPreview?.background = null
                    } else {
                        val previewViews = listOf(ivPreview1, ivPreview2, ivPreview3, ivPreview4)
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
                                iv?.setImageDrawable(maskedMini ?: childIcon)
                                iv?.visibility = View.VISIBLE
                            } else {
                                iv?.setImageDrawable(null)
                                iv?.visibility = View.INVISIBLE
                            }
                        }
                    }

                    var lastTouchX = 0f
                    var lastTouchY = 0f
                    var startTouchX = 0f
                    var startTouchY = 0f
                    var isSwipingUp = false
                    val touchSlop = ViewConfiguration.get(cellLayout.context).scaledTouchSlop

                    folderView.setOnTouchListener { v, event ->
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN -> {
                                val loc = IntArray(2)
                                v.getLocationOnScreen(loc)
                                lastTouchX = loc[0] + event.x
                                lastTouchY = loc[1] + event.y
                                startTouchX = event.x
                                startTouchY = event.y
                                isSwipingUp = false
                                false
                            }
                            MotionEvent.ACTION_MOVE -> {
                                val diffY = startTouchY - event.y
                                val diffX = abs(startTouchX - event.x)
                                if (diffY > touchSlop * 1.8f && diffY > diffX * 1.3f) {
                                    isSwipingUp = true
                                    v.parent?.requestDisallowInterceptTouchEvent(true)
                                }
                                false
                            }
                            MotionEvent.ACTION_UP -> {
                                if (isSwipingUp) {
                                    isSwipingUp = false
                                    v.cancelLongPress()
                                    // Tactile bounce animation matching Nova Launcher p1
                                    v.animate()
                                        .translationY(-24f)
                                        .setDuration(120)
                                        .withEndAction {
                                            v.animate().translationY(0f).setDuration(100).start()
                                        }
                                        .start()

                                    val prefs = v.context.getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                                    val tapAction = prefs.getString(
                                        FolderSettingsActivity.KEY_FOLDER_TAP_ACTION,
                                        FolderSettingsActivity.TAP_ACTION_OPEN_FOLDER
                                    )

                                    if (tapAction == FolderSettingsActivity.TAP_ACTION_FIRST_APP) {
                                        // Swipe up opens folder overlay
                                        onFolderClick?.invoke(folderResolved)
                                    } else {
                                        // Swipe up launches first app in folder
                                        val firstApp = folderResolved.children.firstOrNull()?.appInfo
                                        if (firstApp != null) {
                                            onAppClick(firstApp)
                                        } else {
                                            onFolderClick?.invoke(folderResolved)
                                        }
                                    }
                                    true
                                } else {
                                    false
                                }
                            }
                            MotionEvent.ACTION_CANCEL -> {
                                isSwipingUp = false
                                false
                            }
                            else -> false
                        }
                    }

                    folderView.setOnClickListener { v ->
                        val prefs = v.context.getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                        val tapAction = prefs.getString(
                            FolderSettingsActivity.KEY_FOLDER_TAP_ACTION,
                            FolderSettingsActivity.TAP_ACTION_OPEN_FOLDER
                        )

                        if (tapAction == FolderSettingsActivity.TAP_ACTION_FIRST_APP) {
                            // Signature Nova Mode: Tap launches first app!
                            val firstApp = folderResolved.children.firstOrNull()?.appInfo
                            if (firstApp != null) {
                                onAppClick(firstApp)
                            } else {
                                onFolderClick?.invoke(folderResolved)
                            }
                        } else {
                            // Default Mode: Tap opens folder overlay
                            onFolderClick?.invoke(folderResolved)
                        }
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

                        // Widget Corner Radius Clipping (Nova Launcher style)
                        val widgetPrefs = cellLayout.context.getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                        val cornerRadiusDp = widgetPrefs.getInt("key_widget_corner_radius", 16)
                        val radiusPx = cornerRadiusDp * cellLayout.context.resources.displayMetrics.density
                        if (radiusPx > 0f) {
                            hostView.outlineProvider = object : android.view.ViewOutlineProvider() {
                                override fun getOutline(view: View, outline: android.graphics.Outline) {
                                    outline.setRoundRect(0, 0, view.width, view.height, radiusPx)
                                }
                            }
                            hostView.clipToOutline = true
                        } else {
                            hostView.clipToOutline = false
                        }

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
