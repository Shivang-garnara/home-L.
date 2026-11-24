package com.example.launcherai.ai.model

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: String = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
    val reasoning: String? = null,
    val toolCallName: String? = null,
    val toolCallResult: String? = null,
    val requiresConfirmation: Boolean = false,
    val isConfirmed: Boolean = false,
    val toolCallParams: Map<String, Any?>? = null
)
