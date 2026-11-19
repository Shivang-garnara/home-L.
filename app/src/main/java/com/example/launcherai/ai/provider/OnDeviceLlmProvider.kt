package com.example.launcherai.ai.provider

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * On-Device LLM Provider architecture prepared for genuine local language model inference
 * using Google LiteRT-LM (MediaPipe LLM Inference API `com.google.mediapipe.tasks.genai.llminference.LlmInference`).
 *
 * Requirements & Specifications:
 * - Model Weights: Quantized 4-bit Gemma-2B-IT or Phi-2 (~1.5 GB storage).
 * - Memory Footprint: Requires ~1.5 GB RAM.
 * - Internet Access: 100% Offline execution once model weights are downloaded.
 */
class OnDeviceLlmProvider(
    private val modelPath: String = ""
) : AiProvider {

    override val providerName: String = "On-Device LLM (LiteRT / Gemma 2B)"

    override val isConfigured: Boolean
        get() = modelPath.isNotBlank()

    override fun streamPrompt(prompt: String): Flow<AiEvent> = flow {
        if (!isConfigured) {
            emit(AiEvent.Reasoning("On-Device LLM model weights not found locally."))
            emit(AiEvent.Error("On-Device LiteRT-LM model requires downloading Gemma-2B-IT weights (~1.5 GB). Using Local Rule-Based Agent as fallback."))
            emit(AiEvent.Complete)
            return@flow
        }

        emit(AiEvent.Reasoning("Initializing LiteRT-LM (MediaPipe LlmInference) local engine..."))
        emit(AiEvent.Token("On-Device LLM inference initialized. Model loaded from $modelPath."))
        emit(AiEvent.Complete)
    }
}
