package com.example.launcherai.ai.tools.impl

import android.content.Context
import com.example.launcherai.AppManager
import com.example.launcherai.ai.tools.LauncherTool
import com.example.launcherai.ai.tools.ToolResult
import com.example.launcherai.data.repository.WorkspaceRepository
import java.util.Locale

class SearchDevicePackagesTool : LauncherTool {
    override val name: String = "search_device_packages"
    override val description: String = "Searches installed applications by query name"
    override val requiresConfirmation: Boolean = false

    override suspend fun execute(
        params: Map<String, Any?>,
        context: Context,
        repository: WorkspaceRepository
    ): ToolResult {
        val query = params["query"]?.toString()?.lowercase(Locale.ROOT) ?: ""
        val apps = AppManager.getInstalledApps(context)
        val filtered = if (query.isBlank()) {
            apps
        } else {
            apps.filter { it.label.lowercase(Locale.ROOT).contains(query) || it.packageName.lowercase(Locale.ROOT).contains(query) }
        }

        val summary = filtered.joinToString("\n") { "• ${it.label} (${it.packageName})" }
        return ToolResult(
            success = true,
            output = if (summary.isNotBlank()) "Found ${filtered.size} matching apps:\n$summary" else "No matching applications found for '$query'"
        )
    }
}
