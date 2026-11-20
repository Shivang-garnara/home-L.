package com.example.launcherai.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.example.launcherai.AppInfo
import com.example.launcherai.R
import com.example.launcherai.data.db.WorkspaceItemEntity

/**
 * Data-driven, responsive Dock (Hotseat) layout view supporting dynamic capacities (3, 5, 7 slots),
 * App Drawer button reservation, icon pack rendering, and drag-and-drop cell target conversion.
 */
class DockLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    var capacity: Int = 5
        set(value) {
            field = value.coerceIn(3, 7)
            requestLayout()
        }

    val appDrawerSlot: Int
        get() = capacity / 2

    private var dockDisplayItems: List<WorkspaceItemDisplay> = emptyList()

    private var onAppClickListener: ((AppInfo) -> Unit)? = null
    private var onFolderClickListener: ((WorkspaceFolderResolvedItem) -> Unit)? = null
    private var onAppDrawerClickListener: (() -> Unit)? = null
    private var onAppLongClickListener: ((View, WorkspaceResolvedItem, Float, Float) -> Boolean)? = null
    private var onFolderLongClickListener: ((View, WorkspaceFolderResolvedItem, Float, Float) -> Boolean)? = null

    init {
        setBackgroundResource(0) // Clean wallpaper dock without heavy glass panel
    }

    fun setListeners(
        onAppClick: (AppInfo) -> Unit,
        onFolderClick: (WorkspaceFolderResolvedItem) -> Unit,
        onAppDrawerClick: () -> Unit,
        onAppLongClick: (View, WorkspaceResolvedItem, Float, Float) -> Boolean,
        onFolderLongClick: (View, WorkspaceFolderResolvedItem, Float, Float) -> Boolean
    ) {
        this.onAppClickListener = onAppClick
        this.onFolderClickListener = onFolderClick
        this.onAppDrawerClickListener = onAppDrawerClick
        this.onAppLongClickListener = onAppLongClick
        this.onFolderLongClickListener = onFolderLongClick
    }

    fun updateDockItems(items: List<WorkspaceItemDisplay>, newCapacity: Int = capacity) {
        this.capacity = newCapacity
        this.dockDisplayItems = items
        rebuildViews()
    }

    fun refreshDock() {
        rebuildViews()
    }

    private fun rebuildViews() {
        removeAllViews()
        val inflater = LayoutInflater.from(context)

        val prefs = context.getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        val shape = prefs.getString("key_icon_shape", "Round") ?: "Round"
        val matchDesktop = prefs.getBoolean("key_dock_match_desktop_size", true)
        val sizePercent = if (matchDesktop) {
            prefs.getInt("key_icon_size_percent", 100)
        } else {
            prefs.getInt("key_dock_icon_size_percent", 100)
        }
        val showLabels = prefs.getBoolean("key_dock_show_labels", false)
        val bgStyle = prefs.getString("key_dock_bg_style", "Transparent") ?: "Transparent"
        when (bgStyle) {
            "Glass" -> setBackgroundResource(R.drawable.bg_dock_glass)
            "Platform" -> setBackgroundResource(R.drawable.bg_dock_platform)
            else -> background = null
        }

        val density = context.resources.displayMetrics.density
        val dockHeightPx = if (height > 0) height else (72 * density).toInt()
        val scaledDockSize = com.example.launcherai.icons.DeviceIconMetrics.getDockIconSizePx(
            context,
            capacity,
            dockHeightPx,
            sizePercent
        )

        // Map items by cellX slot
        val slotMap = mutableMapOf<Int, WorkspaceItemDisplay>()
        for (displayItem in dockDisplayItems) {
            val cellX = displayItem.entity.cellX
            slotMap[cellX] = displayItem
        }

        val drawerSlot = appDrawerSlot

        for (slot in 0 until capacity) {
            if (slot == drawerSlot) {
                // Reserved App Drawer Button Slot
                val drawerContainer = FrameLayout(context).apply {
                    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                    isClickable = true
                    isFocusable = true
                    setOnClickListener { onAppDrawerClickListener?.invoke() }
                }
                val ivDrawer = ImageView(context).apply {
                    layoutParams = FrameLayout.LayoutParams(scaledDockSize, scaledDockSize).apply {
                        gravity = android.view.Gravity.CENTER
                    }
                    val bgDrawable = com.example.launcherai.icons.IconShapeMasker.createShapeThumbnail(
                        context,
                        shape,
                        scaledDockSize,
                        androidx.core.content.ContextCompat.getColor(context, R.color.accent_primary)
                    )
                    background = bgDrawable
                    val pad = (scaledDockSize * 0.22f).toInt()
                    setPadding(pad, pad, pad, pad)
                    setImageResource(R.drawable.ic_apps_drawer)
                    setColorFilter(context.getColor(R.color.white))
                    contentDescription = "App Drawer"
                }
                drawerContainer.addView(ivDrawer)
                addView(drawerContainer)
            } else {
                val itemDisplay = slotMap[slot]
                if (itemDisplay == null) {
                    // System fallback icon for empty default slots
                    val fallbackSpec = when (slot) {
                        0 -> Triple(R.drawable.ic_phone, R.color.status_green, "phone")
                        1 -> Triple(R.drawable.ic_messages, R.color.status_blue, "messages")
                        3 -> Triple(R.drawable.ic_browser, R.color.accent_cyan, "browser")
                        4 -> Triple(R.drawable.ic_camera, R.color.accent_secondary, "camera")
                        else -> null
                    }

                    if (fallbackSpec != null) {
                        val (resId, tintId, action) = fallbackSpec
                        val containerView = FrameLayout(context).apply {
                            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                            isClickable = true
                            isFocusable = true
                            setOnClickListener {
                                com.example.launcherai.AppManager.launchSystemAction(context, action)
                            }
                        }
                        val ivIcon = ImageView(context).apply {
                            layoutParams = FrameLayout.LayoutParams(scaledDockSize, scaledDockSize).apply {
                                gravity = android.view.Gravity.CENTER
                            }
                            val bgDrawable = com.example.launcherai.icons.IconShapeMasker.createShapeThumbnail(
                                context,
                                shape,
                                scaledDockSize,
                                android.graphics.Color.parseColor("#333333")
                            )
                            background = bgDrawable
                            val pad = (scaledDockSize * 0.22f).toInt()
                            setPadding(pad, pad, pad, pad)
                            setImageResource(resId)
                            setColorFilter(context.getColor(tintId))
                        }
                        containerView.addView(ivIcon)
                        addView(containerView)
                    } else {
                        val emptySlotView = View(context).apply {
                            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                            tag = slot
                        }
                        addView(emptySlotView)
                    }
                } else {
                    when (itemDisplay) {
                        is WorkspaceItemDisplay.App -> {
                            val resolvedApp = itemDisplay.item
                            val entity = resolvedApp.entity
                            val appInfo = resolvedApp.appInfo

                            val iconView = inflater.inflate(R.layout.item_app_grid, this, false)
                            iconView.tag = entity

                            val ivAppIcon = iconView.findViewById<ImageView>(R.id.ivAppIcon)
                            val tvAppLabel = iconView.findViewById<TextView>(R.id.tvAppLabel)

                            val maskedIcon = com.example.launcherai.icons.IconShapeMasker.createMaskedIcon(
                                context,
                                appInfo.icon ?: androidx.core.content.ContextCompat.getDrawable(context, R.mipmap.ic_launcher),
                                shape,
                                sizePercent,
                                targetSizePx = scaledDockSize
                            )

                            ivAppIcon.setImageDrawable(maskedIcon ?: appInfo.icon)

                            ivAppIcon.layoutParams = ivAppIcon.layoutParams.apply {
                                width = scaledDockSize
                                height = scaledDockSize
                            }

                            tvAppLabel.text = appInfo.label
                            tvAppLabel.visibility = if (showLabels) View.VISIBLE else View.GONE
                            if (showLabels) {
                                val labelColor = prefs.getInt("key_label_color", android.graphics.Color.WHITE)
                                tvAppLabel.setTextColor(labelColor)
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
                                onAppClickListener?.invoke(appInfo)
                            }

                            iconView.setOnLongClickListener { v ->
                                onAppLongClickListener?.invoke(v, resolvedApp, lastTouchX, lastTouchY) ?: false
                            }

                            addView(iconView)
                        }

                        is WorkspaceItemDisplay.Folder -> {
                            val folderResolved = itemDisplay.folder
                            val entity = folderResolved.entity

                            val folderView = inflater.inflate(R.layout.item_folder_grid, this, false)
                            folderView.tag = entity

                            val tvFolderTitle = folderView.findViewById<TextView>(R.id.tvFolderTitle)
                            val ivPreview1 = folderView.findViewById<ImageView>(R.id.ivPreview1)
                            val ivPreview2 = folderView.findViewById<ImageView>(R.id.ivPreview2)
                            val ivPreview3 = folderView.findViewById<ImageView>(R.id.ivPreview3)
                            val ivPreview4 = folderView.findViewById<ImageView>(R.id.ivPreview4)

                            tvFolderTitle.text = folderResolved.title
                            tvFolderTitle.visibility = if (showLabels) View.VISIBLE else View.GONE
                            if (showLabels) {
                                val labelColor = prefs.getInt("key_label_color", android.graphics.Color.WHITE)
                                tvFolderTitle.setTextColor(labelColor)
                            }

                            val previewViews = listOf(ivPreview1, ivPreview2, ivPreview3, ivPreview4)
                            val children = folderResolved.children
                            previewViews.forEachIndexed { index, iv ->
                                if (index < children.size) {
                                    val childIcon = children[index].appInfo.icon
                                    val masked = com.example.launcherai.icons.IconShapeMasker.createMaskedIcon(
                                        context,
                                        childIcon,
                                        shape,
                                        100
                                    )
                                    iv.setImageDrawable(masked ?: childIcon)
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
                                onFolderClickListener?.invoke(folderResolved)
                            }

                            folderView.setOnLongClickListener { v ->
                                onFolderLongClickListener?.invoke(v, folderResolved, lastTouchX, lastTouchY) ?: false
                            }

                            addView(folderView)
                        }

                        is WorkspaceItemDisplay.Widget -> {
                            // Widgets are not supported in dock slots
                        }
                    }
                }
            }
        }
    }

    /**
     * Converts a local X pixel coordinate into grid dock slot index [0 until capacity].
     */
    fun pointToSlot(x: Float): Int {
        val availableW = width - paddingLeft - paddingRight
        val slotWidth = maxOf(1, availableW / maxOf(1, capacity))
        val relX = x - paddingLeft
        return (relX / slotWidth).toInt().coerceIn(0, capacity - 1)
    }

    /**
     * Returns the child View occupying dock slot index, if any.
     */
    fun getChildAtSlot(slot: Int): View? {
        if (slot in 0 until childCount) {
            val child = getChildAt(slot)
            if (child.tag != null) return child
        }
        return null
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        val slotWidth = (width - paddingLeft - paddingRight) / maxOf(1, capacity)
        val slotHeight = height - paddingTop - paddingBottom

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != GONE) {
                child.measure(
                    MeasureSpec.makeMeasureSpec(slotWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(slotHeight, MeasureSpec.EXACTLY)
                )
            }
        }

        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val width = r - l
        val height = b - t

        val slotWidth = (width - paddingLeft - paddingRight) / maxOf(1, capacity)
        val slotHeight = height - paddingTop - paddingBottom

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != GONE) {
                val childLeft = paddingLeft + i * slotWidth
                val childTop = paddingTop
                child.layout(childLeft, childTop, childLeft + slotWidth, childTop + slotHeight)
            }
        }
    }
}
