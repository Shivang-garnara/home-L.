package com.example.launcherai.ai.tools.impl

import android.content.Context
import com.example.launcherai.ai.tools.LauncherTool
import com.example.launcherai.ai.tools.ToolResult
import com.example.launcherai.data.repository.WorkspaceRepository
import com.example.launcherai.util.AppManager

class ToggleHiddenPackageTool : LauncherTool {
    override val name: String = "toggle_hidden_package"
    override val description: String = "Hides or unhides an application from the app drawer"
    override val requiresConfirmation: Boolean = true

    override suspend fun execute(
        params: Map<String, Any?>,
        context: Context,
        repository: WorkspaceRepository
    ): ToolResult {
        val packageName = params["packageName"]?.toString()
            ?: return ToolResult(false, "", "Missing required 'packageName' parameter")

        val apps = AppManager.getInstalledApps(context)
        val targetApp = apps.find { it.packageName == packageName }
            ?: return ToolResult(false, "", "Package '$packageName' not installed on device")

        val prefs = context.getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        val currentSet = prefs.getStringSet("key_hidden_packages", emptySet())?.toMutableSet() ?: mutableSetOf()

        val isHidden = if (currentSet.contains(packageName)) {
            currentSet.remove(packageName)
            false
        } else {
            currentSet.add(packageName)
            true
        }

        prefs.edit().putStringSet("key_hidden_packages", currentSet).apply()

        val status = if (isHidden) "hidden from" else "restored to"
        return ToolResult(true, "Application ${targetApp.label} ($packageName) $status App Drawer")
    }
}
