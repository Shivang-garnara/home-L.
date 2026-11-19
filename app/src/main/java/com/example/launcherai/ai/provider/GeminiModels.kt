package com.example.launcherai.ai.provider

/**
 * Centralized Google Gemini Model catalog based on official Google Developer documentation
 * (https://ai.google.dev/gemini-api/docs/models & https://ai.google.dev/gemini-api/docs/deprecations).
 *
 * Obsolete or retired model IDs are strictly excluded to prevent 404 / unsupported model errors.
 */
object GeminiModels {

    // --- Stable Production Models (Supporting Text Streaming & Function Calling) ---
    const val GEMINI_2_5_FLASH = "gemini-2.5-flash"
    const val GEMINI_2_5_PRO = "gemini-2.5-pro"
    const val GEMINI_2_0_FLASH = "gemini-2.0-flash"
    const val GEMINI_1_5_FLASH = "gemini-1.5-flash"
    const val GEMINI_1_5_PRO = "gemini-1.5-pro"
    const val GEMINI_1_5_FLASH_8B = "gemini-1.5-flash-8b"

    // --- Preview Models ---
    const val GEMINI_2_0_FLASH_LITE_PREVIEW = "gemini-2.0-flash-lite-preview-02-05"

    val STABLE_MODELS = arrayOf(
        GEMINI_2_5_FLASH,
        GEMINI_2_5_PRO,
        GEMINI_2_0_FLASH,
        GEMINI_1_5_FLASH,
        GEMINI_1_5_PRO,
        GEMINI_1_5_FLASH_8B
    )

    val PREVIEW_MODELS = arrayOf(
        GEMINI_2_0_FLASH_LITE_PREVIEW
    )

    val ALL_SUPPORTED_MODELS = STABLE_MODELS + PREVIEW_MODELS

    fun isValidModel(model: String): Boolean {
        val cleanModel = sanitizeModelId(model)
        return ALL_SUPPORTED_MODELS.contains(cleanModel)
    }

    fun isPreviewModel(model: String): Boolean {
        val cleanModel = sanitizeModelId(model)
        return PREVIEW_MODELS.contains(cleanModel)
    }

    fun sanitizeModelId(modelId: String): String {
        return modelId.removePrefix("models/").trim()
    }

    fun getDefaultModel(): String = GEMINI_2_5_FLASH
}
