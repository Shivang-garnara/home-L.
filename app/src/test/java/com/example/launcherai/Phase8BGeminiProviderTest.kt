package com.example.launcherai

import com.example.launcherai.ai.provider.AiEvent
import com.example.launcherai.ai.provider.GeminiAiProvider
import com.example.launcherai.ai.provider.GeminiModels
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.nio.charset.StandardCharsets

class Phase8BGeminiProviderTest {

    @Test
    fun testGeminiModelsCatalog() {
        assertTrue(GeminiModels.isValidModel("gemini-1.5-flash"))
        assertTrue(GeminiModels.isValidModel("gemini-1.5-pro"))
        assertTrue(GeminiModels.isValidModel("gemini-1.5-flash-8b"))
        assertTrue(GeminiModels.isValidModel("gemini-2.0-flash"))
        assertTrue(GeminiModels.isValidModel("gemini-2.0-flash-lite-preview-02-05"))

        assertTrue(GeminiModels.isPreviewModel("gemini-2.0-flash-lite-preview-02-05"))
        assertFalse(GeminiModels.isPreviewModel("gemini-1.5-flash"))

        assertFalse(GeminiModels.isValidModel("retired-gemini-model-v0"))
        assertFalse(GeminiModels.isValidModel("gemini-3.8-flash"))
        assertEquals("gemini-2.5-flash", GeminiModels.getDefaultModel())
    }

    @Test
    fun testHeaderAuthenticationNoKeyInUrl() = runBlocking {
        var requestedUrl = ""
        val mockFactory: (String) -> HttpURLConnection = { urlStr ->
            requestedUrl = urlStr
            object : HttpURLConnection(java.net.URL(urlStr)) {
                override fun getResponseCode(): Int = 200
                override fun getOutputStream(): java.io.OutputStream = java.io.ByteArrayOutputStream()
                override fun getInputStream(): java.io.InputStream = ByteArrayInputStream("data: {\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"OK\"}]}}]}".toByteArray(StandardCharsets.UTF_8))
                override fun disconnect() {}
                override fun usingProxy(): Boolean = false
                override fun connect() {}
            }
        }

        val provider = GeminiAiProvider(
            apiKey = "SECRET_API_KEY_12345",
            model = "gemini-1.5-flash",
            httpConnectionFactory = mockFactory
        )

        val events = provider.streamPrompt("Test prompt").toList()

        // Verify URL does NOT transmit key in query parameter string
        assertFalse("API key should not be exposed in URL query params", requestedUrl.contains("key="))
        assertTrue("URL must target streamGenerateContent endpoint", requestedUrl.contains("streamGenerateContent?alt=sse"))
        assertTrue(events.any { it is AiEvent.Token })
    }

    @Test
    fun testUnconfiguredGeminiProviderEmitsError() = runBlocking {
        val provider = GeminiAiProvider(apiKey = "")
        assertFalse(provider.isConfigured)

        val events = provider.streamPrompt("Search apps").toList()
        assertTrue(events.any { it is AiEvent.Error })
        val errorEvent = events.first { it is AiEvent.Error } as AiEvent.Error
        assertTrue(errorEvent.errorMessage.contains("not configured"))
    }

    @Test
    fun testParseSseInputStreamTextTokens() = runBlocking {
        val provider = GeminiAiProvider(apiKey = "dummy_key")
        assertTrue(provider.isConfigured)

        val sseData = """
            data: {"candidates":[{"content":{"parts":[{"text":"Hello, "}]}}]}

            data: {"candidates":[{"content":{"parts":[{"text":"I am Gemini AI!"}]}}]}

            data: [DONE]
        """.trimIndent()

        val inputStream = ByteArrayInputStream(sseData.toByteArray(StandardCharsets.UTF_8))
        val events = provider.parseSseInputStream(inputStream).toList()

        assertEquals(2, events.size)
        assertTrue(events[0] is AiEvent.Token)
        assertEquals("Hello, ", (events[0] as AiEvent.Token).text)
        assertEquals("I am Gemini AI!", (events[1] as AiEvent.Token).text)
    }

    @Test
    fun testParseSseInputStreamFunctionToolCall() = runBlocking {
        val provider = GeminiAiProvider(apiKey = "dummy_key")

        val sseData = """
            data: {"candidates":[{"content":{"parts":[{"functionCall":{"name":"search_device_packages","args":{"query":"camera"}}}]}}]}
        """.trimIndent()

        val inputStream = ByteArrayInputStream(sseData.toByteArray(StandardCharsets.UTF_8))
        val events = provider.parseSseInputStream(inputStream).toList()

        assertEquals(1, events.size)
        assertTrue(events[0] is AiEvent.ToolCall)
        val toolCall = events[0] as AiEvent.ToolCall
        assertEquals("search_device_packages", toolCall.name)
        assertEquals("camera", toolCall.params["query"])
    }

    @Test
    fun testParseSseInputStreamGridModifyToolCall() = runBlocking {
        val provider = GeminiAiProvider(apiKey = "dummy_key")

        val sseData = """
            data: {"candidates":[{"content":{"parts":[{"functionCall":{"name":"modify_workspace_grid","args":{"cols":5,"rows":6}}}]}}]}
        """.trimIndent()

        val inputStream = ByteArrayInputStream(sseData.toByteArray(StandardCharsets.UTF_8))
        val events = provider.parseSseInputStream(inputStream).toList()

        assertEquals(1, events.size)
        assertTrue(events[0] is AiEvent.ToolCall)
        val toolCall = events[0] as AiEvent.ToolCall
        assertEquals("modify_workspace_grid", toolCall.name)
        assertEquals(5, toolCall.params["cols"])
        assertEquals(6, toolCall.params["rows"])
    }

    @Test
    fun testParseModelsListJson() {
        val provider = GeminiAiProvider(apiKey = "test_key")
        val sampleJson = """
            {
              "models": [
                {
                  "name": "models/gemini-1.5-flash",
                  "supportedGenerationMethods": ["generateContent"]
                },
                {
                  "name": "models/gemini-1.5-pro",
                  "supportedGenerationMethods": ["generateContent"]
                },
                {
                  "name": "models/embedding-001",
                  "supportedGenerationMethods": ["embedContent"]
                }
              ]
            }
        """.trimIndent()

        val models = provider.parseModelsListJson(sampleJson)
        assertTrue(models.contains("gemini-1.5-flash"))
        assertTrue(models.contains("gemini-1.5-pro"))
        assertFalse(models.contains("embedding-001"))
    }

    @Test
    fun testHttp404ModelNotFoundHandling() = runBlocking {
        val mockFactory: (String) -> HttpURLConnection = { urlStr ->
            object : HttpURLConnection(java.net.URL(urlStr)) {
                override fun getResponseCode(): Int = 404
                override fun getOutputStream(): java.io.OutputStream = java.io.ByteArrayOutputStream()
                override fun getInputStream(): java.io.InputStream = ByteArrayInputStream(ByteArray(0))
                override fun disconnect() {}
                override fun usingProxy(): Boolean = false
                override fun connect() {}
            }
        }

        val provider = GeminiAiProvider(
            apiKey = "test_key",
            model = "invalid-model",
            httpConnectionFactory = mockFactory
        )

        val events = provider.streamPrompt("Hi").toList()
        assertTrue(events.any { it is AiEvent.Error })
        val errorEvent = events.first { it is AiEvent.Error } as AiEvent.Error
        assertTrue(errorEvent.errorMessage.contains("not found or retired"))
    }
}
