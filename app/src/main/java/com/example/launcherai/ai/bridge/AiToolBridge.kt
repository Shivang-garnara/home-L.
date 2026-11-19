package com.example.launcherai.ai.bridge

import android.content.Context
import com.example.launcherai.ai.tools.LauncherTool
import com.example.launcherai.ai.tools.ToolResult
import com.example.launcherai.ai.tools.impl.CreateWorkspaceFolderTool
import com.example.launcherai.ai.tools.impl.LaunchApplicationTool
import com.example.launcherai.ai.tools.impl.ModifyWorkspaceGridTool
import com.example.launcherai.ai.tools.impl.SearchDevicePackagesTool
import com.example.launcherai.ai.tools.impl.ToggleHiddenPackageTool
import com.example.launcherai.data.repository.WorkspaceRepository

/**
 * Safe native tool bridge registering only confirmed launcher tools.
 * Prevents arbitrary reflection, class loading, or untrusted execution.
 */
object AiToolBridge {

    private val toolsRegistry = mutableMapOf<String, LauncherTool>()

    init {
        registerTool(SearchDevicePackagesTool())
        registerTool(LaunchApplicationTool())
        registerTool(ModifyWorkspaceGridTool())
        registerTool(ToggleHiddenPackageTool())
        registerTool(CreateWorkspaceFolderTool())
    }

    fun registerTool(tool: LauncherTool) {
        toolsRegistry[tool.name] = tool
    }

    fun getTool(name: String): LauncherTool? {
        return toolsRegistry[name]
    }

    fun getAllTools(): List<LauncherTool> {
        return toolsRegistry.values.toList()
    }

    suspend fun executeTool(
        name: String,
        params: Map<String, Any?>,
        context: Context,
        repository: WorkspaceRepository
    ): ToolResult {
        val tool = getTool(name)
            ?: return ToolResult(false, "", "Tool '$name' is not registered in AiToolBridge")

        return try {
            tool.execute(params, context, repository)
        } catch (e: Exception) {
            ToolResult(false, "", "Execution error in tool '$name': ${e.message}")
        }
    }
}
