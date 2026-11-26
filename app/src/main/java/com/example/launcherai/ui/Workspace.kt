package com.example.launcherai.ui

import android.appwidget.AppWidgetHost
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.launcherai.data.db.WorkspacePageEntity
import com.example.launcherai.data.model.AppInfo
import com.example.launcherai.data.repository.WorkspaceGridConfig
import com.example.launcherai.gestures.GestureMath
import com.example.launcherai.gestures.GestureSlot
import com.example.launcherai.gestures.NamedAction
import kotlin.math.abs

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
    var onEdgeSwipeListener: ((GestureSlot) -> Unit)? = null
    var isSwipeLeftGestureBound: () -> Boolean = { false }
    var isSwipeRightGestureBound: () -> Boolean = { false }

    var isInfiniteScrollEnabled: Boolean = false
    private var isDraggingAtEnd = false
    private var lastScrollState = ViewPager2.SCROLL_STATE_IDLE

    init {
        addView(viewPager)
        addView(pageIndicator)

        installEdgeOverscrollGestureTracker()

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
                clearAllDropTargets()
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
        onAppClick: (AppInfo, View?) -> Unit,
        onFolderClick: ((WorkspaceFolderResolvedItem) -> Unit)? = null,
        onItemSwipeAction: ((View, com.example.launcherai.data.db.WorkspaceItemEntity, NamedAction) -> Unit)? = null,
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
                onItemSwipeAction = onItemSwipeAction,
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

    /**
     * Mirrors Nova Workspace + PagedView + fz.i edge overscroll detection:
     * Pulling past the outer boundary page (when Infinite Scroll is off and Swipe Left/Right is bound)
     * captures the drag past touchSlop and fires onRelease when accumulated pull >= 0.30 of page width.
     */
    private fun installEdgeOverscrollGestureTracker() {
        val rv = viewPager.getChildAt(0) as? RecyclerView ?: return
        val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        val maxPullVisualPx = 24f * resources.displayMetrics.density

        var startX = 0f
        var startY = 0f
        var edgeTracking = 0 // 1 = left edge (Swipe right), -1 = right edge (Swipe left)
        var pullFraction = 0f

        rv.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(recyclerView: RecyclerView, e: MotionEvent): Boolean {
                if (isInfiniteScrollEnabled || e.pointerCount > 1) {
                    edgeTracking = 0
                    pullFraction = 0f
                    return false
                }
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = e.x
                        startY = e.y
                        edgeTracking = 0
                        pullFraction = 0f
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = e.x - startX
                        val dy = e.y - startY
                        if (abs(dx) > touchSlop && abs(dx) > abs(dy) * 1.4f) {
                            val curPage = viewPager.currentItem
                            val lastPage = (adapter?.itemCount ?: 1) - 1
                            if (curPage == 0 && dx > touchSlop && isSwipeRightGestureBound()) {
                                edgeTracking = 1
                                return true
                            }
                            if (lastPage >= 0 && curPage == lastPage && dx < -touchSlop && isSwipeLeftGestureBound()) {
                                edgeTracking = -1
                                return true
                            }
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        edgeTracking = 0
                        pullFraction = 0f
                    }
                }
                return false
            }

            override fun onTouchEvent(recyclerView: RecyclerView, e: MotionEvent) {
                if (edgeTracking == 0) return
                when (e.actionMasked) {
                    MotionEvent.ACTION_MOVE -> {
                        val dx = e.x - startX
                        val signedDx = if (edgeTracking > 0) dx else -dx
                        val w = recyclerView.width.coerceAtLeast(1).toFloat()
                        pullFraction = (signedDx / w).coerceAtLeast(0f)
                        recyclerView.translationX = (if (edgeTracking > 0) {
                            (signedDx * 0.16f).coerceIn(0f, maxPullVisualPx)
                        } else {
                            -(signedDx * 0.16f).coerceIn(0f, maxPullVisualPx)
                        })
                    }
                    MotionEvent.ACTION_UP -> {
                        val triggeredDir = edgeTracking
                        val finalPull = pullFraction
                        edgeTracking = 0
                        pullFraction = 0f
                        recyclerView.animate().translationX(0f).setDuration(150L).start()
                        if (GestureMath.shouldTriggerEdgePull(finalPull)) {
                            onEdgeSwipeListener?.invoke(
                                if (triggeredDir > 0) GestureSlot.SWIPE_RIGHT else GestureSlot.SWIPE_LEFT
                            )
                        }
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        edgeTracking = 0
                        pullFraction = 0f
                        recyclerView.animate().translationX(0f).setDuration(150L).start()
                    }
                }
            }

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })
    }

    fun getActiveCellLayout(): CellLayout? {
        val recyclerView = viewPager.getChildAt(0) as? RecyclerView ?: return null
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
