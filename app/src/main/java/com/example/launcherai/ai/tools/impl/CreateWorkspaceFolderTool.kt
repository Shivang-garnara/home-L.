package com.example.launcherai.ai.tools.impl

import android.content.Context
import com.example.launcherai.ai.tools.LauncherTool
import com.example.launcherai.ai.tools.ToolResult
import com.example.launcherai.data.db.WorkspaceItemEntity
import com.example.launcherai.data.repository.WorkspaceRepository
import com.example.launcherai.util.AppManager

class CreateWorkspaceFolderTool : LauncherTool {
    override val name: String = "create_workspace_folder"
    override val description: String = "Creates a workspace folder with selected installed applications"
    override val requiresConfirmation: Boolean = true

    override suspend fun execute(
        params: Map<String, Any?>,
        context: Context,
        repository: WorkspaceRepository
    ): ToolResult {
        val folderTitle = params["title"]?.toString() ?: "AI Folder"
        val packageListRaw = params["packages"] as? List<*> ?: emptyList<Any>()
        val packageNames = packageListRaw.mapNotNull { it?.toString() }

        if (packageNames.size < 2) {
            return ToolResult(false, "", "Folder creation requires at least 2 valid package names")
        }

        val installedApps = AppManager.getInstalledApps(context)
        val validApps = packageNames.mapNotNull { pkg -> installedApps.find { it.packageName == pkg } }

        if (validApps.size < 2) {
            return ToolResult(false, "", "Fewer than 2 specified packages are installed on this device")
        }

        val appA = validApps[0]
        val appB = validApps[1]

        val itemA = WorkspaceItemEntity(
            itemType = WorkspaceItemEntity.ITEM_TYPE_APP,
            packageName = appA.packageName,
            className = appA.activityName,
            container = WorkspaceItemEntity.CONTAINER_DESKTOP,
            screenId = 0,
            cellX = 0,
            cellY = 0,
            title = appA.label
        )

        val itemB = WorkspaceItemEntity(
            itemType = WorkspaceItemEntity.ITEM_TYPE_APP,
            packageName = appB.packageName,
            className = appB.activityName,
            container = WorkspaceItemEntity.CONTAINER_DESKTOP,
            screenId = 0,
            cellX = 1,
            cellY = 0,
            title = appB.label
        )

        val folderId = repository.createFolder(itemA, itemB, folderTitle)

        // Add remaining apps to folder
        for (i in 2 until validApps.size) {
            val extraApp = validApps[i]
            val extraItem = WorkspaceItemEntity(
                itemType = WorkspaceItemEntity.ITEM_TYPE_APP,
                packageName = extraApp.packageName,
                className = extraApp.activityName,
                container = WorkspaceItemEntity.CONTAINER_DESKTOP,
                title = extraApp.label
            )
            repository.addItemToFolder(folderId, extraItem)
        }

        return ToolResult(
            success = true,
            output = "Created folder '$folderTitle' containing ${validApps.size} apps (${validApps.joinToString { it.label }})"
        )
    }
}
