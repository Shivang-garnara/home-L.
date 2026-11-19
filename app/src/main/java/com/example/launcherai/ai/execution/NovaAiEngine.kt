package com.example.launcherai.ai.execution

import android.content.Context
import com.example.launcherai.ai.bridge.AiToolBridge
import com.example.launcherai.ai.confirmation.HitlConfirmationManager
import com.example.launcherai.ai.provider.AiEvent
import com.example.launcherai.ai.provider.AiProvider
import com.example.launcherai.ai.provider.LocalAgentProvider
import com.example.launcherai.ai.repository.AiRepository
import com.example.launcherai.data.db.ChatMessageEntity
import com.example.launcherai.data.repository.WorkspaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object NovaAiEngine {

    var activeProvider: AiProvider = LocalAgentProvider()

    fun submitPrompt(
        prompt: String,
        context: Context,
        repository: WorkspaceRepository,
        aiRepository: AiRepository
    ): Flow<ChatMessageEntity> = flow {
        val userMsg = ChatMessageEntity(
            text = prompt,
            isUser = true
        )
        aiRepository.saveMessage(userMsg)
        emit(userMsg)

        var aiText = ""
        var aiReasoning: String? = null
        var toolName: String? = null
        var toolResultStr: String? = null
        var requiresConfirm = false

        val msgId = java.util.UUID.randomUUID().toString()

        activeProvider.streamPrompt(prompt).collect { event ->
            when (event) {
                is AiEvent.Token -> {
                    aiText += event.text
                }
                is AiEvent.Reasoning -> {
                    aiReasoning = event.reasoningText
                }
                is AiEvent.ToolCall -> {
                    toolName = event.name
                    if (HitlConfirmationManager.requiresConfirmation(event.name)) {
                        requiresConfirm = true
                        toolResultStr = HitlConfirmationManager.buildConfirmationPrompt(event.name, event.params)
                    } else {
                        val result = AiToolBridge.executeTool(event.name, event.params, context, repository)
                        toolResultStr = if (result.success) result.output else "Tool Error: ${result.error}"
                    }
                }
                is AiEvent.Error -> {
                    aiText = event.errorMessage
                }
                is AiEvent.Complete -> {}
            }

            val currentAiMsg = ChatMessageEntity(
                id = msgId,
                text = aiText,
                isUser = false,
                reasoning = aiReasoning,
                toolCallName = toolName,
                toolCallResult = toolResultStr,
                requiresConfirmation = requiresConfirm,
                isConfirmed = false
            )

            aiRepository.saveMessage(currentAiMsg)
            emit(currentAiMsg)
        }
    }

    suspend fun confirmAndExecuteTool(
        message: ChatMessageEntity,
        params: Map<String, Any?>,
        context: Context,
        repository: WorkspaceRepository,
        aiRepository: AiRepository
    ): ChatMessageEntity {
        val toolName = message.toolCallName ?: ""
        val result = AiToolBridge.executeTool(toolName, params, context, repository)

        val updatedMsg = message.copy(
            isConfirmed = true,
            requiresConfirmation = false,
            toolCallResult = if (result.success) "✓ Confirmed & Executed: ${result.output}" else "Tool Execution Failed: ${result.error}"
        )

        aiRepository.saveMessage(updatedMsg)
        return updatedMsg
    }
}
