package com.example.launcherai.ui

import android.appwidget.AppWidgetHost
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.viewpager2.widget.ViewPager2
import com.example.launcherai.AppInfo
import com.example.launcherai.data.db.WorkspacePageEntity
import com.example.launcherai.data.repository.WorkspaceGridConfig

/**
 * Multi-page workspace container view wrapping ViewPager2 and PageIndicatorView.
 * Supports horizontal swiping between pages, persistent active page tracking,
 * and PageIndicator synchronization.
 */
class Workspace @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val viewPager: ViewPager2 = ViewPager2(context).apply {
        layoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        offscreenPageLimit = 2
    }

    private val pageIndicator: PageIndicatorView = PageIndicatorView(context).apply {
        val indicatorHeight = (24 * resources.displayMetrics.density).toInt()
        val lp = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            indicatorHeight
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = (8 * resources.displayMetrics.density).toInt()
        }
        layoutParams = lp
    }

    var adapter: WorkspacePagerAdapter? = null
    private var onPageSelectedListener: ((Int) -> Unit)? = null

    init {
        addView(viewPager)
        addView(pageIndicator)

        isLongClickable = true
        setOnLongClickListener {
            val activeCell = getActiveCellLayout()
            if (activeCell != null) {
                activeCell.onEmptySpaceLongClickListener?.invoke(0, 0) ?: false
            } else false
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                pageIndicator.onPageScrolled(position, positionOffset)
            }

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                pageIndicator.onPageSelected(position)
                onPageSelectedListener?.invoke(position)
                clearAllDropTargets()
            }
        })
    }

    fun setOnPageChangeListener(listener: (Int) -> Unit) {
        this.onPageSelectedListener = listener
    }

    fun updateDisplayPages(
        pages: List<WorkspacePageEntity>,
        pageDisplayItemsMap: Map<Int, List<WorkspaceItemDisplay>>,
        gridConfig: WorkspaceGridConfig = WorkspaceGridConfig(),
        appWidgetHost: AppWidgetHost? = null,
        onAppClick: (AppInfo) -> Unit,
        onFolderClick: ((WorkspaceFolderResolvedItem) -> Unit)? = null,
        onAppLongClick: ((View, WorkspaceResolvedItem, Float, Float) -> Boolean)? = null,
        onFolderLongClick: ((View, WorkspaceFolderResolvedItem, Float, Float) -> Boolean)? = null,
        onWidgetLongClick: ((View, WorkspaceWidgetResolvedItem, Float, Float) -> Boolean)? = null,
        onWidgetDragRequested: ((View, WorkspaceWidgetResolvedItem, Float, Float) -> Unit)? = null,
        onEmptySpaceLongClick: ((cellX: Int, cellY: Int) -> Boolean)? = null
    ) {
        pageIndicator.setPageCount(pages.size)
        if (adapter == null) {
            adapter = WorkspacePagerAdapter(
                pages = pages,
                pageDisplayItemsMap = pageDisplayItemsMap,
                gridConfig = gridConfig,
                appWidgetHost = appWidgetHost,
                onAppClick = onAppClick,
                onFolderClick = onFolderClick,
                onAppLongClick = onAppLongClick,
                onFolderLongClick = onFolderLongClick,
                onWidgetLongClick = onWidgetLongClick,
                onWidgetDragRequested = onWidgetDragRequested,
                onEmptySpaceLongClick = onEmptySpaceLongClick
            )
            viewPager.adapter = adapter
        } else {
            adapter?.appWidgetHost = appWidgetHost
            adapter?.updateData(pages, pageDisplayItemsMap, gridConfig)
        }
    }

    fun getActiveCellLayout(): CellLayout? {
        val recyclerView = viewPager.getChildAt(0) as? androidx.recyclerview.widget.RecyclerView ?: return null
        val viewHolder = recyclerView.findViewHolderForAdapterPosition(viewPager.currentItem)
                as? WorkspacePagerAdapter.WorkspacePageViewHolder
        return viewHolder?.cellLayout
    }

    fun clearAllDropTargets() {
        adapter?.clearAllDropTargets()
        val recyclerView = viewPager.getChildAt(0) as? androidx.recyclerview.widget.RecyclerView
        if (recyclerView != null) {
            for (i in 0 until recyclerView.childCount) {
                val child = recyclerView.getChildAt(i)
                val holder = recyclerView.getChildViewHolder(child) as? WorkspacePagerAdapter.WorkspacePageViewHolder
                holder?.cellLayout?.clearDropTarget()
            }
        }
    }

    fun getPageCount(): Int = adapter?.itemCount ?: 0

    fun getCurrentPage(): Int = viewPager.currentItem

    fun setCurrentPage(pageIndex: Int, smoothScroll: Boolean = true) {
        if (pageIndex in 0 until (adapter?.itemCount ?: 0)) {
            viewPager.setCurrentItem(pageIndex, smoothScroll)
        }
    }

    fun getCurrentPageScreenId(): Int {
        val curPos = viewPager.currentItem
        return adapter?.getPageAt(curPos)?.screenId ?: curPos
    }

    fun getPageAtPosition(position: Int): WorkspacePageEntity? {
        return adapter?.getPageAt(position)
    }

    fun getPositionForScreenId(screenId: Int): Int {
        return adapter?.getPositionForScreenId(screenId) ?: 0
    }

    fun navigateToScreenId(screenId: Int, smoothScroll: Boolean = true) {
        val targetPos = getPositionForScreenId(screenId)
        setCurrentPage(targetPos, smoothScroll)
    }

    fun notifyDataChanged() {
        adapter?.notifyDataSetChanged()
    }
}
