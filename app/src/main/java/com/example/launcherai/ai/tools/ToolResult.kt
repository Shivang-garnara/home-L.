package com.example.launcherai.ai.tools

data class ToolResult(
    val success: Boolean,
    val output: String,
    val error: String? = null
)
