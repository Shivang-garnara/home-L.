package com.example.launcherai.ai.tools.impl

import android.content.Context
import com.example.launcherai.ai.tools.LauncherTool
import com.example.launcherai.ai.tools.ToolResult
import com.example.launcherai.data.repository.GridChangeResult
import com.example.launcherai.data.repository.WorkspaceRepository

class ModifyWorkspaceGridTool : LauncherTool {
    override val name: String = "modify_workspace_grid"
    override val description: String = "Changes workspace grid columns and rows"
    override val requiresConfirmation: Boolean = true

    override suspend fun execute(
        params: Map<String, Any?>,
        context: Context,
        repository: WorkspaceRepository
    ): ToolResult {
        val cols = (params["cols"] as? Number)?.toInt() ?: (params["cols"]?.toString()?.toIntOrNull()) ?: 4
        val rows = (params["rows"] as? Number)?.toInt() ?: (params["rows"]?.toString()?.toIntOrNull()) ?: 5

        val validCols = cols.coerceIn(3, 7)
        val validRows = rows.coerceIn(3, 8)

        val result = repository.setGridConfig(validCols, validRows)
        return when (result) {
            is GridChangeResult.Success -> {
                ToolResult(
                    success = true,
                    output = "Workspace grid updated to $validCols columns x $validRows rows"
                )
            }
            is GridChangeResult.Failure -> {
                ToolResult(
                    success = false,
                    output = "Failed to update workspace grid: ${result.reason}"
                )
            }
        }
    }
}
