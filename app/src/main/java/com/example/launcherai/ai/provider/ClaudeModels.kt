package com.example.launcherai.ai.provider

/**
 * Centralized Anthropic Claude Model catalog verified against official Anthropic API documentation
 * (https://platform.claude.com/docs/en/about-claude/models).
 */
object ClaudeModels {

    // --- Stable Production Models (Aliases) ---
    const val CLAUDE_3_7_SONNET_LATEST = "claude-3-7-sonnet-latest"
    const val CLAUDE_3_5_SONNET_LATEST = "claude-3-5-sonnet-latest"
    const val CLAUDE_3_5_HAIKU_LATEST = "claude-3-5-haiku-latest"
    const val CLAUDE_3_OPUS_LATEST = "claude-3-opus-latest"

    // --- Dated Snapshot Models ---
    const val CLAUDE_3_7_SONNET_20250219 = "claude-3-7-sonnet-20250219"
    const val CLAUDE_3_5_SONNET_20241022 = "claude-3-5-sonnet-20241022"
    const val CLAUDE_3_5_HAIKU_20241022 = "claude-3-5-haiku-20241022"
    const val CLAUDE_3_OPUS_20240229 = "claude-3-opus-20240229"

    val STABLE_MODELS = arrayOf(
        CLAUDE_3_7_SONNET_LATEST,
        CLAUDE_3_5_SONNET_LATEST,
        CLAUDE_3_5_HAIKU_LATEST,
        CLAUDE_3_OPUS_LATEST
    )

    val SNAPSHOT_MODELS = arrayOf(
        CLAUDE_3_7_SONNET_20250219,
        CLAUDE_3_5_SONNET_20241022,
        CLAUDE_3_5_HAIKU_20241022,
        CLAUDE_3_OPUS_20240229
    )

    val ALL_SUPPORTED_MODELS = STABLE_MODELS + SNAPSHOT_MODELS

    fun isValidModel(model: String): Boolean {
        val cleanModel = model.lowercase().trim()
        return ALL_SUPPORTED_MODELS.contains(cleanModel)
    }

    fun isPreviewModel(model: String): Boolean {
        return model.lowercase().contains("preview")
    }

    fun getDefaultModel(): String = CLAUDE_3_7_SONNET_LATEST
}
