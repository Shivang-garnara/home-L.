package com.example.launcherai.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: String = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
    val reasoning: String? = null,
    val toolCallName: String? = null,
    val toolCallResult: String? = null,
    val requiresConfirmation: Boolean = false,
    val isConfirmed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
