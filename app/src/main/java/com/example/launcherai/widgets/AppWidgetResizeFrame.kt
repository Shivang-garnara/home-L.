package com.example.launcherai.widgets

import android.annotation.SuppressLint
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.example.launcherai.R
import com.example.launcherai.data.db.WorkspaceItemEntity
import com.example.launcherai.ui.CellLayout
import com.example.launcherai.ui.DragLayer
import kotlin.math.roundToInt

/**
 * Interactive AppWidget resizing frame modeled after Nova Launcher (com.android.launcher3.AppWidgetResizeFrame).
 * Displays a glowing bounding box over the target widget with 4 directional handles (Left, Top, Right, Bottom),
 * real-time cell span indicator badge ("3 × 2"), collision checking with CellLayout,
 * and quick reconfigure button if the provider provides a configuration activity.
 */
@SuppressLint("ClickableViewAccessibility")
class AppWidgetResizeFrame(
    context: Context,
    private val dragLayer: DragLayer,
    private val cellLayout: CellLayout,
    private val hostView: LauncherAppWidgetHostView,
    private val widgetEntity: WorkspaceItemEntity,
    private val providerInfo: AppWidgetProviderInfo,
    private val onResizeCommitted: (newCellX: Int, newCellY: Int, newSpanX: Int, newSpanY: Int) -> Unit,
    private val onReconfigureClicked: ((widgetId: Int, providerInfo: AppWidgetProviderInfo) -> Unit)? = null
) : FrameLayout(context) {

    private val frameView: View
    private val tvResizeSpan: TextView
    private val btnReconfigureWidget: ImageView
    private val btnDoneResize: ImageView
    private val handleLeft: View
    private val handleRight: View
    private val handleTop: View
    private val handleBottom: View

    private var currentCellX = widgetEntity.cellX
    private var currentCellY = widgetEntity.cellY
    private var currentSpanX = widgetEntity.spanX
    private var currentSpanY = widgetEntity.spanY

    private val minSpanX: Int
    private val minSpanY: Int
    private val maxSpanX: Int = cellLayout.countX
    private val maxSpanY: Int = cellLayout.countY

    var onBodyDragRequested: ((rawX: Float, rawY: Float) -> Unit)? = null
    private val touchSlop = android.view.ViewConfiguration.get(context).scaledTouchSlop
    private var bodyDownRawX = 0f
    private var bodyDownRawY = 0f
    private var isBodyDragging = false

    private var isDismissed = false

    init {
        clipChildren = false
        clipToPadding = false

        frameView = LayoutInflater.from(context).inflate(R.layout.layout_widget_resize_frame, this, true)

        tvResizeSpan = findViewById(R.id.tvResizeSpan)
        btnReconfigureWidget = findViewById(R.id.btnReconfigureWidget)
        btnDoneResize = findViewById(R.id.btnDoneResize)
        handleLeft = findViewById(R.id.handleLeft)
        handleRight = findViewById(R.id.handleRight)
        handleTop = findViewById(R.id.handleTop)
        handleBottom = findViewById(R.id.handleBottom)

        // Compute minSpan from provider info matching Nova's LauncherAppWidgetProviderInfo.p()
        val density = context.resources.displayMetrics.density
        val (computedMinX, computedMinY) = WidgetDiscoveryHelper.calculateMinResizeSpans(
            context = context,
            provider = providerInfo,
            defaultSpanX = currentSpanX,
            defaultSpanY = currentSpanY,
            gridCols = maxSpanX,
            gridRows = maxSpanY,
            density = density
        )
        minSpanX = computedMinX
        minSpanY = computedMinY

        // Resize mode handles visibility
        val resizeMode = providerInfo.resizeMode
        val allowH = ((resizeMode and AppWidgetProviderInfo.RESIZE_HORIZONTAL) != 0 || resizeMode == 0) && minSpanX < maxSpanX
        val allowV = ((resizeMode and AppWidgetProviderInfo.RESIZE_VERTICAL) != 0 || resizeMode == 0) && minSpanY < maxSpanY

        handleLeft.visibility = if (allowH) View.VISIBLE else View.GONE
        handleRight.visibility = if (allowH) View.VISIBLE else View.GONE
        handleTop.visibility = if (allowV) View.VISIBLE else View.GONE
        handleBottom.visibility = if (allowV) View.VISIBLE else View.GONE

        // Reconfigure button visibility
        if (providerInfo.configure != null && onReconfigureClicked != null) {
            btnReconfigureWidget.visibility = View.VISIBLE
            btnReconfigureWidget.setOnClickListener {
                dismiss()
                onReconfigureClicked.invoke(widgetEntity.appWidgetId, providerInfo)
            }
        }

        btnDoneResize.setOnClickListener {
            dismiss()
        }

        updateSpanBadge()
        setupHandleTouchListeners()
    }

    private fun updateSpanBadge() {
        tvResizeSpan.text = "$currentSpanX × $currentSpanY"
    }

    fun show() {
        dragLayer.activeResizeFrame = this
        val lp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dragLayer.addView(this, lp)
        updateFramePosition()
    }

    fun updateFramePosition() {
        post {
            val dragLayerLoc = IntArray(2)
            dragLayer.getLocationOnScreen(dragLayerLoc)

            val hostLoc = IntArray(2)
            hostView.getLocationOnScreen(hostLoc)

            val relX = hostLoc[0] - dragLayerLoc[0]
            val relY = hostLoc[1] - dragLayerLoc[1]

            val lp = layoutParams as? FrameLayout.LayoutParams ?: return@post
            lp.width = hostView.width
            lp.height = hostView.height
            lp.leftMargin = relX
            lp.topMargin = relY
            layoutParams = lp
            requestLayout()
        }
    }

    private fun setupHandleTouchListeners() {
        var startRawX = 0f
        var startRawY = 0f
        var startCellX = currentCellX
        var startCellY = currentCellY
        var startSpanX = currentSpanX
        var startSpanY = currentSpanY

        fun onHandleTouch(view: View, event: MotionEvent, handleType: Int): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startRawX = event.rawX
                    startRawY = event.rawY
                    startCellX = currentCellX
                    startCellY = currentCellY
                    startSpanX = currentSpanX
                    startSpanY = currentSpanY
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - startRawX
                    val deltaY = event.rawY - startRawY

                    val cellW = cellLayout.getCellWidth()
                    val cellH = cellLayout.getCellHeight()
                    if (cellW <= 0 || cellH <= 0) return true

                    val deltaCellsX = (deltaX / cellW).roundToInt()
                    val deltaCellsY = (deltaY / cellH).roundToInt()

                    var candidateCellX = startCellX
                    var candidateCellY = startCellY
                    var candidateSpanX = startSpanX
                    var candidateSpanY = startSpanY

                    when (handleType) {
                        HANDLE_RIGHT -> {
                            val maxAvailableSpanX = cellLayout.countX - startCellX
                            candidateSpanX = (startSpanX + deltaCellsX).coerceIn(minSpanX, maxAvailableSpanX)
                        }
                        HANDLE_LEFT -> {
                            val maxSpanLeft = startCellX + startSpanX
                            candidateSpanX = (startSpanX - deltaCellsX).coerceIn(minSpanX, maxSpanLeft)
                            candidateCellX = (startCellX + startSpanX) - candidateSpanX
                        }
                        HANDLE_BOTTOM -> {
                            val maxAvailableSpanY = cellLayout.countY - startCellY
                            candidateSpanY = (startSpanY + deltaCellsY).coerceIn(minSpanY, maxAvailableSpanY)
                        }
                        HANDLE_TOP -> {
                            val maxSpanTop = startCellY + startSpanY
                            candidateSpanY = (startSpanY - deltaCellsY).coerceIn(minSpanY, maxSpanTop)
                            candidateCellY = (startCellY + startSpanY) - candidateSpanY
                        }
                    }

                    if (candidateCellX != currentCellX || candidateCellY != currentCellY ||
                        candidateSpanX != currentSpanX || candidateSpanY != currentSpanY
                    ) {
                        val available = cellLayout.isRegionAvailableForChild(
                            candidateCellX,
                            candidateCellY,
                            candidateSpanX,
                            candidateSpanY,
                            hostView
                        )
                        if (available) {
                            currentCellX = candidateCellX
                            currentCellY = candidateCellY
                            currentSpanX = candidateSpanX
                            currentSpanY = candidateSpanY

                            cellLayout.updateChildSpanAndPosition(
                                hostView,
                                currentCellX,
                                currentCellY,
                                currentSpanX,
                                currentSpanY
                            )
                            hostView.updateSizeOptionsForPixels(
                                cellLayout.getSpanWidthPx(currentCellX, currentSpanX),
                                cellLayout.getSpanHeightPx(currentCellY, currentSpanY)
                            )
                            updateSpanBadge()
                            updateFramePosition()
                        }
                    }
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    parent?.requestDisallowInterceptTouchEvent(false)
                    updateFramePosition()
                    return true
                }
            }
            return false
        }

        handleRight.setOnTouchListener { v, ev -> onHandleTouch(v, ev, HANDLE_RIGHT) }
        handleLeft.setOnTouchListener { v, ev -> onHandleTouch(v, ev, HANDLE_LEFT) }
        handleBottom.setOnTouchListener { v, ev -> onHandleTouch(v, ev, HANDLE_BOTTOM) }
        handleTop.setOnTouchListener { v, ev -> onHandleTouch(v, ev, HANDLE_TOP) }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                bodyDownRawX = event.rawX
                bodyDownRawY = event.rawY
                isBodyDragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isBodyDragging) {
                    val dx = event.rawX - bodyDownRawX
                    val dy = event.rawY - bodyDownRawY
                    if ((dx * dx + dy * dy) > (touchSlop * touchSlop)) {
                        isBodyDragging = true
                        onBodyDragRequested?.invoke(event.rawX, event.rawY)
                        return true
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isBodyDragging = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun dismiss() {
        if (isDismissed) return
        isDismissed = true

        if (dragLayer.activeResizeFrame === this) {
            dragLayer.activeResizeFrame = null
        }
        onResizeCommitted.invoke(currentCellX, currentCellY, currentSpanX, currentSpanY)
        dragLayer.removeView(this)
    }

    companion object {
        private const val HANDLE_LEFT = 1
        private const val HANDLE_RIGHT = 2
        private const val HANDLE_TOP = 3
        private const val HANDLE_BOTTOM = 4

        fun show(
            dragLayer: DragLayer,
            cellLayout: CellLayout,
            hostView: LauncherAppWidgetHostView,
            widgetEntity: WorkspaceItemEntity,
            providerInfo: AppWidgetProviderInfo,
            onResizeCommitted: (newCellX: Int, newCellY: Int, newSpanX: Int, newSpanY: Int) -> Unit,
            onReconfigureClicked: ((widgetId: Int, providerInfo: AppWidgetProviderInfo) -> Unit)? = null,
            onBodyDragRequested: ((rawX: Float, rawY: Float) -> Unit)? = null
        ): AppWidgetResizeFrame {
            val frame = AppWidgetResizeFrame(
                context = dragLayer.context,
                dragLayer = dragLayer,
                cellLayout = cellLayout,
                hostView = hostView,
                widgetEntity = widgetEntity,
                providerInfo = providerInfo,
                onResizeCommitted = onResizeCommitted,
                onReconfigureClicked = onReconfigureClicked
            )
            frame.onBodyDragRequested = onBodyDragRequested
            frame.show()
            return frame
        }
    }
}
