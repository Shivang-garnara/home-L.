package com.example.launcherai

import com.example.launcherai.ai.bridge.AiToolBridge
import com.example.launcherai.ai.provider.AiEvent
import com.example.launcherai.ai.provider.AiProviderRegistry
import com.example.launcherai.ai.provider.ClaudeAiProvider
import com.example.launcherai.ai.provider.ClaudeModels
import com.example.launcherai.ai.provider.GeminiAiProvider
import com.example.launcherai.ai.provider.LocalAgentProvider
import com.example.launcherai.ai.provider.OnDeviceLlmProvider
import com.example.launcherai.ai.provider.OpenAiProvider
import com.example.launcherai.ai.provider.OpenAiModels
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.nio.charset.StandardCharsets

class Phase8CMultiProviderTest {

    @Test
    fun testProviderRegistryInstances() {
        val gemini = AiProviderRegistry.getProvider(AiProviderRegistry.PROVIDER_GEMINI)
        val openai = AiProviderRegistry.getProvider(AiProviderRegistry.PROVIDER_OPENAI)
        val claude = AiProviderRegistry.getProvider(AiProviderRegistry.PROVIDER_CLAUDE)
        val local = AiProviderRegistry.getProvider(AiProviderRegistry.PROVIDER_LOCAL)
        val onDevice = AiProviderRegistry.getProvider(AiProviderRegistry.PROVIDER_ON_DEVICE_LLM)

        assertTrue(gemini is GeminiAiProvider)
        assertTrue(openai is OpenAiProvider)
        assertTrue(claude is ClaudeAiProvider)
        assertTrue(local is LocalAgentProvider)
        assertTrue(onDevice is OnDeviceLlmProvider)

        val allProviders = AiProviderRegistry.getAllProviders()
        assertEquals(5, allProviders.size)
    }

    @Test
    fun testOpenAiModelsCatalog() {
        assertTrue(OpenAiModels.isValidModel("gpt-4o"))
        assertTrue(OpenAiModels.isValidModel("gpt-4o-mini"))
        assertTrue(OpenAiModels.isValidModel("o1"))
        assertTrue(OpenAiModels.isValidModel("o3-mini"))
        assertFalse(OpenAiModels.isValidModel("non-existent-gpt-model"))
        assertEquals("gpt-4o", OpenAiModels.getDefaultModel())
    }

    @Test
    fun testClaudeModelsCatalog() {
        assertTrue(ClaudeModels.isValidModel("claude-3-7-sonnet-latest"))
        assertTrue(ClaudeModels.isValidModel("claude-3-5-sonnet-latest"))
        assertTrue(ClaudeModels.isValidModel("claude-3-5-haiku-latest"))
        assertTrue(ClaudeModels.isValidModel("claude-3-opus-latest"))
        assertTrue(ClaudeModels.isValidModel("claude-3-5-sonnet-20241022"))
        assertFalse(ClaudeModels.isValidModel("non-existent-claude-model"))
        assertEquals("claude-3-7-sonnet-latest", ClaudeModels.getDefaultModel())
    }

    @Test
    fun testOpenAiSseTokenAndToolCallParsing() = runBlocking {
        val provider = OpenAiProvider(apiKey = "dummy_openai_key")
        assertTrue(provider.isConfigured)

        val sseData = """
            data: {"choices":[{"delta":{"content":"Hello from GPT-4o!"}}]}

            data: {"choices":[{"delta":{"tool_calls":[{"function":{"name":"search_device_packages","arguments":"{\"query\":\"camera\"}"}}]}}]}

            data: [DONE]
        """.trimIndent()

        val inputStream = ByteArrayInputStream(sseData.toByteArray(StandardCharsets.UTF_8))
        val events = provider.parseSseInputStream(inputStream).toList()

        assertEquals(2, events.size)
        assertTrue(events[0] is AiEvent.Token)
        assertEquals("Hello from GPT-4o!", (events[0] as AiEvent.Token).text)

        assertTrue(events[1] is AiEvent.ToolCall)
        val toolCall = events[1] as AiEvent.ToolCall
        assertEquals("search_device_packages", toolCall.name)
        assertEquals("camera", toolCall.params["query"])
    }

    @Test
    fun testClaudeSseTokenAndToolUseParsing() = runBlocking {
        val provider = ClaudeAiProvider(apiKey = "dummy_claude_key")
        assertTrue(provider.isConfigured)

        val sseData = """
            data: {"type":"content_block_delta","delta":{"type":"text_delta","text":"Hello from Claude 3.7 Sonnet!"}}

            data: {"type":"content_block_start","content_block":{"type":"tool_use","name":"launch_application","input":{"packageName":"com.example.camera"}}}

            data: [DONE]
        """.trimIndent()

        val inputStream = ByteArrayInputStream(sseData.toByteArray(StandardCharsets.UTF_8))
        val events = provider.parseSseInputStream(inputStream).toList()

        assertEquals(2, events.size)
        assertTrue(events[0] is AiEvent.Token)
        assertEquals("Hello from Claude 3.7 Sonnet!", (events[0] as AiEvent.Token).text)

        assertTrue(events[1] is AiEvent.ToolCall)
        val toolCall = events[1] as AiEvent.ToolCall
        assertEquals("launch_application", toolCall.name)
        assertEquals("com.example.camera", toolCall.params["packageName"])
    }

    @Test
    fun testOpenAiHeaderAuthenticationNoKeyInUrl() = runBlocking {
        var requestedUrl = ""
        val mockFactory: (String) -> HttpURLConnection = { urlStr ->
            requestedUrl = urlStr
            object : HttpURLConnection(java.net.URL(urlStr)) {
                override fun getResponseCode(): Int = 200
                override fun getOutputStream(): java.io.OutputStream = java.io.ByteArrayOutputStream()
                override fun getInputStream(): java.io.InputStream = ByteArrayInputStream("data: {\"choices\":[{\"delta\":{\"content\":\"GPT Response\"}}]}".toByteArray(StandardCharsets.UTF_8))
                override fun disconnect() {}
                override fun usingProxy(): Boolean = false
                override fun connect() {}
            }
        }

        val provider = OpenAiProvider(
            apiKey = "SECRET_OPENAI_KEY_9999",
            model = "gpt-4o",
            httpConnectionFactory = mockFactory
        )

        val events = provider.streamPrompt("Test prompt").toList()

        assertFalse("API key should not be exposed in URL query params", requestedUrl.contains("key="))
        assertTrue("URL must target completions endpoint", requestedUrl.contains("v1/chat/completions"))
        assertTrue(events.any { it is AiEvent.Token })
    }

    @Test
    fun testClaudeHeaderAuthenticationNoKeyInUrl() = runBlocking {
        var requestedUrl = ""
        val mockFactory: (String) -> HttpURLConnection = { urlStr ->
            requestedUrl = urlStr
            object : HttpURLConnection(java.net.URL(urlStr)) {
                override fun getResponseCode(): Int = 200
                override fun getOutputStream(): java.io.OutputStream = java.io.ByteArrayOutputStream()
                override fun getInputStream(): java.io.InputStream = ByteArrayInputStream("data: {\"type\":\"content_block_delta\",\"delta\":{\"type\":\"text_delta\",\"text\":\"Claude Response\"}}".toByteArray(StandardCharsets.UTF_8))
                override fun disconnect() {}
                override fun usingProxy(): Boolean = false
                override fun connect() {}
            }
        }

        val provider = ClaudeAiProvider(
            apiKey = "SECRET_CLAUDE_KEY_8888",
            model = "claude-3-7-sonnet-latest",
            httpConnectionFactory = mockFactory
        )

        val events = provider.streamPrompt("Test prompt").toList()

        assertFalse("API key should not be exposed in URL query params", requestedUrl.contains("key="))
        assertTrue("URL must target messages endpoint", requestedUrl.contains("v1/messages"))
        assertTrue(events.any { it is AiEvent.Token })
    }

    @Test
    fun testModelCapabilitiesMetadata() {
        val geminiCaps = com.example.launcherai.ai.provider.ModelCapabilities.getCapabilities("google_gemini", "gemini-2.5-pro")
        assertTrue(geminiCaps.supportsStreaming)
        assertTrue(geminiCaps.supportsFunctionCalling)
        assertTrue(geminiCaps.supportsReasoning)

        val claudeCaps = com.example.launcherai.ai.provider.ModelCapabilities.getCapabilities("anthropic_claude", "claude-3-7-sonnet-latest")
        assertTrue(claudeCaps.supportsStreaming)
        assertTrue(claudeCaps.supportsFunctionCalling)
        assertTrue(claudeCaps.supportsReasoning)

        val openAiCaps = com.example.launcherai.ai.provider.ModelCapabilities.getCapabilities("openai_gpt", "gpt-4o")
        assertTrue(openAiCaps.supportsStreaming)
        assertTrue(openAiCaps.supportsFunctionCalling)
        assertTrue(openAiCaps.supportsMultimodal)
    }

    @Test
    fun testAllProvidersUseSharedNativeToolBridge() {
        val tools = AiToolBridge.getAllTools()
        assertEquals(5, tools.size)
        val toolNames = tools.map { it.name }
        assertTrue(toolNames.contains("search_device_packages"))
        assertTrue(toolNames.contains("launch_application"))
        assertTrue(toolNames.contains("modify_workspace_grid"))
        assertTrue(toolNames.contains("toggle_hidden_package"))
        assertTrue(toolNames.contains("create_workspace_folder"))
    }
}
