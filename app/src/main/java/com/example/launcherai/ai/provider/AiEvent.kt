package com.example.launcherai.ai.provider

sealed class AiEvent {
    data class Token(val text: String) : AiEvent()
    data class Reasoning(val reasoningText: String) : AiEvent()
    data class ToolCall(val name: String, val params: Map<String, Any?>) : AiEvent()
    data class Error(val errorMessage: String) : AiEvent()
    object Complete : AiEvent()
}
