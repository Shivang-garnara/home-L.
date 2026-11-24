package com.example.launcherai.widgets

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.launcherai.R
import com.example.launcherai.dragndrop.DragView
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Nova Launcher-style Categorized & Searchable AppWidget Picker (WidgetsFullSheet equivalent).
 * Features:
 *  - Dynamically discovers all installed widget providers across user profiles.
 *  - Groups installed widgets by owning Application with App Icon, Label, and widget count badge.
 *  - Collapsible / expandable accordion sections.
 *  - Real-time search filtering across application names and widget labels with automatic expansion.
 *  - Real preview imagery, dimensions, configurable indicator, and description.
 *  - Graceful empty states and non-blocking asynchronous loading indicator.
 *  - Standard mode (bb.b): tap plays reference scale bounce animation (k0) and displays instructional Toast; long-press drags to workspace.
 *  - Tap-select mode (TapSelectWidgetsBottomSheet): tap directly selects and places widget.
 */
class WidgetPickerDialog(
    context: Context,
    private val initialProviders: List<AppWidgetProviderInfo>? = null,
    val isTapSelectMode: Boolean = true,
    private val onProviderSelected: (AppWidgetProviderInfo, spanX: Int, spanY: Int) -> Unit
) : BottomSheetDialog(context, R.style.Theme_LauncherAI) {

    private val dialogScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var adapter: GroupedWidgetAdapter
    private val appGroups = mutableListOf<AppWidgetGroup>()
    private var activeToast: Toast? = null

    var onStartWidgetDrag: ((widget: WidgetModel, previewBitmap: Bitmap, rawX: Float, rawY: Float) -> Unit)? = null
    var onDragMoveListener: ((Float, Float) -> Unit)? = null
    var onDragEndListener: ((Float, Float) -> Unit)? = null
    var onDragCancelListener: (() -> Unit)? = null
    var isDraggingWidget: Boolean = false
        private set

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (isDraggingWidget) {
            when (ev.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    onDragMoveListener?.invoke(ev.rawX, ev.rawY)
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    isDraggingWidget = false
                    onDragEndListener?.invoke(ev.rawX, ev.rawY)
                    dismiss()
                    return true
                }
                MotionEvent.ACTION_CANCEL -> {
                    isDraggingWidget = false
                    onDragCancelListener?.invoke()
                    dismiss()
                    return true
                }
            }
            return true
        }
        return super.dispatchTouchEvent(ev)
    }

    fun startInteractiveWidgetDrag(widget: WidgetModel, previewBitmap: Bitmap, rawX: Float, rawY: Float) {
        isDraggingWidget = true
        try {
            val sheet = findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            sheet?.animate()?.translationY(sheet.height.toFloat())?.alpha(0f)?.setDuration(180)?.start()
            window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        onStartWidgetDrag?.invoke(widget, previewBitmap, rawX, rawY)
    }

    sealed class ListItem {
        data class Header(val group: AppWidgetGroup) : ListItem()
        data class TableRow(val row: WidgetTableRow) : ListItem()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_widget_picker)

        // Nova parity: open directly at full expanded sheet height (WidgetsFullSheet)
        behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true

        val rv = findViewById<RecyclerView>(R.id.rvWidgetProviders)
        val etSearch = findViewById<EditText>(R.id.etWidgetSearch)
        val btnClearSearch = findViewById<ImageView>(R.id.btnClearWidgetSearch)
        val tvSearchEmpty = findViewById<TextView>(R.id.tvWidgetSearchEmpty)
        val layoutLoading = findViewById<View>(R.id.layoutWidgetLoading)
        val layoutNoWidgets = findViewById<View>(R.id.layoutNoWidgetsEmpty)

        adapter = GroupedWidgetAdapter { widget ->
            dismiss()
            onProviderSelected(widget.providerInfo, widget.spanX, widget.spanY)
        }

        rv?.layoutManager = LinearLayoutManager(context)
        rv?.adapter = adapter

        etSearch?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim() ?: ""
                btnClearSearch?.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                adapter.submitGroups(appGroups, query)
                val isEmpty = adapter.itemCount == 0 && appGroups.isNotEmpty()
                tvSearchEmpty?.visibility = if (isEmpty) View.VISIBLE else View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnClearSearch?.setOnClickListener {
            etSearch?.setText("")
        }

        loadWidgets(layoutLoading, layoutNoWidgets, rv, etSearch)
    }

    /**
     * Loads and discovers widget providers asynchronously on Dispatchers.IO.
     */
    private fun loadWidgets(
        layoutLoading: View?,
        layoutNoWidgets: View?,
        rv: RecyclerView?,
        etSearch: EditText?
    ) {
        layoutLoading?.visibility = View.VISIBLE
        layoutNoWidgets?.visibility = View.GONE
        rv?.visibility = View.GONE

        dialogScope.launch {
            val loadedGroups = withContext(Dispatchers.IO) {
                val app = context.applicationContext as? com.example.launcherai.LauncherApplication
                val gridConfig = app?.repository?.getGridConfig()
                    ?: com.example.launcherai.data.repository.WorkspaceGridConfig()

                WidgetDiscoveryHelper.loadGroupsFromSystem(
                    context = context,
                    rawProviders = initialProviders,
                    gridCols = gridConfig.cols,
                    gridRows = gridConfig.rows
                )
            }

            layoutLoading?.visibility = View.GONE
            appGroups.clear()
            appGroups.addAll(loadedGroups)

            if (appGroups.isEmpty()) {
                layoutNoWidgets?.visibility = View.VISIBLE
                rv?.visibility = View.GONE
            } else {
                layoutNoWidgets?.visibility = View.GONE
                rv?.visibility = View.VISIBLE
                adapter.submitGroups(appGroups, etSearch?.text?.toString()?.trim() ?: "")
            }
        }
    }

    fun expandGroup(appNameOrPkg: String) {
        val group = appGroups.find {
            it.appName.equals(appNameOrPkg, ignoreCase = true) ||
            it.packageName.equals(appNameOrPkg, ignoreCase = true)
        }
        if (group != null) {
            group.isExpanded = true
            val query = findViewById<EditText>(R.id.etWidgetSearch)?.text?.toString()?.trim() ?: ""
            if (::adapter.isInitialized) {
                adapter.submitGroups(appGroups, query)
                val targetIndex = adapter.findHeaderIndex(group)
                if (targetIndex >= 0) {
                    val rv = findViewById<RecyclerView>(R.id.rvWidgetProviders)
                    rv?.post {
                        rv.scrollToPosition(targetIndex)
                    }
                }
            }
        }
    }

    fun tapWidget(appNameOrPkg: String, index: Int = 0) {
        val group = appGroups.find {
            it.appName.equals(appNameOrPkg, ignoreCase = true) ||
            it.packageName.equals(appNameOrPkg, ignoreCase = true)
        }
        val widget = group?.widgets?.getOrNull(index) ?: return
        activeToast?.cancel()
        val t = Toast.makeText(context, "Touch & hold to move a widget.", Toast.LENGTH_SHORT)
        t.show()
        activeToast = t
    }

    fun longPressWidget(appNameOrPkg: String, index: Int = 0) {
        selectWidget(appNameOrPkg, index)
    }

    fun selectWidget(appNameOrPkg: String, index: Int = 0) {
        val group = appGroups.find {
            it.appName.equals(appNameOrPkg, ignoreCase = true) ||
            it.packageName.equals(appNameOrPkg, ignoreCase = true)
        }
        val widget = group?.widgets?.getOrNull(index)
        if (widget != null) {
            activeToast?.cancel()
            dismiss()
            onProviderSelected(widget.providerInfo, widget.spanX, widget.spanY)
        }
    }

    override fun onStop() {
        super.onStop()
        dialogScope.cancel()
    }

    private inner class GroupedWidgetAdapter(
        private val onWidgetSelected: (WidgetModel) -> Unit
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val displayItems = mutableListOf<ListItem>()
        private var currentQuery: String = ""

        fun submitGroups(groups: List<AppWidgetGroup>, query: String) {
            currentQuery = query
            displayItems.clear()

            for (group in groups) {
                if (query.isEmpty()) {
                    displayItems.add(ListItem.Header(group))
                    if (group.isExpanded) {
                        val packedRows = WidgetDiscoveryHelper.packWidgetsIntoRows(group.widgets)
                        packedRows.forEach { displayItems.add(ListItem.TableRow(it)) }
                    }
                } else {
                    val appMatches = group.appName.contains(query, ignoreCase = true)
                    val matchingWidgets = group.widgets.filter {
                        appMatches || it.label.contains(query, ignoreCase = true)
                    }

                    if (matchingWidgets.isNotEmpty()) {
                        // Automatically expand group in search mode so matching widgets are directly visible
                        val packedRows = WidgetDiscoveryHelper.packWidgetsIntoRows(matchingWidgets)
                        displayItems.add(ListItem.Header(group.copy(isExpanded = true, widgets = matchingWidgets)))
                        packedRows.forEach { displayItems.add(ListItem.TableRow(it)) }
                    }
                }
            }
            notifyDataSetChanged()
        }

        fun findHeaderIndex(group: AppWidgetGroup): Int {
            return displayItems.indexOfFirst {
                it is ListItem.Header && (it.group.packageName == group.packageName || it.group.appName == group.appName)
            }
        }

        override fun getItemViewType(position: Int): Int {
            return when (displayItems[position]) {
                is ListItem.Header -> VIEW_TYPE_HEADER
                is ListItem.TableRow -> VIEW_TYPE_TABLE_ROW
            }
        }

        override fun getItemCount(): Int = displayItems.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return if (viewType == VIEW_TYPE_HEADER) {
                val v = inflater.inflate(R.layout.item_widget_app_header, parent, false)
                HeaderViewHolder(v)
            } else {
                val v = inflater.inflate(R.layout.item_widget_table_row, parent, false)
                TableRowViewHolder(v)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val item = displayItems[position]) {
                is ListItem.Header -> (holder as HeaderViewHolder).bind(item.group)
                is ListItem.TableRow -> (holder as TableRowViewHolder).bind(item.row)
            }
        }

        inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val ivIcon: ImageView = view.findViewById(R.id.ivAppGroupIcon)
            private val tvTitle: TextView = view.findViewById(R.id.tvAppGroupTitle)
            private val tvCount: TextView = view.findViewById(R.id.tvAppGroupCount)
            private val ivChevron: ImageView = view.findViewById(R.id.ivAppGroupChevron)

            fun bind(group: AppWidgetGroup) {
                tvTitle.text = group.appName
                tvCount.text = "${group.widgets.size} ${if (group.widgets.size == 1) "widget" else "widgets"}"

                if (group.appIcon != null) {
                    ivIcon.setImageDrawable(group.appIcon)
                } else {
                    ivIcon.setImageResource(R.drawable.ic_apps_drawer)
                }

                ivChevron.rotation = if (group.isExpanded) 180f else 0f

                itemView.setOnClickListener {
                    if (currentQuery.isNotEmpty()) return@setOnClickListener
                    group.isExpanded = !group.isExpanded
                    submitGroups(appGroups, currentQuery)
                }
            }
        }

        inner class TableRowViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val cell1: View = view.findViewById(R.id.cellWidget1)
            private val ivPreview1: ImageView = view.findViewById(R.id.ivWidgetPreview1)
            private val tvLabel1: TextView = view.findViewById(R.id.tvWidgetLabel1)
            private val tvSpan1: TextView = view.findViewById(R.id.tvWidgetSpan1)

            private val cell2: View = view.findViewById(R.id.cellWidget2)
            private val ivPreview2: ImageView = view.findViewById(R.id.ivWidgetPreview2)
            private val tvLabel2: TextView = view.findViewById(R.id.tvWidgetLabel2)
            private val tvSpan2: TextView = view.findViewById(R.id.tvWidgetSpan2)

            fun bind(row: WidgetTableRow) {
                bindCell(cell1, ivPreview1, tvLabel1, tvSpan1, row.firstWidget)

                val second = row.secondWidget
                if (second != null) {
                    cell2.visibility = View.VISIBLE
                    bindCell(cell2, ivPreview2, tvLabel2, tvSpan2, second)
                } else {
                    cell2.visibility = View.GONE
                }
            }

            private fun playNovaBounceAnimation(view: View) {
                val animSet = AnimatorSet()
                val animUpX = ObjectAnimator.ofFloat(view, "scaleX", 1.0f, 1.2f).apply {
                    duration = 200
                    interpolator = DecelerateInterpolator()
                }
                val animUpY = ObjectAnimator.ofFloat(view, "scaleY", 1.0f, 1.2f).apply {
                    duration = 200
                    interpolator = DecelerateInterpolator()
                }
                val animDownX = ObjectAnimator.ofFloat(view, "scaleX", 1.2f, 1.0f).apply {
                    duration = 300
                    interpolator = BounceInterpolator()
                }
                val animDownY = ObjectAnimator.ofFloat(view, "scaleY", 1.2f, 1.0f).apply {
                    duration = 300
                    interpolator = BounceInterpolator()
                }
                animSet.play(animUpX).with(animUpY)
                animSet.play(animDownX).with(animDownY).after(animUpX)
                animSet.start()
            }

            private fun bindCell(
                container: View,
                ivPreview: ImageView,
                tvLabel: TextView,
                tvSpan: TextView,
                widget: WidgetModel
            ) {
                tvLabel.text = widget.label
                tvSpan.text = "${widget.spanX} × ${widget.spanY}"

                if (widget.previewDrawable != null) {
                    ivPreview.setImageDrawable(widget.previewDrawable)
                } else {
                    ivPreview.setImageResource(R.drawable.ic_apps_drawer)
                }

                var downRawX = 0f
                var downRawY = 0f
                container.setOnTouchListener { _, event ->
                    if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                        downRawX = event.rawX
                        downRawY = event.rawY
                    }
                    false
                }

                if (isTapSelectMode) {
                    // TapSelectWidgetsBottomSheet behavior: tap immediately adds to workspace
                    container.setOnClickListener {
                        dismiss()
                        onWidgetSelected(widget)
                    }
                } else {
                    // Standard WidgetsFullSheet (bb.b) behavior:
                    // Tap plays reference scale bounce (k0) and displays instructional Toast
                    container.setOnClickListener {
                        activeToast?.cancel()
                        playNovaBounceAnimation(container)
                        val t = Toast.makeText(container.context, "Touch & hold to move a widget.", Toast.LENGTH_SHORT)
                        t.show()
                        activeToast = t
                    }
                }

                // Long click initiates interactive drag-and-drop placement (g0 / PendingItemDragHelper)
                container.setOnLongClickListener {
                    activeToast?.cancel()
                    if (onStartWidgetDrag != null) {
                        val previewBitmap = DragView.createBitmapFromView(container)
                        startInteractiveWidgetDrag(widget, previewBitmap, downRawX, downRawY)
                    } else {
                        dismiss()
                        onWidgetSelected(widget)
                    }
                    true
                }
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_TABLE_ROW = 1
    }
}
