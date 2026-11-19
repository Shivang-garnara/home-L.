package com.example.launcherai.dragndrop

import android.content.Context
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import com.example.launcherai.AppManager
import com.example.launcherai.R
import com.example.launcherai.data.db.WorkspaceItemEntity
import com.example.launcherai.ui.DragLayer
import com.example.launcherai.ui.Workspace
import com.example.launcherai.ui.WorkspaceResolvedItem
import com.example.launcherai.ui.WorkspaceViewModel

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

    private var sourceView: View? = null
    private var dragView: DragView? = null

    private var targetRemove: View? = null
    private var targetAppInfo: View? = null
    private var targetUninstall: View? = null

    private val edgeScrollHandler = Handler(Looper.getMainLooper())
    private var isEdgeScrolling = false
    private var edgeScrollDirection = 0

    private val edgeScrollRunnable = Runnable {
        workspace?.let { ws ->
            val curPage = ws.getCurrentPage()
            val totalPages = ws.getPageCount()
            val newPage = (curPage + edgeScrollDirection).coerceIn(0, totalPages - 1)
            if (newPage != curPage) {
                ws.setCurrentPage(newPage, smoothScroll = true)
            }
        }
        isEdgeScrolling = false
    }

    fun initDropTargetBar(bar: View) {
        this.dropTargetBar = bar
        this.targetRemove = bar.findViewById(R.id.targetRemove)
        this.targetAppInfo = bar.findViewById(R.id.targetAppInfo)
        this.targetUninstall = bar.findViewById(R.id.targetUninstall)
    }

    fun startDrag(v: View, item: WorkspaceResolvedItem, touchX: Float, touchY: Float) {
        if (isDragging) return
        isDragging = true
        draggedItem = item
        sourceView = v

        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        v.alpha = 0.2f

        val bitmap = DragView.createBitmapFromView(v)
        val regX = v.width / 2
        val regY = v.height / 2

        val dv = DragView(context, bitmap, regX, regY)
        dragView = dv

        val lp = FrameLayout.LayoutParams(bitmap.width, bitmap.height)
        dragLayer?.addView(dv, lp)
        dv.move(touchX, touchY)

        dropTargetBar?.visibility = View.VISIBLE
    }

    fun onDragMove(touchX: Float, touchY: Float) {
        if (!isDragging) return
        dragView?.move(touchX, touchY)

        checkDropTargetHover(touchX, touchY)
        checkEdgeScroll(touchX)
    }

    fun endDrag(touchX: Float, touchY: Float) {
        if (!isDragging) return
        stopEdgeScroll()

        val item = draggedItem
        val ws = workspace

        val hitTarget = evaluateDropTargetHit(touchX, touchY)
        val dock = dockLayout as? com.example.launcherai.ui.DockLayout
        val isDropOverDock = if (dock != null && dock.visibility == View.VISIBLE) {
            val loc = IntArray(2)
            dock.getLocationOnScreen(loc)
            val dockRect = Rect(loc[0], loc[1], loc[0] + dock.width, loc[1] + dock.height)
            dockRect.contains(touchX.toInt(), touchY.toInt())
        } else false

        if (hitTarget != null && item != null) {
            when (hitTarget) {
                DropTargetType.REMOVE -> {
                    if (item.entity.itemType == WorkspaceItemEntity.ITEM_TYPE_WIDGET && item.entity.appWidgetId != -1) {
                        appWidgetHost?.deleteAppWidgetId(item.entity.appWidgetId)
                    }
                    viewModel?.deleteWorkspaceItem(item.entity.id)
                    Toast.makeText(context, "Removed ${item.appInfo.label} from launcher", Toast.LENGTH_SHORT).show()
                }
                DropTargetType.APP_INFO -> {
                    AppManager.openAppDetails(context, item.appInfo.packageName)
                }
                DropTargetType.UNINSTALL -> {
                    AppManager.uninstallApp(context, item.appInfo.packageName)
                }
            }
        } else if (isDropOverDock && dock != null && item != null) {
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
                        viewModel?.createFolder(
                            existingItem = targetEntity,
                            draggedItem = item.entity,
                            folderTitle = "Folder"
                        )
                        Toast.makeText(context, "Folder created in dock", Toast.LENGTH_SHORT).show()
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
        } else if (item != null && ws != null) {
            val curPage = ws.getCurrentPage()
            val cellLayout = ws.getActiveCellLayout()
            if (cellLayout != null) {
                val loc = IntArray(2)
                cellLayout.getLocationOnScreen(loc)
                val localX = touchX - loc[0]
                val localY = touchY - loc[1]

                val targetCell = cellLayout.pointToCellExact(localX, localY)
                val targetCellX = targetCell[0]
                val targetCellY = targetCell[1]

                val targetChildView = cellLayout.getChildAtCell(targetCellX, targetCellY)
                val targetEntity = targetChildView?.tag as? WorkspaceItemEntity

                if (targetEntity != null && targetEntity.id != item.entity.id) {
                    // Dropped onto another workspace item
                    if (targetEntity.itemType == WorkspaceItemEntity.ITEM_TYPE_APP) {
                        // Create a new folder containing both apps
                        viewModel?.createFolder(
                            existingItem = targetEntity,
                            draggedItem = item.entity,
                            folderTitle = "Folder"
                        )
                        Toast.makeText(context, "Folder created", Toast.LENGTH_SHORT).show()
                    } else if (targetEntity.itemType == WorkspaceItemEntity.ITEM_TYPE_FOLDER) {
                        // Add dragged app to existing folder
                        viewModel?.addItemToFolder(
                            folderId = targetEntity.id,
                            item = item.entity
                        )
                        Toast.makeText(context, "Added to folder", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Target cell is empty or occupied by self
                    if (item.entity.container > 0) {
                        // Dragged out of a folder onto workspace grid
                        viewModel?.moveItemOutOfFolder(
                            item = item.entity,
                            targetScreenId = curPage,
                            targetCellX = targetCellX,
                            targetCellY = targetCellY
                        )
                    } else {
                        val isOccupiedBySelf = item.entity.id != 0L &&
                                item.entity.container == WorkspaceItemEntity.CONTAINER_DESKTOP &&
                                item.entity.screenId == curPage &&
                                item.entity.cellX == targetCellX &&
                                item.entity.cellY == targetCellY

                        if (isOccupiedBySelf) {
                            // Keep position
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
                                        screenId = curPage,
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
                        } else if (cellLayout.isCellEmpty(targetCellX, targetCellY, item.entity.spanX, item.entity.spanY)) {
                            viewModel?.updateItemPosition(item.entity.id, curPage, targetCellX, targetCellY)
                        } else {
                            val vacant = cellLayout.findVacantCell(item.entity.spanX, item.entity.spanY)
                            if (vacant != null) {
                                viewModel?.updateItemPosition(item.entity.id, curPage, vacant[0], vacant[1])
                            } else {
                                Toast.makeText(context, "No space on page", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }

        cleanupDrag()
    }

    fun cancelDrag() {
        if (!isDragging) return
        stopEdgeScroll()
        cleanupDrag()
    }

    private fun cleanupDrag() {
        dragView?.let { dv ->
            dragLayer?.removeView(dv)
        }
        dragView = null
        sourceView?.alpha = 1.0f
        sourceView = null
        draggedItem = null

        dropTargetBar?.visibility = View.GONE
        resetTargetHighlights()
        isDragging = false
    }

    private fun evaluateDropTargetHit(x: Float, y: Float): DropTargetType? {
        val bar = dropTargetBar ?: return null
        if (bar.visibility != View.VISIBLE) return null

        val removeRect = getRelativeBounds(targetRemove)
        if (removeRect.contains(x.toInt(), y.toInt())) return DropTargetType.REMOVE

        val infoRect = getRelativeBounds(targetAppInfo)
        if (infoRect.contains(x.toInt(), y.toInt())) return DropTargetType.APP_INFO

        val uninstallRect = getRelativeBounds(targetUninstall)
        if (uninstallRect.contains(x.toInt(), y.toInt())) return DropTargetType.UNINSTALL

        return null
    }

    private fun checkDropTargetHover(x: Float, y: Float) {
        val hit = evaluateDropTargetHit(x, y)
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

    enum class DropTargetType {
        REMOVE, APP_INFO, UNINSTALL
    }
}
