package com.example.launcherai.ai.provider

import kotlinx.coroutines.flow.Flow

/**
 * Streaming response interface supporting remote and local AI inference providers.
 */
interface AiProvider {
    val providerName: String
    val isConfigured: Boolean

    fun streamPrompt(prompt: String): Flow<AiEvent>
}
