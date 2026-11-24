package com.example.launcherai.ui

import android.appwidget.AppWidgetHost
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.launcherai.data.db.WorkspacePageEntity
import com.example.launcherai.data.model.AppInfo
import com.example.launcherai.data.repository.WorkspaceGridConfig

/**
 * Multi-page workspace container view wrapping ViewPager2 and PageIndicatorView.
 * Supports:
 * - Horizontal page swiping
 * - Custom page transitions (Simple, Cube, Card stack, Accordion, Flip)
 * - Infinite scroll loop
 * - Synchronized PageIndicator with style and tint customization
 * - Wallpaper parallax scroll callbacks
 */
class Workspace @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    val viewPager: ViewPager2 = ViewPager2(context).apply {
        layoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        offscreenPageLimit = 2
    }

    val pageIndicator: PageIndicatorView = PageIndicatorView(context).apply {
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
    private var onPageScrolledListener: ((position: Int, positionOffset: Float, positionOffsetPixels: Int) -> Unit)? = null

    var isInfiniteScrollEnabled: Boolean = false
    private var isDraggingAtEnd = false
    private var lastScrollState = ViewPager2.SCROLL_STATE_IDLE

    init {
        addView(viewPager)
        addView(pageIndicator)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                pageIndicator.onPageScrolled(position, positionOffset)
                onPageScrolledListener?.invoke(position, positionOffset, positionOffsetPixels)

                val count = adapter?.itemCount ?: 0
                if (isInfiniteScrollEnabled && count >= 2) {
                    if (position == count - 1 && positionOffset > 0.05f) {
                        isDraggingAtEnd = true
                    }
                }
            }

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                pageIndicator.onPageSelected(position)
                onPageSelectedListener?.invoke(position)
                isDraggingAtEnd = false
            }

            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                val count = adapter?.itemCount ?: 0
                if (isInfiniteScrollEnabled && count >= 2) {
                    if (lastScrollState == ViewPager2.SCROLL_STATE_DRAGGING && state == ViewPager2.SCROLL_STATE_SETTLING) {
                        if (isDraggingAtEnd && viewPager.currentItem == count - 1) {
                            viewPager.setCurrentItem(0, true)
                            isDraggingAtEnd = false
                        }
                    }
                }
                lastScrollState = state
            }
        })
    }

    fun setPageTransformer(transformer: ViewPager2.PageTransformer) {
        viewPager.setPageTransformer(transformer)
    }

    fun setPageIndicatorStyle(style: PageIndicatorView.Style) {
        pageIndicator.setStyle(style)
    }

    fun setPageIndicatorColor(color: Int) {
        pageIndicator.setIndicatorColor(color)
    }

    fun setOnPageChangeListener(listener: (Int) -> Unit) {
        this.onPageSelectedListener = listener
    }

    fun setOnPageScrolledListener(listener: (position: Int, positionOffset: Float, positionOffsetPixels: Int) -> Unit) {
        this.onPageScrolledListener = listener
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
        onWidgetLongClick: ((View, WorkspaceWidgetResolvedItem, Float, Float) -> Boolean)? = null
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
                onWidgetLongClick = onWidgetLongClick
            )
            viewPager.adapter = adapter
        } else {
            adapter?.appWidgetHost = appWidgetHost
            adapter?.updateData(pages, pageDisplayItemsMap, gridConfig)
        }
    }

    fun getActiveCellLayout(): CellLayout? {
        val recyclerView = viewPager.getChildAt(0) as? RecyclerView ?: return null
        val viewHolder = recyclerView.findViewHolderForAdapterPosition(viewPager.currentItem)
                as? WorkspacePagerAdapter.WorkspacePageViewHolder
        return viewHolder?.cellLayout
    }

    fun getPageCount(): Int = adapter?.itemCount ?: 0

    fun getCurrentPage(): Int = viewPager.currentItem

    fun setCurrentPage(pageIndex: Int, smoothScroll: Boolean = true) {
        if (pageIndex in 0 until (adapter?.itemCount ?: 0)) {
            viewPager.setCurrentItem(pageIndex, smoothScroll)
        }
    }

    fun notifyDataChanged() {
        adapter?.notifyDataSetChanged()
    }
}
