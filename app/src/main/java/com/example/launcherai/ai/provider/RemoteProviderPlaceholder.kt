package com.example.launcherai.ai.provider

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class RemoteProviderPlaceholder : AiProvider {
    override val providerName: String = "Instabridge Remote Cloud AI"
    override val isConfigured: Boolean = false

    override fun streamPrompt(prompt: String): Flow<AiEvent> = flow {
        emit(AiEvent.Error("Remote Instabridge AI provider disconnected. Server-side API key configuration is required."))
        emit(AiEvent.Complete)
    }
}
