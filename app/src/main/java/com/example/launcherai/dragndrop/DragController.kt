package com.example.launcherai.dragndrop

import android.content.Context
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import com.example.launcherai.R
import com.example.launcherai.data.db.WorkspaceItemEntity
import com.example.launcherai.data.model.AppInfo
import com.example.launcherai.data.model.SmartCategoryHelper
import com.example.launcherai.ui.DragLayer
import com.example.launcherai.ui.Workspace
import com.example.launcherai.ui.WorkspaceResolvedItem
import com.example.launcherai.ui.WorkspaceViewModel
import com.example.launcherai.util.AppManager

class DragController(
    private val context: Context,
    var dragLayer: DragLayer? = null,
    var workspace: Workspace? = null,
    var dockLayout: View? = null,
    var dropTargetBar: View? = null,
    var viewModel: WorkspaceViewModel? = null,
    var appWidgetHost: android.appwidget.AppWidgetHost? = null
) {
    var isDragging: Boolean = false
        private set

    var draggedItem: WorkspaceResolvedItem? = null
        private set

    data class PendingWidgetDrag(
        val providerInfo: android.appwidget.AppWidgetProviderInfo,
        val spanX: Int,
        val spanY: Int,
        val label: String,
        val previewBitmap: android.graphics.Bitmap
    )

    var pendingWidget: PendingWidgetDrag? = null
        private set

    var onPendingWidgetDropped: ((providerInfo: android.appwidget.AppWidgetProviderInfo, screenId: Int, cellX: Int, cellY: Int, spanX: Int, spanY: Int) -> Unit)? = null
    var onDragEnded: ((item: WorkspaceResolvedItem?, hitTarget: DropTargetType?) -> Unit)? = null

    var onItemRemovedListener: ((WorkspaceResolvedItem) -> Unit)? = null
    var onItemClickedInPlaceListener: ((View, WorkspaceResolvedItem) -> Unit)? = null
    var onDragStartedMovingListener: (() -> Unit)? = null
    private var lastHoverTarget: DropTargetType? = null

    private var initialMoveX: Float = -1f
    private var initialMoveY: Float = -1f
    private var hasMovedBeyondSlop: Boolean = false

    private var sourceView: View? = null
    private var dragView: DragView? = null
    private var dragRegistrationX: Int = 0
    private var dragRegistrationY: Int = 0
    private var lastDropTargetCellLayout: com.example.launcherai.ui.CellLayout? = null

    private var targetRemove: View? = null
    private var targetAppInfo: View? = null
    private var targetUninstall: View? = null

    private val edgeScrollHandler = Handler(Looper.getMainLooper())
    private var isEdgeScrolling = false
    private var edgeScrollDirection = 0
    private var lastScreenCreationTime: Long = 0L

    private val edgeScrollRunnable = object : Runnable {
        override fun run() {
            if (!isDragging) {
                isEdgeScrolling = false
                return
            }
            val ws = workspace ?: run {
                isEdgeScrolling = false
                return
            }
            val curPage = ws.getCurrentPage()
            val totalPages = ws.getPageCount()

            if (edgeScrollDirection == -1) {
                if (curPage > 0) {
                    ws.clearAllDropTargets()
                    lastDropTargetCellLayout?.clearDropTarget()
                    lastDropTargetCellLayout = null
                    ws.setCurrentPage(curPage - 1, smoothScroll = true)
                    edgeScrollHandler.postDelayed(this, 700)
                } else {
                    isEdgeScrolling = false
                }
            } else if (edgeScrollDirection == 1) {
                if (curPage < totalPages - 1) {
                    ws.clearAllDropTargets()
                    lastDropTargetCellLayout?.clearDropTarget()
                    lastDropTargetCellLayout = null
                    ws.setCurrentPage(curPage + 1, smoothScroll = true)
                    edgeScrollHandler.postDelayed(this, 700)
                } else {
                    val now = System.currentTimeMillis()
                    if (now - lastScreenCreationTime > 1200) {
                        lastScreenCreationTime = now
                        viewModel?.addNewPage { newPage ->
                            ws.post {
                                val updatedTotal = ws.getPageCount()
                                if (updatedTotal > totalPages) {
                                    ws.setCurrentPage(updatedTotal - 1, smoothScroll = true)
                                }
                            }
                        }
                        edgeScrollHandler.postDelayed(this, 1000)
                    } else {
                        isEdgeScrolling = false
                    }
                }
            }
        }
    }

    fun initDropTargetBar(bar: View) {
        this.dropTargetBar = bar
        this.targetRemove = bar.findViewById(R.id.targetRemove)
        this.targetAppInfo = bar.findViewById(R.id.targetAppInfo)
        this.targetUninstall = bar.findViewById(R.id.targetUninstall)
    }

    fun startDrag(v: View, item: WorkspaceResolvedItem, touchX: Float, touchY: Float) {
        val prefs = context.getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("key_desktop_lock", false) && item.entity.id != 0L) {
            Toast.makeText(context, "Home screen layout is locked", Toast.LENGTH_SHORT).show()
            return
        }
        if (isDragging) return
        isDragging = true
        draggedItem = item
        pendingWidget = null
        sourceView = v
        initialMoveX = -1f
        initialMoveY = -1f
        hasMovedBeyondSlop = false

        v.parent?.requestDisallowInterceptTouchEvent(false)
        dragLayer?.requestDisallowInterceptTouchEvent(false)

        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        v.alpha = 0.2f

        val lp = v.layoutParams as? com.example.launcherai.ui.CellLayout.LayoutParams
        if (lp != null && v.parent is com.example.launcherai.ui.CellLayout) {
            (v.parent as com.example.launcherai.ui.CellLayout).markCellsAsUnoccupied(lp.cellX, lp.cellY, lp.cellHSpan, lp.cellVSpan)
        }

        val bitmap = DragView.createBitmapFromView(v)
        val viewLoc = IntArray(2)
        v.getLocationOnScreen(viewLoc)
        val regX = (touchX - viewLoc[0]).toInt().coerceIn(0, maxOf(1, v.width))
        val regY = (touchY - viewLoc[1]).toInt().coerceIn(0, maxOf(1, v.height))
        dragRegistrationX = regX
        dragRegistrationY = regY

        val dv = DragView(context, bitmap, regX, regY)
        dragView = dv

        val frameLp = FrameLayout.LayoutParams(bitmap.width, bitmap.height)
        dragLayer?.addView(dv, frameLp)

        val dragLayerLoc = IntArray(2)
        dragLayer?.getLocationOnScreen(dragLayerLoc)
        val localTouchX = touchX - dragLayerLoc[0]
        val localTouchY = touchY - dragLayerLoc[1]
        dv.move(localTouchX, localTouchY)

        dropTargetBar?.visibility = View.VISIBLE
    }

    fun startPendingWidgetDrag(
        providerInfo: android.appwidget.AppWidgetProviderInfo,
        spanX: Int,
        spanY: Int,
        label: String,
        previewBitmap: android.graphics.Bitmap,
        touchX: Float,
        touchY: Float
    ) {
        if (isDragging) return
        isDragging = true
        pendingWidget = PendingWidgetDrag(providerInfo, spanX, spanY, label, previewBitmap)
        draggedItem = null
        sourceView = null

        val regX = previewBitmap.width / 2
        val regY = previewBitmap.height / 2
        dragRegistrationX = regX
        dragRegistrationY = regY

        val dv = DragView(context, previewBitmap, regX, regY)
        dragView = dv

        val lp = FrameLayout.LayoutParams(previewBitmap.width, previewBitmap.height)
        dragLayer?.addView(dv, lp)
        dv.move(touchX, touchY)

        dropTargetBar?.visibility = View.GONE
    }

    fun onDragMove(touchX: Float, touchY: Float) {
        if (!isDragging) return
        if (initialMoveX < 0f) {
            initialMoveX = touchX
            initialMoveY = touchY
        }
        val dx = touchX - initialMoveX
        val dy = touchY - initialMoveY
        val touchSlop = 20f * context.resources.displayMetrics.density
        val dist = Math.hypot(dx.toDouble(), dy.toDouble())
        if (!hasMovedBeyondSlop && dist > touchSlop) {
            hasMovedBeyondSlop = true
            onDragStartedMovingListener?.invoke()
            android.util.Log.d("DragController", "Moved beyond slop! dist=$dist touchSlop=$touchSlop")
        }

        dragView?.move(touchX, touchY)

        checkDropTargetHover(touchX, touchY)
        checkEdgeScroll(touchX)
        updateDropTargetOutline(touchX, touchY)
    }

    fun estimateDropCell(
        localX: Float,
        localY: Float,
        spanX: Int,
        spanY: Int,
        cellLayout: com.example.launcherai.ui.CellLayout
    ): IntArray {
        val usableW = maxOf(1f, (cellLayout.width - cellLayout.paddingLeft - cellLayout.paddingRight).toFloat())
        val usableH = maxOf(1f, (cellLayout.height - cellLayout.paddingTop - cellLayout.paddingBottom).toFloat())

        val cellW = maxOf(1f, cellLayout.getCellWidth().toFloat())
        val cellH = maxOf(1f, cellLayout.getCellHeight().toFloat())
        val pLeft = cellLayout.paddingLeft.toFloat()
        val pTop = cellLayout.paddingTop.toFloat()

        val regX = if (sourceView != null && dragRegistrationX > 0) {
            dragRegistrationX.toFloat()
        } else {
            (spanX * cellW) / 2f
        }
        val regY = if (sourceView != null && dragRegistrationY > 0) {
            dragRegistrationY.toFloat()
        } else {
            (spanY * cellH) / 2f
        }

        return calculateDropCell(
            localX = localX,
            localY = localY,
            spanX = spanX,
            spanY = spanY,
            cellW = cellW,
            cellH = cellH,
            pLeft = pLeft,
            pTop = pTop,
            regX = regX,
            regY = regY,
            countX = cellLayout.countX,
            countY = cellLayout.countY,
            usableW = usableW,
            usableH = usableH
        )
    }

    private fun updateDropTargetOutline(touchX: Float, touchY: Float) {
        val ws = workspace ?: return
        val cellLayout = ws.getActiveCellLayout() ?: return
        val loc = IntArray(2)
        cellLayout.getLocationOnScreen(loc)
        val localX = touchX - loc[0]
        val localY = touchY - loc[1]

        if (localX >= 0 && localX <= cellLayout.width && localY >= 0 && localY <= cellLayout.height) {
            val spanX = pendingWidget?.spanX ?: draggedItem?.entity?.spanX ?: 1
            val spanY = pendingWidget?.spanY ?: draggedItem?.entity?.spanY ?: 1
            val targetCell = estimateDropCell(localX, localY, spanX, spanY, cellLayout)

            val isValid = if (sourceView != null && cellLayout == sourceView?.parent) {
                cellLayout.isRegionAvailableForChild(targetCell[0], targetCell[1], spanX, spanY, sourceView!!)
            } else {
                cellLayout.isCellEmpty(targetCell[0], targetCell[1], spanX, spanY)
            }
            if (lastDropTargetCellLayout != null && lastDropTargetCellLayout != cellLayout) {
                lastDropTargetCellLayout?.clearDropTarget()
            }
            lastDropTargetCellLayout = cellLayout
            cellLayout.setDropTarget(targetCell[0], targetCell[1], spanX, spanY, isValid)
        } else {
            cellLayout.clearDropTarget()
            if (lastDropTargetCellLayout == cellLayout) {
                lastDropTargetCellLayout = null
            }
        }
    }

    fun endDrag(touchX: Float, touchY: Float) {
        if (!isDragging) return
        stopEdgeScroll()
        lastDropTargetCellLayout?.clearDropTarget()
        lastDropTargetCellLayout = null
        workspace?.clearAllDropTargets()
        android.util.Log.d("DragController", "endDrag: hasMovedBeyondSlop=$hasMovedBeyondSlop hitTarget=${evaluateDropTargetHit(touchX, touchY)}")

        val item = draggedItem
        val pending = pendingWidget
        val ws = workspace

        val hitTarget = evaluateDropTargetHit(touchX, touchY)
        val dock = dockLayout as? com.example.launcherai.ui.DockLayout
        val isDropOverDock = if (dock != null && dock.visibility == View.VISIBLE) {
            val loc = IntArray(2)
            dock.getLocationOnScreen(loc)
            val dockRect = Rect(loc[0], loc[1], loc[0] + dock.width, loc[1] + dock.height)
            dockRect.contains(touchX.toInt(), touchY.toInt())
        } else false

        if (!hasMovedBeyondSlop && hitTarget == null && item != null && sourceView != null) {
            onItemClickedInPlaceListener?.invoke(sourceView!!, item)
            cleanupDrag()
            return
        }

        if (hitTarget != null) {
            if (pending != null) {
                Toast.makeText(context, "Cancelled adding ${pending.label}", Toast.LENGTH_SHORT).show()
            } else if (item != null) {
                when (hitTarget) {
                    DropTargetType.REMOVE -> {
                        if (item.entity.itemType == WorkspaceItemEntity.ITEM_TYPE_WIDGET && item.entity.appWidgetId != -1) {
                            appWidgetHost?.deleteAppWidgetId(item.entity.appWidgetId)
                        }
                        val sourceFolderId = item.entity.container
                        viewModel?.deleteWorkspaceItem(item.entity.id)
                        if (sourceFolderId > 0) {
                            viewModel?.evaluateAndCleanupFolder(sourceFolderId.toLong())
                        }
                        onItemRemovedListener?.invoke(item)
                        Toast.makeText(context, "Removed ${item.appInfo.label} from launcher", Toast.LENGTH_SHORT).show()
                    }
                    DropTargetType.APP_INFO -> {
                        AppManager.openAppDetails(context, item.appInfo.packageName)
                    }
                    DropTargetType.UNINSTALL -> {
                        AppManager.uninstallApp(context, item.appInfo.packageName)
                    }
                }
            }
        } else if (isDropOverDock) {
            if (pending != null) {
                Toast.makeText(context, "Widgets cannot be placed in the dock", Toast.LENGTH_SHORT).show()
            } else if (dock != null && item != null) {
                val loc = IntArray(2)
                dock.getLocationOnScreen(loc)
                val localX = touchX - loc[0]
                val targetSlot = dock.pointToSlot(localX)

                if (targetSlot == dock.appDrawerSlot) {
                    Toast.makeText(context, "App Drawer button cannot be replaced", Toast.LENGTH_SHORT).show()
                } else {
                    val targetChildView = dock.getChildAtSlot(targetSlot)
                    val targetEntity = targetChildView?.tag as? WorkspaceItemEntity

                    if (targetEntity != null && targetEntity.id != item.entity.id) {
                        if (targetEntity.itemType == WorkspaceItemEntity.ITEM_TYPE_APP && item.entity.itemType == WorkspaceItemEntity.ITEM_TYPE_APP) {
                            val inferredTitle = inferFolderTitle(targetEntity, item)
                            viewModel?.createFolder(
                                existingItem = targetEntity,
                                draggedItem = item.entity,
                                folderTitle = inferredTitle
                            )
                            val msg = if (inferredTitle == "Folder") "Folder created in dock" else "\"$inferredTitle\" folder created in dock"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        } else if (targetEntity.itemType == WorkspaceItemEntity.ITEM_TYPE_FOLDER) {
                            viewModel?.addItemToFolder(
                                folderId = targetEntity.id,
                                item = item.entity
                            )
                            Toast.makeText(context, "Added to dock folder", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        if (item.entity.id == 0L) {
                            // App dragged from drawer onto dock
                            viewModel?.addWorkspaceItem(
                                item.entity.copy(
                                    container = WorkspaceItemEntity.CONTAINER_DOCK,
                                    screenId = 0,
                                    cellX = targetSlot,
                                    cellY = 0,
                                    spanX = 1,
                                    spanY = 1,
                                    rank = targetSlot
                                )
                            )
                            Toast.makeText(context, "Added ${item.appInfo.label} to dock", Toast.LENGTH_SHORT).show()
                        } else {
                            val sourceFolderId = item.entity.container
                            viewModel?.updateDockItemPosition(item.entity.id, targetSlot)
                            if (sourceFolderId > 0) {
                                viewModel?.evaluateAndCleanupFolder(sourceFolderId.toLong())
                            }
                            Toast.makeText(context, "Positioned in dock", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        } else if (pending != null && ws != null) {
            val curScreenId = ws.getCurrentPageScreenId()
            val cellLayout = ws.getActiveCellLayout()
            if (cellLayout != null) {
                val loc = IntArray(2)
                cellLayout.getLocationOnScreen(loc)
                val localX = touchX - loc[0]
                val localY = touchY - loc[1]

                val targetCell = estimateDropCell(localX, localY, pending.spanX, pending.spanY, cellLayout)
                val targetCellX = targetCell[0]
                val targetCellY = targetCell[1]

                val finalCell = if (cellLayout.isCellEmpty(targetCellX, targetCellY, pending.spanX, pending.spanY)) {
                    intArrayOf(targetCellX, targetCellY)
                } else {
                    cellLayout.findVacantCell(pending.spanX, pending.spanY)
                }

                if (finalCell != null) {
                    onPendingWidgetDropped?.invoke(
                        pending.providerInfo,
                        curScreenId,
                        finalCell[0],
                        finalCell[1],
                        pending.spanX,
                        pending.spanY
                    )
                } else {
                    Toast.makeText(context, "No space on page for ${pending.label}", Toast.LENGTH_SHORT).show()
                }
            }
        } else if (item != null && ws != null) {
            val curScreenId = ws.getCurrentPageScreenId()
            val cellLayout = ws.getActiveCellLayout()
            if (cellLayout != null) {
                val loc = IntArray(2)
                cellLayout.getLocationOnScreen(loc)
                val localX = touchX - loc[0]
                val localY = touchY - loc[1]

                val spanX = item.entity.spanX
                val spanY = item.entity.spanY
                val targetCell = estimateDropCell(localX, localY, spanX, spanY, cellLayout)
                val targetCellX = targetCell[0]
                val targetCellY = targetCell[1]

                val isSelfOrigin = (item.entity.container == WorkspaceItemEntity.CONTAINER_DESKTOP &&
                        item.entity.screenId == curScreenId &&
                        item.entity.cellX == targetCellX &&
                        item.entity.cellY == targetCellY)

                val targetChildView = cellLayout.getChildAtCell(targetCellX, targetCellY)
                val targetEntity = targetChildView?.tag as? WorkspaceItemEntity

                if (!isSelfOrigin && targetEntity != null && targetEntity.id != item.entity.id &&
                    item.entity.itemType == WorkspaceItemEntity.ITEM_TYPE_APP) {
                    // Dropped onto another workspace item (Apps only)
                    if (targetEntity.itemType == WorkspaceItemEntity.ITEM_TYPE_APP) {
                        // Create a new folder containing both apps (inferring category title when both match)
                        val inferredTitle = inferFolderTitle(targetEntity, item)
                        viewModel?.createFolder(
                            existingItem = targetEntity,
                            draggedItem = item.entity,
                            folderTitle = inferredTitle
                        )
                        val msg = if (inferredTitle == "Folder") "Folder created" else "\"$inferredTitle\" folder created"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    } else if (targetEntity.itemType == WorkspaceItemEntity.ITEM_TYPE_FOLDER) {
                        // Add dragged app to existing folder
                        viewModel?.addItemToFolder(
                            folderId = targetEntity.id,
                            item = item.entity
                        )
                        Toast.makeText(context, "Added to folder", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Target cell is empty or occupied by self or widget drop
                    val prefs = context.getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                    if (prefs.getBoolean("key_desktop_lock", false)) {
                        Toast.makeText(context, "Home screen layout is locked", Toast.LENGTH_SHORT).show()
                        cancelDrag()
                        return
                    }

                    if (item.entity.container > 0) {
                        // Dragged out of a folder onto workspace grid
                        val isAvailable = cellLayout.isCellEmpty(targetCellX, targetCellY, spanX, spanY)
                        if (isAvailable) {
                            viewModel?.moveItemOutOfFolder(
                                item = item.entity,
                                targetScreenId = curScreenId,
                                targetCellX = targetCellX,
                                targetCellY = targetCellY
                            )
                        } else {
                            val vacant = cellLayout.findVacantCell(spanX, spanY)
                            if (vacant != null) {
                                viewModel?.moveItemOutOfFolder(
                                    item = item.entity,
                                    targetScreenId = curScreenId,
                                    targetCellX = vacant[0],
                                    targetCellY = vacant[1]
                                )
                            } else {
                                Toast.makeText(context, "No space on page", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else if (item.entity.id == 0L) {
                        // App dragged from drawer onto workspace grid
                        val cellToUse = if (cellLayout.isCellEmpty(targetCellX, targetCellY, item.entity.spanX, item.entity.spanY)) {
                            intArrayOf(targetCellX, targetCellY)
                        } else {
                            cellLayout.findVacantCell(item.entity.spanX, item.entity.spanY)
                        }

                        if (cellToUse != null) {
                            viewModel?.addWorkspaceItem(
                                item.entity.copy(
                                    container = WorkspaceItemEntity.CONTAINER_DESKTOP,
                                    screenId = curScreenId,
                                    cellX = cellToUse[0],
                                    cellY = cellToUse[1],
                                    spanX = 1,
                                    spanY = 1
                                )
                            )
                            Toast.makeText(context, "Added ${item.appInfo.label} to workspace", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "No space on page", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Moving an existing workspace item (widget or app)
                        val isAvailable = if (sourceView != null && cellLayout == sourceView?.parent) {
                            cellLayout.isRegionAvailableForChild(targetCellX, targetCellY, item.entity.spanX, item.entity.spanY, sourceView!!)
                        } else {
                            cellLayout.isCellEmpty(targetCellX, targetCellY, item.entity.spanX, item.entity.spanY)
                        }

                        if (isAvailable) {
                            if (sourceView != null && cellLayout == sourceView?.parent) {
                                cellLayout.updateChildSpanAndPosition(
                                    sourceView!!, targetCellX, targetCellY, spanX, spanY
                                )
                            }
                            viewModel?.updateItemPosition(item.entity.id, curScreenId, targetCellX, targetCellY)
                        } else {
                            // Target is occupied: restore item to its original position
                            if (sourceView != null && sourceView?.parent is com.example.launcherai.ui.CellLayout) {
                                (sourceView?.parent as com.example.launcherai.ui.CellLayout).updateChildSpanAndPosition(
                                    sourceView!!, item.entity.cellX, item.entity.cellY, spanX, spanY
                                )
                            }
                            Toast.makeText(context, "Cannot place here: space occupied", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        cleanupDrag()
        onDragEnded?.invoke(item, hitTarget)
    }

    fun cancelDrag() {
        if (!isDragging) return
        stopEdgeScroll()
        lastDropTargetCellLayout?.clearDropTarget()
        lastDropTargetCellLayout = null
        workspace?.clearAllDropTargets()
        val item = draggedItem
        if (item != null && sourceView != null && sourceView?.parent is com.example.launcherai.ui.CellLayout) {
            (sourceView?.parent as com.example.launcherai.ui.CellLayout).updateChildSpanAndPosition(
                sourceView!!, item.entity.cellX, item.entity.cellY, item.entity.spanX, item.entity.spanY
            )
        }
        cleanupDrag()
    }

    private fun cleanupDrag() {
        lastDropTargetCellLayout?.clearDropTarget()
        lastDropTargetCellLayout = null
        workspace?.clearAllDropTargets()
        dragView?.let { dv ->
            dragLayer?.removeView(dv)
        }
        dragView = null
        sourceView?.alpha = 1.0f
        sourceView = null
        draggedItem = null
        pendingWidget = null
        dragRegistrationX = 0
        dragRegistrationY = 0

        dropTargetBar?.let { bar ->
            val density = context.resources.displayMetrics.density
            val barHeight = if (bar.height > 0) bar.height.toFloat() else 56f * density
            bar.animate()
                .alpha(0f)
                .translationY(-barHeight)
                .setDuration(175)
                .withEndAction {
                    bar.visibility = View.GONE
                    bar.alpha = 1f
                    bar.translationY = 0f
                }
                .start()
        }

        workspace?.animate()
            ?.scaleX(1.0f)
            ?.scaleY(1.0f)
            ?.setDuration(200)
            ?.start()

        resetTargetHighlights()
        lastHoverTarget = null
        isDragging = false
    }

    private fun evaluateDropTargetHit(x: Float, y: Float): DropTargetType? {
        return null
    }

    private fun checkDropTargetHover(x: Float, y: Float) {
        val hit = evaluateDropTargetHit(x, y)
        if (hit != lastHoverTarget) {
            if (hit != null) {
                val targetView = when (hit) {
                    DropTargetType.REMOVE -> targetRemove
                    DropTargetType.APP_INFO -> targetAppInfo
                    DropTargetType.UNINSTALL -> targetUninstall
                }
                targetView?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
            lastHoverTarget = hit
        }

        targetRemove?.scaleX = if (hit == DropTargetType.REMOVE) 1.15f else 1.0f
        targetRemove?.scaleY = if (hit == DropTargetType.REMOVE) 1.15f else 1.0f

        targetAppInfo?.scaleX = if (hit == DropTargetType.APP_INFO) 1.15f else 1.0f
        targetAppInfo?.scaleY = if (hit == DropTargetType.APP_INFO) 1.15f else 1.0f

        targetUninstall?.scaleX = if (hit == DropTargetType.UNINSTALL) 1.15f else 1.0f
        targetUninstall?.scaleY = if (hit == DropTargetType.UNINSTALL) 1.15f else 1.0f
    }

    private fun resetTargetHighlights() {
        targetRemove?.scaleX = 1.0f
        targetRemove?.scaleY = 1.0f
        targetAppInfo?.scaleX = 1.0f
        targetAppInfo?.scaleY = 1.0f
        targetUninstall?.scaleX = 1.0f
        targetUninstall?.scaleY = 1.0f
    }

    private fun checkEdgeScroll(x: Float) {
        val layer = dragLayer ?: return
        val edgeWidth = 40f * context.resources.displayMetrics.density

        if (x < edgeWidth) {
            triggerEdgeScroll(-1)
        } else if (x > layer.width - edgeWidth) {
            triggerEdgeScroll(1)
        } else {
            stopEdgeScroll()
        }
    }

    private fun triggerEdgeScroll(direction: Int) {
        if (isEdgeScrolling && edgeScrollDirection == direction) return
        stopEdgeScroll()
        isEdgeScrolling = true
        edgeScrollDirection = direction
        edgeScrollHandler.postDelayed(edgeScrollRunnable, 450)
    }

    private fun stopEdgeScroll() {
        edgeScrollHandler.removeCallbacks(edgeScrollRunnable)
        isEdgeScrolling = false
    }

    private fun getRelativeBounds(view: View?): Rect {
        if (view == null) return Rect()
        val loc = IntArray(2)
        view.getLocationOnScreen(loc)
        return Rect(loc[0], loc[1], loc[0] + view.width, loc[1] + view.height)
    }

    private fun inferFolderTitle(
        targetEntity: WorkspaceItemEntity,
        draggedItem: WorkspaceResolvedItem
    ): String {
        val installed = viewModel?.installedApps?.value.orEmpty()
        val firstApp = installed.find {
            it.packageName == targetEntity.packageName &&
                    (targetEntity.className.isNullOrEmpty() || it.activityName == targetEntity.className)
        } ?: targetEntity.packageName?.let { pkg ->
            AppInfo(
                label = targetEntity.title ?: pkg,
                packageName = pkg,
                activityName = targetEntity.className.orEmpty()
            )
        }
        val secondApp = installed.find {
            it.packageName == draggedItem.appInfo.packageName &&
                    (draggedItem.appInfo.activityName.isEmpty() || it.activityName == draggedItem.appInfo.activityName)
        } ?: draggedItem.appInfo
        return SmartCategoryHelper.inferFolderTitleForApps(firstApp, secondApp)
    }

    enum class DropTargetType {
        REMOVE, APP_INFO, UNINSTALL
    }

    companion object {
        fun calculateDropCell(
            localX: Float,
            localY: Float,
            spanX: Int,
            spanY: Int,
            cellW: Float,
            cellH: Float,
            pLeft: Float,
            pTop: Float,
            regX: Float,
            regY: Float,
            countX: Int,
            countY: Int,
            usableW: Float = cellW * countX,
            usableH: Float = cellH * countY
        ): IntArray {
            val visualLeft = localX - regX
            val visualTop = localY - regY

            val centerX = visualLeft + (spanX * cellW) / 2f
            val centerY = visualTop + (spanY * cellH) / 2f

            val safeUsableW = maxOf(1f, usableW)
            val safeUsableH = maxOf(1f, usableH)

            val relX = (centerX - pLeft).coerceIn(0f, safeUsableW)
            val relY = (centerY - pTop).coerceIn(0f, safeUsableH)

            val rawCellX = ((relX / safeUsableW) * countX).toInt()
            val rawCellY = ((relY / safeUsableH) * countY).toInt()

            val maxCellX = maxOf(0, countX - spanX)
            val maxCellY = maxOf(0, countY - spanY)

            val cellX = rawCellX.coerceIn(0, maxCellX)
            val cellY = rawCellY.coerceIn(0, maxCellY)
            return intArrayOf(cellX, cellY)
        }
    }
}