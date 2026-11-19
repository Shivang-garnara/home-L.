package com.example.launcherai.ai.provider

/**
 * Centralized Multi-Provider Registry matching Nova Launcher v8.9.6 reference architecture.
 * Manages provider selection, API configurations, and model synchronization across
 * Google Gemini, OpenAI GPT, Anthropic Claude, and Local On-Device Agent.
 */
object AiProviderRegistry {

    const val PROVIDER_LOCAL = "local_agent"
    const val PROVIDER_GEMINI = "google_gemini"
    const val PROVIDER_OPENAI = "openai_gpt"
    const val PROVIDER_CLAUDE = "anthropic_claude"
    const val PROVIDER_ON_DEVICE_LLM = "on_device_llm"

    val AVAILABLE_PROVIDERS = arrayOf(
        PROVIDER_LOCAL,
        PROVIDER_GEMINI,
        PROVIDER_OPENAI,
        PROVIDER_CLAUDE,
        PROVIDER_ON_DEVICE_LLM
    )

    private val providers = mutableMapOf<String, AiProvider>(
        PROVIDER_LOCAL to LocalAgentProvider(),
        PROVIDER_GEMINI to GeminiAiProvider(),
        PROVIDER_OPENAI to OpenAiProvider(),
        PROVIDER_CLAUDE to ClaudeAiProvider(),
        PROVIDER_ON_DEVICE_LLM to OnDeviceLlmProvider()
    )

    fun getProvider(providerId: String): AiProvider {
        return providers[providerId] ?: providers[PROVIDER_LOCAL]!!
    }

    fun registerProvider(id: String, provider: AiProvider) {
        providers[id] = provider
    }

    fun getAllProviders(): List<AiProvider> {
        return providers.values.toList()
    }
}
