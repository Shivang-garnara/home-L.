package com.example.launcherai.ai.tools

import android.content.Context
import com.example.launcherai.data.repository.WorkspaceRepository

/**
 * Common interface for all confirmed native launcher tools.
 * Each tool specifies input validation, execution, and confirmation requirements.
 */
interface LauncherTool {
    val name: String
    val description: String
    val requiresConfirmation: Boolean

    suspend fun execute(
        params: Map<String, Any?>,
        context: Context,
        repository: WorkspaceRepository
    ): ToolResult
}
