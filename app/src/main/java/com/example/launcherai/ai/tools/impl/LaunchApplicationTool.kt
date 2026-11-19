package com.example.launcherai.ai.tools.impl

import android.content.Context
import com.example.launcherai.AppManager
import com.example.launcherai.ai.tools.LauncherTool
import com.example.launcherai.ai.tools.ToolResult
import com.example.launcherai.data.repository.WorkspaceRepository

class LaunchApplicationTool : LauncherTool {
    override val name: String = "launch_application"
    override val description: String = "Launches a validated installed application"
    override val requiresConfirmation: Boolean = false

    override suspend fun execute(
        params: Map<String, Any?>,
        context: Context,
        repository: WorkspaceRepository
    ): ToolResult {
        val packageName = params["packageName"]?.toString() ?: return ToolResult(false, "", "Missing required 'packageName' parameter")
        val apps = AppManager.getInstalledApps(context)
        val targetApp = apps.find { it.packageName == packageName }

        return if (targetApp != null) {
            AppManager.launchApp(context, targetApp)
            ToolResult(true, "Launched ${targetApp.label} (${targetApp.packageName})")
        } else {
            ToolResult(false, "", "Application '$packageName' is not installed on this device")
        }
    }
}
