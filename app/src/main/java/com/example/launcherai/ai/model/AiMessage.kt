package com.example.launcherai.ai.model

import java.util.UUID

data class AiMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: AiRole,
    val text: String,
    val timestamp: String = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
    val reasoning: String? = null,
    val toolCallName: String? = null,
    val toolCallParams: Map<String, Any?>? = null,
    val toolCallResult: String? = null,
    val requiresConfirmation: Boolean = false,
    val isConfirmed: Boolean = false
)
