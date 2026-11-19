package com.example.launcherai.ai.confirmation

import com.example.launcherai.ai.bridge.AiToolBridge

data class PendingConfirmation(
    val id: String = java.util.UUID.randomUUID().toString(),
    val toolName: String,
    val params: Map<String, Any?>,
    val promptMessage: String
)

/**
 * Human-in-the-Loop (HITL) confirmation manager.
 * Enforces explicit user confirmation before executing actions that alter launcher state.
 */
object HitlConfirmationManager {

    fun requiresConfirmation(toolName: String): Boolean {
        val tool = AiToolBridge.getTool(toolName)
        return tool?.requiresConfirmation ?: false
    }

    fun buildConfirmationPrompt(toolName: String, params: Map<String, Any?>): String {
        return when (toolName) {
            "modify_workspace_grid" -> {
                val cols = params["cols"] ?: 4
                val rows = params["rows"] ?: 5
                "Modify workspace grid layout to $cols columns x $rows rows?"
            }
            "toggle_hidden_package" -> {
                val pkg = params["packageName"] ?: ""
                "Hide/unhide application '$pkg' from App Drawer?"
            }
            "create_workspace_folder" -> {
                val title = params["title"] ?: "Folder"
                "Create workspace folder '$title'?"
            }
            else -> "Execute launcher action '$toolName'?"
        }
    }
}
