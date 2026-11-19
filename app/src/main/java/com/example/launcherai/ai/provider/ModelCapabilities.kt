package com.example.launcherai.ai.provider

/**
 * Model capabilities metadata storing streaming, function calling, multimodal,
 * and reasoning support flags for each AI model.
 */
data class ModelCapabilities(
    val modelId: String,
    val providerId: String,
    val supportsStreaming: Boolean = true,
    val supportsFunctionCalling: Boolean = true,
    val supportsMultimodal: Boolean = false,
    val supportsReasoning: Boolean = false
) {
    companion object {
        fun getCapabilities(providerId: String, modelId: String): ModelCapabilities {
            return when (providerId) {
                "google_gemini" -> ModelCapabilities(
                    modelId = modelId,
                    providerId = providerId,
                    supportsStreaming = true,
                    supportsFunctionCalling = true,
                    supportsMultimodal = true,
                    supportsReasoning = modelId.contains("pro")
                )
                "openai_gpt" -> ModelCapabilities(
                    modelId = modelId,
                    providerId = providerId,
                    supportsStreaming = true,
                    supportsFunctionCalling = !modelId.contains("o1"),
                    supportsMultimodal = modelId.contains("4o"),
                    supportsReasoning = modelId.startsWith("o")
                )
                "anthropic_claude" -> ModelCapabilities(
                    modelId = modelId,
                    providerId = providerId,
                    supportsStreaming = true,
                    supportsFunctionCalling = true,
                    supportsMultimodal = true,
                    supportsReasoning = modelId.contains("3-7") || modelId.contains("opus")
                )
                else -> ModelCapabilities(
                    modelId = modelId,
                    providerId = providerId,
                    supportsStreaming = true,
                    supportsFunctionCalling = true,
                    supportsMultimodal = false,
                    supportsReasoning = false
                )
            }
        }
    }
}
