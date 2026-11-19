package com.example.launcherai.ai.provider

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Locale

class LocalAgentProvider : AiProvider {
    override val providerName: String = "Local On-Device Launcher Agent"
    override val isConfigured: Boolean = true

    override fun streamPrompt(prompt: String): Flow<AiEvent> = flow {
        emit(AiEvent.Reasoning("Analyzing local device context and command intent..."))

        val lower = prompt.lowercase(Locale.ROOT)
        when {
            lower.contains("search") || lower.contains("find app") -> {
                val query = lower.replace("search", "").replace("find app", "").trim()
                emit(AiEvent.ToolCall("search_device_packages", mapOf("query" to query)))
                emit(AiEvent.Token("I searched your installed applications for '$query'."))
            }
            lower.contains("launch") || lower.contains("open app") -> {
                val pkg = extractPackageOrName(lower)
                emit(AiEvent.ToolCall("launch_application", mapOf("packageName" to pkg)))
                emit(AiEvent.Token("Attempting to launch application '$pkg'."))
            }
            lower.contains("grid") || lower.contains("columns") || lower.contains("rows") -> {
                emit(AiEvent.ToolCall("modify_workspace_grid", mapOf("cols" to 4, "rows" to 5)))
                emit(AiEvent.Token("I can adjust your home screen grid layout."))
            }
            lower.contains("hide") -> {
                val pkg = extractPackageOrName(lower)
                emit(AiEvent.ToolCall("toggle_hidden_package", mapOf("packageName" to pkg)))
                emit(AiEvent.Token("I can hide or unhide application '$pkg'."))
            }
            lower.contains("folder") -> {
                emit(AiEvent.ToolCall("create_workspace_folder", mapOf("title" to "AI Folder", "packages" to listOf("com.example.app1", "com.example.app2"))))
                emit(AiEvent.Token("I can organize your apps into a home screen folder."))
            }
            else -> {
                emit(AiEvent.Token("I am your Nova AI Assistant. I can help you search installed apps, organize folders, adjust grid layout, or launch applications."))
            }
        }
        emit(AiEvent.Complete)
    }

    private fun extractPackageOrName(prompt: String): String {
        return prompt.split(" ").lastOrNull()?.trim() ?: ""
    }
}
