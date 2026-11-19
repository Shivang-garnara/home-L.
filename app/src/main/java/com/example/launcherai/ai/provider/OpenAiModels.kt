package com.example.launcherai.ai.provider

/**
 * Centralized OpenAI Model catalog verified against official OpenAI API documentation
 * (https://developers.openai.com/api/docs/models).
 */
object OpenAiModels {

    // --- Stable Production Models ---
    const val GPT_4O = "gpt-4o"
    const val GPT_4O_MINI = "gpt-4o-mini"
    const val O1 = "o1"
    const val O3_MINI = "o3-mini"

    // --- Preview Models ---
    const val GPT_4_5_PREVIEW = "gpt-4.5-preview"

    val STABLE_MODELS = arrayOf(
        GPT_4O,
        GPT_4O_MINI,
        O1,
        O3_MINI
    )

    val PREVIEW_MODELS = arrayOf(
        GPT_4_5_PREVIEW
    )

    val ALL_SUPPORTED_MODELS = STABLE_MODELS + PREVIEW_MODELS

    fun isValidModel(model: String): Boolean {
        val cleanModel = model.lowercase().trim()
        return ALL_SUPPORTED_MODELS.contains(cleanModel)
    }

    fun isPreviewModel(model: String): Boolean {
        val cleanModel = model.lowercase().trim()
        return PREVIEW_MODELS.contains(cleanModel)
    }

    fun getDefaultModel(): String = GPT_4O
}
